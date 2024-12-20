package fuookami.ospf.kotlin.framework.solver

import kotlin.time.*
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
        statusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput>

    suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel,
        amount: UInt64,
        toLogModel: Boolean = false,
        statusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        return solveMILP(name, metaModel, toLogModel, statusCallBack)
            .map { Pair(it, listOf(it.solution)) }
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
        statusCallBack: SolvingStatusCallBack? = null
    ): Ret<LPResult>
}
