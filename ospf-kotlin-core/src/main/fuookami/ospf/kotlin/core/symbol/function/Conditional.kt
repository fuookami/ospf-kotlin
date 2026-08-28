/**
 * 条件关系共享语义与范围驱动线性化工具。 / Shared conditional semantics and range-driven linearization utilities.
 */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.toLinearFlattenData
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * 条件多项式的有限闭区间。 / A finite closed interval for a condition polynomial.
 *
 * 范围由调用方提供并必须覆盖条件多项式的实际取值；本类型本身不使用默认 Big-M 猜测范围。
 * The caller supplies the interval and must ensure that it covers the actual polynomial values;
 * this type never guesses a range from a default Big-M value.
 *
 * @param V 数值类型 / numeric value type
 * @property lower 条件多项式下界 / lower bound of the condition polynomial
 * @property upper 条件多项式上界 / upper bound of the condition polynomial
 */
data class ConditionBounds<V>(
    val lower: V,
    val upper: V
) where V : RealNumber<V>, V : NumberField<V>

/**
 * 条件判定的三值结果。 / Three-valued result of condition classification.
 */
enum class TruthValue {
    /** 满足真分支 / The true branch is satisfied. */
    True,

    /** 满足假分支 / The false branch is satisfied. */
    False,

    /** 位于业务间隔内，无法判定 / The value lies in the business gap and cannot be classified. */
    Undefined
}

private data class NormalizedCondition<V>(
    val qLower: V,
    val qUpper: V,
    val trueLower: V,
    val falseUpper: V,
    val negatePolynomial: Boolean,
    val trueBranchExists: Boolean,
    val falseBranchExists: Boolean
) where V : RealNumber<V>, V : NumberField<V>

internal data class ConditionBranchCoverage(
    val trueBranchPossible: Boolean,
    val falseBranchPossible: Boolean
)

private fun <V> V.isConditionFinite(): Boolean where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (!isFinite()) {
            false
        } else {
            val nan = constants.nan
            nan == null || this != nan
        }
    } catch (_: RuntimeException) {
        false
    }
}

/** 检查数值是否能作为求解器系数使用。 / Check whether a value is usable as a solver coefficient. */
private fun <V> V.isUsableConditionValue(): Boolean where V : RealNumber<V>, V : NumberField<V> {
    if (!isConditionFinite()) {
        return false
    }
    return try {
        val solverValue = toFltX()
        solverValue.isFinite() &&
            solverValue.compareTo(FltX.minimum) > 0 &&
            solverValue.compareTo(FltX.maximum) < 0
    } catch (_: RuntimeException) {
        false
    }
}

private fun <V> LinearPolynomial<V>.hasUsableConditionValues(): Boolean
    where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (!constant.isUsableConditionValue()) {
            return false
        }

        // A polynomial may contain repeated terms. Check the effective
        // coefficient too, because individually finite terms can overflow
        // when the flattening step combines them.
        val zero = constant - constant
        val coefficients = LinkedHashMap<Symbol, V>()
        for (monomial in monomials) {
            if (!monomial.coefficient.isUsableConditionValue()) {
                return false
            }
            val coefficient = (coefficients[monomial.symbol] ?: zero) + monomial.coefficient
            if (!coefficient.isUsableConditionValue()) {
                return false
            }
            coefficients[monomial.symbol] = coefficient
        }
        true
    } catch (_: RuntimeException) {
        false
    }
}

private fun <V> LinearInequality<V>.hasUsableConditionValues(): Boolean
    where V : RealNumber<V>, V : NumberField<V> {
    return lhs.hasUsableConditionValues() && rhs.hasUsableConditionValues()
}

/**
 * 检查范围端点是否能安全地作为 solver 边界使用。 / Check whether a range endpoint is safe to use as a solver bound.
 *
 * 数值类型的 `constants.minimum/maximum` 实现并不统一，且部分有理数实现不能安全访问它们。
 * 统一转换到 solver 的 Flt64 边界后检查有限性和全范围哨兵，避免依赖具体数值类型的常量实现。
 * Numeric types do not expose `constants.minimum/maximum` uniformly, and some rational
 * implementations cannot safely access them. Convert through the solver boundary and reject
 * non-finite values and full-range sentinels there instead.
 */
