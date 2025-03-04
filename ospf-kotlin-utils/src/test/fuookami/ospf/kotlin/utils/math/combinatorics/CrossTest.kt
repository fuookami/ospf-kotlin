package fuookami.ospf.kotlin.utils.math.combinatorics

import kotlinx.coroutines.*
import org.junit.jupiter.api.*

class CrossTest {
    @Test
    fun test() {
        val input = listOf(listOf(0, 1), listOf(2, 3))
        assert(cross(input).size == 4)
//        val promise = crossAsync(input)
//        runBlocking {
//            for (perm in promise) {
//                println(perm.joinToString(", "))
//            }
//        }
    }
}
