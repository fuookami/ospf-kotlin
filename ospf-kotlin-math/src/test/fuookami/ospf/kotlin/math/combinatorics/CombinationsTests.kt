package fuookami.ospf.kotlin.math.combinatorics

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CombinationsTests {
    @Test
    fun test() {
        val input = listOf(0, 1, 2)
        assertEquals(7, combine(input).size)
        assertEquals(3, combine(input, 2).size)
        assertEquals(10L, combineCount(5, 2))
        assertEquals(listOf(listOf(0, 1), listOf(0, 2), listOf(1, 2)), combineSequence(input, 2).toList())
    }
}