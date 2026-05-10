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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import kotlin.time.TimeSource
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel

@Deprecated("Use solve(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractLinearSolver.solve(model: LinearTriadModelView): Ret<FeasibleSolverOutput<Flt64>> {
    return solveWithOptions(model, SolveOptions())
}

@Deprecated("Use solve(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
suspend fun AbstractLinearSolver.solveWithOptions(
    model: LinearTriadModelView,
    options: SolveOptions
): Ret<FeasibleSolverOutput<Flt64>> {
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

@Deprecated("Use solve(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractLinearSolver.solve(model: LinearMetaModel<Flt64>): Ret<FeasibleSolverOutput<Flt64>> {
    return solveWithOptions(model, SolveOptions())
}

@Deprecated("Use solve(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractLinearSolver.solveWithOptions(
    model: LinearMetaModel<Flt64>,
    options: SolveOptions
): Ret<FeasibleSolverOutput<Flt64>> {
    val registrationStatusCallBack: RegistrationStatusCallBack? = options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name)) }
    }
    val dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name, quadratic = false)) }
    }

    val mechanismModel = when (val result = dump(
        model = model,
        registrationStatusCallBack = registrationStatusCallBack,
        dumpingStatusCallBack = dumpingStatusCallBack
    )) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return mechanismModel.use {
        solveWithOptions(it, options)
    }
}

@Deprecated("Use solve(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractLinearSolver.solve(model: LinearMechanismModel<Flt64>): Ret<FeasibleSolverOutput<Flt64>> {
    return solveWithOptions(model, SolveOptions())
}

/**
 * V->Flt64 boundary: solve a generic [MechanismModel]<V> by converting to Flt64 first.
 * Uses [convertMechanismModelToFlt64] to validate V=Flt64 at the solver boundary.
 */
@Deprecated("Use solve(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun <V> AbstractLinearSolver.solve(
    model: MechanismModel<V>
): Ret<FeasibleSolverOutput<Flt64>> where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    val f64Model = when (val result = convertMechanismModelToFlt64(model)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    if (f64Model is LinearMechanismModel<Flt64>) {
        return solve(f64Model)
    }
    return Failed(ErrorCode.IllegalArgument, "LinearSolver requires LinearMechanismModel, got ${f64Model::class.simpleName}")
}

@Deprecated("Use solve(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractLinearSolver.solveWithOptions(
    model: LinearMechanismModel<Flt64>,
    options: SolveOptions
): Ret<FeasibleSolverOutput<Flt64>> {
    return dump(model).use {
        solveWithOptions(it, options)
    }
}

@Deprecated("Use solve(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractQuadraticSolver.solve(model: QuadraticMetaModel<Flt64>): Ret<FeasibleSolverOutput<Flt64>> {
    return solveWithOptions(model, SolveOptions())
}

@Deprecated("Use solve(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractQuadraticSolver.solve(model: QuadraticTetradModelView): Ret<FeasibleSolverOutput<Flt64>> {
    return solveWithOptions(model, SolveOptions())
}

@Deprecated("Use solve(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
suspend fun AbstractQuadraticSolver.solveWithOptions(
    model: QuadraticTetradModelView,
    options: SolveOptions
): Ret<FeasibleSolverOutput<Flt64>> {
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

@Deprecated("Use solve(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractQuadraticSolver.solveWithOptions(
    model: QuadraticMetaModel<Flt64>,
    options: SolveOptions
): Ret<FeasibleSolverOutput<Flt64>> {
    val registrationStatusCallBack: RegistrationStatusCallBack? = options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name)) }
    }
    val dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name, quadratic = true)) }
    }

    val mechanismModel = when (val result = dump(
        model = model,
        registrationStatusCallBack = registrationStatusCallBack,
        dumpingStatusCallBack = dumpingStatusCallBack
    )) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return mechanismModel.use {
        solveWithOptions(it, options)
    }
}

@Deprecated("Use solve(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractQuadraticSolver.solve(model: QuadraticMechanismModel<Flt64>): Ret<FeasibleSolverOutput<Flt64>> {
    return solveWithOptions(model, SolveOptions())
}

