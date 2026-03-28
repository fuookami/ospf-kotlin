package fuookami.ospf.kotlin.utils.math.symbol.inequality

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InequalityTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun linearInequalityShouldKeepArguments() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val lhs = LinearPolynomial<Flt64>(listOf(LinearMonomial<Flt64>(Flt64.two, x)), Flt64.one)
        val rhs = LinearPolynomial<Flt64>(listOf(LinearMonomial<Flt64>(Flt64.one, y)), Flt64.zero)

        val inequality = LinearInequality(lhs, rhs, Comparison.LE)

        assertEquals(lhs, inequality.lhs)
        assertEquals(rhs, inequality.rhs)
        assertEquals(Comparison.LE, inequality.comparison)
    }

    @Test
    fun quadraticInequalityShouldKeepArguments() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val lhs = QuadraticPolynomial<Flt64>(listOf(QuadraticMonomial<Flt64>(coefficient = Flt64.one, symbol1 = x, symbol2 = y)), Flt64.one)
        val rhs = QuadraticPolynomial<Flt64>(listOf(QuadraticMonomial<Flt64>(coefficient = Flt64.one, symbol1 = x)), Flt64.zero)

        val inequality = QuadraticInequality(lhs, rhs, Comparison.GT)

        assertEquals(lhs, inequality.lhs)
        assertEquals(rhs, inequality.rhs)
        assertEquals(Comparison.GT, inequality.comparison)
    }

    @Test
    fun comparisonShouldExposeBehaviorFlagsAndReverse() {
        assertEquals("<", Comparison.LT.symbol)
        assertEquals("<=", Comparison.LE.symbol)
        assertEquals("=", Comparison.EQ.symbol)
        assertEquals("!=", Comparison.NE.symbol)
        assertEquals(">=", Comparison.GE.symbol)
        assertEquals(">", Comparison.GT.symbol)

        assertTrue(Comparison.LT.isStrict)
        assertTrue(Comparison.GT.isStrict)
        assertTrue(Comparison.NE.isStrict)
        assertFalse(Comparison.LE.isStrict)

        assertTrue(Comparison.LE.includesEquality)
        assertTrue(Comparison.EQ.includesEquality)
        assertTrue(Comparison.GE.includesEquality)
        assertFalse(Comparison.LT.includesEquality)

        assertTrue(Comparison.LT.isLessLike)
        assertTrue(Comparison.LE.isLessLike)
        assertFalse(Comparison.GE.isLessLike)

        assertTrue(Comparison.GT.isGreaterLike)
        assertTrue(Comparison.GE.isGreaterLike)
        assertFalse(Comparison.LE.isGreaterLike)

        assertEquals(Comparison.GT, Comparison.LT.reverse())
        assertEquals(Comparison.GE, Comparison.LE.reverse())
        assertEquals(Comparison.EQ, Comparison.EQ.reverse())
        assertEquals(Comparison.NE, Comparison.NE.reverse())
        assertEquals(Comparison.LE, Comparison.GE.reverse())
        assertEquals(Comparison.LT, Comparison.GT.reverse())
    }
}



