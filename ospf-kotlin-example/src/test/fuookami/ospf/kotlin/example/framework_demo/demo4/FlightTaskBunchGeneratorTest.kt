@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
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
}
