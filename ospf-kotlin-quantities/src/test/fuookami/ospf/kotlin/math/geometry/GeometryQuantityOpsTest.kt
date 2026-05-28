package fuookami.ospf.kotlin.math.geometry

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

class GeometryQuantityOpsTest {
    @Test
    fun quantityHelpersShouldWorkForFlt64() {
        val lhs = 1.0 * Meter
        val rhs = 50.0 * Centimeter

        assertTrue(quantityPlus(lhs, rhs) eq (1.5 * Meter))
        assertTrue(quantityMinus(lhs, rhs) eq (0.5 * Meter))
        assertTrue(quantityOrd(lhs, rhs, "x") is Order.Greater)
        assertTrue(quantityMax(lhs, rhs, "x") eq lhs)
        assertTrue(quantityMin(lhs, rhs, "x") eq rhs)
        assertTrue(quantityClamp(2.0 * Meter, lhs, 3.0 * Meter, "x") eq (2.0 * Meter))
        assertTrue(quantityClamp(0.2 * Meter, lhs, 3.0 * Meter, "x") eq lhs)
        assertTrue(quantityContainsInRange(2.0 * Meter, lhs, 3.0 * Meter, true, false, "x"))
        assertTrue(quantityZeroOf(lhs) eq (0.0 * Meter))
    }

    @Test
    fun quantityHelpersShouldWorkForFltX() {
        val lhs = Quantity(FltX(1.0), Meter)
        val rhs = Quantity(FltX(50.0), Centimeter)

        assertTrue(quantityPlus(lhs, rhs) eq Quantity(FltX(1.5), Meter))
        assertTrue(quantityMinus(lhs, rhs) eq Quantity(FltX(0.5), Meter))
        assertTrue(quantityOrd(lhs, rhs, "x") is Order.Greater)
        assertTrue(quantityMax(lhs, rhs, "x") eq lhs)
        assertTrue(quantityMin(lhs, rhs, "x") eq rhs)
        assertTrue(quantityClamp(Quantity(FltX(2.0), Meter), lhs, Quantity(FltX(3.0), Meter), "x") eq Quantity(FltX(2.0), Meter))
        assertTrue(quantityClamp(Quantity(FltX(0.2), Meter), lhs, Quantity(FltX(3.0), Meter), "x") eq lhs)
        assertTrue(quantityContainsInRange(Quantity(FltX(2.0), Meter), lhs, Quantity(FltX(3.0), Meter), true, false, "x"))
        assertTrue(quantityZeroOf(lhs) eq Quantity(FltX.zero, Meter))
    }
}
