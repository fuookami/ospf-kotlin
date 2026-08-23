@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 初始航班任务束生成器的配置测试 / Tests for InitialFlightTaskBunchGenerator configuration.
 *
 * 注意：使用实际 Aircraft/FlightTask 对象的完整集成测试需要领域模型初始化。这些测试验证生成器可以被构造 /
 * Note: Full integration tests with actual Aircraft/FlightTask objects require
 * domain model initialization. These tests verify the generator can be constructed.
 */
class InitialFlightTaskBunchGeneratorTest {

    @Test
    fun `InitialFlightTaskBunchGenerator can be constructed`() {
        val feasibilityJudger = FlightTaskFeasibilityJudger(
            aircraftUsability = emptyMap(),
            connectionTimeCalculator = ConnectionTimeCalculator { _, _, _ -> Duration.ZERO },
            ruleChecker = RuleChecker { _, _, _ -> true }
        )
        val connectionTimeCalculator = ConnectionTimeCalculator { _, _, _ -> Duration.ZERO }
        val minimumDepartureTimeCalculator = MinimumDepartureTimeCalculator { arrivalTime, _, _, connectionTime ->
            arrivalTime + connectionTime
        }
        val costCalculator = TotalCostCalculator { _, _ -> null }

        val generator = InitialFlightTaskBunchGenerator(
            feasibilityJudger = feasibilityJudger,
            connectionTimeCalculator = connectionTimeCalculator,
            minimumDepartureTimeCalculator = minimumDepartureTimeCalculator,
            costCalculator = costCalculator
        )

        assertNotNull(generator)
    }

    @Test
    fun `InitialFlightTaskBunchGenerator has default config`() {
        val config = InitialFlightTaskBunchGenerator.config
        assertTrue(config.checkEnabledTime)
    }

    @Test
    fun `InitialFlightTaskBunchGenerator config uses FlightTask time extractor`() {
        val config = InitialFlightTaskBunchGenerator.config
        // The timeExtractor should extract FlightTask.time
        assertNotNull(config.timeExtractor)
    }

    @Test
    fun `soft recovery preserves the original task sequence`() {
        val fixture = demo4TestFixture()
        val bunch = generator(fixture)(
            aircraft = fixture.aircraft,
            aircraftUsability = fixture.usability,
            lockedFlightTasks = emptyList(),
            originBunch = fixture.originBunch
        )

        assertNotNull(bunch)
        assertEquals(
            listOf(fixture.firstTask, fixture.secondTask),
            bunch!!.tasks.map { it.originTask }
        )
    }

    @Test
    fun `locked task is retained during soft recovery`() {
        val fixture = demo4TestFixture()
        val bunch = generator(fixture)(
            aircraft = fixture.aircraft,
            aircraftUsability = fixture.usability,
            lockedFlightTasks = listOf(fixture.secondTask),
            originBunch = fixture.originBunch
        )

        assertNotNull(bunch)
        assertTrue(bunch!!.tasks.any { it.originTask == fixture.secondTask })
    }

    @Test
    fun `empty bunch requires a locked task and a valid cost`() {
        val fixture = demo4TestFixture()
        val generator = generator(fixture)

        assertNull(generator.emptyBunch(fixture.aircraft, fixture.usability, emptyList()))

        val bunch = generator.emptyBunch(
            aircraft = fixture.aircraft,
            aircraftUsability = fixture.usability,
            lockedFlightTasks = listOf(fixture.firstTask)
        )
        assertNotNull(bunch)
        assertEquals(listOf(fixture.firstTask), bunch!!.tasks.map { it.originTask })

        val noCostGenerator = generator(fixture) { _, _ -> null }
        assertNull(
            noCostGenerator.emptyBunch(
                aircraft = fixture.aircraft,
                aircraftUsability = fixture.usability,
                lockedFlightTasks = listOf(fixture.firstTask)
            )
        )
    }

    @Test
    fun `invalid cost is rejected by empty bunch and soft recovery`() {
        val fixture = demo4TestFixture()
        var costCalculatorInvocations = 0
        val invalidCostGenerator = generator(fixture) { _, _ ->
            costCalculatorInvocations += 1
            demo4InvalidCost()
        }

        assertNull(
            invalidCostGenerator.emptyBunch(
                aircraft = fixture.aircraft,
                aircraftUsability = fixture.usability,
                lockedFlightTasks = listOf(fixture.firstTask)
            )
        )
        assertEquals(1, costCalculatorInvocations)

        assertNull(
            invalidCostGenerator(
                aircraft = fixture.aircraft,
                aircraftUsability = fixture.usability,
                lockedFlightTasks = emptyList(),
                originBunch = fixture.originBunch
            )
        )
        assertEquals(2, costCalculatorInvocations)
    }

    private fun generator(
        fixture: Demo4TestFixture,
        totalCostCalculator: TotalCostCalculator = TotalCostCalculator { _, tasks ->
            demo4Cost(tasks.size.toDouble())
        }
    ): InitialFlightTaskBunchGenerator {
        return InitialFlightTaskBunchGenerator(
            feasibilityJudger = FlightTaskFeasibilityJudger(
                aircraftUsability = mapOf(fixture.aircraft to fixture.usability),
                connectionTimeCalculator = ConnectionTimeCalculator { _, _, _ -> 30.minutes },
                ruleChecker = RuleChecker { _, _, _ -> true }
            ),
            connectionTimeCalculator = ConnectionTimeCalculator { _, _, _ -> 30.minutes },
            minimumDepartureTimeCalculator = MinimumDepartureTimeCalculator { arrivalTime, _, _, connectionTime ->
                arrivalTime + connectionTime
            },
            costCalculator = totalCostCalculator
        )
    }
}
