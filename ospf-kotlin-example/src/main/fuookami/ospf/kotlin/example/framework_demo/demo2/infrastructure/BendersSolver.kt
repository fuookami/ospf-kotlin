package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.framework.solver.*

/**
 * Snapshot of a single Benders decomposition iteration capturing the master objective and gap.
 * Benders 分解单次迭代的快照，记录主问题目标值和间隙。
 *
 * @property masterObj The master problem objective value at this iteration. / 本次迭代的主问题目标值
 * @property gap The gap between master and sub problem objectives at this iteration. / 本次迭代主子问题目标间隙
*/
data class BendersIterationSnapshot(
    val masterObj: Double,
    val gap: Double
)

/**
 * Runtime metrics collected during Benders decomposition execution.
 * Benders 分解执行期间收集的运行时指标。
 *
 * @property executedIterations The number of iterations actually executed. / 实际执行的迭代次数
 * @property totalCuts The total number of Benders cuts generated. / 生成的 Benders 割平面总数
 * @property iterationSnapshots The list of per-iteration objective and gap snapshots. / 每次迭代的目标值和间隙快照列表
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
 * @property obj The objective function value of the best solution. / 最优解的目标函数值
 * @property solution The solution variable values as a double array. / 以双精度数组表示的解变量值
 * @property gap The final convergence gap. / 最终收敛间隙
 * @property timeMs The total solve time in milliseconds. / 总求解时间（毫秒）
 * @property bendersIterations The number of Benders iterations performed. / 执行的 Benders 迭代次数
 * @property runtimeMetrics Optional detailed runtime metrics. / 可选的详细运行时指标
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

    /**
     * Validates that the master solver output is feasible and normalizes solution values to Flt64.
     * 验证主问题求解输出是否可行，并将解值归一化为 Flt64。
     *
     * @param output The raw solver output from the master problem. / 主问题的原始求解器输出
     * @return The feasible solver output with Flt64 solution values, or an error. / 包含 Flt64 解值的可行求解器输出，或错误
    */
    private fun requireFeasibleMasterOutput(output: SolverOutput): Ret<FeasibleSolverOutput<Flt64>> {
        return when (output) {
            is FeasibleSolverOutput<*> -> {
                val normalizedSolution = output.solution.mapNotNull { it as? Flt64 }
                if (normalizedSolution.size != output.solution.size) {
                    Failed(fuookami.ospf.kotlin.utils.error.Err(
                        fuookami.ospf.kotlin.utils.error.ErrorCode.ORModelInfeasible,
                        "Master feasible output contains non-Flt64 solution values."
                    ))
                } else {
                    Ok(FeasibleSolverOutput(
                        obj = output.obj,
                        solution = normalizedSolution,
                        time = output.time,
                        possibleBestObj = output.possibleBestObj,
                        gap = output.gap,
                        iterations = output.iterations,
                        nodeCount = output.nodeCount,
                        bestBound = output.bestBound,
                        mipGap = output.mipGap,
                        solveTime = output.solveTime
                    ))
                }
            }

            else -> {
                Failed(fuookami.ospf.kotlin.utils.error.Err(fuookami.ospf.kotlin.utils.error.ErrorCode.ORModelInfeasible))
            }
        }
    }

    /**
     * Executes the Benders decomposition iterative solve loop, alternating between master and sub problem solves with cut generation.
     * 执行 Benders 分解迭代求解循环，交替求解主问题和子问题并生成割平面。
     *
     * @param solver The linear Benders decomposition solver instance. / 线性 Benders 分解求解器实例
     * @param masterModel The master problem linear meta-model. / 主问题线性元模型
     * @param subModel The sub problem linear meta-model. / 子问题线性元模型
     * @param fixedVariables Map of variables fixed from master to sub problem. / 从主问题固定到子问题的变量映射
     * @param objectVariable The objective variable used for cut generation. / 用于割生成的目标变量
     * @param config The effective Benders adaptive configuration. / 有效的 Benders 自适应配置
     * @param notes Mutable list for collecting diagnostic notes during the solve. / 求解过程中收集诊断信息的可变列表
     * @return The Benders solve result containing objective, solution, gap, and metrics. / 包含目标值、解、间隙和指标的 Benders 求解结果
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
        val startTime = System.currentTimeMillis()
        val snapshots = mutableListOf<BendersIterationSnapshot>()
        var totalCuts = 0
        val maxIterations = config.maxIterations
        val tolerance = config.tolerance
        val stallWindow = config.maxStallIterations ?: maxIterations
        val objStallWindow = config.objectiveStallIterations ?: maxIterations

        var bestObj: Double? = null
        var stallCount = 0
        var objStallCount = 0

        for (iteration in 1..maxIterations) {
            val masterOutput = when (val result = solver.solveMaster(
                metaModel = masterModel as LinearMetaModel<Flt64>,
                options = FrameworkSolveOptions.build {
                    bendersIterationLimit = fuookami.ospf.kotlin.math.algebra.number.UInt64(1)
                }
            )) {
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> result.value
                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Failed(result.error)
                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Fatal(result.errors)
            }
            if (masterOutput == null) {
                return Failed(fuookami.ospf.kotlin.utils.error.Err(
                    fuookami.ospf.kotlin.utils.error.ErrorCode.ApplicationError,
                    "Master solver returned null output."
                ))
            }

            val masterFeasible = when (val normalized = requireFeasibleMasterOutput(masterOutput)) {
                is Ok -> normalized.value
                is Failed -> return Failed(normalized.error)
                is Fatal -> return Fatal(normalized.errors)
            }

            val masterObj = masterFeasible.obj.toDouble()
            snapshots.add(BendersIterationSnapshot(masterObj = masterObj, gap = masterFeasible.gap.toDouble()))

            if (bestObj == null || masterObj > bestObj) {
                if (bestObj != null) {
                    stallCount = 0
                    objStallCount = 0
                }
                bestObj = masterObj
            } else {
                stallCount++
                objStallCount++
            }

            val subResult = when (val result = solver.solveSub(
                metaModel = subModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                options = FrameworkSolveOptions()
            )) {
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> result.value
                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Failed(result.error)
                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Fatal(result.errors)
            }

            when (subResult) {
                is LinearBendersDecompositionSolver.LinearFeasibleResult -> {
                    val cuts = subResult.cuts
                    if (cuts != null) {
                        totalCuts += cuts.size
                        for (cut in cuts) {
                            when (val addResult = masterModel.addConstraint(cut, group = null, name = "benders_opt_cut_${iteration}_$totalCuts")) {
                                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Failed(addResult.error)
                                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Fatal(addResult.errors)
                                else -> {}
                            }
                        }
                    }

                    val subObj = subResult.obj.toDouble()
                    val currentGap = if (kotlin.math.abs(masterObj) > 1e-12) {
                        kotlin.math.abs(masterObj - subObj) / kotlin.math.abs(masterObj)
                    } else {
                        kotlin.math.abs(masterObj - subObj)
                    }

                    if (currentGap <= tolerance) {
                        val timeMs = System.currentTimeMillis() - startTime
                        return Ok(BendersResult(
                            obj = masterObj,
                            solution = masterFeasible.solution.map { it.toDouble() }.toDoubleArray(),
                            gap = currentGap,
                            timeMs = timeMs,
                            bendersIterations = iteration,
                            runtimeMetrics = BendersRuntimeMetrics(
                                executedIterations = iteration,
                                totalCuts = totalCuts,
                                iterationSnapshots = snapshots
                            )
                        ))
                    }
                }
                is LinearBendersDecompositionSolver.LinearInfeasibleResult -> {
                    val cuts = subResult.cuts
                    if (cuts != null) {
                        totalCuts += cuts.size
                        for (cut in cuts) {
                            when (val addResult = masterModel.addConstraint(cut, group = null, name = "benders_feas_cut_${iteration}_$totalCuts")) {
                                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Failed(addResult.error)
                                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Fatal(addResult.errors)
                                else -> {}
                            }
                        }
                    }
                }
                null -> {
                    return Failed(fuookami.ospf.kotlin.utils.error.Err(fuookami.ospf.kotlin.utils.error.ErrorCode.ApplicationError, "Unexpected null benders sub result"))
                }
            }

            if (stallCount >= stallWindow || objStallCount >= objStallWindow) {
                notes.add("Benders stalled after $iteration iterations (stall=$stallCount, objStall=$objStallCount)")
                break
            }
        }

        val timeMs = System.currentTimeMillis() - startTime
        val finalObj = bestObj ?: 0.0
        val finalGap = snapshots.lastOrNull()?.gap ?: 1.0

        return Ok(BendersResult(
            obj = finalObj,
            solution = doubleArrayOf(),
            gap = finalGap,
            timeMs = timeMs,
            bendersIterations = snapshots.size,
            runtimeMetrics = BendersRuntimeMetrics(
                executedIterations = snapshots.size,
                totalCuts = totalCuts,
                iterationSnapshots = snapshots
            )
        ))
    }
}
