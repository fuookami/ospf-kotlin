package fuookami.ospf.kotlin.example.framework_demo.demo4

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 束生成配置的默认值和自定义测试 / Tests for BunchGenerationConfiguration defaults and customization.
 */
class BunchGenerationConfigurationTest {

    @Test
    fun `default configuration has withOrderChange false`() {
        val config = BunchGenerationConfiguration()
        assertFalse(config.withOrderChange)
    }

    @Test
    fun `default configuration has maximumLabelPerNode 100`() {
        val config = BunchGenerationConfiguration()
        assertEquals(UInt64(100UL), config.maximumLabelPerNode)
    }

    @Test
    fun `default configuration has maximumColumnGeneratedPerAircraft 10`() {
        val config = BunchGenerationConfiguration()
        assertEquals(UInt64(10UL), config.maximumColumnGeneratedPerAircraft)
    }

    @Test
    fun `custom configuration overrides defaults`() {
        val config = BunchGenerationConfiguration(
            withOrderChange = true,
            maximumLabelPerNode = UInt64(200UL),
            maximumColumnGeneratedPerAircraft = UInt64(20UL)
        )
        assertTrue(config.withOrderChange)
        assertEquals(UInt64(200UL), config.maximumLabelPerNode)
        assertEquals(UInt64(20UL), config.maximumColumnGeneratedPerAircraft)
    }
}
