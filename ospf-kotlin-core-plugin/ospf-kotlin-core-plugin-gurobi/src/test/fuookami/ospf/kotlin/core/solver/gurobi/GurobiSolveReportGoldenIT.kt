package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.variable.Continuous

/** Gurobi SolveReport golden/replay matrix. / Gurobi SolveReport golden/replay 矩阵。 */
class GurobiSolveReportGoldenIT {
    @Test
    fun linearGoldenReplayPreservesStateResidualsAndFingerprints() = runBlocking {
        val first = requireAvailable(GurobiLinearSolver().solveReport(linearModel()), "Gurobi LP")
        val second = requireAvailable(GurobiLinearSolver().solveReport(linearModel()), "Gurobi LP replay")

        assertEquals(ProblemStatus.Feasible, first.problemStatus)
        assertEquals(TerminationReason.Completed, first.terminationReason)
        assertEquals(SolutionPresence.Optimal, first.solutionPresence)
        assertEquals(Flt64(2.0), first.solution?.objective)
        assertNotNull(first.statistics.bestBound)
        assertTrue(first.diagnostics.constraintEvaluations.all { it.satisfied })
        assertNotNull(first.provenance)
        assertEquals("gurobi", first.provenance?.descriptor?.solverId)
        assertNotNull(first.provenance?.configuration)
        assertEquals(first.fingerprints.model, second.fingerprints.model)
        assertEquals(first.fingerprints.configuration, second.fingerprints.configuration)
        assertEquals(first.fingerprints.solver, second.fingerprints.solver)
        assertEquals(first.provenance?.descriptor, second.provenance?.descriptor)
    }

    @Test
    fun infeasibleGoldenRetainsNativeConclusionAndStableEvidence() = runBlocking {
        val solver = GurobiLinearSolver()
        val report = requireAvailable(solver.solveReport(infeasibleModel()), "Gurobi infeasible LP")
        val evidence = requireAvailable(
            solver.diagnosticAnalyzers(IISConfig(time = 5.seconds))
                .first { it.source == InfeasibilityEvidenceSource.NativeIIS }
                .analyze(infeasibleModel()),
            "Gurobi IIS"
        )

        assertEquals(ProblemStatus.Infeasible, report.problemStatus)
        assertEquals(TerminationReason.Completed, report.terminationReason)
        assertEquals(SolutionPresence.None, report.solutionPresence)
        assertNotNull(report.provenance)
        assertNotNull(report.fingerprints.model)
        assertEquals(InfeasibilityEvidenceSource.NativeIIS, evidence.source)
        assertEquals(EvidenceValidity.Verified, evidence.validity)
        assertTrue(evidence.constraintIds.contains(ConstraintId("fixture:constraint:lower")))
        assertTrue(evidence.constraintIds.contains(ConstraintId("fixture:constraint:upper")))
    }

    @Test
    fun quadraticGoldenReplayPreservesObjectiveAndFingerprints() = runBlocking {
        val first = requireAvailable(GurobiQuadraticSolver().solveReport(quadraticModel()), "Gurobi QP")
        val second = requireAvailable(GurobiQuadraticSolver().solveReport(quadraticModel()), "Gurobi QP replay")

        assertEquals(ProblemStatus.Feasible, first.problemStatus)
        assertEquals(TerminationReason.Completed, first.terminationReason)
        assertEquals(SolutionPresence.Optimal, first.solutionPresence)
        val objective = assertNotNull(first.solution?.objective)
        assertEquals(1.0, objective.toDouble(), 1e-6)
        assertTrue(first.diagnostics.constraintEvaluations.all { it.satisfied })
        assertEquals(first.fingerprints.model, second.fingerprints.model)
        assertEquals(first.fingerprints.configuration, second.fingerprints.configuration)
        assertEquals(first.fingerprints.solver, second.fingerprints.solver)
    }

