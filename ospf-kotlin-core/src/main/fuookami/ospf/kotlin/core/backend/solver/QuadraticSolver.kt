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
        statusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput>

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticTetradModelView,
        statusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            return@future this@AbstractQuadraticSolver.invoke(model, statusCallBack)
        }
    }

    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        statusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<SolverOutput, List<Solution>>>

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        statusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            return@future this@AbstractQuadraticSolver.invoke(model, solutionAmount, statusCallBack)
        }
    }

    suspend operator fun invoke(
        model: QuadraticMechanismModel,
        statusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput> {
        val intermediateModel = dump(model)
        return this(intermediateModel, statusCallBack)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMechanismModel,
        statusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            return@future this@AbstractQuadraticSolver.invoke(model, statusCallBack)
        }
    }

    suspend operator fun invoke(
        model: QuadraticMechanismModel,
        solutionAmount: UInt64,
        statusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        val intermediateModel = dump(model)
        return this(intermediateModel, solutionAmount, statusCallBack)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMechanismModel,
        solutionAmount: UInt64,
        statusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            return@future this@AbstractQuadraticSolver.invoke(model, solutionAmount, statusCallBack)
        }
    }

    suspend operator fun invoke(
        model: QuadraticMetaModel,
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
        model: QuadraticMetaModel,
        statusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            return@future this@AbstractQuadraticSolver.invoke(model, statusCallBack)
        }
    }

    suspend operator fun invoke(
        model: QuadraticMetaModel,
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
        model: QuadraticMetaModel,
        solutionAmount: UInt64,
        statusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            return@future this@AbstractQuadraticSolver.invoke(model, solutionAmount, statusCallBack)
        }
    }

    suspend fun dump(model: QuadraticMechanismModel): QuadraticTetradModel {
        return QuadraticTetradModel(model)
    }

    suspend fun dump(model: QuadraticMetaModel): Ret<QuadraticMechanismModel> {
        return QuadraticMechanismModel(model)
    }
}

interface QuadraticSolver : AbstractQuadraticSolver {
    val config: SolverConfig

    override suspend fun dump(model: QuadraticMechanismModel): QuadraticTetradModel {
        return QuadraticTetradModel(model, config.dumpIntermediateModelConcurrent)
    }

    override suspend fun dump(model: QuadraticMetaModel): Ret<QuadraticMechanismModel> {
        return QuadraticMechanismModel(model, config.dumpMechanismModelConcurrent)
    }
}
