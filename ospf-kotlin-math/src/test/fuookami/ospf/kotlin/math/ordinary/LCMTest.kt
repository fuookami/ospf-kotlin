package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LCMTest {
    @Test
    fun lcmTwo() {
        assertEquals(UInt64(12), lcm(UInt64(4), UInt64(6), UInt64))
        assertEquals(UInt64(18), lcm(UInt64(6), UInt64(9), UInt64))
    }

    @Test
    fun lcmSome() {
        assertEquals(UInt64(24), lcm(listOf(UInt64(4), UInt64(6), UInt64(8)), UInt64))
        assertEquals(UInt64(36), lcm(listOf(UInt64(6), UInt64(9), UInt64(12)), UInt64))
    }

    @Test
    fun lcmByFactorization() {
        assertEquals(UInt64(12), lcmByFactorization(UInt64(4), UInt64(6), UInt64))
        assertEquals(UInt64(24), lcmByFactorization(listOf(UInt64(4), UInt64(6), UInt64(8)), UInt64))
        assertEquals(UInt64(36), lcmByFactorization(UInt64(6), UInt64(9), UInt64(12), constants = UInt64))
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

    @Test
    fun lcmWithZero() {
        // lcm(0, x) == 0
        assertEquals(UInt64(0), lcm(UInt64(0), UInt64(5), UInt64))
        assertEquals(UInt64(0), lcm(UInt64(5), UInt64(0), UInt64))
        assertEquals(UInt64(0), lcm(UInt64(0), UInt64(0), UInt64))
    }

    @Test
    fun lcmEmptyAndSingle() {
        // 空集合返回 1
        assertEquals(UInt64.one, lcm(emptyList(), UInt64))
        // 单元素返回自身
        assertEquals(UInt64(5), lcm(listOf(UInt64(5)), UInt64))
    }

    @Test
    fun lcmManyWithZero() {
        // 多参数包含零
        assertEquals(UInt64(0), lcm(listOf(UInt64(0), UInt64(6), UInt64(8)), UInt64))
        assertEquals(UInt64(0), lcm(listOf(UInt64(4), UInt64(0), UInt64(8)), UInt64))
    }
}