internal fun <V> isUsableConditionBound(
    value: V,
    converter: IntoValue<V>
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    val solverValue = try {
        converter.fromValue(value)
    } catch (_: RuntimeException) {
        return false
    }
    return solverValue.isFinite() &&
        solverValue != Flt64.nan &&
        solverValue.compareTo(Flt64.minimum) > 0 &&
        solverValue.compareTo(Flt64.maximum) < 0
}

/**
 * 检查多项式的每个数值是否能安全转换到 solver 边界。 /
 * Check that every polynomial value can be safely converted to the solver boundary.
 */
internal fun <V> hasUsableConditionSolverValues(
    polynomial: LinearPolynomial<V>,
    converter: IntoValue<V>
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (!polynomial.hasUsableConditionValues()) {
            return false
        }
        hasUsableConditionFlattenedValues(polynomial, converter)
    } catch (_: RuntimeException) {
        false
    }
}

private fun <V> LinearPolynomial<V>.hasUsableFlattenedConditionValues(): Boolean
    where V : RealNumber<V>, V : NumberField<V> {
    return try {
        val zero = constant - constant
        val flattened = LinearInequality(
            lhs = this,
            rhs = LinearPolynomial(emptyList(), zero),
            comparison = Comparison.EQ
        ).toLinearFlattenData().getOrElse { return false }
        flattened.constant.isUsableConditionValue() &&
            flattened.monomials.all { it.coefficient.isUsableConditionValue() }
    } catch (_: RuntimeException) {
        false
    }
}

/**
 * 检查模型最终展平并合并后的条件多项式。 /
 * Check the final condition polynomial after model flattening and coefficient merging.
 *
 * 中间符号展开后可能产生底层变量的重复项；只有检查同一展平路径的结果，才能保证
 * 辅助 token 预检与约束写入阶段对系数有限性的判断一致。 / Expanding intermediate symbols
 * may create repeated terms for an underlying variable; checking the result of the same
 * flattening path keeps auxiliary-token prechecks consistent with constraint insertion.
 */
internal fun <V> hasUsableConditionFlattenedValues(
    polynomial: LinearPolynomial<V>,
    converter: IntoValue<V>
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (!polynomial.hasUsableFlattenedConditionValues()) {
            return false
        }
        val zero = polynomial.constant - polynomial.constant
        val flattened = LinearInequality(
            lhs = polynomial,
            rhs = LinearPolynomial(emptyList(), zero),
            comparison = Comparison.EQ
        ).toLinearFlattenData().getOrElse { return false }
        isUsableConditionBound(flattened.constant, converter) &&
            flattened.monomials.all { isUsableConditionBound(it.coefficient, converter) }
    } catch (_: RuntimeException) {
        false
    }
}

private fun <T> conditionFailure(message: String): Ret<T> {
    return Failed(ErrorCode.IllegalArgument, message)
}

private fun <V> validateRelationAndBoundary(
    relation: Comparison,
    strictBoundary: V
): Try where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (relation != Comparison.GT &&
            relation != Comparison.GE &&
            relation != Comparison.LT &&
            relation != Comparison.LE
        ) {
            Failed(
                ErrorCode.IllegalArgument,
                "关系 ${relation.symbol} 不支持；EQ/NE 没有通用二值指示表达。 / Relation ${relation.symbol} is unsupported; EQ/NE have no generic binary-indicator formulation."
            )
        } else if (!strictBoundary.isUsableConditionValue()) {
            Failed(
                ErrorCode.IllegalArgument,
                "strictBoundary/g 必须为有限且可编码的 solver 值。 / strictBoundary/g must be finite and solver-representable."
            )
        } else {
            val zero = strictBoundary - strictBoundary
            if (strictBoundary.compareTo(zero) <= 0) {
                Failed(
                    ErrorCode.IllegalArgument,
                    "strictBoundary/g 必须大于 0。 / strictBoundary/g must be greater than 0."
                )
            } else {
                ok
            }
        }
    } catch (_: RuntimeException) {
        Failed(
            ErrorCode.IllegalArgument,
            "strictBoundary/g 校验失败。 / Failed to validate strictBoundary/g."
        )
    }
}

