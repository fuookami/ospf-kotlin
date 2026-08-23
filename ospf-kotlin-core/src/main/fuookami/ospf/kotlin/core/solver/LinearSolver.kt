/**
 * 线性求解器接口定义 / Linear solver interface definitions
*/
package fuookami.ospf.kotlin.core.solver

import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.future.future
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.iis.InfeasibilityAnalyzer
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.progress.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.*

/**
 * 线性求解器的抽象接口，定义了求解、异步求解和泛型求解等核心能力。 / Abstract interface for linear solvers, defining core capabilities for solving, async solving, and generic solving.
*/
interface AbstractLinearSolver {
    val name: String

    /** 运行时求解器描述符；未覆盖时准确声明为无已知能力 / Runtime descriptor; defaults to no declared capabilities */
    val descriptor: SolverDescriptor
        get() = SolverDescriptor(
            solverId = name,
            backendName = name,
            capabilities = SolverCapabilities(modelTypes = emptySet())
        )

    /**
     * 返回 backend 提供的结构化不可行诊断分析器。 / Return backend-provided structured infeasibility analyzers.
     *
     * 默认返回空列表，使未迁移的旧 solver 继续使用 core 的 legacy IIS 路径。 / The default is empty so
     * legacy solvers continue to use the core fallback path until they opt into the diagnostic SPI.
     */
    fun diagnosticAnalyzers(
        config: IISConfig
    ): List<InfeasibilityAnalyzer<LinearTriadModelView>> = emptyList()

