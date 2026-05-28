package fuookami.ospf.kotlin.math.chaotic

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.*

class ChaoticOperatorTest {
    @Test
    fun lorenzStepShouldBeDeterministicForGivenInput() {
        val system = LorenzSystem()
        val next = system(point3(Flt64.one, Flt64.one, Flt64.one))
        assertTrue(next.x eq Flt64.one)
        assertTrue(next.y eq Flt64(1.0066666666666666))
        assertTrue(next.z eq Flt64(0.73))
    }

    @Test
    fun chenStepShouldBeDeterministicForGivenInput() {
        val system = ChenSystem()
        val next = system(point3(Flt64.one, Flt64.one, Flt64.one))
        assertTrue(next.x eq Flt64.one)
        assertTrue(next.y eq Flt64(1.438))
        assertTrue(next.z eq Flt64(0.9833333333333333))
    }

    @Test
    fun circleMapShouldStayInUnitInterval() {
        val map = CircleMap(alpha = Flt64(0.2), beta = Flt64(0.4))
        val value = map(Flt64(0.3))
        assertTrue(value geq Flt64.zero)
        assertTrue(value ls Flt64.one)
    }

    @Test
    fun gaussMapShouldHandleZeroAndNonZeroInput() {
        val map = GaussMap(mu = Flt64.two)
        assertTrue(map(Flt64.zero) eq Flt64.zero)
        assertTrue(map(Flt64(0.5)) eq Flt64(4.0))
    }

    @Test
    fun chebyshevMapShouldRespectDomain() {
        val map = ChebyshevMap(a = Flt64.three)
        assertTrue(map(Flt64(0.5)) eq Flt64(-1.0))
        assertTrue(map(Flt64(2.0)) eq Flt64.zero)
    }

    @Test
    fun generatorShouldReturnCurrentThenAdvance() {
        val initial = point3(Flt64.one, Flt64.one, Flt64.one)
        val generator = LorenzSystemGenerator(
            a = Flt64(10.0),
            b = Flt64(28.0),
            c = Flt64(8.0 / 3.0),
            h = Flt64(0.01),
            x = initial
        )
        val current = generator()
        assertTrue(current.x eq initial.x)
        assertTrue(current.y eq initial.y)
        assertTrue(current.z eq initial.z)
        assertTrue(generator.x.y eq Flt64(1.0066666666666666))
    }
}
