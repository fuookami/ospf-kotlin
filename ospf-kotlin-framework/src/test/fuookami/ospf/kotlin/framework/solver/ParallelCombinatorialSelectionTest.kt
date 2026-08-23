/**
 * 并行组合求解器选择依据测试。 / Parallel combinatorial solver selection tests.
 */
package fuookami.ospf.kotlin.framework.solver

import java.time.Instant
import kotlin.time.TimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.BasicLinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.LinearObjective
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix
import fuookami.ospf.kotlin.core.solver.AbstractLinearSolver
import fuookami.ospf.kotlin.core.solver.progress.SolverProgressContext
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.report.AuditFingerprint
import fuookami.ospf.kotlin.core.solver.report.CancellationToken
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.ProofStatus
import fuookami.ospf.kotlin.core.solver.report.SolveFingerprints
import fuookami.ospf.kotlin.core.solver.report.SolveAttemptId
import fuookami.ospf.kotlin.core.solver.report.SolveAttemptTrace
import fuookami.ospf.kotlin.core.solver.report.SolveProof
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.SolveSelectionReason
import fuookami.ospf.kotlin.core.solver.report.SolveSolution
import fuookami.ospf.kotlin.core.solver.report.SolveHandle
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.core.solver.report.SolverCapabilities
import fuookami.ospf.kotlin.core.solver.report.SolverDescriptor
import fuookami.ospf.kotlin.core.solver.report.SolverModelType
import fuookami.ospf.kotlin.core.solver.report.SolverProvenance
import fuookami.ospf.kotlin.core.solver.report.TerminationReason

/**
 * 并行组合求解器选择依据测试。 / Parallel combinatorial solver selection tests.
 */
class ParallelCombinatorialSelectionTest {
    /**
     * First 模式选择第一个成功 backend。
     * First mode selects the first successful backend.
     */
    @Test
    fun firstModeSelectsFirstSuccessfulAttempt() = runBlocking {
        val solver = ParallelCombinatorialLinearSolver(
            listOf(
                ObjectiveStubLinearSolver("first-backend", Flt64(3.0)),
                ObjectiveStubLinearSolver("second-backend", Flt64(1.0))
            ),
            mode = ParallelCombinatorialMode.First
        )

        val result = solver.solveCombinatorialReport(model(ObjectCategory.Minimum))
        val report = when (val unwrapped = result) { is Ok -> unwrapped.value; else -> error("Expected Ok but was $unwrapped") }

        assertEquals("parallel-0", report.selectedAttemptId?.value)
        assertEquals(SolveSelectionReason.FirstFeasible, report.selectionReason)
        assertEquals(Flt64(3.0), report.finalReport?.solution?.objective)
        assertEquals(2, report.attempts.size)
        assertEquals("parallel-0", report.attempts[0].attemptId.value)
        assertEquals("first-backend", report.attempts[0].backendId)
        assertEquals("parallel-1", report.attempts[1].attemptId.value)
        assertEquals("second-backend", report.attempts[1].backendId)
        assertTrue(report.attempts.all { it.elapsed != null })
        assertTrue(report.attempts.all { it.parentAttemptId == SolveAttemptId("parallel-combinatorial") })
        assertNotNull(report.attempts[0].report?.provenance)
        assertEquals("stub-backend", report.attempts[0].report?.provenance?.descriptor?.backendName)
        assertEquals("stub-model-fingerprint", report.attempts[0].report?.fingerprints?.model?.value)
        assertEquals(report.selectedAttemptId, report.attempts[0].attemptId)
        assertEquals(report.finalReport, report.attempts[0].report)
    }

