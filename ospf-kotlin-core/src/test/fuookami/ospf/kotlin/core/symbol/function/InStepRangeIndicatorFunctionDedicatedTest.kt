package fuookami.ospf.kotlin.core.symbol.function

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok

/** Dedicated focused tests for the InStepRangeIndicatorFunction symbol. */
class InStepRangeIndicatorFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(InStepRangeIndicatorFunction::class.java))
    }

    @Test
    fun evaluatesGridMembershipBoundariesAndMissingInput() {
        val inputVariable = RealVar("step_indicator_input")
        val input = LinearPolynomial(
            listOf(LinearMonomial(Flt64.one, inputVariable)),
            Flt64.zero
        )
        val function = InStepRangeIndicatorFunction(
            input = input,
            lower = Flt64.one,
            upper = Flt64(7.0),
            step = Flt64(3.0),
            bigM = Flt64(100.0),
            converter = IntoValue.Identity,
            name = "step_indicator_eval"
        )

        assertNull(function.evaluate(emptyMap()))
        for (point in listOf(Flt64.one, Flt64(4.0), Flt64(7.0))) {
            assertEquals(
                Flt64.one,
                function.evaluate(mapOf<Symbol, Flt64>(inputVariable to point)),
                "grid point $point should be selected"
            )
        }
        for (offPoint in listOf(Flt64(2.0), Flt64(5.0), Flt64(8.0))) {
            assertEquals(
                Flt64.zero,
                function.evaluate(mapOf<Symbol, Flt64>(inputVariable to offPoint)),
                "off-grid value $offPoint should not be selected"
            )
        }
    }

    @Test
    fun registersPointBandsAndComplementRowsWithExplicitBigM() {
        val zero = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)
        val function = InStepRangeIndicatorFunction(
            input = LinearPolynomial(emptyList(), Flt64(4.0)),
            lower = Flt64.one,
            upper = Flt64(7.0),
            step = Flt64(3.0),
            bigM = Flt64(100.0),
            converter = IntoValue.Identity,
            name = "step_indicator_rows"
        )
        assertEquals(7, function.helperVariables.size)

        val metaModel = LinearMetaModel<Flt64>(
            name = "step-indicator-constraints",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(metaModel.minimize(zero) is Ok)
            assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            assertEquals(7, metaModel.tokens.tokens.size)
            val mechanism = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanism is Ok)

            val before = mechanism.value.constraints.size
            assertTrue(function.registerConstraints(mechanism.value) is Ok)
            val appended = mechanism.value.constraints.subList(before, mechanism.value.constraints.size)
            assertEquals(16, appended.size, "five rows per point plus the OR upper row")
            val names = appended.map { it.name }
            assertTrue("step_indicator_rows_pt0_band_ub" in names)
            assertTrue("step_indicator_rows_pt0_band_lb" in names)
            assertTrue("step_indicator_rows_pt2_out_lb" in names)
            assertTrue("step_indicator_rows_pt2_out_ub" in names)
            assertTrue("step_indicator_rows_or_lb_0" in names)
            assertTrue("step_indicator_rows_or_ub" in names)
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun rejectsNonPositiveStepAndBigM() {
        val input = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)
        assertFailsWith<IllegalArgumentException> {
            InStepRangeIndicatorFunction(
                input = input,
                lower = Flt64.zero,
                upper = Flt64.one,
                step = Flt64.zero,
                converter = IntoValue.Identity,
                name = "invalid_indicator_step"
            )
        }
        assertFailsWith<IllegalArgumentException> {
            InStepRangeIndicatorFunction(
                input = input,
                lower = Flt64.zero,
                upper = Flt64.one,
                step = Flt64.one,
                bigM = Flt64.zero,
                converter = IntoValue.Identity,
                name = "invalid_indicator_big_m"
            )
        }
    }
}
