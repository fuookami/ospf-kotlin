package fuookami.ospf.kotlin.math.symbol.inequality

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

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

        val inequality = QuadraticInequalityOf<Flt64>(lhs, rhs, Comparison.GT)

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

    @Test
    fun linearInequalityOperatorsShouldBuildComparison() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val lhs = LinearPolynomial<Flt64>(listOf(LinearMonomial<Flt64>(Flt64.two, x)), Flt64.one)
        val rhs = LinearPolynomial<Flt64>(listOf(LinearMonomial<Flt64>(Flt64.one, y)), Flt64.zero)

        val leIneq = lhs le rhs
        assertEquals(Comparison.LE, leIneq.comparison)
        assertEquals(lhs, leIneq.lhs)
        assertEquals(rhs, leIneq.rhs)

        val geConst = lhs ge Flt64.one
        assertEquals(Comparison.GE, geConst.comparison)
        assertEquals(Flt64.one, geConst.rhs.constant)
    }

    @Test
    fun quadraticInequalityOperatorsShouldSupportMixedOrder() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val q = QuadraticPolynomial<Flt64>(
            listOf(QuadraticMonomial<Flt64>(coefficient = Flt64.one, symbol1 = x, symbol2 = y)),
            Flt64.one
        )
        val l = LinearPolynomial<Flt64>(
            listOf(LinearMonomial<Flt64>(Flt64.two, x)),
            Flt64.zero
        )

        val ltIneq = q lt l
        assertEquals(Comparison.LT, ltIneq.comparison)

        val gtConst = Flt64.zero gt q
        assertEquals(Comparison.GT, gtConst.comparison)
        assertEquals(Flt64.zero, gtConst.lhs.constant)
    }

    @Test
    fun linearInequalityShouldSupportSatisfiabilityCheck() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val inequality = LinearInequality(
            lhs = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(Flt64.two, x),
                    LinearMonomial(Flt64.one, y)
                ),
                constant = Flt64.zero
            ),
            rhs = LinearPolynomial(constant = Flt64(5.0)),
            comparison = Comparison.LE
        )

        assertEquals(
            true,
            inequality.isSatisfied(mapOf<Symbol, Flt64>(x to Flt64.one, y to Flt64.two))
        )
        assertEquals(
            false,
            inequality.isSatisfied(mapOf<Symbol, Flt64>(x to Flt64(2.0), y to Flt64(3.0)))
        )

        val order = listOf(y, x)
        assertEquals(
            true,
            inequality.isSatisfiedOrdered(order, listOf(Flt64.two, Flt64.one))
        )
        assertEquals(
            false,
            inequality.isSatisfiedOrdered(order, listOf(Flt64(3.0), Flt64(2.0)))
        )
    }

    @Test
    fun quadraticInequalityShouldSupportSatisfiabilityCheck() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val inequality: QuadraticInequalityOf<Flt64> = QuadraticInequalityOf(
            lhs = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial(Flt64.one, x, x),
                    QuadraticMonomial(Flt64.one, y, null)
                ),
                constant = Flt64.zero
            ),
            rhs = QuadraticPolynomial(constant = Flt64(6.0)),
            comparison = Comparison.LE
        )

        assertEquals(
            true,
            inequality.isSatisfied(mapOf<Symbol, Flt64>(x to Flt64(2.0), y to Flt64.one))
        )
        assertEquals(
            false,
            inequality.isSatisfied(mapOf<Symbol, Flt64>(x to Flt64(3.0), y to Flt64.zero))
        )

        val order = listOf(y, x)
        assertEquals(
            true,
            inequality.isSatisfiedOrdered(order, listOf(Flt64.one, Flt64(2.0)))
        )
        assertEquals(
            false,
            inequality.isSatisfiedOrdered(order, listOf(Flt64.zero, Flt64(3.0)))
        )
    }

    @Test
    fun linearInequalityAliasNamesShouldWork() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val lhs = LinearPolynomial<Flt64>(listOf(LinearMonomial<Flt64>(Flt64.two, x)), Flt64.one)
        val rhs = LinearPolynomial<Flt64>(listOf(LinearMonomial<Flt64>(Flt64.one, y)), Flt64.zero)

        val leqIneq = lhs leq rhs
        assertEquals(Comparison.LE, leqIneq.comparison)

        val geqIneq = lhs geq rhs
        assertEquals(Comparison.GE, geqIneq.comparison)

        val neqIneq = lhs neq rhs
        assertEquals(Comparison.NE, neqIneq.comparison)

        val lsIneq = lhs ls rhs
        assertEquals(Comparison.LT, lsIneq.comparison)

        val grIneq = lhs gr rhs
        assertEquals(Comparison.GT, grIneq.comparison)
    }

    @Test
    fun quadraticInequalityAliasNamesShouldWork() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val q = QuadraticPolynomial<Flt64>(
            listOf(QuadraticMonomial<Flt64>(coefficient = Flt64.one, symbol1 = x, symbol2 = y)),
            Flt64.one
        )
        val l = LinearPolynomial<Flt64>(
            listOf(LinearMonomial<Flt64>(Flt64.two, x)),
            Flt64.zero
        )

        val leqIneq = q leq l
        assertEquals(Comparison.LE, leqIneq.comparison)

        val geqIneq = q geq l
        assertEquals(Comparison.GE, geqIneq.comparison)

        val neqIneq = q neq l
        assertEquals(Comparison.NE, neqIneq.comparison)

        val lsIneq = q ls l
        assertEquals(Comparison.LT, lsIneq.comparison)

        val grIneq = q gr l
        assertEquals(Comparison.GT, grIneq.comparison)
    }

    @Test
    fun canonicalInequalityShouldSupportSatisfiabilityCheck() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val inequality = CanonicalInequality<Flt64>(
            lhs = CanonicalPolynomial(
                monomials = listOf(
                    CanonicalMonomial(Flt64.one, listOf(x, y))
                ),
                constant = Flt64.one
            ),
            rhs = CanonicalPolynomial(constant = Flt64(5.0)),
            comparison = Comparison.LE
        )

        assertEquals(
            true,
            inequality.isSatisfied(mapOf<Symbol, Flt64>(x to Flt64.one, y to Flt64(2.0)))
        )
        assertEquals(
            false,
            inequality.isSatisfied(mapOf<Symbol, Flt64>(x to Flt64(3.0), y to Flt64(2.0)))
        )

        val order = listOf(y, x)
        assertEquals(
            true,
            inequality.isSatisfiedOrdered(order, listOf(Flt64(2.0), Flt64.one))
        )
        assertEquals(
            false,
            inequality.isSatisfiedOrdered(order, listOf(Flt64(2.0), Flt64(3.0)))
        )
    }
}
