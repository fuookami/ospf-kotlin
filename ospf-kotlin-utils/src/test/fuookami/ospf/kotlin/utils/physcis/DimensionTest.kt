package fuookami.ospf.kotlin.utils.physcis

import fuookami.ospf.kotlin.utils.physics.dimension.Acceleration
import fuookami.ospf.kotlin.utils.physics.dimension.Force
import fuookami.ospf.kotlin.utils.physics.dimension.Mass
import fuookami.ospf.kotlin.utils.physics.dimension.times
import org.junit.jupiter.api.Test

class DimensionTest {
    @Test
    fun dimensions() {
        assert(Force == Mass * Acceleration)
    }
}
