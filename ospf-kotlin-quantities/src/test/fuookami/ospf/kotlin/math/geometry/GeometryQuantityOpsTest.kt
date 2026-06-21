package fuookami.ospf.kotlin.math.geometry

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.orFail
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

class GeometryQuantityOpsTest {
    @Test
    fun quantityHelpersShouldWorkForFlt64() {
        val lhs = 1.0 * Meter
        val rhs = 50.0 * Centimeter

        assertTrue((lhs + rhs).orFail() eq (1.5 * Meter))
        assertTrue((lhs - rhs).orFail() eq (0.5 * Meter))
        assertTrue(lhs.partialOrd(rhs) is Order.Greater)
        assertTrue(lhs eq (100.0 * Centimeter))
    }

    @Test
    fun quantityHelpersShouldWorkForFltX() {
        val lhs = Quantity(FltX(1.0), Meter)
        val rhs = Quantity(FltX(50.0), Centimeter)

        assertTrue((lhs + rhs).orFail() eq Quantity(FltX(1.5), Meter))
        assertTrue((lhs - rhs).orFail() eq Quantity(FltX(0.5), Meter))
        assertTrue(lhs.partialOrd(rhs) is Order.Greater)
        assertTrue(lhs eq Quantity(FltX(100.0), Centimeter))
    }
}
