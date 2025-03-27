package fuookami.ospf.kotlin.utils.math.value_range

import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*

class QuantityValueRangeTest {
    @Test
    fun testConstructor() {
        val range = ValueRange(Flt64.one, Flt64.two)
        assert(range.ok)
        assert(range.value!!.lowerBound.value.unwrap() eq Flt64.one)
        assert(range.value!!.upperBound.value.unwrap() eq Flt64.two)
        val invalidRange = ValueRange(Flt64.two, Flt64.one)
        assert(!invalidRange.ok)
    }

    @Test
    fun testPlus() {
        val range = ValueRange(Flt64.one, Flt64.two).value!!
        val addedRange = range + Flt64.one
        assert(addedRange.lowerBound.value.unwrap() eq Flt64.two)
        assert(addedRange.upperBound.value.unwrap() eq Flt64.three)
        val twiceRange = range + range
        assert(twiceRange.lowerBound.value.unwrap() eq Flt64.two)
        assert(twiceRange.upperBound.value.unwrap() eq Flt64(4.0))
        val infRange = range + Flt64.infinity
        assert(infRange.lowerBound.value is ValueWrapper.Infinity)
        assert(infRange.lowerBound.interval == Interval.Open)
        assert(infRange.upperBound.value is ValueWrapper.Infinity)
        assert(infRange.upperBound.interval == Interval.Open)
        val negInfRange = range + -Flt64.infinity
        assert(negInfRange.lowerBound.value is ValueWrapper.NegativeInfinity)
        assert(negInfRange.lowerBound.interval == Interval.Open)
        assert(negInfRange.upperBound.value is ValueWrapper.NegativeInfinity)
        assert(negInfRange.upperBound.interval == Interval.Open)
        val infRange2 = ValueRange(Flt64.one, Flt64.infinity).value!! + Flt64.one
        assert(infRange2.lowerBound.value.unwrap() eq Flt64.two)
        assert(infRange2.lowerBound.interval == Interval.Closed)
        assert(infRange2.upperBound.value is ValueWrapper.Infinity)
        assert(infRange2.upperBound.interval == Interval.Open)
        val negInfRange2 = ValueRange(-Flt64.infinity, Flt64.one).value!! + Flt64.one
        assert(negInfRange2.lowerBound.value is ValueWrapper.NegativeInfinity)
        assert(negInfRange2.lowerBound.interval == Interval.Open)
        assert(negInfRange2.upperBound.value.unwrap() eq Flt64.two)
        assert(negInfRange2.upperBound.interval == Interval.Closed)
    }

    @Test
    fun testSubtract() {
        val range = ValueRange(Flt64.one, Flt64.two).value!!
        val subtractedRange = range - Flt64.one
        assert(subtractedRange.lowerBound.value.unwrap() eq Flt64.zero)
        assert(subtractedRange.upperBound.value.unwrap() eq Flt64.one)
        val noneRange = range - range
        assert(noneRange.lowerBound.value.unwrap() eq -Flt64.one)
        assert(noneRange.upperBound.value.unwrap() eq Flt64.one)
    }

    @Test
    fun testMultiply() {
        val range = ValueRange(Flt64.one, Flt64.two).value!!
        val zeroRange = range * Flt64.zero
        assert(zeroRange!!.fixedValue eq Flt64.zero)
        val twiceRange = range * Flt64.two
        assert(twiceRange!!.lowerBound.value.unwrap() eq Flt64.two)
        assert(twiceRange.upperBound.value.unwrap() eq Flt64(4.0))
        val negTwiceRange = range * -Flt64.two
        assert(negTwiceRange!!.lowerBound.value.unwrap() eq Flt64(-4.0))
        assert(negTwiceRange.upperBound.value.unwrap() eq -Flt64.two)
        val squareRange = range * range
        assert(squareRange!!.lowerBound.value.unwrap() eq Flt64.one)
        assert(squareRange.upperBound.value.unwrap() eq Flt64(4.0))
    }

    @Test
    fun testDivide() {
        val range = ValueRange(Flt64.one, Flt64.two).value!!
        val halfRange = range / Flt64.two
        assert(halfRange!!.lowerBound.value.unwrap() eq Flt64(0.5))
        assert(halfRange.upperBound.value.unwrap() eq Flt64.one)
        val negHalfRange = range / -Flt64.two
        assert(negHalfRange!!.lowerBound.value.unwrap() eq -Flt64.one)
        assert(negHalfRange.upperBound.value.unwrap() eq -Flt64(0.5))
    }

