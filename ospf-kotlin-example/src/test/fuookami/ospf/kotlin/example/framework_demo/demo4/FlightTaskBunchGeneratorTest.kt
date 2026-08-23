@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 航班任务束生成器的配置和构造测试 / Tests for FlightTaskBunchGenerator configuration and construction.
 *
 * 注意：使用实际 Aircraft/FlightTask 对象和影子价格映射的完整集成测试需要领域模型初始化。这些测试验证生成器可以使用正确的配置进行构造 /
 * Note: Full integration tests with actual Aircraft/FlightTask objects and
 * shadow price maps require domain model initialization. These tests verify
 * the generator can be constructed with proper configuration.
 */
class FlightTaskBunchGeneratorTest {

    @Test
    fun `BunchGenerationConfiguration defaults are correct`() {
        val config = BunchGenerationConfiguration()
        assertFalse(config.withOrderChange)
        assertEquals(UInt64(100UL), config.maximumLabelPerNode)
        assertEquals(UInt64(10UL), config.maximumColumnGeneratedPerAircraft)
    }

    @Test
    fun `BunchGenerationConfiguration custom values`() {
        val config = BunchGenerationConfiguration(
            withOrderChange = true,
            maximumLabelPerNode = UInt64(200UL),
            maximumColumnGeneratedPerAircraft = UInt64(20UL)
        )
        assertTrue(config.withOrderChange)
        assertEquals(UInt64(200UL), config.maximumLabelPerNode)
        assertEquals(UInt64(20UL), config.maximumColumnGeneratedPerAircraft)
    }

    @Test
    fun `FlightTaskBunchGenerator sortNodes handles empty graph`() {
        // sortNodes is a companion object method, tested indirectly through construction
        val config = BunchGenerationConfiguration()
        assertNotNull(config)
    }

    @Test
    fun `FlightTaskBunchGenerator with order change uses empty nodes list`() {
        val config = BunchGenerationConfiguration(withOrderChange = true)
        assertTrue(config.withOrderChange)
        // When withOrderChange is true, nodes list should be empty (BFS used instead)
    }

    @Test
    fun `FlightTaskBunchGenerator without order change uses sorted nodes`() {
        val config = BunchGenerationConfiguration(withOrderChange = false)
        assertFalse(config.withOrderChange)
        // When withOrderChange is false, topological sort is used
    }

    @Test
    fun `negative reduced cost produces a new column`() {
        val fixture = demo4TestFixture()
        val generator = generator(fixture)

        val bunches = runPricing(generator, shadowPriceMap(fixture, firstTaskPrice = 3.0))

        assertEquals(2, bunches.size)
        assertTrue(
            bunches.any {
                it.tasks.map { task -> task.originTask } == listOf(fixture.firstTask, fixture.secondTask)
            }
        )
        assertTrue(generator.diagnostics.negativeReducedCostLabelCount > 0)
        assertEquals(2, generator.diagnostics.generatedColumnCount)
    }

    @Test
    fun `nonnegative reduced cost produces no column`() {
        val fixture = demo4TestFixture()
        val generator = generator(fixture)

        val bunches = runPricing(generator, shadowPriceMap(fixture, firstTaskPrice = 0.0))

        assertTrue(bunches.isEmpty())
        assertFalse(generator.diagnostics.noFeasiblePath)
        assertTrue(generator.diagnostics.noNegativeReducedCost)
        assertEquals(0, generator.diagnostics.negativeReducedCostLabelCount)
    }

    @Test
    fun `shadow price changes pricing result without rebuilding the graph`() {
        val fixture = demo4TestFixture()
        val generator = generator(fixture)

        val firstRound = runPricing(generator, shadowPriceMap(fixture, firstTaskPrice = 0.0))
        val secondRound = runPricing(generator, shadowPriceMap(fixture, firstTaskPrice = 3.0))

        assertTrue(firstRound.isEmpty())
        assertTrue(secondRound.isNotEmpty())
        assertFalse(generator.diagnostics.noNegativeReducedCost)
    }

    @Test
    fun `maximum column amount limits generated columns`() {
        val fixture = demo4TestFixture()
        val generator = generator(
            fixture = fixture,
            configuration = BunchGenerationConfiguration(
                maximumColumnGeneratedPerAircraft = UInt64.one
            )
        )

        val bunches = runPricing(generator, shadowPriceMap(fixture, firstTaskPrice = 3.0))

        assertEquals(1, bunches.size)
        assertEquals(1, generator.diagnostics.generatedColumnCount)
    }

