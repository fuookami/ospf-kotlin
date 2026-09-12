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

/** 二次绝对松弛函数的独立契约测试。 / Dedicated quadratic absolute-slack contract tests. */
class QuadraticSlackFunctionTest {
    @Test
    fun returnsExactAbsoluteDifferenceOnBothSides() {
        val x = RealVar("quadratic_slack_x")
        val left = QuadraticPolynomial(
            listOf(QuadraticMonomial.quadratic(Flt64.one, x, x)),
            Flt64.zero
        )
        val right = QuadraticPolynomial<Flt64>(emptyList(), Flt64.one)
        val function = QuadraticSlackFunction(
            left = left,
            right = right,
            bigM = Flt64(100.0),
            converter = IntoValue.Identity,
            name = "quadratic_slack"
        )
        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        tokens.add(listOf(x))

        assertEquals(
            Flt64.one,
            function.evaluate(mapOf(x to Flt64.zero), tokens, IntoValue.Identity, false)
        )
        assertEquals(
            Flt64(3.0),
            function.evaluate(mapOf(x to Flt64.two), tokens, IntoValue.Identity, false)
        )
    }

    @Test
    fun quadraticInputRegistersExpectedHelpersAndRows() {
        val x = RealVar("quadratic_slack_rows_x")
        val left = QuadraticPolynomial(
            listOf(QuadraticMonomial.quadratic(Flt64.one, x, x)),
            Flt64.zero
        )
        val right = QuadraticPolynomial<Flt64>(emptyList(), Flt64.one)
        val model = QuadraticMetaModel<Flt64>(
            name = "quadratic-slack-dedicated-rows",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(model.add(x) is Ok)
            assertTrue(model.minimize(left) is Ok)
            val function = QuadraticSlackFunction(
                left = left,
                right = right,
                bigM = Flt64(100.0),
                converter = IntoValue.Identity,
                name = "quadratic_slack_rows"
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

            val invalid = QuadraticSlackFunction(
                left = left,
                right = right,
                bigM = Flt64.zero,
                converter = IntoValue.Identity,
                name = "quadratic_slack_invalid_m"
            )
            val invalidRows = mechanism.constraints.size
            assertTrue(invalid.registerConstraints(mechanism) is Failed)
            assertEquals(invalidRows, mechanism.constraints.size)
        } finally {
            model.close()
        }
    }
}
