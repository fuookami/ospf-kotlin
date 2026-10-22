package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.toSolverStatus
import fuookami.ospf.kotlin.framework.solver.*

/**
 * Snapshot of a single Benders decomposition iteration, including the master objective,
 * projected objective value and sub-problem objective.
 * Benders 分解单次迭代的快照，记录主问题完整目标、投影目标变量值和子问题目标值。
 *
 * @property masterObj 本次迭代的主问题完整目标值 / The complete master objective value at this iteration.
 * @property subObj 本次迭代的子问题目标值；子问题不可行时为空 / The sub-problem objective value; null when infeasible.
 * @property objectValue 主问题目标变量（theta/z）的值 / The master objective-variable (theta/z) value.
 * @property gap 主问题目标变量与子问题目标的相对间隙 / The relative gap between the master objective variable and sub-problem objective.
*/
data class BendersIterationSnapshot(
    val masterObj: Double,
    val subObj: Double?,
    val objectValue: Double?,
    val gap: Double
)

/**
 * Runtime metrics collected during Benders decomposition execution.
 * Benders 分解执行期间收集的运行时指标。
 *
 * @property executedIterations 实际执行的迭代次数 / The number of iterations actually executed.
 * @property totalCuts 生成的 Benders 割平面总数 / The total number of Benders cuts generated.
 * @property iterationSnapshots 每次迭代的目标值和间隙快照列表 / The list of per-iteration objective and gap snapshots.
*/
data class BendersRuntimeMetrics(
    val executedIterations: Int,
    val totalCuts: Int,
    val iterationSnapshots: List<BendersIterationSnapshot>
)

/**
 * Result of a Benders decomposition solve containing the solution and convergence information.
 * Benders 分解求解结果，包含解和收敛信息。
 *
 * @property obj 最优解的目标函数值 / The objective function value of the best solution.
 * @property solution 以双精度数组表示的解变量值 / The solution variable values as a double array.
 * @property gap 最终收敛间隙 / The final convergence gap.
 * @property timeMs 总求解时间（毫秒） / The total solve time in milliseconds.
 * @property bendersIterations 执行的 Benders 迭代次数 / The number of Benders iterations performed.
 * @property runtimeMetrics 可选的详细运行时指标 / Optional detailed runtime metrics.
*/
data class BendersResult(
    val obj: Double,
    val solution: DoubleArray,
    val gap: Double,
    val timeMs: Long,
    val bendersIterations: Int,
    val runtimeMetrics: BendersRuntimeMetrics?
)

/**
 * Solver for Benders decomposition, iteratively solving master and sub problems until convergence.
 * Benders 分解求解器，迭代求解主问题和子问题直到收敛。
*/
object BendersSolver {
    /** 转接共享执行器，旧值仅用于决策身份，不作为固定值缓存。 / Delegate to the shared executor; old values identify decisions only.
     * @param solver 后端 / Backend
     * @param masterModel 主模型 / Master model
     * @param subModel 连续追索 / Continuous recourse
     * @param fixedVariables 绑定身份 / Binding identities
     * @param objectVariable 追索投影 / Recourse projection
     * @param config 预算 / Budget
     * @param notes 诊断 / Diagnostics
     * @return 共享执行结果 / Shared execution result
     */
    suspend fun solve(
        solver: LinearBendersDecompositionSolver,
        masterModel: AbstractLinearMetaModel<Flt64>,
        subModel: LinearMetaModel<Flt64>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        config: EffectiveBendersAdaptiveConfig,
        notes: MutableList<String>
    ): Ret<BendersResult> {
        val master = masterModel as? LinearMetaModel<Flt64>
            ?: return Failed(ErrorCode.IllegalArgument, "需要线性主模型 / Linear master model required")
        val started = System.nanoTime()
        val result = when (val result = LinearBendersAlgorithm(solver).solve(
            models = LinearBendersModels(
                master = master,
                sub = subModel,
                bindings = fixedVariables.keys.associateBy { it.key.toString() },
                theta = objectVariable
            ),
            config = LinearBendersConfig(
                iterationLimit = config.maxIterations,
                tolerance = config.tolerance,
                stallLimit = config.maxStallIterations ?: config.maxIterations
            )
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        notes.add("benders_status=${result.status}")
        val values = master.tokens.tokensInSolver.map { token ->
            result.values[token.variable]?.toDouble() ?: return Failed(ErrorCode.DataNotFound, "缺少合并变量 / Missing merged variable")
        }.toDoubleArray()
        return Ok(BendersResult(
            obj = result.objective.toDouble(),
            solution = values,
            gap = result.gap,
            timeMs = (System.nanoTime() - started) / 1000000,
            bendersIterations = result.iterations.size,
            runtimeMetrics = BendersRuntimeMetrics(
                executedIterations = result.iterations.size,
                totalCuts = result.iterations.fold(0) { total, iteration -> total + iteration.cuts },
                iterationSnapshots = result.iterations.map { BendersIterationSnapshot(it.lowerBound, it.upperBound, null,
                    it.upperBound?.let { upper -> kotlin.math.max(0.0, upper - it.lowerBound) / kotlin.math.max(1.0, kotlin.math.abs(upper)) } ?: Double.POSITIVE_INFINITY) }
            )
        ))
    }
}
