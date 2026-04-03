package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

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
}
