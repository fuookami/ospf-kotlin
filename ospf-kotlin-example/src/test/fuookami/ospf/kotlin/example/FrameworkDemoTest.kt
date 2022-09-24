package fuookami.ospf.kotlin.example

import fuookami.ospf.kotlin.example.framework_demo.*
import org.junit.jupiter.api.Test

class FrameworkDemoTest {
    @Test
    fun runDemo1() {
        val demo = Demo1()
        assert(demo().isOk())
    }
}
