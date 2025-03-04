package fuookami.ospf.kotlin.utils.math.combinatorics

import kotlinx.coroutines.*
import org.junit.jupiter.api.*

class PermutationsTests {
    @Test
    fun test() {
        val input = listOf(0, 1, 2)
        assert(permute(input).size == 6)
//        val promise = permuteAsync(input)
//        runBlocking {
//            for (perm in promise) {
//                println(perm.joinToString(", "))
//            }
//        }
    }
}
