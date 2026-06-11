package fuookami.ospf.kotlin.math.symbol.parse

import kotlin.test.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*

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
        val result = parseCanonicalFlt64("42", symbolOf)
        assertEquals(Flt64(42.0), result.constant)
        assertTrue(result.monomials.isEmpty())
    }

    @Test
    fun testNegativeConstant() {
        val result = parseCanonicalFlt64("-3", symbolOf)
        assertEquals(Flt64(-3.0), result.constant)
        assertTrue(result.monomials.isEmpty())
    }

    @Test
    fun testSingleVariable() {
        val result = parseCanonicalFlt64("x", symbolOf)
        assertEquals<Flt64>(Flt64.zero, result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64.one, result.monomials[0].coefficient)
        assertEquals(mapOf<Symbol, Int32>(x to Int32.one), result.monomials[0].powers)
    }

    @Test
    fun testVariablePlusConstant() {
        val result = parseCanonicalFlt64("x + 5", symbolOf)
        assertEquals(Flt64(5.0), result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64.one, result.monomials[0].coefficient)
    }

    @Test
    fun testVariableMinusConstant() {
        val result = parseCanonicalFlt64("x - 3", symbolOf)
        assertEquals(Flt64(-3.0), result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64.one, result.monomials[0].coefficient)
    }

    @Test
    fun testScaledVariable() {
        val result = parseCanonicalFlt64("2 * x", symbolOf)
        assertEquals<Flt64>(Flt64.zero, result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64(2.0), result.monomials[0].coefficient)
    }

    @Test
    fun testLinearCombination() {
        val result = parseCanonicalFlt64("3 * x + 2 * y + 1", symbolOf)
        assertEquals(Flt64(1.0), result.constant)
        assertEquals(2, result.monomials.size)
    }

    @Test
    fun testQuadraticTerm() {
        val result = parseCanonicalFlt64("x * y", symbolOf)
        assertEquals<Flt64>(Flt64.zero, result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64.one, result.monomials[0].coefficient)
        assertEquals(2, result.monomials[0].powers.size)
    }

    @Test
    fun testParentheses() {
        val result = parseCanonicalFlt64("(x + 1) * 2", symbolOf)
        assertEquals(Flt64(2.0), result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64(2.0), result.monomials[0].coefficient)
    }

    @Test
    fun testParseLinear() {
        val result = parseLinearFlt64("3 * x + 2 * y + 1", symbolOf)
        assertNotNull(result)
        assertEquals(Flt64(1.0), result.constant)
    }

    @Test
    fun testParseLinearRejectsQuadratic() {
        val result = parseLinearFlt64("x * y", symbolOf)
        assertNull(result)
    }

    @Test
    fun testParseQuadratic() {
        val result = parseQuadraticFlt64("x * y + 3 * x + 1", symbolOf)
        assertNotNull(result)
    }

    @Test
    fun testParseCanonicalInequality() {
        val result = parseCanonicalInequalityFlt64("x + 1 <= 5", symbolOf)
        assertNotNull(result)
        assertEquals(Comparison.LE, result.comparison)
    }

    @Test
    fun testParseLinearInequality() {
        val result = parseLinearInequalityFlt64("3 * x + 2 <= 10", symbolOf)
        assertNotNull(result)
    }

    @Test
    fun testParseQuadraticInequality() {
        val result = parseQuadraticInequalityFlt64("x * y + 3 * x <= 10", symbolOf)
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
            val result = parseCanonicalInequalityFlt64(input, symbolOf)
            assertEquals(expected, result.comparison, "Failed for input: $input")
        }
    }
}