    /**
     * 使用统一报告契约求解线性模型。 / Solve a linear model using the unified report contract.
     *
     * 旧求解器实现可通过默认实现直接迁移；backend 应逐步覆盖本方法以无损保留限制、取消和不可行终态。 / Legacy implementations can migrate through this default method; backends should override it to
     * preserve limit, cancellation, and infeasibility terminal states without loss.
     *
     * @param model 线性三元模型视图 / Linear triad model view
     * @param progressContext 进度上报上下文 / Progress reporting context
     * @return 统一求解报告 / Unified solve report
     */
    suspend fun solveReport(
        model: LinearTriadModelView,
        progressContext: SolverProgressContext? = null
    ): Ret<SolveReport<Flt64>> {
        when (val validation = model.identityValidation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        progressContext?.report(
            SolverProgressSnapshot(
                stage = SolverStages.MILP,
                progressInStage = 0,
                overallProgress = 0,
                diagnostics = mapOf("solver" to name)
            )
        )
        return when (val result = invoke(model, null)) {
            is Ok -> {
                progressContext?.report(
                    SolverProgressSnapshot(
                        stage = SolverStages.MILP,
                        progressInStage = 100,
                        overallProgress = 100,
                        diagnostics = mapOf("solver" to name)
                    )
                )
                Ok(result.value.toSolveReport())
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 使用取消令牌求解线性模型。 / Solve a linear model with a cancellation token.
     *
     * @param model 线性三元模型视图 / Linear triad model view
     * @param progressContext 进度上报上下文 / Progress reporting context
     * @param cancellationToken 求解取消令牌 / Solve cancellation token
     * @return 统一求解报告 / Unified solve report
     */
    suspend fun solveReport(
        model: LinearTriadModelView,
        progressContext: SolverProgressContext?,
        cancellationToken: CancellationToken?
    ): Ret<SolveReport<Flt64>> {
        if (cancellationToken == null) {
            return solveReport(model, progressContext)
        }
        if (cancellationToken?.isCancellationRequested == true) {
            return Ok(cancelledSolveReport(cancellationToken.record?.reason))
        }
        when (val validation = model.identityValidation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        progressContext?.report(
            SolverProgressSnapshot(
                stage = SolverStages.MILP,
                progressInStage = 0,
                overallProgress = 0,
                diagnostics = mapOf("solver" to name)
            )
        )
        return when (val result = invoke(model, null, cancellationToken)) {
            is Ok -> {
                progressContext?.report(
                    SolverProgressSnapshot(
                        stage = SolverStages.MILP,
                        progressInStage = 100,
                        overallProgress = 100,
                        diagnostics = mapOf("solver" to name)
                    )
                )
                Ok(result.value.toSolveReport())
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 求解线性模型（阻塞）。 / Solve linear model (blocking).
     *
     * @param model 线性三元模型视图 / Linear triad model view
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @return 求解结果 / Solve result
    */
    suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolveReport<Flt64>>

    /** 使用取消令牌求解线性模型。 / Solve a linear model with a cancellation token. */
    suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        cancellationToken: CancellationToken?
    ): Ret<SolveReport<Flt64>> {
        return invoke(model, solvingStatusCallBack)
    }

    /**
     * 求解线性模型并启用 IIS 诊断（阻塞）。 / Solve linear model with IIS diagnostics (blocking).
     *
     * @param model 线性三元模型 / Linear triad model
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @param iisConfig IIS 配置 / IIS configuration
     * @return 求解结果（可能包含 IIS）/ Solve result (may contain IIS)
    */
    suspend operator fun invoke(
        model: LinearTriadModel,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<SolverOutput> {
        return solveWithOptionsAndIIS(
            model = model,
            options = SolveOptions(
                solvingStatusCallBack = solvingStatusCallBack
            ),
            iisConfig = iisConfig
        )
    }

    /**
     * 异步求解线性模型。 / Solve linear model asynchronously.
     *
     * @param model 线性三元模型视图 / Linear triad model view
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @param callBack 结果回调（可选）/ Result callback (optional)
     * @return 异步求解结果 / Async solve result
    */
    fun solveAsync(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<SolveReport<Flt64>>) -> Unit)? = null,
        cancellationToken: CancellationToken? = null
    ): CompletableFuture<Ret<SolveReport<Flt64>>> {
        val token = cancellationToken ?: SolveHandle.create().token
        return cancellableSolveFuture(token) {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                solvingStatusCallBack = solvingStatusCallBack,
                cancellationToken = token
            )
            callBack?.invoke(result)
            result
        }
    }

    /**
     * 异步求解线性模型并启用 IIS 诊断。 / Solve linear model asynchronously with IIS diagnostics.
     *
     * @param model 线性三元模型 / Linear triad model
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @param iisConfig IIS 配置 / IIS configuration
     * @param callBack 结果回调（可选）/ Result callback (optional)
     * @return 异步求解结果 / Async solve result
    */
    fun solveAsync(
        model: LinearTriadModel,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<SolverOutput>) -> Unit)? = null,
        cancellationToken: CancellationToken? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        val token = cancellationToken ?: SolveHandle.create().token
        return cancellableSolveFuture(token) {
            val result = solveWithOptionsAndIIS(
                model = model,
                options = SolveOptions(
                    solvingStatusCallBack = solvingStatusCallBack,
                    cancellationToken = token
                ),
                iisConfig = iisConfig
            )
            callBack?.invoke(result)
            result
        }
    }

    /**
     * 求解线性模型获取多个解（阻塞）。 / Solve linear model for multiple solutions (blocking).
     *
     * @param model 线性三元模型视图 / Linear triad model view
     * @param solutionAmount 期望解数量 / Desired solution amount
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @return 求解结果与解列表 / Solve result with solution list
    */
    suspend operator fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>>

    /** 使用取消令牌获取多个线性解。 / Solve for multiple linear solutions with a cancellation token. */
    suspend operator fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        cancellationToken: CancellationToken?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        return invoke(model, solutionAmount, solvingStatusCallBack)
    }

    /**
     * 求解线性模型获取多个解并启用 IIS 诊断（阻塞）。 / Solve linear model for multiple solutions with IIS diagnostics (blocking).
     *
     * @param model 线性三元模型视图 / Linear triad model view
     * @param solutionAmount 期望解数量 / Desired solution amount
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @param iisConfig IIS 配置 / IIS configuration
     * @return 求解结果与解列表（可能包含 IIS）/ Solve result with solution list (may contain IIS)
    */
    suspend operator fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<Pair<SolverOutput, List<List<Flt64>>>> {
        return solveWithOptionsAndIISForSolutionPool(
            model = model,
            options = SolveOptions(
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            ),
            iisConfig = iisConfig
        )
    }

    /**
     * 异步求解线性模型获取多个解。 / Solve linear model asynchronously for multiple solutions.
     *
     * @param model 线性三元模型视图 / Linear triad model view
     * @param solutionAmount 期望解数量 / Desired solution amount
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @param callBack 结果回调（可选）/ Result callback (optional)
     * @return 异步求解结果与解列表 / Async solve result with solution list
    */
    fun solveAsync(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>>) -> Unit)? = null,
        cancellationToken: CancellationToken? = null
    ): CompletableFuture<Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>>> {
        val token = cancellationToken ?: SolveHandle.create().token
        return cancellableSolveFuture(token) {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack,
                cancellationToken = token
            )
            callBack?.invoke(result)
            result
        }
    }

    /**
     * 异步求解线性模型获取多个解并启用 IIS 诊断。 / Solve linear model asynchronously for multiple solutions with IIS diagnostics.
     *
     * @param model 线性三元模型视图 / Linear triad model view
     * @param solutionAmount 期望解数量 / Desired solution amount
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @param iisConfig IIS 配置 / IIS configuration
     * @param callBack 结果回调（可选）/ Result callback (optional)
     * @return 异步求解结果与解列表 / Async solve result with solution list
    */
    fun solveAsync(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<Pair<SolverOutput, List<List<Flt64>>>>) -> Unit)? = null,
        cancellationToken: CancellationToken? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<List<Flt64>>>>> {
        val token = cancellationToken ?: SolveHandle.create().token
        return cancellableSolveFuture(token) {
            val result = solveWithOptionsAndIISForSolutionPool(
                model = model,
                options = SolveOptions(
                    solutionAmount = solutionAmount,
                    solvingStatusCallBack = solvingStatusCallBack,
                    cancellationToken = token
                ),
                iisConfig = iisConfig
            )
            callBack?.invoke(result)
            result
        }
    }

    // ========== 泛型主接口 / Generic primary interface ==========
    // solve 是泛型主入口；triad solve 调用仍是求解器边界。 / solve is the primary generic entry point; triad solve calls remain the solver boundary.

    /**
     * 泛型求解线性模型。 / Solve linear model with generic value conversion.
     *
     * @param V 值类型 / Value type
     * @param model 线性三元模型视图 / Linear triad model view
     * @param converter 值转换器 / Value converter
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @return 求解结果 / Solve result
    */
    suspend fun <V> solve(
        model: LinearTriadModelView,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = invoke(model, solvingStatusCallBack)) {
            is Ok -> Ok(result.value.convertTo(converter))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 泛型求解线性模型获取多个解。 / Solve linear model with generic value conversion for multiple solutions.
     *
     * @param V 值类型 / Value type
     * @param model 线性三元模型视图 / Linear triad model view
     * @param solutionAmount 期望解数量 / Desired solution amount
     * @param converter 值转换器 / Value converter
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @return 求解结果与解列表 / Solve result with solution list
    */
    suspend fun <V> solve(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<SolveReport<V>, List<Solution<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = invoke(model, solutionAmount, solvingStatusCallBack)) {
            is Ok -> {
                val (output, solutions) = result.value
                Ok(Pair(output.convertTo(converter), solutions.map { it.map { v -> converter.intoValue(v) } }))
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    // MechanismModel<V> 的泛型 solve 全链路：dump -> solve -> convert。 / Generic solve for MechanismModel<V>: full pipeline (dump -> solve -> convert)
    /**
     * 从机制模型求解线性问题（全链路：转储 -> 求解 -> 转换）。 / Solve linear problem from mechanism model (full pipeline: dump -> solve -> convert).
     *
     * @param V 值类型 / Value type
     * @param model 机制模型 / Mechanism model
     * @param converter 值转换器 / Value converter
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @return 求解结果 / Solve result
    */
    suspend fun <V> solve(
        model: MechanismModel<V>,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val converted = convertMechanismModelToFlt64(model)) {
            is Ok -> {
                val linearModel = converted.value as? LinearMechanismModel<Flt64>
                    ?: return Failed(Err(ErrorCode.IllegalArgument, "Linear solver requires LinearMechanismModel, but got ${converted.value::class.simpleName}"))
                when (val dumped = dumpResult(linearModel)) {
                    is Ok -> dumped.value.use { solve(it, converter, solvingStatusCallBack) }
                    is Failed -> Failed(dumped.error)
                    is Fatal -> Fatal(dumped.errors)
                }
            }

            is Failed -> {
                Failed(converted.error)
            }

            is Fatal -> {
                Fatal(converted.errors)
            }
        }
    }

    /**
     * 从机制模型求解线性问题获取多个解（全链路：转储 -> 求解 -> 转换）。 / Solve linear problem from mechanism model for multiple solutions (full pipeline: dump -> solve -> convert).
     *
     * @param V 值类型 / Value type
     * @param model 机制模型 / Mechanism model
     * @param solutionAmount 期望解数量 / Desired solution amount
     * @param converter 值转换器 / Value converter
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @return 求解结果与解列表 / Solve result with solution list
    */
    suspend fun <V> solve(
        model: MechanismModel<V>,
        solutionAmount: UInt64,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<SolveReport<V>, List<Solution<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val converted = convertMechanismModelToFlt64(model)) {
            is Ok -> {
                val linearModel = converted.value as? LinearMechanismModel<Flt64>
                    ?: return Failed(Err(ErrorCode.IllegalArgument, "Linear solver requires LinearMechanismModel, but got ${converted.value::class.simpleName}"))
                when (val dumped = dumpResult(linearModel)) {
                    is Ok -> dumped.value.use { solve(it, solutionAmount, converter, solvingStatusCallBack) }
                    is Failed -> Failed(dumped.error)
                    is Fatal -> Fatal(dumped.errors)
                }
            }

            is Failed -> {
                Failed(converted.error)
            }

            is Fatal -> {
                Fatal(converted.errors)
            }
        }
    }

    /**
     * 转储线性机制模型为三元组模型。 / Dump linear mechanism model to triad model.
     *
     * @param model 线性机制模型 / Linear mechanism model
     * @return 线性三元组模型 / Linear triad model
    */
    suspend fun dump(model: LinearMechanismModel<Flt64>): LinearTriadModel {
        return LinearTriadModel(model)
    }

    /**
     * 转储并返回身份校验结果，供生产求解链路使用。 / Dump a model and propagate identity validation for production solve pipelines.
     *
     * @param model 线性机制模型 / Linear mechanism model
     * @return 已校验的线性三元模型或结构化错误 / Validated linear triad model or a structured error
     */
    suspend fun dumpResult(model: LinearMechanismModel<Flt64>): Ret<LinearTriadModel> {
        val dumped = dump(model)
        return when (val validation = dumped.identityValidation) {
            is Ok -> Ok(dumped)
            is Failed -> Failed(validation.error)
            is Fatal -> Fatal(validation.errors)
        }
    }

    /**
     * 转储线性元模型为机制模型。 / Dump linear meta model to mechanism model.
     *
     * @param model 线性元模型 / Linear meta model
     * @param registrationStatusCallBack 注册状态回调（可选）/ Registration status callback (optional)
     * @param dumpingStatusCallBack 转储状态回调（可选）/ Dumping status callback (optional)
     * @return 线性机制模型 / Linear mechanism model
    */
    suspend fun dump(
        model: LinearMetaModel<Flt64>,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<LinearMechanismModel<Flt64>> {
        return LinearMechanismModel.invoke<Flt64>(
            metaModel = model,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }
}

/**
 * 线性求解器接口，扩展 [AbstractLinearSolver] 并提供配置驱动的模型转储能力。 / Linear solver interface extending [AbstractLinearSolver] with configuration-driven model dumping.
 *
 * @property config 求解器配置 / Solver configuration
*/
interface LinearSolver : AbstractLinearSolver {

    /** Solver configuration / 求解器配置 */
    val config: SolverConfig

    override suspend fun dump(model: LinearMechanismModel<Flt64>): LinearTriadModel {
        return LinearTriadModel(
            model = model,
            fixedVariables = null,
            dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
            forceDumpBounds = config.dumpIntermediateModelForceBounds,
            concurrent = config.dumpIntermediateModelConcurrent,
            identityRegistry = model.identityRegistry
        )
    }

    override suspend fun dump(
        model: LinearMetaModel<Flt64>,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<LinearMechanismModel<Flt64>> {
        return LinearMechanismModel.invoke<Flt64>(
            metaModel = model,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpIntermediateModelConcurrent,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }
}