private fun <V> normalizedCondition(
    relation: Comparison,
    bounds: ConditionBounds<V>,
    strictBoundary: V
): Ret<NormalizedCondition<V>> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        when (val validation = validateRelationAndBoundary(
            relation = relation,
            strictBoundary = strictBoundary
        )) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }

        if (!strictBoundary.isUsableConditionValue() ||
            !bounds.lower.isUsableConditionValue() ||
            !bounds.upper.isUsableConditionValue()
        ) {
            return conditionFailure(
                "ConditionBounds 和 strictBoundary 必须是有限且可编码的 solver 值。 / ConditionBounds and strictBoundary must be finite solver-representable values."
            )
        }
        if (bounds.lower.compareTo(bounds.upper) > 0) {
            return conditionFailure(
                "ConditionBounds 要求 lower <= upper。 / ConditionBounds requires lower <= upper."
            )
        }

        val zero = strictBoundary - strictBoundary
        val negatePolynomial = relation == Comparison.LT || relation == Comparison.LE
        val qLower = if (negatePolynomial) -bounds.upper else bounds.lower
        val qUpper = if (negatePolynomial) -bounds.lower else bounds.upper
        val trueLower = when (relation) {
            Comparison.GT, Comparison.LT -> strictBoundary
            Comparison.GE, Comparison.LE -> zero
            Comparison.EQ, Comparison.NE -> zero
        }
        val falseUpper = when (relation) {
            Comparison.GT, Comparison.LT -> zero
            Comparison.GE, Comparison.LE -> -strictBoundary
            Comparison.EQ, Comparison.NE -> zero
        }

        if (!qLower.isUsableConditionValue() ||
            !qUpper.isUsableConditionValue() ||
            !trueLower.isUsableConditionValue() ||
            !falseUpper.isUsableConditionValue()
        ) {
            return conditionFailure(
                "归一化后的条件范围必须为有限且可编码的 solver 值。 / The normalized condition range must be finite and solver-representable."
            )
        }
        if (qLower.compareTo(qUpper) > 0) {
            return conditionFailure(
                "归一化后的条件范围无效。 / The normalized condition range is invalid."
            )
        }
        if (falseUpper.compareTo(trueLower) >= 0) {
            return conditionFailure(
                "真、假分支之间必须存在正间隔。 / A positive gap must separate the true and false branches."
            )
        }

        val trueBranchExists = qUpper.compareTo(trueLower) >= 0
        val falseBranchExists = qLower.compareTo(falseUpper) <= 0
        if (!trueBranchExists && !falseBranchExists) {
            return conditionFailure(
                "给定范围没有覆盖有效真分支或假分支，无法构造可行二值条件。 / The supplied range intersects neither a valid true nor false branch, so no feasible binary condition can be constructed."
            )
        }

        val lowerAdjustment = trueLower - qLower
        val upperAdjustment = qUpper - falseUpper
        if (!lowerAdjustment.isUsableConditionValue() || !upperAdjustment.isUsableConditionValue()) {
            return conditionFailure(
                "条件范围计算产生非有限或不可编码系数。 / Condition range calculation produced a non-finite or unrepresentable coefficient."
            )
        }

        Ok(
            NormalizedCondition(
                qLower = qLower,
                qUpper = qUpper,
                trueLower = trueLower,
                falseUpper = falseUpper,
                negatePolynomial = negatePolynomial,
                trueBranchExists = trueBranchExists,
                falseBranchExists = falseBranchExists
            )
        )
    } catch (_: RuntimeException) {
        conditionFailure(
            "条件范围计算失败。 / Failed to calculate conditional bounds."
        )
    }
}

