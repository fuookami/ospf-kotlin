package fuookami.ospf.kotlin.core.backend.plugins.mindopt

import java.util.*
import kotlinx.coroutines.*
import com.alibaba.damo.mindopt.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.framework.solver.*

class MindOPTColumnGenerationSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: MindOPTLinearSolverCallBack = MindOPTLinearSolverCallBack()
) : ColumnGenerationSolver {
    override val name = "mindopt"

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean,
        statusCallBack: SolvingStatusCallBack?
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

        val solver = MindOPTLinearSolver(
            config = config,
            callBack = callBack.copy()
        )

        return when (val result = solver(model, statusCallBack)) {
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
        toLogModel: Boolean,
        statusCallBack: SolvingStatusCallBack?
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
        val solver = MindOPTLinearSolver(
            config = config,
            callBack = callBack.copy()
                .configuration { mindopt, _, _ ->
                    if (amount gr UInt64.one) {
                        mindopt.set(MDO.IntParam.MIP_SolutionPoolSize, amount.toInt())
                    }
                    ok
                }.analyzingSolution { mindopt, variables, _ ->
                    for (i in 0 until kotlin.math.min(amount.toInt(), mindopt.get(MDO.IntAttr.SolCount))) {
                        mindopt.set(MDO.IntParam.MIP_SolutionNumber, i)
                        val thisResults = variables.map { Flt64(it.get(MDO.DoubleAttr.Xn)) }
                        if (!results.any { it.toTypedArray() contentEquals thisResults.toTypedArray() }) {
                            results.add(thisResults)
                        }
                    }
                    ok
                }
        )

        return when (val result = solver(model, statusCallBack)) {
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
        toLogModel: Boolean,
        statusCallBack: SolvingStatusCallBack?
    ): Ret<ColumnGenerationSolver.LPResult> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(GlobalScope.launch(Dispatchers.IO) { metaModel.export("$name.opm") })
        }
        val model = when (val result = LinearMechanismModel(metaModel, config.dumpMechanismModelConcurrent)) {
            is Ok -> {
                LinearTriadModel(result.value, config.dumpIntermediateModelConcurrent)
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

        lateinit var dualSolution: Solution
        val solver = MindOPTLinearSolver(
            config = config,
            callBack = callBack.copy()
                .analyzingSolution { _, _, constraints ->
                    dualSolution = constraints.map { Flt64(it.get(MDO.DoubleAttr.DualSoln)) }
                    ok
                }
        )

        return when (val result = solver(model, statusCallBack)) {
            is Ok -> {
                metaModel.tokens.setSolution(result.value.solution)
                jobs.forEach { it.join() }
                Ok(ColumnGenerationSolver.LPResult(result.value, dualSolution))
            }

            is Failed -> {
                jobs.forEach { it.join() }
                Failed(result.error)
            }
        }
    }
}
