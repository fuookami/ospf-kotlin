package fuookami.ospf.kotlin.utils.math.ordinary

import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.utils.math.*

class FactorizationTest {
    @Test
    fun factorize() {
        assert(factorize(UInt64.two) == listOf(UInt64.two to 1))
        assert(factorize(UInt64(4)) == listOf(UInt64.two to 2))
        assert(factorize(UInt64(12)) == listOf(UInt64.two to 2, UInt64.three to 1))
    }
}
