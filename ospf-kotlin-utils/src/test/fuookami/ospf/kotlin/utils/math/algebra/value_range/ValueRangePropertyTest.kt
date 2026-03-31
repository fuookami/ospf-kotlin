package fuookami.ospf.kotlin.utils.math.algebra.value_range

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.operator.eq
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValueRangePropertyTest {
    @Test
    fun openAndClosedBoundsShouldAffectContains() {
        val openRange = ValueRange(Flt64.one, Flt64.three, Interval.Open, Interval.Open).value!!
        val closedRange = ValueRange(Flt64.one, Flt64.three, Interval.Closed, Interval.Closed).value!!

        assertTrue(!openRange.contains(Flt64.one))
        assertTrue(!openRange.contains(Flt64.three))
        assertTrue(openRange.contains(Flt64.two))

        assertTrue(closedRange.contains(Flt64.one))
        assertTrue(closedRange.contains(Flt64.three))
    }

    @Test
    fun equalBoundsWithOpenIntervalShouldBeInvalid() {
        val invalid = ValueRange(Flt64.one, Flt64.one, Interval.Open, Interval.Open)
        val valid = ValueRange(Flt64.one, Flt64.one, Interval.Closed, Interval.Closed)

        assertTrue(!invalid.ok)
        assertTrue(valid.ok)
    }

    @Test
    fun intersectionAndUnionShouldBeSymmetricWhenOverlapping() {
        val lhs = ValueRange(Flt64.one, Flt64(4.0)).value!!
        val rhs = ValueRange(Flt64(3.0), Flt64(6.0)).value!!

        val i1 = lhs intersect rhs
        val i2 = rhs intersect lhs
        val u1 = lhs union rhs
        val u2 = rhs union lhs

        assertNotNull(i1)
        assertNotNull(i2)
        assertNotNull(u1)
        assertNotNull(u2)

        assertTrue(i1.lowerBound.value.unwrap() eq Flt64(3.0))
        assertTrue(i1.upperBound.value.unwrap() eq Flt64(4.0))
        assertEquals(i1, i2)

        assertTrue(u1.lowerBound.value.unwrap() eq Flt64.one)
        assertTrue(u1.upperBound.value.unwrap() eq Flt64(6.0))
        assertEquals(u1, u2)
    }

    @Test
    fun disjointRangesShouldNotIntersectOrUnion() {
        val lhs = ValueRange(Flt64.one, Flt64.two).value!!
        val rhs = ValueRange(Flt64(3.0), Flt64(4.0)).value!!

        assertNull(lhs intersect rhs)
        assertNull(lhs union rhs)
    }

    @Test
    fun negativeScaleShouldFlipRangeBounds() {
        val range = ValueRange(Flt64.one, Flt64.two).value!!
        val scaled = range * Flt64(-2.0)

        assertNotNull(scaled)
        assertTrue(scaled.lowerBound.value.unwrap() eq Flt64(-4.0))
        assertTrue(scaled.upperBound.value.unwrap() eq Flt64(-2.0))
    }
}
