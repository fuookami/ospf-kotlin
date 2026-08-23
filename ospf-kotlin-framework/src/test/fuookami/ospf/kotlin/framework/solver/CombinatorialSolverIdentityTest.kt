/**
 * 组合求解器身份门禁测试。 / Combinatorial solver identity gate tests.
 *
 * 验证串行/并行组合求解器在模型身份校验失败时返回结构化错误，
 * 且不会启动任何 backend 尝试。 / Verifies that serial and parallel combinatorial
 * solvers return a structured error when model identity validation fails,
 * without starting any backend attempt.
 */
package fuookami.ospf.kotlin.framework.solver

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
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
import fuookami.ospf.kotlin.core.model.intermediate.BasicQuadraticTetradModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.LinearObjective
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticObjective
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModel
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix
import fuookami.ospf.kotlin.core.model.intermediate.SparseQuadraticMatrix
import fuookami.ospf.kotlin.core.solver.AbstractLinearSolver
import fuookami.ospf.kotlin.core.solver.AbstractQuadraticSolver
import fuookami.ospf.kotlin.core.solver.report.CancellationToken
import fuookami.ospf.kotlin.core.solver.report.CombinatorialSolveReport
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.core.solver.report.SolveAttemptId
import fuookami.ospf.kotlin.core.solver.report.SolveHandle
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.SolveSelectionReason
import fuookami.ospf.kotlin.core.solver.report.SolveSolution
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack

/**
 * 组合求解器身份门禁测试。 / Combinatorial solver identity gate tests.
 */
class CombinatorialSolverIdentityTest {
    /**
     * 串行组合线性求解器在身份校验失败时不启动 backend。
     * Serial combinatorial linear solver must not start a backend when identity validation fails.
     */
    @Test
    fun serialLinearSolverPropagatesIdentityFailureWithoutInvokingBackend() = runBlocking {
        val backend = RecordingLinearSolver()
        val solver = SerialCombinatorialLinearSolver(listOf(backend))

        val result = solver.invoke(linearModelWithFailedIdentity(), null)

        assertTrue(result is Failed)
        assertEquals(0, backend.calls.get())
    }

    /**
     * 串行组合二次求解器在身份校验失败时不启动 backend。
     * Serial combinatorial quadratic solver must not start a backend when identity validation fails.
     */
    @Test
    fun serialQuadraticSolverPropagatesIdentityFailureWithoutInvokingBackend() = runBlocking {
        val backend = RecordingQuadraticSolver()
        val solver = SerialCombinatorialQuadraticSolver(listOf(backend))

        val result = solver.invoke(quadraticModelWithFailedIdentity(), null)

        assertTrue(result is Failed)
        assertEquals(0, backend.calls.get())
    }

    /**
     * 并行组合线性求解器在身份校验失败时不启动 backend，且返回组合报告错误。
     * Parallel combinatorial linear solver must not start a backend when identity validation fails.
     */
    @Test
    fun parallelLinearSolverPropagatesIdentityFailureWithoutInvokingBackend() = runBlocking {
        val backend = RecordingLinearSolver()
        val solver = ParallelCombinatorialLinearSolver(listOf(backend))

        val reportResult = solver.solveCombinatorialReport(linearModelWithFailedIdentity())
        assertTrue(reportResult is Failed)
        assertEquals(0, backend.calls.get())

        val invokeResult = solver.invoke(linearModelWithFailedIdentity(), null)
        assertTrue(invokeResult is Failed)
        assertEquals(0, backend.calls.get())
    }

    /**
     * 并行组合二次求解器在身份校验失败时不启动 backend，且返回组合报告错误。
     * Parallel combinatorial quadratic solver must not start a backend when identity validation fails.
     */
    @Test
    fun parallelQuadraticSolverPropagatesIdentityFailureWithoutInvokingBackend() = runBlocking {
        val backend = RecordingQuadraticSolver()
        val solver = ParallelCombinatorialQuadraticSolver(listOf(backend))

        val invokeResult = solver.invoke(quadraticModelWithFailedIdentity(), null)
        assertTrue(invokeResult is Failed)
        assertEquals(0, backend.calls.get())
    }

