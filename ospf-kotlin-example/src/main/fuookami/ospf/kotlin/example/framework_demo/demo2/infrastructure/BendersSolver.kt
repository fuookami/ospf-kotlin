package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.framework.solver.*

data class BendersIterationSnapshot(
    val masterObj: Double,
    val gap: Double
)

data class BendersRuntimeMetrics(
    val executedIterations: Int,
    val totalCuts: Int,
    val iterationSnapshots: List<BendersIterationSnapshot>
)

data class BendersResult(
    val obj: Double,
    val solution: DoubleArray,
    val gap: Double,
    val timeMs: Long,
    val bendersIterations: Int,
    val runtimeMetrics: BendersRuntimeMetrics?
)

object BendersSolver {
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
