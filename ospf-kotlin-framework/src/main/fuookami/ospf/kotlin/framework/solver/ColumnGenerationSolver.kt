package fuookami.ospf.kotlin.framework.solver

import java.util.concurrent.*
import kotlin.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

interface ColumnGenerationSolver {
    val name: String

    suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput>

    @OptIn(DelicateCoroutinesApi::class)
    fun solveMILPAsync(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            return@future this@ColumnGenerationSolver.solveMILP(name, metaModel, toLogModel, registrationStatusCallBack, solvingStatusCallBack)
        }
    }

    suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel,
        amount: UInt64,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        return solveMILP(name, metaModel, toLogModel, registrationStatusCallBack, solvingStatusCallBack)
            .map { Pair(it, listOf(it.solution)) }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveMILPAsync(
        name: String,
        metaModel: LinearMetaModel,
        amount: UInt64,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            return@future this@ColumnGenerationSolver.solveMILP(name, metaModel, amount, toLogModel, registrationStatusCallBack, solvingStatusCallBack)
        }
    }

    data class LPResult(
        val result: SolverOutput,
        val dualSolution: Solution
    ) {
        val obj: Flt64 by result::obj
        val solution: Solution by result::solution
        val time: Duration by result::time
        val possibleBestObj by result::possibleBestObj
        val gap: Flt64 by result::gap
    }

    suspend fun solveLP(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<LPResult>

    @OptIn(DelicateCoroutinesApi::class)
    fun solveLPAsync(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<LPResult>> {
        return GlobalScope.future {
            return@future this@ColumnGenerationSolver.solveLP(name, metaModel, toLogModel, registrationStatusCallBack, solvingStatusCallBack)
        }
    }
}
