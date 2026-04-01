package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.math.algebra.concept.CompanionConstantProviderResolver
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GCDTest {
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
    fun gcdTwo() {
        assertEquals(UInt64(2), gcd(UInt64(4), UInt64(6)))
        assertEquals(UInt64(3), gcd(UInt64(6), UInt64(9)))
    }

    @Test
    fun gcdSome() {
        assertEquals(UInt64(2), gcd(listOf(UInt64(4), UInt64(6), UInt64(8))))
        assertEquals(UInt64(3), gcd(listOf(UInt64(6), UInt64(9), UInt64(12))))
    }

    @Test
    fun gcdMod() {
        assertEquals(UInt64(6), gcdMod(UInt64(48), UInt64(18)))
        assertEquals(UInt64(6), gcdMod(listOf(UInt64(48), UInt64(18), UInt64(30))))
    }

    @Test
    fun extendedGcd() {
        val result = extendedGcd(Int64(240), Int64(46))
        assertEquals(Int64(2), result.gcd)
        assertEquals(result.gcd, Int64(240) * result.x + Int64(46) * result.y)
    }
}



