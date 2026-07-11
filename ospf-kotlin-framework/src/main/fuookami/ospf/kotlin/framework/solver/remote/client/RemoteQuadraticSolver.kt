/**
 * 远程二次求解器
 * Remote quadratic solver
*/
package fuookami.ospf.kotlin.framework.solver.remote.client

import kotlin.time.Duration
import kotlinx.serialization.json.Json
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.QuadraticSolver
import fuookami.ospf.kotlin.framework.solver.remote.adapter.ospf.OspfRemoteModelSerializer
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 远程二次求解器。
 * Remote quadratic solver.
 *
 * @property remoteClient 远程客户端 / Remote client
 * @property resultStoragePort 结果对象存储 / Result object storage
 * @property runtimeConfig 运行配置 / Runtime config
*/
class RemoteQuadraticSolver(
    delegate: QuadraticSolver,
    private val remoteClient: RemoteSolverClient,
    private val resultStoragePort: ObjectStoragePort? = null,
    private val runtimeConfig: RemoteSolverRuntimeConfig = RemoteSolverRuntimeConfig(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) : QuadraticSolver by delegate {

    /**
     * 使用执行端口构造远程二次求解器。
     * Construct remote quadratic solver with execution port.
     *
     * @param delegate 本地求解器委托 / Local solver delegate
     * @param executionPort 求解执行端口 / Solve execution port
     * @param resultStoragePort 结果对象存储 / Result object storage
     * @param runtimeConfig 运行配置 / Runtime config
    */
    constructor(
        delegate: QuadraticSolver,
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
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
        return when (val result = solveRemote(
            payload = SolvePayload(
                modelData = OspfRemoteModelSerializer.modelData(model),
                taskMeta = TaskMeta(targetType = TargetTypeName.of("quadratic"))
            ),
            taskId = runtimeConfig.taskIdProvider(),
            sliceId = runtimeConfig.sliceIdProvider(),
            nodeId = runtimeConfig.nodeId,
            tenantId = runtimeConfig.tenantId,
            quantum = runtimeConfig.quantum,
            maxRounds = runtimeConfig.maxRounds
        )) {
            is Ok -> result.value.toFeasibleOutput(model.variables.size)
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
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
     * 执行远程二次求解。
     * Execute remote quadratic solve.
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
    ): Ret<SolveResult> {
        val normalizedPayload = payload.copy(
            taskMeta = payload.taskMeta.copy(
                targetType = payload.taskMeta.targetType ?: TargetTypeName.of("quadratic")
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

    /**
     * Converts the remote solve result to a feasible solver output.
     * 将远程求解结果转换为可行求解器输出。
     *
     * @param variableCount the expected number of variables / 预期变量数量
     * @return the feasible solver output or an error / 可行求解器输出或错误
    */
    private suspend fun SolveResult.toFeasibleOutput(variableCount: Int): Ret<FeasibleSolverOutput<Flt64>> {
        if (!feasible) {
            return Failed(Err(ErrorCode.ORModelInfeasible, message ?: "Remote quadratic solve is infeasible."))
        }
        val solution = readSerializedSolution()
            ?: return toEmptySolutionOutput(variableCount)
        if (!solution.feasible) {
            return Failed(Err(ErrorCode.ORModelInfeasible, solution.message ?: "Remote quadratic solve is infeasible."))
        }
        if (solution.variableValues.size != variableCount) {
            return Failed(
                Err(
                    ErrorCode.ORSolutionInvalid,
                    "Remote quadratic solution size ${solution.variableValues.size} does not match model variable count $variableCount."
                )
            )
        }
        val objective = solution.objectiveValue ?: objectiveValue
            ?: return Failed(Err(ErrorCode.ORSolutionInvalid, "Remote quadratic solution objective is missing."))
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

    /**
     * Converts the remote solve result to an empty solution output.
     * 将远程求解结果转换为空解输出。
     *
     * @param variableCount the expected number of variables / 预期变量数量
     * @return the feasible solver output or an error / 可行求解器输出或错误
    */
    private fun SolveResult.toEmptySolutionOutput(variableCount: Int): Ret<FeasibleSolverOutput<Flt64>> {
        if (variableCount != 0) {
            return Failed(
                Err(
                    ErrorCode.ORSolutionInvalid,
                    "Remote quadratic solve result does not contain a readable SerializedSolution resultRef."
                )
            )
        }
        val objective = objectiveValue
            ?: return Failed(Err(ErrorCode.ORSolutionInvalid, "Remote quadratic solution objective is missing."))
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

    /**
     * Reads the serialized solution from object storage.
     * 从对象存储读取序列化的求解结果。
     *
     * @return the serialized solution, or null if unavailable / 序列化求解结果，如果不可用则返回 null
    */
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

