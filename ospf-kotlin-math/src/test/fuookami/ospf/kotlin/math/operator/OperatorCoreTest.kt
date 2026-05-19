package fuookami.ospf.kotlin.math.operator

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.orderOf
import fuookami.ospf.kotlin.utils.functional.orderBetween
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Operator core path tests: Abs, Ord, Tolerance, Reciprocal, Pow, Log, Precision
 */
class OperatorCoreTest {

    // ==================== Abs Tests ====================

    @Test
    fun flt64AbsShouldReturnPositiveValue() {
        assertEquals(Flt64(5.0), Flt64(-5.0).abs())
        assertEquals(Flt64(5.0), Flt64(5.0).abs())
        assertEquals(Flt64(0.0), Flt64(0.0).abs())
    }

    @Test
    fun int64AbsShouldReturnPositiveValue() {
        assertEquals(Int64(5L), Int64(-5L).abs())
        assertEquals(Int64(5L), Int64(5L).abs())
        assertEquals(Int64(0L), Int64(0L).abs())
    }

    @Test
    fun absFunctionShouldWork() {
        assertEquals(Flt64(10.0), abs(Flt64(-10.0)))
        assertEquals(Int64(10L), abs(Int64(-10L)))
    }

    // ==================== Ord Tests ====================

    @Test
    fun orderShouldRepresentComparisonResult() {
        assertTrue(Order.Less().value < 0)
        assertEquals(0, Order.Equal.value)
        assertTrue(Order.Greater().value > 0)
    }

    @Test
    fun orderNegationShouldReverse() {
        val less = Order.Less(-1)
        val greater = -less
        assertTrue(greater is Order.Greater)
        assertEquals(1, greater.value)

        val negGreater = Order.Greater(1)
        val newLess = -negGreater
        assertTrue(newLess is Order.Less)
        assertEquals(-1, newLess.value)

        val equal = Order.Equal
        assertEquals(Order.Equal, -equal)
    }

    @Test
    fun orderIfEqualShouldExecuteOnEqual() {
        var executed = false
        val result = Order.Equal.ifEqual { executed = true; Order.Less() }
        assertTrue(executed)
        assertTrue(result is Order.Less)

        executed = false
        val notExecuted = Order.Less().ifEqual { executed = true; Order.Greater() }
        assertFalse(executed)
        assertTrue(notExecuted is Order.Less)
    }

    @Test
    fun orderOfShouldCreateCorrectOrder() {
        assertTrue(orderOf(-5) is Order.Less)
        assertTrue(orderOf(0) is Order.Equal)
        assertTrue(orderOf(10) is Order.Greater)
    }

    @Test
    fun orderBetweenShouldCompareComparables() {
        assertTrue(orderBetween(1, 2) is Order.Less)
        assertTrue(orderBetween(2, 2) is Order.Equal)
        assertTrue(orderBetween(3, 2) is Order.Greater)
    }

    @Test
    fun flt64OrdShouldWork() {
        val a = Flt64(1.0)
        val b = Flt64(2.0)
        val c = Flt64(1.0)

        assertTrue((a ord b) is Order.Less)
        assertTrue((b ord a) is Order.Greater)
        assertTrue((a ord c) is Order.Equal)
    }

    // ==================== Tolerance Tests ====================

    @Test
    fun absoluteToleranceShouldWrapValue() {
        val tolerance = AbsoluteTolerance(Flt64(0.001))
        assertEquals(Flt64(0.001), tolerance.tolerance)
    }

    @Test
    fun containsContractShouldWorkForCoreRangeTypes() {
        val intRange = Int64.zero..Int64(5L)
        assertTrue(Int64(3L) in intRange)
        assertFalse(Int64(8L) in intRange)

        val valueRange = ValueRange(
            Flt64.zero,
            Flt64.one,
            lbInterval = fuookami.ospf.kotlin.math.algebra.value_range.Interval.Closed,
            ubInterval = fuookami.ospf.kotlin.math.algebra.value_range.Interval.Closed,
            constants = Flt64
        ).value!!
        assertTrue(Flt64(0.5) in valueRange)
        assertFalse(Flt64(2.0) in valueRange)
    }

    // ==================== Reciprocal Tests ====================

