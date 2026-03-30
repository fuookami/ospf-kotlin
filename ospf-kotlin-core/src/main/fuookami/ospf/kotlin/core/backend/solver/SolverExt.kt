package fuookami.ospf.kotlin.core.backend.solver

import fuookami.ospf.kotlin.core.backend.intermediate_model.LinearTriadModelView
import fuookami.ospf.kotlin.core.backend.intermediate_model.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.backend.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.backend.solver.iis.computeIIS
import fuookami.ospf.kotlin.core.backend.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.backend.solver.output.LinearInfeasibleSolverOutput
import fuookami.ospf.kotlin.core.backend.solver.output.QuadraticInfeasibleSolverOutput
import fuookami.ospf.kotlin.core.backend.solver.output.resolveInfeasibleUnifiedFields
import fuookami.ospf.kotlin.core.backend.solver.output.SolverOutput
import fuookami.ospf.kotlin.core.backend.solver.output.SolvingStatus
import fuookami.ospf.kotlin.core.backend.solver.value.validateLinearModelValueConversion
import fuookami.ospf.kotlin.core.backend.solver.value.validateQuadraticModelValueConversion
import fuookami.ospf.kotlin.core.backend.solver.value.withSolveValueConversionPolicy
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.MechanismModelDumpingStatusCallBack
import fuookami.ospf.kotlin.core.frontend.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.frontend.model.mechanism.toModelBuildingStatus
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

suspend fun AbstractLinearSolver.solve(model: LinearTriadModelView): Ret<FeasibleSolverOutput> {
    return solveWithOptions(model, SolveOptions())
}

suspend fun AbstractLinearSolver.solveWithOptions(
    model: LinearTriadModelView,
    options: SolveOptions
): Ret<FeasibleSolverOutput> {
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

suspend fun AbstractLinearSolver.solve(model: LinearMetaModel): Ret<FeasibleSolverOutput> {
    return solveWithOptions(model, SolveOptions())
}

suspend fun AbstractLinearSolver.solveWithOptions(
    model: LinearMetaModel,
    options: SolveOptions
): Ret<FeasibleSolverOutput> {
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

suspend fun AbstractLinearSolver.solve(model: LinearMechanismModel): Ret<FeasibleSolverOutput> {
    return solveWithOptions(model, SolveOptions())
}

suspend fun AbstractLinearSolver.solveWithOptions(
    model: LinearMechanismModel,
    options: SolveOptions
): Ret<FeasibleSolverOutput> {
    return dump(model).use {
        solveWithOptions(it, options)
    }
}

suspend fun AbstractQuadraticSolver.solve(model: QuadraticMetaModel): Ret<FeasibleSolverOutput> {
    return solveWithOptions(model, SolveOptions())
}

suspend fun AbstractQuadraticSolver.solve(model: QuadraticTetradModelView): Ret<FeasibleSolverOutput> {
    return solveWithOptions(model, SolveOptions())
}

suspend fun AbstractQuadraticSolver.solveWithOptions(
    model: QuadraticTetradModelView,
    options: SolveOptions
): Ret<FeasibleSolverOutput> {
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

suspend fun AbstractQuadraticSolver.solveWithOptions(
    model: QuadraticMetaModel,
    options: SolveOptions
): Ret<FeasibleSolverOutput> {
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

suspend fun AbstractQuadraticSolver.solve(model: QuadraticMechanismModel): Ret<FeasibleSolverOutput> {
    return solveWithOptions(model, SolveOptions())
}

suspend fun AbstractQuadraticSolver.solveWithOptions(
    model: QuadraticMechanismModel,
    options: SolveOptions
): Ret<FeasibleSolverOutput> {
    return dump(model).use {
        solveWithOptions(it, options)
    }
}

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

suspend fun AbstractLinearSolver.solveWithOptionsAndIISForSolutionPool(
    model: LinearTriadModelView,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<Pair<SolverOutput, List<Solution>>> {
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

suspend fun AbstractLinearSolver.solveWithOptionsAndIIS(
    model: LinearMechanismModel,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<SolverOutput> {
    return dump(model).use {
        solveWithOptionsAndIIS(it, options, iisConfig)
    }
}

suspend fun AbstractLinearSolver.solveWithOptionsAndIISForSolutionPool(
    model: LinearMechanismModel,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<Pair<SolverOutput, List<Solution>>> {
    return dump(model).use {
        solveWithOptionsAndIISForSolutionPool(it, options, iisConfig)
    }
}

suspend fun AbstractLinearSolver.solveWithOptionsAndIIS(
    model: LinearMetaModel,
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

suspend fun AbstractLinearSolver.solveWithOptionsAndIISForSolutionPool(
    model: LinearMetaModel,
    options: SolveOptions,
    iisConfig: IISConfig,
    registrationStatusCallBack: RegistrationStatusCallBack? = null,
    dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
): Ret<Pair<SolverOutput, List<Solution>>> {
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

suspend fun AbstractQuadraticSolver.solveWithOptionsAndIISForSolutionPool(
    model: QuadraticTetradModelView,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<Pair<SolverOutput, List<Solution>>> {
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

suspend fun AbstractQuadraticSolver.solveWithOptionsAndIIS(
    model: QuadraticMechanismModel,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<SolverOutput> {
    return dump(model).use {
        solveWithOptionsAndIIS(it, options, iisConfig)
    }
}

suspend fun AbstractQuadraticSolver.solveWithOptionsAndIISForSolutionPool(
    model: QuadraticMechanismModel,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<Pair<SolverOutput, List<Solution>>> {
    return dump(model).use {
        solveWithOptionsAndIISForSolutionPool(it, options, iisConfig)
    }
}

suspend fun AbstractQuadraticSolver.solveWithOptionsAndIIS(
    model: QuadraticMetaModel,
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

suspend fun AbstractQuadraticSolver.solveWithOptionsAndIISForSolutionPool(
    model: QuadraticMetaModel,
    options: SolveOptions,
    iisConfig: IISConfig,
    registrationStatusCallBack: RegistrationStatusCallBack? = null,
    dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
): Ret<Pair<SolverOutput, List<Solution>>> {
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

private fun unwrapSolution(result: Ret<Pair<FeasibleSolverOutput, List<Solution>>>): Ret<FeasibleSolverOutput> {
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

@OptIn(DelicateCoroutinesApi::class)
fun AbstractLinearSolver.solveAsync(
    model: LinearMetaModel,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun AbstractLinearSolver.solveAsync(
    model: LinearMechanismModel,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun AbstractLinearSolver.solveAsync(
    model: LinearTriadModelView,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun AbstractQuadraticSolver.solveAsync(
    model: QuadraticMetaModel,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun AbstractQuadraticSolver.solveAsync(
    model: QuadraticMechanismModel,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun AbstractQuadraticSolver.solveAsync(
    model: QuadraticTetradModelView,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}
