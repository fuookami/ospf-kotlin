package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.math.geometry.point2
import fuookami.ospf.kotlin.math.geometry.vector2

class GeometryPrimitiveTest {
    @Test
    fun pointWithVectorRoundTrip() {
        val p = point2(Flt64(1.0), Flt64(2.0))
        val v = vector2(Flt64(3.0), Flt64(-4.0))

        val moved = p + v
        val restored = moved - v

        assertTrue(moved.x eq Flt64(4.0))
        assertTrue(moved.y eq Flt64(-2.0))
        assertTrue(restored eq p)
    }

    @Test
    fun distanceFamiliesShouldMatchKnownValues() {
        val a = point2(Flt64.one, Flt64.one)
        val b = point2(Flt64(4.0), Flt64(5.0))

        val euclidean = a distance b
        val manhattan = a.distanceBetween(b, Distance.Manhattan)
        val chebyshev = a.distanceBetween(b, Distance.Chebyshev)
        val minkowski3 = a.distanceBetween(b, Distance.Minkowski(3))

        assertTrue(euclidean eq Flt64(5.0))
        assertTrue(manhattan eq Flt64(7.0))
        assertTrue(chebyshev eq Flt64(4.0))
        assertTrue(minkowski3 eq Flt64(91.0).pow(Flt64(1.0 / 3.0)).toFlt64())
    }

    @Test
    fun edgeLengthShouldSupportCustomDistance() {
        val edge = Edge<Point<Dim2, Flt64>, Dim2, Flt64>(point2(), point2(Flt64(3.0), Flt64(4.0)))

        assertTrue(edge.length eq Flt64(5.0))
        assertTrue(edge.length(Distance.Manhattan) eq Flt64(7.0))
        assertTrue(edge.vector.x eq Flt64(3.0))
        assertTrue(edge.vector.y eq Flt64(4.0))
    }

    @Test
    fun triangleDegenerateAreaAndIllegalState() {
        val collinear = Triangle<Point<Dim2, Flt64>, Dim2, Flt64>(
            point2(),
            point2(Flt64.one, Flt64.one),
            point2(Flt64(2.0), Flt64(2.0))
        )
        val verticalLine = Triangle<Point<Dim2, Flt64>, Dim2, Flt64>(
            point2(),
            point2(Flt64.zero, Flt64.one),
            point2(Flt64.zero, Flt64(2.0))
        )

        assertTrue(collinear.area eq Flt64.zero)
        assertTrue(!collinear.illegal)
        assertTrue(verticalLine.illegal)
    }

    @Test
    fun circumcircleOfRightTriangle() {
        val triangle = Triangle<Point<Dim2, Flt64>, Dim2, Flt64>(
            point2(),
            point2(Flt64(2.0), Flt64.zero),
            point2(Flt64.zero, Flt64(2.0))
        )

        val circle = Circle.circumcircleOf(triangle)

        assertTrue(circle.x eq Flt64.one)
        assertTrue(circle.y eq Flt64.one)
        assertTrue(circle.radius eq Flt64(2.0).sqrt())
    }

    @Test
    fun circleConstructedByRadiusVectorShouldNormalizeDirection() {
        val circle = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(
            center = point2(),
            radiusVec = vector2(Flt64(3.0), Flt64(4.0))
        )

        assertTrue(circle.radius eq Flt64(5.0))
        assertTrue(circle.direction.x eq Flt64(3.0 / 5.0))
        assertTrue(circle.direction.y eq Flt64(4.0 / 5.0))
    }
}