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
import fuookami.ospf.kotlin.core.model.intermediate.MechanismModelDumpingStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel

interface AbstractLinearSolver {
    val name: String

    suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput<Flt64>>

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

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<FeasibleSolverOutput<Flt64>>) -> Unit)? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput<Flt64>>> {
        return GlobalScope.future {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callBack?.invoke(result)
            result
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearTriadModel,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<SolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
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
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>>

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

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>>> {
        return GlobalScope.future {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callBack?.invoke(result)
            result
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<Pair<SolverOutput, List<List<Flt64>>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<List<Flt64>>>>> {
        return GlobalScope.future {
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

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    suspend operator fun invoke(
        model: LinearMechanismModel<Flt64>,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
    ): Ret<FeasibleSolverOutput<Flt64>> {
        return solveWithOptions(
            model = model,
            options = SolveOptions(
                solvingStatusCallBack = solvingStatusCallBack
            )
        )
    }

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    suspend operator fun invoke(
        model: LinearMechanismModel<Flt64>,
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

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMechanismModel<Flt64>,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<FeasibleSolverOutput<Flt64>>) -> Unit)? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput<Flt64>>> {
        return solveAsync(
            model = model,
            options = SolveOptions(
                solvingStatusCallBack = solvingStatusCallBack
            ),
            callBack = callBack
        )
    }

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMechanismModel<Flt64>,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<SolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
            callBack?.invoke(result)
            return@future result
        }
    }

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    suspend operator fun invoke(
        model: LinearMechanismModel<Flt64>,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
        return dump(model).use { intermediateModel ->
            this(
                model = intermediateModel,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    suspend operator fun invoke(
        model: LinearMechanismModel<Flt64>,
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

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMechanismModel<Flt64>,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>>> {
        return GlobalScope.future {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callBack?.invoke(result)
            return@future result
        }
    }

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMechanismModel<Flt64>,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<Pair<SolverOutput, List<List<Flt64>>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<List<Flt64>>>>> {
        return GlobalScope.future {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
            callBack?.invoke(result)
            return@future result
        }
    }

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    suspend operator fun invoke(
        model: LinearMetaModel<Flt64>,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput<Flt64>> {
        return when (val result = dump(
            model = model,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }.use {
            solveWithOptions(
                model = it,
                options = SolveOptions(
                    solvingStatusCallBack = solvingStatusCallBack
                )
            )
        }
    }

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    suspend operator fun invoke(
        model: LinearMetaModel<Flt64>,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<SolverOutput> {
        return solveWithOptionsAndIIS(
            model = model,
            options = SolveOptions(
                solvingStatusCallBack = solvingStatusCallBack
            ),
            iisConfig = iisConfig,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMetaModel<Flt64>,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<FeasibleSolverOutput<Flt64>>) -> Unit)? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput<Flt64>>> {
        return GlobalScope.future {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                registrationStatusCallBack = registrationStatusCallBack,
                dumpingStatusCallBack = dumpingStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callBack?.invoke(result)
            return@future result
        }
    }

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMetaModel<Flt64>,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: (Ret<SolverOutput>) -> Unit
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                registrationStatusCallBack = registrationStatusCallBack,
                dumpingStatusCallBack = dumpingStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
            callBack(result)
            return@future result
        }
    }

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    suspend operator fun invoke(
        model: LinearMetaModel<Flt64>,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
        return when (val result = dump(
            model = model,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack,
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }.use {
            this(
                model = it,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    suspend operator fun invoke(
        model: LinearMetaModel<Flt64>,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<Pair<SolverOutput, List<List<Flt64>>>> {
        return solveWithOptionsAndIISForSolutionPool(
            model = model,
            options = SolveOptions(
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            ),
            iisConfig = iisConfig,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMetaModel<Flt64>,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callback: ((Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>>> {
        return GlobalScope.future {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                registrationStatusCallBack = registrationStatusCallBack,
                dumpingStatusCallBack = dumpingStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callback?.invoke(result)
            return@future result
        }
    }

    @Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMetaModel<Flt64>,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callback: ((Ret<Pair<SolverOutput, List<List<Flt64>>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<List<Flt64>>>>> {
        return GlobalScope.future {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                registrationStatusCallBack = registrationStatusCallBack,
                dumpingStatusCallBack = dumpingStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
            callback?.invoke(result)
            return@future result
        }
    }

    // ========== V-generic primary interface ==========
    // solveV is the primary V-generic entry point. Flt64 invoke() is the adapter boundary
    // for external solvers that operate on double. solveV contains the full pipeline
    // (validate -> dump -> solve -> convert) rather than being a thin post-conversion wrapper.

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

    @Deprecated("Use solveV(model: MechanismModel<V>, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    suspend fun <V> solveV(
        model: LinearMechanismModel<Flt64>,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
        @Suppress("DEPRECATION")
        return when (val result = invoke(model, solvingStatusCallBack)) {
            is Ok -> Ok(result.value.convertTo(converter))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    @Deprecated("Use solveV(model: MechanismModel<V>, converter) for V-typed results. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
    suspend fun <V> solveV(
        model: LinearMechanismModel<Flt64>,
        solutionAmount: UInt64,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput<V>, List<Solution<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        @Suppress("DEPRECATION")
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
    @Suppress("DEPRECATION")
    suspend fun <V> solveV(
        model: MechanismModel<V>,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
        // Adapter boundary: external solvers require Flt64. Convert model, solve, then convert back.
        val flt64Model = model as LinearMechanismModel<Flt64>
        return solveV(flt64Model, converter, solvingStatusCallBack)
    }

    @Suppress("DEPRECATION")
    suspend fun <V> solveV(
        model: MechanismModel<V>,
        solutionAmount: UInt64,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput<V>, List<Solution<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        val flt64Model = model as LinearMechanismModel<Flt64>
        return solveV(flt64Model, solutionAmount, converter, solvingStatusCallBack)
    }

    suspend fun dump(model: LinearMechanismModel<Flt64>): LinearTriadModel {
        return LinearTriadModel(model)
    }

    suspend fun dump(
        model: LinearMetaModel<Flt64>,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<LinearMechanismModel<Flt64>> {
        @Suppress("DEPRECATION")
        return LinearMechanismModel.invoke<Flt64>(
            metaModel = model,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }
}

interface LinearSolver : AbstractLinearSolver {
    val config: SolverConfig

    override suspend fun dump(model: LinearMechanismModel<Flt64>): LinearTriadModel {
        return LinearTriadModel(
            model = model,
            fixedVariables = null,
            dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
            forceDumpBounds = config.dumpIntermediateModelForceBounds,
            concurrent = config.dumpIntermediateModelConcurrent
        )
    }

    override suspend fun dump(
        model: LinearMetaModel<Flt64>,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<LinearMechanismModel<Flt64>> {
        @Suppress("DEPRECATION")
        return LinearMechanismModel.invoke<Flt64>(
            metaModel = model,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpIntermediateModelConcurrent,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }
}



