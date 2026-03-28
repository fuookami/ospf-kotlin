package fuookami.ospf.kotlin.utils.math.symbol.generic


import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class QuadraticGenericTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun genericQuadraticPolynomialShouldEvaluateAndCombine() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = GenericQuadraticPolynomial(
            monomials = listOf(
                GenericQuadraticMonomial(Int64(2L), x, y),
                GenericQuadraticMonomial(Int64(3L), y, x),
                GenericQuadraticMonomial(Int64(4L), x, null)
            ),
            constant = Int64.one
        ).combineTerms(Int64.zero)

        val value = polynomial.evaluate(
            values = mapOf(
                x to Int64(2L),
                y to Int64(3L)
            )
        )

        assertNotNull(value)
        assertEquals(Int64(39L), value)
        assertEquals(2, polynomial.monomials.size)
    }

    @Test
    fun genericQuadraticPolynomialShouldEvaluateInOrder() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = GenericQuadraticPolynomial(
            monomials = listOf(
                GenericQuadraticMonomial(Int64(5L), x, y),
                GenericQuadraticMonomial(Int64(2L), x, null)
            ),
            constant = Int64(3L)
        )

        val value = polynomial.evaluateOrdered(
            order = listOf(y, x),
            values = listOf(Int64(4L), Int64(6L))
        )

        assertEquals(Int64(135L), value)
    }

    @Test
    fun genericQuadraticPolynomialShouldSupportPartialEvaluate() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = GenericQuadraticPolynomial(
            monomials = listOf(
                GenericQuadraticMonomial(Int64(2L), x, y),
                GenericQuadraticMonomial(Int64(3L), x, null)
            ),
            constant = Int64.one
        )

        val partial = polynomial.partialEvaluate(
            values = mapOf(x to Int64(5L)),
            zero = Int64.zero
        )

        assertEquals(Int64(16L), partial.constant)
        assertEquals(1, partial.monomials.size)
        assertEquals(y, partial.monomials.first().symbol1)
        assertEquals(null, partial.monomials.first().symbol2)
        assertEquals(Int64(10L), partial.monomials.first().coefficient)
    }

    @Test
    fun genericQuadraticPolynomialShouldSupportRoundTripWithFlt64Path() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val original = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(coefficient = Flt64.one, symbol1 = x, symbol2 = y),
                QuadraticMonomial<Flt64>(coefficient = Flt64.one, symbol1 = x, symbol2 = null)
            ),
            constant = Flt64.zero
        )

        val restored = original
            .toGenericQuadraticPolynomial()
            .toQuadraticPolynomial()

        assertEquals(original, restored)
    }
}




