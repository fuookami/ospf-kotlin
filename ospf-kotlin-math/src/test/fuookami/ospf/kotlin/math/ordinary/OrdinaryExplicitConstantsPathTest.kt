package fuookami.ospf.kotlin.math.ordinary

import kotlin.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.concept.CompanionConstantProviderResolver
import fuookami.ospf.kotlin.math.algebra.number.*

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

    private fun assertNear(actual: Flt64?, expected: Flt64, tolerance: Flt64 = Flt64(1e-8)) {
        assertTrue(actual != null)
        assertTrue((actual - expected).abs() <= tolerance, "actual=$actual expected=$expected tolerance=$tolerance")
    }

    @Test
    fun explicitConstantsPathsShouldWorkWhenFallbackDisabled() {
        val factors = factorize(UInt64(12), UInt64)
        assertEquals(listOf(UInt64.two to 2, UInt64.three to 1), factors)
        assertEquals(UInt64(12), defactorize(factors, UInt64).value!!)
        assertEquals(
            listOf(UInt64.one, UInt64.two, UInt64(3), UInt64(4), UInt64(6), UInt64(12)),
            divisors(UInt64(12), UInt64)
        )
        assertEquals(6, divisorCount(UInt64(12), UInt64))
        assertEquals(UInt64(4), eulerTotient(UInt64(12), UInt64))

        val numbers = listOf(UInt64(48), UInt64(18), UInt64(30))
        assertEquals(UInt64(6), gcd(numbers, UInt64))
        assertEquals(UInt64(6), gcdMod(numbers, UInt64))
        assertEquals(UInt64(6), gcd(UInt64(48), UInt64(18), UInt64(30), constants = UInt64))
        assertEquals(UInt64(6), gcdMod(UInt64(48), UInt64(18), UInt64(30), constants = UInt64))

        val lcmNumbers = listOf(UInt64(4), UInt64(6), UInt64(8))
        assertEquals(UInt64(24), lcmByFactorization(lcmNumbers, UInt64))
        assertEquals(UInt64(24), lcm(lcmNumbers, UInt64))
        assertEquals(UInt64(24), lcmByFactorization(UInt64(4), UInt64(6), UInt64(8), constants = UInt64))
        assertEquals(UInt64(24), lcm(UInt64(4), UInt64(6), UInt64(8), constants = UInt64))

        assertEquals(
            listOf(UInt64.two, UInt64.three, UInt64.five, UInt64(7), UInt64(11)),
            getPrimes(UInt64(11), UInt64)
        )

        assertNear(ln(Flt64.e, Flt64), Flt64.one)
        assertNear(log(Flt64.e, Flt64.e, Flt64), Flt64.one)
        val powValue = powSafe(UInt64.two, 10, UInt64).value!!
        assertEquals(UInt64(1024), powValue)
        assertNear(powf(Flt64(9.0), Flt64.half, Flt64), Flt64(3.0))
        assertNear(exp(Flt64.one, Flt64), Flt64.e)
    }

    @Test
    /** 验证禁用回退时具态路径失败 / Verify reified paths fail when fallback disabled */
    fun reifiedPathsShouldFailWhenFallbackDisabled() {
        assertTrue(factorize(UInt64(12)).failed)
        assertTrue(defactorize(listOf(UInt64.two to 2, UInt64.three to 1)).failed)
        assertTrue(divisors(UInt64(12)).failed)
        assertTrue(divisorCount(UInt64(12)).failed)
        assertTrue(eulerTotient(UInt64(12)).failed)
        assertTrue(gcd(listOf(UInt64(48), UInt64(18), UInt64(30))).failed)
        assertTrue(gcdMod(listOf(UInt64(48), UInt64(18), UInt64(30))).failed)
        assertTrue(gcd(UInt64(48), UInt64(18), UInt64(30)).failed)
        assertTrue(gcdMod(UInt64(48), UInt64(18), UInt64(30)).failed)
        assertTrue(lcmByFactorization(listOf(UInt64(4), UInt64(6), UInt64(8))).failed)
        assertTrue(lcm(listOf(UInt64(4), UInt64(6), UInt64(8))).failed)
        assertTrue(lcmByFactorization(UInt64(4), UInt64(6), UInt64(8)).failed)
        assertTrue(lcm(UInt64(4), UInt64(6), UInt64(8)).failed)
        assertNull(ln(Flt64.e))
        assertNull(log(Flt64.e, Flt64.e))
        assertTrue(pow(UInt64.two, 10).failed)
        assertTrue(powf(Flt64(9.0), Flt64.half).failed)
        assertTrue(exp(Flt64.one).failed)
    }
}
