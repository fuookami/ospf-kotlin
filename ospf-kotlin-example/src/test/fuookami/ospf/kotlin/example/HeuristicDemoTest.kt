package fuookami.ospf.kotlin.example

import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.example.heuristic_demo.*

class HeuristicDemoTest {
    @Test
    fun runDemo1() {
        val demo = Demo1()
        assert(demo().ok)
    }

    @Test
    fun runDemo2() {
        val demo = Demo2()
        assert(demo().ok)
    }
}