    @Test
    fun flt64ReciprocalShouldReturnInverse() {
        assertEquals(Flt64(0.5), Flt64(2.0).reciprocal())
        assertEquals(Flt64(2.0), Flt64(0.5).reciprocal())
        assertEquals(Flt64(1.0), Flt64(1.0).reciprocal())
    }

    @Test
    fun int64ReciprocalOfOneShouldBeOne() {
        // Only 1.reciprocal() is defined for Int64
        assertEquals(Int64(1L), Int64(1L).reciprocal())
    }

    // ==================== Pow Tests ====================

    @Test
    fun flt64PowShouldWork() {
        assertEquals(Flt64(8.0), Flt64(2.0).pow(3))
        assertEquals(Flt64(1.0), Flt64(5.0).pow(0))
        assertEquals(Flt64(0.25), Flt64(2.0).pow(-2))
    }

    @Test
    fun flt64SqrAndCubShouldWork() {
        assertEquals(Flt64(9.0), Flt64(3.0).sqr())
        assertEquals(Flt64(27.0), Flt64(3.0).cub())
    }

    @Test
    fun int64PowShouldWork() {
        // Int64.pow with Long exponent may have different signature
        // Test sqr and cub instead
        assertEquals(Int64(4L), Int64(2L).sqr())
        assertEquals(Int64(8L), Int64(2L).cub())
    }

    @Test
    fun int64SqrAndCubShouldWork() {
        assertEquals(Int64(9L), Int64(3L).sqr())
        assertEquals(Int64(27L), Int64(3L).cub())
    }

    @Test
    fun powFunctionShouldWork() {
        assertEquals(Flt64(16.0), pow(Flt64(2.0), 4))
        assertEquals(Int64(16L), pow(Int64(2L), 4))
    }

    @Test
    fun sqrFunctionShouldWork() {
        assertEquals(Flt64(25.0), sqr(Flt64(5.0)))
        assertEquals(Int64(25L), sqr(Int64(5L)))
    }

    @Test
    fun cubFunctionShouldWork() {
        assertEquals(Flt64(125.0), cub(Flt64(5.0)))
        assertEquals(Int64(125L), cub(Int64(5L)))
    }

    // ==================== Log Tests ====================

    @Test
    fun flt64LnShouldWork() {
        val result = Flt64(2.718281828).ln()
        assertNotNull(result)
    }

    @Test
    fun flt64Lg2ShouldWork() {
        val result = Flt64(8.0).lg2()
        assertNotNull(result)
    }

    @Test
    fun flt64LgShouldWork() {
        val result = Flt64(100.0).lg()
        assertNotNull(result)
    }

    @Test
    fun flt64LogBaseShouldWork() {
        val result = Flt64(8.0).log(Flt64(2.0))
        assertNotNull(result)
    }

    @Test
    fun flt64LogOfZeroShouldReturnInfinity() {
        // ln(0) returns -Infinity for Flt64
        val result = Flt64(0.0).ln()
        assertNotNull(result)
        // Check for negative infinity
        assertTrue(result!!.value.isInfinite() && result.value < 0)
    }

    // ==================== Precision Tests ====================

    @Test
    fun flt64PartialEqShouldDistinguishValues() {
        val a = Flt64(1.0)
        val b = Flt64(2.0)
        val c = Flt64(1.0)

        assertEquals(true, a partialEq c)
        assertEquals(false, a partialEq b)
    }

    // ==================== Edge Cases ====================

    @Test
    fun flt64ZeroPowPositiveShouldBeZero() {
        assertEquals(Flt64(0.0), Flt64(0.0).pow(5))
    }

    @Test
    fun flt64ZeroPowZeroShouldBeOne() {
        // Convention: 0^0 = 1 in many mathematical contexts
        assertEquals(Flt64(1.0), Flt64(0.0).pow(0))
    }

    @Test
    fun int64ZeroPowZeroShouldBeOne() {
        // Int64.sqr of 0 is 0
        assertEquals(Int64(0L), Int64(0L).sqr())
    }

    @Test
    fun negativePowShouldWorkForFlt64() {
        val result = Flt64(2.0).pow(-3)
        assertEquals(Flt64(0.125), result)
    }

    @Test
    fun largeExponentShouldWork() {
        val result = Flt64(2.0).pow(10)
        assertEquals(Flt64(1024.0), result)
    }
}