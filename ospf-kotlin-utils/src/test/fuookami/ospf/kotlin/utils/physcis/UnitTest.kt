package fuookami.ospf.kotlin.utils.physcis

import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.unit.*

class UnitTest {
    @Test
    fun units() {
        val newton = Kilogram * MeterPerSquareSecond
        assert(Newton == newton)
    }

    @Test
    fun convert() {
        assert(Meter.to(Kilometer)!!.value eq Flt64(0.001))
    }
}
