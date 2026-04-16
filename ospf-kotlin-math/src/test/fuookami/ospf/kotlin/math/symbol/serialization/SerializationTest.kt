package fuookami.ospf.kotlin.math.symbol.serialization

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.operation.combineTerms
import fuookami.ospf.kotlin.math.symbol.parser.Expr
import fuookami.ospf.kotlin.math.symbol.parser.parseSymbolExpression
import fuookami.ospf.kotlin.math.symbol.parser.parseSymbolInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.serde.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SerializationTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    // ============================================================================
    // Expr 基础序列化测试 / Basic Expr serialization tests
    // ============================================================================

    @Test
    fun parserExprShouldRoundTripThroughJson() {
        val expression = parseSymbolExpression("x^2 * y + 2*x - 1")
        val json = expression.toJsonString()
        val restored = symbolExprFromJson(json)

        assertEquals(expression, restored)
    }

    @Test
    fun parserInequalityShouldRoundTripThroughJson() {
        val inequality = parseSymbolInequality("x^2 + y != z + 1")
        val json = inequality.toJsonString()
        val restored = symbolExprFromJson(json)

        assertEquals(inequality, restored)
    }

    // ============================================================================
    // CanonicalPolynomial 序列化测试 / CanonicalPolynomial serialization tests
    // ============================================================================

    @Test
    fun canonicalPolynomialShouldRoundTripThroughExprModel() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")
        val symbolByName = mapOf(
            "x" to x,
            "y" to y,
            "z" to z
        )
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64(3.0), listOf(x, x, y)),
                CanonicalMonomial<Flt64>(Flt64(-2.0), listOf(z))
            ),
            constant = Flt64.one
        )

        val expr = polynomial.toExpr()
        val restored = expr.toCanonicalPolynomial { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }

        assertEquals(polynomial.combineTerms(), restored.combineTerms())
    }

    @Test
    fun canonicalPolynomialShouldRoundTripThroughJson() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf("x" to x, "y" to y)

        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64(2.5), listOf(x, x)),
                CanonicalMonomial<Flt64>(Flt64(-1.0), listOf(y))
            ),
            constant = Flt64(3.0)
        ).combineTerms()

        val json = polynomial.toJsonString()
        val restored = canonicalPolynomialFromJson(json) { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }

        assertEquals(polynomial, restored.combineTerms())
    }

    @Test
    fun canonicalPolynomialConstantOnly() {
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = emptyList(),
            constant = Flt64(42.0)
        )

        val json = polynomial.toJsonString()
        val restored = canonicalPolynomialFromJson(json)

        assertEquals(Flt64(42.0), restored.constant)
        assertTrue(restored.monomials.isEmpty())
    }

    // ============================================================================
    // LinearPolynomial 序列化测试 / LinearPolynomial serialization tests
    // ============================================================================

    @Test
    fun linearPolynomialShouldRoundTripThroughJson() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf("x" to x, "y" to y)

        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(Flt64(-3.0), y)
            ),
            constant = Flt64(5.0)
        )

        val json = polynomial.toJsonString()
        val restored = linearPolynomialFromJson(json) { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }

        assertNotNull(restored)
        // Find the coefficients in restored monomials
        val xCoeff = restored.monomials.find { it.symbol == x }?.coefficient
        val yCoeff = restored.monomials.find { it.symbol == y }?.coefficient
        assertEquals(Flt64(2.0), xCoeff)
        assertEquals(Flt64(-3.0), yCoeff)
        assertEquals(Flt64(5.0), restored.constant)
    }

    @Test
    fun linearPolynomialSimple() {
        val x = TestSymbol("x")
        val symbolByName = mapOf("x" to x)

        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )

        val json = polynomial.toJsonString()
        val restored = linearPolynomialFromJson(json) { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }

        assertNotNull(restored)
        val xCoeff = restored.monomials.find { it.symbol == x }?.coefficient
        assertEquals(Flt64.one, xCoeff)
    }

    // ============================================================================
    // QuadraticPolynomial 序列化测试 / QuadraticPolynomial serialization tests
    // ============================================================================

    @Test
    fun quadraticPolynomialShouldRoundTripThroughJson() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf("x" to x, "y" to y)
        val symbolComparator = compareBy<Symbol> { it.name }

        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial.quadratic(Flt64(1.0), x, x),
                QuadraticMonomial.linear(Flt64(2.0), x),
                QuadraticMonomial.linear(Flt64(-1.0), y)
            ),
            constant = Flt64(3.0)
        )

        val json = polynomial.toJsonString(symbolComparator)
        val restored = quadraticPolynomialFromJson(
            json,
            symbolOf = { name -> symbolByName[name] ?: error("Unknown symbol: $name") },
            symbolComparator = symbolComparator
        )

        assertNotNull(restored)
        // Find the quadratic coefficient for x*x
        val xQuadCoeff = restored.monomials.find { it.symbol1 == x && it.symbol2 == x }?.coefficient
        assertEquals(Flt64(1.0), xQuadCoeff)
        // Find linear coefficient for x
        val xLinCoeff = restored.monomials.find { it.symbol1 == x && it.symbol2 == null }?.coefficient
        assertEquals(Flt64(2.0), xLinCoeff)
        // Find linear coefficient for y
        val yCoeff = restored.monomials.find { it.symbol1 == y && it.symbol2 == null }?.coefficient
        assertEquals(Flt64(-1.0), yCoeff)
        assertEquals(Flt64(3.0), restored.constant)
    }

    // ============================================================================
    // CanonicalInequality 序列化测试 / CanonicalInequality serialization tests
    // ============================================================================

    @Test
    fun canonicalInequalityShouldRoundTripThroughExprModel() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf(
            "x" to x,
            "y" to y
        )
        val inequality = CanonicalInequality(
            lhs = CanonicalPolynomial<Flt64>(
                monomials = listOf(
                    CanonicalMonomial<Flt64>(Flt64.one, listOf(x, x)),
                    CanonicalMonomial<Flt64>(Flt64(2.0), listOf(y))
                ),
                constant = Flt64(-1.0)
            ),
            rhs = CanonicalPolynomial<Flt64>(
                monomials = listOf(
                    CanonicalMonomial<Flt64>(Flt64(3.0), listOf(x))
                ),
                constant = Flt64.one
            ),
            comparison = Comparison.LE
        )

        val expr = inequality.toExpr()
        val restored = (expr as Expr.Comparison).toCanonicalInequality { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }

        assertEquals(
            inequality.copy(
                lhs = inequality.lhs.combineTerms(),
                rhs = inequality.rhs.combineTerms()
            ),
            restored
        )
    }

    @Test
    fun canonicalInequalityShouldRoundTripThroughJson() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf("x" to x, "y" to y)

        val inequality = CanonicalInequality(
            lhs = CanonicalPolynomial<Flt64>(
                monomials = listOf(CanonicalMonomial<Flt64>(Flt64.one, listOf(x))),
                constant = Flt64.zero
            ),
            rhs = CanonicalPolynomial<Flt64>(
                monomials = listOf(CanonicalMonomial<Flt64>(Flt64(2.0), listOf(y))),
                constant = Flt64.one
            ),
            comparison = Comparison.GE
        )

        val json = inequality.toJsonString()
        val restored = canonicalInequalityFromJson(json) { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }

        assertNotNull(restored)
        assertEquals(Comparison.GE, restored.comparison)
    }

    // ============================================================================
    // LinearInequality 序列化测试 / LinearInequality serialization tests
    // ============================================================================

    @Test
    fun linearInequalityShouldRoundTripThroughJson() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val symbolByName = mapOf("x" to x, "y" to y)

        val inequality = LinearInequality(
            lhs = LinearPolynomial<Flt64>(
                monomials = listOf(LinearMonomial(Flt64(2.0), x)),
                constant = Flt64.zero
            ),
            rhs = LinearPolynomial<Flt64>(
                monomials = listOf(LinearMonomial(Flt64(3.0), y)),
                constant = Flt64(5.0)
            ),
            comparison = Comparison.LT
        )

        val json = inequality.toJsonString()
        val restored = linearInequalityFromJson(json) { name ->
            symbolByName[name] ?: error("Unknown symbol: $name")
        }

        assertNotNull(restored)
        assertEquals(Comparison.LT, restored.comparison)
    }

    // ============================================================================
    // QuadraticInequality 序列化测试 / QuadraticInequality serialization tests
    // ============================================================================

    @Test
    fun quadraticInequalityShouldRoundTripThroughJson() {
        val x = TestSymbol("x")
        val symbolByName = mapOf("x" to x)
        val symbolComparator = compareBy<Symbol> { it.name }

        val inequality: QuadraticInequalityOf<Flt64> = QuadraticInequalityOf(
            lhs = QuadraticPolynomial<Flt64>(
                monomials = listOf(
                    QuadraticMonomial.quadratic(Flt64(1.0), x, x),
                    QuadraticMonomial.linear(Flt64(-2.0), x)
                ),
                constant = Flt64.zero
            ),
            rhs = QuadraticPolynomial<Flt64>(
                monomials = emptyList(),
                constant = Flt64(4.0)
            ),
            comparison = Comparison.LE
        )

        val json = inequality.toJsonString(symbolComparator)
        val restored = quadraticInequalityFromJson(
            json,
            symbolOf = { name -> symbolByName[name] ?: error("Unknown symbol: $name") },
            symbolComparator = symbolComparator
        )

        assertNotNull(restored)
        assertEquals(Comparison.LE, restored.comparison)
    }

    // ============================================================================
    // 边界与错误场景测试 / Edge cases and error scenarios
    // ============================================================================

    @Test
    fun nonPolynomialExprShouldReturnNullForLinear() {
        // x^2 不是线性多项式
        val expr = parseSymbolExpression("x^2")
        val x = TestSymbol("x")
        val symbolByName = mapOf("x" to x)

        val result = expr.toLinearPolynomialOrNull(
            symbolOf = { name -> symbolByName[name] ?: error("Unknown symbol: $name") }
        )

        assertNull(result)
    }

    @Test
    fun nonPolynomialExprShouldReturnNullForQuadratic() {
        // x^3 不是二次多项式
        val expr = parseSymbolExpression("x^3")
        val x = TestSymbol("x")
        val symbolByName = mapOf("x" to x)

        val result = expr.toQuadraticPolynomialOrNull(
            symbolOf = { name -> symbolByName[name] ?: error("Unknown symbol: $name") }
        )

        assertNull(result)
    }

    @Test
    fun functionCallCannotConvertToPolynomial() {
        val expr = parseSymbolExpression("sin(x)")

        assertThrows<IllegalArgumentException> {
            expr.toCanonicalPolynomial()
        }
    }

    @Test
    fun emptyPolynomialSerializesToZero() {
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = emptyList(),
            constant = Flt64.zero
        )

        val json = polynomial.toJsonString()
        assertTrue(json.contains("0"))
    }

    @Test
    fun allComparisonOperators() {
        val x = TestSymbol("x")
        val symbolByName = mapOf("x" to x)

        for (comparison in Comparison.entries) {
            val inequality = CanonicalInequality(
                lhs = CanonicalPolynomial<Flt64>(
                    monomials = listOf(CanonicalMonomial<Flt64>(Flt64.one, listOf(x))),
                    constant = Flt64.zero
                ),
                rhs = CanonicalPolynomial<Flt64>(
                    monomials = emptyList(),
                    constant = Flt64.one
                ),
                comparison = comparison
            )

            val json = inequality.toJsonString()
            val restored = canonicalInequalityFromJson(json) { name ->
                symbolByName[name] ?: error("Unknown symbol: $name")
            }

            assertNotNull(restored, "Failed for comparison: $comparison")
            assertEquals(comparison, restored.comparison, "Comparison mismatch for: $comparison")
        }
    }
}