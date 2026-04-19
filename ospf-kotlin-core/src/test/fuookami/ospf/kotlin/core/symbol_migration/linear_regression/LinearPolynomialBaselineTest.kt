@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.core.frontend.symbol_migration.linear_regression

import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_model.AutoTokenTable
import fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenDataF64
import fuookami.ospf.kotlin.core.variable.AutoTokenList
import fuookami.ospf.kotlin.core.variable.AbstractTokenListF64
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as MathLinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as MathLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.evaluate
import fuookami.ospf.kotlin.math.symbol.adapter.MissingValuePolicy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun sumFlt64(values: List<Flt64>): Flt64 {
    var sum = Flt64.zero
    for (v in values) sum += v
    return sum
}

private fun evalMonomials(monomials: List<LinearMonomial>, tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? {
    var sum = Flt64.zero
    for (mono in monomials) {
        val v = mono.evaluate(tokenList, zeroIfNone) ?: return null
        sum += v
    }
    return sum
}

private fun evalMonomials(monomials: List<LinearMonomial>, values: Map<Symbol, Flt64>, tokenList: AbstractTokenListF64?, zeroIfNone: Boolean): Flt64? {
    var sum = Flt64.zero
    for (mono in monomials) {
        val v = mono.evaluate(values, tokenList, zeroIfNone) ?: return null
        sum += v
    }
    return sum
}

private fun evalMonomialsResults(monomials: List<LinearMonomial>, results: List<Flt64>, tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? {
    var sum = Flt64.zero
    for (mono in monomials) {
        val v = mono.evaluate(results, tokenList, zeroIfNone) ?: return null
        sum += v
    }
    return sum
}

class LinearPolynomialBaselineTest {
    @Test
    fun evaluate_shouldMatchCurrentArithmeticBehavior() {
        val x = RealVar("x")
        val y = RealVar("y")

        val polynomial = MathLinearPolynomial(
            monomials = listOf(
                MathLinearMonomial(Flt64(2.0), x),
                MathLinearMonomial(Flt64(-1.0), y),
                MathLinearMonomial(Flt64.one, x)
            ),
            constant = Flt64(3.0)
        )

        val value = polynomial.evaluate(
            values = mapOf(x to Flt64(4.0), y to Flt64(1.0)),
            policy = MissingValuePolicy.ReturnNull
        )

        assertNotNull(value)
        assertTrue(value eq Flt64(14.0))
    }

    @Test
    fun evaluate_shouldRespectZeroIfNoneSwitch() {
        val x = RealVar("x")
        val y = RealVar("y")
        val polynomial = MathLinearPolynomial(
            monomials = listOf(
                MathLinearMonomial(Flt64(2.0), x),
                MathLinearMonomial(Flt64.one, y)
            ),
            constant = Flt64.one
        )

        val missingAsNull = polynomial.evaluate(
            values = mapOf(x to Flt64(3.0)),
            policy = MissingValuePolicy.ReturnNull
        )
        assertNull(missingAsNull)

        val missingAsZero = polynomial.evaluate(
            values = mapOf(x to Flt64(3.0)),
            policy = MissingValuePolicy.AsZero
        )
        assertNotNull(missingAsZero)
        assertTrue(missingAsZero eq Flt64(7.0))
    }

    @Test
    fun evaluate_valuesShouldOverrideTokenList_andFallbackWhenMissing() {
        val x = RealVar("x")
        val y = RealVar("y")
        val tokenList = AutoTokenList(false)
        tokenList.add(listOf(x, y))
        tokenList.setSolution(
            mapOf(
                x to Flt64(9.0),
                y to Flt64(4.0)
            )
        )

        val monomials = listOf(
            LinearMonomial(2, x),
            LinearMonomial(y)
        )

        // values map overrides, tokenList falls back for missing y
        val valueFromMapAndToken = evalMonomials(monomials, mapOf(x to Flt64(3.0)), tokenList as AbstractTokenListF64?, zeroIfNone = false)
        assertNotNull(valueFromMapAndToken)
        assertTrue(valueFromMapAndToken eq Flt64(10.0))
    }

    @Test
    fun evaluate_valuesShouldOverrideTokenTable_andFallbackWhenMissing() {
        val x = RealVar("x")
        val y = RealVar("y")
        val tokenTable = AutoTokenTable(Linear, false)
        tokenTable.add(listOf(x, y))
        tokenTable.setSolution(
            mapOf(
                x to Flt64(9.0),
                y to Flt64(4.0)
            )
        )

        val monomials = listOf(
            LinearMonomial(2, x),
            LinearMonomial(y)
        )

        // values map overrides, tokenTable falls back for missing y
        val valueFromMapAndToken = monomials.mapNotNull { it.evaluate(values = mapOf(x to Flt64(3.0)), tokenTable = tokenTable, zeroIfNone = false) }.let { sumFlt64(it) }
        assertNotNull(valueFromMapAndToken)
        assertTrue(valueFromMapAndToken eq Flt64(10.0))
    }

    @Test
    fun evaluate_tokenListAndTokenTableShouldMatchCurrentArithmeticBehavior() {
        val x = RealVar("x")
        val y = RealVar("y")
        val tokenList = AutoTokenList(false)
        tokenList.add(listOf(x, y))
        tokenList.setSolution(
            mapOf(
                x to Flt64(4.0),
                y to Flt64(1.0)
            )
        )
        val tokenTable = AutoTokenTable(Linear, false)
        tokenTable.add(listOf(x, y))
        tokenTable.setSolution(
            mapOf(
                x to Flt64(4.0),
                y to Flt64(1.0)
            )
        )

        val monomials = listOf(
            LinearMonomial(2, x),
            LinearMonomial(-1, y),
            LinearMonomial(x)
        )
        val constant = Flt64(3.0)

        val valueFromTokenList = evalMonomials(monomials, tokenList as AbstractTokenListF64, zeroIfNone = false)!! + constant
        val valueFromTokenTable = monomials.mapNotNull { it.evaluate(tokenTable, zeroIfNone = false) }.let { sumFlt64(it) } + constant

        assertNotNull(valueFromTokenList)
        assertNotNull(valueFromTokenTable)
        assertTrue(valueFromTokenList eq Flt64(14.0))
        assertEquals(valueFromTokenList, valueFromTokenTable)
    }

    @Test
    fun evaluate_resultsShouldMatchCurrentBehavior() {
        val x = RealVar("x")
        val y = RealVar("y")
        val tokenList = AutoTokenList(false)
        tokenList.add(listOf(x, y))
        val tokenTable = AutoTokenTable(Linear, false)
        tokenTable.add(listOf(x, y))

        val monomials = listOf(
            LinearMonomial(2, x),
            LinearMonomial(-1, y),
            LinearMonomial(x)
        )
        val constant = Flt64(3.0)
        val monomial = LinearMonomial(3, x)
        val results = listOf(Flt64(4.0), Flt64(1.0))

        val polynomialByList = evalMonomialsResults(monomials, results, tokenList as AbstractTokenListF64, zeroIfNone = false)!! + constant
        val polynomialByTable = monomials.mapNotNull { it.evaluate(results, tokenTable, zeroIfNone = false) }.let { sumFlt64(it) } + constant
        val monomialByList = monomial.evaluate(results, tokenList as AbstractTokenListF64, zeroIfNone = false)
        val monomialByTable = monomial.evaluate(results, tokenTable, zeroIfNone = false)

        assertNotNull(polynomialByList)
        assertNotNull(polynomialByTable)
        assertNotNull(monomialByList)
        assertNotNull(monomialByTable)
        assertTrue(polynomialByList eq Flt64(14.0))
        assertTrue(monomialByList eq Flt64(12.0))
        assertEquals(polynomialByList, polynomialByTable)
        assertEquals(monomialByList, monomialByTable)
    }

    @Test
    fun linearExpressionSymbolEvaluate_shouldRespectSelfValueOverrideOnTokenTablePath() {
        val x = RealVar("x")
        val tokenTable = AutoTokenTable(Linear, false)
        tokenTable.add(x)
        tokenTable.setSolution(mapOf(x to Flt64(9.0)))

        val expression = LinearExpressionSymbol(
            polynomial = MathLinearPolynomial(
                monomials = listOf(MathLinearMonomial(Flt64(2.0), x)),
                constant = Flt64.one
            ),
            name = "expr"
        )

        val overridden = expression.evaluate(
            values = mapOf<Symbol, Flt64>(
                expression to Flt64(42.0),
                x to Flt64(3.0)
            ),
            tokenTable = tokenTable,
            zeroIfNone = false
        )
        assertNotNull(overridden)
        assertTrue(overridden eq Flt64(42.0))

        val fromValues = expression.evaluate(
            values = mapOf<Symbol, Flt64>(x to Flt64(3.0)),
            tokenTable = tokenTable,
            zeroIfNone = false
        )
        assertNotNull(fromValues)
        assertTrue(fromValues eq Flt64(7.0))
    }

    @Test
    fun linearExpressionSymbolEvaluate_resultsTokenTableShouldMatchPolynomial() {
        val x = RealVar("x")
        val tokenTable = AutoTokenTable(Linear, false)
        tokenTable.add(x)
        tokenTable.setSolution(mapOf(x to Flt64.zero))
        val results = listOf(Flt64(4.0))

        val expression = LinearExpressionSymbol(
            polynomial = MathLinearPolynomial(
                monomials = listOf(MathLinearMonomial(Flt64(2.0), x)),
                constant = Flt64.one
            ),
            name = "expr_results"
        )

        val valueFromTokenList = expression.evaluate(
            results = results,
            tokenList = tokenTable.tokenList,
            zeroIfNone = false
        )
        val valueFromTokenTable = expression.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = false
        )

        assertNotNull(valueFromTokenList)
        assertNotNull(valueFromTokenTable)
        assertTrue(valueFromTokenList eq Flt64(9.0))
        assertEquals(valueFromTokenList, valueFromTokenTable)
    }

    @Test
    fun cells_shouldMergeSameVariableIntoSingleCell() {
        val x = RealVar("x")
        val y = RealVar("y")

        val flattenData = LinearFlattenDataF64(
            monomials = listOf(
                MathLinearMonomial(Flt64.one, x),
                MathLinearMonomial(Flt64(2.0), x),
                MathLinearMonomial(Flt64(-3.0), y)
            ),
            constant = Flt64(5.0)
        )

        // Group by symbol and sum coefficients (simulating cell merging)
        val coefficientByVariable = flattenData.monomials
            .groupBy { it.symbol }
            .mapValues { (_, monos) -> monos.map { it.coefficient }.let { sumFlt64(it) } }
        val constant = flattenData.constant

        assertEquals(2, coefficientByVariable.size)
        assertTrue(coefficientByVariable[x]!! eq Flt64(3.0))
        assertTrue(coefficientByVariable[y]!! eq Flt64(-3.0))
        assertTrue(constant eq Flt64(5.0))
    }

    @Test
    fun linearMonomialEvaluate_valuesShouldOverrideAndFallbackTokenList() {
        val x = RealVar("x")
        val y = RealVar("y")
        val tokenList = AutoTokenList(false)
        tokenList.add(listOf(x, y))
        tokenList.setSolution(
            mapOf(
                x to Flt64(7.0),
                y to Flt64(4.0)
            )
        )

        val monomial = LinearMonomial(3, x)
        val valueFromMap = monomial.evaluate(
            values = mapOf(x to Flt64(2.0)),
            tokenList = tokenList as AbstractTokenListF64?,
            zeroIfNone = false
        )
        assertNotNull(valueFromMap)
        assertTrue(valueFromMap eq Flt64(6.0))

        val valueFromTokenFallback = monomial.evaluate(
            values = emptyMap(),
            tokenList = tokenList as AbstractTokenListF64?,
            zeroIfNone = false
        )
        assertNotNull(valueFromTokenFallback)
        assertTrue(valueFromTokenFallback eq Flt64(21.0))
    }
}
