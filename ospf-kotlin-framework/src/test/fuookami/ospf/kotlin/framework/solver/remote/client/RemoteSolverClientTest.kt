@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.solver.remote.client

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.solver.LinearSolver
import fuookami.ospf.kotlin.core.solver.QuadraticSolver
import fuookami.ospf.kotlin.core.solver.config.SolverConfig as CoreSolverConfig
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.SolverExecutionPort
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.Ret

class RemoteSolverClientTest {
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
        assertEquals(true, result.feasible)
        assertEquals(true, result.optimal)
        assertEquals(Flt64(8.0), result.objectiveValue)
        assertEquals(30.milliseconds, result.elapsed)
        assertEquals(ObjectRef.of(path = "checkpoints/2"), result.checkpointRef)
        assertEquals("done", result.message)
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
        assertEquals(snapshotRef, result.checkpointRef)
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
        assertEquals(listOf(321.milliseconds), port.quantum)
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
        assertEquals(listOf(654.milliseconds), port.quantum)
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

        assertSame(finalResult, result)
        assertNotNull(result.resultRef)
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

    private class RecordingExecutionPort(
        private val sliceResults: MutableList<SliceResult>,
        private val finalResult: SolveResult? = null,
        private val checkpoints: MutableList<ObjectRef?> = mutableListOf()
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
}
