package fuookami.ospf.kotlin.utils.math.algebra.law

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.Int64
import fuookami.ospf.kotlin.utils.operator.Order
import fuookami.ospf.kotlin.utils.operator.TolerancedEq
import fuookami.ospf.kotlin.utils.operator.TolerancedOrd
import fuookami.ospf.kotlin.utils.operator.eq
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
}

