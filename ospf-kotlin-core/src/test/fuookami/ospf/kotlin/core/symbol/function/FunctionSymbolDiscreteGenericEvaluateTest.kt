package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.testing.*
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import kotlin.test.*

class FunctionSymbolDiscreteGenericEvaluateTest {
    @Test
    fun balanceInStepAndModEvaluateShouldWorkForFourNumberTypes() {
        runBalanceInStepModCase(GenericNumberCases.flt64)
        runBalanceInStepModCase(GenericNumberCases.fltX)
        runBalanceInStepModCase(GenericNumberCases.rtn64)
        runBalanceInStepModCase(GenericNumberCases.rtnX)
    }

    @Test
    fun floorCeilingAndRoundingEvaluateShouldWorkForFourNumberTypes() {
        runRoundingFamilyCase(GenericNumberCases.flt64)
        runRoundingFamilyCase(GenericNumberCases.fltX)
        runRoundingFamilyCase(GenericNumberCases.rtn64)
        runRoundingFamilyCase(GenericNumberCases.rtnX)
    }

    private fun <V> runBalanceInStepModCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_balance_eval_x")
        val lb = RealVar("${numberCase.name.lowercase()}_step_eval_lb")
        val ub = RealVar("${numberCase.name.lowercase()}_step_eval_ub")
        val modX = RealVar("${numberCase.name.lowercase()}_mod_eval_x")

        val xPoly = LinearPolynomial(listOf(LinearMonomial(numberCase.one, x)), numberCase.zero)
        val lbPoly = LinearPolynomial(listOf(LinearMonomial(numberCase.one, lb)), numberCase.zero)
        val ubPoly = LinearPolynomial(listOf(LinearMonomial(numberCase.one, ub)), numberCase.zero)
        val modPoly = LinearPolynomial(listOf(LinearMonomial(numberCase.one, modX)), numberCase.zero)

        val balance = BalanceTernaryzationFunction(
            x = xPoly,
            converter = numberCase.converter,
            name = "balance_eval_${numberCase.name.lowercase()}"
        )
        val inStep = InStepRangeFunction(
            lb = lbPoly,
            ub = ubPoly,
            step = numberCase.two,
            converter = numberCase.converter,
            name = "in_step_eval_${numberCase.name.lowercase()}"
        )
        val mod = ModFunction(
            x = modPoly,
            d = numberCase.two,
            converter = numberCase.converter,
            name = "mod_eval_${numberCase.name.lowercase()}"
        )

        val balancePositive = balance.evaluate(mapOf<Symbol, V>(x to numberCase.five))
        assertNotNull(balancePositive, "${numberCase.name}: balance positive should not be null")
        assertTrue(balancePositive eq numberCase.one, "${numberCase.name}: positive should map to one")

        val balanceZero = balance.evaluate(mapOf<Symbol, V>(x to numberCase.zero))
        assertNotNull(balanceZero, "${numberCase.name}: balance zero should not be null")
        assertTrue(balanceZero eq numberCase.zero, "${numberCase.name}: zero should map to zero")

        val balanceNegative = balance.evaluate(mapOf<Symbol, V>(x to -numberCase.five))
        assertNotNull(balanceNegative, "${numberCase.name}: balance negative should not be null")
        assertTrue(balanceNegative eq -numberCase.one, "${numberCase.name}: negative should map to minus one")

        val stepAtUpper = inStep.evaluate(
            mapOf<Symbol, V>(
                lb to numberCase.one,
                ub to numberCase.five
            )
        )
        assertNotNull(stepAtUpper, "${numberCase.name}: in-step at upper should not be null")
        assertTrue(stepAtUpper eq numberCase.five, "${numberCase.name}: expected stepped value 5")

        val stepInside = inStep.evaluate(
            mapOf<Symbol, V>(
                lb to numberCase.one,
                ub to numberCase.two + numberCase.two
            )
        )
        assertNotNull(stepInside, "${numberCase.name}: in-step inside should not be null")
        assertTrue(stepInside eq numberCase.three(), "${numberCase.name}: expected stepped value 3")

        val modValue = mod.evaluate(mapOf<Symbol, V>(modX to numberCase.five))
        assertNotNull(modValue, "${numberCase.name}: mod evaluate should not be null")
        assertTrue(modValue eq numberCase.one, "${numberCase.name}: 5 mod 2 should be 1")
    }

    private fun <V> runRoundingFamilyCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_round_eval_x")
        val xPoly = LinearPolynomial(listOf(LinearMonomial(numberCase.one, x)), numberCase.zero)

        val floorFunction = FloorFunction(
            x = xPoly,
            converter = numberCase.converter,
            name = "floor_eval_${numberCase.name.lowercase()}"
        )
        val ceilingFunction = CeilingFunction(
            x = xPoly,
            converter = numberCase.converter,
            name = "ceiling_eval_${numberCase.name.lowercase()}"
        )
        val roundingFunction = RoundingFunction(
            x = xPoly,
            converter = numberCase.converter,
            name = "round_eval_${numberCase.name.lowercase()}"
        )

        val x12 = numberCase.converter.intoValue(Flt64(1.2))
        val x16 = numberCase.converter.intoValue(Flt64(1.6))

        val floorValue = floorFunction.evaluate(mapOf<Symbol, V>(x to x12))
        assertNotNull(floorValue, "${numberCase.name}: floor evaluate should not be null")
        assertTrue(floorValue eq numberCase.one, "${numberCase.name}: floor(1.2) should be 1")

        val ceilingValue = ceilingFunction.evaluate(mapOf<Symbol, V>(x to x12))
        assertNotNull(ceilingValue, "${numberCase.name}: ceiling evaluate should not be null")
        assertTrue(ceilingValue eq numberCase.two, "${numberCase.name}: ceil(1.2) should be 2")

        val roundLow = roundingFunction.evaluate(mapOf<Symbol, V>(x to x12))
        assertNotNull(roundLow, "${numberCase.name}: round(1.2) should not be null")
        assertTrue(roundLow eq numberCase.one, "${numberCase.name}: round(1.2) should be 1")

        val roundHigh = roundingFunction.evaluate(mapOf<Symbol, V>(x to x16))
        assertNotNull(roundHigh, "${numberCase.name}: round(1.6) should not be null")
        assertTrue(roundHigh eq numberCase.two, "${numberCase.name}: round(1.6) should be 2")
    }

    private fun <V> GenericNumberCase<V>.three(): V
            where V : RealNumber<V>, V : NumberField<V> {
        return two + one
    }
}