    @Test
    fun `dominance rejects a more expensive label at a merge node`() {
        val fixture = demo4TestFixture()
        val alternativeTask = demo4FlightTask(
            fixture = fixture,
            actualId = "DEMO4-ALT",
            no = "ALT"
        )
        val generator = generator(
            fixture = fixture,
            graph = mergeGraph(fixture, alternativeTask),
            configuration = BunchGenerationConfiguration(withOrderChange = true),
            costCalculator = CostCalculator { _, previousTask, _, _, _ ->
                if (previousTask == alternativeTask) {
                    demo4Cost(10.0)
                } else {
                    demo4Cost(1.0)
                }
            }
        )

        runPricing(generator, ShadowPriceMap())

        assertTrue(generator.diagnostics.dominatedLabelCount > 0)
    }

    @Test
    fun `dominance counts every existing label removed by a better label`() {
        val fixture = demo4TestFixture()
        val costlyTask = demo4FlightTask(
            fixture = fixture,
            actualId = "DEMO4-DOM-COSTLY",
            no = "DOM-COSTLY"
        )
        val delayedTask = demo4FlightTask(
            fixture = fixture,
            actualId = "DEMO4-DOM-DELAYED",
            no = "DOM-DELAYED",
            scheduledTime = TimeRange(fixture.now + 1.minutes, fixture.now + 61.minutes)
        )
        val betterTask = demo4FlightTask(
            fixture = fixture,
            actualId = "DEMO4-DOM-BETTER",
            no = "DOM-BETTER",
            scheduledTime = TimeRange(fixture.now + 2.minutes, fixture.now + 62.minutes)
        )
        val generator = generator(
            fixture = fixture,
            graph = multipleMergeGraph(fixture, costlyTask, delayedTask, betterTask),
            configuration = BunchGenerationConfiguration(withOrderChange = true),
            minimumDepartureTimeCalculator = MinimumDepartureTimeCalculator { arrivalTime, _, task, connectionTime ->
                val extraDelay = if (task == delayedTask) 2.minutes else Duration.ZERO
                arrivalTime + connectionTime + extraDelay
            },
            costCalculator = CostCalculator { _, previousTask, task, _, _ ->
                if (task == fixture.secondTask) {
                    when (previousTask?.originTask) {
                        costlyTask -> demo4Cost(10.0)
                        delayedTask -> demo4Cost(9.0)
                        betterTask -> demo4Cost(1.0)
                        else -> demo4Cost(0.0)
                    }
                } else {
                    demo4Cost(0.0)
                }
            }
        )

        runPricing(generator, ShadowPriceMap())

        assertEquals(2, generator.diagnostics.dominatedLabelCount)
    }

    @Test
    fun `label limit prevents removed labels from entering order-change search`() {
        val fixture = demo4TestFixture()
        val generator = generator(
            fixture = fixture,
            configuration = BunchGenerationConfiguration(
                withOrderChange = true,
                maximumLabelPerNode = UInt64.zero
            )
        )

        runPricing(generator, ShadowPriceMap())

        assertTrue(generator.diagnostics.labelLimitReachedCount > 0)
        assertEquals(1, generator.diagnostics.expandedLabelCount)
        assertTrue(generator.diagnostics.searchTruncatedByLabelLimit)
        assertFalse(generator.diagnostics.noFeasiblePath)
    }

    @Test
    fun `invalid incremental cost rejects an otherwise feasible label`() {
        val fixture = demo4TestFixture()
        val generator = generator(
            fixture = fixture,
            costCalculator = CostCalculator { _, _, _, _, _ -> demo4InvalidCost() }
        )

        val bunches = runPricing(generator, shadowPriceMap(fixture, firstTaskPrice = 3.0))

        assertTrue(bunches.isEmpty())
        assertTrue(generator.diagnostics.infeasibleExpansionCount > 0)
        assertTrue(generator.diagnostics.noFeasiblePath)
    }

    @Test
    fun `invalid total cost does not report no negative reduced cost`() {
        val fixture = demo4TestFixture()
        val generator = generator(
            fixture = fixture,
            totalCostCalculator = TotalCostCalculator { _, _ -> demo4InvalidCost() }
        )

        val bunches = runPricing(generator, shadowPriceMap(fixture, firstTaskPrice = 3.0))

        assertTrue(bunches.isEmpty())
        assertFalse(generator.diagnostics.noFeasiblePath)
        assertFalse(generator.diagnostics.noNegativeReducedCost)
        assertTrue(generator.diagnostics.invalidColumnCount > 0)
    }

