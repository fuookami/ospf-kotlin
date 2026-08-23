@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.report

import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration
import kotlinx.serialization.Serializable
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel
import fuookami.ospf.kotlin.core.solver.output.LinearInfeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.LinearSolverOutput
import fuookami.ospf.kotlin.core.solver.output.QuadraticInfeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.QuadraticSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.output.UnifiedSolverOutput
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/** 问题结论 / Problem conclusion */
enum class ProblemStatus {
    Feasible,
    Infeasible,
    Unbounded,
    InfeasibleOrUnbounded,
    Unknown
}

/** 求解终止原因 / Solve termination reason */
enum class TerminationReason {
    Completed,
    TimeLimit,
    NodeLimit,
    IterationLimit,
    SolutionLimit,
    ObjectiveLimit,
    Cancelled,
    Interrupted,
    NumericalFailure,
    BackendFailure
}

/** 解存在性 / Solution presence */
enum class SolutionPresence {
    None,
    Incumbent,
    Optimal
}

/** 证明状态 / Proof status */
enum class ProofStatus {
    None,
    Claimed,
    Verified
}

/** 模型类型 / Model type */
enum class SolverModelType {
    LP,
    MIP,
    QP,
    QCP,
    CP
}

/** 模型元素稳定标识 / Stable model element identifier */
@JvmInline
value class ModelElementId(val value: String)

/** 模型元素身份作用域。 / Model-element identity scope. */
enum class ModelElementScope {
    /** 可由调用方提供并跨重建复用的身份。 / Caller-owned identity reusable across rebuilds. */
    Stable,

    /** 仅在当前模型实例内有效的身份。 / Identity valid only within the current model instance. */
    ModelLocal
}

/**
 * 模型元素来源标识。 / Origin reference for a model element.
 *
 * @property kind 来源类型 / Origin kind
 * @property key 来源键 / Origin key
 */
data class ModelElementOrigin(
    val kind: String,
    val key: String
)

/** 变量稳定标识 / Stable variable identifier */
@JvmInline
value class VariableId(val value: String)

/** 变量边界方向 / Variable bound side */
enum class BoundSide {
    Lower,
    Upper
}

/**
 * 稳定变量边界引用。 / Stable variable-bound reference.
 *
 * @property variableId 变量稳定标识 / Stable variable identifier
 * @property side 边界方向 / Bound side
 */
data class VariableBoundRef(
    val variableId: VariableId,
    val side: BoundSide
)

/**
 * 稳定变量值域引用。 / Stable variable-domain reference.
 *
 * @property variableId 变量稳定标识 / Stable variable identifier
 */
data class VariableDomainRef(
    val variableId: VariableId
)

/** 约束稳定标识 / Stable constraint identifier */
@JvmInline
value class ConstraintId(val value: String)

/** 目标稳定标识 / Stable objective identifier */
@JvmInline
value class ObjectiveId(val value: String)

/** 求解运行标识 / Solve run identifier */
@JvmInline
value class SolveRunId(val value: String)

/** 求解尝试标识 / Solve attempt identifier */
@JvmInline
value class SolveAttemptId(val value: String)

/** 诊断或执行问题类别 / Diagnostic or execution issue category */
enum class SolveIssueCategory {
    InvalidInput,
    Environment,
    License,
    Numerical,
    Callback,
    Parsing,
    Backend,
    Protocol,
    Unsupported
}

/**
 * 结构化求解问题。 / Structured solve issue.
 *
 * @property code 稳定错误码 / Stable issue code
 * @property category 问题类别 / Issue category
 * @property message 脱敏消息 / Redacted message
 * @property details 结构化详情 / Structured details
 */
data class SolveIssue(
    val code: String,
    val category: SolveIssueCategory,
    val message: String,
    val details: Map<String, String> = emptyMap()
)

/**
 * 求解器能力声明。 / Solver capability declaration.
 *
 * @property modelTypes 支持的模型类型 / Supported model types
 * @property nativeIIS 是否支持原生 IIS / Whether native IIS is supported
 * @property dual 是否支持对偶信息 / Whether dual information is supported
 * @property farkas 是否支持 Farkas 证书 / Whether Farkas certificates are supported
 * @property warmStart 是否支持热启动 / Whether warm starts are supported
 * @property solutionPool 是否支持解池 / Whether solution pools are supported
 * @property callback 是否支持回调 / Whether callbacks are supported
 * @property interrupt 是否支持中断 / Whether interruption is supported
 * @property checkpoint 是否支持检查点 / Whether checkpoints are supported
 * @property resume 是否支持恢复 / Whether resume is supported
 * @property constraintProgrammingFeatures CP 特性及支持级别 / CP features and support levels
 */
