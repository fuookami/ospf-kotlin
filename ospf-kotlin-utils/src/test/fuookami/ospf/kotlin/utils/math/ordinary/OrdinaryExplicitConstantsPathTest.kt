package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.math.algebra.concept.CompanionConstantProviderResolver
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrdinaryExplicitConstantsPathTest {
    companion object {
        private val propertyKey = CompanionConstantProviderResolver.reflectionFallbackEnabledProperty
        private var previousValue: String? = null

        @JvmStatic
        @BeforeAll
        fun disableReflectionFallback() {
            previousValue = System.getProperty(propertyKey)
            System.setProperty(propertyKey, "false")
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
    fun explicitConstantsPathsShouldWorkWhenFallbackDisabled() {
        val factors = factorize(UInt64(12), UInt64)
        assertEquals(listOf(UInt64.two to 2, UInt64.three to 1), factors)
        assertEquals(UInt64(12), defactorize(factors, UInt64))
        assertEquals(
            listOf(UInt64.one, UInt64.two, UInt64(3), UInt64(4), UInt64(6), UInt64(12)),
            divisors(UInt64(12), UInt64)
        )
        assertEquals(6, divisorCount(UInt64(12), UInt64))
        assertEquals(UInt64(4), eulerTotient(UInt64(12), UInt64))

        val numbers = listOf(UInt64(48), UInt64(18), UInt64(30))
        assertEquals(UInt64(6), gcd(numbers, UInt64))
        assertEquals(UInt64(6), gcdMod(numbers, UInt64))

        val lcmNumbers = listOf(UInt64(4), UInt64(6), UInt64(8))
        assertEquals(UInt64(24), lcmByFactorization(lcmNumbers, UInt64))
        assertEquals(UInt64(24), lcm(lcmNumbers, UInt64))
    }

    @Test
    fun reifiedPathsShouldThrowWhenFallbackDisabled() {
        assertFailsWith<IllegalStateException> { factorize(UInt64(12)) }
        assertFailsWith<IllegalStateException> { defactorize(listOf(UInt64.two to 2, UInt64.three to 1)) }
        assertFailsWith<IllegalStateException> { divisors(UInt64(12)) }
        assertFailsWith<IllegalStateException> { divisorCount(UInt64(12)) }
        assertFailsWith<IllegalStateException> { eulerTotient(UInt64(12)) }
        assertFailsWith<IllegalStateException> { gcd(listOf(UInt64(48), UInt64(18), UInt64(30))) }
        assertFailsWith<IllegalStateException> { gcdMod(listOf(UInt64(48), UInt64(18), UInt64(30))) }
        assertFailsWith<IllegalStateException> { lcmByFactorization(listOf(UInt64(4), UInt64(6), UInt64(8))) }
        assertFailsWith<IllegalStateException> { lcm(listOf(UInt64(4), UInt64(6), UInt64(8))) }
    }
}
