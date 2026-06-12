/**
 * 远程线性求解器
 * Remote linear solver
 */
package fuookami.ospf.kotlin.framework.solver.remote.client

import kotlin.time.Duration
import kotlinx.serialization.json.Json
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.solver.LinearSolver
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.framework.solver.remote.adapter.ospf.OspfRemoteModelSerializer
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.ObjectStoragePort
import fuookami.ospf.kotlin.framework.solver.remote.port.SolverExecutionPort

/**
 * 远程线性求解器。
 * Remote linear solver.
 *
 * @property remoteClient 远程客户端 / Remote client
 * @property resultStoragePort 结果对象存储 / Result object storage
 * @property runtimeConfig 运行配置 / Runtime config
 */
class RemoteLinearSolver(
    delegate: LinearSolver,
    private val remoteClient: RemoteSolverClient,
    private val resultStoragePort: ObjectStoragePort? = null,
    private val runtimeConfig: RemoteSolverRuntimeConfig = RemoteSolverRuntimeConfig(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) : LinearSolver by delegate {
    /**
     * 使用执行端口构造远程线性求解器。
     * Construct remote linear solver with execution port.
     *
     * @param delegate 本地求解器委托 / Local solver delegate
     * @param executionPort 求解执行端口 / Solve execution port
     * @param resultStoragePort 结果对象存储 / Result object storage
     * @param runtimeConfig 运行配置 / Runtime config
     */
    constructor(
        delegate: LinearSolver,
        executionPort: SolverExecutionPort,
        resultStoragePort: ObjectStoragePort? = null,
        runtimeConfig: RemoteSolverRuntimeConfig = RemoteSolverRuntimeConfig()
    ) : this(
        delegate = delegate,
        remoteClient = RemoteSolverClient(executionPort),
        resultStoragePort = resultStoragePort,
        runtimeConfig = runtimeConfig
    )

    override suspend fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
        val result = solveRemote(
            payload = SolvePayload(
                modelData = OspfRemoteModelSerializer.modelData(model),
                taskMeta = TaskMeta(targetType = TargetTypeName.of("linear"))
            ),
            taskId = runtimeConfig.taskIdProvider(),
            sliceId = runtimeConfig.sliceIdProvider(),
            nodeId = runtimeConfig.nodeId,
            tenantId = runtimeConfig.tenantId,
            quantum = runtimeConfig.quantum,
            maxRounds = runtimeConfig.maxRounds
        )
        return result.toFeasibleOutput(model.variables.size)
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
        return when (val result = invoke(model, solvingStatusCallBack)) {
            is Ok -> Ok(result.value to listOf(result.value.solution))
            is Failed -> Failed(result.error)
            else -> result.map { it to emptyList() }
        }
    }

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
        quantum: Duration = runtimeConfig.quantum,
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

    private suspend fun SolveResult.toFeasibleOutput(variableCount: Int): Ret<FeasibleSolverOutput<Flt64>> {
        if (!feasible) {
            return Failed(Err(ErrorCode.ORModelInfeasible, message ?: "Remote linear solve is infeasible."))
        }
        val solution = readSerializedSolution()
            ?: return toEmptySolutionOutput(variableCount)
        if (!solution.feasible) {
            return Failed(Err(ErrorCode.ORModelInfeasible, solution.message ?: "Remote linear solve is infeasible."))
        }
        if (solution.variableValues.size != variableCount) {
            return Failed(
                Err(
                    ErrorCode.ORSolutionInvalid,
                    "Remote linear solution size ${solution.variableValues.size} does not match model variable count $variableCount."
                )
            )
        }
        val objective = solution.objectiveValue ?: objectiveValue
            ?: return Failed(Err(ErrorCode.ORSolutionInvalid, "Remote linear solution objective is missing."))
        val solutionGap = solution.gap ?: gap ?: Flt64.zero
        return Ok(
            FeasibleSolverOutput(
                obj = objective,
                solution = solution.variableValues,
                time = solution.elapsed,
                possibleBestObj = objective,
                gap = solutionGap,
                mipGap = solutionGap,
                solveTime = solution.elapsed
            )
        )
    }

    private fun SolveResult.toEmptySolutionOutput(variableCount: Int): Ret<FeasibleSolverOutput<Flt64>> {
        if (variableCount != 0) {
            return Failed(
                Err(
                    ErrorCode.ORSolutionInvalid,
                    "Remote linear solve result does not contain a readable SerializedSolution resultRef."
                )
            )
        }
        val objective = objectiveValue
            ?: return Failed(Err(ErrorCode.ORSolutionInvalid, "Remote linear solution objective is missing."))
        val solutionGap = gap ?: Flt64.zero
        return Ok(
            FeasibleSolverOutput(
                obj = objective,
                solution = emptyList(),
                time = elapsed,
                possibleBestObj = objective,
                gap = solutionGap,
                mipGap = solutionGap,
                solveTime = elapsed
            )
        )
    }

    private suspend fun SolveResult.readSerializedSolution(): SerializedSolution? {
        val ref = resultRef ?: return null
        val storage = resultStoragePort ?: return null
        val bytes = storage.get(ref) ?: return null
        return json.decodeFromString(
            SerializedSolution.serializer(),
            bytes.decodeToString()
        )
    }
}