    /**
     * 并行组合求解器在预取消时不初始化 backend，且普通/解池入口都保留取消终态。
     * Parallel combinatorial solvers must not initialize backends when pre-cancelled,
     * and both ordinary and solution-pool entry points must retain the cancellation terminal state.
     */
    @Test
    fun parallelCombinatorialSolversShortCircuitPreCancellation() = runBlocking {
        val linearInitializations = AtomicInteger(0)
        val linearSolver = ParallelCombinatorialLinearSolver(
            listOf<() -> AbstractLinearSolver>({
                linearInitializations.incrementAndGet()
                RecordingLinearSolver()
            })
        )
        val linearHandle = SolveHandle.create()
        assertTrue(linearHandle.cancel(reason = "pre-cancelled").ok)

        val linearResult = linearSolver.solveReport(validLinearModel(), null, linearHandle.token)
        assertTrue(linearResult is Ok)
        assertEquals(TerminationReason.Cancelled, (linearResult as Ok).value.terminationReason)
        assertEquals(0, linearInitializations.get())

        val linearPoolResult = linearSolver.invoke(
            validLinearModel(),
            UInt64(1),
            null,
            linearHandle.token
        )
        assertTrue(linearPoolResult is Ok)
        assertEquals(TerminationReason.Cancelled, (linearPoolResult as Ok).value.first.terminationReason)
        assertEquals(0, linearInitializations.get())

        val quadraticInitializations = AtomicInteger(0)
        val quadraticSolver = ParallelCombinatorialQuadraticSolver(
            listOf<() -> AbstractQuadraticSolver>({
                quadraticInitializations.incrementAndGet()
                RecordingQuadraticSolver()
            })
        )
        val quadraticHandle = SolveHandle.create()
        assertTrue(quadraticHandle.cancel(reason = "pre-cancelled").ok)

        val quadraticResult = quadraticSolver.solveReport(validQuadraticModel(), null, quadraticHandle.token)
        assertTrue(quadraticResult is Ok)
        assertEquals(TerminationReason.Cancelled, (quadraticResult as Ok).value.terminationReason)
        assertEquals(0, quadraticInitializations.get())

        val quadraticPoolResult = quadraticSolver.invoke(
            validQuadraticModel(),
            UInt64(1),
            null,
            quadraticHandle.token
        )
        assertTrue(quadraticPoolResult is Ok)
        assertEquals(TerminationReason.Cancelled, (quadraticPoolResult as Ok).value.first.terminationReason)
        assertEquals(0, quadraticInitializations.get())
    }

    /**
     * 并行二次解池入口必须把 backend 返回的解池写入统一报告。
     * Parallel quadratic solution-pool entry points must retain the backend pool in the unified report.
     */
    @Test
    fun parallelQuadraticSolutionPoolIsPreservedInReport() = runBlocking {
        val solver = ParallelCombinatorialQuadraticSolver(listOf(PoolQuadraticSolver()))

        val result = solver.invoke(validQuadraticModel(), UInt64(2), null)

        assertTrue(result is Ok)
        val value = (result as Ok).value
        assertEquals(2, value.second.size)
        assertEquals(value.second, value.first.solution?.pool)
    }

