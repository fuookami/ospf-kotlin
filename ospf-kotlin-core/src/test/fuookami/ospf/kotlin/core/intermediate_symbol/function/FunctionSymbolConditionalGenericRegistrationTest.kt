package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.LinearMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.LinearMetaModel
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl
import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class FunctionSymbolConditionalGenericRegistrationTest {
    @Test
    fun ifInAndIfThenAndImplyAndOneOfShouldRegisterConstraintsForFourNumberTypes() {
        runConditionalCase(GenericNumberCases.flt64)
        runConditionalCase(GenericNumberCases.fltX)
        runConditionalCase(GenericNumberCases.rtn64)
        runConditionalCase(GenericNumberCases.rtnX)
    }

    private fun <V> runConditionalCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_cond_x")
        val y = RealVar("${numberCase.name.lowercase()}_cond_y")

        val model = LinearMetaModel<V>(
            name = "generic-conditional-${numberCase.name.lowercase()}",
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
            val twoPoly = LinearPolynomial<V>(emptyList(), numberCase.two)

            val ifIn = IfInFunction(
                x = xPoly,
                lower = numberCase.zero,
                upper = numberCase.two,
                converter = numberCase.converter,
                bigM = numberCase.ten,
                tolerance = numberCase.one,
                strictBoundary = numberCase.one,
                name = "ifin_${numberCase.name.lowercase()}"
            )
            val ifThen = IfThenFunction(
                condition = xPoly,
                thenPoly = yPoly,
                converter = numberCase.converter,
                bigM = numberCase.ten,
                tolerance = numberCase.one,
                strictBoundary = numberCase.one,
                name = "ifthen_${numberCase.name.lowercase()}"
            )
            val imply = ImplyFunction(
                antecedent = xPoly,
                consequent = yPoly,
                converter = numberCase.converter,
                bigM = numberCase.ten,
                tolerance = numberCase.one,
                strictBoundary = numberCase.one,
                name = "imply_${numberCase.name.lowercase()}"
            )
            val oneOf = OneOfFunction(
                polynomials = listOf(xPoly, yPoly, twoPoly),
                converter = numberCase.converter,
                bigM = numberCase.ten,
                tolerance = numberCase.one,
                strictBoundary = numberCase.one,
                name = "oneof_${numberCase.name.lowercase()}"
            )

            assertTrue(ifIn.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: ifIn auxiliary tokens should succeed")
            assertTrue(ifThen.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: ifThen auxiliary tokens should succeed")
            assertTrue(imply.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: imply auxiliary tokens should succeed")
            assertTrue(oneOf.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: oneOf auxiliary tokens should succeed")

            assertTrue(model.minimize(xPoly) is Ok, "${numberCase.name}: add objective should succeed")

            @Suppress("DEPRECATION")
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: dump mechanism model should succeed")
            val mechanismModel = mechanismResult.value

            assertConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "ifIn",
                register = { ifIn.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name)
            )
            assertConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "ifThen",
                register = { ifThen.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name, y.name)
            )
            assertConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "imply",
                register = { imply.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name, y.name)
            )
            assertConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "oneOf",
                register = { oneOf.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name, y.name)
            )
        } finally {
            model.close()
        }
    }

    private fun <V> assertConstraintRegistration(
        numberCase: GenericNumberCase<V>,
        mechanismModel: LinearMechanismModel<V>,
        label: String,
        register: () -> Try,
        expectedInputTokenNames: Set<String>
    ) where V : RealNumber<V>, V : NumberField<V> {
        val before = mechanismModel.constraints.size
        assertTrue(register() is Ok, "${numberCase.name}: $label registerConstraints should succeed")
        val after = mechanismModel.constraints.size
        assertTrue(after > before, "${numberCase.name}: $label should append constraints")

        @Suppress("UNCHECKED_CAST")
        val appended = mechanismModel.constraints.subList(before, after).map { it as LinearConstraintImpl<V> }
        val coefficients = appended.flatMap { constraint -> constraint.lhs.map { cell -> cell.coefficient } }
        assertTrue(coefficients.isNotEmpty(), "${numberCase.name}: $label appended constraints should contain coefficients")
        assertTrue(
            coefficients.all { it::class == numberCase.one::class },
            "${numberCase.name}: $label coefficient type should stay V instead of leaking Flt64"
        )
        assertTrue(
            appended.all { it.rhs::class == numberCase.one::class },
            "${numberCase.name}: $label rhs type should stay V"
        )
        assertTrue(
            appended.any { constraint ->
                constraint.lhs.any { cell -> cell.token.variable.name in expectedInputTokenNames }
            },
            "${numberCase.name}: $label should reference input token(s) ${expectedInputTokenNames.joinToString()}"
        )
        assertTrue(
            appended.any {
                it.sign == ConstraintRelation.LessEqual ||
                    it.sign == ConstraintRelation.GreaterEqual ||
                    it.sign == ConstraintRelation.Equal
            },
            "${numberCase.name}: $label should produce comparable sign"
        )
    }
}
