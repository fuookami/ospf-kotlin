package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import kotlin.time.Duration

interface ColumnGenerationSolver {
    suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean = false
    ): Ret<LinearSolverOutput>

    suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel,
        amount: UInt64,
        toLogModel: Boolean = false
    ): Ret<Pair<LinearSolverOutput, List<Solution>>> {
        return solveMILP(name, metaModel, toLogModel)
            .map { Pair(it, listOf(it.solution)) }
    }

    data class LPResult(
        val result: LinearSolverOutput,
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
        toLogModel: Boolean = false
    ): Ret<LPResult>
}