/**
 * 预检关系、业务间隔和条件范围。 / Precheck a relation, business gap, and condition range.
 *
 * 当范围只覆盖一个分支时预检仍然成功，范围驱动约束会自动固定指示变量，供上层常量折叠。
 * Validation still succeeds when the range covers only one branch; the range-driven constraints
 * then fix the indicator for upper-layer constant folding.
 *
 * @param bounds 条件多项式的有限范围 / finite range of the condition polynomial
 * @param relation 条件关系 / condition relation
 * @param strictBoundary 真、假分支的最小间隔 g / minimum true-false branch gap g
 * @return 成功或带双语消息的失败结果 / success or failure with a bilingual message
 */
fun <V> validateConditionBounds(
    bounds: ConditionBounds<V>,
    relation: Comparison,
    strictBoundary: V
): Try where V : RealNumber<V>, V : NumberField<V> {
    return when (val validation = normalizedCondition(
        relation = relation,
        bounds = bounds,
        strictBoundary = strictBoundary
    )) {
        is Ok -> ok
        is Failed -> Failed(validation.error)
        is Fatal -> Fatal(validation.errors)
    }
}

/**
 * 检查条件范围可能覆盖的真、假分支。 / Check which true and false branches a condition range can cover.
 *
 * @param bounds 条件多项式的有限范围 / finite range of the condition polynomial
 * @param relation 条件关系 / condition relation
 * @param strictBoundary 真、假分支的最小间隔 g / minimum true-false branch gap g
 * @return 分支覆盖信息 / branch coverage information
 */
internal fun <V> conditionBranchCoverage(
    bounds: ConditionBounds<V>,
    relation: Comparison,
    strictBoundary: V
): Ret<ConditionBranchCoverage> where V : RealNumber<V>, V : NumberField<V> {
    return when (val validation = normalizedCondition(
        relation = relation,
        bounds = bounds,
        strictBoundary = strictBoundary
    )) {
        is Ok -> Ok(
            ConditionBranchCoverage(
                trueBranchPossible = validation.value.trueBranchExists,
                falseBranchPossible = validation.value.falseBranchExists
            )
        )
        is Failed -> Failed(validation.error)
        is Fatal -> Fatal(validation.errors)
    }
}

private fun <V> classifyValue(
    d: V,
    relation: Comparison,
    strictBoundary: V
): Ret<TruthValue> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (!d.isUsableConditionValue()) {
            return conditionFailure(
                "条件值 d 必须为有限且可编码的 solver 值。 / Condition value d must be finite and solver-representable."
            )
        }

        val zero = strictBoundary - strictBoundary
        val negativeBoundary = -strictBoundary
        val result = when (relation) {
            Comparison.GT -> when {
                d.compareTo(strictBoundary) >= 0 -> TruthValue.True
                d.compareTo(zero) <= 0 -> TruthValue.False
                else -> TruthValue.Undefined
            }
            Comparison.GE -> when {
                d.compareTo(zero) >= 0 -> TruthValue.True
                d.compareTo(negativeBoundary) <= 0 -> TruthValue.False
                else -> TruthValue.Undefined
            }
            Comparison.LT -> when {
                d.compareTo(negativeBoundary) <= 0 -> TruthValue.True
                d.compareTo(zero) >= 0 -> TruthValue.False
                else -> TruthValue.Undefined
            }
            Comparison.LE -> when {
                d.compareTo(zero) <= 0 -> TruthValue.True
                d.compareTo(strictBoundary) >= 0 -> TruthValue.False
                else -> TruthValue.Undefined
            }
            Comparison.EQ, Comparison.NE -> {
                return conditionFailure(
                    "关系 ${relation.symbol} 不支持 classifier。 / Relation ${relation.symbol} is unsupported by the classifier."
                )
            }
        }
        Ok(result)
    } catch (_: RuntimeException) {
        conditionFailure(
            "条件值判定失败。 / Failed to classify the condition value."
        )
    }
}

