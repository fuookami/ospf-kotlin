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
    private val config: SolverConfig = SolverConfig(),
    private val callBack: SCIPSolverCallBack = SCIPSolverCallBack()
) : ColumnGenerationSolver {
    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean
    ): Ret<SolverOutput> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(GlobalScope.launch(Dispatchers.IO) { metaModel.export("$name.opm") })
        }
        val model = when (val result = LinearMechanismModel(metaModel)) {
            is Ok -> {
                LinearTriadModel(result.value)
            }

            is Failed -> {
                jobs.forEach { it.join() }
                return Failed(result.error)
            }
        }
        if (toLogModel) {
            jobs.add(GlobalScope.launch(Dispatchers.IO) { model.export("$name.lp", ModelFileFormat.LP) })
        }

        val solver = SCIPLinearSolver(
            config = config,
            callBack = callBack.copy()
        )

        return when (val result = solver(model)) {
            is Ok -> {
                metaModel.tokens.setSolution(result.value.solution)
                jobs.forEach { it.join() }
                Ok(result.value)
            }

            is Failed -> {
                jobs.forEach { it.join() }
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
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(GlobalScope.launch(Dispatchers.IO) { metaModel.export("$name.opm") })
        }
        val model = when (val result = LinearMechanismModel(metaModel)) {
            is Ok -> {
                LinearTriadModel(result.value)
            }

            is Failed -> {
                jobs.forEach { it.join() }
                return Failed(result.error)
            }
        }
        if (toLogModel) {
            jobs.add(GlobalScope.launch(Dispatchers.IO) { model.export("$name.lp", ModelFileFormat.LP) })
        }

        val results = ArrayList<Solution>()
        val solver = SCIPLinearSolver(
            config = config,
            callBack = callBack.copy()
                .configuration { scip, _, _ ->
                    scip.setIntParam("heuristics/dins/solnum", min(UInt64.ten, amount).toInt())
                    ok
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
                            if (!results.any { it.toTypedArray() contentEquals thisResults.toTypedArray() }) {
                                results.add(thisResults)
                            }
                        }
                        ++i
                        if (i >= amount) {
                            break
                        }
                    }
                    ok
                }
        )

        return when (val result = solver(model)) {
            is Ok -> {
                metaModel.tokens.setSolution(result.value.solution)
                results.add(0, result.value.solution)
                jobs.forEach { it.join() }
                Ok(Pair(result.value, results))
            }

            is Failed -> {
                jobs.forEach { it.join() }
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
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(GlobalScope.launch(Dispatchers.IO) { metaModel.export("$name.opm") })
        }
        val model = when (val result = LinearMechanismModel(metaModel)) {
            is Ok -> {
                LinearTriadModel(result.value)
            }

            is Failed -> {
                jobs.forEach { it.join() }
                return Failed(result.error)
            }
        }
        model.linearRelax()
        if (toLogModel) {
            jobs.add(GlobalScope.launch(Dispatchers.IO) { model.export("$name.lp", ModelFileFormat.LP) })
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
                                jobs.forEach { it.join() }
                                Ok(ColumnGenerationSolver.LPResult(result.value, dualResult.value.solution))
                            }

                            is Failed -> {
                                error = dualResult.error
                                cancel()
                                jobs.forEach { it.join() }
                                Failed(dualResult.error)
                            }
                        }
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        jobs.forEach { it.join() }
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
