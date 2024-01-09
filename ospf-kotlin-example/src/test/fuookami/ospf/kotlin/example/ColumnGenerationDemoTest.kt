package fuookami.ospf.kotlin.example

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.example.column_generation_demo.*

class ColumnGenerationDemoTest {
    @Test
    fun runDemo1() {
        val demo = Demo1()
        assert(runBlocking { demo().ok })
    }
}
