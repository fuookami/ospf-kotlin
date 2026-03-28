package fuookami.ospf.kotlin.utils.math.symbol.generic


import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CanonicalGenericTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun genericCanonicalPolynomialShouldEvaluateAndCombine() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = GenericCanonicalPolynomial(
            monomials = listOf(
                GenericCanonicalMonomial(Int64(2L), listOf(x, y)),
                GenericCanonicalMonomial(Int64(3L), listOf(y, x)),
                GenericCanonicalMonomial(Int64(4L), listOf(x))
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
    fun genericCanonicalPolynomialShouldEvaluateInOrder() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = GenericCanonicalPolynomial(
            monomials = listOf(
                GenericCanonicalMonomial(Int64(2L), listOf(x, y, y)),
                GenericCanonicalMonomial(Int64(3L), listOf(x))
            ),
            constant = Int64(5L)
        )

        val value = polynomial.evaluateOrdered(
            order = listOf(y, x),
            values = listOf(Int64(2L), Int64(4L))
        )

        assertEquals(Int64(49L), value)
    }

    @Test
    fun genericCanonicalPolynomialShouldSupportPartialEvaluate() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = GenericCanonicalPolynomial(
            monomials = listOf(
                GenericCanonicalMonomial(Int64(2L), listOf(x, y)),
                GenericCanonicalMonomial(Int64(3L), listOf(x)),
                GenericCanonicalMonomial(Int64(5L), emptyList())
            ),
            constant = Int64.one
        )

        val partial = polynomial.partialEvaluate(
            values = mapOf(x to Int64(4L)),
            zero = Int64.zero
        )

        assertEquals(Int64(18L), partial.constant)
        assertEquals(1, partial.monomials.size)
        assertEquals(listOf(y), partial.monomials.first().factors)
        assertEquals(Int64(8L), partial.monomials.first().coefficient)
    }

    @Test
    fun genericCanonicalPolynomialShouldSupportRoundTripWithFlt64Path() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val original = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(coefficient = Flt64.one, factors = listOf(x, y)),
                CanonicalMonomial<Flt64>(coefficient = Flt64.one, factors = listOf(x))
            ),
            constant = Flt64.zero
        )

        val restored = original
            .toGenericCanonicalPolynomial()
            .toCanonicalPolynomial()

        assertEquals(original, restored)
    }
}




