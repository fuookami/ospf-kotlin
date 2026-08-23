@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.solver.remote.client

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.ObjectStoragePort
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.ExErr
import fuookami.ospf.kotlin.utils.functional.Failed

/**
 * 远程求解器 HTTP 客户端测试。
 * Remote solver HTTP client tests.
 */
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
        ).valueOrFail()

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
    fun probeCapabilitiesMapsProtocolAndModelTypes() {
        val http = RecordingHttpHandler(
            RemoteSolverHttpResponse(
                statusCode = 200,
                body =
                """
                {
                  "code": "OK",
                  "message": "success",
                  "data": {
                    "schemaVersion": "1.0",
                    "protocolVersions": ["2.0"],
                    "supportedModelTypes": ["CP", "LINEAR"],
                    "supportsPortableCheckpoint": true,
                    "supportsNativeCheckpoint": false
                  }
                }
                """.trimIndent()
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http
        )

        val capabilities = client.probeCapabilities().valueOrFail()

        assertEquals("1.0", capabilities.schemaVersion)
        assertEquals(setOf("2.0"), capabilities.protocolVersions)
        assertEquals(setOf("CP", "LINEAR"), capabilities.supportedModelTypes)
        assertTrue(capabilities.supportsPortableCheckpoint)
        assertFalse(capabilities.supportsNativeCheckpoint)
        assertEquals("GET", http.lastRequest?.method)
        assertEquals("http://localhost/api/v1/capabilities", http.lastRequest?.url)
    }

    @Test
    fun canonicalCpV2FixtureDecodesWithExactObjectiveAndStatistics() {
        val fixture = checkNotNull(javaClass.getResource("/fixtures/remote-cp-result-v2.json"))
            .readText()
        val solution = json.decodeFromString(SerializedSolution.serializer(), fixture)

        assertEquals("2.0", solution.schemaVersion)
        assertEquals(9_007_199_254_740_993L, solution.objectiveValueInt64)
        assertEquals(9_007_199_254_740_993L, solution.variableValuesById["x"])
        assertEquals(SerializedIntervalValue(1L, 2L, 3L, true), solution.intervalValues["job"])
        assertEquals(RemoteTerminationReason.TIME_LIMIT, solution.terminationReason)
        assertEquals("1.0", solution.fingerprintSchemas["model"])
        assertEquals("0.5", solution.statistics["bestBound"])
    }

    /**
     * 验证线性与二次共享的 v2 报告字段可由客户端读取。
     * Verifies that the shared v2 linear/quadratic report fields are readable by the client.
     */
    @Test
    fun canonicalLinearV2FixtureDecodesReportFields() {
        val fixture = checkNotNull(javaClass.getResource("/fixtures/remote-linear-result-v2.json"))
            .readText()
        val solution = json.decodeFromString(SerializedSolution.serializer(), fixture)

        assertEquals("2.0", solution.schemaVersion)
        assertEquals(12.5, solution.objectiveValue?.toDouble())
        assertEquals(0.2, solution.gap?.toDouble())
        assertEquals(listOf(2.0, 3.0), solution.variableValues.map { it.toDouble() })
        assertEquals(RemoteSolutionPresence.INCUMBENT, solution.solutionPresence)
        assertEquals(RemoteProofStatus.CLAIMED, solution.proofStatus)
        assertEquals(RemoteTerminationReason.TIME_LIMIT, solution.terminationReason)
        assertEquals("2.0", solution.fingerprintSchemas["solver"])
        assertEquals("10.5", solution.statistics["bestBound"])
        assertEquals("run-linear-1", solution.runId)
        assertEquals("attempt-linear-1", solution.attemptId)
    }

    @Test
    fun cpStartRejectsMissingCapabilityBeforeUploadingPayload() = runBlocking {
        val http = RecordingHttpHandler(
            RemoteSolverHttpResponse(
                statusCode = 200,
                body =
                """
                {
                  "code": "OK",
                  "message": "success",
                  "data": {
                    "schemaVersion": "1.0",
                    "protocolVersions": ["2.0"],
                    "supportedModelTypes": ["LINEAR"],
                    "supportsPortableCheckpoint": true,
                    "supportsNativeCheckpoint": false
                  }
                }
                """.trimIndent()
            )
        )
        val storage = RecordingObjectStoragePort()
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http,
            objectStoragePort = storage
        )

        val result = client.start(
            payload = SolvePayload(
                modelData = ModelData.raw(
                    bytes = "{}".encodeToByteArray(),
                    format = "ospf-cp-snapshot-json"
                )
            ),
            taskId = TaskId.of("task-cp"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-a")
        )

        assertTrue(result is Failed)
        val failed = result as Failed<*, *, *>
        assertEquals(ErrorCode.IllegalArgument, failed.code)
        assertTrue(failed.error.message?.contains("does not advertise CP") == true)
        assertTrue(storage.putPaths.isEmpty())
        assertEquals("/api/v1/capabilities", http.lastRequest?.url?.substringAfter("http://localhost"))
    }

    @Test
    fun cpStartRejectsLegacyServerWithoutCapabilityEndpoint() = runBlocking {
        val http = RecordingHttpHandler(RemoteSolverHttpResponse(statusCode = 404, body = "{}"))
        val storage = RecordingObjectStoragePort()
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http,
            objectStoragePort = storage
        )

        val result = client.start(
            payload = SolvePayload(
                modelData = ModelData.raw(
                    bytes = "{}".encodeToByteArray(),
                    format = "ospf-cp-snapshot-json"
                )
            ),
            taskId = TaskId.of("task-cp"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-a")
        )

        assertTrue(result is Failed)
        assertEquals(ErrorCode.ApplicationError, (result as Failed<*, *, *>).code)
        assertTrue(storage.putPaths.isEmpty())
    }

    @Test
    fun cpStartProbesCapabilityBeforeSubmittingTask() = runBlocking {
        val http = QueueHttpHandler(
            mutableListOf(
                RemoteSolverHttpResponse(
                    statusCode = 200,
                    body =
                    """
                    {
                      "code": "OK",
                      "message": "success",
                      "data": {
                        "schemaVersion": "1.0",
                        "protocolVersions": ["2.0"],
                        "supportedModelTypes": ["CP"],
                        "supportsPortableCheckpoint": true,
                        "supportsNativeCheckpoint": false
                      }
                    }
                    """.trimIndent()
                ),
                RemoteSolverHttpResponse(
                    statusCode = 200,
                    body =
                    """
                    {
                      "code": "OK",
                      "message": "success",
                      "data": {
                        "taskId": "task-cp",
                        "accepted": true,
                        "status": "ACCEPTED",
                        "message": "accepted"
                      }
                    }
                    """.trimIndent()
                )
            )
        )
        val storage = RecordingObjectStoragePort()
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http,
            objectStoragePort = storage
        )

        val handle = client.start(
            payload = SolvePayload(
                modelData = ModelData.raw(
                    bytes = "{}".encodeToByteArray(),
                    format = "ospf-cp-snapshot-json"
                )
            ),
            taskId = TaskId.of("task-cp"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-a")
        ).valueOrFail()

        assertEquals(TaskId.of("task-cp"), handle.taskId)
        assertEquals(2, http.requests.size)
        assertEquals("/api/v1/capabilities", http.requests[0].url.substringAfter("http://localhost"))
        assertEquals("/api/v1/tasks", http.requests[1].url.substringAfter("http://localhost"))
        assertEquals(1, storage.putPaths.size)
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

        val response = client.get(TaskId.of("task-1")).valueOrFail()

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

        val response = client.get(TaskId.of("missing-task"))

        assertTrue(response.ok)
        assertNull(response.value)
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
        ).valueOrFail()
        val resumed = client.resume(
            taskId = TaskId.of("task-1"),
            request = RemoteTaskResumeRequest(
                operator = OperatorId.of("tester"),
                source = OperationSource.of("unit-test"),
                reason = ReasonCode.of("manual-resume")
            )
        ).valueOrFail()

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

        val result = client.submit(RemoteTaskSubmitRequest(payloadRef = ObjectPath.of("payloads/1")))

        assertTrue(result is Failed)
        val error = result as Failed<*, *, *>
        assertEquals(ErrorCode.IllegalArgument, error.code)
        assertTrue(error.message?.contains("payloadRef is required") == true)
        assertTrue(error.message?.contains("trace-error") == true)
        assertTrue(error.message?.contains("status=400") == true)
    }

    @Test
    /** 验证错误信封包含详情 / Verify error envelope contains detail */
    fun errorEnvelopeShouldContainDetailInExErrValue() {
        val http = RecordingHttpHandler(
            RemoteSolverHttpResponse(
                statusCode = 500,
                body =
                """
                {
                  "code": "INTERNAL_ERROR",
                  "message": "Solver execution failed",
                  "traceId": "trace-detail",
                  "data": null
                }
                """.trimIndent()
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http
        )

        val result = client.submit(RemoteTaskSubmitRequest(payloadRef = ObjectPath.of("payloads/1")))

        assertTrue(result is Failed)
        val failed = result as Failed<*, *, *>
        assertEquals(ErrorCode.ApplicationError, failed.code)

        val error = failed.error
        assertTrue(error is ExErr<*, *>)
        @Suppress("UNCHECKED_CAST")
        val exErr = error as ExErr<*, RemoteSolverFailureDetail>
        val detail = exErr.value

        assertEquals(RemoteSolverErrorCode.INTERNAL_ERROR, detail.code)
        assertEquals("Solver execution failed", detail.message)
        assertEquals(500, detail.httpStatus)
        assertTrue(detail.metadata.containsKey("traceId"))
        assertEquals("trace-detail", detail.metadata["traceId"])
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

        val response = client.submit(RemoteTaskSubmitRequest(payloadRef = ObjectPath.of("payloads/1"))).valueOrFail()

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

        assertEquals(plugin, RemoteSolverHttpTransportPlugins.resolve("unit-registry").valueOrFail())
        assertEquals(true, RemoteSolverHttpTransportPlugins.names().contains("jdk"))
        assertEquals(true, RemoteSolverHttpTransportPlugins.names().contains("unit-registry"))
    }

    @Test
    fun httpClientCanBridgeSolverExecutionPort() = runBlocking {
        val storage = RecordingObjectStoragePort()
        storage.objects[ObjectPath.of("results/latest")] = json.encodeToString(
            SerializedSolution(
                feasible = true,
                optimal = true,
                objectiveValue = Flt64(2.0),
                gap = Flt64.zero,
                variableValues = listOf(Flt64.one),
                problemStatus = RemoteProblemStatus.FEASIBLE,
                solutionPresence = RemoteSolutionPresence.OPTIMAL,
                proofStatus = RemoteProofStatus.VERIFIED,
                elapsed = 12.milliseconds,
                solverStatus = "OPTIMAL"
            )
        ).encodeToByteArray()
        val http = QueueHttpHandler(
            mutableListOf(
                RemoteSolverHttpResponse(
                    statusCode = 200,
                    body =
                    """
                    {
                      "code": "OK",
                      "message": "success",
                      "data": {
                        "taskId": "task-1",
                        "accepted": true,
                        "status": "ACCEPTED",
                        "message": "accepted"
                      }
                    }
                    """.trimIndent()
                ),
                RemoteSolverHttpResponse(statusCode = 200, body = taskViewEnvelope("COMPLETED")),
                RemoteSolverHttpResponse(statusCode = 200, body = taskViewEnvelope("COMPLETED"))
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http,
            objectStoragePort = storage,
            requestIdProvider = { _, sliceId, _ -> RequestId.of("request-${sliceId.value}") }
        )

        val handle = client.start(
            payload = SolvePayload(SerializedLinearModel.empty()),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-a")
        ).valueOrFail()
        val slice = client.awaitSliceEnd(handle, 100.milliseconds).valueOrFail()

        assertEquals(TaskId.of("task-1"), handle.taskId)
        assertEquals(true, slice.completed)
        assertEquals(true, slice.feasible)
        assertEquals(Flt64(2.0), slice.objectiveValue)
        assertEquals(RemoteSolutionPresence.OPTIMAL, slice.solutionPresence)
        assertEquals(RemoteProofStatus.VERIFIED, slice.proofStatus)
        assertEquals(ObjectRef.of(path = "results/latest"), slice.resultRef)
        assertEquals(ObjectPath.of("tenant-a/payloads/task-1/slice-1.json"), storage.putPaths.single())
        assertEquals("POST", http.requests[0].method)
        assertEquals("GET", http.requests[1].method)
        val body = json.parseToJsonElement(http.requests[0].body ?: "").jsonObject
        assertEquals("request-slice-1", body.getValue("requestId").jsonPrimitive.content)
    }

    @Test
    fun stoppedTaskDoesNotInheritCompletedArtifactProof() = runBlocking {
        val storage = RecordingObjectStoragePort()
        storage.objects[ObjectPath.of("results/stopped")] = json.encodeToString(
            SerializedSolution(
                feasible = true,
                optimal = true,
                objectiveValue = Flt64(3.0),
                solutionPresence = RemoteSolutionPresence.OPTIMAL,
                proofStatus = RemoteProofStatus.VERIFIED,
                terminationReason = RemoteTerminationReason.COMPLETED
            )
        ).encodeToByteArray()
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = RecordingHttpHandler(
                RemoteSolverHttpResponse(
                    statusCode = 200,
                    body = taskViewEnvelope("STOPPED", "results/stopped")
                )
            ),
            objectStoragePort = storage
        )

        val result = client.fetchFinalResult(
            ExecutionHandle(
                handleId = HandleId.of("handle-stopped"),
                taskId = TaskId.of("task-1"),
                sliceId = SliceId.of("slice-1"),
                nodeId = NodeId.of("node-1"),
                startedAt = Instant.fromEpochMilliseconds(0L)
            )
        ).valueOrFail()

        assertNotNull(result)
        assertFalse(result!!.optimal)
        assertEquals(RemoteProofStatus.NONE, result.proofStatus)
        assertEquals(RemoteTerminationReason.CANCELLED, result.terminationReason)
        assertEquals(RemoteSolutionPresence.INCUMBENT, result.solutionPresence)
    }

    @Test
    fun strictCheckpointResumeFailsExplicitly() {
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = RecordingHttpHandler(RemoteSolverHttpResponse(statusCode = 200, body = actionEnvelope("QUEUED"))),
            resumeMode = RemoteSolverHttpResumeMode.STRICT_CHECKPOINT
        )

        val result = runBlocking {
            client.resume(
                payload = SolvePayload(SerializedLinearModel.empty()),
                checkpoint = ObjectRef.of(path = "checkpoints/specific"),
                taskId = TaskId.of("task-1"),
                sliceId = SliceId.of("slice-1"),
                nodeId = NodeId.of("node-1"),
                tenantId = TenantId.of("tenant-a")
            )
        }

        assertTrue(result is Failed)
        val error = result as Failed<*, *, *>
        assertEquals(ErrorCode.IllegalArgument, error.code)
        assertTrue(error.message?.contains("checkpoints/specific") == true)
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

    private fun taskViewEnvelope(status: String, latestResultPath: String = "results/latest"): String {
        return """
            {
              "code": "OK",
              "message": "success",
              "data": {
                "taskId": "task-1",
                "tenantId": "tenant-a",
                "status": "$status",
                "currentNodeId": "node-1",
                "latestCheckpointPath": "checkpoints/latest",
              "latestResultPath": "$latestResultPath",
                "consumedCost": 1.5
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

    private class RecordingObjectStoragePort : ObjectStoragePort {
        val objects = mutableMapOf<ObjectPath, ByteArray>()
        val putPaths = mutableListOf<ObjectPath>()

        override suspend fun put(
            path: ObjectPath,
            bytes: ByteArray,
            metadata: Map<String, String>
        ): ObjectRef {
            objects[path] = bytes
            putPaths.add(path)
            return ObjectRef(path = path)
        }

        override suspend fun get(ref: ObjectRef): ByteArray? {
            return objects[ref.path]
        }

        override suspend fun delete(ref: ObjectRef): Boolean {
            return objects.remove(ref.path) != null
        }

        override suspend fun exists(ref: ObjectRef): Boolean {
            return objects.containsKey(ref.path)
        }
    }
}
