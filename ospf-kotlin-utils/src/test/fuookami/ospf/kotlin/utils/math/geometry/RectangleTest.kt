package fuookami.ospf.kotlin.utils.math.geometry

import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.utils.math.*

class RectangleTest {
    @Test
    fun rectangle2() {
        val rectangle = Rectangle2(Point2(Flt64.zero, Flt64.zero), Point2(Flt64.one, Flt64.one))
        assert(rectangle.length eq Flt64.one)
        assert(rectangle.width eq Flt64.one)
        assert(rectangle.area eq Flt64.one)
    }
}
