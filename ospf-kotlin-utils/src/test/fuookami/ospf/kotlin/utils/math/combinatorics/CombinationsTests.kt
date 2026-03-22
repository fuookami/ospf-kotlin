package fuookami.ospf.kotlin.utils.math.combinatorics

import org.junit.jupiter.api.Test

class CombinationsTests {
    @Test
    fun test() {
        val input = listOf(0, 1, 2)
        assert(combine(input).size == 7)
//        val promise = combineAsync(input)
//        runBlocking {
//            for (perm in promise) {
//                println(perm.joinToString(", "))
//            }
//        }
    }
}
