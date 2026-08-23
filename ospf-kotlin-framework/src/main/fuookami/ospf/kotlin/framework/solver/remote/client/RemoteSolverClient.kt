/**
 * 远程求解器客户端 / Remote solver client
*/
package fuookami.ospf.kotlin.framework.solver.remote.client

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.solver.report.CancellationRecord
import fuookami.ospf.kotlin.core.solver.report.CancellationToken
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.SolverExecutionPort

/**
 * 远程求解器客户端。 / Remote solver client.
 *
 * @property executionPort 求解执行端口 / Solve execution port
*/
class RemoteSolverClient(
    private val executionPort: SolverExecutionPort
) {

    /**
     * 执行远程求解。 / Execute remote solve.
     *
     * @param payload 求解载荷 / Solve payload
     * @param taskId 任务 ID / Task ID
     * @param sliceId 切片 ID / Slice ID
     * @param nodeId 节点 ID / Node ID
     * @param tenantId 租户 ID / Tenant ID
     * @param quantum 时间片 / Quantum
     * @param maxRounds 最大轮数 / Maximum rounds
     * @param exportCheckpointEachRound 是否每轮导出检查点 / Whether export checkpoint each round
     * @param cancellationToken 本地取消令牌 / Local cancellation token
     * @return 求解结果 / Solve result
    */
    suspend fun solve(
        payload: SolvePayload,
        taskId: TaskId,
        sliceId: SliceId,
        nodeId: NodeId,
        tenantId: TenantId,
        quantum: Duration,
        maxRounds: UInt64 = UInt64(64),
        exportCheckpointEachRound: Boolean = true,
        cancellationToken: CancellationToken? = null
    ): Ret<SolveResult> {
        if (quantum <= Duration.ZERO) {
            return Failed(ErrorCode.IllegalArgument, "quantum must be positive.")
        }
        if (maxRounds <= UInt64.zero) {
            return Failed(ErrorCode.IllegalArgument, "maxRounds must be positive.")
        }
        if (cancellationToken?.isCancellationRequested == true) {
            return Ok(cancelledResult(cancellationToken.record?.reason))
        }

        val handle = when (val result = payload.snapshotRef?.let {
            executionPort.resume(
                payload = payload,
                checkpoint = it,
                taskId = taskId,
                sliceId = sliceId,
                nodeId = nodeId,
                tenantId = tenantId
            )
        } ?: executionPort.start(
            payload = payload,
            taskId = taskId,
            sliceId = sliceId,
            nodeId = nodeId,
            tenantId = tenantId
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val stopRequested = AtomicBoolean(false)
        suspend fun stopOnce(): Ret<Boolean> {
            return if (stopRequested.compareAndSet(false, true)) {
                executionPort.stop(handle)
            } else {
                Ok(true)
            }
        }

        var totalElapsed = Duration.ZERO
        var latestCheckpoint: ObjectRef? = payload.snapshotRef
        var latestSlice: SliceResult? = null
        var rounds = UInt64.zero
        var finalResult: SolveResult? = null
        var completed = false
        val cancellationListener: ((CancellationRecord) -> Try)? = cancellationToken?.let {
            {
                try {
                    when (val result = runBlocking { stopOnce() }) {
                        is Ok -> ok
                        is Failed -> Failed(result.error)
                        is Fatal -> Fatal(result.errors)
                    }
                } catch (error: Exception) {
                    Failed(
                        ErrorCode.ApplicationError,
                        "远程 stop 失败：${error.message ?: error::class.simpleName} / " +
                            "Remote stop failed: ${error.message ?: error::class.simpleName}"
                    )
                }
            }
        }
        var cancellationListenerRegistered = false
        fun unregisterCancellationListener() {
            if (cancellationListenerRegistered) {
                cancellationToken?.let { token ->
                    cancellationListener?.let { listener -> token.unregister(listener) }
                }
                cancellationListenerRegistered = false
            }
        }
        cancellationListener?.let { listener ->
            when (val registered = cancellationToken!!.register(listener)) {
                is Ok -> cancellationListenerRegistered = true
                is Failed -> {
                    stopOnce()
                    return Failed(registered.error)
                }
                is Fatal -> {
                    stopOnce()
                    return Fatal(registered.errors)
                }
            }
        }

        try {
            suspend fun cancelled(): Ret<SolveResult> {
                unregisterCancellationListener()
                when (val stopped = stopOnce()) {
                    is Failed -> return Failed(stopped.error)
                    is Fatal -> return Fatal(stopped.errors)
                    is Ok -> {}
                }
                return Ok(cancelledResult(
                    reason = cancellationToken?.record?.reason,
                    elapsed = totalElapsed,
                    checkpoint = latestCheckpoint,
                    slice = latestSlice
                ))
            }

            while (rounds < maxRounds) {
                if (cancellationToken?.isCancellationRequested == true) {
                    return cancelled()
                }
                rounds += UInt64.one
                val sliceResult = when (val result = executionPort.awaitSliceEnd(
                    handle = handle,
                    quantum = quantum
                )) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                latestSlice = sliceResult
                totalElapsed += sliceResult.elapsed
                if (cancellationToken?.isCancellationRequested == true) {
                    return cancelled()
                }
                if (exportCheckpointEachRound) {
                    latestCheckpoint = when (val result = executionPort.exportCheckpoint(handle)) {
                        is Ok -> result.value ?: latestCheckpoint
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                }
                if (sliceResult.completed) {
                    val fetchedResult = when (val result = executionPort.fetchFinalResult(handle)) {
                        is Ok -> result.value
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                    finalResult = fetchedResult ?: SolveResult(
                        feasible = sliceResult.feasible,
                        optimal = sliceResult.solutionPresence == RemoteSolutionPresence.OPTIMAL &&
                            sliceResult.proofStatus != RemoteProofStatus.NONE,
                        objectiveValue = sliceResult.objectiveValue,
                        objectiveValueInt64 = sliceResult.objectiveValueInt64,
                        gap = sliceResult.gap,
                        elapsed = totalElapsed,
                        checkpointRef = latestCheckpoint,
                        message = sliceResult.message,
                        schemaVersion = sliceResult.schemaVersion,
                        problemStatus = sliceResult.problemStatus,
                        terminationReason = sliceResult.terminationReason,
                        solutionPresence = sliceResult.solutionPresence,
                        proofStatus = sliceResult.proofStatus,
                        resultRef = sliceResult.resultRef,
                        provenance = sliceResult.provenance,
                        fingerprints = sliceResult.fingerprints,
                        fingerprintSchemas = sliceResult.fingerprintSchemas,
                        statistics = sliceResult.statistics,
                        diagnostics = sliceResult.diagnostics,
                        runId = sliceResult.runId,
                        attemptId = sliceResult.attemptId,
                        artifactDigest = sliceResult.artifactDigest
                    )
                    completed = true
                    break
                }
            }
            unregisterCancellationListener()
            when (val stopped = stopOnce()) {
                is Failed -> if (!completed) return Failed(stopped.error)
                is Fatal -> if (!completed) return Fatal(stopped.errors)
                is Ok -> {}
            }

            return finalResult?.let { Ok(it) } ?: Failed(
                ExErr(
                    code = ErrorCode.ApplicationFailed,
                    message = "Remote solve does not complete within maxRounds=$maxRounds (taskId=$taskId, sliceId=$sliceId).",
                    value = RemoteSolverFailureDetail(
                        code = RemoteSolverErrorCode.REMOTE_SOLVE_NOT_COMPLETED_WITHIN_MAX_ROUNDS,
                        message = "Remote solve does not complete within maxRounds=$maxRounds.",
                        metadata = mapOf(
                            "taskId" to taskId.value,
                            "sliceId" to sliceId.value,
                            "maxRounds" to maxRounds.toString()
                        ),
                        taskId = taskId.value,
                        sliceId = sliceId.value
                    )
                )
            )
        } finally {
            unregisterCancellationListener()
            if (!completed) {
                stopOnce()
            }
        }
    }

    private fun cancelledResult(
        reason: String? = null,
        elapsed: Duration = Duration.ZERO,
        checkpoint: ObjectRef? = null,
        slice: SliceResult? = null
    ): SolveResult {
        return SolveResult(
            feasible = slice?.feasible == true,
            optimal = false,
            objectiveValue = slice?.objectiveValue,
            objectiveValueInt64 = slice?.objectiveValueInt64,
            gap = slice?.gap,
            elapsed = elapsed,
            checkpointRef = checkpoint,
            message = reason?.let { "远程求解已取消：$it / Remote solve cancelled: $it" }
                ?: "远程求解已取消 / Remote solve cancelled",
            schemaVersion = slice?.schemaVersion ?: "2.0",
            problemStatus = slice?.problemStatus ?: RemoteProblemStatus.UNKNOWN,
            terminationReason = RemoteTerminationReason.CANCELLED,
            solutionPresence = if (slice?.feasible == true) {
                RemoteSolutionPresence.INCUMBENT
            } else {
                RemoteSolutionPresence.NONE
            },
            proofStatus = RemoteProofStatus.NONE,
            provenance = slice?.provenance ?: emptyMap(),
            fingerprints = slice?.fingerprints ?: emptyMap(),
            fingerprintSchemas = slice?.fingerprintSchemas ?: emptyMap(),
            statistics = slice?.statistics ?: emptyMap(),
            diagnostics = slice?.diagnostics ?: emptyMap(),
            runId = slice?.runId,
            attemptId = slice?.attemptId,
            artifactDigest = slice?.artifactDigest
        )
    }
}
