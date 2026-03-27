package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.Int64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CompileGenericTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun genericLinearCompileShouldSupportInt64() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = GenericLinearPolynomial(
            monomials = listOf(
                GenericLinearMonomial(Int64(2L), x),
                GenericLinearMonomial(Int64(3L), y)
            ),
            constant = Int64(4L)
        )

        val eval = polynomial.compileEval(
            order = listOf(x, y),
            zero = Int64.zero
        )
        val gradient = polynomial.compileGradient(
            order = listOf(x, y),
            zero = Int64.zero
        )

        val values = listOf(Int64(5L), Int64(7L))
        assertEquals(Int64(35L), eval(values))
        assertEquals(listOf(Int64(2L), Int64(3L)), gradient(values))
    }

    @Test
    fun genericQuadraticCompileShouldSupportInt64() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = GenericQuadraticPolynomial(
            monomials = listOf(
                GenericQuadraticMonomial(Int64(2L), x, y),
                GenericQuadraticMonomial(Int64(3L), x, x),
                GenericQuadraticMonomial(Int64(4L), y, null)
            ),
            constant = Int64.one
        )

        val eval = polynomial.compileEval(
            order = listOf(x, y),
            zero = Int64.zero
        )
        val gradient = polynomial.compileGradient(
            order = listOf(x, y),
            zero = Int64.zero
        )

        val values = listOf(Int64(2L), Int64(5L))
        assertEquals(Int64(53L), eval(values))
        assertEquals(listOf(Int64(22L), Int64(8L)), gradient(values))
    }

    @Test
    fun genericCanonicalCompileShouldSupportInt64() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = GenericCanonicalPolynomial(
            monomials = listOf(
                GenericCanonicalMonomial(Int64(2L), listOf(x, x, y)),
                GenericCanonicalMonomial(Int64(3L), listOf(y))
            ),
            constant = Int64.one
        )

        val eval = polynomial.compileEval(
            order = listOf(x, y),
            zero = Int64.zero
        )
        val gradient = polynomial.compileGradient(
            order = listOf(x, y),
            zero = Int64.zero
        )

        val values = listOf(Int64(2L), Int64(5L))
        assertEquals(Int64(56L), eval(values))
        assertEquals(listOf(Int64(40L), Int64(11L)), gradient(values))
    }
}
