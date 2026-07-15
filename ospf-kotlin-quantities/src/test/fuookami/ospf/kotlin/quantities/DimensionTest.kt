package fuookami.ospf.kotlin.quantities

import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.quantities.dimension.*

class DimensionTest {
    @Test
    fun dimensions() {
        assert(Force == Mass * Acceleration)
    }
}
