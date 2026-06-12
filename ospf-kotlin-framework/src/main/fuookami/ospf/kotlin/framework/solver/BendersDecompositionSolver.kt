@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * Benders 分解求解器
 * Benders Decomposition Solver
 *
 * 定义线性和二次 Benders 分解求解器接口，支持主问题和子问题求解及值转换扩展。
 * Defines linear and quadratic Benders decomposition solver interfaces with master/sub problem solving
 * and value conversion extensions.
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
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.output.convertTo
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 将线性不等式转换为目标数值类型
 * Convert linear inequality to target number type
 *
 * @param converter 值转换器 / Value converter
 * @param V 目标数值类型 / Target number type
 * @return 转换后的线性不等式 / Linear inequality in target number type
 */
private fun <V> LinearInequality<Flt64>.convertTo(converter: IntoValue<V>): LinearInequality<V>
        where V : RealNumber<V>, V : NumberField<V> {
    val lhs = LinearPolynomial(
        monomials = this.lhs.monomials.map { LinearMonomial(converter.intoValue(it.coefficient), it.symbol) },
        constant = converter.intoValue(this.lhs.constant)
    )
    val rhs = LinearPolynomial(
        monomials = this.rhs.monomials.map { LinearMonomial(converter.intoValue(it.coefficient), it.symbol) },
        constant = converter.intoValue(this.rhs.constant)
    )
    return LinearInequality(lhs, rhs, comparison)
}

/**
 * 将二次不等式转换为目标数值类型
 * Convert quadratic inequality to target number type
 *
 * @param converter 值转换器 / Value converter
 * @param V 目标数值类型 / Target number type
 * @return 转换后的二次不等式 / Quadratic inequality in target number type
 */
private fun <V> QuadraticInequalityOf<Flt64>.convertTo(converter: IntoValue<V>): QuadraticInequalityOf<V>
        where V : RealNumber<V>, V : NumberField<V> {
    val lhs = QuadraticPolynomial(
        monomials = this.lhs.monomials.map { QuadraticMonomial(converter.intoValue(it.coefficient), it.symbol1, it.symbol2) },
        constant = converter.intoValue(this.lhs.constant)
    )
    val rhs = QuadraticPolynomial(
        monomials = this.rhs.monomials.map { QuadraticMonomial(converter.intoValue(it.coefficient), it.symbol1, it.symbol2) },
        constant = converter.intoValue(this.rhs.constant)
    )
    return QuadraticInequalityOf(lhs, rhs, comparison)
}

/**
 * 将求解器输出转换为目标数值类型
 * Convert solver output to target number type
 *
 * @param converter 值转换器 / Value converter
 * @param V 目标数值类型 / Target number type
 * @return 转换后的求解器输出 / Solver output in target number type
 */
@Suppress("DEPRECATION")
private fun <V> SolverOutput.convertTo(converter: IntoValue<V>): SolverOutput
        where V : RealNumber<V>, V : NumberField<V> {
    return when (this) {
        is FeasibleSolverOutput<*> -> {
            val targetValues = solution.map { value ->
                if (value is Flt64) {
                    converter.intoValue(value)
                } else {
                    return this
                }
            }
            FeasibleSolverOutput(
                obj = obj,
                solution = targetValues,
                time = time,
                possibleBestObj = possibleBestObj,
                gap = gap,
                iterations = iterations,
                nodeCount = nodeCount,
                bestBound = bestBound,
                mipGap = mipGap,
                solveTime = solveTime,
                objValue = converter.intoValue(obj),
                possibleBestObjValue = converter.intoValue(possibleBestObj),
                bestBoundValue = bestBound?.let { converter.intoValue(it) }
            )
        }

        else -> this
    }
}

/**
 * 线性 Benders 分解求解器接口
 * Linear Benders decomposition solver interface
 */
interface LinearBendersDecompositionSolver {
    /** 求解器名称 / Solver name */
    val name: String

