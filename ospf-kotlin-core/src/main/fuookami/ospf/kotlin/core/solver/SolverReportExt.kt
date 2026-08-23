/**
 * 统一求解报告入口 / Unified solve-report entry points
 */
package fuookami.ospf.kotlin.core.solver

import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.future.future
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.progress.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.value.*

/**
 * 使用解池入口返回线性统一报告。 / Return a unified report from the linear solution-pool entry point.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @param solutionAmount 期望解数量 / Desired number of solutions
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @param progressContext 统一进度上下文 / Unified progress context
 * @param cancellationToken 求解取消令牌 / Solve cancellation token
 * @return 统一求解报告 / Unified solve report
 */
 suspend fun AbstractLinearSolver.solveReport(
     model: LinearTriadModelView,
     solutionAmount: UInt64,
     solvingStatusCallBack: SolvingStatusCallBack? = null,
    progressContext: SolverProgressContext? = null,
    cancellationToken: CancellationToken? = null
 ): Ret<SolveReport<Flt64>> {
     return solveReportWithSolutionPool(
         model = model,
         solutionAmount = solutionAmount,
         solvingStatusCallBack = solvingStatusCallBack,
         progressContext = progressContext,
        valueConversionPolicy = SolveValueConversionPolicy.AllowRounding,
        cancellationToken = cancellationToken
     )
 }

