@file:Suppress("unused")

/** 蕴含函数符号 / If-then implication function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AbstractMutableTokenList
import fuookami.ospf.kotlin.core.token.AbstractMutableTokenTable
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.token.TokenListSnapshot
import fuookami.ospf.kotlin.core.variable.*

/**
 * 蕴含-则函数符号 / If-Then function symbol
 *
 * 提供 [IfThenFunction]，实现条件-结果联动的范围驱动线性化建模。
 * Provides [IfThenFunction] for range-driven linearized conditional-consequent modeling.
 */

private data class IfThenBounds<V>(
    val condition: ConditionBounds<V>,
    val then: ConditionBounds<V>
) where V : RealNumber<V>, V : NumberField<V>

private data class IfThenResultRangeSnapshot(
    val range: ValueRange<Flt64>?,
    val set: Boolean,
    val committed: Boolean
)

private data class IfThenTokenSnapshot<V : RealNumber<V>>(
    val tokenList: AbstractMutableTokenList<V>?,
    val state: TokenListSnapshot<V>?
)

private fun <V> V.isIfThenFinite(): Boolean where V : RealNumber<V>, V : NumberField<V> {
    if (!isFinite()) {
        return false
    }
    val nan = constants.nan ?: return true
    return this != nan
}

private fun <T> ifThenFailure(message: String): Ret<T> {
    return Failed(ErrorCode.IllegalArgument, message)
}

private fun <T> ifThenRuntimeFailure(operation: String, error: RuntimeException): Ret<T> {
    val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName ?: "unknown runtime error"
    return ifThenFailure(
        "$operation 失败：$detail / $operation failed: $detail"
    )
}

private fun <V> LinearPolynomial<V>.hasUsableIfThenValues(
    converter: IntoValue<V>
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    return isUsableConditionBound(constant, converter) &&
        monomials.all { isUsableConditionBound(it.coefficient, converter) }
}

private fun <V> LinearPolynomial<V>.hasUsableIfThenFlattenedValues(
    converter: IntoValue<V>
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    return try {
        val zero = constant - constant
        val flattened = LinearInequality(
            lhs = this,
            rhs = LinearPolynomial(emptyList(), zero),
            comparison = Comparison.EQ
        ).toLinearFlattenData().getOrElse { return false }
        LinearPolynomial(
            monomials = flattened.monomials,
            constant = flattened.constant
        ).hasUsableIfThenValues(converter)
    } catch (_: RuntimeException) {
        false
    }
}

private fun <V> LinearInequality<V>.hasUsableIfThenValues(
    converter: IntoValue<V>
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    return lhs.hasUsableIfThenValues(converter) && rhs.hasUsableIfThenValues(converter)
}

/**
 * 校验模型实际写入前的最终扁平化结果。 / Validate the final flattened result before model insertion.
 *
 * 两侧单项式分别有限并不能保证合并后的系数有限；模型扁平化还会展开中间符号并合并同变量项，
 * 因此必须校验与模型相同的最终结果。 / Finite individual monomials do not guarantee finite
 * merged coefficients; model flattening also expands intermediate symbols and merges terms,
 * so the final result from the same model path must be validated.
 */
private fun <V> LinearInequality<V>.hasUsableIfThenFlattenedValues(
    converter: IntoValue<V>
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    return try {
        val flattened = toLinearFlattenData().getOrElse { return false }
        val flattenedPolynomial = LinearPolynomial(
            monomials = flattened.monomials,
            constant = flattened.constant
        )
        flattenedPolynomial.hasUsableIfThenValues(converter)
    } catch (_: RuntimeException) {
        false
    }
}

private fun ifThenRollbackFailure(failure: Try, rollback: Try): Try {
    if (rollback is Ok) {
        return failure
    }

    val errors = ArrayList<Error<ErrorCode>>()
    when (failure) {
        is Ok -> {}
        is Failed -> errors += failure.error
        is Fatal -> errors += failure.errors
    }
    when (rollback) {
        is Ok -> {}
        is Failed -> errors += rollback.error
        is Fatal -> errors += rollback.errors
    }
    return Fatal(errors)
}

