package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.LinearMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.LinearMetaModel
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

            @Suppress("DEPRECATION")
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok, "${numberCase.name}: dump mechanism model should succeed")
            val mechanismModel = mechanismResult.value

            val before = mechanismModel.constraints.size
            assertTrue(sameAs.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: SameAsFunction registerConstraints should succeed")
            assertTrue(satisfiedAmount.registerConstraints(mechanismModel) is Ok, "${numberCase.name}: SatisfiedAmountFunction registerConstraints should succeed")
            val after = mechanismModel.constraints.size
            assertTrue(after > before, "${numberCase.name}: same/satisfied-amount constraints should be appended")

            val newConstraint = mechanismModel.constraints.last() as LinearConstraintImpl<V>
            val firstCell = newConstraint.lhs.first()
            val coefficient = firstCell.coefficient
            assertTrue(
                coefficient::class == numberCase.one::class,
                "${numberCase.name}: same/satisfied-amount coefficient type should stay V instead of leaking Flt64"
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
}