    /**
     * Best 模式在最小化方向上选择最小目标。
     * Best mode selects the minimum objective for minimization.
     */
    @Test
    fun bestModeSelectsMinimumForMinimization() = runBlocking {
        val solver = ParallelCombinatorialLinearSolver(
            listOf(
                ObjectiveStubLinearSolver("first-backend", Flt64(3.0)),
                ObjectiveStubLinearSolver("second-backend", Flt64(1.0))
            ),
            mode = ParallelCombinatorialMode.Best
        )

        val result = solver.solveCombinatorialReport(model(ObjectCategory.Minimum))
        val report = when (val unwrapped = result) { is Ok -> unwrapped.value; else -> error("Expected Ok but was $unwrapped") }

        assertEquals("parallel-1", report.selectedAttemptId?.value)
        assertEquals(SolveSelectionReason.BestObjective, report.selectionReason)
        assertEquals(Flt64(1.0), report.finalReport?.solution?.objective)
    }

    /**
     * Best 模式在最大化方向上选择最大目标。
     * Best mode selects the maximum objective for maximization.
     */
    @Test
    fun bestModeSelectsMaximumForMaximization() = runBlocking {
        val solver = ParallelCombinatorialLinearSolver(
            listOf(
                ObjectiveStubLinearSolver("first-backend", Flt64(1.0)),
                ObjectiveStubLinearSolver("second-backend", Flt64(3.0))
            ),
            mode = ParallelCombinatorialMode.Best
        )

        val result = solver.solveCombinatorialReport(model(ObjectCategory.Maximum))
        val report = when (val unwrapped = result) { is Ok -> unwrapped.value; else -> error("Expected Ok but was $unwrapped") }

        assertEquals("parallel-1", report.selectedAttemptId?.value)
        assertEquals(SolveSelectionReason.BestObjective, report.selectionReason)
        assertEquals(Flt64(3.0), report.finalReport?.solution?.objective)
    }

    /**
     * 全部 backend 失败时返回空选择依据。
     * No successful attempt yields NoSuccessfulAttempt and a null final report.
     */
    @Test
    fun noSuccessfulAttemptReturnsNullReport() = runBlocking {
        val solver = ParallelCombinatorialLinearSolver(
            listOf(
                FailingStubLinearSolver("first-backend"),
                FailingStubLinearSolver("second-backend")
            ),
            mode = ParallelCombinatorialMode.Best
        )

        val result = solver.solveCombinatorialReport(model(ObjectCategory.Minimum))
        val report = when (val unwrapped = result) { is Ok -> unwrapped.value; else -> error("Expected Ok but was $unwrapped") }

        assertNull(report.selectedAttemptId)
        assertEquals(SolveSelectionReason.NoSuccessfulAttempt, report.selectionReason)
        assertNull(report.finalReport)
        assertTrue(report.attempts.all { it.elapsed != null })
        assertTrue(report.attempts.all { it.parentAttemptId == SolveAttemptId("parallel-combinatorial") })
    }

    @Test
    fun cancelledAttemptRetainsCancellationReasonAndElapsedMetadata() {
        val handle = SolveHandle.create()
        handle.cancel(reason = "test cancellation")
        val trace = SolveAttemptTrace<Flt64>(
            attemptId = SolveAttemptId("attempt-0"),
            backendId = "fixture",
            report = SolveReport(
                problemStatus = ProblemStatus.Unknown,
                terminationReason = TerminationReason.Cancelled,
                solutionPresence = SolutionPresence.None
            )
        ).withAttemptMetadata(
            started = TimeSource.Monotonic.markNow(),
            cancellationRecord = null,
            parentAttemptId = SolveAttemptId("parallel-combinatorial"),
            cancellationRecordAtCompletion = handle.token.record,
            completedAt = Instant.now()
        )

        assertEquals("test cancellation", trace.cancellationReason)
        assertNotNull(trace.elapsed)
        assertEquals(SolveAttemptId("parallel-combinatorial"), trace.parentAttemptId)
    }

