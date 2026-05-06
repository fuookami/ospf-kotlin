package fuookami.ospf.kotlin.math.symbol.parse

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.parseCanonical
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.parseLinear
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.parseQuadratic
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.parseLinearInequality
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.parseQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.parseCanonicalInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import kotlin.test.*

private data class TestSymbol(
    override val name: String,
    override val displayName: String? = null
) : Symbol

class DirectPolynomialParserTest {

    private val x = TestSymbol("x")
    private val y = TestSymbol("y")
    private val z = TestSymbol("z")

    private val symbolOf: (String) -> Symbol = { name ->
        when (name) {
            "x" -> x
            "y" -> y
            "z" -> z
            else -> TestSymbol(name)
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
        assertEquals<Flt64>(Flt64.zero, result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64.one, result.monomials[0].coefficient)
        assertEquals(mapOf<Symbol, Int32>(x to Int32.one), result.monomials[0].powers)
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
        assertEquals<Flt64>(Flt64.zero, result.constant)
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
        assertEquals<Flt64>(Flt64.zero, result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64.one, result.monomials[0].coefficient)
        assertEquals(2, result.monomials[0].powers.size)
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