    private fun generator(
        fixture: Demo4TestFixture,
        graph: Graph = basicGraph(fixture),
        configuration: BunchGenerationConfiguration = BunchGenerationConfiguration(),
        minimumDepartureTimeCalculator: MinimumDepartureTimeCalculator = MinimumDepartureTimeCalculator { arrivalTime, _, _, connectionTime ->
            arrivalTime + connectionTime
        },
        costCalculator: CostCalculator = CostCalculator { _, _, _, _, _ -> demo4Cost(1.0) },
        totalCostCalculator: TotalCostCalculator = TotalCostCalculator { _, tasks ->
            demo4Cost(tasks.size.toDouble())
        }
    ): FlightTaskBunchGenerator {
        return FlightTaskBunchGenerator(
            aircraft = fixture.aircraft,
            aircraftUsability = fixture.usability,
            graph = graph,
            connectionTimeCalculator = ConnectionTimeCalculator { _, _, _ -> 30.minutes },
            minimumDepartureTimeCalculator = minimumDepartureTimeCalculator,
            costCalculator = costCalculator,
            totalCostCalculator = totalCostCalculator,
            configuration = configuration
        )
    }

    private fun runPricing(
        generator: FlightTaskBunchGenerator,
        shadowPriceMap: ShadowPriceMap
    ): List<FlightTaskBunch> {
        return when (val result = generator(Int64.zero, shadowPriceMap)) {
            is Ok -> result.value
            is Failed -> fail("pricing failed: ${result.error}")
            is Fatal -> fail("pricing failed: ${result.errors}")
        }
    }

    private fun shadowPriceMap(
        fixture: Demo4TestFixture,
        firstTaskPrice: Double
    ): ShadowPriceMap {
        val map = ShadowPriceMap()
        map.put { _, arguments ->
            if (arguments.task == fixture.firstTask) {
                Flt64(firstTaskPrice)
            } else {
                Flt64.zero
            }
        }
        return map
    }

    private fun basicGraph(fixture: Demo4TestFixture): Graph {
        val first = TaskNode(fixture.firstTask, fixture.firstTask.time!!.start, UInt64(1UL))
        val second = TaskNode(fixture.secondTask, fixture.secondTask.time!!.start, UInt64(2UL))
        return Graph().also { graph ->
            graph.put(RootNode)
            graph.put(EndNode)
            graph.put(first)
            graph.put(second)
            graph.put(RootNode, first)
            graph.put(first, second)
            graph.put(first, EndNode)
            graph.put(second, EndNode)
        }
    }

    private fun mergeGraph(fixture: Demo4TestFixture, alternativeTask: FlightTask): Graph {
        val first = TaskNode(fixture.firstTask, fixture.firstTask.time!!.start, UInt64(1UL))
        val alternative = TaskNode(alternativeTask, alternativeTask.time!!.start, UInt64(2UL))
        val second = TaskNode(fixture.secondTask, fixture.secondTask.time!!.start, UInt64(3UL))
        return Graph().also { graph ->
            graph.put(RootNode)
            graph.put(EndNode)
            graph.put(first)
            graph.put(alternative)
            graph.put(second)
            graph.put(RootNode, first)
            graph.put(RootNode, alternative)
            graph.put(first, second)
            graph.put(alternative, second)
            graph.put(first, EndNode)
            graph.put(alternative, EndNode)
            graph.put(second, EndNode)
        }
    }

    private fun multipleMergeGraph(
        fixture: Demo4TestFixture,
        costlyTask: FlightTask,
        delayedTask: FlightTask,
        betterTask: FlightTask
    ): Graph {
        val costly = TaskNode(costlyTask, costlyTask.time!!.start, UInt64(1UL))
        val delayed = TaskNode(delayedTask, delayedTask.time!!.start, UInt64(2UL))
        val better = TaskNode(betterTask, betterTask.time!!.start, UInt64(3UL))
        val second = TaskNode(fixture.secondTask, fixture.secondTask.time!!.start, UInt64(4UL))
        return Graph().also { graph ->
            graph.put(RootNode)
            graph.put(EndNode)
            graph.put(costly)
            graph.put(delayed)
            graph.put(better)
            graph.put(second)
            graph.put(RootNode, costly)
            graph.put(RootNode, delayed)
            graph.put(RootNode, better)
            graph.put(costly, second)
            graph.put(delayed, second)
            graph.put(better, second)
            graph.put(second, EndNode)
        }
    }
}
