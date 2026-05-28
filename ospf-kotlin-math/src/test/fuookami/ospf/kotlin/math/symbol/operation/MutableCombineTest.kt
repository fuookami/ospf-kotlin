package fuookami.ospf.kotlin.math.symbol.operation

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class MutableCombineTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    // ========================================================================
    // MutableLinearPolynomial Tests
    // ========================================================================

    @Test
    fun testMutableLinearCombineTerms() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        val mutable = MutableLinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(Flt64(3.0), x),
                LinearMonomial(Flt64(-1.0), y),
                LinearMonomial(Flt64(1.0), y)
            ),
            constant = Flt64(5.0)
        )

        mutable.combineTerms(Flt64.zero)

        assertEquals(1, mutable.monomials.size)
        assertEquals(x, mutable.monomials[0].symbol)
        assertTrue(mutable.monomials[0].coefficient == Flt64(5.0))
        assertTrue(mutable.constant == Flt64(5.0))
    }

    @Test
    fun testMutableLinearCombineTermsDropsZeroCoefficients() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        val mutable = MutableLinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(Flt64(-2.0), x),  // cancels out
                LinearMonomial(Flt64(1.0), y)
            ),
            constant = Flt64.zero
        )

        mutable.combineTerms(Flt64.zero)

        assertEquals(1, mutable.monomials.size)
        assertEquals(y, mutable.monomials[0].symbol)
    }

    @Test
    fun testMutableLinearAddAssignAndCombine() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        val mutable = MutableLinearPolynomial<Flt64>(
            monomials = listOf(LinearMonomial(Flt64(1.0), x)),
            constant = Flt64.zero
        )

        val poly = LinearPolynomial<Flt64>(
            monomials = listOf(LinearMonomial(Flt64(2.0), x)),
            constant = Flt64(3.0)
        )

        mutable.addAssignAndCombine(poly, Flt64.zero)

        assertEquals(1, mutable.monomials.size)
        assertTrue(mutable.monomials[0].coefficient == Flt64(3.0))
        assertTrue(mutable.constant == Flt64(3.0))
    }

    @Test
    fun testFastSumPattern() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        // FastSum pattern: accumulate without combining, then combine once
        // Use fromConstant() for explicit zero value (reflection not required)
        val result = MutableLinearPolynomial.fromConstant(Flt64.zero)

        val polys = listOf(
            LinearPolynomial<Flt64>(
                monomials = listOf(LinearMonomial(Flt64(1.0), x)),
                constant = Flt64(1.0)
            ),
            LinearPolynomial<Flt64>(
                monomials = listOf(LinearMonomial(Flt64(2.0), x)),
                constant = Flt64(2.0)
            ),
            LinearPolynomial<Flt64>(
                monomials = listOf(LinearMonomial(Flt64(3.0), y)),
                constant = Flt64(3.0)
            )
        )

        for (poly in polys) {
            result += poly  // Fast accumulation
        }

        // Before combine: 2 monomials for x, 1 for y
        assertEquals(3, result.monomials.size)

        result.combineTerms(Flt64.zero)

        // After combine: 1 monomial for x (1+2=3), 1 for y (3)
        assertEquals(2, result.monomials.size)
        assertTrue(result.constant == Flt64(6.0))
    }

    // ========================================================================
    // MutableQuadraticPolynomial Tests
    // ========================================================================

    @Test
    fun testMutableQuadraticCombineTerms() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        val mutable = MutableQuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64(2.0), x, y),
                QuadraticMonomial(Flt64(3.0), y, x),  // Same as xy, normalized
                QuadraticMonomial(Flt64(1.0), x, null)
            ),
            constant = Flt64.zero
        )

        mutable.combineTerms(Flt64.zero)

        assertEquals(2, mutable.monomials.size)
    }

    @Test
    fun testMutableQuadraticAddAssignAndCombine() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        val mutable = MutableQuadraticPolynomial<Flt64>(
            monomials = listOf(QuadraticMonomial(Flt64(1.0), x, null)),
            constant = Flt64.zero
        )

        val poly = QuadraticPolynomial<Flt64>(
            monomials = listOf(QuadraticMonomial(Flt64(2.0), x, y)),
            constant = Flt64(5.0)
        )

        mutable.addAssignAndCombine(poly, Flt64.zero)

        assertEquals(2, mutable.monomials.size)
        assertTrue(mutable.constant == Flt64(5.0))
    }

    // ========================================================================
    // MutableCanonicalPolynomial Tests
    // ========================================================================

    @Test
    fun testMutableCanonicalCombineTerms() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        val mutable = MutableCanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial(Flt64(2.0), listOf(x, x)),  // 2x^2
                CanonicalMonomial(Flt64(3.0), listOf(x, x)),  // 3x^2
                CanonicalMonomial(Flt64(1.0), listOf(y)),     // y
                CanonicalMonomial(Flt64(-1.0), listOf(y))     // -y
            ),
            constant = Flt64.zero
        )

        mutable.combineTerms(Flt64.zero)

        assertEquals(1, mutable.monomials.size)
        assertEquals(listOf(x, x), mutable.monomials[0].factors)
        assertTrue(mutable.monomials[0].coefficient == Flt64(5.0))
    }

    @Test
    fun testMutableCanonicalAddAssignAndCombine() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        val mutable = MutableCanonicalPolynomial<Flt64>(
            monomials = listOf(CanonicalMonomial(Flt64(1.0), listOf(x))),
            constant = Flt64.zero
        )

        val poly = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial(Flt64(2.0), listOf(x)),
                CanonicalMonomial(Flt64(3.0), listOf(y, y))
            ),
            constant = Flt64(7.0)
        )

        mutable.addAssignAndCombine(poly, Flt64.zero)

        assertEquals(2, mutable.monomials.size)
        assertTrue(mutable.constant == Flt64(7.0))
    }

    @Test
    fun testCanonicalFastSumPattern() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")

        // Use fromConstant() for explicit zero value (reflection not required)
        val result = MutableCanonicalPolynomial.fromConstant(Flt64.zero)

        val polys = listOf(
            CanonicalPolynomial<Flt64>(
                monomials = listOf(CanonicalMonomial(Flt64(1.0), listOf(x, x))),
                constant = Flt64.zero
            ),
            CanonicalPolynomial<Flt64>(
                monomials = listOf(CanonicalMonomial(Flt64(2.0), listOf(x, x))),
                constant = Flt64.zero
            ),
            CanonicalPolynomial<Flt64>(
                monomials = listOf(CanonicalMonomial(Flt64(1.0), listOf(y))),
                constant = Flt64.zero
            )
        )

        for (poly in polys) {
            result += poly
        }

        assertEquals(3, result.monomials.size)

        result.combineTerms(Flt64.zero)

        assertEquals(2, result.monomials.size)
    }
}
