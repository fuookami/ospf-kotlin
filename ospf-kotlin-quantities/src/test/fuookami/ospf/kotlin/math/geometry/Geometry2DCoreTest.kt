package fuookami.ospf.kotlin.math.geometry

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.valueOrFail
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

class Geometry2DCoreTest {
    @Test
    fun axisPermutation2ShouldMapRectangleSizeExactly() {
        val rectangle = QuantityRectangle2(
            width = 2.0 * Meter,
            height = 3.0 * Meter
        )

        val swapped = QuantityAxisPermutation2.YX.apply(rectangle)
        val kept = QuantityAxisPermutation2.XY.apply(rectangle)

        assertTrue(swapped.width eq (3.0 * Meter))
        assertTrue(swapped.height eq (2.0 * Meter))
        assertTrue(kept.width eq (2.0 * Meter))
        assertTrue(kept.height eq (3.0 * Meter))
    }

    @Test
    fun circleBoundingBoxShouldMatchDiameter() {
        val circle = QuantityCircle2(radius = 1.5 * Meter)
        val box = circle.boundingBoxAtOrigin()

        assertTrue(circle.area(Flt64(2.0)) eq (4.5 * SquareMeter))
        assertTrue(box.width eq (3.0 * Meter))
        assertTrue(box.height eq (3.0 * Meter))
    }

    @Test
    fun box2RectangleContainsOverlapAndIntersectionShouldWork() {
        val lhs = QuantityBox2(
            x = 0.0 * Meter,
            y = 0.0 * Meter,
            shape = QuantityRectangle2(width = 4.0 * Meter, height = 3.0 * Meter)
        )
        val rhs = QuantityBox2(
            x = 3.0 * Meter,
            y = 1.0 * Meter,
            shape = QuantityRectangle2(width = 2.0 * Meter, height = 3.0 * Meter)
        )

        assertTrue(lhs.contains(x = 1.0 * Meter, y = 1.0 * Meter).value!!)
        assertTrue(lhs.maxX().value!! eq (4.0 * Meter))
        assertTrue(lhs.maxY().value!! eq (3.0 * Meter))
        assertTrue(lhs.overlapped(rhs).value!!)
        val intersection = lhs.intersect(rhs).valueOrFail()
        assertNotNull(intersection)
        assertTrue(intersection.width eq (1.0 * Meter))
        assertTrue(intersection.height eq (2.0 * Meter))
    }

    @Test
    fun box2CircleCircleAndCircleRectangleRelationShouldWork() {
        val circleA = QuantityBox2(
            x = 0.0 * Meter,
            y = 0.0 * Meter,
            shape = QuantityCircle2(radius = 1.0 * Meter)
        )
        val circleB = QuantityBox2(
            x = 1.0 * Meter,
            y = 0.0 * Meter,
            shape = QuantityCircle2(radius = 1.0 * Meter)
        )
        val farCircle = QuantityBox2(
            x = 5.0 * Meter,
            y = 5.0 * Meter,
            shape = QuantityCircle2(radius = 1.0 * Meter)
        )
        val rectangle = QuantityBox2(
            x = 1.5 * Meter,
            y = 1.5 * Meter,
            shape = QuantityRectangle2(width = 1.0 * Meter, height = 1.0 * Meter)
        )

        assertTrue(circleA.overlapped(circleB).value!!)
        assertTrue(!circleA.overlapped(farCircle).value!!)
        assertTrue(circleB.overlapped(rectangle).value!!)
        val intersection = farCircle.intersect(rectangle)
        assertTrue(intersection.ok)
        assertNull(intersection.value)
    }
}
