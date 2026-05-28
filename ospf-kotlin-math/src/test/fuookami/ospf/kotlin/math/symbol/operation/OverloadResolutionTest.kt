package fuookami.ospf.kotlin.math.symbol.operation

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Compile-level tests: verify Kotlin overload resolution does not regress
 * when both generic math-layer operators and Flt64-specific adapter operators
 * are in scope simultaneously.
 */
class OverloadResolutionTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    @Test
    fun symbolPlusSymbolShouldResolve() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val result: LinearPolynomial<Flt64> = x + y
        assertNotNull(result)
        assertEquals(2, result.monomials.size)
    }

    @Test
    fun symbolMinusSymbolShouldResolve() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val result: LinearPolynomial<Flt64> = x - y
        assertNotNull(result)
        assertEquals(2, result.monomials.size)
    }

    @Test
    fun flt64TimesSymbolShouldResolve() {
        val x = TestSymbol("x")
        val result: LinearMonomial<Flt64> = Flt64(2.0) * x
        assertNotNull(result)
        assertEquals(Flt64(2.0), result.coefficient)
    }

    @Test
    fun symbolTimesFlt64ShouldResolve() {
        val x = TestSymbol("x")
        val result: LinearMonomial<Flt64> = x * Flt64(3.0)
        assertNotNull(result)
        assertEquals(Flt64(3.0), result.coefficient)
    }

    @Test
    fun intTimesSymbolShouldResolve() {
        val x = TestSymbol("x")
        val result: LinearMonomial<Flt64> = 2 * x
        assertNotNull(result)
        assertEquals(Flt64(2.0), result.coefficient)
    }

    @Test
    fun symbolTimesIntShouldResolve() {
        val x = TestSymbol("x")
        val result: LinearMonomial<Flt64> = x * 3
        assertNotNull(result)
        assertEquals(Flt64(3.0), result.coefficient)
    }

    @Test
    fun doubleTimesSymbolShouldResolve() {
        val x = TestSymbol("x")
        val result: LinearMonomial<Flt64> = 2.5 * x
        assertNotNull(result)
        assertEquals(Flt64(2.5), result.coefficient)
    }

    @Test
    fun symbolTimesDoubleShouldResolve() {
        val x = TestSymbol("x")
        val result: LinearMonomial<Flt64> = x * 3.5
        assertNotNull(result)
        assertEquals(Flt64(3.5), result.coefficient)
    }

    @Test
    fun intMinusSymbolShouldResolve() {
        val x = TestSymbol("x")
        val result: LinearPolynomial<Flt64> = 5 - x
        assertNotNull(result)
        assertEquals(Flt64(5.0), result.constant)
    }

    @Test
    fun doubleMinusSymbolShouldResolve() {
        val x = TestSymbol("x")
        val result: LinearPolynomial<Flt64> = 5.0 - x
        assertNotNull(result)
        assertEquals(Flt64(5.0), result.constant)
    }

    @Test
    fun linearMonomialTimesSymbolShouldResolve() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val m = LinearMonomial(Flt64(2.0), x)
        val result: QuadraticMonomial<Flt64> = m * y
        assertNotNull(result)
    }

    @Test
    fun symbolComparisonWithFlt64ShouldResolve() {
        val x = TestSymbol("x")
        val ineq = x le Flt64(10.0)
        assertNotNull(ineq)
        assertEquals(Comparison.LE, ineq.comparison)
    }

    @Test
    fun sumFunctionShouldResolveFromPolynomialPackage() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val monomials = listOf(LinearMonomial(Flt64.one, x), LinearMonomial(Flt64.one, y))
        val result: LinearPolynomial<Flt64> = sum(monomials)
        assertNotNull(result)
        assertEquals(2, result.monomials.size)
    }

    @Test
    fun qsumFunctionShouldResolveFromPolynomialPackage() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val monomials = listOf(
            QuadraticMonomial.quadratic(Flt64.one, x, y),
            QuadraticMonomial.linear(Flt64(2.0), x)
        )
        val result: QuadraticPolynomial<Flt64> = qsum(monomials)
        assertNotNull(result)
        assertEquals(2, result.monomials.size)
    }

    @Test
    fun linearPolynomialQuickConstructorShouldResolve() {
        val x = TestSymbol("x")
        val poly: LinearPolynomial<Flt64> = LinearPolynomial(x)
        assertNotNull(poly)
        assertEquals(1, poly.monomials.size)
    }

    @Test
    fun quadraticPolynomialQuickConstructorShouldResolve() {
        val x = TestSymbol("x")
        val poly: QuadraticPolynomial<Flt64> = QuadraticPolynomial(x)
        assertNotNull(poly)
        assertEquals(1, poly.monomials.size)
    }

    @Test
    fun linearPolynomialPlusIntShouldResolve() {
        val x = TestSymbol("x")
        val poly = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val result = poly + 5
        assertNotNull(result)
        assertEquals(Flt64(5.0), result.constant)
    }

    @Test
    fun linearPolynomialTimesDoubleShouldResolve() {
        val x = TestSymbol("x")
        val poly = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.one)
        val result = poly * 2.0
        assertNotNull(result)
        assertEquals(Flt64(2.0), result.monomials[0].coefficient)
        assertEquals(Flt64(2.0), result.constant)
    }

    @Test
    fun symbolPlusIntShouldResolve() {
        val x = TestSymbol("x")
        val result: LinearPolynomial<Flt64> = x + 5
        assertNotNull(result)
        assertEquals(Flt64(5.0), result.constant)
    }

    @Test
    fun symbolMinusIntShouldResolve() {
        val x = TestSymbol("x")
        val result: LinearPolynomial<Flt64> = x - 5
        assertNotNull(result)
        assertEquals(Flt64(-5.0), result.constant)
    }

    @Test
    fun symbolPlusDoubleShouldResolve() {
        val x = TestSymbol("x")
        val result: LinearPolynomial<Flt64> = x + 2.5
        assertNotNull(result)
        assertEquals(Flt64(2.5), result.constant)
    }

    @Test
    fun symbolMinusDoubleShouldResolve() {
        val x = TestSymbol("x")
        val result: LinearPolynomial<Flt64> = x - 2.5
        assertNotNull(result)
        assertEquals(Flt64(-2.5), result.constant)
    }

    @Test
    fun intPlusSymbolShouldResolve() {
        val x = TestSymbol("x")
        val result: LinearPolynomial<Flt64> = 5 + x
        assertNotNull(result)
        assertEquals(Flt64(5.0), result.constant)
    }

    @Test
    fun doublePlusSymbolShouldResolve() {
        val x = TestSymbol("x")
        val result: LinearPolynomial<Flt64> = 2.5 + x
        assertNotNull(result)
        assertEquals(Flt64(2.5), result.constant)
    }

    @Test
    fun intTimesLinearPolynomialShouldResolve() {
        val x = TestSymbol("x")
        val poly = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.one)
        val result = 2 * poly
        assertNotNull(result)
        assertEquals(Flt64(2.0), result.monomials[0].coefficient)
        assertEquals(Flt64(2.0), result.constant)
    }

    @Test
    fun doubleTimesLinearPolynomialShouldResolve() {
        val x = TestSymbol("x")
        val poly = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.one)
        val result = 1.5 * poly
        assertNotNull(result)
        assertEquals(Flt64(1.5), result.monomials[0].coefficient)
        assertEquals(Flt64(1.5), result.constant)
    }

    @Test
    fun sumVarsShouldResolve() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val items = listOf(x, y)
        val result: LinearPolynomial<Flt64> = sumVars(items) { it }
        assertNotNull(result)
        assertEquals(2, result.monomials.size)
    }

    @Test
    fun qsumVarsShouldResolve() {
        val x = TestSymbol("x")
        val y = TestSymbol("y")
        val items = listOf(x, y)
        val result: QuadraticPolynomial<Flt64> = qsumVars(items) { it }
        assertNotNull(result)
        assertEquals(2, result.monomials.size)
    }

    @Test
    fun genericInequalityAliasNamesShouldResolve() {
        val x = TestSymbol("x")
        val poly = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val leqIneq = poly leq poly
        val geqIneq = poly geq poly
        val neqIneq = poly neq poly
        val lsIneq = poly ls poly
        val grIneq = poly gr poly
        assertNotNull(leqIneq)
        assertNotNull(geqIneq)
        assertNotNull(neqIneq)
        assertNotNull(lsIneq)
        assertNotNull(grIneq)
    }
}
