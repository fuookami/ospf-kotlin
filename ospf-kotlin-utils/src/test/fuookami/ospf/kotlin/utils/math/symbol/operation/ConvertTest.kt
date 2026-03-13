package fuookami.ospf.kotlin.utils.math.symbol.operation

import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.inequality.*
import fuookami.ospf.kotlin.utils.math.symbol.monomial.*
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.*

class ConvertTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun moveAllToLhsShouldGenerateZeroRightHandSide() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val inequality = LinearInequality(
            lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.two, x)),
                constant = Flt64.one
            ),
            rhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, y)),
                constant = Flt64(-2.0)
            ),
            comparison = Comparison.LE
        )

        val moved = inequality.moveAllToLhs()
        val coefficients = moved.lhs.monomials.associate { it.symbol to it.coefficient }

        assertEquals(Comparison.LE, moved.comparison)
        assertTrue(moved.rhs.constant == Flt64.zero)
        assertTrue(coefficients[x] == Flt64.two)
        assertTrue(coefficients[y] == Flt64(-1.0))
        assertTrue(moved.lhs.constant == Flt64(3.0))
    }

    @Test
    fun normalizeToLessEqualFormShouldReverseGreaterSide() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val inequality = LinearInequality(
            lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.two, x)),
                constant = Flt64.one
            ),
            rhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, y)),
                constant = Flt64(-2.0)
            ),
            comparison = Comparison.GE
        )

        val normalized = inequality.normalizeToLessEqualForm()
        val coefficients = normalized.lhs.monomials.associate { it.symbol to it.coefficient }

        assertEquals(Comparison.LE, normalized.comparison)
        assertTrue(normalized.rhs.constant == Flt64.zero)
        assertTrue(coefficients[x] == Flt64(-2.0))
        assertTrue(coefficients[y] == Flt64.one)
        assertTrue(normalized.lhs.constant == Flt64(-3.0))
    }

    @Test
    fun linearPolynomialShouldConvertToQuadraticAndBack() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val linear = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64.two, x),
                LinearMonomial(Flt64(-1.0), y)
            ),
            constant = Flt64(5.0)
        )

        val quadratic = linear.toQuadraticPolynomial()
        val convertedBack = quadratic.toLinearPolynomialOrNull()

        assertNotNull(convertedBack)
        assertEquals(linear.constant, convertedBack.constant)
        assertEquals(linear.monomials.toSet(), convertedBack.monomials.toSet())
    }

    @Test
    fun quadraticPolynomialWithQuadraticTermShouldNotConvertToLinear() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val quadratic = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial(Flt64.one, x, y)
            ),
            constant = Flt64.zero
        )

        assertNull(quadratic.toLinearPolynomialOrNull())
        val ret = quadratic.toLinearPolynomialRet()
        assertTrue(ret is Failed)
    }
}
