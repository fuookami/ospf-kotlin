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

interface AbstractLinearSolver {
    val name: String

    suspend operator fun invoke(
        model: LinearTriadModelView,
        statusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput>

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearTriadModelView,
        statusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            return@future this@AbstractLinearSolver.invoke(model, statusCallBack)
        }
    }

    suspend operator fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        statusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<SolverOutput, List<Solution>>>

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        statusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            return@future this@AbstractLinearSolver.invoke(model, solutionAmount, statusCallBack)
        }
    }

    suspend operator fun invoke(
        model: LinearMechanismModel,
        statusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput> {
        val intermediateModel = dump(model)
        return this(intermediateModel, statusCallBack)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMechanismModel,
        statusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            return@future this@AbstractLinearSolver.invoke(model, statusCallBack)
        }
    }

    suspend operator fun invoke(
        model: LinearMechanismModel,
        solutionAmount: UInt64,
        statusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        val intermediateModel = dump(model)
        return this(intermediateModel, solutionAmount, statusCallBack)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMechanismModel,
        solutionAmount: UInt64,
        statusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            return@future this@AbstractLinearSolver.invoke(model, solutionAmount, statusCallBack)
        }
    }

    suspend operator fun invoke(
        model: LinearMetaModel,
        statusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput> {
        val mechanismModel = when (val result = dump(model)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }
        return this(mechanismModel, statusCallBack)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMetaModel,
        statusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            return@future this@AbstractLinearSolver.invoke(model, statusCallBack)
        }
    }

    suspend operator fun invoke(
        model: LinearMetaModel,
        solutionAmount: UInt64,
        statusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        val mechanismModel = when (val result = dump(model)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }
        return this(mechanismModel, solutionAmount, statusCallBack)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: LinearMetaModel,
        solutionAmount: UInt64,
        statusCallBack: SolvingStatusCallBack? = null,
        callback: (Ret<Pair<SolverOutput, List<Solution>>>) -> Unit
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            val result = this@AbstractLinearSolver.invoke(model, solutionAmount, statusCallBack)
            callback(result)
            return@future result
        }
    }

    suspend fun dump(model: LinearMechanismModel): LinearTriadModel {
        return LinearTriadModel(model)
    }

    suspend fun dump(model: LinearMetaModel): Ret<LinearMechanismModel> {
        return LinearMechanismModel(model)
    }
}

interface LinearSolver : AbstractLinearSolver {
    val config: SolverConfig

    override suspend fun dump(model: LinearMechanismModel): LinearTriadModel {
        return LinearTriadModel(model, config.dumpIntermediateModelConcurrent)
    }

    override suspend fun dump(model: LinearMetaModel): Ret<LinearMechanismModel> {
        return LinearMechanismModel(model, config.dumpMechanismModelConcurrent)
    }
}
