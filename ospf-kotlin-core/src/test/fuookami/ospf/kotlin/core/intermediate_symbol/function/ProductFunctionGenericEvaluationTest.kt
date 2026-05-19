package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductFunctionGenericEvaluationTest {
    @Test
    fun evaluateAndPrepareShouldWorkForFourNumberTypes() {
        runCase(GenericNumberCases.flt64)
        runCase(GenericNumberCases.fltX)
        runCase(GenericNumberCases.rtn64)
        runCase(GenericNumberCases.rtnX)
    }

    private fun <V> runCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_prod_eval_x")
        val y = RealVar("${numberCase.name.lowercase()}_prod_eval_y")
        val left = LinearPolynomial(
            monomials = listOf(LinearMonomial(numberCase.one, x)),
            constant = numberCase.two
        )
        val right = LinearPolynomial(
            monomials = listOf(LinearMonomial(numberCase.one, y)),
            constant = -numberCase.one
        )
        val product = ProductFunction(
            left = left,
            right = right,
            converter = numberCase.converter,
            name = "product_eval_${numberCase.name.lowercase()}"
        )

        val tokenTable = AutoTokenTable<V>(Quadratic, false)
        tokenTable.add(listOf(x, y))
        val values = mapOf<Symbol, V>(
            x to numberCase.two,
            y to numberCase.five
        )
        val results = listOf(numberCase.two, numberCase.five)
        val expected = Flt64(16.0)

        val prepared = product.prepare(values, tokenTable, numberCase.converter)
        assertEquals(
            expected,
            numberCase.converter.fromValue(prepared!!),
            "${numberCase.name}: prepare(values) should match expected product"
        )

        val evalByValues = product.evaluate(values, tokenTable, numberCase.converter, zeroIfNone = false)
        assertEquals(
            expected,
            numberCase.converter.fromValue(evalByValues!!),
            "${numberCase.name}: evaluate(values) should match expected product"
        )

        val evalByResults = product.evaluate(results, tokenTable, numberCase.converter, zeroIfNone = false)
        assertEquals(
            expected,
            numberCase.converter.fromValue(evalByResults!!),
            "${numberCase.name}: evaluate(results) should match expected product"
        )
    }
}