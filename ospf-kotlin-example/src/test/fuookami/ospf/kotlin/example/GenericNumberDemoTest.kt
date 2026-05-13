package fuookami.ospf.kotlin.example

import fuookami.ospf.kotlin.example.core_demo.GenericNumberDemo
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GenericNumberDemoTest {
    @Test
    fun genericNumberDemoShouldReturnFourTypeBuildSummary() {
        val summaries = GenericNumberDemo.runBuildAndDump()
        assertEquals(4, summaries.size)
        assertEquals(setOf("Flt64", "Rtn64", "FltX", "RtnX"), summaries.map { it.numberType }.toSet())

        for (summary in summaries) {
            assertTrue(summary.linear.success, "${summary.numberType}: linear build should succeed")
            assertTrue(summary.quadratic.success, "${summary.numberType}: quadratic build should succeed")
            assertEquals(1, summary.linear.constraintCount, "${summary.numberType}: linear constraint count mismatch")
            assertEquals(1, summary.quadratic.constraintCount, "${summary.numberType}: quadratic constraint count mismatch")

            val linearX = "${summary.numberType.lowercase()}_demo_linear_x"
            val linearY = "${summary.numberType.lowercase()}_demo_linear_y"
            assertEquals(Flt64(2.0), summary.linear.objectiveCoefficients[linearX], "${summary.numberType}: linear x coefficient mismatch")
            assertEquals(Flt64.one, summary.linear.objectiveCoefficients[linearY], "${summary.numberType}: linear y coefficient mismatch")

            val quadX = "${summary.numberType.lowercase()}_demo_quad_x"
            val quadY = "${summary.numberType.lowercase()}_demo_quad_y"
            assertEquals(Flt64.one, summary.quadratic.objectiveCoefficients[quadX to quadY], "${summary.numberType}: quadratic x*y coefficient mismatch")
            assertEquals(Flt64.one, summary.quadratic.objectiveCoefficients[quadX to null], "${summary.numberType}: quadratic linear x coefficient mismatch")
        }
    }
}
