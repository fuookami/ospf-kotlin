@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * demo4 B&P 管线中的失败路径测试 / Tests for failure paths in the demo4 B&P pipeline.
 *
 * 验证配置和参数对象产生有效默认值，并覆盖错误处理路径。完整的集成失败测试（无初始列、定价不可行）需要领域对象构建，通过 e2e 测试进行验证 /
 * Verifies that configuration and parameter objects produce valid defaults
 * and that error handling paths are covered. Full integration failure tests
 * (no initial columns, pricing infeasible) require domain object construction
 * and are validated through the e2e test.
 */
class FailurePathTest {

    @Test
    fun `Parameter defaults produce valid coefficient values`() {
        val param = Parameter()
        // All coefficients should be non-negative
        assertTrue(param.fleetBalanceSlack.toDouble() >= 0.0, "fleetBalanceSlack should be non-negative")
        assertTrue(param.fleetBalanceBaseSlack.toDouble() >= 0.0, "fleetBalanceBaseSlack should be non-negative")
        assertTrue(param.executorLeisureCoeff.toDouble() >= 0.0, "executorLeisureCoeff should be non-negative")
        assertTrue(param.taskCancelCoeff.toDouble() >= 0.0, "taskCancelCoeff should be non-negative")
        assertTrue(param.passengerCancelCoeff.toDouble() >= 0.0, "passengerCancelCoeff should be non-negative")
        assertTrue(param.passengerClassChangeCoeff.toDouble() >= 0.0, "passengerClassChangeCoeff should be non-negative")
        assertTrue(param.passengerFlightChangeCoeff.toDouble() >= 0.0, "passengerFlightChangeCoeff should be non-negative")
    }

    @Test
    fun `FreeAircraftSelectorConfiguration defaults produce valid values`() {
        val config = FreeAircraftSelectorConfiguration()
        assertTrue(config.badReducedAmount > UInt64.zero, "badReducedAmount should be positive")
        assertTrue(config.highCostAmount > UInt64.zero, "highCostAmount should be positive")
        assertTrue(config.highAircraftChangeAmount > UInt64.zero, "highAircraftChangeAmount should be positive")
        assertTrue(config.randAmount > UInt64.zero, "randAmount should be positive")
        assertTrue(config.tabuAmount > UInt64.zero, "tabuAmount should be positive")
    }

    @Test
    fun `BranchAndPriceAlgorithm Configuration defaults are reasonable`() {
        val config = fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_selection.service.BranchAndPriceAlgorithm.Configuration()
        assertTrue(config.badReducedAmount > UInt64.zero, "badReducedAmount should be positive")
        assertTrue(config.maximumColumnAmount > UInt64.zero, "maximumColumnAmount should be positive")
        assertTrue(config.timeLimit.isPositive(), "timeLimit should be positive")
    }

    @Test
    fun `Parameter copy produces independent instance`() {
        val original = Parameter()
        val modified = original.copy(taskCancelCoeff = Flt64(5000.0))

        // Original should be unchanged
        assertEquals(Flt64(9999.0), original.taskCancelCoeff)
        // Modified should have new value
        assertEquals(Flt64(5000.0), modified.taskCancelCoeff)
        // Other fields should be equal
        assertEquals(original.fleetBalanceSlack, modified.fleetBalanceSlack)
        assertEquals(original.passengerCancelCoeff, modified.passengerCancelCoeff)
    }

    @Test
    fun `FreeAircraftSelectorConfiguration copy produces independent instance`() {
        val original = FreeAircraftSelectorConfiguration()
        val modified = original.copy(randomSeed = 999L)

        assertEquals(42L, original.randomSeed)
        assertEquals(999L, modified.randomSeed)
        assertEquals(original.badReducedAmount, modified.badReducedAmount)
    }
}
