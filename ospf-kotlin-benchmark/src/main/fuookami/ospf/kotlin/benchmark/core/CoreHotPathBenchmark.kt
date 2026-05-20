package fuookami.ospf.kotlin.benchmark.core

import fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix
import fuookami.ospf.kotlin.core.model.intermediate.SparseVector
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.Symbol
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
 * core 热点路径基准
 * Core hot path benchmark
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class CoreHotPathBenchmark {
    @Param("small", "medium")
    lateinit var dataset: String

    private lateinit var linearFlattenData: List<LinearFlattenData<Flt64>>
    private lateinit var quadraticFlattenData: List<QuadraticFlattenData<Flt64>>
    private lateinit var sparseMatrix: SparseMatrix<Flt64>

    @Setup
    fun setup() {
        val variableCount = if (dataset == "small") 64 else 256
        val block = if (dataset == "small") 16 else 32

        val vars = List(variableCount) { BinVar("b$it") }

        linearFlattenData = List(block) { bi ->
            val monomials = buildList {
                for (i in vars.indices) {
                    add(LinearMonomial(Flt64(((i + bi) % 5 + 1).toDouble()), vars[i]))
                }
            }
            LinearFlattenData(
                monomials = monomials,
                constant = Flt64(bi.toDouble())
            )
        }

        quadraticFlattenData = List(block) { bi ->
            val monomials = buildList {
                for (i in vars.indices) {
                    val left = vars[i]
                    val right = vars[(i * 13 + bi) % vars.size]
                    add(QuadraticMonomial(Flt64(((i % 3) + 1).toDouble()), left, right))
                }
            }
            QuadraticFlattenData(
                monomials = monomials,
                constant = Flt64.zero
            )
        }

        sparseMatrix = SparseMatrix()
        val rows = if (dataset == "small") 128 else 512
        val cols = if (dataset == "small") 256 else 1024
        for (r in 0 until rows) {
            val row = SparseVector<Flt64>()
            var c = r % 5
            while (c < cols) {
                row.add(c, Flt64(((c + r) % 11 + 1).toDouble()))
                c += 17
            }
            sparseMatrix.addRow(row)
        }
    }

    /**
     * 对应 P19 flatten merge 单趟累积热点。
     * Maps to the P19 flatten merge one-pass accumulation hot path.
     */
    @Benchmark
    fun mergeLinearFlattenDataLikeP19(): Int {
        val merged = HashMap<Any, Flt64>()
        var constant = Flt64.zero
        for (data in linearFlattenData) {
            constant += data.constant
            for (monomial in data.monomials) {
                val key = monomial.symbol
                merged[key] = (merged[key] ?: Flt64.zero) + monomial.coefficient
            }
        }
        return merged.size + if (constant >= Flt64.zero) 1 else 0
    }

    /**
     * 对应 P19 quadratic flatten merge 聚合热点。
     * Maps to the P19 quadratic flatten merge aggregation hot path.
     */
    @Benchmark
    fun mergeQuadraticFlattenDataLikeP19(): Int {
        val merged = HashMap<Pair<Any, Any?>, Flt64>()
        for (data in quadraticFlattenData) {
            for (monomial in data.monomials) {
                val s1 = monomial.symbol1
                val s2 = monomial.symbol2
                val key = if (s2 == null || compareSymbolKey(s1, s2) <= 0) {
                    s1 to s2
                } else {
                    s2 to s1
                }
                merged[key] = (merged[key] ?: Flt64.zero) + monomial.coefficient
            }
        }
        return merged.size
    }

    /**
     * 对应 P19 SparseMatrix.transpose 单趟 maxCol + 填充热点。
     * Maps to the P19 SparseMatrix.transpose one-pass maxCol + fill hot path.
     */
    @Benchmark
    fun sparseMatrixTranspose(): Int {
        return sparseMatrix.transpose().numRows()
    }

    private fun compareSymbolKey(lhs: Symbol, rhs: Symbol): Int {
        val id = lhs.name.compareTo(rhs.name)
        return if (id != 0) id else System.identityHashCode(lhs).compareTo(System.identityHashCode(rhs))
    }
}
