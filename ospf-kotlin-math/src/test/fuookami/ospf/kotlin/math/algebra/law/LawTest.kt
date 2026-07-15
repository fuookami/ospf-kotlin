package fuookami.ospf.kotlin.math.algebra.law

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.operator.*
import fuookami.ospf.kotlin.math.*

class LawTest {
    @Test
    fun int64AdditiveGroupLawShouldHold() {
        val samples = listOf(Int64(-2L), Int64.zero, Int64.one, Int64.two, Int64(3L))
        val law = GroupLaw(
            samples = samples,
            add = { lhs, rhs -> lhs + rhs },
            zero = Int64.zero,
            negate = { -it },
            equal = { lhs, rhs -> lhs eq rhs }
        )
        assertTrue(law.validate())
    }

    @Test
    fun int64RingLawShouldHold() {
        val samples = listOf(Int64(-2L), Int64.zero, Int64.one, Int64.two, Int64(3L))
        val law = RingLaw(
            samples = samples,
            add = { lhs, rhs -> lhs + rhs },
            mul = { lhs, rhs -> lhs * rhs },
            zero = Int64.zero,
            one = Int64.one,
            negate = { -it },
            equal = { lhs, rhs -> lhs eq rhs }
        )
        assertTrue(law.validate())
    }

    @Test
    fun intXRingLawShouldHold() {
        val samples = listOf(IntX(-2L), IntX.zero, IntX.one, IntX.two, IntX(3L))
        val law = RingLaw(
            samples = samples,
            add = { lhs, rhs -> lhs + rhs },
            mul = { lhs, rhs -> lhs * rhs },
            zero = IntX.zero,
            one = IntX.one,
            negate = { -it },
            equal = { lhs, rhs -> lhs eq rhs }
        )
        assertTrue(law.validate())
    }

    @Test
    fun integerReciprocalShouldOnlyAllowUnits() {
        assertEquals(Int64.one, Int64.one.reciprocal())
        assertEquals(Int64(-1L), Int64(-1L).reciprocal())
        assertFailsWith<ArithmeticException> { Int64.zero.reciprocal() }
        assertFailsWith<ArithmeticException> { Int64.two.reciprocal() }
    }

    @Test
    fun uIntegerReciprocalShouldOnlyAllowOne() {
        assertEquals(UInt64.one, UInt64.one.reciprocal())
        assertFailsWith<ArithmeticException> { UInt64.zero.reciprocal() }
        assertFailsWith<ArithmeticException> { UInt64.two.reciprocal() }
    }

    @Test
    fun typedNumberPropertiesShouldExposeConstantsBackedTraits() {
        assertTrue(Flt64.one.isBounded)
        assertEquals(Flt64.minimum, Flt64.one.minBound)
        assertEquals(Flt64.maximum, Flt64.one.maxBound)
        assertTrue(Flt64.one.supportsInfinity)
        assertEquals(Flt64.infinity, Flt64.one.positiveInfinity)
        assertEquals(Flt64.negativeInfinity, Flt64.one.negativeInfinityValue)
        assertTrue(Flt64.one.isFixed)
        assertEquals(Flt64.decimalDigits, Flt64.one.fixedDigits)
        assertEquals(Flt64.decimalPrecision, Flt64.one.fixedPrecision)
        assertEquals(Flt64.epsilon, Flt64.one.precisionEpsilon)

        assertTrue(Int64.one.isBounded)
        assertEquals(Int64.minimum, Int64.one.minBound)
        assertEquals(Int64.maximum, Int64.one.maxBound)
        assertTrue(!Int64.one.supportsInfinity)
        assertEquals(null, Int64.one.positiveInfinity)
        assertTrue(!Int64.one.isFixed)

        assertTrue(!IntX.one.isBounded)
        assertEquals(null, IntX.one.minBound)
        assertEquals(null, IntX.one.maxBound)

        assertTrue(!UIntX.one.isBounded)
        assertEquals(null, UIntX.one.minBound)
        assertEquals(null, UIntX.one.maxBound)

        val rtnX = RtnX(IntX.one, IntX.one)
        assertTrue(!rtnX.isBounded)
        assertEquals(null, rtnX.minBound)
        assertEquals(null, rtnX.maxBound)

        assertTrue(!FltX.one.isBounded)
        assertEquals(null, FltX.one.minBound)
        assertEquals(null, FltX.one.maxBound)
    }

    @Test
    fun rationalShouldRejectZeroDenominatorAndDropNaNInfinityConstants() {
        assertFailsWith<ArithmeticException> { Rtn64.invoke(Int64.one, Int64.zero) }
        assertFailsWith<ArithmeticException> { URtn64.invoke(UInt64.one, UInt64.zero) }

        assertEquals(null, Rtn64.nan)
        assertEquals(null, Rtn64.infinity)
        assertEquals(null, Rtn64.negativeInfinity)
    }

    @Test
    fun flt64FieldLawShouldHoldWithTolerance() {
        val eps = Flt64(1e-9)
        val samples = listOf(Flt64(-2.0), Flt64(-1.0), Flt64.zero, Flt64.one, Flt64.two)
        val law = FieldLaw(
            samples = samples,
            add = { lhs, rhs -> lhs + rhs },
            mul = { lhs, rhs -> lhs * rhs },
            zero = Flt64.zero,
            one = Flt64.one,
            negate = { -it },
            reciprocal = { it.reciprocal() },
            isZero = { it.abs() <= eps },
            equal = { lhs, rhs -> (lhs - rhs).abs() <= eps }
        )
        assertTrue(law.validate())
    }