/**
 * V->Flt64 boundary: solve a generic [MechanismModel]<V> by converting to Flt64 first.
 * Uses [convertMechanismModelToFlt64] to validate V=Flt64 at the solver boundary.
 */
@Deprecated("Use solve(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun <V> AbstractQuadraticSolver.solve(
    model: MechanismModel<V>
): Ret<FeasibleSolverOutput<Flt64>> where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    val f64Model = when (val result = convertMechanismModelToFlt64(model)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    if (f64Model is QuadraticMechanismModel<Flt64>) {
        return solve(f64Model)
    }
    return Failed(ErrorCode.IllegalArgument, "QuadraticSolver requires QuadraticMechanismModel, got ${f64Model::class.simpleName}")
}

@Deprecated("Use solve(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractQuadraticSolver.solveWithOptions(
    model: QuadraticMechanismModel<Flt64>,
    options: SolveOptions
): Ret<FeasibleSolverOutput<Flt64>> {
    return dump(model).use {
        solveWithOptions(it, options)
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
): Ret<Pair<SolverOutput, List<List<Flt64>>>> {
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

@Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractLinearSolver.solveWithOptionsAndIIS(
    model: LinearMechanismModel<Flt64>,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<SolverOutput> {
    return dump(model).use {
        solveWithOptionsAndIIS(it, options, iisConfig)
    }
}

@Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractLinearSolver.solveWithOptionsAndIISForSolutionPool(
    model: LinearMechanismModel<Flt64>,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<Pair<SolverOutput, List<List<Flt64>>>> {
    return dump(model).use {
        solveWithOptionsAndIISForSolutionPool(it, options, iisConfig)
    }
}

@Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractLinearSolver.solveWithOptionsAndIIS(
    model: LinearMetaModel<Flt64>,
    options: SolveOptions,
    iisConfig: IISConfig,
    registrationStatusCallBack: RegistrationStatusCallBack? = null,
    dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
): Ret<SolverOutput> {
    val actualRegistrationStatusCallBack = registrationStatusCallBack ?: options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name)) }
    }
    val actualDumpingStatusCallBack = dumpingStatusCallBack ?: options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name, quadratic = false)) }
    }

    val mechanismModel = when (val result = dump(
        model = model,
        registrationStatusCallBack = actualRegistrationStatusCallBack,
        dumpingStatusCallBack = actualDumpingStatusCallBack
    )) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return mechanismModel.use {
        solveWithOptionsAndIIS(it, options, iisConfig)
    }
}

@Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractLinearSolver.solveWithOptionsAndIISForSolutionPool(
    model: LinearMetaModel<Flt64>,
    options: SolveOptions,
    iisConfig: IISConfig,
    registrationStatusCallBack: RegistrationStatusCallBack? = null,
    dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
): Ret<Pair<SolverOutput, List<List<Flt64>>>> {
    val actualRegistrationStatusCallBack = registrationStatusCallBack ?: options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name)) }
    }
    val actualDumpingStatusCallBack = dumpingStatusCallBack ?: options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name, quadratic = false)) }
    }

    val mechanismModel = when (val result = dump(
        model = model,
        registrationStatusCallBack = actualRegistrationStatusCallBack,
        dumpingStatusCallBack = actualDumpingStatusCallBack
    )) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return mechanismModel.use {
        solveWithOptionsAndIISForSolutionPool(it, options, iisConfig)
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
): Ret<Pair<SolverOutput, List<List<Flt64>>>> {
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

@Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractQuadraticSolver.solveWithOptionsAndIIS(
    model: QuadraticMechanismModel<Flt64>,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<SolverOutput> {
    return dump(model).use {
        solveWithOptionsAndIIS(it, options, iisConfig)
    }
}

@Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractQuadraticSolver.solveWithOptionsAndIISForSolutionPool(
    model: QuadraticMechanismModel<Flt64>,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<Pair<SolverOutput, List<List<Flt64>>>> {
    return dump(model).use {
        solveWithOptionsAndIISForSolutionPool(it, options, iisConfig)
    }
}

@Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractQuadraticSolver.solveWithOptionsAndIIS(
    model: QuadraticMetaModel<Flt64>,
    options: SolveOptions,
    iisConfig: IISConfig,
    registrationStatusCallBack: RegistrationStatusCallBack? = null,
    dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
): Ret<SolverOutput> {
    val actualRegistrationStatusCallBack = registrationStatusCallBack ?: options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name)) }
    }
    val actualDumpingStatusCallBack = dumpingStatusCallBack ?: options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name, quadratic = true)) }
    }

    val mechanismModel = when (val result = dump(
        model = model,
        registrationStatusCallBack = actualRegistrationStatusCallBack,
        dumpingStatusCallBack = actualDumpingStatusCallBack
    )) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return mechanismModel.use {
        solveWithOptionsAndIIS(it, options, iisConfig)
    }
}

@Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
suspend fun AbstractQuadraticSolver.solveWithOptionsAndIISForSolutionPool(
    model: QuadraticMetaModel<Flt64>,
    options: SolveOptions,
    iisConfig: IISConfig,
    registrationStatusCallBack: RegistrationStatusCallBack? = null,
    dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
): Ret<Pair<SolverOutput, List<List<Flt64>>>> {
    val actualRegistrationStatusCallBack = registrationStatusCallBack ?: options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name)) }
    }
    val actualDumpingStatusCallBack = dumpingStatusCallBack ?: options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name, quadratic = true)) }
    }

    val mechanismModel = when (val result = dump(
        model = model,
        registrationStatusCallBack = actualRegistrationStatusCallBack,
        dumpingStatusCallBack = actualDumpingStatusCallBack
    )) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return mechanismModel.use {
        solveWithOptionsAndIISForSolutionPool(it, options, iisConfig)
    }
}

private fun unwrapSolution(result: Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>>): Ret<FeasibleSolverOutput<Flt64>> {
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

@Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
@OptIn(DelicateCoroutinesApi::class)
fun AbstractLinearSolver.solveAsync(
    model: LinearMetaModel<Flt64>,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput<Flt64>>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput<Flt64>>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

@Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
@OptIn(DelicateCoroutinesApi::class)
fun AbstractLinearSolver.solveAsync(
    model: LinearMechanismModel<Flt64>,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput<Flt64>>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput<Flt64>>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

@Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
@OptIn(DelicateCoroutinesApi::class)
fun AbstractLinearSolver.solveAsync(
    model: LinearTriadModelView,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput<Flt64>>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput<Flt64>>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

@Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
@OptIn(DelicateCoroutinesApi::class)
fun AbstractQuadraticSolver.solveAsync(
    model: QuadraticMetaModel<Flt64>,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput<Flt64>>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput<Flt64>>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

@Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
@OptIn(DelicateCoroutinesApi::class)
fun AbstractQuadraticSolver.solveAsync(
    model: QuadraticMechanismModel<Flt64>,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput<Flt64>>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput<Flt64>>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

@Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION")
@OptIn(DelicateCoroutinesApi::class)
fun AbstractQuadraticSolver.solveAsync(
    model: QuadraticTetradModelView,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput<Flt64>>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput<Flt64>>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

// ========== V-generic primary solve extensions ==========
// These are the recommended V-generic entry points. They delegate to solveV which
// contains the full pipeline (validate -> dump -> solve -> convert).
// Flt64 solve() overloads above are adapter boundary for backward compatibility.

suspend fun <V> AbstractLinearSolver.solve(
    model: LinearTriadModelView,
    converter: IntoValue<V>
): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
    return solveV(model, converter)
}

@Suppress("DEPRECATION")
suspend fun <V> AbstractLinearSolver.solve(
    model: LinearMechanismModel<Flt64>,
    converter: IntoValue<V>
): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
    return solveV(model, converter)
}

@Suppress("DEPRECATION")
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

@Suppress("DEPRECATION")
suspend fun <V> AbstractQuadraticSolver.solve(
    model: QuadraticMechanismModel<Flt64>,
    converter: IntoValue<V>
): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
    return solveV(model, converter)
}

@Suppress("DEPRECATION")
suspend fun <V> AbstractQuadraticSolver.solve(
    model: MechanismModel<V>,
    converter: IntoValue<V>
): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
    return solveV(model, converter)
}
