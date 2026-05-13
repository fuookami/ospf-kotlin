package fuookami.ospf.kotlin.core.solver.value

import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SolveValuePrecisionPolicyTest {
    @Test
    fun strictPolicyShouldAcceptFiniteValuesFromAllConverters() {
        runFinitePolicyCase(GenericNumberCases.flt64)
        runFinitePolicyCase(GenericNumberCases.rtn64)
        runFinitePolicyCase(GenericNumberCases.fltX)
        runFinitePolicyCase(GenericNumberCases.rtnX)
    }

    @Test
    fun strictAndAllowPoliciesShouldHandleSpecialValuesExplicitly() {
        assertFailsWith<IllegalArgumentException> {
            Flt64.nan.toSolverDouble(
                policy = SolveValueConversionPolicy.Strict,
                fieldName = "nan-field"
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Flt64.infinity.toSolverDouble(
                policy = SolveValueConversionPolicy.Strict,
                fieldName = "infinity-field"
            )
        }

        assertTrue(
            Flt64.nan.toSolverDouble(
                policy = SolveValueConversionPolicy.AllowRounding,
                fieldName = "nan-field"
            ).isNaN()
        )
        assertTrue(
            Flt64.infinity.toSolverDouble(
                policy = SolveValueConversionPolicy.AllowRounding,
                fieldName = "infinity-field"
            ).isInfinite()
        )
    }

    private fun <V> runFinitePolicyCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val finiteSamples = listOf(
            Flt64(-3.5),
            Flt64.zero,
            Flt64(4.75),
            Flt64(1024.0)
        )
        for (sample in finiteSamples) {
            val v = numberCase.converter.intoValue(sample)
            val back = numberCase.converter.fromValue(v)
            val strict = back.toSolverDouble(
                policy = SolveValueConversionPolicy.Strict,
                fieldName = "${numberCase.name}-finite"
            )
            assertEquals(sample.toDouble(), strict, "${numberCase.name}: strict policy should keep finite value")
        }
    }
}

