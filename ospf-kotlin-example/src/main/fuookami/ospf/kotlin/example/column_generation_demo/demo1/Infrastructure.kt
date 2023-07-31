package fuookami.ospf.kotlin.example.column_generation_demo.demo1

import java.util.*
import kotlinx.coroutines.*
import ilog.cplex.*
import ilog.cplex.IloCplex.Param.*
import gurobi.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.parallel.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.plugins.scip.*
import fuookami.ospf.kotlin.core.backend.plugins.cplex.*
import fuookami.ospf.kotlin.core.backend.plugins.gurobi.*

data class SolverParameter(
    val solver: String = if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")) {
        "cplex"
    } else {
        "gurobi"
    }
)

@OptIn(DelicateCoroutinesApi::class)
suspend fun solveMIP(name: String, metaModel: LinearMetaModel, parameter: SolverParameter = SolverParameter(), config: LinearSolverConfig = LinearSolverConfig()): Result<List<Flt64>, Error> {
    // GlobalScope.launch(Dispatchers.IO) { metaModel.export("$name.opm") }
    val model = LinearTriadModel(LinearModel(metaModel))
    // GlobalScope.launch(Dispatchers.IO) { model.export("$name.lp", ModelFileFormat.LP) }
    val ret = when (parameter.solver) {
        "cplex" -> {
            val solver = CplexLinearSolver(config)
            solver(model)
        }

        "gurobi" -> {
            val solver = GurobiLinearSolver(config)
            solver(model)
        }

        "scip" -> {
            val solver = SCIPLinearSolver(config)
            solver(model)
        }

        else -> {
            return Failed(Err(ErrorCode.SolverNotFound, parameter.solver))
        }
    }
    return when (ret) {
        is Ok -> {
            metaModel.tokens.setSolution(ret.value.results)
            Ok(ret.value.results)
        }

        is Failed -> {
            Failed(ret.error)
        }
    }
}

suspend fun solveMIP(name: String, metaModel: LinearMetaModel, amount: UInt64, parameter: SolverParameter= SolverParameter()): Result<List<List<Flt64>>, Error> {
    val results = ArrayList<List<Flt64>>()
    val model = LinearTriadModel(LinearModel(metaModel))

    val ret = when (parameter.solver) {
        "cplex" -> {
            val callBack = CplexSolverCallBack()
                .configuration { cplex, _, _ ->
                    cplex.setParam(MIP.Pool.Capacity, min(UInt64.ten, amount).toInt())
                }.analyzingSolution { cplex, variables, _ ->
                    val solutionAmount = cplex.solnPoolNsolns
                    for (i in 0 until solutionAmount) {
                        results.add(variables.map { Flt64(cplex.getValue(it, i)) })
                    }
                }
            val solver = CplexLinearSolver(LinearSolverConfig(), callBack)
            solver(model)
        }

        "gurobi" -> {
            val callBack = GurobiSolverCallBack()
                .configuration { gurobi, _, _ ->
                    gurobi.set(GRB.IntParam.PoolSolutions, amount.toInt())
                }.analyzingSolution { gurobi, variables, _ ->
                    for (i in 0 until gurobi.get(GRB.IntAttr.SolCount)) {
                        gurobi.set(GRB.IntParam.SolutionNumber, i)
                        results.add(variables.map { Flt64(it.get(GRB.DoubleAttr.Xn)) })
                    }
                }
            val solver = GurobiLinearSolver(LinearSolverConfig(), callBack)
            solver(model)
        }

        "scip" -> {
            val callBack = SCIPSolverCallBack()
                .configuration { scip, _, _ ->
                    scip.setIntParam("heuristics/dins/solnum", min(UInt64.ten, amount).toInt())
                }
                .analyzingSolution { scip, variables, _ ->
                    val bestSol = scip.bestSol
                    val sols = scip.sols
                    var i = UInt64.zero
                    for (sol in sols) {
                        if (sol != bestSol) {
                            val thisResults = ArrayList<Flt64>()
                            for (scipVar in variables) {
                                thisResults.add(Flt64(scip.getSolVal(sol, scipVar)))
                            }
                            results.add(thisResults)
                        }
                        ++i
                        if (i >= amount) {
                            break
                        }
                    }
                }
            val solver = SCIPLinearSolver(LinearSolverConfig(), callBack)
            solver(model)
        }

        else -> {
            return Failed(Err(ErrorCode.SolverNotFound, parameter.solver))
        }
    }
    return when (ret) {
        is Ok -> {
            metaModel.tokens.setSolution(ret.value.results)
            results.add(0, ret.value.results)
            Ok(results)
        }

        is Failed -> {
            Failed(ret.error)
        }
    }
}

data class LPResult(
    val obj: Flt64,
    val result: List<Flt64>,
    val dualResult: List<Flt64>
)

suspend fun solveLP(name: String, metaModel: LinearMetaModel, parameter: SolverParameter = SolverParameter()): Result<LPResult, Error> {
    // GlobalScope.launch(Dispatchers.IO) { metaModel.export("$name.opm") }
    lateinit var dualResult: List<Flt64>
    val model = LinearTriadModel(LinearModel(metaModel))
    model.linearRelax()
    // GlobalScope.launch(Dispatchers.IO) { model.export("$name.lp", ModelFileFormat.LP) }
    val ret = when (parameter.solver) {
        "cplex" -> {
            val callBack = CplexSolverCallBack().analyzingSolution { cplex, _, constraints ->
                dualResult = constraints.map { Flt64(cplex.getDual(it)) }
            }
            val solver = CplexLinearSolver(LinearSolverConfig(), callBack)
            solver(model)
        }

        "gurobi" -> {
            val callBack = GurobiSolverCallBack().analyzingSolution { _, _, constraints ->
                dualResult = constraints.map { Flt64(it.get(GRB.DoubleAttr.Pi)) }
            }
            val solver = GurobiLinearSolver(LinearSolverConfig(), callBack)
            solver(model)
        }

        "scip" -> {
            coroutineScope {
                val solver = SCIPLinearSolver(LinearSolverConfig())
                val promise = async(Dispatchers.Default) {
                    val temp = model.copy()
                    temp.normalize()
                    temp.dual()
                }

                val ret = solver(model)
                if (ret is Failed) {
                    return@coroutineScope Failed(ret.error)
                }
                val dualModel = promise.await()

                when (val dualRet = solver(dualModel)) {
                    is Ok -> {
                        dualResult = dualRet.value.results
                    }

                    is Failed -> {
                        return@coroutineScope Failed(dualRet.error)
                    }
                }
                ret
            }
        }

        else -> {
            return Failed(Err(ErrorCode.SolverNotFound, parameter.solver))
        }
    }

    return when (ret) {
        is Ok -> {
            metaModel.tokens.setSolution(ret.value.results)
            Ok(LPResult(ret.value.obj, ret.value.results, dualResult))
        }

        is Failed -> {
            Failed(ret.error)
        }
    }
}

