package fuookami.ospf.kotlin.core.backend.plugins.gurobi

import java.util.*
import kotlinx.coroutines.*
import gurobi.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.framework.solver.*

class GurobiColumnGenerationSolver(
    private val config: LinearSolverConfig = LinearSolverConfig(),
    private val callBack: GurobiSolverCallBack = GurobiSolverCallBack()
): ColumnGenerationSolver {
    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean
    ): Ret<LinearSolverOutput> {
        if (toLogModel) {
            GlobalScope.launch(Dispatchers.IO) { metaModel.export("$name.opm") }
        }
        val model = LinearTriadModel(LinearModel(metaModel))
        if (toLogModel) {
            GlobalScope.launch(Dispatchers.IO) { model.export("$name.lp", ModelFileFormat.LP) }
        }

        val solver = GurobiLinearSolver(
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
        val model = LinearTriadModel(LinearModel(metaModel))
        if (toLogModel) {
            GlobalScope.launch(Dispatchers.IO) { model.export("$name.lp", ModelFileFormat.LP) }
        }

        val results = ArrayList<Solution>()
        val solver = GurobiLinearSolver(
            config = config,
            callBack = callBack.copy()
                .configuration { gurobi, _, _ ->
                    gurobi.set(GRB.IntParam.PoolSolutions, min(UInt64.ten, amount).toInt())
                }.analyzingSolution { gurobi, variables, _ ->
                    for (i in 0 until gurobi.get(GRB.IntAttr.SolCount)) {
                        gurobi.set(GRB.IntParam.SolutionNumber, i)
                        results.add(variables.map { Flt64(it.get(GRB.DoubleAttr.Xn)) })
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
        val model = LinearTriadModel(LinearModel(metaModel))
        model.linearRelax()
        if (toLogModel) {
            GlobalScope.launch(Dispatchers.IO) { model.export("$name.lp", ModelFileFormat.LP) }
        }

        lateinit var dualSolution: Solution
        val solver = GurobiLinearSolver(
            config = config,
            callBack = callBack.copy()
                .analyzingSolution { _, _, constraints ->
                    dualSolution = constraints.map { Flt64(it.get(GRB.DoubleAttr.Pi)) }
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
