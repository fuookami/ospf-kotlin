package fuookami.ospf.kotlin.core.solver.report

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.ConstraintSource
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.model.intermediate.BasicLinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.LinearObjective
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix
import fuookami.ospf.kotlin.core.model.intermediate.SparseVector
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.Failed

/** Regression tests for model-side constraint and variable-bound diagnostics. */
class ConstraintEvaluationTest {
    @Test
    fun linearEvaluationReportsSignedSlackViolationAndFiniteBounds() {
        val model = linearModel()

        val constraints = model.evaluateConstraints(listOf(Flt64(2.0), Flt64(3.0)))
        val bounds = model.evaluateVariableBounds(listOf(Flt64(2.0), Flt64(3.0)))

        assertTrue(constraints.ok)
        assertTrue(bounds.ok)
        val rows = constraints.value!!
        assertEquals(Flt64(5.0), rows[0].lhs)
        assertEquals(Flt64(5.0), rows[0].slack)
        assertEquals(Flt64.zero, rows[0].violation)
        assertTrue(rows[0].satisfied)
        assertEquals(Flt64(1.0), rows[1].violation)
        assertFalse(rows[1].satisfied)
        assertTrue(bounds.value!!.all { it.satisfied })
        assertTrue(bounds.value!!.any { it.side == BoundSide.Lower })
        assertTrue(bounds.value!!.any { it.side == BoundSide.Upper })
    }

    @Test
    fun modelDiagnosticsPreserveTerminalConclusionAndReportEvaluationFailuresStructurally() {
        val model = linearModel()
        val report = SolveReport<Flt64>(
            problemStatus = ProblemStatus.Feasible,
            terminationReason = TerminationReason.TimeLimit,
            solutionPresence = SolutionPresence.Incumbent,
            solution = SolveSolution(values = listOf(Flt64(2.0), Flt64(3.0)))
        )

        val enriched = report.withModelDiagnostics(model)
        val malformed = report.copy(solution = SolveSolution(values = listOf(Flt64.one)))
            .withModelDiagnostics(model)

        assertEquals(report.problemStatus, enriched.problemStatus)
        assertEquals(report.terminationReason, enriched.terminationReason)
        assertEquals(2, enriched.diagnostics.constraintEvaluations.size)
        assertTrue(enriched.diagnostics.errors.isEmpty())
        assertEquals(report.problemStatus, malformed.problemStatus)
        assertTrue(malformed.diagnostics.errors.any { it.code == "constraint-evaluation-failed" })
        assertTrue(malformed.diagnostics.errors.any { it.code == "variable-bound-evaluation-failed" })
    }

    @Test
    fun evaluationRejectsNegativeToleranceAndMissingValuesAsInputErrors() {
        val model = linearModel()

        val negativeTolerance = model.evaluateConstraints(
            values = listOf(Flt64.zero, Flt64.zero),
            tolerance = Flt64(-0.1)
        )
        val missingValue = model.evaluateVariableBounds(listOf(Flt64.zero))

        assertTrue(negativeTolerance is Failed)
        assertEquals(fuookami.ospf.kotlin.utils.error.ErrorCode.IllegalArgument, negativeTolerance.error.code)
        assertTrue(missingValue is Failed)
        assertEquals(fuookami.ospf.kotlin.utils.error.ErrorCode.IllegalArgument, missingValue.error.code)
    }

    private fun linearModel(): LinearTriadModel {
        val lhs = SparseMatrix<Flt64>().also { matrix ->
            matrix.addRow(SparseVector<Flt64>().also {
                it.add(0, Flt64.one)
                it.add(1, Flt64.one)
            })
            matrix.addRow(SparseVector<Flt64>().also {
                it.add(0, Flt64.one)
            })
        }
        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = listOf(
                    Variable(
                        index = 0,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64(10.0),
                        type = Continuous,
                        origin = null,
                        name = "x"
                    ),
                    Variable(
                        index = 1,
                        lowerBound = Flt64.negativeInfinity,
                        upperBound = Flt64(5.0),
                        type = Continuous,
                        origin = null,
                        name = "y"
                    )
                ),
                constraints = LinearConstraintBatch(
                    sparseLhs = lhs,
                    signs = listOf(ConstraintRelation.LessEqual, ConstraintRelation.GreaterEqual),
                    rhs = listOf(Flt64(10.0), Flt64(3.0)),
                    names = listOf("capacity", "minimum-x"),
                    sources = listOf(ConstraintSource.Origin, ConstraintSource.Origin)
                ),
                name = "constraint-evaluation-fixture"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(ObjectCategory.Minimum, emptyList())
        )
    }
}
