package fuookami.ospf.kotlin.core.backend.plugins.scip

import fuookami.ospf.kotlin.core.backend.intermediate_model.LinearTriadModel
import fuookami.ospf.kotlin.core.backend.intermediate_model.ModelFileFormat
import fuookami.ospf.kotlin.core.backend.intermediate_model.solveDual
import fuookami.ospf.kotlin.core.backend.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.backend.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.backend.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearDualSolution
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.RegistrationStatusCallBack
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.UInt64
import fuookami.ospf.kotlin.utils.operator.abs
import jscip.SCIP_ParamSetting
import kotlinx.coroutines.*

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
        return when (val result = LinearMechanismModel(
            metaModel = metaModel,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            registrationStatusCallBack = registrationStatusCallBack
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                jobs.joinAll()
                return Failed(result.error)
            }

            is Fatal -> {
                jobs.joinAll()
                return Fatal(result.errors)
            }

            is Fatal -> {
                jobs.joinAll()
                return Fatal(result.errors)
            }
        }.use { mechanismModel ->
            LinearTriadModel(
                model = mechanismModel,
                fixedVariables = null,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
                forceDumpBounds = config.dumpIntermediateModelForceBounds,
                concurrent = config.dumpIntermediateModelConcurrent
            ).use { model ->
                if (toLogModel) {
                    jobs.add(GlobalScope.launch(Dispatchers.IO) {
                        model.export("$name.lp", ModelFileFormat.LP)
                    })
                }

                val solver = ScipLinearSolver(
                    config = config,
                    callBack = callBack.copy()
                )

                when (val result = solver(model, solvingStatusCallBack)) {
                    is Ok -> {
                        metaModel.tokens.setSolution(result.value.solution)
                        jobs.joinAll()
                        Ok(result.value)
                    }

                    is Failed -> {
                        jobs.joinAll()
                        Failed(result.error)
                    }

                    is Fatal -> {
                        jobs.joinAll()
                        Fatal(result.errors)
                    }
                }
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
        return when (val result = LinearMechanismModel(
            metaModel = metaModel,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            registrationStatusCallBack = registrationStatusCallBack
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                jobs.joinAll()
                return Failed(result.error)
            }

            is Fatal -> {
                jobs.joinAll()
                return Fatal(result.errors)
            }

            is Fatal -> {
                jobs.joinAll()
                return Fatal(result.errors)
            }
        }.use { mechanismModel ->
            LinearTriadModel(
                model = mechanismModel,
                fixedVariables = null,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
                forceDumpBounds = config.dumpIntermediateModelForceBounds,
                concurrent = config.dumpIntermediateModelConcurrent
            ).use { model ->
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

                when (val result = solver(model, solvingStatusCallBack)) {
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

                    is Fatal -> {
                        jobs.joinAll()
                        Fatal(result.errors)
                    }
                }
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
        return when (val result = LinearMechanismModel(
            metaModel = metaModel,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            registrationStatusCallBack = registrationStatusCallBack
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                jobs.joinAll()
                return Failed(result.error)
            }

            is Fatal -> {
                jobs.joinAll()
                return Fatal(result.errors)
            }

            is Fatal -> {
                jobs.joinAll()
                return Fatal(result.errors)
            }
        }.use { mechanismModel ->
            LinearTriadModel(
                model = mechanismModel,
                fixedVariables = null,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds ?: false,
                forceDumpBounds = config.dumpIntermediateModelForceBounds ?: false,
                concurrent = config.dumpIntermediateModelConcurrent
            ).use { model ->
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
                            dualSolution = model.tidyDualSolution(constraints.map { constraint ->
                                Flt64(scipModel.getDual(constraint))
                            })
                            ok
                        }
                )

                when (val result = solver(model, solvingStatusCallBack)) {
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

                                is Fatal -> {
                                    jobs.joinAll()
                                    return Fatal(result.errors)
                                }

                                is Fatal -> {
                                    jobs.joinAll()
                                    return Fatal(result.errors)
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

                    is Fatal -> {
                        jobs.joinAll()
                        Fatal(result.errors)
                    }
                }
            }
        }
    }
}