data class SolverCapabilities(
    val modelTypes: Set<SolverModelType>,
    val nativeIIS: Boolean = false,
    val dual: Boolean = false,
    val farkas: Boolean = false,
    val warmStart: Boolean = false,
    val solutionPool: Boolean = false,
    val callback: Boolean = false,
    val interrupt: Boolean = false,
    val checkpoint: Boolean = false,
    val resume: Boolean = false,
    val constraintProgrammingFeatures: Map<ConstraintProgrammingFeature, ConstraintProgrammingSupportLevel> = emptyMap()
)

/** 求解器描述符 / Solver descriptor */
data class SolverDescriptor(
    val solverId: String,
    val backendName: String,
    val backendVersion: String? = null,
    val pluginVersion: String? = null,
    val capabilities: SolverCapabilities
)

/** Typed value used by a backend configuration snapshot. / backend 配置快照中的类型化值。 */
@Serializable
sealed class BackendParameterValue {
    /** Text parameter. / 文本参数。 */
    @Serializable
    data class Text(val value: String) : BackendParameterValue()

    /** Integer parameter. / 整数参数。 */
    @Serializable
    data class Integer(val value: Long) : BackendParameterValue()

    /** Decimal parameter encoded with a deterministic string. / 使用确定性字符串编码的十进制参数。 */
    @Serializable
    data class Decimal(val value: String) : BackendParameterValue()

    /** Boolean parameter. / 布尔参数。 */
    @Serializable
    data class BooleanValue(val value: Boolean) : BackendParameterValue()
}

/** One redacted and serializable backend parameter. / 一个脱敏且可序列化的 backend 参数。 */
@Serializable
data class BackendParameter(
    val name: String,
    val value: BackendParameterValue,
    val sensitive: Boolean = false
)

/**
 * Serializable and fingerprintable backend configuration snapshot. / 可序列化且可生成指纹的 backend 配置快照。
 *
 * Sensitive values must be represented by a redaction marker and never enter the snapshot in clear text.
 * 敏感值必须使用脱敏标记，不能以明文进入快照。
 *
 * @property type 配置类型 / Configuration type
 * @property parameters 脱敏参数 / Redacted parameters
 */
@Serializable
data class BackendConfigurationSnapshot(
    val type: String,
    val parameters: List<BackendParameter>
) {
    /** Deterministic configuration fingerprint. / 确定性配置指纹。 */
    fun fingerprint(): ConfigurationFingerprint {
        val canonical = parameters
            .sortedBy { it.name }
            .joinToString("\n") { parameter ->
                "${parameter.name}=${parameter.canonicalValue()}|sensitive=${parameter.sensitive}"
            }
        return SolveFingerprinting.sha256("type=$type\n$canonical", "backend-config-1")
    }
}

/** Return a stable redacted representation suitable for reports and fingerprints. / 返回适合报告和指纹的稳定脱敏表示。 */
fun BackendParameter.redactedValue(): String {
    if (sensitive) {
        return "<redacted>"
    }
    return when (val parameterValue = value) {
        is BackendParameterValue.Text -> parameterValue.value
        is BackendParameterValue.Integer -> parameterValue.value.toString()
        is BackendParameterValue.Decimal -> parameterValue.value
        is BackendParameterValue.BooleanValue -> parameterValue.value.toString()
    }
}

private fun BackendParameter.canonicalValue(): String {
    if (sensitive) {
        return "redacted:<redacted>"
    }
    return when (val parameterValue = value) {
        is BackendParameterValue.Text -> "text:${parameterValue.value}"
        is BackendParameterValue.Integer -> "integer:${parameterValue.value}"
        is BackendParameterValue.Decimal -> "decimal:${parameterValue.value}"
        is BackendParameterValue.BooleanValue -> "boolean:${parameterValue.value}"
    }
}

/**
 * 可审计 backend 配置。 / Auditable backend configuration.
 *
 * 实现不得在公开参数或指纹参数中返回密码、令牌或连接密钥。 / Implementations must not expose passwords, tokens, or connection secrets.
 */
