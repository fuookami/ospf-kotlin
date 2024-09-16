package fuookami.ospf.kotlin.utils.physcis

import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.utils.physics.unit.*

class UnitTest {
    @Test
    fun units() {
        val newton = Kilogram * MeterPerSquareSecond
        assert(Newton == newton)
    }
}
