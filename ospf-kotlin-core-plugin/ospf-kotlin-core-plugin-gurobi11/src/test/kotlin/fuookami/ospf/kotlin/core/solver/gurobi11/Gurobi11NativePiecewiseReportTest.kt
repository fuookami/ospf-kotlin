package fuookami.ospf.kotlin.core.solver.gurobi11

import kotlin.test.*
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.BasicLinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.LinearObjective
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.*

class Gurobi11NativePiecewiseReportTest {
    @Test
    fun reportWrapperMarksGurobi11WithoutDroppingReportState() {
        val report = SolveReport<Flt64>(
            problemStatus = ProblemStatus.Feasible,
            terminationReason = TerminationReason.Completed,
            solutionPresence = SolutionPresence.None,
            statistics = SolveStatistics(bestBound = Flt64.one),
            diagnostics = SolveDiagnostics(
                warnings = listOf(
                    SolveIssue(
                        code = "test-warning",
                        category = SolveIssueCategory.Protocol,
                        message = "warning"
                    )
                )
            )
        )
        val restored = requireReport(
            restoreGurobiPiecewiseSolution(
                report = report,
                nativeModel = emptyModel(),
                originalTokens = emptyList(),
                structures = emptyList()
            )
        )

        assertEquals("gurobi11-pwl-1", restored.fingerprints.model?.schemaVersion)
        assertEquals(report.problemStatus, restored.problemStatus)
        assertEquals(report.terminationReason, restored.terminationReason)
        assertEquals(report.solutionPresence, restored.solutionPresence)
        assertEquals(report.statistics, restored.statistics)
        assertEquals(report.diagnostics, restored.diagnostics)
    }

    private fun emptyModel(): LinearTriadModel {
        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = emptyList(),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix<Flt64>(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "gurobi11-report"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList()
            )
        )
    }

    private fun requireReport(result: Ret<SolveReport<Flt64>>): SolveReport<Flt64> {
        return when (result) {
            is Ok -> result.value
            is Failed -> fail(result.error.message ?: "unexpected failure")
            is Fatal -> fail(result.errors.toString())
        }
    }
}
