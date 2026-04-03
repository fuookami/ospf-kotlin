package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FactorizationTest {
    @Test
    fun factorize() {
        assertEquals(listOf(UInt64.two to 1), factorize(UInt64.two, UInt64))
        assertEquals(listOf(UInt64.two to 2), factorize(UInt64(4), UInt64))
        assertEquals(listOf(UInt64.two to 2, UInt64.three to 1), factorize(UInt64(12), UInt64))
    }

    @Test
    fun defactorize() {
        assertEquals(UInt64(12), defactorize(listOf(UInt64.two to 2, UInt64.three to 1), UInt64))
        assertEquals(UInt64.one, defactorize(emptyList(), UInt64))
    }

    @Test
    fun divisors() {
        assertEquals(listOf(UInt64.one, UInt64.two, UInt64(3), UInt64(4), UInt64(6), UInt64(12)), divisors(UInt64(12), UInt64))
        assertEquals(
            listOf(UInt64.one, UInt64.two, UInt64(3), UInt64(4), UInt64(6), UInt64(12)),
            divisors(listOf(UInt64.two to 2, UInt64.three to 1), UInt64)
        )
    }

    @Test
    fun divisorCount() {
        assertEquals(6, divisorCount(UInt64(12), UInt64))
        assertEquals(6, divisorCount(listOf(UInt64.two to 2, UInt64.three to 1)))
    }

    @Test
    fun eulerTotient() {
        assertEquals(UInt64.one, eulerTotient(UInt64.one, UInt64))
        assertEquals(UInt64(4), eulerTotient(UInt64(12), UInt64))
        assertEquals(UInt64(6), eulerTotient(UInt64(9), UInt64))
    }
}
