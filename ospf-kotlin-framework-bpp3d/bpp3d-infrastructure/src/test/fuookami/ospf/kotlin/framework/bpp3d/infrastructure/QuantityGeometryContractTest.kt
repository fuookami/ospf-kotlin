/**
 * 物理量几何契约测试。
 * Quantity geometry contract test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

class QuantityGeometryContractTest {
    @Test
    fun quantityArithmeticShouldRespectUnits() {
        val width = (fltX(2.0) * Meter) + (fltX(50.0) * Centimeter)
        val height = fltX(3.0) * Meter
        val depth = fltX(4.0) * Meter
        val weight = fltX(12.0) * Kilogram

        val area = width * height
        val volume = area * depth
        val linearDensity = weight / depth

        assertTrue(width eq (fltX(2.5) * Meter))
        assertTrue(area eq (fltX(7.5) * SquareMeter))
        assertTrue(volume eq (fltX(30.0) * CubicMeter))
        assertEquals((Kilogram / Meter).quantity.dimensionSymbol(), linearDensity.unit.quantity.dimensionSymbol())
        assertEquals(3.0, linearDensity.value.toDouble(), 1e-10)
    }

    @Test
    fun quantityPoint2TranslateAndOrder() {
        val point = QuantityPoint2(
            x = fltX(1.0) * Meter,
            y = fltX(2.0) * Meter
        )
        val offset = QuantityVector2(
            x = fltX(50.0) * Centimeter,
            y = fltX(100.0) * Centimeter
        )

        val moved = point + offset
        val restored = moved - offset

        assertTrue(moved.x eq (fltX(1.5) * Meter))
        assertTrue(moved.y eq (fltX(3.0) * Meter))
        assertTrue(restored.x eq point.x)
        assertTrue(restored.y eq point.y)
        assertTrue((point ord moved) is Order.Less)
    }

    @Test
    fun quantityPoint3OrderPriority() {
        val p1 = QuantityPoint3(
            x = fltX(1.0) * Meter,
            y = fltX(2.0) * Meter,
            z = fltX(3.0) * Meter
        )
        val p2 = QuantityPoint3(
            x = fltX(100.0) * Meter,
            y = fltX(0.0) * Meter,
            z = fltX(4.0) * Meter
        )
        val p3 = QuantityPoint3(
            x = fltX(0.0) * Meter,
            y = fltX(3.0) * Meter,
            z = fltX(3.0) * Meter
        )

        assertTrue((p1 ord p2) is Order.Less)
        assertTrue((p1 ord p3) is Order.Less)
    }

    @Test
    fun rectangleIntersectArea() {
        val lhs = Rectangle2(
            minX = fltX(0.0) * Meter,
            minY = fltX(0.0) * Meter,
            maxX = fltX(4.0) * Meter,
            maxY = fltX(3.0) * Meter
        )
        val rhs = Rectangle2(
            minX = fltX(2.0) * Meter,
            minY = fltX(1.0) * Meter,
            maxX = fltX(6.0) * Meter,
            maxY = fltX(5.0) * Meter
        )

        val intersect = lhs.intersect(rhs)
        assertNotNull(intersect)
        assertTrue(intersect.width eq (fltX(2.0) * Meter))
        assertTrue(intersect.height eq (fltX(2.0) * Meter))
        assertTrue(intersect.area eq (fltX(4.0) * SquareMeter))
        assertTrue(lhs.intersectArea(rhs)!! eq (fltX(4.0) * SquareMeter))
    }

    @Test
    fun rectangleNoIntersectWhenOnlyTouchingBorder() {
        val lhs = Rectangle2(
            minX = fltX(0.0) * Meter,
            minY = fltX(0.0) * Meter,
            maxX = fltX(4.0) * Meter,
            maxY = fltX(3.0) * Meter
        )
        val rhs = Rectangle2(
            minX = fltX(4.0) * Meter,
            minY = fltX(1.0) * Meter,
            maxX = fltX(6.0) * Meter,
            maxY = fltX(2.0) * Meter
        )

        assertNull(lhs.intersect(rhs))
        assertNull(lhs.intersectArea(rhs))
    }

    @Test
    fun quantityPointRejectsIncomparableUnits() {
        val lhs = QuantityPoint2(
            x = fltX(1.0) * Meter,
            y = fltX(1.0) * Meter
        )
        val rhs = QuantityPoint2(
            x = fltX(1.0) * Kilogram,
            y = fltX(1.0) * Meter
        )

        assertFailsWith<IllegalArgumentException> {
            lhs ord rhs
        }
    }

    @Test
    fun quantityGeometryShouldSupportFltX() {
        val base = point2(
            x = Quantity(FltX(1.0), Meter),
            y = Quantity(FltX(2.0), Meter)
        )
        val offset = vector2(
            x = Quantity(FltX(0.5), Meter),
            y = Quantity(FltX(1.0), Meter)
        )
        val moved = base + offset

        assertTrue(moved.x eq Quantity(FltX(1.5), Meter))
        assertTrue(moved.y eq Quantity(FltX(3.0), Meter))

        val lhs = Rectangle2(
            minX = Quantity(FltX.zero, Meter),
            minY = Quantity(FltX.zero, Meter),
            maxX = Quantity(FltX(4.0), Meter),
            maxY = Quantity(FltX(3.0), Meter)
        )
        val rhs = Rectangle2(
            minX = Quantity(FltX(2.0), Meter),
            minY = Quantity(FltX(1.0), Meter),
            maxX = Quantity(FltX(6.0), Meter),
            maxY = Quantity(FltX(5.0), Meter)
        )

        val intersect = lhs.intersect(rhs)
        assertNotNull(intersect)
        assertTrue(intersect.area eq Quantity(FltX(4.0), SquareMeter))
    }
}
