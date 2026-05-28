package fuookami.ospf.kotlin.math.symbol.operation

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*

class MatrixFormConversionTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun linearMatrixFormShouldRoundTrip() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = LinearPolynomial<Int64>(
            monomials = listOf(
                LinearMonomial(Int64(2L), x),
                LinearMonomial(Int64(-3L), y),
                LinearMonomial(Int64.one, x)
            ),
            constant = Int64(5L)
        )

        val form = polynomial.toMatrixForm(
            order = listOf(y, x),
            zero = Int64.zero
        )
        val rebuilt = linearPolynomialFromMatrixForm(
            form = form,
            zero = Int64.zero
        )
        val expected = polynomial.combineLinearTerms(Int64.zero)

        assertEquals(listOf(Int64(-3L), Int64(3L)), form.c)
        assertEquals(Int64(5L), form.d)
        assertEquals(expected.constant, rebuilt.constant)
        assertEquals(expected.monomials.toSet(), rebuilt.monomials.toSet())
    }

    @Test
    fun quadraticMatrixFormShouldRoundTrip() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = QuadraticPolynomial<Int64>(
            monomials = listOf(
                QuadraticMonomial(Int64(4L), x, x),
                QuadraticMonomial(Int64(6L), x, y),
                QuadraticMonomial(Int64(8L), y, y),
                QuadraticMonomial(Int64(5L), x, null)
            ),
            constant = Int64(7L)
        )

        val form = polynomial.toMatrixForm(
            order = listOf(x, y),
            zero = Int64.zero,
            splitOffDiagonal = { coefficient ->
                val half = coefficient / Int64.two
                half to half
            }
        )
        val rebuilt = quadraticPolynomialFromMatrixForm(
            form = form,
            zero = Int64.zero
        )
        val expected = polynomial.combineQuadraticTerms(Int64.zero)

        assertEquals(Int64(4L), form.q[0][0])
        assertEquals(Int64(3L), form.q[0][1])
        assertEquals(Int64(3L), form.q[1][0])
        assertEquals(Int64(8L), form.q[1][1])
        assertEquals(listOf(Int64(5L), Int64.zero), form.c)
        assertEquals(Int64(7L), form.d)
        assertEquals(expected.constant, rebuilt.constant)
        assertEquals(expected.monomials.toSet(), rebuilt.monomials.toSet())
    }

    @Test
    fun flt64QuickPathShouldMatchMatrixFormPath() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val order = listOf(x, y)
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64(3.0), x, x),
                QuadraticMonomial<Flt64>(Flt64(2.0), x, y),
                QuadraticMonomial<Flt64>(Flt64(4.0), x, null)
            ),
            constant = Flt64(5.0)
        )

        val quickPath = polynomial.toFlt64MatrixForm(order = order)
        val matrixPath = polynomial.toMatrixForm(
            order = order,
            zero = Flt64.zero,
            splitOffDiagonal = { coefficient ->
                val half = coefficient / Flt64.two
                half to half
            }
        )

        for (i in order.indices) {
            for (j in order.indices) {
                assertEquals(matrixPath.q[i][j].toDouble(), quickPath.q[i][j], 1e-10)
            }
            assertEquals(matrixPath.c[i].toDouble(), quickPath.c[i], 1e-10)
        }
        assertEquals(matrixPath.d, quickPath.d)
    }

    @Test
    fun quadraticMatrixFormShouldValidateDimensions() {
        val x = TestSymbol("x")

        assertFailsWith<IllegalArgumentException> {
            quadraticPolynomialFromMatrixForm(
                q = listOf(listOf(Int64.one)),
                c = listOf(Int64.one, Int64.two),
                d = Int64.zero,
                order = listOf(x),
                zero = Int64.zero
            )
        }
    }

    @Test
    fun linearMatrixFormShouldValidateDimensions() {
        val x = TestSymbol("x")

        assertFailsWith<IllegalArgumentException> {
            linearPolynomialFromMatrixForm(
                c = listOf(Int64.one, Int64.two),
                d = Int64.zero,
                order = listOf(x),
                zero = Int64.zero
            )
        }
    }

    @Test
    fun matrixFormShouldRejectDuplicatedOrder() {
        val x = TestSymbol("x")
        val polynomial = LinearPolynomial<Int64>(
            monomials = listOf(
                LinearMonomial(Int64.one, x)
            ),
            constant = Int64.zero
        )

        assertFailsWith<IllegalArgumentException> {
            polynomial.toMatrixForm(
                order = listOf(x, x),
                zero = Int64.zero
            )
        }
    }

    @Test
    fun quadraticMatrixFormRoundTripShouldPreserveEvaluation() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")
        val order = listOf(x, y, z)
        val values = listOf(Flt64(2.0), Flt64(3.0), Flt64(4.0))
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64(2.0), x, y),
                QuadraticMonomial(Flt64(3.0), x, x),
                QuadraticMonomial(Flt64(-4.0), y, z),
                QuadraticMonomial(Flt64(5.0), z, z),
                QuadraticMonomial(Flt64(7.0), x, null)
            ),
            constant = Flt64(11.0)
        ).combineQuadraticTerms(Flt64.zero)

        val form = polynomial.toMatrixForm(
            order = order,
            zero = Flt64.zero,
            splitOffDiagonal = { coefficient ->
                val half = coefficient / Flt64.two
                half to half
            }
        )
        val rebuilt = quadraticPolynomialFromMatrixForm(
            form = form,
            zero = Flt64.zero
        )

        val originValue = polynomial.evaluateQuadraticOrdered(order = order, values = values)
        val rebuiltValue = rebuilt.evaluateQuadraticOrdered(order = order, values = values)

        assertEquals(originValue.toDouble(), rebuiltValue.toDouble(), 1e-10)
        assertEquals(polynomial.constant, rebuilt.constant)
    }

    @Test
    fun matrixFormAndFlt64HessianShouldMatchTwiceMatrixQ() {
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
                QuadraticMonomial<Flt64>(Flt64(7.0), x, null)
            ),
            constant = Flt64(11.0)
        )

        val matrix = polynomial.combineQuadraticTerms(Flt64.zero)
        val matrixForm = matrix.toMatrixForm(
            order = order,
            zero = Flt64.zero,
            splitOffDiagonal = { coefficient ->
                val half = coefficient / Flt64.two
                half to half
            }
        )
        val hessianFromFlt64 = polynomial.hessian(order = order)

        for (i in order.indices) {
            for (j in order.indices) {
                val expected = 2.0 * matrixForm.q[i][j].toDouble()
                assertEquals(expected, hessianFromFlt64[i][j], 1e-10)
            }
        }
    }

    @Test
    fun canonicalMatrixFormShouldRoundTripThroughQuadratic() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val canonical = CanonicalPolynomial<Int64>(
            monomials = listOf(
                CanonicalMonomial(Int64(4L), mapOf(x to Int64(2L).toInt32())),
                CanonicalMonomial(Int64(6L), mapOf(x to Int64(1L).toInt32(), y to Int64(1L).toInt32())),
                CanonicalMonomial(Int64(5L), mapOf(x to Int64(1L).toInt32()))
            ),
            constant = Int64(7L)
        )

        val form = canonical.toMatrixForm(
            order = listOf(x, y),
            zero = Int64.zero,
            splitOffDiagonal = { coefficient ->
                val half = coefficient / Int64.two
                half to half
            }
        )
        val rebuilt = quadraticPolynomialFromMatrixForm(
            form = form,
            zero = Int64.zero
        )

        assertEquals(Int64(4L), form.q[0][0])
        assertEquals(Int64(3L), form.q[0][1])
        assertEquals(Int64(3L), form.q[1][0])
        assertEquals(Int64.zero, form.q[1][1])
        assertEquals(listOf(Int64(5L), Int64.zero), form.c)
        assertEquals(Int64(7L), form.d)
        assertEquals(Int64(28L), rebuilt.evaluateQuadraticOrdered(listOf(x, y), listOf(Int64.one, Int64.two)))
    }

    @Test
    fun canonicalMatrixFormShouldRejectHigherDegreeTerm() {
        val x = TestSymbol("x")
        val cubicCanonical = CanonicalPolynomial<Int64>(
            monomials = listOf(
                CanonicalMonomial(Int64.one, mapOf(x to Int64(3L).toInt32()))
            ),
            constant = Int64.zero
        )

        assertFailsWith<IllegalArgumentException> {
            cubicCanonical.toMatrixForm(
                order = listOf(x),
                zero = Int64.zero,
                splitOffDiagonal = { coefficient ->
                    val half = coefficient / Int64.two
                    half to half
                }
            )
        }
    }
}
