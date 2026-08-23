/**
 * 求解器扩展函数 / Solver extension functions
*/
package fuookami.ospf.kotlin.core.solver

import java.util.concurrent.CompletableFuture
import kotlin.time.TimeSource
import kotlinx.coroutines.future.future
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.iis.*
import fuookami.ospf.kotlin.core.solver.value.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.SolveDiagnostics
import fuookami.ospf.kotlin.core.solver.report.SolveIssue
import fuookami.ospf.kotlin.core.solver.report.SolveIssueCategory
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.SolveHandle

/**
 * 使用默认选项求解线性模型。 / Solve a linear model with default options.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @return 求解结果 / Solve result
*/
suspend fun AbstractLinearSolver.solve(model: LinearTriadModelView): Ret<SolveReport<Flt64>> {
    return solveWithOptions(model, SolveOptions())
}

/**
 * 使用指定选项求解线性模型。 / Solve a linear model with specified options.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @param options 求解选项 / Solve options
 * @return 求解结果 / Solve result
*/
suspend fun AbstractLinearSolver.solveWithOptions(
    model: LinearTriadModelView,
    options: SolveOptions
): Ret<SolveReport<Flt64>> {
    if (options.cancellationToken?.isCancellationRequested == true) {
        return Ok(cancelledSolveReport(options.cancellationToken.record?.reason))
    }
    when (val identity = model.identityValidation) {
        is Ok -> {}
        is Failed -> return Failed(identity.error)
        is Fatal -> return Fatal(identity.errors)
    }
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
                solvingStatusCallBack = options.solvingStatusCallBack,
                cancellationToken = options.cancellationToken
            ))
        } ?: this@solveWithOptions(
            model = model,
            solvingStatusCallBack = options.solvingStatusCallBack,
            cancellationToken = options.cancellationToken
        )
    }
}

/**
 * 使用默认选项求解二次模型。 / Solve a quadratic model with default options.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @return 求解结果 / Solve result
*/
suspend fun AbstractQuadraticSolver.solve(model: QuadraticTetradModelView): Ret<SolveReport<Flt64>> {
    return solveWithOptions(model, SolveOptions())
}

/**
 * 使用指定选项求解二次模型。 / Solve a quadratic model with specified options.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param options 求解选项 / Solve options
 * @return 求解结果 / Solve result
*/
suspend fun AbstractQuadraticSolver.solveWithOptions(
    model: QuadraticTetradModelView,
    options: SolveOptions
): Ret<SolveReport<Flt64>> {
    if (options.cancellationToken?.isCancellationRequested == true) {
        return Ok(cancelledSolveReport(options.cancellationToken.record?.reason))
    }
    when (val identity = model.identityValidation) {
        is Ok -> {}
        is Failed -> return Failed(identity.error)
        is Fatal -> return Fatal(identity.errors)
    }
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
                solvingStatusCallBack = options.solvingStatusCallBack,
                cancellationToken = options.cancellationToken
            ))
        } ?: this@solveWithOptions(
            model = model,
            solvingStatusCallBack = options.solvingStatusCallBack,
            cancellationToken = options.cancellationToken
        )
    }
}

