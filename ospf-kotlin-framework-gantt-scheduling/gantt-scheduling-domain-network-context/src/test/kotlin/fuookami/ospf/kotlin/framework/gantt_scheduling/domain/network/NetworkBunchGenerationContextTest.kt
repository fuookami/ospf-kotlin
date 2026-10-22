package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.network

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.framework.model.ColumnGenerationCompletion
import fuookami.ospf.kotlin.framework.model.ColumnGenerationScope
import fuookami.ospf.kotlin.framework.network_scheduling.domain.path.PathProblem
import fuookami.ospf.kotlin.framework.network_scheduling.domain.path.PathSearchBudget
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkArcId
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkGraph
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkGraphAccess
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkNodeId
import fuookami.ospf.kotlin.utils.functional.Ok

class NetworkBunchGenerationContextTest {
    private val scope = ColumnGenerationScope(
        request = "request-1",
        group = "executor-1",
        branch = "root",
        global = true,
        model = "model-1",
        dual = "dual-1"
    )

    @Test
    fun exhaustiveSearchPreservesScopeAndConvertsTheTerminalPath() {
        val graph = graph(withPath = true)
        val context = NetworkBunchGenerationContext()

        val result = context.generate(
            problem = problem(graph),
            budget = PathSearchBudget(),
            scope = scope,
            extract = { label, _ -> Ok(label.state) }
        )

        assertTrue(result is Ok)
        val batch = (result as Ok).value
        assertEquals(listOf("root/target"), batch.columns)
        assertEquals(ColumnGenerationCompletion.Exhausted, batch.completion)
        assertTrue(batch.certifies(scope))
        assertFalse(batch.certifiesNoImprovingColumns(scope))
    }

    @Test
    fun outputLimitPreservesExhaustionButDoesNotInferNoImprovement() {
        val graph = graph(withPath = true)
        val context = NetworkBunchGenerationContext()

        val limited = context.generate(
            problem = improvingProblem(graph),
            budget = PathSearchBudget(),
            scope = scope,
            maximumResults = 0,
            noImprovementEvidence = { search ->
                search.labels.none { it.node == NetworkNodeId("target") && it.state < 0 }
            },
            extract = { label, _ -> Ok(label.state) }
        )
        val rejected = context.generate(
            problem = problem(graph),
            budget = PathSearchBudget(),
            scope = scope,
            extract = { _, _ -> Ok(null) }
        )

        assertTrue(limited is Ok)
        assertEquals(ColumnGenerationCompletion.Exhausted, (limited as Ok).value.completion)
        assertFalse(limited.value.noImprovingColumns == true)
        assertFalse(limited.value.certifiesNoImprovingColumns(scope))
        assertTrue(rejected is Ok)
        assertEquals(ColumnGenerationCompletion.Heuristic, (rejected as Ok).value.completion)
        assertFalse(rejected.value.certifiesNoImprovingColumns(scope))
    }

    @Test
    fun exhaustiveSearchWithNoPathDoesNotInferNoImprovementFromEmptyOutput() {
        val result = NetworkBunchGenerationContext().generate(
            problem = problem(graph(withPath = false)),
            budget = PathSearchBudget(),
            scope = scope,
            maximumResults = 0,
            extract = { label, _ -> Ok(label.state) }
        )

        assertTrue(result is Ok)
        val batch = (result as Ok).value
        assertTrue(batch.columns.isEmpty())
        assertEquals(ColumnGenerationCompletion.Exhausted, batch.completion)
        assertEquals(null, batch.noImprovingColumns)
        assertTrue(batch.certifies(scope))
        assertFalse(batch.certifiesNoImprovingColumns(scope))
    }

