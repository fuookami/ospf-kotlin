package fuookami.ospf.kotlin.example

import fuookami.ospf.kotlin.example.core_demo.GenericNumberDemo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Smoke test that the generic-number demo produces the expected number of build summaries. */
class CoreDemoTest {
    @Test
    fun genericNumberDemoSmokeTest() {
        val summaries = GenericNumberDemo.runBuildAndDump()
        assertEquals(4, summaries.size)
    }
}
