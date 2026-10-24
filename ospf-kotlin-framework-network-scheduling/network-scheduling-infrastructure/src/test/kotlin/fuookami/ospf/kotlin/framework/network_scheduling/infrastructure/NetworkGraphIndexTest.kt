package fuookami.ospf.kotlin.framework.network_scheduling.infrastructure

import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*
import org.junit.jupiter.api.Test

class NetworkGraphIndexTest {
    @Test
    fun emptyTraversalsAndTrivialWitnessLeaveAdjacencyUnbuilt() {
        val node = NetworkNodeId("s")
        val access = NetworkGraphAccess<NetworkNodeId, Arc>(
            nodeId = { it },
            arcId = { NetworkArcId(it.id) },
            from = { it.from },
            to = { it.to }
        )
        val graph = NetworkGraph(nodes = listOf(node), arcs = emptyList(), access = access).value!!
        val topology = NetworkTopology(graph)
        assertTrue(topology.reachable(emptySet()).value!!.isEmpty())
        assertEquals(emptyList(), topology.witness(node, node).value)
        assertEquals(setOf("arcIdentity", "nodeIndex"), graph.initializedIndexes())
        val empty = NetworkGraph(nodes = emptyList(), arcs = emptyList(), access = access).value!!
        assertTrue(NetworkTopology(empty).stronglyConnectedComponents().isEmpty())
        assertEquals(emptyList(), NetworkTopology(empty).topologicalOrder())
        assertEquals(setOf("arcIdentity"), empty.initializedIndexes())
    }

    @Test
    fun optionalIndexesBuildOnDemandAndReverseSharesTheCapturedSnapshot() {
        var reads = 0
        val nodes = mutableListOf(NetworkNodeId("s"), NetworkNodeId("t"))
        val arc = Arc("a", nodes[0], nodes[1])
        val arcs = mutableListOf(arc)
        val access = NetworkGraphAccess<NetworkNodeId, Arc>(
            nodeId = { ++reads; it },
            arcId = { ++reads; NetworkArcId(it.id) },
            from = { ++reads; it.from },
            to = { ++reads; it.to }
        )
        val graph = NetworkGraph(nodes = nodes, arcs = arcs, access = access).value!!
        val capturedReads = reads
        val reverse = graph.reverse()
        nodes.clear()
        arcs.clear()
        arc.from = NetworkNodeId("changed")
        assertEquals(setOf("arcIdentity"), graph.initializedIndexes())
        assertEquals(setOf("arcIdentity"), reverse.initializedIndexes())
        assertSame(graph.nodes, reverse.nodes)
        assertSame(graph.arcs, reverse.arcs)
        assertSame(graph.nodeIds, reverse.nodeIds)
        assertSame(graph.arcIds, reverse.arcIds)

        val outgoing = graph.outgoing(NetworkNodeId("s"))
        assertEquals(listOf(arc), outgoing)
        assertSame(outgoing, reverse.incoming(NetworkNodeId("s")))
        assertEquals(setOf("arcIdentity", "outgoing"), graph.initializedIndexes())
        assertEquals(graph.initializedIndexes(), reverse.initializedIndexes())
        assertSame(graph.incoming(NetworkNodeId("t")), reverse.outgoing(NetworkNodeId("t")))
        assertSame(outgoing, reverse.reverse().outgoing(NetworkNodeId("s")))
        assertEquals(NetworkNodeId("s"), graph.from(arc))
        assertEquals(NetworkNodeId("s"), reverse.to(arc))
        assertSame(arc, graph.arc(NetworkArcId("a")))
        assertEquals(capturedReads, reads)
        assertFailsWith<UnsupportedOperationException> { (outgoing as MutableList).clear() }
        assertFailsWith<UnsupportedOperationException> { (graph.outgoingIndex() as MutableMap).clear() }

        val filtered = reverse.filtered { false }
        assertEquals(setOf("arcIdentity"), filtered.initializedIndexes())
        assertTrue(filtered.outgoing(NetworkNodeId("t")).isEmpty())
        assertEquals(listOf(arc), reverse.outgoing(NetworkNodeId("t")))
    }

    @Test
    fun outgoingTraversalLeavesIncomingAndIdLookupTablesUnbuilt() {
        val nodes = List(32) { NetworkNodeId("n$it") }
        val arcs = (0 until nodes.lastIndex).map { Arc("a$it", nodes[it], nodes[it + 1]) }
        val graph = NetworkGraph(
            nodes = nodes,
            arcs = arcs,
            access = NetworkGraphAccess<NetworkNodeId, Arc>(
                nodeId = { it },
                arcId = { NetworkArcId(it.id) },
                from = { it.from },
                to = { it.to }
            )
        ).value!!
        assertEquals(nodes.toSet(), NetworkTopology(graph).reachable(setOf(nodes.first())).value)
        assertEquals(setOf("nodeIndex", "arcIdentity", "outgoing"), graph.initializedIndexes())
        repeat(10) {
            assertEquals(nodes.toSet(), NetworkTopology(graph).reachable(setOf(nodes.first())).value)
        }
        assertEquals(setOf("nodeIndex", "arcIdentity", "outgoing"), graph.initializedIndexes())
        assertEquals(nodes.size, NetworkTopology(graph).stronglyConnectedComponents().size)
        assertEquals(setOf("nodeIndex", "arcIdentity", "outgoing", "incoming"), graph.initializedIndexes())
    }

    @Test
    fun concurrentFirstReadersPublishOneImmutableAdjacencyPerDirection() {
        val nodes = List(128) { NetworkNodeId("n$it") }
        val arcs = nodes.indices.map { Arc("a$it", nodes[it], nodes[(it + 1) % nodes.size]) }
        val graph = NetworkGraph(
            nodes = nodes,
            arcs = arcs,
            access = NetworkGraphAccess<NetworkNodeId, Arc>(
                nodeId = { it },
                arcId = { NetworkArcId(it.id) },
                from = { it.from },
                to = { it.to }
            )
        ).value!!
        val reverse = graph.reverse()
        val pool = Executors.newFixedThreadPool(8)
        val barrier = CyclicBarrier(8)
        try {
            val results = pool.invokeAll(List(8) { worker -> Callable {
                barrier.await(10, TimeUnit.SECONDS)
                val outgoing = if (worker % 2 == 0) graph.outgoing(nodes[0]) else reverse.incoming(nodes[0])
                val incoming = if (worker % 2 == 0) graph.incoming(nodes[0]) else reverse.outgoing(nodes[0])
                repeat(100) {
                    assertSame(arcs[0], graph.arc(NetworkArcId("a0")))
                    assertEquals(nodes[0], graph.from(arcs[0]))
                    assertEquals(nodes[1], reverse.from(arcs[0]))
                    assertEquals(0, reverse.indexOf(nodes[0]))
                    assertEquals(nodes[0], graph.nodeId(nodes[0]))
                }
                outgoing to incoming
            } }, 30, TimeUnit.SECONDS).map { it.get() }
            results.forEach {
                assertSame(results.first().first, it.first)
                assertSame(results.first().second, it.second)
            }
            assertEquals(listOf(arcs.first()), results.first().first)
            assertEquals(listOf(arcs.last()), results.first().second)
            assertEquals(6, graph.initializedIndexes().size)
        } finally {
            pool.shutdownNow()
        }
    }

    private data class Arc(val id: String, var from: NetworkNodeId, var to: NetworkNodeId)
}
