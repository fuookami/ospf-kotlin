package fuookami.ospf.kotlin.math.ordinary

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*

class GCDTest {
    @Test
    fun gcdTwo() {
        assertEquals(UInt64(2), gcd(listOf(UInt64(4), UInt64(6)), UInt64))
        assertEquals(UInt64(3), gcd(listOf(UInt64(6), UInt64(9)), UInt64))
    }

    @Test
    fun gcdSome() {
        assertEquals(UInt64(2), gcd(listOf(UInt64(4), UInt64(6), UInt64(8)), UInt64))
        assertEquals(UInt64(3), gcd(listOf(UInt64(6), UInt64(9), UInt64(12)), UInt64))
    }

    @Test
    fun gcdMod() {
        assertEquals(UInt64(6), gcdMod(listOf(UInt64(48), UInt64(18)), UInt64))
        assertEquals(UInt64(6), gcdMod(listOf(UInt64(48), UInt64(18), UInt64(30)), UInt64))
    }

    @Test
    fun extendedGcd() {
        val result = extendedGcd(Int64(240), Int64(46))
        assertEquals(Int64(2), result.gcd)
        assertEquals(result.gcd, Int64(240) * result.x + Int64(46) * result.y)
    }

    @Test
    fun gcdWithZero() {
        // gcd(0, 5) == 5
        assertEquals(UInt64(5), gcd(listOf(UInt64(0), UInt64(5)), UInt64))
        // gcd(5, 0) == 5
        assertEquals(UInt64(5), gcd(listOf(UInt64(5), UInt64(0)), UInt64))
        // gcd(0, 0) == 0 (数学约定)
        assertEquals(UInt64(0), gcd(listOf(UInt64(0), UInt64(0)), UInt64))
    }

    @Test
    fun gcdEmptyAndSingle() {
        // 空集合返回 1
        assertEquals(UInt64.one, gcd(emptyList(), UInt64))
        // 单元素返回自身
        assertEquals(UInt64(6), gcd(listOf(UInt64(6)), UInt64))
    }

    @Test
    fun gcdManyWithZero() {
        // 多参数包含零
        assertEquals(UInt64(2), gcd(listOf(UInt64(4), UInt64(0), UInt64(6)), UInt64))
        assertEquals(UInt64(3), gcd(listOf(UInt64(0), UInt64(9), UInt64(12)), UInt64))
    }
}
