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

private data class SelectPlan<V>(
    val thenBounds: ConditionBounds<V>,
    val elseBounds: ConditionBounds<V>,
    val observedThenBounds: ConditionBounds<V>?,
    val observedElseBounds: ConditionBounds<V>?,
    val deltaBounds: ConditionBounds<V>,
    val difference: LinearPolynomial<V>
) where V : RealNumber<V>, V : NumberField<V>

/**
 * 在两个线性表达式间按二值变量精确选择，结果为 `mask * then + (1-mask) * otherwise`。
 * Exactly select between two linear expressions using a binary variable.
 *
 * @property mask 二值选择变量 / binary selector
 * @property then 真分支线性表达式 / linear expression for the true branch
 * @property otherwise 假分支线性表达式 / linear expression for the false branch
 * @property resultVar 选择结果变量 / selected result variable
 * @param converter 数值类型转换器 / numeric value converter
 * @param thenBounds 可选的真分支有限界 / optional finite bounds for the true branch
 * @param otherwiseBounds 可选的假分支有限界 / optional finite bounds for the false branch
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class SelectFunction<V>(
    val mask: AbstractVariableItem<*, *>,
    then: LinearPolynomial<V>,
    otherwise: LinearPolynomial<V>,
    private val converter: IntoValue<V>,
    private val thenBounds: ConditionBounds<V>? = null,
    private val otherwiseBounds: ConditionBounds<V>? = null,
    override var name: String = "select",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val then = LinearPolynomial(then.monomials.toList(), then.constant)
    private val otherwise = LinearPolynomial(otherwise.monomials.toList(), otherwise.constant)
    private var plan: SelectPlan<V>? = null

    private val resultVariable = RealVar("${name}_select")
    val resultVar: RealVar
        get() {
            resolvePlan()
            return resultVariable
        }
    private val maskedDifferenceVar = RealVar("${name}_select_delta")

    override val resultPolynomial: LinearPolynomial<V>
        get() {
            resolvePlan()
            return LinearPolynomial(listOf(LinearMonomial(converter.one, resultVariable)), converter.zero)
        }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVariable, maskedDifferenceVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val current = resolvePlan() ?: return null
        val maskValue = values[mask] ?: return null
        val thenValue = then.evaluateWith(values) ?: return null
        val elseValue = otherwise.evaluateWith(values) ?: return null
        if (!within(thenValue, current.thenBounds) || !within(elseValue, current.elseBounds)) return null
        return when {
            maskValue.compareTo(converter.zero) == 0 -> elseValue
            maskValue.compareTo(converter.one) == 0 -> thenValue
            else -> null
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        val current = resolvePlan()
            ?: return selectFailure("Select 要求二值选择变量以及有限分支范围。 / Select requires a binary selector and finite branch bounds.")
        if (!rangesRemainCovered(current)) {
            return selectFailure("Select 分支范围超出了已捕获范围。 / Select branch bounds exceed the captured bounds.")
        }
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val current = resolvePlan()
            ?: return selectFailure("Select 要求二值选择变量以及有限分支范围。 / Select requires a binary selector and finite branch bounds.")
        if (!rangesRemainCovered(current)) {
            return selectFailure("Select 分支范围超出了已捕获范围。 / Select branch bounds exceed the captured bounds.")
        }

        val constraints = try {
            buildSelectConstraints(current)
        } catch (_: RuntimeException) {
            return selectFailure("Select 约束系数无效或溢出。 / Select constraint coefficients are invalid or overflowed.")
        }
        if (!constraints.all { row ->
                hasUsableConditionFlattenedValues(row.lhs, converter) &&
                    hasUsableConditionFlattenedValues(row.rhs, converter)
            }) {
            return selectFailure("Select 约束包含非有限系数。 / Select constraints contain non-finite coefficients.")
        }
        return addConstraints(model, constraints) ?: ok
    }

    private fun resolvePlan(): SelectPlan<V>? {
        plan?.let { return it }
        if (!mask.type.isBinaryType) return null
        try {
            val observedThen = then.finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) }
            val observedElse = otherwise.finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) }
            val thenRange = thenBounds ?: observedThen ?: return null
            val elseRange = otherwiseBounds ?: observedElse ?: return null
            if (!validBounds(thenRange) || !validBounds(elseRange)) return null

            val difference = LinearPolynomial(
                monomials = then.monomials + otherwise.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
                constant = then.constant - otherwise.constant
            )
            val deltaLower = thenRange.lower - elseRange.upper
            val deltaUpper = thenRange.upper - elseRange.lower
            val outputLower = if (thenRange.lower.compareTo(elseRange.lower) <= 0) thenRange.lower else elseRange.lower
            val outputUpper = if (thenRange.upper.compareTo(elseRange.upper) >= 0) thenRange.upper else elseRange.upper
            val candidate = SelectPlan(
                thenBounds = thenRange,
                elseBounds = elseRange,
                observedThenBounds = observedThen,
                observedElseBounds = observedElse,
                deltaBounds = ConditionBounds(deltaLower, deltaUpper),
                difference = difference
            )
            if (!validBounds(candidate.deltaBounds) || !validBounds(ConditionBounds(outputLower, outputUpper)) ||
                !hasUsableConditionFlattenedValues(difference, converter)) {
                return null
            }
            resultVariable.range.geq(converter.fromValue(outputLower))
            resultVariable.range.leq(converter.fromValue(outputUpper))
            val maskLower = if (deltaLower.compareTo(converter.zero) < 0) deltaLower else converter.zero
            val maskUpper = if (deltaUpper.compareTo(converter.zero) > 0) deltaUpper else converter.zero
            maskedDifferenceVar.range.geq(converter.fromValue(maskLower))
            maskedDifferenceVar.range.leq(converter.fromValue(maskUpper))
            plan = candidate
            return candidate
        } catch (_: RuntimeException) {
            return null
        }
    }

    private fun rangesRemainCovered(current: SelectPlan<V>): Boolean {
        val actualThen = then.finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) }
        val actualElse = otherwise.finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) }
        if (actualThen == null && current.observedThenBounds != null) return false
        if (actualElse == null && current.observedElseBounds != null) return false
        if (actualThen != null && current.observedThenBounds != null && !contains(current.observedThenBounds, actualThen)) return false
        if (actualElse != null && current.observedElseBounds != null && !contains(current.observedElseBounds, actualElse)) return false
        return true
    }

    private fun buildSelectConstraints(current: SelectPlan<V>): List<LinearInequality<V>> {
        val zero = converter.zero
        val one = converter.one
        val z = maskedDifferenceVar
        val constraints = maskingConstraints(
            difference = current.difference,
            lower = current.deltaBounds.lower,
            upper = current.deltaBounds.upper
        )
        constraints += LinearInequality(
            lhs = then,
            rhs = LinearPolynomial(emptyList(), current.thenBounds.upper),
            comparison = Comparison.LE,
            name = "${name}_select_then_upper"
        )
        constraints += LinearInequality(
            lhs = then,
            rhs = LinearPolynomial(emptyList(), current.thenBounds.lower),
            comparison = Comparison.GE,
            name = "${name}_select_then_lower"
        )
        constraints += LinearInequality(
            lhs = otherwise,
            rhs = LinearPolynomial(emptyList(), current.elseBounds.upper),
            comparison = Comparison.LE,
            name = "${name}_select_else_upper"
        )
        constraints += LinearInequality(
            lhs = otherwise,
            rhs = LinearPolynomial(emptyList(), current.elseBounds.lower),
            comparison = Comparison.GE,
            name = "${name}_select_else_lower"
        )
        val resultTerms = mutableListOf(LinearMonomial(one, resultVariable), LinearMonomial(-one, z))
        resultTerms += otherwise.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
        constraints += LinearInequality(
            lhs = LinearPolynomial(resultTerms, -otherwise.constant),
            rhs = LinearPolynomial(emptyList(), zero),
            comparison = Comparison.EQ,
            name = "${name}_select_result"
        )
        return constraints
    }

    private fun maskingConstraints(difference: LinearPolynomial<V>, lower: V, upper: V): MutableList<LinearInequality<V>> {
        val zero = converter.zero
        val bit = mask
        val value = maskedDifferenceVar
        val valueMonomial = LinearMonomial(converter.one, value)
        val negativeTerms = difference.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
        return mutableListOf(
            LinearInequality(
                LinearPolynomial(listOf(valueMonomial, LinearMonomial(-upper, bit)), zero),
                LinearPolynomial(emptyList(), zero),
                Comparison.LE,
                "${name}_select_zero_upper"
            ),
            LinearInequality(
                LinearPolynomial(listOf(valueMonomial, LinearMonomial(-lower, bit)), zero),
                LinearPolynomial(emptyList(), zero),
                Comparison.GE,
                "${name}_select_zero_lower"
            ),
            LinearInequality(
                LinearPolynomial(listOf(valueMonomial) + negativeTerms + LinearMonomial(-lower, bit), -difference.constant),
                LinearPolynomial(emptyList(), -lower),
                Comparison.LE,
                "${name}_select_one_upper"
            ),
            LinearInequality(
                LinearPolynomial(listOf(valueMonomial) + negativeTerms + LinearMonomial(-upper, bit), -difference.constant),
                LinearPolynomial(emptyList(), -upper),
                Comparison.GE,
                "${name}_select_one_lower"
            )
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

    private fun within(value: V, bounds: ConditionBounds<V>): Boolean {
        return value.compareTo(bounds.lower) >= 0 && value.compareTo(bounds.upper) <= 0
    }

    companion object {
        /** 创建双分支选择函数符号。 / Create a binary selection function symbol.
         * @param mask 二值选择变量 / binary selector
         * @param then 真分支线性表达式 / linear expression for the true branch
         * @param otherwise 假分支线性表达式 / linear expression for the false branch
         * @param converter 数值类型转换器 / numeric value converter
         * @param thenBounds 可选的真分支有限界 / optional finite bounds for the true branch
         * @param otherwiseBounds 可选的假分支有限界 / optional finite bounds for the false branch
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return 双分支选择函数符号 / binary selection function symbol
         */
        operator fun <V> invoke(
            mask: AbstractVariableItem<*, *>,
            then: LinearPolynomial<V>,
            otherwise: LinearPolynomial<V>,
            converter: IntoValue<V>,
            thenBounds: ConditionBounds<V>? = null,
            otherwiseBounds: ConditionBounds<V>? = null,
            name: String = "select",
            displayName: String? = null
        ): SelectFunction<V> where V : RealNumber<V>, V : NumberField<V> {
            return SelectFunction(
                mask = mask,
                then = then,
                otherwise = otherwise,
                converter = converter,
                thenBounds = thenBounds,
                otherwiseBounds = otherwiseBounds,
                name = name,
                displayName = displayName
            )
        }
    }
}

/** 双分支条件选择函数的别名。 / Alias for a binary if-then-else selector. */
typealias IfThenElseFunction<V> = SelectFunction<V>

private fun <T> selectFailure(message: String): Ret<T> {
    return Failed(ErrorCode.IllegalArgument, message)
}
