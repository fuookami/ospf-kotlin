package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintInput
import fuookami.ospf.kotlin.core.testing.*
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol

class SatisfiedAmountFunctionsGenericEvaluateTest {
    @Test
    fun satisfiedAmountFunctionEvaluateShouldWorkForFourNumberTypes() {
        runSatisfiedAmountFunctionCase(GenericNumberCases.flt64)
        runSatisfiedAmountFunctionCase(GenericNumberCases.fltX)
        runSatisfiedAmountFunctionCase(GenericNumberCases.rtn64)
        runSatisfiedAmountFunctionCase(GenericNumberCases.rtnX)
    }

    @Test
    fun satisfiedAmountInequalityFunctionEvaluateShouldWorkForFourNumberTypes() {
        runSatisfiedAmountInequalityCase(GenericNumberCases.flt64)
        runSatisfiedAmountInequalityCase(GenericNumberCases.fltX)
        runSatisfiedAmountInequalityCase(GenericNumberCases.rtn64)
        runSatisfiedAmountInequalityCase(GenericNumberCases.rtnX)
    }

    private fun <V> runSatisfiedAmountFunctionCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_sat_amt_eval_x")
        val y = RealVar("${numberCase.name.lowercase()}_sat_amt_eval_y")

        val xPoly = LinearPolynomial(listOf(LinearMonomial(numberCase.one, x)), numberCase.zero)
        val yPoly = LinearPolynomial(listOf(LinearMonomial(numberCase.one, y)), numberCase.zero)
        val onePoly = LinearPolynomial<V>(emptyList(), numberCase.one)
        val zeroPoly = LinearPolynomial<V>(emptyList(), numberCase.zero)

        val inequalities = listOf(
            LinearInequality(xPoly, onePoly, Comparison.LE, "${numberCase.name.lowercase()}_x_le_1"),
            LinearInequality(yPoly, zeroPoly, Comparison.GE, "${numberCase.name.lowercase()}_y_ge_0")
        )

        val countFunction = SatisfiedAmountFunction(
            inequalities = inequalities,
            amount = null,
            epsilon = numberCase.converter.intoValue(Flt64(1e-6)),
            bigM = numberCase.ten,
            converter = numberCase.converter,
            name = "sat_amount_count_${numberCase.name.lowercase()}"
        )

        val validValues = mapOf<Symbol, V>(
            x to numberCase.zero,
            y to numberCase.one
        )
        val invalidValues = mapOf<Symbol, V>(
            x to numberCase.five,
            y to -numberCase.one
        )

        val count = countFunction.evaluate(validValues)
        assertNotNull(count, "${numberCase.name}: count evaluate should not be null")
        assertTrue(count eq numberCase.two, "${numberCase.name}: expected satisfied count = 2")

        val amountFunction = SatisfiedAmountFunction(
            inequalities = inequalities,
            amount = UInt64(2),
            epsilon = numberCase.converter.intoValue(Flt64(1e-6)),
            bigM = numberCase.ten,
            converter = numberCase.converter,
            name = "sat_amount_threshold_${numberCase.name.lowercase()}"
        )

        val met = amountFunction.evaluate(validValues)
        assertNotNull(met, "${numberCase.name}: amount-met evaluate should not be null")
        assertTrue(met eq numberCase.one, "${numberCase.name}: threshold-met should be one")

        val notMet = amountFunction.evaluate(invalidValues)
        assertNotNull(notMet, "${numberCase.name}: amount-not-met evaluate should not be null")
        assertTrue(notMet eq numberCase.zero, "${numberCase.name}: threshold-not-met should be zero")
    }

    private fun <V> runSatisfiedAmountInequalityCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_sat_ineq_eval_x")
        val y = RealVar("${numberCase.name.lowercase()}_sat_ineq_eval_y")

        val xPoly = LinearPolynomial(listOf(LinearMonomial(numberCase.one, x)), numberCase.zero)
        val yPoly = LinearPolynomial(listOf(LinearMonomial(numberCase.one, y)), numberCase.zero)
        val onePoly = LinearPolynomial<V>(emptyList(), numberCase.one)
        val zeroPoly = LinearPolynomial<V>(emptyList(), numberCase.zero)

        val lhsRange = ValueRange(
            lb = numberCase.converter.intoValue(Flt64(-1000.0)),
            ub = numberCase.converter.intoValue(Flt64(1000.0)),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = numberCase.zero.constants
        ).value!!

        val inputs = listOf(
            LinearConstraintInput.from(
                relation = LinearInequality(xPoly, onePoly, Comparison.LE, "${numberCase.name.lowercase()}_ineq_x_le_1"),
                lhsRange = lhsRange,
                rhsConstant = numberCase.one
            ).value ?: fail("linear constraint input should be built"),
            LinearConstraintInput.from(
                relation = LinearInequality(yPoly, zeroPoly, Comparison.GE, "${numberCase.name.lowercase()}_ineq_y_ge_0"),
                lhsRange = lhsRange,
                rhsConstant = numberCase.zero
            ).value ?: fail("linear constraint input should be built")
        )

        val countFunction = SatisfiedAmountInequalityFunction.from(
            inputs = inputs,
            amount = null,
            converter = numberCase.converter,
            name = "sat_ineq_count_${numberCase.name.lowercase()}"
        )

        val validValues = mapOf<Symbol, V>(
            x to numberCase.zero,
            y to numberCase.one
        )
        val invalidValues = mapOf<Symbol, V>(
            x to numberCase.five,
            y to -numberCase.one
        )

        val count = countFunction.evaluate(validValues)
        assertNotNull(count, "${numberCase.name}: inequality count evaluate should not be null")
        assertTrue(count eq numberCase.two, "${numberCase.name}: expected inequality satisfied count = 2")

        val amountRange = ValueRange(UInt64.one, UInt64.two).value!!
        val amountFunction = SatisfiedAmountInequalityFunction.from(
            inputs = inputs,
            amount = amountRange,
            converter = numberCase.converter,
            name = "sat_ineq_amount_${numberCase.name.lowercase()}"
        )

        val met = amountFunction.evaluate(validValues)
        assertNotNull(met, "${numberCase.name}: inequality amount-met evaluate should not be null")
        assertTrue(met eq numberCase.one, "${numberCase.name}: inequality amount-met should be one")

        val notMet = amountFunction.evaluate(invalidValues)
        assertNotNull(notMet, "${numberCase.name}: inequality amount-not-met evaluate should not be null")
        assertTrue(notMet eq numberCase.zero, "${numberCase.name}: inequality amount-not-met should be zero")
    }
}
