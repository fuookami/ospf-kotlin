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
    fun pow() {
        assert((Meter * Second.reciprocal() * Second.reciprocal()) == (Meter / Second.pow(2)))
    }

    @Test
    fun convert() {
        assert(Meter.to(Kilometer)!!.value eq FltX(0.001))
    }
}
