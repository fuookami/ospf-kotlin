/**
 * 远程求解器客户端
 * Remote solver client
 */
package fuookami.ospf.kotlin.framework.solver.remote.client

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.SolverExecutionPort

/**
 * 远程求解器客户端。
 * Remote solver client.
 *
 * @property executionPort 求解执行端口 / Solve execution port
 */
class RemoteSolverClient(
    private val executionPort: SolverExecutionPort
) {
    /**
     * 执行远程求解。
     * Execute remote solve.
     *
     * @param payload 求解载荷 / Solve payload
     * @param taskId 任务 ID / Task ID
     * @param sliceId 切片 ID / Slice ID
     * @param nodeId 节点 ID / Node ID
     * @param tenantId 租户 ID / Tenant ID
     * @param quantum 时间片 / Quantum
     * @param maxRounds 最大轮数 / Maximum rounds
     * @param exportCheckpointEachRound 是否每轮导出检查点 / Whether export checkpoint each round
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
        exportCheckpointEachRound: Boolean = true
    ): SolveResult {
        require(quantum > Duration.ZERO) { "quantum must be positive." }
        require(maxRounds > UInt64.zero) { "maxRounds must be positive." }

        val handle = payload.snapshotRef?.let {
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
        )

        var totalElapsed = Duration.ZERO
        var latestCheckpoint: ObjectRef? = payload.snapshotRef
        var rounds = UInt64.zero
        var finalResult: SolveResult? = null
        try {
            while (rounds < maxRounds) {
                rounds += UInt64.one
                val sliceResult = executionPort.awaitSliceEnd(
                    handle = handle,
                    quantum = quantum
                )
                totalElapsed += sliceResult.elapsed
                if (exportCheckpointEachRound) {
                    latestCheckpoint = executionPort.exportCheckpoint(handle) ?: latestCheckpoint
                }
                if (sliceResult.completed) {
                    finalResult = executionPort.fetchFinalResult(handle) ?: SolveResult(
                        feasible = sliceResult.feasible,
                        optimal = (sliceResult.gap ?: Flt64.one).toDouble() <= 0.0,
                        objectiveValue = sliceResult.objectiveValue,
                        gap = sliceResult.gap,
                        elapsed = totalElapsed,
                        checkpointRef = latestCheckpoint,
                        message = sliceResult.message
                    )
                    break
                }
            }
        } finally {
            runCatching {
                executionPort.stop(handle)
            }
        }

        return finalResult ?: throw RemoteSolverException(
            code = RemoteSolverErrorCode.REMOTE_SOLVE_NOT_COMPLETED_WITHIN_MAX_ROUNDS,
            message = "Remote solve does not complete within maxRounds=$maxRounds (taskId=$taskId, sliceId=$sliceId).",
            metadata = mapOf(
                "taskId" to taskId.value,
                "sliceId" to sliceId.value,
                "maxRounds" to maxRounds.toString()
            )
        )
    }
}
