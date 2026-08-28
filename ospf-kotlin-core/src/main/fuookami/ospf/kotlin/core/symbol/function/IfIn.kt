@file:Suppress("unused")

/** 区间条件函数符号 / If-in-range condition function symbol */
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

/** 已解析的 IfIn 条件范围 / Resolved ranges used by IfIn registration. */
private data class ResolvedIfInBounds<V>(
    val lowerDifference: ConditionBounds<V>,
    val upperDifference: ConditionBounds<V>,
    val lowerPolynomial: LinearPolynomial<V>,
    val upperPolynomial: LinearPolynomial<V>
) where V : RealNumber<V>, V : NumberField<V>

private fun <V> isFiniteIfIn(value: V): Boolean where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (!value.isFinite()) {
            false
        } else {
            val nan = value.constants.nan
            nan == null || value != nan
        }
    } catch (_: RuntimeException) {
        false
    }
}

private fun <T> ifInFailure(message: String): Ret<T> {
    return Failed(ErrorCode.IllegalArgument, message)
}

/**
 * 区间条件函数：当 a <= x <= b 时 y = 1，否则 y = 0。 / If-In function: `y = 1 if a <= x <= b, else y = 0`.
 *
 * 下界和上界都使用 GE 关系指示器；因此区间端点包含在真分支中，距离边界小于
 * [strictBoundary] 的连续区间保持为不可判定。 / Both sides use GE relation indicators;
 * interval endpoints are included in the true branch, while a continuous region less than
 * [strictBoundary] away from a boundary remains undecidable.
 *
 * @property x 输入线性多项式 / the input linear polynomial
 * @property lower 下界 (a) / the lower bound (a)
 * @property upper 上界 (b) / the upper bound (b)
 * @param bounds x 的显式有限取值范围（兼容名称），可缺省为从 x 推导 / explicit finite range of x (compatibility name), optionally inferred from x
 * @param conditionBounds x 的显式有限取值范围 / explicit finite range of x
 * @param converter 值类型转换器 / value type converter
 * @param bigM 兼容旧调用的 Big-M 参数，不可替代 bounds / legacy Big-M parameter for source compatibility; it cannot replace bounds
 * @param tolerance 兼容旧调用的容差；未传 strictBoundary 时作为业务间隔 / legacy tolerance; used as the business gap when strictBoundary is omitted
 * @param strictBoundary 真、假分支之间的最小业务间隔 / minimum business gap between true and false branches
 * @property delta 离散步长，缺省取 [strictBoundary] / discrete step, defaulting to [strictBoundary]
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
    override var displayName: String? = null,
    bounds: ConditionBounds<V>? = null,
    conditionBounds: ConditionBounds<V>? = null,
    delta: V? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val explicitBigM: V? = bigM
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val declaredConditionBounds: ConditionBounds<V>? = conditionBounds ?: bounds

    /** 生效的业务间隔；未显式传入时兼容旧 tolerance。 / Effective business gap; defaults to the legacy tolerance. */
    val strictBoundary: V = strictBoundary ?: this.tolerance
    private val requestedDelta: V? = delta
    val delta: V = requestedDelta ?: this.strictBoundary

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_ifin")
    val geVar: AbstractVariableItem<*, *> = BinVar("${name}_ge")
    val leVar: AbstractVariableItem<*, *> = BinVar("${name}_le")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, geVar, leVar)

    override val resultPolynomial: LinearPolynomial<V> by lazy {
        LinearPolynomial(
            monomials = listOf(LinearMonomial(converter.one, resultVar)),
            constant = converter.zero
        )
    }

    /** 结果多项式兼容入口 / Compatibility accessor for the result polynomial. */
    val result: LinearPolynomial<V> get() = resultPolynomial

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

    /**
     * 对输入值执行区间三值判定。 / Classify the input value using three-valued interval semantics.
     *
     * 任一侧为 False 时返回 False，两侧均为 True 时返回 True，其余情况返回 Undefined。
     * Returns False if either side is False, True if both sides are True, and Undefined otherwise.
     * 缺失输入或参数非法时返回 Failed。 / Missing input or invalid parameters return Failed.
     *
     * @param values 符号值映射 / symbol-to-value mapping
     * @return 区间条件的三值判定 / three-valued interval classification
     */
    fun classify(values: Map<Symbol, V>): Ret<TruthValue> {
        return try {
            when (val validation = validateClassificationArguments()) {
                null -> {}
                is Ok -> {}
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
            val xValue = x.evaluateWith(values)
                ?: return ifInFailure("缺少 x 的输入值。 / The input value of x is missing.")
            if (!isUsableBound(xValue)) {
                return ifInFailure(
                    "x 的求值结果必须为有限且可编码的 solver 值。 / The evaluated x value must be finite and solver-representable."
                )
            }

            val lowerDifference = xValue - lower
            val upperDifference = upper - xValue
            if (!isFiniteIfIn(lowerDifference) || !isUsableBound(lowerDifference) ||
                !isFiniteIfIn(upperDifference) || !isUsableBound(upperDifference)
            ) {
                return ifInFailure(
                    "区间端点差值必须为有限且可编码的 solver 值。 / Differences from interval endpoints must be finite and solver-representable."
                )
            }

            val lowerClassification = when (val result = classifyDiscreteCondition(
                d = lowerDifference,
                relation = Comparison.GE,
                delta = delta,
                strictBoundary = strictBoundary
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val upperClassification = when (val result = classifyDiscreteCondition(
                d = upperDifference,
                relation = Comparison.GE,
                delta = delta,
                strictBoundary = strictBoundary
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            Ok(
                when {
                    lowerClassification == TruthValue.False || upperClassification == TruthValue.False -> TruthValue.False
                    lowerClassification == TruthValue.True && upperClassification == TruthValue.True -> TruthValue.True
                    else -> TruthValue.Undefined
                }
            )
        } catch (_: RuntimeException) {
            ifInFailure(
                "区间条件求值失败。 / Failed to evaluate the interval condition."
            )
        }
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        return when (val classification = classify(values)) {
            is Ok -> when (classification.value) {
                TruthValue.True -> converter.one
                TruthValue.False -> converter.zero
                TruthValue.Undefined -> null
            }
            is Failed -> null
            is Fatal -> null
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val validation = validateRegistration()) {
            is Ok -> when (val result = tokens.add(helperVariables)) {
                is Ok -> ok
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
            is Failed -> Failed(validation.error)
            is Fatal -> Fatal(validation.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val bounds = when (val validation = validateRegistration()) {
            is Ok -> validation.value
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }

        val lowerCondition = when (val result = normalizeDiscreteCondition(
            poly = bounds.lowerPolynomial,
            relation = Comparison.GE,
            bounds = bounds.lowerDifference,
            delta = delta,
            strictBoundary = strictBoundary
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val upperCondition = when (val result = normalizeDiscreteCondition(
            poly = bounds.upperPolynomial,
            relation = Comparison.GE,
            bounds = bounds.upperDifference,
            delta = delta,
            strictBoundary = strictBoundary
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val zero = converter.zero
        val one = converter.one
        val allConstraints = mutableListOf<LinearInequality<V>>()

        fun addIndicatorConstraints(
            condition: DiscreteConditionLinearization<V>,
            indicator: AbstractVariableItem<*, *>,
            prefix: String
        ): Try {
            return when (val fixedValue = condition.fixedValue) {
                TruthValue.True -> {
                    allConstraints += fixedVariableEquality(
                        variable = indicator,
                        value = one,
                        zero = zero,
                        one = one,
                        name = "${prefix}_fold_true"
                    )
                    ok
                }
                TruthValue.False -> {
                    allConstraints += fixedVariableEquality(
                        variable = indicator,
                        value = zero,
                        zero = zero,
                        one = one,
                        name = "${prefix}_fold_false"
                    )
                    ok
                }
                TruthValue.Undefined -> when (val result = relationIndicatorConstraints(
                    poly = condition.polynomial,
                    indicator = indicator,
                    relation = Comparison.GT,
                    bounds = condition.bounds,
                    strictBoundary = strictBoundary,
                    namePrefix = prefix
                )) {
                    is Ok -> {
                        allConstraints += result.value
                        ok
                    }
                    is Failed -> Failed(result.error)
                    is Fatal -> Fatal(result.errors)
                }
                null -> when (val result = relationIndicatorConstraints(
                    poly = condition.polynomial,
                    indicator = indicator,
                    relation = Comparison.GT,
                    bounds = condition.bounds,
                    strictBoundary = strictBoundary,
                    namePrefix = prefix
                )) {
                    is Ok -> {
                        allConstraints += result.value
                        ok
                    }
                    is Failed -> Failed(result.error)
                    is Fatal -> Fatal(result.errors)
                }
            }
        }

        when (val result = addIndicatorConstraints(
            condition = lowerCondition,
            indicator = geVar,
            prefix = "${name}_ge"
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = addIndicatorConstraints(
            condition = upperCondition,
            indicator = leVar,
            prefix = "${name}_le"
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val resultFixedValue = when {
            lowerCondition.fixedValue == TruthValue.False || upperCondition.fixedValue == TruthValue.False -> zero
            lowerCondition.fixedValue == TruthValue.True && upperCondition.fixedValue == TruthValue.True -> one
            else -> null
        }
        if (resultFixedValue != null) {
            allConstraints += fixedVariableEquality(
                variable = resultVar,
                value = resultFixedValue,
                zero = zero,
                one = one,
                name = "${name}_fold_result"
            )
        } else if (lowerCondition.fixedValue == TruthValue.True) {
            allConstraints += LinearInequality(
                lhs = LinearPolynomial(
                    monomials = listOf(
                        LinearMonomial(one, resultVar),
                        LinearMonomial(-one, leVar)
                    ),
                    constant = zero
                ),
                rhs = LinearPolynomial(emptyList(), zero),
                comparison = Comparison.EQ,
                name = "${name}_fold_result_le"
            )
        } else if (upperCondition.fixedValue == TruthValue.True) {
            allConstraints += LinearInequality(
                lhs = LinearPolynomial(
                    monomials = listOf(
                        LinearMonomial(one, resultVar),
                        LinearMonomial(-one, geVar)
                    ),
                    constant = zero
                ),
                rhs = LinearPolynomial(emptyList(), zero),
                comparison = Comparison.EQ,
                name = "${name}_fold_result_ge"
            )
        } else {
            // result = ge AND le / 结果等于 ge 与 le 的合取
            allConstraints += LinearInequality(
                lhs = LinearPolynomial(
                    monomials = listOf(
                        LinearMonomial(one, resultVar),
                        LinearMonomial(-one, geVar)
                    ),
                    constant = zero
                ),
                rhs = LinearPolynomial(emptyList(), zero),
                comparison = Comparison.LE,
                name = "${name}_link_ge"
            )
            allConstraints += LinearInequality(
                lhs = LinearPolynomial(
                    monomials = listOf(
                        LinearMonomial(one, resultVar),
                        LinearMonomial(-one, leVar)
                    ),
                    constant = zero
                ),
                rhs = LinearPolynomial(emptyList(), zero),
                comparison = Comparison.LE,
                name = "${name}_link_le"
            )
            allConstraints += LinearInequality(
                lhs = LinearPolynomial(
                    monomials = listOf(
                        LinearMonomial(one, resultVar),
                        LinearMonomial(-one, geVar),
                        LinearMonomial(-one, leVar)
                    ),
                    constant = zero
                ),
                rhs = LinearPolynomial(emptyList(), -one),
                comparison = Comparison.GE,
                name = "${name}_link_lb"
            )
        }

        // Build every constraint before mutating the model / 先完整构造约束，再修改模型
        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    private fun validateClassificationArguments(): Try? {
        if (!isUsableBound(lower) || !isUsableBound(upper)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "区间边界必须为有限且可编码的 solver 值。 / Interval boundaries must be finite and solver-representable."
            )
        }
        if (lower.compareTo(upper) > 0) {
            return Failed(
                ErrorCode.IllegalArgument,
                "区间要求 lower <= upper。 / The interval requires lower <= upper."
            )
        }
        when (val validation = validateDiscreteConditionParameters(
            delta = delta,
            strictBoundary = strictBoundary
        )) {
            is Ok -> {}
            is Failed -> return validation
            is Fatal -> return validation
        }
        if (!hasUsableConditionFlattenedValues(x, converter)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "x 的系数和常数必须为有限且可编码的 solver 值。 / Coefficients and constant of x must be finite and solver-representable."
            )
        }
        return null
    }

    private fun resolveXBounds(): Ret<ConditionBounds<V>> {
        declaredConditionBounds?.let { bounds ->
            return usableBounds(bounds)?.let { Ok(it) } ?: ifInFailure(
                "显式 x 范围必须为有限且有序的 solver 范围。 / Explicit x bounds must be finite and ordered for the solver."
            )
        }
        val inferred = try {
            x.finiteBounds(converter)
        } catch (_: RuntimeException) {
            null
        }?.let {
            usableBounds(
                ConditionBounds(
                    lower = it.lower,
                    upper = it.upper
                )
            )
        }
        inferred?.let { return Ok(it) }
        return ifInFailure(
            "缺少 x 的有限范围；单独的 bigM 不能替代 ConditionBounds。 / A finite range for x is missing; bigM alone cannot replace ConditionBounds."
        )
    }

    private fun validateRegistration(): Ret<ResolvedIfInBounds<V>> {
        return try {
        when (val validation = validateClassificationArguments()) {
            null -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
            is Ok -> {}
        }

        explicitBigM?.let { value ->
            if (!isUsableBound(value)) {
                return ifInFailure(
                    "bigM 必须为有限且可编码的 solver 值；它仍不能替代 x 的范围。 / bigM must be finite and solver-representable; it still cannot replace the range of x."
                )
            }
            val zero = value - value
            if (value.compareTo(zero) <= 0) {
                return ifInFailure(
                    "bigM 必须大于 0；它仍不能替代 x 的范围。 / bigM must be greater than 0; it still cannot replace the range of x."
                )
            }
        }

        if (!isUsableBound(strictBoundary) || !isUsableBound(delta)) {
            return ifInFailure(
                "strictBoundary 和 delta 必须是有限且可编码的 solver 值。 / strictBoundary and delta must be finite solver-representable values."
            )
        }

        val xBounds = when (val result = resolveXBounds()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (!isUsableBound(xBounds.lower) || !isUsableBound(xBounds.upper)) {
            return ifInFailure(
                "x 的范围端点必须为有限且可编码的 solver 值。 / The endpoints of x's range must be finite and solver-representable."
            )
        }
        if (xBounds.lower.compareTo(xBounds.upper) > 0) {
            return ifInFailure(
                "x 的范围要求 lower <= upper。 / x's range requires lower <= upper."
            )
        }

        val lowerDifference = ConditionBounds(
            lower = xBounds.lower - lower,
            upper = xBounds.upper - lower
        )
        val upperDifference = ConditionBounds(
            lower = upper - xBounds.upper,
            upper = upper - xBounds.lower
        )
        if (!isUsableBound(lowerDifference.lower) ||
            !isUsableBound(lowerDifference.upper) ||
            !isUsableBound(upperDifference.lower) ||
            !isUsableBound(upperDifference.upper)
        ) {
            return ifInFailure(
                "x 的差值范围必须为有限且可编码的 solver 值。 / Difference ranges derived from x must be finite and solver-representable."
            )
        }
        if (lowerDifference.lower.compareTo(lowerDifference.upper) > 0 ||
            upperDifference.lower.compareTo(upperDifference.upper) > 0
        ) {
            return ifInFailure(
                "x 的差值范围无效。 / A derived difference range for x is invalid."
            )
        }
        val lowerPolynomial = LinearPolynomial(
            monomials = x.monomials,
            constant = x.constant - lower
        )
        val upperPolynomial = LinearPolynomial(
            monomials = x.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
            constant = -x.constant + upper
        )
        if (!hasUsableConditionFlattenedValues(lowerPolynomial, converter) ||
            !hasUsableConditionFlattenedValues(upperPolynomial, converter)
        ) {
            return ifInFailure(
                "区间差值多项式必须为有限且可编码的 solver 值。 / Interval-difference polynomials must be finite and solver-representable."
            )
        }

        when (val validation = normalizeDiscreteCondition(
            poly = lowerPolynomial,
            bounds = lowerDifference,
            relation = Comparison.GE,
            delta = delta,
            strictBoundary = strictBoundary
        )) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        when (val validation = normalizeDiscreteCondition(
            poly = upperPolynomial,
            bounds = upperDifference,
            relation = Comparison.GE,
            delta = delta,
            strictBoundary = strictBoundary
        )) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }

        return Ok(
            ResolvedIfInBounds(
                lowerDifference = lowerDifference,
                upperDifference = upperDifference,
                lowerPolynomial = lowerPolynomial,
                upperPolynomial = upperPolynomial
            )
        )
        } catch (_: RuntimeException) {
            ifInFailure(
                "区间条件范围计算失败。 / Failed to calculate interval-condition bounds."
            )
        }
    }

    companion object {
        /**
         * 创建区间条件函数实例 / Create an if-in function instance.
         *
         * @param x 输入线性多项式 / input linear polynomial
         * @param lower 下界 / lower bound
         * @param upper 上界 / upper bound
         * @param converter 值类型转换器 / value type converter
         * @param bigM 兼容旧调用的 Big-M 参数 / legacy Big-M parameter for source compatibility
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @param bounds x 的有限取值范围（兼容名称）/ finite range of x (compatibility name)
         * @param conditionBounds x 的有限取值范围 / finite range of x
         * @param tolerance 兼容旧调用的容差 / legacy tolerance
         * @param strictBoundary 真、假分支之间的最小业务间隔 / minimum business gap
         * @param delta 离散步长 / discrete step
         * @return [IfInFunction] 实例 / [IfInFunction] instance
         */
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            lower: V,
            upper: V,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null,
            bounds: ConditionBounds<V>? = null,
            tolerance: V? = null,
            strictBoundary: V? = null,
            conditionBounds: ConditionBounds<V>? = null,
            delta: V? = null
        ): IfInFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            IfInFunction(
                x = x,
                lower = lower,
                upper = upper,
                converter = converter,
                bigM = bigM,
                tolerance = tolerance,
                strictBoundary = strictBoundary,
                name = name,
                displayName = displayName,
                bounds = bounds,
                conditionBounds = conditionBounds,
                delta = delta
            )
    }
}
