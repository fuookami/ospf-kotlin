@file:Suppress("unused")

/** 蕴含函数符号 / Implication function symbol */
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
import fuookami.ospf.kotlin.core.model.mechanism.toLinearFlattenData
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.token.AbstractMutableTokenList
import fuookami.ospf.kotlin.core.token.AbstractMutableTokenTable
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.token.TokenListSnapshot
import fuookami.ospf.kotlin.core.variable.*

private data class ResolvedImplyBounds<V>(
    val antecedent: ConditionBounds<V>,
    val consequent: ConditionBounds<V>
) where V : RealNumber<V>, V : NumberField<V>

private data class RelationAdjustments<V>(
    val lower: V,
    val upper: V
) where V : RealNumber<V>, V : NumberField<V>

private data class ImplyTokenSnapshot<V : RealNumber<V>>(
    val tokenList: AbstractMutableTokenList<V>?,
    val state: TokenListSnapshot<V>?
)

private fun <V> LinearInequality<V>.hasUsableImplySolverValues(
    converter: IntoValue<V>
): Boolean
    where V : RealNumber<V>, V : NumberField<V> {
    return hasUsableConditionSolverValues(lhs, converter) &&
        hasUsableConditionSolverValues(rhs, converter)
}

/**
 * 校验模型实际写入前的扁平化结果。 / Validate the flattened form written to the model.
 *
 * 两侧单独有限并不能保证 `lhs - rhs` 有限；模型的扁平化还会展开中间符号并合并同变量系数，
 * 因此必须在写入前检查同一条扁平化路径的结果。 / Finite values on each side do not guarantee
 * that `lhs - rhs` is finite; model flattening also expands intermediate symbols and merges
 * coefficients, so the result of that same flattening path must be checked before writing.
 */
private fun <V> LinearInequality<V>.hasUsableImplyFlattenedValues(
    converter: IntoValue<V>
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    return try {
        val flattened = toLinearFlattenData().getOrElse { return false }
        hasUsableConditionSolverValues(
            LinearPolynomial(
                monomials = flattened.monomials,
                constant = flattened.constant
            ),
            converter
        )
    } catch (_: RuntimeException) {
        false
    }
}

/**
 * 获取符号的可用求解器边界。 / Get usable solver bounds for a symbol.
 *
 * Flt64 minimum/maximum are full-range sentinels rather than proven finite bounds.
 * Flt64 的 minimum/maximum 是全范围哨兵，不是已证明的有限边界。
 */
private fun Symbol.usableImplyFlt64Bounds(): Pair<Flt64, Flt64>? {
    val range = try {
        when (this) {
            is AbstractVariableItem<*, *> -> this.range.valueRange
            is IntermediateSymbol<*> -> this.range.valueRange
            else -> null
        }
    } catch (_: RuntimeException) {
        return null
    } ?: return null

    val lower = range.lowerBound.value.unwrapOrNull() ?: return null
    val upper = range.upperBound.value.unwrapOrNull() ?: return null
    if (!isUsableConditionBound(lower, IntoValue.Identity) ||
        !isUsableConditionBound(upper, IntoValue.Identity) ||
        lower.compareTo(upper) > 0
    ) {
        return null
    }
    return lower to upper
}

private fun <T> implyFailure(message: String): Ret<T> {
    return Failed(ErrorCode.IllegalArgument, message)
}

