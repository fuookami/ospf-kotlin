package fuookami.ospf.kotlin.utils.math.geometry

import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.utils.math.*

class TriangulationTest {
    @Test
    fun triangulate2() {
        val triangles = triangulate(listOf(
            Point2(Flt64.zero, Flt64.zero),
            Point2(Flt64.zero, Flt64.one),
            Point2(Flt64.one, Flt64.zero),
            Point2(Flt64.one, Flt64.one)
        ))
        assert(triangles.size == 2)
    }
}
