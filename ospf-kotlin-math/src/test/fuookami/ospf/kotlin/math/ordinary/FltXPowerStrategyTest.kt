package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.math.algebra.number.FltX
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FltXPowerStrategyTest {
    @Test
    fun defaultPrecisionShouldRespectDigitsBounds() {
        val p0 = FltXPowerStrategy.defaultPrecision(0)
        val p1 = FltXPowerStrategy.defaultPrecision(1)
        val p30 = FltXPowerStrategy.defaultPrecision(30)

        assertEquals(p1, p0)
        assertTrue(p0 > FltX.zero)
        assertTrue(p0 <= FltX.epsilon)
        assertTrue(p30 < FltX.epsilon)
    }

    @Test
    fun lnAndExpShouldRoundTrip() {
        val digits = 18
        val precision = FltXPowerStrategy.defaultPrecision(digits)
        val input = FltX("1.25")

        val lnValue = FltXPowerStrategy.ln(input, digits, precision)
        assertNotNull(lnValue)

        val restored = FltXPowerStrategy.exp(lnValue, digits, precision)
        val error = (restored - input).abs()
        assertTrue(error <= FltX("1e-8"))
    }

    @Test
    fun powShouldHandleIntegerAndFractionalIndex() {
        val digits = 18
        val precision = FltXPowerStrategy.defaultPrecision(digits)

        val intPow = FltXPowerStrategy.pow(FltX("2"), FltX("10"), digits, precision)
        assertTrue((intPow - FltX("1024")).abs() <= FltX("1e-8"))

        val fractionalPow = FltXPowerStrategy.pow(FltX("9"), FltX("0.5"), digits, precision)
        assertTrue((fractionalPow - FltX("3")).abs() <= FltX("1e-6"))
    }

    @Test
    fun lnShouldReturnNullForNonPositiveValues() {
        assertNull(FltXPowerStrategy.ln(FltX.zero, digits = 18))
        assertNull(FltXPowerStrategy.ln(-FltX.one, digits = 18))
    }

    @Test
    fun expWithStatsShouldRespectMaxIterations() {
        val stats = FltXPowerStrategy.expWithStats(
            index = FltX.one,
            digits = 18,
            precision = FltX("1e-40", scale = 50),
            maxIterations = 1
        )

        assertFalse(stats.converged)
        assertEquals(1, stats.iterations)
    }

    @Test
    fun powShouldThrowForFractionalExponentOnNonPositiveBase() {
        val digits = 18
        val precision = FltXPowerStrategy.defaultPrecision(digits)

        assertFailsWith<ArithmeticException> {
            FltXPowerStrategy.pow(FltX("-4"), FltX("0.5"), digits, precision)
        }
        assertFailsWith<ArithmeticException> {
            FltXPowerStrategy.pow(FltX.zero, FltX("0.5"), digits, precision)
        }
    }
}