/**
 * 按关系和业务间隔对条件差值 d 进行三值判定。 / Classify condition difference d using a relation and business gap.
 *
 * 判定严格使用 GT: `d >= g`/`d <= 0`、GE: `d >= 0`/`d <= -g`、
 * LT: `d <= -g`/`d >= 0`、LE: `d <= 0`/`d >= g`；间隔返回 [TruthValue.Undefined]。
 * Classification uses exactly GT: `d >= g`/`d <= 0`, GE: `d >= 0`/`d <= -g`,
 * LT: `d <= -g`/`d >= 0`, and LE: `d <= 0`/`d >= g`; the gap returns [TruthValue.Undefined].
 *
 * @param d 统一的 lhs-rhs 条件差值 / unified lhs-rhs condition difference
 * @param relation 条件关系 / condition relation
 * @param strictBoundary 真、假分支的最小间隔 g / minimum true-false branch gap g
 * @return 三值判定结果，参数非法或关系不支持时失败 / three-valued result, or failure for invalid arguments or unsupported relations
 */
fun <V> classify(
    d: V,
    relation: Comparison,
    strictBoundary: V
): Ret<TruthValue> where V : RealNumber<V>, V : NumberField<V> {
    return when (val validation = validateRelationAndBoundary(
        relation = relation,
        strictBoundary = strictBoundary
    )) {
        is Ok -> classifyValue(
            d = d,
            relation = relation,
            strictBoundary = strictBoundary
        )
        is Failed -> Failed(validation.error)
        is Fatal -> Fatal(validation.errors)
    }
}

/**
 * 使用声明范围预检后对条件差值 d 进行三值判定。 / Classify condition difference d after validating its declared range.
 *
 * `bounds` 用于复用与建模相同的范围和分支预检；具体 d 仍按关系矩阵判定。
 * `bounds` is used to reuse the same range and branch precheck as modeling; d itself is classified by the relation matrix.
 *
 * @param d 统一的 lhs-rhs 条件差值 / unified lhs-rhs condition difference
 * @param relation 条件关系 / condition relation
 * @param bounds 条件多项式的有限范围 / finite range of the condition polynomial
 * @param strictBoundary 真、假分支的最小间隔 g / minimum true-false branch gap g
 * @return 三值判定结果，预检失败或关系不支持时失败 / three-valued result, or failure when precheck fails or relation is unsupported
 */
fun <V> classify(
    d: V,
    relation: Comparison,
    bounds: ConditionBounds<V>,
    strictBoundary: V
): Ret<TruthValue> where V : RealNumber<V>, V : NumberField<V> {
    return when (val validation = normalizedCondition(
        relation = relation,
        bounds = bounds,
        strictBoundary = strictBoundary
    )) {
        is Ok -> classifyValue(
            d = d,
            relation = relation,
            strictBoundary = strictBoundary
        )
        is Failed -> Failed(validation.error)
        is Fatal -> Fatal(validation.errors)
    }
}

/**
 * 按有限条件范围构造关系指示约束。 / Build relation-indicator constraints from a finite condition range.
 *
 * `poly` 表示统一差值 `d = lhs - rhs`，`indicator = 1` 表示真分支。
 * 对 LT/LE，内部使用 `q = -d` 归一化，并对 `q in [Lq, Uq]` 构造：
 * `q >= Lq + (a - Lq) * y` 与 `q <= b + (Uq - b) * y`。
 * `poly` is the unified difference `d = lhs - rhs`, and `indicator = 1` denotes the true branch.
 * LT/LE are normalized with `q = -d`; for `q in [Lq, Uq]` this builds:
 * `q >= Lq + (a - Lq) * y` and `q <= b + (Uq - b) * y`.
 *
 * 范围只覆盖一个分支时，返回的两条约束会固定 indicator；不会使用或回退到默认 Big-M。
 * When the range covers only one branch, the returned pair fixes indicator; no default Big-M is used or consulted.
 *
 * @param poly 统一的 lhs-rhs 条件差值多项式 / unified lhs-rhs condition difference polynomial
 * @param indicator 二值指示变量 / binary indicator variable
 * @param relation 条件关系 / condition relation
 * @param bounds poly 的有限范围 / finite range of poly
 * @param strictBoundary 真、假分支的最小间隔 g / minimum true-false branch gap g
 * @param namePrefix 约束名称前缀 / constraint name prefix
 * @return 生成的两条约束，参数非法或关系不支持时失败 / two generated constraints, or failure for invalid arguments or unsupported relations
 */
