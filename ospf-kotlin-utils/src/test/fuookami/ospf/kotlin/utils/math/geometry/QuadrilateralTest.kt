package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class QuadrilateralTest {
    @Test
    fun perimeterAndDiagonalsShouldMatchRectangle() {
        val q = quadrilateral2(
            point2(),
            point2(Flt64(4.0), Flt64.zero),
            point2(Flt64(4.0), Flt64(3.0)),
            point2(Flt64.zero, Flt64(3.0))
        )

        assertTrue(q.perimeter eq Flt64(14.0))
        assertTrue(q.diagonals[0].length eq Flt64(5.0))
        assertTrue(q.diagonals[1].length eq Flt64(5.0))
    }

    @Test
    fun areaShouldMatchTriangleDecompositionForBothOrders() {
        val clockwise = quadrilateral2(
            point2(),
            point2(Flt64(4.0), Flt64.zero),
            point2(Flt64(4.0), Flt64(3.0)),
            point2(Flt64.zero, Flt64(3.0))
        )
        val counterClockwise = quadrilateral2(
            point2(),
            point2(Flt64.zero, Flt64(3.0)),
            point2(Flt64(4.0), Flt64(3.0)),
            point2(Flt64(4.0), Flt64.zero)
        )

        assertTrue(clockwise.area eq Flt64(12.0))
        assertTrue(clockwise.area eq clockwise.areaByTriangles)
        assertTrue(counterClockwise.area eq Flt64(12.0))
        assertTrue(counterClockwise.area eq counterClockwise.areaByTriangles)
    }

    @Test
    fun convexityShouldDistinguishConvexAndConcave() {
        val convex = quadrilateral2(
            point2(),
            point2(Flt64(4.0), Flt64.zero),
            point2(Flt64(4.0), Flt64(3.0)),
            point2(Flt64.zero, Flt64(3.0))
        )
        val concave = quadrilateral2(
            point2(),
            point2(Flt64(4.0), Flt64.zero),
            point2(Flt64.one, Flt64.one),
            point2(Flt64.zero, Flt64(4.0))
        )

        assertTrue(convex.isConvex())
        assertTrue(!concave.isConvex())
    }

    @Test
    fun centroidShouldBeAverageOfVertices() {
        val q = quadrilateral2(
            point2(),
            point2(Flt64(4.0), Flt64.zero),
            point2(Flt64(4.0), Flt64(2.0)),
            point2(Flt64.zero, Flt64(2.0))
        )

        assertTrue(q.centroid.x eq Flt64(2.0))
        assertTrue(q.centroid.y eq Flt64.one)
    }

    @Test
    fun degenerateQuadrilateralShouldBeIllegal() {
        val q = quadrilateral2(
            point2(),
            point2(Flt64.one, Flt64.one),
            point2(Flt64(2.0), Flt64(2.0)),
            point2(Flt64(3.0), Flt64(3.0))
        )

        assertTrue(q.area eq Flt64.zero)
        assertTrue(q.illegal)
    }
}
