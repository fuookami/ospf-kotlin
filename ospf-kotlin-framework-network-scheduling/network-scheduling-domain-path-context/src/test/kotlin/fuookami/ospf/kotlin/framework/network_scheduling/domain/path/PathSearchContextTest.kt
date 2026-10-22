package fuookami.ospf.kotlin.framework.network_scheduling.domain.path

import kotlin.random.Random
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

class PathSearchContextTest {
    @Test
    fun dagSearchShouldMatchExhaustiveSimplePathOracle() {
        val random = Random(9127)
        repeat(8) { sample ->
            val nodes = (0..5).map { "n$it" }
            val arcs = mutableListOf<PathArc>()
            for (from in 0 until nodes.lastIndex) {
                for (to in (from + 1)..nodes.lastIndex) {
                    if (random.nextInt(4) == 0) continue
                    repeat(if (random.nextInt(6) == 0) 2 else 1) { parallel ->
                        arcs.add(PathArc("s$sample-$from-$to-$parallel", nodes[from], nodes[to]))
                    }
                }
            }
            val graph = graphOf(nodes, arcs)
            for (sourceIndex in nodes.indices) {
                for (targetIndex in sourceIndex until nodes.size) {
                    val source = nodeId(nodes[sourceIndex])
                    val target = nodeId(nodes[targetIndex])
                    val expected = enumeratePaths(nodes[sourceIndex], nodes[targetIndex], arcs)
                    val problem = PathProblem(
                        graph = graph,
                        source = source,
                        initial = emptyList<String>(),
                        transition = { previous, arc -> ok(previous.state + arc.id) },
                        terminal = { node, _ -> node == target }
                    )
                    val result = assertNotNull(PathSearchContext().search(problem).value)
                    val actual = result.paths.map { path -> result.trace(path).drop(1).mapNotNull { it.incoming?.id } }.toSet()

                    assertTrue(result.complete)
                    assertEquals(expected.toSet(), actual)
                }
            }
        }
    }

    @Test
    fun dominanceShouldRespectElementaryAccessHistoryAndNonMonotoneState() {
        val arcs = listOf(
            PathArc("sa", "s", "a"),
            PathArc("sb", "s", "b"),
            PathArc("ax", "a", "x"),
            PathArc("bx", "b", "x"),
            PathArc("xt", "x", "t")
        )
        val graph = graphOf(listOf("s", "a", "b", "x", "t"), arcs)
        val problem = PathProblem(
            graph = graph,
            source = nodeId("s"),
            initial = 0,
            transition = { previous, arc ->
                ok(
                    previous.state + when (arc.id) {
                        "sa" -> 10
                        "ax" -> -10
                        "sb" -> 2
                        "bx" -> -2
                        else -> 1
                    }
                )
            },
            terminal = { node, _ -> node == nodeId("t") },
            dominates = { _, _ -> true },
            dominanceIsExact = true,
            order = compareBy { it }
        )

        val result = assertNotNull(PathSearchContext().search(problem).value)
        val paths = result.paths.map { path ->
            result.trace(path).drop(1).mapNotNull { it.incoming?.id }
        }.toSet()

        assertEquals(setOf(listOf("sa", "ax", "xt"), listOf("sb", "bx", "xt")), paths)
        assertEquals(setOf(1), result.paths.map { it.state }.toSet())
        assertTrue(result.complete)
        assertEquals(0, result.dominatedLabels)
    }

    @Test
    fun untriggeredUncertifiedDominanceShouldNotMarkSearchIncomplete() {
        val graph = graphOf(listOf("s", "a", "t"), listOf(PathArc("sa", "s", "a"), PathArc("at", "a", "t")))
        val problem = PathProblem(
            graph = graph,
            source = nodeId("s"),
            initial = 0,
            transition = { previous, _ -> ok(previous.state + 1) },
            terminal = { node, _ -> node == nodeId("t") },
            dominates = { _, _ -> false }
        )

        val result = assertNotNull(PathSearchContext().search(problem).value)
        assertTrue(result.complete)
        assertFalse(result.heuristicDominanceUsed)
        assertEquals(PathSearchCompletion.Exhausted, result.completion)
    }

