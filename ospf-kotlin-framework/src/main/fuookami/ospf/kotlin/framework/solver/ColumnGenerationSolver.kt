@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 列生成求解器 / Column Generation Solver
 *
 * 定义列生成求解器接口及其 MILP/LP 求解、异步变体和值转换扩展。 / Defines column generation solver interface with MILP/LP solving, async variants, and value conversion extensions.
*/
package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.toSolverStatus
import java.util.concurrent.CompletableFuture
import fuookami.ospf.kotlin.core.solver.report.*
import kotlin.time.Duration
import fuookami.ospf.kotlin.core.solver.report.*
import kotlinx.coroutines.future.future
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.progress.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.symbol.castLinearMetaModelForSolver

/** Flt64 线性元模型 / Flt64 linear meta model */
typealias Flt64LinearMetaModel = LinearMetaModel<Flt64>

/** Flt64 可行求解器输出 / Flt64 feasible solver output */
typealias Flt64SolveReport = SolveReport<Flt64>

/** Flt64 解池 / Flt64 solution pool */
typealias Flt64SolutionPool = List<Solution<Flt64>>

/**
 * 列生成求解器接口 / Column generation solver interface
*/
interface ColumnGenerationSolver {

    /** 求解器名称 / Solver name */
    val name: String

    /**
     * 求解 MILP 问题 / Solve MILP problem
     *
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果 / Solve result
    */
    suspend fun solveMILP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Flt64SolveReport>

    /**
     * 求解 MILP 并保留不可行终态。 / Solve MILP while preserving the infeasible terminal state.
     *
     * 旧实现仍可只实现 [solveMILP]；默认实现将可行输出包装为结构化结果。
     * 具备类型化不可行输出能力的后端应覆盖此方法，避免把不可行误报为技术失败。
     */
    sealed interface MILPSolveResult {
        /**
         * 可行 MILP 结果 / Feasible MILP result.
         *
         * @property output 可行输出 / Feasible output
         */
        data class Feasible(val output: Flt64SolveReport) : MILPSolveResult

        /**
         * 不可行 MILP 结果 / Infeasible MILP result.
         *
         * @property output IIS 输出 / IIS output
         */
        data class Infeasible(val output: LinearInfeasibleSolverOutput) : MILPSolveResult
    }

