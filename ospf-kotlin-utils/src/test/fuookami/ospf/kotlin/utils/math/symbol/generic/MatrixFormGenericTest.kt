package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MatrixFormGenericTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun genericLinearMatrixFormShouldRespectOrder() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = GenericLinearPolynomial(
            monomials = listOf(
                GenericLinearMonomial(Int64(2L), x),
                GenericLinearMonomial(Int64(3L), y),
                GenericLinearMonomial(Int64.one, x)
            ),
            constant = Int64(4L)
        )

        val form = polynomial.toGenericMatrixForm(
            order = listOf(y, x),
            zero = Int64.zero
        )

        assertEquals(listOf(Int64(3L), Int64(3L)), form.c)
        assertEquals(Int64(4L), form.d)
        assertEquals(listOf(y, x), form.order)
    }

    @Test
    fun genericQuadraticMatrixFormShouldUseSplitStrategy() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = GenericQuadraticPolynomial(
            monomials = listOf(
                GenericQuadraticMonomial(Int64(4L), x, x),
                GenericQuadraticMonomial(Int64(6L), x, y),
                GenericQuadraticMonomial(Int64(5L), y, null)
            ),
            constant = Int64(7L)
        )

        val form = polynomial.toGenericMatrixForm(
            order = listOf(x, y),
            zero = Int64.zero,
            splitOffDiagonal = { coefficient ->
                val half = coefficient / Int64.two
                half to half
            }
        )

        assertEquals(Int64(4L), form.q[0][0])
        assertEquals(Int64(3L), form.q[0][1])
        assertEquals(Int64(3L), form.q[1][0])
        assertEquals(Int64.zero, form.q[1][1])
        assertEquals(listOf(Int64.zero, Int64(5L)), form.c)
        assertEquals(Int64(7L), form.d)
    }

    @Test
    fun flt64FastPathShouldMatchGenericMatrixForm() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = GenericQuadraticPolynomial(
            monomials = listOf(
                GenericQuadraticMonomial(Flt64(3.0), x, x),
                GenericQuadraticMonomial(Flt64.two, x, y),
                GenericQuadraticMonomial(Flt64(4.0), x, null)
            ),
            constant = Flt64(5.0)
        )

        val (q, c) = polynomial.toMatrixPair(order = listOf(x, y))
        val genericForm = polynomial.toGenericMatrixForm(
            order = listOf(x, y),
            zero = Flt64.zero,
            splitOffDiagonal = { coefficient ->
                val half = coefficient / Flt64.two
                half to half
            }
        )

        assertEquals(genericForm.q[0][0].toDouble(), q[0][0])
        assertEquals(genericForm.q[0][1].toDouble(), q[0][1])
        assertEquals(genericForm.q[1][0].toDouble(), q[1][0])
        assertEquals(genericForm.q[1][1].toDouble(), q[1][1])
        assertEquals(genericForm.c[0].toDouble(), c[0])
        assertEquals(genericForm.c[1].toDouble(), c[1])
    }
}