@Suppress("UNCHECKED_CAST")
private fun <V> mutableIfThenTokenList(
    tokens: AddableTokenCollection<V>
): AbstractMutableTokenList<V>? where V : RealNumber<V>, V : NumberField<V> {
    return when (tokens) {
        is AbstractMutableTokenTable<*> -> tokens.tokenList as? AbstractMutableTokenList<V>
        is AbstractMutableTokenList<*> -> tokens as? AbstractMutableTokenList<V>
        else -> null
    }
}

private fun <V> snapshotIfThenTokens(
    tokens: AddableTokenCollection<V>
): IfThenTokenSnapshot<V> where V : RealNumber<V>, V : NumberField<V> {
    val tokenList = mutableIfThenTokenList(tokens)
    return IfThenTokenSnapshot(
        tokenList = tokenList,
        state = tokenList?.snapshotState()
    )
}

private fun <V> rollbackIfThenTokens(
    tokens: AddableTokenCollection<V>,
    snapshot: IfThenTokenSnapshot<V>
): Try where V : RealNumber<V>, V : NumberField<V> {
    val tokenList = snapshot.tokenList
    val state = snapshot.state
    if (tokenList == null || state == null) {
        // Existing AddableTokenCollection implementations promise atomic batch add;
        // there is no observable mutable state to restore through this interface. /
        // 现有 AddableTokenCollection 实现承诺批量添加具有原子性；该接口没有可恢复的可观察可变状态。
        return ok
    }
    return try {
        tokenList.restoreState(state)
    } catch (error: RuntimeException) {
        ifThenRuntimeFailure(
            operation = "回滚 IfThen 辅助 token / Roll back IfThen auxiliary tokens",
            error = error
        )
    }
}

private fun <V> validateIfThenBounds(
    bounds: ConditionBounds<V>,
    label: String,
    converter: IntoValue<V>
): Ret<ConditionBounds<V>> where V : RealNumber<V>, V : NumberField<V> {
    if (!isUsableConditionBound(bounds.lower, converter) ||
        !isUsableConditionBound(bounds.upper, converter)
    ) {
        return ifThenFailure(
            "$label 的 lower/upper 必须为有限值。 / $label.lower/upper must be finite."
        )
    }
    if (bounds.lower.compareTo(bounds.upper) > 0) {
        return ifThenFailure(
            "$label 要求 lower <= upper。 / $label requires lower <= upper."
        )
    }
    return Ok(bounds)
}

/**
 * 蕴含-则函数：条件成立时结果等于 then 多项式，否则结果为零。
 * If-Then function: the result equals then polynomial when the condition is true, and is zero otherwise.
 *
 * 条件关系使用共享三值 classifier 和范围驱动指示约束。旧版 [bigM] 参数保留用于源码兼容，
 * 但不能替代条件或 then 多项式的有限范围。/ The legacy [bigM] parameter is retained for source
 * compatibility, but cannot replace finite bounds for the condition or then polynomial.
 *
 * @property condition 条件线性多项式 / condition linear polynomial
 * @property thenPoly "则"线性多项式 / the then linear polynomial
 * @property relation 条件比较关系，默认 [Comparison.GT] / condition relation, defaulting to [Comparison.GT]
 * @param converter 值类型转换器 / value type converter
 * @param bigM 旧版 Big-M 参数，仅用于源码兼容 / legacy Big-M parameter kept for source compatibility
 * @param tolerance 零容差 / zero tolerance
 * @param strictBoundary 真、假分支的最小间隔，缺省取 tolerance / minimum true-false branch gap, defaulting to tolerance
 * @property delta 离散步长，缺省取 [strictBoundary] / discrete step, defaulting to [strictBoundary]
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 * @param conditionBounds 条件多项式的显式有限范围 / explicit finite bounds of the condition polynomial
 * @param thenBounds then 多项式的显式有限范围 / explicit finite bounds of the then polynomial
 * @param bounds 条件多项式范围的兼容别名 / compatibility alias for condition bounds
 */
