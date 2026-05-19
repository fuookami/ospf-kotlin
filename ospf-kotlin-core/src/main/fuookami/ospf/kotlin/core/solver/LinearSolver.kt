package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.output.convertTo
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.MechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.convertMechanismModelToFlt64
import fuookami.ospf.kotlin.core.model.intermediate.MechanismModelDumpingStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel

interface AbstractLinearSolver {
    val name: String

    suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>>

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

        fun solveAsync(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>>) -> Unit)? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>>> {
        return coreSolverAsyncScope.future {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callBack?.invoke(result)
            result
        }
    }

        fun solveAsync(
        model: LinearTriadModel,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<SolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return coreSolverAsyncScope.future {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
            callBack?.invoke(result)
            result
        }
    }

    suspend operator fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>, List<List<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>>

    suspend operator fun invoke(
        model: LinearTriadModelView,
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
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<Pair<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>, List<List<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>, List<List<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>>> {
        return coreSolverAsyncScope.future {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callBack?.invoke(result)
            result
        }
    }

        fun solveAsync(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<Pair<SolverOutput, List<List<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<List<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>>> {
        return coreSolverAsyncScope.future {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
            callBack?.invoke(result)
            result
        }
    }

    // ========== V-generic primary interface ==========
    // solveV is the primary V-generic entry point; triad solve calls remain the solver boundary.

    suspend fun <V> solveV(
        model: LinearTriadModelView,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = invoke(model, solvingStatusCallBack)) {
            is Ok -> Ok(result.value.convertTo(converter))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    suspend fun <V> solveV(
        model: LinearTriadModelView,
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

    // V-generic solveV for generic MechanismModel<V>: full pipeline (dump -> solve -> convert)
    suspend fun <V> solveV(
        model: MechanismModel<V>,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val converted = convertMechanismModelToFlt64(model)) {
            is Ok -> {
                val linearModel = converted.value as? LinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
                    ?: return Failed(Err(ErrorCode.IllegalArgument, "Linear solver requires LinearMechanismModel, but got ${converted.value::class.simpleName}"))
                dump(linearModel).use { solveV(it, converter, solvingStatusCallBack) }
            }

            is Failed -> {
                Failed(converted.error)
            }

            is Fatal -> {
                Fatal(converted.errors)
            }
        }
    }

    suspend fun <V> solveV(
        model: MechanismModel<V>,
        solutionAmount: UInt64,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput<V>, List<Solution<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val converted = convertMechanismModelToFlt64(model)) {
            is Ok -> {
                val linearModel = converted.value as? LinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
                    ?: return Failed(Err(ErrorCode.IllegalArgument, "Linear solver requires LinearMechanismModel, but got ${converted.value::class.simpleName}"))
                dump(linearModel).use { solveV(it, solutionAmount, converter, solvingStatusCallBack) }
            }

            is Failed -> {
                Failed(converted.error)
            }

            is Fatal -> {
                Fatal(converted.errors)
            }
        }
    }

    suspend fun dump(model: LinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearTriadModel {
        return LinearTriadModel(model)
    }

    suspend fun dump(
        model: LinearMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<LinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        @Suppress("DEPRECATION")
        return LinearMechanismModel.invoke<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
            metaModel = model,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }
}

interface LinearSolver : AbstractLinearSolver {
    val config: SolverConfig

    override suspend fun dump(model: LinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearTriadModel {
        return LinearTriadModel(
            model = model,
            fixedVariables = null,
            dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
            forceDumpBounds = config.dumpIntermediateModelForceBounds,
            concurrent = config.dumpIntermediateModelConcurrent
        )
    }

    override suspend fun dump(
        model: LinearMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<LinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        @Suppress("DEPRECATION")
        return LinearMechanismModel.invoke<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
            metaModel = model,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpIntermediateModelConcurrent,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }
}


