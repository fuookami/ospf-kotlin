package fuookami.ospf.kotlin.core.backend.solver

import java.util.concurrent.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

interface AbstractQuadraticSolver {
    val name: String

    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput>

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            return@future this@AbstractQuadraticSolver.invoke(model, solvingStatusCallBack)
        }
    }

    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<SolverOutput, List<Solution>>>

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            return@future this@AbstractQuadraticSolver.invoke(model, solutionAmount, solvingStatusCallBack)
        }
    }

    suspend operator fun invoke(
        model: QuadraticMechanismModel,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput> {
        val intermediateModel = dump(model)
        return this(intermediateModel, solvingStatusCallBack)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMechanismModel,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            return@future this@AbstractQuadraticSolver.invoke(model, solvingStatusCallBack)
        }
    }

    suspend operator fun invoke(
        model: QuadraticMechanismModel,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        val intermediateModel = dump(model)
        return this(intermediateModel, solutionAmount, solvingStatusCallBack)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMechanismModel,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            return@future this@AbstractQuadraticSolver.invoke(model, solutionAmount, solvingStatusCallBack)
        }
    }

    suspend operator fun invoke(
        model: QuadraticMetaModel,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput> {
        val mechanismModel = when (val result = dump(model, registrationStatusCallBack)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }
        return this(mechanismModel, solvingStatusCallBack)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMetaModel,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            return@future this@AbstractQuadraticSolver.invoke(model, registrationStatusCallBack, solvingStatusCallBack)
        }
    }

    suspend operator fun invoke(
        model: QuadraticMetaModel,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        val mechanismModel = when (val result = dump(model, registrationStatusCallBack)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }
        return this(mechanismModel, solutionAmount, solvingStatusCallBack)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMetaModel,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        registrationStatusCallBack: RegistrationStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            return@future this@AbstractQuadraticSolver.invoke(model, solutionAmount, registrationStatusCallBack, solvingStatusCallBack)
        }
    }

    suspend fun dump(model: QuadraticMechanismModel): QuadraticTetradModel {
        return QuadraticTetradModel(model)
    }

    suspend fun dump(
        model: QuadraticMetaModel,
        registrationStatusCallBack: RegistrationStatusCallBack? = null
    ): Ret<QuadraticMechanismModel> {
        return QuadraticMechanismModel(
            metaModel = model,
            registrationStatusCallBack = registrationStatusCallBack
        )
    }
}

interface QuadraticSolver : AbstractQuadraticSolver {
    val config: SolverConfig

    override suspend fun dump(model: QuadraticMechanismModel): QuadraticTetradModel {
        return QuadraticTetradModel(model, config.dumpIntermediateModelConcurrent)
    }

    override suspend fun dump(
        model: QuadraticMetaModel,
        registrationStatusCallBack: RegistrationStatusCallBack?
    ): Ret<QuadraticMechanismModel> {
        return QuadraticMechanismModel(
            metaModel = model,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            registrationStatusCallBack = registrationStatusCallBack
        )
    }
}
