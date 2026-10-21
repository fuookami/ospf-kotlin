/**
 * Windmi 吸引子测试 / Windmi attractor tests
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.*

class WindmiAttractorTest {

    @Test
    fun attractorShouldAdvanceByOneEulerStep() {
        val attractor = WindmiAttractor(
            a = Flt64.one,
            b = Flt64.zero,
            c = Flt64.zero,
            d = Flt64.zero,
            e = Flt64.zero,
            f = Flt64.zero,
            g = Flt64.zero,
            h = Flt64(0.01)
        )
        val next = attractor(point3(Flt64(1.0), Flt64(2.0), Flt64(3.0)))
        assertEquals(1.01, next[0].value, 1.0e-9)
        assertEquals(1.97, next[1].value, 1.0e-9)
        assertEquals(3.02, next[2].value, 1.0e-9)
    }

    @Test
    fun generatorShouldProduceConsecutiveStates() {
        val attractor = WindmiAttractor()
        val generator = WindmiAttractorGenerator(
            attractor = attractor,
            _state = point3(Flt64(1.0), Flt64(2.0), Flt64(3.0))
        )
        val first = generator.invoke()
        val second = generator.invoke()
        assertEquals(point3(Flt64(1.0), Flt64(2.0), Flt64(3.0)), first)
        assertEquals(attractor(first), second)
    }
}
