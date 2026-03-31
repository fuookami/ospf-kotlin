package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.math.algebra.number.FltX
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LCMTest {
    @Test
    fun lcmTwo() {
        assertEquals(UInt64(12), lcm(UInt64(4), UInt64(6)))
        assertEquals(UInt64(18), lcm(UInt64(6), UInt64(9)))
    }

    @Test
    fun lcmSome() {
        assertEquals(UInt64(24), lcm(listOf(UInt64(4), UInt64(6), UInt64(8))))
        assertEquals(UInt64(36), lcm(listOf(UInt64(6), UInt64(9), UInt64(12))))
    }

    @Test
    fun lcmByFactorization() {
        assertEquals(UInt64(24), lcmByFactorization(listOf(UInt64(4), UInt64(6), UInt64(8))))
        assertEquals(UInt64(36), lcmByFactorization(UInt64(6), UInt64(9), UInt64(12)))
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



