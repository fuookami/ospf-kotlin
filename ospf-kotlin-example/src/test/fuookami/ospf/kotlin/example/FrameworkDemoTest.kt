package fuookami.ospf.kotlin.example

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.example.framework_demo.*

class FrameworkDemoTest {
    @Test
    fun runDemo1() {
        val demo = Demo1()
        assert(runBlocking { demo().ok })
    }
}
