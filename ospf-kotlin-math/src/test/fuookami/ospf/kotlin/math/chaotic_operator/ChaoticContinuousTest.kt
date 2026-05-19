package fuookami.ospf.kotlin.math.chaotic_operator

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Dim2
import fuookami.ospf.kotlin.math.geometry.Dim3
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.math.geometry.point2
import fuookami.ospf.kotlin.math.geometry.point3

class ChaoticContinuousTest {
    @Test
    fun doublePendulumStepShouldKeepFiniteValues() {
        val system = DoublePendulumSystem()
        val (x1, y1) = system(
            x = point2(Flt64(0.2), Flt64(0.1)),
            y = point2(Flt64(0.3), Flt64(0.2))
        )

        assertTrue(x1[0].toDouble().isFinite())
        assertTrue(x1[1].toDouble().isFinite())
        assertTrue(y1[0].toDouble().isFinite())
        assertTrue(y1[1].toDouble().isFinite())
    }

    @Test
    fun chuaCircuitShouldKeepOriginFixed() {
        val system = ChuaCircuit()
        val next = system(point3(Flt64.zero, Flt64.zero, Flt64.zero))
        assertTrue(next[0] eq Flt64.zero)
        assertTrue(next[1] eq Flt64.zero)
        assertTrue(next[2] eq Flt64.zero)
    }

    @Test
    fun coupledLorenzShouldKeepOriginPairFixed() {
        val system = CoupledLorenzAttractor()
        val next = system(point3(Flt64.zero, Flt64.zero, Flt64.zero) to point3(Flt64.zero, Flt64.zero, Flt64.zero))
        assertTrue(next.first[0] eq Flt64.zero)
        assertTrue(next.first[1] eq Flt64.zero)
        assertTrue(next.first[2] eq Flt64.zero)
        assertTrue(next.second[0] eq Flt64.zero)
        assertTrue(next.second[1] eq Flt64.zero)
        assertTrue(next.second[2] eq Flt64.zero)
    }

    @Test
    fun coupledLorenzGeneratorShouldReturnCurrentThenAdvance() {
        val initialX = point3(Flt64(0.1), Flt64(0.2), Flt64(0.3))
        val initialY = point3(Flt64(0.2), Flt64(0.1), Flt64(0.4))
        val generator = CoupledLorenzAttractorGenerator(x = initialX, y = initialY)

        val current = generator()
        assertTrue(current.first[0] eq initialX[0])
        assertTrue(current.first[1] eq initialX[1])
        assertTrue(current.first[2] eq initialX[2])
        assertTrue(current.second[0] eq initialY[0])
        assertTrue(current.second[1] eq initialY[1])
        assertTrue(current.second[2] eq initialY[2])

        assertTrue(generator.x[0].toDouble().isFinite())
        assertTrue(generator.x[1].toDouble().isFinite())
        assertTrue(generator.x[2].toDouble().isFinite())
        assertTrue(generator.y[0].toDouble().isFinite())
        assertTrue(generator.y[1].toDouble().isFinite())
        assertTrue(generator.y[2].toDouble().isFinite())
    }

    @Test
    fun chuaCircuitGeneratorShouldReturnCurrentThenAdvance() {
        val initial = point3(Flt64(0.1), Flt64(0.1), Flt64(0.1))
        val generator = ChuaCircuitGenerator(x = initial)
        val current = generator()
        assertTrue(current[0] eq initial[0])
        assertTrue(current[1] eq initial[1])
        assertTrue(current[2] eq initial[2])
        assertTrue(generator.x[0].toDouble().isFinite())
        assertTrue(generator.x[1].toDouble().isFinite())
        assertTrue(generator.x[2].toDouble().isFinite())
    }
}