private fun implyRollbackFailure(failure: Try, rollback: Try): Try {
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
private fun <V> snapshotImplyTokens(
    tokens: AddableTokenCollection<V>
): ImplyTokenSnapshot<V> where V : RealNumber<V>, V : NumberField<V> {
    val tokenList = when (tokens) {
        is AbstractMutableTokenTable<*> -> tokens.tokenList as? AbstractMutableTokenList<V>
        is AbstractMutableTokenList<*> -> tokens as? AbstractMutableTokenList<V>
        else -> null
    }
    return ImplyTokenSnapshot(
        tokenList = tokenList,
        state = tokenList?.snapshotState()
    )
}

private fun <V> restoreImplyTokens(
    snapshot: ImplyTokenSnapshot<V>
): Try where V : RealNumber<V>, V : NumberField<V> {
    val tokenList = snapshot.tokenList
    val state = snapshot.state
    if (tokenList == null || state == null) {
        // AddableTokenCollection batch additions are atomic by contract when no exact
        // mutable-list state is exposed through the interface. /
        // 当接口未暴露可恢复的可变列表状态时，批量添加按契约应当是原子的。
        return ok
    }
    return try {
        tokenList.restoreState(state)
    } catch (error: RuntimeException) {
        implyRuntimeFailure(
            operation = "回滚蕴含函数辅助 token / Roll back implication auxiliary tokens",
            error = error
        )
    }
}

/** 将蕴含函数边界异常转换为失败结果。 / Convert implication boundary exceptions to failure results. */
private fun <T> implyRuntimeFailure(operation: String, error: RuntimeException): Ret<T> {
    val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName ?: "unknown runtime error"
    return implyFailure(
        "$operation 失败：$detail / $operation failed: $detail"
    )
}

/**
 * 执行并校验蕴含线性化中的数值运算。 / Execute and validate an arithmetic operation used by implication linearization.
 *
 * 运算异常或非有限结果都必须转换为 [Failed]，不能把非法值写入 token 或约束。
 * Arithmetic failures and non-finite results are converted to [Failed] and must never be written to tokens or constraints.
 */
private fun <V> checkedImplyArithmetic(
    label: String,
    converter: IntoValue<V>,
    operation: () -> V
): Ret<V> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        val value = operation()
        if (isUsableConditionBound(value, converter)) {
            Ok(value)
        } else {
            implyFailure(
                "$label 产生非有限值。 / $label produced a non-finite value."
            )
        }
    } catch (_: RuntimeException) {
        implyFailure(
            "$label 计算失败。 / $label calculation failed."
        )
    }
}

/**
 * 蕴含函数符号 / Implication function symbol
 *
 * 提供 [ImplyFunction]，实现 `(antecedent relation 0) => (consequent relation 0)` 的线性化建模。
 * Provides [ImplyFunction] for linearized modeling of
 * `(antecedent relation 0) => (consequent relation 0)`.
 */