    @Test
    fun lateCancellationDoesNotRelabelAlreadyReturnedFailedAttempt() {
        val handle = SolveHandle.create()
        val cancellationRecordAtBackendReturn = handle.token.record
        val completedAt = Instant.now()
        assertNull(cancellationRecordAtBackendReturn)
        assertTrue(handle.cancel(reason = "late cancellation").ok)

        val trace = SolveAttemptTrace<Flt64>(
            attemptId = SolveAttemptId("attempt-1"),
            backendId = "fixture"
        ).withAttemptMetadata(
            started = TimeSource.Monotonic.markNow(),
            cancellationRecord = cancellationRecordAtBackendReturn,
            parentAttemptId = SolveAttemptId("parallel-combinatorial"),
            cancellationRecordAtCompletion = null,
            completedAt = completedAt
        )

        assertNull(trace.cancellationReason)
        assertNotNull(trace.elapsed)
    }

    @Test
    fun cancelledAttemptDoesNotInheritCancellationRequestedAfterBackendCompletion() {
        val handle = SolveHandle.create()
        val completedAt = Instant.now().minusSeconds(1)
        assertTrue(handle.cancel(reason = "other backend cancellation").ok)
        val lateCancellationRecord = handle.token.record
        assertNotNull(lateCancellationRecord)

        val cancelledTrace = SolveAttemptTrace<Flt64>(
            attemptId = SolveAttemptId("attempt-cancelled"),
            backendId = "backend-a",
            report = SolveReport(
                problemStatus = ProblemStatus.Unknown,
                terminationReason = TerminationReason.Cancelled,
                solutionPresence = SolutionPresence.None
            )
        ).withAttemptMetadata(
            started = TimeSource.Monotonic.markNow(),
            cancellationRecord = null,
            parentAttemptId = SolveAttemptId("parallel-combinatorial"),
            cancellationRecordAtCompletion = lateCancellationRecord,
            completedAt = completedAt
        )

        val fatalTrace = SolveAttemptTrace<Flt64>(
            attemptId = SolveAttemptId("attempt-fatal"),
            backendId = "backend-a"
        ).withAttemptMetadata(
            started = TimeSource.Monotonic.markNow(),
            cancellationRecord = null,
            parentAttemptId = SolveAttemptId("parallel-combinatorial"),
            cancellationRecordAtCompletion = lateCancellationRecord,
            completedAt = completedAt
        )

        assertEquals("cancellation requested", cancelledTrace.cancellationReason)
        assertNull(fatalTrace.cancellationReason)
    }

    /**
     * 并行 wrapper 跨 backend 屏障保留 backend 期间产生的取消原因。
     * The parallel wrapper retains the cancellation reason produced during backend solving across a backend barrier.
     */
    @Test
    fun parallelWrapperPreservesCancellationReasonAcrossBackendBarrier() = runBlocking {
        val firstBackendReady = CompletableDeferred<Unit>()
        val solver = ParallelCombinatorialLinearSolver(
            listOf(
                BarrierCancelledLinearSolver(firstBackendReady),
                BarrierWaitingLinearSolver(firstBackendReady)
            )
        )

        val result = solver.solveCombinatorialReport(
            model = model(ObjectCategory.Minimum),
            cancellationToken = SolveHandle.create().token
        )
        val report = when (val unwrapped = result) {
            is Ok -> unwrapped.value
            else -> error("Expected Ok but was $result")
        }

        assertEquals(2, report.attempts.size)
        assertEquals("backend cancellation", report.attempts[0].cancellationReason)
        assertEquals("barrier-cancelled", report.attempts[0].backendId)
    }

    /**
     * 全部 backend 失败时，终态报告必须保留每次 attempt 的原始错误。
     * When every backend fails, the terminal report must retain every attempt's original error.
     */
    @Test
    fun terminalReportRetainsDuplicateErrorsFromAllFailedAttempts() = runBlocking {
        val solver = ParallelCombinatorialLinearSolver(
            listOf(
                FailingStubLinearSolver("first-backend"),
                FailingStubLinearSolver("second-backend")
            )
        )

        val result = solver.solveReport(model(ObjectCategory.Minimum))
        assertTrue(result is Ok)
        val terminal = (result as Ok).value

        assertEquals(ProblemStatus.Unknown, terminal.problemStatus)
        assertEquals(TerminationReason.BackendFailure, terminal.terminationReason)
        assertEquals(2, terminal.diagnostics.errors.size)
        assertEquals(
            listOf(ErrorCode.ORModelInfeasible.toString(), ErrorCode.ORModelInfeasible.toString()),
            terminal.diagnostics.errors.map { it.code }
        )
    }

