@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.core.frontend.symbol_migration.quadratic_regression

import fuookami.ospf.kotlin.core.intermediate_model.AutoTokenTable
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomialCellF64
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenDataF64
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.AutoTokenList
import fuookami.ospf.kotlin.core.variable.AbstractTokenListF64
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.variable.times
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as MathQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as MathQuadraticPolynomial
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

private fun evalQMonomials(monomials: List<QuadraticMonomial>, tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? {
    var sum = Flt64.zero
    for (mono in monomials) {
        val v = mono.evaluate(tokenList, zeroIfNone) ?: return null
        sum += v
    }
    return sum
}

private fun evalQMonomials(monomials: List<QuadraticMonomial>, values: Map<Symbol, Flt64>, tokenList: AbstractTokenListF64?, zeroIfNone: Boolean): Flt64? {
    var sum = Flt64.zero
    for (mono in monomials) {
        val v = mono.evaluate(values, tokenList, zeroIfNone) ?: return null
        sum += v
    }
    return sum
}

private fun evalQMonomialsResults(monomials: List<QuadraticMonomial>, results: List<Flt64>, tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? {
    var sum = Flt64.zero
    for (mono in monomials) {
        val v = mono.evaluate(results, tokenList, zeroIfNone) ?: return null
        sum += v
    }
    return sum
}

class QuadraticPolynomialBaselineTest {
    @Test
    fun evaluate_shouldMatchCurrentQuadraticBehavior() {
        val x = RealVar("x")
        val y = RealVar("y")
        val polynomial = MathQuadraticPolynomial(
            monomials = listOf(
                MathQuadraticMonomial.quadratic(Flt64.one, x, x),
                MathQuadraticMonomial.quadratic(Flt64.one, x, y),
                MathQuadraticMonomial.quadratic(Flt64.one, y, x),
                MathQuadraticMonomial.linear(Flt64(3.0), x)
            ),
            constant = Flt64(4.0)
        )

        val value = polynomial.evaluate(
            values = mapOf(x to Flt64(2.0), y to Flt64(5.0)),
            policy = MissingValuePolicy.ReturnNull
        )

        assertNotNull(value)
        assertTrue(value eq Flt64(34.0))
    }

    @Test
    fun evaluate_shouldRespectZeroIfNoneSwitch() {
        val x = RealVar("x")
        val y = RealVar("y")
        val polynomial = MathQuadraticPolynomial(
            monomials = listOf(
                MathQuadraticMonomial.quadratic(Flt64.one, x, x),
                MathQuadraticMonomial.quadratic(Flt64.one, y, y)
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

        val monomials = listOf(
            x * x,
            x * y
        )

        // values map overrides, tokenList falls back for missing y
        val valueFromMapAndToken = evalQMonomials(monomials, mapOf(x to Flt64(3.0)), tokenList as AbstractTokenListF64?, zeroIfNone = false)
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

        val monomials = listOf(
            x * x,
            x * y
        )

        // values map overrides, tokenTable falls back for missing y
        val valueFromMapAndToken = monomials.mapNotNull { it.evaluate(values = mapOf(x to Flt64(3.0)), tokenTable = tokenTable, zeroIfNone = false) }.let { sumFlt64(it) }
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

        val monomials = listOf(
            x * x,
            x * y,
            y * x,
            QuadraticMonomial(3 * x)
        )
        val constant = Flt64(4.0)

        val valueFromTokenList = evalQMonomials(monomials, tokenList as AbstractTokenListF64, zeroIfNone = false)!! + constant
        val valueFromTokenTable = monomials.mapNotNull { it.evaluate(tokenTable, zeroIfNone = false) }.let { sumFlt64(it) } + constant

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

        val monomials = listOf(
            x * x,
            x * y,
            y * x,
            QuadraticMonomial(3 * x)
        )
        val constant = Flt64(4.0)
        val monomial = x * y
        val results = listOf(Flt64(2.0), Flt64(5.0))

        val polynomialByList = evalQMonomialsResults(monomials, results, tokenList as AbstractTokenListF64, zeroIfNone = false)!! + constant
        val polynomialByTable = monomials.mapNotNull { it.evaluate(results, tokenTable, zeroIfNone = false) }.let { sumFlt64(it) } + constant
        val monomialByList = monomial.evaluate(results, tokenList as AbstractTokenListF64, zeroIfNone = false)
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
    fun quadraticExpressionSymbolEvaluate_shouldRespectSelfValueOverrideOnTokenTablePath() {
        val x = RealVar("x")
        val tokenTable = AutoTokenTable(Quadratic, false)
        tokenTable.add(x)
        tokenTable.setSolution(mapOf(x to Flt64(5.0)))

        val symbol = QuadraticExpressionSymbol(
            _utilsPolynomial = fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial(
                monomials = listOf(fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial.quadratic(Flt64.one, x, x)),
                constant = Flt64.one
            ),
            name = "q_linear"
        )

        val overridden = symbol.evaluate(
            values = mapOf<Symbol, Flt64>(
                symbol to Flt64(40.0),
                x to Flt64(3.0)
            ),
            tokenTable = tokenTable,
            zeroIfNone = false
        )
        assertNotNull(overridden)
        assertTrue(overridden eq Flt64(40.0))

        val fromValues = symbol.evaluate(
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

        val monomials = listOf(
            x * y,
            y * x,
            2 * (x * x),
            QuadraticMonomial(3 * x)
        )
        val constant = Flt64(7.0)

        // Collect cells by flattening each monomial's cells
        val allCells = monomials.flatMap { it.cells } +
            QuadraticMonomialCellF64(constant)

        // x*y and y*x each produce a separate cell with coefficient 1.0
        val xyCells = allCells.filter {
            it.isTriple &&
                    it.triple!!.variable2 != null &&
                    setOf(it.triple!!.variable1, it.triple!!.variable2!!) == setOf<AbstractVariableItem<*, *>>(x, y)
        }
        assertTrue(xyCells.isNotEmpty())
        val xyCoeff = xyCells.sumOf { it.triple!!.coefficient.toDouble() }
        assertTrue(Flt64(xyCoeff) eq Flt64(2.0))

        val xxCell = allCells.firstOrNull {
            it.isTriple &&
                    it.triple!!.variable1 == x &&
                    it.triple!!.variable2 == x
        }
        assertNotNull(xxCell)
        assertTrue(xxCell!!.triple!!.coefficient eq Flt64(2.0))

        val linearXCell = allCells.firstOrNull {
            it.isTriple &&
                    it.triple!!.variable1 == x &&
                    it.triple!!.variable2 == null
        }
        assertNotNull(linearXCell)
        assertTrue(linearXCell!!.triple!!.coefficient eq Flt64(3.0))

        val constCell = allCells.first { it.isConstant }.constant!!
        assertTrue(constCell eq Flt64(7.0))
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
            tokenList = tokenList as AbstractTokenListF64?,
            zeroIfNone = false
        )
        assertNotNull(valueFromMap)
        assertTrue(valueFromMap eq Flt64(8.0))

        val valueFromTokenFallback = monomial.evaluate(
            values = emptyMap(),
            tokenList = tokenList as AbstractTokenListF64?,
            zeroIfNone = false
        )
        assertNotNull(valueFromTokenFallback)
        assertTrue(valueFromTokenFallback eq Flt64(28.0))
    }
}
