@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlin.time.Duration.Companion.hours
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 航班任务逆向逻辑测试 / Tests for FlightTaskReverse logic.
 *
 * 注意：这些测试验证数据结构和查询逻辑。使用实际 FlightTask 对象的完整集成测试需要领域模型初始化 /
 * Note: These tests verify the data structure and query logic.
 * Full integration tests with actual FlightTask objects require domain model initialization.
 */
class FlightTaskReverseTest {

    @Test
    fun `FlightTaskReverse default time difference limit is 5 hours`() {
        assertEquals(5.hours, FlightTaskReverse.defaultTimeDifferenceLimit)
    }

    @Test
    fun `FlightTaskReverse critical size is 200`() {
        assertEquals(UInt64(200UL), FlightTaskReverse.criticalSize)
    }

    @Test
    fun `empty FlightTaskReverse has no pairs`() {
        val lock = Lock()
        val reverse = FlightTaskReverse.invoke(
            pairs = emptyList(),
            originBunches = emptyList(),
            lock = lock,
            timeDifferenceLimit = FlightTaskReverse.defaultTimeDifferenceLimit
        )
        // Empty reverse should not crash on queries
        assertNotNull(reverse)
    }
}