interface BackendConfiguration {
    /** 配置类型标识 / Configuration type identifier */
    val type: String

    /** 脱敏、类型化且确定排序的参数 / Redacted, typed, and deterministically ordered parameters */
    fun parameters(): List<BackendParameter>

    /** 脱敏配置快照 / Redacted configuration snapshot */
    fun snapshot(): BackendConfigurationSnapshot {
        return BackendConfigurationSnapshot(type = type, parameters = parameters())
    }
}

/** 求解器执行来源 / Solver execution provenance */
data class SolverProvenance(
    val descriptor: SolverDescriptor,
    val nativeVersion: String? = null,
    val effectiveParameters: Map<String, String> = emptyMap(),
    val ignoredParameters: Map<String, String> = emptyMap(),
    val configuration: BackendConfigurationSnapshot? = null,
    val threadCount: Int? = null,
    val randomSeed: Long? = null,
    val deterministic: Boolean? = null,
    val environmentSummary: Map<String, String> = emptyMap()
)

/** 求解统计 / Solve statistics */
data class SolveStatistics<V>(
    val solveTime: Duration? = null,
    val iterations: ULong? = null,
    val nodes: ULong? = null,
    val bestBound: V? = null,
    val gap: V? = null
)

/** 求解解及解池 / Solve solution and pool */
data class SolveSolution<V>(
    val values: Solution<V>,
    val objective: V? = null,
    val pool: List<Solution<V>> = emptyList()
) : List<V> by values

/** 求解证明 / Solve proof */
data class SolveProof(
    val status: ProofStatus,
    val kind: String? = null,
    val reference: String? = null
)

/** 约束关系 / Constraint relation */
enum class ConstraintRelation {
    LessEqual,
    Equal,
    GreaterEqual,
    Range
}

/** 约束求值 / Constraint evaluation */
data class ConstraintEvaluation<V>(
    val constraintId: ConstraintId,
    val lhs: V,
    val rhs: V,
    val relation: ConstraintRelation,
    val slack: V,
    val violation: V,
    val tolerance: V,
    val satisfied: Boolean,
    val dual: V? = null
)

/**
 * 变量上下界求值。 / Variable-bound evaluation.
 *
 * @param V 数值类型 / Numeric type
 * @property variableId 变量稳定标识 / Stable variable identifier
 * @property side 上下界方向 / Bound side
 * @property bound 声明的边界值 / Declared bound value
 * @property value 解中的变量值 / Variable value in the solution
 * @property slack 相对边界的松弛 / Slack from the bound
 * @property violation 相对边界的违反量 / Violation against the bound
 * @property tolerance 可接受的违反容差 / Accepted violation tolerance
 * @property satisfied 是否满足边界 / Whether the bound is satisfied
 */
data class VariableBoundEvaluation<V>(
    val variableId: VariableId,
    val side: BoundSide,
    val bound: V,
    val value: V,
    val slack: V,
    val violation: V,
    val tolerance: V,
    val satisfied: Boolean
)

/** 不可行证据来源 / Infeasibility evidence source */
enum class InfeasibilityEvidenceSource {
    NativeIIS,
    Farkas,
    ConstraintConflict,
    ElasticFilter,
    DeletionFilter,
    None
}

/** 证据有效性 / Evidence validity */
enum class EvidenceValidity {
    Verified,
    Heuristic,
    Unknown
}

/** 证据最小性 / Evidence minimality */
enum class EvidenceMinimality {
    Irreducible,
    Partial,
    NotChecked
}

/** 结构化不可行成员 / Structured infeasibility member */
sealed interface InfeasibilityMember {
    /**
     * 原始约束成员。 / Original constraint member.
     *
     * @property id 约束稳定标识 / Stable constraint identifier
     */
    data class Constraint(val id: ConstraintId) : InfeasibilityMember

    /**
     * 变量上下界成员。 / Variable-bound member.
     *
     * @property ref 变量边界引用 / Variable-bound reference
     */
    data class VariableBound(val ref: VariableBoundRef) : InfeasibilityMember

    /**
     * 稀疏值域成员。 / Sparse-domain member.
     *
     * @property ref 变量值域引用 / Variable-domain reference
     */
    data class VariableDomain(val ref: VariableDomainRef) : InfeasibilityMember
}

