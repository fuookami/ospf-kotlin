/**
 * 二次求解器接口定义
 * Quadratic solver interface definitions
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
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.*

/**
 * 二次求解器的抽象接口，定义了求解、异步求解和泛型求解等核心能力。
 * Abstract interface for quadratic solvers, defining core capabilities for solving, async solving, and generic solving.
 */
interface AbstractQuadraticSolver {
    val name: String

    /**
     * 求解二次模型（阻塞）。
     * Solve quadratic model (blocking).
     *
     * @param model 二次四元模型视图 / Quadratic tetrad model view
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @return 求解结果 / Solve result
     */
    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput<Flt64>>

    /**
     * 求解二次模型并启用 IIS 诊断（阻塞）。
     * Solve quadratic model with IIS diagnostics (blocking).
     *
     * @param model 二次四元模型视图 / Quadratic tetrad model view
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @param iisConfig IIS 配置 / IIS configuration
     * @return 求解结果（可能包含 IIS）/ Solve result (may contain IIS)
     */
    suspend operator fun invoke(
        model: QuadraticTetradModelView,
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
     * 异步求解二次模型。
     * Solve quadratic model asynchronously.
     *
     * @param model 二次四元模型视图 / Quadratic tetrad model view
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @param callBack 结果回调（可选）/ Result callback (optional)
     * @return 异步求解结果 / Async solve result
     */
    fun solveAsync(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<FeasibleSolverOutput<Flt64>>) -> Unit)? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput<Flt64>>> {
        return coreSolverAsyncScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callBack?.invoke(result)
            result
        }
    }

    /**
     * 异步求解二次模型并启用 IIS 诊断。
     * Solve quadratic model asynchronously with IIS diagnostics.
     *
     * @param model 二次四元模型视图 / Quadratic tetrad model view
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @param iisConfig IIS 配置 / IIS configuration
     * @param callBack 结果回调（可选）/ Result callback (optional)
     * @return 异步求解结果 / Async solve result
     */
    fun solveAsync(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<SolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return coreSolverAsyncScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
            callBack?.invoke(result)
            result
        }
    }

    /**
     * 求解二次模型获取多个解（阻塞）。
     * Solve quadratic model for multiple solutions (blocking).
     *
     * @param model 二次四元模型视图 / Quadratic tetrad model view
     * @param solutionAmount 期望解数量 / Desired solution amount
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @return 求解结果与解列表 / Solve result with solution list
     */
    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>>

    /**
     * 求解二次模型获取多个解并启用 IIS 诊断（阻塞）。
     * Solve quadratic model for multiple solutions with IIS diagnostics (blocking).
     *
     * @param model 二次四元模型视图 / Quadratic tetrad model view
     * @param solutionAmount 期望解数量 / Desired solution amount
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @param iisConfig IIS 配置 / IIS configuration
     * @return 求解结果与解列表（可能包含 IIS）/ Solve result with solution list (may contain IIS)
     */
    suspend operator fun invoke(
        model: QuadraticTetradModelView,
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
     * 异步求解二次模型获取多个解。
     * Solve quadratic model asynchronously for multiple solutions.
     *
     * @param model 二次四元模型视图 / Quadratic tetrad model view
     * @param solutionAmount 期望解数量 / Desired solution amount
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @param callBack 结果回调（可选）/ Result callback (optional)
     * @return 异步求解结果与解列表 / Async solve result with solution list
     */
    fun solveAsync(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>>> {
        return coreSolverAsyncScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callBack?.invoke(result)
            result
        }
    }

    /**
     * 异步求解二次模型获取多个解并启用 IIS 诊断。
     * Solve quadratic model asynchronously for multiple solutions with IIS diagnostics.
     *
     * @param model 二次四元模型视图 / Quadratic tetrad model view
     * @param solutionAmount 期望解数量 / Desired solution amount
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @param iisConfig IIS 配置 / IIS configuration
     * @param callBack 结果回调（可选）/ Result callback (optional)
     * @return 异步求解结果与解列表 / Async solve result with solution list
     */
    fun solveAsync(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<Pair<SolverOutput, List<List<Flt64>>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<List<Flt64>>>>> {
        return coreSolverAsyncScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
            callBack?.invoke(result)
            result
        }
    }

    // ========== 泛型主接口 / Generic primary interface ==========
    // solve 是泛型主入口；tetrad solve 调用仍是求解器边界。 / solve is the primary generic entry point; tetrad solve calls remain the solver boundary.

    /**
     * 泛型求解二次模型。
     * Solve quadratic model with generic value conversion.
     *
     * @param V 值类型 / Value type
     * @param model 二次四元模型视图 / Quadratic tetrad model view
     * @param converter 值转换器 / Value converter
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @return 求解结果 / Solve result
     */
    suspend fun <V> solve(
        model: QuadraticTetradModelView,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = invoke(model, solvingStatusCallBack)) {
            is Ok -> Ok(result.value.convertTo(converter))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 泛型求解二次模型获取多个解。
     * Solve quadratic model with generic value conversion for multiple solutions.
     *
     * @param V 值类型 / Value type
     * @param model 二次四元模型视图 / Quadratic tetrad model view
     * @param solutionAmount 期望解数量 / Desired solution amount
     * @param converter 值转换器 / Value converter
     * @param solvingStatusCallBack 求解状态回调（可选）/ Solving status callback (optional)
     * @return 求解结果与解列表 / Solve result with solution list
     */
    suspend fun <V> solve(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput<V>, List<Solution<V>>>> where V : RealNumber<V>, V : NumberField<V> {
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
     * 从机制模型求解二次问题（全链路：转储 -> 求解 -> 转换）。
     * Solve quadratic problem from mechanism model (full pipeline: dump -> solve -> convert).
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
    ): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val converted = convertMechanismModelToFlt64(model)) {
            is Ok -> {
                val quadraticModel = converted.value as? QuadraticMechanismModel<Flt64>
                    ?: return Failed(Err(ErrorCode.IllegalArgument, "Quadratic solver requires QuadraticMechanismModel, but got ${converted.value::class.simpleName}"))
                dump(quadraticModel).use { solve(it, converter, solvingStatusCallBack) }
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
     * 从机制模型求解二次问题获取多个解（全链路：转储 -> 求解 -> 转换）。
     * Solve quadratic problem from mechanism model for multiple solutions (full pipeline: dump -> solve -> convert).
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
    ): Ret<Pair<FeasibleSolverOutput<V>, List<Solution<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val converted = convertMechanismModelToFlt64(model)) {
            is Ok -> {
                val quadraticModel = converted.value as? QuadraticMechanismModel<Flt64>
                    ?: return Failed(Err(ErrorCode.IllegalArgument, "Quadratic solver requires QuadraticMechanismModel, but got ${converted.value::class.simpleName}"))
                dump(quadraticModel).use { solve(it, solutionAmount, converter, solvingStatusCallBack) }
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
     * 转储二次机制模型为四元组模型。
     * Dump quadratic mechanism model to tetrad model.
     *
     * @param model 二次机制模型 / Quadratic mechanism model
     * @return 二次四元组模型 / Quadratic tetrad model
     */
    suspend fun dump(model: QuadraticMechanismModel<Flt64>): QuadraticTetradModel {
        return QuadraticTetradModel(model)
    }

    /**
     * 转储二次元模型为机制模型。
     * Dump quadratic meta model to mechanism model.
     *
     * @param model 二次元模型 / Quadratic meta model
     * @param registrationStatusCallBack 注册状态回调（可选）/ Registration status callback (optional)
     * @param dumpingStatusCallBack 转储状态回调（可选）/ Dumping status callback (optional)
     * @return 二次机制模型 / Quadratic mechanism model
     */
    suspend fun dump(
        model: QuadraticMetaModel<Flt64>,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<QuadraticMechanismModel<Flt64>> {
        return QuadraticMechanismModel.invoke<Flt64>(
            metaModel = model,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }
}

/**
 * 二次求解器接口，扩展 [AbstractQuadraticSolver] 并提供配置驱动的模型转储能力。
 * Quadratic solver interface extending [AbstractQuadraticSolver] with configuration-driven model dumping.
 *
 * @property config 求解器配置 / Solver configuration
 */
interface QuadraticSolver : AbstractQuadraticSolver {
    val config: SolverConfig

    override suspend fun dump(model: QuadraticMechanismModel<Flt64>): QuadraticTetradModel {
        return QuadraticTetradModel(
            model = model,
            fixedVariables = null,
            dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
            forceDumpBounds = config.dumpIntermediateModelForceBounds,
            concurrent = config.dumpIntermediateModelConcurrent
        )
    }

    override suspend fun dump(
        model: QuadraticMetaModel<Flt64>,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<QuadraticMechanismModel<Flt64>> {
        return QuadraticMechanismModel.invoke<Flt64>(
            metaModel = model,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }
}
