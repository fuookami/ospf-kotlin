@file:Suppress("unused")

/** 区间条件函数符号 / If-in-range condition function symbol */
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
 * 区间条件函数符号 / If-In function symbol
 *
 * 提供 [IfInFunction]，实现 y = (a <= x <= b ? 1 : 0) 的线性化建模。
 *
 * Provides [IfInFunction] for linearized modeling of y = 1 if a <= x <= b, else y = 0.
 */

/**
 * 区间条件函数：当 a <= x <= b 时 y = 1，否则 y = 0。
 * If-In function: `y = 1 if a <= x <= b, else y = 0`.
 *
 * 使用两个二值指示变量进行下界和上界检查，通过类 AND 约束组合。
 * Uses two binary indicators for the lower and upper bound checks,
 * combined via an AND-like constraint.
 *
 * @property x 输入线性多项式 / the input linear polynomial
 * @property lower 下界 (a) / the lower bound (a)
 * @property upper 上界 (b) / the upper bound (b)
 * @param converter 值类型转换器 / value type converter
 * @param bigM Big-M 界限（默认从两侧差值范围推导，失败时回退到 1e6）/ Big-M bound (inferred from both side-difference ranges by default, falls back to 1e6)
 * @param tolerance 零容差（默认 1e-6）/ zero tolerance (default 1e-6)
 * @param strictBoundary 严格边界值（默认 0.5）/ strict boundary value (default 0.5)
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class IfInFunction<V>(
    val x: LinearPolynomial<V>,
    val lower: V,
    val upper: V,
    converter: IntoValue<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "ifin",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val explicitBigM: V? = bigM
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_ifin")
    val geVar: AbstractVariableItem<*, *> = BinVar("${name}_ge")
    val leVar: AbstractVariableItem<*, *> = BinVar("${name}_le")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, geVar, leVar)

    val result: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xValue = x.evaluateWith(values) ?: return null
        return if (!(xValue ls lower) && !(xValue gr upper)) converter.one else converter.zero
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

        // x - lower >= 0 indicator (x >= lower) / x - lower >= 0 指示约束（x >= 下界）
        val xMinusLower = LinearPolynomial(x.monomials, x.constant - lower)
        allConstraints += nonnegativeIndicatorConstraints(
            xMinusLower,
            geVar,
            explicitBigM ?: xMinusLower.defaultBigM(converter),
            tolerance,
            "${name}_ge"
        )

        // upper - x >= 0 indicator (x <= upper) / upper - x >= 0 指示约束（x <= 上界）
        val upperMinusX = LinearPolynomial(x.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }, -x.constant + upper)
        allConstraints += nonnegativeIndicatorConstraints(
            upperMinusX,
            leVar,
            explicitBigM ?: upperMinusX.defaultBigM(converter),
            tolerance,
            "${name}_le"
        )

        // result = ge AND le: result <= ge, result <= le, result >= ge + le - 1 / 结果 = ge AND le：result <= ge, result <= le, result >= ge + le - 1
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(LinearMonomial(one, resultVar), LinearMonomial(-one, geVar)), zero),
            LinearPolynomial(emptyList(), zero),
            Comparison.LE, "${name}_link_ge"
        )

        allConstraints += LinearInequality(
            LinearPolynomial(listOf(LinearMonomial(one, resultVar), LinearMonomial(-one, leVar)), zero),
            LinearPolynomial(emptyList(), zero),
            Comparison.LE, "${name}_link_le"
        )

        allConstraints += LinearInequality(
            LinearPolynomial(
                listOf(LinearMonomial(one, resultVar), LinearMonomial(-one, geVar), LinearMonomial(-one, leVar)),
                zero
            ),
            LinearPolynomial(emptyList(), -one),
            Comparison.GE, "${name}_link_lb"
        )

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        /**
         * 创建区间条件函数实例 / Create an if-in function instance
         * @param x 输入线性多项式 / input linear polynomial
         * @param lower 下界 / lower bound
         * @param upper 上界 / upper bound
         * @param converter 值类型转换器 / value type converter
         * @param bigM Big-M 界限 / Big-M bound
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return [IfInFunction] 实例 / [IfInFunction] instance
         */
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            lower: V,
            upper: V,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): IfInFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            IfInFunction(x, lower, upper, converter, bigM, name = name, displayName = displayName)
    }
}