    /**
     * 串行线性解池路径保留成功 backend 之前的失败 attempt。
     * Serial linear solution-pool paths retain failed attempts before the successful backend.
     */
    @Test
    fun serialLinearSolutionPoolRetainsEarlierFailedAttempt() = runBlocking {
        val solver = SerialCombinatorialLinearSolver(
            listOf(FailingPoolLinearSolver(), PoolLinearSolver())
        )

        val result = solver.invoke(validLinearModel(), UInt64(2), null)

        assertTrue(result is Ok)
        val value = (result as Ok).value
        assertEquals(2, value.first.attempts.size)
        assertEquals("linear-backend-a", value.first.attempts[0].backendId)
        assertEquals(
            ErrorCode.ApplicationError.toString(),
            value.first.attempts[0].errors.single().code
        )
        assertTrue(value.first.attempts.all { it.elapsed != null })
        assertTrue(value.first.attempts.all {
            it.parentAttemptId == SolveAttemptId("serial-combinatorial")
        })
        assertEquals(value.second, value.first.solution?.pool)
        assertTrue(value.first.attempts[1].report != null)
    }

    /**
     * 串行二次解池路径保留成功 backend 之前的失败 attempt。
     * Serial quadratic solution-pool paths retain failed attempts before the successful backend.
     */
    @Test
    fun serialQuadraticSolutionPoolRetainsEarlierFailedAttempt() = runBlocking {
        val solver = SerialCombinatorialQuadraticSolver(
            listOf(FailingPoolQuadraticSolver(), PoolQuadraticSolver())
        )

        val result = solver.invoke(validQuadraticModel(), UInt64(2), null)

        assertTrue(result is Ok)
        val value = (result as Ok).value
        assertEquals(2, value.first.attempts.size)
        assertEquals("quadratic-backend-a", value.first.attempts[0].backendId)
        assertEquals(
            ErrorCode.ApplicationError.toString(),
            value.first.attempts[0].errors.single().code
        )
        assertTrue(value.first.attempts.all { it.elapsed != null })
        assertTrue(value.first.attempts.all {
            it.parentAttemptId == SolveAttemptId("serial-combinatorial")
        })
        assertEquals(value.second, value.first.solution?.pool)
        assertTrue(value.first.attempts[1].report != null)
    }

    /**
     * 串行线性解池遇到无 incumbent 的 Infeasible 终态时不得伪造首次可行选择。
     * A serial linear solution-pool stop without an incumbent must not invent a first-feasible selection.
     */
    @Test
    fun serialLinearSolutionPoolWithoutIncumbentDoesNotSelectAttempt() = runBlocking {
        val solver = SerialCombinatorialLinearSolver(listOf(InfeasiblePoolLinearSolver()))

        val result = solver.invoke(validLinearModel(), UInt64(1), null)

        assertTrue(result is Ok)
        val report = (result as Ok).value.first
        assertEquals(ProblemStatus.Infeasible, report.problemStatus)
        assertNull(report.selectedAttemptId)
        assertEquals(SolveSelectionReason.NoSuccessfulAttempt, report.selectionReason)
    }

    /**
     * 串行二次解池遇到无 incumbent 的 Infeasible 终态时不得伪造首次可行选择。
     * A serial quadratic solution-pool stop without an incumbent must not invent a first-feasible selection.
     */
    @Test
    fun serialQuadraticSolutionPoolWithoutIncumbentDoesNotSelectAttempt() = runBlocking {
        val solver = SerialCombinatorialQuadraticSolver(listOf(InfeasiblePoolQuadraticSolver()))

        val result = solver.invoke(validQuadraticModel(), UInt64(1), null)

        assertTrue(result is Ok)
        val report = (result as Ok).value.first
        assertEquals(ProblemStatus.Infeasible, report.problemStatus)
        assertNull(report.selectedAttemptId)
        assertEquals(SolveSelectionReason.NoSuccessfulAttempt, report.selectionReason)
    }

