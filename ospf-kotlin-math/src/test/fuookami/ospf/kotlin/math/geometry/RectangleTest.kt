package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.geometry.point2
import kotlin.test.assertTrue

class RectangleTest {
    @Test
    fun rectangle() {
        val rectangle = Rectangle(point2(Flt64.zero, Flt64.zero), point2(Flt64.one, Flt64.one))
        assertTrue(rectangle.length eq Flt64.one)
        assertTrue(rectangle.width eq Flt64.one)
        assertTrue(rectangle.area eq Flt64.one)
    }

    @Test
    fun rectangleShouldNormalizeCornerOrder() {
        val rectangle = Rectangle(
            point2(Flt64(3.0), Flt64(5.0)),
            point2(Flt64.one, Flt64(2.0))
        )

        assertTrue(rectangle.leftUpperPoint.x eq Flt64.one)
        assertTrue(rectangle.leftUpperPoint.y eq Flt64(2.0))
        assertTrue(rectangle.rightBottomPoint.x eq Flt64(3.0))
        assertTrue(rectangle.rightBottomPoint.y eq Flt64(5.0))
        assertTrue(rectangle.length eq Flt64(3.0))
        assertTrue(rectangle.width eq Flt64(2.0))
        assertTrue(rectangle.area eq Flt64(6.0))
    }
}


