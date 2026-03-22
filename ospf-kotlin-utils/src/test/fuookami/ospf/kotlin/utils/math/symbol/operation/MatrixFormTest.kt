package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
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
        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial(Flt64(3.0), x, x),
                QuadraticMonomial(Flt64.two, x, y),
                QuadraticMonomial(Flt64(4.0), x, null)
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
    fun matrixFormShouldFailWhenOrderMissesSymbol() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial(Flt64.one, x, y)
            )
        )

        assertFailsWith<IllegalArgumentException> {
            polynomial.toMatrixForm(order = listOf(x))
        }
    }

    @Test
    fun canonicalMatrixFormShouldMatchQuadraticMatrixForm() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = CanonicalPolynomial(
            monomials = listOf(
                CanonicalMonomial(Flt64(3.0), listOf(x, x)),
                CanonicalMonomial(Flt64.two, listOf(y, x)),
                CanonicalMonomial(Flt64(4.0), listOf(x))
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
    fun canonicalMatrixFormShouldFailWhenPolynomialContainsHigherOrderTerm() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")
        val polynomial = CanonicalPolynomial(
            monomials = listOf(
                CanonicalMonomial(Flt64.one, listOf(x, y, z))
            )
        )

        assertFailsWith<IllegalArgumentException> {
            polynomial.toMatrixForm(order = listOf(x, y, z))
        }
    }
}
