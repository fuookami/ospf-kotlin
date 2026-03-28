package fuookami.ospf.kotlin.utils.math.symbol.generic


import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LinearGenericTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun genericCoefficientAndExponentShouldCompile() {
        val coefficient: Coefficient<Int64> = RingCoefficient(Int64.one)
        val exponent: Exponent<Int> = NonNegativeExponent(2)

        assertEquals(Int64.one, coefficient.value)
        assertEquals(2, exponent.value)
    }

    @Test
    fun genericLinearPolynomialShouldEvaluateAndCombine() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = GenericLinearPolynomial(
            monomials = listOf(
                GenericLinearMonomial(Int64.one, x),
                GenericLinearMonomial(Int64(2L), x),
                GenericLinearMonomial(Int64(3L), y)
            ),
            constant = Int64(4L)
        ).combineTerms(Int64.zero)

        val value = polynomial.evaluate(
            values = mapOf(
                x to Int64(2L),
                y to Int64(5L)
            )
        )

        assertNotNull(value)
        assertEquals(Int64(25L), value)
        assertEquals(2, polynomial.monomials.size)
    }

    @Test
    fun genericLinearPolynomialShouldSupportRoundTripWithFlt64Path() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val original = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(coefficient = Flt64.one, symbol = x),
                LinearMonomial<Flt64>(coefficient = Flt64.one, symbol = y)
            ),
            constant = Flt64.zero
        )

        val restored = original
            .toGenericLinearPolynomial()
            .toLinearPolynomial()

        assertEquals(original, restored)
    }

    @Test
    fun genericLinearPolynomialShouldSupportPartialEvaluate() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = GenericLinearPolynomial(
            monomials = listOf(
                GenericLinearMonomial(Int64(2L), x),
                GenericLinearMonomial(Int64(3L), y)
            ),
            constant = Int64.one
        )

        val partial = polynomial.partialEvaluate(
            values = mapOf(x to Int64(4L)),
            zero = Int64.zero
        )

        assertEquals(Int64(9L), partial.constant)
        assertEquals(1, partial.monomials.size)
        assertEquals(y, partial.monomials.first().symbol)
    }
}



