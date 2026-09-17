@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.solver.remote.client

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.report.CancellationRecord
import fuookami.ospf.kotlin.core.solver.report.CancellationSource
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.*

/**
 * 远程求解 HTTP 客户端。 / Remote solver HTTP client.
 *
 * @property baseUrl dispatcher 基础地址 / Dispatcher base URL
 * @property transport HTTP 传输 / HTTP transport
 * @property json JSON 编解码器 / JSON codec
 * @property tenantId 默认租户 ID / Default tenant ID
 * @property traceIdProvider trace ID 提供器 / Trace ID provider
 * @property objectStoragePort 载荷与结果对象存储 / Payload and result object storage
 * @property payloadPathProvider 载荷对象路径生成器 / Payload object path provider
 * @property requestIdProvider 请求 ID 生成器 / Request ID provider
 * @property resumeMode HTTP 恢复模式 / HTTP resume mode
 * @property pollInterval 任务轮询间隔 / Task poll interval
*/
class RemoteSolverHttpClient(
    private val baseUrl: String,
    private val transport: RemoteSolverHttpTransport = JavaNetRemoteSolverHttpTransport(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
    private val tenantId: TenantId? = null,
    private val traceIdProvider: () -> TraceId? = { null },
    private val objectStoragePort: ObjectStoragePort? = null,
    private val payloadPathProvider: (TaskId, SliceId, TenantId) -> ObjectPath = { taskId, sliceId, tenantId ->
        ObjectPath.of("${tenantId.value}/payloads/${taskId.value}/${sliceId.value}.json")
    },
    private val requestIdProvider: (TaskId, SliceId, TenantId) -> RequestId = { taskId, _, _ ->
        RequestId.of(taskId.value)
    },
    private val resumeMode: RemoteSolverHttpResumeMode = RemoteSolverHttpResumeMode.STRICT_CHECKPOINT,
    private val pollInterval: Duration = 200.milliseconds
) : SolverExecutionPort {
    private val normalizedBaseUrl = baseUrl.trim().trimEnd('/')

    /**
     * 将远程求解器错误码转换为框架错误码。 / Convert remote solver error code to framework error code.
     *
     * @return 对应的框架错误码 / Corresponding framework error code
    */
    private fun RemoteSolverErrorCode.toErrorCode(): ErrorCode {
        return when (this) {
            RemoteSolverErrorCode.INVALID_ARGUMENT -> ErrorCode.IllegalArgument
            RemoteSolverErrorCode.INTERNAL_ERROR -> ErrorCode.ApplicationError
            else -> ErrorCode.ApplicationFailed
        }
    }

    /**
     * 构建远程求解器错误消息。 / Build remote solver error message.
     *
     * @param code 错误码 / Error code
     * @param message 错误描述 / Error description
     * @param metadata 附加元数据 / Additional metadata
     * @return 格式化的错误消息 / Formatted error message
    */
    private fun remoteErrorMessage(
        code: RemoteSolverErrorCode,
        message: String,
        metadata: Map<String, String> = emptyMap()
    ): String {
        return if (metadata.isEmpty()) {
            "Remote solver error ${code.name}: $message"
        } else {
            "Remote solver error ${code.name}: $message metadata=$metadata"
        }
    }

    /**
     * 构建远程求解器失败结果，包含详细错误信息。 / Build remote solver failure result with detailed error info.
     *
     * @param code 远程错误码 / Remote error code
     * @param message 错误描述 / Error description
     * @param metadata 附加元数据 / Additional metadata
     * @param httpStatus HTTP 状态码（可选）/ HTTP status code (optional)
     * @param taskId 任务 ID（可选）/ Task ID (optional)
     * @param sliceId 切片 ID（可选）/ Slice ID (optional)
     * @param requestId 请求 ID（可选）/ Request ID (optional)
     * @return 失败的 Ret 结果 / Failed Ret result
    */
    private fun <T> failedRemote(
        code: RemoteSolverErrorCode,
        message: String,
        metadata: Map<String, String> = emptyMap(),
        httpStatus: Int? = null,
        taskId: String? = null,
        sliceId: String? = null,
        requestId: String? = null
    ): Ret<T> {
        val detail = RemoteSolverFailureDetail(
            code = code,
            message = message,
            metadata = metadata,
            httpStatus = httpStatus,
            taskId = taskId,
            sliceId = sliceId,
            requestId = requestId
        )
        return Failed(
            ExErr(
                code = code.toErrorCode(),
                message = remoteErrorMessage(code = code, message = message, metadata = metadata),
                value = detail
            )
        )
    }

    /**
     * 使用 HTTP 传输插件构造客户端。 / Construct client with HTTP transport plugin.
     *
     * @param baseUrl dispatcher 基础地址 / Dispatcher base URL
     * @param transportPlugin HTTP 传输插件 / HTTP transport plugin
     * @param transportConfig HTTP 传输配置 / HTTP transport config
     * @param json JSON 编解码器 / JSON codec
     * @param tenantId 默认租户 ID / Default tenant ID
     * @param traceIdProvider trace ID 提供器 / Trace ID provider
     * @param objectStoragePort 载荷与结果对象存储 / Payload and result object storage
     * @param payloadPathProvider 载荷对象路径生成器 / Payload object path provider
     * @param requestIdProvider 请求 ID 生成器 / Request ID provider
     * @param resumeMode HTTP 恢复模式 / HTTP resume mode
     * @param pollInterval 任务轮询间隔 / Task poll interval
    */
    constructor(
        baseUrl: String,
        transportPlugin: RemoteSolverHttpTransportPlugin,
        transportConfig: RemoteSolverHttpTransportConfig = RemoteSolverHttpTransportConfig(),
        json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        },
        tenantId: TenantId? = null,
        traceIdProvider: () -> TraceId? = { null },
        objectStoragePort: ObjectStoragePort? = null,
        payloadPathProvider: (TaskId, SliceId, TenantId) -> ObjectPath = { taskId, sliceId, tenantId ->
            ObjectPath.of("${tenantId.value}/payloads/${taskId.value}/${sliceId.value}.json")
        },
        requestIdProvider: (TaskId, SliceId, TenantId) -> RequestId = { taskId, _, _ ->
            RequestId.of(taskId.value)
        },
        resumeMode: RemoteSolverHttpResumeMode = RemoteSolverHttpResumeMode.STRICT_CHECKPOINT,
        pollInterval: Duration = 200.milliseconds
    ) : this(
        baseUrl = baseUrl,
        transport = transportPlugin.create(transportConfig),
        json = json,
        tenantId = tenantId,
        traceIdProvider = traceIdProvider,
        objectStoragePort = objectStoragePort,
        payloadPathProvider = payloadPathProvider,
        requestIdProvider = requestIdProvider,
        resumeMode = resumeMode,
        pollInterval = pollInterval
    )

    init {
        require(normalizedBaseUrl.isNotBlank()) { "baseUrl must not be blank." }
    }

    override suspend fun start(
        payload: SolvePayload,
        taskId: TaskId,
        sliceId: SliceId,
        nodeId: NodeId,
        tenantId: TenantId
    ): Ret<ExecutionHandle> {
        if (payload.modelData.modelType == NormalizedModelType.CP) {
            when (val capabilities = probeCapabilities()) {
                is Ok -> {
                    val supportsCapabilitySchema = capabilities.value.schemaVersion
                        .substringBefore('.')
                        .toIntOrNull() == 1
                    val supportsProtocol = "2.0" in capabilities.value.protocolVersions
                    val supportsCp = capabilities.value.supportedModelTypes.any {
                        it.equals(NormalizedModelType.CP.name, ignoreCase = true)
                    }
                    if (!supportsCapabilitySchema || !supportsProtocol || !supportsCp) {
                        return failedRemote(
                            code = RemoteSolverErrorCode.INVALID_ARGUMENT,
                            message = "Remote solver does not advertise CP protocol/model capability.",
                            metadata = mapOf(
                                "capabilitySchemaVersion" to capabilities.value.schemaVersion,
                                "requiredProtocol" to "2.0",
                                "protocolVersions" to capabilities.value.protocolVersions.sorted().joinToString(","),
                                "supportedModelTypes" to capabilities.value.supportedModelTypes.sorted().joinToString(",")
                            ),
                            taskId = taskId.value,
                            sliceId = sliceId.value
                        )
                    }
                }
                is Failed -> return Failed(capabilities.error)
                is Fatal -> return Fatal(capabilities.errors)
            }
        }
        val payloadRef = when (val result = putPayload(
            payload = payload,
            taskId = taskId,
            sliceId = sliceId,
            tenantId = tenantId
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val response = when (val result = submit(
            RemoteTaskSubmitRequest(
                payloadRef = payloadRef.path,
                requestId = requestIdProvider(taskId, sliceId, tenantId),
                tenantId = tenantId,
                complexity = payload.scheduling?.complexity,
                timeSensitivity = payload.scheduling?.timeSensitivity,
                priority = payload.scheduling?.priority,
                budgetScope = payload.scheduling?.budgetScope,
                budgetLimit = payload.scheduling?.budgetLimit,
                deadline = payload.scheduling?.deadline,
                scheduling = payload.scheduling
            )
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (!response.accepted || response.status == TaskStatus.UNKNOWN) {
            return failedRemote(
                code = RemoteSolverErrorCode.INVALID_TASK_STATE_TRANSITION,
                message = response.message.ifBlank { "Remote task submission returned an unknown or rejected status." },
                metadata = mapOf(
                    "taskId" to response.taskId.value,
                    "sliceId" to sliceId.value,
                    "status" to response.status.name,
                    "accepted" to "false"
                ),
                taskId = response.taskId.value,
                sliceId = sliceId.value
            )
        }
        return Ok(ExecutionHandle(
            handleId = HandleId.of(response.taskId.value),
            taskId = response.taskId,
            sliceId = sliceId,
            nodeId = nodeId,
            startedAt = Clock.System.now()
        ))
    }

    override suspend fun resume(
        payload: SolvePayload,
        checkpoint: ObjectRef,
        taskId: TaskId,
        sliceId: SliceId,
        nodeId: NodeId,
        tenantId: TenantId
    ): Ret<ExecutionHandle> {
        // The canonical HTTP endpoint resumes a task's server-selected latest
        // checkpoint and has no checkpoint selector. Never silently substitute
        // that behavior for a caller-provided reference unless latest mode was explicit.
        if (resumeMode == RemoteSolverHttpResumeMode.STRICT_CHECKPOINT) {
            return failedRemote(
                code = RemoteSolverErrorCode.INVALID_ARGUMENT,
                message = "HTTP task resume API does not support checkpoint-specific resume; latest resume requires explicit SERVER_TASK_LATEST_CHECKPOINT mode.",
                metadata = mapOf(
                    "taskId" to taskId.value,
                    "checkpointPath" to checkpoint.path.value,
                    "resumeMode" to resumeMode.name
                ),
                taskId = taskId.value,
                sliceId = sliceId.value
            )
        }
        val sourceCheckpoint = when (val result = validateResumeCheckpoint(
            payload = payload,
            checkpoint = checkpoint,
            taskId = taskId,
            sliceId = sliceId,
            tenantId = tenantId
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val action = when (val result = resume(taskId = taskId)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (!action.accepted || action.status == TaskStatus.FAILED || action.status == TaskStatus.UNKNOWN) {
            return failedRemote(
                code = RemoteSolverErrorCode.INVALID_TASK_STATE_TRANSITION,
                message = action.message ?: "Remote task resume returned an unknown or rejected status.",
                metadata = mapOf(
                    "taskId" to taskId.value,
                    "status" to action.status.name,
                    "resumeMode" to resumeMode.name
                ),
                taskId = taskId.value,
                sliceId = sliceId.value
            )
        }
        val actionIdentityError = listOf(
            "taskId" to (action.taskId.value == taskId.value),
            "tenantId" to (action.tenantId?.value == tenantId.value),
            "runId" to (action.runId != null && action.runId == sourceCheckpoint.runId),
            "attemptId" to (action.attemptId != null && action.attemptId == sourceCheckpoint.attemptId),
            "modelFingerprint" to (action.modelFingerprint != null && action.modelFingerprint == sourceCheckpoint.modelFingerprint),
            "configurationFingerprint" to (action.configurationFingerprint != null &&
                action.configurationFingerprint == sourceCheckpoint.configurationFingerprint),
            "solverFingerprint" to (action.solverFingerprint != null &&
                action.solverFingerprint == sourceCheckpoint.solverFingerprint)
        ).filterNot { it.second }.map { it.first }
        if (actionIdentityError.isNotEmpty()) {
            return failedRemote(
                code = RemoteSolverErrorCode.INVALID_ARGUMENT,
                message = "Remote resume action does not preserve the selected checkpoint identity.",
                metadata = mapOf(
                    "taskId" to taskId.value,
                    "checkpointPath" to checkpoint.path.value,
                    "mismatchedFields" to actionIdentityError.joinToString(","),
                    "checkpointId" to sourceCheckpoint.checkpointId,
                    "checkpointRunId" to (sourceCheckpoint.runId ?: ""),
                    "checkpointAttemptId" to (sourceCheckpoint.attemptId ?: "")
                ),
                taskId = taskId.value,
                sliceId = sliceId.value
            )
        }
        return Ok(ExecutionHandle(
            handleId = HandleId.of(taskId.value),
            taskId = taskId,
            sliceId = sliceId,
            nodeId = nodeId,
            startedAt = Clock.System.now(),
            scheduling = action.scheduling
        ))
    }

    /** Load a tenant-scoped, verified v2 checkpoint before invoking task resume. */
    private suspend fun validateResumeCheckpoint(
        payload: SolvePayload,
        checkpoint: ObjectRef,
        taskId: TaskId,
        sliceId: SliceId,
        tenantId: TenantId
    ): Ret<PortableCheckpointEnvelope> {
        val segments = checkpoint.path.value.split('/')
        if (segments.size != 4 || segments[0] != tenantId.value || segments[1] != "checkpoint" ||
            segments[2] != taskId.value || segments[3].isBlank() || segments.any { it == "." || it == ".." }
        ) {
            return failedRemote(
                code = RemoteSolverErrorCode.CHECKPOINT_EXPORT_FAILED,
                message = "Checkpoint reference is not a tenant-scoped reference for this task.",
                metadata = mapOf(
                    "taskId" to taskId.value,
                    "tenantId" to tenantId.value,
                    "checkpointPath" to checkpoint.path.value
                ),
                taskId = taskId.value,
                sliceId = sliceId.value
            )
        }
        val storage = objectStoragePort ?: return failedRemote(
            code = RemoteSolverErrorCode.CHECKPOINT_EXPORT_FAILED,
            message = "objectStoragePort is required to verify a checkpoint before resume.",
            metadata = mapOf("taskId" to taskId.value, "checkpointPath" to checkpoint.path.value),
            taskId = taskId.value,
            sliceId = sliceId.value
        )
        val bytes = try {
            storage.get(checkpoint)
        } catch (error: Exception) {
            return failedRemote(
                code = RemoteSolverErrorCode.STORAGE_IO_FAILED,
                message = "Failed to read checkpoint before resume: ${error.message}",
                metadata = mapOf("taskId" to taskId.value, "checkpointPath" to checkpoint.path.value),
                taskId = taskId.value,
                sliceId = sliceId.value
            )
        } ?: return failedRemote(
            code = RemoteSolverErrorCode.CHECKPOINT_EXPORT_FAILED,
            message = "Checkpoint object is missing.",
            metadata = mapOf("taskId" to taskId.value, "checkpointPath" to checkpoint.path.value),
            taskId = taskId.value,
            sliceId = sliceId.value
        )
        val envelope = PortableCheckpointCodec.decodeOrNull(bytes.decodeToString())
            ?: return failedRemote(
                code = RemoteSolverErrorCode.CHECKPOINT_EXPORT_FAILED,
                message = "Checkpoint is not a verified portable v2 envelope.",
                metadata = mapOf("taskId" to taskId.value, "checkpointPath" to checkpoint.path.value),
                taskId = taskId.value,
                sliceId = sliceId.value
            )
        val pathCheckpointId = segments[3]
        val requiredEnvelopeFields = mapOf(
            "schemaVersion" to envelope.schemaVersion,
            "sourceFormat" to envelope.sourceFormat,
            "checkpointId" to envelope.checkpointId,
            "identitySchemaVersion" to envelope.identitySchemaVersion,
            "identityNamespace" to envelope.identityNamespace,
            "modelFingerprint" to envelope.modelFingerprint,
            "configurationFingerprint" to envelope.configurationFingerprint,
            "solverFingerprint" to envelope.solverFingerprint,
            "runId" to envelope.runId,
            "attemptId" to envelope.attemptId,
            "integritySha256" to envelope.integritySha256
        )
        if (requiredEnvelopeFields.any { it.value.isNullOrBlank() } ||
            envelope.schemaVersion != "3.0" || envelope.sourceFormat != "v2" ||
            envelope.checkpointId != pathCheckpointId || envelope.runId != taskId.value ||
            envelope.attemptId?.let { !pathCheckpointId.startsWith("$it-") } != false
        ) {
            return failedRemote(
                code = RemoteSolverErrorCode.CHECKPOINT_EXPORT_FAILED,
                message = "Checkpoint identity is incomplete or does not belong to this task/reference.",
                metadata = requiredEnvelopeFields.mapValues { it.value ?: "" } +
                    mapOf("taskId" to taskId.value, "checkpointPath" to checkpoint.path.value),
                taskId = taskId.value,
                sliceId = sliceId.value
            )
        }
        val expectedModel = payload.scheduling?.modelFingerprint
            ?: payload.extension["modelFingerprint"]
            ?: payload.modelData.rawBytes
                ?.takeIf { payload.modelData.format == "ospf-cp-snapshot-json" }
                ?.let { PortableCheckpointCodec.sha256(it.decodeToString()) }
        val expectedConfiguration = payload.scheduling?.metadata?.get("configurationFingerprint")
            ?: payload.extension["configurationFingerprint"]
        val expectedSolver = payload.scheduling?.metadata?.get("solverFingerprint")
            ?: payload.extension["solverFingerprint"]
        val expected = mapOf(
            "modelFingerprint" to expectedModel,
            "configurationFingerprint" to expectedConfiguration,
            "solverFingerprint" to expectedSolver
        )
        if (expected.any { it.value.isNullOrBlank() }) {
            return failedRemote(
                code = RemoteSolverErrorCode.INVALID_ARGUMENT,
                message = "Resume payload must declare model, configuration, and solver fingerprints.",
                metadata = expected.mapValues { it.value ?: "" } + mapOf("taskId" to taskId.value),
                taskId = taskId.value,
                sliceId = sliceId.value
            )
        }
        val mismatched = expected.filter { (key, value) ->
            when (key) {
                "modelFingerprint" -> value != envelope.modelFingerprint
                "configurationFingerprint" -> value != envelope.configurationFingerprint
                "solverFingerprint" -> value != envelope.solverFingerprint
                else -> true
            }
        }.keys
        if (mismatched.isNotEmpty()) {
            return failedRemote(
                code = RemoteSolverErrorCode.INVALID_ARGUMENT,
                message = "Resume payload identity does not match the selected checkpoint.",
                metadata = mapOf(
                    "taskId" to taskId.value,
                    "checkpointPath" to checkpoint.path.value,
                    "mismatchedFields" to mismatched.joinToString(",")
                ),
                taskId = taskId.value,
                sliceId = sliceId.value
            )
        }
        return Ok(envelope)
    }

    override suspend fun awaitSliceEnd(handle: ExecutionHandle, quantum: Duration): Ret<SliceResult> {
        if (quantum <= Duration.ZERO) {
            return Failed(ErrorCode.IllegalArgument, "quantum must be positive.")
        }
        val startedAt = Clock.System.now()
        while (true) {
            val wallElapsed = Clock.System.now() - startedAt
            val view = when (val result = get(handle.taskId)) {
                is Ok -> result.value ?: return failedRemote(
                    code = RemoteSolverErrorCode.TASK_FAILED,
                    message = "Remote task is not found: ${handle.taskId}.",
                    metadata = mapOf("taskId" to handle.taskId.value),
                    taskId = handle.taskId.value,
                    sliceId = handle.sliceId.value
                )
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val elapsed = if (wallElapsed < Duration.ZERO) Duration.ZERO else wallElapsed
            when (view.status) {
                TaskStatus.COMPLETED,
                TaskStatus.FAILED,
                TaskStatus.STOPPED -> {
                    val result = when (val ret = fetchFinalResult(handle)) {
                        is Ok -> ret.value
                        is Failed -> return Failed(ret.error)
                        is Fatal -> return Fatal(ret.errors)
                    }
                    return Ok(view.toSliceResult(handle, result, elapsed))
                }

                TaskStatus.SUSPENDED -> {
                    return Ok(view.toSliceResult(handle, result = null, elapsed = elapsed))
                }

                TaskStatus.UNKNOWN -> {
                    return failedRemote(
                        code = RemoteSolverErrorCode.INVALID_ARGUMENT,
                        message = "Remote task returned an unknown status; refusing to infer a slice result.",
                        metadata = mapOf(
                            "taskId" to handle.taskId.value,
                            "sliceId" to handle.sliceId.value,
                            "status" to view.status.name
                        ),
                        taskId = handle.taskId.value,
                        sliceId = handle.sliceId.value
                    )
                }

                else -> Unit
            }

            // The dispatcher owns the quantum and performs checkpoint/stop/requeue.
            // A client-side deadline would turn a normal server suspension into a task stop.
            delay(pollInterval.coerceAtLeast(1.milliseconds))
        }
    }

    /** Map a task observation to the canonical slice contract. / 将任务观测映射为规范切片契约。 */
    private fun RemoteTaskView.toSliceResult(
        handle: ExecutionHandle,
        result: SolveResult?,
        elapsed: Duration,
        forceTimeout: Boolean = false
    ): SliceResult {
        val unknownSemantics = hasUnknownOutcomeSemantics() || result?.hasUnknownOutcomeSemantics() == true
        val checkpoint = result?.checkpointRef ?: latestCheckpointRef ?: scheduling?.checkpointRef
        val incumbent = result?.incumbentRef ?: incumbentRef ?: scheduling?.incumbentRef
        val inferredFeasible = result?.feasible ?: (
            solutionPresence == RemoteSolutionPresence.INCUMBENT ||
                solutionPresence == RemoteSolutionPresence.OPTIMAL ||
                incumbent != null
            )
        val feasible = if (unknownSemantics) false else inferredFeasible
        val inferredOutcome = result?.outcome ?: outcome ?: when {
            status == TaskStatus.STOPPED -> SliceOutcome.CANCELLED
            status == TaskStatus.FAILED -> SliceOutcome.FAILED
            status == TaskStatus.COMPLETED -> SliceOutcome.COMPLETED
            checkpoint != null -> SliceOutcome.CHECKPOINTED
            feasible -> SliceOutcome.RESUMABLE
            else -> SliceOutcome.PREEMPTED
        }
        val effectiveOutcome = if (unknownSemantics) SliceOutcome.UNKNOWN else inferredOutcome
        val effectiveTermination = if (unknownSemantics) {
            RemoteTerminationReason.UNKNOWN
        } else {
            when {
                forceTimeout -> RemoteTerminationReason.TIME_LIMIT
                status == TaskStatus.STOPPED -> RemoteTerminationReason.CANCELLED
                status == TaskStatus.FAILED -> result?.terminationReason
                    ?.takeIf { it != RemoteTerminationReason.COMPLETED }
                    ?: terminationReason?.takeIf { it != RemoteTerminationReason.COMPLETED }
                    ?: RemoteTerminationReason.BACKEND_FAILURE
                else -> result?.terminationReason ?: terminationReason
                    ?: if (effectiveOutcome == SliceOutcome.COMPLETED) {
                        RemoteTerminationReason.COMPLETED
                    } else {
                        RemoteTerminationReason.TIME_LIMIT
                    }
            }
        }
        val effectiveScheduling = (result?.scheduling ?: scheduling ?: handle.scheduling)?.let {
            it.copy(
                taskId = it.taskId ?: taskId,
                sliceId = it.sliceId ?: (sliceId ?: handle.sliceId),
                nodeId = it.nodeId ?: currentNodeId,
                checkpointRef = it.checkpointRef ?: checkpoint,
                incumbentRef = it.incumbentRef ?: incumbent,
                modelFingerprint = it.modelFingerprint ?: (result?.modelFingerprint ?: modelFingerprint),
                outcome = if (unknownSemantics) SliceOutcome.UNKNOWN else it.outcome ?: effectiveOutcome
            )
        }
        val effectiveStatistics = result?.statistics ?: statistics
        val effectiveProvenance = buildMap {
            putAll(provenance)
            putAll(result?.provenance ?: emptyMap())
        }
        val effectiveFingerprints = buildMap {
            putAll(identityFingerprints())
            putAll(result?.fingerprints ?: emptyMap())
        }
        val effectiveFingerprintSchemas = buildMap {
            putAll(identityFingerprintSchemas())
            putAll(result?.fingerprintSchemas ?: emptyMap())
        }
        return SliceResult(
            sliceId = sliceId ?: handle.sliceId,
            completed = status != TaskStatus.UNKNOWN &&
                (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED || status == TaskStatus.STOPPED),
            feasible = feasible,
            objectiveValue = result?.objectiveValue ?: objectiveValue,
            objectiveValueInt64 = result?.objectiveValueInt64 ?: objectiveValueInt64,
            gap = result?.gap ?: gap,
            elapsed = result?.elapsed ?: elapsed,
            message = result?.message ?: if (forceTimeout) {
                "Remote slice quantum expired while task status was $status."
            } else {
                null
            },
            schemaVersion = result?.schemaVersion ?: "1.0",
            problemStatus = if (unknownSemantics) {
                RemoteProblemStatus.UNKNOWN
            } else {
                result?.problemStatus ?: problemStatus ?: if (feasible) {
                    RemoteProblemStatus.FEASIBLE
                } else {
                    RemoteProblemStatus.UNKNOWN
                }
            },
            terminationReason = effectiveTermination,
            solutionPresence = if (unknownSemantics) {
                RemoteSolutionPresence.UNKNOWN
            } else {
                result?.solutionPresence ?: solutionPresence ?: if (feasible) {
                    RemoteSolutionPresence.INCUMBENT
                } else {
                    RemoteSolutionPresence.NONE
                }
            },
            proofStatus = if (unknownSemantics) {
                RemoteProofStatus.UNKNOWN
            } else {
                result?.proofStatus ?: proofStatus ?: RemoteProofStatus.NONE
            },
            resultRef = result?.resultRef ?: latestResultRef,
            provenance = effectiveProvenance,
            fingerprints = effectiveFingerprints,
            fingerprintSchemas = effectiveFingerprintSchemas,
            statistics = effectiveStatistics,
            diagnostics = (result?.diagnostics ?: emptyMap()) + diagnostics,
            runId = result?.runId ?: runId,
            attemptId = result?.attemptId ?: attemptId,
            artifactDigest = result?.artifactDigest ?: artifactDigest,
            bestBound = result?.bestBound ?: bestBound ?: bound
                ?: effectiveStatistics["bestBound"]?.toDoubleOrNull()?.let(::Flt64),
            checkpointRef = checkpoint,
            incumbentRef = incumbent,
            modelFingerprint = result?.modelFingerprint ?: modelFingerprint
                ?: effectiveScheduling?.modelFingerprint,
            scheduling = effectiveScheduling,
            outcome = effectiveOutcome
        )
    }

    override suspend fun exportCheckpoint(handle: ExecutionHandle): Ret<ObjectRef?> {
        return get(handle.taskId).map { it?.latestCheckpointRef }
    }

    override suspend fun fetchFinalResult(handle: ExecutionHandle): Ret<SolveResult?> {
        val view = when (val result = get(handle.taskId)) {
            is Ok -> result.value ?: return Ok(null)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val resultRef = view.latestResultRef
        if (resultRef == null) {
            return Ok(view.toObservedSolveResult(handle))
        }
        val storage = objectStoragePort ?: return Ok(view.toObservedSolveResult(handle))
        val bytes = storage.get(resultRef) ?: return Ok(view.toObservedSolveResult(handle))
        val solution = try {
            decodeSerializedSolution(bytes.decodeToString())
        } catch (e: Exception) {
            return failedRemote(
                code = RemoteSolverErrorCode.INTERNAL_ERROR,
                message = "Failed to decode remote solve result: ${e.message}",
                metadata = mapOf(
                    "taskId" to handle.taskId.value,
                    "sliceId" to handle.sliceId.value,
                    "resultRef" to resultRef.path.value
                ),
                taskId = handle.taskId.value,
                sliceId = handle.sliceId.value
            )
        }
        val result = SolveResult(
            feasible = solution.feasible,
            optimal = solution.optimal,
            objectiveValue = solution.objectiveValue ?: view.objectiveValue,
            objectiveValueInt64 = solution.objectiveValueInt64 ?: view.objectiveValueInt64,
            gap = solution.gap ?: view.gap,
            elapsed = solution.elapsed,
            checkpointRef = view.latestCheckpointRef ?: view.scheduling?.checkpointRef,
            resultRef = resultRef,
            message = solution.message,
            schemaVersion = solution.schemaVersion,
            problemStatus = solution.problemStatus ?: if (solution.feasible) {
                RemoteProblemStatus.FEASIBLE
            } else {
                view.problemStatus ?: RemoteProblemStatus.UNKNOWN
            },
            terminationReason = solution.terminationReason ?: view.terminationReason
                ?: RemoteTerminationReason.COMPLETED,
            solutionPresence = solution.solutionPresence ?: when {
                solution.optimal -> RemoteSolutionPresence.OPTIMAL
                solution.feasible -> RemoteSolutionPresence.INCUMBENT
                else -> view.solutionPresence ?: RemoteSolutionPresence.NONE
            },
            proofStatus = solution.proofStatus ?: view.proofStatus ?: RemoteProofStatus.NONE,
            provenance = view.provenance + solution.provenance,
            fingerprints = view.identityFingerprints() + solution.fingerprints,
            fingerprintSchemas = view.identityFingerprintSchemas() + solution.fingerprintSchemas,
            statistics = solution.statistics,
            diagnostics = solution.diagnostics,
            runId = solution.runId ?: view.runId,
            attemptId = solution.attemptId ?: view.attemptId,
            artifactDigest = solution.artifactDigest ?: view.artifactDigest,
            bestBound = view.bestBound ?: view.bound
                ?: solution.statistics["bestBound"]?.toDoubleOrNull()?.let(::Flt64),
            incumbentRef = view.incumbentRef ?: view.scheduling?.incumbentRef,
            modelFingerprint = view.modelFingerprint ?: view.scheduling?.modelFingerprint
                ?: solution.fingerprints["model"],
            scheduling = view.scheduling,
            outcome = view.outcome
        )
        val unknownSemantics = view.hasUnknownOutcomeSemantics() || result.hasUnknownOutcomeSemantics()
        val terminalResult = when (view.status) {
            TaskStatus.STOPPED -> result.asTerminalFailure(RemoteTerminationReason.CANCELLED)
            TaskStatus.FAILED -> result.asTerminalFailure(
                result.terminationReason.takeIf { it != RemoteTerminationReason.COMPLETED }
                    ?: RemoteTerminationReason.BACKEND_FAILURE
            )
            else -> result
        }
        return Ok(if (unknownSemantics) terminalResult.asUnknownOutcomeFailure() else terminalResult)
    }

    /** Build a final result from an extended task view when no artifact is available. */
    private fun RemoteTaskView.toObservedSolveResult(handle: ExecutionHandle): SolveResult? {
        val hasObservation = status == TaskStatus.COMPLETED || status == TaskStatus.FAILED ||
            status == TaskStatus.STOPPED || status == TaskStatus.UNKNOWN || objectiveValue != null ||
            objectiveValueInt64 != null || gap != null || incumbentRef != null || latestResultRef != null ||
            hasUnknownOutcomeSemantics()
        if (!hasObservation) {
            return null
        }
        val feasible = status == TaskStatus.COMPLETED ||
            solutionPresence == RemoteSolutionPresence.INCUMBENT ||
            solutionPresence == RemoteSolutionPresence.OPTIMAL || incumbentRef != null
        val terminalReason = when (status) {
            TaskStatus.STOPPED -> RemoteTerminationReason.CANCELLED
            TaskStatus.FAILED -> terminationReason?.takeIf { it != RemoteTerminationReason.COMPLETED }
                ?: RemoteTerminationReason.BACKEND_FAILURE
            else -> terminationReason ?: RemoteTerminationReason.COMPLETED
        }
        val result = SolveResult(
            feasible = feasible,
            optimal = solutionPresence == RemoteSolutionPresence.OPTIMAL,
            objectiveValue = objectiveValue,
            objectiveValueInt64 = objectiveValueInt64,
            gap = gap,
            elapsed = slice?.elapsed ?: Duration.ZERO,
            checkpointRef = latestCheckpointRef ?: scheduling?.checkpointRef,
            resultRef = latestResultRef,
            schemaVersion = "1.0",
            problemStatus = problemStatus ?: if (feasible) {
                RemoteProblemStatus.FEASIBLE
            } else {
                RemoteProblemStatus.UNKNOWN
            },
            terminationReason = terminalReason,
            solutionPresence = solutionPresence ?: if (feasible) {
                RemoteSolutionPresence.INCUMBENT
            } else {
                RemoteSolutionPresence.NONE
            },
            proofStatus = proofStatus ?: RemoteProofStatus.NONE,
            provenance = provenance,
            fingerprints = identityFingerprints(),
            fingerprintSchemas = identityFingerprintSchemas(),
            statistics = statistics,
            diagnostics = diagnostics,
            runId = runId,
            attemptId = attemptId,
            artifactDigest = artifactDigest,
            bestBound = bestBound ?: bound,
            incumbentRef = incumbentRef ?: scheduling?.incumbentRef,
            modelFingerprint = modelFingerprint ?: scheduling?.modelFingerprint,
            scheduling = scheduling,
            outcome = outcome
        )
        return if (hasUnknownOutcomeSemantics()) {
            result.asUnknownOutcomeFailure()
        } else {
            result
        }
    }

    private fun RemoteTaskView.hasUnknownOutcomeSemantics(): Boolean {
        return status == TaskStatus.UNKNOWN ||
            outcome == SliceOutcome.UNKNOWN ||
            terminationReason == RemoteTerminationReason.UNKNOWN ||
            solutionPresence == RemoteSolutionPresence.UNKNOWN ||
            proofStatus == RemoteProofStatus.UNKNOWN ||
            scheduling?.outcome == SliceOutcome.UNKNOWN ||
            slice?.outcome == SliceOutcome.UNKNOWN ||
            slice?.terminationReason == RemoteTerminationReason.UNKNOWN ||
            slice?.solutionPresence == RemoteSolutionPresence.UNKNOWN ||
            slice?.proofStatus == RemoteProofStatus.UNKNOWN
    }

    private fun SolveResult.hasUnknownOutcomeSemantics(): Boolean {
        return outcome == SliceOutcome.UNKNOWN ||
            terminationReason == RemoteTerminationReason.UNKNOWN ||
            solutionPresence == RemoteSolutionPresence.UNKNOWN ||
            proofStatus == RemoteProofStatus.UNKNOWN
    }

    /** Preserve unknown wire semantics instead of manufacturing a successful result. */
    private fun SolveResult.asUnknownOutcomeFailure(): SolveResult {
        return copy(
            feasible = false,
            optimal = false,
            problemStatus = RemoteProblemStatus.UNKNOWN,
            terminationReason = RemoteTerminationReason.UNKNOWN,
            solutionPresence = RemoteSolutionPresence.UNKNOWN,
            proofStatus = RemoteProofStatus.UNKNOWN,
            outcome = SliceOutcome.UNKNOWN
        )
    }

    /**
     * 将旧 artifact 与任务终态合并，避免失败或取消任务继承正常完成语义。
     * Merge a legacy artifact with the task terminal state so failure or cancellation cannot inherit completion semantics.
     *
     * @param terminationReason 任务终止原因 / Task termination reason
     * @return 终态语义一致的结果 / Result with consistent terminal semantics
     */
    private fun SolveResult.asTerminalFailure(terminationReason: RemoteTerminationReason): SolveResult {
        val hasIncumbent = feasible || solutionPresence == RemoteSolutionPresence.INCUMBENT ||
            solutionPresence == RemoteSolutionPresence.OPTIMAL || incumbentRef != null
        return copy(
            optimal = false,
            problemStatus = if (hasIncumbent) {
                RemoteProblemStatus.FEASIBLE
            } else {
                RemoteProblemStatus.UNKNOWN
            },
            terminationReason = terminationReason,
            solutionPresence = if (hasIncumbent) {
                RemoteSolutionPresence.INCUMBENT
            } else {
                RemoteSolutionPresence.NONE
            },
            proofStatus = RemoteProofStatus.NONE
        )
    }

    override suspend fun stop(handle: ExecutionHandle): Ret<Boolean> {
        val action = when (val result = stop(taskId = handle.taskId)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (!action.accepted || action.status == TaskStatus.FAILED || action.status == TaskStatus.UNKNOWN) {
            return Ok(false)
        }
        var status = action.status
        var attempts = 0
        while (!status.isTerminal() && attempts < STOP_CONFIRMATION_ATTEMPTS) {
            val view = when (val result = get(handle.taskId)) {
                is Ok -> result.value ?: return Ok(false)
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            status = view.status
            attempts += 1
            if (!status.isTerminal()) {
                delay(pollInterval.coerceAtLeast(1.milliseconds))
            }
        }
        return Ok(status == TaskStatus.STOPPED || status == TaskStatus.COMPLETED)
    }

    /**
     * 提交任务。 / Submit task.
     *
     * @param request 提交请求 / Submit request
     * @return 提交响应 / Submit response
    */
    fun submit(request: RemoteTaskSubmitRequest): Ret<RemoteTaskSubmitResponse> {
        val response = send(
            request(
                method = "POST",
                path = "/api/v1/tasks",
                body = json.encodeToString(RemoteTaskSubmitRequest.serializer(), request)
            )
        )
        return when (response) {
            is Ok -> decodeEnvelope(
                response = response.value,
                dataDeserializer = SubmitTaskHttpResponse.serializer()
            ).map { it.toDomain() }
            is Failed -> Failed(response.error)
            is Fatal -> Fatal(response.errors)
        }
    }

    /**
     * 查询服务端能力和协议版本。 / Query server capabilities and protocol versions.
     *
     * @return 服务端能力摘要 / Server capability summary
    */
    fun probeCapabilities(): Ret<RemoteSolverCapabilities> {
        val response = when (val result = send(
            request(
                method = "GET",
                path = "/api/v1/capabilities"
            )
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return decodeEnvelope(
            response = response,
            dataDeserializer = RemoteSolverCapabilities.serializer()
        )
    }

    /**
     * 发送 HTTP 请求到远程求解器。 / Send HTTP request to remote solver.
     *
     * @param request HTTP 请求 / HTTP request
     * @return HTTP 响应或失败结果 / HTTP response or failure result
    */
    private fun send(request: RemoteSolverHttpRequest): Ret<RemoteSolverHttpResponse> {
        return try {
            Ok(transport.send(request))
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            failedRemote(
                code = RemoteSolverErrorCode.INTERNAL_ERROR,
                message = "Remote solver HTTP request interrupted: ${e.message}",
                metadata = mapOf("url" to request.url, "method" to request.method)
            )
        } catch (e: Exception) {
            failedRemote(
                code = RemoteSolverErrorCode.INTERNAL_ERROR,
                message = "Remote solver HTTP request failed: ${e.message}",
                metadata = mapOf("url" to request.url, "method" to request.method)
            )
        }
    }

    /**
     * 查询任务。 / Get task.
     *
     * @param taskId 任务 ID / Task ID
     * @return 任务视图，不存在时返回 null / Task view, null if not found
    */
    fun get(taskId: TaskId): Ret<RemoteTaskView?> {
        val response = when (val result = send(
            request(
                method = "GET",
                path = "/api/v1/tasks/${taskId.value}"
            )
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (response.statusCode == 404) {
            return Ok(null)
        }
        return decodeEnvelope(
            response = response,
            dataDeserializer = TaskViewHttpResponse.serializer()
        ).map { it.toDomain() }
    }

    /**
     * 停止任务。 / Stop task.
     *
     * @param taskId 任务 ID / Task ID
     * @param request 停止请求 / Stop request
     * @return 操作响应 / Action response
    */
    fun stop(
        taskId: TaskId,
        request: RemoteTaskStopRequest = RemoteTaskStopRequest()
    ): Ret<RemoteTaskAction> {
        val response = when (val result = send(
            request(
                method = "POST",
                path = "/api/v1/tasks/${taskId.value}/stop",
                body = json.encodeToString(RemoteTaskStopRequest.serializer(), request)
            )
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return decodeEnvelope(
            response = response,
            dataDeserializer = TaskActionHttpResponse.serializer()
        ).map { it.toDomain() }
    }

    /**
     * 恢复任务。 / Resume task.
     *
     * @param taskId 任务 ID / Task ID
     * @param request 恢复请求 / Resume request
     * @return 操作响应 / Action response
    */
    fun resume(
        taskId: TaskId,
        request: RemoteTaskResumeRequest = RemoteTaskResumeRequest()
    ): Ret<RemoteTaskAction> {
        val response = when (val result = send(
            request(
                method = "POST",
                path = "/api/v1/tasks/${taskId.value}/resume",
                body = json.encodeToString(RemoteTaskResumeRequest.serializer(), request)
            )
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return decodeEnvelope(
            response = response,
            dataDeserializer = TaskActionHttpResponse.serializer()
        ).map { it.toDomain() }
    }

    /**
     * 构建远程求解器 HTTP 请求，自动注入租户和追踪头。 / Build remote solver HTTP request with auto-injected tenant and trace headers.
     *
     * @param method HTTP 方法 / HTTP method
     * @param path 请求路径 / Request path
     * @param body 请求体（可选）/ Request body (optional)
     * @return HTTP 请求对象 / HTTP request object
    */
    private fun request(
        method: String,
        path: String,
        body: String? = null
    ): RemoteSolverHttpRequest {
        val headers = linkedMapOf("Accept" to "application/json")
        if (body != null) {
            headers["Content-Type"] = "application/json"
        }
        tenantId?.let {
            headers["X-Tenant-Id"] = it.value
        }
        traceIdProvider()?.let {
            headers["X-Trace-Id"] = it.value
        }
        return RemoteSolverHttpRequest(
            method = method,
            url = "$normalizedBaseUrl$path",
            headers = headers,
            body = body
        )
    }

    /**
     * 解析 API 信封响应，提取数据或错误。 / Decode API envelope response, extract data or error.
     *
     * @param response HTTP 响应 / HTTP response
     * @param dataDeserializer 数据反序列化器 / Data deserializer
     * @return 解析后的数据或失败结果 / Parsed data or failure result
    */
    private fun <T> decodeEnvelope(
        response: RemoteSolverHttpResponse,
        dataDeserializer: KSerializer<T>
    ): Ret<T> {
        if (response.statusCode !in 200..299) {
            return decodeError(response)
        }
        val envelope = try {
            json.decodeFromString(
                ApiEnvelope.serializer(dataDeserializer),
                response.body
            )
        } catch (e: Exception) {
            return failedRemote(
                code = RemoteSolverErrorCode.INTERNAL_ERROR,
                message = "Failed to decode remote solver response: ${e.message}",
                metadata = buildMap {
                    put("status", response.statusCode.toString())
                    if (response.body.isNotBlank()) {
                        put("body", response.body.take(500))
                    }
                },
                httpStatus = response.statusCode
            )
        }
        if (envelope.code != "OK") {
            return failedRemote(
                code = envelope.code.toRemoteErrorCode(),
                message = envelope.message,
                metadata = envelope.traceId?.let { mapOf("traceId" to it) } ?: emptyMap(),
                httpStatus = response.statusCode
            )
        }
        return envelope.data
            ?.let { Ok(it) }
            ?: failedRemote(
            code = RemoteSolverErrorCode.INTERNAL_ERROR,
            message = "Remote solver response data is null.",
            metadata = envelope.traceId?.let { mapOf("traceId" to it) } ?: emptyMap(),
            httpStatus = response.statusCode
        )
    }

    /**
     * 解析错误响应，提取错误信息并构建失败结果。 / Decode error response, extract error info and build failure result.
     *
     * @param response HTTP 响应 / HTTP response
     * @return 包含错误详情的失败结果 / Failure result with error details
    */
    private fun <T> decodeError(response: RemoteSolverHttpResponse): Ret<T> {
        val body = response.body
        val envelope = runCatching {
            json.decodeFromString(
                ApiEnvelope.serializer(kotlinx.serialization.json.JsonElement.serializer()),
                body
            )
        }.getOrNull()
        return failedRemote(
            code = envelope?.code?.toRemoteErrorCode() ?: RemoteSolverErrorCode.INTERNAL_ERROR,
            message = envelope?.message ?: "Remote solver HTTP request failed with status ${response.statusCode}.",
            metadata = buildMap {
                put("status", response.statusCode.toString())
                envelope?.traceId?.let { put("traceId", it) }
                if (body.isNotBlank()) {
                    put("body", body)
                }
            },
            httpStatus = response.statusCode
        )
    }

    /**
     * 将字符串错误码转换为 RemoteSolverErrorCode 枚举。
     * Convert string error code to RemoteSolverErrorCode enum.
     *
     * @return 对应的远程求解错误码，无法识别时返回 INTERNAL_ERROR / Corresponding remote solver error code, or INTERNAL_ERROR if unrecognized
    */
    private fun String.toRemoteErrorCode(): RemoteSolverErrorCode {
        return runCatching { RemoteSolverErrorCode.valueOf(this) }
            .getOrDefault(RemoteSolverErrorCode.INTERNAL_ERROR)
    }

    /**
     * 将求解载荷上传到对象存储。 / Upload solve payload to object storage.
     *
     * @param payload 求解载荷 / Solve payload
     * @param taskId 任务 ID / Task ID
     * @param sliceId 切片 ID / Slice ID
     * @param tenantId 租户 ID / Tenant ID
     * @return 对象引用或失败结果 / Object reference or failure result
    */
    private suspend fun putPayload(
        payload: SolvePayload,
        taskId: TaskId,
        sliceId: SliceId,
        tenantId: TenantId
    ): Ret<ObjectRef> {
        val storage = objectStoragePort ?: return failedRemote(
            code = RemoteSolverErrorCode.INVALID_ARGUMENT,
            message = "objectStoragePort is required when RemoteSolverHttpClient is used as SolverExecutionPort.",
            metadata = mapOf("taskId" to taskId.value),
            taskId = taskId.value,
            sliceId = sliceId.value
        )
        return try {
            Ok(storage.put(
                path = payloadPathProvider(taskId, sliceId, tenantId),
                bytes = json.encodeToString(SolvePayload.serializer(), payload).encodeToByteArray(),
                metadata = mapOf(
                    "taskId" to taskId.value,
                    "sliceId" to sliceId.value,
                    "tenantId" to tenantId.value
                )
            ))
        } catch (e: Exception) {
            failedRemote(
                code = RemoteSolverErrorCode.STORAGE_IO_FAILED,
                message = "Failed to put remote solver payload: ${e.message}",
                metadata = mapOf("taskId" to taskId.value),
                taskId = taskId.value,
                sliceId = sliceId.value
            )
        }
    }

    /** Decode result artifacts while preserving newer enum values as explicit UNKNOWN. */
    private fun decodeSerializedSolution(body: String): SerializedSolution {
        val root = json.parseToJsonElement(body) as? JsonObject ?: return json.decodeFromString(
            SerializedSolution.serializer(),
            body
        )
        val normalized = root
            .filterKnownEnum("problemStatus", setOf(
                RemoteProblemStatus.FEASIBLE.name,
                RemoteProblemStatus.INFEASIBLE.name,
                RemoteProblemStatus.UNBOUNDED.name,
                RemoteProblemStatus.INFEASIBLE_OR_UNBOUNDED.name,
                RemoteProblemStatus.UNKNOWN.name
            ))
            .filterKnownEnum("terminationReason", setOf(
                RemoteTerminationReason.COMPLETED.name,
                RemoteTerminationReason.TIME_LIMIT.name,
                RemoteTerminationReason.NODE_LIMIT.name,
                RemoteTerminationReason.ITERATION_LIMIT.name,
                RemoteTerminationReason.SOLUTION_LIMIT.name,
                RemoteTerminationReason.OBJECTIVE_LIMIT.name,
                RemoteTerminationReason.CANCELLED.name,
                RemoteTerminationReason.INTERRUPTED.name,
                RemoteTerminationReason.NUMERICAL_FAILURE.name,
                RemoteTerminationReason.BACKEND_FAILURE.name,
                RemoteTerminationReason.UNKNOWN.name
            ))
            .filterKnownEnum("solutionPresence", setOf(
                RemoteSolutionPresence.NONE.name,
                RemoteSolutionPresence.INCUMBENT.name,
                RemoteSolutionPresence.OPTIMAL.name,
                RemoteSolutionPresence.UNKNOWN.name
            ))
            .filterKnownEnum("proofStatus", setOf(
                RemoteProofStatus.NONE.name,
                RemoteProofStatus.CLAIMED.name,
                RemoteProofStatus.VERIFIED.name,
                RemoteProofStatus.UNKNOWN.name
            ))
        return json.decodeFromJsonElement(SerializedSolution.serializer(), normalized)
    }
}

/**
 * HTTP 恢复模式。 / HTTP resume mode.
*/
enum class RemoteSolverHttpResumeMode {
    /** 恢复服务端任务的最新检查点 / Resume server task from its latest checkpoint */
    SERVER_TASK_LATEST_CHECKPOINT,

    /** 严格要求按指定检查点恢复，当前 HTTP API 不支持 / Require checkpoint-specific resume, unsupported by current HTTP API */
    STRICT_CHECKPOINT
}

/**
 * 远程求解 HTTP 传输接口。 / Remote solver HTTP transport.
*/
fun interface RemoteSolverHttpTransport {

    /**
     * 发送 HTTP 请求。 / Send HTTP request.
     *
     * @param request HTTP 请求 / HTTP request
     * @return HTTP 响应 / HTTP response
    */
    fun send(request: RemoteSolverHttpRequest): RemoteSolverHttpResponse
}

/**
 * 远程求解 HTTP 请求。 / Remote solver HTTP request.
 *
 * @property method HTTP 方法 / HTTP method
 * @property url 请求 URL / Request URL
 * @property headers 请求头 / Request headers
 * @property body 请求体 / Request body
*/
data class RemoteSolverHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null
)

/**
 * 远程求解 HTTP 响应。 / Remote solver HTTP response.
 *
 * @property statusCode HTTP 状态码 / HTTP status code
 * @property body 响应体 / Response body
*/
data class RemoteSolverHttpResponse(
    val statusCode: Int,
    val body: String
)

/**
 * JDK 标准 HTTP 传输实现。 / JDK standard HTTP transport implementation.
 *
 * @property client JDK HTTP 客户端 / JDK HTTP client
 * @property config HTTP 传输配置 / HTTP transport config
*/
class JavaNetRemoteSolverHttpTransport(
    private val client: HttpClient = HttpClient.newHttpClient(),
    private val config: RemoteSolverHttpTransportConfig = RemoteSolverHttpTransportConfig()
) : RemoteSolverHttpTransport {
    override fun send(request: RemoteSolverHttpRequest): RemoteSolverHttpResponse {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(request.url))
        config.requestTimeout?.let {
            builder.timeout(java.time.Duration.ofMillis(it.inWholeMilliseconds))
        }
        (config.headers + request.headers).forEach { (name, value) ->
            builder.header(name, value)
        }
        val bodyPublisher = request.body?.let { HttpRequest.BodyPublishers.ofString(it) }
            ?: HttpRequest.BodyPublishers.noBody()
        val response = client.send(
            builder.method(request.method, bodyPublisher).build(),
            HttpResponse.BodyHandlers.ofString()
        )
        return RemoteSolverHttpResponse(
            statusCode = response.statusCode(),
            body = response.body()
        )
    }
}

/**
 * 远程任务提交请求。 / Remote task submit request.
 *
 * @property payloadRef 载荷对象路径 / Payload object path
 * @property requestId 幂等请求 ID / Idempotent request ID
 * @property tenantId 租户 ID / Tenant ID
 * @property complexity 任务复杂度 / Task complexity
 * @property timeSensitivity 时间敏感度 / Time sensitivity
 * @property priority 优先级 / Priority
 * @property budgetScope 预算范围 / Budget scope
 * @property budgetLimit 预算上限 / Budget limit
 * @property deadline 截止时间戳 / Deadline timestamp
 * @property scheduling V1.2 调度请求 / V1.2 scheduling request
*/
@Serializable
data class RemoteTaskSubmitRequest(
    val payloadRef: ObjectPath,
    val requestId: RequestId? = null,
    val tenantId: TenantId? = null,
    val complexity: TaskComplexity? = null,
    val timeSensitivity: TimeSensitivity? = null,
    val priority: Int? = null,
    val budgetScope: BudgetScopeId? = null,
    val budgetLimit: Flt64? = null,
    @SerialName("deadlineEpochMs")
    @Serializable(with = RemoteSolverEpochMillisecondsInstantSerializer::class)
    val deadline: Instant? = null,
    val scheduling: SchedulingRequest? = null
)

/**
 * 远程任务提交响应。 / Remote task submit response.
 *
 * @property taskId 任务 ID / Task ID
 * @property accepted 是否接受 / Whether accepted
 * @property status 任务状态 / Task status
 * @property message 响应消息 / Response message
*/
data class RemoteTaskSubmitResponse(
    val taskId: TaskId,
    val accepted: Boolean,
    val status: TaskStatus,
    val message: String
)

/**
 * 远程任务视图。 / Remote task view.
 *
 * @property taskId 任务 ID / Task ID
 * @property tenantId 租户 ID / Tenant ID
 * @property status 任务状态 / Task status
 * @property currentNodeId 当前节点 ID / Current node ID
 * @property latestCheckpointRef 最新检查点引用 / Latest checkpoint reference
 * @property latestResultRef 最新结果引用 / Latest result reference
 * @property consumedCost 已消耗成本 / Consumed cost
*/
data class RemoteTaskView(
    val taskId: TaskId,
    val tenantId: TenantId,
    val status: TaskStatus,
    val currentNodeId: NodeId? = null,
    val latestCheckpointRef: ObjectRef? = null,
    val latestResultRef: ObjectRef? = null,
    val consumedCost: Flt64 = Flt64.zero,
    val requestId: RequestId? = null,
    val complexity: TaskComplexity? = null,
    val timeSensitivity: TimeSensitivity? = null,
    val priority: Int? = null,
    val budgetScope: BudgetScopeId? = null,
    val budgetLimit: Flt64? = null,
    val sliceId: SliceId? = null,
    val objectiveValue: Flt64? = null,
    val objectiveValueInt64: Long? = null,
    val bestBound: Flt64? = null,
    val bound: Flt64? = null,
    val gap: Flt64? = null,
    val progress: Flt64? = null,
    val deadlineRisk: Flt64? = null,
    val deadline: Instant? = null,
    val dispatchId: DispatchId? = null,
    val runId: String? = null,
    val attemptId: String? = null,
    val artifactDigest: String? = null,
    val incumbentRef: ObjectRef? = null,
    val modelFingerprint: String? = null,
    val modelFingerprintSchema: String? = null,
    val configurationFingerprint: String? = null,
    val configurationFingerprintSchema: String? = null,
    val solverFingerprint: String? = null,
    val solverFingerprintSchema: String? = null,
    val fingerprints: Map<String, String> = emptyMap(),
    val provenance: Map<String, String> = emptyMap(),
    val scheduling: SchedulingDecision? = null,
    val outcome: SliceOutcome? = null,
    val problemStatus: RemoteProblemStatus? = null,
    val terminationReason: RemoteTerminationReason? = null,
    val solutionPresence: RemoteSolutionPresence? = null,
    val proofStatus: RemoteProofStatus? = null,
    val statistics: Map<String, String> = emptyMap(),
    val diagnostics: Map<String, String> = emptyMap(),
    val fingerprintSchemas: Map<String, String> = emptyMap(),
    val slice: SliceResult? = null
)

/** Provenance shape returned by the Kotlin dispatcher task-action API. */
@Serializable
data class RemoteTaskActionProvenance(
    val solverId: String = "unknown",
    val backendName: String = "unknown",
    val backendVersion: String? = null,
    val pluginVersion: String? = null,
    val requestedConfiguration: Map<String, String> = emptyMap(),
    val effectiveConfiguration: Map<String, String> = emptyMap(),
    val threadCount: Int? = null,
    val randomSeed: Long? = null,
    val deterministic: Boolean? = null,
    val environmentSummary: Map<String, String> = emptyMap()
)

/**
 * Kotlin dispatcher task-action API 返回的取消事实形状。 / Cancellation-record shape returned by the Kotlin dispatcher task-action API.
 *
 * `origin` 与 checkpoint envelope 的取消链共用**同一份规范代码词表**（由跨语言契约
 * `analysis-fixtures/checkpoint-wire-contract.tsv` 的 `[cancellation-origin]` 段规定），因此不能把它
 * 当作自由文本：`backend` / `future` 这类本侧没有专用变体的代码也必须能原样往返。请用
 * [toCancellationRecord] / [fromCancellationRecord] 做转换，而不是手工拼字符串。
 *
 * `reason` 是契约定义的可空字段；此前本 DTO 没有该字段，导致这一面上的取消原因无处承载。
 *
 * `origin` shares **one canonical code vocabulary** with the checkpoint envelope's cancellation chain
 * (defined by the `[cancellation-origin]` section of the cross-language contract
 * `analysis-fixtures/checkpoint-wire-contract.tsv`), so it must not be treated as free text: codes with
 * no dedicated variant on this side, such as `backend` / `future`, must round-trip verbatim. Convert with
 * [toCancellationRecord] / [fromCancellationRecord] rather than assembling strings by hand.
 *
 * `reason` is a contract-defined nullable field; this DTO previously lacked it, so a cancellation reason
 * had nowhere to live on this face.
 *
 * @property origin 取消来源规范代码 / Canonical cancellation-origin code
 * @property requestedAtEpochMs 取消请求时间（epoch 毫秒）/ Cancellation request time in epoch milliseconds
 * @property reason 取消原因 / Cancellation reason
 */
@Serializable
data class RemoteTaskActionCancellation(
    val origin: String,
    val requestedAtEpochMs: Long,
    val reason: String? = null
) {
    /**
     * 按规范代码解析为核心取消事实。 / Parse into a core cancellation fact using the canonical code.
     *
     * @return 取消事实 / Cancellation fact
     */
    fun toCancellationRecord(): CancellationRecord {
        val source = CancellationSource.fromWireCode(origin)
        return CancellationRecord(
            source = source,
            requestedAt = java.time.Instant.ofEpochMilli(requestedAtEpochMs),
            reason = reason,
            // 只有兜底变体需要保留原始文本，否则 `backend` / `future` 会被写成 `Other` 的代码而失真。
            // Only the catch-all variant needs the raw text; otherwise `backend` / `future` would be
            // written back as `Other`'s code and lose fidelity.
            wireOrigin = origin.takeIf { source == CancellationSource.Other }
        )
    }

    companion object {
        /**
         * 由核心取消事实构造线格式形状。 / Build the wire shape from a core cancellation fact.
         *
         * @param record 取消事实 / Cancellation fact
         * @return 线格式取消形状 / Wire cancellation shape
         */
        fun fromCancellationRecord(record: CancellationRecord): RemoteTaskActionCancellation {
            return RemoteTaskActionCancellation(
                origin = record.wireOrigin ?: record.source.toWireCode(),
                requestedAtEpochMs = record.requestedAt.toEpochMilli(),
                reason = record.reason
            )
        }
    }
}

/**
 * 远程任务操作响应。 / Remote task action response.
 *
 * @property taskId 任务 ID / Task ID
 * @property status 任务状态 / Task status
*/
data class RemoteTaskAction(
    val taskId: TaskId,
    val status: TaskStatus,
    val accepted: Boolean = true,
    val tenantId: TenantId? = null,
    val currentNodeId: NodeId? = null,
    val latestCheckpointRef: ObjectRef? = null,
    val latestResultRef: ObjectRef? = null,
    val consumedCost: Flt64 = Flt64.zero,
    val requestId: RequestId? = null,
    val complexity: TaskComplexity? = null,
    val timeSensitivity: TimeSensitivity? = null,
    val priority: Int? = null,
    val deadline: Instant? = null,
    val budgetScope: BudgetScopeId? = null,
    val budgetLimit: Flt64? = null,
    val dispatchId: DispatchId? = null,
    val sliceId: SliceId? = null,
    val runId: String? = null,
    val attemptId: String? = null,
    val artifactDigest: String? = null,
    val modelFingerprint: String? = null,
    val modelFingerprintSchema: String? = null,
    val configurationFingerprint: String? = null,
    val configurationFingerprintSchema: String? = null,
    val solverFingerprint: String? = null,
    val solverFingerprintSchema: String? = null,
    val fingerprints: Map<String, String> = emptyMap(),
    val fingerprintSchemas: Map<String, String> = emptyMap(),
    val provenance: RemoteTaskActionProvenance? = null,
    val cancellationChain: List<RemoteTaskActionCancellation> = emptyList(),
    val scheduling: SchedulingDecision? = null,
    val outcome: SliceOutcome? = null,
    val problemStatus: RemoteProblemStatus? = null,
    val terminationReason: RemoteTerminationReason? = null,
    val solutionPresence: RemoteSolutionPresence? = null,
    val proofStatus: RemoteProofStatus? = null,
    val statistics: Map<String, String> = emptyMap(),
    val diagnostics: Map<String, String> = emptyMap(),
    val slice: SliceResult? = null,
    val message: String? = null
)

/**
 * 远程任务停止请求。 / Remote task stop request.
 *
 * @property reason 停止原因 / Stop reason
 * @property operator 操作者 / Operator
 * @property source 来源 / Source
*/
@Serializable
data class RemoteTaskStopRequest(
    val reason: ReasonCode? = null,
    val operator: OperatorId? = null,
    val source: OperationSource? = null
)

/**
 * 远程任务恢复请求。 / Remote task resume request.
 *
 * @property operator 操作者 / Operator
 * @property source 来源 / Source
 * @property reason 恢复原因 / Resume reason
*/
@Serializable
data class RemoteTaskResumeRequest(
    val operator: OperatorId? = null,
    val source: OperationSource? = null,
    val reason: ReasonCode? = null
)

/**
 * API response envelope.
 * API 响应信封。
 *
 * @param T 数据类型 / the data type
 * @property code the API response code / API 响应码
 * @property message 响应消息 / the response message
 * @property traceId 追踪 ID，可为 null / the trace ID, nullable
 * @property data 响应数据，可为 null / the response data, nullable
*/
@Serializable
private data class ApiEnvelope<T>(
    val code: String,
    val message: String,
    val traceId: String? = null,
    val data: T? = null
)

/**
 * HTTP response for task submission.
 * 任务提交的 HTTP 响应。
 *
 * @property taskId 任务 ID / the task ID
 * @property accepted 是否接受 / whether the task was accepted
 * @property status 任务状态字符串 / the task status string
 * @property message 响应消息 / the response message
*/
@Serializable
private data class SubmitTaskHttpResponse(
    val taskId: String,
    val accepted: Boolean,
    val status: String,
    val message: String
) {

    /**
     * Converts to domain model.
     * 转换为领域模型。
     *
     * @return 领域提交响应 / the domain submit response
    */
    fun toDomain(): RemoteTaskSubmitResponse {
        return RemoteTaskSubmitResponse(
            taskId = TaskId.of(taskId),
            accepted = accepted,
            status = status.toTaskStatusOrUnknown(),
            message = message
        )
    }
}

/**
 * HTTP response for task view.
 * 任务视图的 HTTP 响应。
 *
 * @property taskId 任务 ID / the task ID
 * @property tenantId 租户 ID / the tenant ID
 * @property status 任务状态字符串 / the task status string
 * @property currentNodeId 当前节点 ID，可为 null / the current node ID, nullable
 * @property latestCheckpointPath 最新检查点路径，可为 null / the latest checkpoint path, nullable
 * @property latestResultPath 最新结果路径，可为 null / the latest result path, nullable
 * @property consumedCost 已消耗成本 / the consumed cost
*/
@Serializable
private data class TaskViewHttpResponse(
    val taskId: String,
    val tenantId: String,
    val status: String,
    val currentNodeId: String? = null,
    val latestCheckpointPath: JsonElement? = null,
    val latestResultPath: JsonElement? = null,
    val latestCheckpointRef: JsonElement? = null,
    val latestResultRef: JsonElement? = null,
    val consumedCost: JsonElement? = null,
    val requestId: String? = null,
    val complexity: String? = null,
    val timeSensitivity: String? = null,
    val priority: Int? = null,
    val budgetScope: String? = null,
    val budgetLimit: JsonElement? = null,
    val sliceId: String? = null,
    @SerialName("currentSliceId")
    val currentSliceId: String? = null,
    val objectiveValue: JsonElement? = null,
    val objectiveValueInt64: JsonElement? = null,
    val bestBound: JsonElement? = null,
    val bound: JsonElement? = null,
    val gap: JsonElement? = null,
    val progress: JsonElement? = null,
    val deadlineRisk: JsonElement? = null,
    @SerialName("deadlineEpochMs")
    val deadlineEpochMs: JsonElement? = null,
    val deadline: JsonElement? = null,
    val dispatchId: String? = null,
    val runId: String? = null,
    val attemptId: String? = null,
    val artifactDigest: String? = null,
    val incumbentRef: JsonElement? = null,
    val modelFingerprint: JsonElement? = null,
    val modelFingerprintSchema: String? = null,
    val configurationFingerprint: JsonElement? = null,
    val configurationFingerprintSchema: String? = null,
    val solverFingerprint: JsonElement? = null,
    val solverFingerprintSchema: String? = null,
    val fingerprints: JsonElement? = null,
    val provenance: JsonElement? = null,
    val scheduling: JsonElement? = null,
    val outcome: String? = null,
    val problemStatus: String? = null,
    val terminationReason: String? = null,
    val solutionPresence: String? = null,
    val proofStatus: String? = null,
    val statistics: Map<String, String> = emptyMap(),
    val diagnostics: Map<String, String> = emptyMap(),
    val fingerprintSchemas: Map<String, String> = emptyMap(),
    val slice: JsonElement? = null
) {

    /**
     * Converts to domain model.
     * 转换为领域模型。
     *
     * @return 领域任务视图 / the domain task view
    */
    fun toDomain(): RemoteTaskView {
        val effectiveSliceId = (sliceId ?: currentSliceId)?.let { SliceId.of(it) }
        val parsedModelFingerprint = modelFingerprint.toFingerprintValueOrNull()
        val parsedConfigurationFingerprint = configurationFingerprint.toFingerprintValueOrNull()
        val parsedSolverFingerprint = solverFingerprint.toFingerprintValueOrNull()
        val parsedFingerprints = buildMap {
            putAll(fingerprints.toFingerprintMap())
            parsedModelFingerprint?.let { put("model", it) }
            parsedConfigurationFingerprint?.let { put("configuration", it) }
            parsedSolverFingerprint?.let { put("solver", it) }
        }
        val parsedFingerprintSchemas = buildMap {
            putAll(fingerprintSchemas)
            modelFingerprint.toFingerprintSchemaOrNull()?.let { put("model", it) }
            modelFingerprintSchema?.let { put("model", it) }
            configurationFingerprint.toFingerprintSchemaOrNull()?.let { put("configuration", it) }
            configurationFingerprintSchema?.let { put("configuration", it) }
            solverFingerprint.toFingerprintSchemaOrNull()?.let { put("solver", it) }
            solverFingerprintSchema?.let { put("solver", it) }
        }
        return RemoteTaskView(
            taskId = TaskId.of(taskId),
            tenantId = TenantId.of(tenantId),
            status = status.toTaskStatusOrUnknown(),
            currentNodeId = currentNodeId?.let { NodeId.of(it) },
            latestCheckpointRef = latestCheckpointRef.toObjectRefOrNull()
                ?: latestCheckpointPath.toObjectRefOrNull(),
            latestResultRef = latestResultRef.toObjectRefOrNull()
                ?: latestResultPath.toObjectRefOrNull(),
            consumedCost = consumedCost.toFlt64OrNull() ?: Flt64.zero,
            requestId = requestId?.takeIf { it.isNotBlank() }?.let(RequestId::of),
            complexity = complexity.toTaskComplexityOrNull(),
            timeSensitivity = timeSensitivity.toTimeSensitivityOrNull(),
            priority = priority,
            budgetScope = budgetScope?.takeIf { it.isNotBlank() }?.let(BudgetScopeId::of),
            budgetLimit = budgetLimit.toFlt64OrNull(),
            sliceId = effectiveSliceId,
            objectiveValue = objectiveValue.toFlt64OrNull(),
            objectiveValueInt64 = objectiveValueInt64.toLongOrNull(),
            bestBound = bestBound.toFlt64OrNull(),
            bound = bound.toFlt64OrNull(),
            gap = gap.toFlt64OrNull(),
            progress = progress.toFlt64OrNull(),
            deadlineRisk = deadlineRisk.toFlt64OrNull(),
            deadline = deadlineEpochMs.toInstantOrNull() ?: deadline.toInstantOrNull(),
            dispatchId = dispatchId?.let(DispatchId::of),
            runId = runId,
            attemptId = attemptId,
            artifactDigest = artifactDigest,
            incumbentRef = incumbentRef.toObjectRefOrNull(),
            modelFingerprint = parsedModelFingerprint,
            modelFingerprintSchema = modelFingerprint.toFingerprintSchemaOrNull()
                ?: modelFingerprintSchema,
            configurationFingerprint = parsedConfigurationFingerprint,
            configurationFingerprintSchema = configurationFingerprint.toFingerprintSchemaOrNull()
                ?: configurationFingerprintSchema,
            solverFingerprint = parsedSolverFingerprint,
            solverFingerprintSchema = solverFingerprint.toFingerprintSchemaOrNull()
                ?: solverFingerprintSchema,
            fingerprints = parsedFingerprints,
            provenance = provenance.toFlatStringMap(),
            scheduling = scheduling.toSchedulingDecisionOrNull(),
            outcome = outcome.toSliceOutcomeOrNull(),
            problemStatus = problemStatus.toRemoteProblemStatusOrNull(),
            terminationReason = terminationReason.toRemoteTerminationReasonOrNull(),
            solutionPresence = solutionPresence.toRemoteSolutionPresenceOrNull(),
            proofStatus = proofStatus.toRemoteProofStatusOrNull(),
            statistics = statistics,
            diagnostics = diagnostics,
            fingerprintSchemas = parsedFingerprintSchemas,
            slice = slice.toSliceResultOrNull()
        )
    }

}

/**
 * HTTP response for task action.
 * 任务操作的 HTTP 响应。
 *
 * @property taskId 任务 ID / the task ID
 * @property status 任务状态字符串 / the task status string
*/
@Serializable
private data class TaskActionHttpResponse(
    val taskId: String,
    val tenantId: String? = null,
    val status: String,
    val accepted: Boolean = true,
    val currentNodeId: String? = null,
    val latestCheckpointPath: JsonElement? = null,
    val latestResultPath: JsonElement? = null,
    val latestCheckpointRef: JsonElement? = null,
    val latestResultRef: JsonElement? = null,
    val consumedCost: JsonElement? = null,
    val requestId: String? = null,
    val complexity: String? = null,
    val timeSensitivity: String? = null,
    val priority: Int? = null,
    @SerialName("deadlineEpochMs")
    val deadlineEpochMs: JsonElement? = null,
    val deadline: JsonElement? = null,
    val budgetScope: String? = null,
    val budgetLimit: JsonElement? = null,
    val dispatchId: String? = null,
    val sliceId: String? = null,
    val runId: String? = null,
    val attemptId: String? = null,
    val artifactDigest: String? = null,
    val modelFingerprint: JsonElement? = null,
    val modelFingerprintSchema: String? = null,
    val configurationFingerprint: JsonElement? = null,
    val configurationFingerprintSchema: String? = null,
    val solverFingerprint: JsonElement? = null,
    val solverFingerprintSchema: String? = null,
    val fingerprints: JsonElement? = null,
    val fingerprintSchemas: Map<String, String> = emptyMap(),
    val provenance: RemoteTaskActionProvenance? = null,
    val cancellationChain: List<RemoteTaskActionCancellation> = emptyList(),
    val scheduling: JsonElement? = null,
    val outcome: String? = null,
    val problemStatus: String? = null,
    val terminationReason: String? = null,
    val solutionPresence: String? = null,
    val proofStatus: String? = null,
    val statistics: Map<String, String> = emptyMap(),
    val diagnostics: Map<String, String> = emptyMap(),
    val slice: JsonElement? = null,
    val message: String? = null
) {

    /**
     * Converts to domain model.
     * 转换为领域模型。
     *
     * @return 领域任务操作 / the domain task action
    */
    fun toDomain(): RemoteTaskAction {
        val parsedModelFingerprint = modelFingerprint.toFingerprintValueOrNull()
        val parsedConfigurationFingerprint = configurationFingerprint.toFingerprintValueOrNull()
        val parsedSolverFingerprint = solverFingerprint.toFingerprintValueOrNull()
        val parsedFingerprints = buildMap {
            putAll(fingerprints.toFingerprintMap())
            parsedModelFingerprint?.let { put("model", it) }
            parsedConfigurationFingerprint?.let { put("configuration", it) }
            parsedSolverFingerprint?.let { put("solver", it) }
        }
        val parsedFingerprintSchemas = buildMap {
            putAll(fingerprintSchemas)
            modelFingerprint.toFingerprintSchemaOrNull()?.let { put("model", it) }
            modelFingerprintSchema?.let { put("model", it) }
            configurationFingerprint.toFingerprintSchemaOrNull()?.let { put("configuration", it) }
            configurationFingerprintSchema?.let { put("configuration", it) }
            solverFingerprint.toFingerprintSchemaOrNull()?.let { put("solver", it) }
            solverFingerprintSchema?.let { put("solver", it) }
        }
        return RemoteTaskAction(
            taskId = TaskId.of(taskId),
            status = status.toTaskStatusOrUnknown(),
            accepted = accepted,
            tenantId = tenantId?.takeIf { it.isNotBlank() }?.let(TenantId::of),
            currentNodeId = currentNodeId?.let(NodeId::of),
            latestCheckpointRef = latestCheckpointRef.toObjectRefOrNull()
                ?: latestCheckpointPath.toObjectRefOrNull(),
            latestResultRef = latestResultRef.toObjectRefOrNull()
                ?: latestResultPath.toObjectRefOrNull(),
            consumedCost = consumedCost.toFlt64OrNull() ?: Flt64.zero,
            requestId = requestId?.takeIf { it.isNotBlank() }?.let(RequestId::of),
            complexity = complexity.toTaskComplexityOrNull(),
            timeSensitivity = timeSensitivity.toTimeSensitivityOrNull(),
            priority = priority,
            deadline = deadlineEpochMs.toInstantOrNull() ?: deadline.toInstantOrNull(),
            budgetScope = budgetScope?.takeIf { it.isNotBlank() }?.let(BudgetScopeId::of),
            budgetLimit = budgetLimit.toFlt64OrNull(),
            dispatchId = dispatchId?.let(DispatchId::of),
            sliceId = sliceId?.let(SliceId::of),
            runId = runId,
            attemptId = attemptId,
            artifactDigest = artifactDigest,
            modelFingerprint = parsedModelFingerprint,
            modelFingerprintSchema = modelFingerprint.toFingerprintSchemaOrNull()
                ?: modelFingerprintSchema,
            configurationFingerprint = parsedConfigurationFingerprint,
            configurationFingerprintSchema = configurationFingerprint.toFingerprintSchemaOrNull()
                ?: configurationFingerprintSchema,
            solverFingerprint = parsedSolverFingerprint,
            solverFingerprintSchema = solverFingerprint.toFingerprintSchemaOrNull()
                ?: solverFingerprintSchema,
            fingerprints = parsedFingerprints,
            fingerprintSchemas = parsedFingerprintSchemas,
            provenance = provenance,
            cancellationChain = cancellationChain,
            scheduling = scheduling.toSchedulingDecisionOrNull(),
            outcome = outcome.toSliceOutcomeOrNull(),
            problemStatus = problemStatus.toRemoteProblemStatusOrNull(),
            terminationReason = terminationReason.toRemoteTerminationReasonOrNull(),
            solutionPresence = solutionPresence.toRemoteSolutionPresenceOrNull(),
            proofStatus = proofStatus.toRemoteProofStatusOrNull(),
            statistics = statistics,
            diagnostics = diagnostics,
            slice = slice.toSliceResultOrNull(),
            message = message
        )
    }
}

private val remoteSolverHttpDecodeJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private fun RemoteTaskView.identityFingerprints(): Map<String, String> {
    return buildMap {
        putAll(fingerprints)
        modelFingerprint?.let { put("model", it) }
        configurationFingerprint?.let { put("configuration", it) }
        solverFingerprint?.let { put("solver", it) }
        scheduling?.modelFingerprint?.let { put("model", it) }
    }
}

private fun RemoteTaskView.identityFingerprintSchemas(): Map<String, String> {
    return buildMap {
        putAll(fingerprintSchemas)
        modelFingerprintSchema?.let { put("model", it) }
        configurationFingerprintSchema?.let { put("configuration", it) }
        solverFingerprintSchema?.let { put("solver", it) }
        scheduling?.modelFingerprintSchema?.let { put("model", it) }
    }
}

/** Parse task states without making an older client fail on a newer state. */
private fun String.toTaskStatusOrUnknown(): TaskStatus {
    return runCatching { TaskStatus.valueOf(trim().uppercase()) }
        .getOrDefault(TaskStatus.UNKNOWN)
}

private fun String?.toTaskComplexityOrNull(): TaskComplexity? {
    return this?.let { runCatching { TaskComplexity.valueOf(it.uppercase()) }.getOrNull() }
}

private fun String?.toTimeSensitivityOrNull(): TimeSensitivity? {
    return this?.let { runCatching { TimeSensitivity.valueOf(it.uppercase()) }.getOrNull() }
}

private const val STOP_CONFIRMATION_ATTEMPTS = 3

private fun TaskStatus.isTerminal(): Boolean {
    return this == TaskStatus.COMPLETED || this == TaskStatus.FAILED || this == TaskStatus.STOPPED
}

private fun JsonElement?.toTextOrNull(): String? {
    return (this as? JsonPrimitive)?.contentOrNull
}

private fun JsonElement?.toFlt64OrNull(): Flt64? {
    return toTextOrNull()?.toDoubleOrNull()?.takeIf { it.isFinite() }?.let(::Flt64)
}

private fun JsonElement?.toLongOrNull(): Long? {
    return toTextOrNull()?.toLongOrNull()
}

private fun JsonElement?.toInstantOrNull(): Instant? {
    val primitive = this as? JsonPrimitive ?: return null
    val text = primitive.contentOrNull ?: return null
    return text.toLongOrNull()?.let(Instant::fromEpochMilliseconds)
        ?: runCatching { Instant.parse(text) }.getOrNull()
}

private fun JsonElement?.toObjectRefOrNull(): ObjectRef? {
    val primitive = this as? JsonPrimitive
    if (primitive != null) {
        return primitive.contentOrNull?.takeIf { it.isNotBlank() }?.let(ObjectRef::of)
    }
    val objectValue = this as? JsonObject ?: return null
    val path = objectValue["path"]?.toTextOrNull()
        ?: objectValue["objectPath"]?.toTextOrNull()
        ?: return null
    return ObjectRef.of(
        path = path,
        version = objectValue["version"]?.toTextOrNull(),
        etag = objectValue["etag"]?.toTextOrNull()
    )
}

private fun JsonElement?.toFingerprintValueOrNull(): String? {
    toTextOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
    val objectValue = this as? JsonObject ?: return null
    return objectValue["value"]?.toTextOrNull()?.takeIf { it.isNotBlank() }
}

private fun JsonElement?.toFingerprintSchemaOrNull(): String? {
    val objectValue = this as? JsonObject ?: return null
    return objectValue["schemaVersion"]?.toTextOrNull()
        ?: objectValue["schema"]?.toTextOrNull()
}

private fun JsonElement?.toFingerprintMap(): Map<String, String> {
    val objectValue = this as? JsonObject ?: return emptyMap()
    return objectValue.mapNotNull { (key, value) ->
        value.toFingerprintValueOrNull()?.let { key to it }
    }.toMap()
}

private fun JsonElement?.toFlatStringMap(): Map<String, String> {
    val objectValue = this as? JsonObject ?: return emptyMap()
    return objectValue.mapNotNull { (key, value) ->
        value.toTextOrNull()?.let { key to it }
    }.toMap()
}

private fun String?.toSliceOutcomeOrNull(): SliceOutcome? {
    return this?.let { runCatching { SliceOutcome.valueOf(it.uppercase()) }.getOrDefault(SliceOutcome.UNKNOWN) }
}

private fun String?.toRemoteTerminationReasonOrNull(): RemoteTerminationReason? {
    return this?.let {
        runCatching { RemoteTerminationReason.valueOf(it.uppercase()) }
            .getOrDefault(RemoteTerminationReason.UNKNOWN)
    }
}

private fun String?.toRemoteProblemStatusOrNull(): RemoteProblemStatus? {
    return this?.let {
        runCatching { RemoteProblemStatus.valueOf(it.uppercase()) }
            .getOrDefault(RemoteProblemStatus.UNKNOWN)
    }
}

private fun String?.toRemoteSolutionPresenceOrNull(): RemoteSolutionPresence? {
    return this?.let {
        runCatching { RemoteSolutionPresence.valueOf(it.uppercase()) }
            .getOrDefault(RemoteSolutionPresence.UNKNOWN)
    }
}

private fun String?.toRemoteProofStatusOrNull(): RemoteProofStatus? {
    return this?.let {
        runCatching { RemoteProofStatus.valueOf(it.uppercase()) }
            .getOrDefault(RemoteProofStatus.UNKNOWN)
    }
}

private fun JsonElement?.toSliceResultOrNull(): SliceResult? {
    return this?.let {
        runCatching {
            remoteSolverHttpDecodeJson.decodeFromJsonElement(
                SliceResult.serializer(),
                it.toKnownSliceEnums()
            )
        }.getOrNull()
    }
}

private fun JsonElement?.toSchedulingDecisionOrNull(): SchedulingDecision? {
    return this?.let {
        runCatching {
            remoteSolverHttpDecodeJson.decodeFromJsonElement(
                SchedulingDecision.serializer(),
                it.toKnownSchedulingEnums()
            )
        }.getOrNull()
    }
}

private fun JsonElement?.toKnownSchedulingEnums(): JsonElement {
    val objectValue = this as? JsonObject ?: return this ?: JsonObject(emptyMap())
    return JsonObject(objectValue.filterKnownEnum("preemptionMode", setOf(
        PreemptionMode.NON_PREEMPTIBLE.name,
        PreemptionMode.CONTROLLED_RETURN.name,
        PreemptionMode.NATIVE.name,
        PreemptionMode.UNKNOWN.name
    )).filterKnownEnum("resumeMode", setOf(
        ResumeMode.NONE.name,
        ResumeMode.WARM_START.name,
        ResumeMode.BASIS.name,
        ResumeMode.NATIVE_CHECKPOINT.name,
        ResumeMode.UNKNOWN.name
    )).filterKnownEnum("outcome", setOf(
        SliceOutcome.COMPLETED.name,
        SliceOutcome.PREEMPTED.name,
        SliceOutcome.CHECKPOINTED.name,
        SliceOutcome.RESUMABLE.name,
        SliceOutcome.CANCELLED.name,
        SliceOutcome.FAILED.name,
        SliceOutcome.UNKNOWN.name
    )))
}

private fun JsonElement?.toKnownSliceEnums(): JsonElement {
    val objectValue = this as? JsonObject ?: return this ?: JsonObject(emptyMap())
    val normalized = objectValue
        .filterKnownEnum("problemStatus", setOf(
            RemoteProblemStatus.FEASIBLE.name,
            RemoteProblemStatus.INFEASIBLE.name,
            RemoteProblemStatus.UNBOUNDED.name,
            RemoteProblemStatus.INFEASIBLE_OR_UNBOUNDED.name,
            RemoteProblemStatus.UNKNOWN.name
        ))
        .filterKnownEnum("terminationReason", setOf(
            RemoteTerminationReason.COMPLETED.name,
            RemoteTerminationReason.TIME_LIMIT.name,
            RemoteTerminationReason.NODE_LIMIT.name,
            RemoteTerminationReason.ITERATION_LIMIT.name,
            RemoteTerminationReason.SOLUTION_LIMIT.name,
            RemoteTerminationReason.OBJECTIVE_LIMIT.name,
            RemoteTerminationReason.CANCELLED.name,
            RemoteTerminationReason.INTERRUPTED.name,
            RemoteTerminationReason.NUMERICAL_FAILURE.name,
            RemoteTerminationReason.BACKEND_FAILURE.name,
            RemoteTerminationReason.UNKNOWN.name
        ))
        .filterKnownEnum("solutionPresence", setOf(
            RemoteSolutionPresence.NONE.name,
            RemoteSolutionPresence.INCUMBENT.name,
            RemoteSolutionPresence.OPTIMAL.name,
            RemoteSolutionPresence.UNKNOWN.name
        ))
        .filterKnownEnum("proofStatus", setOf(
            RemoteProofStatus.NONE.name,
            RemoteProofStatus.CLAIMED.name,
            RemoteProofStatus.VERIFIED.name,
            RemoteProofStatus.UNKNOWN.name
        ))
        .filterKnownEnum("outcome", setOf(
            SliceOutcome.COMPLETED.name,
            SliceOutcome.PREEMPTED.name,
            SliceOutcome.CHECKPOINTED.name,
            SliceOutcome.RESUMABLE.name,
            SliceOutcome.CANCELLED.name,
            SliceOutcome.FAILED.name,
            SliceOutcome.UNKNOWN.name
        ))
    return JsonObject(normalized.mapValues { (key, value) ->
        if (key == "scheduling") value.toKnownSchedulingEnums() else value
    })
}

private fun JsonObject.filterKnownEnum(key: String, known: Set<String>): JsonObject {
    val value = this[key] as? JsonPrimitive ?: return this
    val text = value.contentOrNull ?: return this
    return if (text.uppercase() in known) {
        this
    } else {
        JsonObject(toMutableMap().apply { this[key] = JsonPrimitive("UNKNOWN") })
    }
}
