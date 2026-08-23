package fuookami.ospf.kotlin.core.solver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.report.*

/** 固定线性/二次 solver 终态 fixture。 / Fixed linear/quadratic solver terminal fixtures. */
class SolverFixtureMatrixTest {
    @Test
    fun fixedLinearAndQuadraticFixturesCoverTheTerminalMatrix() = runBlocking {
        val cases = terminalFixtures()
        val linearReports = cases.map { fixture ->
            requireReport(FixedLinearSolver(fixture.report).solveReport(linearModel()))
        }
        val quadraticReports = cases.map { fixture ->
            requireReport(FixedQuadraticSolver(fixture.report).solveReport(quadraticModel()))
        }

        assertEquals(10, cases.size)
        (linearReports + quadraticReports).forEachIndexed { index, report ->
            val fixture = cases[index % cases.size]
            assertEquals(fixture.problemStatus, report.problemStatus, fixture.name)
            assertEquals(fixture.terminationReason, report.terminationReason, fixture.name)
            assertEquals(fixture.solutionPresence, report.solutionPresence, fixture.name)
            if (fixture.solutionPresence == SolutionPresence.None) {
                assertNull(report.solution, fixture.name)
            } else {
                assertNotNull(report.solution, fixture.name)
            }
        }

        val numerical = linearReports.single { it.terminationReason == TerminationReason.NumericalFailure }
        assertTrue(numerical.diagnostics.errors.any { it.category == SolveIssueCategory.Numerical })
    }

    @Test
    fun fixtureCounterexamplesKeepNormalTerminationsOutOfFailedResult() = runBlocking {
        val cases = terminalFixtures()
        cases.filter { it.problemStatus != ProblemStatus.Unknown }.forEach { fixture ->
            val result = FixedLinearSolver(fixture.report).solveReport(linearModel())
            assertTrue(result is Ok, fixture.name)
            assertEquals(fixture.problemStatus, (result as Ok).value.problemStatus, fixture.name)
        }
    }

    private fun terminalFixtures(): List<TerminalFixture> {
        val incumbent = SolveSolution(values = listOf(Flt64(1.0)), objective = Flt64(2.0))
        return listOf(
            TerminalFixture(
                name = "optimal",
                problemStatus = ProblemStatus.Feasible,
                terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.Optimal,
                report = SolveReport(
                    problemStatus = ProblemStatus.Feasible,
                    terminationReason = TerminationReason.Completed,
                    solutionPresence = SolutionPresence.Optimal,
                    solution = incumbent,
                    proof = SolveProof(ProofStatus.Verified)
                )
            ),
            TerminalFixture(
                name = "feasible-incumbent",
                problemStatus = ProblemStatus.Feasible,
                terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.Incumbent,
                report = SolveReport(
                    problemStatus = ProblemStatus.Feasible,
                    terminationReason = TerminationReason.Completed,
                    solutionPresence = SolutionPresence.Incumbent,
                    solution = incumbent
                )
            ),
            TerminalFixture(
                name = "infeasible",
                problemStatus = ProblemStatus.Infeasible,
                terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.None,
                report = noSolutionReport(ProblemStatus.Infeasible, TerminationReason.Completed)
            ),
            TerminalFixture(
                name = "unbounded",
                problemStatus = ProblemStatus.Unbounded,
                terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.None,
                report = noSolutionReport(ProblemStatus.Unbounded, TerminationReason.Completed)
            ),
            TerminalFixture(
                name = "timeout-with-incumbent",
                problemStatus = ProblemStatus.Feasible,
                terminationReason = TerminationReason.TimeLimit,
                solutionPresence = SolutionPresence.Incumbent,
                report = noSolutionReport(ProblemStatus.Feasible, TerminationReason.TimeLimit).copy(
                    solutionPresence = SolutionPresence.Incumbent,
                    solution = incumbent
                )
            ),
            TerminalFixture(
                name = "timeout-without-incumbent",
                problemStatus = ProblemStatus.Unknown,
                terminationReason = TerminationReason.TimeLimit,
                solutionPresence = SolutionPresence.None,
                report = noSolutionReport(ProblemStatus.Unknown, TerminationReason.TimeLimit)
            ),
            TerminalFixture(
                name = "cancelled",
                problemStatus = ProblemStatus.Unknown,
                terminationReason = TerminationReason.Cancelled,
                solutionPresence = SolutionPresence.None,
                report = cancelledSolveReport("fixture cancellation")
            ),
            TerminalFixture(
                name = "node-limit",
                problemStatus = ProblemStatus.Feasible,
                terminationReason = TerminationReason.NodeLimit,
                solutionPresence = SolutionPresence.Incumbent,
                report = noSolutionReport(ProblemStatus.Feasible, TerminationReason.NodeLimit).copy(
                    solutionPresence = SolutionPresence.Incumbent,
                    solution = incumbent
                )
            ),
            TerminalFixture(
                name = "iteration-limit",
                problemStatus = ProblemStatus.Unknown,
                terminationReason = TerminationReason.IterationLimit,
                solutionPresence = SolutionPresence.None,
                report = noSolutionReport(ProblemStatus.Unknown, TerminationReason.IterationLimit)
            ),
            TerminalFixture(
                name = "numerical-failure",
                problemStatus = ProblemStatus.Unknown,
                terminationReason = TerminationReason.NumericalFailure,
                solutionPresence = SolutionPresence.None,
                report = noSolutionReport(ProblemStatus.Unknown, TerminationReason.NumericalFailure).copy(
                    diagnostics = SolveDiagnostics(
                        errors = listOf(
                            SolveIssue(
                                code = ErrorCode.OREngineSolvingException.toString(),
                                category = SolveIssueCategory.Numerical,
                                message = "fixture numerical failure"
                            )
                        )
                    )
                )
            )
        )
    }

