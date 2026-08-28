@file:Suppress("unused")

/** Sigmoid 函数符号 / Sigmoid function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*

/**
 * Sigmoid/阶跃函数符号 / Sigmoid/step function symbol
 *
 * 提供 [SigmoidFunction]，实现按比较关系选择 0/1 的线性化建模。
 *
 * Provides [SigmoidFunction] for relation-aware linearized modeling of a binary result.
*/

/**
 * Sigmoid/阶跃函数：满足 [relation] 时 y = 1，否则为 0。
 * Sigmoid/step function: y = 1 when [relation] is satisfied, else 0.
 *
 * 使用条件范围驱动的关系指示约束。 / Uses range-driven relation-indicator constraints.
 *
 * @property condition 条件线性多项式 / condition linear polynomial
 * @property relation 条件比较关系，默认 [Comparison.GT] / condition comparison relation, defaulting to [Comparison.GT]
 * @property strictBoundary 真、假分支之间的业务间隔 / business gap between the true and false branches
 * @property delta 离散步长，缺省取 [strictBoundary] / discrete step, defaulting to [strictBoundary]
 * @param bigM 旧版 Big-M 参数，仅用于源码兼容；不能替代范围 / legacy Big-M parameter kept for source compatibility; it cannot replace bounds
 * @param tolerance 兼容旧调用的容差（默认 [NONZERO_TOLERANCE]）/ legacy tolerance (default [NONZERO_TOLERANCE])
 * @param bounds 条件多项式的显式有限范围（兼容名称）/ explicit finite bounds of the condition polynomial (compatibility name)
 * @param conditionBounds 条件多项式的显式有限范围 / explicit finite bounds of the condition polynomial
 * @property converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
*/
class SigmoidFunction<V>(
    val condition: LinearPolynomial<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "sigmoid",
    override var displayName: String? = null,
    val relation: Comparison = Comparison.GT,
    bounds: ConditionBounds<V>? = null,
    conditionBounds: ConditionBounds<V>? = null,
    delta: V? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val explicitBigM: V? = bigM
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    val strictBoundary: V = strictBoundary ?: this.tolerance
    private val requestedDelta: V? = delta
    val delta: V = requestedDelta ?: this.strictBoundary
    private val declaredConditionBounds: ConditionBounds<V>? = conditionBounds ?: bounds

    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_sig_ind")

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, indicatorVar)), converter.zero)

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(indicatorVar)

    private fun isUsableBound(value: V): Boolean {
        return isUsableConditionBound(value, converter)
    }

    private fun usableBounds(bounds: ConditionBounds<V>): ConditionBounds<V>? {
        return if (isUsableBound(bounds.lower) &&
            isUsableBound(bounds.upper) &&
            bounds.lower.compareTo(bounds.upper) <= 0
        ) {
            bounds
        } else {
            null
        }
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        return when (val classification = classify(values)) {
            is Ok -> when (classification.value) {
                TruthValue.True -> converter.one
                TruthValue.False -> converter.zero
                TruthValue.Undefined -> null
            }
            is Failed, is Fatal -> null
        }
    }

    /**
     * 对给定符号值执行统一三值条件判定。 / Classify the condition using the shared three-valued semantics.
     *
     * @param values 符号到值的映射 / mapping from symbols to values
     * @return 条件判定结果；缺失输入或非法关系时失败 / condition result; failure for missing input or an invalid relation
    */
    fun classify(values: Map<Symbol, V>): Ret<TruthValue> {
        return try {
            val condValue = condition.evaluateWith(values)
                ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "条件输入缺失，无法判定。 / A condition input is missing, so the condition cannot be classified."
                )
            classifyDiscreteCondition(
                d = condValue,
                relation = relation,
                delta = delta,
                strictBoundary = strictBoundary
            )
        } catch (error: RuntimeException) {
            val detail = error.message?.takeIf { it.isNotBlank() }
                ?: error::class.simpleName
                ?: "unknown runtime error"
            Failed(
                ErrorCode.IllegalArgument,
                "条件判定失败：$detail / Condition classification failed: $detail"
            )
        }
    }

    private fun resolveBounds(): Ret<ConditionBounds<V>> {
        declaredConditionBounds?.let { bounds ->
            return usableBounds(bounds)?.let { Ok(it) } ?: Failed(
                ErrorCode.IllegalArgument,
                "显式条件范围必须为有限且有序的 solver 范围。 / Explicit condition bounds must be finite and ordered for the solver."
            )
        }
        val inferred = try {
            condition.finiteBounds(converter)
        } catch (_: RuntimeException) {
            null
        }?.let {
            usableBounds(ConditionBounds(it.lower, it.upper))
        }
        inferred?.let { return Ok(it) }
        return Failed(
            ErrorCode.IllegalArgument,
            "条件范围缺失：请迁移调用并提供 ConditionBounds；旧版 bigM 不能替代范围。 / Condition bounds are missing: migrate the call to provide ConditionBounds; the legacy bigM cannot replace them."
        )
    }

    private fun precheck(): Ret<ConditionBounds<V>> {
        explicitBigM?.let { value ->
            if (!isUsableConditionBound(value, converter)) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "bigM 必须为有限且可编码的 solver 值；它仍不能替代条件范围。 / bigM must be finite and solver-representable; it still cannot replace the condition bounds."
                )
            }
            val zero = value - value
            if (value.compareTo(zero) <= 0) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "bigM 必须大于 0；它仍不能替代条件范围。 / bigM must be greater than 0; it still cannot replace the condition bounds."
                )
            }
        }
        if (!hasUsableConditionFlattenedValues(condition, converter)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "条件多项式的系数和常数必须为有限且可编码的 solver 值。 / Condition polynomial coefficients and constant must be finite and solver-representable."
            )
        }
        when (val validation = validateDiscreteConditionParameters(
            delta = delta,
            strictBoundary = strictBoundary
        )) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        if (!isUsableConditionBound(strictBoundary, converter) ||
            !isUsableConditionBound(delta, converter)
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "strictBoundary 和 delta 必须是有限且可编码的 solver 值。 / strictBoundary and delta must be finite solver-representable values."
            )
        }
        val resolved = when (val result = resolveBounds()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return when (val validation = normalizeDiscreteCondition(
            poly = condition,
            bounds = resolved,
            relation = relation,
            delta = delta,
            strictBoundary = strictBoundary
        )) {
            is Ok -> Ok(resolved)
            is Failed -> Failed(validation.error)
            is Fatal -> Fatal(validation.errors)
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        when (val validation = precheck()) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val bounds = when (val validation = precheck()) {
            is Ok -> validation.value
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        val normalized = when (val result = normalizeDiscreteCondition(
            poly = condition,
            relation = relation,
            bounds = bounds,
            delta = delta,
            strictBoundary = strictBoundary
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        normalized.fixedValue?.let { fixedValue ->
            val value = if (fixedValue == TruthValue.True) converter.one else converter.zero
            val constraint = fixedVariableEquality(
                variable = indicatorVar,
                value = value,
                zero = converter.zero,
                one = converter.one,
                name = "${name}_sig_fold_indicator"
            )
            addConstraints(model, listOf(constraint))?.let { return it }
            return ok
        }

        val constraints = when (val result = relationIndicatorConstraints(
            poly = normalized.polynomial,
            indicator = indicatorVar,
            relation = Comparison.GT,
            bounds = normalized.bounds,
            strictBoundary = strictBoundary,
            namePrefix = "${name}_sig"
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // The indicator is the result, keeping the public polynomial contract stable. / 指示变量即为结果，保持公开多项式契约稳定。
        addConstraints(model, constraints)?.let { return it }
        return ok
    }

    companion object {
        /** 创建 [SigmoidFunction] 实例。 / Create a [SigmoidFunction] instance. */
        operator fun <V> invoke(
            condition: LinearPolynomial<V>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String = "sigmoid",
            displayName: String? = null,
            relation: Comparison = Comparison.GT,
            tolerance: V? = null,
            strictBoundary: V? = null,
            bounds: ConditionBounds<V>? = null,
            conditionBounds: ConditionBounds<V>? = null,
            delta: V? = null
        ): SigmoidFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            SigmoidFunction(
                condition = condition,
                bigM = bigM,
                tolerance = tolerance,
                strictBoundary = strictBoundary,
                converter = converter,
                name = name,
                displayName = displayName,
                relation = relation,
                bounds = bounds,
                conditionBounds = conditionBounds,
                delta = delta
            )
    }
}
