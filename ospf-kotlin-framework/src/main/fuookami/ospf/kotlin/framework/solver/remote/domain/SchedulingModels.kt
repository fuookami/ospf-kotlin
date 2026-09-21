@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.solver.remote.domain

import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 调度器可停止运行中求解切片的方式。 / How a running solver slice may be stopped by the scheduler.
 */
@Serializable
enum class PreemptionMode {
    NON_PREEMPTIBLE,
    CONTROLLED_RETURN,
    NATIVE,
    UNKNOWN
}

/**
 * 后续切片继续前一切片的方式。 / How a later slice can continue a previous slice.
 */
@Serializable
enum class ResumeMode {
    NONE,
    WARM_START,
    BASIS,
    NATIVE_CHECKPOINT,
    UNKNOWN
}

/**
 * 调度器切片的明确结果。 / Explicit result of a scheduler slice.
 */
@Serializable
enum class SliceOutcome {
    COMPLETED,
    PREEMPTED,
    CHECKPOINTED,
    RESUMABLE,
    CANCELLED,
    FAILED,
    UNKNOWN
}

/**
 * 用于调度计费的估算值，时长按毫秒编码。 / Estimates used for scheduler accounting; durations are encoded as milliseconds.
 *
 * @property runtime 预估运行时长 / Estimated runtime
 * @property checkpoint 预估检查点时长 / Estimated checkpoint duration
 * @property queueWait 预估排队等待时长 / Estimated queue-wait duration
 * @property cost 预估成本 / Estimated cost
 */
@Serializable
data class SchedulingEstimate(
    @SerialName("estimatedRuntimeMs")
    @Serializable(with = RemoteSolverMillisecondsDurationSerializer::class)
    val runtime: Duration? = null,
    @SerialName("estimatedCheckpointMs")
    @Serializable(with = RemoteSolverMillisecondsDurationSerializer::class)
    val checkpoint: Duration? = null,
    @SerialName("estimatedQueueWaitMs")
    @Serializable(with = RemoteSolverMillisecondsDurationSerializer::class)
    val queueWait: Duration? = null,
    val cost: Flt64? = null
)

/**
 * 调度与执行共用的、与求解器无关的质量目标。 / Solver-neutral quality target used by scheduling and execution.
 *
 * @property maxGap 最大允许间隙 / Maximum allowed gap
 * @property objectiveLimit 目标值限制 / Objective limit
 * @property requireFeasible 是否要求可行解 / Whether a feasible solution is required
 * @property requireOptimal 是否要求最优解 / Whether an optimal solution is required
 * @property metadata 扩展元数据 / Extension metadata
 */
@Serializable
data class QualityTarget(
    val maxGap: Flt64? = null,
    val objectiveLimit: Flt64? = null,
    val requireFeasible: Boolean? = null,
    val requireOptimal: Boolean? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 附加到求解载荷的可选调度要求。 / Optional scheduling requirements attached to a solve payload.
 *
 * @property complexity 任务复杂度 / Task complexity
 * @property timeSensitivity 时间敏感度 / Time sensitivity
 * @property priority 优先级 / Priority
 * @property deadline 截止时间 / Deadline
 * @property budgetScope 预算范围 / Budget scope
 * @property budgetLimit 预算上限 / Budget limit
 * @property estimate 调度估算值 / Scheduling estimate
 * @property modelFingerprint 模型指纹 / Model fingerprint
 * @property modelFingerprintSchema 模型指纹 schema / Model fingerprint schema
 * @property checkpointRef 检查点引用 / Checkpoint reference
 * @property incumbentRef incumbent 引用 / Incumbent reference
 * @property qualityTarget 质量目标 / Quality target
 * @property preemptionMode 抢占模式 / Preemption mode
 * @property resumeMode 恢复模式 / Resume mode
 * @property metadata 扩展元数据 / Extension metadata
 */
@Serializable
data class SchedulingRequest(
    val complexity: TaskComplexity? = null,
    val timeSensitivity: TimeSensitivity? = null,
    val priority: Int? = null,
    @SerialName("deadlineEpochMs")
    @Serializable(with = RemoteSolverEpochMillisecondsInstantSerializer::class)
    val deadline: Instant? = null,
    val budgetScope: BudgetScopeId? = null,
    val budgetLimit: Flt64? = null,
    val estimate: SchedulingEstimate? = null,
    val modelFingerprint: String? = null,
    val modelFingerprintSchema: String? = null,
    val checkpointRef: ObjectRef? = null,
    val incumbentRef: ObjectRef? = null,
    val qualityTarget: QualityTarget? = null,
    val preemptionMode: PreemptionMode? = null,
    val resumeMode: ResumeMode? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 随切片或最终结果返回的有效调度信息。 / Effective scheduling information returned with a slice or final result.
 *
 * @property dispatchId 分发操作 ID / Dispatch operation ID
 * @property taskId 任务 ID / Task ID
 * @property sliceId 切片 ID / Slice ID
 * @property nodeId 节点 ID / Node ID
 * @property priority 优先级 / Priority
 * @property deadline 截止时间 / Deadline
 * @property budgetScope 预算范围 / Budget scope
 * @property budgetLimit 预算上限 / Budget limit
 * @property quantum 调度时间片 / Scheduling quantum
 * @property queueWait 排队等待时长 / Queue-wait duration
 * @property estimate 调度估算值 / Scheduling estimate
 * @property modelFingerprint 模型指纹 / Model fingerprint
 * @property modelFingerprintSchema 模型指纹 schema / Model fingerprint schema
 * @property checkpointRef 检查点引用 / Checkpoint reference
 * @property incumbentRef incumbent 引用 / Incumbent reference
 * @property qualityTarget 质量目标 / Quality target
 * @property preemptionMode 抢占模式 / Preemption mode
 * @property resumeMode 恢复模式 / Resume mode
 * @property outcome 切片结果 / Slice outcome
 * @property reason 调度原因 / Scheduling reason
 * @property metadata 扩展元数据 / Extension metadata
 */
@Serializable
data class SchedulingDecision(
    val dispatchId: DispatchId? = null,
    val taskId: TaskId? = null,
    val sliceId: SliceId? = null,
    val nodeId: NodeId? = null,
    val priority: Int? = null,
    @SerialName("deadlineEpochMs")
    @Serializable(with = RemoteSolverEpochMillisecondsInstantSerializer::class)
    val deadline: Instant? = null,
    val budgetScope: BudgetScopeId? = null,
    val budgetLimit: Flt64? = null,
    @SerialName("quantumMs")
    @Serializable(with = RemoteSolverMillisecondsDurationSerializer::class)
    val quantum: Duration? = null,
    @SerialName("queueWaitMs")
    @Serializable(with = RemoteSolverMillisecondsDurationSerializer::class)
    val queueWait: Duration? = null,
    val estimate: SchedulingEstimate? = null,
    val modelFingerprint: String? = null,
    val modelFingerprintSchema: String? = null,
    val checkpointRef: ObjectRef? = null,
    val incumbentRef: ObjectRef? = null,
    val qualityTarget: QualityTarget? = null,
    val preemptionMode: PreemptionMode? = null,
    val resumeMode: ResumeMode? = null,
    val outcome: SliceOutcome? = null,
    val reason: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
