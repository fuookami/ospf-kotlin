@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.solver.remote.client

import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig as CoreSolverConfig
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.*

class RemoteSolverClientTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
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
                    message = "done"
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

    @Test
    fun solveThrowsAndStopsWhenMaxRoundsExceeded() {
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

        val error = assertThrows(RemoteSolverException::class.java) {
            runBlocking {
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
        }

        assertEquals(RemoteSolverErrorCode.REMOTE_SOLVE_NOT_COMPLETED_WITHIN_MAX_ROUNDS, error.code)
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
        assertEquals(Flt64.zero, result.value.obj)
        assertEquals(emptyList<Flt64>(), result.value.solution)
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
                optimal = true,
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
        assertEquals(Flt64.zero, result.value.obj)
        assertEquals(emptyList<Flt64>(), result.value.solution)
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

    private class RecordingExecutionPort(
        private val sliceResults: MutableList<SliceResult>,
        private val finalResult: SolveResult? = null,
        private val checkpoints: MutableList<ObjectRef?> = mutableListOf(),
        private val stopFailure: RuntimeException? = null
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
        ): ExecutionHandle {
            startCalls += 1
            startedPayload = payload
            return handle
        }

        override suspend fun resume(
            payload: SolvePayload,
            checkpoint: ObjectRef,
            taskId: TaskId,
            sliceId: SliceId,
            nodeId: NodeId,
            tenantId: TenantId
        ): ExecutionHandle {
            resumeCalls += 1
            startedPayload = payload
            resumeCheckpoint = checkpoint
            return handle
        }

        override suspend fun awaitSliceEnd(handle: ExecutionHandle, quantum: Duration): SliceResult {
            this.quantum.add(quantum)
            return sliceResults.removeFirst()
        }

        override suspend fun exportCheckpoint(handle: ExecutionHandle): ObjectRef? {
            return checkpoints.removeFirstOrNull()
        }

        override suspend fun fetchFinalResult(handle: ExecutionHandle): SolveResult? {
            return finalResult
        }

        override suspend fun stop(handle: ExecutionHandle): Boolean {
            stopCalls += 1
            stopFailure?.let { throw it }
            return true
        }
    }

    private class StubLinearSolver : LinearSolver {
        override val name: String = "stub-linear"
        override val config: CoreSolverConfig = CoreSolverConfig()

        override suspend fun invoke(
            model: LinearTriadModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<FeasibleSolverOutput<Flt64>> {
            throw UnsupportedOperationException("Local solve is not used in remote wrapper tests.")
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
            throw UnsupportedOperationException("Local solve is not used in remote wrapper tests.")
        }
    }

    private class StubQuadraticSolver : QuadraticSolver {
        override val name: String = "stub-quadratic"
        override val config: CoreSolverConfig = CoreSolverConfig()

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<FeasibleSolverOutput<Flt64>> {
            throw UnsupportedOperationException("Local solve is not used in remote wrapper tests.")
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
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