    @Test
    fun uncertifiedDominanceThatActuallyPrunesShouldBeReported() {
        val graph = graphOf(
            listOf("s", "a", "t"),
            listOf(
                PathArc("sa-fast", "s", "a"),
                PathArc("sa-slow", "s", "a"),
                PathArc("at", "a", "t")
            )
        )
        val problem = PathProblem(
            graph = graph,
            source = nodeId("s"),
            initial = 0,
            transition = { previous, _ -> ok(previous.state) },
            terminal = { node, _ -> node == nodeId("t") },
            dominates = { _, _ -> true }
        )

        val result = assertNotNull(PathSearchContext().search(problem).value)
        assertEquals(PathSearchCompletion.UncertifiedDominance, result.completion)
        assertTrue(result.heuristicDominanceUsed)
        assertFalse(result.complete)
    }

    @Test
    fun terminalLabelsShouldNotDominateLaterExpandableLabelsAtTheSameNode() {
        val graph = graphOf(
            listOf("s", "a", "b", "c", "x", "t"),
            listOf(
                PathArc("sa", "s", "a"),
                PathArc("ax", "a", "x"),
                PathArc("sb", "s", "b"),
                PathArc("bc", "b", "c"),
                PathArc("cx", "c", "x"),
                PathArc("xt", "x", "t")
            )
        )
        val problem = PathProblem(
            graph = graph,
            source = nodeId("s"),
            initial = 0,
            transition = { previous, arc -> ok(if (arc.id == "cx") 1 else previous.state) },
            terminal = { node, state -> (node == nodeId("x") && state == 0) || node == nodeId("t") },
            elementary = false,
            dominates = { first, second -> first.state <= second.state },
            dominanceIsExact = true
        )

        val result = assertNotNull(PathSearchContext().search(problem).value)
        val terminalPaths = result.paths.map { path ->
            result.trace(path).mapNotNull { label -> label.incoming?.id } to path.state
        }.toSet()

        assertEquals(setOf(listOf("sa", "ax") to 0, listOf("sb", "bc", "cx", "xt") to 1), terminalPaths)
        assertTrue(result.complete)
    }

    @Test
    fun startTransitionAndTerminalContributionsShouldBeAppliedInOrder() {
        val graph = graphOf(listOf("s", "t"), listOf(PathArc("st", "s", "t", cost = 3)))
        val problem = PathProblem(
            graph = graph,
            source = nodeId("s"),
            initial = 2,
            transition = { previous, arc -> ok(previous.state + arc.cost) },
            terminal = { node, _ -> node == nodeId("t") },
            startContribution = { source, state -> ok(if (source == nodeId("s")) state + 10 else null) },
            terminalContribution = { label -> ok(label.state + 100) }
        )

        val result = assertNotNull(PathSearchContext().search(problem).value)
        val path = result.paths.single()

        assertEquals(115, path.state)
        assertEquals(listOf(12, 115), result.trace(path).map { it.state })

        val noStartProblem = PathProblem(
            graph = graph,
            source = nodeId("s"),
            initial = 2,
            transition = problem.transition,
            terminal = problem.terminal,
            startContribution = { _, _ -> ok(null) }
        )
        val noStart = PathSearchContext().search(noStartProblem)
        assertTrue(noStart.ok)
        assertTrue(assertNotNull(noStart.value).complete)
        assertTrue(assertNotNull(noStart.value).paths.isEmpty())

        val noTerminal = PathSearchContext().search(
            PathProblem(
                graph = graph,
                source = nodeId("s"),
                initial = 2,
                transition = problem.transition,
                terminal = problem.terminal,
                terminalContribution = { ok(null) }
            )
        )
        assertTrue(noTerminal.ok)
        assertTrue(assertNotNull(noTerminal.value).complete)
        assertTrue(assertNotNull(noTerminal.value).paths.isEmpty())
        assertEquals(0, assertNotNull(noTerminal.value).terminalCount)
    }

