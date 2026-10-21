/**
 * 分形逃逸时间与维数测试 / Fractal escape time and dimension tests
*/
package fuookami.ospf.kotlin.math.fractal

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.*

class FractalTest {

    private fun <T> Ret<T>.valueOrFail(): T {
        return when (this) {
            is Ok -> value
            else -> fail("expected Ok but got $this")
        }
    }

    @Test
    fun escapeTimeShouldRemainBoundedInsideTheSet() {
        val result = escapeTime(
            z = point2(Flt64.zero, Flt64.zero),
            c = point2(Flt64(-0.5), Flt64.zero),
            maxIterations = UInt64(50)
        ).valueOrFail()
        assertEquals(UInt64(50), result)
    }

    @Test
    fun escapeTimeShouldBeZeroWhenAlreadyOutsideTheBailoutRadius() {
        val result = escapeTime(
            z = point2(Flt64(3.0), Flt64.zero),
            c = point2(),
            maxIterations = UInt64(50)
        ).valueOrFail()
        assertEquals(UInt64.zero, result)
    }

    @Test
    fun escapeTimeShouldRejectInvalidParameters() {
        assertTrue(
            escapeTime(
                z = point2(),
                c = point2(),
                maxIterations = UInt64.zero
            ) is Failed
        )
        assertTrue(
            escapeTime(
                z = point2(),
                c = point2(),
                bailoutRadius = Flt64.zero
            ) is Failed
        )
    }

    @Test
    fun boxCountingDimensionShouldApproximateALine() {
        val points = (0..100).map { index ->
            point2(Flt64(index / 100.0), Flt64.zero)
        }
        val dimension = boxCountingDimension(points).valueOrFail().value
        assertEquals(1.0, dimension, 0.05)
    }

    @Test
    fun boxCountingDimensionShouldApproximateAFilledSquare() {
        val points = ArrayList<Point<Dim2, Flt64>>()
        for (x in 0..32) {
            for (y in 0..32) {
                points.add(point2(Flt64(x / 32.0), Flt64(y / 32.0)))
            }
        }
        val dimension = boxCountingDimension(points).valueOrFail().value
        assertEquals(2.0, dimension, 0.05)
    }

    @Test
    fun boxCountingDimensionShouldRejectDegenerateInput() {
        assertTrue(boxCountingDimension(listOf(point2())) is Failed)
        assertTrue(
            boxCountingDimension(
                points = listOf(point2(), point2()),
                scales = listOf(2)
            ) is Failed
        )
    }

    @Test
    fun correlationDimensionShouldApproximateAOneDimensionalSet() {
        val solutions = (0..100).map { index ->
            listOf(Flt64(index / 100.0))
        }
        val dimension = correlationDimension(solutions).valueOrFail().value
        assertEquals(1.0, dimension, 0.25)
    }

    @Test
    fun correlationDimensionShouldApproximateATwoDimensionalSet() {
        val solutions = ArrayList<List<Flt64>>()
        for (x in 0..39) {
            for (y in 0..39) {
                solutions.add(listOf(Flt64(x / 39.0), Flt64(y / 39.0)))
            }
        }
        val dimension = correlationDimension(solutions).valueOrFail().value
        assertEquals(2.0, dimension, 0.25)
    }

    @Test
    fun correlationDimensionShouldRejectDegenerateInput() {
        assertTrue(correlationDimension(emptyList()) is Failed)
        assertTrue(correlationDimension(listOf(listOf(Flt64.one))) is Failed)
        assertTrue(
            correlationDimension(
                solutions = listOf(
                    listOf(Flt64.one, Flt64.two),
                    listOf(Flt64.one)
                )
            ) is Failed
        )
        assertTrue(
            correlationDimension(
                solutions = listOf(
                    listOf(Flt64.one),
                    listOf(Flt64.one)
                )
            ) is Failed
        )
    }
}