    @Test
    fun independentNoImprovementEvidenceCanCertifyAnExhaustiveEmptyDomain() {
        val result = NetworkBunchGenerationContext().generate(
            problem = problem(graph(withPath = false)),
            budget = PathSearchBudget(),
            scope = scope,
            noImprovementEvidence = { search -> search.terminalCount == 0 },
            extract = { label, _ -> Ok(label.state) }
        )

        assertTrue(result is Ok)
        val batch = (result as Ok).value
        assertTrue(batch.columns.isEmpty())
        assertEquals(ColumnGenerationCompletion.Exhausted, batch.completion)
        assertEquals(true, batch.noImprovingColumns)
        assertTrue(batch.certifiesNoImprovingColumns(scope))
    }

    @Test
    fun exhaustiveSearchWithAnImprovingPathDoesNotCertifyNoImprovement() {
        val result = NetworkBunchGenerationContext().generate(
            problem = improvingProblem(graph(withPath = true)),
            budget = PathSearchBudget(),
            scope = scope,
            noImprovementEvidence = { search ->
                search.labels.none { it.node == NetworkNodeId("target") && it.state < 0 }
            },
            extract = { label, _ -> Ok(label.state) }
        )

        assertTrue(result is Ok)
        val batch = (result as Ok).value
        assertEquals(ColumnGenerationCompletion.Exhausted, batch.completion)
        assertEquals(listOf(-1), batch.columns)
        assertEquals(null, batch.noImprovingColumns)
        assertFalse(batch.certifiesNoImprovingColumns(scope))
    }

    @Test
    fun rejectedFullTraceValidationDowngradesTheBatchToHeuristic() {
        val graph = graph(withPath = true)
        val problem = PathProblem(
            graph = graph,
            source = NetworkNodeId("source"),
            initial = "root",
            transition = { previous, arc -> Ok("${previous.state}/${arc.to.name}") },
            terminal = { node, _ -> node == NetworkNodeId("target") },
            validate = { Ok(false) }
        )

        val result = NetworkBunchGenerationContext().generate(
            problem = problem,
            budget = PathSearchBudget(),
            scope = scope,
            noImprovementEvidence = { search -> search.terminalCount == 0 },
            extract = { label, _ -> Ok(label.state) }
        )

        assertTrue(result is Ok)
        val batch = (result as Ok).value
        assertEquals(ColumnGenerationCompletion.Heuristic, batch.completion)
        assertEquals(null, batch.noImprovingColumns)
        assertFalse(batch.certifiesNoImprovingColumns(scope))
    }

    private fun graph(withPath: Boolean): NetworkGraph<TestNode, TestArc> {
        val source = TestNode("source")
        val target = TestNode("target")
        val arcs = if (withPath) listOf(TestArc(source, target)) else emptyList()
        val result = NetworkGraph(
            nodes = listOf(source, target),
            arcs = arcs,
            access = NetworkGraphAccess(
                nodeId = { NetworkNodeId(it.name) },
                arcId = { NetworkArcId("${it.from.name}->${it.to.name}") },
                from = { NetworkNodeId(it.from.name) },
                to = { NetworkNodeId(it.to.name) }
            )
        )
        return (result as Ok).value
    }

    private fun problem(graph: NetworkGraph<TestNode, TestArc>): PathProblem<TestNode, TestArc, String> {
        return PathProblem(
            graph = graph,
            source = NetworkNodeId("source"),
            initial = "root",
            transition = { previous, arc -> Ok("${previous.state}/${arc.to.name}") },
            terminal = { node, _ -> node == NetworkNodeId("target") }
        )
    }

    private fun improvingProblem(graph: NetworkGraph<TestNode, TestArc>): PathProblem<TestNode, TestArc, Int> {
        return PathProblem(
            graph = graph,
            source = NetworkNodeId("source"),
            initial = 0,
            transition = { _, _ -> Ok(-1) },
            terminal = { node, _ -> node == NetworkNodeId("target") },
            accept = { it < 0 }
        )
    }

    private data class TestNode(val name: String)

    private data class TestArc(val from: TestNode, val to: TestNode)
}
