package fuookami.ospf.kotlin.framework.network_scheduling.domain.resilience

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.*

/** 单故障逐场景复核，并区分结构冗余与流量保障。 / Check explicit failures and distinguish structure from capacity assurance. */
class NetworkResilienceContextTest {
    @Test
    fun singleFailuresShareConstructionButHaveIndependentRecourse() {
        val problem = parallelProblem(1.0, true)
        val context = NetworkResilienceContext(
            design = NetworkDesignContext(problem, Flt64NetworkSchedulingSolverValueAdapter),
            scenarios = FailureScenario.singleArcFailures(setOf(NetworkArcId("a"), NetworkArcId("b"))),
            connectivity = listOf(ConnectivityRequirement(NetworkNodeId("s"), NetworkNodeId("t"), 2, ConnectivityKind.EdgeDisjoint))
        )
        val model = LinearMetaModel(name = "recourse_oracle")
        assertTrue(context.register(model).ok)
        assertNotSame(assertNotNull(context.flow("nominal").value), assertNotNull(context.flow("arc:a").value))
        assertFalse(context.flow("unknown").ok)
        val built = model.tokens.tokensInSolver.count { it.variable.name.startsWith("network_design_a_") || it.variable.name.startsWith("network_design_b_") }
        assertEquals(2, built)
        val values = enumerate(model) { name -> if (name.endsWith("_worst_operation")) 2 else 1 }
        assertNotNull(values)
        model.setSolution(values)
        val result = assertNotNull(context.extractSolution(model).value)
        assertEquals(Flt64(2.0), result.design.constructionCost.value)
        assertEquals(Flt64(2.0), result.worstOperatingCost.value)
        assertEquals(Flt64(2.0), result.scenarios.getValue(result.worstScenario).cost.value)
        assertEquals(setOf("nominal", "arc:a", "arc:b"), result.scenarios.keys)
        assertEquals(Flt64.one, result.scenarios.getValue("arc:a").flows.getValue(FlowKey("goods", NetworkArcId("b"))).value)
        assertEquals(Flt64.zero, result.effectiveCapacities.getValue("arc:a").getValue(NetworkArcId("a")).value)
        assertEquals(Flt64.one, result.effectiveCapacities.getValue("arc:b").getValue(NetworkArcId("a")).value)
    }

    @Test
    fun degradationBelowHardMinimumIsNormalModelInfeasibility() {
        val original = parallelProblem(1.0, false).flow
        val flow = FlowProblem(
            graph = original.graph,
            arcs = original.arcs.map { arc ->
                assertNotNull(FlowArc(
                    networkArc = arc.networkArc,
                    capacity = assertNotNull(CapacityBounds(q(if (arc.networkArc.id == NetworkArcId("a")) 1.0 else 0.0), q(1.0)).value)
                ).value)
            },
            commodities = original.commodities,
            zero = Flt64.zero
        )
        val context = NetworkResilienceContext(
            design = NetworkDesignContext(NetworkDesignProblem(flow), Flt64NetworkSchedulingSolverValueAdapter),
            scenarios = listOf(FailureScenario(
                id = "below-minimum",
                upperCapacities = mapOf(NetworkArcId("a") to q(0.0))
            ))
        )
        val model = LinearMetaModel(name = "degraded_minimum")
        assertTrue(context.register(model).ok)
        assertNull(enumerate(model) { name -> if (name.endsWith("_worst_operation")) 3 else 1 })
    }

    @Test
    fun closedArcStillRejectsCapacityAboveItsNominalUpperBound() {
        val context = NetworkResilienceContext(
            design = NetworkDesignContext(
                problem = parallelProblem(1.0, false),
                adapter = Flt64NetworkSchedulingSolverValueAdapter
            ),
            scenarios = listOf(FailureScenario(
                id = "closed-too-large",
                closedArcs = setOf(NetworkArcId("a")),
                upperCapacities = mapOf(NetworkArcId("a") to q(100.0))
            ))
        )
        val model = LinearMetaModel(name = "closed_arc_invalid_upper")

        assertTrue(context.register(model).failed)
    }