    private fun noSolutionReport(
        problemStatus: ProblemStatus,
        terminationReason: TerminationReason
    ): SolveReport<Flt64> {
        return SolveReport(
            problemStatus = problemStatus,
            terminationReason = terminationReason,
            solutionPresence = SolutionPresence.None
        )
    }

    private fun linearModel(): LinearTriadModel {
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
                name = "fixed-linear-fixture"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(ObjectCategory.Minimum, emptyList())
        )
    }

    private fun quadraticModel(): QuadraticTetradModel {
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
                name = "fixed-quadratic-fixture"
            ),
            tokensInSolver = emptyList(),
            objective = QuadraticObjective(ObjectCategory.Minimum, emptyList())
        )
    }

    private suspend fun requireReport(result: Ret<SolveReport<Flt64>>): SolveReport<Flt64> {
        return when (result) {
            is Ok -> result.value
            is Failed -> error("fixture solve failed: ${result.error}")
            is Fatal -> error("fixture solve failed fatally: ${result.errors}")
        }
    }

    private data class TerminalFixture(
        val name: String,
        val problemStatus: ProblemStatus,
        val terminationReason: TerminationReason,
        val solutionPresence: SolutionPresence,
        val report: SolveReport<Flt64>
    )

    private class FixedLinearSolver(
        private val fixedReport: SolveReport<Flt64>
    ) : AbstractLinearSolver {
        override val name: String = "fixed-linear-fixture"

        override suspend fun invoke(
            model: LinearTriadModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            return Ok(fixedReport)
        }

        override suspend fun invoke(
            model: LinearTriadModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            return Ok(fixedReport to emptyList())
        }
    }

    private class FixedQuadraticSolver(
        private val fixedReport: SolveReport<Flt64>
    ) : AbstractQuadraticSolver {
        override val name: String = "fixed-quadratic-fixture"

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            return Ok(fixedReport)
        }

        override suspend fun invoke(
            model: QuadraticTetradModelView,
            solutionAmount: UInt64,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
            return Ok(fixedReport to emptyList())
        }
    }
}