    @Test
    fun independentTraceValidationShouldFilterWithoutClaimingExhaustion() {
        val graph = graphOf(
            listOf("s", "a", "b", "t"),
            listOf(
                PathArc("sa", "s", "a"),
                PathArc("at", "a", "t"),
                PathArc("sb", "s", "b"),
                PathArc("bt", "b", "t")
            )
        )
        val validatedTraces = mutableListOf<List<String?>>()
        val problem = PathProblem(
            graph = graph,
            source = nodeId("s"),
            initial = emptyList<String>(),
            transition = { previous, arc -> ok(previous.state + arc.id) },
            terminal = { node, _ -> node == nodeId("t") },
            terminalContribution = { ok(it.state + "terminal") },
            validate = { trace ->
                val arcIds = trace.drop(1).map { it.incoming?.id }
                validatedTraces.add(arcIds)
                assertNull(trace.first().incoming)
                assertEquals(trace.drop(1).map { it.incoming?.id }, trace.last().state.dropLast(1))
                assertEquals("terminal", trace.last().state.last())
                ok(arcIds != listOf("sb", "bt"))
            }
        )

        val result = assertNotNull(PathSearchContext().search(problem).value)

        assertEquals(setOf(listOf("sa", "at"), listOf("sb", "bt")), validatedTraces.map { it.filterNotNull() }.toSet())
        assertEquals(PathSearchCompletion.UncertifiedValidation, result.completion)
        assertFalse(result.complete)
        assertEquals(2, result.terminalCount)
        assertEquals(setOf(listOf("sa", "at", "terminal")), result.paths.map { it.state }.toSet())
    }

    @Test
    fun traceValidationFailuresShouldPropagateAsRetErrors() {
        val graph = graphOf(listOf("s", "t"), listOf(PathArc("st", "s", "t")))
        val problem = PathProblem(
            graph = graph,
            source = nodeId("s"),
            initial = 0,
            transition = { previous, _ -> ok(previous.state + 1) },
            terminal = { node, _ -> node == nodeId("t") },
            validate = { networkSchedulingFailure("路径复核失败 / Trace validation failed") }
        )

        assertTrue(PathSearchContext().search(problem).failed)
    }

    @Test
    fun filtersAndReversedGraphShouldControlPathTraversal() {
        val graph = graphOf(
            listOf("s", "a", "t"),
            listOf(
                PathArc("sa", "s", "a"),
                PathArc("at", "a", "t"),
                PathArc("st", "s", "t")
            )
        )
        val forward = PathProblem(
            graph = graph,
            source = nodeId("s"),
            initial = emptyList<String>(),
            transition = { previous, arc -> ok(previous.state + arc.id) },
            terminal = { node, _ -> node == nodeId("t") },
            allowedArc = { it.id == "st" },
            allowedNode = { it != nodeId("a") }
        )
        val reverse = PathProblem(
            graph = graph.reverse(),
            source = nodeId("t"),
            initial = emptyList<String>(),
            transition = { previous, arc -> ok(previous.state + arc.id) },
            terminal = { node, _ -> node == nodeId("s") },
            allowedArc = { it.id == "st" },
            allowedNode = { it != nodeId("a") }
        )

        val forwardPaths = assertNotNull(PathSearchContext().search(forward).value)
        val reversePaths = assertNotNull(PathSearchContext().search(reverse).value)

        assertEquals(setOf(listOf("st")), forwardPaths.paths.map { forwardPaths.trace(it).drop(1).mapNotNull { label -> label.incoming?.id } }.toSet())
        assertEquals(setOf(listOf("st")), reversePaths.paths.map { reversePaths.trace(it).drop(1).mapNotNull { label -> label.incoming?.id } }.toSet())
    }

    @Test
    fun resultLimitDoesNotTruncateSearchButBudgetDoes() {
        val graph = graphOf(
            listOf("s", "a", "b", "t"),
            listOf(
                PathArc("sa", "s", "a"),
                PathArc("sb", "s", "b"),
                PathArc("at", "a", "t"),
                PathArc("bt", "b", "t")
            )
        )
        val problem = pathProblem(graph)
        val limitedOutput = assertNotNull(PathSearchContext().search(problem, maximumResults = 1).value)
        val noOutput = assertNotNull(PathSearchContext().search(problem, maximumResults = 0).value)
        val budgetStopped = assertNotNull(
            PathSearchContext().search(problem, budget = PathSearchBudget(maximumLabels = 1)).value
        )
        val interrupted = assertNotNull(
            PathSearchContext().search(problem, budget = PathSearchBudget(shouldStop = { true })).value
        )

        assertEquals(2, limitedOutput.terminalCount)
        assertEquals(1, limitedOutput.paths.size)
        assertTrue(limitedOutput.complete)
        assertTrue(noOutput.paths.isEmpty())
        assertEquals(2, noOutput.terminalCount)
        assertTrue(noOutput.complete)
        assertTrue(budgetStopped.paths.isEmpty())
        assertEquals(PathSearchCompletion.BudgetLimit, budgetStopped.completion)
        assertFalse(budgetStopped.complete)
        assertTrue(interrupted.paths.isEmpty())
        assertEquals(PathSearchCompletion.Interrupted, interrupted.completion)
        assertFalse(interrupted.complete)

        val mergeGraph = graphOf(
            listOf("s", "a", "b", "x", "t"),
            listOf(
                PathArc("sa", "s", "a"),
                PathArc("sb", "s", "b"),
                PathArc("ax", "a", "x"),
                PathArc("bx", "b", "x"),
                PathArc("xt", "x", "t")
            )
        )
        val perNodeBudget = assertNotNull(
            PathSearchContext().search(
                pathProblem(mergeGraph),
                budget = PathSearchBudget(maximumLabelsPerNode = 1)
            ).value
        )
        assertEquals(PathSearchCompletion.BudgetLimit, perNodeBudget.completion)
        assertFalse(perNodeBudget.complete)
    }

