package fuookami.ospf.kotlin.quantities

import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.unit.*

class UnitTest {
    @Test
    fun units() {
        val newton = Kilogram * MeterPerSecondSquared
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
