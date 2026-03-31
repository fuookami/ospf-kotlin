package fuookami.ospf.kotlin.utils.math.combinatorics

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PermutationsTests {
    @Test
    fun test() {
        val input = listOf(0, 1, 2)
        assertEquals(6, permute(input).size)
        assertEquals(6, permute(input, 2).size)
        assertEquals(20L, permuteCount(5, 2))
        assertEquals(6, permuteSequence(input, 2).toList().size)
    }
}
