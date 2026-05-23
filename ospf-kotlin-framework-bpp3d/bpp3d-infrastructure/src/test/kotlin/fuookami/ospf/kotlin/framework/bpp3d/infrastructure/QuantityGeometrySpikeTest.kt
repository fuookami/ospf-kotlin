package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.CubicMeter
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.SquareMeter
import fuookami.ospf.kotlin.quantities.unit.Centimeter
import fuookami.ospf.kotlin.quantities.unit.div
import fuookami.ospf.kotlin.utils.functional.Order
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuantityGeometrySpikeTest {
    @Test
    fun quantityArithmeticSpike() {
        val width = (2.0 * Meter) + (50.0 * Centimeter)
        val height = 3.0 * Meter
        val depth = 4.0 * Meter
        val weight = 12.0 * Kilogram

        val area = width * height
        val volume = area * depth
        val linearDensity = weight / depth

        assertTrue(width eq (2.5 * Meter))
        assertTrue(area eq (7.5 * SquareMeter))
        assertTrue(volume eq (30.0 * CubicMeter))
        assertEquals((Kilogram / Meter).quantity.dimensionSymbol(), linearDensity.unit.quantity.dimensionSymbol())
        assertEquals(3.0, linearDensity.value.toDouble(), 1e-10)
    }

    @Test
    fun quantityPoint2TranslateAndOrder() {
        val point = QuantityPoint2(
            x = 1.0 * Meter,
            y = 2.0 * Meter
        )
        val offset = QuantityVector2(
            x = 50.0 * Centimeter,
            y = 100.0 * Centimeter
        )

        val moved = point + offset
        val restored = moved - offset

        assertTrue(moved.x eq (1.5 * Meter))
        assertTrue(moved.y eq (3.0 * Meter))
        assertTrue(restored.x eq point.x)
        assertTrue(restored.y eq point.y)
        assertTrue((point ord moved) is Order.Less)
    }

    @Test
    fun quantityPoint3OrderPriority() {
        val p1 = QuantityPoint3(
            x = 1.0 * Meter,
            y = 2.0 * Meter,
            z = 3.0 * Meter
        )
        val p2 = QuantityPoint3(
            x = 100.0 * Meter,
            y = 0.0 * Meter,
            z = 4.0 * Meter
        )
        val p3 = QuantityPoint3(
            x = 0.0 * Meter,
            y = 3.0 * Meter,
            z = 3.0 * Meter
        )

        assertTrue((p1 ord p2) is Order.Less)
        assertTrue((p1 ord p3) is Order.Less)
    }

    @Test
    fun quantityRectangleIntersectArea() {
        val lhs = QuantityRectangle2(
            minX = 0.0 * Meter,
            minY = 0.0 * Meter,
            maxX = 4.0 * Meter,
            maxY = 3.0 * Meter
        )
        val rhs = QuantityRectangle2(
            minX = 2.0 * Meter,
            minY = 1.0 * Meter,
            maxX = 6.0 * Meter,
            maxY = 5.0 * Meter
        )

        val intersect = lhs.intersect(rhs)
        assertNotNull(intersect)
        assertTrue(intersect.width eq (2.0 * Meter))
        assertTrue(intersect.height eq (2.0 * Meter))
        assertTrue(intersect.area eq (4.0 * SquareMeter))
        assertTrue(lhs.intersectArea(rhs)!! eq (4.0 * SquareMeter))
    }

    @Test
    fun quantityRectangleNoIntersectWhenOnlyTouchingBorder() {
        val lhs = QuantityRectangle2(
            minX = 0.0 * Meter,
            minY = 0.0 * Meter,
            maxX = 4.0 * Meter,
            maxY = 3.0 * Meter
        )
        val rhs = QuantityRectangle2(
            minX = 4.0 * Meter,
            minY = 1.0 * Meter,
            maxX = 6.0 * Meter,
            maxY = 2.0 * Meter
        )

        assertNull(lhs.intersect(rhs))
        assertNull(lhs.intersectArea(rhs))
    }

    @Test
    fun quantityPointRejectsIncomparableUnits() {
        val lhs = QuantityPoint2(
            x = 1.0 * Meter,
            y = 1.0 * Meter
        )
        val rhs = QuantityPoint2(
            x = 1.0 * Kilogram,
            y = 1.0 * Meter
        )

        assertFailsWith<IllegalArgumentException> {
            lhs ord rhs
        }
    }
}
