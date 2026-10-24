package fuookami.ospf.kotlin.example.framework_demo.demo1

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.Aggregation

class BandwidthPreparationTest {
    @Test
    fun `indexed DFS preserves paths and occurrence order on cyclic multigraphs`() {
        val random = Random(61024)
        repeat(40) {
            AutoIndexed.flush<Node>()
            AutoIndexed.flush<Edge>()
            val nodes: List<Node> = List(5) { NormalNode(UInt64(it.toULong())) } +
                List(2) { ClientNode(UInt64((it + 5).toULong()), UInt64.one) }
            val edges = List(15) {
                Edge(
                    from = nodes[random.nextInt(nodes.size)],
                    to = nodes[random.nextInt(nodes.size)],
                    maxBandwidth = UInt64.one,
                    costPerBandwidth = UInt64.one
                ) to UInt64.one
            }.let { it + it.take(3) }
            val expected = ArrayList<List<Node>>()
            fun visit(current: Node, path: List<Node>) {
                if (current is ClientNode) {
                    expected.add(path)
                } else {
                    for ((edge, _) in edges) {
                        if (edge.from == current && edge.to !in path) {
                            visit(edge.to, path + edge.to)
                        }
                    }
                }
            }
            for ((edge, _) in edges) {
                if (edge.from == nodes.first()) {
                    visit(edge.to, listOf(nodes.first(), edge.to))
                }
            }
            val actual = ArrayList<List<Node>>()
            appendServicePaths(
                firstNode = nodes.first(),
                edges = edges,
                links = actual
            )
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `bandwidth adjacency retains self loops parallel occurrences and isolated rows`() {
        AutoIndexed.flush<Node>()
        AutoIndexed.flush<Edge>()
        AutoIndexed.flush<Service>()
        val nodes = arrayListOf<Node>(NormalNode(UInt64.zero), NormalNode(UInt64.one), ClientNode(UInt64.two, UInt64.one))
        val edges = arrayListOf(
            Edge(
                from = nodes[0],
                to = nodes[0],
                maxBandwidth = UInt64.one,
                costPerBandwidth = UInt64.one
            ),
            Edge(
                from = nodes[0],
                to = nodes[2],
                maxBandwidth = UInt64.one,
                costPerBandwidth = UInt64.one
            )
        )
        val services = listOf(
            Service(id = UInt64.zero, capacity = UInt64.one, cost = UInt64.one),
            Service(id = UInt64.one, capacity = UInt64.one, cost = UInt64.one)
        )
        val graph = Graph(nodes, edges)
        val bandwidth = EdgeBandwidth(edges, services)
        val model = LinearMetaModel(name = "bandwidth-index-test", converter = IntoValue.Identity)
        assertTrue(bandwidth.register(model) is Ok)
        edges.add(edges.first())
        val rows = ServiceBandwidth(
            graph = graph,
            services = services,
            edgeBandwidth = bandwidth
        )
        assertTrue(rows.register(model) is Ok)
        for (node in nodes) {
            for (service in services) {
                val expectedIn = edges.filter { it.to == node }.map { Flt64.one * bandwidth.y[it, service] }
                val expectedOut = if (node is NormalNode) {
                    edges.filter { it.from == node }.map { Flt64.one * bandwidth.y[it, service] }
                } else {
                    emptyList()
                }
                assertEquals(expectedIn, rows.inDegree[node, service].polynomial.monomials)
                assertEquals(expectedOut, rows.outDegree[node, service].polynomial.monomials)
            }
        }
        val assignment = Assignment(nodes, services)
        assertTrue(assignment.register(model) is Ok)
        val solution = MutableList(model.tokens.tokensInSolver.size) { Flt64.zero }
        solution[model.tokens.find(assignment.x[nodes[0], services[0]])!!.solverIndex] = Flt64.one
        solution[model.tokens.find(bandwidth.y[edges[0], services[0]])!!.solverIndex] = Flt64.one
        solution[model.tokens.find(bandwidth.y[edges[1], services[0]])!!.solverIndex] = Flt64.one
        val analyzer = SolutionAnalyzer(
            graph = graph,
            services = services,
            assignment = assignment,
            aggregation = Aggregation(
                edgeBandwidth = bandwidth,
                serviceBandwidth = rows,
                nodeBandwidth = NodeBandwidth(
                    nodes = nodes,
                    services = services,
                    serviceBandwidth = rows
                )
            )
        )
        val result = analyzer(model, solution)
        assertTrue(result is Ok)
        assertEquals(listOf(listOf(nodes[0], nodes[0], nodes[2]), listOf(nodes[0], nodes[2])), (result as Ok).value)
    }
}