class IfThenFunction<V>(
    val condition: LinearPolynomial<V>,
    val thenPoly: LinearPolynomial<V>,
    converter: IntoValue<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "ifthen",
    override var displayName: String? = null,
    val relation: Comparison = Comparison.GT,
    conditionBounds: ConditionBounds<V>? = null,
    thenBounds: ConditionBounds<V>? = null,
    bounds: ConditionBounds<V>? = null,
    delta: V? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val explicitBigM: V? = bigM
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    val strictBoundary: V = strictBoundary ?: this.tolerance
    private val requestedDelta: V? = delta
    val delta: V = requestedDelta ?: this.strictBoundary
    private val declaredConditionBounds: ConditionBounds<V>? = conditionBounds ?: bounds
    private val thenBounds: ConditionBounds<V>? = thenBounds
    private var resultRangeCommitted = false

    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_ind")
    private val resultVar: RealVar by lazy { RealVar("${name}_y") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(indicatorVar, resultVar)

    val result: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)
    }

    override val resultPolynomial: LinearPolynomial<V>
        get() = result

    /**
     * 对给定符号值执行统一三值条件判定。 / Classify the condition using shared three-valued semantics.
     *
     * @param values 符号到值的映射 / mapping from symbols to values
     * @return 条件判定结果；缺失输入或非法关系时失败 / condition result; failure for missing input or invalid relation
     */
    fun classify(values: Map<Symbol, V>): Ret<TruthValue> {
        return try {
            val conditionValue = condition.evaluateWith(values)
                ?: return ifThenFailure(
                    "条件输入缺失，无法判定。 / A condition input is missing, so the condition cannot be classified."
                )
            classifyDiscreteCondition(
                d = conditionValue,
                relation = relation,
                delta = delta,
                strictBoundary = strictBoundary
            )
        } catch (error: RuntimeException) {
            val detail = error.message?.takeIf { it.isNotBlank() }
                ?: error::class.simpleName
                ?: "unknown runtime error"
            ifThenFailure(
                "条件判定失败：$detail / Condition classification failed: $detail"
            )
        }
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        return try {
            when (val classification = classify(values)) {
                is Ok -> when (classification.value) {
                    TruthValue.True -> thenPoly.evaluateWith(values)
                    TruthValue.False -> converter.zero
                    TruthValue.Undefined -> null
                }
                is Failed, is Fatal -> null
            }
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun resolveBounds(
        poly: LinearPolynomial<V>,
        explicitBounds: ConditionBounds<V>?,
        label: String
    ): Ret<ConditionBounds<V>> {
        return try {
            // Explicit bounds are an intentional solver contract and must override an
            // adapter's broad or synthetic range. / 显式范围是调用方的 solver 契约，必须覆盖适配器的宽泛或合成范围。
            val resolved = explicitBounds ?: poly.finiteBounds(converter)?.let {
                ConditionBounds(
                    lower = it.lower,
                    upper = it.upper
                ).takeIf { bounds ->
                    isUsableConditionBound(bounds.lower, converter) &&
                        isUsableConditionBound(bounds.upper, converter) &&
                        bounds.lower.compareTo(bounds.upper) <= 0
                }
            } ?: return ifThenFailure(
                "$label 缺失有限范围：请提供 ConditionBounds。 / $label has no finite bounds: provide ConditionBounds."
            )
            validateIfThenBounds(resolved, label, converter)
        } catch (error: RuntimeException) {
            ifThenRuntimeFailure(
                operation = "解析 $label / Resolve $label",
                error = error
            )
        }
    }

    private fun validatePolynomial(
        poly: LinearPolynomial<V>,
        label: String
    ): Try {
        if (!poly.constant.isIfThenFinite() ||
            poly.monomials.any { !it.coefficient.isIfThenFinite() } ||
            !poly.hasUsableIfThenValues(converter)
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "$label 的系数和常数必须为有限且可编码的 solver 值。 / The coefficients and constant of $label must be finite and solver-representable."
            )
        }
        return ok
    }

    private fun validateFinalThenPolynomial(): Try {
        return if (thenPoly.hasUsableIfThenFlattenedValues(converter)) {
            ok
        } else {
            Failed(
                ErrorCode.IllegalArgument,
                "then 多项式展开合并后的系数必须为有限且可编码的 solver 值。 / The coefficients of the flattened and merged then polynomial must be finite and solver-representable."
            )
        }
    }

    private fun resolveRegistrationBounds(): Ret<IfThenBounds<V>> {
        explicitBigM?.let { value ->
            if (!isUsableConditionBound(value, converter)) {
                return ifThenFailure(
                    "bigM 必须为有限且可编码的 solver 值，且不能替代条件范围。 / bigM must be finite and solver-representable and cannot replace condition bounds."
                )
            }
            val zero = value - value
            if (value.compareTo(zero) <= 0) {
                return ifThenFailure(
                    "bigM 必须大于 0，且不能替代条件范围。 / bigM must be greater than 0 and cannot replace condition bounds."
                )
            }
        }
        when (val validation = validatePolynomial(condition, "条件 / condition")) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        when (val validation = validatePolynomial(thenPoly, "then")) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        when (val validation = validateFinalThenPolynomial()) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
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
            return ifThenFailure(
                "strictBoundary 和 delta 必须是有限且可编码的 solver 值。 / strictBoundary and delta must be finite solver-representable values."
            )
        }
        val resolvedConditionBounds = when (val result = resolveBounds(
            poly = condition,
            explicitBounds = declaredConditionBounds,
            label = "条件范围 / Condition bounds"
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val resolvedThenBounds = when (val result = resolveBounds(
            poly = thenPoly,
            explicitBounds = thenBounds,
            label = "then 范围 / Then bounds"
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return when (val validation = normalizeDiscreteCondition(
            poly = condition,
            bounds = resolvedConditionBounds,
            relation = relation,
            delta = delta,
            strictBoundary = strictBoundary
        )) {
            is Ok -> Ok(IfThenBounds(resolvedConditionBounds, resolvedThenBounds))
            is Failed -> Failed(validation.error)
            is Fatal -> Fatal(validation.errors)
        }
    }

    private fun buildResultRange(bounds: ConditionBounds<V>): Ret<ValueRange<Flt64>> {
        return try {
            val zero = converter.zero
            val lower = if (bounds.lower.compareTo(zero) <= 0) bounds.lower else zero
            val upper = if (bounds.upper.compareTo(zero) >= 0) bounds.upper else zero
            val lowerFlt64 = converter.fromValue(lower)
            val upperFlt64 = converter.fromValue(upper)
            if (!isUsableIfThenResultBound(lowerFlt64) || !isUsableIfThenResultBound(upperFlt64)) {
                return ifThenFailure(
                    "IfThen 结果范围必须能表示为有限的 Flt64。 / The IfThen result range must be representable as finite Flt64 values."
                )
            }
            return when (val result = ValueRange(
                lb = lowerFlt64,
                ub = upperFlt64,
                lbInterval = Interval.Closed,
                ubInterval = Interval.Closed,
                constants = Flt64
            )) {
                is Ok -> Ok(result.value)
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        } catch (error: RuntimeException) {
            ifThenRuntimeFailure(
                operation = "构造 IfThen 结果范围 / Build IfThen result range",
                error = error
            )
        }
    }

    private fun isUsableIfThenResultBound(value: Flt64): Boolean {
        return value.isFinite() &&
            value != Flt64.nan &&
            value.compareTo(Flt64.minimum) > 0 &&
            value.compareTo(Flt64.maximum) < 0
    }

    private fun snapshotResultRange(): IfThenResultRangeSnapshot {
        return IfThenResultRangeSnapshot(
            range = resultVar.range.range,
            set = resultVar.range.set,
            committed = resultRangeCommitted
        )
    }

    private fun restoreResultRange(snapshot: IfThenResultRangeSnapshot): Try {
        return try {
            if (resultVar.range.range != snapshot.range || resultVar.range.set != snapshot.set) {
                resultVar.range.restore(snapshot.range, snapshot.set)
            }
            resultRangeCommitted = snapshot.committed
            if (resultVar.range.range != snapshot.range || resultVar.range.set != snapshot.set) {
                Failed(
                    ErrorCode.ApplicationError,
                    "IfThen 结果范围恢复后仍不一致。 / The IfThen result range remained inconsistent after restoration."
                )
            } else {
                ok
            }
        } catch (error: RuntimeException) {
            resultRangeCommitted = snapshot.committed
            ifThenRuntimeFailure(
                operation = "恢复 IfThen 结果范围 / Restore IfThen result range",
                error = error
            )
        }
    }

    private fun commitResultRange(range: ValueRange<Flt64>) {
        // Keep the local marker behind the range write so a throwing write cannot look committed. /
        // 将本地标记置于范围写入之后，避免抛出异常的写入被误认为已提交。
        resultVar.range.set(range)
        resultRangeCommitted = true
    }

    private fun registerIfThenTokens(
        tokens: AddableTokenCollection<V>,
        snapshot: IfThenTokenSnapshot<V>
    ): Try {
        val registration = try {
            tokens.add(helperVariables)
        } catch (error: RuntimeException) {
            ifThenRuntimeFailure(
                operation = "注册 IfThen 辅助 token / Register IfThen auxiliary tokens",
                error = error
            )
        }
        return when (registration) {
            is Ok -> ok
            is Failed -> ifThenRollbackFailure(
                Failed(registration.error),
                rollbackIfThenTokens(tokens, snapshot)
            )
            is Fatal -> ifThenRollbackFailure(
                Fatal(registration.errors),
                rollbackIfThenTokens(tokens, snapshot)
            )
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        val rangeSnapshot = try {
            snapshotResultRange()
        } catch (error: RuntimeException) {
            return ifThenRuntimeFailure(
                operation = "读取 IfThen 结果范围快照 / Read IfThen result-range snapshot",
                error = error
            )
        }
        val tokenSnapshot = try {
            snapshotIfThenTokens(tokens)
        } catch (error: RuntimeException) {
            val failure: Try = ifThenRuntimeFailure(
                operation = "读取 IfThen token 快照 / Read IfThen token snapshot",
                error = error
            )
            return ifThenRollbackFailure(failure, restoreResultRange(rangeSnapshot))
        }
        if (tokenSnapshot.tokenList != null && tokenSnapshot.state == null) {
            return ifThenRollbackFailure(
                ifThenFailure(
                    "可变 token 列表不支持事务快照，无法保证 IfThen 注册原子性。 / The mutable token list does not support transactional snapshots, so IfThen registration cannot be made atomic."
                ),
                restoreResultRange(rangeSnapshot)
            )
        }
        var tokenOperationStarted = false
        return try {
            val bounds = when (val result = resolveRegistrationBounds()) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val resultRange = when (val result = buildResultRange(bounds.then)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            tokenOperationStarted = true
            when (val result = registerIfThenTokens(tokens, tokenSnapshot)) {
                is Ok -> {}
                is Failed -> return ifThenRollbackFailure(
                    Failed(result.error),
                    restoreResultRange(rangeSnapshot)
                )
                is Fatal -> return ifThenRollbackFailure(
                    Fatal(result.errors),
                    restoreResultRange(rangeSnapshot)
                )
            }

            try {
                // Commit the result range only after token registration succeeds. / 仅在辅助 token 注册成功后提交结果范围。
                commitResultRange(resultRange)
                ok
            } catch (error: RuntimeException) {
                val commitFailure: Try = ifThenRuntimeFailure(
                    operation = "提交 IfThen 结果范围 / Commit IfThen result range",
                    error = error
                )
                val tokenRollback = rollbackIfThenTokens(tokens, tokenSnapshot)
                val rangeRollback = restoreResultRange(rangeSnapshot)
                ifThenRollbackFailure(
                    ifThenRollbackFailure(commitFailure, tokenRollback),
                    rangeRollback
                )
            }
        } catch (error: RuntimeException) {
            val failure: Try = ifThenRuntimeFailure(
                operation = "注册 IfThen 辅助 token / Register IfThen auxiliary tokens",
                error = error
            )
            val tokenRollback = if (tokenOperationStarted) {
                rollbackIfThenTokens(tokens, tokenSnapshot)
            } else {
                ok
            }
            ifThenRollbackFailure(
                ifThenRollbackFailure(failure, tokenRollback),
                restoreResultRange(rangeSnapshot)
            )
        }
    }

    private fun buildConstraints(
        bounds: IfThenBounds<V>
    ): Ret<List<LinearInequality<V>>> {
        val normalized = when (val result = normalizeDiscreteCondition(
            poly = condition,
            relation = relation,
            bounds = bounds.condition,
            delta = delta,
            strictBoundary = strictBoundary
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val zero = converter.zero
        val one = converter.one

        fun finish(constraints: List<LinearInequality<V>>): Ret<List<LinearInequality<V>>> {
            return if (constraints.all {
                it.hasUsableIfThenValues(converter) &&
                    it.hasUsableIfThenFlattenedValues(converter)
            }) {
                Ok(constraints)
            } else {
                ifThenFailure(
                    "IfThen 生成的约束包含非有限或不可编码的最终系数。 / IfThen generated constraints contain non-finite or unrepresentable final coefficients."
                )
            }
        }

        normalized.fixedValue?.let { fixedValue ->
            val constraints = mutableListOf<LinearInequality<V>>()
            val indicatorValue = if (fixedValue == TruthValue.True) one else zero
            constraints += fixedVariableEquality(
                variable = indicatorVar,
                value = indicatorValue,
                zero = zero,
                one = one,
                name = "${name}_fold_indicator"
            )
            if (fixedValue == TruthValue.False) {
                constraints += fixedVariableEquality(
                    variable = resultVar,
                    value = zero,
                    zero = zero,
                    one = one,
                    name = "${name}_fold_result"
                )
            } else if (thenPoly.monomials.isEmpty()) {
                constraints += fixedVariableEquality(
                    variable = resultVar,
                    value = thenPoly.constant,
                    zero = zero,
                    one = one,
                    name = "${name}_fold_result"
                )
            } else {
                constraints += LinearInequality(
                    lhs = LinearPolynomial(
                        monomials = listOf(LinearMonomial(one, resultVar)) + thenPoly.monomials.map {
                            LinearMonomial(-it.coefficient, it.symbol)
                        },
                        constant = -thenPoly.constant
                    ),
                    rhs = LinearPolynomial(emptyList(), zero),
                    comparison = Comparison.EQ,
                    name = "${name}_fold_result_then"
                )
            }
            return finish(constraints)
        }

        val indicatorConstraints = when (val result = relationIndicatorConstraints(
            poly = normalized.polynomial,
            indicator = indicatorVar,
            relation = Comparison.GT,
            bounds = normalized.bounds,
            strictBoundary = strictBoundary,
            namePrefix = "${name}_cond"
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val lower = bounds.then.lower
        val upper = bounds.then.upper
        val yMono = LinearMonomial(one, resultVar)
        val negThenMonomials = thenPoly.monomials.map {
            LinearMonomial(-it.coefficient, it.symbol)
        }
        val thenConstraints = listOf(
            // y <= upper * indicator / 上界：条件不满足时 y <= 0
            LinearInequality(
                lhs = LinearPolynomial(
                    monomials = listOf(yMono, LinearMonomial(-upper, indicatorVar)),
                    constant = zero
                ),
                rhs = LinearPolynomial(emptyList(), zero),
                comparison = Comparison.LE,
                name = "${name}_zero_ub"
            ),
            // y >= lower * indicator / 下界：条件不满足时 y >= 0
            LinearInequality(
                lhs = LinearPolynomial(
                    monomials = listOf(yMono, LinearMonomial(-lower, indicatorVar)),
                    constant = zero
                ),
                rhs = LinearPolynomial(emptyList(), zero),
                comparison = Comparison.GE,
                name = "${name}_zero_lb"
            ),
            // y - thenPoly <= -lower * (1 - indicator) / 条件满足时 y <= thenPoly
            LinearInequality(
                lhs = LinearPolynomial(
                    monomials = negThenMonomials + listOf(yMono, LinearMonomial(-lower, indicatorVar)),
                    constant = -thenPoly.constant
                ),
                rhs = LinearPolynomial(emptyList(), -lower),
                comparison = Comparison.LE,
                name = "${name}_then_ub"
            ),
            // y - thenPoly >= -upper * (1 - indicator) / 条件满足时 y >= thenPoly
            LinearInequality(
                lhs = LinearPolynomial(
                    monomials = negThenMonomials + listOf(yMono, LinearMonomial(-upper, indicatorVar)),
                    constant = -thenPoly.constant
                ),
                rhs = LinearPolynomial(emptyList(), -upper),
                comparison = Comparison.GE,
                name = "${name}_then_lb"
            )
        )
        val constraints = indicatorConstraints + thenConstraints
        return finish(constraints)
    }

    private fun addConstraintsAtomically(
        model: AbstractLinearMechanismModel<V>,
        constraints: List<LinearInequality<V>>
    ): Try {
        try {
            return addConstraints(model, constraints) ?: ok
        } catch (error: RuntimeException) {
            return ifThenRuntimeFailure(
                operation = "写入 IfThen 约束 / Write IfThen constraints",
                error = error
            )
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val rangeSnapshot = try {
            snapshotResultRange()
        } catch (error: RuntimeException) {
            return ifThenRuntimeFailure(
                operation = "读取 IfThen 结果范围快照 / Read IfThen result-range snapshot",
                error = error
            )
        }
        var originalConstraintCount: Int? = null
        var constraintOperationStarted = false
        return try {
            val bounds = when (val result = resolveRegistrationBounds()) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            when (val validation = validateFinalThenPolynomial()) {
                is Ok -> {}
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
            val resultRange = when (val result = buildResultRange(bounds.then)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val constraints = when (val result = buildConstraints(bounds)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            originalConstraintCount = try {
                model.constraints.size
            } catch (error: RuntimeException) {
                return ifThenRuntimeFailure(
                    operation = "读取 IfThen 约束数量 / Read IfThen constraint count",
                    error = error
                )
            }
            constraintOperationStarted = true
            when (val result = addConstraintsAtomically(
                model = model,
                constraints = constraints
            )) {
                is Ok -> {}
                is Failed -> return ifThenRollbackFailure(
                    Failed(result.error),
                    restoreResultRange(rangeSnapshot)
                )
                is Fatal -> return ifThenRollbackFailure(
                    Fatal(result.errors),
                    restoreResultRange(rangeSnapshot)
                )
            }

            try {
                // Commit the result range only after atomic constraint registration succeeds. / 仅在约束原子注册成功后提交结果范围。
                commitResultRange(resultRange)
                ok
            } catch (error: RuntimeException) {
                val commitFailure: Try = ifThenRuntimeFailure(
                    operation = "提交 IfThen 结果范围 / Commit IfThen result range",
                    error = error
                )
                val constraintRollback = try {
                    model.rollbackConstraintsTo(originalConstraintCount!!)
                } catch (rollbackError: RuntimeException) {
                    ifThenRuntimeFailure(
                        operation = "回滚 IfThen 约束 / Roll back IfThen constraints",
                        error = rollbackError
                    )
                }
                val rangeRollback = restoreResultRange(rangeSnapshot)
                ifThenRollbackFailure(
                    ifThenRollbackFailure(commitFailure, constraintRollback),
                    rangeRollback
                )
            }
        } catch (error: RuntimeException) {
            val failure: Try = ifThenRuntimeFailure(
                operation = "注册 IfThen 约束 / Register IfThen constraints",
                error = error
            )
            val constraintRollback = if (constraintOperationStarted && originalConstraintCount != null) {
                try {
                    model.rollbackConstraintsTo(originalConstraintCount!!)
                } catch (rollbackError: RuntimeException) {
                    ifThenRuntimeFailure(
                        operation = "回滚 IfThen 约束 / Roll back IfThen constraints",
                        error = rollbackError
                    )
                }
            } else {
                ok
            }
            ifThenRollbackFailure(
                ifThenRollbackFailure(failure, constraintRollback),
                restoreResultRange(rangeSnapshot)
            )
        }
    }

    companion object {
        /**
         * 创建若-则函数实例 / Create an if-then function instance.
         *
         * @param condition 条件线性多项式 / condition linear polynomial
         * @param thenPoly "则"线性多项式 / then linear polynomial
         * @param converter 值类型转换器 / value type converter
         * @param bigM 旧版 Big-M 参数，仅用于源码兼容 / legacy Big-M parameter kept for source compatibility
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @param relation 条件比较关系 / condition comparison relation
         * @param tolerance 零容差 / zero tolerance
         * @param strictBoundary 真、假分支的最小间隔 / minimum true-false branch gap
         * @param delta 离散步长 / discrete step
         * @param conditionBounds 条件多项式的显式有限范围 / explicit finite bounds of the condition polynomial
         * @param thenBounds then 多项式的显式有限范围 / explicit finite bounds of the then polynomial
         * @return [IfThenFunction] 实例 / [IfThenFunction] instance
         */
        operator fun <V> invoke(
            condition: LinearPolynomial<V>,
            thenPoly: LinearPolynomial<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null,
            relation: Comparison = Comparison.GT,
            tolerance: V? = null,
            strictBoundary: V? = null,
            conditionBounds: ConditionBounds<V>? = null,
            thenBounds: ConditionBounds<V>? = null,
            bounds: ConditionBounds<V>? = null,
            delta: V? = null
        ): IfThenFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            IfThenFunction(
                condition = condition,
                thenPoly = thenPoly,
                converter = converter,
                bigM = bigM,
                tolerance = tolerance,
                strictBoundary = strictBoundary,
                name = name,
                displayName = displayName,
                relation = relation,
                conditionBounds = conditionBounds,
                thenBounds = thenBounds,
                bounds = bounds,
                delta = delta
            )

        /**
         * 约束输入工厂：从约束输入提取条件多项式，并保留输入关系。
         * Constraint-input factory: extracts the condition polynomial and preserves the input relation.
         *
         * @param inequality 约束输入 / constraint input
         * @param converter 值类型转换器 / value type converter
         * @param thenPoly "则"线性多项式，默认为一 / then linear polynomial, defaulting to one
         * @param bigM 旧版 Big-M 参数，仅用于源码兼容 / legacy Big-M parameter kept for source compatibility
         * @param tolerance 零容差 / zero tolerance
         * @param strictBoundary 真、假分支的最小间隔 / minimum true-false branch gap
         * @param delta 离散步长 / discrete step
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @param conditionBounds 条件多项式的显式有限范围 / explicit finite bounds of the condition polynomial
          * @param thenBounds then 多项式的显式有限范围 / explicit finite bounds of the then polynomial
          * @param bounds 条件多项式范围的兼容别名 / compatibility alias for condition bounds
         * @return 包装后的线性函数符号适配器 / wrapped linear function symbol adapter
         */
        fun <V> from(
            inequality: LinearConstraintInput<V>,
            converter: IntoValue<V>,
            thenPoly: LinearPolynomial<V> = LinearPolynomial(emptyList(), converter.one),
            bigM: V? = null,
            tolerance: V? = null,
            strictBoundary: V? = null,
            name: String,
            displayName: String? = null,
            conditionBounds: ConditionBounds<V>? = null,
            thenBounds: ConditionBounds<V>? = null,
            bounds: ConditionBounds<V>? = null,
            delta: V? = null
        ): LinearFunctionSymbolAdapter<V> where V : RealNumber<V>, V : NumberField<V> {
            val conditionPoly = LinearPolynomial(
                inequality.flattenData.monomials.map {
                    LinearMonomial(it.coefficient, it.symbol)
                },
                inequality.flattenData.constant
            )
            return LinearFunctionSymbolAdapter(
                IfThenFunction(
                    condition = conditionPoly,
                    thenPoly = thenPoly,
                    converter = converter,
                    bigM = bigM,
                    tolerance = tolerance,
                    strictBoundary = strictBoundary,
                    name = name,
                    displayName = displayName,
                    relation = inequality.sign,
                    conditionBounds = conditionBounds,
                    thenBounds = thenBounds,
                    bounds = bounds,
                    delta = delta
                ),
                converter = converter
            )
        }
    }
}