/** 不可行证据精度 / Infeasibility evidence exactness */
enum class EvidenceExactness {
    Exact,
    Irreducible,
    Heuristic,
    Unknown
}

/** 不可行证据完整度 / Infeasibility evidence completeness */
enum class EvidenceCompleteness {
    Complete,
    Partial,
    Unavailable
}

/**
 * 不可行证据。 / Infeasibility evidence.
 *
 * @property source 证据来源 / Evidence source
 * @property exactness 证据精确性 / Evidence exactness
 * @property completeness 证据完整度 / Evidence completeness
 * @property constraintIds 原始约束标识集合 / Original constraint identifiers
 * @property variableBoundIds 变量标识集合（兼容字段） / Variable identifiers (compatibility field)
 * @property elapsed 诊断耗时 / Diagnostic elapsed time
 * @property unavailableReason 证据不可用原因 / Reason evidence is unavailable
 * @property validity 证据有效性 / Evidence validity
 * @property minimality 证据最小性 / Evidence minimality
 * @property variableBoundRefs 变量边界引用集合 / Variable-bound references
 * @property variableDomainRefs 变量值域引用集合 / Variable-domain references
 * @property members 结构化证据成员集合 / Structured evidence members
 * @property reference 外部证据引用 / External evidence reference
 * @property assumptionIds 后端诊断假设成员标识 / Backend diagnostic assumption-member identifiers
 * @property verificationChecks 诊断复验次数 / Number of diagnostic verification checks
 * @property terminationReason 诊断复验终止原因 / Diagnostic verification termination reason
 */
data class InfeasibilityEvidence(
    val source: InfeasibilityEvidenceSource,
    val exactness: EvidenceExactness = EvidenceExactness.Unknown,
    val completeness: EvidenceCompleteness = EvidenceCompleteness.Unavailable,
    val constraintIds: Set<ConstraintId> = emptySet(),
    val variableBoundIds: Set<VariableId> = emptySet(),
    val elapsed: Duration? = null,
    val unavailableReason: SolveIssue? = null,
    val validity: EvidenceValidity = EvidenceValidity.Unknown,
    val minimality: EvidenceMinimality = EvidenceMinimality.NotChecked,
    val variableBoundRefs: Set<VariableBoundRef> = emptySet(),
    val variableDomainRefs: Set<VariableDomainRef> = emptySet(),
    val members: Set<InfeasibilityMember> = emptySet(),
    val reference: String? = null,
    val assumptionIds: Set<VariableId> = emptySet(),
    val verificationChecks: UInt64? = null,
    val terminationReason: TerminationReason? = null
)

/** 求解诊断 / Solve diagnostics */
data class SolveDiagnostics<V>(
    val constraintEvaluations: List<ConstraintEvaluation<V>> = emptyList(),
    val variableBoundEvaluations: List<VariableBoundEvaluation<V>> = emptyList(),
    val infeasibilityEvidence: InfeasibilityEvidence? = null,
    val warnings: List<SolveIssue> = emptyList(),
    val errors: List<SolveIssue> = emptyList()
)

/** 带算法和版本信息的加密指纹 / Cryptographic fingerprint with algorithm and schema version */
data class AuditFingerprint(
    val schemaVersion: String,
    val algorithm: String,
    val value: String
)

/** 模型指纹 / Model fingerprint */
typealias ModelFingerprint = AuditFingerprint

/** 配置指纹 / Configuration fingerprint */
typealias ConfigurationFingerprint = AuditFingerprint

/** 求解器指纹 / Solver fingerprint */
typealias SolverFingerprint = AuditFingerprint

/** 求解指纹集合 / Solve fingerprint set */
data class SolveFingerprints(
    val model: AuditFingerprint? = null,
    val configuration: AuditFingerprint? = null,
    val solver: AuditFingerprint? = null
)

