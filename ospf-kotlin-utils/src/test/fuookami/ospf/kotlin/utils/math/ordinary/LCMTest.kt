package fuookami.ospf.kotlin.utils.math.ordinary

import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.utils.math.*

class LCMTest {
    @Test
    fun lcmTwo() {
        assert(lcm(UInt64(4), UInt64(6)) == UInt64(12))
        assert(lcm(UInt64(6), UInt64(9)) == UInt64(18))
    }

    @Test
    fun lcmSome() {
        assert(lcm(listOf(UInt64(4), UInt64(6), UInt64(8))) == UInt64(24))
        assert(lcm(listOf(UInt64(6), UInt64(9), UInt64(12))) == UInt64(36))
    }

    @Test
    fun lcmFTwo() {
        assert(lcm(FltX(0.4), FltX(0.6)) eq FltX(1.2))
        assert(lcm(FltX(0.6), FltX(0.9)) eq FltX(1.8))
    }

    @Test
    fun lcmFSome() {
        assert(lcm(listOf(FltX(0.4), FltX(0.6), FltX(0.8))) eq FltX(2.4))
        assert(lcm(listOf(FltX(0.6), FltX(0.9), FltX(1.2))) eq FltX(3.6))
    }
}
