package fuookami.ospf.kotlin.example

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.example.core_demo.*

class CoreDemoTest {
    @Test
    fun runDemo1() {
        val demo = Demo1()
        assert(runBlocking { demo().ok })
    }

    @Test
    fun runDemo2() {
        val demo = Demo2()
        assert(runBlocking { demo().ok })
    }
}
