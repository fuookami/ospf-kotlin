package fuookami.ospf.kotlin.math.symbol.parse

import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok

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

    /** 解析成功结果 / Parse successful result */
    private fun <T> parseSuccess(result: ParseResult<T>): T {
        assertTrue(result is Ok)
        return result.value
    }

    @Test
    /** 测试常量解析 / Test constant parsing */
    fun testConstant() {
        val result = parseSuccess(parseCanonicalFlt64("42", symbolOf))
        assertEquals(Flt64(42.0), result.constant)
        assertTrue(result.monomials.isEmpty())
    }

    @Test
    /** 测试负常量解析 / Test negative constant parsing */
    fun testNegativeConstant() {
        val result = parseSuccess(parseCanonicalFlt64("-3", symbolOf))
        assertEquals(Flt64(-3.0), result.constant)
        assertTrue(result.monomials.isEmpty())
    }

    @Test
    /** 测试单变量解析 / Test single variable parsing */
    fun testSingleVariable() {
        val result = parseSuccess(parseCanonicalFlt64("x", symbolOf))
        assertEquals<Flt64>(Flt64.zero, result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64.one, result.monomials[0].coefficient)
        assertEquals(mapOf<Symbol, Int32>(x to Int32.one), result.monomials[0].powers)
    }

    @Test
    /** 测试变量加常量解析 / Test variable plus constant parsing */
    fun testVariablePlusConstant() {
        val result = parseSuccess(parseCanonicalFlt64("x + 5", symbolOf))
        assertEquals(Flt64(5.0), result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64.one, result.monomials[0].coefficient)
    }

    @Test
    /** 测试变量减常量解析 / Test variable minus constant parsing */
    fun testVariableMinusConstant() {
        val result = parseSuccess(parseCanonicalFlt64("x - 3", symbolOf))
        assertEquals(Flt64(-3.0), result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64.one, result.monomials[0].coefficient)
    }

    @Test
    /** 测试缩放变量解析 / Test scaled variable parsing */
    fun testScaledVariable() {
        val result = parseSuccess(parseCanonicalFlt64("2 * x", symbolOf))
        assertEquals<Flt64>(Flt64.zero, result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64(2.0), result.monomials[0].coefficient)
    }

    @Test
    /** 测试线性组合解析 / Test linear combination parsing */
    fun testLinearCombination() {
        val result = parseSuccess(parseCanonicalFlt64("3 * x + 2 * y + 1", symbolOf))
        assertEquals(Flt64(1.0), result.constant)
        assertEquals(2, result.monomials.size)
    }

    @Test
    /** 测试二次项解析 / Test quadratic term parsing */
    fun testQuadraticTerm() {
        val result = parseSuccess(parseCanonicalFlt64("x * y", symbolOf))
        assertEquals<Flt64>(Flt64.zero, result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64.one, result.monomials[0].coefficient)
        assertEquals(2, result.monomials[0].powers.size)
    }

    @Test
    /** 测试括号解析 / Test parentheses parsing */
    fun testParentheses() {
        val result = parseSuccess(parseCanonicalFlt64("(x + 1) * 2", symbolOf))
        assertEquals(Flt64(2.0), result.constant)
        assertEquals(1, result.monomials.size)
        assertEquals(Flt64(2.0), result.monomials[0].coefficient)
    }

    @Test
    /** 测试线性解析 / Test linear parsing */
    fun testParseLinear() {
        val result = parseSuccess(parseLinearFlt64("3 * x + 2 * y + 1", symbolOf))
        assertNotNull(result)
        assertEquals(Flt64(1.0), result.constant)
    }

    @Test
    fun testParseLinearRejectsQuadratic() {
        val result = parseLinearFlt64("x * y", symbolOf)
        assertTrue(result is Failed)
    }

    @Test
    /** 测试二次解析 / Test quadratic parsing */
    fun testParseQuadratic() {
        val result = parseSuccess(parseQuadraticFlt64("x * y + 3 * x + 1", symbolOf))
        assertNotNull(result)
    }

    @Test
    /** 测试标准不等式解析 / Test canonical inequality parsing */
    fun testParseCanonicalInequality() {
        val result = parseSuccess(parseCanonicalInequalityFlt64("x + 1 <= 5", symbolOf))
        assertNotNull(result)
        assertEquals(Comparison.LE, result.comparison)
    }

    @Test
    /** 测试线性不等式解析 / Test linear inequality parsing */
    fun testParseLinearInequality() {
        val result = parseSuccess(parseLinearInequalityFlt64("3 * x + 2 <= 10", symbolOf))
        assertNotNull(result)
    }

    @Test
    /** 测试二次不等式解析 / Test quadratic inequality parsing */
    fun testParseQuadraticInequality() {
        val result = parseSuccess(parseQuadraticInequalityFlt64("x * y + 3 * x <= 10", symbolOf))
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
            val result = parseSuccess(parseCanonicalInequalityFlt64(input, symbolOf))
            assertEquals(expected, result.comparison, "Failed for input: $input")
        }
    }
}
