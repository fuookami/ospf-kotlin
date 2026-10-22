package fuookami.ospf.kotlin.framework.network_scheduling.infrastructure

import kotlin.random.Random
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*

class NetworkTopologyTest {
    @Test
    fun graphShouldKeepParallelArcsSelfLoopsAndSnapshotIdentity() {
        val startNode = TestNode("s")
        val nodes = mutableListOf(startNode, TestNode("t"), TestNode("u"))
        val parallelFast = TestArc("fast", "s", "t", 2)
        val parallelSlow = TestArc("slow", "s", "t", 5)
        val loop = TestArc("loop", "t", "t", 0)
        val final = TestArc("final", "t", "u", 1)
        val arcs = mutableListOf(parallelFast, parallelSlow, loop, final)
        val graph = assertNotNull(NetworkGraph(nodes = nodes, arcs = arcs, access = access).value)

        nodes.clear()
        arcs.clear()
        startNode.id = "renamed"
        parallelFast.from = "u"
        parallelFast.to = "s"
        nodesSnapshotMutationCheck(graph, parallelFast, startNode)

        assertEquals(listOf("fast", "slow"), graph.outgoing(id("s")).map { graph.arcId(it).value })
        assertEquals(listOf("loop", "final"), graph.outgoing(id("t")).map { graph.arcId(it).value })
        assertEquals(listOf("fast", "slow", "loop"), graph.reverse().outgoing(id("t")).map { graph.arcId(it).value })
        assertEquals(id("s"), graph.from(parallelFast))
        assertEquals(id("t"), graph.to(parallelFast))

        val onlyFast = graph.filtered { it.id == "fast" }
        assertEquals(listOf("fast"), onlyFast.reverse().outgoing(id("t")).map { onlyFast.arcId(it).value })
        val withoutMiddle = graph.filteredNodes { it.id != "t" }
        assertEquals(listOf("s", "u"), withoutMiddle.nodeIds.map { it.value })
        assertTrue(withoutMiddle.arcs.isEmpty())
        assertFailsWith<UnsupportedOperationException> { (graph.nodes as MutableList).clear() }
        assertFailsWith<UnsupportedOperationException> { (graph.arcs as MutableList).clear() }
    }

    @Test
    fun graphShouldRejectDuplicateArcIdentityButAllowDistinctParallelArcIds() {
        val graph = NetworkGraph(
            nodes = listOf(TestNode("s"), TestNode("t")),
            arcs = listOf(TestArc("p1", "s", "t", 1), TestArc("p2", "s", "t", 2)),
            access = access
        )

        assertTrue(graph.ok)
        assertTrue(
            NetworkGraph(
                nodes = listOf(TestNode("s"), TestNode("t")),
                arcs = listOf(TestArc("p", "s", "t", 1), TestArc("p", "s", "t", 2)),
                access = access
            ).failed
        )
    }

    @Test
    fun topologyFiltersShouldApplyToReversedViewsAndWitnesses() {
        val graph = graphOf(
            listOf("s", "a", "t"),
            listOf(
                TestArc("sa", "s", "a", 1),
                TestArc("at", "a", "t", 1),
                TestArc("st", "s", "t", 8)
            )
        )
        val reverse = NetworkTopology(graph.reverse())
        val reachables = assertNotNull(reverse.reachable(setOf(id("t"))).value)

        assertEquals(setOf(id("t"), id("a"), id("s")), reachables)
        assertEquals(
            listOf("at", "sa"),
            assertNotNull(
                reverse.witness(
                    source = id("t"),
                    target = id("s"),
                    allowedArc = { it.id != "st" }
                ).value
            ).map { it.id }
        )
        assertEquals(
            listOf("st"),
            assertNotNull(
                reverse.witness(
                    source = id("t"),
                    target = id("s"),
                    allowedArc = { it.id == "st" }
                ).value
            ).map { it.id }
        )
        assertNull(
            reverse.witness(
                source = id("t"),
                target = id("s"),
                allowedArc = { it.id != "st" },
                allowedNode = { it != id("a") }
            ).value
        )
        assertEquals(
            setOf(id("t"), id("s")),
            assertNotNull(reverse.reachable(sources = setOf(id("t")), allowed = { it.id == "st" }).value)
        )

        val filtered = NetworkTopology(graph).filtered(
            allowedNode = { it != id("a") },
            allowedArc = { it.id == "st" }
        )
        assertEquals(setOf(id("s"), id("t")), assertNotNull(filtered.reachable(setOf(id("s"))).value))
        assertEquals(
            listOf("st"),
            assertNotNull(filtered.witness(source = id("s"), target = id("t")).value).map { it.id }
        )
    }

    @Test
    fun shortestPathShouldReportOnlySourceReachableTargetRelevantNegativeCycles() {
        val source = id("s")
        val target = id("t")
        val irrelevantCycle = graphOf(
            listOf("s", "t", "c", "d"),
            listOf(
                TestArc("direct", "s", "t", 5),
                TestArc("sc", "s", "c", 0),
                TestArc("cd", "c", "d", -2),
                TestArc("dc", "d", "c", 1)
            )
        )
        val finite = shortestPath(irrelevantCycle, source, target)
        assertEquals(ShortestPathStatus.Found, finite.status)
        assertEquals(Flt64(5.0), finite.cost?.value)
        assertEquals(listOf("direct"), finite.arcs.map { it.id })

        val relevantCycle = graphOf(
            listOf("s", "a", "b", "t"),
            listOf(
                TestArc("sa", "s", "a", 0),
                TestArc("ab", "a", "b", 1),
                TestArc("ba", "b", "a", -3),
                TestArc("bt", "b", "t", 1),
                TestArc("st", "s", "t", 20)
            )
        )
        assertEquals(ShortestPathStatus.NegativeCycle, shortestPath(relevantCycle, source, target).status)
    }

