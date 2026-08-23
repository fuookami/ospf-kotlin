package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.service.MinCostFlowObjective
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.service.limits.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/** 网络流通用层的反向校验。 / Reverse checks for the generic flow layer. */
class FlowContextTest {
    @Test
    fun flowArcShouldSupportFltX() {
        val from = assertNotNull(NetworkNode<FltX>(NetworkNodeId("from")).value)
        val to = assertNotNull(NetworkNode<FltX>(NetworkNodeId("to")).value)
        val arc = assertNotNull(
            NetworkArc(
                from = from.id,
                to = to.id,
                cost = Quantity(FltX("-1"), NoneUnit)
            ).value
        )
        val capacity = assertNotNull(
            CapacityBounds(
                lower = Quantity(FltX("0"), NoneUnit),
                upper = Quantity(FltX("2"), NoneUnit)
            ).value
        )

        assertTrue(FlowArc(arc, capacity).ok)
    }

    @Test
    fun bopCraftShapeShouldBuildWithoutRoutingAttributes() {
        val raw = node("wh-raw")
        val craft = node("craft-node-1")
        val product = node("wh-product")
        val networkArcs = listOf(
            arc(raw.id, craft.id, Flt64.zero),
            arc(craft.id, product.id, Flt64.zero)
        )
        val flowArcs = networkArcs.map { flowArc(it, Flt64(10.0)) }
        val networkGraph = assertNotNull(NetworkGraph(listOf(raw, craft, product), networkArcs).value)
        val flowGraph = assertNotNull(
            FlowGraph(
                networkGraph = networkGraph,
                arcs = flowArcs,
                flowUnit = NoneUnit
            ).value
        )

        assertEquals(3, flowGraph.nodes.size)
        assertEquals(2, flowGraph.arcs.size)
        assertTrue(flowGraph.nodes.all { it.attributes.isEmpty() })
        assertTrue(flowGraph.nodes.all { it.balances.isEmpty() })
    }

    @Test
    fun minCostFlowFixtureShouldRegisterKnownOptimalFeasibleWitness() {
        val nodes = listOf(node("source"), node("a"), node("b"), node("sink"))
        val networkArcs = listOf(
            arc("source", "a", Flt64(10.0)),
            arc("source", "b", Flt64.zero),
            arc("a", "b", Flt64(-5.0)),
            arc("a", "sink", Flt64(10.0)),
            arc("b", "sink", Flt64.one)
        )
        val flowArcs = networkArcs.map {
            val upper = if (it.from.value == "a" && it.to.value == "b") {
                Flt64(2.0)
            } else {
                Flt64(4.0)
            }
            val lower = if (it.from.value == "a" && it.to.value == "b") {
                Flt64.one
            } else {
                Flt64.zero
            }
            flowArc(it, upper, lower)
        }
        val graph = assertNotNull(
            FlowGraph(
                networkGraph = assertNotNull(NetworkGraph(nodes, networkArcs).value),
                arcs = flowArcs,
                supplyDemands = listOf(
                    supply("source", Flt64(4.0)),
                    supply("sink", Flt64(-4.0))
                ),
                flowUnit = NoneUnit
            ).value
        )
        val model = LinearMetaModel(name = "four_node_mcmf")

        assertEquals(Flt64.one, graph.arcs.single { it.from.value == "a" && it.to.value == "b" }
            .capacity.lower.value)

        assertTrue(FlowConservationConstraint(graph, Flt64NetworkSchedulingSolverValueAdapter)(model).ok)
        assertTrue(CapacityBoundConstraint(graph, Flt64NetworkSchedulingSolverValueAdapter)(model).ok)
        assertTrue(
            MinCostFlowObjective(
                graph = graph,
                valueAdapter = Flt64NetworkSchedulingSolverValueAdapter,
                costUnit = NoneUnit
            )(model).ok
        )

        val witness: Map<Pair<NetworkNodeId, NetworkNodeId>, Flt64> = mapOf(
            (NetworkNodeId("source") to NetworkNodeId("a")) to Flt64.one,
            (NetworkNodeId("source") to NetworkNodeId("b")) to Flt64(3.0),
            (NetworkNodeId("a") to NetworkNodeId("b")) to Flt64.one,
            (NetworkNodeId("a") to NetworkNodeId("sink")) to Flt64.zero,
            (NetworkNodeId("b") to NetworkNodeId("sink")) to Flt64(4.0)
        )
        assertMcmfWitnessIsFeasible(graph, witness)
        val objective = graph.arcs.fold(Flt64.zero) { sum, arc ->
            sum + arc.cost.value * witness.getValue(arc.from to arc.to)
        }
        assertEquals(Flt64(9.0), objective)
        assertTrue(model.constraints.isNotEmpty())
    }

