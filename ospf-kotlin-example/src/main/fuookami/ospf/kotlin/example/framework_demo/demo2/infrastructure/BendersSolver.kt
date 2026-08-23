package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.toSolverStatus
import fuookami.ospf.kotlin.core.solver.report.*
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

    /**
     * Validates that the master solver output is feasible and normalizes solution values to Flt64.
     * 验证主问题求解输出是否可行，并将解值归一化为 Flt64。
     *
     * @param output 主问题的原始求解器输出 / The raw solver output from the master problem.
     * @return 包含 Flt64 解值的可行求解器输出，或错误 / The feasible solver output with Flt64 solution values, or an error.
    */
    private fun requireFeasibleMasterOutput(output: SolverOutput): Ret<SolveReport<Flt64>> {
        return when (output) {
            is SolveReport<*> -> {
                val solution = output.solution ?: return Failed(Err(
                    ErrorCode.ORModelInfeasible,
                    "Master feasible output does not contain an incumbent solution."
                ))
                val normalizedSolution = solution.values.mapNotNull { it as? Flt64 }
                if (normalizedSolution.size != solution.values.size) {
                    Failed(Err(
                        ErrorCode.ORModelInfeasible,
                        "Master feasible output contains non-Flt64 solution values."
                    ))
                } else {
                    @Suppress("UNCHECKED_CAST")
                    Ok(output as SolveReport<Flt64>)
                }
            }

            else -> {
                Failed(Err(ErrorCode.ORModelInfeasible))
            }
        }
    }

    /**
     * Executes the Benders decomposition iterative solve loop, alternating between master and sub problem solves with cut generation.
     * 执行 Benders 分解迭代求解循环，交替求解主问题和子问题并生成割平面。
     *
     * @param solver 线性 Benders 分解求解器实例 / The linear Benders decomposition solver instance.
     * @param masterModel 主问题线性元模型 / The master problem linear meta-model.
     * @param subModel 子问题线性元模型 / The sub problem linear meta-model.
     * @param fixedVariables 从主问题固定到子问题的变量映射 / Map of variables fixed from master to sub problem.
     * @param objectVariable 用于割生成的目标变量 / The objective variable used for cut generation.
     * @param config 有效的 Benders 自适应配置 / The effective Benders adaptive configuration.
     * @param notes 求解过程中收集诊断信息的可变列表 / Mutable list for collecting diagnostic notes during the solve.
     * @return 包含目标值、解、间隙和指标的 Benders 求解结果 / The Benders solve result containing objective, solution, gap, and metrics.
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

        var previousMasterObj: Double? = null
        var lastGap: Double? = null
        var cutStallCount = 0
        var objStallCount = 0

        for (iteration in 1..maxIterations) {
            val masterOutput = when (val result = solver.solveMaster(
                metaModel = masterModel as LinearMetaModel<Flt64>,
                options = FrameworkSolveOptions.build {
                    bendersIterationLimit = UInt64(1)
                }
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (masterOutput == null) {
                return Failed(Err(
                    ErrorCode.ApplicationError,
                    "Master solver returned null output."
                ))
            }

            val masterFeasible = when (val normalized = requireFeasibleMasterOutput(masterOutput)) {
                is Ok -> normalized.value
                is Failed -> return Failed(normalized.error)
                is Fatal -> return Fatal(normalized.errors)
            }

            val masterStatus = masterFeasible.toSolverStatus()
            if (masterStatus != SolverStatus.Optimal) {
                return Failed(Err(
                    masterStatus.errCode
                        ?: ErrorCode.OREngineSolvingException,
                    "Benders 主问题终态为 $masterStatus，需要最优主问题证书 / " +
                        "Benders master ended with $masterStatus; an optimal master certificate is required."
                ))
            }

            val masterObj = masterFeasible.solution?.objective?.toFlt64()?.toDouble()
                ?: return Failed(Err(
                    ErrorCode.ORModelInfeasible,
                    "Master feasible output does not contain an objective value."
                ))
            val objectTokenIndex = masterModel.tokens.indexOf(objectVariable)
                ?: return Failed(Err(
                    ErrorCode.ApplicationError,
                    "Benders 主问题缺少目标变量 ${objectVariable.name} 的 token / " +
                        "Benders master is missing the token for objective variable ${objectVariable.name}."
                ))
            val masterObjectValue = masterFeasible.values.getOrNull(objectTokenIndex)?.toDouble()
                ?: return Failed(Err(
                    ErrorCode.ApplicationError,
                    "Benders 主问题解缺少目标变量 ${objectVariable.name} 的值 / " +
                        "Benders master solution is missing a value for objective variable ${objectVariable.name}."
                ))

            val previousObj = previousMasterObj
            objStallCount = if (previousObj != null &&
                kotlin.math.abs(masterObj - previousObj) <= tolerance
            ) {
                objStallCount + 1
            } else {
                0
            }
            previousMasterObj = masterObj
            val cutsBeforeIteration = totalCuts

            val masterFixedVariables = mutableMapOf<AbstractVariableItem<*, *>, Flt64>()
            for (variable in fixedVariables.keys) {
                val tokenIndex = masterModel.tokens.indexOf(variable)
                    ?: return Failed(Err(
                        ErrorCode.ApplicationError,
                        "Benders 主问题缺少固定变量 ${variable.name} 的 token / " +
                            "Benders master is missing a token for fixed variable ${variable.name}."
                    ))
                val value = masterFeasible.values.getOrNull(tokenIndex)
                    ?: return Failed(Err(
                        ErrorCode.ApplicationError,
                        "Benders 主问题解缺少固定变量 ${variable.name} 的值 / " +
                            "Benders master solution is missing a value for fixed variable ${variable.name}."
                    ))
                masterFixedVariables[variable] = value
            }

            val subResult = when (val result = solver.solveSub(
                metaModel = subModel,
                objectVariable = objectVariable,
                fixedVariables = masterFixedVariables,
                options = FrameworkSolveOptions()
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            when (subResult) {
                is LinearBendersDecompositionSolver.LinearFeasibleResult -> {
                    val cuts = subResult.cuts
                    if (cuts != null) {
                        totalCuts += cuts.size
                        for (cut in cuts) {
                            when (val addResult = masterModel.addConstraint(cut, group = null, name = "benders_opt_cut_${iteration}_$totalCuts")) {
                                is Failed -> return Failed(addResult.error)
                                is Fatal -> return Fatal(addResult.errors)
                                else -> {}
                            }
                        }
                    }

                    val subObj = subResult.obj.toDouble()
                    val gapBase = masterObjectValue
                    val currentGap = if (kotlin.math.abs(gapBase) > 1e-12) {
                        kotlin.math.abs(gapBase - subObj) / kotlin.math.abs(gapBase)
                    } else {
                        kotlin.math.abs(gapBase - subObj)
                    }
                    lastGap = currentGap
                    snapshots.add(
                        BendersIterationSnapshot(
                            masterObj = masterObj,
                            subObj = subObj,
                            objectValue = masterObjectValue,
                            gap = currentGap
                        )
                    )

                    if (currentGap <= tolerance) {
                        val timeMs = System.currentTimeMillis() - startTime
                        return Ok(BendersResult(
                            obj = masterObj,
                            solution = masterFeasible.values.map { it.toDouble() }.toDoubleArray(),
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
                    lastGap = Double.POSITIVE_INFINITY
                    snapshots.add(
                        BendersIterationSnapshot(
                            masterObj = masterObj,
                            subObj = null,
                            objectValue = masterObjectValue,
                            gap = Double.POSITIVE_INFINITY
                        )
                    )
                    val cuts = subResult.cuts
                    if (cuts != null) {
                        totalCuts += cuts.size
                        for (cut in cuts) {
                            when (val addResult = masterModel.addConstraint(cut, group = null, name = "benders_feas_cut_${iteration}_$totalCuts")) {
                                is Failed -> return Failed(addResult.error)
                                is Fatal -> return Fatal(addResult.errors)
                                else -> {}
                            }
                        }
                    }
                }
                null -> {
                    return Failed(Err(ErrorCode.ApplicationError, "Unexpected null benders sub result"))
                }
            }

            val cutsAddedThisIteration = totalCuts != cutsBeforeIteration
            if (cutsAddedThisIteration) {
                objStallCount = 0
            }
            cutStallCount = if (!cutsAddedThisIteration) {
                cutStallCount + 1
            } else {
                0
            }

            if (cutStallCount >= stallWindow || objStallCount >= objStallWindow) {
                notes.add(
                    "Benders stalled after $iteration iterations (cutStall=$cutStallCount, objStall=$objStallCount, " +
                        "finalGap=${lastGap ?: Double.POSITIVE_INFINITY})"
                )
                break
            }
        }

        val finalGap = lastGap ?: Double.POSITIVE_INFINITY
        return Failed(Err(
            ErrorCode.OREngineSolvingException,
            "Benders 未在 ${snapshots.size} 次迭代内收敛，最终主子问题 gap=$finalGap / " +
                "Benders did not converge within ${snapshots.size} iterations; final master-subproblem gap=$finalGap."
        ))
    }
}
