/** Remote CP execution client. / 远程 CP 执行客户端。 */
package fuookami.ospf.kotlin.framework.solver.remote.client

import java.security.MessageDigest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingSnapshotCodec
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalValue
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingInfeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolverOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingUnknownOutput
import fuookami.ospf.kotlin.core.solver.output.toCompatibilityFlt64
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.report.AuditFingerprint
import fuookami.ospf.kotlin.core.solver.report.BoundSide
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.EvidenceCompleteness
import fuookami.ospf.kotlin.core.solver.report.EvidenceExactness
import fuookami.ospf.kotlin.core.solver.report.EvidenceMinimality
import fuookami.ospf.kotlin.core.solver.report.EvidenceValidity
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidence
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidenceSource
import fuookami.ospf.kotlin.core.solver.report.SolveIssue
import fuookami.ospf.kotlin.core.solver.report.SolveIssueCategory
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.ProofStatus
import fuookami.ospf.kotlin.core.solver.report.SolveDiagnostics
import fuookami.ospf.kotlin.core.solver.report.SolveFingerprints
import fuookami.ospf.kotlin.core.solver.report.SolveProof
import fuookami.ospf.kotlin.core.solver.report.SolveRunId
import fuookami.ospf.kotlin.core.solver.report.SolveStatistics
import fuookami.ospf.kotlin.core.solver.report.SolverCapabilities
import fuookami.ospf.kotlin.core.solver.report.SolverDescriptor
import fuookami.ospf.kotlin.core.solver.report.SolverModelType
import fuookami.ospf.kotlin.core.solver.report.SolverProvenance
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.report.VariableBoundRef
import fuookami.ospf.kotlin.core.solver.report.VariableDomainRef
import fuookami.ospf.kotlin.framework.solver.remote.adapter.ospf.OspfRemoteModelSerializer
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.ObjectStoragePort
import fuookami.ospf.kotlin.framework.solver.remote.port.SolverExecutionPort

/**
 * 将 portable CP snapshot 接入统一远程执行端口。 / Connect a portable CP snapshot to the shared remote execution port.
 *
 * 该客户端负责协议、切片生命周期和稳定解回填；求解器选择与 CP 编译仍由远端执行端负责。 /
 * The client owns protocol, slice lifecycle, and stable-solution materialization; solver selection and CP compilation remain server-side.
 *
 * @property remoteClient 远程求解执行客户端 / Remote solve execution client
 * @property runtimeConfig 远程 CP 运行时配置 / Remote CP runtime configuration
 * @property resultStoragePort 稳定结果对象存储端口 / Stable result object storage port
 * @property json 结果协议 JSON 编解码器 / JSON codec for result protocol
 */
