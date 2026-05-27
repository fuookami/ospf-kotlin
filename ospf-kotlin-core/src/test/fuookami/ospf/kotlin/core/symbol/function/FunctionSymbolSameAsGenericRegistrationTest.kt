package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl
import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FunctionSymbolSameAsGenericRegistrationTest {
    @Test
    fun sameAsAndSatisfiedAmountShouldRegisterConstraintsForFourNumberTypes() {
        runCase(GenericNumberCases.flt64)
        runCase(GenericNumberCases.fltX)
        runCase(GenericNumberCases.rtn64)
        runCase(GenericNumberCases.rtnX)
    }

    @Test
    fun sameAsAndSatisfiedAmountEvaluateShouldNotDependOnPolynomialConstants() {
        runEvaluateCase(GenericNumberCases.flt64)
        runEvaluateCase(GenericNumberCases.fltX)
        runEvaluateCase(GenericNumberCases.rtn64)
        runEvaluateCase(GenericNumberCases.rtnX)
    }

    private fun <V> runCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_same_x")
        val y = RealVar("${numberCase.name.lowercase()}_same_y")

        val model = LinearMetaModel<V>(
            name = "generic-same-${numberCase.name.lowercase()}",
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
            val onePoly = LinearPolynomial<V>(emptyList(), numberCase.one)
            val zeroPoly = LinearPolynomial<V>(emptyList(), numberCase.zero)
            val inequalities = listOf(
                LinearInequality(xPoly, onePoly, Comparison.LE, "${numberCase.name.lowercase()}_same_x_le_1"),
                LinearInequality(yPoly, zeroPoly, Comparison.GE, "${numberCase.name.lowercase()}_same_y_ge_0")
            )

            val sameAs = SameAsFunction(
                inequalities = inequalities,
                constraint = false,
                epsilon = numberCase.one,
                m = numberCase.ten,
                converter = numberCase.converter,
                name = "same_${numberCase.name.lowercase()}"
            )
            val satisfiedAmount = SatisfiedAmountFunction(
                inequalities = inequalities,
                amount = UInt64(1),
                epsilon = numberCase.one,
                bigM = numberCase.ten,
                converter = numberCase.converter,
                name = "sat_amount_${numberCase.name.lowercase()}"
            )

            assertTrue(sameAs.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: SameAsFunction auxiliary tokens should succeed")
            assertTrue(satisfiedAmount.registerAuxiliaryTokens(model.tokens) is Ok, "${numberCase.name}: SatisfiedAmountFunction auxiliary tokens should succeed")
            assertTrue(model.minimize(xPoly) is Ok, "${numberCase.name}: add objective should succeed")
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: dump mechanism model should succeed")
            val mechanismModel = mechanismResult.value

            assertConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "sameAs",
                register = { sameAs.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name, y.name)
            )
            assertConstraintRegistration(
                numberCase = numberCase,
                mechanismModel = mechanismModel,
                label = "satisfiedAmount",
                register = { satisfiedAmount.registerConstraints(mechanismModel) },
                expectedInputTokenNames = setOf(x.name, y.name)
            )
        } finally {
            model.close()
        }
    }

    private fun <V> runEvaluateCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_same_eval_x")
        val y = RealVar("${numberCase.name.lowercase()}_same_eval_y")

        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(numberCase.one, x)),
            constant = numberCase.zero
        )
        val yPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(numberCase.one, y)),
            constant = numberCase.zero
        )
        val zeroPoly = LinearPolynomial<V>(emptyList(), numberCase.zero)
        val inequalities = listOf(
            LinearInequality(xPoly, zeroPoly, Comparison.LE, "${numberCase.name.lowercase()}_same_eval_x_le_0"),
            LinearInequality(yPoly, zeroPoly, Comparison.GE, "${numberCase.name.lowercase()}_same_eval_y_ge_0")
        )

        val sameAs = SameAsFunction(
            inequalities = inequalities,
            constraint = false,
            epsilon = numberCase.converter.intoValue(Flt64(1e-6)),
            m = numberCase.ten,
            converter = numberCase.converter,
            name = "same_eval_${numberCase.name.lowercase()}"
        )
        val satisfiedAmount = SatisfiedAmountFunction(
            inequalities = inequalities,
            amount = null,
            epsilon = numberCase.converter.intoValue(Flt64(1e-6)),
            bigM = numberCase.ten,
            converter = numberCase.converter,
            name = "sat_amount_eval_${numberCase.name.lowercase()}"
        )
        val satisfiedAtLeastOne = SatisfiedAmountFunction(
            inequalities = inequalities,
            amount = UInt64.one,
            epsilon = numberCase.converter.intoValue(Flt64(1e-6)),
            bigM = numberCase.ten,
            converter = numberCase.converter,
            name = "sat_amount_eval_threshold_${numberCase.name.lowercase()}"
        )

        val allSatisfied = mapOf<fuookami.ospf.kotlin.math.symbol.Symbol, V>(
            x to numberCase.zero,
            y to numberCase.zero
        )
        assertEquals(
            Flt64.one,
            numberCase.converter.fromValue(sameAs.evaluate(allSatisfied)!!),
            "${numberCase.name}: all satisfied should evaluate to one"
        )
        assertEquals(
            Flt64.two,
            numberCase.converter.fromValue(satisfiedAmount.evaluate(allSatisfied)!!),
            "${numberCase.name}: all satisfied should count to two"
        )
        assertEquals(
            Flt64.one,
            numberCase.converter.fromValue(satisfiedAtLeastOne.evaluate(allSatisfied)!!),
            "${numberCase.name}: threshold mode should evaluate to one when count >= 1"
        )

        val mixed = mapOf<fuookami.ospf.kotlin.math.symbol.Symbol, V>(
            x to numberCase.zero,
            y to -numberCase.one
        )
        assertEquals(
            Flt64.zero,
            numberCase.converter.fromValue(sameAs.evaluate(mixed)!!),
            "${numberCase.name}: mixed satisfaction should evaluate to zero"
        )
        assertEquals(
            Flt64.one,
            numberCase.converter.fromValue(satisfiedAmount.evaluate(mixed)!!),
            "${numberCase.name}: mixed satisfaction should count to one"
        )
        assertEquals(
            Flt64.one,
            numberCase.converter.fromValue(satisfiedAtLeastOne.evaluate(mixed)!!),
            "${numberCase.name}: threshold mode should still be one for count = 1"
        )
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