/**
 * 蕴含函数：前件成立时后件必须成立。 / Implication function: the consequent must hold when the antecedent holds.
 *
 * 前件指示器始终由其关系约束确定；后件关系约束由前件指示器门控，
 * 因此前件为假时后件可以保持未判定而不会使模型不可行。 / The antecedent indicator is always
 * determined by its relation constraints; consequent relation constraints are gated by it,
 * so an undecidable consequent does not make the model infeasible when the antecedent is false.
 *
 * [classify] 使用与建模一致的三值策略：前件为 [TruthValue.False] 时结果为真，
 * 前件为 [TruthValue.True] 时结果等于后件，前件为 [TruthValue.Undefined] 时结果未定义。
 * [classify] uses the same three-valued strategy as the model: a false antecedent is true,
 * a true antecedent has the consequent's value, and an undefined antecedent is undefined.
 *
 * @property antecedent 前件线性多项式 / antecedent linear polynomial
 * @property consequent 后件线性多项式 / consequent linear polynomial
 * @property relation 前后件使用的比较关系，默认 [Comparison.GT] / relation used by both operands, defaulting to [Comparison.GT]
 * @property strictBoundary 真、假分支之间的业务间隔 / business gap between true and false branches
 * @param converter 值类型转换器 / value type converter
 * @param bigM 旧版 Big-M 参数，仅用于源码兼容，不能替代范围 / legacy Big-M parameter kept for source compatibility; it cannot replace bounds
 * @param tolerance 兼容旧调用的容差，未传 strictBoundary 时作为业务间隔 / legacy tolerance, used as the business gap when strictBoundary is omitted
 * @param strictBoundary 真、假分支的最小间隔，缺省取 tolerance / minimum true-false branch gap, defaulting to tolerance
 * @property delta 离散步长，缺省取 [strictBoundary] / discrete step, defaulting to [strictBoundary]
 * @param antecedentBounds 前件多项式的显式有限范围 / explicit finite bounds of the antecedent polynomial
 * @param consequentBounds 后件多项式的显式有限范围 / explicit finite bounds of the consequent polynomial
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class ImplyFunction<V>(
    val antecedent: LinearPolynomial<V>,
    val consequent: LinearPolynomial<V>,
    converter: IntoValue<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "imply",
    override var displayName: String? = null,
    val relation: Comparison = Comparison.GT,
    antecedentBounds: ConditionBounds<V>? = null,
    consequentBounds: ConditionBounds<V>? = null,
    delta: V? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val explicitBigM: V? = bigM
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    val strictBoundary: V = strictBoundary ?: this.tolerance
    private val requestedDelta: V? = delta
    val delta: V = requestedDelta ?: this.strictBoundary
    private val antecedentBounds: ConditionBounds<V>? = antecedentBounds
    private val consequentBounds: ConditionBounds<V>? = consequentBounds

    val antecedentIndicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_ant_nz")
    val consequentIndicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_con_nz")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(antecedentIndicatorVar, consequentIndicatorVar)

    /**
     * 对给定符号值执行蕴含的三值判定。 / Classify the implication using three-valued logic.
     *
     * 前件为假或未定义时后件可以短路；前件为真且后件缺失或非有限输入时返回失败，间隔区返回 [TruthValue.Undefined]。
     * The consequent is short-circuited when the antecedent is false or undefined; missing or non-finite input fails only when the antecedent is true, and a business-gap value returns [TruthValue.Undefined].
     *
     * @param values 符号到值的映射 / mapping from symbols to values
     * @return 蕴含的三值判定 / three-valued implication result
     */
    fun classify(values: Map<Symbol, V>): Ret<TruthValue> {
        return try {
            val antecedentValue = antecedent.evaluateWith(values)
                ?: return implyFailure(
                    "缺少前件输入值，无法判定蕴含。 / The antecedent input is missing, so the implication cannot be classified."
                )
            val antecedentClassification = when (val result = classifyDiscreteCondition(
                d = antecedentValue,
                relation = relation,
                delta = delta,
                strictBoundary = strictBoundary
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (antecedentClassification == TruthValue.False) {
                return Ok(TruthValue.True)
            }
            if (antecedentClassification == TruthValue.Undefined) {
                return Ok(TruthValue.Undefined)
            }

            val consequentValue = consequent.evaluateWith(values)
                ?: return implyFailure(
                    "缺少后件输入值，无法判定蕴含。 / The consequent input is missing, so the implication cannot be classified."
                )

            when (val result = classifyDiscreteCondition(
                d = consequentValue,
                relation = relation,
                delta = delta,
                strictBoundary = strictBoundary
            )) {
                is Ok -> Ok(result.value)
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        } catch (error: RuntimeException) {
            implyRuntimeFailure(
                operation = "判定蕴含函数 / Classify implication",
                error = error
            )
        }
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        return try {
            when (val classification = classify(values)) {
                is Ok -> when (classification.value) {
                    TruthValue.True -> converter.one
                    TruthValue.False -> converter.zero
                    TruthValue.Undefined -> null
                }
                is Failed, is Fatal -> null
            }
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun validatePolynomial(
        poly: LinearPolynomial<V>,
        label: String
    ): Try {
        if (!hasUsableConditionSolverValues(poly, converter)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "$label 的系数和常数必须是有限且可编码的 solver 值。 / The coefficients and constant of $label must be finite and representable at the solver boundary."
            )
        }
        return ok
    }

    /**
     * 复用条件函数的有限范围和哨兵校验。 / Reuse the conditional-function finite-range and sentinel validation.
     */
    private fun validateImplyBounds(
        bounds: ConditionBounds<V>,
        label: String,
        boundsRelation: Comparison = relation
    ): Try {
        return try {
            if (!isUsableConditionBound(bounds.lower, converter) ||
                !isUsableConditionBound(bounds.upper, converter) ||
                bounds.lower.compareTo(bounds.upper) > 0
            ) {
                Failed(
                    ErrorCode.IllegalArgument,
                    "$label 必须是有限且有序的 solver 范围，不能使用 Flt64.minimum/maximum 哨兵。 / $label must be finite and ordered for the solver; Flt64.minimum/maximum sentinels are not allowed."
                )
            } else {
                when (val validation = validateConditionBounds(
                    bounds = bounds,
                    relation = boundsRelation,
                    strictBoundary = strictBoundary
                )) {
                    is Ok -> ok
                    is Failed -> Failed(validation.error)
                    is Fatal -> Fatal(validation.errors)
                }
            }
        } catch (_: RuntimeException) {
            Failed(
                ErrorCode.IllegalArgument,
                "$label 校验失败。 / Failed to validate $label."
            )
        }
    }

    private fun normalizeImplyCondition(
        poly: LinearPolynomial<V>,
        bounds: ConditionBounds<V>,
        label: String
    ): Ret<DiscreteConditionLinearization<V>> {
        val normalized = when (val result = normalizeDiscreteCondition(
            poly = poly,
            relation = relation,
            bounds = bounds,
            delta = delta,
            strictBoundary = strictBoundary
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        when (val validation = validatePolynomial(
            poly = normalized.polynomial,
            label = "$label 归一化多项式 / $label normalized polynomial"
        )) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        when (val validation = validateImplyBounds(
            bounds = normalized.bounds,
            label = "$label 归一化范围 / $label normalized bounds",
            boundsRelation = Comparison.GT
        )) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return Ok(normalized)
    }

    private fun resolveBounds(
        poly: LinearPolynomial<V>,
        explicitBounds: ConditionBounds<V>?,
        label: String
    ): Ret<ConditionBounds<V>> {
        return try {
            val resolved = if (explicitBounds != null) {
                // Explicit bounds are authoritative; do not run an unrelated inference
                // path that could hide an arithmetic failure. /
                // 显式范围是权威契约；不能再运行会吞掉算术异常的无关推导路径。
                explicitBounds
            } else {
                val inferredBounds = try {
                    poly.finiteBounds(converter)
                } catch (error: RuntimeException) {
                    return implyRuntimeFailure(
                        operation = "推导 $label / Infer $label",
                        error = error
                    )
                }
                inferredBounds
                    ?.takeIf { isUsableInferredBounds(poly, it) }
                    ?.let {
                        ConditionBounds(
                            lower = it.lower,
                            upper = it.upper
                        )
                    }
                    ?: return implyFailure(
                        "$label 缺失有限范围：请提供 ConditionBounds；单独的 bigM 不能替代范围。 / $label has no finite bounds: provide ConditionBounds; bigM alone cannot replace the range."
                    )
            }

            when (val validation = validateImplyBounds(resolved, label)) {
                is Ok -> {}
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }

            when (val validation = normalizeImplyCondition(
                poly = poly,
                bounds = resolved,
                label = label
            )) {
                is Ok -> Ok(resolved)
                is Failed -> Failed(validation.error)
                is Fatal -> Fatal(validation.errors)
            }
        } catch (error: RuntimeException) {
            implyRuntimeFailure(
                operation = "解析 $label / Resolve $label",
                error = error
            )
        }
    }

    /**
     * 校验自动推导范围没有使用哨兵或溢出结果。 / Validate that inferred bounds do not use sentinels or overflow results.
     */
    private fun isUsableInferredBounds(
        poly: LinearPolynomial<V>,
        inferred: LinearPolynomialBounds<V>
    ): Boolean {
        if (!hasFiniteInferredFlt64Arithmetic(poly) ||
            !isUsableConditionBound(inferred.lower, converter) ||
            !isUsableConditionBound(inferred.upper, converter) ||
            inferred.lower.compareTo(inferred.upper) > 0
        ) {
            return false
        }

        return true
    }

    /**
     * 重放 finiteBounds 的 Flt64 运算并逐步检查结果。 / Replay finiteBounds arithmetic in Flt64 and check every result.
     */
    private fun hasFiniteInferredFlt64Arithmetic(poly: LinearPolynomial<V>): Boolean {
        var lower = try {
            converter.fromValue(poly.constant)
        } catch (_: RuntimeException) {
            return false
        }
        var upper = lower
        if (!isUsableConditionBound(lower, IntoValue.Identity)) {
            return false
        }

        for (monomial in poly.monomials) {
            val coefficient = try {
                converter.fromValue(monomial.coefficient)
            } catch (_: RuntimeException) {
                return false
            }
            if (!isUsableConditionBound(coefficient, IntoValue.Identity)) {
                return false
            }
            if (coefficient.compareTo(Flt64.zero) == 0) {
                continue
            }

            val (symbolLower, symbolUpper) = monomial.symbol.usableImplyFlt64Bounds() ?: return false
            val lowerTerm = if (coefficient.compareTo(Flt64.zero) > 0) {
                coefficient * symbolLower
            } else {
                coefficient * symbolUpper
            }
            val upperTerm = if (coefficient.compareTo(Flt64.zero) > 0) {
                coefficient * symbolUpper
            } else {
                coefficient * symbolLower
            }
            if (!isUsableConditionBound(lowerTerm, IntoValue.Identity) ||
                !isUsableConditionBound(upperTerm, IntoValue.Identity)
            ) {
                return false
            }

            lower += lowerTerm
            upper += upperTerm
            if (!isUsableConditionBound(lower, IntoValue.Identity) ||
                !isUsableConditionBound(upper, IntoValue.Identity)
            ) {
                return false
            }
        }
        return true
    }

    private fun precheck(): Ret<ResolvedImplyBounds<V>> {
        return try {
            val zero = converter.zero
            val one = converter.one
            if (!isUsableConditionBound(zero, converter) ||
                !isUsableConditionBound(one, converter) ||
                !isUsableConditionBound(strictBoundary, converter) ||
                !isUsableConditionBound(delta, converter)
            ) {
                return implyFailure(
                    "strictBoundary、delta、零值和单位值必须是有限且可编码的 solver 值。 / strictBoundary, delta, zero, and one must be finite solver-representable values."
                )
            }

            explicitBigM?.let { value ->
                if (!isUsableConditionBound(value, converter)) {
                    return implyFailure(
                        "bigM 必须为有限值，且不能替代条件范围。 / bigM must be finite and cannot replace the condition ranges."
                    )
                }
                if (value.compareTo(zero) <= 0) {
                    return implyFailure(
                        "bigM 必须大于 0，且不能替代条件范围。 / bigM must be greater than 0 and cannot replace the condition ranges."
                    )
                }
            }

            when (val validation = validatePolynomial(antecedent, "前件 / antecedent")) {
                is Ok -> {}
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
            // Validate the consequent even when the antecedent is constant-false; invalid model data must never be accepted by short-circuiting. /
            // 即使前件恒假也要校验后件；不能通过短路接受非法模型数据。
            when (val validation = validatePolynomial(consequent, "后件 / consequent")) {
                is Ok -> {}
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
            // Resolve the consequent before any false-antecedent short circuit. /
            // 在前件恒假短路前也必须解析后件范围。
            val resolvedConsequentBounds = when (val result = resolveBounds(
                poly = consequent,
                explicitBounds = consequentBounds,
                label = "后件范围 / Consequent bounds"
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            val resolvedAntecedentBounds = when (val result = resolveBounds(
                poly = antecedent,
                explicitBounds = antecedentBounds,
                label = "前件范围 / Antecedent bounds"
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            val antecedentCondition = when (val result = normalizeImplyCondition(
                poly = antecedent,
                bounds = resolvedAntecedentBounds,
                label = "前件范围 / Antecedent bounds"
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            if (!antecedentCondition.trueBranchPossible) {
                return Ok(
                    ResolvedImplyBounds(
                        antecedent = resolvedAntecedentBounds,
                        consequent = resolvedConsequentBounds
                    )
                )
            }

            Ok(
                ResolvedImplyBounds(
                    antecedent = resolvedAntecedentBounds,
                    consequent = resolvedConsequentBounds
                )
            )
        } catch (error: RuntimeException) {
            implyRuntimeFailure(
                operation = "预检蕴含函数 / Precheck implication function",
                error = error
            )
        }
    }

    private fun relationAdjustments(
        condition: DiscreteConditionLinearization<V>
    ): Ret<RelationAdjustments<V>> {
        val zero = converter.zero
        if (!isUsableConditionBound(zero, converter) ||
            !isUsableConditionBound(strictBoundary, converter) ||
            !isUsableConditionBound(condition.bounds.lower, converter) ||
            !isUsableConditionBound(condition.bounds.upper, converter) ||
            !hasUsableConditionSolverValues(condition.polynomial, converter)
        ) {
            return implyFailure(
                "后件门控范围计算产生非有限值。 / Consequent gating range calculation produced a non-finite value."
            )
        }

        val lower = when (val result = checkedImplyArithmetic(
            label = "后件门控下界松弛量 / consequent lower gating relaxation",
            converter = converter
        ) { strictBoundary - condition.bounds.lower }) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val upper = when (val result = checkedImplyArithmetic(
            label = "后件门控上界松弛量 / consequent upper gating relaxation",
            converter = converter
        ) { condition.bounds.upper - zero }) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return Ok(RelationAdjustments(lower = lower, upper = upper))
    }

    private fun nonNegative(value: V): Ret<V> {
        val zero = converter.zero
        if (!isUsableConditionBound(value, converter) ||
            !isUsableConditionBound(zero, converter)
        ) {
            return implyFailure(
                "门控松弛量必须为有限值。 / Gating relaxation must be finite."
            )
        }
        return try {
            val result = if (value.compareTo(zero) > 0) value else zero
            if (isUsableConditionBound(result, converter)) {
                Ok(result)
            } else {
                implyFailure(
                    "非负门控松弛量产生非有限值。 / Non-negative gating relaxation produced a non-finite value."
                )
            }
        } catch (_: RuntimeException) {
            implyFailure(
                "门控松弛量比较失败。 / Failed to compare gating relaxation."
            )
        }
    }

    private fun gateConstraint(
        constraint: LinearInequality<V>,
        relaxation: V,
        constraintName: String
    ): Ret<LinearInequality<V>> {
        if (!constraint.hasUsableImplySolverValues(converter) ||
            !isUsableConditionBound(relaxation, converter)
        ) {
            return implyFailure(
                "门控约束或松弛量包含非有限值。 / The gated constraint or relaxation contains a non-finite value."
            )
        }

        val indicatorCoefficient: V
        val gatedRhsConstant: V
        when (constraint.comparison) {
            Comparison.GE -> {
                indicatorCoefficient = when (val result = checkedImplyArithmetic(
                    label = "门控约束指示系数 / gated-constraint indicator coefficient",
                    converter = converter
                ) { -relaxation }) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                gatedRhsConstant = when (val result = checkedImplyArithmetic(
                    label = "门控约束右端减法 / gated-constraint rhs subtraction",
                    converter = converter
                ) { constraint.rhs.constant - relaxation }) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            Comparison.LE -> {
                indicatorCoefficient = relaxation
                gatedRhsConstant = when (val result = checkedImplyArithmetic(
                    label = "门控约束右端加法 / gated-constraint rhs addition",
                    converter = converter
                ) { constraint.rhs.constant + relaxation }) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            else -> return implyFailure(
                "仅支持 GE/LE 关系约束门控，实际为 ${constraint.comparison.symbol}。 / Only GE/LE relation constraints can be gated, but ${constraint.comparison.symbol} was received."
            )
        }
        if (!isUsableConditionBound(indicatorCoefficient, converter) ||
            !isUsableConditionBound(gatedRhsConstant, converter)
        ) {
            return implyFailure(
                "门控约束计算产生非有限系数或常数。 / Gating the constraint produced a non-finite coefficient or constant."
            )
        }

        val gatedConstraint = LinearInequality(
            lhs = LinearPolynomial(
                monomials = constraint.lhs.monomials + LinearMonomial(
                    coefficient = indicatorCoefficient,
                    symbol = antecedentIndicatorVar
                ),
                constant = constraint.lhs.constant
            ),
            rhs = LinearPolynomial(
                monomials = constraint.rhs.monomials,
                constant = gatedRhsConstant
            ),
            comparison = constraint.comparison,
            name = constraintName,
            displayName = constraint.displayName
        )
        if (!gatedConstraint.hasUsableImplySolverValues(converter)) {
            return implyFailure(
                "生成的门控约束包含非有限系数或常数。 / The generated gated constraint contains a non-finite coefficient or constant."
            )
        }
        return Ok(gatedConstraint)
    }

    private fun buildConstraints(
        bounds: ResolvedImplyBounds<V>
    ): Ret<List<LinearInequality<V>>> {
        val antecedentCondition = when (val result = normalizeImplyCondition(
            poly = antecedent,
            bounds = bounds.antecedent,
            label = "前件范围 / Antecedent bounds"
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val zero = converter.zero
        val one = converter.one
        if (!isUsableConditionBound(zero, converter) ||
            !isUsableConditionBound(one, converter)
        ) {
            return implyFailure(
                "指示变量的零和单位值必须为有限值。 / Indicator zero and one values must be finite."
            )
        }
        val allConstraints = mutableListOf<LinearInequality<V>>()

        fun finish(): Ret<List<LinearInequality<V>>> {
            return if (allConstraints.all {
                it.hasUsableImplySolverValues(converter) &&
                    it.hasUsableImplyFlattenedValues(converter)
            }) {
                Ok(allConstraints)
            } else {
                implyFailure(
                    "生成的蕴含约束包含非有限系数或常数。 / Generated implication constraints contain a non-finite coefficient or constant."
                )
            }
        }

        fun addIndicator(
            indicator: AbstractVariableItem<*, *>,
            value: TruthValue,
            suffix: String
        ) {
            allConstraints += fixedVariableEquality(
                variable = indicator,
                value = if (value == TruthValue.True) one else zero,
                zero = zero,
                one = one,
                name = "${name}_${suffix}"
            )
        }

        fun addRelation(
            condition: DiscreteConditionLinearization<V>,
            indicator: AbstractVariableItem<*, *>,
            prefix: String
        ): Try {
            return when (val result = relationIndicatorConstraints(
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

        val consequentCondition = when (val result = normalizeImplyCondition(
            poly = consequent,
            bounds = bounds.consequent,
            label = "后件范围 / Consequent bounds"
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        when (val fixedValue = consequentCondition.fixedValue) {
            TruthValue.True -> addIndicator(
                indicator = consequentIndicatorVar,
                value = TruthValue.True,
                suffix = "con_fold_true"
            )
            TruthValue.False -> addIndicator(
                indicator = consequentIndicatorVar,
                value = TruthValue.False,
                suffix = "con_fold_false"
            )
            TruthValue.Undefined -> {}
            null -> {}
        }

        if (antecedentCondition.fixedValue == TruthValue.False) {
            // A false antecedent makes the implication true; skip the irrelevant consequent relation. /
            // 前件恒假时蕴含恒真，跳过无关的后件关系约束。
            addIndicator(
                indicator = antecedentIndicatorVar,
                value = TruthValue.False,
                suffix = "ant_fold_false"
            )
            if (consequentCondition.fixedValue == null) {
                addIndicator(
                    indicator = consequentIndicatorVar,
                    value = TruthValue.False,
                    suffix = "con_fold_irrelevant"
                )
            }
            return finish()
        }

        when (val result = antecedentCondition.fixedValue) {
            TruthValue.True -> addIndicator(
                indicator = antecedentIndicatorVar,
                value = TruthValue.True,
                suffix = "ant_fold_true"
            )
            TruthValue.False -> addIndicator(
                indicator = antecedentIndicatorVar,
                value = TruthValue.False,
                suffix = "ant_fold_false"
            )
            TruthValue.Undefined -> when (val relationResult = addRelation(
                condition = antecedentCondition,
                indicator = antecedentIndicatorVar,
                prefix = "${name}_ant"
            )) {
                is Ok -> {}
                is Failed -> return Failed(relationResult.error)
                is Fatal -> return Fatal(relationResult.errors)
            }
            null -> when (val relationResult = addRelation(
                condition = antecedentCondition,
                indicator = antecedentIndicatorVar,
                prefix = "${name}_ant"
            )) {
                is Ok -> {}
                is Failed -> return Failed(relationResult.error)
                is Fatal -> return Fatal(relationResult.errors)
            }
        }

        val implicationLink = LinearInequality(
            lhs = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(one, antecedentIndicatorVar),
                    LinearMonomial(
                        coefficient = when (val result = checkedImplyArithmetic(
                            label = "蕴含链接指示系数 / implication-link indicator coefficient",
                            converter = converter
                        ) { -one }) {
                            is Ok -> result.value
                            is Failed -> return Failed(result.error)
                            is Fatal -> return Fatal(result.errors)
                        },
                        symbol = consequentIndicatorVar
                    )
                ),
                constant = zero
            ),
            rhs = LinearPolynomial(emptyList(), zero),
            comparison = Comparison.LE,
            name = "${name}_imply_link"
        )

        if (consequentCondition.fixedValue == null) {
            val consequentConstraints = when (val result = relationIndicatorConstraints(
                poly = consequentCondition.polynomial,
                indicator = consequentIndicatorVar,
                relation = Comparison.GT,
                bounds = consequentCondition.bounds,
                strictBoundary = strictBoundary,
                namePrefix = "${name}_con"
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (consequentConstraints.size < 2) {
                return implyFailure(
                    "后件关系约束数量不足，无法完成门控。 / Consequent relation constraints are incomplete, so gating cannot be built."
                )
            }

            if (antecedentCondition.fixedValue == TruthValue.True) {
                // A true antecedent activates the consequent relation directly. /
                // 前件恒真时直接启用后件关系约束。
                allConstraints += consequentConstraints
            } else {
                val adjustments = when (val result = relationAdjustments(consequentCondition)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                val lowerRelaxation = when (val result = nonNegative(adjustments.lower)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                val upperRelaxation = when (val result = nonNegative(adjustments.upper)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                val gatedLower = when (val result = gateConstraint(
                    constraint = consequentConstraints[0],
                    relaxation = lowerRelaxation,
                    constraintName = "${name}_con_gated_lower"
                )) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                val gatedUpper = when (val result = gateConstraint(
                    constraint = consequentConstraints[1],
                    relaxation = upperRelaxation,
                    constraintName = "${name}_con_gated_upper"
                )) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                allConstraints += gatedLower
                allConstraints += gatedUpper
            }
        }

        val needsLink = when {
            antecedentCondition.fixedValue == TruthValue.True &&
                consequentCondition.fixedValue == TruthValue.True -> false
            antecedentCondition.fixedValue == null &&
                consequentCondition.fixedValue == TruthValue.True -> false
            else -> true
        }
        if (needsLink) {
            allConstraints += implicationLink
        }
        return finish()
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return try {
            val bounds = when (val validation = precheck()) {
                is Ok -> validation.value
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
            // Build constraints before adding tokens so a non-finite gate leaves no partial state. /
            // 先构造约束再添加 token，避免非有限门控留下半注册状态。
            when (val result = buildConstraints(bounds)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            val tokenSnapshot = snapshotImplyTokens(tokens)
            if (tokenSnapshot.tokenList != null && tokenSnapshot.state == null) {
                return implyFailure(
                    "可变 token 列表不支持事务快照，无法保证蕴含函数注册原子性。 / The mutable token list does not support transactional snapshots, so implication registration cannot be made atomic."
                )
            }
            val registration = try {
                tokens.add(helperVariables)
            } catch (error: RuntimeException) {
                implyRuntimeFailure(
                    operation = "注册蕴含函数辅助令牌 / Register implication auxiliary tokens",
                    error = error
                )
            }
            when (registration) {
                is Ok -> ok
                is Failed -> implyRollbackFailure(
                    Failed(registration.error),
                    restoreImplyTokens(tokenSnapshot)
                )
                is Fatal -> implyRollbackFailure(
                    Fatal(registration.errors),
                    restoreImplyTokens(tokenSnapshot)
                )
            }
        } catch (error: RuntimeException) {
            implyRuntimeFailure(
                operation = "注册蕴含函数辅助令牌 / Register implication auxiliary tokens",
                error = error
            )
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        return try {
            val bounds = when (val validation = precheck()) {
                is Ok -> validation.value
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
            val constraints = when (val result = buildConstraints(bounds)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            addConstraints(model, constraints)?.let { return it }
            ok
        } catch (error: RuntimeException) {
            implyRuntimeFailure(
                operation = "注册蕴含函数约束 / Register implication constraints",
                error = error
            )
        }
    }

    companion object {
        /**
         * 创建蕴含函数实例 / Create an implication function instance.
         *
         * @param antecedent 前件线性多项式 / antecedent linear polynomial
         * @param consequent 后件线性多项式 / consequent linear polynomial
         * @param converter 值类型转换器 / value type converter
         * @param bigM 旧版 Big-M 参数，仅用于源码兼容 / legacy Big-M parameter kept for source compatibility
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @param relation 前后件使用的比较关系 / relation used by both operands
         * @param tolerance 兼容旧调用的容差 / legacy tolerance
         * @param strictBoundary 真、假分支之间的业务间隔 / business gap between true and false branches
         * @param delta 离散步长 / discrete step
         * @param antecedentBounds 前件多项式的显式有限范围 / explicit finite bounds of the antecedent polynomial
         * @param consequentBounds 后件多项式的显式有限范围 / explicit finite bounds of the consequent polynomial
         * @return [ImplyFunction] 实例 / [ImplyFunction] instance
         */
        operator fun <V> invoke(
            antecedent: LinearPolynomial<V>,
            consequent: LinearPolynomial<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null,
            relation: Comparison = Comparison.GT,
            tolerance: V? = null,
            strictBoundary: V? = null,
            antecedentBounds: ConditionBounds<V>? = null,
            consequentBounds: ConditionBounds<V>? = null,
            delta: V? = null
        ): ImplyFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            ImplyFunction(
                antecedent = antecedent,
                consequent = consequent,
                converter = converter,
                bigM = bigM,
                tolerance = tolerance,
                strictBoundary = strictBoundary,
                name = name,
                displayName = displayName,
                relation = relation,
                antecedentBounds = antecedentBounds,
                consequentBounds = consequentBounds,
                delta = delta
            )
    }
}
