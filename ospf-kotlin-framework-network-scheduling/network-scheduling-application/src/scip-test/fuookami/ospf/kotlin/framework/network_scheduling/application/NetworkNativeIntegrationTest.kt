package fuookami.ospf.kotlin.framework.network_scheduling.application

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeTrue
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.toMeta
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.scip.ScipColumnGenerationSolver
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.resilience.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.allocation.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.path_compilation.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.service.*

/** 显式 SCIP profile 的真实结构/韧性闭环，不作为默认测试前置条件。 / Opt-in SCIP structural/resilience closure. */
class NetworkNativeIntegrationTest {
    @Test
    fun actualLpDualOfExtraResourceChangesCompletePathPricing() = runBlocking {
        assumeTrue(runtimeAvailable(), "SCIP native runtime unavailable; not verified")
        val context = PathCompilationContext(
            problem = PathCompilationProblem(
                rows = listOf(
                    PathCompilationRow(id = "order", unit = NoneUnit, lower = q(1.0), upper = q(1.0)),
                    PathCompilationRow(id = "resource", unit = NoneUnit, upper = q(0.5))
                ),
                columns = listOf(
                    PathColumn(id = "road", payload = "road", cost = q(1.0), coefficients = mapOf("order" to q(1.0), "resource" to q(1.0))),
                    PathColumn(id = "rail", payload = "rail", cost = q(5.0), coefficients = mapOf("order" to q(1.0)))
                ),
                costUnit = NoneUnit
            ),
            adapter = Flt64NetworkSchedulingSolverValueAdapter
        )
        val model = LinearMetaModel(name = "complete_path_duals")
        try {
            assertTrue(context.register(model).ok)
            val solver = ScipColumnGenerationSolver(config = SolverConfig(threadNum = UInt64.one))
            val lp = assertNotNull(solver.solveLP(name = "complete_path_duals", metaModel = model).value)
            assertTrue(assertNotNull(lp.result.solution?.objective) eq Flt64(3.0))
            val duals = assertNotNull(context.aggregation.refreshShadowPrices(
                target = model,
                duals = lp.dualSolution.toMeta(),
                scope = "unfixed/all-services/v1"
            ).value)
            val candidate = PathColumn(
                id = "new-rail",
                payload = "complete rail with terminal discount",
                cost = q(4.0),
                coefficients = mapOf("order" to q(1.0))
            )
            val reducedCost = assertNotNull(context.aggregation.reducedCost(
                candidate = candidate,
                duals = duals,
                scope = "unfixed/all-services/v1"
            ).value)
            assertTrue(reducedCost eq Flt64(-1.0))
            assertTrue(duals.prices.getValue("resource") eq Flt64(-4.0))
        } finally {
            model.close()
        }
    }

    @Test
    fun indivisibleOrdersAndAllocationMatchFiniteHandOracles() = runBlocking {
        assumeTrue(runtimeAvailable(), "SCIP native runtime unavailable; not verified")
        val solver = ScipColumnGenerationSolver(config = SolverConfig(threadNum = UInt64.one))
        val rows = listOf(
            PathCompilationRow(id = "order1", unit = NoneUnit, lower = q(1.0), upper = q(1.0)),
            PathCompilationRow(id = "order2", unit = NoneUnit, lower = q(1.0), upper = q(1.0)),
            PathCompilationRow(id = "truck", unit = NoneUnit, upper = q(3.0))
        )
        val columns = listOf(
            PathColumn(id = "1-road", payload = "1-road", cost = q(1.0), coefficients = mapOf("order1" to q(1.0), "truck" to q(2.0))),
            PathColumn(id = "1-rail", payload = "1-rail", cost = q(4.0), coefficients = mapOf("order1" to q(1.0))),
            PathColumn(id = "2-road", payload = "2-road", cost = q(2.0), coefficients = mapOf("order2" to q(1.0), "truck" to q(2.0))),
            PathColumn(id = "2-rail", payload = "2-rail", cost = q(8.0), coefficients = mapOf("order2" to q(1.0)))
        )
        val selected = assertNotNull(NetworkPathApplicationService(solver, Flt64NetworkSchedulingSolverValueAdapter).solve(
            PathCompilationProblem(
                rows = rows,
                columns = columns,
                costUnit = NoneUnit
            )
        ).value?.solution)
        assertTrue(selected.cost.value eq Flt64(6.0))
        assertEquals(setOf("1-rail", "2-road"), selected.selected.map { it.id }.toSet())

        val source = NetworkNodeId("source")
        val target = NetworkNodeId("target")
        val problem = AllocationProblem(
            locations = listOf(AllocationLocation<Flt64>(source), AllocationLocation<Flt64>(target)),
            services = listOf(AllocationService(
                id = NetworkArcId("road"),
                from = source,
                to = target,
                cost = q(2.0),
                capacity = assertNotNull(CapacityBounds(q(0.0), q(10.0)).value)
            )),
            requests = listOf(AllocationRequest(
                id = "shipment",
                origin = source,
                destination = target,
                quantity = q(4.0),
                integerStep = q(1.0)
            )),
            zero = Flt64.zero
        )
        val allocated = assertNotNull(NetworkAllocationApplicationService(solver, Flt64NetworkSchedulingSolverValueAdapter).solve(problem).value?.solution)
        assertTrue(allocated.totalCost.value eq Flt64(8.0))
        assertTrue(allocated.shipments.getValue(AllocationFlowKey("shipment", NetworkArcId("road"))).value eq Flt64(4.0))
    }

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