class RemoteConstraintProgrammingClient(
    private val remoteClient: RemoteSolverClient,
    private val runtimeConfig: RemoteSolverRuntimeConfig = RemoteSolverRuntimeConfig(),
    private val resultStoragePort: ObjectStoragePort? = null,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) {
    private val digestJson = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }
    /** 使用执行端口构造远程 CP 客户端。 / Construct a remote CP client from an execution port.
     *
     * @param executionPort Shared remote execution port. / 共享远程执行端口。
     * @param runtimeConfig Remote CP runtime configuration. / 远程 CP 运行时配置。
     * @param resultStoragePort Storage used to load stable result artifacts. / 用于读取稳定结果 artifact 的对象存储端口。
     */
    constructor(
        executionPort: SolverExecutionPort,
        runtimeConfig: RemoteSolverRuntimeConfig = RemoteSolverRuntimeConfig(),
        resultStoragePort: ObjectStoragePort? = null
    ) : this(
        remoteClient = RemoteSolverClient(executionPort),
        runtimeConfig = runtimeConfig,
        resultStoragePort = resultStoragePort
    )

    /**
     * 提交 CP snapshot 并等待远端完成。 / Submit a CP snapshot and wait for remote completion.
     *
     * @param snapshot 不含 native 句柄的 CP snapshot / CP snapshot without native handles
     * @param taskId 任务 ID / Task ID
     * @param sliceId 切片 ID / Slice ID
     * @param nodeId 节点 ID / Node ID
     * @param tenantId 租户 ID / Tenant ID
     * @param quantum 单次切片时长 / Slice quantum
     * @param maxRounds 最大切片轮数 / Maximum slice rounds
     * @param solverConfig CP 求解配置 / CP solver configuration
     * @return 远程求解结果 / Remote solve result
     */
    suspend fun solve(
        snapshot: ConstraintProgrammingModelSnapshot,
        taskId: TaskId = runtimeConfig.taskIdProvider(),
        sliceId: SliceId = runtimeConfig.sliceIdProvider(),
        nodeId: NodeId = runtimeConfig.nodeId,
        tenantId: TenantId = runtimeConfig.tenantId,
        quantum: Duration = runtimeConfig.quantum,
        maxRounds: UInt64 = runtimeConfig.maxRounds,
        solverConfig: SolverConfig = runtimeConfig.solverConfig
    ): Ret<SolveResult> {
        val modelData = when (val encoded = OspfRemoteModelSerializer.modelData(snapshot)) {
            is Ok -> encoded.value
            is Failed -> return Failed(encoded.error)
            is Fatal -> return Fatal(encoded.errors)
        }
        return remoteClient.solve(
            payload = SolvePayload(
                modelData = modelData,
                // 调度量子由远程切片接口传递，不属于 CP 语义配置或 checkpoint 指纹。
                // The scheduler quantum is carried by the slice API, not semantic CP configuration or checkpoint fingerprints.
                config = solverConfig,
                taskMeta = TaskMeta(
                    targetType = TargetTypeName.of("cp"),
                    estimatedVariableCount = snapshot.variables.size,
                    estimatedConstraintCount = snapshot.constraints.size
                )
            ),
            taskId = taskId,
            sliceId = sliceId,
            nodeId = nodeId,
            tenantId = tenantId,
            quantum = quantum,
            maxRounds = maxRounds
        )
    }

    /**
     * 提交 CP snapshot 并物化稳定整数解。 / Submit a CP snapshot and materialize its stable integer solution.
     *
     * `solve` 保留原始协议结果；本方法读取 `resultRef` 并校验所有 snapshot 成员。 /
     * `solve` remains the raw protocol API; this method reads `resultRef` and validates every snapshot member.
     *
     * @param snapshot 不含 native 句柄的 CP snapshot / CP snapshot without native handles
     * @param taskId 任务 ID / Task ID
     * @param sliceId 切片 ID / Slice ID
     * @param nodeId 节点 ID / Node ID
     * @param tenantId 租户 ID / Tenant ID
     * @param quantum 单次切片时长 / Slice quantum
     * @param maxRounds 最大切片轮数 / Maximum slice rounds
     * @param solverConfig CP 求解配置 / CP solver configuration
     * @return 物化的 CP 输出 / Materialized CP output
     */
    suspend fun solveOutput(
        snapshot: ConstraintProgrammingModelSnapshot,
        taskId: TaskId = runtimeConfig.taskIdProvider(),
        sliceId: SliceId = runtimeConfig.sliceIdProvider(),
        nodeId: NodeId = runtimeConfig.nodeId,
        tenantId: TenantId = runtimeConfig.tenantId,
        quantum: Duration = runtimeConfig.quantum,
        maxRounds: UInt64 = runtimeConfig.maxRounds,
        solverConfig: SolverConfig = runtimeConfig.solverConfig
    ): Ret<ConstraintProgrammingSolverOutput> {
        val result = when (val raw = solve(
            snapshot = snapshot,
            taskId = taskId,
            sliceId = sliceId,
            nodeId = nodeId,
            tenantId = tenantId,
            quantum = quantum,
            maxRounds = maxRounds,
            solverConfig = solverConfig
        )) {
            is Ok -> raw.value
            is Failed -> return Failed(raw.error)
            is Fatal -> return Fatal(raw.errors)
        }
        return materialize(
            snapshot = snapshot,
            result = result,
            expectedTaskId = taskId.value,
            expectedSliceId = sliceId.value,
            expectedConfigurationFingerprint = configurationFingerprint(solverConfig)
        )
    }

    private suspend fun materialize(
        snapshot: ConstraintProgrammingModelSnapshot,
        result: SolveResult,
        expectedTaskId: String,
        expectedSliceId: String,
        expectedConfigurationFingerprint: String
    ): Ret<ConstraintProgrammingSolverOutput> {
        val normalizedResult = normalizeLegacyResult(result)
        when (val validation = validateRawResult(normalizedResult)) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        when (val validation = validateExecutionBinding(
            snapshot = snapshot,
            result = normalizedResult,
            expectedTaskId = expectedTaskId,
            expectedSliceId = expectedSliceId,
            expectedConfigurationFingerprint = expectedConfigurationFingerprint
        )) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        when (val validation = validateDiagnostics(normalizedResult, snapshot)) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        val termination = normalizedResult.terminationReason.toCoreReason()
        val serialized = when (val loaded = normalizedResult.readSerializedSolution(
            strict = normalizedResult.isStrictV2()
        )) {
            is Ok -> loaded.value
            is Failed -> return Failed(loaded.error)
            is Fatal -> return Fatal(loaded.errors)
        }
        if (serialized != null) {
            when (val validation = validateSerializedResult(normalizedResult, serialized)) {
                is Ok -> {}
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
        }
        if (!normalizedResult.feasible) {
            if (normalizedResult.problemStatus == RemoteProblemStatus.INFEASIBLE &&
                normalizedResult.terminationReason == RemoteTerminationReason.COMPLETED
            ) {
                return ok(
                    ConstraintProgrammingInfeasibleOutput(
                        proofStatus = normalizedResult.proofStatus.toCoreProofStatus(),
                        report = normalizedResult.toReport()
                    )
                )
            }
            return ok(
                ConstraintProgrammingUnknownOutput(
                    terminationReason = termination,
                    report = normalizedResult.toReport()
                )
            )
        }
        if (serialized == null) {
            if (snapshot.variables.isNotEmpty() || snapshot.intervals.isNotEmpty()) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "远程 CP 解缺少 resultRef：无法物化稳定解 / Remote CP resultRef is missing; stable solution cannot be materialized"
                )
            }
            when (val validation = validateConstraints(snapshot, emptyMap())) {
                is Ok -> {}
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
            when (val validation = validateObjective(
                snapshot = snapshot,
                values = emptyMap(),
                rawObjectiveValue = normalizedResult.objectiveValueInt64,
                serializedObjectiveValue = null
            )) {
                is Ok -> {}
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
            return ok(
                    ConstraintProgrammingFeasibleOutput(
                        solution = ConstraintProgrammingSolution(),
                    objective = normalizedResult.objectiveValueInt64?.let(::Int64)?.toCompatibilityFlt64(),
                    bestBound = normalizedResult.statistics["bestBound"]?.toFlt64OrNull(),
                    status = if (normalizedResult.optimal) SolverStatus.Optimal else SolverStatus.Feasible,
                    proofStatus = normalizedResult.proofStatus.toCoreProofStatus(),
                    report = normalizedResult.toReport(),
                    exactObjective = normalizedResult.objectiveValueInt64?.let(::Int64)
                )
            )
        }
        if (!serialized.feasible) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP 协议结果与稳定解可行性不一致 / Remote CP feasibility disagrees with serialized solution"
            )
        }
        val expectedVariables = snapshot.variables.mapTo(linkedSetOf()) { it.id.value }
        val actualVariables = serialized.variableValuesById.keys
        val unknownVariables = actualVariables - expectedVariables
        val missingVariables = expectedVariables - actualVariables
        if (unknownVariables.isNotEmpty() || missingVariables.isNotEmpty()) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP 稳定变量集合不匹配：unknown=$unknownVariables, missing=$missingVariables / " +
                    "Remote CP stable variable IDs do not match the snapshot"
            )
        }
        val expectedIntervals = snapshot.intervals.mapTo(linkedSetOf()) { it.id.value }
        val actualIntervals = serialized.intervalValues.keys
        val unknownIntervals = actualIntervals - expectedIntervals
        val missingIntervals = expectedIntervals - actualIntervals
        if (unknownIntervals.isNotEmpty() || missingIntervals.isNotEmpty()) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP interval 集合不匹配：unknown=$unknownIntervals, missing=$missingIntervals / " +
                    "Remote CP interval IDs do not match the snapshot"
            )
        }
        val values = serialized.variableValuesById.mapKeys { (id, _) ->
            VariableId(id)
        }.mapValues { (_, value) -> Int64(value) }
        for (definition in snapshot.variables) {
            val value = values[definition.id]!!
            if (!definition.domain.contains(value)) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "远程 CP 变量值超出值域：${definition.id}=$value / " +
                        "Remote CP variable value is outside its domain: ${definition.id}=$value"
                )
            }
        }
        val intervals = serialized.intervalValues.mapKeys { (id, _) -> IntervalId(id) }.mapValues { (_, value) ->
            IntervalValue(
                start = Int64(value.start),
                size = Int64(value.size),
                end = Int64(value.end),
                present = value.present
            )
        }
        for (definition in snapshot.intervals) {
            val actual = intervals[definition.id]!!
            val evaluated = definition.evaluate(values)
            when (evaluated) {
                is Ok -> if (evaluated.value != actual) {
                    return Failed(
                        ErrorCode.ORSolutionInvalid,
                        "远程 CP interval 解不满足定义：${definition.id} / " +
                            "Remote CP interval solution violates its definition: ${definition.id}"
                    )
                }

                is Failed -> return Failed(evaluated.error)
                is Fatal -> return Fatal(evaluated.errors)
            }
        }
        when (val validation = validateConstraints(snapshot, values)) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        when (val validation = validateObjective(
            snapshot = snapshot,
            values = values,
            rawObjectiveValue = normalizedResult.objectiveValueInt64,
            serializedObjectiveValue = serialized.objectiveValueInt64
        )) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        val solution = ConstraintProgrammingSolution(values = values, intervals = intervals)
        return ok(
            ConstraintProgrammingFeasibleOutput(
                solution = solution,
                objective = (serialized.objectiveValueInt64 ?: normalizedResult.objectiveValueInt64)
                    ?.let(::Int64)
                    ?.toCompatibilityFlt64(),
                bestBound = normalizedResult.statistics["bestBound"]?.toFlt64OrNull(),
                status = if (normalizedResult.optimal && normalizedResult.proofStatus == RemoteProofStatus.VERIFIED) {
                    SolverStatus.Optimal
                } else {
                    SolverStatus.Feasible
                },
                proofStatus = normalizedResult.proofStatus.toCoreProofStatus(),
                report = normalizedResult.toReport(solution),
                exactObjective = (serialized.objectiveValueInt64 ?: normalizedResult.objectiveValueInt64)
                    ?.let(::Int64)
            )
        )
    }

    private fun normalizeLegacyResult(result: SolveResult): SolveResult {
        if (result.schemaVersion == "1.0" &&
            !result.feasible &&
            result.problemStatus == RemoteProblemStatus.INFEASIBLE &&
            result.terminationReason == RemoteTerminationReason.COMPLETED &&
            result.proofStatus == RemoteProofStatus.NONE
        ) {
            return result.copy(proofStatus = RemoteProofStatus.CLAIMED)
        }
        return result
    }

    private fun validateRawResult(result: SolveResult): Try {
        val majorVersion = result.schemaVersion.substringBefore('.').toIntOrNull()
        if (majorVersion == null || majorVersion !in SUPPORTED_SCHEMA_MAJOR_VERSIONS) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "不支持的远程 CP schema 主版本：${result.schemaVersion} / " +
                    "Unsupported remote CP schema major version: ${result.schemaVersion}"
            )
        }
        if (result.feasible && result.problemStatus != RemoteProblemStatus.FEASIBLE) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 的问题状态与可行性不一致 / Remote CP raw problem status disagrees with feasibility"
            )
        }
        if (!result.feasible && result.problemStatus == RemoteProblemStatus.FEASIBLE) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 将不可行结果标记为 FEASIBLE / Remote CP raw result marks an infeasible result as FEASIBLE"
            )
        }
        if (!result.feasible && result.optimal) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 将不可行结果标记为最优 / Remote CP raw result marks an infeasible result as optimal"
            )
        }
        if (result.optimal && result.proofStatus == RemoteProofStatus.NONE) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 声称最优但没有证明状态 / Remote CP raw result claims optimality without a proof status"
            )
        }
        if (result.schemaVersion != "1.0" && result.fingerprints.keys != result.fingerprintSchemas.keys) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP 指纹与 fingerprint schema 清单不一致 / Remote CP fingerprints and fingerprint schemas disagree"
            )
        }
        if (result.schemaVersion != "1.0" && result.fingerprintSchemas.values.any { it.isBlank() }) {
            return Failed(ErrorCode.ORSolutionInvalid, "远程 CP 指纹 schema 不能为空 / Remote CP fingerprint schemas must not be blank")
        }
        if (result.objectiveValueInt64 != null && result.objectiveValue != null) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP 精确目标不得同时使用浮点目标字段 / " +
                    "Remote CP exact objectives must not be duplicated in the floating-point field"
            )
        }
        for (key in listOf("bestBound", "gap")) {
            val value = result.statistics[key] ?: continue
            if (value.toFlt64OrNull() == null) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "远程 CP 统计字段无效：$key=$value / Invalid remote CP statistic: $key=$value"
                )
            }
        }
        if ((result.resultRef == null) != (result.artifactDigest == null)) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP 结果引用与 artifact 摘要必须成对出现 / " +
                    "Remote CP result references and artifact digests must be provided together"
            )
        }
        val expectedPresence = when {
            result.optimal -> RemoteSolutionPresence.OPTIMAL
            result.feasible -> RemoteSolutionPresence.INCUMBENT
            else -> RemoteSolutionPresence.NONE
        }
        if (result.solutionPresence != expectedPresence) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 的解存在性与可行性/最优性不一致 / Remote CP raw solution presence disagrees with feasibility or optimality"
            )
        }
        return ok
    }

    private fun validateSerializedResult(
        result: SolveResult,
        serialized: SerializedSolution
    ): Try {
        val strict = serialized.schemaVersion.substringBefore('.').toIntOrNull()?.let { it >= 2 } == true
        if (serialized.feasible != result.feasible) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 与稳定解可行性不一致 / Remote CP raw result disagrees with serialized feasibility"
            )
        }
        if (serialized.optimal != result.optimal) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 与稳定解最优性不一致 / Remote CP raw result disagrees with serialized optimality"
            )
        }
        if ((strict && serialized.proofStatus != result.proofStatus) ||
            (!strict && serialized.proofStatus != null && serialized.proofStatus != result.proofStatus)
        ) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 与稳定解证明状态不一致 / Remote CP raw result disagrees with serialized proof status"
            )
        }
        if (serialized.schemaVersion != result.schemaVersion) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 与稳定解 schema 版本不一致 / Remote CP raw result disagrees with serialized schema version"
            )
        }
        if ((strict && serialized.terminationReason != result.terminationReason) ||
            (!strict && serialized.terminationReason != null && serialized.terminationReason != result.terminationReason)
        ) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 与稳定解终止原因不一致 / Remote CP raw result disagrees with serialized termination reason"
            )
        }
        if (!serialized.feasible && serialized.optimal) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "稳定解将不可行结果标记为最优 / Serialized solution marks an infeasible result as optimal"
            )
        }
        val inferredPresence = when {
            serialized.optimal -> RemoteSolutionPresence.OPTIMAL
            serialized.feasible -> RemoteSolutionPresence.INCUMBENT
            else -> RemoteSolutionPresence.NONE
        }
        if ((strict && serialized.solutionPresence != inferredPresence) ||
            (!strict && serialized.solutionPresence != null && serialized.solutionPresence != inferredPresence)
        ) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "稳定解的解存在性与自身可行性/最优性不一致 / Serialized solution presence disagrees with its feasibility or optimality"
            )
        }
        if (inferredPresence != result.solutionPresence) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 与稳定解解存在性不一致 / Remote CP raw result disagrees with serialized solution presence"
            )
        }
        val serializedStatus = serialized.problemStatus
        if (serializedStatus == RemoteProblemStatus.FEASIBLE && !serialized.feasible) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "稳定解将不可行结果标记为 FEASIBLE / Serialized solution marks an infeasible result as FEASIBLE"
            )
        }
        if ((strict && serializedStatus != result.problemStatus) ||
            (!strict && serializedStatus != null && serializedStatus != result.problemStatus)
        ) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 与稳定解问题状态不一致 / Remote CP raw result disagrees with serialized problem status"
            )
        }
        if (serialized.objectiveValueInt64 != result.objectiveValueInt64 ||
            serialized.objectiveValue != result.objectiveValue
        ) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 与稳定解目标值不一致 / Remote CP raw result disagrees with serialized objective"
            )
        }
        if (serialized.objectiveValueInt64 != null && serialized.objectiveValue != null) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP artifact 的精确目标不得同时使用浮点目标字段 / " +
                    "Remote CP artifacts must not duplicate exact objectives in the floating-point field"
            )
        }
        if ((strict && serialized.gap != result.gap) ||
            (!strict && serialized.gap != null && serialized.gap != result.gap)
        ) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 与稳定解间隙不一致 / Remote CP raw result disagrees with serialized gap"
            )
        }
        if (serialized.provenance != result.provenance ||
            serialized.fingerprints != result.fingerprints ||
            serialized.fingerprintSchemas != result.fingerprintSchemas
        ) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 与稳定解来源或指纹不一致 / Remote CP raw result disagrees with serialized provenance or fingerprints"
            )
        }
        if (serialized.statistics != result.statistics || serialized.diagnostics != result.diagnostics) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 与稳定解统计或诊断不一致 / Remote CP raw result disagrees with serialized statistics or diagnostics"
            )
        }
        if ((strict && (serialized.runId != result.runId || serialized.attemptId != result.attemptId)) ||
            (!strict && ((serialized.runId != null && serialized.runId != result.runId) ||
                (serialized.attemptId != null && serialized.attemptId != result.attemptId)))
        ) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 与稳定解 run/attempt 不一致 / Remote CP raw result disagrees with serialized run/attempt"
            )
        }
        if ((serialized.artifactDigest == null) != (result.artifactDigest == null)) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 与稳定解 artifact 摘要缺失状态不一致 / " +
                    "Remote CP raw result and serialized artifact disagree about digest presence"
            )
        }
        if (serialized.artifactDigest != null && serialized.artifactDigest != result.artifactDigest) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP raw result 与稳定解 artifact 摘要不一致 / Remote CP raw result disagrees with serialized artifact digest"
            )
        }
        return ok
    }

    private fun validateExecutionBinding(
        snapshot: ConstraintProgrammingModelSnapshot,
        result: SolveResult,
        expectedTaskId: String,
        expectedSliceId: String,
        expectedConfigurationFingerprint: String
    ): Try {
        val strict = result.schemaVersion.substringBefore('.').toIntOrNull()?.let { it >= 2 } == true
        if (strict && (result.runId != expectedTaskId || result.attemptId != expectedSliceId)) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP 结果未绑定本次 task/slice：run=${result.runId}, attempt=${result.attemptId} / " +
                "Remote CP result is not bound to this task/slice"
            )
        }
        if (!strict &&
            ((result.runId != null && result.runId != expectedTaskId) ||
                (result.attemptId != null && result.attemptId != expectedSliceId))
        ) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "legacy 远程 CP 结果的 task/slice 绑定不一致：run=${result.runId}, attempt=${result.attemptId} / " +
                    "Legacy remote CP result has conflicting task/slice binding"
            )
        }
        val encoded = when (val value = ConstraintProgrammingSnapshotCodec.encode(snapshot)) {
            is Ok -> value.value
            is Failed -> return Failed(value.error)
            is Fatal -> return Fatal(value.errors)
        }
        val expectedModel = MessageDigest.getInstance("SHA-256")
            .digest(encoded!!.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
        val requiresArtifactBinding = result.feasible || result.resultRef != null ||
            result.problemStatus != RemoteProblemStatus.UNKNOWN ||
            result.terminationReason != RemoteTerminationReason.BACKEND_FAILURE
        val actualModel = result.fingerprints["model"]
        if (strict && requiresArtifactBinding && actualModel == null) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "严格远程 CP 结果缺少模型指纹 / Strict remote CP result is missing the model fingerprint"
            )
        }
        if (actualModel != null && actualModel != expectedModel) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP 结果模型指纹与请求不一致 / Remote CP result model fingerprint disagrees with the request"
            )
        }
        if (strict && requiresArtifactBinding) {
            val actualConfiguration = result.fingerprints["configuration"]
            if (actualConfiguration == null || actualConfiguration != expectedConfigurationFingerprint) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "远程 CP 结果配置指纹与请求不一致 / Remote CP result configuration fingerprint disagrees with the request"
                )
            }
        }
        return ok
    }

    private fun SolveResult.isStrictV2(): Boolean {
        return schemaVersion.substringBefore('.').toIntOrNull()?.let { it >= 2 } == true
    }

    private fun configurationFingerprint(config: SolverConfig): String {
        val effectiveConfig = config.copy(
            timeLimit = config.timeLimit?.inWholeMilliseconds?.milliseconds,
            threads = config.threads ?: DEFAULT_REMOTE_CP_THREADS
        )
        val canonical = buildString {
            appendCanonicalConfiguration("timeLimitMs", effectiveConfig.timeLimit?.inWholeMilliseconds?.toString())
            appendCanonicalConfiguration("solutionLimit", effectiveConfig.solutionLimit?.toString())
            appendCanonicalConfiguration("mipGapTolerance", effectiveConfig.mipGapTolerance?.toString())
            appendCanonicalConfiguration("threads", effectiveConfig.threads?.toString())
            effectiveConfig.solverParams.toSortedMap().forEach { (key, value) ->
                appendCanonicalConfiguration("solverParam.key", key)
                appendCanonicalConfiguration("solverParam.value", value)
            }
        }
        return sha256(canonical)
    }

    private fun sha256(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun StringBuilder.appendCanonicalConfiguration(name: String, value: String?) {
        append(name.length)
            .append(':')
            .append(name)
            .append(value?.length ?: -1)
            .append(':')
            .append(value ?: "")
    }

    private fun validateDiagnostics(
        result: SolveResult,
        snapshot: ConstraintProgrammingModelSnapshot
    ): Try {
        val diagnostics = result.diagnostics
        val strict = result.schemaVersion.substringBefore('.').toIntOrNull()?.let { it >= 2 } == true
        val source = diagnostics["infeasibility.source"]
        val hasInfeasibilityFields = diagnostics.keys.any { it.startsWith("infeasibility.") }
        if (source == null) {
            if (strict && hasInfeasibilityFields) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "严格远程 CP 诊断缺少 source / Strict remote CP diagnostics are missing the source"
                )
            }
            return validateIssueEntries(diagnostics)
        }
        if (runCatching { InfeasibilityEvidenceSource.valueOf(source) }.isFailure) {
            return Failed(ErrorCode.ORSolutionInvalid, "远程 CP 诊断 source 无效 / Remote CP diagnostic source is invalid")
        }
        val enumFields = mapOf(
            "infeasibility.exactness" to EvidenceExactness::class.java,
            "infeasibility.completeness" to EvidenceCompleteness::class.java,
            "infeasibility.validity" to EvidenceValidity::class.java,
            "infeasibility.minimality" to EvidenceMinimality::class.java
        )
        for ((key, type) in enumFields) {
            val value = diagnostics[key] ?: continue
            if (runCatching { java.lang.Enum.valueOf(type, value) }.isFailure) {
                return Failed(ErrorCode.ORSolutionInvalid, "远程 CP 诊断枚举无效：$key / Invalid remote CP diagnostic enum: $key")
            }
        }
        val variableIds = snapshot.variables.mapTo(linkedSetOf()) { it.id.value }
        val constraintIds = snapshot.constraints.mapTo(linkedSetOf()) { it.id.value }
        val boundRefs = when (val parsed = diagnosticList(diagnostics, "infeasibility.variableBoundRefs")) {
            is Ok -> parsed.value!!
            is Failed -> return Failed(parsed.error)
            is Fatal -> return Fatal(parsed.errors)
        }
        boundRefs.forEach { encoded ->
            val separator = encoded.lastIndexOf(':')
            if (separator <= 0 || encoded.substring(0, separator) !in variableIds ||
                runCatching { BoundSide.valueOf(encoded.substring(separator + 1)) }.isFailure
            ) {
                return Failed(ErrorCode.ORSolutionInvalid, "远程 CP 变量界诊断成员无效 / Invalid remote CP variable-bound diagnostic member")
            }
        }
        val domains = when (val parsed = diagnosticList(diagnostics, "infeasibility.variableDomainRefs")) {
            is Ok -> parsed.value!!
            is Failed -> return Failed(parsed.error)
            is Fatal -> return Fatal(parsed.errors)
        }
        if (domains.any { it !in variableIds }) {
            return Failed(ErrorCode.ORSolutionInvalid, "远程 CP 变量域诊断引用未知变量 / Remote CP variable-domain diagnostic references an unknown variable")
        }
        val constraints = when (val parsed = diagnosticList(diagnostics, "infeasibility.constraintIds")) {
            is Ok -> parsed.value!!
            is Failed -> return Failed(parsed.error)
            is Fatal -> return Fatal(parsed.errors)
        }
        if (constraints.any { it !in constraintIds }) {
            return Failed(ErrorCode.ORSolutionInvalid, "远程 CP 约束诊断引用未知约束 / Remote CP constraint diagnostic references an unknown constraint")
        }
        val assumptions = when (val parsed = diagnosticList(diagnostics, "infeasibility.assumptionIds")) {
            is Ok -> parsed.value!!
            is Failed -> return Failed(parsed.error)
            is Fatal -> return Fatal(parsed.errors)
        }
        if (assumptions.any { it !in variableIds }) {
            return Failed(ErrorCode.ORSolutionInvalid, "远程 CP assumption 诊断引用未知变量 / Remote CP assumption diagnostic references an unknown variable")
        }
        val members = when (val parsed = diagnosticList(diagnostics, "infeasibility.members")) {
            is Ok -> parsed.value!!
            is Failed -> return Failed(parsed.error)
            is Fatal -> return Fatal(parsed.errors)
        }
        for (member in members) {
            when {
                member.startsWith("constraint:") -> {
                    if (member.removePrefix("constraint:") !in constraintIds) {
                        return Failed(ErrorCode.ORSolutionInvalid, "远程 CP 诊断成员引用未知约束 / Remote CP diagnostic member references an unknown constraint")
                    }
                }
                member.startsWith("bound:") -> {
                    val encoded = member.removePrefix("bound:")
                    val separator = encoded.lastIndexOf(':')
                    if (separator <= 0 || encoded.substring(0, separator) !in variableIds ||
                        runCatching { BoundSide.valueOf(encoded.substring(separator + 1)) }.isFailure
                    ) {
                        return Failed(ErrorCode.ORSolutionInvalid, "远程 CP 诊断 bound 成员无效 / Invalid remote CP diagnostic bound member")
                    }
                }
                member.startsWith("domain:") -> {
                    if (member.removePrefix("domain:") !in variableIds) {
                        return Failed(ErrorCode.ORSolutionInvalid, "远程 CP 诊断 domain 成员无效 / Invalid remote CP diagnostic domain member")
                    }
                }
                else -> return Failed(ErrorCode.ORSolutionInvalid, "远程 CP 诊断成员类型未知 / Unknown remote CP diagnostic member type")
            }
        }
        diagnostics["infeasibility.terminationReason"]?.let {
            if (runCatching { RemoteTerminationReason.valueOf(it) }.isFailure) {
                return Failed(ErrorCode.ORSolutionInvalid, "远程 CP 诊断终止原因无效 / Invalid remote CP diagnostic termination reason")
            }
        }
        diagnostics["infeasibility.unavailable.category"]?.let {
            if (runCatching { SolveIssueCategory.valueOf(it) }.isFailure) {
                return Failed(ErrorCode.ORSolutionInvalid, "远程 CP 诊断错误类别无效 / Invalid remote CP diagnostic issue category")
            }
        }
        diagnostics["infeasibility.verificationChecks"]?.toULongOrNull() ?: diagnostics["infeasibility.verificationChecks"]?.let {
            return Failed(ErrorCode.ORSolutionInvalid, "远程 CP 诊断复验次数无效 / Invalid remote CP diagnostic verification count")
        }
        diagnostics["infeasibility.elapsedMs"]?.toLongOrNull() ?: diagnostics["infeasibility.elapsedMs"]?.let {
            return Failed(ErrorCode.ORSolutionInvalid, "远程 CP 诊断耗时无效 / Invalid remote CP diagnostic elapsed time")
        }
        if (diagnostics["infeasibility.unavailable.code"] != null &&
            diagnostics["infeasibility.unavailable.message"] == null
        ) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP 不可用诊断缺少 message / Remote CP unavailable diagnostic is missing its message"
            )
        }
        return validateIssueEntries(diagnostics)
    }

    private fun validateIssueEntries(diagnostics: Map<String, String>): Try {
        for (prefix in listOf("warning", "error")) {
            val indices = diagnostics.keys.mapNotNull { key ->
                Regex("^${Regex.escape(prefix)}\\.(\\d+)\\.code$")
                    .matchEntire(key)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
            }
            for (index in indices) {
                val code = diagnostics["$prefix.$index.code"]
                val category = diagnostics["$prefix.$index.category"]
                if (code.isNullOrBlank() || category.isNullOrBlank() ||
                    runCatching { SolveIssueCategory.valueOf(category) }.isFailure
                ) {
                    return Failed(
                        ErrorCode.ORSolutionInvalid,
                        "远程 CP 结构化问题字段无效：$prefix.$index / Invalid remote CP issue entry: $prefix.$index"
                    )
                }
                if (diagnostics["$prefix.$index.message"] == null) {
                    return Failed(
                        ErrorCode.ORSolutionInvalid,
                        "远程 CP 结构化问题缺少 message：$prefix.$index / Remote CP issue entry is missing its message: $prefix.$index"
                    )
                }
            }
        }
        return ok
    }

    private fun diagnosticList(
        diagnostics: Map<String, String>,
        key: String
    ): Ret<List<String>> {
        val encoded = diagnostics[key] ?: return ok(emptyList())
        return try {
            ok(json.decodeFromString<List<String>>(encoded))
        } catch (error: Throwable) {
            Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP 诊断列表格式无效：$key / Invalid remote CP diagnostic list: $key"
            )
        }
    }

    private fun SolveResult.toReport(
        solution: ConstraintProgrammingSolution? = null
    ): SolveReport<Int64> {
        val provenance = if (provenance.isEmpty()) {
            null
        } else {
            SolverProvenance(
                descriptor = SolverDescriptor(
                    solverId = provenance["solverId"] ?: "remote",
                    backendName = provenance["backend"] ?: "remote",
                    backendVersion = provenance["backendVersion"],
                    pluginVersion = provenance["pluginVersion"],
                    capabilities = SolverCapabilities(modelTypes = setOf(SolverModelType.CP))
                ),
                nativeVersion = provenance["nativeVersion"],
                effectiveParameters = provenance
                    .filterKeys { it.startsWith("parameter.") }
                    .mapKeys { (key, _) -> key.removePrefix("parameter.") },
                threadCount = provenance["threads"]?.toIntOrNull(),
                randomSeed = provenance["randomSeed"]?.toLongOrNull(),
                deterministic = provenance["deterministic"]?.toBooleanStrictOrNull(),
                environmentSummary = provenance
                    .filterKeys { it.startsWith("environment.") }
                    .mapKeys { (key, _) -> key.removePrefix("environment.") }
            )
        }
        val fingerprints = SolveFingerprints(
            model = fingerprints["model"]?.let { AuditFingerprint(fingerprintSchemas["model"] ?: schemaVersion, "SHA-256", it) },
            configuration = fingerprints["configuration"]?.let { AuditFingerprint(fingerprintSchemas["configuration"] ?: schemaVersion, "SHA-256", it) },
            solver = fingerprints["solver"]?.let { AuditFingerprint(fingerprintSchemas["solver"] ?: schemaVersion, "SHA-256", it) }
        )
        val reportSolution = solution?.let { values ->
            fuookami.ospf.kotlin.core.solver.report.SolveSolution<Int64>(
                values = values.asList(values.values.keys.toList()),
                objective = objectiveValueInt64?.let(::Int64)
            )
        }
        return SolveReport(
            schemaVersion = schemaVersion,
            runId = runId?.let(::SolveRunId),
            problemStatus = problemStatus.toCoreProblemStatus(),
            terminationReason = terminationReason.toCoreReason(),
            solutionPresence = solutionPresence.toCoreSolutionPresence(),
            solution = reportSolution,
            proof = SolveProof(proofStatus.toCoreProofStatus()),
            statistics = SolveStatistics(
                solveTime = elapsed,
                iterations = statistics["iterations"]?.toULongOrNull(),
                nodes = statistics["nodes"]?.toULongOrNull(),
                bestBound = statistics["bestBound"]?.toFlt64OrNull(),
                gap = statistics["gap"]?.toFlt64OrNull()
            ),
            diagnostics = decodeDiagnostics(),
            provenance = provenance,
            fingerprints = fingerprints
        )
    }

    private fun SolveResult.decodeDiagnostics(): SolveDiagnostics<Int64> {
        val evidence = diagnostics["infeasibility.source"]?.let { source ->
            val parsedSource = runCatching { InfeasibilityEvidenceSource.valueOf(source) }.getOrNull()
                ?: return@let null
            InfeasibilityEvidence(
                source = parsedSource,
                exactness = diagnostics["infeasibility.exactness"]
                    ?.let { runCatching { EvidenceExactness.valueOf(it) }.getOrNull() }
                    ?: EvidenceExactness.Unknown,
                completeness = diagnostics["infeasibility.completeness"]
                    ?.let { runCatching { EvidenceCompleteness.valueOf(it) }.getOrNull() }
                    ?: EvidenceCompleteness.Unavailable,
                validity = diagnostics["infeasibility.validity"]
                    ?.let { runCatching { EvidenceValidity.valueOf(it) }.getOrNull() }
                    ?: EvidenceValidity.Unknown,
                minimality = diagnostics["infeasibility.minimality"]
                    ?.let { runCatching { EvidenceMinimality.valueOf(it) }.getOrNull() }
                    ?: EvidenceMinimality.NotChecked,
                constraintIds = decodeDiagnosticList("infeasibility.constraintIds")
                    .mapTo(linkedSetOf(), ::ConstraintId),
                variableBoundRefs = decodeDiagnosticList("infeasibility.variableBoundRefs")
                    .filter { it.lastIndexOf(':') > 0 }
                    .mapNotNullTo(linkedSetOf()) { value ->
                        val separator = value.lastIndexOf(':')
                        val side = runCatching { BoundSide.valueOf(value.substring(separator + 1)) }.getOrNull()
                        side?.let { VariableBoundRef(VariableId(value.substring(0, separator)), it) }
                    },
                variableDomainRefs = decodeDiagnosticList("infeasibility.variableDomainRefs")
                    .mapTo(linkedSetOf()) { VariableDomainRef(VariableId(it)) },
                variableBoundIds = decodeDiagnosticList("infeasibility.variableBoundRefs")
                    .filter { it.lastIndexOf(':') > 0 }
                    .mapTo(linkedSetOf()) { VariableId(it.substring(0, it.lastIndexOf(':'))) },
                members = decodeEvidenceMembers(),
                reference = diagnostics["infeasibility.reference"],
                assumptionIds = decodeDiagnosticList("infeasibility.assumptionIds")
                    .mapTo(linkedSetOf(), ::VariableId),
                elapsed = diagnostics["infeasibility.elapsedMs"]
                    ?.toLongOrNull()
                    ?.milliseconds,
                unavailableReason = decodeUnavailableReason(),
                verificationChecks = diagnostics["infeasibility.verificationChecks"]
                    ?.toULongOrNull()
                    ?.let(::UInt64),
                terminationReason = diagnostics["infeasibility.terminationReason"]
                    ?.let { value -> runCatching { RemoteTerminationReason.valueOf(value).toCoreReason() }.getOrNull() }
            )
        }
        return SolveDiagnostics(
            infeasibilityEvidence = evidence,
            warnings = decodeIssues("warning"),
            errors = decodeIssues("error")
        )
    }

    private fun SolveResult.decodeEvidenceMembers(): Set<InfeasibilityMember> {
        return decodeDiagnosticList("infeasibility.members")
            .mapNotNullTo(linkedSetOf()) { member ->
                when {
                    member.startsWith("constraint:") -> {
                        InfeasibilityMember.Constraint(ConstraintId(member.removePrefix("constraint:")))
                    }

                    member.startsWith("bound:") -> {
                        val encoded = member.removePrefix("bound:")
                        val separator = encoded.lastIndexOf(':')
                        if (separator <= 0) {
                            null
                        } else {
                            runCatching {
                                InfeasibilityMember.VariableBound(
                                    VariableBoundRef(
                                        variableId = VariableId(encoded.substring(0, separator)),
                                        side = BoundSide.valueOf(encoded.substring(separator + 1))
                                    )
                                )
                            }.getOrNull()
                        }
                    }

                    member.startsWith("domain:") -> {
                        InfeasibilityMember.VariableDomain(
                            VariableDomainRef(VariableId(member.removePrefix("domain:")))
                        )
                    }

                    else -> null
                }
            }
    }

    private fun SolveResult.decodeDiagnosticList(key: String): List<String> {
        val encoded = diagnostics[key] ?: return emptyList()
        return runCatching { json.decodeFromString<List<String>>(encoded) }.getOrDefault(emptyList())
    }

    private fun SolveResult.decodeUnavailableReason(): SolveIssue? {
        val code = diagnostics["infeasibility.unavailable.code"] ?: return null
        return SolveIssue(
            code = code,
            category = diagnostics["infeasibility.unavailable.category"]
                ?.let { runCatching { SolveIssueCategory.valueOf(it) }.getOrNull() }
                ?: SolveIssueCategory.Backend,
            message = diagnostics["infeasibility.unavailable.message"].orEmpty()
        )
    }

    private fun SolveResult.decodeIssues(prefix: String): List<SolveIssue> {
        return diagnostics.keys
            .mapNotNull { key ->
                val match = Regex("^${Regex.escape(prefix)}\\.(\\d+)\\.code$").matchEntire(key) ?: return@mapNotNull null
                val index = match.groupValues[1]
                val code = diagnostics[key] ?: return@mapNotNull null
                SolveIssue(
                    code = code,
                    category = diagnostics["$prefix.$index.category"]
                        ?.let { runCatching { SolveIssueCategory.valueOf(it) }.getOrNull() }
                        ?: SolveIssueCategory.Backend,
                    message = diagnostics["$prefix.$index.message"].orEmpty()
                )
            }
            .sortedBy { it.code }
    }

    private fun String.toFlt64OrNull(): Flt64? {
        val decimal = toBigDecimalOrNull() ?: return null
        val value = decimal.toDouble()
        return value.takeIf { it.isFinite() }?.let(::Flt64)
    }

    private fun RemoteProblemStatus.toCoreProblemStatus(): ProblemStatus {
        return when (this) {
            RemoteProblemStatus.FEASIBLE -> ProblemStatus.Feasible
            RemoteProblemStatus.INFEASIBLE -> ProblemStatus.Infeasible
            RemoteProblemStatus.UNBOUNDED -> ProblemStatus.Unbounded
            RemoteProblemStatus.INFEASIBLE_OR_UNBOUNDED -> ProblemStatus.InfeasibleOrUnbounded
            RemoteProblemStatus.UNKNOWN -> ProblemStatus.Unknown
        }
    }

    private fun RemoteSolutionPresence.toCoreSolutionPresence(): fuookami.ospf.kotlin.core.solver.report.SolutionPresence {
        return when (this) {
            RemoteSolutionPresence.NONE -> fuookami.ospf.kotlin.core.solver.report.SolutionPresence.None
            RemoteSolutionPresence.INCUMBENT -> fuookami.ospf.kotlin.core.solver.report.SolutionPresence.Incumbent
            RemoteSolutionPresence.OPTIMAL -> fuookami.ospf.kotlin.core.solver.report.SolutionPresence.Optimal
        }
    }

    private fun RemoteProofStatus.toCoreProofStatus(): ProofStatus {
        return when (this) {
            RemoteProofStatus.NONE -> ProofStatus.None
            RemoteProofStatus.CLAIMED -> ProofStatus.Claimed
            RemoteProofStatus.VERIFIED -> ProofStatus.Verified
        }
    }

    private fun validateConstraints(
        snapshot: ConstraintProgrammingModelSnapshot,
        values: Map<VariableId, Int64>
    ): Ret<Unit> {
        for (definition in snapshot.constraints) {
            when (val checked = definition.constraint.isSatisfied(values)) {
                is Ok -> if (checked.value != true) {
                    return Failed(
                        ErrorCode.ORSolutionInvalid,
                        "远程 CP 解违反约束：${definition.id} / Remote CP solution violates constraint: ${definition.id}"
                    )
                }

                is Failed -> return Failed(checked.error)
                is Fatal -> return Fatal(checked.errors)
            }
        }
        return ok(Unit)
    }

    private fun validateObjective(
        snapshot: ConstraintProgrammingModelSnapshot,
        values: Map<VariableId, Int64>,
        rawObjectiveValue: Long?,
        serializedObjectiveValue: Long?
    ): Try {
        val objective = snapshot.objectives.firstOrNull()
        if (objective == null) {
            if (rawObjectiveValue != null || serializedObjectiveValue != null) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "snapshot 没有目标但远程 CP 返回了目标值 / Remote CP returned an objective for a snapshot without an objective"
                )
            }
            return ok
        }
        val expected = when (val evaluated = objective.expression.evaluate(values)) {
            is Ok -> evaluated.value!!.toLong()
            is Failed -> return Failed(evaluated.error)
            is Fatal -> return Fatal(evaluated.errors)
        }
        val reported = serializedObjectiveValue ?: rawObjectiveValue
            ?: return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP 解缺少目标值：${objective.id} / " +
                    "Remote CP result is missing the objective value: ${objective.id}"
            )
        if (reported != expected) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP 目标值与 snapshot 求值不一致：reported=$reported, expected=$expected / " +
                    "Remote CP objective disagrees with the snapshot evaluation: reported=$reported, expected=$expected"
            )
        }
        return ok
    }

    private suspend fun SolveResult.readSerializedSolution(strict: Boolean): Ret<SerializedSolution?> {
        val ref = resultRef ?: return ok(null)
        val storage = resultStoragePort
            ?: return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP 结果引用存在但未配置对象存储 / Remote CP resultRef exists but no object storage is configured"
            )
        val bytes = try {
            storage.get(ref)
        } catch (error: Throwable) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "读取远程 CP 结果失败：${error.message ?: error::class.simpleName} / " +
                    "Failed to read remote CP result: ${error.message ?: error::class.simpleName}"
            )
        } ?: return Failed(
            ErrorCode.ORSolutionInvalid,
            "远程 CP 结果对象不存在：${ref.path.value} / Remote CP result object does not exist: ${ref.path.value}"
        )
        return try {
            val encoded = bytes.decodeToString()
            val rawObject = digestJson.parseToJsonElement(encoded).jsonObject
            val serialized = json.decodeFromString(SerializedSolution.serializer(), encoded)
            val rawSchemaVersion = rawObject["schemaVersion"]?.jsonPrimitive?.content
            val artifactMajorVersion = rawSchemaVersion?.substringBefore('.')?.toIntOrNull()
            val hasNonEmptyFingerprintSchemas = rawObject["fingerprintSchemas"]
                ?.jsonObject
                ?.isNotEmpty() == true
            val hasV2Markers = artifactMajorVersion?.let { it >= 2 } == true ||
                hasNonEmptyFingerprintSchemas
            if (hasV2Markers && artifactMajorVersion != null && artifactMajorVersion < 2) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "远程 CP artifact 的 v2 字段与 schema 版本冲突 / Remote CP artifact has v2 fields with a legacy schema"
                )
            }
            if (strict || hasV2Markers) {
                val requiredFields = setOf(
                    "schemaVersion",
                    "feasible",
                    "optimal",
                    "objectiveValue",
                    "objectiveValueInt64",
                    "elapsedMs",
                    "message",
                    "variableValuesById",
                    "intervalValues",
                    "problemStatus",
                    "solutionPresence",
                    "proofStatus",
                    "terminationReason",
                    "gap",
                    "runId",
                    "attemptId",
                    "artifactDigest",
                    "fingerprints",
                    "fingerprintSchemas",
                    "provenance",
                    "statistics",
                    "diagnostics"
                )
                val missing = requiredFields.filterNot(rawObject::containsKey)
                if (missing.isNotEmpty()) {
                    return Failed(
                        ErrorCode.ORSolutionInvalid,
                        "严格 v2 结果 artifact 缺少审计字段：${missing.joinToString(",")} / " +
                            "Strict v2 result artifact is missing audit fields: ${missing.joinToString(",")}"
                    )
                }
                val requiredNonNull = setOf(
                    "schemaVersion",
                    "problemStatus",
                    "solutionPresence",
                    "proofStatus",
                    "terminationReason",
                    "runId",
                    "attemptId",
                    "artifactDigest"
                )
                val missingValues = requiredNonNull.filter { key ->
                    rawObject[key]?.jsonPrimitive?.isString == true &&
                        rawObject[key]?.jsonPrimitive?.content.isNullOrBlank()
                } + requiredNonNull.filter { key -> rawObject[key]?.toString() == "null" }
                if (missingValues.isNotEmpty()) {
                    return Failed(
                        ErrorCode.ORSolutionInvalid,
                        "严格 v2 结果 artifact 含空审计字段：${missingValues.distinct().joinToString(",")} / " +
                            "Strict v2 result artifact contains blank audit fields: ${missingValues.distinct().joinToString(",")}"
                    )
                }
            }
            val declaredDigest = serialized.artifactDigest ?: return Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP 结果 artifact 缺少摘要 / Remote CP result artifact is missing its digest"
            )
            val unsigned = serialized.copy(artifactDigest = null)
            val canonical = digestJson.encodeToString(SerializedSolution.serializer(), unsigned)
            val actualDigest = MessageDigest.getInstance("SHA-256")
                .digest(canonical.toByteArray(Charsets.UTF_8))
                .joinToString(separator = "") { byte -> "%02x".format(byte) }
            if (declaredDigest != actualDigest) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "远程 CP 结果 artifact 摘要不匹配：declared=$declaredDigest, actual=$actualDigest / " +
                        "Remote CP result artifact digest mismatch: declared=$declaredDigest, actual=$actualDigest"
                )
            }
            ok(serialized)
        } catch (error: Throwable) {
            Failed(
                ErrorCode.ORSolutionInvalid,
                "远程 CP 结果反序列化失败：${error.message ?: error::class.simpleName} / " +
                    "Failed to deserialize remote CP result: ${error.message ?: error::class.simpleName}"
            )
        }
    }

    private companion object {
        const val DEFAULT_REMOTE_CP_THREADS = 8
        val SUPPORTED_SCHEMA_MAJOR_VERSIONS = setOf(1, 2)
    }
}
