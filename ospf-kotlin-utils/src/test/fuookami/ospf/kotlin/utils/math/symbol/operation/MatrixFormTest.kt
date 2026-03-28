package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MatrixFormTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun matrixFormShouldUseSymmetricQAndLinearVector() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64(3.0), x, x),
                QuadraticMonomial<Flt64>(Flt64.two, x, y),
                QuadraticMonomial<Flt64>(Flt64(4.0), x, null)
            ),
            constant = Flt64(5.0)
        )

        val matrixForm = polynomial.toMatrixForm(order = listOf(x, y))

        assertEquals(3.0, matrixForm.q[0][0])
        assertEquals(1.0, matrixForm.q[0][1])
        assertEquals(1.0, matrixForm.q[1][0])
        assertEquals(0.0, matrixForm.q[1][1])
        assertEquals(4.0, matrixForm.c[0])
        assertEquals(0.0, matrixForm.c[1])
        assertEquals(Flt64(5.0), matrixForm.d)
    }

    @Test
    fun linearMatrixFormShouldRoundTrip() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64.two, x),
                LinearMonomial<Flt64>(Flt64(-3.0), y)
            ),
            constant = Flt64(7.0)
        )

        val matrixForm = polynomial.toMatrixForm(order = listOf(y, x))
        val rebuilt = linearPolynomialFromMatrixForm(matrixForm).combineTerms()

        assertEquals(-3.0, matrixForm.c[0])
        assertEquals(2.0, matrixForm.c[1])
        assertEquals(Flt64(7.0), matrixForm.d)
        assertEquals(polynomial.constant, rebuilt.constant)
        assertEquals(polynomial.monomials.toSet(), rebuilt.monomials.toSet())
    }

    @Test
    fun quadraticMatrixFormShouldRoundTrip() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64(3.0), x, x),
                QuadraticMonomial<Flt64>(Flt64.two, x, y),
                QuadraticMonomial<Flt64>(Flt64(4.0), x, null),
                QuadraticMonomial<Flt64>(Flt64(-1.0), y, null)
            ),
            constant = Flt64(5.0)
        )

        val matrixForm = polynomial.toMatrixForm(order = listOf(x, y))
        val rebuilt = quadraticPolynomialFromMatrixForm(matrixForm).combineTerms()

        assertEquals(polynomial.constant, rebuilt.constant)
        assertEquals(polynomial.monomials.toSet(), rebuilt.monomials.toSet())
    }

    @Test
    fun matrixFormShouldFailWhenOrderMissesSymbol() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64.one, x, y)
            ),
            constant = Flt64.zero
        )

        assertFailsWith<IllegalArgumentException> {
            polynomial.toMatrixForm(order = listOf(x))
        }
    }

    @Test
    fun matrixFormShouldFailWhenOrderContainsDuplicatedSymbols() {
        val x = TestSymbol("x")
        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(LinearMonomial<Flt64>(Flt64.one, x)),
            constant = Flt64.zero
        )

        assertFailsWith<IllegalArgumentException> {
            polynomial.toMatrixForm(order = listOf(x, x))
        }
    }

    @Test
    fun quadraticFromMatrixFormShouldFailWhenDimensionsMismatch() {
        val x = TestSymbol("x")

        assertFailsWith<IllegalArgumentException> {
            quadraticPolynomialFromMatrixForm(
                q = arrayOf(doubleArrayOf(1.0, 2.0)),
                c = doubleArrayOf(1.0),
                d = Flt64.zero,
                order = listOf(x)
            )
        }
    }

    @Test
    fun canonicalMatrixFormShouldMatchQuadraticMatrixForm() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64(3.0), listOf(x, x)),
                CanonicalMonomial<Flt64>(Flt64.two, listOf(y, x)),
                CanonicalMonomial<Flt64>(Flt64(4.0), listOf(x))
            ),
            constant = Flt64(5.0)
        )

        val matrixForm = polynomial.toMatrixForm(order = listOf(x, y))

        assertEquals(3.0, matrixForm.q[0][0])
        assertEquals(1.0, matrixForm.q[0][1])
        assertEquals(1.0, matrixForm.q[1][0])
        assertEquals(0.0, matrixForm.q[1][1])
        assertEquals(4.0, matrixForm.c[0])
        assertEquals(0.0, matrixForm.c[1])
        assertEquals(Flt64(5.0), matrixForm.d)
    }

    @Test
    fun hessianShouldMatchTwiceOfMatrixQ() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")
        val order = listOf(x, y, z)
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64(3.0), x, x),
                QuadraticMonomial<Flt64>(Flt64(2.0), x, y),
                QuadraticMonomial<Flt64>(Flt64(-4.0), y, z),
                QuadraticMonomial<Flt64>(Flt64(5.0), z, z),
                QuadraticMonomial<Flt64>(Flt64.one, x, null)
            ),
            constant = Flt64(6.0)
        )

        val matrixForm = polynomial.toMatrixForm(order = order)
        val hessian = polynomial.hessian(order = order)

        for (i in order.indices) {
            for (j in order.indices) {
                assertEquals(2.0 * matrixForm.q[i][j], hessian[i][j])
            }
        }
    }

    @Test
    fun canonicalMatrixFormShouldFailWhenPolynomialContainsHigherOrderTerm() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64.one, listOf(x, y, z))
            ),
            constant = Flt64.zero
        )

        assertFailsWith<IllegalArgumentException> {
            polynomial.toMatrixForm(order = listOf(x, y, z))
        }
    }
}



