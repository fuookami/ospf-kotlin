@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.core.backend.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.backend.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearDualSolution
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.RegistrationStatusCallBack
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.UInt64
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration

interface ColumnGenerationSolver {
    val name: String

    suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput>

    @OptIn(DelicateCoroutinesApi::class)
    fun solveMILPAsync(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput>> {
        return GlobalScope.future {
            return@future this@ColumnGenerationSolver.solveMILP(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel,
        amount: UInt64,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        return solveMILP(
            name = name,
            metaModel = metaModel,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
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
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            return@future this@ColumnGenerationSolver.solveMILP(
                name = name,
                metaModel = metaModel,
                amount = amount,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    data class LPResult(
        val result: FeasibleSolverOutput,
        val dualSolution: LinearDualSolution
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
            return@future this@ColumnGenerationSolver.solveLP(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }
}