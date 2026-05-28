package fuookami.ospf.kotlin.math.algebra.concept

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.ordinary.*

class ConstantProviderReflectionFallbackTest {
    private val propertyKey = CompanionConstantProviderResolver.reflectionFallbackEnabledProperty

    private fun withReflectionFallbackProperty(value: String?, block: () -> Unit) {
        val previous = System.getProperty(propertyKey)
        try {
            if (value == null) {
                System.clearProperty(propertyKey)
            } else {
                System.setProperty(propertyKey, value)
            }
            block()
        } finally {
            if (previous == null) {
                System.clearProperty(propertyKey)
            } else {
                System.setProperty(propertyKey, previous)
            }
        }
    }

    @Test
    fun reflectionFallbackShouldBeDisabledByDefault() {
        withReflectionFallbackProperty(null) {
            assertFalse(CompanionConstantProviderResolver.reflectionFallbackEnabled)
            val error = assertFailsWith<IllegalStateException> {
                pow<Int32>(Int32.two, 3)
            }
            assertTrue(error.message?.contains("Companion reflection fallback is disabled") == true)
        }
    }

    @Test
    fun reflectionFallbackShouldWorkWhenEnabled() {
        withReflectionFallbackProperty("true") {
            assertTrue(CompanionConstantProviderResolver.reflectionFallbackEnabled)
            val powValue = pow<Int32>(Int32.two, 3)
            val lnValue = ln<Flt64>(Flt64.e)

            assertTrue(powValue eq Int32(8))
            assertNotNull(lnValue)
            assertTrue(lnValue > Flt64.zero)
        }
    }
}
