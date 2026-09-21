package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar

/** [QuadraticSlackRangeFunction] 的独立契约测试。 / Dedicated contract tests. */
class QuadraticSlackRangeFunctionTest {
    @Test
    fun returnsExactDistanceBelowInsideAndAboveTheInterval() {
        val x = RealVar("quadratic_slack_range_x")
        val input = QuadraticPolynomial(
            listOf(QuadraticMonomial.quadratic(Flt64.one, x, x)),
            Flt64.zero
        )
        val function = QuadraticSlackRangeFunction(
            input = input,
            lower = Flt64.one,
            upper = Flt64(4.0),
            bigM = Flt64(100.0),
            converter = IntoValue.Identity,
            name = "quadratic_slack_range"
        )
        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        tokens.add(listOf(x))

        assertEquals(Flt64.one, function.evaluate(mapOf(x to Flt64.zero), tokens, IntoValue.Identity, false))
        assertEquals(Flt64.zero, function.evaluate(mapOf(x to Flt64.one), tokens, IntoValue.Identity, false))
        assertEquals(Flt64(5.0), function.evaluate(mapOf(x to Flt64(3.0)), tokens, IntoValue.Identity, false))
    }

    @Test
    fun rejectsReversedBounds() {
        assertFailsWith<IllegalArgumentException> {
            QuadraticSlackRangeFunction(
                input = QuadraticPolynomial<Flt64>(emptyList(), Flt64.zero),
                lower = Flt64.two,
                upper = Flt64.one,
                converter = IntoValue.Identity,
                name = "invalid_quadratic_slack_range"
            )
        }
    }

    @Test
    fun quadraticInputRegistersExpectedHelpersAndRows() {
        val x = RealVar("quadratic_slack_range_rows_x")
        val input = QuadraticPolynomial(
            listOf(QuadraticMonomial.quadratic(Flt64.one, x, x)),
            Flt64.zero
        )
        val model = QuadraticMetaModel<Flt64>(
            name = "quadratic-slack-range-dedicated-rows",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(model.add(x) is Ok)
            assertTrue(model.minimize(input) is Ok)
            val function = QuadraticSlackRangeFunction(
                input = input,
                lower = Flt64.one,
                upper = Flt64(4.0),
                bigM = Flt64(100.0),
                converter = IntoValue.Identity,
                name = "quadratic_slack_range_rows"
            )
            val beforeTokens = model.tokens.tokens.size
            assertTrue(function.registerAuxiliaryTokens(model.tokens) is Ok)
            assertEquals(beforeTokens + 6, model.tokens.tokens.size)

            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value
            val beforeRows = mechanism.constraints.size
            assertTrue(function.registerConstraints(mechanism) is Ok)
            assertEquals(beforeRows + 10, mechanism.constraints.size)
        } finally {
            model.close()
        }
    }
}
