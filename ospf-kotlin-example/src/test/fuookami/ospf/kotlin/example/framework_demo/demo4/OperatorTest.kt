@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlin.time.Duration
import kotlin.time.Instant
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 算子类型别名（RuleChecker、ConnectionTimeCalculator 等）的测试 / Tests for Operator type aliases (RuleChecker, ConnectionTimeCalculator, etc.).
 *
 * 这些测试验证类型别名可以作为函数引用使用 / These tests verify that the type aliases can be used as function references.
 */
class OperatorTest {

    @Test
    fun `RuleChecker can be assigned as lambda`() {
        val checker: RuleChecker = { _, _, _ -> true }
        assertNotNull(checker)
    }

    @Test
    fun `ConnectionTimeCalculator can be assigned as lambda`() {
        val calculator: ConnectionTimeCalculator = { _, _, _ -> Duration.ZERO }
        assertNotNull(calculator)
    }

    @Test
    fun `MinimumDepartureTimeCalculator can be assigned as lambda`() {
        val calculator: MinimumDepartureTimeCalculator = { arrivalTime, _, _, connectionTime ->
            arrivalTime + connectionTime
        }
        assertNotNull(calculator)
    }

    @Test
    fun `CostCalculator can be assigned as lambda`() {
        val calculator: CostCalculator = { _, _, _, _, _ -> null }
        assertNotNull(calculator)
    }

    @Test
    fun `TotalCostCalculator can be assigned as lambda`() {
        val calculator: TotalCostCalculator = { _, _ -> null }
        assertNotNull(calculator)
    }
}