private suspend fun AbstractLinearSolver.solveReportWithSolutionPool(
    model: LinearTriadModelView,
    solutionAmount: UInt64,
    solvingStatusCallBack: SolvingStatusCallBack? = null,
    progressContext: SolverProgressContext? = null,
    valueConversionPolicy: SolveValueConversionPolicy,
    cancellationToken: CancellationToken? = null
 ): Ret<SolveReport<Flt64>> {
    if (cancellationToken?.isCancellationRequested == true) {
        return Ok(cancelledSolveReport(cancellationToken.record?.reason))
    }
     when (val validation = model.identityValidation) {
        is Ok -> {}
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
    }
    when (val validation = validateLinearModelValueConversion(model, valueConversionPolicy)) {
        is Ok -> {}
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
    }
    reportLinearProgress(progressContext, name, 0)
     return withSolveValueConversionPolicy(valueConversionPolicy) {
        when (val result = invoke(model, solutionAmount, solvingStatusCallBack, cancellationToken)) {
            is Ok -> {
                reportLinearProgress(progressContext, name, 100)
                Ok(
                    result.value.first.toSolveReport(
                        solutionPool = result.value.second
                    ).withModelDiagnostics(model)
                )
            }

            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }
}

/**
 * 使用解池入口返回二次统一报告。 / Return a unified report from the quadratic solution-pool entry point.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param solutionAmount 期望解数量 / Desired number of solutions
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @param progressContext 统一进度上下文 / Unified progress context
 * @param cancellationToken 求解取消令牌 / Solve cancellation token
 * @return 统一求解报告 / Unified solve report
 */
 suspend fun AbstractQuadraticSolver.solveReport(
     model: QuadraticTetradModelView,
     solutionAmount: UInt64,
     solvingStatusCallBack: SolvingStatusCallBack? = null,
    progressContext: SolverProgressContext? = null,
    cancellationToken: CancellationToken? = null
 ): Ret<SolveReport<Flt64>> {
     return solveReportWithSolutionPool(
         model = model,
         solutionAmount = solutionAmount,
         solvingStatusCallBack = solvingStatusCallBack,
         progressContext = progressContext,
        valueConversionPolicy = SolveValueConversionPolicy.AllowRounding,
        cancellationToken = cancellationToken
     )
 }

private suspend fun AbstractQuadraticSolver.solveReportWithSolutionPool(
    model: QuadraticTetradModelView,
    solutionAmount: UInt64,
    solvingStatusCallBack: SolvingStatusCallBack? = null,
    progressContext: SolverProgressContext? = null,
    valueConversionPolicy: SolveValueConversionPolicy,
    cancellationToken: CancellationToken? = null
 ): Ret<SolveReport<Flt64>> {
    if (cancellationToken?.isCancellationRequested == true) {
        return Ok(cancelledSolveReport(cancellationToken.record?.reason))
    }
     when (val validation = model.identityValidation) {
        is Ok -> {}
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
    }
    when (val validation = validateQuadraticModelValueConversion(model, valueConversionPolicy)) {
        is Ok -> {}
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
    }
    reportQuadraticProgress(progressContext, name, 0)
     return withSolveValueConversionPolicy(valueConversionPolicy) {
        when (val result = invoke(model, solutionAmount, solvingStatusCallBack, cancellationToken)) {
            is Ok -> {
                reportQuadraticProgress(progressContext, name, 100)
                Ok(
                    result.value.first.toSolveReport(
                        solutionPool = result.value.second
                    ).withModelDiagnostics(model)
                )
            }

            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }
}

/**
 * 使用泛型数值转换返回线性报告。 / Return a linear report with generic numeric values.
 *
 * @param V 目标数值类型 / Target numeric type
 * @param model 线性三元模型视图 / Linear triad model view
 * @param converter 数值转换器 / Numeric converter
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @return 泛型数值求解报告 / Generic numeric solve report
 */
suspend fun <V> AbstractLinearSolver.solveReport(
    model: LinearTriadModelView,
    converter: IntoValue<V>,
    solvingStatusCallBack: SolvingStatusCallBack? = null
): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
    return when (val result = invoke(model, solvingStatusCallBack)) {
        is Ok -> Ok(result.value.toSolveReport().withModelDiagnostics(model).convertTo(converter))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 使用泛型数值转换返回线性解池报告。 / Return a linear solution-pool report with generic numeric values.
 *
 * @param V 目标数值类型 / Target numeric type
 * @param model 线性三元模型视图 / Linear triad model view
 * @param solutionAmount 期望解数量 / Desired number of solutions
 * @param converter 数值转换器 / Numeric converter
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @return 泛型数值解池报告 / Generic numeric solution-pool report
 */
suspend fun <V> AbstractLinearSolver.solveReport(
    model: LinearTriadModelView,
    solutionAmount: UInt64,
    converter: IntoValue<V>,
    solvingStatusCallBack: SolvingStatusCallBack? = null
): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
    return when (val result = invoke(model, solutionAmount, solvingStatusCallBack)) {
        is Ok -> Ok(
            result.value.first.toSolveReport(solutionPool = result.value.second).withModelDiagnostics(model).convertTo(converter)
        )
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 使用泛型数值转换从线性机制模型返回报告。 / Return a report with generic numeric values from a linear mechanism model.
 *
 * @param V 目标数值类型 / Target numeric type
 * @param model 线性机制模型 / Linear mechanism model
 * @param converter 数值转换器 / Numeric converter
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @return 泛型数值求解报告 / Generic numeric solve report
 */
suspend fun <V> AbstractLinearSolver.solveReport(
    model: MechanismModel<V>,
    converter: IntoValue<V>,
    solvingStatusCallBack: SolvingStatusCallBack? = null
): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
    return when (val converted = convertMechanismModelToFlt64(model)) {
        is Ok -> {
            val linearModel = converted.value as? LinearMechanismModel<Flt64>
                ?: return Failed(
                    Err(
                        ErrorCode.IllegalArgument,
                        "Linear solver requires LinearMechanismModel, but got ${converted.value::class.simpleName}"
                    )
                )
            when (val dumped = dumpResult(linearModel)) {
                is Ok -> dumped.value.use {
                    solveReport(it, converter, solvingStatusCallBack)
                }
                is Failed -> Failed(dumped.error)
                is Fatal -> Fatal(dumped.errors)
            }
        }

        is Failed -> Failed(converted.error)
        is Fatal -> Fatal(converted.errors)
    }
}

/**
 * 使用泛型数值转换从线性机制模型返回解池报告。 / Return a generic numeric solution-pool report from a linear mechanism model.
 *
 * @param V 目标数值类型 / Target numeric type
 * @param model 线性机制模型 / Linear mechanism model
 * @param solutionAmount 期望解数量 / Desired number of solutions
 * @param converter 数值转换器 / Numeric converter
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @return 泛型数值解池报告 / Generic numeric solution-pool report
 */
suspend fun <V> AbstractLinearSolver.solveReport(
    model: MechanismModel<V>,
    solutionAmount: UInt64,
    converter: IntoValue<V>,
    solvingStatusCallBack: SolvingStatusCallBack? = null
): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
    return when (val converted = convertMechanismModelToFlt64(model)) {
        is Ok -> {
            val linearModel = converted.value as? LinearMechanismModel<Flt64>
                ?: return Failed(
                    Err(
                        ErrorCode.IllegalArgument,
                        "Linear solver requires LinearMechanismModel, but got ${converted.value::class.simpleName}"
                    )
                )
            when (val dumped = dumpResult(linearModel)) {
                is Ok -> dumped.value.use {
                    solveReport(it, solutionAmount, converter, solvingStatusCallBack)
                }
                is Failed -> Failed(dumped.error)
                is Fatal -> Fatal(dumped.errors)
            }
        }

        is Failed -> Failed(converted.error)
        is Fatal -> Fatal(converted.errors)
    }
}

/**
 * 使用泛型数值转换返回二次报告。 / Return a quadratic report with generic numeric values.
 *
 * @param V 目标数值类型 / Target numeric type
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param converter 数值转换器 / Numeric converter
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @return 泛型数值求解报告 / Generic numeric solve report
 */
suspend fun <V> AbstractQuadraticSolver.solveReport(
    model: QuadraticTetradModelView,
    converter: IntoValue<V>,
    solvingStatusCallBack: SolvingStatusCallBack? = null
): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
    return when (val result = invoke(model, solvingStatusCallBack)) {
        is Ok -> Ok(result.value.toSolveReport().withModelDiagnostics(model).convertTo(converter))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 使用泛型数值转换返回二次解池报告。 / Return a quadratic solution-pool report with generic numeric values.
 *
 * @param V 目标数值类型 / Target numeric type
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param solutionAmount 期望解数量 / Desired number of solutions
 * @param converter 数值转换器 / Numeric converter
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @return 泛型数值解池报告 / Generic numeric solution-pool report
 */
suspend fun <V> AbstractQuadraticSolver.solveReport(
    model: QuadraticTetradModelView,
    solutionAmount: UInt64,
    converter: IntoValue<V>,
    solvingStatusCallBack: SolvingStatusCallBack? = null
): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
    return when (val result = invoke(model, solutionAmount, solvingStatusCallBack)) {
        is Ok -> Ok(
            result.value.first.toSolveReport(solutionPool = result.value.second).withModelDiagnostics(model).convertTo(converter)
        )
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 使用泛型数值转换从二次机制模型返回报告。 / Return a report with generic numeric values from a quadratic mechanism model.
 *
 * @param V 目标数值类型 / Target numeric type
 * @param model 二次机制模型 / Quadratic mechanism model
 * @param converter 数值转换器 / Numeric converter
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @return 泛型数值求解报告 / Generic numeric solve report
 */
suspend fun <V> AbstractQuadraticSolver.solveReport(
    model: MechanismModel<V>,
    converter: IntoValue<V>,
    solvingStatusCallBack: SolvingStatusCallBack? = null
): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
    return when (val converted = convertMechanismModelToFlt64(model)) {
        is Ok -> {
            val quadraticModel = converted.value as? QuadraticMechanismModel<Flt64>
                ?: return Failed(
                    Err(
                        ErrorCode.IllegalArgument,
                        "Quadratic solver requires QuadraticMechanismModel, but got ${converted.value::class.simpleName}"
                    )
                )
            when (val dumped = dumpResult(quadraticModel)) {
                is Ok -> dumped.value.use {
                    solveReport(it, converter, solvingStatusCallBack)
                }
                is Failed -> Failed(dumped.error)
                is Fatal -> Fatal(dumped.errors)
            }
        }

        is Failed -> Failed(converted.error)
        is Fatal -> Fatal(converted.errors)
    }
}

/**
 * 使用泛型数值转换从二次机制模型返回解池报告。 / Return a generic numeric solution-pool report from a quadratic mechanism model.
 *
 * @param V 目标数值类型 / Target numeric type
 * @param model 二次机制模型 / Quadratic mechanism model
 * @param solutionAmount 期望解数量 / Desired number of solutions
 * @param converter 数值转换器 / Numeric converter
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @return 泛型数值解池报告 / Generic numeric solution-pool report
 */
suspend fun <V> AbstractQuadraticSolver.solveReport(
    model: MechanismModel<V>,
    solutionAmount: UInt64,
    converter: IntoValue<V>,
    solvingStatusCallBack: SolvingStatusCallBack? = null
): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
    return when (val converted = convertMechanismModelToFlt64(model)) {
        is Ok -> {
            val quadraticModel = converted.value as? QuadraticMechanismModel<Flt64>
                ?: return Failed(
                    Err(
                        ErrorCode.IllegalArgument,
                        "Quadratic solver requires QuadraticMechanismModel, but got ${converted.value::class.simpleName}"
                    )
                )
            when (val dumped = dumpResult(quadraticModel)) {
                is Ok -> dumped.value.use {
                    solveReport(it, solutionAmount, converter, solvingStatusCallBack)
                }
                is Failed -> Failed(dumped.error)
                is Fatal -> Fatal(dumped.errors)
            }
        }

        is Failed -> Failed(converted.error)
        is Fatal -> Fatal(converted.errors)
    }
}

/**
 * 使用旧 IIS 编排入口返回线性报告。 / Return a linear report through the legacy IIS orchestration entry point.
 *
 * @param model 线性三元模型 / Linear triad model
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @param iisConfig IIS 配置 / IIS configuration
 * @return 统一求解报告 / Unified solve report
 */
suspend fun AbstractLinearSolver.solveReport(
    model: LinearTriadModel,
    solvingStatusCallBack: SolvingStatusCallBack? = null,
    iisConfig: IISConfig
): Ret<SolveReport<Flt64>> {
    return when (val result = solveWithOptionsAndIIS(
        model = model,
        options = SolveOptions(solvingStatusCallBack = solvingStatusCallBack),
        iisConfig = iisConfig
    )) {
        is Ok -> Ok(result.value.toSolveReport().withModelDiagnostics(model))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 使用旧 IIS 编排入口返回线性解池报告。 / Return a linear solution-pool report through legacy IIS orchestration.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @param solutionAmount 期望解数量 / Desired number of solutions
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @param iisConfig IIS 配置 / IIS configuration
 * @return 统一解池报告 / Unified solution-pool report
 */
suspend fun AbstractLinearSolver.solveReport(
    model: LinearTriadModelView,
    solutionAmount: UInt64,
    solvingStatusCallBack: SolvingStatusCallBack? = null,
    iisConfig: IISConfig
): Ret<SolveReport<Flt64>> {
    return when (val result = solveWithOptionsAndIISForSolutionPool(
        model = model,
        options = SolveOptions(
            solutionAmount = solutionAmount,
            solvingStatusCallBack = solvingStatusCallBack
        ),
        iisConfig = iisConfig
    )) {
        is Ok -> Ok(result.value.first.toSolveReport(solutionPool = result.value.second).withModelDiagnostics(model))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 使用旧 IIS 编排入口返回二次报告。 / Return a quadratic report through the legacy IIS orchestration entry point.
 *
 * @param model 二次四元模型 / Quadratic tetrad model
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @param iisConfig IIS 配置 / IIS configuration
 * @return 统一求解报告 / Unified solve report
 */
suspend fun AbstractQuadraticSolver.solveReport(
    model: QuadraticTetradModel,
    solvingStatusCallBack: SolvingStatusCallBack? = null,
    iisConfig: IISConfig
): Ret<SolveReport<Flt64>> {
    return when (val result = solveWithOptionsAndIIS(
        model = model,
        options = SolveOptions(solvingStatusCallBack = solvingStatusCallBack),
        iisConfig = iisConfig
    )) {
        is Ok -> Ok(result.value.toSolveReport().withModelDiagnostics(model))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 使用旧 IIS 编排入口返回二次解池报告。 / Return a quadratic solution-pool report through legacy IIS orchestration.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param solutionAmount 期望解数量 / Desired number of solutions
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @param iisConfig IIS 配置 / IIS configuration
 * @return 统一解池报告 / Unified solution-pool report
 */
suspend fun AbstractQuadraticSolver.solveReport(
    model: QuadraticTetradModelView,
    solutionAmount: UInt64,
    solvingStatusCallBack: SolvingStatusCallBack? = null,
    iisConfig: IISConfig
): Ret<SolveReport<Flt64>> {
    return when (val result = solveWithOptionsAndIISForSolutionPool(
        model = model,
        options = SolveOptions(
            solutionAmount = solutionAmount,
            solvingStatusCallBack = solvingStatusCallBack
        ),
        iisConfig = iisConfig
    )) {
        is Ok -> Ok(result.value.first.toSolveReport(solutionPool = result.value.second).withModelDiagnostics(model))
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 使用选项返回线性报告；选项中的解池数量会进入报告的 `solution.pool`。 /
 * Return a linear report with options; a configured solution-pool amount is stored in `solution.pool`.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @param options 求解选项 / Solve options
 * @return 统一求解报告 / Unified solve report
 */
suspend fun AbstractLinearSolver.solveReport(
    model: LinearTriadModelView,
    options: SolveOptions
): Ret<SolveReport<Flt64>> {
    return if (options.solutionAmount == null) {
        reportLinearProgress(options.progressContext, name, 0)
        when (val result = solveWithOptions(model, options)) {
            is Ok -> {
                reportLinearProgress(options.progressContext, name, 100)
                Ok(result.value.toSolveReport().withModelDiagnostics(model))
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    } else {
        solveReportWithSolutionPool(
            model = model,
            solutionAmount = options.solutionAmount,
            solvingStatusCallBack = options.solvingStatusCallBack,
            progressContext = options.progressContext,
            valueConversionPolicy = options.effectiveValueConversionPolicy,
            cancellationToken = options.cancellationToken
        )
    }
}

/**
 * 使用选项返回二次报告；选项中的解池数量会进入报告的 `solution.pool`。 /
 * Return a quadratic report with options; a configured solution-pool amount is stored in `solution.pool`.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param options 求解选项 / Solve options
 * @return 统一求解报告 / Unified solve report
 */
suspend fun AbstractQuadraticSolver.solveReport(
    model: QuadraticTetradModelView,
    options: SolveOptions
): Ret<SolveReport<Flt64>> {
    return if (options.solutionAmount == null) {
        reportQuadraticProgress(options.progressContext, name, 0)
        when (val result = solveWithOptions(model, options)) {
            is Ok -> {
                reportQuadraticProgress(options.progressContext, name, 100)
                Ok(result.value.toSolveReport())
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    } else {
        solveReportWithSolutionPool(
            model = model,
            solutionAmount = options.solutionAmount,
            solvingStatusCallBack = options.solvingStatusCallBack,
            progressContext = options.progressContext,
            valueConversionPolicy = options.effectiveValueConversionPolicy,
            cancellationToken = options.cancellationToken
        )
    }
}

/**
 * 异步返回线性统一报告。 / Return a linear unified report asynchronously.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @param progressContext 统一进度上下文 / Unified progress context
 * @param callBack 结果回调 / Result callback
 * @return 异步报告结果 / Asynchronous report result
 */
fun AbstractLinearSolver.solveReportAsync(
    model: LinearTriadModelView,
    progressContext: SolverProgressContext? = null,
    callBack: ((Ret<SolveReport<Flt64>>) -> Unit)? = null,
    cancellationToken: CancellationToken? = null
): CompletableFuture<Ret<SolveReport<Flt64>>> {
    val token = cancellationToken ?: SolveHandle.create().token
    return cancellableSolveFuture(token) {
        val result = solveReport(model, progressContext, token)
        callBack?.invoke(result)
        result
    }
}

/**
 * 异步返回线性解池报告。 / Return a linear solution-pool report asynchronously.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @param solutionAmount 期望解数量 / Desired number of solutions
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @param progressContext 统一进度上下文 / Unified progress context
 * @param callBack 结果回调 / Result callback
 * @return 异步报告结果 / Asynchronous report result
 */
fun AbstractLinearSolver.solveReportAsync(
    model: LinearTriadModelView,
    solutionAmount: UInt64,
    solvingStatusCallBack: SolvingStatusCallBack? = null,
    progressContext: SolverProgressContext? = null,
    callBack: ((Ret<SolveReport<Flt64>>) -> Unit)? = null,
    cancellationToken: CancellationToken? = null
): CompletableFuture<Ret<SolveReport<Flt64>>> {
    val token = cancellationToken ?: SolveHandle.create().token
    return cancellableSolveFuture(token) {
        val result = solveReport(
            model = model,
            options = SolveOptions(
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack,
                progressContext = progressContext,
                cancellationToken = token
            )
        )
        callBack?.invoke(result)
        result
    }
}

/**
 * 异步返回二次统一报告。 / Return a quadratic unified report asynchronously.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param progressContext 统一进度上下文 / Unified progress context
 * @param callBack 结果回调 / Result callback
 * @return 异步报告结果 / Asynchronous report result
 */
fun AbstractQuadraticSolver.solveReportAsync(
    model: QuadraticTetradModelView,
    progressContext: SolverProgressContext? = null,
    callBack: ((Ret<SolveReport<Flt64>>) -> Unit)? = null,
    cancellationToken: CancellationToken? = null
): CompletableFuture<Ret<SolveReport<Flt64>>> {
    val token = cancellationToken ?: SolveHandle.create().token
    return cancellableSolveFuture(token) {
        val result = solveReport(model, progressContext, token)
        callBack?.invoke(result)
        result
    }
}

private fun reportLinearProgress(
    progressContext: SolverProgressContext?,
    solverName: String,
    progress: Int
) {
    progressContext?.report(
        SolverProgressSnapshot(
            stage = SolverStages.MILP,
            progressInStage = progress,
            overallProgress = progress,
            diagnostics = mapOf("solver" to solverName)
        )
    )
}

private fun reportQuadraticProgress(
    progressContext: SolverProgressContext?,
    solverName: String,
    progress: Int
) {
    progressContext?.report(
        SolverProgressSnapshot(
            stage = SolverStages.MILP,
            progressInStage = progress,
            overallProgress = progress,
            diagnostics = mapOf("solver" to solverName)
        )
    )
}

/**
 * 异步返回二次解池报告。 / Return a quadratic solution-pool report asynchronously.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param solutionAmount 期望解数量 / Desired number of solutions
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @param progressContext 统一进度上下文 / Unified progress context
 * @param callBack 结果回调 / Result callback
 * @return 异步报告结果 / Asynchronous report result
 */
fun AbstractQuadraticSolver.solveReportAsync(
    model: QuadraticTetradModelView,
    solutionAmount: UInt64,
    solvingStatusCallBack: SolvingStatusCallBack? = null,
    progressContext: SolverProgressContext? = null,
    callBack: ((Ret<SolveReport<Flt64>>) -> Unit)? = null,
    cancellationToken: CancellationToken? = null
): CompletableFuture<Ret<SolveReport<Flt64>>> {
    val token = cancellationToken ?: SolveHandle.create().token
    return cancellableSolveFuture(token) {
        val result = solveReport(
            model = model,
            options = SolveOptions(
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack,
                progressContext = progressContext,
                cancellationToken = token
            )
        )
        callBack?.invoke(result)
        result
    }
}