/**
 * 统一求解报告。 / Unified solve report.
 *
 * 解与诊断使用 `V`，统计字段统一使用 `Flt64`，避免 CP 的 Int64 解在报告边界发生精度损失。 /
 * Solutions and diagnostics use `V`, while statistics always use `Flt64` so CP Int64 solutions retain exact precision at the report boundary.
 *
 * @param V 解与诊断数值类型 / Solution and diagnostic value type
 * @property schemaVersion 报告 schema 版本 / Report schema version
 * @property runId 求解运行标识 / Solve run identifier
 * @property problemStatus 问题状态 / Problem status
 * @property terminationReason 求解终止原因 / Solve termination reason
 * @property solutionPresence 解存在性 / Solution presence
 * @property solution 精确解与目标值 / Exact solution and objective value
 * @property proof 求解证明状态 / Solve proof status
 * @property statistics 求解统计，使用浮点统计值 / Solve statistics using floating-point statistic values
 * @property diagnostics 约束评估与不可行证据 / Constraint evaluations and infeasibility evidence
 * @property provenance 求解器及运行参数来源 / Solver and runtime provenance
 * @property fingerprints 模型、配置与求解器审计指纹 / Model, configuration, and solver audit fingerprints
 * @property attempts 组合求解的完整 backend 尝试轨迹 / Complete backend attempt traces for combinatorial solves
 * @property selectedAttemptId 被选择的 backend 尝试标识 / Selected backend attempt identifier
 * @property selectionReason 组合求解选择依据 / Combinatorial selection reason
 */
data class SolveReport<V>(
    val schemaVersion: String = CURRENT_SCHEMA_VERSION,
    val runId: SolveRunId? = null,
    val problemStatus: ProblemStatus,
    val terminationReason: TerminationReason,
    val solutionPresence: SolutionPresence,
    val solution: SolveSolution<V>? = null,
    val proof: SolveProof = SolveProof(ProofStatus.None),
    val statistics: SolveStatistics<Flt64> = SolveStatistics(),
    val diagnostics: SolveDiagnostics<V> = SolveDiagnostics(),
    val provenance: SolverProvenance? = null,
    val fingerprints: SolveFingerprints = SolveFingerprints(),
    val attempts: List<SolveAttemptTrace<V>> = emptyList(),
    val selectedAttemptId: SolveAttemptId? = null,
    val selectionReason: SolveSelectionReason? = null
) : SolverOutput, LinearSolverOutput, QuadraticSolverOutput, UnifiedSolverOutput {
    override val iterations: UInt64?
        get() = statistics.iterations?.let(::UInt64)

    override val nodeCount: UInt64?
        get() = statistics.nodes?.let(::UInt64)

    override val bestBound: Flt64?
        get() = statistics.bestBound

    override val mipGap: Flt64?
        get() = statistics.gap

    override val solveTime: Duration?
        get() = statistics.solveTime

    /** 主解值列表视图；没有 incumbent 时为空列表。 / Main-solution values; empty when no incumbent exists. */
    val values: Solution<V>
        get() = solution?.values ?: emptyList()

    companion object {
        const val CURRENT_SCHEMA_VERSION: String = "1.0"
    }
}

/** 组合求解选择依据 / Combinatorial solve selection reason */
enum class SolveSelectionReason {
    FirstFeasible,
    BestObjective,
    BestBound,
    PreferredBackend,
    NoSuccessfulAttempt
}

/** 单次 backend 尝试轨迹 / Single backend attempt trace */
data class SolveAttemptTrace<V>(
    val attemptId: SolveAttemptId,
    val parentAttemptId: SolveAttemptId? = null,
    val backendId: String,
    val report: SolveReport<V>? = null,
    val elapsed: Duration? = null,
    val errors: List<SolveIssue> = emptyList(),
    val cancellationReason: String? = null
)

/** 组合求解报告 / Combinatorial solve report */
data class CombinatorialSolveReport<V>(
    val finalReport: SolveReport<V>?,
    val attempts: List<SolveAttemptTrace<V>>,
    val selectedAttemptId: SolveAttemptId?,
    val selectionReason: SolveSelectionReason
)

/** 取消来源 / Cancellation source */
enum class CancellationSource {
    Caller,
    Callback,
    Coroutine,
    Future,
    Combinatorial,
    Remote,
    Timeout
}

/** 取消事实 / Cancellation fact */
data class CancellationRecord(
    val source: CancellationSource,
    val requestedAt: Instant,
    val reason: String? = null
)

