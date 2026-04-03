package fuookami.ospf.kotlin.multiarray

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FastSumTest {

    // ========================================================================
    // sumAll Tests
    // ========================================================================

    @Test
    fun testSumAll() {
        // Create 2x3 array:
        // [[1, 2, 3],
        //  [4, 5, 6]]
        val array = MultiArray.newBy(Shape2(2, 3)) { i, _ -> Flt64(i + 1.0) }

        // 1 + 2 + 3 + 4 + 5 + 6 = 21
        val sum = array.sumAll(Flt64.zero)
        assertTrue((sum - Flt64(21.0)).abs() < Flt64(1e-10))
    }

    @Test
    fun testSumAllEmpty() {
        val array = MultiArray.newWith(Shape1(0), Flt64.zero)
        val sum = array.sumAll(Flt64.zero)
        assertEquals(Flt64.zero, sum)
    }

    @Test
    fun testSumAll1D() {
        val array = MultiArray.newBy(Shape1(5)) { i, _ -> Flt64(i + 1.0) }
        val sum = array.sumAll(Flt64.zero)
        assertTrue((sum - Flt64(15.0)).abs() < Flt64(1e-10))
    }

    // ========================================================================
    // sumAxis Tests
    // ========================================================================

    @Test
    fun testSumAxis0() {
        // Create 2x3 array:
        // [[1, 2, 3],
        //  [4, 5, 6]]
        val array = MultiArray.newBy(Shape2(2, 3)) { i, _ -> Flt64(i + 1.0) }

        // Sum along axis 0: [1+4, 2+5, 3+6] = [5, 7, 9]
        val sum = array.sumAxis(0, Flt64.zero)

        assertEquals(3, sum.size)
        assertTrue((sum[0] - Flt64(5.0)).abs() < Flt64(1e-10))
        assertTrue((sum[1] - Flt64(7.0)).abs() < Flt64(1e-10))
        assertTrue((sum[2] - Flt64(9.0)).abs() < Flt64(1e-10))
    }

    @Test
    fun testSumAxis1() {
        // Create 2x3 array:
        // [[1, 2, 3],
        //  [4, 5, 6]]
        val array = MultiArray.newBy(Shape2(2, 3)) { i, _ -> Flt64(i + 1.0) }

        // Sum along axis 1: [1+2+3, 4+5+6] = [6, 15]
        val sum = array.sumAxis(1, Flt64.zero)

        assertEquals(2, sum.size)
        assertTrue((sum[0] - Flt64(6.0)).abs() < Flt64(1e-10))
        assertTrue((sum[1] - Flt64(15.0)).abs() < Flt64(1e-10))
    }

    @Test
    fun testSumAxisOutOfBounds() {
        val array = MultiArray.newWith(Shape2(2, 3), Flt64.zero)

        assertFailsWith<AxisOutOfBoundsException> {
            array.sumAxis(2, Flt64.zero)
        }
    }

    @Test
    fun testSumAxis3D() {
        // Create 2x3x2 array
        val array = MultiArray.newBy(Shape3(2, 3, 2)) { i, _ -> Flt64(i + 1.0) }

        // Sum along axis 1 -> shape becomes [2, 2]
        val sum = array.sumAxis(1, Flt64.zero)

        assertEquals(4, sum.size)
        assertEquals(2, sum.shape.dimension)
        assertEquals(2, sum.shape[0])
        assertEquals(2, sum.shape[1])
    }

    // ========================================================================
    // sumAxes Tests
    // ========================================================================

    @Test
    fun testSumAxesEmpty() {
        val array = MultiArray.newBy(Shape2(2, 3)) { i, _ -> Flt64(i + 1.0) }
        val sum = array.sumAxes(intArrayOf(), Flt64.zero)

        assertEquals(array.size, sum.size)
    }

    @Test
    fun testSumAxesMultiple() {
        // Create 2x3x2 array
        val array = MultiArray.newBy(Shape3(2, 3, 2)) { i, _ -> Flt64(i + 1.0) }

        // Sum along axes [0, 2] -> shape becomes [3]
        val sum = array.sumAxes(intArrayOf(0, 2), Flt64.zero)

        assertEquals(3, sum.size)
        assertEquals(1, sum.shape.dimension)
    }

    @Test
    fun testSumAxesAll() {
        val array = MultiArray.newBy(Shape2(2, 3)) { i, _ -> Flt64(i + 1.0) }

        // Sum along all axes -> scalar (represented as 0-dim array with size 1)
        val sum = array.sumAxes(intArrayOf(0, 1), Flt64.zero)

        assertEquals(1, sum.size)
        assertTrue((sum[0] - Flt64(21.0)).abs() < Flt64(1e-10))
    }

    // ========================================================================
    // cumsumAxis Tests
    // ========================================================================

    @Test
    fun testCumsumAxis1() {
        // Create 2x3 array:
        // [[1, 2, 3],
        //  [4, 5, 6]]
        val array = MultiArray.newBy(Shape2(2, 3)) { i, _ -> Flt64(i + 1.0) }

        // Cumsum along axis 1:
        // [[1, 1+2, 1+2+3], [4, 4+5, 4+5+6]]
        // = [[1, 3, 6], [4, 9, 15]]
        val cumsum = array.cumsumAxis(1, Flt64.zero)

        assertEquals(6, cumsum.size)
        assertTrue((cumsum[intArrayOf(0, 0)] - Flt64(1.0)).abs() < Flt64(1e-10))
        assertTrue((cumsum[intArrayOf(0, 1)] - Flt64(3.0)).abs() < Flt64(1e-10))
        assertTrue((cumsum[intArrayOf(0, 2)] - Flt64(6.0)).abs() < Flt64(1e-10))
        assertTrue((cumsum[intArrayOf(1, 0)] - Flt64(4.0)).abs() < Flt64(1e-10))
        assertTrue((cumsum[intArrayOf(1, 1)] - Flt64(9.0)).abs() < Flt64(1e-10))
        assertTrue((cumsum[intArrayOf(1, 2)] - Flt64(15.0)).abs() < Flt64(1e-10))
    }

    @Test
    fun testCumsumAxis0() {
        // Create 2x3 array:
        // [[1, 2, 3],
        //  [4, 5, 6]]
        val array = MultiArray.newBy(Shape2(2, 3)) { i, _ -> Flt64(i + 1.0) }

        // Cumsum along axis 0:
        // [[1, 2, 3], [1+4, 2+5, 3+6]]
        // = [[1, 2, 3], [5, 7, 9]]
        val cumsum = array.cumsumAxis(0, Flt64.zero)

        assertEquals(6, cumsum.size)
        assertTrue((cumsum[intArrayOf(0, 0)] - Flt64(1.0)).abs() < Flt64(1e-10))
        assertTrue((cumsum[intArrayOf(0, 1)] - Flt64(2.0)).abs() < Flt64(1e-10))
        assertTrue((cumsum[intArrayOf(1, 0)] - Flt64(5.0)).abs() < Flt64(1e-10))
        assertTrue((cumsum[intArrayOf(1, 2)] - Flt64(9.0)).abs() < Flt64(1e-10))
    }

    @Test
    fun testCumsumAxisOutOfBounds() {
        val array = MultiArray.newWith(Shape2(2, 3), Flt64.zero)

        assertFailsWith<AxisOutOfBoundsException> {
            array.cumsumAxis(2, Flt64.zero)
        }
    }
}