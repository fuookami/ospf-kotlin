@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.model.ColumnGenerationCompletion
import fuookami.ospf.kotlin.framework.model.ColumnGenerationScope
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 批次生成上下文行为测试。Behavior tests for the bunch generation context. */
class BunchGenerationContextTest {

    @Test
    fun `init builds reverse graph and initial bunches`() {
        val fixture = demo4TestFixture()
        val context = initializedContext(fixture)

        assertNotNull(context.reverse)
        assertEquals(1, context.graphs.size)
        assertTrue(context.graphs[fixture.aircraft]!!.nodes.values.any { node ->
            node is TaskNode && node.task == fixture.firstTask
        })
        assertTrue(context.initialFlightBunches.any { it.contains(fixture.firstTask) })
        assertEquals(1, context.routeGraphDiagnostics.size)
        assertTrue(context.routeGraphDiagnostics[fixture.aircraft]!!.candidateTaskCount >= 1)
    }

    @Test
    fun `generate pricing reuses the initialized static graph`() {
        val fixture = demo4TestFixture()
        val context = initializedContext(fixture)
        val graph = context.graphs[fixture.aircraft]

        val negative = context.generateFlightTaskBunch(
            aircrafts = listOf(fixture.aircraft),
            iteration = Int64.zero,
            shadowPriceMap = shadowPriceMap(fixture, 3.0)
        )
        val nonNegative = context.generateFlightTaskBunch(
            aircrafts = listOf(fixture.aircraft),
            iteration = Int64.one,
            shadowPriceMap = shadowPriceMap(fixture, -100.0)
        )

        assertTrue(negative is Ok)
        assertTrue((negative as Ok).value.isNotEmpty())
        assertTrue(nonNegative is Ok)
        assertTrue((nonNegative as Ok).value.isEmpty())
        assertSame(graph, context.graphs[fixture.aircraft])
        assertEquals(1, context.pricingDiagnostics.size)
        assertTrue(context.pricingDiagnostics[fixture.aircraft]!!.noNegativeReducedCost)
        assertFalse(context.pricingDiagnostics[fixture.aircraft]!!.noFeasiblePath)
    }

    @Test
    fun `network pricing output matches legal Demo4 flight sequences`() {
        val fixture = demo4TestFixture()
        val context = initializedContext(fixture)
        val judger = FlightTaskFeasibilityJudger(
            aircraftUsability = mapOf(fixture.aircraft to fixture.usability),
            connectionTimeCalculator = connectionTimeCalculator(),
            ruleChecker = RuleChecker { _, _, _ -> true }
        )

        val result = context.generateFlightTaskBunch(
            aircrafts = listOf(fixture.aircraft),
            iteration = Int64.zero,
            shadowPriceMap = shadowPriceMap(fixture, 3.0)
        )

        assertTrue(result is Ok)
        val bunches = (result as Ok).value
        val sequences = bunches.map { bunch -> bunch.tasks.map { it.originTask } }.toSet()
        assertEquals(
            setOf(listOf(fixture.firstTask), listOf(fixture.firstTask, fixture.secondTask)),
            sequences
        )
        for (bunch in bunches) {
            assertEquals(fixture.aircraft, bunch.executor)
            var previousTask = fixture.usability.lastTask
            for (task in bunch.tasks) {
                assertTrue(
                    judger(
                        aircraft = fixture.aircraft,
                        prevFlightTask = previousTask,
                        flightTask = task,
                        config = FlightTaskFeasibilityJudger.Config(timeExtractor = { it.time })
                    ),
                    "generated route must remain legal under the Demo4 business rules"
                )
                previousTask = task
            }
        }
        assertEquals(ColumnGenerationCompletion.Exhausted, context.lastBatch?.completion)
    }

    @Test
    fun `batch evidence is returned only for the requested aircraft scope`() {
        val fixture = demo4TestFixture()
        val context = initializedContext(fixture)
        val scope = ColumnGenerationScope(
            request = "demo4:pricing:1",
            group = "${fixture.aircraft.id}#0:${fixture.aircraft}",
            branch = "root",
            global = true,
            model = "demo4-model",
            dual = "lp-1"
        )

        val result = context.generateFlightTaskBunch(
            aircrafts = listOf(fixture.aircraft),
            iteration = Int64.zero,
            shadowPriceMap = ShadowPriceMap(),
            scope = scope
        )

        assertTrue(result is Ok)
        assertEquals(scope, context.lastBatch?.scope)
        assertTrue(context.lastBatch!!.certifiesNoImprovingColumns(scope))
        assertEquals(1, context.lastBatch!!.children.size)
        val child = context.lastBatch!!.children.single()
        assertEquals("${scope.request}:aircraft:${fixture.aircraft.id}", child.scope?.request)
        assertEquals("${fixture.aircraft.id}#0:${fixture.aircraft}", child.scope?.group)
        assertTrue(child.certifiesNoImprovingColumns(child.scope!!))
    }

    @Test
    fun `pricing diagnostics distinguish a feasible path from no negative reduced cost`() {
        val fixture = demo4TestFixture()
        val context = BunchGenerationContext()
        val result = context.init(
            aircrafts = listOf(fixture.aircraft),
            aircraftUsability = mapOf(fixture.aircraft to fixture.usability),
            flightTasks = emptyList(),
            originBunches = emptyList(),
            lock = Lock(),
            connectionTimeCalculator = connectionTimeCalculator(),
            minimumDepartureTimeCalculator = minimumDepartureTimeCalculator(),
            ruleChecker = RuleChecker { _, _, _ -> true },
            costCalculator = costCalculator(),
            totalCostCalculator = totalCostCalculator()
        )
        assertTrue(result is Ok)

        val bunches = context.generateFlightTaskBunch(
            aircrafts = listOf(fixture.aircraft),
            iteration = Int64.zero,
            shadowPriceMap = ShadowPriceMap()
        )

        assertTrue(bunches is Ok)
        assertTrue((bunches as Ok).value.isEmpty())
        val diagnostics = context.pricingDiagnostics[fixture.aircraft]!!
        assertTrue(diagnostics.noFeasiblePath)
        assertTrue(diagnostics.noNegativeReducedCost)
    }

