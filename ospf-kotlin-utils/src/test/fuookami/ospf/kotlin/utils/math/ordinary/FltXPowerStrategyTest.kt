package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.math.algebra.number.FltX
import fuookami.ospf.kotlin.utils.math.algebra.concept.CompanionConstantProviderResolver
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.math.E
import kotlin.math.abs
import kotlin.test.assertTrue

class FltXPowerStrategyTest {
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
    fun lnAndExpShouldRoundTrip() {
        val digits = 18
        val precision = FltXPowerStrategy.defaultPrecision(digits)
        val input = FltX("1.25")
        val lnValue = FltXPowerStrategy.ln(input, digits, precision)
        assertTrue(lnValue != null)
        val restored = FltXPowerStrategy.exp(lnValue!!, digits, precision)
        val error = (restored - input).abs()
        assertTrue(error <= FltX("1e-6"))
    }

    @Test
    fun powShouldHandleIntegerAndFractionalIndex() {
        val digits = 18
        val precision = FltXPowerStrategy.defaultPrecision(digits)

        val intPow = FltXPowerStrategy.pow(FltX("2"), FltX("10"), digits, precision)
        assertTrue((intPow - FltX("1024")).abs() <= FltX("1e-6"))

        val fractionalPow = FltXPowerStrategy.pow(FltX("9"), FltX("0.5"), digits, precision)
        assertTrue((fractionalPow - FltX("3")).abs() <= FltX("1e-4"))
    }

    @Test
    fun higherDigitsShouldImproveExpAccuracyAndConverge() {
        val lowDigits = 6
        val highDigits = 12

        val low = FltXPowerStrategy.expWithStats(FltX.one, lowDigits)
        val high = FltXPowerStrategy.expWithStats(FltX.one, highDigits)

        assertTrue(low.converged)
        assertTrue(high.converged)
        assertTrue(low.iterations < 8192)
        assertTrue(high.iterations < 8192)

        val lowError = abs(low.value.toFlt64().toDouble() - E)
        val highError = abs(high.value.toFlt64().toDouble() - E)
        assertTrue(highError <= lowError)
    }
}




