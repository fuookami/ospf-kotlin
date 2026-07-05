package fuookami.ospf.kotlin.benchmark.math

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.operation.combineLinearTerms
import fuookami.ospf.kotlin.math.symbol.operation.combineQuadraticTerms
import fuookami.ospf.kotlin.math.symbol.operation.combineTerms
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.plusAssign
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import java.util.concurrent.TimeUnit

/**
 * math symbol combine 热点基准
 * Benchmark for math symbol combine hot paths
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class SymbolCombineBenchmark {
    @Param("small", "medium", "large")
    lateinit var dataset: String

    private data class BenchSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    private lateinit var symbols: List<BenchSymbol>
    private lateinit var linearMonomials: List<LinearMonomial<Flt64>>
    private lateinit var quadraticMonomials: List<QuadraticMonomial<Flt64>>
    private lateinit var linearPolynomial: LinearPolynomial<Flt64>
    private lateinit var quadraticPolynomial: QuadraticPolynomial<Flt64>

    @Setup
    fun setup() {
        val symbolCount = when (dataset) {
            "small" -> 64
            "medium" -> 256
            "large" -> 768
            else -> 64
        }
        symbols = List(symbolCount) { BenchSymbol("x$it") }

        val repeat = when (dataset) {
            "small" -> 8
            "medium" -> 16
            "large" -> 24
            else -> 8
        }
        linearMonomials = buildList(symbolCount * repeat) {
            for (r in 0 until repeat) {
                for (i in symbols.indices) {
                    val sign = if ((i + r) % 2 == 0) 1.0 else -1.0
                    add(LinearMonomial(Flt64(sign * ((i % 7) + 1.0)), symbols[i]))
                }
            }
        }

        quadraticMonomials = buildList(symbolCount * 2) {
            for (i in symbols.indices) {
                val a = symbols[i]
                val b = symbols[(i * 17 + 11) % symbols.size]
                add(QuadraticMonomial(Flt64(((i % 5) + 1).toDouble()), a, b))
                add(QuadraticMonomial(Flt64(((i % 5) + 1).toDouble()), b, a))
            }
        }

        linearPolynomial = LinearPolynomial(linearMonomials, Flt64.one)
        quadraticPolynomial = QuadraticPolynomial(quadraticMonomials, Flt64.zero)
    }

    @Benchmark
    fun combineLinearIterable(): Int {
        return linearMonomials.combineTerms().size
    }

    @Benchmark
    fun combineLinearPolynomialGeneric(): Int {
        return linearPolynomial.combineLinearTerms(zero = Flt64.zero).monomials.size
    }

    @Benchmark
    fun combineQuadraticIterable(): Int {
        return quadraticMonomials.combineTerms().size
    }

    @Benchmark
    fun combineQuadraticPolynomialGeneric(): Int {
        return quadraticPolynomial.combineQuadraticTerms(zero = Flt64.zero).monomials.size
    }

    @Benchmark
    fun mutableLinearAccumulateAndCombine(): Int {
        val mutable = MutableLinearPolynomial<Flt64>(constant = Flt64.zero)
        for (monomial in linearMonomials) {
            mutable += monomial
        }
        mutable.combineTerms(zero = Flt64.zero)
        return mutable.monomials.size
    }

    /**
     * 可变二次多项式累加后合并基准
     * Benchmark for mutable quadratic polynomial accumulate-then-combine
     *
     * @return 合并后单项式数量 / monomial count after combining
     */
    @Benchmark
    fun mutableQuadraticAccumulateAndCombine(): Int {
        val mutable = MutableQuadraticPolynomial<Flt64>(constant = Flt64.zero)
        for (monomial in quadraticMonomials) {
            mutable += monomial
        }
        mutable.combineTerms(zero = Flt64.zero)
        return mutable.monomials.size
    }
}
