/**
 * 远程线性求解器 / Remote linear solver
*/
package fuookami.ospf.kotlin.framework.solver.remote.client

import kotlin.time.Duration
import kotlinx.serialization.json.Json
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.solver.LinearSolver
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.progress.SolverProgressContext
import fuookami.ospf.kotlin.framework.solver.remote.adapter.ospf.OspfRemoteModelSerializer
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.*

/**
 * 远程线性求解器。 / Remote linear solver.
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
     * 使用执行端口构造远程线性求解器。 / Construct remote linear solver with execution port.
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
    ): Ret<SolveReport<Flt64>> {
        return invoke(model, solvingStatusCallBack, null)
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?,
        cancellationToken: CancellationToken?
    ): Ret<SolveReport<Flt64>> {
        return when (val result = executeRemote(model, cancellationToken)) {
            is Ok -> result.value.toSolveReport(model.variables.size)
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override suspend fun solveReport(
        model: LinearTriadModelView,
        progressContext: SolverProgressContext?
    ): Ret<SolveReport<Flt64>> {
        return solveReport(model, progressContext, null)
    }

    override suspend fun solveReport(
        model: LinearTriadModelView,
        progressContext: SolverProgressContext?,
        cancellationToken: CancellationToken?
    ): Ret<SolveReport<Flt64>> {
        return when (val result = executeRemote(model, cancellationToken)) {
            is Ok -> result.value.toSolveReport(model.variables.size)
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    private suspend fun executeRemote(
        model: LinearTriadModelView,
        cancellationToken: CancellationToken?
    ): Ret<SolveResult> {
        when (val validation = model.identityValidation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        val modelData = when (val serialized = OspfRemoteModelSerializer.modelData(model)) {
            is Ok -> serialized.value
            is Failed -> return Failed(serialized.error)
            is Fatal -> return Fatal(serialized.errors)
        }
        return solveRemote(
            payload = SolvePayload(
                modelData = modelData,
                taskMeta = TaskMeta(targetType = TargetTypeName.of("linear"))
            ),
            taskId = runtimeConfig.taskIdProvider(),
            sliceId = runtimeConfig.sliceIdProvider(),
            nodeId = runtimeConfig.nodeId,
            tenantId = runtimeConfig.tenantId,
            quantum = runtimeConfig.quantum,
            maxRounds = runtimeConfig.maxRounds,
            cancellationToken = cancellationToken
        )
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        return invoke(model, solutionAmount, solvingStatusCallBack, null)
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?,
        cancellationToken: CancellationToken?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        return when (val result = executeRemote(model, cancellationToken)) {
            is Ok -> {
                when (val report = result.value.toSolveReport(model.variables.size)) {
                    is Ok -> Ok(report.value to if (report.value.solution == null) emptyList() else listOf(report.value.values))
                    is Failed -> Failed(report.error)
                    is Fatal -> Fatal(report.errors)
                }
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 执行远程线性求解。 / Execute remote linear solve.
     *
     * @param payload 求解载荷 / Solve payload
     * @param taskId 任务 ID / Task ID
     * @param sliceId 切片 ID / Slice ID
     * @param nodeId 节点 ID / Node ID
     * @param tenantId 租户 ID / Tenant ID
     * @param quantum 时间片 / Quantum
     * @param maxRounds 最大轮数 / Maximum rounds
     * @param cancellationToken 本地取消令牌 / Local cancellation token
     * @return 求解结果 / Solve result
    */
    suspend fun solveRemote(
        payload: SolvePayload,
        taskId: TaskId,
        sliceId: SliceId,
        nodeId: NodeId,
        tenantId: TenantId,
        quantum: Duration = runtimeConfig.quantum,
        maxRounds: UInt64 = UInt64(64),
        cancellationToken: CancellationToken? = null
    ): Ret<SolveResult> {
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
            maxRounds = maxRounds,
            cancellationToken = cancellationToken
        )
    }

    /**
     * Converts the remote incumbent artifact to a partial solve report.
     * 将远程 incumbent artifact 转换为部分求解报告。
     *
     * @param variableCount 变量数量 / Variable count
     * @return 部分求解报告或错误 / Partial solve report or error
    */
    private suspend fun SolveResult.toIncumbentReport(variableCount: Int): Ret<SolveReport<Flt64>> {
        when (val validation = validateLinearQuadraticResult()) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        if (!feasible) {
            return Failed(Err(ErrorCode.ORModelInfeasible, message ?: "Remote linear solve is infeasible."))
        }
        val solution = when (val serialized = readSerializedSolution()) {
            is Ok -> serialized.value ?: return toEmptySolutionOutput(variableCount)
            is Failed -> return Failed(serialized.error)
            is Fatal -> return Fatal(serialized.errors)
        }
        when (val agreement = validateLinearQuadraticArtifact(solution)) {
            is Ok -> {}
            is Failed -> return Failed(agreement.error)
            is Fatal -> return Fatal(agreement.errors)
        }
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
        val solutionGap = solution.gap ?: gap
        val reportedBestBound = solution.statistics.remoteFlt64("bestBound")
            ?: statistics.remoteFlt64("bestBound")
        return Ok(
            SolveReport(
                problemStatus = problemStatus.toCoreStatus(),
                terminationReason = terminationReason.toCoreReason(),
                solutionPresence = solutionPresence.toCorePresence(),
                solution = SolveSolution(
                    values = solution.variableValues,
                    objective = objective
                ),
                proof = SolveProof(proofStatus.toCoreProof()),
                statistics = SolveStatistics(
                    solveTime = solution.elapsed,
                    bestBound = reportedBestBound,
                    gap = solutionGap
                )
            )
        )
    }

    private suspend fun SolveResult.toSolveReport(variableCount: Int): Ret<SolveReport<Flt64>> {
        when (val validation = validateLinearQuadraticResult()) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        val output = if (solutionPresence != RemoteSolutionPresence.NONE) {
            when (val incumbentReport = toIncumbentReport(variableCount)) {
                is Ok -> incumbentReport.value
                is Failed -> return Failed(incumbentReport.error)
                is Fatal -> return Fatal(incumbentReport.errors)
            }
        } else {
            null
        }
        return toRemoteSolveReport(
            output = output,
            modelTypes = setOf(SolverModelType.LP, SolverModelType.MIP)
        )
    }

    /**
     * Converts a remote result without an artifact into an empty solution report.
     * 将没有 artifact 的远程结果转换为空解报告。
     *
     * @param variableCount 变量数量 / Variable count
     * @return 空解报告或错误 / Empty-solution report or error
    */
    private fun SolveResult.toEmptySolutionOutput(variableCount: Int): Ret<SolveReport<Flt64>> {
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
        val solutionGap = gap
        val reportedBestBound = statistics.remoteFlt64("bestBound")
        return Ok(
            SolveReport(
                problemStatus = problemStatus.toCoreStatus(),
                terminationReason = terminationReason.toCoreReason(),
                solutionPresence = solutionPresence.toCorePresence(),
                solution = SolveSolution(
                    values = emptyList(),
                    objective = objective
                ),
                proof = SolveProof(proofStatus.toCoreProof()),
                statistics = SolveStatistics(
                    solveTime = elapsed,
                    bestBound = reportedBestBound,
                    gap = solutionGap
                )
            )
        )
    }

    /**
     * Reads the serialized solution from object storage.
     * 从对象存储读取序列化的求解结果。
     *
     * @return 序列化求解结果，如果不可用则返回 null / Serialized solution, or null if unavailable
    */
    private suspend fun SolveResult.readSerializedSolution(): Ret<SerializedSolution?> {
        val ref = resultRef ?: return Ok(null)
        val storage = resultStoragePort
            ?: return Failed(ErrorCode.ORSolutionInvalid, "远程结果引用缺少对象存储 / Remote result reference has no object storage")
        val bytes = storage.get(ref)
            ?: return Failed(ErrorCode.ORSolutionInvalid, "远程结果 artifact 不存在 / Remote result artifact is missing")
        return try {
            Ok(
                json.decodeFromString(
                    SerializedSolution.serializer(),
                    bytes.decodeToString()
                )
            )
        } catch (error: Exception) {
            Failed(
                ErrorCode.ORSolutionInvalid,
                "远程结果 artifact 无法解码：${error.message ?: "invalid JSON"} / Remote result artifact cannot be decoded"
            )
        }
    }
}
