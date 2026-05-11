package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.LinearMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl
import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class FunctionSymbolPiecewiseGenericRegistrationTest {
    @Test
    fun slackAndMaxAndMinShouldRegisterConstraintsForFourNumberTypes() {
        runPiecewiseCase(GenericNumberCases.flt64)
        runPiecewiseCase(GenericNumberCases.fltX)
        runPiecewiseCase(GenericNumberCases.rtn64)
        runPiecewiseCase(GenericNumberCases.rtnX)
    }

    private fun <V> runPiecewiseCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_piecewise_x")
        val y = RealVar("${numberCase.name.lowercase()}_piecewise_y")

        val model = LinearMetaModel<V>(
            name = "generic-piecewise-${numberCase.name.lowercase()}",
            converter = numberCase.converter
        )

        try {
            assertTrue(model.add(listOf(x, y)) is Ok, "${numberCase.name}: add variables should succeed")

            val xPoly = LinearPolynomial(
                monomials = listOf(LinearMonomial(numberCase.one, x)),
                constant = numberCase.zero
            )
            val yPoly = LinearPolynomial(
                monomials = listOf(LinearMonomial(numberCase.one, y)),
                constant = numberCase.zero
            )
            val zeroPoly = LinearPolynomial<V>(emptyList(), numberCase.zero)

            val slack = SlackFunction(
                x = xPoly,
                y = zeroPoly,
                converter = numberCase.converter,
                name = "slack_${numberCase.name.lowercase()}"
            )
            val max = MaxFunction(
                polynomials = listOf(xPoly, yPoly),
                converter = numberCase.converter,
                bigM = numberCase.ten,
                name = "max_${numberCase.name.lowercase()}"
            )
            val min = MinFunction(
                polynomials = listOf(xPoly, yPoly),
                converter = numberCase.converter,
                bigM = numberCase.ten,
                name = "min_${numberCase.name.lowercase()}"
            )

            assertTrue(slack.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: slack auxiliary tokens should succeed")
            assertTrue(max.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: max auxiliary tokens should succeed")
            assertTrue(min.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: min auxiliary tokens should succeed")

            assertTrue(model.minimize(xPoly) is Ok, "${numberCase.name}: add objective should succeed")

            @Suppress("DEPRECATION")
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: dump mechanism model should succeed")
            val mechanismModel = mechanismResult.value

            val before = mechanismModel.constraints.size
            assertTrue(slack.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: slack registerConstraints should succeed")
            assertTrue(max.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: max registerConstraints should succeed")
            assertTrue(min.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: min registerConstraints should succeed")
            val after = mechanismModel.constraints.size

            assertTrue(after > before, "${numberCase.name}: piecewise function constraints should be appended")

            val newConstraint = mechanismModel.constraints.last() as LinearConstraintImpl<V>
            val firstCell = newConstraint.lhs.first()
            val coefficient = firstCell.coefficient
            assertTrue(
                coefficient::class == numberCase.one::class,
                "${numberCase.name}: piecewise constraint coefficient type should stay V instead of leaking Flt64"
            )
        } finally {
            model.close()
        }
    }
}
