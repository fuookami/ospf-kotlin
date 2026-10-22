package fuookami.ospf.kotlin.core.solver.heuristic.tier3

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.heuristic.runner.CmaNumerics

class CmaNumericsTest {
    @Test
    fun fdlibmPrimitivesShouldMatchSharedBinary64Vectors() {
        val mode = CmaNumerics.PolarFdlibmV1
        val logs = listOf(Double.MIN_VALUE to "c0874385446d71c3", 1e-12 to "c03ba18a998fffa0", 0.125 to "c000a2b23f3bab73", 0.75 to "bfd269621134db92", 1.0 to "0000000000000000", 4.0 to "3ff62e42fefa39ef")
        val exps = listOf(-745.0 to "0000000000000001", -27.6 to "3d72257eee500edc", -0.1 to "3fecf46d99d52b3a", 0.0 to "3ff0000000000000", 0.1 to "3ff1aec7b35a00d4", 27.6 to "426c3700dcfb6769", 709.0 to "7fdd422d2be5dc9b")
        val roots = listOf(Double.MIN_VALUE to "1e60000000000000", 1e-20 to "3ddb7cdfd9d7bdbb", 0.75 to "3febb67ae8584caa")
        val powers = listOf(2.0 to "3fe47ae147ae147c", 100.0 to "3debff2ee48e055c", 1_000_000.0 to "0000000000000000")
        for ((input, expected) in logs) assertEquals(expected.toULong(16).toLong(), mode.ln(input).toRawBits())
        for ((input, expected) in exps) assertEquals(expected.toULong(16).toLong(), mode.exp(input).toRawBits())
        for ((input, expected) in roots) assertEquals(expected.toULong(16).toLong(), mode.sqrt(input).toRawBits())
        for ((input, expected) in powers) assertEquals(expected.toULong(16).toLong(), mode.pow(0.8, input).toRawBits())
    }

    @Test
    fun polarGaussianShouldRejectZeroAndOuterPairsWithoutHiddenCache() {
        val draws = listOf(0.5, 0.5, 0.0, 0.0, 0.75, 0.625, 0.75, 0.625)
        var used = 0
        val entropy = Generator { Flt64(draws[used++]) }
        repeat(2) { assertEquals(0x3ff5d3c33f332f3dL, CmaNumerics.PolarFdlibmV1.gaussian(entropy).toRawBits()) }
        assertEquals(8, used)
    }

    @Test
    fun logarithmShouldPreserveOriginalFdlibmGroupingAtPolarInputs() {
        for ((input, expected) in listOf("3fed03d5192d1533" to "bfb9118bde7c4c17", "3fe90a282aa45379" to "bfcf65261fdd34cd", "3fe4263db49013ff" to "bfdd9a9f74bbacfa")) {
            assertEquals(expected.toULong(16).toLong(), CmaNumerics.PolarFdlibmV1.ln(Double.fromBits(input.toULong(16).toLong())).toRawBits())
        }
    }
}
