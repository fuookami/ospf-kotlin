package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.testing.*
import fuookami.ospf.kotlin.core.variable.*

class QuadraticFunctionGenericEvaluationTest {
    @Test
    fun quadraticLinearEvaluateAndPrepareShouldWorkForFourNumberTypes() {
        runQuadraticLinearCase(GenericNumberCases.flt64)
        runQuadraticLinearCase(GenericNumberCases.fltX)
        runQuadraticLinearCase(GenericNumberCases.rtn64)
        runQuadraticLinearCase(GenericNumberCases.rtnX)
    }

    @Test
    fun quadraticMinEvaluateAndPrepareShouldWorkForFourNumberTypes() {
        runQuadraticMinCase(GenericNumberCases.flt64)
        runQuadraticMinCase(GenericNumberCases.fltX)
        runQuadraticMinCase(GenericNumberCases.rtn64)
        runQuadraticMinCase(GenericNumberCases.rtnX)
    }

    @Test
    fun quadraticMaskingRangeEvaluateAndPrepareShouldWorkForFourNumberTypes() {
        runQuadraticMaskingRangeCase(GenericNumberCases.flt64)
        runQuadraticMaskingRangeCase(GenericNumberCases.fltX)
        runQuadraticMaskingRangeCase(GenericNumberCases.rtn64)
        runQuadraticMaskingRangeCase(GenericNumberCases.rtnX)
    }

    @Test
    fun quadraticInStepRangeEvaluateAndPrepareShouldWorkForFourNumberTypes() {
        runQuadraticInStepRangeCase(GenericNumberCases.flt64)
        runQuadraticInStepRangeCase(GenericNumberCases.fltX)
        runQuadraticInStepRangeCase(GenericNumberCases.rtn64)
        runQuadraticInStepRangeCase(GenericNumberCases.rtnX)
    }

