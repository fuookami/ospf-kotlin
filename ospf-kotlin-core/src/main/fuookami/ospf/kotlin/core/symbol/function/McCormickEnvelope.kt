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

private data class McCormickPlan<V>(
    val leftBounds: ConditionBounds<V>,
    val rightBounds: ConditionBounds<V>,
    val observedLeftBounds: ConditionBounds<V>?,
    val observedRightBounds: ConditionBounds<V>?,
    val resultBounds: ConditionBounds<V>
) where V : RealNumber<V>, V : NumberField<V>

/**
 * 为连续线性表达式乘积建立四条 McCormick 包络。结果变量是松弛变量，未保证等于实际乘积。
 * Adds four McCormick envelope inequalities for the product of two continuous linear expressions.
 * The result is a relaxation variable and is not guaranteed to equal the actual product.
 *
 * @property left 左侧线性表达式 / left linear expression
 * @property right 右侧线性表达式 / right linear expression
 * @property leftBounds 左侧表达式的有限界 / finite bounds for the left expression
 * @property rightBounds 右侧表达式的有限界 / finite bounds for the right expression
 * @property resultVar 松弛结果变量 / relaxed result variable
 * @param converter 数值类型转换器 / numeric value converter
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class McCormickEnvelopeFunction<V>(
    left: LinearPolynomial<V>,
    right: LinearPolynomial<V>,
    private val leftBounds: ConditionBounds<V>,
    private val rightBounds: ConditionBounds<V>,
    private val converter: IntoValue<V>,
    override var name: String = "mccormick",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val left = LinearPolynomial(left.monomials.toList(), left.constant)
    private val right = LinearPolynomial(right.monomials.toList(), right.constant)
    private var plan: McCormickPlan<V>? = null

    private val resultVariable = RealVar("${name}_mccormick")
    val resultVar: RealVar
        get() {
            resolvePlan()
            return resultVariable
        }

    override val resultPolynomial: LinearPolynomial<V>
        get() {
            resolvePlan()
            return LinearPolynomial(listOf(LinearMonomial(converter.one, resultVariable)), converter.zero)
        }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVariable)

    /** 求值求解器提供的松弛结果变量。 / Evaluate the relaxed result variable supplied by the solver.
     * @param values 符号和值的映射 / symbol-value mapping
     * @return 求解器松弛结果；若未解析或超出结果范围则返回 null / solver relaxation value, or null if unresolved or outside its result range
     */
    override fun evaluate(values: Map<Symbol, V>): V? {
        val current = resolvePlan() ?: return null
        val leftValue = left.evaluateWith(values) ?: return null
        val rightValue = right.evaluateWith(values) ?: return null
        if (leftValue.compareTo(current.leftBounds.lower) < 0 || leftValue.compareTo(current.leftBounds.upper) > 0 ||
            rightValue.compareTo(current.rightBounds.lower) < 0 || rightValue.compareTo(current.rightBounds.upper) > 0) {
            return null
        }
        val relaxed = values[resultVariable] ?: return null
        return if (relaxed.compareTo(current.resultBounds.lower) >= 0 && relaxed.compareTo(current.resultBounds.upper) <= 0) relaxed else null
    }

    /** 仅对输入表达式求值，返回其精确数值乘积，不代表 MILP 松弛结果。 / Evaluate only the input expressions and return their exact numeric product, not the MILP relaxation value.
     * @param values 符号和值的映射 / symbol-value mapping
     * @return 输入表达式的精确乘积；若未解析或超出输入界则返回 null / exact input product, or null if unresolved or outside input bounds
     */
    fun evaluateProduct(values: Map<Symbol, V>): V? {
        val current = resolvePlan() ?: return null
        return try {
            val leftValue = left.evaluateWith(values) ?: return null
            val rightValue = right.evaluateWith(values) ?: return null
            if (leftValue.compareTo(current.leftBounds.lower) < 0 || leftValue.compareTo(current.leftBounds.upper) > 0 ||
                rightValue.compareTo(current.rightBounds.lower) < 0 || rightValue.compareTo(current.rightBounds.upper) > 0) {
                return null
            }
            leftValue * rightValue
        } catch (_: RuntimeException) {
            null
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        val current = resolvePlan()
            ?: return mccormickFailure("McCormick 松弛需要有效且覆盖输入的有限界。 / McCormick relaxation requires valid finite bounds covering its inputs.")
        if (!rangesRemainCovered(current)) {
            return mccormickFailure("McCormick 输入范围超出了已捕获范围。 / McCormick input bounds exceed the captured bounds.")
        }
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val current = resolvePlan()
            ?: return mccormickFailure("McCormick 松弛需要有效且覆盖输入的有限界。 / McCormick relaxation requires valid finite bounds covering its inputs.")
        if (!rangesRemainCovered(current)) {
            return mccormickFailure("McCormick 输入范围超出了已捕获范围。 / McCormick input bounds exceed the captured bounds.")
        }
        val constraints = try {
            buildConstraints(current)
        } catch (_: RuntimeException) {
            return mccormickFailure("McCormick 约束系数无效或溢出。 / McCormick constraint coefficients are invalid or overflowed.")
        }
        if (!constraints.all { row ->
                hasUsableConditionFlattenedValues(row.lhs, converter) &&
                    hasUsableConditionFlattenedValues(row.rhs, converter)
            }) {
            return mccormickFailure("McCormick 约束包含非有限系数。 / McCormick constraints contain non-finite coefficients.")
        }
        return addConstraints(model, constraints) ?: ok
    }

    private fun resolvePlan(): McCormickPlan<V>? {
        plan?.let { return it }
        return try {
            if (!validBounds(leftBounds) || !validBounds(rightBounds)) return null
            val observedLeft = left.finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) }
            val observedRight = right.finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) }
            val products = listOf(
                leftBounds.lower * rightBounds.lower,
                leftBounds.lower * rightBounds.upper,
                leftBounds.upper * rightBounds.lower,
                leftBounds.upper * rightBounds.upper
            )
            if (products.any { !isUsableConditionBound(it, converter) }) return null
            var resultLower = products.first()
            var resultUpper = products.first()
            for (product in products.drop(1)) {
                if (product.compareTo(resultLower) < 0) resultLower = product
                if (product.compareTo(resultUpper) > 0) resultUpper = product
            }
            val resultBounds = ConditionBounds(resultLower, resultUpper)
            resultVariable.range.geq(converter.fromValue(resultBounds.lower))
            resultVariable.range.leq(converter.fromValue(resultBounds.upper))
            McCormickPlan(leftBounds, rightBounds, observedLeft, observedRight, resultBounds).also { plan = it }
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun rangesRemainCovered(current: McCormickPlan<V>): Boolean {
        val observedLeft = left.finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) }
        val observedRight = right.finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) }
        if (observedLeft == null && current.observedLeftBounds != null) return false
        if (observedRight == null && current.observedRightBounds != null) return false
        if (observedLeft != null && current.observedLeftBounds != null && !contains(current.observedLeftBounds, observedLeft)) return false
        if (observedRight != null && current.observedRightBounds != null && !contains(current.observedRightBounds, observedRight)) return false
        return true
    }

    private fun buildConstraints(current: McCormickPlan<V>): List<LinearInequality<V>> {
        val constraints = mutableListOf<LinearInequality<V>>()
        constraints += LinearInequality(left, LinearPolynomial(emptyList(), current.leftBounds.upper), Comparison.LE, "${name}_left_upper")
        constraints += LinearInequality(left, LinearPolynomial(emptyList(), current.leftBounds.lower), Comparison.GE, "${name}_left_lower")
        constraints += LinearInequality(right, LinearPolynomial(emptyList(), current.rightBounds.upper), Comparison.LE, "${name}_right_upper")
        constraints += LinearInequality(right, LinearPolynomial(emptyList(), current.rightBounds.lower), Comparison.GE, "${name}_right_lower")

        constraints += envelopeRow(
            leftScale = current.rightBounds.lower,
            rightScale = current.leftBounds.lower,
            rhs = -(current.leftBounds.lower * current.rightBounds.lower),
            comparison = Comparison.GE,
            suffix = "lower_left"
        )
        constraints += envelopeRow(
            leftScale = current.rightBounds.upper,
            rightScale = current.leftBounds.upper,
            rhs = -(current.leftBounds.upper * current.rightBounds.upper),
            comparison = Comparison.GE,
            suffix = "lower_right"
        )
        constraints += envelopeRow(
            leftScale = current.rightBounds.lower,
            rightScale = current.leftBounds.upper,
            rhs = -(current.leftBounds.upper * current.rightBounds.lower),
            comparison = Comparison.LE,
            suffix = "upper_left"
        )
        constraints += envelopeRow(
            leftScale = current.rightBounds.upper,
            rightScale = current.leftBounds.lower,
            rhs = -(current.leftBounds.lower * current.rightBounds.upper),
            comparison = Comparison.LE,
            suffix = "upper_right"
        )
        return constraints
    }

    private fun envelopeRow(leftScale: V, rightScale: V, rhs: V, comparison: Comparison, suffix: String): LinearInequality<V> {
        val terms = mutableListOf(LinearMonomial(converter.one, resultVariable))
        terms += left.monomials.map { LinearMonomial(-leftScale * it.coefficient, it.symbol) }
        terms += right.monomials.map { LinearMonomial(-rightScale * it.coefficient, it.symbol) }
        val constant = -(leftScale * left.constant) - (rightScale * right.constant)
        return LinearInequality(
            lhs = LinearPolynomial(terms, constant),
            rhs = LinearPolynomial(emptyList(), rhs),
            comparison = comparison,
            name = "${name}_$suffix"
        )
    }

    private fun validBounds(bounds: ConditionBounds<V>): Boolean {
        return bounds.lower.compareTo(bounds.upper) <= 0 &&
            isUsableConditionBound(bounds.lower, converter) &&
            isUsableConditionBound(bounds.upper, converter)
    }

    private fun contains(outer: ConditionBounds<V>, inner: ConditionBounds<V>): Boolean {
        return outer.lower.compareTo(inner.lower) <= 0 && outer.upper.compareTo(inner.upper) >= 0
    }

    companion object {
        /** 创建 McCormick 连续乘积松弛。 / Create a McCormick continuous-product relaxation.
         * @param left 左侧线性表达式 / left linear expression
         * @param right 右侧线性表达式 / right linear expression
         * @param leftBounds 左侧表达式的有限界 / finite bounds for the left expression
         * @param rightBounds 右侧表达式的有限界 / finite bounds for the right expression
         * @param converter 数值类型转换器 / numeric value converter
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return McCormick 乘积松弛函数符号 / McCormick product-relaxation symbol
         */
        operator fun <V> invoke(
            left: LinearPolynomial<V>,
            right: LinearPolynomial<V>,
            leftBounds: ConditionBounds<V>,
            rightBounds: ConditionBounds<V>,
            converter: IntoValue<V>,
            name: String = "mccormick",
            displayName: String? = null
        ): McCormickEnvelopeFunction<V> where V : RealNumber<V>, V : NumberField<V> {
            return McCormickEnvelopeFunction(
                left = left,
                right = right,
                leftBounds = leftBounds,
                rightBounds = rightBounds,
                converter = converter,
                name = name,
                displayName = displayName
            )
        }
    }
}

private fun <T> mccormickFailure(message: String): Ret<T> {
    return Failed(ErrorCode.IllegalArgument, message)
}
