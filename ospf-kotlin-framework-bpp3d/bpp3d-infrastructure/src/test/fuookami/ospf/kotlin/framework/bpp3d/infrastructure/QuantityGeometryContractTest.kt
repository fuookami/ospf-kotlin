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
        val width = (FltX(2.0) * Meter) + (FltX(50.0) * Centimeter)
        val height = FltX(3.0) * Meter
        val depth = FltX(4.0) * Meter
        val weight = FltX(12.0) * Kilogram

        val area = width * height
        val volume = area * depth
        val linearDensity = weight / depth

        assertTrue(width eq (FltX(2.5) * Meter))
        assertTrue(area eq (FltX(7.5) * SquareMeter))
        assertTrue(volume eq (FltX(30.0) * CubicMeter))
        assertEquals((Kilogram / Meter).quantity.dimensionSymbol(), linearDensity.unit.quantity.dimensionSymbol())
        assertEquals(3.0, linearDensity.value.toDouble(), 1e-10)
    }

    @Test
    fun quantityPoint2TranslateAndOrder() {
        val point = QuantityPoint2(
            x = FltX(1.0) * Meter,
            y = FltX(2.0) * Meter
        )
        val offset = QuantityVector2(
            x = FltX(50.0) * Centimeter,
            y = FltX(100.0) * Centimeter
        )

        val moved = point + offset
        val restored = moved - offset

        assertTrue(moved.x eq (FltX(1.5) * Meter))
        assertTrue(moved.y eq (FltX(3.0) * Meter))
        assertTrue(restored.x eq point.x)
        assertTrue(restored.y eq point.y)
        assertTrue((point ord moved) is Order.Less)
    }

    @Test
    fun quantityPoint3OrderPriority() {
        val p1 = QuantityPoint3(
            x = FltX(1.0) * Meter,
            y = FltX(2.0) * Meter,
            z = FltX(3.0) * Meter
        )
        val p2 = QuantityPoint3(
            x = FltX(100.0) * Meter,
            y = FltX(0.0) * Meter,
            z = FltX(4.0) * Meter
        )
        val p3 = QuantityPoint3(
            x = FltX(0.0) * Meter,
            y = FltX(3.0) * Meter,
            z = FltX(3.0) * Meter
        )

        assertTrue((p1 ord p2) is Order.Less)
        assertTrue((p1 ord p3) is Order.Less)
    }

    @Test
    fun rectangleIntersectArea() {
        val lhs = Rectangle2(
            minX = FltX(0.0) * Meter,
            minY = FltX(0.0) * Meter,
            maxX = FltX(4.0) * Meter,
            maxY = FltX(3.0) * Meter
        )
        val rhs = Rectangle2(
            minX = FltX(2.0) * Meter,
            minY = FltX(1.0) * Meter,
            maxX = FltX(6.0) * Meter,
            maxY = FltX(5.0) * Meter
        )

        val intersect = lhs.intersect(rhs)
        assertNotNull(intersect)
        assertTrue(intersect.width eq (FltX(2.0) * Meter))
        assertTrue(intersect.height eq (FltX(2.0) * Meter))
        assertTrue(intersect.area eq (FltX(4.0) * SquareMeter))
        assertTrue(lhs.intersectArea(rhs)!! eq (FltX(4.0) * SquareMeter))
    }

    @Test
    fun rectangleNoIntersectWhenOnlyTouchingBorder() {
        val lhs = Rectangle2(
            minX = FltX(0.0) * Meter,
            minY = FltX(0.0) * Meter,
            maxX = FltX(4.0) * Meter,
            maxY = FltX(3.0) * Meter
        )
        val rhs = Rectangle2(
            minX = FltX(4.0) * Meter,
            minY = FltX(1.0) * Meter,
            maxX = FltX(6.0) * Meter,
            maxY = FltX(2.0) * Meter
        )

        assertNull(lhs.intersect(rhs))
        assertNull(lhs.intersectArea(rhs))
    }

    @Test
    fun quantityPointSafeOrderReportsIncomparableUnits() {
        val lhs = QuantityPoint2(
            x = FltX(1.0) * Meter,
            y = FltX(1.0) * Meter
        )
        val rhs = QuantityPoint2(
            x = FltX(1.0) * Kilogram,
            y = FltX(1.0) * Meter
        )

        assertTrue((lhs ordSafe rhs).failed)
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