    private fun <V> runQuadraticLinearCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_ql_eval_x")
        val y = RealVar("${numberCase.name.lowercase()}_ql_eval_y")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial.quadratic(numberCase.one, x, y),
                QuadraticMonomial.linear(numberCase.one, x)
            ),
            constant = numberCase.one
        )
        val function = QuadraticLinearFunction(
            polynomial = polynomial,
            converter = numberCase.converter,
            name = "quadratic_linear_eval_${numberCase.name.lowercase()}"
        )
        val tokenTable = AutoTokenTable<V>(Quadratic, false)
        tokenTable.add(listOf(x, y))
        val values = mapOf<Symbol, V>(
            x to numberCase.two,
            y to numberCase.five
        )
        val results = listOf(numberCase.two, numberCase.five)
        val expected = Flt64(13.0)

        val prepared = function.prepare(values, tokenTable, numberCase.converter)
        assertNotNull(prepared, "${numberCase.name}: quadratic linear prepare should not be null")
        assertEquals(expected, numberCase.converter.fromValue(prepared))

        val evalByValues = function.evaluate(values, tokenTable, numberCase.converter, zeroIfNone = false)
        assertNotNull(evalByValues, "${numberCase.name}: quadratic linear evaluate(values) should not be null")
        assertEquals(expected, numberCase.converter.fromValue(evalByValues))

        val evalByResults = function.evaluate(results, tokenTable, numberCase.converter, zeroIfNone = false)
        assertNotNull(evalByResults, "${numberCase.name}: quadratic linear evaluate(results) should not be null")
        assertEquals(expected, numberCase.converter.fromValue(evalByResults))
    }

    private fun <V> runQuadraticMinCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_qmin_eval_x")
        val y = RealVar("${numberCase.name.lowercase()}_qmin_eval_y")
        val p1 = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial.quadratic(numberCase.one, x, y)
            ),
            constant = numberCase.one
        )
        val p2 = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial.linear(numberCase.one, x)
            ),
            constant = numberCase.two
        )
        val function = QuadraticMinFunction(
            polynomials = listOf(p1, p2),
            exact = true,
            bigM = numberCase.ten,
            converter = numberCase.converter,
            name = "quadratic_min_eval_${numberCase.name.lowercase()}"
        )
        val tokenTable = AutoTokenTable<V>(Quadratic, false)
        tokenTable.add(listOf(x, y))
        val values = mapOf<Symbol, V>(
            x to numberCase.two,
            y to numberCase.five
        )
        val results = listOf(numberCase.two, numberCase.five)
        val expected = Flt64(4.0)

        val prepared = function.prepare(values, tokenTable, numberCase.converter)
        assertNotNull(prepared, "${numberCase.name}: quadratic min prepare should not be null")
        assertEquals(expected, numberCase.converter.fromValue(prepared))

        val evalByValues = function.evaluate(values, tokenTable, numberCase.converter, zeroIfNone = false)
        assertNotNull(evalByValues, "${numberCase.name}: quadratic min evaluate(values) should not be null")
        assertEquals(expected, numberCase.converter.fromValue(evalByValues))

        val evalByResults = function.evaluate(results, tokenTable, numberCase.converter, zeroIfNone = false)
        assertNotNull(evalByResults, "${numberCase.name}: quadratic min evaluate(results) should not be null")
        assertEquals(expected, numberCase.converter.fromValue(evalByResults))
    }

    private fun <V> runQuadraticMaskingRangeCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_qmask_eval_x")
        val y = RealVar("${numberCase.name.lowercase()}_qmask_eval_y")
        val z = BinVar("${numberCase.name.lowercase()}_qmask_eval_z")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial.quadratic(numberCase.one, x, y),
                QuadraticMonomial.linear(numberCase.one, x)
            ),
            constant = numberCase.one
        )
        val function = QuadraticMaskingRangeFunction(
            polynomial = polynomial,
            z = z,
            bigM = numberCase.ten,
            converter = numberCase.converter,
            name = "quadratic_masking_range_eval_${numberCase.name.lowercase()}"
        )
        val tokenTable = AutoTokenTable<V>(Quadratic, false)
        tokenTable.add(listOf(x, y, z))

        val valuesOn = mapOf<Symbol, V>(
            x to numberCase.two,
            y to numberCase.five,
            z to numberCase.one
        )
        val resultsOn = listOf(numberCase.two, numberCase.five, numberCase.one)
        val expectedOn = Flt64(13.0)

        val preparedOn = function.prepare(valuesOn, tokenTable, numberCase.converter)
        assertNotNull(preparedOn, "${numberCase.name}: qmask prepare(on) should not be null")
        assertEquals(expectedOn, numberCase.converter.fromValue(preparedOn))

        val evalByValuesOn = function.evaluate(valuesOn, tokenTable, numberCase.converter, zeroIfNone = false)
        assertNotNull(evalByValuesOn, "${numberCase.name}: qmask evaluate(values,on) should not be null")
        assertEquals(expectedOn, numberCase.converter.fromValue(evalByValuesOn))

        val evalByResultsOn = function.evaluate(resultsOn, tokenTable, numberCase.converter, zeroIfNone = false)
        assertNotNull(evalByResultsOn, "${numberCase.name}: qmask evaluate(results,on) should not be null")
        assertEquals(expectedOn, numberCase.converter.fromValue(evalByResultsOn))

        val valuesOff = valuesOn + (z to numberCase.zero)
        val resultsOff = listOf(numberCase.two, numberCase.five, numberCase.zero)
        val expectedOff = Flt64.zero

        val preparedOff = function.prepare(valuesOff, tokenTable, numberCase.converter)
        assertNotNull(preparedOff, "${numberCase.name}: qmask prepare(off) should not be null")
        assertEquals(expectedOff, numberCase.converter.fromValue(preparedOff))

        val evalByValuesOff = function.evaluate(valuesOff, tokenTable, numberCase.converter, zeroIfNone = false)
        assertNotNull(evalByValuesOff, "${numberCase.name}: qmask evaluate(values,off) should not be null")
        assertEquals(expectedOff, numberCase.converter.fromValue(evalByValuesOff))

        val evalByResultsOff = function.evaluate(resultsOff, tokenTable, numberCase.converter, zeroIfNone = false)
        assertNotNull(evalByResultsOff, "${numberCase.name}: qmask evaluate(results,off) should not be null")
        assertEquals(expectedOff, numberCase.converter.fromValue(evalByResultsOff))
    }

    private fun <V> runQuadraticInStepRangeCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_qstep_eval_x")
        val y = RealVar("${numberCase.name.lowercase()}_qstep_eval_y")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial.quadratic(numberCase.one, x, y),
                QuadraticMonomial.linear(numberCase.one, x)
            ),
            constant = numberCase.one
        )
        val function = QuadraticInStepRangeFunction(
            x = polynomial,
            lower = numberCase.zero,
            upper = numberCase.ten,
            bigM = numberCase.ten,
            converter = numberCase.converter,
            name = "quadratic_step_range_eval_${numberCase.name.lowercase()}"
        )
        val tokenTable = AutoTokenTable<V>(Quadratic, false)
        tokenTable.add(listOf(x, y))

        val valuesIn = mapOf<Symbol, V>(
            x to numberCase.one,
            y to numberCase.two
        )
        val resultsIn = listOf(numberCase.one, numberCase.two)
        val expectedIn = Flt64(4.0)

        val preparedIn = function.prepare(valuesIn, tokenTable, numberCase.converter)
        assertNotNull(preparedIn, "${numberCase.name}: qstep prepare(in-range) should not be null")
        assertEquals(expectedIn, numberCase.converter.fromValue(preparedIn))

        val evalByValuesIn = function.evaluate(valuesIn, tokenTable, numberCase.converter, zeroIfNone = false)
        assertNotNull(evalByValuesIn, "${numberCase.name}: qstep evaluate(values,in-range) should not be null")
        assertEquals(expectedIn, numberCase.converter.fromValue(evalByValuesIn))

        val evalByResultsIn = function.evaluate(resultsIn, tokenTable, numberCase.converter, zeroIfNone = false)
        assertNotNull(evalByResultsIn, "${numberCase.name}: qstep evaluate(results,in-range) should not be null")
        assertEquals(expectedIn, numberCase.converter.fromValue(evalByResultsIn))

        val valuesOut = mapOf<Symbol, V>(
            x to numberCase.two,
            y to numberCase.ten
        )
        val resultsOut = listOf(numberCase.two, numberCase.ten)
        val expectedOut = Flt64.zero

        val preparedOut = function.prepare(valuesOut, tokenTable, numberCase.converter)
        assertNotNull(preparedOut, "${numberCase.name}: qstep prepare(out-of-range) should not be null")
        assertEquals(expectedOut, numberCase.converter.fromValue(preparedOut))

        val evalByValuesOut = function.evaluate(valuesOut, tokenTable, numberCase.converter, zeroIfNone = false)
        assertNotNull(evalByValuesOut, "${numberCase.name}: qstep evaluate(values,out-of-range) should not be null")
        assertEquals(expectedOut, numberCase.converter.fromValue(evalByValuesOut))

        val evalByResultsOut = function.evaluate(resultsOut, tokenTable, numberCase.converter, zeroIfNone = false)
        assertNotNull(evalByResultsOut, "${numberCase.name}: qstep evaluate(results,out-of-range) should not be null")
        assertEquals(expectedOut, numberCase.converter.fromValue(evalByResultsOut))
    }
}
