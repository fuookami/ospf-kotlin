@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for InitialFlightTaskBunchGenerator configuration.
 *
 * Note: Full integration tests with actual Aircraft/FlightTask objects require
 * domain model initialization. These tests verify the generator can be constructed.
 */
class InitialFlightTaskBunchGeneratorTest {

    @Test
    fun `InitialFlightTaskBunchGenerator can be constructed`() {
        val feasibilityJudger = FlightTaskFeasibilityJudger(
            aircraftUsability = emptyMap(),
            connectionTimeCalculator = { _, _, _ -> Duration.ZERO },
            ruleChecker = { _, _, _ -> true }
        )
        val connectionTimeCalculator: ConnectionTimeCalculator = { _, _, _ -> Duration.ZERO }
        val minimumDepartureTimeCalculator: MinimumDepartureTimeCalculator = { arrivalTime, _, _, connectionTime ->
            arrivalTime + connectionTime
        }
        val costCalculator: TotalCostCalculator = { _, _ -> null }

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
        assertFalse(config.checkEnabledTime)
    }

    @Test
    fun `InitialFlightTaskBunchGenerator config uses FlightTask time extractor`() {
        val config = InitialFlightTaskBunchGenerator.config
        // The timeExtractor should extract FlightTask.time
        assertNotNull(config.timeExtractor)
    }
}
