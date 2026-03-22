package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.Flt64
import org.junit.jupiter.api.Test

class TriangulationTest {
    @Test
    fun triangulate2() {
        val triangles = triangulate(
            listOf(
                point2(),
                point2(y = Flt64.one),
                point2(x = Flt64.one),
                point2(x = Flt64.one, y = Flt64.one)
            )
        )
        assert(triangles.size == 2)
    }
}