    /**
     * 求解线性 Benders 主问题 / Solve linear Benders master problem
     *
     * @param name 模型名称 / model name
     * @param metaModel 线性元模型 / linear meta model
     * @param toLogModel 是否导出模型文件 / whether to export model file
     * @param registrationStatusCallBack 注册状态回调 / registration status callback
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return 求解结果 / solving result
     */
    suspend fun solveMaster(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput>

    /**
     * 使用选项求解线性 Benders 主问题（便捷重载）
     * Solve linear Benders master problem with options (convenience overload)
     *
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果 / Solve result
     */
    suspend fun solveMaster(
        metaModel: LinearMetaModel<Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<SolverOutput> {
        return solveMaster(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 异步求解线性 Benders 主问题
     * Asynchronously solve linear Benders master problem
     *
     * @param name 模型名称 / Model name
     * @param metaModel 线性元模型 / Linear meta model
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
     */
    fun solveMasterAsync(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return frameworkAsyncScope.future {
            return@future this@LinearBendersDecompositionSolver.solveMaster(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项异步求解线性 Benders 主问题
     * Asynchronously solve linear Benders master problem with options
     *
     * @param metaModel 线性元模型 / Linear meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
     */
    fun solveMasterAsync(
        metaModel: LinearMetaModel<Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<SolverOutput>> {
        return frameworkAsyncScope.future {
            return@future this@LinearBendersDecompositionSolver.solveMaster(
                metaModel = metaModel,
                options = options
            )
        }
    }

    /**
     * 线性子问题求解结果密封接口
     * Sealed interface for linear sub problem solving result
     *
     * 若提供了固定变量，则结果中包含割平面列表。
     * If fixed variables are provided, the result contains cut list.
     *
     * @property cuts 割平面列表 / List of cuts generated by sub problem
     */
    sealed interface LinearSubResult {
        val cuts: List<LinearInequality<Flt64>>?
    }

    /**
     * 线性可行子问题结果
     * Linear feasible sub problem result
     *
     * @property result 可行求解器输出 / Feasible solver output
     * @property dualSolution 对偶解 / Dual solution
     * @property cuts 割平面列表 / Cut list
     */
    data class LinearFeasibleResult(
        val result: FeasibleSolverOutput<Flt64>,
        val dualSolution: Map<Constraint<Flt64, Linear>, Flt64>,
        override val cuts: List<LinearInequality<Flt64>>?
    ) : LinearSubResult {
        val obj: Flt64 by result::obj
        val solution: List<Flt64> by result::solution
        val time: Duration by result::time
        val possibleBestObj by result::possibleBestObj
        val gap: Flt64 by result::gap
    }

    /**
     * 线性不可行子问题结果
     * Linear infeasible sub problem result
     *
     * @property farkasDualSolution Farkas 对偶解 / Farkas dual solution
     * @property cuts 割平面列表 / Cut list
     */
    data class LinearInfeasibleResult(
        val farkasDualSolution: Map<Constraint<Flt64, Linear>, Flt64>,
        override val cuts: List<LinearInequality<Flt64>>?
    ) : LinearSubResult

    /**
     * 求解线性 Benders 子问题 / Solve linear Benders sub problem
     *
     * @param name 模型名称 / model name
     * @param metaModel 线性元模型 / linear meta model
     * @param objectVariable 目标变量 / objective variable
     * @param fixedVariables 固定变量映射 / fixed variables mapping
     * @param toLogModel 是否导出模型文件 / whether to export model file
     * @param registrationStatusCallBack 注册状态回调 / registration status callback
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return 子问题求解结果 / sub problem solving result
     */
    suspend fun solveSub(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<LinearSubResult>

    /**
     * 使用选项求解线性 Benders 子问题（便捷重载）
     * Solve linear Benders sub problem with options (convenience overload)
     *
     * @param metaModel 线性元模型 / Linear meta model
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param options 框架求解选项 / Framework solve options
     * @return 子问题求解结果 / Sub problem solving result
     */
    suspend fun solveSub(
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<LinearSubResult> {
        return solveSub(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 异步求解线性 Benders 子问题
     * Asynchronously solve linear Benders sub problem
     *
     * @param name 模型名称 / Model name
     * @param metaModel 线性元模型 / Linear meta model
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 子问题结果的 CompletableFuture / CompletableFuture of sub problem result
     */
    fun solveSubAsync(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<LinearSubResult>> {
        return frameworkAsyncScope.future {
            return@future this@LinearBendersDecompositionSolver.solveSub(
                name = name,
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项异步求解线性 Benders 子问题
     * Asynchronously solve linear Benders sub problem with options
     *
     * @param metaModel 线性元模型 / Linear meta model
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param options 框架求解选项 / Framework solve options
     * @return 子问题结果的 CompletableFuture / CompletableFuture of sub problem result
     */
    fun solveSubAsync(
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<LinearSubResult>> {
        return frameworkAsyncScope.future {
            return@future this@LinearBendersDecompositionSolver.solveSub(
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                options = options
            )
        }
    }

    /**
     * 使用值转换器求解线性 Benders 主问题并转换输出值类型
     * Solve linear Benders master problem with value converter and convert output type
     *
     * @param name 模型名称 / Model name
     * @param metaModel 线性元模型（Flt64） / Linear meta model (Flt64)
     * @param converter 值转换器 / Value converter
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param V 目标数值类型 / Target number type
     * @return 转换后的求解结果 / Solve result in target number type
     */
    suspend fun <V> solveMasterAs(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = solveMaster(
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
     * 求解线性 Benders 主问题并使用元模型内置转换器转换输出值类型
     * Solve linear Benders master problem and convert output using meta model's built-in converter
     *
     * @param name 模型名称 / Model name
     * @param metaModel 线性元模型（泛型数值类型） / Linear meta model (generic number type)
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param V 目标数值类型 / Target number type
     * @return 转换后的求解结果 / Solve result in target number type
     */
    suspend fun <V> solveMasterAs(
        name: String,
        metaModel: LinearMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return solveMasterAs(
            name = name,
            metaModel = castLinearMetaModelForSolver(metaModel),
            converter = metaModel.converter,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    /**
     * 使用选项和值转换器求解线性 Benders 主问题
     * Solve linear Benders master problem with options and value converter
     *
     * @param metaModel 线性元模型（Flt64） / Linear meta model (Flt64)
     * @param converter 值转换器 / Value converter
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后的求解结果 / Solve result in target number type
     */
    suspend fun <V> solveMasterAs(
        metaModel: LinearMetaModel<Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return solveMasterAs(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            converter = converter,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 使用选项求解线性 Benders 主问题并使用元模型内置转换器转换输出值类型
     * Solve linear Benders master problem with options and convert using meta model's built-in converter
     *
     * @param metaModel 线性元模型（泛型数值类型） / Linear meta model (generic number type)
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后的求解结果 / Solve result in target number type
     */
    suspend fun <V> solveMasterAs(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return solveMasterAs(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 异步使用值转换器求解线性 Benders 主问题
     * Asynchronously solve linear Benders master problem with value converter
     *
     * @param name 模型名称 / Model name
     * @param metaModel 线性元模型（Flt64） / Linear meta model (Flt64)
     * @param converter 值转换器 / Value converter
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param V 目标数值类型 / Target number type
     * @return 转换后求解结果的 CompletableFuture / CompletableFuture of target-number-type solve result
     */
    fun <V> solveMasterAsAsync(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterAs(
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
     * 使用选项异步使用值转换器求解线性 Benders 主问题
     * Asynchronously solve linear Benders master problem with options and value converter
     *
     * @param metaModel 线性元模型（Flt64） / Linear meta model (Flt64)
     * @param converter 值转换器 / Value converter
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后求解结果的 CompletableFuture / CompletableFuture of target-number-type solve result
     */
    fun <V> solveMasterAsAsync(
        metaModel: LinearMetaModel<Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterAs(
                metaModel = metaModel,
                converter = converter,
                options = options
            )
        }
    }

    /**
     * 异步求解线性 Benders 主问题并使用元模型内置转换器转换输出值类型
     * Asynchronously solve linear Benders master problem and convert using meta model's built-in converter
     *
     * @param name 模型名称 / Model name
     * @param metaModel 线性元模型（泛型数值类型） / Linear meta model (generic number type)
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param V 目标数值类型 / Target number type
     * @return 转换后求解结果的 CompletableFuture / CompletableFuture of target-number-type solve result
     */
    fun <V> solveMasterAsAsync(
        name: String,
        metaModel: LinearMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterAs(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项异步求解线性 Benders 主问题并使用元模型内置转换器转换输出值类型
     * Asynchronously solve linear Benders master problem with options and convert using meta model's built-in converter
     *
     * @param metaModel 线性元模型（泛型数值类型） / Linear meta model (generic number type)
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后求解结果的 CompletableFuture / CompletableFuture of target-number-type solve result
     */
    fun <V> solveMasterAsAsync(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterAs(
                metaModel = metaModel,
                options = options
            )
        }
    }

    /**
     * 带值类型转换的线性子问题求解结果密封接口
     * Sealed interface for linear sub problem solving result with value type conversion
     *
     * @param V 目标数值类型 / Target number type
     * @property cuts 转换后的割平面列表 / Cut list in target number type
     */
    sealed interface LinearSubResultOf<V> where V : RealNumber<V>, V : NumberField<V> {
        val cuts: List<LinearInequality<V>>?
    }

    /**
     * 带值类型转换的线性可行子问题结果
     * Linear feasible sub problem result with value type conversion
     *
     * @param V 目标数值类型 / Target number type
     * @property result 转换后的可行求解器输出 / Feasible solver output in target number type
     * @property dualSolution 对偶解 / Dual solution
     * @property cuts 转换后的割平面列表 / Cut list in target number type
     */
    data class LinearFeasibleResultOf<V>(
        val result: FeasibleSolverOutput<V>,
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>,
        override val cuts: List<LinearInequality<V>>?
    ) : LinearSubResultOf<V> where V : RealNumber<V>, V : NumberField<V> {
        val obj: Flt64 by result::obj
        val solution: List<V> by result::solution
        val time: Duration by result::time
        val possibleBestObj by result::possibleBestObj
        val gap: Flt64 by result::gap
    }

    /**
     * 带值类型转换的线性不可行子问题结果
     * Linear infeasible sub problem result with value type conversion
     *
     * @param V 目标数值类型 / Target number type
     * @property farkasDualSolution Farkas 对偶解 / Farkas dual solution
     * @property cuts 转换后的割平面列表 / Cut list in target number type
     */
    data class LinearInfeasibleResultOf<V>(
        val farkasDualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>,
        override val cuts: List<LinearInequality<V>>?
    ) : LinearSubResultOf<V> where V : RealNumber<V>, V : NumberField<V>

    /**
     * 使用值转换器求解线性 Benders 子问题并转换输出值类型
     * Solve linear Benders sub problem with value converter and convert output type
     *
     * @param name 模型名称 / Model name
     * @param metaModel 线性元模型（Flt64） / Linear meta model (Flt64)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param converter 值转换器 / Value converter
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param V 目标数值类型 / Target number type
     * @return 转换后的子问题求解结果 / Sub problem solving result in target number type
     */
    suspend fun <V> solveSubAs(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<LinearSubResultOf<V>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = solveSub(
            name = name,
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )) {
            is Ok -> {
                when (val value = result.value) {
                    is LinearFeasibleResult -> {
                        Ok(
                            LinearFeasibleResultOf(
                                result = value.result.convertTo(converter),
                                dualSolution = value.dualSolution,
                                cuts = value.cuts?.map { it.convertTo(converter) }
                            )
                        )
                    }

                    is LinearInfeasibleResult -> {
                        Ok(
                            LinearInfeasibleResultOf(
                                farkasDualSolution = value.farkasDualSolution,
                                cuts = value.cuts?.map { it.convertTo(converter) }
                            )
                        )
                    }
                }
            }

            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 求解线性 Benders 子问题并使用元模型内置转换器转换输出值类型
     * Solve linear Benders sub problem and convert output using meta model's built-in converter
     *
     * @param name 模型名称 / Model name
     * @param metaModel 线性元模型（泛型数值类型） / Linear meta model (generic number type)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param V 目标数值类型 / Target number type
     * @return 转换后的子问题求解结果 / Sub problem solving result in target number type
     */
    suspend fun <V> solveSubAs(
        name: String,
        metaModel: LinearMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<LinearSubResultOf<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveSubAs(
            name = name,
            metaModel = castLinearMetaModelForSolver(metaModel),
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            converter = metaModel.converter,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    /**
     * 使用选项和值转换器求解线性 Benders 子问题
     * Solve linear Benders sub problem with options and value converter
     *
     * @param metaModel 线性元模型（Flt64） / Linear meta model (Flt64)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param converter 值转换器 / Value converter
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后的子问题求解结果 / Sub problem solving result in target number type
     */
    suspend fun <V> solveSubAs(
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<LinearSubResultOf<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveSubAs(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            converter = converter,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 使用选项求解线性 Benders 子问题并使用元模型内置转换器转换输出值类型
     * Solve linear Benders sub problem with options and convert using meta model's built-in converter
     *
     * @param metaModel 线性元模型（泛型数值类型） / Linear meta model (generic number type)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后的子问题求解结果 / Sub problem solving result in target number type
     */
    suspend fun <V> solveSubAs(
        metaModel: LinearMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<LinearSubResultOf<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveSubAs(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 异步使用值转换器求解线性 Benders 子问题
     * Asynchronously solve linear Benders sub problem with value converter
     *
     * @param name 模型名称 / Model name
     * @param metaModel 线性元模型（Flt64） / Linear meta model (Flt64)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param converter 值转换器 / Value converter
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     */
    fun <V> solveSubAsAsync(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<LinearSubResultOf<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubAs(
                name = name,
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                converter = converter,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项异步使用值转换器求解线性 Benders 子问题
     * Asynchronously solve linear Benders sub problem with options and value converter
     *
     * @param metaModel 线性元模型（Flt64） / Linear meta model (Flt64)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param converter 值转换器 / Value converter
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后子问题结果的 CompletableFuture / CompletableFuture of target-number-type sub problem result
     */
    fun <V> solveSubAsAsync(
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<LinearSubResultOf<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubAs(
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                converter = converter,
                options = options
            )
        }
    }

    /**
     * 异步求解线性 Benders 子问题并使用元模型内置转换器转换输出值类型
     * Asynchronously solve linear Benders sub problem and convert using meta model's built-in converter
     *
     * @param name 模型名称 / Model name
     * @param metaModel 线性元模型（泛型数值类型） / Linear meta model (generic number type)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param V 目标数值类型 / Target number type
     * @return 转换后子问题结果的 CompletableFuture / CompletableFuture of target-number-type sub problem result
     */
    fun <V> solveSubAsAsync(
        name: String,
        metaModel: LinearMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<LinearSubResultOf<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubAs(
                name = name,
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项异步求解线性 Benders 子问题并使用元模型内置转换器转换输出值类型
     * Asynchronously solve linear Benders sub problem with options and convert using meta model's built-in converter
     *
     * @param metaModel 线性元模型（泛型数值类型） / Linear meta model (generic number type)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后子问题结果的 CompletableFuture / CompletableFuture of target-number-type sub problem result
     */
    fun <V> solveSubAsAsync(
        metaModel: LinearMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<LinearSubResultOf<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubAs(
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                options = options
            )
        }
    }
}

/**
 * 二次 Benders 分解求解器接口
 * Quadratic Benders decomposition solver interface
 */
interface QuadraticBendersDecompositionSolver : LinearBendersDecompositionSolver {
    /**
     * 求解二次 Benders 主问题
     * Solve quadratic Benders master problem
     *
     * @param name 模型名称 / Model name
     * @param metaModel 二次元模型 / Quadratic meta model
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果 / Solve result
     */
    suspend fun solveMaster(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput>

    /**
     * 使用选项求解二次 Benders 主问题（便捷重载）
     * Solve quadratic Benders master problem with options (convenience overload)
     *
     * @param metaModel 二次元模型 / Quadratic meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果 / Solve result
     */
    suspend fun solveMaster(
        metaModel: QuadraticMetaModel<Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<SolverOutput> {
        return solveMaster(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 异步求解二次 Benders 主问题
     * Asynchronously solve quadratic Benders master problem
     *
     * @param name 模型名称 / Model name
     * @param metaModel 二次元模型 / Quadratic meta model
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
     */
    fun solveMasterAsync(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return frameworkAsyncScope.future {
            return@future this@QuadraticBendersDecompositionSolver.solveMaster(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项异步求解二次 Benders 主问题
     * Asynchronously solve quadratic Benders master problem with options
     *
     * @param metaModel 二次元模型 / Quadratic meta model
     * @param options 框架求解选项 / Framework solve options
     * @return 求解结果的 CompletableFuture / CompletableFuture of solve result
     */
    fun solveMasterAsync(
        metaModel: QuadraticMetaModel<Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<SolverOutput>> {
        return frameworkAsyncScope.future {
            return@future this@QuadraticBendersDecompositionSolver.solveMaster(
                metaModel = metaModel,
                options = options
            )
        }
    }

    /**
     * Sub Problem Solving Result (with cuts if fixed variables are provided)
     *
     * @property linearCuts     list of linear cuts generated by sub problem, if fixed variables are provided.
     * @property quadraticCuts  list of quadratic cuts generated by sub problem, if fixed variables are provided.
     * Otherwise, there should not be any fixed variables in the sub problem model. In other words, the fixed variables should be replaced with their values.
     */
    sealed interface QuadraticSubResult {
        val linearCuts: List<LinearInequality<Flt64>>?
        val quadraticCuts: List<QuadraticInequalityOf<Flt64>>?
    }

    /**
     * 二次可行子问题结果
     * Quadratic feasible sub problem result
     *
     * @property result 可行求解器输出 / Feasible solver output
     * @property dualSolution 对偶解 / Dual solution
     * @property linearCuts 线性割平面列表 / Linear cut list
     * @property quadraticCuts 二次割平面列表 / Quadratic cut list
     */
    data class QuadraticFeasibleResult(
        val result: FeasibleSolverOutput<Flt64>,
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>,
        override val linearCuts: List<LinearInequality<Flt64>>?,
        override val quadraticCuts: List<QuadraticInequalityOf<Flt64>>?,
    ) : QuadraticSubResult {
        val obj: Flt64 by result::obj
        val solution: List<Flt64> by result::solution
        val time: Duration by result::time
        val possibleBestObj by result::possibleBestObj
        val gap: Flt64 by result::gap
    }

    /**
     * 二次不可行子问题结果
     * Quadratic infeasible sub problem result
     *
     * @property farkasDualSolution Farkas 对偶解 / Farkas dual solution
     * @property linearCuts 线性割平面列表 / Linear cut list
     * @property quadraticCuts 二次割平面列表 / Quadratic cut list
     */
    data class QuadraticInfeasibleResult(
        val farkasDualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>,
        override val linearCuts: List<LinearInequality<Flt64>>?,
        override val quadraticCuts: List<QuadraticInequalityOf<Flt64>>?,
    ) : QuadraticSubResult

    /**
     * 求解二次 Benders 子问题
     * Solve quadratic Benders sub problem
     *
     * @param name 模型名称 / Model name
     * @param metaModel 二次元模型 / Quadratic meta model
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 子问题求解结果 / Sub problem solving result
     */
    suspend fun solveSub(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<QuadraticSubResult>

    /**
     * 使用选项求解二次 Benders 子问题（便捷重载）
     * Solve quadratic Benders sub problem with options (convenience overload)
     *
     * @param metaModel 二次元模型 / Quadratic meta model
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param options 框架求解选项 / Framework solve options
     * @return 子问题求解结果 / Sub problem solving result
     */
    suspend fun solveSub(
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<QuadraticSubResult> {
        return solveSub(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 异步求解二次 Benders 子问题
     * Asynchronously solve quadratic Benders sub problem
     *
     * @param name 模型名称 / Model name
     * @param metaModel 二次元模型 / Quadratic meta model
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 子问题结果的 CompletableFuture / CompletableFuture of sub problem result
     */
    fun solveSubAsync(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<QuadraticSubResult>> {
        return frameworkAsyncScope.future {
            return@future this@QuadraticBendersDecompositionSolver.solveSub(
                name = name,
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项异步求解二次 Benders 子问题
     * Asynchronously solve quadratic Benders sub problem with options
     *
     * @param metaModel 二次元模型 / Quadratic meta model
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param options 框架求解选项 / Framework solve options
     * @return 子问题结果的 CompletableFuture / CompletableFuture of sub problem result
     */
    fun solveSubAsync(
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<QuadraticSubResult>> {
        return frameworkAsyncScope.future {
            return@future this@QuadraticBendersDecompositionSolver.solveSub(
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                options = options
            )
        }
    }

    /**
     * 使用值转换器求解二次 Benders 主问题并转换输出值类型
     * Solve quadratic Benders master problem with value converter and convert output type
     *
     * @param name 模型名称 / Model name
     * @param metaModel 二次元模型（Flt64） / Quadratic meta model (Flt64)
     * @param converter 值转换器 / Value converter
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param V 目标数值类型 / Target number type
     * @return 转换后的求解结果 / Solve result in target number type
     */
    suspend fun <V> solveMasterAs(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = solveMaster(
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
     * 求解二次 Benders 主问题并使用元模型内置转换器转换输出值类型
     * Solve quadratic Benders master problem and convert output using meta model's built-in converter
     *
     * @param name 模型名称 / Model name
     * @param metaModel 二次元模型（泛型数值类型） / Quadratic meta model (generic number type)
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param V 目标数值类型 / Target number type
     * @return 转换后的求解结果 / Solve result in target number type
     */
    suspend fun <V> solveMasterAs(
        name: String,
        metaModel: QuadraticMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return solveMasterAs(
            name = name,
            metaModel = castQuadraticMetaModelForSolver(metaModel),
            converter = metaModel.converter,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    /**
     * 使用选项和值转换器求解二次 Benders 主问题
     * Solve quadratic Benders master problem with options and value converter
     *
     * @param metaModel 二次元模型（Flt64） / Quadratic meta model (Flt64)
     * @param converter 值转换器 / Value converter
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后的求解结果 / Solve result in target number type
     */
    suspend fun <V> solveMasterAs(
        metaModel: QuadraticMetaModel<Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return solveMasterAs(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            converter = converter,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 使用选项求解二次 Benders 主问题并使用元模型内置转换器转换输出值类型
     * Solve quadratic Benders master problem with options and convert using meta model's built-in converter
     *
     * @param metaModel 二次元模型（泛型数值类型） / Quadratic meta model (generic number type)
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后的求解结果 / Solve result in target number type
     */
    suspend fun <V> solveMasterAs(
        metaModel: QuadraticMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return solveMasterAs(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 异步使用值转换器求解二次 Benders 主问题
     * Asynchronously solve quadratic Benders master problem with value converter
     *
     * @param name 模型名称 / Model name
     * @param metaModel 二次元模型（Flt64） / Quadratic meta model (Flt64)
     * @param converter 值转换器 / Value converter
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param V 目标数值类型 / Target number type
     * @return 转换后求解结果的 CompletableFuture / CompletableFuture of target-number-type solve result
     */
    fun <V> solveMasterAsAsync(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterAs(
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
     * 使用选项异步使用值转换器求解二次 Benders 主问题
     * Asynchronously solve quadratic Benders master problem with options and value converter
     *
     * @param metaModel 二次元模型（Flt64） / Quadratic meta model (Flt64)
     * @param converter 值转换器 / Value converter
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后求解结果的 CompletableFuture / CompletableFuture of target-number-type solve result
     */
    fun <V> solveMasterAsAsync(
        metaModel: QuadraticMetaModel<Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterAs(
                metaModel = metaModel,
                converter = converter,
                options = options
            )
        }
    }

    /**
     * 异步求解二次 Benders 主问题并使用元模型内置转换器转换输出值类型
     * Asynchronously solve quadratic Benders master problem and convert using meta model's built-in converter
     *
     * @param name 模型名称 / Model name
     * @param metaModel 二次元模型（泛型数值类型） / Quadratic meta model (generic number type)
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param V 目标数值类型 / Target number type
     * @return 转换后求解结果的 CompletableFuture / CompletableFuture of target-number-type solve result
     */
    fun <V> solveMasterAsAsync(
        name: String,
        metaModel: QuadraticMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterAs(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项异步求解二次 Benders 主问题并使用元模型内置转换器转换输出值类型
     * Asynchronously solve quadratic Benders master problem with options and convert using meta model's built-in converter
     *
     * @param metaModel 二次元模型（泛型数值类型） / Quadratic meta model (generic number type)
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后求解结果的 CompletableFuture / CompletableFuture of target-number-type solve result
     */
    fun <V> solveMasterAsAsync(
        metaModel: QuadraticMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterAs(
                metaModel = metaModel,
                options = options
            )
        }
    }

    /**
     * 带值类型转换的二次子问题求解结果密封接口
     * Sealed interface for quadratic sub problem solving result with value type conversion
     *
     * @param V 目标数值类型 / Target number type
     * @property linearCuts 转换后的线性割平面列表 / Linear cut list in target number type
     * @property quadraticCuts 转换后的二次割平面列表 / Quadratic cut list in target number type
     */
    sealed interface QuadraticSubResultOf<V> where V : RealNumber<V>, V : NumberField<V> {
        val linearCuts: List<LinearInequality<V>>?
        val quadraticCuts: List<QuadraticInequalityOf<V>>?
    }

    /**
     * 带值类型转换的二次可行子问题结果
     * Quadratic feasible sub problem result with value type conversion
     *
     * @param V 目标数值类型 / Target number type
     * @property result 转换后的可行求解器输出 / Feasible solver output in target number type
     * @property dualSolution 对偶解 / Dual solution
     * @property linearCuts 转换后的线性割平面列表 / Linear cut list in target number type
     * @property quadraticCuts 转换后的二次割平面列表 / Quadratic cut list in target number type
     */
    data class QuadraticFeasibleResultOf<V>(
        val result: FeasibleSolverOutput<V>,
        val dualSolution: Map<Constraint<Flt64, Quadratic>, Flt64>,
        override val linearCuts: List<LinearInequality<V>>?,
        override val quadraticCuts: List<QuadraticInequalityOf<V>>?
    ) : QuadraticSubResultOf<V> where V : RealNumber<V>, V : NumberField<V> {
        val obj: Flt64 by result::obj
        val solution: List<V> by result::solution
        val time: Duration by result::time
        val possibleBestObj by result::possibleBestObj
        val gap: Flt64 by result::gap
    }

    /**
     * 带值类型转换的二次不可行子问题结果
     * Quadratic infeasible sub problem result with value type conversion
     *
     * @param V 目标数值类型 / Target number type
     * @property farkasDualSolution Farkas 对偶解 / Farkas dual solution
     * @property linearCuts 转换后的线性割平面列表 / Linear cut list in target number type
     * @property quadraticCuts 转换后的二次割平面列表 / Quadratic cut list in target number type
     */
    data class QuadraticInfeasibleResultOf<V>(
        val farkasDualSolution: Map<Constraint<Flt64, Quadratic>, Flt64>,
        override val linearCuts: List<LinearInequality<V>>?,
        override val quadraticCuts: List<QuadraticInequalityOf<V>>?
    ) : QuadraticSubResultOf<V> where V : RealNumber<V>, V : NumberField<V>

    /**
     * 使用值转换器求解二次 Benders 子问题并转换输出值类型
     * Solve quadratic Benders sub problem with value converter and convert output type
     *
     * @param name 模型名称 / Model name
     * @param metaModel 二次元模型（Flt64） / Quadratic meta model (Flt64)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param converter 值转换器 / Value converter
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param V 目标数值类型 / Target number type
     * @return 转换后的子问题求解结果 / Sub problem solving result in target number type
     */
    suspend fun <V> solveSubAs(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<QuadraticSubResultOf<V>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = solveSub(
            name = name,
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )) {
            is Ok -> {
                when (val value = result.value) {
                    is QuadraticFeasibleResult -> {
                        Ok(
                            QuadraticFeasibleResultOf(
                                result = value.result.convertTo(converter),
                                dualSolution = value.dualSolution,
                                linearCuts = value.linearCuts?.map { it.convertTo(converter) },
                                quadraticCuts = value.quadraticCuts?.map { it.convertTo(converter) }
                            )
                        )
                    }

                    is QuadraticInfeasibleResult -> {
                        Ok(
                            QuadraticInfeasibleResultOf(
                                farkasDualSolution = value.farkasDualSolution,
                                linearCuts = value.linearCuts?.map { it.convertTo(converter) },
                                quadraticCuts = value.quadraticCuts?.map { it.convertTo(converter) }
                            )
                        )
                    }
                }
            }

            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 求解二次 Benders 子问题并使用元模型内置转换器转换输出值类型
     * Solve quadratic Benders sub problem and convert output using meta model's built-in converter
     *
     * @param name 模型名称 / Model name
     * @param metaModel 二次元模型（泛型数值类型） / Quadratic meta model (generic number type)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param V 目标数值类型 / Target number type
     * @return 转换后的子问题求解结果 / Sub problem solving result in target number type
     */
    suspend fun <V> solveSubAs(
        name: String,
        metaModel: QuadraticMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<QuadraticSubResultOf<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveSubAs(
            name = name,
            metaModel = castQuadraticMetaModelForSolver(metaModel),
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            converter = metaModel.converter,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    /**
     * 使用选项和值转换器求解二次 Benders 子问题
     * Solve quadratic Benders sub problem with options and value converter
     *
     * @param metaModel 二次元模型（Flt64） / Quadratic meta model (Flt64)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param converter 值转换器 / Value converter
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后的子问题求解结果 / Sub problem solving result in target number type
     */
    suspend fun <V> solveSubAs(
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<QuadraticSubResultOf<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveSubAs(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            converter = converter,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 使用选项求解二次 Benders 子问题并使用元模型内置转换器转换输出值类型
     * Solve quadratic Benders sub problem with options and convert using meta model's built-in converter
     *
     * @param metaModel 二次元模型（泛型数值类型） / Quadratic meta model (generic number type)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后的子问题求解结果 / Sub problem solving result in target number type
     */
    suspend fun <V> solveSubAs(
        metaModel: QuadraticMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<QuadraticSubResultOf<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveSubAs(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    /**
     * 异步使用值转换器求解二次 Benders 子问题
     * Asynchronously solve quadratic Benders sub problem with value converter
     *
     * @param name 模型名称 / Model name
     * @param metaModel 二次元模型（Flt64） / Quadratic meta model (Flt64)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param converter 值转换器 / Value converter
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param V 目标数值类型 / Target number type
     * @return 转换后子问题结果的 CompletableFuture / CompletableFuture of target-number-type sub problem result
     */
    fun <V> solveSubAsAsync(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<QuadraticSubResultOf<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubAs(
                name = name,
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                converter = converter,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项异步使用值转换器求解二次 Benders 子问题
     * Asynchronously solve quadratic Benders sub problem with options and value converter
     *
     * @param metaModel 二次元模型（Flt64） / Quadratic meta model (Flt64)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param converter 值转换器 / Value converter
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后子问题结果的 CompletableFuture / CompletableFuture of target-number-type sub problem result
     */
    fun <V> solveSubAsAsync(
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<QuadraticSubResultOf<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubAs(
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                converter = converter,
                options = options
            )
        }
    }

    /**
     * 异步求解二次 Benders 子问题并使用元模型内置转换器转换输出值类型
     * Asynchronously solve quadratic Benders sub problem and convert using meta model's built-in converter
     *
     * @param name 模型名称 / Model name
     * @param metaModel 二次元模型（泛型数值类型） / Quadratic meta model (generic number type)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param toLogModel 是否输出模型日志 / Whether to log the model
     * @param registrationStatusCallBack 注册状态回调 / Registration status callback
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @param V 目标数值类型 / Target number type
     * @return 转换后子问题结果的 CompletableFuture / CompletableFuture of target-number-type sub problem result
     */
    fun <V> solveSubAsAsync(
        name: String,
        metaModel: QuadraticMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<QuadraticSubResultOf<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubAs(
                name = name,
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    /**
     * 使用选项异步求解二次 Benders 子问题并使用元模型内置转换器转换输出值类型
     * Asynchronously solve quadratic Benders sub problem with options and convert using meta model's built-in converter
     *
     * @param metaModel 二次元模型（泛型数值类型） / Quadratic meta model (generic number type)
     * @param objectVariable 目标变量 / Objective variable
     * @param fixedVariables 固定变量映射 / Fixed variables mapping
     * @param options 框架求解选项 / Framework solve options
     * @param V 目标数值类型 / Target number type
     * @return 转换后子问题结果的 CompletableFuture / CompletableFuture of target-number-type sub problem result
     */
    fun <V> solveSubAsAsync(
        metaModel: QuadraticMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<QuadraticSubResultOf<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubAs(
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                options = options
            )
        }
    }
}
