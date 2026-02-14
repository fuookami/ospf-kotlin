package fuookami.ospf.kotlin.core.backend.solver

import java.util.concurrent.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.iis.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

interface AbstractLinearSolver {
    val name: String

    suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput>

    suspend operator fun invoke(
        model: LinearTriadModel,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<SolverOutput> {
        return when (val result = this(
            model = model,
            solvingStatusCallBack = solvingStatusCallBack
        )) {
            is Ok -> {
                Ok(result.value)
            }

            is Failed -> {
                if (result.error.code == ErrorCode.ORModelInfeasible) {
                    when (val result = computeIIS(model, this, iisConfig)) {
                        is Ok -> {
                            Ok(LinearInfeasibleSolverOutput(result.value))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }
                    }
                } else {
                    Failed(result.error)
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput>> {
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
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>>

    suspend operator fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        return when (val result = this(
            model = model,
            solutionAmount = solutionAmount,
            solvingStatusCallBack = solvingStatusCallBack
        )) {
            is Ok -> {
                Ok(result.value)
            }

            is Failed -> {
                if (result.error.code == ErrorCode.ORModelInfeasible) {
                    when (val result = computeIIS(model, this, iisConfig)) {
                        is Ok -> {
                            Ok(LinearInfeasibleSolverOutput(result.value) to emptyList())
                        }

                        is Failed -> {
                            Failed(result.error)
                        }
                    }
                } else {
                    Failed(result.error)
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<Pair<FeasibleSolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput, List<Solution>>>> {
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
        callBack: ((Ret<Pair<SolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
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

    suspend operator fun invoke(
        model: LinearMechanismModel,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
    ): Ret<FeasibleSolverOutput> {
        return dump(model).use { intermediateModel ->
            this(
                model = intermediateModel,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    suspend operator fun invoke(
        model: LinearMechanismModel,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<SolverOutput> {
        return dump(model).use { intermediateModel ->
            this(
                model = intermediateModel,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMechanismModel,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput>> {
        return GlobalScope.future {
            val result = this@AbstractLinearSolver.invoke(
                model = model,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callBack?.invoke(result)
            return@future result
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMechanismModel,
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

    suspend operator fun invoke(
        model: LinearMechanismModel,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        return dump(model).use { intermediateModel ->
            this(
                model = intermediateModel,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    suspend operator fun invoke(
        model: LinearMechanismModel,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        return dump(model).use { intermediateModel ->
            this(
                model = intermediateModel,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMechanismModel,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<Pair<FeasibleSolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput, List<Solution>>>> {
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

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMechanismModel,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<Pair<SolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
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

    suspend operator fun invoke(
        model: LinearMetaModel,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput> {
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
        }.use {
            this(
                model = it,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    suspend operator fun invoke(
        model: LinearMetaModel,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<SolverOutput> {
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
        }.use {
            this(
                model = it,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMetaModel,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput>> {
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

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMetaModel,
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

    suspend operator fun invoke(
        model: LinearMetaModel,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
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
        }.use {
            this(
                model = it,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    suspend operator fun invoke(
        model: LinearMetaModel,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<Pair<SolverOutput, List<Solution>>> {
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
        }.use {
            this(
                model = it,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMetaModel,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callback: ((Ret<Pair<FeasibleSolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput, List<Solution>>>> {
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

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMetaModel,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callback: ((Ret<Pair<SolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
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

    suspend fun dump(model: LinearMechanismModel): LinearTriadModel {
        return LinearTriadModel(model)
    }

    suspend fun dump(
        model: LinearMetaModel,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<LinearMechanismModel> {
        return LinearMechanismModel(
            metaModel = model,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }
}

interface LinearSolver : AbstractLinearSolver {
    val config: SolverConfig

    override suspend fun dump(model: LinearMechanismModel): LinearTriadModel {
        return LinearTriadModel(
            model = model,
            fixedVariables = null,
            dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
            forceDumpBounds = config.dumpIntermediateModelForceBounds,
            concurrent = config.dumpIntermediateModelConcurrent
        )
    }

    override suspend fun dump(
        model: LinearMetaModel,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<LinearMechanismModel> {
        return LinearMechanismModel(
            metaModel = model,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpIntermediateModelConcurrent,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }
}