    /**
     * 串行线性 wrapper 必须冻结 backend 调用前的取消事实，避免迟到取消污染失败 attempt。
     * The serial linear wrapper must freeze cancellation before backend invocation so a late request cannot relabel a failed attempt.
     */
    @Test
    fun serialLinearWrappersDoNotRelabelFailureAfterBackendRequestsCancellation() = runBlocking {
        val reportResult = SerialCombinatorialLinearSolver(
            listOf(CancellingFailedLinearSolver())
        ).solveCombinatorialReport(
            model = validLinearModel(),
            cancellationToken = SolveHandle.create().token
        )

        assertTrue(reportResult is Ok)
        assertNull((reportResult as Ok).value.attempts.single().cancellationReason)

        val handle = SolveHandle.create()
        val poolResult = SerialCombinatorialLinearSolver(
            listOf(CancellingFailedLinearSolver())
        ).invoke(validLinearModel(), UInt64(1), null, handle.token)

        assertTrue(poolResult is Ok)
        assertNull((poolResult as Ok).value.first.attempts.single().cancellationReason)
    }

    /**
     * 串行二次 wrapper 必须冻结 backend 调用前的取消事实，避免迟到取消污染失败 attempt。
     * The serial quadratic wrapper must freeze cancellation before backend invocation so a late request cannot relabel a failed attempt.
     */
    @Test
    fun serialQuadraticWrappersDoNotRelabelFailureAfterBackendRequestsCancellation() = runBlocking {
        val reportResult = SerialCombinatorialQuadraticSolver(
            listOf(CancellingFailedQuadraticSolver())
        ).solveCombinatorialReport(
            model = validQuadraticModel(),
            cancellationToken = SolveHandle.create().token
        )

        assertTrue(reportResult is Ok)
        assertNull((reportResult as Ok).value.attempts.single().cancellationReason)

        val handle = SolveHandle.create()
        val poolResult = SerialCombinatorialQuadraticSolver(
            listOf(CancellingFailedQuadraticSolver())
        ).invoke(validQuadraticModel(), UInt64(1), null, handle.token)

        assertTrue(poolResult is Ok)
        assertNull((poolResult as Ok).value.first.attempts.single().cancellationReason)
    }

    /**
     * backend 在求解期间取消时，所有组合 wrapper 都保留真实取消原因。
     * Every combinatorial wrapper retains the real cancellation reason produced during backend solving.
     */
    @Test
    fun combinatorialWrappersRetainCancellationReasonProducedDuringBackend() = runBlocking {
        val reason = "backend cancelled during solve"

        fun assertReportCancellation(result: Ret<CombinatorialSolveReport<Flt64>>) {
            assertTrue(result is Ok)
            assertEquals(reason, (result as Ok).value.attempts.single().cancellationReason)
        }

        fun assertPoolCancellation(result: Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>>) {
            assertTrue(result is Ok)
            assertEquals(reason, (result as Ok).value.first.attempts.single().cancellationReason)
        }

        assertReportCancellation(
            SerialCombinatorialLinearSolver(listOf(CancellingReportLinearSolver(reason)))
                .solveCombinatorialReport(
                    model = validLinearModel(),
                    cancellationToken = SolveHandle.create().token
                )
        )
        assertPoolCancellation(
            SerialCombinatorialLinearSolver(listOf(CancellingReportLinearSolver(reason)))
                .invoke(validLinearModel(), UInt64(1), null, SolveHandle.create().token)
        )
        assertReportCancellation(
            ParallelCombinatorialLinearSolver(listOf(CancellingReportLinearSolver(reason)))
                .solveCombinatorialReport(
                    model = validLinearModel(),
                    cancellationToken = SolveHandle.create().token
                )
        )
        assertPoolCancellation(
            ParallelCombinatorialLinearSolver(listOf(CancellingReportLinearSolver(reason)))
                .invoke(validLinearModel(), UInt64(1), null, SolveHandle.create().token)
        )
        assertReportCancellation(
            SerialCombinatorialQuadraticSolver(listOf(CancellingReportQuadraticSolver(reason)))
                .solveCombinatorialReport(
                    model = validQuadraticModel(),
                    cancellationToken = SolveHandle.create().token
                )
        )
        assertPoolCancellation(
            SerialCombinatorialQuadraticSolver(listOf(CancellingReportQuadraticSolver(reason)))
                .invoke(validQuadraticModel(), UInt64(1), null, SolveHandle.create().token)
        )
        assertReportCancellation(
            ParallelCombinatorialQuadraticSolver(listOf(CancellingReportQuadraticSolver(reason)))
                .solveCombinatorialReport(
                    model = validQuadraticModel(),
                    cancellationToken = SolveHandle.create().token
                )
        )
        assertPoolCancellation(
            ParallelCombinatorialQuadraticSolver(listOf(CancellingReportQuadraticSolver(reason)))
                .invoke(validQuadraticModel(), UInt64(1), null, SolveHandle.create().token)
        )
    }

