package fuookami.ospf.kotlin.example

import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.example.framework_demo.*

class FrameworkDemoTest {
    @Test
    fun runDemo1() {
        val demo = Demo1()
        assert(demo().ok)
    }
}
