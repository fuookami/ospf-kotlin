package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DifferentiateTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun linearDerivativeShouldReturnConstantCoefficient() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64.two, x),
                LinearMonomial<Flt64>(Flt64(3.0), y),
                LinearMonomial<Flt64>(Flt64(-1.0), x)
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
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64.two, x, y),
                QuadraticMonomial<Flt64>(Flt64(3.0), x, x),
                QuadraticMonomial<Flt64>(Flt64(4.0), y, null)
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
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64.two, x, y),
                QuadraticMonomial<Flt64>(Flt64(3.0), x, x),
                QuadraticMonomial<Flt64>(Flt64(4.0), y, null)
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

    @Test
    fun canonicalDerivativeShouldSupportRepeatedFactors() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val monomial = CanonicalMonomial<Flt64>(
            coefficient = Flt64(3.0),
            factors = listOf(x, x, y)
        )

        val derivativeByX = monomial.derivative(x)
        assertEquals(1, derivativeByX.monomials.size)
        assertEquals(Flt64(6.0), derivativeByX.monomials.first().coefficient)
        assertEquals(listOf(x, y), derivativeByX.monomials.first().factors)
    }

    @Test
    fun canonicalGradientShouldFollowSymbolOrder() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64.two, listOf(x, y)),
                CanonicalMonomial<Flt64>(Flt64(3.0), listOf(x, x)),
                CanonicalMonomial<Flt64>(Flt64(4.0), listOf(y))
            )
        )

        val gradient = polynomial.gradient(listOf(y, x))
        val gradY = gradient[0].toLinearPolynomialOrNull()
        val gradX = gradient[1].toLinearPolynomialOrNull()

        assertTrue(gradY != null)
        assertTrue(gradX != null)
        val gradYCoefficients = gradY.monomials.associate { it.symbol to it.coefficient }
        val gradXCoefficients = gradX.monomials.associate { it.symbol to it.coefficient }

        assertTrue(gradYCoefficients[x] == Flt64.two)
        assertEquals(Flt64(4.0), gradY.constant)
        assertTrue(gradXCoefficients[x] == Flt64(6.0))
        assertTrue(gradXCoefficients[y] == Flt64.two)
    }

    @Test
    fun quadraticHessianShouldBeZeroForLinearOnlyPolynomial() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64(2.0), x, null),
                QuadraticMonomial<Flt64>(Flt64(-3.0), y, null)
            ),
            constant = Flt64.one
        )

        val hessian = polynomial.hessian(order = listOf(y, x))

        assertEquals(0.0, hessian[0][0])
        assertEquals(0.0, hessian[0][1])
        assertEquals(0.0, hessian[1][0])
        assertEquals(0.0, hessian[1][1])
    }

    @Test
    fun quadraticHessianShouldFollowOrderForMixedTerms() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64.two, x, y),
                QuadraticMonomial<Flt64>(Flt64(3.0), x, x),
                QuadraticMonomial<Flt64>(Flt64(4.0), y, null)
            ),
            constant = Flt64.one
        )

        val hessian = polynomial.hessian(order = listOf(y, x))

        assertEquals(0.0, hessian[0][0])
        assertEquals(2.0, hessian[0][1])
        assertEquals(2.0, hessian[1][0])
        assertEquals(6.0, hessian[1][1])
    }

    @Test
    fun quadraticHessianShouldSupportRepeatedVariableMonomials() {
        val x = TestSymbol("x")
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64(1.0), x, x),
                QuadraticMonomial<Flt64>(Flt64(2.0), x, x)
            )
        )

        val hessian = polynomial.hessian(order = listOf(x))

        assertEquals(6.0, hessian[0][0])
    }

    @Test
    fun canonicalHessianShouldFailForHigherOrderTerm() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64.one, listOf(x, y, z))
            )
        )

        assertFailsWith<IllegalArgumentException> {
            polynomial.hessian(order = listOf(x, y, z))
        }
    }
}