    @Test
    fun rtnXCoreAlgebraicIdentitiesShouldHold() {
        val rtnXZero = RtnX(IntX.zero, IntX.one)
        val rtnXOne = RtnX(IntX.one, IntX.one)
        val a = RtnX(IntX.one, IntX(2L))
        val b = RtnX(IntX.one, IntX(3L))
        val c = RtnX(IntX.one, IntX(6L))

        assertTrue(((a + b) + c) eq (a + (b + c)))
        assertTrue((a + rtnXZero) eq a)
        assertTrue((a * rtnXOne) eq a)
        assertTrue((a * (b + c)) eq ((a * b) + (a * c)))
        assertTrue((a * a.reciprocal()) eq rtnXOne)
    }

    @Test
    fun fltXCoreAlgebraicIdentitiesShouldHoldWithTolerance() {
        val eps = FltX("1e-6")
        val a = FltX.two

        assertTrue(((a + FltX.zero) - a).abs() <= eps)
        assertTrue(((a * FltX.one) - a).abs() <= eps)
    }

    @Test
    fun tolerancedContractsShouldWork() {
        val eq = TolerancedEq<Flt64> { lhs, rhs, tolerance ->
            (lhs - rhs).abs() <= tolerance
        }
        val ord = TolerancedOrd<Flt64> { lhs, rhs, tolerance ->
            val delta = lhs - rhs
            if (delta.abs() <= tolerance) {
                Order.Equal
            } else if (delta < Flt64.zero) {
                Order.Less()
            } else {
                Order.Greater()
            }
        }

        assertTrue(eq.test(Flt64.one, Flt64(1.0000001), Flt64(1e-5)))
        assertEquals(Order.Equal, ord.test(Flt64.one, Flt64(1.0000001), Flt64(1e-5)))
        assertTrue(ord.test(Flt64.zero, Flt64.one, Flt64.zero) is Order.Less)
    }

    @Test
    fun defaultToleranceImplementationsShouldCoverMainNumericPaths() {
        val fltEq = defaultTolerancedEq<Flt64>()
        val fltOrd = defaultTolerancedOrd<Flt64>()
        assertTrue(fltEq.test(Flt64.one, Flt64(1.0000001), Flt64(1e-5)))
        assertEquals(Order.Equal, fltOrd.test(Flt64.one, Flt64(1.0000001), Flt64(1e-5)))

        val rtnEq = defaultTolerancedEq<Rtn8>()
        val rtnOrd = defaultTolerancedOrd<Rtn8>()
        assertTrue(rtnEq.test(Rtn8(Int8(2), Int8(1)), Rtn8(Int8(1), Int8(1)), Rtn8(Int8.one, Int8.one)))
        assertEquals(Order.Greater(), rtnOrd.test(Rtn8(Int8(2), Int8.one), Rtn8(Int8.one, Int8.one), Rtn8(Int8.zero, Int8.one)))

        val fltXEq = defaultTolerancedEq<FltX>()
        val fltXOrd = defaultTolerancedOrd<FltX>()
        assertTrue(fltXEq.test(FltX("1.000"), FltX("1.001"), FltX("0.01")))
        assertEquals(Order.Greater(), fltXOrd.test(FltX("2.0"), FltX("1.0"), FltX("0.0")))

        val intXEq = defaultTolerancedEq<IntX>()
        val intXOrd = defaultTolerancedOrd<IntX>()
        assertTrue(intXEq.test(IntX(10L), IntX(11L), IntX(1L)))
        assertEquals(Order.Less(), intXOrd.test(IntX(10L), IntX(12L), IntX.zero))

        val uIntXEq = defaultTolerancedEq<UIntX>()
        val uIntXOrd = defaultTolerancedOrd<UIntX>()
        assertTrue(uIntXEq.test(UIntX(10L), UIntX(11L), UIntX(1L)))
        assertEquals(Order.Less(), uIntXOrd.test(UIntX(10L), UIntX(12L), UIntX.zero))

        val rtnXEq = defaultTolerancedEq<RtnX>()
        val rtnXOrd = defaultTolerancedOrd<RtnX>()
        assertTrue(rtnXEq.test(RtnX(IntX(10L), IntX.one), RtnX(IntX(11L), IntX.one), RtnX(IntX.one, IntX.one)))
        assertEquals(
            Order.Less(),
            rtnXOrd.test(
                RtnX(IntX(10L), IntX.one),
                RtnX(IntX(12L), IntX.one),
                RtnX(IntX.zero, IntX.one)
            )
        )

        val uRtnXEq = defaultTolerancedEq<URtnX>()
        val uRtnXOrd = defaultTolerancedOrd<URtnX>()
        assertTrue(
            uRtnXEq.test(
                URtnX(UIntX(10L), UIntX.one),
                URtnX(UIntX(11L), UIntX.one),
                URtnX(UIntX.one, UIntX.one)
            )
        )
        assertEquals(
            Order.Less(),
            uRtnXOrd.test(
                URtnX(UIntX(10L), UIntX.one),
                URtnX(UIntX(12L), UIntX.one),
                URtnX(UIntX.zero, UIntX.one)
            )
        )
    }
}
