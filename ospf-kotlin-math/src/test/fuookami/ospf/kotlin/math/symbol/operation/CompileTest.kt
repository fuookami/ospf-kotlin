package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.operation.compileEval
import fuookami.ospf.kotlin.math.symbol.operation.compileGradient
import fuookami.ospf.kotlin.math.symbol.operation.evaluateOrdered
import fuookami.ospf.kotlin.math.symbol.operation.gradient
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CompileTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun compileEvalShouldMatchEvaluateOrdered() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")
        val order = listOf(z, x, y)
        val values = listOf(Flt64(7.0), Flt64(2.0), Flt64(3.0))

        val linear = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64.two, x),
                LinearMonomial<Flt64>(Flt64(-1.0), z)
            ),
            constant = Flt64.one
        )
        val quadratic = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64(3.0), x, y),
                QuadraticMonomial<Flt64>(Flt64(-2.0), z, null)
            ),
            constant = Flt64(4.0)
        )
        val canonical = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64(5.0), listOf(x, y, z)),
                CanonicalMonomial<Flt64>(Flt64(-2.0), listOf(y, y))
            ),
            constant = Flt64(-1.0)
        )

        assertEquals(linear.evaluateOrdered(order, values), linear.compileEval(order)(values))
        assertEquals(quadratic.evaluateOrdered(order, values), quadratic.compileEval(order)(values))
        assertEquals(canonical.evaluateOrdered(order, values), canonical.compileEval(order)(values))
    }

    @Test
    fun compileGradientShouldMatchGradientThenEvaluate() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")
        val order = listOf(y, z, x)
        val values = listOf(Flt64(3.0), Flt64(-2.0), Flt64(5.0))

        val linear = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64(4.0), x),
                LinearMonomial<Flt64>(Flt64(-1.0), z)
            ),
            constant = Flt64.one
        )
        val quadratic = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64.two, x, y),
                QuadraticMonomial<Flt64>(Flt64(3.0), x, x),
                QuadraticMonomial<Flt64>(Flt64(-4.0), z, null)
            ),
            constant = Flt64(6.0)
        )
        val canonical = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64(2.0), listOf(x, x, y)),
                CanonicalMonomial<Flt64>(Flt64(-3.0), listOf(y, z)),
                CanonicalMonomial<Flt64>(Flt64(4.0), listOf(x))
            ),
            constant = Flt64(7.0)
        )

        val linearExpected = linear.gradient(order)
        val quadraticExpected = quadratic.gradient(order).map { it.evaluateOrdered(order, values) }
        val canonicalExpected = canonical.gradient(order).map { it.evaluateOrdered(order, values) }

        assertEquals(linearExpected, linear.compileGradient(order)(values))
        assertEquals(quadraticExpected, quadratic.compileGradient(order)(values))
        assertEquals(canonicalExpected, canonical.compileGradient(order)(values))
    }

    @Test
    fun compileShouldSupportEmptyExpression() {
        val emptyLinear = LinearPolynomial<Flt64>(constant = Flt64(8.0))
        val emptyCanonical = CanonicalPolynomial<Flt64>(constant = Flt64(-3.0))
        val order = emptyList<Symbol>()
        val values = emptyList<Flt64>()

        assertEquals(Flt64(8.0), emptyLinear.compileEval(order)(values))
        assertEquals(emptyList(), emptyLinear.compileGradient(order)(values))
        assertEquals(Flt64(-3.0), emptyCanonical.compileEval(order)(values))
        assertEquals(emptyList(), emptyCanonical.compileGradient(order)(values))
    }

    @Test
    fun compileShouldFailWhenOrderMissesSymbol() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = QuadraticPolynomial<Flt64>(
            monomials = listOf(
                QuadraticMonomial<Flt64>(Flt64.one, x, y)
            ),
            constant = Flt64.zero
        )

        assertFailsWith<IllegalArgumentException> {
            polynomial.compileEval(order = listOf(x))
        }
        assertFailsWith<IllegalArgumentException> {
            polynomial.compileGradient(order = listOf(x))
        }
    }

    @Test
    fun compileShouldFailWhenOrderContainsDuplicatedSymbols() {
        val x = TestSymbol("x")
        val polynomial = LinearPolynomial<Flt64>(
            monomials = listOf(
                LinearMonomial<Flt64>(Flt64.one, x)
            ),
            constant = Flt64.zero
        )

        assertFailsWith<IllegalArgumentException> {
            polynomial.compileEval(order = listOf(x, x))
        }
        assertFailsWith<IllegalArgumentException> {
            polynomial.compileGradient(order = listOf(x, x))
        }
    }

    @Test
    fun compiledClosureShouldFailWhenValuesSizeMismatch() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial<Flt64>(Flt64.one, listOf(x, y))
            ),
            constant = Flt64.zero
        )
        val compiledEval = polynomial.compileEval(order = listOf(x, y))
        val compiledGradient = polynomial.compileGradient(order = listOf(x, y))

        assertFailsWith<IllegalArgumentException> {
            compiledEval(listOf(Flt64.one))
        }
        assertFailsWith<IllegalArgumentException> {
            compiledGradient(listOf(Flt64.one))
        }
    }
}


