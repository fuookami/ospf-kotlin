package fuookami.ospf.kotlin.core.intermediate_plugins.hexaly

import fuookami.ospf.kotlin.core.intermediate_model.LinearTriadModel
import fuookami.ospf.kotlin.core.intermediate_model.ModelFileFormat
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.model.Solution
import fuookami.ospf.kotlin.core.intermediate_model.LinearMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.LinearMetaModelF64
import fuookami.ospf.kotlin.core.intermediate_model.RegistrationStatusCallBack
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlinx.coroutines.*

class HexalyColumnGenerationSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: HexalySolverCallBack = HexalySolverCallBack()
) : ColumnGenerationSolver {
    override val name = "hexaly"

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
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

                val solver = HexalyLinearSolver(
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
        metaModel: LinearMetaModel<Flt64>,
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
                val solver = HexalyLinearSolver(
                    config = config,
                    callBack = callBack.copy()
                        .configuration { _, hexaly, _, _ ->
                            ok
                        }
                        .analyzingSolution { _, hexaly, variables, _ ->
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
        metaModel: LinearMetaModel<Flt64>,
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

                var error: Error? = null
                var errors: List<Error>? = null
                try {
                    coroutineScope {
                        val solver = HexalyLinearSolver(
                            config = config,
                            callBack = callBack
                        )
                        val dualSolutionPromises = async(Dispatchers.Default) {
                            val dualModel = model.dual()
                            solver(dualModel)
                        }
                        when (val result = solver(model, solvingStatusCallBack)) {
                            is Ok -> {
                                when (val dualResult = dualSolutionPromises.await()) {
                                    is Ok -> {
                                        metaModel.tokens.setSolution(result.value.solution)
                                        jobs.joinAll()
                                        Ok(
                                            ColumnGenerationSolver.LPResult(
                                                result = result.value,
                                                dualSolution = model.tidyDualSolution(dualResult.value.solution)
                                            )
                                        )
                                    }

                                    is Failed -> {
                                        error = dualResult.error
                                        cancel()
                                        jobs.joinAll()
                                        Failed(dualResult.error)
                                    }

                                    is Fatal -> {
                                        errors = dualResult.errors
                                        cancel()
                                        jobs.joinAll()
                                        Fatal(dualResult.errors)
                                    }
                                }
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                jobs.joinAll()
                                Failed(result.error)
                            }

                            is Fatal -> {
                                errors = result.errors
                                cancel()
                                jobs.joinAll()
                                Fatal(result.errors)
                            }
                        }
                    }
                } catch (_: CancellationException) {
                    errors?.let { Fatal(it) }
                        ?: error?.let { Failed(it) }
                        ?: Failed(Err(ErrorCode.OREngineSolvingException))
                }
            }
        }
    }
}


