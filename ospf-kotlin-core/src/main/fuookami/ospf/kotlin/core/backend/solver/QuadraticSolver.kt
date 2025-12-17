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

interface AbstractQuadraticSolver {
    val name: String

    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput>

    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<SolverOutput> {
        return when (val result = this(model, solvingStatusCallBack)) {
            is Ok -> {
                Ok(result.value)
            }

            is Failed -> {
                if (result.error.code == ErrorCode.ORModelInfeasible) {
                    when (val result = computeIIS(model, this, iisConfig)) {
                        is Ok -> {
                            Ok(QuadraticInfeasibleSolverOutput(result.value))
                        }

                        is Failed -> {
                            return Failed(result.error)
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
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(model, solvingStatusCallBack)
            callBack?.invoke(result)
            result
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<SolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(model, solvingStatusCallBack, iisConfig)
            callBack?.invoke(result)
            result
        }
    }

    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>>

    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        return when (val result = this(model, solutionAmount, solvingStatusCallBack)) {
            is Ok -> {
                Ok(result.value)
            }

            is Failed -> {
                if (result.error.code == ErrorCode.ORModelInfeasible) {
                    when (val result = computeIIS(model, this, iisConfig)) {
                        is Ok -> {
                            Ok(QuadraticInfeasibleSolverOutput(result.value) to emptyList())
                        }

                        is Failed -> {
                            return Failed(result.error)
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
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<Pair<FeasibleSolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(model, solutionAmount, solvingStatusCallBack)
            callBack?.invoke(result)
            result
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<Pair<SolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(model, solutionAmount, solvingStatusCallBack, iisConfig)
            callBack?.invoke(result)
            result
        }
    }

    suspend operator fun invoke(
        model: QuadraticMechanismModel,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput> {
        val intermediateModel = dump(model)
        return this(intermediateModel, solvingStatusCallBack)
    }

    suspend operator fun invoke(
        model: QuadraticMechanismModel,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<SolverOutput> {
        val intermediateModel = dump(model)
        return this(intermediateModel, solvingStatusCallBack, iisConfig)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMechanismModel,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(model, solvingStatusCallBack)
            callBack?.invoke(result)
            result
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMechanismModel,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<SolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(model, solvingStatusCallBack, iisConfig)
            callBack?.invoke(result)
            result
        }
    }

    suspend operator fun invoke(
        model: QuadraticMechanismModel,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        val intermediateModel = dump(model)
        return this(intermediateModel, solutionAmount, solvingStatusCallBack)
    }

    suspend operator fun invoke(
        model: QuadraticMechanismModel,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        val intermediateModel = dump(model)
        return this(intermediateModel, solutionAmount, solvingStatusCallBack, iisConfig)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMechanismModel,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<Pair<FeasibleSolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(model, solutionAmount, solvingStatusCallBack)
            callBack?.invoke(result)
            result
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMechanismModel,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<Pair<SolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(model, solutionAmount, solvingStatusCallBack, iisConfig)
            callBack?.invoke(result)
            result
        }
    }

    suspend operator fun invoke(
        model: QuadraticMetaModel,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput> {
        val mechanismModel = when (val result = dump(model, registrationStatusCallBack, dumpingStatusCallBack)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }
        return this(mechanismModel, solvingStatusCallBack)
    }

    suspend operator fun invoke(
        model: QuadraticMetaModel,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<SolverOutput> {
        val mechanismModel = when (val result = dump(model, registrationStatusCallBack, dumpingStatusCallBack)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }
        return this(mechanismModel, solvingStatusCallBack, iisConfig)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMetaModel,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(model, registrationStatusCallBack, dumpingStatusCallBack, solvingStatusCallBack)
            callBack?.invoke(result)
            result
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMetaModel,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<SolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(model, registrationStatusCallBack, dumpingStatusCallBack, solvingStatusCallBack, iisConfig)
            callBack?.invoke(result)
            result
        }
    }

    suspend operator fun invoke(
        model: QuadraticMetaModel,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        val mechanismModel = when (val result = dump(model, registrationStatusCallBack, dumpingStatusCallBack)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }
        return this(mechanismModel, solutionAmount, solvingStatusCallBack)
    }

    suspend operator fun invoke(
        model: QuadraticMetaModel,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        val mechanismModel = when (val result = dump(model, registrationStatusCallBack, dumpingStatusCallBack)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }
        return this(mechanismModel, solutionAmount, solvingStatusCallBack, iisConfig)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMetaModel,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<Pair<FeasibleSolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(model, solutionAmount, registrationStatusCallBack, dumpingStatusCallBack, solvingStatusCallBack)
            callBack?.invoke(result)
            result
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMetaModel,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<Pair<SolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(model, solutionAmount, registrationStatusCallBack, dumpingStatusCallBack, solvingStatusCallBack, iisConfig)
            callBack?.invoke(result)
            result
        }
    }

    suspend fun dump(model: QuadraticMechanismModel): QuadraticTetradModel {
        return QuadraticTetradModel(model)
    }

    suspend fun dump(
        model: QuadraticMetaModel,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<QuadraticMechanismModel> {
        return QuadraticMechanismModel(
            metaModel = model,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }
}

interface QuadraticSolver : AbstractQuadraticSolver {
    val config: SolverConfig

    override suspend fun dump(model: QuadraticMechanismModel): QuadraticTetradModel {
        return QuadraticTetradModel(
            model = model,
            fixedVariables = null,
            concurrent = config.dumpIntermediateModelConcurrent,
            withDumpingBounds = config.dumpIntermediateModelBounds,
            withForceDumpingBounds = config.dumpIntermediateModelForceBounds
        )
    }

    override suspend fun dump(
        model: QuadraticMetaModel,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<QuadraticMechanismModel> {
        return QuadraticMechanismModel(
            metaModel = model,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }
}
