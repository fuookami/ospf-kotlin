package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.geometry.point2

class RectangleTest {
    @Test
    fun rectangle() {
        val rectangle = Rectangle(point2(Flt64.zero, Flt64.zero), point2(Flt64.one, Flt64.one))
        assert(rectangle.length eq Flt64.one)
        assert(rectangle.width eq Flt64.one)
        assert(rectangle.area eq Flt64.one)
    }
}


