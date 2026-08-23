@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.solver.remote.domain

import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 求解器类型。 / Solver type.
*/
@Serializable
enum class SolverType {
    /** SCIP 求解器 / SCIP solver */
    SCIP,

    /** Gurobi 求解器 / Gurobi solver */
    GUROBI,

    /** 自动选择 / Auto selection */
    AUTO
}

/** 远程问题结论 / Remote problem conclusion */
@Serializable
enum class RemoteProblemStatus {
    FEASIBLE,
    INFEASIBLE,
    UNBOUNDED,
    INFEASIBLE_OR_UNBOUNDED,
    UNKNOWN
}

/** 远程终止原因 / Remote termination reason */
@Serializable
enum class RemoteTerminationReason {
    COMPLETED,
    TIME_LIMIT,
    NODE_LIMIT,
    ITERATION_LIMIT,
    SOLUTION_LIMIT,
    OBJECTIVE_LIMIT,
    CANCELLED,
    INTERRUPTED,
    NUMERICAL_FAILURE,
    BACKEND_FAILURE
}

/** 远程解存在性 / Remote solution presence */
@Serializable
enum class RemoteSolutionPresence {
    NONE,
    INCUMBENT,
    OPTIMAL
}

/** 远程证明状态 / Remote proof status */
@Serializable
enum class RemoteProofStatus {
    NONE,
    CLAIMED,
    VERIFIED
}

/**
 * 远程求解器能力与协议版本摘要。 / Remote solver capability and protocol version summary.
 *
 * 客户端在提交 CP 载荷前使用该摘要确认服务端已声明对应协议和模型能力。
 * The client uses this summary before submitting a CP payload to verify that the server
 * advertises the required protocol and model capability.
 *
 * @property schemaVersion 能力摘要 schema 版本 / Capability summary schema version
 * @property protocolVersions 服务端支持的远程协议版本 / Remote protocol versions supported by the server
 * @property supportedModelTypes 服务端当前可调度的模型类型 / Model types currently schedulable by the server
 * @property supportsPortableCheckpoint 是否支持 portable checkpoint / Whether portable checkpoints are supported
 * @property supportsNativeCheckpoint 是否支持原生搜索状态恢复 / Whether native search-state resume is supported
 */
@Serializable
data class RemoteSolverCapabilities(
    val schemaVersion: String = "1.0",
    val protocolVersions: Set<String> = emptySet(),
    val supportedModelTypes: Set<String> = emptySet(),
    val supportsPortableCheckpoint: Boolean = false,
    val supportsNativeCheckpoint: Boolean = false
)

/**
 * 执行句柄。 / Execution handle.
 *
 * @property handleId 句柄 ID / Handle ID
 * @property taskId 任务 ID / Task ID
 * @property sliceId 切片 ID / Slice ID
 * @property nodeId 节点 ID / Node ID
 * @property startedAt 启动时间戳 / Started timestamp
*/
@Serializable
data class ExecutionHandle(
    val handleId: HandleId,
    val taskId: TaskId,
    val sliceId: SliceId,
    val nodeId: NodeId,
    @SerialName("startedAtEpochMs")
    @Serializable(with = RemoteSolverEpochMillisecondsInstantSerializer::class)
    val startedAt: Instant
)

