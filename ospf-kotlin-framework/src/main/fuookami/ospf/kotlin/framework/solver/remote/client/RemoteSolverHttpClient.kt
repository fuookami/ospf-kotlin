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
import kotlinx.serialization.json.Json
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 远程求解 HTTP 客户端。
 * Remote solver HTTP client.
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
        ObjectPath.of("payloads/${tenantId.value}/${taskId.value}/${sliceId.value}.json")
    },
    private val requestIdProvider: (TaskId, SliceId, TenantId) -> RequestId = { taskId, _, _ ->
        RequestId.of(taskId.value)
    },
    private val resumeMode: RemoteSolverHttpResumeMode = RemoteSolverHttpResumeMode.SERVER_TASK_LATEST_CHECKPOINT,
    private val pollInterval: Duration = 200.milliseconds
) : SolverExecutionPort {
    private val normalizedBaseUrl = baseUrl.trim().trimEnd('/')

    /**
     * 将远程求解器错误码转换为框架错误码。
     * Convert remote solver error code to framework error code.
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
     * 构建远程求解器错误消息。
     * Build remote solver error message.
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
     * 构建远程求解器失败结果，包含详细错误信息。
     * Build remote solver failure result with detailed error info.
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
     * 使用 HTTP 传输插件构造客户端。
     * Construct client with HTTP transport plugin.
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
            ObjectPath.of("payloads/${tenantId.value}/${taskId.value}/${sliceId.value}.json")
        },
        requestIdProvider: (TaskId, SliceId, TenantId) -> RequestId = { taskId, _, _ ->
            RequestId.of(taskId.value)
        },
        resumeMode: RemoteSolverHttpResumeMode = RemoteSolverHttpResumeMode.SERVER_TASK_LATEST_CHECKPOINT,
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
                tenantId = tenantId
            )
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
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
        if (resumeMode == RemoteSolverHttpResumeMode.STRICT_CHECKPOINT) {
            return failedRemote(
                code = RemoteSolverErrorCode.INVALID_ARGUMENT,
                message = "HTTP task resume API does not support checkpoint-specific resume.",
                metadata = mapOf(
                    "taskId" to taskId.value,
                    "checkpointPath" to checkpoint.path.value
                ),
                taskId = taskId.value,
                sliceId = sliceId.value
            )
        }
        when (val result = resume(taskId = taskId)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return Ok(ExecutionHandle(
            handleId = HandleId.of(taskId.value),
            taskId = taskId,
            sliceId = sliceId,
            nodeId = nodeId,
            startedAt = Clock.System.now()
        ))
    }

    override suspend fun awaitSliceEnd(handle: ExecutionHandle, quantum: Duration): Ret<SliceResult> {
        if (quantum <= Duration.ZERO) {
            return Failed(ErrorCode.IllegalArgument, "quantum must be positive.")
        }
        var elapsed = Duration.ZERO
        while (elapsed < quantum) {
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
            when (view.status) {
                TaskStatus.COMPLETED -> {
                    val result = when (val ret = fetchFinalResult(handle)) {
                        is Ok -> ret.value
                        is Failed -> return Failed(ret.error)
                        is Fatal -> return Fatal(ret.errors)
                    }
                    return Ok(SliceResult(
                        sliceId = handle.sliceId,
                        completed = true,
                        feasible = result?.feasible ?: true,
                        objectiveValue = result?.objectiveValue,
                        gap = result?.gap,
                        elapsed = result?.elapsed ?: elapsed,
                        message = result?.message
                    ))
                }

                TaskStatus.FAILED, TaskStatus.STOPPED -> {
                    return Ok(SliceResult(
                        sliceId = handle.sliceId,
                        completed = true,
                        feasible = false,
                        objectiveValue = null,
                        gap = null,
                        elapsed = elapsed,
                        message = "Remote task ended with status ${view.status}."
                    ))
                }

                else -> {}
            }
            val wait = minOf(pollInterval, quantum - elapsed)
            delay(wait)
            elapsed += wait
        }
        return Ok(SliceResult(
            sliceId = handle.sliceId,
            completed = false,
            feasible = false,
            objectiveValue = null,
            gap = null,
            elapsed = quantum
        ))
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
        val resultRef = view.latestResultRef ?: return Ok(null)
        val storage = objectStoragePort ?: return Ok(null)
        val bytes = storage.get(resultRef) ?: return Ok(null)
        val solution = try {
            json.decodeFromString(
                SerializedSolution.serializer(),
                bytes.decodeToString()
            )
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
        return Ok(SolveResult(
            feasible = solution.feasible,
            optimal = solution.optimal,
            objectiveValue = solution.objectiveValue,
            gap = solution.gap,
            elapsed = solution.elapsed,
            checkpointRef = view.latestCheckpointRef,
            resultRef = resultRef,
            message = solution.message
        ))
    }

    override suspend fun stop(handle: ExecutionHandle): Ret<Boolean> {
        return stop(taskId = handle.taskId).map { it.status != TaskStatus.FAILED }
    }

    /**
     * 提交任务。
     * Submit task.
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
     * 发送 HTTP 请求到远程求解器。
     * Send HTTP request to remote solver.
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
     * 查询任务。
     * Get task.
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
     * 停止任务。
     * Stop task.
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
     * 恢复任务。
     * Resume task.
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
     * 构建远程求解器 HTTP 请求，自动注入租户和追踪头。
     * Build remote solver HTTP request with auto-injected tenant and trace headers.
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
     * 解析 API 信封响应，提取数据或错误。
     * Decode API envelope response, extract data or error.
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
     * 解析错误响应，提取错误信息并构建失败结果。
     * Decode error response, extract error info and build failure result.
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
     * 将求解载荷上传到对象存储。
     * Upload solve payload to object storage.
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
}

/**
 * HTTP 恢复模式。
 * HTTP resume mode.
*/
enum class RemoteSolverHttpResumeMode {
    /** 恢复服务端任务的最新检查点 / Resume server task from its latest checkpoint */
    SERVER_TASK_LATEST_CHECKPOINT,

    /** 严格要求按指定检查点恢复，当前 HTTP API 不支持 / Require checkpoint-specific resume, unsupported by current HTTP API */
    STRICT_CHECKPOINT
}

/**
 * 远程求解 HTTP 传输接口。
 * Remote solver HTTP transport.
*/
fun interface RemoteSolverHttpTransport {

    /**
     * 发送 HTTP 请求。
     * Send HTTP request.
     *
     * @param request HTTP 请求 / HTTP request
     * @return HTTP 响应 / HTTP response
    */
    fun send(request: RemoteSolverHttpRequest): RemoteSolverHttpResponse
}

/**
 * 远程求解 HTTP 请求。
 * Remote solver HTTP request.
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
 * 远程求解 HTTP 响应。
 * Remote solver HTTP response.
 *
 * @property statusCode HTTP 状态码 / HTTP status code
 * @property body 响应体 / Response body
*/
data class RemoteSolverHttpResponse(
    val statusCode: Int,
    val body: String
)

/**
 * JDK 标准 HTTP 传输实现。
 * JDK standard HTTP transport implementation.
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
 * 远程任务提交请求。
 * Remote task submit request.
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
    val deadline: Instant? = null
)

/**
 * 远程任务提交响应。
 * Remote task submit response.
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
 * 远程任务视图。
 * Remote task view.
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
    val consumedCost: Flt64
)

/**
 * 远程任务操作响应。
 * Remote task action response.
 *
 * @property taskId 任务 ID / Task ID
 * @property status 任务状态 / Task status
*/
data class RemoteTaskAction(
    val taskId: TaskId,
    val status: TaskStatus
)

/**
 * 远程任务停止请求。
 * Remote task stop request.
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
 * 远程任务恢复请求。
 * Remote task resume request.
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
 * @param T the data type / 数据类型
 * @property code the API response code / API 响应码
 * @property message the response message / 响应消息
 * @property traceId the trace ID, nullable / 追踪 ID，可为 null
 * @property data the response data, nullable / 响应数据，可为 null
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
 * @property taskId the task ID / 任务 ID
 * @property accepted whether the task was accepted / 是否接受
 * @property status the task status string / 任务状态字符串
 * @property message the response message / 响应消息
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
     * @return the domain submit response / 领域提交响应
    */
    fun toDomain(): RemoteTaskSubmitResponse {
        return RemoteTaskSubmitResponse(
            taskId = TaskId.of(taskId),
            accepted = accepted,
            status = TaskStatus.valueOf(status),
            message = message
        )
    }
}

