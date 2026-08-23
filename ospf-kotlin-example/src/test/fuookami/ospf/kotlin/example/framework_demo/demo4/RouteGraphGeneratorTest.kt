@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 路线图生成器配置及 Configuration 数据类的测试 / Tests for RouteGraphGenerator configuration and Configuration data class.
 *
 * 注意：使用实际 Aircraft/FlightTask 对象的完整集成测试需要领域模型初始化。这些测试验证配置结构 /
 * Note: Full integration tests with actual Aircraft/FlightTask objects require
 * domain model initialization. These tests verify the configuration structure.
 */
class RouteGraphGeneratorTest {

    @Test
    fun `Configuration default has withOrderChange false`() {
        val config = Configuration()
        assertFalse(config.withOrderChange)
    }

    @Test
    fun `Configuration withOrderChange can be set to true`() {
        val config = Configuration(withOrderChange = true)
        assertTrue(config.withOrderChange)
    }

    @Test
    fun `RouteGraphGenerator can be constructed with Configuration`() {
        val reverse = FlightTaskReverse.invoke(
            pairs = emptyList(),
            originBunches = emptyList(),
            lock = fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.Lock(),
            timeDifferenceLimit = FlightTaskReverse.defaultTimeDifferenceLimit
        )
        val config = Configuration(withOrderChange = false)
        val judger: (fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.Aircraft,
            fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.FlightTask?,
            fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.FlightTask) -> Boolean = { _, _, _ -> true }

        val generator = RouteGraphGenerator(reverse, config, judger)
        assertNotNull(generator)
    }

    @Test
    fun `feasible task transitions create route graph edges`() {
        val fixture = demo4TestFixture()
        val graph = generate(fixture = fixture, withOrderChange = false)
        val first = taskNode(graph, fixture.firstTask)
        val second = taskNode(graph, fixture.secondTask)

        assertTrue(graph.connected(RootNode, first))
        assertTrue(graph.connected(first, second))
        assertTrue(graph.connected(second, EndNode))
    }

    @Test
    fun `infeasible task transitions are excluded and terminate at end`() {
        val fixture = demo4TestFixture()
        val graph = generate(fixture = fixture, withOrderChange = false) { previous, task ->
            previous == null || task != fixture.secondTask
        }
        val first = taskNode(graph, fixture.firstTask)

        assertFalse(graph.nodes.values.filterIsInstance<TaskNode>().any { it.task == fixture.secondTask })
        assertTrue(graph.connected(first, EndNode))
    }

    @Test
    fun `task without successors connects directly to end`() {
        val fixture = demo4TestFixture()
        val graph = generate(
            fixture = fixture,
            withOrderChange = false,
            tasks = listOf(fixture.firstTask)
        )
        val first = taskNode(graph, fixture.firstTask)

        assertTrue(graph.connected(RootNode, first))
        assertTrue(graph.connected(first, EndNode))
    }

    @Test
    fun `withOrderChange creates reverse transition when normal transition is infeasible`() {
        val fixture = demo4TestFixture()
        val (laterTask, earlierTask) = demo4ReverseTaskPair(fixture)
        val reverse = FlightTaskReverse.invoke(
            pairs = listOf(earlierTask to laterTask),
            originBunches = emptyList(),
            lock = Lock(),
            timeDifferenceLimit = FlightTaskReverse.defaultTimeDifferenceLimit
        )
        val feasibleInTimeOrder: (FlightTask?, FlightTask) -> Boolean = { previous, task ->
            previous == null || previous.time!!.start < task.time!!.start
        }

        val normalGraph = generate(
            fixture = fixture,
            withOrderChange = false,
            tasks = listOf(laterTask, earlierTask),
            reverse = reverse,
            feasible = feasibleInTimeOrder
        )
        val orderChangeGraph = generate(
            fixture = fixture,
            withOrderChange = true,
            tasks = listOf(laterTask, earlierTask),
            reverse = reverse,
            feasible = feasibleInTimeOrder
        )
        val laterOrderChange = taskNode(orderChangeGraph, laterTask)
        val earlierOrderChange = taskNode(orderChangeGraph, earlierTask)

        assertFalse(normalGraph.nodes.values.filterIsInstance<TaskNode>().any { it.task == earlierTask })
        assertTrue(orderChangeGraph.connected(laterOrderChange, earlierOrderChange))
    }

    private fun generate(
        fixture: Demo4TestFixture,
        withOrderChange: Boolean,
        tasks: List<FlightTask> = listOf(fixture.firstTask, fixture.secondTask),
        reverse: FlightTaskReverse = FlightTaskReverse.invoke(
            pairs = emptyList(),
            originBunches = emptyList(),
            lock = Lock(),
            timeDifferenceLimit = FlightTaskReverse.defaultTimeDifferenceLimit
        ),
        feasible: (FlightTask?, FlightTask) -> Boolean = { _, _ -> true }
    ): Graph {
        val generator = RouteGraphGenerator(
            reverse = reverse,
            configuration = Configuration(withOrderChange = withOrderChange),
            feasibilityJudger = { _, previous, task -> feasible(previous, task) }
        )
        return when (val result = generator(fixture.aircraft, fixture.usability, tasks.groupBy { it.dep })) {
            is Ok -> result.value
            is Failed -> fail("route graph generation failed: ${result.error}")
            is Fatal -> fail("route graph generation failed: ${result.errors}")
        }
    }

    private fun taskNode(graph: Graph, task: FlightTask): TaskNode {
        return graph.nodes.values.filterIsInstance<TaskNode>().single { it.task == task }
    }
}
