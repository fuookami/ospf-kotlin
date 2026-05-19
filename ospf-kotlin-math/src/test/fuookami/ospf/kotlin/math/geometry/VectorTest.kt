package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.InnerProductSpace
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.math.geometry.vector2
import fuookami.ospf.kotlin.math.geometry.vector3

class VectorTest {
    @Test
    fun minusShouldSubtractByCoordinate() {
        val lhs = vector2(Flt64(3.0), Flt64(1.0))
        val rhs = vector2(Flt64(2.0), Flt64(5.0))

        val diff = lhs - rhs

        assertEquals(Flt64.one, diff.x)
        assertEquals(Flt64(-4.0), diff.y)
    }

    @Test
    fun angleShouldReturnRightAngle() {
        val xAxis = vector2(Flt64.one, Flt64.zero)
        val yAxis = vector2(Flt64.zero, Flt64.one)

        val angle = xAxis.angle(yAxis) as? Flt64

        assertNotNull(angle)
        assertTrue(angle!! eq (Flt64.pi / Flt64.two))
    }

    @Test
    fun innerProductSpaceDefaultAngleShouldBeAvailableThroughInterface() {
        val xAxis = vector2(Flt64.one, Flt64.zero)
        val yAxis = vector2(Flt64.zero, Flt64.one)
        val zero = vector2(Flt64.zero, Flt64.zero)
        val space: InnerProductSpace<Vector<Dim2, Flt64>, Flt64> = xAxis

        val angle = space.angle(yAxis) as? Flt64

        assertNotNull(angle)
        assertTrue(angle!! eq (Flt64.pi / Flt64.two))
        assertEquals(null, zero.angle(yAxis))
    }

    @Test
    fun projectionShouldProjectOnAxis() {
        val vector = vector2(Flt64(3.0), Flt64(4.0))
        val xAxis = vector2(Flt64.one, Flt64.zero)

        val projection = vector.projectionOn(xAxis)

        assertNotNull(projection)
        assertTrue(projection.x eq Flt64(3.0))
        assertTrue(projection.y eq Flt64.zero)
    }

    @Test
    fun innerProductDefaultsShouldProvideOrthogonalHelpers() {
        val xAxis = vector2(Flt64.one, Flt64.zero)
        val yAxis = vector2(Flt64.zero, Flt64.one)
        val vector = vector2(Flt64(3.0), Flt64(4.0))

        assertTrue(xAxis.isOrthogonal(yAxis, Flt64.epsilon))
        assertFalse(vector.isOrthogonal(xAxis, Flt64.epsilon))

        val orthogonal = vector.orthogonalComponent(yAxis)
        assertNotNull(orthogonal)
        assertTrue(orthogonal.x eq Flt64(3.0))
        assertTrue(orthogonal.y eq Flt64.zero)
    }

    @Test
    fun normedSpaceDefaultsShouldProvideSquaredNormAndNormalize() {
        val vector = vector2(Flt64(3.0), Flt64(4.0))
        val zero = vector2(Flt64.zero, Flt64.zero)

        assertTrue(vector.normSquared() eq Flt64(25.0))

        val normalized = vector.normalize()
        assertNotNull(normalized)
        assertTrue(normalized.norm eq Flt64.one)

        assertEquals(null, zero.normalize())
    }

    @Test
    fun crossShouldWorkFor2DAnd3D() {
        val cross2 = vector2(Flt64.one, Flt64.zero) cross vector2(Flt64.zero, Flt64.one)
        assertTrue(cross2 eq Flt64.one)

        val cross3 = vector3(Flt64.one, Flt64.zero, Flt64.zero) cross vector3(Flt64.zero, Flt64.one, Flt64.zero)
        assertTrue(cross3.x eq Flt64.zero)
        assertTrue(cross3.y eq Flt64.zero)
        assertTrue(cross3.z eq Flt64.one)
    }
}