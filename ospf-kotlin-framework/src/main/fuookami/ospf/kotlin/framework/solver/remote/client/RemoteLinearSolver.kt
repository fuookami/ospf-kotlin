/**
 * 远程线性求解器
 * Remote linear solver
 */
package fuookami.ospf.kotlin.framework.solver.remote.client

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import fuookami.ospf.kotlin.core.solver.LinearSolver
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.SolverExecutionPort
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * 远程线性求解器。
 * Remote linear solver.
 *
 * @property remoteClient 远程客户端 / Remote client
 */
class RemoteLinearSolver(
    delegate: LinearSolver,
    private val remoteClient: RemoteSolverClient
) : LinearSolver by delegate {
    /**
     * 使用执行端口构造远程线性求解器。
     * Construct remote linear solver with execution port.
     *
     * @param delegate 本地求解器委托 / Local solver delegate
     * @param executionPort 求解执行端口 / Solve execution port
     */
    constructor(delegate: LinearSolver, executionPort: SolverExecutionPort) : this(
        delegate = delegate,
        remoteClient = RemoteSolverClient(executionPort)
    )

    /**
     * 执行远程线性求解。
     * Execute remote linear solve.
     *
     * @param payload 求解载荷 / Solve payload
     * @param taskId 任务 ID / Task ID
     * @param sliceId 切片 ID / Slice ID
     * @param nodeId 节点 ID / Node ID
     * @param tenantId 租户 ID / Tenant ID
     * @param quantum 时间片 / Quantum
     * @param maxRounds 最大轮数 / Maximum rounds
     * @return 求解结果 / Solve result
     */
    suspend fun solveRemote(
        payload: SolvePayload,
        taskId: TaskId,
        sliceId: SliceId,
        nodeId: NodeId,
        tenantId: TenantId,
        quantum: Duration = payload.taskMeta.timeLimit ?: 4000.milliseconds,
        maxRounds: UInt64 = UInt64(64)
    ): SolveResult {
        val normalizedPayload = payload.copy(
            taskMeta = payload.taskMeta.copy(
                targetType = payload.taskMeta.targetType ?: TargetTypeName.of("linear")
            )
        )
        return remoteClient.solve(
            payload = normalizedPayload,
            taskId = taskId,
            sliceId = sliceId,
            nodeId = nodeId,
            tenantId = tenantId,
            quantum = quantum,
            maxRounds = maxRounds
        )
    }
}
