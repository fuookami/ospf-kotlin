/** SCIP 结构化不可行分析器测试。 / SCIP structured infeasibility analyzer tests. */
package fuookami.ospf.kotlin.core.solver.scip

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
import fuookami.ospf.kotlin.core.solver.report.EvidenceValidity
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidenceSource
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.Failed

/** Verify SCIP's continuous LP Farkas provider on a minimal infeasible model. / 验证 SCIP 连续 LP Farkas provider。 */
class ScipFarkasDiagnosticIT {
    @Test
    fun farkasMustReturnVerifiedNonzeroRows() = runBlocking {
        val analyzer = ScipLinearSolver()
            .diagnosticAnalyzers(IISConfig(time = 5.seconds))
            .single { it.source == InfeasibilityEvidenceSource.Farkas }
        val result = analyzer.analyze(infeasibleModel())

        assertTrue(
            result.ok,
            "SCIP Farkas diagnosis failed: " +
                if (result is Failed) "${result.code}: ${result.message}" else result
        )
        val evidence = result.value!!
        assertEquals(InfeasibilityEvidenceSource.Farkas, evidence.source)
        assertEquals(EvidenceExactness.Exact, evidence.exactness)
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
                ),
                Variable(
                    index = 2,
                    lowerBound = Flt64.zero,
                    upperBound = Flt64.one,
                    type = Continuous,
                    origin = null,
                    name = "unused"
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
            name = "scip-farkas-diagnostic-it"
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
