/**
 * 二次求解器接口定义
 * Quadratic solver interface definitions
 */
package fuookami.ospf.kotlin.core.solver

import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.future.future
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
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

    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>>

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

        fun solveAsync(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>>) -> Unit)? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>>> {
        return coreSolverAsyncScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callBack?.invoke(result)
            result
        }
    }

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

    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>, List<List<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>>

    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<Pair<SolverOutput, List<List<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>> {
        return solveWithOptionsAndIISForSolutionPool(
            model = model,
            options = SolveOptions(
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            ),
            iisConfig = iisConfig
        )
    }

        fun solveAsync(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<Pair<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>, List<List<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>, List<List<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>>> {
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

        fun solveAsync(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<Pair<SolverOutput, List<List<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<List<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>>> {
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
    suspend fun <V> solve(
        model: MechanismModel<V>,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val converted = convertMechanismModelToFlt64(model)) {
            is Ok -> {
                val quadraticModel = converted.value as? QuadraticMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
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

    suspend fun <V> solve(
        model: MechanismModel<V>,
        solutionAmount: UInt64,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput<V>, List<Solution<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val converted = convertMechanismModelToFlt64(model)) {
            is Ok -> {
                val quadraticModel = converted.value as? QuadraticMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
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

    suspend fun dump(model: QuadraticMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticTetradModel {
        return QuadraticTetradModel(model)
    }

    suspend fun dump(
        model: QuadraticMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<QuadraticMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        @Suppress("DEPRECATION")
        return QuadraticMechanismModel.invoke<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
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

    override suspend fun dump(model: QuadraticMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticTetradModel {
        return QuadraticTetradModel(
            model = model,
            fixedVariables = null,
            dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
            forceDumpBounds = config.dumpIntermediateModelForceBounds,
            concurrent = config.dumpIntermediateModelConcurrent
        )
    }

    override suspend fun dump(
        model: QuadraticMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<QuadraticMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        @Suppress("DEPRECATION")
        return QuadraticMechanismModel.invoke<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
            metaModel = model,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }
}
