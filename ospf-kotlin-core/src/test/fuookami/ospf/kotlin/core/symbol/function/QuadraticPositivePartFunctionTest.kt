package fuookami.ospf.kotlin.core.symbol.function

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
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
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok

/** 二次正部函数的独立契约测试。 / Dedicated contract tests for the quadratic positive-part function. */
class QuadraticPositivePartFunctionTest {
    @Test
    fun clampsNegativeValuesAndKeepsPositiveValues() {
        val x = RealVar("positive_part_x")
        val input = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial.linear(Flt64.one, x)),
            constant = Flt64.zero
        )
        val function = QuadraticPositivePartFunction(
            input = input,
            bigM = Flt64(100.0),
            converter = IntoValue.Identity,
            name = "positive_part"
        )
        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        tokens.add(listOf(x))

        assertEquals(
            Flt64.zero,
            function.evaluate(mapOf(x to Flt64(-2.0)), tokens, IntoValue.Identity, false)
        )
        assertEquals(
            Flt64(3.0),
            function.evaluate(mapOf(x to Flt64(3.0)), tokens, IntoValue.Identity, false)
        )
        assertEquals(
            Flt64.zero,
            function.evaluate(mapOf(x to Flt64.zero), tokens, IntoValue.Identity, false)
        )
    }

    @Test
    fun quadraticInputRegistersExpectedHelpersAndRows() {
        val x = RealVar("positive_part_rows_x")
        val y = RealVar("positive_part_rows_y")
        val input = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial.quadratic(Flt64.one, x, y)),
            constant = Flt64.zero
        )
        val model = QuadraticMetaModel<Flt64>(
            name = "positive-part-dedicated-rows",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(model.add(listOf(x, y)) is Ok)
            assertTrue(model.minimize(input) is Ok)
            val function = QuadraticPositivePartFunction(
                input = input,
                bigM = Flt64(100.0),
                converter = IntoValue.Identity,
                name = "positive_part_rows"
            )
            val beforeTokens = model.tokens.tokens.size
            assertTrue(function.registerAuxiliaryTokens(model.tokens) is Ok)
            assertEquals(beforeTokens + 3, model.tokens.tokens.size)

            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value
            val beforeRows = mechanism.constraints.size
            assertTrue(function.registerConstraints(mechanism) is Ok)
            assertEquals(beforeRows + 5, mechanism.constraints.size)

            val invalid = QuadraticPositivePartFunction(
                input = input,
                bigM = Flt64.zero,
                converter = IntoValue.Identity,
                name = "positive_part_invalid_m"
            )
            val invalidRows = mechanism.constraints.size
            assertTrue(invalid.registerConstraints(mechanism) is Failed)
            assertEquals(invalidRows, mechanism.constraints.size)
        } finally {
            model.close()
        }
    }

    @Test
    fun signKnownInputAvoidsSelectorVariables() {
        val x = RealVar("positive_part_sign_known_x")
        x.range.geq(Flt64.one)
        x.range.leq(Flt64(5.0))
        val input = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial.linear(Flt64.one, x)),
            constant = Flt64.zero
        )
        val model = QuadraticMetaModel<Flt64>(
            name = "positive-part-sign-known",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(model.add(x) is Ok)
            assertTrue(model.minimize(input) is Ok)
            val function = QuadraticPositivePartFunction(
                input = input,
                converter = IntoValue.Identity,
                name = "positive_part_sign_known"
            )

            val beforeTokens = model.tokens.tokens.size
            assertTrue(function.registerAuxiliaryTokens(model.tokens) is Ok)
            assertEquals(beforeTokens + 1, model.tokens.tokens.size)

            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value
            val beforeRows = mechanism.constraints.size
            assertTrue(function.registerConstraints(mechanism) is Ok)
            assertEquals(beforeRows + 1, mechanism.constraints.size)
        } finally {
            model.close()
        }
    }
}
