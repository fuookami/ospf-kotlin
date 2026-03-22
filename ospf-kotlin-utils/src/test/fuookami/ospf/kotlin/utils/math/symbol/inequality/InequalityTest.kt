package fuookami.ospf.kotlin.utils.math.symbol.inequality

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class InequalityTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun linearInequalityShouldKeepArguments() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.two, x)), Flt64.one)
        val rhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, y)), Flt64.zero)

        val inequality = LinearInequality(lhs, rhs, Comparison.LE)

        assertEquals(lhs, inequality.lhs)
        assertEquals(rhs, inequality.rhs)
        assertEquals(Comparison.LE, inequality.comparison)
    }

    @Test
    fun quadraticInequalityShouldKeepArguments() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val lhs = QuadraticPolynomial(listOf(QuadraticMonomial(symbol1 = x, symbol2 = y)), Flt64.one)
        val rhs = QuadraticPolynomial(listOf(QuadraticMonomial(symbol1 = x)), Flt64.zero)

        val inequality = QuadraticInequality(lhs, rhs, Comparison.GT)

        assertEquals(lhs, inequality.lhs)
        assertEquals(rhs, inequality.rhs)
        assertEquals(Comparison.GT, inequality.comparison)
    }
}
