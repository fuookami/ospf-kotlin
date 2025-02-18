package fuookami.ospf.kotlin.utils.physcis

import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.Order
import fuookami.ospf.kotlin.utils.physics.unit.*
import fuookami.ospf.kotlin.utils.physics.quantity.*

class QuantityTest {
    @Test
    fun eq() {
        val value = Flt64.one * Meter
        assert((value * value) eq (Flt64.one * SquaredMeter))
    }

    @Test
    fun ord() {
        val value = Flt64.one * Meter
        assert((value * value) partialOrd (Flt64.one * SquaredMeter) == Order.Equal)
        assert((value * value) partialOrd (Flt64.two * SquaredMeter) is Order.Less)
        assert((value * value) partialOrd (Flt64.one * CubicMeter) == null)
    }

    @Test
    fun convert() {
        val value1 = Flt64.one * Meter
        assert(value1.to(Kilometer)!! eq (Flt64(0.001) * Kilometer))

        val value2 = Flt64.one * Inch
        assert(value2.to(Centimeter)!! eq (Flt64(2.54) * Centimeter))
    }
}