    private fun linearModel(infeasible: Boolean = false): LinearTriadModel {
        val lhs = SparseMatrix<Flt64>().also { matrix ->
            matrix.addRow(SparseVector<Flt64>().also { it.add(0, Flt64.one) })
            if (infeasible) {
                matrix.addRow(SparseVector<Flt64>().also { it.add(0, Flt64.one) })
            }
        }
        val constraintIds = if (infeasible) {
            listOf(ConstraintId("fixture:constraint:lower"), ConstraintId("fixture:constraint:upper"))
        } else {
            listOf(ConstraintId("fixture:constraint:lower"))
        }
        val origins = constraintIds.map { id ->
            ModelElementOrigin("constraint", id.value.substringAfterLast(':'))
        }
        val constraints = LinearConstraintBatch(
            sparseLhs = lhs,
            signs = if (infeasible) {
                listOf(ConstraintRelation.GreaterEqual, ConstraintRelation.LessEqual)
            } else {
                listOf(ConstraintRelation.GreaterEqual)
            },
            rhs = if (infeasible) listOf(Flt64(2.0), Flt64.one) else listOf(Flt64(2.0)),
            names = if (infeasible) listOf("lower", "upper") else listOf("lower"),
            sources = constraintIds.map { ConstraintSource.Origin },
            ids = constraintIds,
            identityNamespace = "fixture",
            identitySchemaVersion = "1.1",
            identityScopes = constraintIds.map { ModelElementScope.Stable },
            identityOrigins = origins,
            identityProvenance = origins.map { listOf(it) }
        )
        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = listOf(stableVariable()),
                constraints = constraints,
                name = "gurobi-golden-linear"
            ),
            tokensInSolver = emptyList(),
            objective = Objective(
                category = ObjectCategory.Minimum,
                objective = listOf(LinearObjectiveCell(0, Flt64.one)),
                id = ObjectiveId("fixture:objective:linear"),
                identityScope = ModelElementScope.Stable,
                identityOrigin = ModelElementOrigin("objective", "linear"),
                identityNamespace = "fixture",
                identitySchemaVersion = "1.1",
                identityProvenance = listOf(ModelElementOrigin("objective", "linear"))
            )
        )
    }

    private fun infeasibleModel(): LinearTriadModel = linearModel(infeasible = true)

    private fun quadraticModel(): QuadraticTetradModel {
        val lhs = SparseQuadraticMatrix().also { matrix ->
            matrix.addRow(SparseQuadraticVector().also { it.add(0, null, Flt64.one) })
        }
        val origin = ModelElementOrigin("constraint", "lower")
        return QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = listOf(stableVariable()),
                constraints = QuadraticConstraintBatch(
                    sparseLhs = lhs,
                    signs = listOf(ConstraintRelation.GreaterEqual),
                    rhs = listOf(Flt64.one),
                    names = listOf("lower"),
                    sources = listOf(ConstraintSource.Origin),
                    ids = listOf(ConstraintId("fixture:constraint:lower")),
                    identityNamespace = "fixture",
                    identitySchemaVersion = "1.1",
                    identityScopes = listOf(ModelElementScope.Stable),
                    identityOrigins = listOf(origin),
                    identityProvenance = listOf(listOf(origin))
                ),
                name = "gurobi-golden-quadratic"
            ),
            tokensInSolver = emptyList(),
            objective = Objective(
                category = ObjectCategory.Minimum,
                objective = listOf(QuadraticObjectiveCell(0, 0, Flt64.one)),
                id = ObjectiveId("fixture:objective:quadratic"),
                identityScope = ModelElementScope.Stable,
                identityOrigin = ModelElementOrigin("objective", "quadratic"),
                identityNamespace = "fixture",
                identitySchemaVersion = "1.1",
                identityProvenance = listOf(ModelElementOrigin("objective", "quadratic"))
            )
        )
    }

    private fun stableVariable(): Variable {
        val origin = ModelElementOrigin("variable", "x")
        return Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64(10.0),
            type = Continuous,
            origin = null,
            name = "x",
            id = VariableId("fixture:variable:x"),
            identityScope = ModelElementScope.Stable,
            identityOrigin = origin,
            identityNamespace = "fixture",
            identitySchemaVersion = "1.1",
            identityProvenance = listOf(origin)
        )
    }

    private fun <T> requireAvailable(result: Ret<T>, label: String): T {
        return when (result) {
            is Ok -> result.value
            is Failed -> {
                if (result.error.code in unavailableCodes) {
                    assumeTrue(false, "$label skipped: ${result.error.message}")
                }
                error("$label failed: ${result.error.code}: ${result.error.message}")
            }
            is Fatal -> {
                if (result.errors.all { it.code in unavailableCodes }) {
                    assumeTrue(false, "$label skipped: ${result.errors.joinToString { it.message ?: "" }}")
                }
                error("$label failed fatally: ${result.errors}")
            }
        }
    }

    private companion object {
        val unavailableCodes = setOf(
            ErrorCode.SolverNotFound,
            ErrorCode.OREngineEnvironmentLost,
            ErrorCode.OREngineConnectionOvertime
        )
    }
}
