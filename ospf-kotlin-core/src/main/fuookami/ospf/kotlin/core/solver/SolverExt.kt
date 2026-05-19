package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.iis.computeIIS
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.LinearInfeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.QuadraticInfeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.resolveInfeasibleUnifiedFields
import fuookami.ospf.kotlin.core.solver.output.SolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatus
import fuookami.ospf.kotlin.core.solver.value.validateLinearModelValueConversion
import fuookami.ospf.kotlin.core.solver.value.validateQuadraticModelValueConversion
import fuookami.ospf.kotlin.core.solver.value.withSolveValueConversionPolicy
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.MechanismModel
import fuookami.ospf.kotlin.core.model.intermediate.MechanismModelDumpingStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.mechanism.convertMechanismModelToFlt64
import fuookami.ospf.kotlin.core.model.basic.toModelBuildingStatus
import fuookami.ospf.kotlin.core.model.intermediate.toModelBuildingStatus
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import kotlin.time.TimeSource
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel

suspend fun AbstractLinearSolver.solve(model: LinearTriadModelView): Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    return solveWithOptions(model, SolveOptions())
}

suspend fun AbstractLinearSolver.solveWithOptions(
    model: LinearTriadModelView,
    options: SolveOptions
): Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    when (val validation = validateLinearModelValueConversion(model, options.effectiveValueConversionPolicy)) {
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
        else -> {}
    }

    return withSolveValueConversionPolicy(options.effectiveValueConversionPolicy) {
        options.solutionAmount?.let { solutionAmount ->
            unwrapSolution(this@solveWithOptions(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = options.solvingStatusCallBack
            ))
        } ?: this@solveWithOptions(
            model = model,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }
}

suspend fun AbstractQuadraticSolver.solve(model: QuadraticTetradModelView): Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    return solveWithOptions(model, SolveOptions())
}

suspend fun AbstractQuadraticSolver.solveWithOptions(
    model: QuadraticTetradModelView,
    options: SolveOptions
): Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    when (val validation = validateQuadraticModelValueConversion(model, options.effectiveValueConversionPolicy)) {
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
        else -> {}
    }

    return withSolveValueConversionPolicy(options.effectiveValueConversionPolicy) {
        options.solutionAmount?.let { solutionAmount ->
            unwrapSolution(this@solveWithOptions(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = options.solvingStatusCallBack
            ))
        } ?: this@solveWithOptions(
            model = model,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }
}

