package fuookami.ospf.kotlin.utils.math.algebra.value_range

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.operator.Order
import fuookami.ospf.kotlin.utils.functional.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Tests for ValueRange subcomponents: Bound, Interval, ValueWrapper
 */
class ValueRangeComponentTest {

    // ==================== Interval Tests ====================

    @Test
    fun intervalOpenShouldHaveCorrectSigns() {
        assertEquals("(", Interval.Open.lowerSign)
        assertEquals(")", Interval.Open.upperSign)
    }

    @Test
    fun intervalClosedShouldHaveCorrectSigns() {
        assertEquals("[", Interval.Closed.lowerSign)
        assertEquals("]", Interval.Closed.upperSign)
    }

    @Test
    fun intervalUnionShouldWork() {
        assertEquals(Interval.Open, Interval.Open union Interval.Open)
        assertEquals(Interval.Closed, Interval.Open union Interval.Closed)
        assertEquals(Interval.Closed, Interval.Closed union Interval.Open)
        assertEquals(Interval.Closed, Interval.Closed union Interval.Closed)
    }

    @Test
    fun intervalIntersectShouldWork() {
        assertEquals(Interval.Open, Interval.Open intersect Interval.Open)
        assertEquals(Interval.Open, Interval.Open intersect Interval.Closed)
        assertEquals(Interval.Open, Interval.Closed intersect Interval.Open)
        assertEquals(Interval.Closed, Interval.Closed intersect Interval.Closed)
    }

    @Test
    fun intervalOuterShouldWork() {
        assertFalse(Interval.Open outer Interval.Open)
        assertFalse(Interval.Open outer Interval.Closed)
        assertTrue(Interval.Closed outer Interval.Open)
        assertFalse(Interval.Closed outer Interval.Closed)
    }

    @Test
    fun intervalLowerBoundOperatorShouldWorkForOpen() {
        val op = Interval.Open.lowerBoundOperator<Flt64>()
        assertTrue(op(Flt64(1.0), Flt64(2.0)))  // 1 < 2
        assertFalse(op(Flt64(2.0), Flt64(1.0)))  // 2 < 1 is false
        assertFalse(op(Flt64(1.0), Flt64(1.0)))  // 1 < 1 is false
    }

    @Test
    fun intervalLowerBoundOperatorShouldWorkForClosed() {
        val op = Interval.Closed.lowerBoundOperator<Flt64>()
        assertTrue(op(Flt64(1.0), Flt64(2.0)))  // 1 <= 2
        assertFalse(op(Flt64(2.0), Flt64(1.0)))  // 2 <= 1 is false
        assertTrue(op(Flt64(1.0), Flt64(1.0)))  // 1 <= 1
    }

    @Test
    fun intervalUpperBoundOperatorShouldWorkForOpen() {
        val op = Interval.Open.upperBoundOperator<Flt64>()
        assertFalse(op(Flt64(1.0), Flt64(2.0)))  // 1 > 2 is false
        assertTrue(op(Flt64(2.0), Flt64(1.0)))  // 2 > 1
        assertFalse(op(Flt64(1.0), Flt64(1.0)))  // 1 > 1 is false
    }

    @Test
    fun intervalUpperBoundOperatorShouldWorkForClosed() {
        val op = Interval.Closed.upperBoundOperator<Flt64>()
        assertFalse(op(Flt64(1.0), Flt64(2.0)))  // 1 >= 2 is false
        assertTrue(op(Flt64(2.0), Flt64(1.0)))  // 2 >= 1
        assertTrue(op(Flt64(1.0), Flt64(1.0)))  // 1 >= 1
    }

    // ==================== ValueWrapper Tests ====================

    @Test
    fun valueWrapperShouldWrapFiniteValue() {
        val result = ValueWrapper(Flt64(42.0), Flt64)
        assertTrue(result is Ok)
        val wrapper = (result as Ok).value
        assertTrue(wrapper is ValueWrapper.Value<Flt64>)
        assertEquals(Flt64(42.0), wrapper.value)
    }

    @Test
    fun valueWrapperShouldCreateInfinity() {
        val inf = ValueWrapper.Infinity(Flt64)
        assertTrue(inf.isInfinity)
        assertFalse(inf.isNegativeInfinity)
        assertTrue(inf.isInfinityOrNegativeInfinity)
    }

    @Test
    fun valueWrapperShouldCreateNegativeInfinity() {
        val negInf = ValueWrapper.NegativeInfinity(Flt64)
        assertFalse(negInf.isInfinity)
        assertTrue(negInf.isNegativeInfinity)
        assertTrue(negInf.isInfinityOrNegativeInfinity)
    }

