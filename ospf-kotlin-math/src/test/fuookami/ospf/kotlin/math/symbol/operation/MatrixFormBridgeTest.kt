package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.hessian
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.toMatrixForm
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MatrixFormBridgeTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun typedLinearMatrixBridgeShouldRoundTrip() {
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

        val form = polynomial.toTypedMatrixForm(
            order = listOf(y, x),
            zero = Int64.zero
        )
        val rebuilt = typedLinearPolynomialFromMatrixForm(
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
    fun typedQuadraticMatrixBridgeShouldRoundTrip() {
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

        val form = polynomial.toTypedMatrixForm(
            order = listOf(x, y),
            zero = Int64.zero,
            splitOffDiagonal = { coefficient ->
                val half = coefficient / Int64.two
                half to half
            }
        )
        val rebuilt = typedQuadraticPolynomialFromMatrixForm(
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
    fun flt64QuickPathShouldMatchTypedBridgePath() {
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

        val quickPath = polynomial.toMatrixForm(order = order)
        val typedPath = polynomial.toTypedMatrixForm(
            order = order,
            zero = Flt64.zero,
            splitOffDiagonal = { coefficient ->
                val half = coefficient / Flt64.two
                half to half
            }
        )

        for (i in order.indices) {
            for (j in order.indices) {
                assertEquals(typedPath.q[i][j].toDouble(), quickPath.q[i][j], 1e-10)
            }
            assertEquals(typedPath.c[i].toDouble(), quickPath.c[i], 1e-10)
        }
        assertEquals(typedPath.d, quickPath.d)
    }

    @Test
    fun typedQuadraticMatrixBridgeShouldValidateDimensions() {
        val x = TestSymbol("x")

        assertFailsWith<IllegalArgumentException> {
            typedQuadraticPolynomialFromMatrixForm(
                q = listOf(listOf(Int64.one)),
                c = listOf(Int64.one, Int64.two),
                d = Int64.zero,
                order = listOf(x),
                zero = Int64.zero
            )
        }
    }

    @Test
    fun typedLinearMatrixBridgeShouldValidateDimensions() {
        val x = TestSymbol("x")

        assertFailsWith<IllegalArgumentException> {
            typedLinearPolynomialFromMatrixForm(
                c = listOf(Int64.one, Int64.two),
                d = Int64.zero,
                order = listOf(x),
                zero = Int64.zero
            )
        }
    }

    @Test
    fun matrixBridgeShouldRejectDuplicatedOrder() {
        val x = TestSymbol("x")
        val polynomial = LinearPolynomial<Int64>(
            monomials = listOf(
                LinearMonomial(Int64.one, x)
            ),
            constant = Int64.zero
        )

        assertFailsWith<IllegalArgumentException> {
            polynomial.toTypedMatrixForm(
                order = listOf(x, x),
                zero = Int64.zero
            )
        }
    }

    @Test
    fun typedQuadraticBridgeRoundTripShouldPreserveEvaluation() {
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

        val form = polynomial.toTypedMatrixForm(
            order = order,
            zero = Flt64.zero,
            splitOffDiagonal = { coefficient ->
                val half = coefficient / Flt64.two
                half to half
            }
        )
        val rebuilt = typedQuadraticPolynomialFromMatrixForm(
            form = form,
            zero = Flt64.zero
        )

        val originValue = polynomial.evaluateQuadraticOrdered(order = order, values = values)
        val rebuiltValue = rebuilt.evaluateQuadraticOrdered(order = order, values = values)

        assertEquals(originValue.toDouble(), rebuiltValue.toDouble(), 1e-10)
        assertEquals(polynomial.constant, rebuilt.constant)
    }

    @Test
    fun typedAndFlt64HessianShouldMatchTwiceMatrixQ() {
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

        val typed = polynomial.combineQuadraticTerms(Flt64.zero)
        val typedForm = typed.toTypedMatrixForm(
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
                val expected = 2.0 * typedForm.q[i][j].toDouble()
                assertEquals(expected, hessianFromFlt64[i][j], 1e-10)
            }
        }
    }

    @Test
    fun typedCanonicalMatrixBridgeShouldRoundTripThroughQuadratic() {
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

        val form = canonical.toTypedMatrixForm(
            order = listOf(x, y),
            zero = Int64.zero,
            splitOffDiagonal = { coefficient ->
                val half = coefficient / Int64.two
                half to half
            }
        )
        val rebuilt = typedQuadraticPolynomialFromMatrixForm(
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
    fun typedCanonicalMatrixBridgeShouldRejectHigherDegreeTerm() {
        val x = TestSymbol("x")
        val cubicCanonical = CanonicalPolynomial<Int64>(
            monomials = listOf(
                CanonicalMonomial(Int64.one, mapOf(x to Int64(3L).toInt32()))
            ),
            constant = Int64.zero
        )

        assertFailsWith<IllegalArgumentException> {
            cubicCanonical.toTypedMatrixForm(
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