    @Test
    fun `unknown terminal cost does not certify negative labels or no improvement`() {
        val fixture = demo4TestFixture()
        val context = BunchGenerationContext()
        val result = context.init(
            aircrafts = listOf(fixture.aircraft),
            aircraftUsability = mapOf(fixture.aircraft to fixture.usability),
            flightTasks = listOf(fixture.firstTask),
            originBunches = emptyList(),
            lock = Lock(),
            connectionTimeCalculator = connectionTimeCalculator(),
            minimumDepartureTimeCalculator = minimumDepartureTimeCalculator(),
            ruleChecker = RuleChecker { _, _, _ -> true },
            costCalculator = costCalculator(),
            totalCostCalculator = TotalCostCalculator { _, _ -> null }
        )
        assertTrue(result is Ok)

        val shadowPriceMap = ShadowPriceMap()
        shadowPriceMap.put { _, _ -> Flt64(100.0) }
        val bunches = context.generateFlightTaskBunch(
            aircrafts = listOf(fixture.aircraft),
            iteration = Int64.zero,
            shadowPriceMap = shadowPriceMap
        )

        assertTrue(bunches is Ok)
        assertTrue((bunches as Ok).value.isEmpty())
        val diagnostics = context.pricingDiagnostics[fixture.aircraft]!!
        assertFalse(diagnostics.noFeasiblePath)
        assertFalse(diagnostics.noNegativeReducedCost)
        assertEquals(0, diagnostics.negativeReducedCostLabelCount)
        assertEquals(1, diagnostics.invalidColumnCount)
        assertFalse(context.lastBatch?.noImprovingColumns == true)
    }

    @Test
    fun `missing usability returns Failed`() {
        val fixture = demo4TestFixture()
        val context = BunchGenerationContext()

        val result = context.init(
            aircrafts = listOf(fixture.aircraft),
            aircraftUsability = emptyMap(),
            flightTasks = listOf(fixture.firstTask),
            originBunches = emptyList(),
            lock = Lock(),
            connectionTimeCalculator = connectionTimeCalculator(),
            minimumDepartureTimeCalculator = minimumDepartureTimeCalculator(),
            ruleChecker = RuleChecker { _, _, _ -> true },
            costCalculator = costCalculator(),
            totalCostCalculator = totalCostCalculator()
        )

        assertTrue(result is Failed)
    }

    @Test
    fun `generation before initialization returns Failed`() {
        val fixture = demo4TestFixture()
        val result = BunchGenerationContext().generateFlightTaskBunch(
            aircrafts = listOf(fixture.aircraft),
            iteration = Int64.zero,
            shadowPriceMap = ShadowPriceMap()
        )

        assertTrue(result is Failed)
    }

    @Test
    fun `missing graph returns Failed`() {
        val fixture = demo4TestFixture()
        val emptyAggregation = Aggregation(
            graphs = emptyMap(),
            reverse = FlightTaskReverse(emptyList(), emptyList(), Lock(), 0.minutes),
            initialFlightBunches = emptyList()
        )
        val context = BunchGenerationContext(
            aggregationInitializer = AggregationInitializerProvider { _, _, _, _, _, _, _, _ ->
                Ok(emptyAggregation)
            }
        )

        val result = context.init(
            aircrafts = listOf(fixture.aircraft),
            aircraftUsability = mapOf(fixture.aircraft to fixture.usability),
            flightTasks = listOf(fixture.firstTask),
            originBunches = emptyList(),
            lock = Lock(),
            connectionTimeCalculator = connectionTimeCalculator(),
            minimumDepartureTimeCalculator = minimumDepartureTimeCalculator(),
            ruleChecker = RuleChecker { _, _, _ -> true },
            costCalculator = costCalculator(),
            totalCostCalculator = totalCostCalculator()
        )

        assertTrue(result is Failed)
    }

    private fun initializedContext(fixture: Demo4TestFixture): BunchGenerationContext {
        val context = BunchGenerationContext()
        val result = context.init(
            aircrafts = listOf(fixture.aircraft),
            aircraftUsability = mapOf(fixture.aircraft to fixture.usability),
            flightTasks = listOf(fixture.firstTask, fixture.secondTask),
            originBunches = listOf(fixture.originBunch),
            lock = Lock(),
            connectionTimeCalculator = connectionTimeCalculator(),
            minimumDepartureTimeCalculator = minimumDepartureTimeCalculator(),
            ruleChecker = RuleChecker { _, _, _ -> true },
            costCalculator = costCalculator(),
            totalCostCalculator = totalCostCalculator()
        )
        check(result is Ok) { "context init failed: $result" }
        return context
    }

    private fun connectionTimeCalculator() = ConnectionTimeCalculator { _, _, _ -> 30.minutes }

    private fun minimumDepartureTimeCalculator() = MinimumDepartureTimeCalculator { arrivalTime, _, _, connectionTime ->
        arrivalTime + connectionTime
    }

    private fun costCalculator() = CostCalculator { _, _, _, _, _ -> demo4Cost(1.0) }

    private fun totalCostCalculator() = TotalCostCalculator { _, tasks -> demo4Cost(tasks.size.toDouble()) }

    private fun shadowPriceMap(fixture: Demo4TestFixture, firstTaskPrice: Double): ShadowPriceMap {
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
}
