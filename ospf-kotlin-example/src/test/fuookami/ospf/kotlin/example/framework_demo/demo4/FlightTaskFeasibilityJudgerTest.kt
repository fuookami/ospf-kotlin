package fuookami.ospf.kotlin.example.framework_demo.demo4

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for FlightTaskFeasibilityJudger configuration.
 */
class FlightTaskFeasibilityJudgerTest {

    @Test
    fun `default Config has checkEnabledTime true`() {
        val config = FlightTaskFeasibilityJudger.Config()
        assertTrue(config.checkEnabledTime)
    }

    @Test
    fun `default Config has null departureTime`() {
        val config = FlightTaskFeasibilityJudger.Config()
        assertNull(config.departureTime)
    }

    @Test
    fun `custom Config overrides checkEnabledTime`() {
        val config = FlightTaskFeasibilityJudger.Config(
            checkEnabledTime = false
        )
        assertFalse(config.checkEnabledTime)
    }
}
