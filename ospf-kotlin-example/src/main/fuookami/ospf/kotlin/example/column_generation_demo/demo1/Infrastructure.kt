package fuookami.ospf.kotlin.example.column_generation_demo.demo1

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.parallel.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.plugins.scip.*
import fuookami.ospf.kotlin.core.backend.plugins.scip.SCIPLinearSolver

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
        val solver = SCIPLinearSolver(LinearSolverConfig())
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

data class LPResult(
    val result: List<Flt64>,
    val dualResult: List<Flt64>
)

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
