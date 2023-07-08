package fuookami.ospf.kotlin.example.column_generation_demo.demo1

import kotlinx.coroutines.*
import ilog.cplex.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.parallel.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.plugins.scip.*
import fuookami.ospf.kotlin.core.backend.plugins.cplex.*

@OptIn(DelicateCoroutinesApi::class)
internal suspend fun solveMIP(name: String, metaModel: LinearMetaModel): Result<List<Flt64>, Error> {
    GlobalScope.launch(Dispatchers.IO) { metaModel.export("$name.opm") }

    val results = ArrayList<List<Flt64>>()
    val callBack = CplexSolverCallBack()
        .configuration { cplex, _, _ -> cplex.setParam(IloCplex.Param.MIP.Pool.Capacity, 10) }
        .analyzingSolution { cplex, variables, _ ->
            val solutionAmount = cplex.solnPoolNsolns
            for (i in 0 until solutionAmount) {
                results.add(variables.map { Flt64(cplex.getValue(it, i)) })
            }
        }
    val solver = CplexLinearSolver(LinearSolverConfig(), callBack)
    val model = LinearTriadModel(LinearModel(metaModel))
    GlobalScope.launch(Dispatchers.IO) { model.export("$name.lp", ModelFileFormat.LP) }

    return when (val ret = solver(model)) {
        is Ok -> {
            metaModel.tokens.setSolution(ret.value.results)
            Ok(ret.value.results)
        }

        is Failed -> {
            Failed(ret.error)
        }
    }
}

/*
internal fun solveMIP(name: String, metaModel: LinearMetaModel): Result<List<Flt64>, Error> {
    ThreadGuard(Thread {
        metaModel.export("$name.opm")
    }).use {
        //    val results = ArrayList<List<Flt64>>()
//    val callBack = SCIPSolverCallBack().analyzingSolution { scip, variables, _ ->
//        val bestSol = scip.bestSol
//        val sols = scip.sols
//        for (sol in sols) {
//            if (sol != bestSol) {
//                val thisResults = ArrayList<Flt64>()
//                for (scipVar in variables) {
//                    thisResults.add(Flt64(scip.getSolVal(sol, scipVar)))
//                }
//                results.add(thisResults)
//            }
//        }
//    }
//    val solver = SCIPLinearSolver(LinearSolverConfig(), callBack)
        // val solver = SCIPLinearSolver(LinearSolverConfig())
        val solver = CplexLinearSolver(LinearSolverConfig())
        val model = LinearTriadModel(LinearModel(metaModel))
        ThreadGuard(Thread {
            model.export("$name.lp", ModelFileFormat.LP)
        }).use {
            return when (val ret = solver(model)) {
                is Ok -> {
                    metaModel.tokens.setSolution(ret.value.results)
                    Ok(ret.value.results)
                }

                is Failed -> {
                    Failed(ret.error)
                }
            }
        }
    }
}
*/

data class LPResult(
    val result: List<Flt64>,
    val dualResult: List<Flt64>
)

@OptIn(DelicateCoroutinesApi::class)
internal suspend fun solveLP(name: String, metaModel: LinearMetaModel): Result<LPResult, Error> {
    GlobalScope.launch(Dispatchers.IO) { metaModel.export("$name.opm") }

    lateinit var dualResult: List<Flt64>
    val callBack = CplexSolverCallBack().analyzingSolution { cplex, _, constraints ->
        dualResult = constraints.map { Flt64(cplex.getDual(it)) }
    }

    val solver = CplexLinearSolver(LinearSolverConfig(), callBack)
    val model = LinearTriadModel(LinearModel(metaModel))
    GlobalScope.launch(Dispatchers.IO) { model.export("$name.lp", ModelFileFormat.LP) }

    model.linearRelax()

    return when (val ret = solver(model)) {
        is Ok -> {
            metaModel.tokens.setSolution(ret.value.results)
            Ok(LPResult(ret.value.results, dualResult))
        }

        is Failed -> {
            Failed(ret.error)
        }
    }
}

/*
internal fun solveLP(name: String, metaModel: LinearMetaModel): Result<LPResult, Error> {
    ThreadGuard(Thread {
        metaModel.export("$name.opm")
    }).use {
        val solver = SCIPLinearSolver(LinearSolverConfig())
        val model = LinearTriadModel(LinearModel(metaModel))
        model.linearRelax()
        lateinit var dualModel: LinearTriadModel

        ThreadGuard(Thread {
            model.export("$name.lp", ModelFileFormat.LP)
        }).use {
            val dualModelGenerator = ThreadGuard(Thread {
                val temp = model.clone()
                temp.normalize()
                dualModel = temp.dual()
            })

            val ret = solver(model)
            dualModelGenerator.join()

            if (ret is Failed) {
                return Failed(ret.error)
            }

            ThreadGuard(Thread {
                dualModel.export("$name-dual.lp", ModelFileFormat.LP)
            }).use {
                return when (val dualRet = solver(dualModel)) {
                    is Ok -> {
                        metaModel.tokens.setSolution(ret.value()!!.results)
                        Ok(LPResult(ret.value()!!.results, dualRet.value.results))
                    }

                    is Failed -> {
                        Failed(dualRet.error)
                    }
                }
            }
        }
    }
}
*/