    /**
     * 求解 MILP 并保留不可行终态。 / Solve MILP while preserving an infeasible terminal state.
     *
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param iisConfig 不可行子系统配置 / Infeasible subsystem configuration
     * @return 结构化 MILP 终态 / Structured MILP terminal result
     */
    suspend fun solveMILPWithStatus(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig = IISConfig()
    ): Ret<MILPSolveResult> {
        return when (val result = solveMILP(
            name = name,
            metaModel = metaModel,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )) {
            is Ok -> Ok(MILPSolveResult.Feasible(result.value))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 使用选项求解 MILP 问题（便捷重载） / Solve MILP problem with options (convenience overload)
     *
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果 / Solve result
    */
    suspend fun solveMILP(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<Flt64SolveReport> {
        val solutionAmount = options.solutionAmount
        val progress = options.progressContext
        progress?.report(
            SolverProgressSnapshot(
                stage = SolverStages.MILP,
                progressInStage = 0,
                overallProgress = 30,
                diagnostics = mapOf("model" to metaModel.name, "solver" to name)
            )
        )
        val result = if (solutionAmount != null) {
            solveMILP(
                name = options.solveName(metaModel.name),
                metaModel = metaModel,
                amount = solutionAmount,
                toLogModel = options.toLogModel,
                registrationStatusCallBack = progress?.registrationCallback(options.registrationStatusCallBack)
                    ?: options.registrationStatusCallBack,
                solvingStatusCallBack = progress?.solvingCallback(SolverStages.MILP, options.solvingStatusCallBack)
                    ?: options.solvingStatusCallBack
            ).map { it.first }
        } else {
            solveMILP(
                name = options.solveName(metaModel.name),
                metaModel = metaModel,
                toLogModel = options.toLogModel,
                registrationStatusCallBack = progress?.registrationCallback(options.registrationStatusCallBack)
                    ?: options.registrationStatusCallBack,
                solvingStatusCallBack = progress?.solvingCallback(SolverStages.MILP, options.solvingStatusCallBack)
                    ?: options.solvingStatusCallBack
            )
        }
        if (result is Ok) {
            progress?.report(
                SolverProgressSnapshot(
                    stage = SolverStages.MILP,
                    progressInStage = 100,
                    overallProgress = 100,
                    diagnostics = mapOf("model" to metaModel.name, "solver" to name)
                )
            )
        }
        return result
    }

    /**
     * 异步求解 MILP 问题 / Asynchronously solve MILP problem
     *
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
    */
    fun solveMILPAsync(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Flt64SolveReport>> {
        return frameworkAsyncScope.future {
            return@future this@ColumnGenerationSolver.solveMILP(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项异步求解 MILP 问题 / Asynchronously solve MILP problem with options
     *
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
    */
    fun solveMILPAsync(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<Flt64SolveReport>> {
        return frameworkAsyncScope.future {
            return@future this@ColumnGenerationSolver.solveMILP(
                metaModel = metaModel,
                options = options
            )
        }
    }

    /**
     * 求解 MILP 问题并返回指定数量的解 / Solve MILP problem and return a specified number of solutions
     *
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param amount 期望解数量 / Desired solution amount
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果与解池 / Solve result with solution pool
    */
    suspend fun solveMILP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        amount: UInt64,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<Flt64SolveReport, List<List<Flt64>>>> {
        return solveMILP(
            name = name,
            metaModel = metaModel,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
            .map { Pair(it, listOf(it.values)) }
    }

    /**
     * 使用选项求解 MILP 问题并返回解池 / Solve MILP problem with options and return solution pool
     *
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果与解池 / Solve result with solution pool
    */
    suspend fun solveMILPWithSolutionPool(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions
    ): Ret<Pair<Flt64SolveReport, List<List<Flt64>>>> {
        return solveMILP(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            amount = options.solutionAmount ?: UInt64.one,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 异步求解 MILP 问题并返回指定数量的解 / Asynchronously solve MILP problem and return a specified number of solutions
     *
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param amount 期望解数量 / Desired solution amount
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果与解池的 CompletableFuture / CompletableFuture of solve result with solution pool
    */
    fun solveMILPAsync(
        name: String,
        metaModel: Flt64LinearMetaModel,
        amount: UInt64,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<Flt64SolveReport, List<List<Flt64>>>>> {
        return frameworkAsyncScope.future {
            return@future this@ColumnGenerationSolver.solveMILP(
                name = name,
                metaModel = metaModel,
                amount = amount,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项异步求解 MILP 问题并返回解池 / Asynchronously solve MILP problem with options and return solution pool
     *
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果与解池的 CompletableFuture / CompletableFuture of solve result with solution pool
    */
    fun solveMILPWithSolutionPoolAsync(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions
    ): CompletableFuture<Ret<Pair<Flt64SolveReport, List<List<Flt64>>>>> {
        return frameworkAsyncScope.future {
            return@future this@ColumnGenerationSolver.solveMILPWithSolutionPool(
                metaModel = metaModel,
                options = options
            )
        }
    }

    /**
     * LP 求解结果 / LP solve result
     *
     * @property result 可行求解器输出 / Feasible solver output
     * @property dualSolution 对偶解 / Dual solution
     * @property status 求解终态，只有 Optimal 才能作为精确定价证书 / Solver termination status; only Optimal is a pricing certificate
    */
    data class LPResult(
        val result: Flt64SolveReport,
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
    ) {
        val obj: Flt64 get() = result.solution?.objective ?: Flt64.zero
        val solution: List<Flt64> get() = result.values
        val time: Duration get() = result.statistics.solveTime ?: Duration.ZERO
        val possibleBestObj: Flt64 get() = result.statistics.bestBound ?: Flt64.zero
        val gap: Flt64 get() = result.statistics.gap ?: Flt64.infinity
        val status: SolverStatus get() = result.toSolverStatus()
    }

    /**
     * LP 求解结果，保留不可行终态。 / LP solve result preserving the infeasible terminal state.
     */
    sealed interface LPResultWithStatus {
        /**
         * 可行 LP 结果 / Feasible LP result.
         *
         * @property result LP 结果 / LP result
         */
        data class Feasible(val result: LPResult) : LPResultWithStatus

        /**
         * 不可行 LP 结果 / Infeasible LP result.
         *
         * @property output IIS 输出 / IIS output
         */
        data class Infeasible(val output: LinearInfeasibleSolverOutput) : LPResultWithStatus
    }

    /**
     * 求解 LP 问题 / Solve LP problem
     *
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果 / Solve result
    */
    suspend fun solveLP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<LPResult>

    /**
     * 求解 LP 并保留不可行终态。 / Solve LP while preserving an infeasible terminal state.
     *
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param iisConfig 不可行子系统配置 / Infeasible subsystem configuration
     * @return 结构化 LP 终态 / Structured LP terminal result
     */
    suspend fun solveLPWithStatus(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig = IISConfig()
    ): Ret<LPResultWithStatus> {
        return when (val result = solveLP(
            name = name,
            metaModel = metaModel,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )) {
            is Ok -> Ok(LPResultWithStatus.Feasible(result.value))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 使用选项求解 LP 问题（便捷重载） / Solve LP problem with options (convenience overload)
     *
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果 / Solve result
    */
    suspend fun solveLP(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<LPResult> {
        val progress = options.progressContext
        progress?.report(
            SolverProgressSnapshot(
                stage = SolverStages.MasterLP,
                progressInStage = 0,
                overallProgress = 30,
                diagnostics = mapOf("model" to metaModel.name, "solver" to name)
            )
        )
        val result = solveLP(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = progress?.registrationCallback(options.registrationStatusCallBack)
                ?: options.registrationStatusCallBack,
            solvingStatusCallBack = progress?.solvingCallback(SolverStages.MasterLP, options.solvingStatusCallBack)
                ?: options.solvingStatusCallBack
        )
        if (result is Ok) {
            progress?.report(
                SolverProgressSnapshot(
                    stage = SolverStages.MasterLP,
                    progressInStage = 100,
                    overallProgress = 70,
                    diagnostics = mapOf("model" to metaModel.name, "solver" to name)
                )
            )
        }
        return result
    }

    /**
     * 异步求解 LP 问题 / Asynchronously solve LP problem
     *
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
    */
    fun solveLPAsync(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<LPResult>> {
        return frameworkAsyncScope.future {
            return@future this@ColumnGenerationSolver.solveLP(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项异步求解 LP 问题 / Asynchronously solve LP problem with options
     *
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
    */
    fun solveLPAsync(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<LPResult>> {
        return frameworkAsyncScope.future {
            return@future this@ColumnGenerationSolver.solveLP(
                metaModel = metaModel,
                options = options
            )
        }
    }

    /**
     * 带值转换求解 MILP 问题 / Solve MILP problem with value conversion
     *
     * @param V 目标数值类型 / Target number type
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param converter 值转换器 / Value converter
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果 / Solve result
    */
    suspend fun <V> solveMILPAs(
        name: String,
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = solveMILP(
            name = name,
            metaModel = metaModel,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )) {
            is Ok -> Ok(result.value.convertTo(converter))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 带值转换求解 MILP 问题（使用模型自带转换器） / Solve MILP problem with value conversion (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果 / Solve result
    */
    suspend fun <V> solveMILPAs(
        name: String,
        metaModel: LinearMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveMILPAs(
            name = name,
            metaModel = castLinearMetaModelForSolver(metaModel),
            converter = metaModel.converter,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    /**
     * 使用选项带值转换求解 MILP 问题 / Solve MILP problem with value conversion and options
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param converter 值转换器 / Value converter
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果 / Solve result
    */
    suspend fun <V> solveMILPAs(
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveMILPAs(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            converter = converter,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 使用选项带值转换求解 MILP 问题（使用模型自带转换器） / Solve MILP problem with value conversion and options (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果 / Solve result
    */
    suspend fun <V> solveMILPAs(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveMILPAs(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 异步带值转换求解 MILP 问题 / Asynchronously solve MILP problem with value conversion
     *
     * @param V 目标数值类型 / Target number type
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param converter 值转换器 / Value converter
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
    */
    fun <V> solveMILPAsAsync(
        name: String,
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolveReport<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPAs(
                name = name,
                metaModel = metaModel,
                converter = converter,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 异步带值转换求解 MILP 问题（使用模型自带转换器） / Asynchronously solve MILP problem with value conversion (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
    */
    fun <V> solveMILPAsAsync(
        name: String,
        metaModel: LinearMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolveReport<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPAs(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项异步带值转换求解 MILP 问题 / Asynchronously solve MILP problem with value conversion and options
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param converter 值转换器 / Value converter
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
    */
    fun <V> solveMILPAsAsync(
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<SolveReport<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPAs(
                metaModel = metaModel,
                converter = converter,
                options = options
            )
        }
    }

    /**
     * 使用选项异步带值转换求解 MILP 问题（使用模型自带转换器） / Asynchronously solve MILP problem with value conversion and options (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
    */
    fun <V> solveMILPAsAsync(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<SolveReport<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPAs(
                metaModel = metaModel,
                options = options
            )
        }
    }

    /**
     * 带值转换求解 MILP 问题并返回指定数量的解 / Solve MILP problem with value conversion and return a specified number of solutions
     *
     * @param V 目标数值类型 / Target number type
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param amount 期望解数量 / Desired solution amount
     * @param converter 值转换器 / Value converter
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果与解池 / Solve result with solution pool
    */
    suspend fun <V> solveMILPAs(
        name: String,
        metaModel: Flt64LinearMetaModel,
        amount: UInt64,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<SolveReport<V>, List<List<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = solveMILP(
            name = name,
            metaModel = metaModel,
            amount = amount,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )) {
            is Ok -> {
                val (output, pool) = result.value
                Ok(Pair(output.convertTo(converter), pool.map { it.map(converter::intoValue) }))
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 带值转换求解 MILP 问题并返回指定数量的解（使用模型自带转换器） / Solve MILP problem with value conversion and return a specified number of solutions (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param amount 期望解数量 / Desired solution amount
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果与解池 / Solve result with solution pool
    */
    suspend fun <V> solveMILPAs(
        name: String,
        metaModel: LinearMetaModel<V>,
        amount: UInt64,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<SolveReport<V>, List<List<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        return solveMILPAs(
            name = name,
            metaModel = castLinearMetaModelForSolver(metaModel),
            amount = amount,
            converter = metaModel.converter,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    /**
     * 异步带值转换求解 MILP 问题并返回指定数量的解 / Asynchronously solve MILP problem with value conversion and return a specified number of solutions
     *
     * @param V 目标数值类型 / Target number type
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param amount 期望解数量 / Desired solution amount
     * @param converter 值转换器 / Value converter
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果与解池的 CompletableFuture / CompletableFuture of solve result with solution pool
    */
    fun <V> solveMILPAsAsync(
        name: String,
        metaModel: Flt64LinearMetaModel,
        amount: UInt64,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<SolveReport<V>, List<List<V>>>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPAs(
                name = name,
                metaModel = metaModel,
                amount = amount,
                converter = converter,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 异步带值转换求解 MILP 问题并返回指定数量的解（使用模型自带转换器） / Asynchronously solve MILP problem with value conversion and return a specified number of solutions (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param amount 期望解数量 / Desired solution amount
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果与解池的 CompletableFuture / CompletableFuture of solve result with solution pool
    */
    fun <V> solveMILPAsAsync(
        name: String,
        metaModel: LinearMetaModel<V>,
        amount: UInt64,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<SolveReport<V>, List<List<V>>>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPAs(
                name = name,
                metaModel = metaModel,
                amount = amount,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项带值转换求解 MILP 问题并返回解池 / Solve MILP problem with value conversion, options, and return solution pool
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param converter 值转换器 / Value converter
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果与解池 / Solve result with solution pool
    */
    suspend fun <V> solveMILPWithSolutionPoolAs(
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions
    ): Ret<Pair<SolveReport<V>, List<List<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        return solveMILPAs(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            amount = options.solutionAmount ?: UInt64.one,
            converter = converter,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 使用选项带值转换求解 MILP 问题并返回解池（使用模型自带转换器） / Solve MILP problem with value conversion, options, and return solution pool (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果与解池 / Solve result with solution pool
    */
    suspend fun <V> solveMILPWithSolutionPoolAs(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions
    ): Ret<Pair<SolveReport<V>, List<List<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        return solveMILPWithSolutionPoolAs(
            metaModel = castLinearMetaModelForSolver(metaModel),
            converter = metaModel.converter,
            options = options
        )
    }

    /**
     * 使用选项异步带值转换求解 MILP 问题并返回解池 / Asynchronously solve MILP problem with value conversion, options, and return solution pool
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param converter 值转换器 / Value converter
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果与解池的 CompletableFuture / CompletableFuture of solve result with solution pool
    */
    fun <V> solveMILPWithSolutionPoolAsAsync(
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions
    ): CompletableFuture<Ret<Pair<SolveReport<V>, List<List<V>>>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPWithSolutionPoolAs(
                metaModel = metaModel,
                converter = converter,
                options = options
            )
        }
    }

    /**
     * 使用选项异步带值转换求解 MILP 问题并返回解池（使用模型自带转换器） / Asynchronously solve MILP problem with value conversion, options, and return solution pool (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果与解池的 CompletableFuture / CompletableFuture of solve result with solution pool
    */
    fun <V> solveMILPWithSolutionPoolAsAsync(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions
    ): CompletableFuture<Ret<Pair<SolveReport<V>, List<List<V>>>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPWithSolutionPoolAs(
                metaModel = metaModel,
                options = options
            )
        }
    }

    /**
     * 带值转换的 LP 求解结果 / LP solve result with value conversion
     *
     * @property result 可行求解器输出 / Feasible solver output
     * @property dualSolution 对偶解 / Dual solution
     * @property status 求解终态，只有 Optimal 才能作为精确定价证书 / Solver termination status; only Optimal is a pricing certificate
     * @param V 目标数值类型 / Target number type
    */
    data class LPResultOf<V>(
        val result: SolveReport<V>,
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
    ) where V : RealNumber<V>, V : NumberField<V> {
        val obj: Flt64 get() = (result.solution?.objective as? Flt64) ?: Flt64.zero
        val solution: List<V> get() = result.values
        val time: Duration get() = result.statistics.solveTime ?: Duration.ZERO
        val possibleBestObj: Flt64 get() = result.statistics.bestBound ?: Flt64.zero
        val gap: Flt64 get() = result.statistics.gap ?: Flt64.infinity
        val status: SolverStatus get() = result.toSolverStatus()
    }

    /**
     * 带值转换求解 LP 问题 / Solve LP problem with value conversion
     *
     * @param V 目标数值类型 / Target number type
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param converter 值转换器 / Value converter
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果 / Solve result
    */
    suspend fun <V> solveLPAs(
        name: String,
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<LPResultOf<V>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = solveLP(
            name = name,
            metaModel = metaModel,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )) {
            is Ok -> Ok(LPResultOf(result.value.result.convertTo(converter), result.value.dualSolution))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 带值转换求解 LP 问题（使用模型自带转换器） / Solve LP problem with value conversion (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果 / Solve result
    */
    suspend fun <V> solveLPAs(
        name: String,
        metaModel: LinearMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<LPResultOf<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveLPAs(
            name = name,
            metaModel = castLinearMetaModelForSolver(metaModel),
            converter = metaModel.converter,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    /**
     * 使用选项带值转换求解 LP 问题 / Solve LP problem with value conversion and options
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param converter 值转换器 / Value converter
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果 / Solve result
    */
    suspend fun <V> solveLPAs(
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<LPResultOf<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveLPAs(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            converter = converter,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 使用选项带值转换求解 LP 问题（使用模型自带转换器） / Solve LP problem with value conversion and options (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果 / Solve result
    */
    suspend fun <V> solveLPAs(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<LPResultOf<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveLPAs(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 异步带值转换求解 LP 问题 / Asynchronously solve LP problem with value conversion
     *
     * @param V 目标数值类型 / Target number type
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param converter 值转换器 / Value converter
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
    */
    fun <V> solveLPAsAsync(
        name: String,
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<LPResultOf<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveLPAs(
                name = name,
                metaModel = metaModel,
                converter = converter,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 异步带值转换求解 LP 问题（使用模型自带转换器） / Asynchronously solve LP problem with value conversion (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param name 求解名称 / Solve name
     * @param metaModel 线性元模型 / Linear meta model
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
    */
    fun <V> solveLPAsAsync(
        name: String,
        metaModel: LinearMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<LPResultOf<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveLPAs(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项异步带值转换求解 LP 问题 / Asynchronously solve LP problem with value conversion and options
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param converter 值转换器 / Value converter
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
    */
    fun <V> solveLPAsAsync(
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<LPResultOf<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveLPAs(
                metaModel = metaModel,
                converter = converter,
                options = options
            )
        }
    }

    /**
     * 使用选项异步带值转换求解 LP 问题（使用模型自带转换器） / Asynchronously solve LP problem with value conversion and options (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
    */
    fun <V> solveLPAsAsync(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<LPResultOf<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveLPAs(
                metaModel = metaModel,
                options = options
            )
        }
    }
}
