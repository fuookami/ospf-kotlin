/** CP build, solve, and portable restore benchmarks. / CP 构建、求解和可移植恢复基准。 */
package fuookami.ospf.kotlin.benchmark.constraintprogramming

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingSnapshotCodec
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalVariable
import fuookami.ospf.kotlin.core.model.constraint_programming.NoOverlap
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingCheckpointCodec
import fuookami.ospf.kotlin.core.solver.constraint_programming.FakeConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingToLinearModelLowerer
import fuookami.ospf.kotlin.core.solver.toSolveReport
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.framework.solver.BendersMasterProblemSolver
import fuookami.ospf.kotlin.framework.solver.BinaryBendersVariable
import fuookami.ospf.kotlin.framework.solver.BinaryBendersVariableBinding
import fuookami.ospf.kotlin.framework.solver.ConstraintProgrammingValueSource
import fuookami.ospf.kotlin.framework.solver.LogicBasedBendersEngine
import fuookami.ospf.kotlin.framework.solver.LogicBasedBendersOptions

/**
 * Small representative CP benchmark fixture. /
 * 小型代表性 CP 基准夹具。
 *
 * The benchmark intentionally uses the Fake solver so it remains executable without a native
 * library. SCIP integration timings are collected by the plugin integration suite separately. /
 * 基准刻意使用 Fake solver，使其不依赖原生库即可执行；SCIP 集成耗时由插件集成套件单独采集。
 *
 * @property dataset Fixture size. / 夹具规模。
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class ConstraintProgrammingBenchmark {
    @Param("small", "medium", "large")
    lateinit var dataset: String

    private lateinit var directModel: ConstraintProgrammingModel
    private lateinit var schedulingModel: ConstraintProgrammingModel
    private lateinit var optionalSchedulingModel: ConstraintProgrammingModel
    private lateinit var variableDurationSchedulingModel: ConstraintProgrammingModel
    private lateinit var bendersMasterModel: fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel<Flt64>
    private lateinit var bendersSubproblemModel: ConstraintProgrammingModel
    private lateinit var bendersEngine: LogicBasedBendersEngine
    private lateinit var snapshotJson: String
    private lateinit var checkpointJson: String
    private lateinit var solver: FakeConstraintProgrammingSolver

    /** Build direct and scheduling fixtures once per benchmark state. / 每个基准状态构建一次直接模型与排程模型。 */
    @Setup
    fun setup() {
        val count = when (dataset) {
            "small" -> 4
            "medium" -> 6
            "large" -> 8
            else -> 4
        }
        directModel = ConstraintProgrammingModel("benchmark-direct", ObjectCategory.Minimum)
        val directVariables = List(count) { index ->
            IntVar("direct-$index").also { variable ->
                directModel.registerVariable(variable, IntegerDomain.boolean)
                val expression = ConstraintProgrammingExpression.Variable(variable)
                directModel.addConstraint(
                    ConstraintProgrammingConstraint.lessOrEqual(expression, Int64.one).value!!,
                    id = "direct-bound-$index"
                )
            }
        }
        directModel.minimize(ConstraintProgrammingExpression.Variable(directVariables.first()))
        snapshotJson = ConstraintProgrammingSnapshotCodec.encode(directModel.snapshot().value!!).value!!

        schedulingModel = ConstraintProgrammingModel("benchmark-scheduling", ObjectCategory.Minimum)
        val intervals = List(count) { index ->
            val start = IntVar("start-$index")
            val end = IntVar("end-$index")
            schedulingModel.registerVariable(start, IntegerDomain.interval(0, count.toLong()).value!!)
            schedulingModel.registerVariable(end, IntegerDomain.interval(1, (count * 2).toLong()).value!!)
            val startExpression = ConstraintProgrammingExpression.Variable(start)
            val endExpression = ConstraintProgrammingExpression.Variable(end)
            val interval = IntervalVariable.fixed(
                id = IntervalId("job-$index"),
                start = startExpression,
                size = Int64.one,
                end = endExpression
            ).value!!
            schedulingModel.registerInterval(interval)
            interval
        }
        schedulingModel.addConstraint(NoOverlap.create(intervals).value!!, id = "schedule-no-overlap")

        optionalSchedulingModel = ConstraintProgrammingModel("benchmark-optional-scheduling", ObjectCategory.Minimum)
        val optionalIntervals = List(3) { index ->
            val start = IntVar("optional-start-$index")
            val end = IntVar("optional-end-$index")
            val presence = fuookami.ospf.kotlin.core.variable.BinVar("optional-presence-$index")
            optionalSchedulingModel.registerVariable(start, IntegerDomain.interval(0, 6).value!!)
            optionalSchedulingModel.registerVariable(end, IntegerDomain.interval(0, 8).value!!)
            optionalSchedulingModel.registerVariable(presence)
            IntervalVariable.fixed(
                id = IntervalId("optional-job-$index"),
                start = ConstraintProgrammingExpression.Variable(start),
                size = Int64.one,
                end = ConstraintProgrammingExpression.Variable(end),
                presence = fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral(presence)
            ).value!!.also(optionalSchedulingModel::registerInterval)
        }
        optionalSchedulingModel.addConstraint(NoOverlap.create(optionalIntervals).value!!, id = "optional-no-overlap")

        variableDurationSchedulingModel = ConstraintProgrammingModel("benchmark-variable-duration", ObjectCategory.Minimum)
        val variableDurationIntervals = List(3) { index ->
            val start = IntVar("variable-start-$index")
            val size = IntVar("variable-size-$index")
            val end = IntVar("variable-end-$index")
            variableDurationSchedulingModel.registerVariable(start, IntegerDomain.interval(0, 6).value!!)
            variableDurationSchedulingModel.registerVariable(size, IntegerDomain.interval(1, 3).value!!)
            variableDurationSchedulingModel.registerVariable(end, IntegerDomain.interval(1, 9).value!!)
            IntervalVariable.create(
                id = IntervalId("variable-job-$index"),
                start = ConstraintProgrammingExpression.Variable(start),
                size = ConstraintProgrammingExpression.Variable(size),
                end = ConstraintProgrammingExpression.Variable(end)
            ).value!!.also(variableDurationSchedulingModel::registerInterval)
        }
        variableDurationSchedulingModel.addConstraint(
            NoOverlap.create(variableDurationIntervals).value!!,
            id = "variable-no-overlap"
        )
        solver = FakeConstraintProgrammingSolver()

        val bendersMasterVariable = BinVar("benchmark-benders-master")
        bendersMasterModel = fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel(
            name = "benchmark-benders-master",
            converter = IntoValue.Identity
        )
        bendersMasterModel.add(bendersMasterVariable)
        val bendersSubproblemVariable = BinVar("benchmark-benders-subproblem")
        bendersSubproblemModel = ConstraintProgrammingModel("benchmark-benders-subproblem")
        bendersSubproblemModel.registerVariable(
            VariableId("benders:decision"),
            bendersSubproblemVariable,
            IntegerDomain.boolean
        )
        bendersSubproblemModel.addConstraint(
            ConstraintProgrammingConstraint.equal(
                ConstraintProgrammingExpression.Variable(bendersSubproblemVariable),
                Int64.one
            ).value!!,
            id = "benders-decision-required"
        )
        bendersEngine = LogicBasedBendersEngine(
            masterSolver = BendersMasterProblemSolver { model ->
                val value = if (model.relationConstraints.isEmpty()) Flt64.zero else Flt64.one
                ok(
                    SolverStatus.Optimal.toSolveReport(
                        objective = value,
                        values = listOf(value),
                        solveTime = kotlin.time.Duration.ZERO,
                        bestBound = value,
                        gap = Flt64.zero
                    )
                )
            },
            subproblemSolver = solver,
            binding = BinaryBendersVariableBinding(
                listOf(
                    BinaryBendersVariable(
                        key = "benders",
                        masterVariable = bendersMasterVariable,
                        subproblemVariable = bendersSubproblemVariable,
                        subproblemVariableId = "benders:decision"
                    )
                )
            ),
            options = LogicBasedBendersOptions(
                masterSolutionSource = { result ->
                    ok(
                        ConstraintProgrammingValueSource.of(
                            mapOf("benders" to result.values[bendersMasterVariable.index])
                        )
                    )
                },
                maxIterations = 4
            )
        )
        checkpointJson = ConstraintProgrammingCheckpointCodec.capture(
            snapshot = directModel.snapshot().value!!,
            descriptor = solver.descriptor,
            checkpointId = "benchmark-checkpoint",
            createdAtEpochMs = 0L
        ).value
            ?.let(ConstraintProgrammingCheckpointCodec::encode)
            ?.value
            .orEmpty()
    }

    /**
     * Measure CP snapshot encoding. / 测量 CP snapshot 编码。
     *
     * @return Encoded snapshot length. / 编码后的 snapshot 长度。
     */
    @Benchmark
    fun snapshotEncode(): Int {
        return ConstraintProgrammingSnapshotCodec.encode(directModel.snapshot().value!!).value!!.length
    }

    /**
     * Measure direct CP enumeration solve. / 测量直接 CP 枚举求解。
     *
     * @return String representation of the solver result. / 求解结果的字符串表示。
     */
    @Benchmark
    fun directSolve(): String {
        return runBlocking { solver.solve(directModel).value.toString() }
    }

    /**
     * Measure fixed-duration scheduling solve. / 测量固定时长排程求解。
     *
     * @return String representation of the solver result. / 求解结果的字符串表示。
     */
    @Benchmark
    fun schedulingSolve(): String {
        return runBlocking { solver.solve(schedulingModel).value.toString() }
    }

    /** Measure optional fixed-duration scheduling solve. / 测量可选固定时长排程求解。 */
    @Benchmark
    fun optionalSchedulingSolve(): String {
        return runBlocking { solver.solve(optionalSchedulingModel).value.toString() }
    }

    /** Measure variable-duration scheduling solve. / 测量可变时长排程求解。 */
    @Benchmark
    fun variableDurationSchedulingSolve(): String {
        return runBlocking { solver.solve(variableDurationSchedulingModel).value.toString() }
    }

    /**
     * Measure fixed-duration exact MIP decomposition build. / 测量固定时长 exact MIP 分解构建。
     *
     * @return Number of generated relation constraints. / 生成的关系约束数量。
     */
    @Benchmark
    fun fixedDurationMipLowering(): Int {
        return lowerAndCountConstraints(schedulingModel)
    }

    /**
     * Measure optional fixed-duration exact MIP decomposition build. / 测量可选固定时长 exact MIP 分解构建。
     *
     * @return Number of generated relation constraints. / 生成的关系约束数量。
     */
    @Benchmark
    fun optionalDurationMipLowering(): Int {
        return lowerAndCountConstraints(optionalSchedulingModel)
    }

    /**
     * Measure variable-duration exact MIP decomposition build. / 测量可变时长 exact MIP 分解构建。
     *
     * @return Number of generated relation constraints. / 生成的关系约束数量。
     */
    @Benchmark
    fun variableDurationMipLowering(): Int {
        return lowerAndCountConstraints(variableDurationSchedulingModel)
    }

    /** Measure one Fake CP Logic-Based Benders run. / 测量一次 Fake CP Logic-Based Benders 求解。 */
    @Benchmark
    fun bendersSolve(): String {
        return runBlocking { bendersEngine.solve(bendersMasterModel, bendersSubproblemModel).value.toString() }
    }

    /**
     * Measure portable checkpoint capture and digest verification. / 测量可移植 checkpoint 捕获与摘要复验。
     *
     * @return Encoded checkpoint JSON, or an empty string when capture fails. /
     * 编码后的 checkpoint JSON；捕获失败时返回空字符串。
     */
    @Benchmark
    fun checkpointCapture(): String {
        val snapshot = directModel.snapshot().value!!
        val captured = ConstraintProgrammingCheckpointCodec.capture(
            snapshot = snapshot,
            descriptor = solver.descriptor,
            checkpointId = "benchmark-checkpoint",
            createdAtEpochMs = 0L
        )
        val envelope = captured.value ?: return ""
        val encoded = ConstraintProgrammingCheckpointCodec.encode(envelope)
        return encoded.value ?: ""
    }

    /** Measure portable checkpoint decode and restore. / 测量 portable checkpoint 解码与恢复。 */
    @Benchmark
    fun checkpointRestore(): String {
        val decoded = ConstraintProgrammingCheckpointCodec.decodeCompatible(checkpointJson).value
            ?: return ""
        return ConstraintProgrammingCheckpointCodec.restore(
            envelope = decoded,
            snapshot = directModel.snapshot().value!!,
            expectedConfigurationFingerprint = decoded.configurationFingerprint,
            expectedSolverFingerprint = decoded.solverFingerprint
        ).value?.envelope?.checkpointId.orEmpty()
    }

    private fun lowerAndCountConstraints(model: ConstraintProgrammingModel): Int {
        val lowered = ConstraintProgrammingToLinearModelLowerer().lower(model).value ?: return 0
        return try {
            lowered.model.relationConstraints.size
        } finally {
            lowered.close()
        }
    }
}
