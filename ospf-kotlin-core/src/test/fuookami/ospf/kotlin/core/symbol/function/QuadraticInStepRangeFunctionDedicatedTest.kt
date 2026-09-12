package fuookami.ospf.kotlin.core.symbol.function

import kotlinx.coroutines.runBlocking
import kotlin.test.assertFailsWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok

/** Dedicated contract test for the QuadraticInStepRangeFunction symbol. */
class QuadraticInStepRangeFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(QuadraticInStepRangeFunction::class.simpleName == "QuadraticInStepRangeFunction")
    }

    @Test
    fun gateUsesClosedIntervalSemantics() {
        val x = RealVar("qstep_x")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial.linear(Flt64.one, x)),
            constant = Flt64.zero
        )
        val function = QuadraticInStepRangeFunction(
            x = polynomial,
            lower = Flt64.zero,
            upper = Flt64(4.0),
            bigM = Flt64(100.0),
            outsideTolerance = Flt64(0.1),
            converter = IntoValue.Identity,
            name = "qstep_contract"
        )
        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        tokens.add(listOf(x))
        assertEquals(
            Flt64(4.0),
            function.evaluate(mapOf(x to Flt64(4.0)), tokens, IntoValue.Identity, false)
        )
        assertEquals(
            Flt64.zero,
            function.evaluate(mapOf(x to Flt64(4.1)), tokens, IntoValue.Identity, false)
        )
        assertNull(
            function.evaluate(mapOf(x to Flt64(4.05)), tokens, IntoValue.Identity, false)
        )
    }

    @Test
    fun quadraticInputRegistersFourHelpersAndNineRows() {
        val x = RealVar("qstep_rows_x")
        val input = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial.quadratic(Flt64.one, x, x)),
            constant = Flt64.zero
        )
        val model = QuadraticMetaModel<Flt64>(
            name = "qstep-dedicated-rows",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(model.add(x) is Ok)
            assertTrue(model.minimize(input) is Ok)
            val function = QuadraticInStepRangeFunction(
                x = input,
                lower = Flt64.zero,
                upper = Flt64(4.0),
                bigM = Flt64(100.0),
                converter = IntoValue.Identity,
                name = "qstep_dedicated_rows"
            )
            val beforeTokens = model.tokens.tokens.size
            assertTrue(function.registerAuxiliaryTokens(model.tokens) is Ok)
            assertEquals(beforeTokens + 4, model.tokens.tokens.size)

            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value
            val beforeRows = mechanism.constraints.size
            assertTrue(function.registerConstraints(mechanism) is Ok)
            assertEquals(beforeRows + 9, mechanism.constraints.size)
        } finally {
            model.close()
        }
    }

    @Test
    fun defaultToleranceAndReversedBoundsArePartOfTheDedicatedContract() {
        val x = RealVar("qstep_default_x")
        val input = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial.linear(Flt64.one, x)),
            constant = Flt64.zero
        )
        val function = QuadraticInStepRangeFunction(
            x = input,
            lower = Flt64.zero,
            upper = Flt64(4.0),
            converter = IntoValue.Identity,
            name = "qstep_default_tolerance"
        )
        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        try {
            tokens.add(listOf(x))
            assertNull(
                function.evaluate(
                    mapOf(x to Flt64(4.0 + 0.5e-6)),
                    tokens,
                    IntoValue.Identity,
                    false
                )
            )
            assertEquals(
                Flt64.zero,
                function.evaluate(
                    mapOf(x to Flt64(4.0 + 1e-6)),
                    tokens,
                    IntoValue.Identity,
                    false
                )
            )
        } finally {
            tokens.close()
        }

        assertFailsWith<IllegalArgumentException> {
            QuadraticInStepRangeFunction(
                x = input,
                lower = Flt64(4.0),
                upper = Flt64.one,
                converter = IntoValue.Identity,
                name = "qstep_invalid_bounds"
            )
        }
    }
}