    @Test
    fun mrpShapeShouldUseOnlyConservationAndCapacityBricks() {
        val raw = node("raw")
        val product = node("product")
        val networkArc = arc(raw.id, product.id, Flt64.zero)
        val flowGraph = assertNotNull(
            FlowGraph(
                networkGraph = assertNotNull(NetworkGraph(listOf(raw, product), listOf(networkArc)).value),
                arcs = listOf(flowArc(networkArc, Flt64(5.0))),
                supplyDemands = listOf(
                    supply(raw.id.value, Flt64(5.0)),
                    supply(product.id.value, Flt64(-5.0))
                ),
                flowUnit = NoneUnit
            ).value
        )
        val model = LinearMetaModel(name = "mrp_shape")

        assertTrue(FlowConservationConstraint(flowGraph, Flt64NetworkSchedulingSolverValueAdapter)(model).ok)
        assertTrue(CapacityBoundConstraint(flowGraph, Flt64NetworkSchedulingSolverValueAdapter)(model).ok)
        assertEquals(4, model.constraints.size)
        assertEquals(Flt64(5.0), flowGraph.nodes.first { it.id == NetworkNodeId("raw") }
            .balanceOf("default")?.net?.value)
    }

    @Test
    fun multiCommodityFlowsShouldShareArcCapacity() {
        val nodes = listOf(node("s1"), node("s2"), node("middle"), node("sink"))
        val networkArcs = listOf(
            arc("s1", "middle", Flt64.one),
            arc("s2", "middle", Flt64.one),
            arc("middle", "sink", Flt64(-2.0))
        )
        val flowArcs = networkArcs.map { flowArc(it, Flt64(2.0)) }
        val commodity1 = assertNotNull(
            FlowCommodity(
                id = "c1",
                supplyDemands = listOf(
                    supply("s1", Flt64.one),
                    supply("sink", Flt64(-1.0))
                )
            ).value
        )
        val commodity2 = assertNotNull(
            FlowCommodity(
                id = "c2",
                supplyDemands = listOf(
                    supply("s2", Flt64.one),
                    supply("sink", Flt64(-1.0))
                )
            ).value
        )
        val graph = assertNotNull(
            FlowGraph.withCommodities(
                networkGraph = assertNotNull(NetworkGraph(nodes, networkArcs).value),
                arcs = flowArcs,
                commodities = listOf(commodity1, commodity2),
                flowUnit = NoneUnit
            ).value
        )
        val model = LinearMetaModel(name = "multi_commodity_flow")

        assertTrue(FlowConservationConstraint(graph, Flt64NetworkSchedulingSolverValueAdapter)(model).ok)
        assertTrue(CapacityBoundConstraint(graph, Flt64NetworkSchedulingSolverValueAdapter)(model).ok)
        assertEquals(14, model.constraints.size)
    }

    @Test
    fun supplyDemandQuantityOverloadShouldRejectBlankNodeId() {
        assertFalse(
            SupplyDemand(
                nodeId = NetworkNodeId(""),
                net = Quantity(Flt64.one, NoneUnit)
            ).ok
        )
    }

    private fun node(id: String): NetworkNode<Flt64> {
        return assertNotNull(NetworkNode<Flt64>(NetworkNodeId(id)).value)
    }

    private fun arc(from: String, to: String, cost: Flt64): NetworkArc<Flt64> {
        return arc(NetworkNodeId(from), NetworkNodeId(to), cost)
    }

    private fun arc(from: NetworkNodeId, to: NetworkNodeId, cost: Flt64): NetworkArc<Flt64> {
        return assertNotNull(
            NetworkArc(
                from = from,
                to = to,
                cost = Quantity(cost, NoneUnit)
            ).value
        )
    }

    private fun flowArc(arc: NetworkArc<Flt64>, upper: Flt64, lower: Flt64 = Flt64.zero): FlowArc<Flt64> {
        return assertNotNull(
            FlowArc(
                networkArc = arc,
                capacity = assertNotNull(
                    CapacityBounds(
                        lower = Quantity(lower, NoneUnit),
                        upper = Quantity(upper, NoneUnit)
                    ).value
                )
            ).value
        )
    }

    private fun supply(id: String, net: Flt64): SupplyDemand<Flt64> {
        return assertNotNull(
            SupplyDemand(
                nodeId = NetworkNodeId(id),
                balance = assertNotNull(NodeBalance(Quantity(net, NoneUnit)).value)
            ).value
        )
    }

    private fun assertMcmfWitnessIsFeasible(
        graph: FlowGraph<Flt64>,
        witness: Map<Pair<NetworkNodeId, NetworkNodeId>, Flt64>
    ) {
        for (arc in graph.arcs) {
            val flow = assertNotNull(witness[arc.from to arc.to])
            assertTrue(flow.geq(arc.capacity.lower.value))
            assertTrue(flow.leq(arc.capacity.upper.value))
        }
        for (node in graph.nodes) {
            val incoming = graph.arcs
                .filter { it.to == node.id }
                .fold(Flt64.zero) { sum, arc -> sum + witness.getValue(arc.from to arc.to) }
            val outgoing = graph.arcs
                .filter { it.from == node.id }
                .fold(Flt64.zero) { sum, arc -> sum + witness.getValue(arc.from to arc.to) }
            val balance = graph.supplyDemandOf(node.id)?.net?.value ?: Flt64.zero
            assertEquals(balance, outgoing - incoming)
        }
    }
}
