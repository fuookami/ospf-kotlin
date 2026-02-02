package fuookami.ospf.kotlin.core.backend.plugins.copt

import java.util.*
import kotlinx.coroutines.*
import copt.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.framework.solver.*

class CplexLinearBendersDecompositionSolver(
    private val config: SolverConfig = SolverConfig(),
    private val linearCallBack: CoptLinearSolverCallBack = CoptLinearSolverCallBack()
) : LinearBendersDecompositionSolver {
    override val name = "copt"

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveMaster(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
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
        }.use { mechanismModel ->
            val model = LinearTriadModel(
                model = mechanismModel,
                fixedVariables = null,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
                forceDumpBounds = config.dumpIntermediateModelForceBounds,
                concurrent = config.dumpIntermediateModelConcurrent
            )
            if (toLogModel) {
                jobs.add(GlobalScope.launch(Dispatchers.IO) {
                    model.export("$name.lp", ModelFileFormat.LP)
                })
            }

            val solver = CoptLinearSolver(
                config = config,
                callBack = linearCallBack.copy()
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
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveSub(
        name: String,
        metaModel: LinearMetaModel,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<LinearBendersDecompositionSolver.LinearSubResult> {
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
            fixedVariables = fixedVariables,
            registrationStatusCallBack = registrationStatusCallBack
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                jobs.joinAll()
                return Failed(result.error)
            }
        }.use { mechanismModel ->
            val model = LinearTriadModel(
                model = mechanismModel,
                fixedVariables = null,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds ?: true,
                forceDumpBounds = config.dumpIntermediateModelForceBounds ?: false,
                concurrent = config.dumpIntermediateModelConcurrent
            )
            model.linearRelax()
            if (toLogModel) {
                jobs.add(GlobalScope.launch(Dispatchers.IO) {
                    model.export("$name.lp", ModelFileFormat.LP)
                })
            }

            lateinit var dualSolution: LinearDualSolution
            lateinit var farkasSolution: LinearDualSolution
            val solver = CoptLinearSolver(
                config = config,
                callBack = linearCallBack.copy()
                    .analyzingSolution { _, _, _, constraints ->
                        dualSolution = model.tidyDualSolution(constraints.map { constraint ->
                            Flt64(constraint.get(COPT.DoubleInfo.Dual))
                        })
                        ok
                    }
                    .afterFailure { status, _, _, constraints ->
                        if (status == SolverStatus.Infeasible) {
                            farkasSolution = model.tidyDualSolution(constraints.map { constraint ->
                                Flt64(constraint.get(COPT.DoubleInfo.DualFarkas))
                            })
                        }
                        ok
                    }
            )

            when (val result = solver(model, solvingStatusCallBack)) {
                is Ok -> {
                    metaModel.tokens.setSolution(model.tokensInSolver.mapIndexed { index, token ->
                        token.variable to result.value.solution[index]
                    }.toMap() + fixedVariables)
                    jobs.joinAll()
                    Ok(
                        LinearBendersDecompositionSolver.LinearFeasibleResult(
                            result = result.value,
                            dualSolution = dualSolution,
                            cuts = mechanismModel.generateOptimalCut(
                                objectVariable = objectVariable,
                                fixedVariables = fixedVariables,
                                dualSolution = dualSolution
                            )
                        )
                    )
                }

                is Failed -> {
                    jobs.joinAll()
                    if (result.error.code == ErrorCode.ORModelInfeasible) {
                        Ok(
                            LinearBendersDecompositionSolver.LinearInfeasibleResult(
                                farkasDualSolution = farkasSolution,
                                cuts = mechanismModel.generateFeasibleCut(
                                    fixedVariables = fixedVariables,
                                    farkasDualSolution = farkasSolution
                                )
                            )
                        )
                    } else {
                        Failed(result.error)
                    }
                }
            }
        }
    }
}

class CoptLinearBendersDecompositionSolver(
    private val config: SolverConfig = SolverConfig(),
    private val linearCallBack: CoptLinearSolverCallBack = CoptLinearSolverCallBack(),
    private val quadraticCallBack: CoptQuadraticSolverCallBack = CoptQuadraticSolverCallBack()
) : QuadraticBendersDecompositionSolver {
    override val name = "copt"
    private val linear = CplexLinearBendersDecompositionSolver(config, linearCallBack)

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveMaster(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        return linear.solveMaster(name, metaModel, toLogModel, registrationStatusCallBack)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveMaster(
        name: String,
        metaModel: QuadraticMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(GlobalScope.launch(Dispatchers.IO) {
                metaModel.export("$name.opm")
            })
        }
        return when (val result = QuadraticMechanismModel(
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
        }.use { mechanismModel ->
            val model = QuadraticTetradModel(
                model = mechanismModel,
                fixedVariables = null,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
                forceDumpBounds = config.dumpIntermediateModelForceBounds,
                concurrent = config.dumpIntermediateModelConcurrent
            )
            if (toLogModel) {
                jobs.add(GlobalScope.launch(Dispatchers.IO) {
                    model.export("$name.lp", ModelFileFormat.LP)
                })
            }

            val solver = CoptQuadraticSolver(
                config = config,
                callBack = quadraticCallBack.copy()
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
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveSub(
        name: String,
        metaModel: LinearMetaModel,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<LinearBendersDecompositionSolver.LinearSubResult> {
        return linear.solveSub(name, metaModel, objectVariable, fixedVariables, toLogModel, registrationStatusCallBack)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveSub(
        name: String,
        metaModel: QuadraticMetaModel,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<QuadraticBendersDecompositionSolver.QuadraticSubResult> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(GlobalScope.launch(Dispatchers.IO) {
                metaModel.export("$name.opm")
            })
        }
        return when (val result = QuadraticMechanismModel(
            metaModel = metaModel,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            fixedVariables = fixedVariables,
            registrationStatusCallBack = registrationStatusCallBack
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                jobs.joinAll()
                return Failed(result.error)
            }
        }.use { mechanismModel ->
            val model = QuadraticTetradModel(
                model = mechanismModel,
                fixedVariables = fixedVariables,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds ?: true,
                forceDumpBounds = config.dumpIntermediateModelForceBounds ?: false,
                concurrent = config.dumpIntermediateModelConcurrent
            )

            model.linearRelax()
            if (toLogModel) {
                jobs.add(GlobalScope.launch(Dispatchers.IO) {
                    model.export("$name.lp", ModelFileFormat.LP)
                })
            }

            lateinit var dualSolution: QuadraticDualSolution
            lateinit var farkasSolution: QuadraticDualSolution
            val solver = CoptQuadraticSolver(
                config = config,
                callBack = quadraticCallBack.copy()
                    .analyzingSolution { _, _, _, constraints ->
                        dualSolution = model.tidyDualSolution(constraints.map { constraint ->
                            Flt64(constraint.get(COPT.DoubleInfo.Dual))
                        })
                        ok
                    }
                    .afterFailure { status, _, _, constraints ->
                        if (status == SolverStatus.Infeasible) {
                            farkasSolution = model.tidyDualSolution(constraints.map { constraint ->
                                Flt64(constraint.get(COPT.DoubleInfo.DualFarkas))
                            })
                        }
                        ok
                    }
            )

            when (val result = solver(model, solvingStatusCallBack)) {
                is Ok -> {
                    metaModel.tokens.setSolution(model.tokensInSolver.mapIndexed { index, token ->
                        token.variable to result.value.solution[index]
                    }.toMap() + fixedVariables)
                    jobs.joinAll()
                    val cuts = when (val result = mechanismModel.generateOptimalCut(
                        objective = result.value.obj,
                        objectVariable = objectVariable,
                        fixedVariables = fixedVariables,
                        dualSolution = dualSolution
                    )) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                    Ok(
                        QuadraticBendersDecompositionSolver.QuadraticFeasibleResult(
                            result = result.value,
                            dualSolution = dualSolution,
                            linearCuts = cuts.filterIsInstance<LinearInequality>(),
                            quadraticCuts = cuts.filterIsInstance<QuadraticInequality>()
                        )
                    )
                }

                is Failed -> {
                    jobs.joinAll()
                    if (result.error.code == ErrorCode.ORModelInfeasible) {
                        val cuts = when (val result = mechanismModel.generateFeasibleCut(
                            fixedVariables = fixedVariables,
                            farkasDualSolution = farkasSolution
                        )) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                return Failed(result.error)
                            }
                        }
                        Ok(
                            QuadraticBendersDecompositionSolver.QuadraticInfeasibleResult(
                                farkasDualSolution = farkasSolution,
                                linearCuts = cuts.filterIsInstance<LinearInequality>(),
                                quadraticCuts = cuts.filterIsInstance<QuadraticInequality>()
                            )
                        )
                    } else {
                        Failed(result.error)
                    }
                }
            }
        }
    }
}
