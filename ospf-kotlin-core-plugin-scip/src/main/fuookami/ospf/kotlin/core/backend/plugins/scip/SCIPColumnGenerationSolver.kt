package fuookami.ospf.kotlin.core.backend.plugins.scip

import java.util.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.framework.solver.*

class SCIPColumnGenerationSolver(
    private val config: LinearSolverConfig = LinearSolverConfig(),
    private val callBack: SCIPSolverCallBack = SCIPSolverCallBack()
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

        val solver = SCIPLinearSolver(
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
        val solver = SCIPLinearSolver(
            config = config,
            callBack = callBack.copy()
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

        var error: Error? = null
        return try {
            coroutineScope {
                val solver = SCIPLinearSolver(
                    config = config,
                    callBack = callBack
                )
                val dualSolutionPromises = async(Dispatchers.Default) {
                    val temp = model.copy()
                    temp.normalize()
                    val dualModel = temp.dual()
                    solver(dualModel)
                }
                when (val result = solver(model)) {
                    is Ok -> {
                        when (val dualResult = dualSolutionPromises.await()) {
                            is Ok -> {
                                metaModel.tokens.setSolution(result.value.solution)
                                Ok(ColumnGenerationSolver.LPResult(result.value, dualResult.value.solution))
                            }

                            is Failed -> {
                                error = dualResult.error
                                cancel()
                                Failed(dualResult.error)
                            }
                        }
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        Failed(result.error)
                    }
                }
            }
        } catch (e: CancellationException) {
            error?.let { Failed(it) }
                ?: Failed(Err(ErrorCode.OREngineSolvingException))
        }
    }
}
