package fuookami.ospf.kotlin.utils.math.algebra.value_range

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TypedValueRangeTest {
    @Test
    fun closedAndOpenShouldAffectBoundaryContains() {
        val closed: ClosedTypedValueRange<Flt64> = TypedValueRange.closed(Flt64.one, Flt64.three, Flt64).value!!
        val open: OpenTypedValueRange<Flt64> = TypedValueRange.open(Flt64.one, Flt64.three, Flt64).value!!

        assertTrue(Flt64.one in closed)
        assertTrue(Flt64.three in closed)
        assertFalse(Flt64.one in open)
        assertFalse(Flt64.three in open)
        assertTrue(Flt64.two in open)
    }

    @Test
    fun fromDynamicShouldRejectIntervalMismatch() {
        val dynamic = ValueRange(
            lb = Flt64.one,
            ub = Flt64.three,
            lbInterval = Interval.Open,
            ubInterval = Interval.Closed,
            constants = Flt64
        ).value!!

        val result = TypedValueRange.fromDynamic(
            range = dynamic,
            lowerKind = ClosedIntervalKind,
            upperKind = ClosedIntervalKind
        )

        assertTrue(result is Failed)
    }

    @Test
    fun unionAndIntersectShouldReturnDynamicIntervals() {
        val lhs = TypedValueRange.closed(Flt64.one, Flt64(4.0), Flt64).value!!
        val rhs = TypedValueRange.open(Flt64(2.0), Flt64(6.0), Flt64).value!!

        val union = lhs union rhs
        val intersect = lhs intersect rhs

        assertNotNull(union)
        assertNotNull(intersect)
        assertEquals(Interval.Closed, union.lowerInterval)
        assertEquals(Interval.Open, union.upperInterval)
        assertEquals(Interval.Open, intersect.lowerInterval)
        assertEquals(Interval.Closed, intersect.upperInterval)
        assertTrue(Flt64(5.0) in union)
        assertFalse(Flt64(2.0) in intersect)
    }

    @Test
    fun plusAndMinusShouldFollowValueRangeSemantics() {
        val lhs = TypedValueRange.closed(Flt64.one, Flt64.two, Flt64).value!!
        val rhs = TypedValueRange.closed(Flt64(3.0), Flt64(4.0), Flt64).value!!

        val sum = lhs + rhs
        val diff = rhs - lhs

        assertEquals(Flt64(4.0), sum.lowerBound.unwrap())
        assertEquals(Flt64(6.0), sum.upperBound.unwrap())
        assertEquals(Flt64.one, diff.lowerBound.unwrap())
        assertEquals(Flt64(3.0), diff.upperBound.unwrap())
    }

    @Test
    fun timesAndDivShouldSupportSignAndZeroRules() {
        val range = TypedValueRange.closed(Flt64.one, Flt64.two, Flt64).value!!

        val negScaled = range * Flt64(-2.0)
        val divided = range / Flt64.two
        val divByZero = range / Flt64.zero

        assertNotNull(negScaled)
        assertNotNull(divided)
        assertEquals(Flt64(-4.0), negScaled.lowerBound.unwrap())
        assertEquals(Flt64(-2.0), negScaled.upperBound.unwrap())
        assertEquals(Flt64(0.5), divided.lowerBound.unwrap())
        assertEquals(Flt64.one, divided.upperBound.unwrap())
        assertNull(divByZero)
    }

    @Test
    fun rangeContainsShouldWorkBetweenTypedRanges() {
        val outer = TypedValueRange.closed(Flt64.one, Flt64(10.0), Flt64).value!!
        val inner = TypedValueRange.open(Flt64(2.0), Flt64(9.0), Flt64).value!!

        assertTrue(inner in outer)
        assertFalse(outer in inner)
    }
}
