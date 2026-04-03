package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

        val angle = xAxis.angle(yAxis)

        assertNotNull(angle)
        assertTrue(angle eq (Flt64.pi / Flt64.two))
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
    fun crossShouldWorkFor2DAnd3D() {
        val cross2 = vector2(Flt64.one, Flt64.zero) cross vector2(Flt64.zero, Flt64.one)
        assertTrue(cross2 eq Flt64.one)

        val cross3 = vector3(Flt64.one, Flt64.zero, Flt64.zero) cross vector3(Flt64.zero, Flt64.one, Flt64.zero)
        assertTrue(cross3.x eq Flt64.zero)
        assertTrue(cross3.y eq Flt64.zero)
        assertTrue(cross3.z eq Flt64.one)
    }
}