    @Test
    fun cyclicWalksRequireFiniteBoundsAndDagsCanBeSearchedWithoutThem() {
        val cycle = graphOf(
            listOf("s", "t"),
            listOf(PathArc("loop", "s", "s"), PathArc("st", "s", "t"))
        )
        val unbounded = pathProblem(cycle, elementary = false)
        assertTrue(PathSearchContext().search(unbounded).failed)

        val bounded = PathSearchContext().search(pathProblem(cycle, elementary = false, maximumSteps = 2))
        val boundedResult = assertNotNull(bounded.value)
        assertTrue(boundedResult.complete)
        assertEquals(2, boundedResult.paths.size)

        val dag = graphOf(listOf("s", "t"), listOf(PathArc("st", "s", "t")))
        assertTrue(PathSearchContext().search(pathProblem(dag, elementary = false)).ok)
    }

    @Test
    fun dagTraversalShouldExpandLabelsInTopologicalOrder() {
        val graph = graphOf(
            listOf("s", "w", "a", "x", "b", "t"),
            listOf(
                PathArc("sa", "s", "a"),
                PathArc("ab", "a", "b"),
                PathArc("bw", "b", "w"),
                PathArc("sx", "s", "x"),
                PathArc("wx", "w", "x"),
                PathArc("xt", "x", "t")
            )
        )
        val expandedNodes = mutableListOf<String>()
        val problem = PathProblem(
            graph = graph,
            source = nodeId("s"),
            initial = emptyList<String>(),
            transition = { previous, arc ->
                expandedNodes.add(previous.node.value)
                ok(previous.state + arc.id)
            },
            terminal = { node, _ -> node == nodeId("t") }
        )

        val result = assertNotNull(PathSearchContext().search(problem).value)

        assertTrue(result.complete)
        assertTrue(expandedNodes.indexOf("b") < expandedNodes.indexOf("x"))
    }

    private fun pathProblem(
        graph: NetworkGraph<PathNode, PathArc>,
        elementary: Boolean = true,
        maximumSteps: Int? = null
    ): PathProblem<PathNode, PathArc, Int> {
        return PathProblem(
            graph = graph,
            source = nodeId("s"),
            initial = 0,
            transition = { previous, _ -> ok(previous.state + 1) },
            terminal = { node, _ -> node == nodeId("t") },
            elementary = elementary,
            maximumSteps = maximumSteps
        )
    }

    private fun enumeratePaths(source: String, target: String, arcs: List<PathArc>): List<List<String>> {
        val result = mutableListOf<List<String>>()
        fun visit(node: String, path: List<String>) {
            if (node == target) {
                result.add(path)
                return
            }
            for (arc in arcs.filter { it.from == node }) visit(arc.to, path + arc.id)
        }
        visit(source, emptyList())
        return result
    }

    private fun graphOf(nodes: List<String>, arcs: List<PathArc>): NetworkGraph<PathNode, PathArc> {
        return assertNotNull(NetworkGraph(nodes = nodes.map(::PathNode), arcs = arcs, access = access).value)
    }

    private fun nodeId(value: String) = NetworkNodeId(value)

    private data class PathNode(val id: String)

    private data class PathArc(val id: String, val from: String, val to: String, val cost: Int = 1)

    private val access = NetworkGraphAccess<PathNode, PathArc>(
        nodeId = { nodeId(it.id) },
        arcId = { NetworkArcId(it.id) },
        from = { nodeId(it.from) },
        to = { nodeId(it.to) }
    )
}
