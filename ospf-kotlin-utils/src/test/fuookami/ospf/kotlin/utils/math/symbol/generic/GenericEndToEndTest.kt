package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 端到端回归测试：evaluate -> gradient -> matrixForm -> compile 一致性校验
 * End-to-end regression test: evaluate -> gradient -> matrixForm -> compile consistency check
 */
class GenericEndToEndTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    private fun Flt64.isApproximately(expected: Flt64, tolerance: Double = 1e-10): Boolean {
        return abs(this.toDouble() - expected.toDouble()) < tolerance
    }

    // ==================== Linear Tests ====================

    @Test
    fun linearEvaluateVsCompileShouldMatch() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val order = listOf(x, y)

        val polynomial = GenericLinearPolynomial(
            monomials = listOf(
                GenericLinearMonomial(Flt64(2.0), x),
                GenericLinearMonomial(Flt64(3.0), y)
            ),
            constant = Flt64(5.0)
        )

        val values: Map<Symbol, Flt64> = mapOf(x to Flt64(4.0), y to Flt64(6.0))
        val orderedValues = listOf(Flt64(4.0), Flt64(6.0))

        // evaluate path
        val evalResult = polynomial.evaluate(values)!!

        // compile path
        val compiledEval = polynomial.compileEval(order = order, zero = Flt64.zero)
        val compileResult = compiledEval(orderedValues)

        // Debug: print values
        println("evalResult = ${evalResult.toDouble()}, compileResult = ${compileResult.toDouble()}")
        assertTrue(evalResult.isApproximately(compileResult), "evalResult ($evalResult) != compileResult ($compileResult)")
        assertTrue(evalResult.isApproximately(Flt64(31.0))) // 2*4 + 3*6 + 5 = 8 + 18 + 5 = 31
    }

    @Test
    fun linearGradientShouldBeConsistent() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val order = listOf(x, y)

        val polynomial = GenericLinearPolynomial(
            monomials = listOf(
                GenericLinearMonomial(Flt64(2.0), x),
                GenericLinearMonomial(Flt64(3.0), y)
            ),
            constant = Flt64(5.0)
        )

        // derivative path
        val dx = polynomial.derivative(x, Flt64.zero)
        val dy = polynomial.derivative(y, Flt64.zero)

        // gradient path
        val gradient = polynomial.gradient(order, Flt64.zero)

        assertTrue(dx.isApproximately(Flt64(2.0)))
        assertTrue(dy.isApproximately(Flt64(3.0)))
        assertTrue(gradient[0].isApproximately(Flt64(2.0)))
        assertTrue(gradient[1].isApproximately(Flt64(3.0)))

        // compile gradient path
        val compiledGradient = polynomial.compileGradient(order = order, zero = Flt64.zero)
        val values = listOf(Flt64(4.0), Flt64(6.0))
        val compileGradResult = compiledGradient(values)

        assertTrue(compileGradResult[0].isApproximately(Flt64(2.0)))
        assertTrue(compileGradResult[1].isApproximately(Flt64(3.0)))
    }

    // ==================== Quadratic Tests ====================

    @Test
    fun quadraticEvaluateVsCompileShouldMatch() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val order = listOf(x, y)

        // f(x,y) = 2*x*y + 3*x^2 + 4*y + 5
        val polynomial = GenericQuadraticPolynomial(
            monomials = listOf(
                GenericQuadraticMonomial(Flt64(2.0), x, y),
                GenericQuadraticMonomial(Flt64(3.0), x, x),
                GenericQuadraticMonomial(Flt64(4.0), y, null)
            ),
            constant = Flt64(5.0)
        )

        val values: Map<Symbol, Flt64> = mapOf<Symbol, Flt64>(x to Flt64(2.0), y to Flt64(3.0))
        val orderedValues = listOf(Flt64(2.0), Flt64(3.0))

        // evaluate path
        val evalResult = polynomial.evaluate(values)!!

        // compile path
        val compiledEval = polynomial.compileEval(order = order, zero = Flt64.zero)
        val compileResult = compiledEval(orderedValues)

        assertTrue(evalResult.isApproximately(compileResult))
        // f(2,3) = 2*2*3 + 3*4 + 4*3 + 5 = 12 + 12 + 12 + 5 = 41
        assertTrue(evalResult.isApproximately(Flt64(41.0)))
    }

    @Test
    fun quadraticGradientEvaluateVsCompileShouldMatch() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val order = listOf(x, y)

        // f(x,y) = 2*x*y + 3*x^2 + 4*y + 5
        // df/dx = 2*y + 6*x
        // df/dy = 2*x + 4
        val polynomial = GenericQuadraticPolynomial(
            monomials = listOf(
                GenericQuadraticMonomial(Flt64(2.0), x, y),
                GenericQuadraticMonomial(Flt64(3.0), x, x),
                GenericQuadraticMonomial(Flt64(4.0), y, null)
            ),
            constant = Flt64(5.0)
        )

        val orderedValues = listOf(Flt64(2.0), Flt64(3.0))

        // derivative path
        val dx = polynomial.derivative(x, Flt64.zero)
        val dy = polynomial.derivative(y, Flt64.zero)

        // evaluate derivatives at point
        val dxValue = dx.evaluate(mapOf<Symbol, Flt64>(x to Flt64(2.0), y to Flt64(3.0)))!!
        val dyValue = dy.evaluate(mapOf<Symbol, Flt64>(x to Flt64(2.0), y to Flt64(3.0)))!!

        // compile gradient path
        val compiledGradient = polynomial.compileGradient(order = order, zero = Flt64.zero)
        val compileGradResult = compiledGradient(orderedValues)

        // df/dx(2,3) = 2*3 + 6*2 = 6 + 12 = 18
        // df/dy(2,3) = 2*2 + 4 = 8
        assertTrue(dxValue.isApproximately(Flt64(18.0)))
        assertTrue(dyValue.isApproximately(Flt64(8.0)))
        assertTrue(compileGradResult[0].isApproximately(Flt64(18.0)))
        assertTrue(compileGradResult[1].isApproximately(Flt64(8.0)))
    }

    @Test
    fun quadraticMatrixFormShouldMatchCoefficients() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val order = listOf(x, y)

        // f(x,y) = 2*x*y + 3*x^2 + 4*y + 5
        // Matrix form: Q = [[3, 1], [1, 0]], c = [0, 4], d = 5
        val polynomial = GenericQuadraticPolynomial(
            monomials = listOf(
                GenericQuadraticMonomial(Flt64(2.0), x, y),
                GenericQuadraticMonomial(Flt64(3.0), x, x),
                GenericQuadraticMonomial(Flt64(4.0), y, null)
            ),
            constant = Flt64(5.0)
        ).combineTerms(Flt64.zero)

        val (q, c) = polynomial.toMatrixPair(order)

        // Check Q matrix (symmetric)
        assertEquals(3.0, q[0][0], 1e-10) // x^2 coefficient
        assertEquals(1.0, q[0][1], 1e-10) // x*y coefficient (split to 2*1 = 2 total)
        assertEquals(1.0, q[1][0], 1e-10) // y*x coefficient
        assertEquals(0.0, q[1][1], 1e-10) // no y^2 term

        // Check linear coefficients
        assertEquals(0.0, c[0], 1e-10) // no x linear term
        assertEquals(4.0, c[1], 1e-10) // y linear term
    }

    // ==================== Canonical Tests ====================

    @Test
    fun canonicalEvaluateVsCompileShouldMatch() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val order = listOf(x, y)

        // f(x,y) = 2*x^2*y + 3*x + 5
        val polynomial = GenericCanonicalPolynomial(
            monomials = listOf(
                GenericCanonicalMonomial(Flt64(2.0), mapOf(x to 2, y to 1)),
                GenericCanonicalMonomial(Flt64(3.0), mapOf(x to 1))
            ),
            constant = Flt64(5.0)
        )

        val values: Map<Symbol, Flt64> = mapOf<Symbol, Flt64>(x to Flt64(2.0), y to Flt64(3.0))
        val orderedValues = listOf(Flt64(2.0), Flt64(3.0))

        // evaluate path
        val evalResult = polynomial.evaluate(values, one = Flt64.one)!!

        // compile path
        val compiledEval = polynomial.compileEval(order = order, zero = Flt64.zero, one = Flt64.one)
        val compileResult = compiledEval(orderedValues)

        assertTrue(evalResult.isApproximately(compileResult))
        // f(2,3) = 2*4*3 + 3*2 + 5 = 24 + 6 + 5 = 35
        assertTrue(evalResult.isApproximately(Flt64(35.0)))
    }

    @Test
    fun canonicalGradientEvaluateVsCompileShouldMatch() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val order = listOf(x, y)

        // f(x,y) = 2*x^2*y + 3*x + 5
        // df/dx = 4*x*y + 3
        // df/dy = 2*x^2
        val polynomial = GenericCanonicalPolynomial(
            monomials = listOf(
                GenericCanonicalMonomial(Flt64(2.0), mapOf(x to 2, y to 1)),
                GenericCanonicalMonomial(Flt64(3.0), mapOf(x to 1))
            ),
            constant = Flt64(5.0)
        )

        val orderedValues = listOf(Flt64(2.0), Flt64(3.0))

        // derivative path
        val dx = polynomial.derivative(x, Flt64.zero)
        val dy = polynomial.derivative(y, Flt64.zero)

        // evaluate derivatives at point
        val dxValue = dx.evaluate(mapOf<Symbol, Flt64>(x to Flt64(2.0), y to Flt64(3.0)), one = Flt64.one)!!
        val dyValue = dy.evaluate(mapOf<Symbol, Flt64>(x to Flt64(2.0), y to Flt64(3.0)), one = Flt64.one)!!

        // compile gradient path
        val compiledGradient = polynomial.compileGradient(order = order, zero = Flt64.zero)
        val compileGradResult = compiledGradient(orderedValues)

        // df/dx(2,3) = 4*2*3 + 3 = 24 + 3 = 27
        // df/dy(2,3) = 2*4 = 8
        assertTrue(dxValue.isApproximately(Flt64(27.0)))
        assertTrue(dyValue.isApproximately(Flt64(8.0)))
        assertTrue(compileGradResult[0].isApproximately(Flt64(27.0)))
        assertTrue(compileGradResult[1].isApproximately(Flt64(8.0)))
    }

    @Test
    fun canonicalQuadraticPathShouldMatchQuadraticPolynomial() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val order = listOf(x, y)

        // Create same polynomial as Canonical (degree 2) and Quadratic
        val canonical = GenericCanonicalPolynomial(
            monomials = listOf(
                GenericCanonicalMonomial(Flt64(2.0), mapOf(x to 1, y to 1)), // 2*x*y
                GenericCanonicalMonomial(Flt64(3.0), mapOf(x to 2)),          // 3*x^2
                GenericCanonicalMonomial(Flt64(4.0), mapOf(y to 1))           // 4*y
            ),
            constant = Flt64(5.0)
        )

        val quadratic = GenericQuadraticPolynomial(
            monomials = listOf(
                GenericQuadraticMonomial(Flt64(2.0), x, y),
                GenericQuadraticMonomial(Flt64(3.0), x, x),
                GenericQuadraticMonomial(Flt64(4.0), y, null)
            ),
            constant = Flt64(5.0)
        )

        val orderedValues = listOf(Flt64(2.0), Flt64(3.0))

        // Evaluate both
        val canonicalValue = canonical.evaluateOrdered(order, orderedValues, Flt64.one)
        val quadraticValue = quadratic.evaluateOrdered(order, orderedValues)

        assertTrue(canonicalValue.isApproximately(quadraticValue))
        assertTrue(canonicalValue.isApproximately(Flt64(41.0)))

        // Gradient
        val canonicalGrad = canonical.compileGradient(order = order, zero = Flt64.zero)(orderedValues)
        val quadraticGrad = quadratic.compileGradient(order = order, zero = Flt64.zero)(orderedValues)

        assertTrue(canonicalGrad[0].isApproximately(quadraticGrad[0]))
        assertTrue(canonicalGrad[1].isApproximately(quadraticGrad[1]))

        // Conversion to Quadratic
        val convertedQuadratic = canonical.toGenericQuadraticPolynomialOrNull(Flt64.zero)
        assertTrue(convertedQuadratic != null)

        val convertedValue = convertedQuadratic.evaluateOrdered(order, orderedValues)
        assertTrue(convertedValue.isApproximately(canonicalValue))
    }

    // ==================== Edge Cases ====================

    @Test
    fun constantPolynomialShouldHaveZeroGradient() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val order = listOf(x, y)

        val linear = GenericLinearPolynomial(
            monomials = emptyList(),
            constant = Flt64(42.0)
        )

        val quadratic = GenericQuadraticPolynomial(
            monomials = emptyList(),
            constant = Flt64(42.0)
        )

        val canonical = GenericCanonicalPolynomial(
            monomials = emptyList(),
            constant = Flt64(42.0)
        )

        for (symbol in order) {
            assertTrue(linear.derivative(symbol, Flt64.zero).isApproximately(Flt64.zero))
            assertTrue(quadratic.derivative(symbol, Flt64.zero).evaluate(emptyMap())!!.isApproximately(Flt64.zero))
            assertTrue(canonical.derivative(symbol, Flt64.zero).evaluate(emptyMap(), one = Flt64.one)!!.isApproximately(Flt64.zero))
        }

        val values = listOf(Flt64(1.0), Flt64(2.0))
        val linearGrad = linear.compileGradient(order = order, zero = Flt64.zero)(values)
        val quadraticGrad = quadratic.compileGradient(order = order, zero = Flt64.zero)(values)
        val canonicalGrad = canonical.compileGradient(order = order, zero = Flt64.zero)(values)

        for (grad in listOf(linearGrad, quadraticGrad, canonicalGrad)) {
            assertTrue(grad[0].isApproximately(Flt64.zero))
            assertTrue(grad[1].isApproximately(Flt64.zero))
        }
    }

    @Test
    fun singleVariablePolynomialShouldWorkCorrectly() {
        val x = TestSymbol("x")
        val order = listOf(x)

        // f(x) = 5*x^3 + 3*x^2 + 2*x + 1
        val polynomial = GenericCanonicalPolynomial(
            monomials = listOf(
                GenericCanonicalMonomial(Flt64(5.0), mapOf(x to 3)),
                GenericCanonicalMonomial(Flt64(3.0), mapOf(x to 2)),
                GenericCanonicalMonomial(Flt64(2.0), mapOf(x to 1))
            ),
            constant = Flt64(1.0)
        )

        val values = listOf(Flt64(2.0))

        // f(2) = 5*8 + 3*4 + 2*2 + 1 = 40 + 12 + 4 + 1 = 57
        val evalResult = polynomial.compileEval(order = order, zero = Flt64.zero, one = Flt64.one)(values)
        assertTrue(evalResult.isApproximately(Flt64(57.0)))

        // df/dx = 15*x^2 + 6*x + 2
        // df/dx(2) = 15*4 + 6*2 + 2 = 60 + 12 + 2 = 74
        val gradResult = polynomial.compileGradient(order = order, zero = Flt64.zero)(values)
        assertTrue(gradResult[0].isApproximately(Flt64(74.0)))
    }
}