    @Test
    fun valueWrapperOrdShouldCompareValues() {
        val a = ValueWrapper(Flt64(1.0), Flt64).value!!
        val b = ValueWrapper(Flt64(2.0), Flt64).value!!

        assertTrue((a ord b) is Order.Less)
        assertTrue((b ord a) is Order.Greater)
        assertTrue((a ord a) is Order.Equal)
    }

    @Test
    fun valueWrapperOrdShouldCompareWithInfinity() {
        val value = ValueWrapper(Flt64(100.0), Flt64).value!!
        val inf = ValueWrapper.Infinity(Flt64)
        val negInf = ValueWrapper.NegativeInfinity(Flt64)

        assertTrue((value ord inf) is Order.Less)
        assertTrue((inf ord value) is Order.Greater)
        assertTrue((negInf ord value) is Order.Less)
        assertTrue((value ord negInf) is Order.Greater)
    }

    @Test
    fun valueWrapperUnwrapShouldReturnValue() {
        val wrapper = ValueWrapper(Flt64(42.0), Flt64).value!!
        assertEquals(Flt64(42.0), wrapper.unwrap())
    }

    @Test
    fun valueWrapperCopyShouldWork() {
        val original = ValueWrapper(Flt64(42.0), Flt64).value!!
        val copy = original.copy()
        // Check values are equal
        assertEquals(original.unwrap(), copy.unwrap())
    }

    // ==================== Bound Tests ====================

    @Test
    fun boundShouldStoreValueAndInterval() {
        val value = ValueWrapper(Flt64(5.0), Flt64).value!!
        val bound = Bound(value, Interval.Closed)

        assertEquals(value, bound.value)
        assertEquals(Interval.Closed, bound.interval)
    }

    @Test
    fun boundEqShouldCompareValue() {
        val value = ValueWrapper(Flt64(5.0), Flt64).value!!
        val bound = Bound(value, Interval.Closed)

        assertTrue(bound.eq(Flt64(5.0)))
        assertFalse(bound.eq(Flt64(4.0)))
    }

    @Test
    fun boundPartialOrdShouldWork() {
        val lower = Bound(ValueWrapper(Flt64(1.0), Flt64).value!!, Interval.Closed)
        val upper = Bound(ValueWrapper(Flt64(2.0), Flt64).value!!, Interval.Closed)

        assertTrue((lower partialOrd upper) is Order.Less)
        assertTrue((upper partialOrd lower) is Order.Greater)
    }

    @Test
    fun boundPlusShouldWork() {
        val value = ValueWrapper(Flt64(5.0), Flt64).value!!
        val bound = Bound(value, Interval.Closed)
        val result = bound + Flt64(3.0)

        assertEquals(Flt64(8.0), result.value.unwrap())
        assertEquals(Interval.Closed, result.interval)
    }

    @Test
    fun boundMinusShouldWork() {
        val value = ValueWrapper(Flt64(5.0), Flt64).value!!
        val bound = Bound(value, Interval.Closed)
        val result = bound - Flt64(3.0)

        assertEquals(Flt64(2.0), result.value.unwrap())
        assertEquals(Interval.Closed, result.interval)
    }

    @Test
    fun boundTimesShouldWork() {
        val value = ValueWrapper(Flt64(5.0), Flt64).value!!
        val bound = Bound(value, Interval.Closed)
        val result = bound * Flt64(2.0)

        assertEquals(Flt64(10.0), result.value.unwrap())
        assertEquals(Interval.Closed, result.interval)
    }

    @Test
    fun boundDivShouldWork() {
        val value = ValueWrapper(Flt64(10.0), Flt64).value!!
        val bound = Bound(value, Interval.Closed)
        val result = bound / Flt64(2.0)

        assertEquals(Flt64(5.0), result.value.unwrap())
        assertEquals(Interval.Closed, result.interval)
    }

    @Test
    fun boundCopyShouldWork() {
        val value = ValueWrapper(Flt64(5.0), Flt64).value!!
        val original = Bound(value, Interval.Closed)
        val copy = original.copy()

        assertEquals(original.value.unwrap(), copy.value.unwrap())
        assertEquals(original.interval, copy.interval)
    }

    @Test
    fun boundWithInfinityShouldForceOpenInterval() {
        val inf = ValueWrapper.Infinity(Flt64)
        val bound = Bound(inf, Interval.Closed)  // Try to create closed infinity

        // Infinity should force open interval
        assertEquals(Interval.Open, bound.interval)
    }

    @Test
    fun boundUnaryMinusShouldWorkForFlt64() {
        val value = ValueWrapper(Flt64(5.0), Flt64).value!!
        val bound = Bound(value, Interval.Closed)
        val negated = -bound

        assertEquals(Flt64(-5.0), negated.value.unwrap())
        assertEquals(Interval.Closed, negated.interval)
    }
}