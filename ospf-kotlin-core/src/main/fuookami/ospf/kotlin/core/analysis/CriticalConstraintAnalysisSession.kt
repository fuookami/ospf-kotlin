/** Immutable-baseline analysis session skeleton. / 基于不可变基线的分析会话骨架。 */
package fuookami.ospf.kotlin.core.analysis

import java.util.EnumMap
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.report.SolverDescriptor
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

/** Analysis-stage cache buckets reserved for later analyzers. */
enum class AnalysisCacheKind {
    Activity,
    FixedIntegerLp,
    Perturbation,
    Target,
    Conflict
}

/**
 * Coordinates analysis stages around one immutable CP snapshot.
 *
 * This type intentionally does not solve, lower, perturb, or mutate a model.
 * It provides the lifecycle and stable inputs that S1+ analyzers can share.
 * Cache access is internal so algorithm packages in this module can reuse the
 * session without exposing solver artifacts through the public API.
 */
class CriticalConstraintAnalysisSession(
    /** Immutable model baseline. / 不可变模型基线。 */
    val baselineSnapshot: ConstraintProgrammingModelSnapshot,
    /** Optional baseline incumbent. / 可选基线 incumbent。 */
    val baselineSolution: ConstraintProgrammingSolution? = null,
    /** Optional baseline objective value. / 可选基线目标值。 */
    val baselineObjectiveValue: Flt64? = null,
    /** Solver descriptor used for dispatch and provenance. / 用于调度与来源记录的求解器描述。 */
    val solverDescriptor: SolverDescriptor? = null,
    /** Capability matrix; derived from the descriptor when omitted. / 能力矩阵，未提供时从 descriptor 推导。 */
    val capabilityMatrix: CapabilityMatrix = solverDescriptor?.let { CapabilityMatrix.from(it) }
        ?: CapabilityMatrix()
) : AutoCloseable {
    /** Validation result for the immutable baseline and session inputs. / 不可变基线及 session 输入的校验结果。 */
    val validation: Try = validateSessionInputs(
        snapshot = baselineSnapshot,
        baselineSolution = baselineSolution,
        baselineObjective = baselineObjectiveValue
    )

    private val caches = EnumMap<AnalysisCacheKind, MutableMap<Any, Any>>(AnalysisCacheKind::class.java).apply {
        AnalysisCacheKind.values().forEach { put(it, LinkedHashMap()) }
    }

    @Volatile
    private var closed = false

    /** Compatibility alias for analyzers that call the baseline simply snapshot. */
    val snapshot: ConstraintProgrammingModelSnapshot
        get() = baselineSnapshot

    /** Compatibility alias for the baseline objective. */
    val baselineObjective: Flt64?
        get() = baselineObjectiveValue

    /** Whether this session has released its analysis caches. */
    val isClosed: Boolean
        get() = closed

    /** Current cache sizes, useful for lifecycle tests and diagnostics. */
    val cacheSizes: Map<AnalysisCacheKind, Int>
        get() = synchronized(caches) {
            AnalysisCacheKind.values().associateWith { caches[it]?.size ?: 0 }
        }

    /** Remove all derived analysis cache entries. */
    fun clearCaches() {
        synchronized(caches) {
            caches.values.forEach { it.clear() }
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> cached(kind: AnalysisCacheKind, key: Any): T? {
        if (closed) {
            return null
        }
        return synchronized(caches) { caches[kind]?.get(key) as? T }
    }

    internal fun cache(kind: AnalysisCacheKind, key: Any, value: Any): Try {
        if (closed) {
            return Failed(
                ErrorCode.ApplicationStopped,
                "分析 session 已关闭 / Analysis session is closed"
            )
        }
        synchronized(caches) {
            if (closed) {
                return Failed(
                    ErrorCode.ApplicationStopped,
                    "分析 session 已关闭 / Analysis session is closed"
                )
            }
            caches.getValue(kind)[key] = value
        }
        return ok
    }

    override fun close() {
        synchronized(caches) {
            if (!closed) {
                caches.values.forEach { it.clear() }
                closed = true
            }
        }
    }

    companion object {
        /**
         * Snapshot a mutable CP model and create a session.
         *
         * The returned `Ret` preserves the model's existing structured
         * validation errors and keeps this protocol compatible with the CP
         * model API.
         */
        fun from(
            model: ConstraintProgrammingModel,
            baselineSolution: ConstraintProgrammingSolution? = null,
            baselineObjectiveValue: Flt64? = null,
            solverDescriptor: SolverDescriptor? = null
        ): Ret<CriticalConstraintAnalysisSession> {
            return model.snapshot().map { snapshot ->
                fromSnapshot(
                    snapshot = snapshot,
                    baselineSolution = baselineSolution,
                    baselineObjectiveValue = baselineObjectiveValue,
                    solverDescriptor = solverDescriptor
                )
            }
        }

        /** Create a session from an already validated immutable snapshot. */
        fun fromSnapshot(
            snapshot: ConstraintProgrammingModelSnapshot,
            baselineSolution: ConstraintProgrammingSolution? = null,
            baselineObjectiveValue: Flt64? = null,
            solverDescriptor: SolverDescriptor? = null,
            capabilityMatrix: CapabilityMatrix = solverDescriptor?.let { CapabilityMatrix.from(it) }
                ?: CapabilityMatrix()
        ): CriticalConstraintAnalysisSession {
            return CriticalConstraintAnalysisSession(
                baselineSnapshot = snapshot,
                baselineSolution = baselineSolution,
                baselineObjectiveValue = baselineObjectiveValue,
                solverDescriptor = solverDescriptor,
                capabilityMatrix = capabilityMatrix
            )
        }

        /** Create a validated session result from an immutable snapshot. / 从不可变 snapshot 创建带校验结果的安全 session。 */
        fun tryFromSnapshot(
            snapshot: ConstraintProgrammingModelSnapshot,
            baselineSolution: ConstraintProgrammingSolution? = null,
            baselineObjectiveValue: Flt64? = null,
            solverDescriptor: SolverDescriptor? = null,
            capabilityMatrix: CapabilityMatrix = solverDescriptor?.let { CapabilityMatrix.from(it) }
                ?: CapabilityMatrix()
        ): Ret<CriticalConstraintAnalysisSession> {
            val session = fromSnapshot(
                snapshot = snapshot,
                baselineSolution = baselineSolution,
                baselineObjectiveValue = baselineObjectiveValue,
                solverDescriptor = solverDescriptor,
                capabilityMatrix = capabilityMatrix
            )
            return when (val result = session.validation) {
                is Ok -> ok(session)
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }

        /**
         * Create a session using a solver descriptor without retaining the
         * solver handle. Solver ownership remains with the caller.
         */
        fun fromSnapshot(
            snapshot: ConstraintProgrammingModelSnapshot,
            solver: ConstraintProgrammingSolver,
            baselineSolution: ConstraintProgrammingSolution? = null,
            baselineObjectiveValue: Flt64? = null
        ): CriticalConstraintAnalysisSession {
            return fromSnapshot(
                snapshot = snapshot,
                baselineSolution = baselineSolution,
                baselineObjectiveValue = baselineObjectiveValue,
                solverDescriptor = solver.descriptor
            )
        }
    }
}

/** Return a structured failure when a session baseline is invalid. / 当 session 基线无效时返回结构化失败。 */
internal fun <T> invalidSessionResult(
    session: CriticalConstraintAnalysisSession
): Ret<T>? {
    return when (val result = session.validation) {
        is Ok -> null
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

private fun validateSessionInputs(
    snapshot: ConstraintProgrammingModelSnapshot,
    baselineSolution: ConstraintProgrammingSolution?,
    baselineObjective: Flt64?
): Try {
    if (!snapshot.validateIdentity()) {
        return Failed(
            ErrorCode.IllegalArgument,
            "Analysis session requires a valid CP snapshot identity manifest / 分析 session 需要有效的 CP snapshot 身份清单"
        )
    }
    if (!snapshot.validateObjectiveSemantics()) {
        return Failed(
            ErrorCode.IllegalArgument,
            "Analysis session requires valid CP objective semantics / 分析 session 需要有效的 CP 目标语义"
        )
    }
    if (baselineObjective != null && !baselineObjective.isFinite()) {
        return Failed(
            ErrorCode.IllegalArgument,
            "Baseline objective value must be finite / 基线目标值必须有限"
        )
    }
    if (baselineSolution != null) {
        val unknown = baselineSolution.values.keys.firstOrNull { snapshot.variable(it) == null }
        if (unknown != null) {
            return Failed(
                ErrorCode.DataNotFound,
                "Baseline CP 解包含未知变量：$unknown / Baseline CP solution contains an unknown variable: $unknown"
            )
        }
        val missing = snapshot.variables.firstOrNull { it.id !in baselineSolution.values }
        if (missing != null) {
            return Failed(
                ErrorCode.DataNotFound,
                "Baseline CP 解缺少变量：${missing.id} / Baseline CP solution misses variable: ${missing.id}"
            )
        }
        val outside = snapshot.variables.firstOrNull { variable ->
            !variable.domain.contains(baselineSolution.values.getValue(variable.id))
        }
        if (outside != null) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "Baseline CP 解超出变量值域：${outside.id} / Baseline CP solution is outside variable domain: ${outside.id}"
            )
        }
        for (definition in snapshot.constraints) {
            when (val result = definition.constraint.isSatisfied(baselineSolution.values)) {
                is Ok -> if (result.value != true) {
                    return Failed(
                        ErrorCode.ORSolutionInvalid,
                        "Baseline CP 解不满足约束：${definition.id} / Baseline CP solution violates constraint: ${definition.id}"
                    )
                }

                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
    }
    if (baselineObjective != null) {
        val solution = baselineSolution ?: return Failed(
            ErrorCode.ORSolutionInvalid,
            "提供基线目标值时必须同时提供基线 CP 解 / A baseline CP solution is required when a baseline objective is supplied"
        )
        val objective = snapshot.objectives.singleOrNull() ?: return Failed(
            ErrorCode.IllegalArgument,
            "基线目标值需要唯一的 CP objective / A baseline objective value requires exactly one CP objective"
        )
        val evaluated = when (val result = objective.expression.evaluate(solution.values)) {
            is Ok -> result.value!!
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (evaluated.toFlt64() != baselineObjective) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "基线目标值与基线 CP 解不一致：${baselineObjective} != ${evaluated.toFlt64()} / " +
                    "Baseline objective does not match the baseline CP solution: " +
                    "${baselineObjective} != ${evaluated.toFlt64()}"
            )
        }
    }
    return ok
}