    private class RecordingLinearSolver : AbstractLinearSolver {
        val calls = AtomicInteger(0)
        override val name: String = "recording-linear"

        override suspend fun invoke(
            model: LinearTriadModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            calls.incrementAndGet()
            return Failed(ErrorCode.ORModelInfeasible)
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            calls.incrementAndGet()
            return Failed(ErrorCode.ORModelInfeasible)
        }
    }

    private class FailingPoolLinearSolver : AbstractLinearSolver {
        override val name: String = "linear-backend-a"

        override suspend fun invoke(
            model: LinearTriadModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            return Failed(ErrorCode.ApplicationError, "backend A failed")
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            return Failed(ErrorCode.ApplicationError, "backend A failed")
        }
    }

    private class CancellingReportLinearSolver(
        private val reason: String
    ) : AbstractLinearSolver {
        override val name: String = "linear-cancelling-report"

        private fun report(): SolveReport<Flt64> {
            return SolveReport(
                problemStatus = ProblemStatus.Unknown,
                terminationReason = TerminationReason.Cancelled,
                solutionPresence = SolutionPresence.None
            )
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

        override suspend fun invoke(
            model: LinearTriadModelView,
            solvingStatusCallBack: SolvingStatusCallBack?,
            cancellationToken: CancellationToken?
        ): Ret<SolveReport<Flt64>> {
            cancellationToken?.request(reason = reason)
            return Ok(report())
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?,
            cancellationToken: CancellationToken?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            cancellationToken?.request(reason = reason)
            return Ok(report() to emptyList())
        }
    }

    private class PoolLinearSolver : AbstractLinearSolver {
        override val name: String = "linear-backend-b"

        private fun report(): SolveReport<Flt64> {
            return SolveReport(
                problemStatus = ProblemStatus.Feasible,
                terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.Incumbent,
                solution = SolveSolution(values = emptyList(), objective = Flt64.zero)
            )
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
            return Ok(report() to listOf(emptyList(), emptyList()))
        }
    }

    private class InfeasiblePoolLinearSolver : AbstractLinearSolver {
        override val name: String = "linear-infeasible"

        private fun report(): SolveReport<Flt64> {
            return SolveReport(
                problemStatus = ProblemStatus.Infeasible,
                terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.None
            )
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

    private class CancellingFailedLinearSolver : AbstractLinearSolver {
        override val name: String = "linear-cancelling-failure"

        override suspend fun invoke(
            model: LinearTriadModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            return Failed(ErrorCode.ApplicationError, "backend failed")
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            return Failed(ErrorCode.ApplicationError, "backend failed")
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solvingStatusCallBack: SolvingStatusCallBack?,
            cancellationToken: CancellationToken?
        ): Ret<SolveReport<Flt64>> {
            cancellationToken?.request(reason = "backend finalization")
            return Failed(ErrorCode.ApplicationError, "backend failed")
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?,
            cancellationToken: CancellationToken?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            cancellationToken?.request(reason = "backend finalization")
            return Failed(ErrorCode.ApplicationError, "backend failed")
        }
    }

    private class RecordingQuadraticSolver : AbstractQuadraticSolver {
        val calls = AtomicInteger(0)
        override val name: String = "recording-quadratic"

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            calls.incrementAndGet()
            return Failed(ErrorCode.ORModelInfeasible)
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            calls.incrementAndGet()
            return Failed(ErrorCode.ORModelInfeasible)
        }
    }

    private class PoolQuadraticSolver : AbstractQuadraticSolver {
        override val name: String = "pool-quadratic"

        private fun report(): SolveReport<Flt64> {
            return SolveReport(
                problemStatus = ProblemStatus.Feasible,
                terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.Incumbent,
                solution = SolveSolution(values = emptyList(), objective = Flt64.zero)
            )
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            return Ok(report())
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            return Ok(report() to listOf(emptyList(), emptyList()))
        }
    }

    private class InfeasiblePoolQuadraticSolver : AbstractQuadraticSolver {
        override val name: String = "quadratic-infeasible"

        private fun report(): SolveReport<Flt64> {
            return SolveReport(
                problemStatus = ProblemStatus.Infeasible,
                terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.None
            )
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            return Ok(report())
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            return Ok(report() to emptyList())
        }
    }

    private class CancellingFailedQuadraticSolver : AbstractQuadraticSolver {
        override val name: String = "quadratic-cancelling-failure"

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            return Failed(ErrorCode.ApplicationError, "backend failed")
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            return Failed(ErrorCode.ApplicationError, "backend failed")
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solvingStatusCallBack: SolvingStatusCallBack?,
            cancellationToken: CancellationToken?
        ): Ret<SolveReport<Flt64>> {
            cancellationToken?.request(reason = "backend finalization")
            return Failed(ErrorCode.ApplicationError, "backend failed")
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?,
            cancellationToken: CancellationToken?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            cancellationToken?.request(reason = "backend finalization")
            return Failed(ErrorCode.ApplicationError, "backend failed")
        }
    }

    private class CancellingReportQuadraticSolver(
        private val reason: String
    ) : AbstractQuadraticSolver {
        override val name: String = "quadratic-cancelling-report"

        private fun report(): SolveReport<Flt64> {
            return SolveReport(
                problemStatus = ProblemStatus.Unknown,
                terminationReason = TerminationReason.Cancelled,
                solutionPresence = SolutionPresence.None
            )
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            return Ok(report())
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            return Ok(report() to emptyList())
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solvingStatusCallBack: SolvingStatusCallBack?,
            cancellationToken: CancellationToken?
        ): Ret<SolveReport<Flt64>> {
            cancellationToken?.request(reason = reason)
            return Ok(report())
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?,
            cancellationToken: CancellationToken?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            cancellationToken?.request(reason = reason)
            return Ok(report() to emptyList())
        }
    }

    private class FailingPoolQuadraticSolver : AbstractQuadraticSolver {
        override val name: String = "quadratic-backend-a"

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            return Failed(ErrorCode.ApplicationError, "backend A failed")
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            return Failed(ErrorCode.ApplicationError, "backend A failed")
        }
    }

    private fun linearModelWithFailedIdentity(): LinearTriadModel {
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
                name = "invalid-identity-linear"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(ObjectCategory.Minimum, emptyList()),
            identityValidation = Failed(ErrorCode.IllegalArgument, "invalid identity")
        )
    }

    private fun validLinearModel(): LinearTriadModel {
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
                name = "valid-linear"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(ObjectCategory.Minimum, emptyList())
        )
    }

    private fun quadraticModelWithFailedIdentity(): QuadraticTetradModel {
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
                name = "invalid-identity-quadratic"
            ),
            tokensInSolver = emptyList(),
            objective = QuadraticObjective(ObjectCategory.Minimum, emptyList()),
            identityValidation = Failed(ErrorCode.IllegalArgument, "invalid identity")
        )
    }

    private fun validQuadraticModel(): QuadraticTetradModel {
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
                name = "valid-quadratic"
            ),
            tokensInSolver = emptyList(),
            objective = QuadraticObjective(ObjectCategory.Minimum, emptyList())
        )
    }
}
