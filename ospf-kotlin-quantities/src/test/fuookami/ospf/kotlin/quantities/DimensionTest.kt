package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.quantities.dimension.Acceleration
import fuookami.ospf.kotlin.quantities.dimension.Force
import fuookami.ospf.kotlin.quantities.dimension.Mass
import fuookami.ospf.kotlin.quantities.dimension.times
import org.junit.jupiter.api.Test

class DimensionTest {
    @Test
    fun dimensions() {
        assert(Force == Mass * Acceleration)
    }
}