/** 求解取消令牌 / Solve cancellation token */
class CancellationToken internal constructor(
    private val cancellation: AtomicReference<CancellationRecord?>,
    private val listeners: CopyOnWriteArrayList<(CancellationRecord) -> Try> = CopyOnWriteArrayList()
) {
    /** 是否已经请求取消 / Whether cancellation has been requested */
    val isCancellationRequested: Boolean get() = cancellation.get() != null

    /** 首次取消事实 / First cancellation record */
    val record: CancellationRecord? get() = cancellation.get()

    /**
     * 注册原生中断监听。若取消已经发生，监听会立即执行。 /
     * Register a native interruption listener; invoke it immediately when already cancelled.
     *
     * @param listener 原生中断回调 / Native interruption callback
     * @return 注册或执行结果 / Registration or execution result
     */
    fun register(listener: (CancellationRecord) -> Try): Try {
        val existing = cancellation.get()
        if (existing != null) {
            return invokeListener(listener, existing)
        }
        listeners += listener
        val raced = cancellation.get()
        return if (raced == null) {
            ok
        } else {
            listeners.remove(listener)
            invokeListener(listener, raced)
        }
    }

    /**
     * 移除原生中断监听，避免求解资源释放后再次取消触碰已关闭的 native 对象。 /
     * Remove a native interruption listener so a later cancellation cannot touch a released native object.
     *
     * @param listener 要移除的监听 / Listener to remove
     * @return 移除结果 / Removal result
     */
    fun unregister(listener: (CancellationRecord) -> Try): Try {
        listeners.remove(listener)
        return ok
    }

    /**
     * 请求取消。重复请求不会覆盖首次来源或重复执行监听。 /
     * Request cancellation; repeated requests do not replace the first source or re-run listeners.
     *
     * @param source 取消来源 / Cancellation source
     * @param reason 取消原因 / Cancellation reason
     * @return 原生中断监听执行结果 / Native interruption result
     */
    fun request(
        source: CancellationSource = CancellationSource.Caller,
        reason: String? = null
    ): Try {
        val record = CancellationRecord(source, Instant.now(), reason)
        if (!cancellation.compareAndSet(null, record)) {
            return ok
        }
        var failure: Try? = null
        listeners.forEach { listener ->
            when (val result = invokeListener(listener, record)) {
                is Failed, is Fatal -> if (failure == null) {
                    failure = result
                }
                else -> {}
            }
        }
        return failure ?: ok
    }

    private fun invokeListener(
        listener: (CancellationRecord) -> Try,
        record: CancellationRecord
    ): Try {
        return try {
            listener(record)
        } catch (error: Exception) {
            Failed(
                ErrorCode.ApplicationError,
                "原生取消监听失败：${error.message ?: error::class.simpleName} / " +
                    "Native cancellation listener failed: ${error.message ?: error::class.simpleName}"
            )
        }
    }
}

/**
 * 单次求解句柄。 / Per-solve handle.
 *
 * @property token 取消令牌 / Cancellation token
 */
class SolveHandle private constructor(
    val token: CancellationToken
) {
    /** 幂等请求取消；仅首次请求调用 backend 中断 / Request cancellation idempotently */
    fun cancel(
        source: CancellationSource = CancellationSource.Caller,
        reason: String? = null
    ): Try {
        return token.request(source, reason)
    }

    companion object {
        /** 创建独立求解句柄 / Create an independent solve handle */
        fun create(interrupt: (CancellationRecord) -> Try = { ok }): SolveHandle {
            val token = CancellationToken(AtomicReference<CancellationRecord?>())
            token.register(interrupt)
            return SolveHandle(token)
        }
    }
}

/** 确定性 SHA-256 指纹工具 / Deterministic SHA-256 fingerprint utility */
object SolveFingerprinting {
    /** 对已规范化的 UTF-8 内容生成指纹 / Fingerprint canonical UTF-8 content */
    fun sha256(canonicalContent: String, schemaVersion: String = "1.0"): AuditFingerprint {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(canonicalContent.toByteArray(Charsets.UTF_8))
        return AuditFingerprint(
            schemaVersion = schemaVersion,
            algorithm = "SHA-256",
            value = bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
        )
    }

    /** 规范化键值配置后生成指纹 / Fingerprint a canonicalized key-value configuration */
    fun configuration(parameters: Map<String, String>, schemaVersion: String = "1.0"): AuditFingerprint {
        val canonical = parameters.toSortedMap().entries.joinToString(separator = "\n") { (key, value) ->
            "${escape(key)}=${escape(value)}"
        }
        return sha256(canonical, schemaVersion)
    }