    @Test
    fun nominalEdgeRedundancyDoesNotImplyPostFailureDemandCapacity() {
        val context = NetworkResilienceContext(
            design = NetworkDesignContext(parallelProblem(2.0, false), Flt64NetworkSchedulingSolverValueAdapter),
            scenarios = listOf(FailureScenario("loss-a", closedArcs = setOf(NetworkArcId("a")))),
            connectivity = listOf(ConnectivityRequirement(NetworkNodeId("s"), NetworkNodeId("t"), 2, ConnectivityKind.EdgeDisjoint))
        )
        val model = LinearMetaModel(name = "capacity_shortfall")
        assertTrue(context.register(model).ok)
        assertNull(enumerate(model) { name -> if (name.endsWith("_worst_operation")) 4 else 1 })
    }

    @Test
    fun endpointFailureKeepsHardDemandUnlessExplicitlyOverridden() {
        fun context(cancel: Boolean): NetworkResilienceContext<Flt64> = NetworkResilienceContext(
            design = NetworkDesignContext(parallelProblem(1.0, false), Flt64NetworkSchedulingSolverValueAdapter),
            scenarios = listOf(FailureScenario(
                id = "lost-target",
                failedNodes = setOf(NetworkNodeId("t")),
                balances = if (cancel) mapOf("goods" to emptyList()) else emptyMap()
            ))
        )
        val hard = context(false)
        val hardModel = LinearMetaModel(name = "hard_failed_endpoint")
        assertTrue(hard.register(hardModel).ok)
        assertNull(enumerate(hardModel) { name -> if (name.endsWith("_worst_operation")) 2 else 1 })
        val cancelled = context(true)
        val cancelledModel = LinearMetaModel(name = "explicit_cancellation")
        assertTrue(cancelled.register(cancelledModel).ok)
        assertNotNull(enumerate(cancelledModel) { name -> if (name.endsWith("_worst_operation")) 2 else 1 })
    }

    @Test
    fun capacityDegradationAndExplicitScenarioShortfallAreObservable() {
        val base = parallelProblem(2.0, false)
        val scenario = FailureScenario(
            id = "degraded",
            closedArcs = setOf(NetworkArcId("a")),
            upperCapacities = mapOf(NetworkArcId("b") to q(1.0)),
            slacks = listOf(
                FlowSlack("goods", NetworkNodeId("s"), q(1.0), q(0.0), q(10.0)),
                FlowSlack("goods", NetworkNodeId("t"), q(0.0), q(1.0), q(10.0))
            )
        )
        val context = NetworkResilienceContext(NetworkDesignContext(base, Flt64NetworkSchedulingSolverValueAdapter), listOf(scenario))
        val model = LinearMetaModel(name = "bounded_shortfall")
        assertTrue(context.register(model).ok)
        val values = model.tokens.tokensInSolver.map { token ->
            val name = token.variable.name
            when {
                name.endsWith("_worst_operation") -> Flt64(22.0)
                name == "network_design_nominal_goods_a" || name == "network_design_nominal_goods_b" -> Flt64.one
                name == "network_resilience_degraded_goods_b" || name == "network_resilience_degraded_unused_goods_s" || name == "network_resilience_degraded_unmet_goods_t" -> Flt64.one
                else -> Flt64.zero
            }
        }
        assertTrue(model.constraints.all { it.isTrue(values, model.tokens, false) == true })
        model.setSolution(values)
        val result = assertNotNull(context.extractSolution(model).value)
        assertEquals(Flt64.one, result.scenarios.getValue("degraded").unmetDemand.getValue("goods" to NetworkNodeId("t")).value)
        assertEquals(Flt64(22.0), result.scenarios.getValue("degraded").cost.value)
    }

