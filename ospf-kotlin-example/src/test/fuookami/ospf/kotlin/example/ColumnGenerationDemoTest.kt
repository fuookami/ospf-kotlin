package fuookami.ospf.kotlin.example

import fuookami.ospf.kotlin.example.column_generation_demo.*
import org.junit.jupiter.api.Test

class ColumnGenerationDemoTest {
    @Test
    fun runDemo1() {
        val demo = fuookami.ospf.kotlin.example.column_generation_demo.Demo1()
        assert(demo().isOk())
    }
}
