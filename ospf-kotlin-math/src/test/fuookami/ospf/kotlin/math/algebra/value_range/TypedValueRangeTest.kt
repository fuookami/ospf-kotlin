package fuookami.ospf.kotlin.math.algebra.value_range

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.algebra.number.Flt64
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
    fun scalarShiftShouldPreserveTypedIntervals() {
        val range = TypedValueRange.closedOpen(Flt64.one, Flt64(4.0), Flt64).value!!

        val shifted = range + Flt64.two
        val shiftedBack = shifted - Flt64.two

        assertEquals(Interval.Closed, shifted.lowerInterval)
        assertEquals(Interval.Open, shifted.upperInterval)
        assertEquals(Flt64(3.0), shifted.lowerBound.unwrap())
        assertEquals(Flt64(6.0), shifted.upperBound.unwrap())
        assertEquals(range, shiftedBack)
    }

    @Test
    fun sameKindUnionAndIntersectShouldKeepTypedIntervals() {
        val lhs = TypedValueRange.closedOpen(Flt64.one, Flt64(5.0), Flt64).value!!
        val rhs = TypedValueRange.closedOpen(Flt64(3.0), Flt64(8.0), Flt64).value!!

        val union = lhs unionTyped rhs
        val intersect = lhs intersectTyped rhs

        assertNotNull(union)
        assertNotNull(intersect)
        assertEquals(Interval.Closed, union.lowerInterval)
        assertEquals(Interval.Open, union.upperInterval)
        assertEquals(Interval.Closed, intersect.lowerInterval)
        assertEquals(Interval.Open, intersect.upperInterval)
        assertTrue(Flt64(7.0) in union)
        assertFalse(Flt64(8.0) in union)
        assertTrue(Flt64(4.0) in intersect)
        assertFalse(Flt64(5.0) in intersect)
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
    fun plusSameKindsShouldNotFallbackToRuntimeKinds() {
        val lhs = TypedValueRange.closed(Flt64.one, Flt64.two, Flt64).value!!
        val rhs = TypedValueRange.closed(Flt64(3.0), Flt64(4.0), Flt64).value!!

        val sum = lhs.plusTypedAcrossKinds(rhs)
        assertNotNull(sum)
        val sumView: TypedValueRange<Flt64, *, *> = sum

        assertFalse(sumView.lowerKind is RuntimeIntervalKind)
        assertFalse(sumView.upperKind is RuntimeIntervalKind)
    }

    @Test
    fun minusSameKindsShouldNotFallbackToRuntimeKinds() {
        val lhs = TypedValueRange.closed(Flt64(3.0), Flt64(5.0), Flt64).value!!
        val rhs = TypedValueRange.closed(Flt64.one, Flt64.two, Flt64).value!!

        val diff = lhs.minusTypedAcrossKinds(rhs)
        assertNotNull(diff)
        val diffView: TypedValueRange<Flt64, *, *> = diff

        assertFalse(diffView.lowerKind is RuntimeIntervalKind)
        assertFalse(diffView.upperKind is RuntimeIntervalKind)
    }

    @Test
    fun minusTypedShouldReturnNullWhenKindsCannotBePreserved() {
        val lhs = TypedValueRange.closedOpen(Flt64(3.0), Flt64(5.0), Flt64).value!!
        val rhs = TypedValueRange.closedOpen(Flt64.one, Flt64.two, Flt64).value!!

        val diff = lhs.minusTyped(rhs)

        assertNull(diff)
    }

    @Test
    fun plusCrossKindsShouldInferStaticKindsInsteadOfRuntimeKinds() {
        val lhs = TypedValueRange.closed(Flt64.one, Flt64.two, Flt64).value!!
        val rhs = TypedValueRange.open(Flt64(3.0), Flt64(4.0), Flt64).value!!

        val sum = lhs.plusTypedAcrossKinds(rhs)

        assertNotNull(sum)
        assertEquals(Interval.Open, sum.lowerInterval)
        assertEquals(Interval.Open, sum.upperInterval)
        assertFalse(sum.lowerKind is RuntimeIntervalKind)
        assertFalse(sum.upperKind is RuntimeIntervalKind)
    }

    @Test
    fun minusCrossKindsShouldInferStaticKindsInsteadOfRuntimeKinds() {
        val lhs = TypedValueRange.closedOpen(Flt64(3.0), Flt64(6.0), Flt64).value!!
        val rhs = TypedValueRange.openClosed(Flt64.one, Flt64.two, Flt64).value!!

        val diff = lhs.minusTypedAcrossKinds(rhs)

        assertNotNull(diff)
        assertEquals(Interval.Closed, diff.lowerInterval)
        assertEquals(Interval.Open, diff.upperInterval)
        assertFalse(diff.lowerKind is RuntimeIntervalKind)
        assertFalse(diff.upperKind is RuntimeIntervalKind)
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
    fun typedSignedScaleShouldKeepOrSwapKinds() {
        val range = TypedValueRange.closedOpen(Flt64.one, Flt64(3.0), Flt64).value!!

        val positive = range.timesPositive(Flt64.two)
        val negative = range.timesNegative(Flt64(-2.0))
        val invalidPositive = range.timesPositive(Flt64.zero)
        val invalidNegative = range.timesNegative(Flt64.one)

        assertNotNull(positive)
        assertNotNull(negative)
        assertEquals(Interval.Closed, positive.lowerInterval)
        assertEquals(Interval.Open, positive.upperInterval)
        assertEquals(Interval.Open, negative.lowerInterval)
        assertEquals(Interval.Closed, negative.upperInterval)
        assertEquals(Flt64(2.0), positive.lowerBound.unwrap())
        assertEquals(Flt64(6.0), positive.upperBound.unwrap())
        assertEquals(Flt64(-6.0), negative.lowerBound.unwrap())
        assertEquals(Flt64(-2.0), negative.upperBound.unwrap())
        assertNull(invalidPositive)
        assertNull(invalidNegative)
    }

    @Test
    fun typedSignedDivShouldKeepOrSwapKinds() {
        val range = TypedValueRange.closedOpen(Flt64(2.0), Flt64(6.0), Flt64).value!!

        val positive = range.divPositive(Flt64.two)
        val negative = range.divNegative(Flt64(-2.0))
        val invalidPositive = range.divPositive(Flt64.zero)
        val invalidNegative = range.divNegative(Flt64.one)

        assertNotNull(positive)
        assertNotNull(negative)
        assertEquals(Interval.Closed, positive.lowerInterval)
        assertEquals(Interval.Open, positive.upperInterval)
        assertEquals(Interval.Open, negative.lowerInterval)
        assertEquals(Interval.Closed, negative.upperInterval)
        assertEquals(Flt64.one, positive.lowerBound.unwrap())
        assertEquals(Flt64(3.0), positive.upperBound.unwrap())
        assertEquals(Flt64(-3.0), negative.lowerBound.unwrap())
        assertEquals(Flt64(-1.0), negative.upperBound.unwrap())
        assertNull(invalidPositive)
        assertNull(invalidNegative)
    }

    @Test
    fun timesTypedShouldDispatchBySignAndKeepTypedKinds() {
        val range = TypedValueRange.closedOpen(Flt64.one, Flt64(3.0), Flt64).value!!

        val positive = range.timesTyped(Flt64.two)
        val negative = range.timesTyped(Flt64(-2.0))
        val zero = range.timesTyped(Flt64.zero)

        assertNotNull(positive)
        assertNotNull(negative)
        assertNull(zero)

        val positiveView: TypedValueRange<Flt64, *, *> = positive
        val negativeView: TypedValueRange<Flt64, *, *> = negative
        assertFalse(positiveView.lowerKind is RuntimeIntervalKind)
        assertFalse(positiveView.upperKind is RuntimeIntervalKind)
        assertFalse(negativeView.lowerKind is RuntimeIntervalKind)
        assertFalse(negativeView.upperKind is RuntimeIntervalKind)
        assertEquals(Interval.Closed, positiveView.lowerInterval)
        assertEquals(Interval.Open, positiveView.upperInterval)
        assertEquals(Interval.Open, negativeView.lowerInterval)
        assertEquals(Interval.Closed, negativeView.upperInterval)
    }

    @Test
    fun divTypedShouldDispatchBySignAndKeepTypedKinds() {
        val range = TypedValueRange.closedOpen(Flt64(2.0), Flt64(6.0), Flt64).value!!

        val positive = range.divTyped(Flt64.two)
        val negative = range.divTyped(Flt64(-2.0))
        val zero = range.divTyped(Flt64.zero)

        assertNotNull(positive)
        assertNotNull(negative)
        assertNull(zero)

        val positiveView: TypedValueRange<Flt64, *, *> = positive
        val negativeView: TypedValueRange<Flt64, *, *> = negative
        assertFalse(positiveView.lowerKind is RuntimeIntervalKind)
        assertFalse(positiveView.upperKind is RuntimeIntervalKind)
        assertFalse(negativeView.lowerKind is RuntimeIntervalKind)
        assertFalse(negativeView.upperKind is RuntimeIntervalKind)
        assertEquals(Interval.Closed, positiveView.lowerInterval)
        assertEquals(Interval.Open, positiveView.upperInterval)
        assertEquals(Interval.Open, negativeView.lowerInterval)
        assertEquals(Interval.Closed, negativeView.upperInterval)
    }

    @Test
    fun timesTypedShouldKeepClosedKindsForZeroOnClosedRange() {
        val range = TypedValueRange.closed(Flt64.one, Flt64(3.0), Flt64).value!!

        val zeroScaled = range.timesTyped(Flt64.zero)

        assertNotNull(zeroScaled)
        assertEquals(Interval.Closed, zeroScaled.lowerInterval)
        assertEquals(Interval.Closed, zeroScaled.upperInterval)
        assertEquals(Flt64.zero, zeroScaled.lowerBound.unwrap())
        assertEquals(Flt64.zero, zeroScaled.upperBound.unwrap())
        assertFalse(zeroScaled.lowerKind is RuntimeIntervalKind)
        assertFalse(zeroScaled.upperKind is RuntimeIntervalKind)
    }

    @Test
    fun timesTypedShouldReturnNullForZeroOnOpenRange() {
        val range = TypedValueRange.open(Flt64.one, Flt64(3.0), Flt64).value!!

        val zeroScaled = range.timesTyped(Flt64.zero)

        assertNull(zeroScaled)
    }

    @Test
    fun timesTypedAcrossKindsShouldInferStaticKinds() {
        val lhs = TypedValueRange.closedOpen(Flt64.one, Flt64(3.0), Flt64).value!!
        val rhs = TypedValueRange.openClosed(Flt64(2.0), Flt64(4.0), Flt64).value!!

        val product = lhs.timesTypedAcrossKinds(rhs)

        assertNotNull(product)
        assertEquals(Interval.Open, product.lowerInterval)
        assertEquals(Interval.Open, product.upperInterval)
        assertEquals(Flt64(2.0), product.lowerBound.unwrap())
        assertEquals(Flt64(12.0), product.upperBound.unwrap())
        assertFalse(product.lowerKind is RuntimeIntervalKind)
        assertFalse(product.upperKind is RuntimeIntervalKind)
    }

    @Test
    fun timesTypedAcrossKindsShouldHandleNegativeAndPositiveRanges() {
        val lhs = TypedValueRange.closed(Flt64(-3.0), Flt64(-1.0), Flt64).value!!
        val rhs = TypedValueRange.closed(Flt64(2.0), Flt64(4.0), Flt64).value!!

        val product = lhs.timesTypedAcrossKinds(rhs)

        assertNotNull(product)
        assertEquals(Interval.Closed, product.lowerInterval)
        assertEquals(Interval.Closed, product.upperInterval)
        assertEquals(Flt64(-12.0), product.lowerBound.unwrap())
        assertEquals(Flt64(-2.0), product.upperBound.unwrap())
        assertFalse(product.lowerKind is RuntimeIntervalKind)
        assertFalse(product.upperKind is RuntimeIntervalKind)
    }

    @Test
    fun divTypedShouldKeepStaticKindsForFixedClosedRange() {
        val range = TypedValueRange.closed(Flt64(2.0), Flt64(2.0), Flt64).value!!

        val divided = range.divTyped(Flt64.two)

        assertNotNull(divided)
        assertEquals(Interval.Closed, divided.lowerInterval)
        assertEquals(Interval.Closed, divided.upperInterval)
        assertEquals(Flt64.one, divided.lowerBound.unwrap())
        assertEquals(Flt64.one, divided.upperBound.unwrap())
        assertFalse(divided.lowerKind is RuntimeIntervalKind)
        assertFalse(divided.upperKind is RuntimeIntervalKind)
    }

    @Test
    fun divTypedShouldKeepStaticKindsForOpenRange() {
        val range = TypedValueRange.open(Flt64(2.0), Flt64(6.0), Flt64).value!!

        val divided = range.divTyped(Flt64.two)

        assertNotNull(divided)
        assertEquals(Interval.Open, divided.lowerInterval)
        assertEquals(Interval.Open, divided.upperInterval)
        assertEquals(Flt64.one, divided.lowerBound.unwrap())
        assertEquals(Flt64(3.0), divided.upperBound.unwrap())
        assertFalse(divided.lowerKind is RuntimeIntervalKind)
        assertFalse(divided.upperKind is RuntimeIntervalKind)
    }

    @Test
    fun rangeContainsShouldWorkBetweenTypedRanges() {
        val outer = TypedValueRange.closed(Flt64.one, Flt64(10.0), Flt64).value!!
        val inner = TypedValueRange.open(Flt64(2.0), Flt64(9.0), Flt64).value!!

        assertTrue(inner in outer)
        assertFalse(outer in inner)
    }

    @Test
    fun containsRangeShouldRespectHalfOpenHalfClosedBoundaries() {
        val outer = TypedValueRange.closedOpen(Flt64.one, Flt64(5.0), Flt64).value!!
        val insideOpenClosed = TypedValueRange.openClosed(Flt64(1.5), Flt64(4.5), Flt64).value!!
        val outsideByUpperClosed = TypedValueRange.openClosed(Flt64(2.0), Flt64(5.0), Flt64).value!!
        val outsideByLower = TypedValueRange.closedOpen(Flt64.zero, Flt64(4.0), Flt64).value!!

        assertTrue(insideOpenClosed in outer)
        assertFalse(outsideByUpperClosed in outer)
        assertFalse(outsideByLower in outer)
    }

    @Test
    fun dynamicTypedRangeShouldHandleInfinityAndNegativeInfinity() {
        val geqZero = ValueRange(
            lb = Flt64.zero,
            ub = Infinity,
            lbInterval = Interval.Closed,
            constants = Flt64
        ).value!!.toDynamicTypedValueRange()
        val bounded = TypedValueRange.closed(Flt64(10.0), Flt64(20.0), Flt64).value!!
        val belowZero = ValueRange(
            lb = NegativeInfinity,
            ub = Flt64.one,
            ubInterval = Interval.Closed,
            constants = Flt64
        ).value!!.toDynamicTypedValueRange()

        assertTrue(Flt64(100.0) in geqZero)
        assertFalse(Flt64(-1.0) in geqZero)
        assertTrue(bounded in geqZero)
        assertFalse(belowZero in geqZero)
    }

    @Test
    fun containsRangeShouldRespectInfiniteBoundaryOpenCloseAtSameValue() {
        val geqClosed = ValueRange.geq(Flt64.one, Interval.Closed, Flt64).value!!.toDynamicTypedValueRange()
        val gtOpen = ValueRange.gr(Flt64.one, Flt64).value!!.toDynamicTypedValueRange()
        val startsAtOneClosed = TypedValueRange.closed(Flt64.one, Flt64(2.0), Flt64).value!!
        val startsAtOneOpen = TypedValueRange.openClosed(Flt64.one, Flt64(2.0), Flt64).value!!

        assertTrue(startsAtOneClosed in geqClosed)
        assertTrue(startsAtOneOpen in geqClosed)
        assertFalse(startsAtOneClosed in gtOpen)
        assertTrue(startsAtOneOpen in gtOpen)
    }

    @Test
    fun containsRangeShouldRespectUpperInfiniteBoundaryOpenCloseAtSameValue() {
        val leqClosed = ValueRange.leq(Flt64.three, Interval.Closed, Flt64).value!!.toDynamicTypedValueRange()
        val ltOpen = ValueRange.ls(Flt64.three, Flt64).value!!.toDynamicTypedValueRange()
        val endsAtThreeClosed = TypedValueRange.closed(Flt64(2.0), Flt64.three, Flt64).value!!
        val endsAtThreeOpen = TypedValueRange.closedOpen(Flt64(2.0), Flt64.three, Flt64).value!!

        assertTrue(endsAtThreeClosed in leqClosed)
        assertTrue(endsAtThreeOpen in leqClosed)
        assertFalse(endsAtThreeClosed in ltOpen)
        assertTrue(endsAtThreeOpen in ltOpen)
    }

    @Test
    fun signedScaleShouldKeepExpectedContainmentForInfiniteRanges() {
        val geqOne = ValueRange.geq(Flt64.one, Interval.Closed, Flt64).value!!.toDynamicTypedValueRange()

        val positive = geqOne.timesPositive(Flt64.two)
        val negative = geqOne.timesNegative(Flt64(-2.0))

        assertNotNull(positive)
        assertNotNull(negative)
        assertTrue(Flt64(2.0) in positive)
        assertTrue(Flt64(100.0) in positive)
        assertFalse(Flt64(1.5) in positive)
        assertTrue(Flt64(-2.0) in negative)
        assertTrue(Flt64(-100.0) in negative)
        assertFalse(Flt64(-1.0) in negative)
    }

    @Test
    fun signedDivShouldKeepExpectedContainmentForInfiniteRanges() {
        val geqTwo = ValueRange.geq(Flt64.two, Interval.Closed, Flt64).value!!.toDynamicTypedValueRange()

        val positive = geqTwo.divPositive(Flt64.two)
        val negative = geqTwo.divNegative(Flt64(-2.0))

        assertNotNull(positive)
        assertNotNull(negative)
        assertTrue(Flt64.one in positive)
        assertTrue(Flt64(50.0) in positive)
        assertFalse(Flt64(0.5) in positive)
        assertTrue(Flt64(-1.0) in negative)
        assertTrue(Flt64(-50.0) in negative)
        assertFalse(Flt64(-0.5) in negative)
    }

    @Test
    fun intersectShouldReturnNullForDisjointRanges() {
        val lhs = TypedValueRange.closed(Flt64.one, Flt64(2.0), Flt64).value!!
        val rhs = TypedValueRange.open(Flt64(3.0), Flt64(4.0), Flt64).value!!

        val intersect = lhs intersect rhs

        assertNull(intersect)
        assertFalse(rhs in lhs)
        assertFalse(lhs in rhs)
    }
}