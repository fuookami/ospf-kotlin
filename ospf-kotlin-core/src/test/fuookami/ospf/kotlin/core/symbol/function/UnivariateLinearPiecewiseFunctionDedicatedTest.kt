package fuookami.ospf.kotlin.core.symbol.function

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok

/** Dedicated contract test for the UnivariateLinearPiecewiseFunction symbol. */
class UnivariateLinearPiecewiseFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(UnivariateLinearPiecewiseFunction::class.java))
    }

    @Test
    fun evaluatesSegmentBoundariesAndRegistersEverySelectorGraphRow() {
        fun functionAt(x: Flt64): UnivariateLinearPiecewiseFunction<Flt64> =
            UnivariateLinearPiecewiseFunction(
                x = LinearPolynomial(emptyList(), x),
                breakpoints = listOf(Flt64.zero, Flt64.one, Flt64(2.0)),
                slopes = listOf(Flt64.two, Flt64(-2.0)),
                intercepts = listOf(Flt64.zero, Flt64(4.0)),
                m = Flt64(20.0),
                converter = IntoValue.Identity,
                name = "ulp_boundary_$x"
            )

        assertEquals(Flt64.zero, functionAt(Flt64.zero).evaluate(emptyMap()))
        assertEquals(Flt64.two, functionAt(Flt64.one).evaluate(emptyMap()))
        assertEquals(Flt64.zero, functionAt(Flt64(2.0)).evaluate(emptyMap()))
        assertEquals(null, functionAt(Flt64(-0.1)).evaluate(emptyMap()))

        val zero = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)
        val metaModel = LinearMetaModel<Flt64>(
            name = "ulp-constraints",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(metaModel.minimize(zero) is Ok)
            val function = UnivariateLinearPiecewiseFunction(
                x = zero,
                breakpoints = listOf(Flt64.zero, Flt64.one, Flt64(2.0)),
                slopes = listOf(Flt64.two, Flt64(-2.0)),
                intercepts = listOf(Flt64.zero, Flt64(4.0)),
                m = Flt64(20.0),
                converter = IntoValue.Identity,
                name = "ulp_rows"
            )
            assertEquals(3, function.helperVariables.size)
            assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            assertEquals(3, metaModel.tokens.tokens.size)
            val mechanism = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanism is Ok)

            val before = mechanism.value.constraints.size
            assertTrue(function.registerConstraints(mechanism.value) is Ok)
            val appended = mechanism.value.constraints.subList(before, mechanism.value.constraints.size)
            assertEquals(9, appended.size, "one selector row plus four rows per segment")
            val names = appended.map { it.name }
            assertTrue("ulp_rows_select_one" in names)
            for (index in 0..1) {
                assertTrue("ulp_rows_seg_${index}_lb" in names)
                assertTrue("ulp_rows_seg_${index}_ub" in names)
                assertTrue("ulp_rows_seg_${index}_eq_lb" in names)
                assertTrue("ulp_rows_seg_${index}_eq_ub" in names)
            }
        } finally {
            metaModel.close()
        }
    }
}
