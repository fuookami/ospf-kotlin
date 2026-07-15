package fuookami.ospf.kotlin.math.symbol.operation

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*

class IntegrationTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    // ============================================================================
    // Linear Integration Tests
    // ============================================================================

    @Test
    fun testIntegrateLinearMonomial() {
        val x = TestSymbol("x")
        val monomial = LinearMonomial<Flt64>(Flt64(4.0), x)

        // ∫(4x) dx = 2x² + C
        val integral = monomial.integrateLinear(x, Flt64.zero)

        assertEquals(1, integral.monomials.size)
        assertEquals(Flt64(2.0), integral.monomials.first().coefficient)
        assertEquals(x, integral.monomials.first().symbol1)
        assertEquals(x, integral.monomials.first().symbol2)
    }

    @Test
    fun testIntegrateLinearPolynomialSimple() {
        val x = TestSymbol("x")
        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64(2.0), x)
            ),
            constant = Flt64(3.0)
        )

        // ∫(2x + 3) dx = x² + 3x + C
        val integral = polynomial.integrateLinear(x, Flt64.zero)

        assertEquals(2, integral.monomials.size)

        // 检查 x² 项
        val xSquared = integral.monomials.find { it.symbol1 == x && it.symbol2 == x }
        assertTrue(xSquared != null)
        assertEquals(Flt64.one, xSquared!!.coefficient)

        // 检查 x 项（常数 3 积分后变成 3x）
        val xTerm = integral.monomials.find { it.symbol1 == x && it.symbol2 == null }
        assertTrue(xTerm != null)
        assertEquals(Flt64(3.0), xTerm!!.coefficient)
    }

    @Test
    fun testIntegrateLinearPolynomialMultiVariable() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64(2.0), x),
                LinearMonomial<Flt64>(Flt64(3.0), y)
            ),
            constant = Flt64(5.0)
        )

        // ∫(2x + 3y + 5) dx = x² + 3yx + 5x + C
        // y 被视为常数
        val integral = polynomial.integrateLinear(x, Flt64.zero)

        assertEquals(3, integral.monomials.size)

        // 检查 x² 项
        val xSquared = integral.monomials.find { it.symbol1 == x && it.symbol2 == x }
        assertTrue(xSquared != null)
        assertEquals(Flt64.one, xSquared!!.coefficient)

        // 检查 yx 项（y 被视为常数，合并后可能顺序不同）
        // 由于 combineQuadraticTerms 会规范化符号顺序，需要检查两种可能
        val yxTerm = integral.monomials.find {
            (it.symbol1 == y && it.symbol2 == x) || (it.symbol1 == x && it.symbol2 == y)
        }
        assertTrue(yxTerm != null)
        assertEquals(Flt64(3.0), yxTerm!!.coefficient)

        // 检查 x 项（常数 5 积分后变成 5x）
        val xTerm = integral.monomials.find { it.symbol1 == x && it.symbol2 == null }
        assertTrue(xTerm != null)
        assertEquals(Flt64(5.0), xTerm!!.coefficient)
    }

    @Test
    fun testIntegrateLinearPolynomialWithIntegrationConstant() {
        val x = TestSymbol("x")
        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64(2.0), x)
            ),
            constant = Flt64.zero
        )

        // ∫(2x) dx = x² + C，这里 C = 10
        val integral = polynomial.integrateLinear(x, Flt64(10.0))

        assertEquals(Flt64(10.0), integral.constant)
    }

    // ============================================================================
    // Quadratic Integration Tests
    // ============================================================================

    @Test
    fun testIntegrateQuadraticMonomialSquared() {
        val x = TestSymbol("x")
        val monomial = QuadraticMonomial<Flt64>(Flt64(6.0), x, x)

        // ∫(6x²) dx = 2x³ + C
        val integral = monomial.integrateQuadratic(x, Flt64.zero)

        assertEquals(1, integral.monomials.size)
        assertEquals(Flt64(2.0), integral.monomials.first().coefficient)
        assertEquals(Int32(3), integral.monomials.first().powers[x])
    }

    @Test
    fun testIntegrateQuadraticMonomialLinear() {
        val x = TestSymbol("x")
        val monomial = QuadraticMonomial<Flt64>(Flt64(4.0), x, null)

        // ∫(4x) dx = 2x² + C
        val integral = monomial.integrateQuadratic(x, Flt64.zero)

        assertEquals(1, integral.monomials.size)
        assertEquals(Flt64(2.0), integral.monomials.first().coefficient)
        assertEquals(Int32(2), integral.monomials.first().powers[x])
    }

    @Test
    fun testIntegrateQuadraticMonomialMixed() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val monomial = QuadraticMonomial<Flt64>(Flt64(4.0), x, y)

        // ∫(4xy) dx = 2yx² + C (y 被视为常数)
        val integral = monomial.integrateQuadratic(x, Flt64.zero)

        assertEquals(1, integral.monomials.size)
        assertEquals(Flt64(2.0), integral.monomials.first().coefficient)
        assertEquals(Int32(2), integral.monomials.first().powers[x])
        assertEquals(Int32(1), integral.monomials.first().powers[y])
    }

    @Test
    fun testIntegrateQuadraticPolynomial() {
        val x = TestSymbol("x")
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64(3.0), x, x),  // 3x² → x³
                QuadraticMonomial<Flt64>(Flt64(2.0), x, null) // 2x → x²
            ),
            constant = Flt64(5.0)
        )

        // ∫(3x² + 2x + 5) dx = x³ + x² + 5x + C
        val integral = polynomial.integrateQuadratic(x, Flt64.zero)

        assertEquals(3, integral.monomials.size)

        // 检查 x³ 项
        val xCubed = integral.monomials.find { it.powers[x] == Int32(3) }
        assertTrue(xCubed != null)
        assertEquals(Flt64.one, xCubed!!.coefficient)

        // 检查 x² 项
        val xSquared = integral.monomials.find { it.powers[x] == Int32(2) && it.powers.size == 1 }
        assertTrue(xSquared != null)
        assertEquals(Flt64.one, xSquared!!.coefficient)

        // 检查 x 项（常数 5 积分后变成 5x）
        val xTerm = integral.monomials.find { it.powers[x] == Int32(1) && it.powers.size == 1 }
        assertTrue(xTerm != null)
        assertEquals(Flt64(5.0), xTerm!!.coefficient)
    }

    // ============================================================================
    // Canonical Integration Tests
    // ============================================================================

    @Test
    fun testIntegrateCanonicalMonomial() {
        val x = TestSymbol("x")
        val monomial = CanonicalMonomial<Flt64>(
            coefficient = Flt64(3.0),
            powers = mapOf(x to Int32(2))
        )

        // ∫(3x²) dx = x³ + C
        val integral = monomial.integrateCanonical(x, Flt64.zero)

        assertEquals(1, integral.monomials.size)
        assertEquals(Flt64.one, integral.monomials.first().coefficient)
        assertEquals(Int32(3), integral.monomials.first().powers[x])
    }

    @Test
    fun testIntegrateCanonicalMonomialHigherDegree() {
        val x = TestSymbol("x")
        val monomial = CanonicalMonomial<Flt64>(
            coefficient = Flt64(4.0),
            powers = mapOf(x to Int32(3))
        )

        // ∫(4x³) dx = x⁴ + C
        val integral = monomial.integrateCanonical(x, Flt64.zero)

        assertEquals(1, integral.monomials.size)
        assertEquals(Flt64.one, integral.monomials.first().coefficient)
        assertEquals(Int32(4), integral.monomials.first().powers[x])
    }

    @Test
    fun testIntegrateCanonicalMonomialMultiVariable() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val monomial = CanonicalMonomial<Flt64>(
            coefficient = Flt64(2.0),
            powers = mapOf(x to Int32(2), y to Int32(1))
        )

        // ∫(2x²y) dx = 2yx³/3 + C
        val integral = monomial.integrateCanonical(x, Flt64.zero)

        assertEquals(1, integral.monomials.size)
        assertTrue((integral.monomials.first().coefficient - Flt64(2.0) / Flt64(3.0)).abs() ls Flt64(1e-10))
        assertEquals(Int32(3), integral.monomials.first().powers[x])
        assertEquals(Int32(1), integral.monomials.first().powers[y])
    }

    @Test
    fun testIntegrateCanonicalPolynomial() {
        val x = TestSymbol("x")
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64(3.0), mapOf(x to Int32(2))),
                CanonicalMonomial<Flt64>(Flt64(2.0), mapOf(x to Int32(1)))
            ),
            constant = Flt64(5.0)
        )

        // ∫(3x² + 2x + 5) dx = x³ + x² + 5x + C
        val integral = polynomial.integrateCanonical(x, Flt64.zero)

        assertEquals(3, integral.monomials.size)

        // 检查各项系数和指数
        val xCubed = integral.monomials.find { it.powers[x] == Int32(3) }
        assertTrue(xCubed != null)
        assertEquals(Flt64.one, xCubed!!.coefficient)

        val xSquared = integral.monomials.find { it.powers[x] == Int32(2) && it.powers.size == 1 }
        assertTrue(xSquared != null)
        assertEquals(Flt64.one, xSquared!!.coefficient)

        val xTerm = integral.monomials.find { it.powers[x] == Int32(1) && it.powers.size == 1 }
        assertTrue(xTerm != null)
        assertEquals(Flt64(5.0), xTerm!!.coefficient)
    }

    @Test
    fun testIntegrateCanonicalPolynomialConstant() {
        val x = TestSymbol("x")
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = emptyList(),
            constant = Flt64(7.0)
        )

        // ∫(7) dx = 7x + C
        val integral = polynomial.integrateCanonical(x, Flt64.zero)

        assertEquals(1, integral.monomials.size)
        assertEquals(Flt64(7.0), integral.monomials.first().coefficient)
        assertEquals(Int32(1), integral.monomials.first().powers[x])
    }

    // ============================================================================
    // Integration-Derivative Roundtrip Tests
    // ============================================================================

    @Test
    fun testLinearIntegrationDerivativeRoundtrip() {
        val x = TestSymbol("x")
        val original = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64(2.0), x)
            ),
            constant = Flt64(3.0)
        )

        // ∫(2x + 3) dx = x² + 3x + C
        val integral = original.integrateLinear(x, Flt64.zero)

        // d/dx(x² + 3x) = 2x + 3
        val derivative = integral.derivativeQuadratic(x, Flt64.zero)

        // 检查结果与原多项式相同（忽略积分常数）
        assertEquals(1, derivative.monomials.size)
        assertEquals(Flt64(2.0), derivative.monomials.first().coefficient)
        assertEquals(x, derivative.monomials.first().symbol)
        assertEquals(Flt64(3.0), derivative.constant)
    }

    @Test
    fun testQuadraticIntegrationDerivativeRoundtrip() {
        val x = TestSymbol("x")
        val original = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64(3.0), x, x),
                QuadraticMonomial<Flt64>(Flt64(2.0), x, null)
            ),
            constant = Flt64(5.0)
        )

        // ∫(3x² + 2x + 5) dx = x³ + x² + 5x + C
        val integral = original.integrateQuadratic(x, Flt64.zero)

        // d/dx(x³ + x² + 5x) = 3x² + 2x + 5
        val derivative = integral.derivativeCanonical(x, Flt64.zero).toQuadraticPolynomialOrNull()

        assertTrue(derivative != null)

        // 验证导数与原多项式相同
        val xSquared = derivative!!.monomials.find { it.symbol1 == x && it.symbol2 == x }
        assertTrue(xSquared != null)
        assertEquals(Flt64(3.0), xSquared!!.coefficient)

        val xTerm = derivative.monomials.find { it.symbol1 == x && it.symbol2 == null }
        assertTrue(xTerm != null)
        assertEquals(Flt64(2.0), xTerm!!.coefficient)

        assertEquals(Flt64(5.0), derivative.constant)
    }

    @Test
    fun testCanonicalIntegrationDerivativeRoundtrip() {
        val x = TestSymbol("x")
        val original = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64(4.0), mapOf(x to Int32(3))),
                CanonicalMonomial<Flt64>(Flt64(3.0), mapOf(x to Int32(2)))
            ),
            constant = Flt64(2.0)
        )

        // ∫(4x³ + 3x² + 2) dx = x⁴ + x³ + 2x + C
        val integral = original.integrateCanonical(x, Flt64.zero)

        // d/dx(x⁴ + x³ + 2x) = 4x³ + 3x² + 2
        // 注意：微分后常数 2 可能作为空 powers 的单项式或 constant 存在
        val derivative = integral.derivativeCanonical(x, Flt64.zero)

        // 检查 x³ 项
        val xCubed = derivative.monomials.find { it.powers[x] == Int32(3) && it.powers.size == 1 }
        assertTrue(xCubed != null)
        assertEquals(Flt64(4.0), xCubed!!.coefficient)

        // 检查 x² 项
        val xSquared = derivative.monomials.find { it.powers[x] == Int32(2) && it.powers.size == 1 }
        assertTrue(xSquared != null)
        assertEquals(Flt64(3.0), xSquared!!.coefficient)

        // 检查常数项 2（可能是 monomial with empty powers 或 constant）
        val constFromMonomial = derivative.monomials.find { it.powers.isEmpty() }
        val constValue = constFromMonomial?.coefficient ?: derivative.constant
        assertEquals(Flt64(2.0), constValue)
    }

    // ============================================================================
    // Edge Cases
    // ============================================================================

    @Test
    fun testIntegrateZeroPolynomial() {
        val x = TestSymbol("x")
        val polynomial = LinearPolynomial<Flt64>(
            monomials = emptyList(),
            constant = Flt64.zero
        )

        // ∫(0) dx = C
        val integral = polynomial.integrateLinear(x, Flt64(10.0))

        assertEquals(0, integral.monomials.size)
        assertEquals(Flt64(10.0), integral.constant)
    }

    @Test
    fun testIntegrateConstantOnly() {
        val x = TestSymbol("x")
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = emptyList(),
            constant = Flt64(5.0)
        )

        // ∫(5) dx = 5x + C
        val integral = polynomial.integrateCanonical(x, Flt64.zero)

        assertEquals(1, integral.monomials.size)
        assertEquals(Flt64(5.0), integral.monomials.first().coefficient)
        assertEquals(Int32(1), integral.monomials.first().powers[x])
    }
}
