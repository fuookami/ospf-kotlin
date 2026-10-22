package fuookami.ospf.kotlin.framework.network_scheduling.application

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeTrue
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.scip.ScipColumnGenerationSolver
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.resilience.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.service.*

/** 显式 SCIP profile 的真实结构/韧性闭环，不作为默认测试前置条件。 / Opt-in SCIP structural/resilience closure. */
class NetworkNativeIntegrationTest {
    @Test
    fun transportationDesignAndWorstScenarioMatchHandOracle() = runBlocking {
        assumeTrue(runtimeAvailable(), "SCIP native runtime unavailable; not verified")
        val solver = ScipColumnGenerationSolver(config = SolverConfig(threadNum = UInt64.one))
        val problem = fixture()
        val design = assertNotNull(NetworkDesignApplicationService(solver, Flt64NetworkSchedulingSolverValueAdapter).solve(problem).value)
        assertEquals(ProblemStatus.Feasible, design.report.problemStatus)
        val designSolution = assertNotNull(design.solution)
        assertTrue(designSolution.constructionCost.value + designSolution.flow.cost.value eq Flt64(5.0))
        assertEquals(mapOf(NetworkArcId("a") to "built"), designSolution.serviceLevels)
        val resilience = assertNotNull(NetworkResilienceApplicationService(solver, Flt64NetworkSchedulingSolverValueAdapter).solve(
            problem = problem,
            scenarios = FailureScenario.singleArcFailures(setOf(NetworkArcId("a"), NetworkArcId("b"))),
            connectivity = listOf(ConnectivityRequirement(NetworkNodeId("s"), NetworkNodeId("t"), 2, ConnectivityKind.EdgeDisjoint))
        ).value)
        val resilient = assertNotNull(resilience.solution)
        assertTrue(resilient.design.constructionCost.value + resilient.worstOperatingCost.value eq Flt64(10.0))
        assertEquals(2, resilient.design.serviceLevels.size)
        assertTrue(resilient.scenarios.values.all { it.unmetDemand.isEmpty() })
    }

    @Test
    fun failedDemandEndpointReturnsStructuredInfeasibility() = runBlocking {
        assumeTrue(runtimeAvailable(), "SCIP native runtime unavailable; not verified")
        val result = assertNotNull(NetworkResilienceApplicationService(
            ScipColumnGenerationSolver(config = SolverConfig(threadNum = UInt64.one)), Flt64NetworkSchedulingSolverValueAdapter
        ).solve(
            problem = fixture(),
            scenarios = listOf(FailureScenario("lost-demand", failedNodes = setOf(NetworkNodeId("t"))))
        ).value)
        assertEquals(ProblemStatus.Infeasible, result.report.problemStatus)
        assertNull(result.solution)
    }

    private fun fixture(): NetworkDesignProblem<Flt64> {
        val nodes = listOf("s", "t").map { assertNotNull(NetworkNode<Flt64>(NetworkNodeId(it)).value) }
        val arcs = listOf("a" to 1.0, "b" to 2.0).map { (id, cost) -> assertNotNull(NetworkArc(
            from = nodes[0].id, to = nodes[1].id, cost = q(cost), id = NetworkArcId(id)
        ).value) }
        val commodity = assertNotNull(FlowCommodity("goods", listOf(
            assertNotNull(SupplyDemand(nodes[0].id, q(2.0)).value), assertNotNull(SupplyDemand(nodes[1].id, q(-2.0)).value)
        )).value)
        val flow = FlowProblem(
            graph = assertNotNull(NetworkGraph(nodes, arcs).value),
            arcs = arcs.map { assertNotNull(FlowArc(it, assertNotNull(CapacityBounds(q(0.0), q(2.0)).value)).value) },
            commodities = listOf(CommodityFlow(commodity = commodity, unit = NoneUnit, integerStep = q(1.0))),
            zero = Flt64.zero
        )
        return NetworkDesignProblem(flow, services = arcs.map { NetworkService(it.id, listOf(CapacityLevel("built", q(2.0), q(3.0)))) })
    }

    private fun q(value: Double) = Quantity(Flt64(value), NoneUnit)
    private fun runtimeAvailable(): Boolean = runCatching { Class.forName("jscip.Scip"); System.loadLibrary("jscip") }.isSuccess
}
