package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.operation.MapValueProvider
import fuookami.ospf.kotlin.math.symbol.operation.compileEval
import fuookami.ospf.kotlin.math.symbol.operation.compileGradient
import fuookami.ospf.kotlin.math.symbol.operation.evaluate
import fuookami.ospf.kotlin.math.symbol.operation.evaluateOrdered
import fuookami.ospf.kotlin.math.symbol.operation.gradient
import fuookami.ospf.kotlin.math.symbol.operation.partialEvaluate
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HighOrderRegressionTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun highOrderCanonicalCompileEvalShouldMatchDirectEvaluate() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")

        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial(Flt64(2.0), listOf(x, x, x, x, x, y, y)),
                CanonicalMonomial(Flt64(-3.0), listOf(x, x, y, y, y)),
                CanonicalMonomial(Flt64(4.0), listOf(z, z, z, z)),
                CanonicalMonomial(Flt64(-7.0), listOf(x, y, z)),
                CanonicalMonomial(Flt64(5.0), listOf(x, x, z))
            ),
            constant = Flt64(11.0)
        )

        val order = listOf(z, x, y)
        val values = listOf(Flt64(0.5), Flt64(2.0), Flt64(-1.0))
        val mapped = mapOf<Symbol, Flt64>(z to values[0], x to values[1], y to values[2])

        val direct = polynomial.evaluate(MapValueProvider(mapped))!!
        val compiled = polynomial.compileEval(order)(values)

        assertTrue((direct - compiled).abs() <= Flt64(1e-9))
    }

    @Test
    fun highOrderCanonicalCompileGradientShouldMatchDirectGradientEvaluation() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")

        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial(Flt64(3.0), listOf(x, x, x, y)),
                CanonicalMonomial(Flt64(-2.0), listOf(y, y, z, z)),
                CanonicalMonomial(Flt64(6.0), listOf(x, z, z, z)),
                CanonicalMonomial(Flt64(-4.0), listOf(x, y, z)),
                CanonicalMonomial(Flt64.one, listOf(y, y, y, y, y))
            ),
            constant = Flt64(-9.0)
        )

        val order = listOf(x, y, z)
        val values = listOf(Flt64(1.5), Flt64(-2.0), Flt64(0.75))

        val expected = polynomial.gradient(order).map { it.evaluateOrdered(order, values) }
        val compiled = polynomial.compileGradient(order)(values)

        assertEquals(expected.size, compiled.size)
        for (i in expected.indices) {
            assertTrue((expected[i] - compiled[i]).abs() <= Flt64(1e-9))
        }
    }

    @Test
    fun highOrderPartialEvaluateShouldStayConsistent() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val z = TestSymbol("z")

        val polynomial = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial(Flt64(2.0), listOf(x, x, x, y, y)),
                CanonicalMonomial(Flt64(-5.0), listOf(x, y, z, z)),
                CanonicalMonomial(Flt64(7.0), listOf(z, z, z))
            ),
            constant = Flt64(3.0)
        )

        val partial = polynomial.partialEvaluate(mapOf<Symbol, Flt64>(x to Flt64(2.0)))
        val fullValues = mapOf<Symbol, Flt64>(x to Flt64(2.0), y to Flt64(-1.5), z to Flt64(0.25))
        val reducedValues = mapOf<Symbol, Flt64>(y to Flt64(-1.5), z to Flt64(0.25))

        val direct = polynomial.evaluate(MapValueProvider(fullValues))!!
        val reduced = partial.evaluate(MapValueProvider(reducedValues))!!

        assertTrue((direct - reduced).abs() <= Flt64(1e-9))
    }
}