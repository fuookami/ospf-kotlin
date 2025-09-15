package fuookami.ospf.kotlin.core.backend.plugins.scip

import java.util.*
import kotlinx.coroutines.*
import jscip.*
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

class ScipBendersDecompositionSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: ScipSolverCallBack = ScipSolverCallBack()
) : BendersDecompositionSolver {
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
                .analyzingSolution { _, model, _, constraints ->
                    dualSolution = constraints.map {
                        Flt64(model.getDual(it))
                    }
                    ok
                }
                .afterFailure { status, model, _, constraints ->
                    if (status == SolverStatus.Infeasible) {
                        farkasSolution = constraints.map {
                            Flt64(model.getDualFarkasLinear(it))
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
                Ok(BendersDecompositionSolver.LinearFeasibleResult(
                    result.value,
                    dualSolution,
                    mechanismModel.generateFeasibleCut(objectVariable, fixedVariables, dualSolution)
                ))
            }

            is Failed -> {
                jobs.joinAll()
                if (result.error.code == ErrorCode.ORModelNoSolution) {
                    Ok(BendersDecompositionSolver.LinearInfeasibleResult(
                        farkasSolution,
                        mechanismModel.generateInfeasibleCut(fixedVariables, farkasSolution)
                    ))
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
        lateinit var qpiSolution: Solution
        lateinit var dualSolution: Solution
        lateinit var farkasSolution: Solution
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
                .analyzingSolution { _, model, _, constraints ->
                    // todo: get qpi solution
                    dualSolution = constraints.map {
                        Flt64(model.getDual(it))
                    }
                    ok
                }
                .afterFailure { status, model, _, constraints ->
                    if (status == SolverStatus.Infeasible) {
                        // todo: get qpi solution
                        // todo: get farkas solution
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
                val cuts = mechanismModel.generateFeasibleCut(model.tokenIndexMap, objectVariable, fixedVariables, qpiSolution, dualSolution)
                Ok(BendersDecompositionSolver.QuadraticFeasibleResult(
                    result.value,
                    qpiSolution,
                    dualSolution,
                    cuts.filterIsInstance<LinearInequality>(),
                    cuts.filterIsInstance<QuadraticInequality>()
                ))
            }

            is Failed -> {
                jobs.joinAll()
                if (result.error.code == ErrorCode.ORModelNoSolution) {
                    val cuts = mechanismModel.generateInfeasibleCut(fixedVariables, qpiSolution, farkasSolution)
                    Ok(BendersDecompositionSolver.QuadraticInfeasibleResult(
                        qpiSolution,
                        farkasSolution,
                        cuts.filterIsInstance<LinearInequality>(),
                        cuts.filterIsInstance<QuadraticInequality>()
                    ))
                } else {
                    Failed(result.error)
                }
            }
        }
    }
}