    private fun escape(value: String): String {
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("=", "\\=")
    }
}

/**
 * 对统一报告应用运行时元数据和解池。 / Apply runtime metadata and solution-pool data to a unified report.
 *
 * @param V 解值类型 / Solution value type
 * @param runId 求解运行标识 / Solve run identifier
 * @param provenance 求解器执行来源 / Solver execution provenance
 * @param fingerprints 审计指纹 / Audit fingerprints
 * @param solutionPool 求解器返回的解池 / Solution pool returned by the solver
 * @return 更新后的统一求解报告 / Updated unified solve report
 */
fun <V> SolveReport<V>.toSolveReport(
    runId: SolveRunId? = null,
    provenance: SolverProvenance? = null,
    fingerprints: SolveFingerprints = SolveFingerprints(),
    solutionPool: List<Solution<V>> = emptyList()
): SolveReport<V> {
    return copy(
        runId = runId ?: this.runId,
        provenance = provenance ?: this.provenance,
        fingerprints = if (fingerprints == SolveFingerprints()) {
            this.fingerprints
        } else {
            fingerprints
        },
        solution = solution?.copy(
            pool = if (solutionPool.isEmpty()) solution.pool else solutionPool
        )
    )
}

/**
 * 将旧统一输出包装为统一求解报告。 / Wrap a legacy unified solver output in a solve report.
 *
 * 该适配器保留旧接口产生的 IIS 诊断；新的求解链路应直接返回 `SolveReport`。 /
 * This adapter preserves IIS diagnostics produced by legacy interfaces; new solve pipelines should return `SolveReport` directly.
 *
 * @param runId 求解运行标识 / Solve run identifier
 * @param provenance 求解器执行来源 / Solver execution provenance
 * @param fingerprints 审计指纹 / Audit fingerprints
 * @param solutionPool 求解器返回的解池 / Solution pool returned by the solver
 * @return 统一求解报告 / Unified solve report
 */
@Suppress("UNCHECKED_CAST")
fun SolverOutput.toSolveReport(
    runId: SolveRunId? = null,
    provenance: SolverProvenance? = null,
    fingerprints: SolveFingerprints = SolveFingerprints(),
    solutionPool: List<Solution<Flt64>> = emptyList()
): SolveReport<Flt64> {
    return when (this) {
        is SolveReport<*> -> {
            val report = this as SolveReport<Flt64>
            report.copy(
                runId = runId ?: report.runId,
                provenance = provenance ?: report.provenance,
                fingerprints = if (fingerprints == SolveFingerprints()) {
                    report.fingerprints
                } else {
                    fingerprints
                },
                solution = report.solution?.copy(
                    pool = if (solutionPool.isEmpty()) report.solution.pool else solutionPool
                )
            )
        }

        is LinearInfeasibleSolverOutput -> SolveReport(
            runId = runId,
            problemStatus = ProblemStatus.Infeasible,
            terminationReason = TerminationReason.Completed,
            solutionPresence = SolutionPresence.None,
            diagnostics = diagnostics.withInfeasibilityEvidence(iisAvailable)
        ).copy(
            provenance = provenance,
            fingerprints = fingerprints,
            statistics = SolveStatistics(
                solveTime = solveTime,
                iterations = iterations?.toULong(),
                nodes = nodeCount?.toULong(),
                bestBound = bestBound,
                gap = mipGap
            )
        )

        is QuadraticInfeasibleSolverOutput -> SolveReport(
            runId = runId,
            problemStatus = ProblemStatus.Infeasible,
            terminationReason = TerminationReason.Completed,
            solutionPresence = SolutionPresence.None,
            diagnostics = diagnostics.withInfeasibilityEvidence(iisAvailable)
        ).copy(
            provenance = provenance,
            fingerprints = fingerprints,
            statistics = SolveStatistics(
                solveTime = solveTime,
                iterations = iterations?.toULong(),
                nodes = nodeCount?.toULong(),
                bestBound = bestBound,
                gap = mipGap
            )
        )

        else -> SolveReport(
            runId = runId,
            problemStatus = ProblemStatus.Unknown,
            terminationReason = TerminationReason.BackendFailure,
            solutionPresence = SolutionPresence.None,
            diagnostics = SolveDiagnostics(
                errors = listOf(
                    SolveIssue(
                        code = "legacy-output-unsupported",
                        category = SolveIssueCategory.Unsupported,
                        message = "旧输出类型无法转换为线性/二次报告：${this::class.simpleName} / " +
                            "Legacy output type cannot be converted to a linear/quadratic report: ${this::class.simpleName}"
                    )
                )
            ),
            provenance = provenance,
            fingerprints = fingerprints
        )
    }
}

