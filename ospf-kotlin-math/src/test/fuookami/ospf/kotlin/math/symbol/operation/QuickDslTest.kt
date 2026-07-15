package fuookami.ospf.kotlin.math.symbol.operation

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*

class QuickDslTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun fltXQuickDslShouldWork() {
        val dsl = QuickDsl(FltX)
        val x = TestSymbol("x")
        val poly: LinearPolynomial<FltX> = dsl.LinearPolynomial(x)
        assertNotNull(poly)
        assertEquals(FltX.one, poly.monomials[0].coefficient)
    }

    @Test
    fun rtn64QuickDslShouldWork() {
        val dsl = QuickDsl(Rtn64)
        val x = TestSymbol("x")
        val poly: LinearPolynomial<Rtn64> = dsl.LinearPolynomial(x)
        assertNotNull(poly)
        assertEquals(Rtn64.one, poly.monomials[0].coefficient)
    }

    @Test
    fun rtnXQuickDslShouldWork() {
        val dsl = QuickDsl(RtnX)
        val x = TestSymbol("x")
        val poly: LinearPolynomial<RtnX> = dsl.LinearPolynomial(x)
        assertNotNull(poly)
        assertEquals(RtnX.one, poly.monomials[0].coefficient)
    }

    @Test
    fun flt64QuickDslShouldWork() {
        val dsl = QuickDsl(Flt64)
        val x = TestSymbol("x")
        val poly: LinearPolynomial<Flt64> = dsl.LinearPolynomial(x)
        assertNotNull(poly)
        assertEquals(Flt64.one, poly.monomials[0].coefficient)
    }

    @Test
    fun sumVarsShouldWork() {
        val dsl = QuickDsl(FltX)
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val items = listOf(x, y)
        val poly: LinearPolynomial<FltX> = dsl.sumVars(items) { it }
        assertNotNull(poly)
        assertEquals(2, poly.monomials.size)
    }

    @Test
    fun qSumVarsShouldWork() {
        val dsl = QuickDsl(Rtn64)
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val items = listOf(x, y)
        val poly: QuadraticPolynomial<Rtn64> = dsl.qsumVars(items) { it }
        assertNotNull(poly)
        assertEquals(2, poly.monomials.size)
    }

    @Test
    fun quadraticPolynomialShouldWork() {
        val dsl = QuickDsl(FltX)
        val x = TestSymbol("x")
        val poly: QuadraticPolynomial<FltX> = dsl.QuadraticPolynomial(x)
        assertNotNull(poly)
        assertEquals(1, poly.monomials.size)
    }

    @Test
    fun quickOpsShouldWork() {
        val ops = QuickOps(FltX)
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val result: LinearPolynomial<FltX> = with(ops) { x + y }
        assertNotNull(result)
        assertEquals(2, result.monomials.size)
    }

    @Test
    fun inequalityShouldWork() {
        val ineq = InequalityDsl(FltX)
        val x = TestSymbol("x")
        val result = with(ineq) { x le Flt64(10.0) }
        assertNotNull(result)
        assertEquals(Comparison.LE, result.comparison)
    }

    @Test
    fun inequalityAliasNamesShouldWork() {
        val ineq = InequalityDsl(Rtn64)
        val x = TestSymbol("x")
        val leqResult = with(ineq) { x leq Flt64(10.0) }
        val geqResult = with(ineq) { x geq Flt64(0.0) }
        val neqResult = with(ineq) { x neq Flt64(5.0) }
        assertNotNull(leqResult)
        assertNotNull(geqResult)
        assertNotNull(neqResult)
        assertEquals(Comparison.LE, leqResult.comparison)
        assertEquals(Comparison.GE, geqResult.comparison)
        assertEquals(Comparison.NE, neqResult.comparison)
    }

    @Test
    fun inequalityIntComparisonShouldWork() {
        val ineq = InequalityDsl(FltX)
        val x = TestSymbol("x")
        val result = with(ineq) { x le 10 }
        assertNotNull(result)
        assertEquals(Comparison.LE, result.comparison)
    }

    @Test
    fun inequalityDoubleComparisonShouldWork() {
        val ineq = InequalityDsl(Rtn64)
        val x = TestSymbol("x")
        val result = with(ineq) { x le 10.0 }
        assertNotNull(result)
        assertEquals(Comparison.LE, result.comparison)
    }
}
