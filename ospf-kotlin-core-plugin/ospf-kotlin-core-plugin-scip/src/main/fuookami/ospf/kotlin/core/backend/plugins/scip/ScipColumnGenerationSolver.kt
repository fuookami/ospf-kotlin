package fuookami.ospf.kotlin.core.backend.plugins.scip

import java.util.*
import kotlinx.coroutines.*
import jscip.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.framework.solver.*

class ScipColumnGenerationSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: ScipSolverCallBack = ScipSolverCallBack()
) : ColumnGenerationSolver {
    override val name = "scip"

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(GlobalScope.launch(Dispatchers.IO) {
                metaModel.export("$name.opm")
            })
        }
        val model = when (val result = LinearMechanismModel(
            metaModel = metaModel,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            registrationStatusCallBack = registrationStatusCallBack
        )) {
            is Ok -> {
                LinearTriadModel(
                    model = result.value,
                    fixedVariables = null,
                    dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
                    forceDumpBounds = config.dumpIntermediateModelForceBounds,
                    concurrent = config.dumpIntermediateModelConcurrent
                )
            }

            is Failed -> {
                jobs.joinAll()
                return Failed(result.error)
            }
        }
        if (toLogModel) {
            jobs.add(GlobalScope.launch(Dispatchers.IO) {
                model.export("$name.lp", ModelFileFormat.LP)
            })
        }

        val solver = ScipLinearSolver(
            config = config,
            callBack = callBack.copy()
        )

        return when (val result = solver(model, solvingStatusCallBack)) {
            is Ok -> {
                metaModel.tokens.setSolution(result.value.solution)
                jobs.joinAll()
                Ok(result.value)
            }

            is Failed -> {
                jobs.joinAll()
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
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(GlobalScope.launch(Dispatchers.IO) {
                metaModel.export("$name.opm")
            })
        }
        val model = when (val result = LinearMechanismModel(
            metaModel = metaModel,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            registrationStatusCallBack = registrationStatusCallBack
        )) {
            is Ok -> {
                LinearTriadModel(
                    model = result.value,
                    fixedVariables = null,
                    dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
                    forceDumpBounds = config.dumpIntermediateModelForceBounds,
                    concurrent = config.dumpIntermediateModelConcurrent
                )
            }

            is Failed -> {
                jobs.joinAll()
                return Failed(result.error)
            }
        }
        if (toLogModel) {
            jobs.add(GlobalScope.launch(Dispatchers.IO) {
                model.export("$name.lp", ModelFileFormat.LP)
            })
        }

        val results = ArrayList<Solution>()
        val solver = ScipLinearSolver(
            config = config,
            callBack = callBack.copy()
                .configuration { _, scip, _, _ ->
                    if (amount gr UInt64.one) {
                        scip.setIntParam("heuristics/dins/solnum", amount.toInt())
                    }
                    ok
                }
                .analyzingSolution { _, scip, variables, _ ->
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

        return when (val result = solver(model, solvingStatusCallBack)) {
            is Ok -> {
                metaModel.tokens.setSolution(result.value.solution)
                results.add(0, result.value.solution)
                jobs.joinAll()
                Ok(Pair(result.value, results))
            }

            is Failed -> {
                jobs.joinAll()
                Failed(result.error)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveLP(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<ColumnGenerationSolver.LPResult> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(GlobalScope.launch(Dispatchers.IO) {
                metaModel.export("$name.opm")
            })
        }
        val model = when (val result = LinearMechanismModel(
            metaModel = metaModel,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            registrationStatusCallBack = registrationStatusCallBack
        )) {
            is Ok -> {
                LinearTriadModel(
                    model = result.value,
                    fixedVariables = null,
                    dumpConstraintsToBounds = config.dumpIntermediateModelBounds ?: false,
                    forceDumpBounds = config.dumpIntermediateModelForceBounds ?: false,
                    concurrent = config.dumpIntermediateModelConcurrent
                )
            }

            is Failed -> {
                jobs.joinAll()
                return Failed(result.error)
            }
        }
        model.linearRelax()
        if (toLogModel) {
            jobs.add(GlobalScope.launch(Dispatchers.IO) {
                model.export("$name.lp", ModelFileFormat.LP)
            })
        }

        lateinit var dualSolution: LinearDualSolution
        val solver = ScipLinearSolver(
            config = config.copy(
                threadNum = UInt64.one
            ),
            callBack = callBack.copy()
                .configuration { _, model, _, _ ->
                    model.setPresolving(SCIP_ParamSetting.SCIP_PARAMSETTING_OFF, true)
                    model.setHeuristics(SCIP_ParamSetting.SCIP_PARAMSETTING_OFF, true)
                    ok
                }
                .analyzingSolution { _, scipModel, _, constraints ->
                    dualSolution = model.tidyDualSolution(constraints.map {
                        Flt64(scipModel.getDual(it))
                    })
                    ok
                }
        )

        return when (val result = solver(model, solvingStatusCallBack)) {
            is Ok -> {
                metaModel.tokens.setSolution(result.value.solution)
                val dualObject = dualSolution.sumOf { (constraint, value) ->
                    constraint.rhs * value
                }
                if (abs(dualObject - result.value.obj) gr Flt64(1e-6)) {
                    // there may bse some configuration is not be properly set, sometimes the dual solution is not accurate, so we need to re-solve the dual problem to get dual solution
                    when (val result = solveDual(model, ScipLinearSolver(config))) {
                        is Ok -> {
                            dualSolution = result.value
                        }

                        is Failed -> {
                            jobs.joinAll()
                            return Failed(result.error)
                        }
                    }
                }
                jobs.joinAll()
                Ok(
                    ColumnGenerationSolver.LPResult(
                        result = result.value,
                        dualSolution = dualSolution
                    )
                )
            }

            is Failed -> {
                jobs.joinAll()
                Failed(result.error)
            }
        }
    }
}
