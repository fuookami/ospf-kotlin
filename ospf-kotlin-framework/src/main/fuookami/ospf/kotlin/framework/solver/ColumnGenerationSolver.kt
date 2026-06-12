@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 列生成求解器
 * Column Generation Solver
 *
 * 定义列生成求解器接口及其 MILP/LP 求解、异步变体和值转换扩展。
 * Defines column generation solver interface with MILP/LP solving, async variants, and value conversion extensions.
 */
package fuookami.ospf.kotlin.framework.solver

import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlinx.coroutines.future.future
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.output.convertTo
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.castLinearMetaModelForSolver

/** Flt64 线性元模型 / Flt64 linear meta model */
typealias Flt64LinearMetaModel = LinearMetaModel<Flt64>
/** Flt64 可行求解器输出 / Flt64 feasible solver output */
typealias Flt64FeasibleSolverOutput = FeasibleSolverOutput<Flt64>
/** Flt64 解池 / Flt64 solution pool */
typealias Flt64SolutionPool = List<Solution<Flt64>>

/**
 * 列生成求解器接口
 * Column generation solver interface
 */
interface ColumnGenerationSolver {
    /** 求解器名称 / Solver name */
    val name: String

    /**
     * 求解 MILP 问题
     * Solve MILP problem
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
    ): Ret<Flt64FeasibleSolverOutput>

    /**
     * 使用选项求解 MILP 问题（便捷重载）
     * Solve MILP problem with options (convenience overload)
     *
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果 / Solve result
     */
    suspend fun solveMILP(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<Flt64FeasibleSolverOutput> {
        val solutionAmount = options.solutionAmount
        return if (solutionAmount != null) {
            solveMILP(
                name = options.solveName(metaModel.name),
                metaModel = metaModel,
                amount = solutionAmount,
                toLogModel = options.toLogModel,
                registrationStatusCallBack = options.registrationStatusCallBack,
                solvingStatusCallBack = options.solvingStatusCallBack
            ).map { it.first }
        } else {
            solveMILP(
                name = options.solveName(metaModel.name),
                metaModel = metaModel,
                toLogModel = options.toLogModel,
                registrationStatusCallBack = options.registrationStatusCallBack,
                solvingStatusCallBack = options.solvingStatusCallBack
            )
        }
    }

    /**
     * 异步求解 MILP 问题
     * Asynchronously solve MILP problem
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
    ): CompletableFuture<Ret<Flt64FeasibleSolverOutput>> {
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
     * 使用选项异步求解 MILP 问题
     * Asynchronously solve MILP problem with options
     *
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
     */
    fun solveMILPAsync(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<Flt64FeasibleSolverOutput>> {
        return frameworkAsyncScope.future {
            return@future this@ColumnGenerationSolver.solveMILP(
                metaModel = metaModel,
                options = options
            )
        }
    }

    /**
     * 求解 MILP 问题并返回指定数量的解
     * Solve MILP problem and return a specified number of solutions
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
    ): Ret<Pair<Flt64FeasibleSolverOutput, List<List<Flt64>>>> {
        return solveMILP(
            name = name,
            metaModel = metaModel,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
            .map { Pair(it, listOf(it.solution)) }
    }

    /**
     * 使用选项求解 MILP 问题并返回解池
     * Solve MILP problem with options and return solution pool
     *
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果与解池 / Solve result with solution pool
     */
    suspend fun solveMILPWithSolutionPool(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions
    ): Ret<Pair<Flt64FeasibleSolverOutput, List<List<Flt64>>>> {
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
     * 异步求解 MILP 问题并返回指定数量的解
     * Asynchronously solve MILP problem and return a specified number of solutions
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
    ): CompletableFuture<Ret<Pair<Flt64FeasibleSolverOutput, List<List<Flt64>>>>> {
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
     * 使用选项异步求解 MILP 问题并返回解池
     * Asynchronously solve MILP problem with options and return solution pool
     *
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果与解池的 CompletableFuture / CompletableFuture of solve result with solution pool
     */
    fun solveMILPWithSolutionPoolAsync(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions
    ): CompletableFuture<Ret<Pair<Flt64FeasibleSolverOutput, List<List<Flt64>>>>> {
        return frameworkAsyncScope.future {
            return@future this@ColumnGenerationSolver.solveMILPWithSolutionPool(
                metaModel = metaModel,
                options = options
            )
        }
    }

    /**
     * LP 求解结果
     * LP solve result
     *
     * @property result 可行求解器输出 / Feasible solver output
     * @property dualSolution 对偶解 / Dual solution
     */
    data class LPResult(
        val result: Flt64FeasibleSolverOutput,
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
    ) {
        val obj: Flt64 by result::obj
        val solution: List<Flt64> by result::solution
        val time: Duration by result::time
        val possibleBestObj by result::possibleBestObj
        val gap: Flt64 by result::gap
    }

    /**
     * 求解 LP 问题
     * Solve LP problem
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
     * 使用选项求解 LP 问题（便捷重载）
     * Solve LP problem with options (convenience overload)
     *
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果 / Solve result
     */
    suspend fun solveLP(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<LPResult> {
        return solveLP(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 异步求解 LP 问题
     * Asynchronously solve LP problem
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
     * 使用选项异步求解 LP 问题
     * Asynchronously solve LP problem with options
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
     * 带值转换求解 MILP 问题
     * Solve MILP problem with value conversion
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
    ): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
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
     * 带值转换求解 MILP 问题（使用模型自带转换器）
     * Solve MILP problem with value conversion (using model's built-in converter)
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
    ): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
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
     * 使用选项带值转换求解 MILP 问题
     * Solve MILP problem with value conversion and options
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
    ): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
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
     * 使用选项带值转换求解 MILP 问题（使用模型自带转换器）
     * Solve MILP problem with value conversion and options (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果 / Solve result
     */
    suspend fun <V> solveMILPAs(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveMILPAs(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 异步带值转换求解 MILP 问题
     * Asynchronously solve MILP problem with value conversion
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
    ): CompletableFuture<Ret<FeasibleSolverOutput<V>>> where V : RealNumber<V>, V : NumberField<V> {
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
     * 异步带值转换求解 MILP 问题（使用模型自带转换器）
     * Asynchronously solve MILP problem with value conversion (using model's built-in converter)
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
    ): CompletableFuture<Ret<FeasibleSolverOutput<V>>> where V : RealNumber<V>, V : NumberField<V> {
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
     * 使用选项异步带值转换求解 MILP 问题
     * Asynchronously solve MILP problem with value conversion and options
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
    ): CompletableFuture<Ret<FeasibleSolverOutput<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPAs(
                metaModel = metaModel,
                converter = converter,
                options = options
            )
        }
    }

    /**
     * 使用选项异步带值转换求解 MILP 问题（使用模型自带转换器）
     * Asynchronously solve MILP problem with value conversion and options (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
     */
    fun <V> solveMILPAsAsync(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<FeasibleSolverOutput<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPAs(
                metaModel = metaModel,
                options = options
            )
        }
    }

    /**
     * 带值转换求解 MILP 问题并返回指定数量的解
     * Solve MILP problem with value conversion and return a specified number of solutions
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
    ): Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>> where V : RealNumber<V>, V : NumberField<V> {
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
     * 带值转换求解 MILP 问题并返回指定数量的解（使用模型自带转换器）
     * Solve MILP problem with value conversion and return a specified number of solutions (using model's built-in converter)
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
    ): Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>> where V : RealNumber<V>, V : NumberField<V> {
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
     * 异步带值转换求解 MILP 问题并返回指定数量的解
     * Asynchronously solve MILP problem with value conversion and return a specified number of solutions
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
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>>> where V : RealNumber<V>, V : NumberField<V> {
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
     * 异步带值转换求解 MILP 问题并返回指定数量的解（使用模型自带转换器）
     * Asynchronously solve MILP problem with value conversion and return a specified number of solutions (using model's built-in converter)
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
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>>> where V : RealNumber<V>, V : NumberField<V> {
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
     * 使用选项带值转换求解 MILP 问题并返回解池
     * Solve MILP problem with value conversion, options, and return solution pool
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
    ): Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>> where V : RealNumber<V>, V : NumberField<V> {
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
     * 使用选项带值转换求解 MILP 问题并返回解池（使用模型自带转换器）
     * Solve MILP problem with value conversion, options, and return solution pool (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果与解池 / Solve result with solution pool
     */
    suspend fun <V> solveMILPWithSolutionPoolAs(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions
    ): Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        return solveMILPWithSolutionPoolAs(
            metaModel = castLinearMetaModelForSolver(metaModel),
            converter = metaModel.converter,
            options = options
        )
    }

    /**
     * 使用选项异步带值转换求解 MILP 问题并返回解池
     * Asynchronously solve MILP problem with value conversion, options, and return solution pool
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
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPWithSolutionPoolAs(
                metaModel = metaModel,
                converter = converter,
                options = options
            )
        }
    }

    /**
     * 使用选项异步带值转换求解 MILP 问题并返回解池（使用模型自带转换器）
     * Asynchronously solve MILP problem with value conversion, options, and return solution pool (using model's built-in converter)
     *
     * @param V 目标数值类型 / Target number type
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果与解池的 CompletableFuture / CompletableFuture of solve result with solution pool
     */
    fun <V> solveMILPWithSolutionPoolAsAsync(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPWithSolutionPoolAs(
                metaModel = metaModel,
                options = options
            )
        }
    }

    /**
     * 带值转换的 LP 求解结果
     * LP solve result with value conversion
     *
     * @property result 可行求解器输出 / Feasible solver output
     * @property dualSolution 对偶解 / Dual solution
     * @param V 目标数值类型 / Target number type
     */
    data class LPResultOf<V>(
        val result: FeasibleSolverOutput<V>,
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
    ) where V : RealNumber<V>, V : NumberField<V> {
        val obj: Flt64 by result::obj
        val solution: List<V> by result::solution
        val time: Duration by result::time
        val possibleBestObj by result::possibleBestObj
        val gap: Flt64 by result::gap
    }

    /**
     * 带值转换求解 LP 问题
     * Solve LP problem with value conversion
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
     * 带值转换求解 LP 问题（使用模型自带转换器）
     * Solve LP problem with value conversion (using model's built-in converter)
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
     * 使用选项带值转换求解 LP 问题
     * Solve LP problem with value conversion and options
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
     * 使用选项带值转换求解 LP 问题（使用模型自带转换器）
     * Solve LP problem with value conversion and options (using model's built-in converter)
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
     * 异步带值转换求解 LP 问题
     * Asynchronously solve LP problem with value conversion
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
     * 异步带值转换求解 LP 问题（使用模型自带转换器）
     * Asynchronously solve LP problem with value conversion (using model's built-in converter)
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
     * 使用选项异步带值转换求解 LP 问题
     * Asynchronously solve LP problem with value conversion and options
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
     * 使用选项异步带值转换求解 LP 问题（使用模型自带转换器）
     * Asynchronously solve LP problem with value conversion and options (using model's built-in converter)
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