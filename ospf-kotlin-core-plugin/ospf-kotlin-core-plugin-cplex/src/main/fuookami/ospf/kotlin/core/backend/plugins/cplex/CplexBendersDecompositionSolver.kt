package fuookami.ospf.kotlin.core.backend.plugins.cplex

import java.util.*
import kotlinx.coroutines.*
import ilog.cplex.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.framework.solver.*

class CplexBendersDecompositionSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: CplexSolverCallBack = CplexSolverCallBack()
) : BendersDecompositionSolver {
    override val name = "cplex"

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
                LinearTriadModel(result.value, null, config.dumpIntermediateModelConcurrent)
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

        val solver = CplexLinearSolver(
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
                QuadraticTetradModel(result.value, null, config.dumpIntermediateModelConcurrent)
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

        val solver = CplexQuadraticSolver(
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
    ): Ret<BendersDecompositionSolver.LinearSubResult> {
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
                result.value to LinearTriadModel(result.value, fixedVariables, config.dumpIntermediateModelConcurrent)
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

        lateinit var dualSolution: List<Flt64>
        lateinit var farkasSolution: List<Flt64>
        val solver = CplexLinearSolver(
            config = config,
            callBack = callBack.copy()
                .configuration { _, cplex, _, _ ->
                    cplex.setParam(IloCplex.Param.Preprocessing.Dual, 1)
                    ok
                }
                .analyzingSolution { _, cplex, _, constraints ->
                    dualSolution = constraints.map {
                        Flt64(cplex.getDual(it))
                    }
                    ok
                }
                .afterFailure { status, _, _, _ ->
                    if (status == SolverStatus.Infeasible) {
                        when (val result = solveFeasibilityProblem(model)) {
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
                metaModel.tokens.setSolution(model.tokenIndexMap.map { (token, index) ->
                    token.variable to result.value.solution[index]
                }.toMap() + fixedVariables)
                jobs.joinAll()
                Ok(
                    BendersDecompositionSolver.LinearFeasibleResult(
                        result = result.value,
                        dualSolution = dualSolution,
                        cuts = mechanismModel.generateOptimalCut(
                            constants = Flt64.zero,
                            objectVariable = objectVariable,
                            fixedVariables = fixedVariables,
                            dualSolution = dualSolution
                        )
                    )
                )
            }

            is Failed -> {
                jobs.joinAll()
                if (result.error.code == ErrorCode.ORModelNoSolution) {
                    Ok(
                        BendersDecompositionSolver.LinearInfeasibleResult(
                            farkasDualSolution = farkasSolution,
                            cuts = mechanismModel.generateFeasibleCut(
                                constants = Flt64.zero,
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

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun solveSub(
        name: String,
        metaModel: QuadraticMetaModel,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<BendersDecompositionSolver.QuadraticSubResult> {
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
                result.value to QuadraticTetradModel(result.value, fixedVariables, config.dumpIntermediateModelConcurrent)
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

        lateinit var dualSolution: Solution
        lateinit var farkasSolution: Solution
        val solver = CplexQuadraticSolver(
            config = config,
            callBack = callBack.copy()
                .configuration { _, cplex, _, _ ->
                    cplex.setParam(IloCplex.Param.Preprocessing.Dual, 1)
                    ok
                }
                .analyzingSolution { _, cplex, _, constraints ->
                    dualSolution = constraints.map {
                        Flt64(cplex.getDual(it))
                    }
                    ok
                }
                .afterFailure { status, _, _, _ ->
                    if (status == SolverStatus.Infeasible) {
                        when (val result = solveFeasibilityProblem(model)) {
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
                metaModel.tokens.setSolution(model.tokenIndexMap.map { (token, index) ->
                    token.variable to result.value.solution[index]
                }.toMap() + fixedVariables)
                jobs.joinAll()
                val cuts = when (val result = mechanismModel.generateOptimalCut(
                    constants = Flt64.zero,
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
                    BendersDecompositionSolver.QuadraticFeasibleResult(
                        result = result.value,
                        dualSolution = dualSolution,
                        linearCuts = cuts.filterIsInstance<LinearInequality>(),
                        quadraticCuts = cuts.filterIsInstance<QuadraticInequality>()
                    )
                )
            }

            is Failed -> {
                jobs.joinAll()
                if (result.error.code == ErrorCode.ORModelNoSolution) {
                    val cuts = when (val result = mechanismModel.generateFeasibleCut(
                        constants = Flt64.zero,
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
                        BendersDecompositionSolver.QuadraticInfeasibleResult(
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

    private suspend fun solveFeasibilityProblem(
        model: LinearTriadModel
    ): Ret<Solution> {
        val feasibilityModel = model.normalize().feasibility()

        lateinit var dualSolution: Solution
        val solver = CplexLinearSolver(
            config = config,
            callBack = callBack.copy()
                .configuration { _, cplex, _, _ ->
                    cplex.setParam(IloCplex.Param.Preprocessing.Dual, 1)
                    ok
                }
                .analyzingSolution { _, cplex, _, constraints ->
                    dualSolution = constraints.map {
                        Flt64(cplex.getDual(it))
                    }
                    ok
                }
        )

        return when (val result = solver(feasibilityModel)) {
            is Ok -> {
                Ok(dualSolution)
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }

    private suspend fun solveFeasibilityProblem(
        model: QuadraticTetradModel
    ): Ret<Solution> {
        val feasibilityModel = model.normalize().feasibility()

        lateinit var dualSolution: Solution
        val solver = CplexQuadraticSolver(
            config = config,
            callBack = callBack.copy()
                .configuration { _, cplex, _, _ ->
                    cplex.setParam(IloCplex.Param.Preprocessing.Dual, 1)
                    ok
                }
                .analyzingSolution { _, cplex, _, constraints ->
                    dualSolution = constraints.map {
                        Flt64(cplex.getDual(it))
                    }
                    ok
                }
        )

        return when (val result = solver(feasibilityModel)) {
            is Ok -> {
                Ok(dualSolution)
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }
}
