@file:Suppress("unused")

/** 单变量线性分段函数符号 / Univariate linear piecewise function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AbstractMutableTokenList
import fuookami.ospf.kotlin.core.token.AbstractMutableTokenTable
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.token.Token
import fuookami.ospf.kotlin.core.token.TokenListSnapshot
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

private data class PiecewiseResultRangeSnapshot(
    val range: ValueRange<Flt64>?,
    val set: Boolean
)

private data class PiecewiseTokenSnapshot<V>(
    val tokens: Map<VariableItemKey, Token<V>>?,
    val tokenList: AbstractMutableTokenList<V>?,
    val state: TokenListSnapshot<V>?
) where V : RealNumber<V>, V : NumberField<V>

/**
 * 单变量线性分段函数符号 / Univariate linear piecewise function symbol
 *
 * 提供 [UnivariateLinearPiecewiseFunction]，使用断点和斜率定义分段线性函数。
 *
 * Provides [UnivariateLinearPiecewiseFunction] for defining piecewise linear functions using breakpoints and slopes.
*/

/**
 * 单变量线性分段函数：由断点和斜率定义的 y = f(x)。 / Univariate linear piecewise function: y = f(x) defined by breakpoints and slopes.
 *
 * 使用二值选择变量选择激活的线段。 / Uses binary selector variables to choose the active segment.
 *
 * @property x 输入线性多项式 / the input linear polynomial
 * @property breakpoints 断点值列表 / list of breakpoint values
 * @property slopes 各段斜率 / slope of each segment
 * @property intercepts 各段截距 / intercept of each segment
 * @param m Big-M 常量（未提供时必须能从输入范围证明有限性）/ Big-M constant (when omitted, a finite input range must be provable)
 * @property converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
*/
class UnivariateLinearPiecewiseFunction<V>(
    val x: LinearPolynomial<V>,
    val breakpoints: List<V>,
    val slopes: List<V>,
    val intercepts: List<V>,
    m: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "piecewise",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultVariable, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val explicitM: V? = m

    private val numSegments = try {
        val breakpointCount = breakpoints.size
        if (breakpointCount >= 2) breakpointCount - 1 else 0
    } catch (_: RuntimeException) {
        // 延迟到 Result 边界报告非法断点列表。 / Report an invalid breakpoint list at the Result boundary.
        0
    }
    /** Validated output bounds, shared by range exposure and constraint registration. / 由值域暴露和约束注册共享的已校验输出范围。 */
    private val outputBoundsResult: Ret<ConditionBounds<V>> by lazy { calculateOutputBounds() }

    /** Flt64 solver range; failure means that no finite solver range can be proven. / Flt64 求解器范围；失败表示无法证明有限求解器范围。 */
    private val outputRangeResult: Ret<ValueRange<Flt64>> by lazy { calculateOutputRange() }

    val selectorVars: List<AbstractVariableItem<*, *>> = (0 until numSegments).map { BinVar("${name}_s${it}") }
    override val resultVar: RealVar = RealVar("${name}_y")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + selectorVars

    val result: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)
    }
    override val resultPolynomial: LinearPolynomial<V> get() = result

    private fun <T> piecewiseFailure(message: String): Ret<T> {
        return Failed(ErrorCode.IllegalArgument, message)
    }

    /**
     * 将边界上的运行时异常转换为失败结果。 / Convert boundary runtime exceptions into failure results.
     *
     * 转换器和数值实现属于可扩展边界，不能让异常穿过 Ret/Try API。
     * Converters and numeric implementations are extension boundaries; their exceptions must not escape Ret/Try APIs.
     */
    private fun <T> piecewiseRuntimeFailure(operation: String, error: RuntimeException): Ret<T> {
        val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName ?: "unknown runtime error"
        return piecewiseFailure(
            "$operation 失败：$detail / $operation failed: $detail"
        )
    }

    /** 组合主操作与回滚失败。 / Combine a primary operation failure with a rollback failure. */
    private fun piecewiseRollbackFailure(failure: Try, rollback: Try): Try {
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

    /** 读取结果范围快照。 / Read an exact result-range snapshot. */
    private fun snapshotResultRange(): PiecewiseResultRangeSnapshot {
        return PiecewiseResultRangeSnapshot(
            range = resultVar.range.range,
            set = resultVar.range.set
        )
    }

    /** 恢复结果范围快照。 / Restore an exact result-range snapshot. */
    private fun restoreResultRange(snapshot: PiecewiseResultRangeSnapshot): Try {
        return try {
            if (resultVar.range.range != snapshot.range || resultVar.range.set != snapshot.set) {
                resultVar.range.restore(snapshot.range, snapshot.set)
            }
            if (resultVar.range.range != snapshot.range || resultVar.range.set != snapshot.set) {
                Failed(
                    ErrorCode.ApplicationError,
                    "分段线性函数结果范围恢复后仍不一致。 / The piecewise result range remained inconsistent after restoration."
                )
            } else {
                ok
            }
        } catch (error: RuntimeException) {
            piecewiseRuntimeFailure(
                operation = "恢复分段线性函数结果范围 / Restore piecewise result range",
                error = error
            )
        }
    }

    /** 读取可变 token 列表。 / Read the mutable token list behind a token collection. */
    @Suppress("UNCHECKED_CAST")
    private fun piecewiseTokenList(tokens: Any): AbstractMutableTokenList<V>? {
        return when (tokens) {
            is AbstractMutableTokenList<*> -> tokens as AbstractMutableTokenList<V>
            is AbstractTokenTable<*> -> tokens.tokenList as? AbstractMutableTokenList<V>
            else -> null
        }
    }

    /** 读取可变 token 集合快照。 / Read a snapshot for a mutable token collection. */
    private fun snapshotPiecewiseTokens(tokens: Any): PiecewiseTokenSnapshot<V> {
        val tokenList = piecewiseTokenList(tokens)
        return PiecewiseTokenSnapshot(
            tokens = piecewiseTokenEntries(tokens)?.associateBy { it.key },
            tokenList = tokenList,
            state = tokenList?.snapshotState()
        )
    }

    /** 拒绝无法精确恢复的可变 token 列表。 / Reject mutable token lists that cannot be restored exactly. */
    private fun piecewiseTokenSnapshotFailure(snapshot: PiecewiseTokenSnapshot<V>): Try? {
        return if (snapshot.tokenList != null && snapshot.state == null) {
            Failed(
                ErrorCode.ApplicationError,
                "分段线性函数 token 列表不支持事务快照。 / The piecewise token list does not support transactional snapshots."
            )
        } else {
            null
        }
    }

    /** 读取当前可变 token 集合。 / Read the current mutable token collection. */
    private fun currentPiecewiseTokens(tokens: Any): Map<VariableItemKey, Token<V>>? {
        return piecewiseTokenEntries(tokens)?.associateBy { it.key }
    }

    /**
     * 统一读取 token 表及其列表包装层，避免在模型持有 AbstractTokenTable 时丢失可变列表。
     * Read token tables and their list wrappers uniformly so a mutable list is not lost behind
     * an AbstractTokenTable reference.
     */
    @Suppress("UNCHECKED_CAST")
    private fun piecewiseTokenEntries(tokens: Any): Collection<Token<V>>? {
        return when (tokens) {
            is AbstractMutableTokenTable<*> -> tokens.tokens as Collection<Token<V>>
            is AbstractMutableTokenList<*> -> tokens.tokens as Collection<Token<V>>
            is AbstractTokenTable<*> -> tokens.tokens as Collection<Token<V>>
            else -> null
        }
    }

    /** 移除 token；返回 false 表示集合不支持回滚。 / Remove a token, or report unsupported rollback. */
    private fun removePiecewiseToken(tokens: Any, variable: AbstractVariableItem<*, *>): Boolean {
        return when (tokens) {
            is AbstractMutableTokenTable<*> -> {
                tokens.remove(variable)
                true
            }
            is AbstractMutableTokenList<*> -> {
                tokens.remove(variable)
                true
            }
            is AbstractTokenTable<*> -> {
                when (val tokenList = tokens.tokenList) {
                    is AbstractMutableTokenList<*> -> {
                        tokenList.remove(variable)
                        true
                    }
                    else -> false
                }
            }
            else -> false
        }
    }

    /** 回滚快照之后新增或替换的 token。 / Roll back tokens added or replaced after the snapshot. */
    private fun rollbackPiecewiseTokens(
        tokens: Any,
        snapshot: PiecewiseTokenSnapshot<V>
    ): Try {
        if (snapshot.tokenList != null && snapshot.state != null) {
            return try {
                snapshot.tokenList.restoreState(snapshot.state)
            } catch (error: RuntimeException) {
                piecewiseRuntimeFailure(
                    operation = "恢复分段线性函数 token 状态 / Restore piecewise token state",
                    error = error
                )
            }
        }

        val originalTokens = snapshot.tokens ?: return ok
        return try {
            val currentTokens = currentPiecewiseTokens(tokens) ?: return Failed(
                ErrorCode.ApplicationError,
                "分段线性函数无法读取当前 token 集合。 / The current piecewise token collection cannot be read."
            )
            for (token in currentTokens.values) {
                if (token.key !in originalTokens) {
                    if (!removePiecewiseToken(tokens, token.variable)) {
                        return Failed(
                            ErrorCode.ApplicationError,
                            "分段线性函数 token 集合不支持回滚。 / The piecewise token collection does not support rollback."
                        )
                    }
                }
            }

            val restoredTokens = currentPiecewiseTokens(tokens) ?: return Failed(
                ErrorCode.ApplicationError,
                "分段线性函数无法验证 token 回滚结果。 / The piecewise token rollback cannot be verified."
            )
            val unremoved = restoredTokens.keys.filter { it !in originalTokens }
            val replaced = originalTokens.keys.filter {
                restoredTokens[it] !== originalTokens[it]
            }
            if (unremoved.isNotEmpty() || replaced.isNotEmpty()) {
                Failed(
                    ErrorCode.ApplicationError,
                    "分段线性函数 token 回滚不完整。 / Piecewise token rollback was incomplete."
                )
            } else {
                ok
            }
        } catch (error: RuntimeException) {
            piecewiseRuntimeFailure(
                operation = "回滚分段线性函数 token / Roll back piecewise tokens",
                error = error
            )
        }
    }

    /** 安全执行约束回滚。 / Execute constraint rollback safely. */
    private fun <V> rollbackPiecewiseConstraints(
        model: AbstractLinearMechanismModel<V>,
        size: Int
    ): Try where V : RealNumber<V>, V : NumberField<V> {
        return try {
            model.rollbackConstraintsTo(size)
        } catch (error: RuntimeException) {
            piecewiseRuntimeFailure(
                operation = "回滚分段线性函数约束 / Roll back piecewise constraints",
                error = error
            )
        }
    }

    private fun rollbackPiecewiseRegistration(
        failure: Try,
        tokens: Any,
        tokenSnapshot: PiecewiseTokenSnapshot<V>,
        rangeSnapshot: PiecewiseResultRangeSnapshot,
        constraintRollback: Try = ok
    ): Try {
        val tokenRollback = rollbackPiecewiseTokens(tokens, tokenSnapshot)
        return piecewiseRollbackFailure(
            piecewiseRollbackFailure(
                piecewiseRollbackFailure(failure, constraintRollback),
                tokenRollback
            ),
            restoreResultRange(rangeSnapshot)
        )
    }

    /**
     * 判断值是否可安全写入 solver。 / Check that a value can be safely written to the solver.
     *
     * Flt64 的最大最小值在变量范围中表示全范围哨兵，不能作为有限模型系数。
     * Flt64 minimum and maximum are full-range sentinels in variable ranges and are not finite model coefficients.
     */
    private fun isUsableSolverFlt64(value: Flt64): Boolean {
        return try {
            value.isFinite() &&
                value != Flt64.nan &&
                value.compareTo(Flt64.minimum) > 0 &&
                value.compareTo(Flt64.maximum) < 0
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun isUsableSolverValue(value: V): Boolean {
        return try {
            if (!value.isFinite() || value.constants.nan?.let { value.compareTo(it) == 0 } == true) {
                return false
            }
            val solverValue = converter.fromValue(value)
            isUsableSolverFlt64(solverValue)
        } catch (_: RuntimeException) {
            false
        }
    }

    /** 判断多项式中的所有数值是否可写入 solver。 / Check that every polynomial value is solver-safe. */
    private fun isUsablePolynomial(polynomial: LinearPolynomial<V>): Boolean {
        return isUsableSolverValue(polynomial.constant) &&
            polynomial.monomials.all { isUsableSolverValue(it.coefficient) }
    }

    /** 判断线性约束中的所有数值是否可写入 solver。 / Check that every constraint value is solver-safe. */
    private fun isUsableConstraint(constraint: LinearInequality<V>): Boolean {
        return isUsablePolynomial(constraint.lhs) && isUsablePolynomial(constraint.rhs)
    }

    /**
     * 判断范围是否有限、有效且不是 solver 哨兵。 / Check that bounds are finite, valid, and not solver sentinels.
     *
     * Continuous 的默认范围使用 +/- (1 / decimalPrecision) 表示自由变量；
     * Continuous's default range uses +/- (1 / decimalPrecision) for free variables;
     * 该范围虽然在 Double 中有限，但不能作为 Big-M 的证明范围。
     * although finite in Double, that range cannot prove a Big-M bound.
     */
    private fun isUsableBounds(bounds: LinearPolynomialBounds<V>): Boolean {
        return try {
            if (!isUsableSolverValue(bounds.lower) ||
                !isUsableSolverValue(bounds.upper) ||
                bounds.lower.compareTo(bounds.upper) > 0
            ) {
                return false
            }
            val freeBound = Flt64.decimalPrecision.reciprocal()
            val lower = converter.fromValue(bounds.lower)
            val upper = converter.fromValue(bounds.upper)
            isUsableSolverFlt64(lower) &&
                isUsableSolverFlt64(upper) &&
                lower.compareTo(-freeBound) > 0 &&
                upper.compareTo(freeBound) < 0
        } catch (_: RuntimeException) {
            false
        }
    }

    /** 校验并规范 Big-M；显式值还必须为正。 / Validate and normalize Big-M; explicit values must also be positive. */
    private fun resolveBigM(candidate: V, explicit: Boolean): Ret<V> {
        return try {
            if (!isUsableSolverValue(candidate) ||
                (explicit && candidate.compareTo(converter.zero) <= 0)
            ) {
                return piecewiseFailure(
                    "分段线性函数的 Big-M 必须为有限、可表示且为正值。 / Piecewise Big-M must be finite, representable, and positive."
                )
            }
            val normalized = ensurePositiveBigM(candidate, converter)
            if (!isUsableSolverValue(normalized) || normalized.compareTo(converter.zero) <= 0) {
                return piecewiseFailure(
                    "分段线性函数的 Big-M 规范化后不可用。 / The normalized piecewise Big-M is not usable."
                )
            }
            Ok(normalized)
        } catch (error: RuntimeException) {
            piecewiseRuntimeFailure(
                operation = "校验分段线性函数 Big-M / Validate piecewise Big-M",
                error = error
            )
        }
    }

    /**
     * 判断值是否有限且可表示为 solver 的 Flt64 边界。
     * Check that a value is finite and representable as a solver-side Flt64 bound.
     *
     * Flt64 的 minimum/maximum 是变量全范围哨兵，不能当作已证明的有限范围。
     * Flt64 minimum/maximum are full-range sentinels and must not be treated as
     * proven finite bounds.
     */
    private fun isUsablePiecewiseValue(value: V): Boolean {
        return isUsableSolverValue(value)
    }

    /**
     * 计算所有分段端点的函数值并取全局包络。
     * Evaluate every segment at both breakpoints and return the global envelope.
     *
     * 线性函数在闭区间上的极值必在端点取得；不连续的相邻分段也分别纳入计算。
     * A linear function reaches its extrema at interval endpoints; discontinuous
     * adjacent segments are included independently.
     */
    private fun calculateOutputBounds(): Ret<ConditionBounds<V>> {
        return try {
            if (breakpoints.size < 2 ||
                slopes.size != breakpoints.size - 1 ||
                intercepts.size != breakpoints.size - 1
            ) {
                return piecewiseFailure(
                    "分段线性函数的断点、斜率和截距数量不匹配。 / Piecewise breakpoints, slopes, and intercept counts are inconsistent."
                )
            }
            if (breakpoints.any { !isUsablePiecewiseValue(it) } ||
                slopes.any { !isUsablePiecewiseValue(it) } ||
                intercepts.any { !isUsablePiecewiseValue(it) }
            ) {
                return piecewiseFailure(
                    "分段线性函数的断点、斜率和截距必须为可表示的有限值。 / Piecewise breakpoints, slopes, and intercepts must be finite representable values."
                )
            }
            for (i in 0 until breakpoints.size - 1) {
                if (breakpoints[i].compareTo(breakpoints[i + 1]) >= 0) {
                    return piecewiseFailure(
                        "分段线性函数的断点必须严格递增。 / Piecewise breakpoints must be strictly increasing."
                    )
                }
            }

            val outputValues = (0 until numSegments).flatMap { i ->
                listOf(
                    slopes[i] * breakpoints[i] + intercepts[i],
                    slopes[i] * breakpoints[i + 1] + intercepts[i]
                )
            }
            if (outputValues.isEmpty() || outputValues.any { !isUsablePiecewiseValue(it) }) {
                return piecewiseFailure(
                    "分段线性函数的端点计算产生非有限或无界输出。 / Evaluating piecewise endpoints produced a non-finite or unbounded output."
                )
            }

            var lower = outputValues.first()
            var upper = outputValues.first()
            for (value in outputValues.drop(1)) {
                if (value.compareTo(lower) < 0) {
                    lower = value
                }
                if (value.compareTo(upper) > 0) {
                    upper = value
                }
            }
            Ok(ConditionBounds(lower = lower, upper = upper))
        } catch (error: RuntimeException) {
            piecewiseRuntimeFailure(
                operation = "计算分段线性函数输出范围 / Calculate piecewise output bounds",
                error = error
            )
        }
    }

    /**
     * 将有限输出范围转换为 RealVar 使用的 Flt64 值域。
     * Convert the finite output range to the Flt64 range used by RealVar.
     */
    internal fun resolveOutputRange(): Ret<ValueRange<Flt64>> {
        return outputRangeResult
    }

    /**
     * 暴露可用于 IfThen 的有限输出上下界。/ Expose finite output bounds usable by IfThen.
     *
     * 失败表示范围缺失、无界、参数非法或端点运算溢出；调用方应传播该 Result。
     * Failure means missing/unbounded range, invalid parameters, or endpoint overflow;
     * callers should propagate the Result.
     */
    fun resolveOutputBounds(): Ret<ConditionBounds<V>> {
        return outputBoundsResult
    }

    private fun calculateOutputRange(): Ret<ValueRange<Flt64>> {
        return try {
            val bounds = when (val result = outputBoundsResult) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val lower = converter.fromValue(bounds.lower)
            val upper = converter.fromValue(bounds.upper)
            if (!isUsableSolverFlt64(lower) || !isUsableSolverFlt64(upper) ||
                lower.compareTo(upper) > 0
            ) {
                return piecewiseFailure(
                    "分段线性函数无法转换为有限的 Flt64 输出范围。 / The piecewise function cannot be converted to a finite Flt64 output range."
                )
            }
            when (val result = ValueRange(
                lb = lower,
                ub = upper,
                lbInterval = Interval.Closed,
                ubInterval = Interval.Closed,
                constants = Flt64
            )) {
                is Ok -> result
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        } catch (error: RuntimeException) {
            piecewiseRuntimeFailure(
                operation = "转换分段线性函数输出范围 / Convert piecewise output range",
                error = error
            )
        }
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        return try {
            val xValue = x.evaluateWith(values) ?: return null
            for (i in 0 until numSegments) {
                val bpLow = breakpoints[i]
                val bpHigh = breakpoints[i + 1]
                val inLower = xValue gr bpLow || xValue eq bpLow
                val inUpper = xValue ls bpHigh || xValue eq bpHigh
                if (inLower && inUpper) {
                    return slopes[i] * xValue + intercepts[i]
                }
            }
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        val rangeSnapshot = try {
            snapshotResultRange()
        } catch (error: RuntimeException) {
            return piecewiseRuntimeFailure(
                operation = "读取分段线性函数结果范围快照 / Read piecewise result-range snapshot",
                error = error
            )
        }
        val tokenSnapshot = try {
            snapshotPiecewiseTokens(tokens)
        } catch (error: RuntimeException) {
            val failure: Try = piecewiseRuntimeFailure(
                operation = "读取分段线性函数 token 快照 / Read piecewise token snapshot",
                error = error
            )
            return piecewiseRollbackFailure(failure, restoreResultRange(rangeSnapshot))
        }
        piecewiseTokenSnapshotFailure(tokenSnapshot)?.let { failure ->
            return piecewiseRollbackFailure(failure, restoreResultRange(rangeSnapshot))
        }
        var tokenOperationStarted = false
        return try {
            val outputRange = when (val result = outputRangeResult) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            val tokenSnapshotKeys = tokenSnapshot.tokens?.keys
            val variablesToRegister = if (tokenSnapshotKeys == null) {
                helperVariables
            } else {
                // Existing variables are already registered. Skipping them avoids replacing
                // their solver tokens, which cannot be losslessly restored through the public
                // AddableTokenCollection contract if a later step fails.
                helperVariables.filter { it.key !in tokenSnapshotKeys }
            }
            tokenOperationStarted = variablesToRegister.isNotEmpty()
            val registration = try {
                if (variablesToRegister.isEmpty()) ok else tokens.add(variablesToRegister)
            } catch (error: RuntimeException) {
                piecewiseRuntimeFailure(
                    operation = "注册分段线性函数辅助令牌 / Register piecewise auxiliary tokens",
                    error = error
                )
            }
            when (registration) {
                is Ok -> {}
                is Failed -> return rollbackPiecewiseRegistration(
                    failure = Failed(registration.error),
                    tokens = tokens,
                    tokenSnapshot = tokenSnapshot,
                    rangeSnapshot = rangeSnapshot
                )
                is Fatal -> return rollbackPiecewiseRegistration(
                    failure = Fatal(registration.errors),
                    tokens = tokens,
                    tokenSnapshot = tokenSnapshot,
                    rangeSnapshot = rangeSnapshot
                )
            }

            try {
                // Commit the derived range only after all helper tokens are registered. /
                // 仅在所有辅助令牌注册成功后提交推导出的范围。
                resultVar.range.set(outputRange)
                ok
            } catch (error: RuntimeException) {
                rollbackPiecewiseRegistration(
                    failure = piecewiseRuntimeFailure(
                        operation = "提交分段线性函数结果范围 / Commit piecewise result range",
                        error = error
                    ),
                    tokens = tokens,
                    tokenSnapshot = tokenSnapshot,
                    rangeSnapshot = rangeSnapshot
                )
            }
        } catch (error: RuntimeException) {
            val failure: Try = piecewiseRuntimeFailure(
                operation = "注册分段线性函数辅助令牌 / Register piecewise auxiliary tokens",
                error = error
            )
            val tokenRollback = if (tokenOperationStarted) {
                rollbackPiecewiseTokens(tokens, tokenSnapshot)
            } else {
                ok
            }
            piecewiseRollbackFailure(
                piecewiseRollbackFailure(failure, tokenRollback),
                restoreResultRange(rangeSnapshot)
            )
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val rangeSnapshot = try {
            snapshotResultRange()
        } catch (error: RuntimeException) {
            return piecewiseRuntimeFailure(
                operation = "读取分段线性函数结果范围快照 / Read piecewise result-range snapshot",
                error = error
            )
        }
        val modelTokens: Any = try {
            model.tokens
        } catch (error: RuntimeException) {
            val failure: Try = piecewiseRuntimeFailure(
                operation = "读取分段线性函数模型 token 表 / Read piecewise model token table",
                error = error
            )
            return piecewiseRollbackFailure(failure, restoreResultRange(rangeSnapshot))
        }
        val tokenSnapshot = try {
            snapshotPiecewiseTokens(modelTokens)
        } catch (error: RuntimeException) {
            val failure: Try = piecewiseRuntimeFailure(
                operation = "读取分段线性函数 token 快照 / Read piecewise token snapshot",
                error = error
            )
            return piecewiseRollbackFailure(failure, restoreResultRange(rangeSnapshot))
        }
        piecewiseTokenSnapshotFailure(tokenSnapshot)?.let { failure ->
            return piecewiseRollbackFailure(failure, restoreResultRange(rangeSnapshot))
        }
        var constraintOperationStarted = false
        var originalConstraintCount: Int? = null
        return try {
            val outputBounds = when (val result = outputBoundsResult) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val outputRange = when (val result = outputRangeResult) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val zero = converter.zero
            val one = converter.one
            if (!isUsablePolynomial(x)) {
                return piecewiseFailure(
                    "分段线性函数的输入多项式包含非有限或不可表示值。 / The piecewise input polynomial contains a non-finite or unrepresentable value."
                )
            }
            val allConstraints = mutableListOf<LinearInequality<V>>()
            val explicitBigM = if (explicitM == null) {
                null
            } else {
                when (val result = resolveBigM(explicitM!!, explicit = true)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            val xBounds: LinearPolynomialBounds<V>?
            val fallbackM: V?
            if (explicitBigM == null) {
                xBounds = try {
                    x.finiteBounds(converter)
                } catch (error: RuntimeException) {
                    return piecewiseRuntimeFailure(
                        operation = "计算分段线性函数输入范围 / Calculate piecewise input bounds",
                        error = error
                    )
                }
                if (xBounds == null) {
                    return piecewiseFailure(
                        "分段线性函数缺少可证明的有限输入范围，不能自动推导 Big-M。 / The piecewise function has no provable finite input range for automatic Big-M inference."
                    )
                }
                if (!isUsableBounds(xBounds)) {
                    return piecewiseFailure(
                        "分段线性函数的输入范围为非有限或 solver 哨兵范围。 / The piecewise input range is non-finite or a solver sentinel range."
                    )
                }
                // xBounds has already been validated. Derive the fallback from that exact
                // proof instead of calling defaultBigM, whose legacy fallback can hide a
                // converter failure or silently manufacture a Big-M without a proof.
                fallbackM = when (val result = resolveBigM(xBounds.absMax, explicit = false)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            } else {
                xBounds = null
                fallbackM = null
            }
            val outputLower = outputBounds.lower
            val outputUpper = outputBounds.upper

            // Exactly one segment must be active: sum(s[i]) = 1 / 恰好一个线段激活：sum(s[i]) = 1
            val sumMonos = selectorVars.map { LinearMonomial(one, it) }
            allConstraints += LinearInequality(
                LinearPolynomial(sumMonos, zero),
                LinearPolynomial(emptyList(), one), Comparison.EQ, "${name}_select_one")

            for (i in 0 until numSegments) {
                val sVar = selectorVars[i]
                val bpLow = breakpoints[i]
                val bpHigh = breakpoints[i + 1]
                val slope = slopes[i]
                val intercept = intercepts[i]
                val bigMValue = if (explicitBigM != null) {
                    explicitBigM
                } else if (xBounds != null) {
                    val xLower = xBounds.lower
                    val xUpper = xBounds.upper
                    val lineAtLower = slope * xLower + intercept
                    val lineAtUpper = slope * xUpper + intercept
                    val lineLower = if (lineAtLower ls lineAtUpper) lineAtLower else lineAtUpper
                    val lineUpper = if (lineAtLower gr lineAtUpper) lineAtLower else lineAtUpper
                    val xLowerRelax = if (bpLow gr xLower) bpLow - xLower else zero
                    val xUpperRelax = if (xUpper gr bpHigh) xUpper - bpHigh else zero
                    val eqUpperRelax = (outputUpper - lineLower).abs()
                    val eqLowerRelax = (outputLower - lineUpper).abs()
                    val candidate = listOf(
                        xLowerRelax,
                        xUpperRelax,
                        eqUpperRelax,
                        eqLowerRelax,
                        fallbackM ?: return piecewiseFailure(
                            "分段线性函数无法解析自动 Big-M。 / The piecewise function cannot resolve an automatic Big-M."
                        )
                    ).reduce { acc, value -> if (value gr acc) value else acc }
                    when (val result = resolveBigM(candidate, explicit = false)) {
                        is Ok -> result.value
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                } else {
                    fallbackM ?: return piecewiseFailure(
                        "分段线性函数无法解析自动 Big-M。 / The piecewise function cannot resolve an automatic Big-M."
                    )
                }

                // Lower bound: x >= bpLow - M*(1 - s[i]) => x + M*s[i] >= bpLow - M... => x + M - M*s >= bpLow
                // 下界：x >= bpLow - M*(1 - s[i])，即 x + M - M*s >= bpLow
                allConstraints += LinearInequality(
                    LinearPolynomial(x.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
                        LinearMonomial(-bigMValue, sVar), x.constant + bigMValue),
                    LinearPolynomial(emptyList(), bpLow), Comparison.GE, "${name}_seg_${i}_lb")

                // Upper bound: x <= bpHigh + M*(1 - s[i]) => x + M*s[i] <= bpHigh + M
                // 上界：x <= bpHigh + M*(1 - s[i])，即 x + M*s[i] <= bpHigh + M
                allConstraints += LinearInequality(
                    LinearPolynomial(x.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
                        LinearMonomial(bigMValue, sVar), x.constant),
                    LinearPolynomial(emptyList(), bpHigh + bigMValue), Comparison.LE, "${name}_seg_${i}_ub")

                // y = slope*x + intercept when s[i]=1
                // s[i]=1 时 y = slope*x + intercept
                // y - slope*x - intercept <= M*(1 - s[i]) => y - slope*x - intercept + M*s[i] <= M
                // y - slope*x - intercept <= M*(1 - s[i])，即 y - slope*x - intercept + M*s[i] <= M
                val negSlopeXMonos = x.monomials.map { LinearMonomial(-it.coefficient * slope, it.symbol) }
                allConstraints += LinearInequality(
                    LinearPolynomial(listOf(LinearMonomial(one, resultVar)) +
                        negSlopeXMonos + LinearMonomial(bigMValue, sVar), -intercept),
                    LinearPolynomial(emptyList(), bigMValue), Comparison.LE, "${name}_seg_${i}_eq_ub")

                // y - slope*x - intercept >= -M*(1 - s[i]) => y - slope*x - intercept - M*s[i] >= -M
                // y - slope*x - intercept >= -M*(1 - s[i])，即 y - slope*x - intercept - M*s[i] >= -M
                allConstraints += LinearInequality(
                    LinearPolynomial(listOf(LinearMonomial(one, resultVar)) +
                        negSlopeXMonos + LinearMonomial(-bigMValue, sVar), -intercept),
                    LinearPolynomial(emptyList(), -bigMValue), Comparison.GE, "${name}_seg_${i}_eq_lb")
            }

            if (allConstraints.any { !isUsableConstraint(it) }) {
                return piecewiseFailure(
                    "分段线性函数生成了非有限或不可表示的约束。 / The piecewise function generated a non-finite or unrepresentable constraint."
                )
            }

            originalConstraintCount = try {
                model.constraints.size
            } catch (error: RuntimeException) {
                return piecewiseRuntimeFailure(
                    operation = "读取分段线性函数约束数量 / Read piecewise constraint count",
                    error = error
                )
            }
            constraintOperationStarted = true
            val registrationFailure = addConstraints(model, allConstraints)
            if (registrationFailure != null) {
                return rollbackPiecewiseRegistration(
                    failure = registrationFailure,
                    tokens = modelTokens,
                    tokenSnapshot = tokenSnapshot,
                    rangeSnapshot = rangeSnapshot,
                    constraintRollback = rollbackPiecewiseConstraints(model, originalConstraintCount!!)
                )
            }

            try {
                // Commit the range only after the complete constraint batch succeeds. /
                // 仅在整批约束成功写入后提交范围。
                resultVar.range.set(outputRange)
                ok
            } catch (error: RuntimeException) {
                rollbackPiecewiseRegistration(
                    failure = piecewiseRuntimeFailure(
                        operation = "提交分段线性函数结果范围 / Commit piecewise result range",
                        error = error
                    ),
                    tokens = modelTokens,
                    tokenSnapshot = tokenSnapshot,
                    rangeSnapshot = rangeSnapshot,
                    constraintRollback = rollbackPiecewiseConstraints(model, originalConstraintCount!!)
                )
            }
        } catch (error: RuntimeException) {
            val failure: Try = piecewiseRuntimeFailure(
                operation = "注册分段线性函数约束 / Register piecewise constraints",
                error = error
            )
            val constraintRollback = if (constraintOperationStarted && originalConstraintCount != null) {
                rollbackPiecewiseConstraints(model, originalConstraintCount!!)
            } else {
                ok
            }
            val tokenRollback = if (constraintOperationStarted) {
                rollbackPiecewiseTokens(modelTokens, tokenSnapshot)
            } else {
                ok
            }
            piecewiseRollbackFailure(
                piecewiseRollbackFailure(
                    piecewiseRollbackFailure(failure, constraintRollback),
                    tokenRollback
                ),
                restoreResultRange(rangeSnapshot)
            )
        }
    }

    companion object {
        /**
         * 创建分段线性函数实例。 / Create a piecewise linear function instance.
         *
         * @param x 输入线性多项式 / the input linear polynomial
         * @param breakpoints 断点值列表 / list of breakpoint values
         * @param slopes 各段斜率 / slope of each segment
         * @param intercepts 各段截距 / intercept of each segment
         * @param m Big-M 常量（未提供时必须能从输入范围证明有限性）/ Big-M constant (when omitted, a finite input range must be provable)
         * @param converter 值类型转换器 / value type converter
         * @param name 此函数的唯一名称 / unique name for this function
         * @param displayName 可选的人类可读显示名称 / optional human-readable display name
         * @return 分段线性函数实例 / piecewise linear function instance
        */
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            breakpoints: List<V>,
            slopes: List<V>,
            intercepts: List<V>,
            m: V? = null,
            converter: IntoValue<V>,
            name: String = "piecewise",
            displayName: String? = null
        ): UnivariateLinearPiecewiseFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            UnivariateLinearPiecewiseFunction(
                x = x, breakpoints = breakpoints, slopes = slopes, intercepts = intercepts, m = m,
                converter = converter, name = name, displayName = displayName
            )

        /**
         * 从采样点创建分段线性函数。 / Create a piecewise linear function from sampling points.
         *
         * @param x 输入线性多项式 / the input linear polynomial
         * @param points 采样点列表 / list of sampling points
         * @param m Big-M 常量（未提供时必须能从输入范围证明有限性）/ Big-M constant (when omitted, a finite input range must be provable)
         * @param converter 值类型转换器 / value type converter
         * @param name 此函数的唯一名称 / unique name for this function
         * @param displayName 可选的人类可读显示名称 / optional human-readable display name
         * @return 分段线性函数实例 / piecewise linear function instance
        */
        @JvmStatic
        @JvmName("fromPoints")
        fun <V> fromPoints(
            x: LinearPolynomial<V>,
            points: List<Point<Dim2, V>>,
            m: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): UnivariateLinearPiecewiseFunction<V> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
            return when (val result = fromPointsResult(
                x = x,
                points = points,
                m = m,
                converter = converter,
                name = name,
                displayName = displayName
            )) {
                is Ok -> result.value
                is Failed, is Fatal -> invalidFromPoints(
                    x = x,
                    points = points,
                    m = m,
                    converter = converter,
                    name = name,
                    displayName = displayName
                )
            }
        }

        /**
         * 从采样点安全创建分段线性函数。 / Safely create a piecewise linear function from sampling points.
         *
         * 点数不足、横坐标重复、数值运算失败或产生非有限值时返回 [Failed]，不抛出异常。
         * Insufficient points, duplicate x coordinates, arithmetic failures, and non-finite values return [Failed] without throwing.
         *
         * @param x 输入线性多项式 / the input linear polynomial
         * @param points 采样点列表 / sampling points
         * @param m Big-M 常量 / Big-M constant
         * @param converter 值类型转换器 / value type converter
         * @param name 此函数的唯一名称 / unique name for this function
         * @param displayName 可选的人类可读显示名称 / optional human-readable display name
         * @return 分段线性函数创建结果 / piecewise linear function creation result
         */
        @JvmStatic
        @JvmName("fromPointsResult")
        fun <V> fromPointsResult(
            x: LinearPolynomial<V>,
            points: List<Point<Dim2, V>>,
            m: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): Ret<UnivariateLinearPiecewiseFunction<V>> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
            return try {
                if (points.size < 2) {
                    return Failed(
                        ErrorCode.IllegalArgument,
                        "从采样点创建分段线性函数至少需要两个点。 / At least two points are required to create a piecewise function."
                    )
                }
                val breakpoints = points.map { it[0] }
                for (i in 0 until breakpoints.size - 1) {
                    if (breakpoints[i].compareTo(breakpoints[i + 1]) >= 0) {
                        return Failed(
                            ErrorCode.IllegalArgument,
                            "采样点的 x 坐标必须严格递增。 / Sampling-point x coordinates must be strictly increasing."
                        )
                    }
                }
                val slopes = (0 until points.size - 1).map { i ->
                    (points[i + 1][1] - points[i][1]) / (points[i + 1][0] - points[i][0])
                }
                val intercepts = (0 until points.size - 1).map { i ->
                    points[i][1] - slopes[i] * points[i][0]
                }
                val function = UnivariateLinearPiecewiseFunction(
                    x = x,
                    breakpoints = breakpoints,
                    slopes = slopes,
                    intercepts = intercepts,
                    m = m,
                    converter = converter,
                    name = name,
                    displayName = displayName
                )
                when (val validation = function.resolveOutputBounds()) {
                    is Ok -> Ok(function)
                    is Failed -> Failed(validation.error)
                    is Fatal -> Fatal(validation.errors)
                }
            } catch (error: RuntimeException) {
                val detail = error.message?.takeIf { it.isNotBlank() }
                    ?: error::class.simpleName
                    ?: "unknown runtime error"
                Failed(
                    ErrorCode.IllegalArgument,
                    "从采样点创建分段线性函数失败：$detail / Failed to create a piecewise function from sampling points: $detail"
                )
            }
        }

        private fun <V> invalidFromPoints(
            x: LinearPolynomial<V>,
            points: List<Point<Dim2, V>>,
            m: V?,
            converter: IntoValue<V>,
            name: String,
            displayName: String?
        ): UnivariateLinearPiecewiseFunction<V> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
            val breakpoints = try {
                points.map { it[0] }
            } catch (_: RuntimeException) {
                emptyList()
            }
            return UnivariateLinearPiecewiseFunction(
                x = x,
                breakpoints = breakpoints,
                slopes = emptyList(),
                intercepts = emptyList(),
                m = m,
                converter = converter,
                name = name,
                displayName = displayName
            )
        }
    }
}
