package fuookami.ospf.kotlin.core.solver.scip

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolveOptions
import fuookami.ospf.kotlin.core.solver.iis.CapabilityAwareInfeasibilityAnalyzer
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidenceSource
import fuookami.ospf.kotlin.core.solver.report.SolverModelType

/** Verify the SCIP diagnostic capability contract. / 验证 SCIP 诊断能力契约。 */
class ScipDiagnosticCapabilityTest {
    @Test
    fun linearDescriptorExposesOnlyContinuousFarkas() {
        val solver = ScipLinearSolver()
        assertFalse(solver.descriptor.capabilities.nativeIIS)
        assertTrue(solver.descriptor.capabilities.farkas)

        val analyzers = solver.diagnosticAnalyzers(IISConfig())
        assertEquals(listOf(InfeasibilityEvidenceSource.Farkas), analyzers.map { it.source })
        val capability = (analyzers.single() as CapabilityAwareInfeasibilityAnalyzer<*>).capabilities
        assertTrue(capability.exact)
        assertEquals(setOf(SolverModelType.LP), capability.modelTypes)
    }

    @Test
    fun deterministicModeRejectsExplicitMultipleThreads() {
        val model = ConstraintProgrammingModel("scip-deterministic-thread-conflict")
        try {
            val result = ScipConstraintProgrammingSolver().createSession(
                model,
                ConstraintProgrammingSolveOptions(
                    deterministic = true,
                    threadCount = 8
                )
            )
            assertTrue(result.failed)
        } finally {
            model.close()
        }
    }
}
