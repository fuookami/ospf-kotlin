package fuookami.ospf.kotlin.core.backend.plugins.cplex

import java.util.*
import kotlinx.coroutines.*
import ilog.cplex.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.framework.solver.*

class CplexColumnGenerationSolver(
    val config: LinearSolverConfig = LinearSolverConfig(),
    val callBack: CplexSolverCallBack = CplexSolverCallBack()
) : ColumnGenerationSolver {
    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean
    ): Ret<LinearSolverOutput> {
        if (toLogModel) {
            GlobalScope.launch(Dispatchers.IO) { metaModel.export("$name.opm") }
        }
        val model = when (val result = LinearModel(metaModel)) {
            is Ok -> {
                LinearTriadModel(result.value)
            }

            is Failed -> {
                return Failed(result.error)
            }
        }
        if (toLogModel) {
            GlobalScope.launch(Dispatchers.IO) { model.export("$name.lp", ModelFileFormat.LP) }
        }

        val solver = CplexLinearSolver(
            config = config,
            callBack = callBack.copy()
        )

        return when (val result = solver(model)) {
            is Ok -> {
                metaModel.tokens.setSolution(result.value.solution)
                Ok(result.value)
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel,
        amount: UInt64,
        toLogModel: Boolean
    ): Ret<Pair<LinearSolverOutput, List<Solution>>> {
        if (toLogModel) {
            GlobalScope.launch(Dispatchers.IO) { metaModel.export("$name.opm") }
        }
        val model = when (val result = LinearModel(metaModel)) {
            is Ok -> {
                LinearTriadModel(result.value)
            }

            is Failed -> {
                return Failed(result.error)
            }
        }
        if (toLogModel) {
            GlobalScope.launch(Dispatchers.IO) { model.export("$name.lp", ModelFileFormat.LP) }
        }

        val results = ArrayList<Solution>()
        val solver = CplexLinearSolver(
            config = config,
            callBack = callBack.copy()
                .configuration { cplex, _, _ ->
                    cplex.setParam(IloCplex.Param.MIP.Pool.Capacity, min(UInt64.ten, amount).toInt())
                }.analyzingSolution { cplex, variables, _ ->
                    val solutionAmount = cplex.solnPoolNsolns
                    for (i in 0 until solutionAmount) {
                        val thisResults = variables.map { Flt64(cplex.getValue(it, i)) }
                        if (!results.any { it.toTypedArray() contentEquals thisResults.toTypedArray() }) {
                            results.add(thisResults)
                        }
                    }
                }
        )

        return when (val result = solver(model)) {
            is Ok -> {
                metaModel.tokens.setSolution(result.value.solution)
                results.add(0, result.value.solution)
                Ok(Pair(result.value, results))
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveLP(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean
    ): Ret<ColumnGenerationSolver.LPResult> {
        if (toLogModel) {
            GlobalScope.launch(Dispatchers.IO) { metaModel.export("$name.opm") }
        }
        val model = when (val result = LinearModel(metaModel)) {
            is Ok -> {
                LinearTriadModel(result.value)
            }

            is Failed -> {
                return Failed(result.error)
            }
        }
        model.linearRelax()
        if (toLogModel) {
            GlobalScope.launch(Dispatchers.IO) { model.export("$name.lp", ModelFileFormat.LP) }
        }

        lateinit var dualSolution: Solution
        val solver = CplexLinearSolver(
            config = config,
            callBack = callBack.copy()
                .analyzingSolution { cplex, _, constraints ->
                    dualSolution = constraints.map { Flt64(cplex.getDual(it)) }
                }
        )

        return when (val result = solver(model)) {
            is Ok -> {
                metaModel.tokens.setSolution(result.value.solution)
                Ok(ColumnGenerationSolver.LPResult(result.value, dualSolution))
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }
}
