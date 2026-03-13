package fuookami.ospf.kotlin.utils.math.symbol.operation

import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.monomial.*
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.*

class DifferentiateTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun linearDerivativeShouldReturnConstantCoefficient() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64.two, x),
                LinearMonomial(Flt64(3.0), y),
                LinearMonomial(Flt64(-1.0), x)
            ),
            constant = Flt64(5.0)
        )

        assertEquals(Flt64.one, polynomial.derivative(x))
        assertEquals(Flt64(3.0), polynomial.derivative(y))
    }

    @Test
    fun quadraticDerivativeShouldFollowAnalyticRules() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial(Flt64.two, x, y),
                QuadraticMonomial(Flt64(3.0), x, x),
                QuadraticMonomial(Flt64(4.0), y, null)
            ),
            constant = Flt64.one
        )

        val derivativeByX = polynomial.derivative(x)
        val coefficients = derivativeByX.monomials.associate { it.symbol to it.coefficient }

        assertTrue(derivativeByX.constant == Flt64.zero)
        assertTrue(coefficients[x] == Flt64(6.0))
        assertTrue(coefficients[y] == Flt64.two)
    }

    @Test
    fun gradientShouldFollowSymbolOrder() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial(Flt64.two, x, y),
                QuadraticMonomial(Flt64(3.0), x, x),
                QuadraticMonomial(Flt64(4.0), y, null)
            )
        )

        val gradient = polynomial.gradient(listOf(y, x))
        val gradY = gradient[0].monomials.associate { it.symbol to it.coefficient }
        val gradX = gradient[1].monomials.associate { it.symbol to it.coefficient }

        assertTrue(gradY[x] == Flt64.two)
        assertTrue(gradient[0].constant == Flt64(4.0))
        assertTrue(gradX[x] == Flt64(6.0))
        assertTrue(gradX[y] == Flt64.two)
    }
}
