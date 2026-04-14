package fuookami.ospf.kotlin.core.frontend.symbol_migration.quadratic_regression

import fuookami.ospf.kotlin.core.expression.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.expression.monomial.times
import fuookami.ospf.kotlin.core.expression.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.core.intermediate_model.AutoTokenTable
import fuookami.ospf.kotlin.core.variable.AutoTokenList
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.intermediate_model.eq
import fuookami.ospf.kotlin.core.intermediate_symbol.legacy.quadratic_function.LinearFunction as QuadraticLinearFunction

class QuadraticPolynomialBaselineTest {
    @Test
    fun evaluate_shouldMatchCurrentQuadraticBehavior() {
        val x = RealVar("x")
        val y = RealVar("y")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                x * x,
                x * y,
                y * x,
                QuadraticMonomial(3 * x)
            ),
            constant = Flt64(4.0)
        )

        val value = polynomial.evaluate(
            values = mapOf(x to Flt64(2.0), y to Flt64(5.0)),
            tokenList = null,
            zeroIfNone = false
        )

        assertNotNull(value)
        assertTrue(value eq Flt64(34.0))
    }

    @Test
    fun evaluate_shouldRespectZeroIfNoneSwitch() {
        val x = RealVar("x")
        val y = RealVar("y")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                x * x,
                y * y
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
        assertTrue(missingAsZero eq Flt64(10.0))
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

        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                x * x,
                x * y
            ),
            constant = Flt64.zero
        )

        val valueFromMapAndToken = polynomial.evaluate(
            values = mapOf(x to Flt64(3.0)),
            tokenList = tokenList,
            zeroIfNone = false
        )
        assertNotNull(valueFromMapAndToken)
        assertTrue(valueFromMapAndToken eq Flt64(21.0))
    }

    @Test
    fun evaluate_valuesShouldOverrideTokenTable_andFallbackWhenMissing() {
        val x = RealVar("x")
        val y = RealVar("y")
        val tokenTable = AutoTokenTable(Quadratic, false)
        tokenTable.add(listOf(x, y))
        tokenTable.setSolution(
            mapOf(
                x to Flt64(9.0),
                y to Flt64(4.0)
            )
        )

        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                x * x,
                x * y
            ),
            constant = Flt64.zero
        )

        val valueFromMapAndToken = polynomial.evaluate(
            values = mapOf(x to Flt64(3.0)),
            tokenTable = tokenTable,
            zeroIfNone = false
        )
        assertNotNull(valueFromMapAndToken)
        assertTrue(valueFromMapAndToken eq Flt64(21.0))
    }

    @Test
    fun evaluate_tokenListAndTokenTableShouldMatchCurrentQuadraticBehavior() {
        val x = RealVar("x")
        val y = RealVar("y")
        val tokenList = AutoTokenList(false)
        tokenList.add(listOf(x, y))
        tokenList.setSolution(
            mapOf(
                x to Flt64(2.0),
                y to Flt64(5.0)
            )
        )
        val tokenTable = AutoTokenTable(Quadratic, false)
        tokenTable.add(listOf(x, y))
        tokenTable.setSolution(
            mapOf(
                x to Flt64(2.0),
                y to Flt64(5.0)
            )
        )

        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                x * x,
                x * y,
                y * x,
                QuadraticMonomial(3 * x)
            ),
            constant = Flt64(4.0)
        )

        val valueFromTokenList = polynomial.evaluate(tokenList, zeroIfNone = false)
        val valueFromTokenTable = polynomial.evaluate(tokenTable, zeroIfNone = false)

        assertNotNull(valueFromTokenList)
        assertNotNull(valueFromTokenTable)
        assertTrue(valueFromTokenList eq Flt64(34.0))
        assertEquals(valueFromTokenList, valueFromTokenTable)
    }

    @Test
    fun evaluate_resultsShouldMatchCurrentQuadraticBehavior() {
        val x = RealVar("x")
        val y = RealVar("y")
        val tokenList = AutoTokenList(false)
        tokenList.add(listOf(x, y))
        val tokenTable = AutoTokenTable(Quadratic, false)
        tokenTable.add(listOf(x, y))

        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                x * x,
                x * y,
                y * x,
                QuadraticMonomial(3 * x)
            ),
            constant = Flt64(4.0)
        )
        val monomial = x * y
        val results = listOf(Flt64(2.0), Flt64(5.0))

        val polynomialByList = polynomial.evaluate(results, tokenList, zeroIfNone = false)
        val polynomialByTable = polynomial.evaluate(results, tokenTable, zeroIfNone = false)
        val monomialByList = monomial.evaluate(results, tokenList, zeroIfNone = false)
        val monomialByTable = monomial.evaluate(results, tokenTable, zeroIfNone = false)

        assertNotNull(polynomialByList)
        assertNotNull(polynomialByTable)
        assertNotNull(monomialByList)
        assertNotNull(monomialByTable)
        assertTrue(polynomialByList eq Flt64(34.0))
        assertTrue(monomialByList eq Flt64(10.0))
        assertEquals(polynomialByList, polynomialByTable)
        assertEquals(monomialByList, monomialByTable)
    }

    @Test
    fun quadraticFunctionEvaluate_shouldRespectSelfValueOverrideOnTokenTablePath() {
        val x = RealVar("x")
        val tokenTable = AutoTokenTable(Quadratic, false)
        tokenTable.add(x)
        tokenTable.setSolution(mapOf(x to Flt64(5.0)))

        val function = QuadraticLinearFunction(
            polynomial = QuadraticPolynomial(
                monomials = listOf(x * x),
                constant = Flt64.one
            ),
            name = "q_linear"
        )

        val overridden = function.evaluate(
            values = mapOf<Symbol, Flt64>(
                function to Flt64(40.0),
                x to Flt64(3.0)
            ),
            tokenTable = tokenTable,
            zeroIfNone = false
        )
        assertNotNull(overridden)
        assertTrue(overridden eq Flt64(40.0))

        val fromValues = function.evaluate(
            values = mapOf<Symbol, Flt64>(x to Flt64(3.0)),
            tokenTable = tokenTable,
            zeroIfNone = false
        )
        assertNotNull(fromValues)
        assertTrue(fromValues eq Flt64(10.0))
    }

    @Test
    fun cells_shouldMergeSymmetricQuadraticTerms() {
        val x = RealVar("x")
        val y = RealVar("y")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                x * y,
                y * x,
                2 * (x * x),
                QuadraticMonomial(3 * x)
            ),
            constant = Flt64(7.0)
        )

        val cells = polynomial.cells

        val xyCell = cells.firstOrNull {
            it.isTriple &&
                    it.triple!!.variable2 != null &&
                    setOf(it.triple!!.variable1, it.triple!!.variable2!!) == setOf(x, y)
        }
        assertNotNull(xyCell)
        assertTrue(xyCell.triple!!.coefficient eq Flt64(2.0))

        val xxCell = cells.firstOrNull {
            it.isTriple &&
                    it.triple!!.variable1 == x &&
                    it.triple!!.variable2 == x
        }
        assertNotNull(xxCell)
        assertTrue(xxCell.triple!!.coefficient eq Flt64(2.0))

        val linearXCell = cells.firstOrNull {
            it.isTriple &&
                    it.triple!!.variable1 == x &&
                    it.triple!!.variable2 == null
        }
        assertNotNull(linearXCell)
        assertTrue(linearXCell.triple!!.coefficient eq Flt64(3.0))

        val constant = cells.first { it.isConstant }.constant!!
        assertTrue(constant eq Flt64(7.0))
        assertEquals(4, cells.size)
    }

    @Test
    fun quadraticMonomialEvaluate_valuesShouldOverrideAndFallbackTokenList() {
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

        val monomial = x * y
        val valueFromMap = monomial.evaluate(
            values = mapOf(x to Flt64(2.0)),
            tokenList = tokenList,
            zeroIfNone = false
        )
        assertNotNull(valueFromMap)
        assertTrue(valueFromMap eq Flt64(8.0))

        val valueFromTokenFallback = monomial.evaluate(
            values = emptyMap(),
            tokenList = tokenList,
            zeroIfNone = false
        )
        assertNotNull(valueFromTokenFallback)
        assertTrue(valueFromTokenFallback eq Flt64(28.0))
    }
}




