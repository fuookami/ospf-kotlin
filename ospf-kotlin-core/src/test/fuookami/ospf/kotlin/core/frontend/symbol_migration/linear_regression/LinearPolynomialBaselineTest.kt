package fuookami.ospf.kotlin.core.frontend.symbol_migration.linear_regression

import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AutoTokenTable
import fuookami.ospf.kotlin.core.frontend.variable.AutoTokenList
import fuookami.ospf.kotlin.core.frontend.variable.RealVar
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinearPolynomialBaselineTest {
    @Test
    fun evaluate_shouldMatchCurrentArithmeticBehavior() {
        val x = RealVar("x")
        val y = RealVar("y")

        val polynomial = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(2, x),
                LinearMonomial(-1, y),
                LinearMonomial(x)
            ),
            constant = Flt64(3.0)
        )

        val value = polynomial.evaluate(
            values = mapOf(x to Flt64(4.0), y to Flt64(1.0)),
            tokenList = null,
            zeroIfNone = false
        )

        assertNotNull(value)
        assertTrue(value eq Flt64(14.0))
    }

    @Test
    fun evaluate_shouldRespectZeroIfNoneSwitch() {
        val x = RealVar("x")
        val y = RealVar("y")
        val polynomial = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(2, x),
                LinearMonomial(y)
            ),
            constant = Flt64.one
        )

        val missingAsNull = polynomial.evaluate(
            values = mapOf(x to Flt64(3.0)),
            tokenList = null,
            zeroIfNone = false
        )
        assertNull(missingAsNull)

        val missingAsZero = polynomial.evaluate(
            values = mapOf(x to Flt64(3.0)),
            tokenList = null,
            zeroIfNone = true
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

        val polynomial = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(2, x),
                LinearMonomial(y)
            ),
            constant = Flt64.zero
        )

        val valueFromMapAndToken = polynomial.evaluate(
            values = mapOf(x to Flt64(3.0)),
            tokenList = tokenList,
            zeroIfNone = false
        )
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

        val polynomial = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(2, x),
                LinearMonomial(y)
            ),
            constant = Flt64.zero
        )

        val valueFromMapAndToken = polynomial.evaluate(
            values = mapOf(x to Flt64(3.0)),
            tokenTable = tokenTable,
            zeroIfNone = false
        )
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

        val polynomial = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(2, x),
                LinearMonomial(-1, y),
                LinearMonomial(x)
            ),
            constant = Flt64(3.0)
        )

        val valueFromTokenList = polynomial.evaluate(tokenList, zeroIfNone = false)
        val valueFromTokenTable = polynomial.evaluate(tokenTable, zeroIfNone = false)

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

        val polynomial = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(2, x),
                LinearMonomial(-1, y),
                LinearMonomial(x)
            ),
            constant = Flt64(3.0)
        )
        val monomial = LinearMonomial(3, x)
        val results = listOf(Flt64(4.0), Flt64(1.0))

        val polynomialByList = polynomial.evaluate(results, tokenList, zeroIfNone = false)
        val polynomialByTable = polynomial.evaluate(results, tokenTable, zeroIfNone = false)
        val monomialByList = monomial.evaluate(results, tokenList, zeroIfNone = false)
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
            polynomial = LinearPolynomial(
                monomials = listOf(LinearMonomial(2, x)),
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
            polynomial = LinearPolynomial(
                monomials = listOf(LinearMonomial(2, x)),
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
        val polynomial = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(x),
                LinearMonomial(2, x),
                LinearMonomial(-3, y)
            ),
            constant = Flt64(5.0)
        )

        val coefficientByVariable = polynomial.cells
            .filter { it.isPair }
            .associate { it.pair!!.variable to it.pair!!.coefficient }
        val constant = polynomial.cells.first { it.isConstant }.constant!!

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
            tokenList = tokenList,
            zeroIfNone = false
        )
        assertNotNull(valueFromMap)
        assertTrue(valueFromMap eq Flt64(6.0))

        val valueFromTokenFallback = monomial.evaluate(
            values = emptyMap(),
            tokenList = tokenList,
            zeroIfNone = false
        )
        assertNotNull(valueFromTokenFallback)
        assertTrue(valueFromTokenFallback eq Flt64(21.0))
    }
}