    @Test
    fun shortestPathOnSmallDagsShouldMatchExhaustivePathOracle() {
        val random = Random(719)
        repeat(8) { sample ->
            val nodeNames = (0..5).map { "n$it" }
            val arcs = mutableListOf<TestArc>()
            for (from in 0 until nodeNames.lastIndex) {
                for (to in (from + 1)..nodeNames.lastIndex) {
                    if (random.nextInt(4) == 0) continue
                    val pairCount = if (random.nextInt(5) == 0) 2 else 1
                    repeat(pairCount) { parallel ->
                        arcs.add(
                            TestArc(
                                id = "s$sample-$from-$to-$parallel",
                                from = nodeNames[from],
                                to = nodeNames[to],
                                cost = random.nextInt(-4, 9)
                            )
                        )
                    }
                }
            }
            val graph = graphOf(nodeNames, arcs)
            val topology = NetworkTopology(graph)

            for (sourceIndex in nodeNames.indices) {
                for (targetIndex in sourceIndex until nodeNames.size) {
                    val expected = enumerateDagCosts(nodeNames[sourceIndex], nodeNames[targetIndex], arcs)
                    val actual = assertNotNull(
                        topology.shortestPath(
                            source = id(nodeNames[sourceIndex]),
                            target = id(nodeNames[targetIndex]),
                            zero = Flt64.zero,
                            unit = NoneUnit,
                            weight = { ok(Quantity(Flt64(it.cost.toDouble()), NoneUnit)) }
                        ).value
                    )
                    if (expected.isEmpty()) {
                        assertEquals(ShortestPathStatus.Unreachable, actual.status)
                        assertNull(actual.cost)
                    } else {
                        assertEquals(ShortestPathStatus.Found, actual.status)
                        assertEquals(Flt64(expected.min().toDouble()), actual.cost?.value)
                        assertEquals(expected.min(), actual.arcs.fold(0) { total, arc -> total + arc.cost })
                    }
                }
            }
        }
    }

    @Test
    fun dagTraversalShouldRespectEveryArcAndSelfLoopShouldBeACycle() {
        val dag = graphOf(
            listOf("a", "b", "c", "d"),
            listOf(
                TestArc("ab", "a", "b", 0),
                TestArc("ac", "a", "c", 0),
                TestArc("bc", "b", "c", 0),
                TestArc("cd", "c", "d", 0)
            )
        )
        val order = assertNotNull(NetworkTopology(dag).topologicalOrder())
        val positions = order.withIndex().associate { it.value to it.index }
        dag.arcs.forEach { assertTrue(positions.getValue(dag.from(it)) < positions.getValue(dag.to(it))) }
        assertFalse(NetworkTopology(dag).hasCycle())

        val selfLoop = graphOf(listOf("a"), listOf(TestArc("aa", "a", "a", 0)))
        assertTrue(NetworkTopology(selfLoop).hasCycle())
        assertNull(NetworkTopology(selfLoop).topologicalOrder())
        assertEquals(listOf(setOf(id("a"))), NetworkTopology(selfLoop).stronglyConnectedComponents())
    }

    private fun shortestPath(
        graph: NetworkGraph<TestNode, TestArc>,
        source: NetworkNodeId,
        target: NetworkNodeId
    ): ShortestPathResult<TestArc, Flt64> {
        return assertNotNull(
            NetworkTopology(graph).shortestPath(
                source = source,
                target = target,
                zero = Flt64.zero,
                unit = NoneUnit,
                weight = { ok(Quantity(Flt64(it.cost.toDouble()), NoneUnit)) }
            ).value
        )
    }

    private fun enumerateDagCosts(source: String, target: String, arcs: List<TestArc>): List<Int> {
        val results = mutableListOf<Int>()
        fun visit(node: String, cost: Int) {
            if (node == target) {
                results.add(cost)
                return
            }
            for (arc in arcs.filter { it.from == node }) visit(arc.to, cost + arc.cost)
        }
        visit(source, 0)
        return results
    }

    private fun graphOf(nodes: List<String>, arcs: List<TestArc>): NetworkGraph<TestNode, TestArc> {
        return assertNotNull(NetworkGraph(nodes = nodes.map(::TestNode), arcs = arcs, access = access).value)
    }

    private fun nodesSnapshotMutationCheck(graph: NetworkGraph<TestNode, TestArc>, arc: TestArc, node: TestNode) {
        assertEquals(listOf("s", "t", "u"), graph.nodeIds.map { it.value })
        assertEquals(id("s"), graph.nodeId(node))
        assertEquals(id("s"), graph.from(arc))
        assertEquals(id("t"), graph.to(arc))
    }

    private fun id(value: String) = NetworkNodeId(value)

    private data class TestNode(var id: String)

    private data class TestArc(val id: String, var from: String, var to: String, val cost: Int)

    private val access = NetworkGraphAccess<TestNode, TestArc>(
        nodeId = { id(it.id) },
        arcId = { NetworkArcId(it.id) },
        from = { id(it.from) },
        to = { id(it.to) }
    )
}