/**
 * 使用选项和 IIS 配置求解线性模型，失败时计算不可行子系统。 / Solve a linear model with options and IIS configuration; compute IIS on infeasibility.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @param options 求解选项 / Solve options
 * @param iisConfig IIS 配置 / IIS configuration
 * @return 求解结果（可能包含 IIS）/ Solve result (may contain IIS)
*/
suspend fun AbstractLinearSolver.solveWithOptionsAndIIS(
    model: LinearTriadModelView,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<SolverOutput> {
    val solveStartedAt = TimeSource.Monotonic.markNow()
    var latestSolvingStatus: SolvingStatus? = null
    val statusBridge = SolvingStatusCallbackBridge(options.solvingStatusCallBack) { status ->
        latestSolvingStatus = status
    }

    val result = solveWithOptions(
        model = model,
        options = options.copy(solvingStatusCallBack = statusBridge.callback)
    )
    statusBridge.failure?.let { return propagateStatusFailure(it) }
    return when (result) {
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
                        val unifiedFields = resolveInfeasibleUnifiedFields(
                            latestStatus = latestSolvingStatus,
                            fallbackSolveTime = solveStartedAt.elapsedNow()
                        )
                        Ok(
                            LinearInfeasibleSolverOutput(
                                iis = snapshotLinearIisModel(model),
                                iisAvailable = false,
                                iterations = unifiedFields.iterations,
                                nodeCount = unifiedFields.nodeCount,
                                bestBound = unifiedFields.bestBound,
                                mipGap = unifiedFields.mipGap,
                                solveTime = unifiedFields.solveTime,
                                diagnostics = iisFailureDiagnostics(iisResult.error)
                            )
                        )
                    }

                    is Fatal -> {
                        val unifiedFields = resolveInfeasibleUnifiedFields(
                            latestStatus = latestSolvingStatus,
                            fallbackSolveTime = solveStartedAt.elapsedNow()
                        )
                        Ok(
                            LinearInfeasibleSolverOutput(
                                iis = snapshotLinearIisModel(model),
                                iisAvailable = false,
                                iterations = unifiedFields.iterations,
                                nodeCount = unifiedFields.nodeCount,
                                bestBound = unifiedFields.bestBound,
                                mipGap = unifiedFields.mipGap,
                                solveTime = unifiedFields.solveTime,
                                diagnostics = iisFailureDiagnostics(iisResult.errors)
                            )
                        )
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

/**
 * 使用选项和 IIS 配置从解池求解线性模型。 / Solve a linear model from solution pool with options and IIS configuration.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @param options 求解选项 / Solve options
 * @param iisConfig IIS 配置 / IIS configuration
 * @return 求解结果与解列表 / Solve result with solution list
*/
suspend fun AbstractLinearSolver.solveWithOptionsAndIISForSolutionPool(
    model: LinearTriadModelView,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<Pair<SolverOutput, List<List<Flt64>>>> {
    when (val identity = model.identityValidation) {
        is Ok -> {}
        is Failed -> return Failed(identity.error)
        is Fatal -> return Fatal(identity.errors)
    }
    when (val validation = validateLinearModelValueConversion(model, options.effectiveValueConversionPolicy)) {
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
        else -> {}
    }

    return withSolveValueConversionPolicy(options.effectiveValueConversionPolicy) {
        val solveStartedAt = TimeSource.Monotonic.markNow()
        var latestSolvingStatus: SolvingStatus? = null
        val statusBridge = SolvingStatusCallbackBridge(options.solvingStatusCallBack) { status ->
            latestSolvingStatus = status
        }

        val solutionAmount =
            options.solutionAmount ?: return@withSolveValueConversionPolicy when (val result = solveWithOptionsAndIIS(
                model = model,
                options = options.copy(solvingStatusCallBack = statusBridge.callback),
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

        statusBridge.failure?.let { return@withSolveValueConversionPolicy propagateStatusFailure(it) }
        when (val result = this@solveWithOptionsAndIISForSolutionPool(
            model = model,
            solutionAmount = solutionAmount,
            solvingStatusCallBack = statusBridge.callback,
            cancellationToken = options.cancellationToken
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
                            val unifiedFields = resolveInfeasibleUnifiedFields(
                                latestStatus = latestSolvingStatus,
                                fallbackSolveTime = solveStartedAt.elapsedNow()
                            )
                            Ok(
                                LinearInfeasibleSolverOutput(
                                    iis = snapshotLinearIisModel(model),
                                    iisAvailable = false,
                                    iterations = unifiedFields.iterations,
                                    nodeCount = unifiedFields.nodeCount,
                                    bestBound = unifiedFields.bestBound,
                                    mipGap = unifiedFields.mipGap,
                                    solveTime = unifiedFields.solveTime,
                                    diagnostics = iisFailureDiagnostics(iisResult.error)
                                ) to emptyList()
                            )
                        }

                        is Fatal -> {
                            val unifiedFields = resolveInfeasibleUnifiedFields(
                                latestStatus = latestSolvingStatus,
                                fallbackSolveTime = solveStartedAt.elapsedNow()
                            )
                            Ok(
                                LinearInfeasibleSolverOutput(
                                    iis = snapshotLinearIisModel(model),
                                    iisAvailable = false,
                                    iterations = unifiedFields.iterations,
                                    nodeCount = unifiedFields.nodeCount,
                                    bestBound = unifiedFields.bestBound,
                                    mipGap = unifiedFields.mipGap,
                                    solveTime = unifiedFields.solveTime,
                                    diagnostics = iisFailureDiagnostics(iisResult.errors)
                                ) to emptyList()
                            )
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

/**
 * 使用选项和 IIS 配置求解二次模型，失败时计算不可行子系统。 / Solve a quadratic model with options and IIS configuration; compute IIS on infeasibility.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param options 求解选项 / Solve options
 * @param iisConfig IIS 配置 / IIS configuration
 * @return 求解结果（可能包含 IIS）/ Solve result (may contain IIS)
*/
suspend fun AbstractQuadraticSolver.solveWithOptionsAndIIS(
    model: QuadraticTetradModelView,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<SolverOutput> {
    val solveStartedAt = TimeSource.Monotonic.markNow()
    var latestSolvingStatus: SolvingStatus? = null
    val statusBridge = SolvingStatusCallbackBridge(options.solvingStatusCallBack) { status ->
        latestSolvingStatus = status
    }

    val result = solveWithOptions(
        model = model,
        options = options.copy(solvingStatusCallBack = statusBridge.callback)
    )
    statusBridge.failure?.let { return propagateStatusFailure(it) }
    return when (result) {
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
                        val unifiedFields = resolveInfeasibleUnifiedFields(
                            latestStatus = latestSolvingStatus,
                            fallbackSolveTime = solveStartedAt.elapsedNow()
                        )
                        Ok(
                            QuadraticInfeasibleSolverOutput(
                                iis = snapshotQuadraticIisModel(model),
                                iisAvailable = false,
                                iterations = unifiedFields.iterations,
                                nodeCount = unifiedFields.nodeCount,
                                bestBound = unifiedFields.bestBound,
                                mipGap = unifiedFields.mipGap,
                                solveTime = unifiedFields.solveTime,
                                diagnostics = iisFailureDiagnostics(iisResult.error)
                            )
                        )
                    }

                    is Fatal -> {
                        val unifiedFields = resolveInfeasibleUnifiedFields(
                            latestStatus = latestSolvingStatus,
                            fallbackSolveTime = solveStartedAt.elapsedNow()
                        )
                        Ok(
                            QuadraticInfeasibleSolverOutput(
                                iis = snapshotQuadraticIisModel(model),
                                iisAvailable = false,
                                iterations = unifiedFields.iterations,
                                nodeCount = unifiedFields.nodeCount,
                                bestBound = unifiedFields.bestBound,
                                mipGap = unifiedFields.mipGap,
                                solveTime = unifiedFields.solveTime,
                                diagnostics = iisFailureDiagnostics(iisResult.errors)
                            )
                        )
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

/**
 * 使用选项和 IIS 配置从解池求解二次模型。 / Solve a quadratic model from solution pool with options and IIS configuration.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param options 求解选项 / Solve options
 * @param iisConfig IIS 配置 / IIS configuration
 * @return 求解结果与解列表 / Solve result with solution list
*/
suspend fun AbstractQuadraticSolver.solveWithOptionsAndIISForSolutionPool(
    model: QuadraticTetradModelView,
    options: SolveOptions,
    iisConfig: IISConfig
): Ret<Pair<SolverOutput, List<List<Flt64>>>> {
    when (val identity = model.identityValidation) {
        is Ok -> {}
        is Failed -> return Failed(identity.error)
        is Fatal -> return Fatal(identity.errors)
    }
    when (val validation = validateQuadraticModelValueConversion(model, options.effectiveValueConversionPolicy)) {
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
        else -> {}
    }

    return withSolveValueConversionPolicy(options.effectiveValueConversionPolicy) {
        val solveStartedAt = TimeSource.Monotonic.markNow()
        var latestSolvingStatus: SolvingStatus? = null
        val statusBridge = SolvingStatusCallbackBridge(options.solvingStatusCallBack) { status ->
            latestSolvingStatus = status
        }

        val solutionAmount = options.solutionAmount
        if (solutionAmount == null) {
            return@withSolveValueConversionPolicy when (val result = solveWithOptionsAndIIS(
                model = model,
                options = options.copy(solvingStatusCallBack = statusBridge.callback),
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

        statusBridge.failure?.let { return@withSolveValueConversionPolicy propagateStatusFailure(it) }
        when (val result = this@solveWithOptionsAndIISForSolutionPool(
            model = model,
            solutionAmount = solutionAmount,
            solvingStatusCallBack = statusBridge.callback,
            cancellationToken = options.cancellationToken
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
                            val unifiedFields = resolveInfeasibleUnifiedFields(
                                latestStatus = latestSolvingStatus,
                                fallbackSolveTime = solveStartedAt.elapsedNow()
                            )
                            Ok(
                                QuadraticInfeasibleSolverOutput(
                                    iis = snapshotQuadraticIisModel(model),
                                    iisAvailable = false,
                                    iterations = unifiedFields.iterations,
                                    nodeCount = unifiedFields.nodeCount,
                                    bestBound = unifiedFields.bestBound,
                                    mipGap = unifiedFields.mipGap,
                                    solveTime = unifiedFields.solveTime,
                                    diagnostics = iisFailureDiagnostics(iisResult.error)
                                ) to emptyList()
                            )
                        }

                        is Fatal -> {
                            val unifiedFields = resolveInfeasibleUnifiedFields(
                                latestStatus = latestSolvingStatus,
                                fallbackSolveTime = solveStartedAt.elapsedNow()
                            )
                            Ok(
                                QuadraticInfeasibleSolverOutput(
                                    iis = snapshotQuadraticIisModel(model),
                                    iisAvailable = false,
                                    iterations = unifiedFields.iterations,
                                    nodeCount = unifiedFields.nodeCount,
                                    bestBound = unifiedFields.bestBound,
                                    mipGap = unifiedFields.mipGap,
                                    solveTime = unifiedFields.solveTime,
                                    diagnostics = iisFailureDiagnostics(iisResult.errors)
                                ) to emptyList()
                            )
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

/** 将 IIS 失败保留为结构化诊断问题。 / Preserve IIS failure as a structured diagnostic issue. */
private fun iisFailureDiagnostics(error: Error<ErrorCode>): SolveDiagnostics<Flt64> {
    return SolveDiagnostics(errors = listOf(iisFailureIssue(error)))
}

/** 将多个 IIS 失败保留为结构化诊断问题。 / Preserve multiple IIS failures as structured diagnostic issues. */
private fun iisFailureDiagnostics(errors: List<Error<ErrorCode>>): SolveDiagnostics<Flt64> {
    return SolveDiagnostics(errors = errors.map(::iisFailureIssue))
}

private fun iisFailureIssue(error: Error<ErrorCode>): SolveIssue {
    return SolveIssue(
        code = "iis-diagnostic-failed",
        category = SolveIssueCategory.Backend,
        message = "IIS 诊断失败，原始不可行结论已保留：${error.message} / " +
            "IIS diagnostic failed; the original infeasible conclusion was preserved: ${error.message}",
        details = mapOf("errorCode" to error.code.toString())
    )
}

private fun snapshotLinearIisModel(model: LinearTriadModelView): BasicLinearTriadModelView {
    return BasicLinearTriadModel(
        variables = model.variables.map { it.copy() },
        constraints = model.constraints.copy(),
        name = "${model.name}_iis_unavailable"
    )
}

private fun snapshotQuadraticIisModel(model: QuadraticTetradModelView): QuadraticTetradModel {
    return QuadraticTetradModel(
        impl = BasicQuadraticTetradModel(
            variables = model.variables.map { it.copy() },
            constraints = model.constraints.copy(),
            name = "${model.name}_iis_unavailable"
        ),
        tokensInSolver = if (model is QuadraticTetradModel) {
            model.tokensInSolver.toList()
        } else {
            emptyList()
        },
        objective = QuadraticObjective(
            category = model.objective.category,
            objective = model.objective.objective.map { it.copy() },
            constant = model.objective.constant.copy()
        )
    )
}

/** Preserve callback failures across solver adapters. / 在 solver adapter 边界保留回调失败结果。 */
private class SolvingStatusCallbackBridge(
    delegate: SolvingStatusCallBack?,
    private val onStatus: (SolvingStatus) -> Unit
) {
    var failure: Try? = null
        private set

    val callback: SolvingStatusCallBack? = delegate?.let { callback ->
        SolvingStatusCallBack { status ->
            onStatus(status)
            val result = callback(status)
            if (result.failed && failure == null) {
                failure = result
            }
            result
        }
    }
}

private fun <T> propagateStatusFailure(result: Try): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Ok -> Failed(
            ErrorCode.ApplicationError,
            "求解状态回调结果无效 / Invalid solving status callback result"
        )
    }
}

/**
 * 从包含解列表的求解结果中提取求解器输出。 / Extract the solver output from a solve result that includes a solution list.
 *
 * @param result 包含求解器输出与解列表的配对结果 / A paired result containing solver output and solution list
 * @return 仅包含求解器输出的求解结果 / Solve result containing only the solver output
*/
private fun unwrapSolution(result: Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>>): Ret<SolveReport<Flt64>> {
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

/**
 * 异步求解线性模型。 / Asynchronously solve a linear model.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @param options 求解选项 / Solve options
 * @param callBack 可选的回调函数 / Optional callback function
 * @return 异步求解结果 / Async solve result
*/
fun AbstractLinearSolver.solveAsync(
    model: LinearTriadModelView,
    options: SolveOptions,
    callBack: ((Ret<SolveReport<Flt64>>) -> Unit)? = null
): CompletableFuture<Ret<SolveReport<Flt64>>> {
    val token = options.cancellationToken ?: SolveHandle.create().token
    return cancellableSolveFuture(token) {
        val result = solveWithOptions(model, options.copy(cancellationToken = token))
        callBack?.invoke(result)
        result
    }
}

/**
 * 异步求解二次模型。 / Asynchronously solve a quadratic model.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param options 求解选项 / Solve options
 * @param callBack 可选的回调函数 / Optional callback function
 * @return 异步求解结果 / Async solve result
*/
fun AbstractQuadraticSolver.solveAsync(
    model: QuadraticTetradModelView,
    options: SolveOptions,
    callBack: ((Ret<SolveReport<Flt64>>) -> Unit)? = null
): CompletableFuture<Ret<SolveReport<Flt64>>> {
    val token = options.cancellationToken ?: SolveHandle.create().token
    return cancellableSolveFuture(token) {
        val result = solveWithOptions(model, options.copy(cancellationToken = token))
        callBack?.invoke(result)
        result
    }
}
