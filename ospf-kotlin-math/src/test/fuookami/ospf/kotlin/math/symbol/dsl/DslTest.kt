package fuookami.ospf.kotlin.math.symbol.dsl

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.parser.Expr
import fuookami.ospf.kotlin.math.symbol.serde.toCanonicalInequality
import fuookami.ospf.kotlin.math.symbol.serde.toCanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.serde.toExpr
import fuookami.ospf.kotlin.math.symbol.serde.toJsonString
import fuookami.ospf.kotlin.math.symbol.serde.symbolExprFromJson
import fuookami.ospf.kotlin.math.symbol.serde.toLinearPolynomialOrNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DslTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    // ============================================================================
    // 基础 DSL 构建测试 / Basic DSL building tests
    // ============================================================================

    @Test
    fun dslShouldBuildLinearExpression() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf(
            "x" to x,
            "y" to y
        )

        val expression = symbolExpr {
            num(2) * symbol("x") + num(3) * symbol("y") - num(1)
        }
        val polynomial = expression.toLinearPolynomialOrNull { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }

        assertNotNull(polynomial)
        val coefficients = polynomial.monomials.associate { it.symbol to it.coefficient }
        assertEquals(Flt64(2.0), coefficients[x])
        assertEquals(Flt64(3.0), coefficients[y])
        assertEquals(Flt64(-1.0), polynomial.constant)
    }

    @Test
    fun dslShouldBuildCanonicalHighOrderExpression() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf(
            "x" to x,
            "y" to y
        )

        val expression = symbolExpr {
            (symbol("x") pow 2) * (symbol("y") pow 3) + num(2) * symbol("x") - num(1)
        }
        val polynomial = expression.toCanonicalPolynomial { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }
        val monomialByDegree = polynomial.monomials.associateBy { it.factors.size }

        assertEquals(Flt64(-1.0), polynomial.constant)
        assertEquals(Flt64(2.0), monomialByDegree[1]?.coefficient)
        assertEquals(listOf(x), monomialByDegree[1]?.factors)
        assertEquals(Flt64.one, monomialByDegree[5]?.coefficient)
        assertEquals(listOf(x, x, y, y, y), monomialByDegree[5]?.factors)
    }

    @Test
    fun dslShouldBuildInequalityExpression() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf(
            "x" to x,
            "y" to y
        )

        val expression = symbolExpr {
            (symbol("x") pow 2) + num(1) le (symbol("y") + num(3))
        }
        val inequality = (expression as Expr.Comparison)
            .toCanonicalInequality { name ->
                symbolByName[name] ?: error("Unknown symbol: $name")
            }

        assertEquals(
            inequality,
            inequality.toExpr().toCanonicalInequality { name ->
                symbolByName[name] ?: error("Unknown symbol: $name")
            }
        )
    }

    @Test
    fun dslExpressionShouldSupportJsonRoundTrip() {
        val expression = symbolExpr {
            call("max", symbol("x"), symbol("y") + num(1))
        }

        val restored = symbolExprFromJson(expression.toJsonString())

        assertEquals(expression, restored)
    }

    // ============================================================================
    // 快捷转换入口测试 / Shortcut conversion entry point tests
    // ============================================================================

    @Test
    fun shortcutLinearPolynomialSuccess() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf("x" to x, "y" to y)

        val polynomial = linearPolynomial(
            symbolOf = { name -> symbolByName[name] ?: error("Unknown symbol: $name") }
        ) {
            num(3) * symbol("x") - num(2) * symbol("y") + num(5)
        }

        assertNotNull(polynomial)
        val coeffs = polynomial.monomials.associate { it.symbol to it.coefficient }
        assertEquals(Flt64(3.0), coeffs[x])
        assertEquals(Flt64(-2.0), coeffs[y])
        assertEquals(Flt64(5.0), polynomial.constant)
    }

    @Test
    fun shortcutLinearPolynomialReturnsNullForNonLinear() {
        val x = TestSymbol("x")
        val symbolByName = mapOf("x" to x)

        val polynomial = linearPolynomial(
            symbolOf = { name -> symbolByName[name] ?: error("Unknown symbol: $name") }
        ) {
            (symbol("x") pow 2) + num(1)
        }

        assertNull(polynomial)
    }

    @Test
    fun shortcutQuadraticPolynomialSuccess() {
        val x = TestSymbol("x")
        val symbolByName = mapOf("x" to x)
        val symbolComparator = compareBy<Symbol> { it.name }

        val polynomial = quadraticPolynomial(
            symbolOf = { name -> symbolByName[name] ?: error("Unknown symbol: $name") },
            symbolComparator = symbolComparator
        ) {
            (symbol("x") pow 2) - num(2) * symbol("x") + num(1)
        }

        assertNotNull(polynomial)
        // x^2 - 2x + 1 should have 2 monomials (x^2 and -2x) plus constant 1
        assertTrue(polynomial.monomials.any { it.symbol1 == x && it.symbol2 == x })
        assertTrue(polynomial.monomials.any { it.symbol1 == x && it.symbol2 == null })
        assertEquals(Flt64(1.0), polynomial.constant)
    }

    @Test
    fun shortcutQuadraticPolynomialReturnsNullForNonQuadratic() {
        val x = TestSymbol("x")
        val symbolByName = mapOf("x" to x)

        val polynomial = quadraticPolynomial(
            symbolOf = { name -> symbolByName[name] ?: error("Unknown symbol: $name") }
        ) {
            (symbol("x") pow 3) + num(1)
        }

        assertNull(polynomial)
    }

    @Test
    fun shortcutCanonicalPolynomialSuccess() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf("x" to x, "y" to y)

        val polynomial = canonicalPolynomial(
            symbolOf = { name -> symbolByName[name] ?: error("Unknown symbol: $name") }
        ) {
            (symbol("x") pow 2) * (symbol("y") pow 3) + num(2) * symbol("x") - num(1)
        }

        assertEquals(Flt64(-1.0), polynomial.constant)
        assertEquals(2, polynomial.monomials.size)
    }

    @Test
    fun shortcutLinearInequalitySuccess() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf("x" to x, "y" to y)

        val inequality = linearInequality(
            symbolOf = { name -> symbolByName[name] ?: error("Unknown symbol: $name") }
        ) {
            num(2) * symbol("x") le (symbol("y") + num(3))
        }

        assertNotNull(inequality)
        assertEquals(Comparison.LE, inequality.comparison)
    }

    @Test
    fun shortcutLinearInequalityReturnsNullForNonLinear() {
        val x = TestSymbol("x")
        val symbolByName = mapOf("x" to x)

        val inequality = linearInequality(
            symbolOf = { name -> symbolByName[name] ?: error("Unknown symbol: $name") }
        ) {
            (symbol("x") pow 2) le num(1)
        }

        assertNull(inequality)
    }

    @Test
    fun shortcutQuadraticInequalitySuccess() {
        val x = TestSymbol("x")
        val symbolByName = mapOf("x" to x)
        val symbolComparator = compareBy<Symbol> { it.name }

        val inequality = quadraticInequality(
            symbolOf = { name -> symbolByName[name] ?: error("Unknown symbol: $name") },
            symbolComparator = symbolComparator
        ) {
            (symbol("x") pow 2) - num(2) * symbol("x") ge num(1)
        }

        assertNotNull(inequality)
        assertEquals(Comparison.GE, inequality.comparison)
    }

    @Test
    fun shortcutCanonicalInequalitySuccess() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf("x" to x, "y" to y)

        val inequality = canonicalInequality(
            symbolOf = { name -> symbolByName[name] ?: error("Unknown symbol: $name") }
        ) {
            (symbol("x") pow 2) + symbol("y") ne num(0)
        }

        assertEquals(Comparison.NE, inequality.comparison)
    }

    // ============================================================================
    // 所有比较操作符测试 / All comparison operator tests
    // ============================================================================

    @Test
    fun allComparisonOperatorsInDsl() {
        val x = TestSymbol("x")
        val symbolByName = mapOf("x" to x)

        val ltInequality = canonicalInequality({ symbolByName[it] ?: error("Unknown symbol: $it") }) { symbol("x") lt num(1) }
        assertEquals(Comparison.LT, ltInequality.comparison)

        val leInequality = canonicalInequality({ symbolByName[it] ?: error("Unknown symbol: $it") }) { symbol("x") le num(1) }
        assertEquals(Comparison.LE, leInequality.comparison)

        val eqInequality = canonicalInequality({ symbolByName[it] ?: error("Unknown symbol: $it") }) { symbol("x") eq num(1) }
        assertEquals(Comparison.EQ, eqInequality.comparison)

        val neInequality = canonicalInequality({ symbolByName[it] ?: error("Unknown symbol: $it") }) { symbol("x") ne num(1) }
        assertEquals(Comparison.NE, neInequality.comparison)

        val geInequality = canonicalInequality({ symbolByName[it] ?: error("Unknown symbol: $it") }) { symbol("x") ge num(1) }
        assertEquals(Comparison.GE, geInequality.comparison)

        val gtInequality = canonicalInequality({ symbolByName[it] ?: error("Unknown symbol: $it") }) { symbol("x") gt num(1) }
        assertEquals(Comparison.GT, gtInequality.comparison)
    }

    // ============================================================================
    // 错误场景测试 / Error scenario tests
    // ============================================================================

    @Test
    fun dslUnknownSymbolThrowsError() {
        assertThrows<IllegalStateException> {
            canonicalPolynomial(
                symbolOf = { name -> error("Unknown symbol: $name") }
            ) {
                symbol("x") + num(1)
            }
        }
    }

    @Test
    fun dslNegativeExponentThrowsError() {
        val x = TestSymbol("x")
        val symbolByName = mapOf("x" to x)

        // Note: DSL doesn't validate exponent at build time, it fails during conversion
        assertThrows<IllegalArgumentException> {
            canonicalPolynomial(
                symbolOf = { symbolByName[it] ?: error("Unknown symbol: $it") }
            ) {
                symbol("x") pow -1
            }
        }
    }

    @Test
    fun dslFunctionCallCannotConvertToPolynomial() {
        val x = TestSymbol("x")
        val symbolByName = mapOf("x" to x)

        assertThrows<IllegalArgumentException> {
            canonicalPolynomial(
                symbolOf = { symbolByName[it] ?: error("Unknown symbol: $it") }
            ) {
                call("sin", symbol("x"))
            }
        }
    }
}