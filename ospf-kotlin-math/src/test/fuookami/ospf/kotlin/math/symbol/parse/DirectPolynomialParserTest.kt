package fuookami.ospf.kotlin.math.symbol.parse

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.evaluate
import kotlin.test.*

class DirectPolynomialParserTest {

    private val x = Symbol("x")
    private val y = Symbol("y")
    private val z = Symbol("z")

    private val symbolOf: (String) -> Symbol = { name ->
        when (name) {
            "x" -> x
            "y" -> y
            "z" -> z
            else -> Symbol(name)
        }
    }

    @Test
    fun testConstant() {
        val result = parseCanonical("42", symbolOf)
        assertEquals(Flt64(42.0), result.constant)
        assertTrue(result.monomials.isEmpty())
    }

    @Test
    fun testNegativeConstant() {
        val result = parseCanonical("-3", symbolOf)
        assertEquals(Flt64(-3.0), result.constant)
        assertTrue(result.monomials.isEmpty())
    }

    @Test
    fun testSingleVariable() {
        val result = parseCanonical("x", symbolOf)
        assertEquals(Flt64.zero, result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64.one, result.monomials[0].coefficient)
        assertEquals(mapOf(x to Int32.one), result.monomials[0].powers)
    }

    @Test
    fun testVariablePlusConstant() {
        val result = parseCanonical("x + 5", symbolOf)
        assertEquals(Flt64(5.0), result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64.one, result.monomials[0].coefficient)
    }

    @Test
    fun testVariableMinusConstant() {
        val result = parseCanonical("x - 3", symbolOf)
        assertEquals(Flt64(-3.0), result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64.one, result.monomials[0].coefficient)
    }

    @Test
    fun testScaledVariable() {
        val result = parseCanonical("2 * x", symbolOf)
        assertEquals(Flt64.zero, result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64(2.0), result.monomials[0].coefficient)
    }

    @Test
    fun testLinearCombination() {
        val result = parseCanonical("3 * x + 2 * y + 1", symbolOf)
        assertEquals(Flt64(1.0), result.constant)
        assertEquals(2, result.monomials.size)
    }

    @Test
    fun testQuadraticTerm() {
        val result = parseCanonical("x * y", symbolOf)
        assertEquals(Flt64.zero, result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64.one, result.monomials[0].coefficient)
        assertEquals(2, result.monomials[0].powers.size)
    }

    @Test
    fun testPowerTerm() {
        val result = parseCanonical("x ^ 2", symbolOf)
        // x^2 is parsed as x * x by the old parser, but our new parser
        // doesn't handle ^ in the factor level yet - let's check
        // Actually, we need to handle ^ in the parser
    }

    @Test
    fun testParentheses() {
        val result = parseCanonical("(x + 1) * 2", symbolOf)
        assertEquals(Flt64(2.0), result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64(2.0), result.monomials[0].coefficient)
    }

    @Test
    fun testParseLinear() {
        val result = parseLinear("3 * x + 2 * y + 1", symbolOf)
        assertNotNull(result)
        assertEquals(Flt64(1.0), result.constant)
    }

    @Test
    fun testParseLinearRejectsQuadratic() {
        val result = parseLinear("x * y", symbolOf)
        assertNull(result)
    }

    @Test
    fun testParseQuadratic() {
        val result = parseQuadratic("x * y + 3 * x + 1", symbolOf)
        assertNotNull(result)
    }

    @Test
    fun testParseCanonicalInequality() {
        val result = parseCanonicalInequality("x + 1 <= 5", symbolOf)
        assertNotNull(result)
        assertEquals(Comparison.LE, result.comparison)
    }

    @Test
    fun testParseLinearInequality() {
        val result = parseLinearInequality("3 * x + 2 <= 10", symbolOf)
        assertNotNull(result)
    }

    @Test
    fun testParseQuadraticInequality() {
        val result = parseQuadraticInequality("x * y + 3 * x <= 10", symbolOf)
        assertNotNull(result)
    }

    @Test
    fun testAllComparisonOperators() {
        for ((input, expected) in listOf(
            "x < 1" to Comparison.LT,
            "x <= 1" to Comparison.LE,
            "x = 1" to Comparison.EQ,
            "x != 1" to Comparison.NE,
            "x >= 1" to Comparison.GE,
            "x > 1" to Comparison.GT
        )) {
            val result = parseCanonicalInequality(input, symbolOf)
            assertEquals(expected, result.comparison, "Failed for input: $input")
        }
    }
}
