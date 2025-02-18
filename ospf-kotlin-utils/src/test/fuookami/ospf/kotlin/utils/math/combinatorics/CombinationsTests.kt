package fuookami.ospf.kotlin.utils.math.combinatorics

import kotlinx.coroutines.*
import org.junit.jupiter.api.*

class CombinationsTests {
    @Test
    fun test() {
        val input = listOf(0, 1, 2)
        val promise = combineAsync(input)
        runBlocking {
            for (perm in promise) {
                println(perm.joinToString(", "))
            }
        }
    }
}
