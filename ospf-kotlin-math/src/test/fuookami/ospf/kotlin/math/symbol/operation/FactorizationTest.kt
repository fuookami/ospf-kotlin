package fuookami.ospf.kotlin.math.symbol.operation

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * Factorization 测试类
 * Factorization test class
 *
 * 测试多项式因式分解和求根功能。
 * Tests polynomial factorization and root finding functionality.
 */
class FactorizationTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    // ============================================================================
    // 系数提取测试 / Coefficient extraction tests
    // ============================================================================

    @Test
    fun testExtractUnivariateCoefficientsSimple() {
        val x = TestSymbol("x")

        // x² - 3x + 2
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64.one, x, x),           // x²
                QuadraticMonomial(Flt64(-3.0), x, null)       // -3x
            ),
            constant = Flt64(2.0)
        )

        val coeffs = polynomial.extractUnivariateCoefficients()!!
        assertTrue((coeffs.a - Flt64.one).abs() ls Flt64(1e-10))
        assertTrue((coeffs.b - Flt64(-3.0)).abs() ls Flt64(1e-10))
        assertTrue((coeffs.c - Flt64(2.0)).abs() ls Flt64(1e-10))
        assertEquals(x, coeffs.symbol)
    }

    @Test
    fun testExtractUnivariateCoefficientsWithLeadingCoefficient() {
        val x = TestSymbol("x")

        // 2x² - 8x + 6
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64(2.0), x, x),          // 2x²
                QuadraticMonomial(Flt64(-8.0), x, null)       // -8x
            ),
            constant = Flt64(6.0)
        )

        val coeffs = polynomial.extractUnivariateCoefficients()!!
        assertTrue((coeffs.a - Flt64(2.0)).abs() ls Flt64(1e-10))
        assertTrue((coeffs.b - Flt64(-8.0)).abs() ls Flt64(1e-10))
        assertTrue((coeffs.c - Flt64(6.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testExtractUnivariateCoefficientsMultivariateReturnsNull() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        // x² + xy (多变量)
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64.one, x, x),
                QuadraticMonomial(Flt64.one, x, y)
            ),
            constant = Flt64.zero
        )

        assertNull(polynomial.extractUnivariateCoefficients())
    }

    // ============================================================================
    // 求根测试 / Root finding tests
    // ============================================================================

    @Test
    fun testSolveQuadraticTwoRealRoots() {
        val x = TestSymbol("x")

        // x² - 5x + 6 = (x - 2)(x - 3), roots: 2, 3
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64.one, x, x),
                QuadraticMonomial(Flt64(-5.0), x, null)
            ),
            constant = Flt64(6.0)
        )

        val roots = polynomial.solve()!!
        assertTrue(roots.isReal)
        assertEquals(2, roots.roots.size)

        val sortedRoots = roots.roots.sortedBy { it.value }
        assertTrue((sortedRoots[0] - Flt64(2.0)).abs() ls Flt64(1e-10))
        assertTrue((sortedRoots[1] - Flt64(3.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testSolveQuadraticOneRepeatedRoot() {
        val x = TestSymbol("x")

        // x² - 4x + 4 = (x - 2)², root: 2 (repeated)
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64.one, x, x),
                QuadraticMonomial(Flt64(-4.0), x, null)
            ),
            constant = Flt64(4.0)
        )

        val roots = polynomial.solve()!!
        assertTrue(roots.isReal)
        assertEquals(1, roots.roots.size)
        assertTrue((roots.roots[0] - Flt64(2.0)).abs() ls Flt64(1e-10))
        assertTrue(roots.discriminant eq Flt64.zero)
    }

    @Test
    fun testSolveQuadraticNoRealRoots() {
        val x = TestSymbol("x")

        // x² + 1 = 0, 无实根
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64.one, x, x)
            ),
            constant = Flt64.one
        )

        val roots = polynomial.solve()!!
        assertFalse(roots.isReal)
        assertTrue(roots.roots.isEmpty())
        assertTrue(roots.discriminant ls Flt64.zero)
    }

    @Test
    fun testSolveQuadraticLinearCase() {
        val x = TestSymbol("x")

        // 2x - 6 = 0, x = 3 (退化为线性)
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64(2.0), x, null)
            ),
            constant = Flt64(-6.0)
        )

        val roots = polynomial.solve()!!
        assertTrue(roots.isReal)
        assertEquals(1, roots.roots.size)
        assertTrue((roots.roots[0] - Flt64(3.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testSolveQuadraticNegativeDiscriminant() {
        val x = TestSymbol("x")

        // x² + x + 1 = 0, 判别式 = -3 < 0
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64.one, x, x),
                QuadraticMonomial(Flt64.one, x, null)
            ),
            constant = Flt64.one
        )

        val roots = polynomial.solve()!!
        assertFalse(roots.isReal)
        assertTrue(roots.roots.isEmpty())
    }

    // ============================================================================
    // 因式分解测试 / Factorization tests
    // ============================================================================

    @Test
    fun testFactorizeQuadraticTwoFactors() {
        val x = TestSymbol("x")

        // x² - 5x + 6 = (x - 2)(x - 3)
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64.one, x, x),
                QuadraticMonomial(Flt64(-5.0), x, null)
            ),
            constant = Flt64(6.0)
        )

        val factorization = polynomial.factorize()!!
        assertTrue((factorization.leadingCoefficient - Flt64.one).abs() ls Flt64(1e-10))
        assertEquals(2, factorization.factors.size)

        val sortedRoots = factorization.factors.map { it.root }.sortedBy { it.value }
        assertTrue((sortedRoots[0] - Flt64(2.0)).abs() ls Flt64(1e-10))
        assertTrue((sortedRoots[1] - Flt64(3.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testFactorizeQuadraticWithLeadingCoefficient() {
        val x = TestSymbol("x")

        // 2x² - 8x + 6 = 2(x - 1)(x - 3)
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64(2.0), x, x),
                QuadraticMonomial(Flt64(-8.0), x, null)
            ),
            constant = Flt64(6.0)
        )

        val factorization = polynomial.factorize()!!
        assertTrue((factorization.leadingCoefficient - Flt64(2.0)).abs() ls Flt64(1e-10))
        assertEquals(2, factorization.factors.size)
    }

    @Test
    fun testFactorizeQuadraticNoRealRoots() {
        val x = TestSymbol("x")

        // x² + 1 无法在实数域因式分解
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64.one, x, x)
            ),
            constant = Flt64.one
        )

        assertNull(polynomial.factorize())
    }

    @Test
    fun testFactorizeQuadraticRepeatedRoot() {
        val x = TestSymbol("x")

        // x² - 4x + 4 = (x - 2)²
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64.one, x, x),
                QuadraticMonomial(Flt64(-4.0), x, null)
            ),
            constant = Flt64(4.0)
        )

        val factorization = polynomial.factorize()!!
        assertEquals(1, factorization.factors.size)
        assertTrue((factorization.factors[0].root - Flt64(2.0)).abs() ls Flt64(1e-10))
    }

    // ============================================================================
    // 因式展开测试 / Factor expansion tests
    // ============================================================================

    @Test
    fun testExpandFactorizationRoundTrip() {
        val x = TestSymbol("x")

        // 原始多项式: x² - 5x + 6
        val original = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64.one, x, x),
                QuadraticMonomial(Flt64(-5.0), x, null)
            ),
            constant = Flt64(6.0)
        )

        val factorization = original.factorize()!!
        val expanded = factorization.expand()

        val expandedCoeffs = expanded.extractUnivariateCoefficients()!!
        val originalCoeffs = original.extractUnivariateCoefficients()!!

        assertTrue((expandedCoeffs.a - originalCoeffs.a).abs() ls Flt64(1e-10))
        assertTrue((expandedCoeffs.b - originalCoeffs.b).abs() ls Flt64(1e-10))
        assertTrue((expandedCoeffs.c - originalCoeffs.c).abs() ls Flt64(1e-10))
    }

    @Test
    fun testExpandLinearFactor() {
        val x = TestSymbol("x")

        // 2x - 6 = 2(x - 3)
        val original = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64(2.0), x, null)
            ),
            constant = Flt64(-6.0)
        )

        val factorization = original.factorize()!!
        val expanded = factorization.expand()
        val expandedCoeffs = expanded.extractUnivariateCoefficients()!!

        assertTrue((expandedCoeffs.b - Flt64(2.0)).abs() ls Flt64(1e-10))
        assertTrue((expandedCoeffs.c - Flt64(-6.0)).abs() ls Flt64(1e-10))
    }

    // ============================================================================
    // 边界情况测试 / Edge case tests
    // ============================================================================

    @Test
    fun testSolveZeroPolynomial() {
        val x = TestSymbol("x")

        // 0 = 0 (所有系数为零)
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = emptyList(),
            constant = Flt64.zero
        )

        val roots = polynomial.solve()!!
        assertTrue(roots.isReal)
        assertTrue(roots.roots.isEmpty())
    }

    @Test
    fun testSolveConstantPolynomial() {
        val x = TestSymbol("x")

        // 5 = 0 (无常数解)
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = emptyList(),
            constant = Flt64(5.0)
        )

        val roots = polynomial.solve()!!
        assertTrue(roots.isReal)
        assertTrue(roots.roots.isEmpty())
    }

    @Test
    fun testFactorizeNegativeRoots() {
        val x = TestSymbol("x")

        // x² + 5x + 6 = (x + 2)(x + 3), roots: -2, -3
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64.one, x, x),
                QuadraticMonomial(Flt64(5.0), x, null)
            ),
            constant = Flt64(6.0)
        )

        val roots = polynomial.solve()!!
        assertTrue(roots.isReal)
        assertEquals(2, roots.roots.size)

        val sortedRoots = roots.roots.sortedBy { it.value }
        assertTrue((sortedRoots[0] - Flt64(-3.0)).abs() ls Flt64(1e-10))
        assertTrue((sortedRoots[1] - Flt64(-2.0)).abs() ls Flt64(1e-10))
    }
}
