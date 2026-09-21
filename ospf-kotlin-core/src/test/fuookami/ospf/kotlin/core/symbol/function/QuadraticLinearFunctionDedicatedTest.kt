package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertEquals
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
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar

/** [QuadraticLinearFunction] 契约测试。 / Dedicated contract tests. */
class QuadraticLinearFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(QuadraticLinearFunction::class.simpleName == "QuadraticLinearFunction")
    }

    @Test
    fun onlyGenuinelyQuadraticInputsCreateAHelperAndRow() {
        val x = RealVar("qlinear_dedicated_x")
        val y = RealVar("qlinear_dedicated_y")
        val model = QuadraticMetaModel<Flt64>(
            name = "qlinear-dedicated",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(model.add(listOf(x, y)) is Ok)
            val quadraticInput = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(Flt64.one, x, y)),
                constant = Flt64.zero
            )
            assertTrue(model.minimize(quadraticInput) is Ok)

            val linear = QuadraticLinearFunction(
                polynomial = QuadraticPolynomial(
                    monomials = listOf(QuadraticMonomial.linear(Flt64.one, x)),
                    constant = Flt64.zero
                ),
                converter = IntoValue.Identity,
                name = "qlinear_dedicated_linear"
            )
            val quadratic = QuadraticLinearFunction(
                polynomial = quadraticInput,
                converter = IntoValue.Identity,
                name = "qlinear_dedicated_quadratic"
            )
            val beforeTokens = model.tokens.tokens.size
            assertTrue(linear.registerAuxiliaryTokens(model.tokens) is Ok)
            assertEquals(beforeTokens, model.tokens.tokens.size)
            assertTrue(quadratic.registerAuxiliaryTokens(model.tokens) is Ok)
            assertEquals(beforeTokens + 1, model.tokens.tokens.size)

            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value
            val beforeRows = mechanism.constraints.size
            assertTrue(linear.registerConstraints(mechanism) is Ok)
            assertEquals(beforeRows, mechanism.constraints.size)
            assertTrue(quadratic.registerConstraints(mechanism) is Ok)
            assertEquals(beforeRows + 1, mechanism.constraints.size)
        } finally {
            model.close()
        }
    }
}
