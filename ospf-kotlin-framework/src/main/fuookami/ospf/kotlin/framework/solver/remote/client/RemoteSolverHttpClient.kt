@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.solver.remote.client

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 远程求解 HTTP 客户端。
 * Remote solver HTTP client.
 *
 * @property baseUrl dispatcher 基础地址 / Dispatcher base URL
 * @property transport HTTP 传输 / HTTP transport
 * @property json JSON 编解码器 / JSON codec
 * @property tenantId 默认租户 ID / Default tenant ID
 * @property traceIdProvider trace ID 提供器 / Trace ID provider
 */
class RemoteSolverHttpClient(
    private val baseUrl: String,
    private val transport: RemoteSolverHttpTransport = JavaNetRemoteSolverHttpTransport(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
    private val tenantId: TenantId? = null,
    private val traceIdProvider: () -> TraceId? = { null }
) {
    private val normalizedBaseUrl = baseUrl.trim().trimEnd('/')

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
        traceIdProvider: () -> TraceId? = { null }
    ) : this(
        baseUrl = baseUrl,
        transport = transportPlugin.create(transportConfig),
        json = json,
        tenantId = tenantId,
        traceIdProvider = traceIdProvider
    )

    init {
        require(normalizedBaseUrl.isNotBlank()) { "baseUrl must not be blank." }
    }

    /**
     * 提交任务。
     * Submit task.
     *
     * @param request 提交请求 / Submit request
     * @return 提交响应 / Submit response
     */
    fun submit(request: RemoteTaskSubmitRequest): RemoteTaskSubmitResponse {
        val response = transport.send(
            request(
                method = "POST",
                path = "/api/v1/tasks",
                body = json.encodeToString(RemoteTaskSubmitRequest.serializer(), request)
            )
        )
        return decodeEnvelope(
            response = response,
            dataDeserializer = SubmitTaskHttpResponse.serializer()
        ).toDomain()
    }

    /**
     * 查询任务。
     * Get task.
     *
     * @param taskId 任务 ID / Task ID
     * @return 任务视图，不存在时返回 null / Task view, null if not found
     */
    fun get(taskId: TaskId): RemoteTaskView? {
        val response = transport.send(
            request(
                method = "GET",
                path = "/api/v1/tasks/${taskId.value}"
            )
        )
        if (response.statusCode == 404) {
            return null
        }
        return decodeEnvelope(
            response = response,
            dataDeserializer = TaskViewHttpResponse.serializer()
        ).toDomain()
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
    ): RemoteTaskAction {
        val response = transport.send(
            request(
                method = "POST",
                path = "/api/v1/tasks/${taskId.value}/stop",
                body = json.encodeToString(RemoteTaskStopRequest.serializer(), request)
            )
        )
        return decodeEnvelope(
            response = response,
            dataDeserializer = TaskActionHttpResponse.serializer()
        ).toDomain()
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
    ): RemoteTaskAction {
        val response = transport.send(
            request(
                method = "POST",
                path = "/api/v1/tasks/${taskId.value}/resume",
                body = json.encodeToString(RemoteTaskResumeRequest.serializer(), request)
            )
        )
        return decodeEnvelope(
            response = response,
            dataDeserializer = TaskActionHttpResponse.serializer()
        ).toDomain()
    }

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

    private fun <T> decodeEnvelope(
        response: RemoteSolverHttpResponse,
        dataDeserializer: KSerializer<T>
    ): T {
        if (response.statusCode !in 200..299) {
            throw decodeError(response)
        }
        val envelope = json.decodeFromString(
            ApiEnvelope.serializer(dataDeserializer),
            response.body
        )
        if (envelope.code != "OK") {
            throw RemoteSolverException(
                code = envelope.code.toRemoteErrorCode(),
                message = envelope.message,
                metadata = envelope.traceId?.let { mapOf("traceId" to it) } ?: emptyMap()
            )
        }
        return envelope.data ?: throw RemoteSolverException(
            code = RemoteSolverErrorCode.INTERNAL_ERROR,
            message = "Remote solver response data is null.",
            metadata = envelope.traceId?.let { mapOf("traceId" to it) } ?: emptyMap()
        )
    }

    private fun decodeError(response: RemoteSolverHttpResponse): RemoteSolverException {
        val body = response.body
        val envelope = runCatching {
            json.decodeFromString(
                ApiEnvelope.serializer(kotlinx.serialization.json.JsonElement.serializer()),
                body
            )
        }.getOrNull()
        return RemoteSolverException(
            code = envelope?.code?.toRemoteErrorCode() ?: RemoteSolverErrorCode.INTERNAL_ERROR,
            message = envelope?.message ?: "Remote solver HTTP request failed with status ${response.statusCode}.",
            metadata = buildMap {
                put("status", response.statusCode.toString())
                envelope?.traceId?.let { put("traceId", it) }
                if (body.isNotBlank()) {
                    put("body", body)
                }
            }
        )
    }

    private fun String.toRemoteErrorCode(): RemoteSolverErrorCode {
        return runCatching { RemoteSolverErrorCode.valueOf(this) }
            .getOrDefault(RemoteSolverErrorCode.INTERNAL_ERROR)
    }
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

@Serializable
private data class ApiEnvelope<T>(
    val code: String,
    val message: String,
    val traceId: String? = null,
    val data: T? = null
)

@Serializable
private data class SubmitTaskHttpResponse(
    val taskId: String,
    val accepted: Boolean,
    val status: String,
    val message: String
) {
    fun toDomain(): RemoteTaskSubmitResponse {
        return RemoteTaskSubmitResponse(
            taskId = TaskId.of(taskId),
            accepted = accepted,
            status = TaskStatus.valueOf(status),
            message = message
        )
    }
}

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

@Serializable
private data class TaskActionHttpResponse(
    val taskId: String,
    val status: String
) {
    fun toDomain(): RemoteTaskAction {
        return RemoteTaskAction(
            taskId = TaskId.of(taskId),
            status = TaskStatus.valueOf(status)
        )
    }
}