private fun SolveDiagnostics<Flt64>.withInfeasibilityEvidence(
    iisAvailable: Boolean
): SolveDiagnostics<Flt64> {
    if (infeasibilityEvidence != null) {
        return this
    }
    return copy(
        infeasibilityEvidence = InfeasibilityEvidence(
            source = if (iisAvailable) {
                InfeasibilityEvidenceSource.NativeIIS
            } else {
                InfeasibilityEvidenceSource.None
            },
            exactness = if (iisAvailable) EvidenceExactness.Exact else EvidenceExactness.Unknown,
            completeness = if (iisAvailable) EvidenceCompleteness.Complete else EvidenceCompleteness.Unavailable,
            validity = if (iisAvailable) EvidenceValidity.Verified else EvidenceValidity.Unknown,
            minimality = if (iisAvailable) EvidenceMinimality.Irreducible else EvidenceMinimality.NotChecked
        )
    )
}

/**
 * 将 Flt64 报告转换为目标数值类型。 / Convert a Flt64 report to the target numeric type.
 *
 * 统计字段保持 Flt64，因为报告合同规定统计值使用浮点类型；解和约束诊断使用目标类型。 /
 * Statistics remain Flt64 because the report contract uses floating-point statistics; solutions and constraint diagnostics use the target type.
 *
 * @param V 目标数值类型 / Target numeric type
 * @param converter 数值转换器 / Numeric converter
 * @return 转换后的报告 / Converted report
 */
fun <V> SolveReport<Flt64>.convertTo(converter: IntoValue<V>): SolveReport<V>
        where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>,
              V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    return SolveReport(
        schemaVersion = schemaVersion,
        runId = runId,
        problemStatus = problemStatus,
        terminationReason = terminationReason,
        solutionPresence = solutionPresence,
        solution = solution?.let {
            SolveSolution(
                values = it.values.map(converter::intoValue),
                objective = it.objective?.let(converter::intoValue),
                pool = it.pool.map { values -> values.map(converter::intoValue) }
            )
        },
        diagnostics = SolveDiagnostics(
            constraintEvaluations = diagnostics.constraintEvaluations.map { evaluation ->
                ConstraintEvaluation(
                    constraintId = evaluation.constraintId,
                    lhs = converter.intoValue(evaluation.lhs),
                    rhs = converter.intoValue(evaluation.rhs),
                    relation = evaluation.relation,
                    slack = converter.intoValue(evaluation.slack),
                    violation = converter.intoValue(evaluation.violation),
                    tolerance = converter.intoValue(evaluation.tolerance),
                    satisfied = evaluation.satisfied,
                    dual = evaluation.dual?.let(converter::intoValue)
                )
            },
            variableBoundEvaluations = diagnostics.variableBoundEvaluations.map { evaluation ->
                VariableBoundEvaluation(
                    variableId = evaluation.variableId,
                    side = evaluation.side,
                    bound = converter.intoValue(evaluation.bound),
                    value = converter.intoValue(evaluation.value),
                    slack = converter.intoValue(evaluation.slack),
                    violation = converter.intoValue(evaluation.violation),
                    tolerance = converter.intoValue(evaluation.tolerance),
                    satisfied = evaluation.satisfied
                )
            },
        infeasibilityEvidence = diagnostics.infeasibilityEvidence,
        warnings = diagnostics.warnings,
        errors = diagnostics.errors
        ),
        proof = proof,
        statistics = statistics,
        provenance = provenance,
        fingerprints = fingerprints,
        attempts = attempts.map { attempt ->
            SolveAttemptTrace(
                attemptId = attempt.attemptId,
                parentAttemptId = attempt.parentAttemptId,
                backendId = attempt.backendId,
                report = attempt.report?.convertTo(converter),
                elapsed = attempt.elapsed,
                errors = attempt.errors,
                cancellationReason = attempt.cancellationReason
            )
        },
        selectedAttemptId = selectedAttemptId,
        selectionReason = selectionReason
    )
}
