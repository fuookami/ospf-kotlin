package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.math.algebra.concept.CompanionConstantProviderResolver
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FactorizationTest {
    companion object {
        private val propertyKey = CompanionConstantProviderResolver.reflectionFallbackEnabledProperty
        private var previousValue: String? = null

        @JvmStatic
        @BeforeAll
        fun enableReflectionFallback() {
            previousValue = System.getProperty(propertyKey)
            System.setProperty(propertyKey, "true")
        }

        @JvmStatic
        @AfterAll
        fun restoreReflectionFallback() {
            if (previousValue == null) {
                System.clearProperty(propertyKey)
            } else {
                System.setProperty(propertyKey, previousValue)
            }
        }
    }

    @Test
    fun factorize() {
        assertEquals(listOf(UInt64.two to 1), factorize(UInt64.two))
        assertEquals(listOf(UInt64.two to 2), factorize(UInt64(4)))
        assertEquals(listOf(UInt64.two to 2, UInt64.three to 1), factorize(UInt64(12)))
    }

    @Test
    fun defactorize() {
        assertEquals(UInt64(12), defactorize(listOf(UInt64.two to 2, UInt64.three to 1)))
        assertEquals(UInt64.one, defactorize(emptyList()))
    }

    @Test
    fun divisors() {
        assertEquals(listOf(UInt64.one, UInt64.two, UInt64(3), UInt64(4), UInt64(6), UInt64(12)), divisors(UInt64(12)))
        assertEquals(
            listOf(UInt64.one, UInt64.two, UInt64(3), UInt64(4), UInt64(6), UInt64(12)),
            divisors(listOf(UInt64.two to 2, UInt64.three to 1))
        )
    }

    @Test
    fun divisorCount() {
        assertEquals(6, divisorCount(UInt64(12)))
        assertEquals(6, divisorCount(listOf(UInt64.two to 2, UInt64.three to 1)))
    }

    @Test
    fun eulerTotient() {
        assertEquals(UInt64.one, eulerTotient(UInt64.one))
        assertEquals(UInt64(4), eulerTotient(UInt64(12)))
        assertEquals(UInt64(6), eulerTotient(UInt64(9)))
    }
}