/**
 * HTTP response for task view.
 * 任务视图的 HTTP 响应。
 *
 * @property taskId the task ID / 任务 ID
 * @property tenantId the tenant ID / 租户 ID
 * @property status the task status string / 任务状态字符串
 * @property currentNodeId the current node ID, nullable / 当前节点 ID，可为 null
 * @property latestCheckpointPath the latest checkpoint path, nullable / 最新检查点路径，可为 null
 * @property latestResultPath the latest result path, nullable / 最新结果路径，可为 null
 * @property consumedCost the consumed cost / 已消耗成本
*/
@Serializable
private data class TaskViewHttpResponse(
    val taskId: String,
    val tenantId: String,
    val status: String,
    val currentNodeId: String? = null,
    val latestCheckpointPath: String? = null,
    val latestResultPath: String? = null,
    val consumedCost: Double
) {

    /**
     * Converts to domain model.
     * 转换为领域模型。
     *
     * @return the domain task view / 领域任务视图
    */
    fun toDomain(): RemoteTaskView {
        return RemoteTaskView(
            taskId = TaskId.of(taskId),
            tenantId = TenantId.of(tenantId),
            status = TaskStatus.valueOf(status),
            currentNodeId = currentNodeId?.let { NodeId.of(it) },
            latestCheckpointRef = latestCheckpointPath?.let { ObjectRef.of(path = it) },
            latestResultRef = latestResultPath?.let { ObjectRef.of(path = it) },
            consumedCost = Flt64(consumedCost)
        )
    }
}

/**
 * HTTP response for task action.
 * 任务操作的 HTTP 响应。
 *
 * @property taskId the task ID / 任务 ID
 * @property status the task status string / 任务状态字符串
*/
@Serializable
private data class TaskActionHttpResponse(
    val taskId: String,
    val status: String
) {

    /**
     * Converts to domain model.
     * 转换为领域模型。
     *
     * @return the domain task action / 领域任务操作
    */
    fun toDomain(): RemoteTaskAction {
        return RemoteTaskAction(
            taskId = TaskId.of(taskId),
            status = TaskStatus.valueOf(status)
        )
    }
}
