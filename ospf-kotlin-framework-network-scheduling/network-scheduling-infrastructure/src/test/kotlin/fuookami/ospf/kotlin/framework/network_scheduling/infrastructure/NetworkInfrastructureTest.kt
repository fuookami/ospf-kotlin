package fuookami.ospf.kotlin.framework.network_scheduling.infrastructure

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

class NetworkInfrastructureTest {
    @Test
    fun graphShouldUseStableLocalIndicesAndImmutableSnapshots() {
        val nodes = mutableListOf(
            node64("start", Flt64.zero),
            node64("customer", Flt64.one),
            node64("end", Flt64.two)
        )
        val arcs = mutableListOf(
            arc64("start", "customer", Flt64.one),
            arc64("customer", "end", Flt64.one)
        )
        val graph = assertNotNull(NetworkGraph(nodes, arcs).value)
        nodes.clear()
        arcs.clear()

        assertEquals(3, graph.nodes.size)
        assertEquals(2, graph.arcs.size)
        assertEquals(1, graph.indexOf(NetworkNodeId("customer")))
        assertEquals(1, graph.outgoing(NetworkNodeId("start")).size)
        assertEquals(1, graph.incoming(NetworkNodeId("end")).size)
    }

    @Test
    fun graphAndArcShouldSupportFltX() {
        val start = assertNotNull(
            NetworkNode(
                id = NetworkNodeId("start"),
                attributes = mapOf(
                    "x" to Quantity(FltX("0"), Meter),
                    "y" to Quantity(FltX("0"), Meter)
                )
            ).value
        )
        val end = assertNotNull(
            NetworkNode(
                id = NetworkNodeId("end"),
                attributes = mapOf(
                    "x" to Quantity(FltX("3"), Meter),
                    "y" to Quantity(FltX("4"), Meter)
                )
            ).value
        )
        val arc = assertNotNull(
            NetworkArc(
                from = start.id,
                to = end.id,
                cost = Quantity(FltX("5"), NoneUnit)
            ).value
        )

        assertTrue(NetworkGraph(listOf(start, end), listOf(arc)).ok)
    }

    @Test
    fun arcShouldAllowNegativeCostAndNoDistance() {
        val from = assertNotNull(NetworkNode<Flt64>(NetworkNodeId("from")).value)
        val to = assertNotNull(NetworkNode<Flt64>(NetworkNodeId("to")).value)
        val arc = NetworkArc(
            from = from.id,
            to = to.id,
            cost = Quantity(Flt64(-1.0), NoneUnit)
        )

        assertTrue(arc.ok)
        val graph = NetworkGraph.Companion.invoke<Flt64>(listOf(from, to), listOf(assertNotNull(arc.value)))
        assertTrue(graph.ok)
    }

    @Test
    fun bopCraftShapeShouldNotRequireCoordinates() {
        val raw = assertNotNull(NetworkNode<Flt64>(NetworkNodeId("wh-raw")).value)
        val craft = assertNotNull(NetworkNode<Flt64>(NetworkNodeId("craft-node-1")).value)
        val product = assertNotNull(NetworkNode<Flt64>(NetworkNodeId("wh-product")).value)
        val arcs = listOf(
            assertNotNull(
                NetworkArc(
                    from = raw.id,
                    to = craft.id,
                    cost = Quantity(Flt64.zero, NoneUnit)
                ).value
            ),
            assertNotNull(
                NetworkArc(
                    from = craft.id,
                    to = product.id,
                    cost = Quantity(Flt64.zero, NoneUnit)
                ).value
            )
        )

        val graph = NetworkGraph.Companion.invoke<Flt64>(listOf(raw, craft, product), arcs)
        assertTrue(graph.ok)
    }

    @Test
    fun capacityBoundsShouldValidateDimensionAndOrder() {
        assertTrue(
            CapacityBounds(
                lower = Quantity(Flt64.one, Kilogram),
                upper = Quantity(Flt64(2.0), Kilogram)
            ).ok
        )
        assertTrue(
            CapacityBounds(
                lower = Quantity(Flt64(2.0), Kilogram),
                upper = Quantity(Flt64.one, Kilogram)
            ).failed
        )
        assertTrue(
            CapacityBounds(
                lower = Quantity(Flt64.one, Meter),
                upper = Quantity(Flt64(2.0), Kilogram)
            ).failed
        )
    }

    @Test
    fun solverAdapterShouldNormalizeUnitsAndRejectDimensionMismatch() {
        val normalized = FltXNetworkSchedulingSolverValueAdapter.normalize(
            quantity = Quantity(FltX("250"), Centimeter),
            targetUnit = Meter
        )

        assertEquals(Flt64(2.5), normalized.value)
        assertTrue(
            Flt64NetworkSchedulingSolverValueAdapter.normalize(
                quantity = Quantity(Flt64.one, Second),
                targetUnit = Meter
            ).failed
        )
    }

    private fun node64(id: String, x: Flt64): NetworkNode<Flt64> {
        return assertNotNull(
            NetworkNode(
                id = NetworkNodeId(id),
                attributes = mapOf(
                    "x" to Quantity(x, Meter),
                    "y" to Quantity(Flt64.zero, Meter)
                )
            ).value
        )
    }

    private fun arc64(from: String, to: String, cost: Flt64): NetworkArc<Flt64> {
        return assertNotNull(
            NetworkArc(
                from = NetworkNodeId(from),
                to = NetworkNodeId(to),
                cost = Quantity(cost, NoneUnit)
            ).value
        )
    }
}
