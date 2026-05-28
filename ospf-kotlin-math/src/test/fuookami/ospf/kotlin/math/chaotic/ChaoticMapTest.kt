package fuookami.ospf.kotlin.math.chaotic

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.point2

class ChaoticMapTest {
    @Test
    fun arnoldsCatMapShouldMatchDefinition() {
        val next = ArnoldsCatMap(point2(Flt64(0.2), Flt64(0.3)))
        assertTrue(next[0] eq Flt64(0.7))
        assertTrue(next[1] eq Flt64(0.5))
    }

    @Test
    fun bakersMapShouldMatchDefinition() {
        val next = BakersMap(point2(Flt64(0.6), Flt64(0.3)))
        assertTrue(next[0] eq Flt64(0.2))
        assertTrue(next[1] eq Flt64(0.65))
    }

    @Test
    fun bogdanovMapShouldMatchDefinition() {
        val map = BogdanovMap(
            epsilon = Flt64(0.1),
            kappa = Flt64(0.2),
            mu = Flt64(0.3)
        )
        val next = map(point2(Flt64(0.5), Flt64(0.4)))
        assertTrue(next[0] eq Flt64(1.05))
        assertTrue(next[1] eq Flt64(0.55))
    }

    @Test
    fun arnoldsCatMapGeneratorShouldReturnCurrentThenAdvance() {
        val initial = point2(Flt64(0.2), Flt64(0.3))
        val generator = ArnoldsCatMapGenerator(x = initial)
        val current = generator()
        assertTrue(current[0] eq initial[0])
        assertTrue(current[1] eq initial[1])
        assertTrue(generator.x[0] eq Flt64(0.7))
        assertTrue(generator.x[1] eq Flt64(0.5))
    }

    @Test
    fun bakersMapGeneratorShouldReturnCurrentThenAdvance() {
        val initial = point2(Flt64(0.6), Flt64(0.3))
        val generator = BakersMapGenerator(x = initial)
        val current = generator()
        assertTrue(current[0] eq initial[0])
        assertTrue(current[1] eq initial[1])
        assertTrue(generator.x[0] eq Flt64(0.2))
        assertTrue(generator.x[1] eq Flt64(0.65))
    }
}
