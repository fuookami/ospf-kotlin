package fuookami.ospf.kotlin.benchmark.coreplugin

import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix
import fuookami.ospf.kotlin.core.model.intermediate.SparseVector
import fuookami.ospf.kotlin.core.variable.Binary
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.core.variable.Integer
import fuookami.ospf.kotlin.math.algebra.number.Flt64
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
 * core-plugin dump 数据准备热点基准（不调用真实 solver）
 * Core-plugin dump data preparation benchmark (without real solver calls)
 *
 * 衡量变量转储、目标系数收集、约束分块和稀疏行扫描等热点路径的性能。
 * Measures performance of hot paths including variable dumping, objective coefficient
 * collection, constraint segmentation, and sparse row traversal.
 *
 * @property dataset 数据集规模标识（small/medium/large） / dataset size identifier (small/medium/large)
 * @property variables 变量列表 / list of variables
 * @property objectiveCells 目标函数系数单元列表 / list of objective function coefficient cells
 * @property constraintsLhs 约束左侧稀疏矩阵 / constraint left-hand side sparse matrix
 * @property constraintsRhs 约束右侧值列表 / constraint right-hand side values
 * @property constraintsSigns 约束关系符号列表 / constraint relation signs
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class CorePluginDumpBenchmark {
    @Param("small", "medium", "large")
    lateinit var dataset: String

    private lateinit var variables: List<Variable>
    private lateinit var objectiveCells: List<ObjectiveCell>
    private lateinit var constraintsLhs: SparseMatrix<Flt64>
    private lateinit var constraintsRhs: List<Flt64>
    private lateinit var constraintsSigns: List<ConstraintRelation>

    /** JMH 基准初始化，根据 dataset 参数构造变量、目标系数和约束矩阵 / JMH setup initializing variables, objective cells, and constraint matrix based on dataset parameter */
    @Setup
    fun setup() {
        val variableCount = when (dataset) {
            "small" -> 128
            "medium" -> 768
            "large" -> 2048
            else -> 128
        }
        val constraintCount = when (dataset) {
            "small" -> 256
            "medium" -> 2048
            "large" -> 8192
            else -> 256
        }
        val stride = when (dataset) {
            "small" -> 11
            "medium" -> 17
            "large" -> 23
            else -> 11
        }

        variables = List(variableCount) { i ->
            val type = when (i % 3) {
                0 -> Binary
                1 -> Integer
                else -> Continuous
            }
            val lowerBound = if (type == Binary) {
                Flt64.zero
            } else {
                Flt64(-50.0 + (i % 5))
            }
            val upperBound = if (type == Binary) {
                Flt64.one
            } else {
                Flt64(500.0 + (i % 7))
            }
            val initialResult = if ((i % 4) == 0) {
                Flt64((i % 13).toDouble())
            } else {
                null
            }
            Variable(
                index = i,
                lowerBound = lowerBound,
                upperBound = upperBound,
                type = type,
                origin = null,
                name = "v$i",
                initialResult = initialResult
            )
        }

        objectiveCells = List(variableCount) { i ->
            ObjectiveCell(
                colIndex = i,
                coefficient = Flt64(((i % 9) + 1).toDouble())
            )
        }

        constraintsLhs = SparseMatrix()
        constraintsRhs = List(constraintCount) { i -> Flt64((i % 37).toDouble()) }
        constraintsSigns = List(constraintCount) { i ->
            when (i % 3) {
                0 -> ConstraintRelation.GreaterEqual
                1 -> ConstraintRelation.LessEqual
                else -> ConstraintRelation.Equal
            }
        }

        for (rowIndex in 0 until constraintCount) {
            val row = SparseVector<Flt64>()
            var col = rowIndex % stride
            while (col < variableCount) {
                row.add(col, Flt64(((col + rowIndex) % 19 + 1).toDouble()))
                col += stride
            }
            constraintsLhs.addRow(row)
        }
    }

    /**
     * 对应各 solver dump 的变量 lower/upper/name/initial 预处理。
     * Maps to variable lower/upper/name/initial preprocessing in solver dumps.
     */
    @Benchmark
    fun prepareVariableDumpingDataHotPath(): Int {
        val data = prepareVariableDumpingDataLikeSolver(
            variables = variables,
            _scopeName = "benchmark.core-plugin"
        )
        return data.initialResults.size + data.names.size
    }

    /**
     * 对应各 solver dump 的 objective 系数到 double 的收集过程。
     * Maps to objective coefficient collection to double in solver dumps.
     *
     * @return 系数累加校验和 / coefficient accumulation checksum
     */
    @Benchmark
    fun collectObjectiveCoefficients(): Double {
        var checksum = 0.0
        for (cell in objectiveCells) {
            checksum += cell.coefficient.toDouble()
        }
        return checksum
    }

    /**
     * 对应各 solver dump 的约束分块大小推导。
     * Maps to constraint segment size derivation in solver dumps.
     *
     * @return 约束分块数量 / number of constraint segments
     */
    @Benchmark
    fun computeConstraintSegments(): Int {
        val segment = computeConstraintSegmentSizeLikeSolver(constraintsSigns.size, availableProcessors = 12)
        return (constraintsSigns.size + segment - 1) / segment
    }

    /**
     * 对应各 solver dump 的 sparse row 扫描与界限转换。
     * Maps to sparse row traversal and bound conversion in solver dumps.
     *
     * @return 系数与界限累加校验和 / coefficient and bound accumulation checksum
     */
    @Benchmark
    fun walkSparseRowsAndBounds(): Double {
        var checksum = 0.0
        for (rowIndex in constraintsSigns.indices) {
            var lowerBound = Flt64.negativeInfinity
            var upperBound = Flt64.infinity
            when (constraintsSigns[rowIndex]) {
                ConstraintRelation.GreaterEqual -> {
                    lowerBound = constraintsRhs[rowIndex]
                }

                ConstraintRelation.LessEqual -> {
                    upperBound = constraintsRhs[rowIndex]
                }

                ConstraintRelation.Equal -> {
                    lowerBound = constraintsRhs[rowIndex]
                    upperBound = constraintsRhs[rowIndex]
                }
            }
            constraintsLhs.forEachEntry(rowIndex) { colIndex, coefficient ->
                checksum += coefficient.toDouble()
            }
            checksum += lowerBound.toDouble()
            checksum += upperBound.toDouble()
        }
        return checksum
    }

    /**
     * 变量转储数据快照，包含下界、上界、名称和初始结果
     * Variable dumping data snapshot containing lower bounds, upper bounds, names, and initial results
     *
     * @property lowerBounds 变量下界数组 / array of variable lower bounds
     * @property upperBounds 变量上界数组 / array of variable upper bounds
     * @property initialResults 带初始值的变量索引与值对列表 / list of variable index and value pairs with initial results
     */
    private data class VariableDumpingDataSnapshot(
        val lowerBounds: DoubleArray,
        val upperBounds: DoubleArray,
        val names: Array<String>,
        val initialResults: List<Pair<Int, Double>>
    )

    /**
     * 复刻 solver dump 变量数组准备逻辑，不依赖具体 solver API。
     * Mirrors solver dump variable-array preparation without concrete solver APIs.
     *
     * @param variables 变量列表 / list of variables
     * @param _scopeName 作用域名称 / scope name
     * @return 变量转储数据快照 / variable dumping data snapshot
     */
    private fun prepareVariableDumpingDataLikeSolver(
        variables: List<Variable>,
        _scopeName: String
    ): VariableDumpingDataSnapshot {
        val variableAmount = variables.size
        val lowerBounds = DoubleArray(variableAmount)
        val upperBounds = DoubleArray(variableAmount)
        val names = Array(variableAmount) { "" }
        val initialResults = ArrayList<Pair<Int, Double>>()
        for ((col, variable) in variables.withIndex()) {
            lowerBounds[col] = variable.lowerBound.toDouble()
            upperBounds[col] = variable.upperBound.toDouble()
            names[col] = variable.name
            variable.initialResult?.let {
                initialResults.add(col to it.toDouble())
            }
        }
        return VariableDumpingDataSnapshot(
            lowerBounds = lowerBounds,
            upperBounds = upperBounds,
            names = names,
            initialResults = initialResults
        )
    }

    /**
     * 复刻 solver dump 约束分块大小逻辑，保持同等复杂度。
     * Mirrors solver dump constraint segment sizing logic with equivalent complexity.
     *
     * @param constraintSize 约束总数 / total number of constraints
     * @param availableProcessors 可用处理器数 / number of available processors
     * @return 约束分块大小 / constraint segment size
     */
    private fun computeConstraintSegmentSizeLikeSolver(
        constraintSize: Int,
        availableProcessors: Int
    ): Int {
        if (constraintSize <= 0) {
            return 10
        }
        val workerCount = (availableProcessors - 1).coerceAtLeast(1)
        var ratio = constraintSize / workerCount
        if (ratio < 10) {
            return 10
        }
        var segment = 1
        while (ratio >= 10) {
            ratio /= 10
            segment *= 10
        }
        return segment
    }

    /**
     * 目标函数系数单元
     * Objective function coefficient cell
     *
     * @property colIndex 列索引 / column index
     * @property coefficient 系数值 / coefficient value
     */
    private data class ObjectiveCell(
        val colIndex: Int,
        val coefficient: Flt64
    )
}