/**
 * 切片结果。 / Slice result.
 *
 * @property sliceId 切片 ID / Slice ID
 * @property completed 是否完成 / Whether completed
 * @property feasible 是否可行 / Whether feasible
 * @property objectiveValue 目标值 / Objective value
 * @property objectiveValueInt64 CP 精确整数目标值 / Exact Int64 CP objective value
 * @property gap 最优间隙 / Optimality gap
 * @property elapsed 耗时 / Elapsed
 * @property message 结果消息 / Result message
 * @property schemaVersion 结果协议版本 / Result protocol version
 * @property problemStatus 正交问题结论 / Orthogonal problem conclusion
 * @property terminationReason 正交终止原因 / Orthogonal termination reason
 * @property solutionPresence 解存在性 / Solution presence
 * @property proofStatus 证明状态 / Proof status
 * @property resultRef 结果对象引用 / Result artifact reference
 * @property provenance 脱敏执行来源 / Redacted execution provenance
 * @property fingerprints 审计指纹 / Audit fingerprints
 * @property fingerprintSchemas 指纹 schema / Fingerprint schemas
 * @property statistics 求解统计 / Solve statistics
 * @property diagnostics 结构化诊断 / Structured diagnostics
 * @property runId 求解运行标识 / Solve run identifier
 * @property attemptId 求解尝试标识 / Solve attempt identifier
 * @property artifactDigest 结果 artifact 摘要 / Result artifact digest
*/
@Serializable
data class SliceResult(
    val sliceId: SliceId,
    val completed: Boolean,
    val feasible: Boolean,
    val objectiveValue: Flt64?,
    val gap: Flt64?,
    @SerialName("elapsedMs")
    @Serializable(with = RemoteSolverMillisecondsDurationSerializer::class)
    val elapsed: Duration,
    val message: String? = null,
    val schemaVersion: String = "1.0",
    val problemStatus: RemoteProblemStatus = if (feasible) {
        RemoteProblemStatus.FEASIBLE
    } else {
        RemoteProblemStatus.UNKNOWN
    },
    val terminationReason: RemoteTerminationReason = RemoteTerminationReason.COMPLETED,
    val solutionPresence: RemoteSolutionPresence = when {
        feasible -> RemoteSolutionPresence.INCUMBENT
        else -> RemoteSolutionPresence.NONE
    },
    val proofStatus: RemoteProofStatus = RemoteProofStatus.NONE,
    val resultRef: ObjectRef? = null,
    val provenance: Map<String, String> = emptyMap(),
    val fingerprints: Map<String, String> = emptyMap(),
    val fingerprintSchemas: Map<String, String> = emptyMap(),
    val statistics: Map<String, String> = emptyMap(),
    val diagnostics: Map<String, String> = emptyMap(),
    val runId: String? = null,
    val attemptId: String? = null,
    val artifactDigest: String? = null,
    val objectiveValueInt64: Long? = null
)

/**
 * 求解结果。 / Solve result.
 *
 * @property feasible 是否可行 / Whether feasible
 * @property optimal 是否最优 / Whether optimal
 * @property objectiveValue 目标值 / Objective value
 * @property objectiveValueInt64 CP 精确整数目标值 / Exact Int64 CP objective value
 * @property gap 最优间隙 / Optimality gap
 * @property elapsed 总耗时 / Total elapsed
 * @property checkpointRef 检查点引用 / Checkpoint reference
 * @property resultRef 结果对象引用 / Result object reference
 * @property message 结果消息 / Result message
 * @property extension 扩展字段 / Extension fields
 * @property schemaVersion 报告协议版本 / Report protocol version
 * @property problemStatus 正交问题结论 / Orthogonal problem conclusion
 * @property terminationReason 正交终止原因 / Orthogonal termination reason
 * @property solutionPresence 解存在性 / Solution presence
 * @property proofStatus 证明状态 / Proof status
 * @property provenance 脱敏执行来源 / Redacted execution provenance
 * @property fingerprints 审计指纹 / Audit fingerprints
 * @property fingerprintSchemas 指纹 schema / Fingerprint schemas
 * @property statistics 求解统计 / Solve statistics
 * @property diagnostics 结构化诊断 / Structured diagnostics
 * @property runId 求解运行标识 / Solve run identifier
 * @property attemptId 求解尝试标识 / Solve attempt identifier
 * @property artifactDigest 结果 artifact 摘要 / Result artifact digest
*/
@Serializable
data class SolveResult(
    val feasible: Boolean,
    val optimal: Boolean,
    val objectiveValue: Flt64?,
    val gap: Flt64?,
    @SerialName("elapsedMs")
    @Serializable(with = RemoteSolverMillisecondsDurationSerializer::class)
    val elapsed: Duration,
    val checkpointRef: ObjectRef? = null,
    val resultRef: ObjectRef? = null,
    val message: String? = null,
    val extension: Map<String, String> = emptyMap(),
    val schemaVersion: String = "1.0",
    val problemStatus: RemoteProblemStatus = if (feasible) {
        RemoteProblemStatus.FEASIBLE
    } else {
        RemoteProblemStatus.INFEASIBLE
    },
    val terminationReason: RemoteTerminationReason = RemoteTerminationReason.COMPLETED,
    val solutionPresence: RemoteSolutionPresence = when {
        optimal -> RemoteSolutionPresence.OPTIMAL
        feasible -> RemoteSolutionPresence.INCUMBENT
        else -> RemoteSolutionPresence.NONE
    },
    val proofStatus: RemoteProofStatus = RemoteProofStatus.NONE,
    val provenance: Map<String, String> = emptyMap(),
    val fingerprints: Map<String, String> = emptyMap(),
    val fingerprintSchemas: Map<String, String> = emptyMap(),
    val statistics: Map<String, String> = emptyMap(),
    val diagnostics: Map<String, String> = emptyMap(),
    val runId: String? = null,
    val attemptId: String? = null,
    val artifactDigest: String? = null,
    val objectiveValueInt64: Long? = null
)
