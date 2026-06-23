@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for FreeAircraftSelector and FreeAircraftSelectorConfiguration.
 *
 * Verifies configuration defaults, custom values, and construction.
 * Full integration tests with ShadowPriceMap and domain objects require
 * solver infrastructure and are covered by the e2e test in phase 4.
 */
class FreeAircraftSelectorTest {

    @Test
    fun `FreeAircraftSelectorConfiguration defaults are correct`() {
        val config = FreeAircraftSelectorConfiguration()
        assertEquals(UInt64(3UL), config.badReducedAmount)
        assertEquals(UInt64(3UL), config.highCostAmount)
        assertEquals(UInt64(3UL), config.highAircraftChangeAmount)
        assertEquals(UInt64(3UL), config.randAmount)
        assertEquals(UInt64(10UL), config.tabuAmount)
        assertEquals(Flt64(0.0), config.fixBar)
        assertEquals(42L, config.randomSeed)
    }

    @Test
    fun `FreeAircraftSelectorConfiguration custom values`() {
        val config = FreeAircraftSelectorConfiguration(
            badReducedAmount = UInt64(5UL),
            highCostAmount = UInt64(2UL),
            highAircraftChangeAmount = UInt64(4UL),
            randAmount = UInt64(1UL),
            tabuAmount = UInt64(20UL),
            fixBar = Flt64(10.0),
            randomSeed = 99L
        )
        assertEquals(UInt64(5UL), config.badReducedAmount)
        assertEquals(UInt64(2UL), config.highCostAmount)
        assertEquals(UInt64(4UL), config.highAircraftChangeAmount)
        assertEquals(UInt64(1UL), config.randAmount)
        assertEquals(UInt64(20UL), config.tabuAmount)
        assertEquals(Flt64(10.0), config.fixBar)
        assertEquals(99L, config.randomSeed)
    }

    @Test
    fun `FreeAircraftSelectorConfiguration is immutable`() {
        val config1 = FreeAircraftSelectorConfiguration()
        val config2 = config1.copy(badReducedAmount = UInt64(99UL))
        assertEquals(UInt64(3UL), config1.badReducedAmount)
        assertEquals(UInt64(99UL), config2.badReducedAmount)
    }

    @Test
    fun `Parameter defaults are correct`() {
        val param = Parameter()
        assertEquals(Flt64(60.0), param.fleetBalanceSlack)
        assertEquals(Flt64(600.0), param.fleetBalanceBaseSlack)
        assertEquals(Flt64(0.0), param.executorLeisureCoeff)
        assertEquals(Flt64(9999.0), param.taskCancelCoeff)
        assertEquals(Flt64(3.0), param.passengerCancelCoeff)
        assertEquals(Flt64(0.0), param.passengerClassChangeCoeff)
        assertEquals(Flt64(1.0), param.passengerFlightChangeCoeff)
    }

    @Test
    fun `Parameter is immutable`() {
        val param1 = Parameter()
        val param2 = param1.copy(taskCancelCoeff = Flt64(5000.0))
        assertEquals(Flt64(9999.0), param1.taskCancelCoeff)
        assertEquals(Flt64(5000.0), param2.taskCancelCoeff)
    }
}
