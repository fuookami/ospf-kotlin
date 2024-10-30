package fuookami.ospf.kotlin.utils.physcis

import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

class DimensionTest {
    @Test
    fun dimensions() {
        assert(Force == Mass * Acceleration)
    }
}
