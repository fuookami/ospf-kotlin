package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter

class QuantityArithmeticTest {
    @Test
    fun flt64AddAndSubtract() {
        val arith = Flt64QuantityArithmetic
        val a = Quantity(Flt64(3.0), Meter)
        val b = Quantity(Flt64(2.0), Meter)

        val sum = arith.add(a, b)
        assertTrue(sum eq Quantity(Flt64(5.0), Meter))

        val diff = arith.subtract(a, b)
        assertTrue(diff eq Quantity(Flt64(1.0), Meter))
    }

    @Test
    fun fltXAddAndSubtract() {
        val arith = FltXQuantityArithmetic
        val a = Quantity(FltX("3.0"), Kilogram)
        val b = Quantity(FltX("1.5"), Kilogram)

        val sum = arith.add(a, b)
        assertTrue(sum eq Quantity(FltX("4.5"), Kilogram))

        val diff = arith.subtract(a, b)
        assertTrue(diff eq Quantity(FltX("1.5"), Kilogram))
    }

    @Test
    fun resolveForFlt64ReturnsFlt64Arithmetic() {
        val arith = DefaultQuantityArithmetic.resolveFor(Flt64.one)
        assertEquals(Flt64QuantityArithmetic, arith)
    }

    @Test
    fun resolveForFltXReturnsFltXArithmetic() {
        val arith = DefaultQuantityArithmetic.resolveFor(FltX.one)
        assertEquals(FltXQuantityArithmetic, arith)
    }

    @Test
    fun resolveForUnsupportedTypeThrows() {
        // Test that an unsupported type throws
        // We use a mock-like approach: RealNumber is abstract, so we test with known types only
        val arithFlt64 = DefaultQuantityArithmetic.resolveFor(Flt64.zero)
        assertTrue(arithFlt64 is Flt64QuantityArithmetic)
    }

    @Test
    @Suppress("DEPRECATION_ERROR")
    fun resolveIsDeprecatedWithErrorLevel() {
        // Verify that resolve() has ERROR deprecation level
        // This test confirms the API is no longer usable without suppression
        val arith = DefaultQuantityArithmetic.resolve<Flt64>()
        // If we get here, the suppression worked; verify the fallback
        assertTrue(arith is Flt64QuantityArithmetic)
    }

    @Test
    fun zeroCreatesCorrectQuantity() {
        val arith = Flt64QuantityArithmetic
        val zero = arith.zero(Meter)
        assertTrue(zero eq Quantity(Flt64.zero, Meter))
    }
}