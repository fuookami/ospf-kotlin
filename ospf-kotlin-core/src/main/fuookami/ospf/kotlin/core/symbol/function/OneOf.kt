@file:Suppress("unused")

/** 选一函数符号 / One-of function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 唯一选择函数符号 / One-of function symbol
 *
 * 提供 [OneOfFunction]，实现恰好一个输入多项式非零的约束建模。
 *
 * Provides [OneOfFunction] for modeling the constraint that exactly one input polynomial is nonzero.
 */

/**
 * 选一函数：恰好一个输入多项式必须非零。
 * OneOf function: exactly one of the input polynomials must be nonzero.
 *
 * 结果：
 * Result:
 * - 若恰好一个输入多项式非零则 y = 1
 * - y = 1 if exactly one of the input polynomials is nonzero
 * - 否则 y = 0
 * - y = 0 otherwise
 *
 * 使用非零指示变量与类 XOR 链接约束。
 * Uses nonzero indicators with XOR-like linking constraints.
 *
 * @property polynomials 输入线性多项式列表 / the list of input linear polynomials
 * @param bigM Big-M 界限（默认从每个输入范围推导，失败时回退到 1e6）/ Big-M bound (inferred from each input range by default, falls back to 1e6)
 * @param tolerance 零容差（默认 1e-6）/ zero tolerance (default 1e-6)
 * @param strictBoundary 严格边界值（默认 0.5）/ strict boundary value (default 0.5)
 * @property converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class OneOfFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "oneof",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val explicitBigM: V? = bigM
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))
    private val n = polynomials.size

    init {
        require(n >= 1) { "OneOfFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_oneof")
    val indicatorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_oneof_nz${it}") }
    val sideVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_oneof_side${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + indicatorVars + sideVars

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun evaluate(values: Map<Symbol, V>): V? {
        var count = 0
        for (poly in polynomials) {
            val v = poly.evaluateWith(values) ?: return null
            if (v neq converter.zero) count++
        }
        return if (count == 1) converter.one else converter.zero
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val zero = converter.zero
        val one = converter.one
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // Nonzero indicators for each polynomial / 为每个多项式构建非零指示约束
        for (i in polynomials.indices) {
            val currentBigM = explicitBigM ?: polynomials[i].defaultBigM(converter)
            allConstraints += nonzeroIndicatorConstraints(
                polynomials[i],
                indicatorVars[i],
                sideVars[i],
                currentBigM,
                tolerance,
                strictBoundary,
                "${name}_oneof_nz_${i}"
            )
        }

        // Exactly one indicator must be 1: sum(indicators) = 1 / 恰好一个指示变量为 1：sum(指示变量) = 1
        val indMonos = indicatorVars.map { LinearMonomial(one, it) }
        allConstraints += LinearInequality(
            LinearPolynomial(indMonos, zero),
            LinearPolynomial(emptyList(), one), Comparison.EQ, "${name}_oneof_exactly_one")

        // result = 1 (since exactly one indicator must be 1) / 结果 = 1（因为恰好一个指示变量为 1）
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(LinearMonomial(one, resultVar)), zero),
            LinearPolynomial(emptyList(), one), Comparison.EQ, "${name}_oneof_result")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        /** 创建 [OneOfFunction] 实例。 / Create a [OneOfFunction] instance. */
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String = "oneof",
            displayName: String? = null
        ): OneOfFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            OneOfFunction(polynomials, bigM, converter = converter, name = name, displayName = displayName)
    }
}
