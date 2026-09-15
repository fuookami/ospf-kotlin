@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.solver.remote.domain

import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/** How a running solver slice may be stopped by the scheduler. */
@Serializable
enum class PreemptionMode {
    NON_PREEMPTIBLE,
    CONTROLLED_RETURN,
    NATIVE,
    UNKNOWN
}

/** How a later slice can continue a previous slice. */
@Serializable
enum class ResumeMode {
    NONE,
    WARM_START,
    BASIS,
    NATIVE_CHECKPOINT,
    UNKNOWN
}

/** Explicit result of a scheduler slice. */
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

/** Estimates used for scheduler accounting. Durations are encoded as milliseconds. */
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

/** Solver-neutral quality target used by scheduling and execution. */
@Serializable
data class QualityTarget(
    val maxGap: Flt64? = null,
    val objectiveLimit: Flt64? = null,
    val requireFeasible: Boolean? = null,
    val requireOptimal: Boolean? = null,
    val metadata: Map<String, String> = emptyMap()
)

/** Optional scheduling requirements attached to a solve payload. */
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

/** Effective scheduling information returned with a slice or final result. */
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
