package fuookami.ospf.kotlin.core.solver.scip

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.intermediate.solveDual
import fuookami.ospf.kotlin.core.model.intermediate.solveFarkasDual
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.SolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.framework.solver.LinearBendersDecompositionSolver
import fuookami.ospf.kotlin.framework.solver.QuadraticBendersDecompositionSolver
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.operator.abs
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.functional.sumOf
import jscip.SCIP_ParamSetting
import kotlinx.coroutines.*

class ScipLinearBendersDecompositionSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: ScipSolverCallBack = ScipSolverCallBack()
) : LinearBendersDecompositionSolver {
    override val name = "scip"

    override suspend fun solveMaster(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
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
        }.use { mechanismModel ->
            LinearTriadModel(
                model = mechanismModel,
                fixedVariables = null,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
                forceDumpBounds = config.dumpIntermediateModelForceBounds,
                concurrent = config.dumpIntermediateModelConcurrent
            ).use { model ->
                if (toLogModel) {
                    jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
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

    override suspend fun solveSub(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<LinearBendersDecompositionSolver.LinearSubResult> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
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

            is Fatal -> {
                jobs.joinAll()
                return Fatal(result.errors)
            }
        }.use { mechanismModel ->
            LinearTriadModel(
                model = mechanismModel,
                fixedVariables = null,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds ?: true,
                forceDumpBounds = config.dumpIntermediateModelForceBounds ?: false,
                concurrent = config.dumpIntermediateModelConcurrent
            ).use { model ->
                model.linearRelax()
                if (toLogModel) {
                    jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
                        model.export("$name.lp", ModelFileFormat.LP)
                    })
                }

                lateinit var dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
                lateinit var farkasSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
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
                        .afterFailure { status, _, _, _ ->
                            if (status == SolverStatus.Infeasible) {
                                when (val result = solveFarkasDual(model, ScipLinearSolver(config))) {
                                    is Ok -> {
                                        farkasSolution = result.value
                                    }

                                    is Failed -> {
                                        return@afterFailure Failed(result.error)
                                    }

                                    is Fatal -> {
                                        return@afterFailure Fatal(result.errors)
                                    }
                                }
                            }
                            ok
                        }
                )

                when (val result = solver(model, solvingStatusCallBack)) {
                    is Ok -> {
                        metaModel.tokens.setSolution(model.tokensInSolver.mapIndexed { index, token ->
                            token.variable to result.value.solution[index]
                        }.toMap() + fixedVariables)
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
                            }
                        }
                        jobs.joinAll()
                        Ok(
                            LinearBendersDecompositionSolver.LinearFeasibleResult(
                                result = result.value,
                                dualSolution = dualSolution,
                                cuts = mechanismModel.generateFlt64OptimalCut(
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
                                    cuts = mechanismModel.generateFlt64FeasibleCut(
                                        fixedVariables = fixedVariables,
                                        farkasDualSolution = farkasSolution
                                    )
                                )
                            )
                        } else {
                            Failed(result.error)
                        }
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

class ScipQuadraticBendersDecompositionSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: ScipSolverCallBack = ScipSolverCallBack()
) : QuadraticBendersDecompositionSolver {
    override val name = "scip"
    private val linear = ScipLinearBendersDecompositionSolver(config, callBack)

    override suspend fun solveMaster(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        return linear.solveMaster(
            name = name,
            metaModel = metaModel,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    override suspend fun solveMaster(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
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

            is Fatal -> {
                jobs.joinAll()
                return Fatal(result.errors)
            }
        }.use { mechanismModel ->
            QuadraticTetradModel(
                model = mechanismModel,
                fixedVariables = null,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
                forceDumpBounds = config.dumpIntermediateModelForceBounds,
                concurrent = config.dumpIntermediateModelConcurrent
            ).use { model ->
                if (toLogModel) {
                    jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
                        model.export("$name.lp", ModelFileFormat.LP)
                    })
                }

                val solver = ScipQuadraticSolver(
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

    override suspend fun solveSub(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<LinearBendersDecompositionSolver.LinearSubResult> {
        return linear.solveSub(
            name = name,
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    override suspend fun solveSub(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<QuadraticBendersDecompositionSolver.QuadraticSubResult> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
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

            is Fatal -> {
                jobs.joinAll()
                return Fatal(result.errors)
            }
        }.use { mechanismModel ->
            QuadraticTetradModel(
                model = mechanismModel,
                fixedVariables = fixedVariables,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds ?: true,
                forceDumpBounds = config.dumpIntermediateModelForceBounds ?: false,
                concurrent = config.dumpIntermediateModelConcurrent
            ).use { model ->
                model.linearRelax()
                if (toLogModel) {
                    jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
                        model.export("$name.lp", ModelFileFormat.LP)
                    })
                }

                lateinit var dualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>
                lateinit var farkasSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>
                val solver = ScipQuadraticSolver(
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
                        .afterFailure { status, _, _, _ ->
                            if (status == SolverStatus.Infeasible) {
                                when (val result = solveFarkasDual(model, ScipQuadraticSolver(config))) {
                                    is Ok -> {
                                        farkasSolution = result.value
                                    }

                                    is Failed -> {
                                        return@afterFailure Failed(result.error)
                                    }

                                    is Fatal -> {
                                        return@afterFailure Fatal(result.errors)
                                    }
                                }
                            }
                            ok
                        }
                )

                when (val result = solver(model, solvingStatusCallBack)) {
                    is Ok -> {
                        metaModel.tokens.setSolution(model.tokensInSolver.mapIndexed { index, token ->
                            token.variable to result.value.solution[index]
                        }.toMap() + fixedVariables)
                        val dualObject = dualSolution.sumOf { (constraint, value) ->
                            constraint.rhs * value
                        }
                        if (abs(dualObject - result.value.obj) gr Flt64(1e-6)) {
                            // there may bse some configuration is not be properly set, sometimes the dual solution is not accurate, so we need to re-solve the dual problem to get dual solution
                            when (val result = solveDual(model, ScipQuadraticSolver(config))) {
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
                            }
                        }
                        jobs.joinAll()
                        val cuts = when (val result = mechanismModel.generateFlt64OptimalCut(
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

                            is Fatal -> {
                                return Fatal(result.errors)
                            }
                        }
                        Ok(
                            QuadraticBendersDecompositionSolver.QuadraticFeasibleResult(
                                result = result.value,
                                dualSolution = dualSolution,
                                linearCuts = cuts.filterIsInstance<LinearInequality<Flt64>>(),
                                quadraticCuts = cuts.filterIsInstance<QuadraticInequalityOf<Flt64>>()
                            )
                        )
                    }

                    is Failed -> {
                        jobs.joinAll()
                        if (result.error.code == ErrorCode.ORModelInfeasible) {
                            val cuts = when (val result = mechanismModel.generateFlt64FeasibleCut(
                                fixedVariables = fixedVariables,
                                farkasDualSolution = farkasSolution
                            )) {
                                is Ok -> {
                                    result.value
                                }

                                is Failed -> {
                                    return Failed(result.error)
                                }

                                is Fatal -> {
                                    return Fatal(result.errors)
                                }
                            }
                            Ok(
                                QuadraticBendersDecompositionSolver.QuadraticInfeasibleResult(
                                    farkasDualSolution = farkasSolution,
                                    linearCuts = cuts.filterIsInstance<LinearInequality<Flt64>>(),
                                    quadraticCuts = cuts.filterIsInstance<QuadraticInequalityOf<Flt64>>()
                                )
                            )
                        } else {
                            Failed(result.error)
                        }
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