    @Test
    fun testIntersection() {
        val range = ValueRange(Flt64.one, Flt64.three).value!!
        val leftHalfRange1 = ValueRange(Flt64.zero, Flt64.two).value!! intersect range
        assert(leftHalfRange1 != null && leftHalfRange1.lowerBound.value.unwrap() eq Flt64.one)
        assert(leftHalfRange1 != null && leftHalfRange1.upperBound.value.unwrap() eq Flt64.two)
        val rightHalfRange1 = range intersect ValueRange(Flt64.zero, Flt64.two).value!!
        assert(rightHalfRange1 != null && rightHalfRange1.lowerBound.value.unwrap() eq Flt64.one)
        assert(rightHalfRange1 != null && rightHalfRange1.upperBound.value.unwrap() eq Flt64.two)
        val leftHalfRange2 = ValueRange(Flt64.two, Flt64.ten).value!! intersect range
        assert(leftHalfRange2 != null && leftHalfRange2.lowerBound.value.unwrap() eq Flt64.two)
        assert(leftHalfRange2 != null && leftHalfRange2.upperBound.value.unwrap() eq Flt64.three)
        val rightHalfRange2 = range intersect ValueRange(Flt64.two, Flt64.ten).value!!
        assert(rightHalfRange2 != null && rightHalfRange2.lowerBound.value.unwrap() eq Flt64.two)
        assert(rightHalfRange2 != null && rightHalfRange2.upperBound.value.unwrap() eq Flt64.three)
        val noneRange = range intersect ValueRange(Flt64(4.0), Flt64.ten).value!!
        assert(noneRange == null)
        val infRange = range intersect ValueRange(Flt64.one, Flt64.infinity).value!!
        assert(infRange != null && infRange.lowerBound.value.unwrap() eq Flt64.one)
        assert(infRange != null && infRange.lowerBound.interval == Interval.Closed)
        assert(infRange != null && infRange.upperBound.value.unwrap() eq Flt64.three)
        assert(infRange != null && infRange.upperBound.interval == Interval.Closed)
        val negInfRange = range intersect ValueRange(-Flt64.infinity, Flt64.two).value!!
        assert(negInfRange != null && negInfRange.lowerBound.value.unwrap() eq Flt64.one)
        assert(negInfRange != null && negInfRange.lowerBound.interval == Interval.Closed)
        assert(negInfRange != null && negInfRange.upperBound.value.unwrap() eq Flt64.two)
        assert(negInfRange != null && negInfRange.upperBound.interval == Interval.Closed)
    }

    @Test
    fun testUnion() {
        val range = ValueRange(Flt64.one, Flt64.three).value!!
        val unionRange1 = range union ValueRange(Flt64.zero, Flt64.two).value!!
        assert(unionRange1 != null && unionRange1.lowerBound.value.unwrap() eq Flt64.zero)
        assert(unionRange1 != null && unionRange1.upperBound.value.unwrap() eq Flt64.three)
        val unionRange2 = range union ValueRange(Flt64.two, Flt64.ten).value!!
        assert(unionRange2 != null && unionRange2.lowerBound.value.unwrap() eq Flt64.one)
        assert(unionRange2 != null && unionRange2.upperBound.value.unwrap() eq Flt64.ten)
        val unionRange3 = range union ValueRange(Flt64.zero, Flt64.ten).value!!
        assert(unionRange3 != null && unionRange3.lowerBound.value.unwrap() eq Flt64.zero)
        assert(unionRange3 != null && unionRange3.upperBound.value.unwrap() eq Flt64.ten)
        val noneRange = range union ValueRange(Flt64(4.0), Flt64.ten).value!!
        assert(noneRange == null)
        val infRange = range union ValueRange(Flt64.one, Flt64.infinity).value!!
        assert(infRange != null && infRange.lowerBound.value.unwrap() eq Flt64.one)
        assert(infRange != null && infRange.lowerBound.interval == Interval.Closed)
        assert(infRange != null && infRange.upperBound.value is ValueWrapper.Infinity)
        assert(infRange != null && infRange.upperBound.interval == Interval.Open)
        val negInfRange = range union ValueRange(-Flt64.infinity, Flt64.one).value!!
        assert(negInfRange != null && negInfRange.lowerBound.value is ValueWrapper.NegativeInfinity)
        assert(negInfRange != null && negInfRange.lowerBound.interval == Interval.Open)
        assert(negInfRange != null && negInfRange.upperBound.value.unwrap() eq Flt64.three)
        assert(negInfRange != null && negInfRange.upperBound.interval == Interval.Closed)
    }

    @Test
    fun testContains() {
        val range = ValueRange(Flt64.one, Flt64.three).value!!
        assert(range.contains(Flt64.one))
        assert(range.contains(Flt64.two))
        assert(range.contains(Flt64.three))
        assert(!range.contains(Flt64.zero))
        assert(!range.contains(Flt64.ten))

        assert(range.contains(ValueRange(Flt64.one, Flt64.two).value!!))
        assert(range.contains(ValueRange(Flt64.two, Flt64.three).value!!))
        assert(range.contains(ValueRange(Flt64.one, Flt64.three).value!!))
        assert(!range.contains(ValueRange(Flt64.zero, Flt64.one).value!!))
        assert(!range.contains(ValueRange(Flt64.zero, Flt64.two).value!!))
        assert(!range.contains(ValueRange(Flt64.two, Flt64.ten).value!!))
    }
}
