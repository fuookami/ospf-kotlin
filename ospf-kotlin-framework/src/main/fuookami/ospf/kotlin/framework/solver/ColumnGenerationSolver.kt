@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.Linear
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput

interface ColumnGenerationSolver {
    val name: String

    suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput<Flt64>>

    suspend fun solveMILP(
        metaModel: LinearMetaModel<Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<FeasibleSolverOutput<Flt64>> {
        val solutionAmount = options.solutionAmount
        return if (solutionAmount != null) {
            solveMILP(
                name = options.solveName(metaModel.name),
                metaModel = metaModel,
                amount = solutionAmount,
                toLogModel = options.toLogModel,
                registrationStatusCallBack = options.registrationStatusCallBack,
                solvingStatusCallBack = options.solvingStatusCallBack
            ).map { it.first }
        } else {
            solveMILP(
                name = options.solveName(metaModel.name),
                metaModel = metaModel,
                toLogModel = options.toLogModel,
                registrationStatusCallBack = options.registrationStatusCallBack,
                solvingStatusCallBack = options.solvingStatusCallBack
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveMILPAsync(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput<Flt64>>> {
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

    @OptIn(DelicateCoroutinesApi::class)
    fun solveMILPAsync(
        metaModel: LinearMetaModel<Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<FeasibleSolverOutput<Flt64>>> {
        return GlobalScope.future {
            return@future this@ColumnGenerationSolver.solveMILP(
                metaModel = metaModel,
                options = options
            )
        }
    }

    suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        amount: UInt64,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
        return solveMILP(
            name = name,
            metaModel = metaModel,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
            .map { Pair(it, listOf(it.solution)) }
    }

    suspend fun solveMILPWithSolutionPool(
        metaModel: LinearMetaModel<Flt64>,
        options: FrameworkSolveOptions
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
        return solveMILP(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            amount = options.solutionAmount ?: UInt64.one,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveMILPAsync(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        amount: UInt64,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>>> {
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

    @OptIn(DelicateCoroutinesApi::class)
    fun solveMILPWithSolutionPoolAsync(
        metaModel: LinearMetaModel<Flt64>,
        options: FrameworkSolveOptions
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>>> {
        return GlobalScope.future {
            return@future this@ColumnGenerationSolver.solveMILPWithSolutionPool(
                metaModel = metaModel,
                options = options
            )
        }
    }

    data class LPResult(
        val result: FeasibleSolverOutput<Flt64>,
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
    ) {
        val obj: Flt64 by result::obj
        val solution: List<Flt64> by result::solution
        val time: Duration by result::time
        val possibleBestObj by result::possibleBestObj
        val gap: Flt64 by result::gap
    }

    suspend fun solveLP(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<LPResult>

    suspend fun solveLP(
        metaModel: LinearMetaModel<Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<LPResult> {
        return solveLP(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveLPAsync(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
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

    @OptIn(DelicateCoroutinesApi::class)
    fun solveLPAsync(
        metaModel: LinearMetaModel<Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<LPResult>> {
        return GlobalScope.future {
            return@future this@ColumnGenerationSolver.solveLP(
                metaModel = metaModel,
                options = options
            )
        }
    }
}


