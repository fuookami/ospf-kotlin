package fuookami.ospf.kotlin.math.chaotic_operator

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ChaoticScalarGeneratorTest {
    @Test
    fun circleMapGeneratorShouldReturnCurrentThenAdvance() {
        val generator = CircleMapGenerator(
            alpha = Flt64(0.2),
            beta = Flt64(0.4),
            x = Flt64(0.3)
        )
        val current = generator()
        assertTrue(current eq Flt64(0.3))
        assertTrue(generator.x geq Flt64.zero)
        assertTrue(generator.x ls Flt64.one)
    }

    @Test
    fun gaussMapGeneratorShouldReturnCurrentThenAdvance() {
        val generator = GaussMapGenerator(
            mu = Flt64.two,
            x = Flt64(0.5)
        )
        val current = generator()
        assertTrue(current eq Flt64(0.5))
        assertTrue(generator.x eq Flt64(4.0))
    }

    @Test
    fun chebyshevMapGeneratorShouldReturnCurrentThenAdvance() {
        val generator = ChebyshevMapGenerator(
            a = Flt64.three,
            x = Flt64(0.5)
        )
        val current = generator()
        assertTrue(current eq Flt64(0.5))
        assertTrue(generator.x eq Flt64(-1.0))
    }
}
