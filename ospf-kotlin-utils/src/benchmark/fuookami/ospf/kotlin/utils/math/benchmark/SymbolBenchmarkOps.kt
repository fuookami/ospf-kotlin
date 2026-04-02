package fuookami.ospf.kotlin.utils.math.benchmark

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.operation.compileEval
import fuookami.ospf.kotlin.utils.math.symbol.operation.compileGradient
import fuookami.ospf.kotlin.utils.math.symbol.operation.combineCanonicalTerms
import fuookami.ospf.kotlin.utils.math.symbol.operation.combineTerms
import fuookami.ospf.kotlin.utils.math.symbol.operation.evaluateOrdered
import fuookami.ospf.kotlin.utils.math.symbol.operation.gradient
import fuookami.ospf.kotlin.utils.math.symbol.operation.toMatrixForm
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.plus
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.minus
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.times
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.div

object SymbolBenchmarkOps {
    private data class BenchSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    data class SymbolBenchmarkState(
        val order: List<Symbol>,
        val values: List<Flt64>,
        val canonical: CanonicalPolynomial<Flt64>
    )

    // ===== S-PERF-2: Polynomial arithmetic benchmarks =====

    data class PolynomialArithmeticState(
        val poly1: CanonicalPolynomial<Flt64>,
        val poly2: CanonicalPolynomial<Flt64>,
        val scalar: Flt64,
        val monomials: List<CanonicalMonomial<Flt64>>  // for combineTerms benchmark
    )

    @JvmStatic
    fun createState(): SymbolBenchmarkState {
        val x = BenchSymbol("x")
        val y = BenchSymbol("y")
        val z = BenchSymbol("z")
        val order = listOf<Symbol>(x, y, z)
        val values = listOf(Flt64(1.5), Flt64(2.5), Flt64(3.5))
        val canonical = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial(Flt64(3.0), listOf(x, x)),
                CanonicalMonomial(Flt64(2.0), listOf(x, y)),
                CanonicalMonomial(Flt64(-4.0), listOf(y, z)),
                CanonicalMonomial(Flt64(5.0), listOf(z, z)),
                CanonicalMonomial(Flt64(7.0), listOf(x))
            ),
            constant = Flt64(11.0)
        ).combineTerms()
        return SymbolBenchmarkState(order, values, canonical)
    }

    @JvmStatic
    fun createArithmeticState(): PolynomialArithmeticState {
        val x = BenchSymbol("x")
        val y = BenchSymbol("y")
        val z = BenchSymbol("z")
        val poly1 = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial(Flt64(3.0), listOf(x, x)),
                CanonicalMonomial(Flt64(2.0), listOf(x, y)),
                CanonicalMonomial(Flt64(5.0), listOf(x))
            ),
            constant = Flt64(10.0)
        )
        val poly2 = CanonicalPolynomial<Flt64>(
            monomials = listOf(
                CanonicalMonomial(Flt64(-1.0), listOf(x, x)),
                CanonicalMonomial(Flt64(4.0), listOf(y, z)),
                CanonicalMonomial(Flt64(2.0), listOf(z))
            ),
            constant = Flt64(5.0)
        )
        // Generate many monomials for combineTerms stress test
        val monomials = (0 until 100).flatMap { i ->
            listOf(
                CanonicalMonomial(Flt64(i.toDouble()), listOf(x, x)),
                CanonicalMonomial(Flt64(i.toDouble() * 2), listOf(x, y)),
                CanonicalMonomial(Flt64(-i.toDouble()), listOf(y, z))
            )
        }
        return PolynomialArithmeticState(poly1, poly2, Flt64(2.5), monomials)
    }

    // ===== Original benchmarks =====

    @JvmStatic
    fun compileEvalHash(state: SymbolBenchmarkState): Int {
        return state.canonical.compileEval(state.order).hashCode()
    }

    @JvmStatic
    fun compileGradientHash(state: SymbolBenchmarkState): Int {
        return state.canonical.compileGradient(state.order).hashCode()
    }

    @JvmStatic
    fun evaluateOrderedAsDouble(state: SymbolBenchmarkState): Double {
        return state.canonical.evaluateOrdered(state.order, state.values).toDouble()
    }

    @JvmStatic
    fun gradientSize(state: SymbolBenchmarkState): Int {
        return state.canonical.gradient(state.order).size
    }

    @JvmStatic
    fun matrixFormSize(state: SymbolBenchmarkState): Int {
        val form = state.canonical.toMatrixForm(state.order)
        return form.q.size + form.c.size
    }

    @JvmStatic
    fun invokeCompiledEvalAsDouble(state: SymbolBenchmarkState): Double {
        val compiled = state.canonical.compileEval(state.order)
        return compiled(state.values).toDouble()
    }

    @JvmStatic
    fun invokeCompiledGradientSize(state: SymbolBenchmarkState): Int {
        val compiled = state.canonical.compileGradient(state.order)
        return compiled(state.values).size
    }

    // ===== S-PERF-2: New polynomial arithmetic benchmarks =====

    @JvmStatic
    fun polynomialPlus(state: PolynomialArithmeticState): Int {
        return (state.poly1 + state.poly2).monomials.size
    }

    @JvmStatic
    fun polynomialMinus(state: PolynomialArithmeticState): Int {
        return (state.poly1 - state.poly2).monomials.size
    }

    @JvmStatic
    fun polynomialTimesScalar(state: PolynomialArithmeticState): Int {
        return (state.poly1 * state.scalar).monomials.size
    }

    @JvmStatic
    fun polynomialDivScalar(state: PolynomialArithmeticState): Int {
        return (state.poly1 / state.scalar).monomials.size
    }

    @JvmStatic
    fun combineTermsStress(state: PolynomialArithmeticState): Int {
        return state.monomials.combineCanonicalTerms().size
    }
}