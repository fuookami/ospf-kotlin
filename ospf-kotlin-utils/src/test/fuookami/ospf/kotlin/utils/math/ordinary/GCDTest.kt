package fuookami.ospf.kotlin.utils.math.ordinary

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.utils.math.*

class GCDTest {
    @Test
    fun gcdTwo() {
        assert(gcd(UInt64(4), UInt64(6)) == UInt64(2))
        assert(gcd(UInt64(6), UInt64(9)) == UInt64(3))
    }

    @Test
    fun gcdSome() {
        assert(gcd(UInt64(4), UInt64(6), UInt64(8)) == UInt64(2))
        assert(gcd(UInt64(6), UInt64(9), UInt64(12)) == UInt64(3))
    }
}
