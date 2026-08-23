@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.solver.remote.client

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig as CoreSolverConfig
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.*

/**
 * 远程求解器客户端测试。
 * Remote solver client tests.
 */
class RemoteSolverClientTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 验证线性/二次协议覆盖完整终态组合。
     * Verifies the linear/quadratic protocol covers the complete terminal-state combinations.
     */
    @Test
    fun linearQuadraticTerminalStateMatrixAcceptsDeclaredCombinations() {
        val feasibleLimits = listOf(
            RemoteTerminationReason.TIME_LIMIT,
            RemoteTerminationReason.NODE_LIMIT,
            RemoteTerminationReason.ITERATION_LIMIT,
            RemoteTerminationReason.SOLUTION_LIMIT,
            RemoteTerminationReason.OBJECTIVE_LIMIT,
            RemoteTerminationReason.CANCELLED,
            RemoteTerminationReason.INTERRUPTED,
            RemoteTerminationReason.NUMERICAL_FAILURE,
            RemoteTerminationReason.BACKEND_FAILURE
        )
        feasibleLimits.forEach { reason ->
            val result = SolveResult(
                feasible = true,
                optimal = false,
                objectiveValue = Flt64(3.0),
                gap = Flt64(0.5),
                elapsed = Duration.ZERO,
                schemaVersion = "2.0",
                problemStatus = RemoteProblemStatus.FEASIBLE,
                terminationReason = reason,
                solutionPresence = RemoteSolutionPresence.INCUMBENT,
                proofStatus = RemoteProofStatus.CLAIMED,
                fingerprints = mapOf("model" to "m", "solver" to "s"),
                fingerprintSchemas = mapOf("model" to "1.0", "solver" to "2.0"),
                runId = "run-1",
                attemptId = "attempt-1"
            )
            assertTrue(result.validateLinearQuadraticResult() is Ok, reason.name)
        }

        listOf(
            RemoteProblemStatus.INFEASIBLE to RemoteTerminationReason.COMPLETED,
            RemoteProblemStatus.UNBOUNDED to RemoteTerminationReason.COMPLETED,
            RemoteProblemStatus.INFEASIBLE_OR_UNBOUNDED to RemoteTerminationReason.COMPLETED,
            RemoteProblemStatus.UNKNOWN to RemoteTerminationReason.BACKEND_FAILURE
        ).forEach { (status, reason) ->
            val result = SolveResult(
                feasible = false,
                optimal = false,
                objectiveValue = null,
                gap = null,
                elapsed = Duration.ZERO,
                schemaVersion = "2.0",
                problemStatus = status,
                terminationReason = reason,
                solutionPresence = RemoteSolutionPresence.NONE,
                proofStatus = RemoteProofStatus.NONE,
                fingerprints = mapOf("model" to "m", "solver" to "s"),
                fingerprintSchemas = mapOf("model" to "1.0", "solver" to "2.0"),
                runId = "run-1",
                attemptId = "attempt-1"
            )
            assertTrue(result.validateLinearQuadraticResult() is Ok, status.name)
        }

        val optimal = SolveResult(
            feasible = true,
            optimal = true,
            objectiveValue = Flt64(8.0),
            gap = Flt64.zero,
            elapsed = Duration.ZERO,
            schemaVersion = "2.0",
            problemStatus = RemoteProblemStatus.FEASIBLE,
            terminationReason = RemoteTerminationReason.COMPLETED,
            solutionPresence = RemoteSolutionPresence.OPTIMAL,
            proofStatus = RemoteProofStatus.VERIFIED,
            fingerprints = mapOf("model" to "m", "solver" to "s"),
            fingerprintSchemas = mapOf("model" to "1.0", "solver" to "2.0"),
            runId = "run-1",
            attemptId = "attempt-1"
        )
        assertTrue(optimal.validateLinearQuadraticResult() is Ok)
    }

    /**
     * 验证未知未来主版本和非法正交状态被拒绝。
     * Verifies unknown future majors and invalid orthogonal states are rejected.
     */
    @Test
    fun linearQuadraticProtocolRejectsFutureSchemaAndInvalidState() {
        val future = SolveResult(
            feasible = true,
            optimal = false,
            objectiveValue = Flt64.one,
            gap = Flt64.zero,
            elapsed = Duration.ZERO,
            schemaVersion = "3.0",
            problemStatus = RemoteProblemStatus.FEASIBLE,
            solutionPresence = RemoteSolutionPresence.INCUMBENT
        )
        assertTrue(future.validateLinearQuadraticResult() is Failed)

        val invalid = future.copy(
            schemaVersion = "2.0",
            fingerprints = mapOf("model" to "m"),
            fingerprintSchemas = mapOf("model" to "1.0"),
            runId = "run-1",
            attemptId = "attempt-1",
            solutionPresence = RemoteSolutionPresence.NONE
        )
        assertTrue(invalid.validateLinearQuadraticResult() is Failed)
    }

    /**
     * 验证损坏的 resultRef artifact 以结构化协议错误返回。
     * Verifies a corrupted resultRef artifact returns a structured protocol error.
     */
    @Test
    fun remoteLinearSolveRejectsCorruptedResultArtifact() = runBlocking {
        val resultRef = ObjectRef.of(path = "results/corrupted")
        val storage = RecordingObjectStoragePort()
        storage.objects[resultRef.path] = "not-json".encodeToByteArray()
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64.one,
                    gap = Flt64.zero,
                    elapsed = Duration.ZERO,
                    solutionPresence = RemoteSolutionPresence.INCUMBENT
                )
            ),
            finalResult = SolveResult(
                feasible = true,
                optimal = false,
                objectiveValue = Flt64.one,
                gap = Flt64.zero,
                elapsed = Duration.ZERO,
                resultRef = resultRef,
                problemStatus = RemoteProblemStatus.FEASIBLE,
                solutionPresence = RemoteSolutionPresence.INCUMBENT
            )
        )
        val solver = RemoteLinearSolver(
            delegate = StubLinearSolver(),
            executionPort = port,
            resultStoragePort = storage
        )

        val result = solver.solveReport(emptyLinearModel())

        assertTrue(result is Failed)
        assertEquals(ErrorCode.ORSolutionInvalid, (result as Failed).error.code)
    }

    @Test
    fun solveStartsAndBuildsFallbackResult() = runBlocking {
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = false,
                    feasible = true,
                    objectiveValue = Flt64(10.0),
                    gap = Flt64(0.5),
                    elapsed = 10.milliseconds
                ),
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64(8.0),
                    gap = Flt64.zero,
                    elapsed = 20.milliseconds,
                    message = "done",
                    solutionPresence = RemoteSolutionPresence.OPTIMAL,
                    proofStatus = RemoteProofStatus.CLAIMED
                )
            ),
            checkpoints = mutableListOf(
                ObjectRef.of(path = "checkpoints/1"),
                ObjectRef.of(path = "checkpoints/2")
            )
        )
        val client = RemoteSolverClient(port)

        val result = client.solve(
            payload = payload(),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-1"),
            quantum = 100.milliseconds,
            maxRounds = UInt64(4)
        )

        assertEquals(1, port.startCalls)
        assertEquals(0, port.resumeCalls)
        assertEquals(listOf(100.milliseconds, 100.milliseconds), port.quantum)
        assertEquals(1, port.stopCalls)
        val value = (result as Ok).value
        assertEquals(true, value.feasible)
        assertEquals(true, value.optimal)
        assertEquals(Flt64(8.0), value.objectiveValue)
        assertEquals(30.milliseconds, value.elapsed)
        assertEquals(ObjectRef.of(path = "checkpoints/2"), value.checkpointRef)
        assertEquals("done", value.message)
    }

    @Test
    fun solveResumesWhenSnapshotExists() = runBlocking {
        val snapshotRef = ObjectRef.of(path = "checkpoints/origin")
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64.one,
                    gap = Flt64.zero,
                    elapsed = 5.milliseconds
                )
            )
        )
        val client = RemoteSolverClient(port)

        val result = client.solve(
            payload = payload(snapshotRef = snapshotRef),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-1"),
            quantum = 100.milliseconds,
            maxRounds = UInt64.one,
            exportCheckpointEachRound = false
        )

        assertEquals(0, port.startCalls)
        assertEquals(1, port.resumeCalls)
        assertEquals(snapshotRef, port.resumeCheckpoint)
        assertEquals(1, port.stopCalls)
        assertEquals(snapshotRef, (result as Ok).value.checkpointRef)
    }

    /** 验证超过最大轮次时求解失败并停止 / Verify solve fails and stops when max rounds exceeded */
    @Test
    fun solveFailsAndStopsWhenMaxRoundsExceeded() {
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = false,
                    feasible = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = 10.milliseconds
                )
            )
        )
        val client = RemoteSolverClient(port)

        val result = runBlocking {
            client.solve(
                payload = payload(),
                taskId = TaskId.of("task-1"),
                sliceId = SliceId.of("slice-1"),
                nodeId = NodeId.of("node-1"),
                tenantId = TenantId.of("tenant-1"),
                quantum = 100.milliseconds,
                maxRounds = UInt64.one
            )
        }

        assertTrue(result.failed)
        assertEquals(1, port.stopCalls)
    }

    @Test
    fun cancellationDuringRemoteSliceStopsOnceAndReturnsCancelled() = runBlocking {
        val reachedAwait = CompletableDeferred<Unit>()
        val releaseAwait = CompletableDeferred<Unit>()
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = false,
                    feasible = false,
                    objectiveValue = null,
                    gap = null,
                    elapsed = 10.milliseconds
                )
            ),
            awaitGate = releaseAwait,
            onAwait = { reachedAwait.complete(Unit) }
        )
        val client = RemoteSolverClient(port)
        val handle = SolveHandle.create()
        val pending = async {
            client.solve(
                payload = payload(),
                taskId = TaskId.of("task-1"),
                sliceId = SliceId.of("slice-1"),
                nodeId = NodeId.of("node-1"),
                tenantId = TenantId.of("tenant-1"),
                quantum = 100.milliseconds,
                maxRounds = UInt64(2),
                cancellationToken = handle.token
            )
        }

        reachedAwait.await()
        assertTrue(handle.cancel(CancellationSource.Caller, "caller cancelled").ok)
        assertTrue(handle.cancel(CancellationSource.Remote, "duplicate cancellation").ok)
        assertEquals(1, port.stopCalls)

        releaseAwait.complete(Unit)
        val result = pending.await()
        assertTrue(result is Ok)
        assertEquals(RemoteTerminationReason.CANCELLED, (result as Ok).value.terminationReason)
        assertEquals(1, port.stopCalls)
    }

    /**
     * 验证远程切片失败时仍停止执行并释放取消监听。
     * Verifies a remote slice failure still stops execution and releases the cancellation listener.
     */
    @Test
    fun solveStopsAndCleansCancellationListenerAfterSliceFailure() = runBlocking {
        val handle = SolveHandle.create()
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(),
            awaitFailure = true
        )
        val client = RemoteSolverClient(port)

        val result = client.solve(
            payload = payload(),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-1"),
            quantum = 100.milliseconds,
            maxRounds = UInt64.one,
            exportCheckpointEachRound = false,
            cancellationToken = handle.token
        )

        assertTrue(result is Failed)
        assertEquals(1, port.stopCalls)
        assertTrue(handle.cancel(CancellationSource.Caller, "after failure").ok)
        assertEquals(1, port.stopCalls)
    }

    @Test
    fun remoteLinearSolverNormalizesPayload() = runBlocking {
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64.one,
                    gap = Flt64.zero,
                    elapsed = 5.milliseconds
                )
            )
        )
        val solver = RemoteLinearSolver(
            delegate = StubLinearSolver(),
            executionPort = port
        )

        solver.solveRemote(
            payload = payload(taskMeta = TaskMeta(timeLimit = 321.milliseconds)),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-1")
        )

        assertEquals(TargetTypeName.of("linear"), port.startedPayload?.taskMeta?.targetType)
        assertEquals(listOf(4000.milliseconds), port.quantum)
    }

    @Test
    fun remoteQuadraticSolverNormalizesPayload() = runBlocking {
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64.one,
                    gap = Flt64.zero,
                    elapsed = 5.milliseconds
                )
            )
        )
        val solver = RemoteQuadraticSolver(
            delegate = StubQuadraticSolver(),
            executionPort = port
        )

        solver.solveRemote(
            payload = payload(taskMeta = TaskMeta(timeLimit = 654.milliseconds)),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-1")
        )

        assertEquals(TargetTypeName.of("quadratic"), port.startedPayload?.taskMeta?.targetType)
        assertEquals(listOf(4000.milliseconds), port.quantum)
    }

    @Test
    fun remoteLinearSolverPropagatesIdentityFailureWithoutCallingExecutionPort() = runBlocking {
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64.zero,
                    gap = Flt64.zero,
                    elapsed = 1.milliseconds
                )
            )
        )
        val solver = RemoteLinearSolver(
            delegate = StubLinearSolver(),
            executionPort = port
        )
        val invalidModel = emptyLinearModel().copy(
            identityValidation = Failed(ErrorCode.IllegalArgument, "invalid identity")
        )

        val result = solver.solveReport(invalidModel)

        assertTrue(result is Failed)
        assertEquals(0, port.startCalls)
    }

    @Test
    fun remoteLinearSolverReturnsSerializerFailureForMalformedIdentity() = runBlocking {
        val port = RecordingExecutionPort(sliceResults = mutableListOf())
        val solver = RemoteLinearSolver(
            delegate = StubLinearSolver(),
            executionPort = port
        )
        val malformedModel = emptyLinearModel().copy(
            objective = LinearObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList(),
                id = ObjectiveId("objective:malformed-remote-linear"),
                identityScope = ModelElementScope.Stable
            ),
            identityValidation = ok
        )

        val result = solver.solveReport(malformedModel)

        assertTrue(result is Failed)
        assertEquals(0, port.startCalls)
    }

    @Test
    fun remoteQuadraticSolverPropagatesIdentityFatalWithoutCallingExecutionPort() = runBlocking {
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64.zero,
                    gap = Flt64.zero,
                    elapsed = 1.milliseconds
                )
            )
        )
        val solver = RemoteQuadraticSolver(
            delegate = StubQuadraticSolver(),
            executionPort = port
        )
        val invalidModel = emptyQuadraticModel().copy(
            identityValidation = Fatal(Err(ErrorCode.OREngineSolvingException))
        )

        val result = solver.solveReport(invalidModel)

        assertTrue(result is Fatal)
        assertEquals(0, port.startCalls)
    }

    @Test
    fun remoteQuadraticSolverReturnsSerializerFailureForMalformedIdentity() = runBlocking {
        val port = RecordingExecutionPort(sliceResults = mutableListOf())
        val solver = RemoteQuadraticSolver(
            delegate = StubQuadraticSolver(),
            executionPort = port
        )
        val malformedModel = emptyQuadraticModel().copy(
            objective = QuadraticObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList(),
                id = ObjectiveId("objective:malformed-remote-quadratic"),
                identityScope = ModelElementScope.Stable
            ),
            identityValidation = ok
        )

        val result = solver.solveReport(malformedModel)

        assertTrue(result is Failed)
        assertEquals(0, port.startCalls)
    }

    @Test
    fun solveUsesFinalResultWhenAvailable() = runBlocking {
        val finalResult = SolveResult(
            feasible = true,
            optimal = true,
            objectiveValue = Flt64(3.0),
            gap = Flt64.zero,
            elapsed = 999.milliseconds,
            resultRef = ObjectRef.of(path = "results/final")
        )
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64.one,
                    gap = Flt64.zero,
                    elapsed = 5.milliseconds
                )
            ),
            finalResult = finalResult
        )
        val client = RemoteSolverClient(port)

        val result = client.solve(
            payload = payload(),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-1"),
            quantum = 100.milliseconds
        )

        assertSame(finalResult, (result as Ok).value)
        assertNotNull((result as Ok).value.resultRef)
    }

    @Test
    fun solveDoesNotLetStopFailureCoverCompletedResult() = runBlocking {
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64(5.0),
                    gap = Flt64.zero,
                    elapsed = 5.milliseconds
                )
            ),
            stopFailure = IllegalStateException("stop failed")
        )
        val client = RemoteSolverClient(port)

        val result = client.solve(
            payload = payload(),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            tenantId = TenantId.of("tenant-1"),
            quantum = 100.milliseconds
        )

        assertEquals(Flt64(5.0), (result as Ok).value.objectiveValue)
        assertEquals(1, port.stopCalls)
    }

    @Test
    fun remoteLinearSolverInvokeUsesRemoteExecution() = runBlocking {
        val resultRef = ObjectRef.of(path = "results/empty")
        val storage = RecordingObjectStoragePort()
        storage.objects[resultRef.path] = json.encodeToString(
            SerializedSolution(
                feasible = true,
                optimal = true,
                objectiveValue = Flt64.zero,
                gap = Flt64.zero,
                variableValues = emptyList(),
                elapsed = 7.milliseconds,
                solverStatus = "OPTIMAL"
            )
        ).encodeToByteArray()
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64.zero,
                    gap = Flt64.zero,
                    elapsed = 7.milliseconds
                )
            ),
            finalResult = SolveResult(
                feasible = true,
                optimal = true,
                objectiveValue = Flt64.zero,
                gap = Flt64.zero,
                elapsed = 7.milliseconds,
                resultRef = resultRef
            )
        )
        val solver = RemoteLinearSolver(
            delegate = StubLinearSolver(),
            executionPort = port,
            resultStoragePort = storage,
            runtimeConfig = RemoteSolverRuntimeConfig(
                tenantId = TenantId.of("tenant-1"),
                nodeId = NodeId.of("node-1"),
                taskIdProvider = { TaskId.of("task-1") },
                sliceIdProvider = { SliceId.of("slice-1") }
            )
        )

        val result = solver.invoke(emptyLinearModel())

        check(result is Ok)
        assertEquals(Flt64.zero, result.value.solution?.objective)
        assertEquals(emptyList<Flt64>(), result.value.values)
        assertEquals(1, port.startCalls)
    }

    @Test
    fun remoteLinearSolverInvokeCanMapEmptyModelWithoutResultObject() = runBlocking {
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64.zero,
                    gap = Flt64.zero,
                    elapsed = 7.milliseconds
                )
            ),
            finalResult = SolveResult(
                feasible = true,
                optimal = false,
                objectiveValue = Flt64.zero,
                gap = Flt64.zero,
                elapsed = 7.milliseconds
            )
        )
        val solver = RemoteLinearSolver(
            delegate = StubLinearSolver(),
            executionPort = port,
            runtimeConfig = RemoteSolverRuntimeConfig(
                tenantId = TenantId.of("tenant-1"),
                nodeId = NodeId.of("node-1"),
                taskIdProvider = { TaskId.of("task-1") },
                sliceIdProvider = { SliceId.of("slice-1") }
            )
        )

        val result = solver.invoke(emptyLinearModel())

        check(result is Ok)
        assertEquals(Flt64.zero, result.value.solution?.objective)
        assertEquals(emptyList<Flt64>(), result.value.values)
        assertEquals(SolverStatus.Feasible, result.value.toSolverStatus())
    }

    @Test
    fun remoteLinearSolveReportRetainsAuditAndTerminalFields() = runBlocking {
        val resultRef = ObjectRef.of(path = "results/report")
        val storage = RecordingObjectStoragePort()
        val diagnostics = mapOf(
            "warning.0.code" to "remote-warning",
            "warning.0.category" to "Backend",
            "warning.0.message" to "backend warning",
            "error.0.code" to "remote-error",
            "error.0.category" to "Protocol",
            "error.0.message" to "protocol error"
        )
        storage.objects[resultRef.path] = json.encodeToString(
            SerializedSolution(
                feasible = true,
                optimal = true,
                objectiveValue = Flt64(8.0),
                gap = Flt64(0.25),
                variableValues = emptyList(),
                elapsed = 7.milliseconds,
                solverStatus = "OPTIMAL",
                schemaVersion = "2.0",
                problemStatus = RemoteProblemStatus.FEASIBLE,
                solutionPresence = RemoteSolutionPresence.OPTIMAL,
                proofStatus = RemoteProofStatus.VERIFIED,
                terminationReason = RemoteTerminationReason.COMPLETED,
                provenance = mapOf(
                    "solverId" to "scip",
                    "backend" to "scip",
                    "pluginVersion" to "plugin-2",
                    "nativeVersion" to "native-9",
                    "threads" to "4",
                    "randomSeed" to "17",
                    "deterministic" to "true"
                ),
                fingerprints = mapOf(
                    "model" to "model-digest",
                    "solver" to "solver-digest"
                ),
                fingerprintSchemas = mapOf(
                    "model" to "1.0",
                    "solver" to "2.0"
                ),
                statistics = mapOf(
                    "bestBound" to "7.5",
                    "gap" to "0.25",
                    "iterations" to "12",
                    "nodes" to "34"
                ),
                diagnostics = diagnostics,
                runId = "run-1",
                attemptId = "attempt-1",
                artifactDigest = "artifact-linear-sha"
            )
        ).encodeToByteArray()
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64(8.0),
                    gap = Flt64(0.25),
                    elapsed = 7.milliseconds,
                    solutionPresence = RemoteSolutionPresence.OPTIMAL,
                    proofStatus = RemoteProofStatus.VERIFIED
                )
            ),
            finalResult = SolveResult(
                feasible = true,
                optimal = true,
                objectiveValue = Flt64(8.0),
                gap = Flt64(0.25),
                elapsed = 7.milliseconds,
                resultRef = resultRef,
                schemaVersion = "2.0",
                problemStatus = RemoteProblemStatus.FEASIBLE,
                solutionPresence = RemoteSolutionPresence.OPTIMAL,
                proofStatus = RemoteProofStatus.VERIFIED,
                provenance = mapOf(
                    "solverId" to "scip",
                    "backend" to "scip",
                    "pluginVersion" to "plugin-2",
                    "nativeVersion" to "native-9",
                    "threads" to "4",
                    "randomSeed" to "17",
                    "deterministic" to "true"
                ),
                fingerprints = mapOf(
                    "model" to "model-digest",
                    "solver" to "solver-digest"
                ),
                fingerprintSchemas = mapOf(
                    "model" to "1.0",
                    "solver" to "2.0"
                ),
                statistics = mapOf(
                    "bestBound" to "7.5",
                    "gap" to "0.25",
                    "iterations" to "12",
                    "nodes" to "34"
                ),
                diagnostics = diagnostics,
                runId = "run-1",
                attemptId = "attempt-1",
                artifactDigest = "artifact-linear-sha"
            )
        )
        val solver = RemoteLinearSolver(
            delegate = StubLinearSolver(),
            executionPort = port,
            resultStoragePort = storage,
            runtimeConfig = RemoteSolverRuntimeConfig(
                tenantId = TenantId.of("tenant-1"),
                nodeId = NodeId.of("node-1"),
                taskIdProvider = { TaskId.of("task-1") },
                sliceIdProvider = { SliceId.of("slice-1") }
            )
        )

        val result = solver.solveReport(emptyLinearModel())

        check(result is Ok)
        assertEquals(SolveRunId("run-1"), result.value.runId)
        assertEquals(ProofStatus.Verified, result.value.proof.status)
        assertEquals(Flt64(7.5), result.value.statistics.bestBound)
        assertEquals(Flt64(0.25), result.value.statistics.gap)
        assertEquals(12uL, result.value.statistics.iterations)
        assertEquals(34uL, result.value.statistics.nodes)
        assertEquals("2.0", result.value.fingerprints.solver?.schemaVersion)
        assertEquals("plugin-2", result.value.provenance?.descriptor?.pluginVersion)
        assertEquals(4, result.value.provenance?.threadCount)
        assertEquals(17L, result.value.provenance?.randomSeed)
        assertEquals(1, result.value.diagnostics.warnings.size)
        assertEquals(1, result.value.diagnostics.errors.size)
    }

    @Test
    fun remoteQuadraticSolveReportRetainsAuditAndTerminalFields() = runBlocking {
        val provenance = mapOf(
            "solverId" to "scip",
            "backend" to "scip",
            "pluginVersion" to "plugin-2",
            "nativeVersion" to "native-9",
            "threads" to "4",
            "randomSeed" to "17",
            "deterministic" to "true"
        )
        val fingerprints = mapOf(
            "model" to "quadratic-model-digest",
            "solver" to "quadratic-solver-digest"
        )
        val fingerprintSchemas = mapOf(
            "model" to "1.0",
            "solver" to "2.0"
        )
        val statistics = mapOf(
            "bestBound" to "7.5",
            "gap" to "0.25",
            "iterations" to "12",
            "nodes" to "34"
        )
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64(8.0),
                    gap = Flt64(0.25),
                    elapsed = 7.milliseconds,
                    solutionPresence = RemoteSolutionPresence.OPTIMAL,
                    proofStatus = RemoteProofStatus.VERIFIED
                )
            ),
            finalResult = SolveResult(
                feasible = true,
                optimal = true,
                objectiveValue = Flt64(8.0),
                gap = Flt64(0.25),
                elapsed = 7.milliseconds,
                schemaVersion = "2.0",
                problemStatus = RemoteProblemStatus.FEASIBLE,
                solutionPresence = RemoteSolutionPresence.OPTIMAL,
                proofStatus = RemoteProofStatus.VERIFIED,
                terminationReason = RemoteTerminationReason.COMPLETED,
                provenance = provenance,
                fingerprints = fingerprints,
                fingerprintSchemas = fingerprintSchemas,
                statistics = statistics,
                runId = "run-1",
                attemptId = "attempt-1"
            )
        )
        val solver = RemoteQuadraticSolver(
            delegate = StubQuadraticSolver(),
            executionPort = port,
            runtimeConfig = RemoteSolverRuntimeConfig(
                tenantId = TenantId.of("tenant-1"),
                nodeId = NodeId.of("node-1"),
                taskIdProvider = { TaskId.of("task-1") },
                sliceIdProvider = { SliceId.of("slice-1") }
            )
        )

        val result = solver.solveReport(emptyQuadraticModel())

        check(result is Ok)
        assertEquals(SolveRunId("run-1"), result.value.runId)
        assertEquals(ProblemStatus.Feasible, result.value.problemStatus)
        assertEquals(TerminationReason.Completed, result.value.terminationReason)
        assertEquals(SolutionPresence.Optimal, result.value.solutionPresence)
        assertEquals(ProofStatus.Verified, result.value.proof.status)
        assertEquals(Flt64(8.0), result.value.solution?.objective)
        assertEquals(Flt64(7.5), result.value.statistics.bestBound)
        assertEquals(Flt64(0.25), result.value.statistics.gap)
        assertEquals(12uL, result.value.statistics.iterations)
        assertEquals(34uL, result.value.statistics.nodes)
        assertEquals("2.0", result.value.fingerprints.solver?.schemaVersion)
        assertEquals("plugin-2", result.value.provenance?.descriptor?.pluginVersion)
        assertEquals(4, result.value.provenance?.threadCount)
        assertEquals(17L, result.value.provenance?.randomSeed)
        assertEquals(setOf(SolverModelType.QP, SolverModelType.QCP), result.value.provenance?.descriptor?.capabilities?.modelTypes)
    }

    @Test
    fun remoteLinearAndQuadraticAdaptersPreserveMissingGapAndBestBoundAsNull() = runBlocking {
        val linearPort = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64(3.0),
                    gap = null,
                    elapsed = 4.milliseconds,
                    terminationReason = RemoteTerminationReason.TIME_LIMIT,
                    solutionPresence = RemoteSolutionPresence.INCUMBENT
                )
            ),
            finalResult = SolveResult(
                feasible = true,
                optimal = false,
                objectiveValue = Flt64(3.0),
                gap = null,
                elapsed = 4.milliseconds,
                problemStatus = RemoteProblemStatus.FEASIBLE,
                terminationReason = RemoteTerminationReason.TIME_LIMIT,
                solutionPresence = RemoteSolutionPresence.INCUMBENT
            )
        )
        val linear = RemoteLinearSolver(
            delegate = StubLinearSolver(),
            executionPort = linearPort
        )
        val linearResult = linear.solveReport(emptyLinearModel())
        assertTrue(linearResult is Ok)
        assertNull((linearResult as Ok).value.statistics.gap)
        assertNull(linearResult.value.statistics.bestBound)

        val quadraticPort = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64(3.0),
                    gap = null,
                    elapsed = 4.milliseconds,
                    terminationReason = RemoteTerminationReason.TIME_LIMIT,
                    solutionPresence = RemoteSolutionPresence.INCUMBENT
                )
            ),
            finalResult = SolveResult(
                feasible = true,
                optimal = false,
                objectiveValue = Flt64(3.0),
                gap = null,
                elapsed = 4.milliseconds,
                problemStatus = RemoteProblemStatus.FEASIBLE,
                terminationReason = RemoteTerminationReason.TIME_LIMIT,
                solutionPresence = RemoteSolutionPresence.INCUMBENT
            )
        )
        val quadratic = RemoteQuadraticSolver(
            delegate = StubQuadraticSolver(),
            executionPort = quadraticPort
        )
        val quadraticResult = quadratic.solveReport(emptyQuadraticModel())
        assertTrue(quadraticResult is Ok)
        assertNull((quadraticResult as Ok).value.statistics.gap)
        assertNull(quadraticResult.value.statistics.bestBound)
    }

    /**
     * 验证严格 v2 artifact 的目标冲突不会被客户端静默接受。
     * Verifies that a strict v2 artifact objective conflict is rejected by the client.
     */
    @Test
    fun remoteLinearSolveReportRejectsStrictArtifactObjectiveMismatch() = runBlocking {
        val resultRef = ObjectRef.of(path = "results/conflicting-report")
        val storage = RecordingObjectStoragePort()
        storage.objects[resultRef.path] = json.encodeToString(
            SerializedSolution(
                feasible = true,
                optimal = false,
                objectiveValue = Flt64.one,
                gap = Flt64.zero,
                elapsed = 7.milliseconds,
                schemaVersion = "2.0",
                problemStatus = RemoteProblemStatus.FEASIBLE,
                solutionPresence = RemoteSolutionPresence.INCUMBENT,
                proofStatus = RemoteProofStatus.CLAIMED,
                terminationReason = RemoteTerminationReason.COMPLETED,
                runId = "run-1",
                attemptId = "attempt-1"
            )
        ).encodeToByteArray()
        val port = RecordingExecutionPort(
            sliceResults = mutableListOf(
                SliceResult(
                    sliceId = SliceId.of("slice-1"),
                    completed = true,
                    feasible = true,
                    objectiveValue = Flt64(2.0),
                    gap = Flt64.zero,
                    elapsed = 7.milliseconds,
                    solutionPresence = RemoteSolutionPresence.INCUMBENT,
                    proofStatus = RemoteProofStatus.CLAIMED
                )
            ),
            finalResult = SolveResult(
                feasible = true,
                optimal = false,
                objectiveValue = Flt64(2.0),
                gap = Flt64.zero,
                elapsed = 7.milliseconds,
                resultRef = resultRef,
                schemaVersion = "2.0",
                problemStatus = RemoteProblemStatus.FEASIBLE,
                solutionPresence = RemoteSolutionPresence.INCUMBENT,
                proofStatus = RemoteProofStatus.CLAIMED,
                terminationReason = RemoteTerminationReason.COMPLETED,
                runId = "run-1",
                attemptId = "attempt-1"
            )
        )
        val solver = RemoteLinearSolver(
            delegate = StubLinearSolver(),
            executionPort = port,
            resultStoragePort = storage,
            runtimeConfig = RemoteSolverRuntimeConfig(
                tenantId = TenantId.of("tenant-1"),
                nodeId = NodeId.of("node-1"),
                taskIdProvider = { TaskId.of("task-1") },
                sliceIdProvider = { SliceId.of("slice-1") }
            )
        )

        val result = solver.solveReport(emptyLinearModel())

        assertTrue(result is Failed)
        assertEquals(ErrorCode.ORSolutionInvalid, (result as Failed).error.code)
    }

    private fun payload(
        taskMeta: TaskMeta = TaskMeta(),
        snapshotRef: ObjectRef? = null
    ): SolvePayload {
        return SolvePayload(
            modelData = ModelData.linear(SerializedLinearModel.empty()),
            snapshotRef = snapshotRef,
            taskMeta = taskMeta
        )
    }

    private fun emptyLinearModel(): LinearTriadModel {
        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = emptyList(),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "empty"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList()
            )
        )
    }

    private fun emptyQuadraticModel(): QuadraticTetradModel {
        return QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = emptyList(),
                constraints = QuadraticConstraintBatch(
                    sparseLhs = SparseQuadraticMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "empty-quadratic"
            ),
            tokensInSolver = emptyList(),
            objective = QuadraticObjective(ObjectCategory.Minimum, emptyList())
        )
    }

    private class RecordingExecutionPort(
        private val sliceResults: MutableList<SliceResult>,
        private val finalResult: SolveResult? = null,
        private val checkpoints: MutableList<ObjectRef?> = mutableListOf(),
        private val stopFailure: RuntimeException? = null,
        private val awaitGate: CompletableDeferred<Unit>? = null,
        private val onAwait: (() -> Unit)? = null,
        private val awaitFailure: Boolean = false
    ) : SolverExecutionPort {
        var startCalls = 0
        var resumeCalls = 0
        var stopCalls = 0
        var startedPayload: SolvePayload? = null
        var resumeCheckpoint: ObjectRef? = null
        val quantum = mutableListOf<Duration>()
        private val handle = ExecutionHandle(
            handleId = HandleId.of("handle-1"),
            taskId = TaskId.of("task-1"),
            sliceId = SliceId.of("slice-1"),
            nodeId = NodeId.of("node-1"),
            startedAt = Instant.fromEpochMilliseconds(0L)
        )

        override suspend fun start(
            payload: SolvePayload,
            taskId: TaskId,
            sliceId: SliceId,
            nodeId: NodeId,
            tenantId: TenantId
        ): Ret<ExecutionHandle> {
            startCalls += 1
            startedPayload = payload
            return Ok(handle)
        }

        override suspend fun resume(
            payload: SolvePayload,
            checkpoint: ObjectRef,
            taskId: TaskId,
            sliceId: SliceId,
            nodeId: NodeId,
            tenantId: TenantId
        ): Ret<ExecutionHandle> {
            resumeCalls += 1
            startedPayload = payload
            resumeCheckpoint = checkpoint
            return Ok(handle)
        }

        override suspend fun awaitSliceEnd(handle: ExecutionHandle, quantum: Duration): Ret<SliceResult> {
            this.quantum.add(quantum)
            onAwait?.invoke()
            awaitGate?.await()
            if (awaitFailure) {
                return Failed(ErrorCode.ApplicationFailed, "await failed")
            }
            return Ok(sliceResults.removeFirst())
        }

        override suspend fun exportCheckpoint(handle: ExecutionHandle): Ret<ObjectRef?> {
            return Ok(checkpoints.removeFirstOrNull())
        }

        override suspend fun fetchFinalResult(handle: ExecutionHandle): Ret<SolveResult?> {
            return Ok(finalResult)
        }

        override suspend fun stop(handle: ExecutionHandle): Ret<Boolean> {
            stopCalls += 1
            return stopFailure
                ?.let { Failed(ErrorCode.ApplicationFailed, it.message ?: "stop failed") }
                ?: Ok(true)
        }
    }

    private class StubLinearSolver : LinearSolver {
        override val name: String = "stub-linear"
        override val config: CoreSolverConfig = CoreSolverConfig()

        override suspend fun invoke(
            model: LinearTriadModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            throw UnsupportedOperationException("Local solve is not used in remote wrapper tests.")
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            throw UnsupportedOperationException("Local solve is not used in remote wrapper tests.")
        }
    }

    private class StubQuadraticSolver : QuadraticSolver {
        override val name: String = "stub-quadratic"
        override val config: CoreSolverConfig = CoreSolverConfig()

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            throw UnsupportedOperationException("Local solve is not used in remote wrapper tests.")
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            throw UnsupportedOperationException("Local solve is not used in remote wrapper tests.")
        }
    }

    private class RecordingObjectStoragePort : ObjectStoragePort {
        val objects = mutableMapOf<ObjectPath, ByteArray>()

        override suspend fun put(
            path: ObjectPath,
            bytes: ByteArray,
            metadata: Map<String, String>
        ): ObjectRef {
            objects[path] = bytes
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
