package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.ConstraintSource
import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.model.intermediate.BasicLinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.LinearObjective
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix
import fuookami.ospf.kotlin.core.model.intermediate.SparseVector
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.EvidenceExactness
import fuookami.ospf.kotlin.core.solver.report.EvidenceMinimality
import fuookami.ospf.kotlin.core.solver.report.EvidenceValidity
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidenceSource
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/** Verify Gurobi native IIS and Farkas evidence on a minimal infeasible LP. / 验证 Gurobi 最小不可行 LP 的原生 IIS 与 Farkas 证据。 */
class GurobiInfeasibilityDiagnosticIT {
    @Test
    fun nativeIisMustReportMinimalityFromBackend() = runBlocking {
        val solver = GurobiLinearSolver()
        val analyzer = solver.diagnosticAnalyzers(IISConfig(time = 5.seconds))
            .first { it.source == InfeasibilityEvidenceSource.NativeIIS }
        val result = analyzer.analyze(infeasibleModel())

        assertTrue(result.ok, "Gurobi native IIS failed: $result")
        val evidence = result.value!!
        assertEquals(InfeasibilityEvidenceSource.NativeIIS, evidence.source)
        assertEquals(EvidenceValidity.Verified, evidence.validity)
        assertEquals(EvidenceExactness.Irreducible, evidence.exactness)
        assertEquals(EvidenceMinimality.Irreducible, evidence.minimality)
        assertEquals(2, evidence.constraintIds.size)
        assertTrue(
            evidence.constraintIds.containsAll(
                setOf(
                    ConstraintId("fixture:constraint:lower"),
                    ConstraintId("fixture:constraint:upper")
                )
            )
        )
    }

    @Test
    fun farkasMustReportNonzeroRowsWithoutVariableAttributeLookup() = runBlocking {
        val solver = GurobiLinearSolver()
        val analyzer = solver.diagnosticAnalyzers(IISConfig(time = 5.seconds))
            .first { it.source == InfeasibilityEvidenceSource.Farkas }
        val result = analyzer.analyze(infeasibleModel())

        assertTrue(result.ok, "Gurobi Farkas diagnosis failed: $result")
        val evidence = result.value!!
        assertEquals(InfeasibilityEvidenceSource.Farkas, evidence.source)
        assertEquals(EvidenceValidity.Verified, evidence.validity)
        assertEquals(2, evidence.constraintIds.size)
        assertTrue(evidence.variableBoundRefs.isEmpty())
        assertTrue(
            evidence.constraintIds.containsAll(
                setOf(
                    ConstraintId("fixture:constraint:lower"),
                    ConstraintId("fixture:constraint:upper")
                )
            )
        )
    }

    private fun infeasibleModel(): LinearTriadModel {
        val lhs = SparseMatrix<Flt64>().also { matrix ->
            matrix.addRow(SparseVector<Flt64>().also {
                it.add(0, Flt64.one)
                it.add(1, Flt64.one)
            })
            matrix.addRow(SparseVector<Flt64>().also {
                it.add(0, Flt64.one)
                it.add(1, Flt64.one)
            })
        }
        val basicModel = BasicLinearTriadModel(
            variables = listOf(
                Variable(
                    index = 0,
                    lowerBound = Flt64.negativeInfinity,
                    upperBound = Flt64.infinity,
                    type = Continuous,
                    origin = null,
                    name = "x"
                ),
                Variable(
                    index = 1,
                    lowerBound = Flt64.negativeInfinity,
                    upperBound = Flt64.infinity,
                    type = Continuous,
                    origin = null,
                    name = "y"
                )
            ),
            constraints = LinearConstraintBatch(
                sparseLhs = lhs,
                signs = listOf(ConstraintRelation.GreaterEqual, ConstraintRelation.LessEqual),
                rhs = listOf(Flt64(2.0), Flt64.zero),
                names = listOf("lower-row", "upper-row"),
                sources = listOf(ConstraintSource.Origin, ConstraintSource.Origin),
                ids = listOf(
                    ConstraintId("fixture:constraint:lower"),
                    ConstraintId("fixture:constraint:upper")
                )
            ),
            name = "gurobi-diagnostic-it"
        )
        return LinearTriadModel(
            impl = basicModel,
            tokensInSolver = emptyList(),
            objective = LinearObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList()
            )
        )
    }
}