fun <V> relationIndicatorConstraints(
    poly: LinearPolynomial<V>,
    indicator: AbstractVariableItem<*, *>,
    relation: Comparison,
    bounds: ConditionBounds<V>,
    strictBoundary: V,
    namePrefix: String
): Ret<List<LinearInequality<V>>> where V : RealNumber<V>, V : NumberField<V> {
    return try {
    if (!indicator.type.isBinaryType) {
        return conditionFailure(
            "指示变量必须是二值变量。 / The indicator must be a binary variable."
        )
    }
    if (poly.monomials.any { it.symbol == indicator }) {
        return conditionFailure(
            "条件多项式不得包含其指示变量。 / The condition polynomial must not contain its own indicator variable."
        )
    }
    if (!poly.hasUsableConditionValues() || !poly.hasUsableFlattenedConditionValues()) {
        return conditionFailure(
            "条件多项式的常数和所有系数必须为有限且可编码的 solver 值。 / The constant and every coefficient of the condition polynomial must be finite and solver-representable."
        )
    }

    val condition = when (val validation = normalizedCondition(
        relation = relation,
        bounds = bounds,
        strictBoundary = strictBoundary
    )) {
        is Ok -> validation.value
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
    }

    val q = if (condition.negatePolynomial) {
        LinearPolynomial(
            monomials = poly.monomials.map { monomial ->
                LinearMonomial(
                    coefficient = -monomial.coefficient,
                    symbol = monomial.symbol
                )
            },
            constant = -poly.constant
        )
    } else {
        poly
    }
    if (!q.hasUsableConditionValues()) {
        return conditionFailure(
            "条件多项式取负后产生非有限或不可编码值。 / Negating the condition polynomial produced a non-finite or unrepresentable value."
        )
    }

    val lowerAdjustment = condition.trueLower - condition.qLower
    val upperAdjustment = condition.qUpper - condition.falseUpper
    val lowerIndicatorCoefficient = -lowerAdjustment
    val upperIndicatorCoefficient = -upperAdjustment
    if (!lowerAdjustment.isUsableConditionValue() ||
        !upperAdjustment.isUsableConditionValue() ||
        !lowerIndicatorCoefficient.isUsableConditionValue() ||
        !upperIndicatorCoefficient.isUsableConditionValue()
    ) {
        return conditionFailure(
            "条件约束的差值或取负产生非有限或不可编码值。 / A difference or negation in the condition constraints produced a non-finite or unrepresentable value."
        )
    }

    val lowerConstraint = LinearInequality(
        lhs = LinearPolynomial(
            monomials = q.monomials + LinearMonomial(
                coefficient = lowerIndicatorCoefficient,
                symbol = indicator
            ),
            constant = q.constant
        ),
        rhs = LinearPolynomial(
            monomials = emptyList(),
            constant = condition.qLower
        ),
        comparison = Comparison.GE,
        name = "${namePrefix}_lower"
    )
    val upperConstraint = LinearInequality(
        lhs = LinearPolynomial(
            monomials = q.monomials + LinearMonomial(
                coefficient = upperIndicatorCoefficient,
                symbol = indicator
            ),
            constant = q.constant
        ),
        rhs = LinearPolynomial(
            monomials = emptyList(),
            constant = condition.falseUpper
        ),
        comparison = Comparison.LE,
        name = "${namePrefix}_upper"
    )

    if (!lowerConstraint.hasUsableConditionValues() || !upperConstraint.hasUsableConditionValues()) {
        return conditionFailure(
            "生成的条件约束包含非有限或不可编码系数。 / The generated condition constraints contain non-finite or unrepresentable coefficients."
        )
    }

    Ok(listOf(lowerConstraint, upperConstraint))
    } catch (_: RuntimeException) {
        conditionFailure(
            "构造条件指示约束失败。 / Failed to construct condition-indicator constraints."
        )
    }
}
