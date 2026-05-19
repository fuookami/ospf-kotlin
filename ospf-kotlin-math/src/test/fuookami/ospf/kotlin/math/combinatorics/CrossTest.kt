package fuookami.ospf.kotlin.math.combinatorics

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CrossTest {
    @Test
    fun test() {
        val input = listOf(listOf(0, 1), listOf(2, 3))
        assertEquals(4, cross(input).size)
        assertEquals(4L, crossCount(input))
        assertEquals(listOf(0 to 2, 0 to 3, 1 to 2, 1 to 3), cross2(listOf(0, 1), listOf(2, 3)))
        assertEquals(8, cross3(listOf(0, 1), listOf(2, 3), listOf(4, 5)).size)
        assertEquals(4, crossSequence(input).toList().size)
    }
}