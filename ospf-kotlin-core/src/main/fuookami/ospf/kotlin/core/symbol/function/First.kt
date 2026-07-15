@file:Suppress("unused")

/** 取首元素函数符号 / First element function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 首选函数符号 / First function symbol
 *
 * 提供 [FirstFunction]，从候选列表中选择第一个满足条件的元素。
 *
 * Provides [FirstFunction] for selecting the first satisfying element from a candidate list.
*/

/**
 * 首元素函数：返回列表中第一个计算值 > 0 的多项式的索引。
 * FirstFunction - Returns the index of the first polynomial in the list that evaluates to > 0.
 *
 * 对每个多项式，BinaryzationFunction 创建二值 bin[i]（当 polynomial[i] > 0 时为 1）。
 * For each polynomial, a BinaryzationFunction creates binary bin[i] (1 if polynomial[i] > 0).
 * 输出二值数组 y[0..n-1]，其中 y[i]=1 表示"多项式 i 是第一个非零的"。
 * Output binary array y[0..n-1] where y[i]=1 means "polynomial i is the first nonzero".
 *
 * 约束：
 * - y[i] <= bin[i] 对每个 i（只有非零才能成为第一个）
 * - y[0] >= bin[0]（如果第一个多项式非零，则它是第一个）
 * - y[i] >= bin[i] - sum(y[0]..y[i-1]) 对 i > 0
 * - y[i] <= y[i-1] 对 i > 0（单调性）
 *
 * Constraints:
 * - y[i] <= bin[i] for each i (can only be first if it's nonzero)
 * - y[0] >= bin[0] (if first polynomial is nonzero, it's the first)
 * - y[i] >= bin[i] - sum(y[0]..y[i-1]) for i > 0
 * - y[i] <= y[i-1] for i > 0 (monotonicity)
 *
 * 输出：result = sum(i * y[i]) + n * (1 - sum(y[i])) = 第一个非零元素的索引，若无则为 n
 * Output: result = sum(i * y[i]) + n * (1 - sum(y[i])) = index of first nonzero, or n if none
 *
 * @property polynomials 输入线性多项式列表 / list of input linear polynomials
 * @property epsilon 非零阈值 / nonzero threshold
 * @param converter 值类型转换器 / value type converter
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
*/
class FirstFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    val epsilon: Flt64 = Flt64(1e-6),
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {

    private val n: Int get() = polynomials.size

    // BinaryzationFunction for each polynomial / 为每个多项式创建二值化函数
    private val binaryFunctions: List<BinaryzationFunction<V>> by lazy {
        polynomials.mapIndexed { i, poly ->
            BinaryzationFunction(
                polynomial = poly,
                converter = converter,
                name = "${name}_bin_$i"
            )
        }
    }

    // Output binary array y[i] = 1 means polynomial i is the first nonzero / 输出二值数组 y[i]=1 表示多项式 i 是第一个非零的
    private val _yVars: BinVariable1 by lazy {
        BinVariable1("${name}_first", Shape1(n))
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOfNotNull(*binaryFunctions.flatMap { it.helperVariables }.toTypedArray()) + _yVars.items

    /**
     * 结果多项式：sum(i * y[i]) + n * (1 - sum(y[i]))
     * Result polynomial: sum(i * y[i]) + n * (1 - sum(y[i]))
     * 返回第一个非零多项式的索引，若无则返回 n。
     * Returns the index of the first nonzero polynomial, or n if none.
    */
    val result: LinearPolynomial<V> by lazy {
        // sum(i * y[i]) / i * y[i] 的加权求和
        val one = converter.one
        val indexedMonos = _yVars.items.mapIndexed { i, yi ->
            LinearMonomial(repeatAdd(one, i), yi)
        }
        // n * (1 - sum(y[i])) = n - n * sum(y[i]) / n * (1 - sum(y[i])) = n - n * sum(y[i])
        val nVal = repeatAdd(one, n)
        val nSumMonos = _yVars.items.map { LinearMonomial(-nVal, it) }
        val indexedPlus = indexedMonos + nSumMonos
        LinearPolynomial(indexedPlus, nVal)
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val one = converter.one
        val epsilonValue = converter.intoValue(epsilon)
        for ((i, poly) in polynomials.withIndex()) {
            val value = poly.evaluateWith(values) ?: return null
            if (value gr epsilonValue) {
                return repeatAdd(one, i)
            }
        }
        return repeatAdd(one, n)
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        // Register all binary function constraints (tokens already registered in registerAuxiliaryTokens)
        // 注册所有二值化函数约束（token 已在 registerAuxiliaryTokens 中注册）
        for (binFunc in binaryFunctions) {
            when (val r = binFunc.registerConstraints(model)) {
                is Ok -> {}
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }
        }

        val zero = converter.zero
        val one = converter.one
        val allConstraints = mutableListOf<LinearInequality<V>>()

        for (i in polynomials.indices) {
            val binResult = binaryFunctions[i].resultVar
            val yi = _yVars[i]

            // y[i] <= bin[i] / y[i] 小于等于 bin[i]
            allConstraints += LinearInequality(
                LinearPolynomial(listOf(LinearMonomial(one, yi)), zero),
                LinearPolynomial(listOf(LinearMonomial(one, binResult)), zero),
                Comparison.LE, "${name}_ub1_$i"
            )

            if (i == 0) {
                // y[0] >= bin[0] / y[0] 大于等于 bin[0]
                allConstraints += LinearInequality(
                    LinearPolynomial(listOf(LinearMonomial(one, yi)), zero),
                    LinearPolynomial(listOf(LinearMonomial(one, binResult)), zero),
                    Comparison.GE, "${name}_lb_0"
                )
            } else {
                // y[i] >= bin[i] - sum(y[0]..y[i-1])
                // y[i] >= bin[i] - sum(y[0]..y[i-1])
                // => y[i] + sum(y[0]..y[i-1]) >= bin[i]
                // => y[i] + sum(y[0]..y[i-1]) >= bin[i]
                val prevYMonos = (0 until i).map { j -> LinearMonomial(one, _yVars[j]) }
                val lhsMonos = listOf(LinearMonomial(one, yi)) + prevYMonos
                allConstraints += LinearInequality(
                    LinearPolynomial(lhsMonos, zero),
                    LinearPolynomial(listOf(LinearMonomial(one, binResult)), zero),
                    Comparison.GE, "${name}_lb_$i"
                )

                // y[i] <= y[i-1] (monotonicity) / y[i] <= y[i-1]（单调性约束）
                allConstraints += LinearInequality(
                    LinearPolynomial(listOf(LinearMonomial(one, yi)), zero),
                    LinearPolynomial(listOf(LinearMonomial(one, _yVars[i - 1])), zero),
                    Comparison.LE, "${name}_y_$i"
                )
            }
        }

        return addConstraints(model, allConstraints) ?: ok
    }
    companion object {
        /**
         * 创建首元素函数实例 / Create a first function instance
         * @param polynomials 输入线性多项式列表 / list of input linear polynomials
         * @param epsilon 非零阈值 / nonzero threshold
         * @param converter 值类型转换器 / value type converter
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return [FirstFunction] 实例 / [FirstFunction] instance
        */
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            epsilon: Flt64 = Flt64(1e-6),
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): FirstFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            FirstFunction(polynomials, epsilon, converter, name, displayName)
    }
}
