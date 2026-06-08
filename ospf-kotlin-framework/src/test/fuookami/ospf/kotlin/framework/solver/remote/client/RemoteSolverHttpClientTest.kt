@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.solver.remote.client

import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class RemoteSolverHttpClientTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun submitPostsTaskAndMapsResponse() {
        val http = RecordingHttpHandler(
            RemoteSolverHttpResponse(
                statusCode = 200,
                body =
                """
                {
                  "code": "OK",
                  "message": "success",
                  "traceId": "trace-1",
                  "data": {
                    "taskId": "task-1",
                    "accepted": true,
                    "status": "ACCEPTED",
                    "message": "accepted"
                  }
                }
                """.trimIndent()
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://127.0.0.1:18080/",
            transport = http,
            tenantId = TenantId.of("tenant-a"),
            traceIdProvider = { TraceId.of("trace-1") }
        )

        val response = client.submit(
            RemoteTaskSubmitRequest(
                payloadRef = ObjectPath.of("payloads/1"),
                requestId = RequestId.of("request-1"),
                complexity = TaskComplexity.COMPLEX,
                timeSensitivity = TimeSensitivity.NON_REALTIME,
                priority = 3,
                budgetScope = BudgetScopeId.of("scope-a"),
                budgetLimit = Flt64(12.5),
                deadline = Instant.fromEpochMilliseconds(123L)
            )
        )

        assertEquals(TaskId.of("task-1"), response.taskId)
        assertEquals(true, response.accepted)
        assertEquals(TaskStatus.ACCEPTED, response.status)
        assertEquals("POST", http.lastRequest?.method)
        assertEquals("http://127.0.0.1:18080/api/v1/tasks", http.lastRequest?.url)
        assertEquals("tenant-a", http.lastRequest?.headers?.get("X-Tenant-Id"))
        assertEquals("trace-1", http.lastRequest?.headers?.get("X-Trace-Id"))
        val body = json.parseToJsonElement(http.lastRequest?.body ?: "").jsonObject
        assertEquals("payloads/1", body.getValue("payloadRef").jsonPrimitive.content)
        assertEquals("COMPLEX", body.getValue("complexity").jsonPrimitive.content)
        assertEquals("NON_REALTIME", body.getValue("timeSensitivity").jsonPrimitive.content)
    }

    @Test
    fun getMapsTaskView() {
        val http = RecordingHttpHandler(
            RemoteSolverHttpResponse(
                statusCode = 200,
                body =
                """
                {
                  "code": "OK",
                  "message": "success",
                  "data": {
                    "taskId": "task-1",
                    "tenantId": "tenant-a",
                    "status": "RUNNING",
                    "currentNodeId": "node-1",
                    "latestCheckpointPath": "checkpoints/latest",
                    "latestResultPath": "results/latest",
                    "consumedCost": 7.5
                  }
                }
                """.trimIndent()
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http
        )

        val response = client.get(TaskId.of("task-1"))

        assertEquals(TaskId.of("task-1"), response?.taskId)
        assertEquals(TaskStatus.RUNNING, response?.status)
        assertEquals(NodeId.of("node-1"), response?.currentNodeId)
        assertEquals(ObjectRef.of(path = "checkpoints/latest"), response?.latestCheckpointRef)
        assertEquals(ObjectRef.of(path = "results/latest"), response?.latestResultRef)
        assertEquals(Flt64(7.5), response?.consumedCost)
        assertEquals("GET", http.lastRequest?.method)
        assertEquals("http://localhost/api/v1/tasks/task-1", http.lastRequest?.url)
    }

    @Test
    fun getReturnsNullWhenNotFound() {
        val http = RecordingHttpHandler(RemoteSolverHttpResponse(statusCode = 404, body = "{}"))
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http
        )

        assertNull(client.get(TaskId.of("missing-task")))
    }

    @Test
    fun stopAndResumePostActionRequests() {
        val http = QueueHttpHandler(
            mutableListOf(
                RemoteSolverHttpResponse(statusCode = 200, body = actionEnvelope("STOPPING")),
                RemoteSolverHttpResponse(statusCode = 200, body = actionEnvelope("QUEUED"))
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http
        )

        val stopped = client.stop(
            taskId = TaskId.of("task-1"),
            request = RemoteTaskStopRequest(
                reason = ReasonCode.of("manual-stop"),
                operator = OperatorId.of("tester"),
                source = OperationSource.of("unit-test")
            )
        )
        val resumed = client.resume(
            taskId = TaskId.of("task-1"),
            request = RemoteTaskResumeRequest(
                operator = OperatorId.of("tester"),
                source = OperationSource.of("unit-test"),
                reason = ReasonCode.of("manual-resume")
            )
        )

        assertEquals(TaskStatus.STOPPING, stopped.status)
        assertEquals(TaskStatus.QUEUED, resumed.status)
        assertEquals("http://localhost/api/v1/tasks/task-1/stop", http.requests[0].url)
        assertEquals("http://localhost/api/v1/tasks/task-1/resume", http.requests[1].url)
        assertEquals("manual-stop", json.parseToJsonElement(http.requests[0].body ?: "").jsonObject.getValue("reason").jsonPrimitive.content)
        assertEquals("manual-resume", json.parseToJsonElement(http.requests[1].body ?: "").jsonObject.getValue("reason").jsonPrimitive.content)
    }

    @Test
    fun errorEnvelopeThrowsRemoteSolverException() {
        val http = RecordingHttpHandler(
            RemoteSolverHttpResponse(
                statusCode = 400,
                body =
                """
                {
                  "code": "INVALID_ARGUMENT",
                  "message": "payloadRef is required",
                  "traceId": "trace-error",
                  "data": null
                }
                """.trimIndent()
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http
        )

        val error = assertThrows(RemoteSolverException::class.java) {
            client.submit(RemoteTaskSubmitRequest(payloadRef = ObjectPath.of("payloads/1")))
        }

        assertEquals(RemoteSolverErrorCode.INVALID_ARGUMENT, error.code)
        assertEquals("payloadRef is required", error.message)
        assertEquals("trace-error", error.metadata["traceId"])
        assertEquals("400", error.metadata["status"])
    }

    @Test
    fun clientCanUseCustomTransportPlugin() {
        val plugin = object : RemoteSolverHttpTransportPlugin {
            override val name: String = "unit-custom"

            override fun create(config: RemoteSolverHttpTransportConfig): RemoteSolverHttpTransport {
                return RecordingHttpHandler(
                    RemoteSolverHttpResponse(
                        statusCode = 200,
                        body =
                        """
                        {
                          "code": "OK",
                          "message": "success",
                          "data": {
                            "taskId": "task-custom",
                            "accepted": true,
                            "status": "ACCEPTED",
                            "message": "${config.properties.getValue("message")}"
                          }
                        }
                        """.trimIndent()
                    )
                )
            }
        }

        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transportPlugin = plugin,
            transportConfig = RemoteSolverHttpTransportConfig(
                properties = mapOf("message" to "from-plugin")
            )
        )

        val response = client.submit(RemoteTaskSubmitRequest(payloadRef = ObjectPath.of("payloads/1")))

        assertEquals(TaskId.of("task-custom"), response.taskId)
        assertEquals("from-plugin", response.message)
    }

    @Test
    fun transportPluginRegistryResolvesRegisteredPlugin() {
        val plugin = object : RemoteSolverHttpTransportPlugin {
            override val name: String = "unit-registry"

            override fun create(config: RemoteSolverHttpTransportConfig): RemoteSolverHttpTransport {
                return RemoteSolverHttpTransport {
                    RemoteSolverHttpResponse(
                        statusCode = 200,
                        body = actionEnvelope("RUNNING")
                    )
                }
            }
        }

        RemoteSolverHttpTransportPlugins.register(plugin)

        assertEquals(plugin, RemoteSolverHttpTransportPlugins.resolve("unit-registry"))
        assertEquals(true, RemoteSolverHttpTransportPlugins.names().contains("jdk"))
        assertEquals(true, RemoteSolverHttpTransportPlugins.names().contains("unit-registry"))
    }

    private fun actionEnvelope(status: String): String {
        return """
            {
              "code": "OK",
              "message": "success",
              "data": {
                "taskId": "task-1",
                "status": "$status"
              }
            }
        """.trimIndent()
    }

    private class RecordingHttpHandler(
        private val response: RemoteSolverHttpResponse
    ) : RemoteSolverHttpTransport {
        var lastRequest: RemoteSolverHttpRequest? = null

        override fun send(request: RemoteSolverHttpRequest): RemoteSolverHttpResponse {
            lastRequest = request
            return response
        }
    }

    private class QueueHttpHandler(
        private val responses: MutableList<RemoteSolverHttpResponse>
    ) : RemoteSolverHttpTransport {
        val requests = mutableListOf<RemoteSolverHttpRequest>()

        override fun send(request: RemoteSolverHttpRequest): RemoteSolverHttpResponse {
            requests.add(request)
            return responses.removeFirst()
        }
    }
}
