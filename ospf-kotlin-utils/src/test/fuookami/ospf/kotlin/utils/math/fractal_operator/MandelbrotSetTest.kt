package fuookami.ospf.kotlin.utils.math.fractal_operator

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.geometry.point2
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class MandelbrotSetTest {
    @Test
    fun mandelbrotStepShouldMatchDefinition() {
        val set = MandelbrotSet(real = Flt64.zero, imag = Flt64.zero)
        val next = set(point2(Flt64.one, Flt64.one))

        assertTrue(next[0] eq Flt64.zero)
        assertTrue(next[1] eq Flt64.two)
    }

    @Test
    fun constructorShouldKeepCPoint() {
        val set = MandelbrotSet(real = Flt64(0.3), imag = Flt64(-0.5))
        assertTrue(set.c[0] eq Flt64(0.3))
        assertTrue(set.c[1] eq Flt64(-0.5))
    }

    @Test
    fun generatorShouldReturnCurrentThenAdvance() {
        val initial = point2(Flt64.one, Flt64.one)
        val generator = MandelbrotSetGenerator(
            real = Flt64.zero,
            imag = Flt64.zero,
            z = initial
        )

        val current = generator()
        assertTrue(current[0] eq initial[0])
        assertTrue(current[1] eq initial[1])
        assertTrue(generator.z[0] eq Flt64.zero)
        assertTrue(generator.z[1] eq Flt64.two)
    }
}
