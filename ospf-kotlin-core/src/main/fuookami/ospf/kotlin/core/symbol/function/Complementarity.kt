@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*

private data class ComplementarityPlan<V>(
    val leftBounds: ConditionBounds<V>,
    val rightBounds: ConditionBounds<V>,
    val observedLeftBounds: ConditionBounds<V>?,
    val observedRightBounds: ConditionBounds<V>?
) where V : RealNumber<V>, V : NumberField<V>

/**
 * 有限非负线性表达式 `x`、`y` 中至多一个可以为正，用一个二值变量和两条上界约束精确表示。
 * At most one of two finite nonnegative linear expressions can be positive; one binary variable and two upper-bound rows express this exactly.
 *
 * 本类表示约束而不是数值结果，因此不暴露结果多项式。
 * This symbol represents a constraint and therefore has no result polynomial.
 *
 * @property x 第一个有限非负线性表达式 / first finite nonnegative linear expression
 * @property y 第二个有限非负线性表达式 / second finite nonnegative linear expression
 * @property selector 互斥分支变量 / mutual-exclusion selector
 * @param converter 数值类型转换器 / numeric value converter
 * @param xBounds 可选的 x 有限界 / optional finite bounds for x
 * @param yBounds 可选的 y 有限界 / optional finite bounds for y
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class ComplementarityFunction<V>(
    x: LinearPolynomial<V>,
    y: LinearPolynomial<V>,
    private val converter: IntoValue<V>,
    private val xBounds: ConditionBounds<V>? = null,
    private val yBounds: ConditionBounds<V>? = null,
    override var name: String = "complementarity",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    val x: LinearPolynomial<V> = LinearPolynomial(x.monomials.toList(), x.constant)
    val y: LinearPolynomial<V> = LinearPolynomial(y.monomials.toList(), y.constant)
    private var plan: ComplementarityPlan<V>? = null

    val selector: BinVar = BinVar("${name}_complementarity")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(selector)

    /** 返回互斥约束的违规指示值；没有正值冲突时为零。 / Return one when both expressions are positive, otherwise zero. */
    override fun evaluate(values: Map<Symbol, V>): V? {
        val current = resolvePlan() ?: return null
        return try {
            val leftValue = x.evaluateWith(values) ?: return null
            val rightValue = y.evaluateWith(values) ?: return null
            if (!within(leftValue, current.leftBounds) || !within(rightValue, current.rightBounds)) return null
            if (leftValue.compareTo(converter.zero) < 0 || rightValue.compareTo(converter.zero) < 0) return null
            if (leftValue.compareTo(converter.zero) > 0 && rightValue.compareTo(converter.zero) > 0) converter.one else converter.zero
        } catch (_: RuntimeException) {
            null
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        val current = resolvePlan()
            ?: return complementarityFailure("互斥正值函数要求有限非负输入范围。 / Mutual exclusivity requires finite nonnegative input bounds.")
        if (!rangesRemainCovered(current)) {
            return complementarityFailure("互斥正值输入范围超出了已捕获范围。 / Mutual-exclusion input bounds exceed the captured bounds.")
        }
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val current = resolvePlan()
            ?: return complementarityFailure("互斥正值函数要求有限非负输入范围。 / Mutual exclusivity requires finite nonnegative input bounds.")
        if (!rangesRemainCovered(current)) {
            return complementarityFailure("互斥正值输入范围超出了已捕获范围。 / Mutual-exclusion input bounds exceed the captured bounds.")
        }
        val constraints = try {
            buildConstraints(current)
        } catch (_: RuntimeException) {
            return complementarityFailure("互斥正值约束系数无效或溢出。 / Mutual-exclusion coefficients are invalid or overflowed.")
        }
        if (!constraints.all { row ->
                hasUsableConditionFlattenedValues(row.lhs, converter) &&
                    hasUsableConditionFlattenedValues(row.rhs, converter)
            }) {
            return complementarityFailure("互斥正值约束包含非有限系数。 / Mutual-exclusion constraints contain non-finite coefficients.")
        }
        return addConstraints(model, constraints) ?: ok
    }

    private fun resolvePlan(): ComplementarityPlan<V>? {
        plan?.let { return it }
        return try {
            val observedLeft = x.finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) }
            val observedRight = y.finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) }
            val leftRange = xBounds ?: observedLeft ?: return null
            val rightRange = yBounds ?: observedRight ?: return null
            if (!validNonnegativeBounds(leftRange) || !validNonnegativeBounds(rightRange)) return null
            ComplementarityPlan(leftRange, rightRange, observedLeft, observedRight).also { plan = it }
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun rangesRemainCovered(current: ComplementarityPlan<V>): Boolean {
        val observedLeft = x.finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) }
        val observedRight = y.finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) }
        if (observedLeft == null && current.observedLeftBounds != null) return false
        if (observedRight == null && current.observedRightBounds != null) return false
        if (observedLeft != null && current.observedLeftBounds != null && !contains(current.observedLeftBounds, observedLeft)) return false
        if (observedRight != null && current.observedRightBounds != null && !contains(current.observedRightBounds, observedRight)) return false
        return true
    }

    private fun buildConstraints(current: ComplementarityPlan<V>): List<LinearInequality<V>> {
        val zero = converter.zero
        val leftSelector = LinearMonomial(-current.leftBounds.upper, selector)
        val rightSelector = LinearMonomial(current.rightBounds.upper, selector)
        return listOf(
            LinearInequality(
                lhs = x,
                rhs = LinearPolynomial(emptyList(), current.leftBounds.upper),
                comparison = Comparison.LE,
                name = "${name}_x_domain_upper"
            ),
            LinearInequality(
                lhs = x,
                rhs = LinearPolynomial(emptyList(), current.leftBounds.lower),
                comparison = Comparison.GE,
                name = "${name}_x_domain_lower"
            ),
            LinearInequality(
                lhs = y,
                rhs = LinearPolynomial(emptyList(), current.rightBounds.upper),
                comparison = Comparison.LE,
                name = "${name}_y_domain_upper"
            ),
            LinearInequality(
                lhs = y,
                rhs = LinearPolynomial(emptyList(), current.rightBounds.lower),
                comparison = Comparison.GE,
                name = "${name}_y_domain_lower"
            ),
            LinearInequality(
                lhs = LinearPolynomial(x.monomials + leftSelector, x.constant),
                rhs = LinearPolynomial(emptyList(), zero),
                comparison = Comparison.LE,
                name = "${name}_x_mutex"
            ),
            LinearInequality(
                lhs = LinearPolynomial(y.monomials + rightSelector, y.constant),
                rhs = LinearPolynomial(emptyList(), current.rightBounds.upper),
                comparison = Comparison.LE,
                name = "${name}_y_mutex"
            )
        )
    }

    private fun within(value: V, bounds: ConditionBounds<V>): Boolean {
        return value.compareTo(bounds.lower) >= 0 && value.compareTo(bounds.upper) <= 0
    }

    private fun validNonnegativeBounds(bounds: ConditionBounds<V>): Boolean {
        return bounds.lower.compareTo(converter.zero) >= 0 &&
            bounds.lower.compareTo(bounds.upper) <= 0 &&
            isUsableConditionBound(bounds.lower, converter) &&
            isUsableConditionBound(bounds.upper, converter)
    }

    private fun contains(outer: ConditionBounds<V>, inner: ConditionBounds<V>): Boolean {
        return outer.lower.compareTo(inner.lower) <= 0 && outer.upper.compareTo(inner.upper) >= 0
    }

    companion object {
        /** 创建互斥正值约束函数。 / Create a mutual-exclusion function symbol.
         * @param x 第一个有限非负线性表达式 / first finite nonnegative linear expression
         * @param y 第二个有限非负线性表达式 / second finite nonnegative linear expression
         * @param converter 数值类型转换器 / numeric value converter
         * @param xBounds 可选的 x 有限界 / optional finite bounds for x
         * @param yBounds 可选的 y 有限界 / optional finite bounds for y
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return 互斥正值约束函数符号 / mutual-exclusion function symbol
         */
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            y: LinearPolynomial<V>,
            converter: IntoValue<V>,
            xBounds: ConditionBounds<V>? = null,
            yBounds: ConditionBounds<V>? = null,
            name: String = "complementarity",
            displayName: String? = null
        ): ComplementarityFunction<V> where V : RealNumber<V>, V : NumberField<V> {
            return ComplementarityFunction(
                x = x,
                y = y,
                converter = converter,
                xBounds = xBounds,
                yBounds = yBounds,
                name = name,
                displayName = displayName
            )
        }
    }
}

/** 两个非负表达式不能同时为正的函数别名。 / Alias for two nonnegative expressions that cannot both be positive. */
typealias MutuallyExclusivePositiveFunction<V> = ComplementarityFunction<V>

private fun <T> complementarityFailure(message: String): Ret<T> {
    return Failed(ErrorCode.IllegalArgument, message)
}
