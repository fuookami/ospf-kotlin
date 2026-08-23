package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.iis.CapabilityAwareInfeasibilityAnalyzer
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidenceSource
import fuookami.ospf.kotlin.core.solver.report.SolverModelType

/** Gurobi native diagnostic capability contract tests. / Gurobi 原生诊断能力契约测试。 */
class GurobiDiagnosticCapabilityTest {
    @Test
    fun linearDescriptorAndProviderCapabilitiesMustAgree() {
        val solver = GurobiLinearSolver()
        assertTrue(solver.descriptor.capabilities.nativeIIS)
        assertTrue(solver.descriptor.capabilities.farkas)

        val analyzers = solver.diagnosticAnalyzers(IISConfig())
        assertEquals(
            listOf(InfeasibilityEvidenceSource.NativeIIS, InfeasibilityEvidenceSource.Farkas),
            analyzers.map { it.source }
        )
        assertTrue(analyzers.all { analyzer ->
            val capability = (analyzer as CapabilityAwareInfeasibilityAnalyzer<*>).capabilities
            capability.exact && SolverModelType.LP in capability.modelTypes
        })
    }
}