    @Test
    fun internalNodeDisjointnessIsStrongerThanEdgeDisjointness() {
        val nodes = listOf("s", "a", "t").map(::node)
        val arcs = listOf(Triple("sa1", "s", "a"), Triple("sa2", "s", "a"), Triple("at1", "a", "t"), Triple("at2", "a", "t")).map { (id, from, to) -> arc(id, from, to, 0.0) }
        val flow = FlowProblem(
            graph = assertNotNull(NetworkGraph(nodes, arcs).value),
            arcs = arcs.map { assertNotNull(FlowArc(it, assertNotNull(CapacityBounds(q(0.0), q(2.0)).value)).value) },
            commodities = listOf(CommodityFlow(assertNotNull(FlowCommodity("goods", emptyList<SupplyDemand<Flt64>>()).value), NoneUnit)),
            zero = Flt64.zero
        )
        fun feasible(kind: ConnectivityKind): Boolean {
            val context = NetworkResilienceContext(
                design = NetworkDesignContext(NetworkDesignProblem(flow), Flt64NetworkSchedulingSolverValueAdapter),
                scenarios = emptyList(),
                connectivity = listOf(ConnectivityRequirement(NetworkNodeId("s"), NetworkNodeId("t"), 2, kind))
            )
            val model = LinearMetaModel(name = kind.name)
            assertTrue(context.register(model).ok)
            return enumerate(model) { name -> if ("_connect_" in name) 1 else 0 } != null
        }
        assertTrue(feasible(ConnectivityKind.EdgeDisjoint))
        assertFalse(feasible(ConnectivityKind.NodeDisjoint))
    }

    private fun parallelProblem(demand: Double, candidates: Boolean): NetworkDesignProblem<Flt64> {
        val nodes = listOf(node("s"), node("t"))
        val arcs = listOf(arc("a", "s", "t", 1.0), arc("b", "s", "t", 2.0))
        val commodity = assertNotNull(FlowCommodity("goods", listOf(
            assertNotNull(SupplyDemand(nodes[0].id, q(demand)).value), assertNotNull(SupplyDemand(nodes[1].id, q(-demand)).value)
        )).value)
        val flow = FlowProblem(
            graph = assertNotNull(NetworkGraph(nodes, arcs).value),
            arcs = arcs.map { assertNotNull(FlowArc(it, assertNotNull(CapacityBounds(q(0.0), q(1.0)).value)).value) },
            commodities = listOf(CommodityFlow(commodity = commodity, unit = NoneUnit, integerStep = q(1.0))),
            zero = Flt64.zero
        )
        return NetworkDesignProblem(flow, services = if (candidates) arcs.map { NetworkService(it.id, listOf(CapacityLevel("built", q(1.0), q(1.0)))) } else emptyList())
    }

    private fun enumerate(model: LinearMetaModel<Flt64>, upper: (String) -> Int): List<Flt64>? {
        val tokens = model.tokens.tokensInSolver
        val values = MutableList(tokens.size) { Flt64.zero }
        var result: List<Flt64>? = null
        fun visit(index: Int) {
            if (result != null) return
            if (index == values.size) {
                if (model.constraints.all { it.isTrue(values, model.tokens, false) == true }) result = values.toList()
                return
            }
            for (value in 0..upper(tokens[index].variable.name)) { values[index] = Flt64(value.toDouble()); visit(index + 1) }
        }
        visit(0)
        return result
    }

    private fun q(value: Double) = Quantity(Flt64(value), NoneUnit)
    private fun node(id: String) = assertNotNull(NetworkNode<Flt64>(NetworkNodeId(id)).value)
    private fun arc(id: String, from: String, to: String, cost: Double) = assertNotNull(NetworkArc(
        from = NetworkNodeId(from), to = NetworkNodeId(to), cost = q(cost), id = NetworkArcId(id)
    ).value)
}
