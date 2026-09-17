@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.solver.remote.client

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.solver.report.CancellationRecord
import fuookami.ospf.kotlin.core.solver.report.CancellationSource
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.ObjectStoragePort
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.ExErr
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok

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
                deadline = Instant.fromEpochMilliseconds(123L),
                scheduling = SchedulingRequest(
                    complexity = TaskComplexity.COMPLEX,
                    priority = 3,
                    deadline = Instant.fromEpochMilliseconds(123L),
                    estimate = SchedulingEstimate(runtime = 1500.milliseconds),
                    preemptionMode = PreemptionMode.CONTROLLED_RETURN,
                    resumeMode = ResumeMode.WARM_START
                )
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
        val scheduling = body.getValue("scheduling").jsonObject
        assertEquals(1500L, scheduling.getValue("estimate").jsonObject
            .getValue("estimatedRuntimeMs").jsonPrimitive.long)
        assertEquals("CONTROLLED_RETURN", scheduling.getValue("preemptionMode").jsonPrimitive.content)
    }

    @Test
    fun startRejectsUnacceptedSubmissionInsteadOfReturningHandle() = runBlocking {
        val http = RecordingHttpHandler(
            RemoteSolverHttpResponse(
                statusCode = 200,
                body =
                """
                {
                  "code": "OK",
                  "message": "success",
                  "data": {
                    "taskId": "task-rejected",
                    "accepted": false,
                    "status": "WAITING_FOR_BUDGET",
                    "message": "budget is exhausted"
                  }
                }
                """.trimIndent()
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http,
            objectStoragePort = RecordingObjectStoragePort()
        )

        val result = client.start(
            payload = SolvePayload(SerializedLinearModel.empty()),
            taskId = TaskId.of("task-requested"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-a")
        )

        assertTrue(result is Failed)
        val failed = result as Failed<*, *, *>
        assertEquals(ErrorCode.ApplicationFailed, failed.code)
        assertTrue(failed.message?.contains("budget is exhausted") == true)
        assertTrue(failed.message?.contains("accepted=false") == true)
    }

    @Test
    fun v12SliceDecisionAndOutcomeDecodeWithMillisecondFields() {
        val result = json.decodeFromString(
            SliceResult.serializer(),
            """
            {
              "sliceId": "slice-1",
              "completed": false,
              "feasible": true,
              "objectiveValue": 42.0,
              "gap": 0.2,
              "elapsedMs": 1900,
              "checkpointRef": {"path": "checkpoints/task-1/slice-1"},
              "modelFingerprint": "sha256:model",
              "scheduling": {
                "dispatchId": "dispatch-1",
                "taskId": "task-1",
                "sliceId": "slice-1",
                "nodeId": "node-1",
                "quantumMs": 2000,
                "queueWaitMs": 125,
                "preemptionMode": "CONTROLLED_RETURN",
                "resumeMode": "WARM_START"
              },
              "outcome": "CHECKPOINTED"
            }
            """.trimIndent()
        )

        assertEquals(1900L, result.elapsed.inWholeMilliseconds)
        assertEquals(2000L, result.scheduling?.quantum?.inWholeMilliseconds)
        assertEquals(125L, result.scheduling?.queueWait?.inWholeMilliseconds)
        assertEquals(SliceOutcome.CHECKPOINTED, result.outcome)
        assertEquals(PreemptionMode.CONTROLLED_RETURN, result.scheduling?.preemptionMode)
        assertEquals(ResumeMode.WARM_START, result.scheduling?.resumeMode)
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
    fun getMapsTaskIdentityFields() {
        val http = RecordingHttpHandler(
            RemoteSolverHttpResponse(
                statusCode = 200,
                body =
                """
                {
                  "code": "OK",
                  "message": "success",
                  "data": {
                    "taskId": "task-identity",
                    "tenantId": "tenant-a",
                    "status": "RUNNING",
                    "dispatchId": "dispatch-1",
                    "runId": "run-1",
                    "attemptId": "attempt-1",
                    "artifactDigest": "artifact-1",
                    "modelFingerprint": {
                      "schemaVersion": "1.0",
                      "algorithm": "SHA-256",
                      "value": "model-1"
                    },
                    "configurationFingerprint": {
                      "schemaVersion": "1.0",
                      "algorithm": "SHA-256",
                      "value": "configuration-1"
                    },
                    "solverFingerprint": "solver-1",
                    "solverFingerprintSchema": "2.0",
                    "fingerprints": {
                      "extra": "extra-1"
                    },
                    "fingerprintSchemas": {
                      "extra": "1.0"
                    },
                    "provenance": {
                      "backend": "remote",
                      "threads": 4
                    }
                  }
                }
                """.trimIndent()
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http
        )

        val response = client.get(TaskId.of("task-identity")).valueOrFail()

        assertNotNull(response)
        assertEquals(DispatchId.of("dispatch-1"), response?.dispatchId)
        assertEquals("run-1", response?.runId)
        assertEquals("attempt-1", response?.attemptId)
        assertEquals("artifact-1", response?.artifactDigest)
        assertEquals("model-1", response?.modelFingerprint)
        assertEquals("1.0", response?.modelFingerprintSchema)
        assertEquals("configuration-1", response?.configurationFingerprint)
        assertEquals("1.0", response?.configurationFingerprintSchema)
        assertEquals("solver-1", response?.solverFingerprint)
        assertEquals("2.0", response?.solverFingerprintSchema)
        assertEquals("extra-1", response?.fingerprints?.get("extra"))
        assertEquals("model-1", response?.fingerprints?.get("model"))
        assertEquals("configuration-1", response?.fingerprints?.get("configuration"))
        assertEquals("solver-1", response?.fingerprints?.get("solver"))
        assertEquals("4", response?.provenance?.get("threads"))
    }

    @Test
    fun getToleratesUnknownWireEnumValues() {
        val http = RecordingHttpHandler(
            RemoteSolverHttpResponse(
                statusCode = 200,
                body =
                """
                {
                  "code": "OK",
                  "message": "success",
                  "data": {
                    "taskId": "task-future",
                    "tenantId": "tenant-a",
                    "status": "FUTURE_TASK_STATUS",
                    "outcome": "FUTURE_SLICE_OUTCOME",
                    "problemStatus": "FUTURE_PROBLEM_STATUS",
                    "terminationReason": "FUTURE_TERMINATION_REASON",
                    "solutionPresence": "FUTURE_SOLUTION_PRESENCE",
                    "proofStatus": "FUTURE_PROOF_STATUS",
                    "scheduling": {
                      "preemptionMode": "FUTURE_PREEMPTION_MODE",
                      "resumeMode": "FUTURE_RESUME_MODE",
                      "outcome": "FUTURE_SCHEDULING_OUTCOME"
                    },
                    "slice": {
                      "sliceId": "slice-future",
                      "completed": false,
                      "feasible": true,
                      "objectiveValue": 1.5,
                      "gap": null,
                      "elapsedMs": 12,
                      "problemStatus": "FUTURE_PROBLEM_STATUS",
                      "terminationReason": "FUTURE_TERMINATION_REASON",
                      "solutionPresence": "FUTURE_SOLUTION_PRESENCE",
                      "proofStatus": "FUTURE_PROOF_STATUS",
                      "outcome": "FUTURE_SLICE_OUTCOME",
                      "scheduling": {
                        "preemptionMode": "FUTURE_PREEMPTION_MODE",
                        "resumeMode": "FUTURE_RESUME_MODE",
                        "outcome": "FUTURE_SCHEDULING_OUTCOME"
                      }
                    }
                  }
                }
                """.trimIndent()
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http
        )

        val response = client.get(TaskId.of("task-future")).valueOrFail()

        assertNotNull(response)
        assertEquals(TaskStatus.UNKNOWN, response?.status)
        assertEquals(SliceOutcome.UNKNOWN, response?.outcome)
        assertEquals(RemoteProblemStatus.UNKNOWN, response?.problemStatus)
        assertEquals(RemoteTerminationReason.UNKNOWN, response?.terminationReason)
        assertEquals(RemoteSolutionPresence.UNKNOWN, response?.solutionPresence)
        assertEquals(RemoteProofStatus.UNKNOWN, response?.proofStatus)
        assertNotNull(response?.scheduling)
        assertEquals(PreemptionMode.UNKNOWN, response?.scheduling?.preemptionMode)
        assertEquals(ResumeMode.UNKNOWN, response?.scheduling?.resumeMode)
        assertEquals(SliceOutcome.UNKNOWN, response?.scheduling?.outcome)
        assertNotNull(response?.slice)
        assertEquals(RemoteProblemStatus.UNKNOWN, response?.slice?.problemStatus)
        assertEquals(RemoteTerminationReason.UNKNOWN, response?.slice?.terminationReason)
        assertEquals(RemoteSolutionPresence.UNKNOWN, response?.slice?.solutionPresence)
        assertEquals(RemoteProofStatus.UNKNOWN, response?.slice?.proofStatus)
        assertEquals(SliceOutcome.UNKNOWN, response?.slice?.outcome)
        assertEquals(PreemptionMode.UNKNOWN, response?.slice?.scheduling?.preemptionMode)
        assertEquals(ResumeMode.UNKNOWN, response?.slice?.scheduling?.resumeMode)
        assertEquals(SliceOutcome.UNKNOWN, response?.slice?.scheduling?.outcome)
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
    fun stopHandleReportsRejectedActionAsFalse() = runBlocking {
        val http = RecordingHttpHandler(
            RemoteSolverHttpResponse(
                statusCode = 200,
                body = actionEnvelope(
                    status = "RUNNING",
                    accepted = false
                )
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http
        )
        val handle = ExecutionHandle(
            handleId = HandleId.of("handle-stop-rejected"),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            startedAt = Instant.fromEpochMilliseconds(0L)
        )

        val result = client.stop(handle)

        assertTrue(result is Ok)
        assertFalse((result as Ok<Boolean, *, *>).value)
    }

    @Test
    fun stopHandleConfirmsStoppingActionWithTerminalTaskView() = runBlocking {
        val http = QueueHttpHandler(
            mutableListOf(
                RemoteSolverHttpResponse(
                    statusCode = 200,
                    body = actionEnvelope("STOPPING")
                ),
                RemoteSolverHttpResponse(
                    statusCode = 200,
                    body = taskViewEnvelope("STOPPED")
                )
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http,
            pollInterval = 1.milliseconds
        )
        val handle = ExecutionHandle(
            handleId = HandleId.of("handle-stop-confirmed"),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            startedAt = Instant.fromEpochMilliseconds(0L)
        )

        val result = client.stop(handle)

        assertTrue(result is Ok)
        assertTrue((result as Ok<Boolean, *, *>).value)
        assertEquals(2, http.requests.size)
        assertEquals("POST", http.requests[0].method)
        assertEquals("GET", http.requests[1].method)
    }

    @Test
    fun actionMapsIdentityFields() {
        val http = RecordingHttpHandler(
            RemoteSolverHttpResponse(
                statusCode = 200,
                body =
                """
                {
                  "code": "OK",
                  "message": "success",
                  "data": {
                    "taskId": "task-action",
                    "accepted": true,
                    "status": "STOPPING",
                    "dispatchId": "dispatch-action",
                    "sliceId": "slice-action",
                    "runId": "run-action",
                    "attemptId": "attempt-action",
                    "artifactDigest": "artifact-action",
                    "modelFingerprint": {
                      "schemaVersion": "1.0",
                      "algorithm": "SHA-256",
                      "value": "model-action"
                    },
                    "configurationFingerprint": "configuration-action",
                    "solverFingerprint": "solver-action",
                    "fingerprints": {
                      "extra": "extra-action"
                    },
                    "fingerprintSchemas": {
                      "extra": "1.0"
                    },
                    "provenance": {
                      "solverId": "solver-action",
                      "backendName": "remote",
                      "backendVersion": "1.2.3",
                      "pluginVersion": "plugin-1",
                      "requestedConfiguration": {
                        "threads": "4"
                      },
                      "effectiveConfiguration": {
                        "threads": "3"
                      },
                      "threadCount": 3,
                      "randomSeed": 17,
                      "deterministic": true,
                      "environmentSummary": {
                        "host": "node-1"
                      }
                    },
                    "cancellationChain": [
                      {
                        "origin": "remoteStop",
                        "requestedAtEpochMs": 100,
                        "reason": "operator pressed stop"
                      },
                      {
                        "origin": "backend",
                        "requestedAtEpochMs": 200,
                        "reason": null
                      }
                    ],
                    "message": "stop accepted"
                  }
                }
                """.trimIndent()
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http
        )

        val action = client.stop(TaskId.of("task-action")).valueOrFail()

        assertTrue(action.accepted)
        assertEquals(TaskStatus.STOPPING, action.status)
        assertEquals(DispatchId.of("dispatch-action"), action.dispatchId)
        assertEquals(SliceId.of("slice-action"), action.sliceId)
        assertEquals("run-action", action.runId)
        assertEquals("attempt-action", action.attemptId)
        assertEquals("artifact-action", action.artifactDigest)
        assertEquals("model-action", action.modelFingerprint)
        assertEquals("1.0", action.modelFingerprintSchema)
        assertEquals("configuration-action", action.configurationFingerprint)
        assertEquals("solver-action", action.solverFingerprint)
        assertEquals("extra-action", action.fingerprints["extra"])
        assertEquals("solver-action", action.provenance?.solverId)
        assertEquals("remote", action.provenance?.backendName)
        assertEquals("1.2.3", action.provenance?.backendVersion)
        assertEquals("4", action.provenance?.requestedConfiguration?.get("threads"))
        assertEquals("3", action.provenance?.effectiveConfiguration?.get("threads"))
        assertEquals("node-1", action.provenance?.environmentSummary?.get("host"))
        assertEquals(3, action.provenance?.threadCount)
        assertEquals(17L, action.provenance?.randomSeed)
        assertEquals(true, action.provenance?.deterministic)
        assertEquals(
            listOf(
                RemoteTaskActionCancellation("remoteStop", 100L, "operator pressed stop"),
                RemoteTaskActionCancellation("backend", 200L, null)
            ),
            action.cancellationChain
        )
        assertEquals("stop accepted", action.message)
    }

    /**
     * 验证 task-action 取消链与 checkpoint envelope 共用**同一份**规范代码词表。
     *
     * 两个面此前各写一种拼写：checkpoint 面写 `user`，而 task-action 面写 SCREAMING_SNAKE 的变体名
     * （`USER`）。同一个枚举在线格式上出现两种拼写时，对端只能把不认识的拼写当成未知代码兜底，身份
     * 与来源语义随之丢失。本用例把两侧钉在同一词表上，并覆盖本侧没有专用变体的代码（`backend`、
     * 完全未知的代码），它们必须原样往返。
     *
     * Verifies that the task-action cancellation chain and the checkpoint envelope share **one** canonical
     * code vocabulary. The two faces previously spelled codes differently: the checkpoint face wrote `user`
     * while the task-action face wrote the SCREAMING_SNAKE variant name (`USER`). When one enum surfaces two
     * spellings on the wire, a peer can only treat the unrecognized spelling as an unknown code, losing the
     * identity and origin semantics. This test pins both faces to one vocabulary and covers codes with no
     * dedicated variant on this side (`backend`, wholly unknown codes), which must round-trip verbatim.
     */
    @Test
    fun taskActionCancellationUsesTheSharedCanonicalOriginVocabulary() {
        val sources = listOf(
            CancellationSource.Caller,
            CancellationSource.Callback,
            CancellationSource.Combinatorial,
            CancellationSource.Remote,
            CancellationSource.Coroutine,
            CancellationSource.Future,
            CancellationSource.Timeout,
            CancellationSource.Other
        )
        val records = sources.mapIndexed { index, source ->
            CancellationRecord(
                source = source,
                requestedAt = java.time.Instant.ofEpochMilli(1_000L + index),
                reason = "reason-$index"
            )
        }

        records.forEach { record ->
            val wire = RemoteTaskActionCancellation.fromCancellationRecord(record)
            // 输出的必须是规范代码，且**不是** SCREAMING_SNAKE 变体名。
            // The output must be the canonical code and **not** the SCREAMING_SNAKE variant name.
            assertEquals(record.source.toWireCode(), wire.origin)
            assertTrue(
                !wire.origin.contains('_') && wire.origin != wire.origin.uppercase(),
                "SCREAMING_SNAKE 变体名不得出现在线格式上 / the SCREAMING_SNAKE variant name must never reach the wire: ${wire.origin}"
            )
            assertEquals(record.reason, wire.reason)

            // 往返必须无损：来源变体、时间戳与原因都不得变化。
            // The round trip must be lossless: the source variant, timestamp, and reason must not change.
            val restored = wire.toCancellationRecord()
            assertEquals(record.source, restored.source)
            assertEquals(record.requestedAt, restored.requestedAt)
            assertEquals(record.reason, restored.reason)
        }

        // 本侧没有专用变体的代码必须**原样**往返，而不是被折叠成某个兜底代码。
        // Codes with no dedicated variant on this side must round-trip **verbatim** rather than collapsing
        // into some catch-all code.
        listOf("backend", "future", "timeout", "wholly-unknown").forEach { code ->
            val wire = RemoteTaskActionCancellation(code, 4_242L, "why")
            val restored = wire.toCancellationRecord()
            val back = RemoteTaskActionCancellation.fromCancellationRecord(restored)
            assertEquals(code, back.origin, "代码 $code 必须原样往返 / code $code must round-trip verbatim")
            assertEquals(4_242L, back.requestedAtEpochMs)
            assertEquals("why", back.reason)
        }
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
    fun dispatcherSuspensionEndsSliceWithoutClientStop() = runBlocking {
        val http = QueueHttpHandler(
            mutableListOf(
                RemoteSolverHttpResponse(statusCode = 200, body = taskViewEnvelope("RUNNING")),
                RemoteSolverHttpResponse(statusCode = 200, body = taskViewEnvelope("SUSPENDED"))
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http,
            pollInterval = 5.milliseconds
        )
        val handle = ExecutionHandle(
            handleId = HandleId.of("handle-timeout"),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            startedAt = Instant.fromEpochMilliseconds(0L)
        )

        val result = client.awaitSliceEnd(handle, 5.milliseconds)

        assertTrue(result is Ok)
        val slice = (result as Ok<SliceResult, *, *>).value
        assertFalse(slice.completed)
        assertEquals(SliceOutcome.CHECKPOINTED, slice.outcome)
        assertEquals(2, http.requests.size)
        assertEquals("GET", http.requests[0].method)
        assertEquals("GET", http.requests[1].method)
        assertTrue(http.requests.none { it.method == "POST" })
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
    fun unknownTaskStatusWithSuccessfulArtifactRemainsUnknownFailure() = runBlocking {
        val storage = RecordingObjectStoragePort()
        storage.objects[ObjectPath.of("results/unknown-status")] = json.encodeToString(
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
                    body = """
                        {
                          "code": "OK",
                          "message": "success",
                          "data": {
                            "taskId": "task-1",
                            "tenantId": "tenant-a",
                            "status": "FUTURE_TASK_STATUS",
                            "latestResultPath": "results/unknown-status",
                            "consumedCost": 0.0
                          }
                        }
                    """.trimIndent()
                )
            ),
            objectStoragePort = storage
        )

        val result = client.fetchFinalResult(
            ExecutionHandle(
                handleId = HandleId.of("handle-unknown-status"),
                taskId = TaskId.of("task-1"),
                sliceId = SliceId.of("slice-1"),
                nodeId = NodeId.of("node-1"),
                startedAt = Instant.fromEpochMilliseconds(0L)
            )
        ).valueOrFail()

        assertNotNull(result)
        assertFalse(result!!.feasible)
        assertFalse(result.optimal)
        assertEquals(RemoteProblemStatus.UNKNOWN, result.problemStatus)
        assertEquals(RemoteTerminationReason.UNKNOWN, result.terminationReason)
        assertEquals(RemoteSolutionPresence.UNKNOWN, result.solutionPresence)
        assertEquals(RemoteProofStatus.UNKNOWN, result.proofStatus)
        assertEquals(SliceOutcome.UNKNOWN, result.outcome)
    }

    @Test
    fun unknownArtifactEnumsDoNotInheritSuccessfulTaskViewSemantics() = runBlocking {
        val storage = RecordingObjectStoragePort()
        storage.objects[ObjectPath.of("results/unknown-artifact")] = """
            {
              "feasible": true,
              "optimal": true,
              "objectiveValue": 3.0,
              "solutionPresence": "FUTURE_SOLUTION_PRESENCE",
              "proofStatus": "FUTURE_PROOF_STATUS",
              "terminationReason": "FUTURE_TERMINATION_REASON",
              "elapsedMs": 1
            }
        """.trimIndent().encodeToByteArray()
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = RecordingHttpHandler(
                RemoteSolverHttpResponse(
                    statusCode = 200,
                    body = """
                        {
                          "code": "OK",
                          "message": "success",
                          "data": {
                            "taskId": "task-1",
                            "tenantId": "tenant-a",
                            "status": "COMPLETED",
                            "latestResultPath": "results/unknown-artifact",
                            "outcome": "COMPLETED",
                            "problemStatus": "FEASIBLE",
                            "terminationReason": "COMPLETED",
                            "solutionPresence": "OPTIMAL",
                            "proofStatus": "VERIFIED",
                            "consumedCost": 0.0
                          }
                        }
                    """.trimIndent()
                )
            ),
            objectStoragePort = storage
        )

        val result = client.fetchFinalResult(
            ExecutionHandle(
                handleId = HandleId.of("handle-unknown-artifact"),
                taskId = TaskId.of("task-1"),
                sliceId = SliceId.of("slice-1"),
                nodeId = NodeId.of("node-1"),
                startedAt = Instant.fromEpochMilliseconds(0L)
            )
        ).valueOrFail()

        assertNotNull(result)
        assertFalse(result!!.feasible)
        assertFalse(result.optimal)
        assertEquals(RemoteProblemStatus.UNKNOWN, result.problemStatus)
        assertEquals(RemoteTerminationReason.UNKNOWN, result.terminationReason)
        assertEquals(RemoteSolutionPresence.UNKNOWN, result.solutionPresence)
        assertEquals(RemoteProofStatus.UNKNOWN, result.proofStatus)
        assertEquals(SliceOutcome.UNKNOWN, result.outcome)
    }

    @Test
    fun defaultCheckpointResumeFailsExplicitly() {
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = RecordingHttpHandler(RemoteSolverHttpResponse(statusCode = 200, body = actionEnvelope("QUEUED")))
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

    @Test
    fun explicitLatestResumeCallsServerLatestEndpointAfterVerifyingSourceCheckpoint() {
        val snapshot = REMOTE_CP_SNAPSHOT
        val checkpointPath = ObjectPath.of("tenant-a/checkpoint/task-1/slice-1-123")
        val storage = RecordingObjectStoragePort()
        storage.objects[checkpointPath] = PortableCheckpointCodec.encode(
            PortableCheckpointEnvelope(
                checkpointId = "slice-1-123",
                identitySchemaVersion = "1.0",
                identityNamespace = "model-local",
                modelName = "remote-cp",
                modelFingerprint = PortableCheckpointCodec.sha256(snapshot),
                configurationFingerprint = "configuration-1",
                solverFingerprint = "solver-1",
                runId = "task-1",
                attemptId = "slice-1",
                createdAtEpochMs = 123L,
                snapshotJson = snapshot
            )
        ).encodeToByteArray()
        val http = RecordingHttpHandler(
            RemoteSolverHttpResponse(
                statusCode = 200,
                body = """
                    {
                      "code": "OK",
                      "message": "success",
                      "data": {
                        "taskId": "task-1",
                        "tenantId": "tenant-a",
                        "accepted": true,
                        "status": "RUNNING",
                        "runId": "task-1",
                        "attemptId": "slice-1",
                        "modelFingerprint": "${PortableCheckpointCodec.sha256(snapshot)}",
                        "configurationFingerprint": "configuration-1",
                        "solverFingerprint": "solver-1"
                      }
                    }
                """.trimIndent()
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http,
            objectStoragePort = storage,
            resumeMode = RemoteSolverHttpResumeMode.SERVER_TASK_LATEST_CHECKPOINT
        )

        val result = runBlocking {
            client.resume(
                payload = SolvePayload(
                    modelData = ModelData.raw(
                        bytes = snapshot.encodeToByteArray(),
                        format = "ospf-cp-snapshot-json"
                    ),
                    extension = mapOf(
                        "configurationFingerprint" to "configuration-1",
                        "solverFingerprint" to "solver-1"
                    )
                ),
                checkpoint = ObjectRef.of(path = checkpointPath.value),
                taskId = TaskId.of("task-1"),
                sliceId = SliceId.of("slice-1"),
                nodeId = NodeId.of("node-1"),
                tenantId = TenantId.of("tenant-a")
            )
        }

        assertTrue(result is Ok)
        assertEquals("POST", http.lastRequest?.method)
        assertEquals("http://localhost/api/v1/tasks/task-1/resume", http.lastRequest?.url)
    }

    @Test
    fun resumeRejectsCheckpointFromAnotherTenantBeforeCallingServer() = runBlocking {
        val http = QueueHttpHandler(mutableListOf())
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http,
            objectStoragePort = RecordingObjectStoragePort(),
            resumeMode = RemoteSolverHttpResumeMode.SERVER_TASK_LATEST_CHECKPOINT
        )

        val result = client.resume(
            payload = SolvePayload(SerializedLinearModel.empty()),
            checkpoint = ObjectRef.of("tenant-b/checkpoint/task-1/slice-1-123"),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-a")
        )

        assertTrue(result is Failed)
        assertTrue((result as Failed<*, *, *>).message?.contains("tenant-b") == true)
        assertTrue(http.requests.isEmpty())
    }

    @Test
    fun resumeRejectsCheckpointForAnotherTaskBeforeCallingServer() = runBlocking {
        val http = QueueHttpHandler(mutableListOf())
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http,
            objectStoragePort = RecordingObjectStoragePort(),
            resumeMode = RemoteSolverHttpResumeMode.SERVER_TASK_LATEST_CHECKPOINT
        )

        val result = client.resume(
            payload = SolvePayload(SerializedLinearModel.empty()),
            checkpoint = ObjectRef.of("tenant-a/checkpoint/task-2/slice-1-123"),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-a")
        )

        assertTrue(result is Failed)
        assertTrue((result as Failed<*, *, *>).message?.contains("task-2") == true)
        assertTrue(http.requests.isEmpty())
    }

    @Test
    fun resumeRejectsTamperedCheckpointIntegrityBeforeCallingServer() = runBlocking {
        val snapshot = REMOTE_CP_SNAPSHOT
        val checkpointPath = ObjectPath.of("tenant-a/checkpoint/task-1/slice-1-123")
        val storage = RecordingObjectStoragePort()
        val encoded = PortableCheckpointCodec.encode(
            PortableCheckpointEnvelope(
                checkpointId = "slice-1-123",
                identitySchemaVersion = "1.0",
                identityNamespace = "model-local",
                modelName = "remote-cp",
                modelFingerprint = PortableCheckpointCodec.sha256(snapshot),
                configurationFingerprint = "configuration-1",
                solverFingerprint = "solver-1",
                runId = "task-1",
                attemptId = "slice-1",
                createdAtEpochMs = 123L,
                snapshotJson = snapshot
            )
        )
        storage.objects[checkpointPath] = encoded
            .replace(jsonEscaped(REMOTE_CP_SNAPSHOT), jsonEscaped(TAMPERED_CP_SNAPSHOT))
            .encodeToByteArray()
        val http = QueueHttpHandler(mutableListOf())
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http,
            objectStoragePort = storage,
            resumeMode = RemoteSolverHttpResumeMode.SERVER_TASK_LATEST_CHECKPOINT
        )

        val result = client.resume(
            payload = SolvePayload(
                modelData = ModelData.raw(snapshot.encodeToByteArray(), "ospf-cp-snapshot-json"),
                extension = mapOf(
                    "configurationFingerprint" to "configuration-1",
                    "solverFingerprint" to "solver-1"
                )
            ),
            checkpoint = ObjectRef.of(checkpointPath.value),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-a")
        )

        assertTrue(result is Failed)
        assertTrue((result as Failed<*, *, *>).message?.contains("verified portable v2") == true)
        assertTrue(http.requests.isEmpty())
    }

    @Test
    fun resumeRejectsCheckpointWithoutConfigurationOrSolverIdentity() = runBlocking {
        val snapshot = REMOTE_CP_SNAPSHOT
        val checkpointPath = ObjectPath.of("tenant-a/checkpoint/task-1/slice-1-123")
        val storage = RecordingObjectStoragePort()
        storage.objects[checkpointPath] = PortableCheckpointCodec.encode(
            PortableCheckpointEnvelope(
                checkpointId = "slice-1-123",
                identitySchemaVersion = "1.0",
                identityNamespace = "model-local",
                modelName = "remote-cp",
                modelFingerprint = PortableCheckpointCodec.sha256(snapshot),
                runId = "task-1",
                attemptId = "slice-1",
                createdAtEpochMs = 123L,
                snapshotJson = snapshot
            )
        ).encodeToByteArray()
        val http = QueueHttpHandler(mutableListOf())
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http,
            objectStoragePort = storage,
            resumeMode = RemoteSolverHttpResumeMode.SERVER_TASK_LATEST_CHECKPOINT
        )

        val result = client.resume(
            payload = SolvePayload(
                modelData = ModelData.raw(snapshot.encodeToByteArray(), "ospf-cp-snapshot-json"),
                extension = mapOf(
                    "configurationFingerprint" to "configuration-1",
                    "solverFingerprint" to "solver-1"
                )
            ),
            checkpoint = ObjectRef.of(checkpointPath.value),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-a")
        )

        assertTrue(result is Failed)
        assertTrue((result as Failed<*, *, *>).message?.contains("identity is incomplete") == true)
        assertTrue(http.requests.isEmpty())
    }

    @Test
    fun resumeRejectsActionWithoutCanonicalCheckpointIdentity() = runBlocking {
        val snapshot = REMOTE_CP_SNAPSHOT
        val checkpointPath = ObjectPath.of("tenant-a/checkpoint/task-1/slice-1-123")
        val storage = RecordingObjectStoragePort()
        storage.objects[checkpointPath] = PortableCheckpointCodec.encode(
            PortableCheckpointEnvelope(
                checkpointId = "slice-1-123",
                identitySchemaVersion = "1.0",
                identityNamespace = "model-local",
                modelName = "remote-cp",
                modelFingerprint = PortableCheckpointCodec.sha256(snapshot),
                configurationFingerprint = "configuration-1",
                solverFingerprint = "solver-1",
                runId = "task-1",
                attemptId = "slice-1",
                createdAtEpochMs = 123L,
                snapshotJson = snapshot
            )
        ).encodeToByteArray()
        val http = RecordingHttpHandler(
            RemoteSolverHttpResponse(statusCode = 200, body = actionEnvelope("RUNNING"))
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http,
            objectStoragePort = storage,
            resumeMode = RemoteSolverHttpResumeMode.SERVER_TASK_LATEST_CHECKPOINT
        )

        val result = client.resume(
            payload = SolvePayload(
                modelData = ModelData.raw(snapshot.encodeToByteArray(), "ospf-cp-snapshot-json"),
                extension = mapOf(
                    "configurationFingerprint" to "configuration-1",
                    "solverFingerprint" to "solver-1"
                )
            ),
            checkpoint = ObjectRef.of(checkpointPath.value),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-a")
        )

        assertTrue(result is Failed)
        assertTrue((result as Failed<*, *, *>).message?.contains("selected checkpoint identity") == true)
    }

    @Test
    fun resumeRejectsActionWithMismatchedAttemptOrFingerprint() = runBlocking {
        val snapshot = REMOTE_CP_SNAPSHOT
        val checkpointPath = ObjectPath.of("tenant-a/checkpoint/task-1/slice-1-123")
        val modelFingerprint = PortableCheckpointCodec.sha256(snapshot)
        val storage = RecordingObjectStoragePort()
        storage.objects[checkpointPath] = PortableCheckpointCodec.encode(
            PortableCheckpointEnvelope(
                checkpointId = "slice-1-123",
                identitySchemaVersion = "1.0",
                identityNamespace = "model-local",
                modelName = "remote-cp",
                modelFingerprint = modelFingerprint,
                configurationFingerprint = "configuration-1",
                solverFingerprint = "solver-1",
                runId = "task-1",
                attemptId = "slice-1",
                createdAtEpochMs = 123L,
                snapshotJson = snapshot
            )
        ).encodeToByteArray()
        val http = RecordingHttpHandler(
            RemoteSolverHttpResponse(
                statusCode = 200,
                body = """
                    {
                      "code": "OK",
                      "message": "success",
                      "data": {
                        "taskId": "task-1",
                        "tenantId": "tenant-a",
                        "accepted": true,
                        "status": "RUNNING",
                        "runId": "task-1",
                        "attemptId": "other-slice",
                        "modelFingerprint": "$modelFingerprint",
                        "configurationFingerprint": "configuration-1",
                        "solverFingerprint": "other-solver"
                      }
                    }
                """.trimIndent()
            )
        )
        val client = RemoteSolverHttpClient(
            baseUrl = "http://localhost",
            transport = http,
            objectStoragePort = storage,
            resumeMode = RemoteSolverHttpResumeMode.SERVER_TASK_LATEST_CHECKPOINT
        )

        val result = client.resume(
            payload = SolvePayload(
                modelData = ModelData.raw(snapshot.encodeToByteArray(), "ospf-cp-snapshot-json"),
                extension = mapOf(
                    "configurationFingerprint" to "configuration-1",
                    "solverFingerprint" to "solver-1"
                )
            ),
            checkpoint = ObjectRef.of(checkpointPath.value),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-a")
        )

        assertTrue(result is Failed)
        val error = result as Failed<*, *, *>
        assertTrue(error.message?.contains("attemptId") == true)
        assertTrue(error.message?.contains("solverFingerprint") == true)
    }

    private fun actionEnvelope(status: String, accepted: Boolean = true): String {
        return """
            {
              "code": "OK",
              "message": "success",
              "data": {
                "taskId": "task-1",
                "accepted": $accepted,
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

    /**
     * JSON 转义一段已序列化文本，便于在签名后的 JSON 文档中定位它。 /
     * JSON-escape already-serialized text so it can be located inside a signed JSON document.
     *
     * @param text 待转义文本 / Text to escape
     * @return 作为 JSON 字符串片段出现时使用的转义形式 / The escaped form used when it appears inside a JSON string value
     */
    private fun jsonEscaped(text: String): String {
        return text.replace("\"", "\\\"")
    }

    private companion object {
        /**
         * 最小但**规范编码**的 CP snapshot。 / The minimal but **canonically encoded** CP snapshot.
         *
         * 这些用例此前使用占位的 `"{}"`：`PortableCheckpointCodec` 当时是 core codec 的一份平行实现，
         * 完全不校验 snapshot。收敛后它委托 core，而 core 会先规范化 snapshot 再计算指纹，因此占位
         * 串已无法解码（它甚至反序列化不成 `SnapshotPayload`）。这里改用规范编码的最小 snapshot，
         * 用例本身的意图（校验客户端身份与指纹比对）完全不变。
         *
         * These cases used the placeholder `"{}"`: `PortableCheckpointCodec` was then a parallel
         * implementation of the core codec and never validated the snapshot. After convergence it
         * delegates to core, which canonicalizes the snapshot before fingerprinting it, so the
         * placeholder can no longer decode (it does not even deserialize into `SnapshotPayload`). The
         * canonically encoded minimal snapshot replaces it, leaving each case's intent — verifying the
         * client-side identity and fingerprint comparisons — unchanged.
         */
        const val REMOTE_CP_SNAPSHOT: String =
            "{\"schema\":1,\"name\":\"remote-cp\",\"objectCategory\":\"Minimum\",\"variables\":[]," +
                "\"intervals\":[],\"expressions\":[],\"constraints\":[],\"objectives\":[]," +
                "\"constraintGroups\":[],\"identitySchemaVersion\":\"1.0\"," +
                "\"identityNamespace\":\"model-local\"}"

        /** 用于篡改场景的替代 snapshot 内容。 / Replacement snapshot content used by the tampering scenario. */
        const val TAMPERED_CP_SNAPSHOT: String =
            "{\"schema\":1,\"name\":\"tampered\",\"objectCategory\":\"Minimum\",\"variables\":[]," +
                "\"intervals\":[],\"expressions\":[],\"constraints\":[],\"objectives\":[]," +
                "\"constraintGroups\":[],\"identitySchemaVersion\":\"1.0\"," +
                "\"identityNamespace\":\"model-local\"}"
    }
}
