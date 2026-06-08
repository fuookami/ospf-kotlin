/**
 * 远程求解执行端口
 * Remote solve execution port
 */
package fuookami.ospf.kotlin.framework.solver.remote.port

import kotlin.time.Duration
import fuookami.ospf.kotlin.framework.solver.remote.domain.*

/**
 * 求解器执行端口。
 * Solver execution port.
 */
interface SolverExecutionPort {
    /**
     * 启动新求解任务。
     * Start a new solve task.
     *
     * @param payload 求解载荷 / Solve payload
     * @param taskId 任务 ID / Task ID
     * @param sliceId 切片 ID / Slice ID
     * @param nodeId 节点 ID / Node ID
     * @param tenantId 租户 ID / Tenant ID
     * @return 执行句柄 / Execution handle
     */
    suspend fun start(
        payload: SolvePayload,
        taskId: TaskId,
        sliceId: SliceId,
        nodeId: NodeId,
        tenantId: TenantId
    ): ExecutionHandle

    /**
     * 从检查点恢复求解。
     * Resume solve from checkpoint.
     *
     * @param payload 求解载荷 / Solve payload
     * @param checkpoint 检查点引用 / Checkpoint reference
     * @param taskId 任务 ID / Task ID
     * @param sliceId 切片 ID / Slice ID
     * @param nodeId 节点 ID / Node ID
     * @param tenantId 租户 ID / Tenant ID
     * @return 执行句柄 / Execution handle
     */
    suspend fun resume(
        payload: SolvePayload,
        checkpoint: ObjectRef,
        taskId: TaskId,
        sliceId: SliceId,
        nodeId: NodeId,
        tenantId: TenantId
    ): ExecutionHandle

    /**
     * 等待切片结束。
     * Wait for slice end.
     *
     * @param handle 执行句柄 / Execution handle
     * @param quantum 时间片 / Quantum
     * @return 切片结果 / Slice result
     */
    suspend fun awaitSliceEnd(handle: ExecutionHandle, quantum: Duration): SliceResult

    /**
     * 导出检查点。
     * Export checkpoint.
     *
     * @param handle 执行句柄 / Execution handle
     * @return 检查点引用 / Checkpoint reference
     */
    suspend fun exportCheckpoint(handle: ExecutionHandle): ObjectRef?

    /**
     * 获取最终结果。
     * Fetch final result.
     *
     * @param handle 执行句柄 / Execution handle
     * @return 最终结果 / Final result
     */
    suspend fun fetchFinalResult(handle: ExecutionHandle): SolveResult?

    /**
     * 停止执行。
     * Stop execution.
     *
     * @param handle 执行句柄 / Execution handle
     * @return 是否停止成功 / Whether stopped
     */
    suspend fun stop(handle: ExecutionHandle): Boolean
}