    private fun model(category: ObjectCategory): LinearTriadModel {
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
                name = "selection-test"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(category, emptyList())
        )
    }

    private class ObjectiveStubLinearSolver(
        override val name: String,
        private val objective: Flt64
    ) : AbstractLinearSolver {
        override suspend fun solveReport(
            model: LinearTriadModelView,
            progressContext: SolverProgressContext?
        ): Ret<SolveReport<Flt64>> {
            return Ok(
                SolveReport(
                    problemStatus = ProblemStatus.Feasible,
                    terminationReason = TerminationReason.Completed,
                    solutionPresence = SolutionPresence.Optimal,
                    solution = SolveSolution(values = emptyList(), objective = objective),
                    proof = SolveProof(ProofStatus.Verified),
                    provenance = SolverProvenance(
                        descriptor = SolverDescriptor(
                            solverId = "stub",
                            backendName = "stub-backend",
                            capabilities = SolverCapabilities(modelTypes = setOf(SolverModelType.LP))
                        ),
                        deterministic = true
                    ),
                    fingerprints = SolveFingerprints(
                        model = AuditFingerprint(
                            schemaVersion = "1.0",
                            algorithm = "sha256",
                            value = "stub-model-fingerprint"
                        )
                    )
                )
            )
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            return Failed(ErrorCode.ORModelInfeasible)
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            return Failed(ErrorCode.ORModelInfeasible)
        }
    }

    private class FailingStubLinearSolver(
        override val name: String
    ) : AbstractLinearSolver {
        override suspend fun solveReport(
            model: LinearTriadModelView,
            progressContext: SolverProgressContext?
        ): Ret<SolveReport<Flt64>> {
            return Failed(ErrorCode.ORModelInfeasible)
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            return Failed(ErrorCode.ORModelInfeasible)
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            return Failed(ErrorCode.ORModelInfeasible)
        }
    }

    private class BarrierCancelledLinearSolver(
        private val firstBackendReady: CompletableDeferred<Unit>
    ) : AbstractLinearSolver {
        override val name: String = "barrier-cancelled"

        private fun report(): SolveReport<Flt64> {
            return SolveReport(
                problemStatus = ProblemStatus.Unknown,
                terminationReason = TerminationReason.Cancelled,
                solutionPresence = SolutionPresence.None
            )
        }

        override suspend fun solveReport(
            model: LinearTriadModelView,
            progressContext: SolverProgressContext?,
            cancellationToken: CancellationToken?
        ): Ret<SolveReport<Flt64>> {
            cancellationToken?.request(reason = "backend cancellation")
            firstBackendReady.complete(Unit)
            return Ok(report())
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            return Ok(report())
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            return Ok(report() to emptyList())
        }
    }

    private class BarrierWaitingLinearSolver(
        private val firstBackendReady: CompletableDeferred<Unit>
    ) : AbstractLinearSolver {
        override val name: String = "barrier-waiting"

        override suspend fun solveReport(
            model: LinearTriadModelView,
            progressContext: SolverProgressContext?,
            cancellationToken: CancellationToken?
        ): Ret<SolveReport<Flt64>> {
            firstBackendReady.await()
            cancellationToken?.request(reason = "other backend cancellation")
            return Failed(ErrorCode.ApplicationError, "barrier backend failed")
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            return Failed(ErrorCode.ApplicationError, "barrier backend failed")
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            return Failed(ErrorCode.ApplicationError, "barrier backend failed")
        }
    }
}
