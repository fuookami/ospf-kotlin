package fuookami.ospf.kotlin.example

import fuookami.ospf.kotlin.example.core_demo.*
import org.junit.jupiter.api.Test

class CoreDemoTest {
    @Test
    fun runDemo1() {
        val demo = Demo1()
        assert(demo().isOk())
    }

    @Test
    fun runDemo2() {
        val demo = Demo2()
        assert(demo().isOk())
    }
}
