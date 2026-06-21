package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*

class QuantityArithmeticTest {
    @Test
    fun flt64AddAndSubtract() {
        val arith = (DefaultQuantityArithmetic.resolveFor(Flt64.one) as Ok).value
        val a = Quantity(Flt64(3.0), Meter)
        val b = Quantity(Flt64(2.0), Meter)

        val sum = assertNotNull(arith.add(a, b).value)
        assertTrue(sum eq Quantity(Flt64(5.0), Meter))

        val diff = assertNotNull(arith.subtract(a, b).value)
        assertTrue(diff eq Quantity(Flt64(1.0), Meter))
    }

    @Test
    fun fltXAddAndSubtract() {
        val arith = (DefaultQuantityArithmetic.resolveFor(FltX.one) as Ok).value
        val a = Quantity(FltX("3.0"), Kilogram)
        val b = Quantity(FltX("1.5"), Kilogram)

        val sum = assertNotNull(arith.add(a, b).value)
        assertTrue(sum eq Quantity(FltX("4.5"), Kilogram))

        val diff = assertNotNull(arith.subtract(a, b).value)
        assertTrue(diff eq Quantity(FltX("1.5"), Kilogram))
    }

    @Test
    fun zeroCreatesCorrectQuantity() {
        val arith = (DefaultQuantityArithmetic.resolveFor(Flt64.one) as Ok).value
        val zero = arith.zero(Meter)
        assertTrue(zero eq Quantity(Flt64.zero, Meter))
    }
}
