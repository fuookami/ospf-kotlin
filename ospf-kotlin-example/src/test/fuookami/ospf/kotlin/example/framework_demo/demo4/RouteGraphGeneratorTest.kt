@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*
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
}