@Suppress("DEPRECATION")
suspend fun AbstractLinearSolver.solveWithOptionsAndIIS(
    model: LinearTriadModelView,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<SolverOutput> {
    val solveStartedAt = TimeSource.Monotonic.markNow()
    var latestSolvingStatus: SolvingStatus? = null
    val bridgingSolvingStatusCallBack = options.solvingStatusCallBack?.let { callback ->
        { status: SolvingStatus ->
            latestSolvingStatus = status
            callback(status)
        }
    }

    return when (val result = solveWithOptions(
        model = model,
        options = options.copy(solvingStatusCallBack = bridgingSolvingStatusCallBack)
    )) {
        is Ok -> {
            Ok(result.value)
        }

        is Failed -> {
            if (result.error.code == ErrorCode.ORModelInfeasible) {
                when (val iisResult = computeIIS(model, this, iisConfig)) {
                    is Ok -> {
                        val unifiedFields = resolveInfeasibleUnifiedFields(
                            latestStatus = latestSolvingStatus,
                            fallbackSolveTime = solveStartedAt.elapsedNow()
                        )
                        Ok(
                            LinearInfeasibleSolverOutput(
                                iis = iisResult.value,
                                iterations = unifiedFields.iterations,
                                nodeCount = unifiedFields.nodeCount,
                                bestBound = unifiedFields.bestBound,
                                mipGap = unifiedFields.mipGap,
                                solveTime = unifiedFields.solveTime
                            )
                        )
                    }

                    is Failed -> {
                        Failed(iisResult.error)
                    }

                    is Fatal -> {
                        Fatal(iisResult.errors)
                    }
                }
            } else {
                Failed(result.error)
            }
        }

        is Fatal -> {
            Fatal(result.errors)
        }
    }
}

@Suppress("DEPRECATION")
suspend fun AbstractLinearSolver.solveWithOptionsAndIISForSolutionPool(
    model: LinearTriadModelView,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<Pair<SolverOutput, List<List<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>> {
    when (val validation = validateLinearModelValueConversion(model, options.effectiveValueConversionPolicy)) {
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
        else -> {}
    }

    return withSolveValueConversionPolicy(options.effectiveValueConversionPolicy) {
        val solveStartedAt = TimeSource.Monotonic.markNow()
        var latestSolvingStatus: SolvingStatus? = null
        val bridgingSolvingStatusCallBack = options.solvingStatusCallBack?.let { callback ->
            { status: SolvingStatus ->
                latestSolvingStatus = status
                callback(status)
            }
        }

        val solutionAmount = options.solutionAmount
        if (solutionAmount == null) {
            return@withSolveValueConversionPolicy when (val result = solveWithOptionsAndIIS(
                model = model,
                options = options.copy(solvingStatusCallBack = bridgingSolvingStatusCallBack),
                iisConfig = iisConfig
            )) {
                is Ok -> {
                    Ok(result.value to emptyList())
                }

                is Failed -> {
                    Failed(result.error)
                }

                is Fatal -> {
                    Fatal(result.errors)
                }
            }
        }

        when (val result = this@solveWithOptionsAndIISForSolutionPool(
            model = model,
            solutionAmount = solutionAmount,
            solvingStatusCallBack = bridgingSolvingStatusCallBack
        )) {
            is Ok -> {
                Ok(result.value.first to result.value.second)
            }

            is Failed -> {
                if (result.error.code == ErrorCode.ORModelInfeasible) {
                    when (val iisResult = computeIIS(model, this@solveWithOptionsAndIISForSolutionPool, iisConfig)) {
                        is Ok -> {
                            val unifiedFields = resolveInfeasibleUnifiedFields(
                                latestStatus = latestSolvingStatus,
                                fallbackSolveTime = solveStartedAt.elapsedNow()
                            )
                            Ok(
                                LinearInfeasibleSolverOutput(
                                    iis = iisResult.value,
                                    iterations = unifiedFields.iterations,
                                    nodeCount = unifiedFields.nodeCount,
                                    bestBound = unifiedFields.bestBound,
                                    mipGap = unifiedFields.mipGap,
                                    solveTime = unifiedFields.solveTime
                                ) to emptyList()
                            )
                        }

                        is Failed -> {
                            Failed(iisResult.error)
                        }

                        is Fatal -> {
                            Fatal(iisResult.errors)
                        }
                    }
                } else {
                    Failed(result.error)
                }
            }

            is Fatal -> {
                Fatal(result.errors)
            }
        }
    }
}

@Suppress("DEPRECATION")
suspend fun AbstractQuadraticSolver.solveWithOptionsAndIIS(
    model: QuadraticTetradModelView,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<SolverOutput> {
    val solveStartedAt = TimeSource.Monotonic.markNow()
    var latestSolvingStatus: SolvingStatus? = null
    val bridgingSolvingStatusCallBack = options.solvingStatusCallBack?.let { callback ->
        { status: SolvingStatus ->
            latestSolvingStatus = status
            callback(status)
        }
    }

    return when (val result = solveWithOptions(
        model = model,
        options = options.copy(solvingStatusCallBack = bridgingSolvingStatusCallBack)
    )) {
        is Ok -> {
            Ok(result.value)
        }

        is Failed -> {
            if (result.error.code == ErrorCode.ORModelInfeasible) {
                when (val iisResult = computeIIS(model, this, iisConfig)) {
                    is Ok -> {
                        val unifiedFields = resolveInfeasibleUnifiedFields(
                            latestStatus = latestSolvingStatus,
                            fallbackSolveTime = solveStartedAt.elapsedNow()
                        )
                        Ok(
                            QuadraticInfeasibleSolverOutput(
                                iis = iisResult.value,
                                iterations = unifiedFields.iterations,
                                nodeCount = unifiedFields.nodeCount,
                                bestBound = unifiedFields.bestBound,
                                mipGap = unifiedFields.mipGap,
                                solveTime = unifiedFields.solveTime
                            )
                        )
                    }

                    is Failed -> {
                        Failed(iisResult.error)
                    }

                    is Fatal -> {
                        Fatal(iisResult.errors)
                    }
                }
            } else {
                Failed(result.error)
            }
        }

        is Fatal -> {
            Fatal(result.errors)
        }
    }
}

@Suppress("DEPRECATION")
suspend fun AbstractQuadraticSolver.solveWithOptionsAndIISForSolutionPool(
    model: QuadraticTetradModelView,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<Pair<SolverOutput, List<List<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>> {
    when (val validation = validateQuadraticModelValueConversion(model, options.effectiveValueConversionPolicy)) {
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
        else -> {}
    }

    return withSolveValueConversionPolicy(options.effectiveValueConversionPolicy) {
        val solveStartedAt = TimeSource.Monotonic.markNow()
        var latestSolvingStatus: SolvingStatus? = null
        val bridgingSolvingStatusCallBack = options.solvingStatusCallBack?.let { callback ->
            { status: SolvingStatus ->
                latestSolvingStatus = status
                callback(status)
            }
        }

        val solutionAmount = options.solutionAmount
        if (solutionAmount == null) {
            return@withSolveValueConversionPolicy when (val result = solveWithOptionsAndIIS(
                model = model,
                options = options.copy(solvingStatusCallBack = bridgingSolvingStatusCallBack),
                iisConfig = iisConfig
            )) {
                is Ok -> {
                    Ok(result.value to emptyList())
                }

                is Failed -> {
                    Failed(result.error)
                }

                is Fatal -> {
                    Fatal(result.errors)
                }
            }
        }

        when (val result = this@solveWithOptionsAndIISForSolutionPool(
            model = model,
            solutionAmount = solutionAmount,
            solvingStatusCallBack = bridgingSolvingStatusCallBack
        )) {
            is Ok -> {
                Ok(result.value.first to result.value.second)
            }

            is Failed -> {
                if (result.error.code == ErrorCode.ORModelInfeasible) {
                    when (val iisResult = computeIIS(model, this@solveWithOptionsAndIISForSolutionPool, iisConfig)) {
                        is Ok -> {
                            val unifiedFields = resolveInfeasibleUnifiedFields(
                                latestStatus = latestSolvingStatus,
                                fallbackSolveTime = solveStartedAt.elapsedNow()
                            )
                            Ok(
                                QuadraticInfeasibleSolverOutput(
                                    iis = iisResult.value,
                                    iterations = unifiedFields.iterations,
                                    nodeCount = unifiedFields.nodeCount,
                                    bestBound = unifiedFields.bestBound,
                                    mipGap = unifiedFields.mipGap,
                                    solveTime = unifiedFields.solveTime
                                ) to emptyList()
                            )
                        }

                        is Failed -> {
                            Failed(iisResult.error)
                        }

                        is Fatal -> {
                            Fatal(iisResult.errors)
                        }
                    }
                } else {
                    Failed(result.error)
                }
            }

            is Fatal -> {
                Fatal(result.errors)
            }
        }
    }
}

private fun unwrapSolution(result: Ret<Pair<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>, List<List<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>>): Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    return when (result) {
        is Ok -> {
            Ok(result.value.first)
        }

        is Failed -> {
            Failed(result.error)
        }

        is Fatal -> {
            Fatal(result.errors)
        }
    }
}

fun AbstractLinearSolver.solveAsync(
    model: LinearTriadModelView,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>>> {
    return coreSolverAsyncScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

fun AbstractQuadraticSolver.solveAsync(
    model: QuadraticTetradModelView,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>>> {
    return coreSolverAsyncScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

// ========== V-generic primary solve extensions ==========
// These are the recommended V-generic entry points. They delegate to solveV which
// contains the full pipeline (validate -> dump -> solve -> convert).

suspend fun <V> AbstractLinearSolver.solve(
    model: LinearTriadModelView,
    converter: IntoValue<V>
): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
    return solveV(model, converter)
}

suspend fun <V> AbstractLinearSolver.solve(
    model: MechanismModel<V>,
    converter: IntoValue<V>
): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
    return solveV(model, converter)
}

suspend fun <V> AbstractQuadraticSolver.solve(
    model: QuadraticTetradModelView,
    converter: IntoValue<V>
): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
    return solveV(model, converter)
}

suspend fun <V> AbstractQuadraticSolver.solve(
    model: MechanismModel<V>,
    converter: IntoValue<V>
): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
    return solveV(model, converter)
}
