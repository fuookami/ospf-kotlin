package fuookami.ospf.kotlin.core.solver.heuristic.tier3

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.heuristic.runner.ShadeMemory
import fuookami.ospf.kotlin.core.solver.heuristic.runner.ShadeNumerics

class ShadeNumericsTest {
    @Test
    fun fdlibmTangentShouldMatchSharedBinary64ContractVectors() {
        val vectors = listOf(
            "c02ebe05be113456" to "3fd6692cd48a42aa",
            "c029e9f8300b6a04" to "bfda5a3cdd51edd9",
            "c019534956763f6c" to "bfa8abe2c5ee870e",
            "4022a3ea01341b64" to "bfbae2617d4619be",
            "c030100029042b02" to "bfd7b10bf21a884f",
            "c03171f3cff42048" to "4017d22d731c64df",
            "4033183d13007fea" to "3fd002909c5eab58",
            "c0238e9444814bcc" to "bfd7a15cb5c047f0",
            "0000000000000000" to "0000000000000000",
            "8000000000000000" to "8000000000000000"
        )
        for ((input, expected) in vectors) {
            val value = Double.fromBits(input.toULong(16).toLong())
            assertEquals(expected.toULong(16).toLong(), ShadeNumerics.FdlibmV1.tangent(value).toRawBits())
        }
    }

    @Test
    fun fdlibmSamplingShouldKeepFiveDrawsAndClipping() {
        val values = listOf(0.7, 0.75, 0.1, 0.2, 0.9)
        var consumed = 0
        val entropy = Generator { Flt64(values[consumed++]) }
        val result = ShadeMemory().sample(entropy, ShadeNumerics.FdlibmV1)
        assertEquals(3, result.memoryIndex)
        assertEquals(0.6.toRawBits(), result.differentialWeight.toRawBits())
        assertEquals((0.5 + 0.1 * (0.1 + 0.2 + 0.9 - 1.5) * 2.0).toRawBits(), result.crossRate.toRawBits())
        assertEquals(5, consumed)
    }
}
