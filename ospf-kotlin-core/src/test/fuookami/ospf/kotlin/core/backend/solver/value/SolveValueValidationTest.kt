package fuookami.ospf.kotlin.core.backend.solver.value

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SolveValueValidationTest {
    @Test
    fun strictPolicyShouldRejectNaN() {
        val result = validateSolverFlt64Value(
            value = Flt64.nan,
            policy = SolveValueConversionPolicy.Strict,
            fieldName = "demo"
        )

        assertTrue(result is Failed)
        assertEquals(ErrorCode.IllegalArgument, (result as Failed).error.code)
    }

    @Test
    fun allowRoundingPolicyShouldAllowNaN() {
        val result = validateSolverFlt64Value(
            value = Flt64.nan,
            policy = SolveValueConversionPolicy.AllowRounding,
            fieldName = "demo"
        )

        assertTrue(result is Ok)
    }

    @Test
    fun strictPolicyShouldAllowFiniteValue() {
        val result = validateSolverFlt64Value(
            value = Flt64(1.25),
            policy = SolveValueConversionPolicy.Strict,
            fieldName = "demo"
        )

        assertTrue(result is Ok)
    }

    @Test
    fun strictPolicyShouldRejectInfinity() {
        val result = validateSolverFlt64Value(
            value = Flt64.infinity,
            policy = SolveValueConversionPolicy.Strict,
            fieldName = "demo"
        )

        assertTrue(result is Failed)
        assertEquals(ErrorCode.IllegalArgument, (result as Failed).error.code)
    }

    @Test
    fun strictPolicyShouldAllowInfinityBound() {
        val result = validateSolverFlt64Bound(
            value = Flt64.infinity,
            policy = SolveValueConversionPolicy.Strict,
            fieldName = "demo-bound"
        )

        assertTrue(result is Ok)
    }
}
