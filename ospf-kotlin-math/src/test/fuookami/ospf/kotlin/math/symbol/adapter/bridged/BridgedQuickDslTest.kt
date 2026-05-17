package fuookami.ospf.kotlin.math.symbol.adapter.bridged

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Rtn64
import fuookami.ospf.kotlin.math.algebra.number.RtnX
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BridgedQuickDslTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun fltXBridgedQuickDslShouldWork() {
        val dsl = BridgedQuickDsl(FltX)
        val x = TestSymbol("x")
        val poly: LinearPolynomial<FltX> = dsl.LinearPolynomial(x)
        assertNotNull(poly)
        assertEquals(FltX.one, poly.monomials[0].coefficient)
    }

    @Test
    fun rtn64BridgedQuickDslShouldWork() {
        val dsl = BridgedQuickDsl(Rtn64)
        val x = TestSymbol("x")
        val poly: LinearPolynomial<Rtn64> = dsl.LinearPolynomial(x)
        assertNotNull(poly)
        assertEquals(Rtn64.one, poly.monomials[0].coefficient)
    }

    @Test
    fun rtnXBridgedQuickDslShouldWork() {
        val dsl = BridgedQuickDsl(RtnX)
        val x = TestSymbol("x")
        val poly: LinearPolynomial<RtnX> = dsl.LinearPolynomial(x)
        assertNotNull(poly)
        assertEquals(RtnX.one, poly.monomials[0].coefficient)
    }

    @Test
    fun flt64BridgedQuickDslShouldWork() {
        val dsl = BridgedQuickDsl(Flt64)
        val x = TestSymbol("x")
        val poly: LinearPolynomial<Flt64> = dsl.LinearPolynomial(x)
        assertNotNull(poly)
        assertEquals(Flt64.one, poly.monomials[0].coefficient)
    }

    @Test
    fun bridgedSumVarsShouldWork() {
        val dsl = BridgedQuickDsl(FltX)
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val items = listOf(x, y)
        val poly: LinearPolynomial<FltX> = dsl.sumVars(items) { it }
        assertNotNull(poly)
        assertEquals(2, poly.monomials.size)
    }

    @Test
    fun bridgedQSumVarsShouldWork() {
        val dsl = BridgedQuickDsl(Rtn64)
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val items = listOf(x, y)
        val poly: QuadraticPolynomial<Rtn64> = dsl.qsumVars(items) { it }
        assertNotNull(poly)
        assertEquals(2, poly.monomials.size)
    }

    @Test
    fun bridgedQuadraticPolynomialShouldWork() {
        val dsl = BridgedQuickDsl(FltX)
        val x = TestSymbol("x")
        val poly: QuadraticPolynomial<FltX> = dsl.QuadraticPolynomial(x)
        assertNotNull(poly)
        assertEquals(1, poly.monomials.size)
    }

    @Test
    fun bridgedQuickOpsShouldWork() {
        val ops = BridgedQuickOps(FltX)
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val result: LinearPolynomial<FltX> = with(ops) { x + y }
        assertNotNull(result)
        assertEquals(2, result.monomials.size)
    }

    @Test
    fun bridgedInequalityShouldWork() {
        val ineq = BridgedInequality(FltX)
        val x = TestSymbol("x")
        val result = with(ineq) { x le Flt64(10.0) }
        assertNotNull(result)
        assertEquals(Comparison.LE, result.comparison)
    }

    @Test
    fun bridgedInequalityAliasNamesShouldWork() {
        val ineq = BridgedInequality(Rtn64)
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
    fun bridgedInequalityIntComparisonShouldWork() {
        val ineq = BridgedInequality(FltX)
        val x = TestSymbol("x")
        val result = with(ineq) { x le 10 }
        assertNotNull(result)
        assertEquals(Comparison.LE, result.comparison)
    }

    @Test
    fun bridgedInequalityDoubleComparisonShouldWork() {
        val ineq = BridgedInequality(Rtn64)
        val x = TestSymbol("x")
        val result = with(ineq) { x le 10.0 }
        assertNotNull(result)
        assertEquals(Comparison.LE, result.comparison)
    }
}