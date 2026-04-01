package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
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

    @Test
    fun triangulateIsolinesShouldPreserveBothLineHeights() {
        val low = Flt64(10.0)
        val high = Flt64(20.0)

        val triangles = triangulate(
            listOf(
                low to listOf(
                    point2(Flt64.zero, Flt64.zero),
                    point2(Flt64.one, Flt64.zero),
                    point2(Flt64(0.5), Flt64(0.2))
                ),
                high to listOf(
                    point2(Flt64.zero, Flt64.one),
                    point2(Flt64.one, Flt64.one),
                    point2(Flt64(0.5), Flt64(1.2))
                )
            )
        )

        assert(triangles.isNotEmpty())
        val zValues = triangles.flatMap { triangle -> listOf(triangle.p1.z, triangle.p2.z, triangle.p3.z) }
        assert(zValues.any { it == low })
        assert(zValues.any { it == high })
    }

    @Test
    fun triangulateShouldHandleDuplicatePoints() {
        val triangles = triangulate(
            listOf(
                point2(Flt64.zero, Flt64.zero),
                point2(Flt64.one, Flt64.zero),
                point2(Flt64.zero, Flt64.one),
                point2(Flt64.one, Flt64.one),
                point2(Flt64.one, Flt64.one)
            )
        )

        assert(triangles.isNotEmpty())
    }

    @Test
    fun triangulateShouldHandleNearCollinearPoints() {
        val triangles = triangulate(
            listOf(
                point2(Flt64.zero, Flt64.zero),
                point2(Flt64(1.0), Flt64(1e-12)),
                point2(Flt64(2.0), Flt64(2e-12)),
                point2(Flt64(1.0), Flt64.one)
            )
        )

        assert(triangles.isNotEmpty())
    }
}



