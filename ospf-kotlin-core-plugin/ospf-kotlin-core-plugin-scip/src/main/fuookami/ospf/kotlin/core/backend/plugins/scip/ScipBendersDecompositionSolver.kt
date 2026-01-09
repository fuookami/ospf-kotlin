package fuookami.ospf.kotlin.core.backend.plugins.scip

import java.util.*
import kotlinx.coroutines.*
import jscip.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.framework.solver.*

class ScipLinearBendersDecompositionSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: ScipSolverCallBack = ScipSolverCallBack()
) : LinearBendersDecompositionSolver {
    override val name = "scip"

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
        val (mechanismModel, model) = when (val result = LinearMechanismModel(
            metaModel = metaModel,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            fixedVariables = fixedVariables,
            registrationStatusCallBack = registrationStatusCallBack
        )) {
            is Ok -> {
                result.value to LinearTriadModel(
                    model = result.value,
                    fixedVariables = null,
                    dumpConstraintsToBounds = config.dumpIntermediateModelBounds ?: true,
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
        lateinit var farkasSolution: LinearDualSolution
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
                .afterFailure { status, _, _, _ ->
                    if (status == SolverStatus.Infeasible) {
                        when (val result = solveFarkasDual(model, ScipLinearSolver(config))) {
                            is Ok -> {
                                farkasSolution = result.value
                            }

                            is Failed -> {
                                return@afterFailure Failed(result.error)
                            }
                        }
                    }
                    ok
                }
        )

        return when (val result = solver(model, solvingStatusCallBack)) {
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
                    }
                }
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

class ScipQuadraticBendersDecompositionSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: ScipSolverCallBack = ScipSolverCallBack()
) : QuadraticBendersDecompositionSolver {
    override val name = "scip"
    private val linear = ScipLinearBendersDecompositionSolver(config, callBack)

    override suspend fun solveMaster(
        name: String,
        metaModel: LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        return linear.solveMaster(name, metaModel, toLogModel, registrationStatusCallBack, solvingStatusCallBack)
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
        val model = when (val result = QuadraticMechanismModel(
            metaModel = metaModel,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            registrationStatusCallBack = registrationStatusCallBack
        )) {
            is Ok -> {
                QuadraticTetradModel(
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

        val solver = ScipQuadraticSolver(
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

    override suspend fun solveSub(
        name: String,
        metaModel: LinearMetaModel,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<LinearBendersDecompositionSolver.LinearSubResult> {
        return linear.solveSub(name, metaModel, objectVariable, fixedVariables, toLogModel, registrationStatusCallBack, solvingStatusCallBack)
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
        val (mechanismModel, model) = when (val result = QuadraticMechanismModel(
            metaModel = metaModel,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            fixedVariables = fixedVariables,
            registrationStatusCallBack = registrationStatusCallBack
        )) {
            is Ok -> {
                result.value to QuadraticTetradModel(
                    model = result.value,
                    fixedVariables = fixedVariables,
                    dumpConstraintsToBounds = config.dumpIntermediateModelBounds ?: true,
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

        lateinit var dualSolution: QuadraticDualSolution
        lateinit var farkasSolution: QuadraticDualSolution
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
                    dualSolution = model.tidyDualSolution(constraints.map {
                        Flt64(scipModel.getDual(it))
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
                        }
                    }
                    ok
                }
        )

        return when (val result = solver(model, solvingStatusCallBack)) {
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
                    }
                }
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
