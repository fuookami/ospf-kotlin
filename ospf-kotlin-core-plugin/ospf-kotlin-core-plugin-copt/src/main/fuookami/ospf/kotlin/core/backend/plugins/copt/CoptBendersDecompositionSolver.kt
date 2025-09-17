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

class CoptBendersDecompositionSolver(
    private val config: SolverConfig = SolverConfig(),
    private val linearCallBack: CoptLinearSolverCallBack = CoptLinearSolverCallBack(),
    private val quadraticCallBack: CoptQuadraticSolverCallBack = CoptQuadraticSolverCallBack()
) : BendersDecompositionSolver {
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

        val solver = CoptLinearSolver(
            config = config,
            callBack = linearCallBack.copy()
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

        val solver = CoptQuadraticSolver(
            config = config,
            callBack = quadraticCallBack.copy()
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
        val solver = CoptLinearSolver(
            config = config,
            callBack = linearCallBack.copy()
                .analyzingSolution { _, _, _, constraints ->
                    dualSolution = constraints.map {
                        Flt64(it.get(COPT.DoubleInfo.Dual))
                    }
                    ok
                }
                .afterFailure { status, _, _, constraints ->
                    if (status == SolverStatus.Infeasible) {
                        farkasSolution = constraints.map {
                            Flt64(it.get(COPT.DoubleInfo.DualFarkas))
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
                    mechanismModel.generateOptimalCut(objectVariable, fixedVariables, dualSolution)
                ))
            }

            is Failed -> {
                jobs.joinAll()
                if (result.error.code == ErrorCode.ORModelNoSolution) {
                    Ok(BendersDecompositionSolver.LinearInfeasibleResult(
                        farkasSolution,
                        mechanismModel.generateFeasibleCut(fixedVariables, farkasSolution)
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

        lateinit var dualSolution: List<Flt64>
        lateinit var farkasSolution: List<Flt64>
        val solver = CoptQuadraticSolver(
            config = config,
            callBack = quadraticCallBack.copy()
                .analyzingSolution { _, _, _, constraints ->
                    dualSolution = constraints.map {
                        Flt64(it.get(COPT.DoubleInfo.Dual))
                    }
                    ok
                }
                .afterFailure { status, _, _, constraints ->
                    if (status == SolverStatus.Infeasible) {
                        farkasSolution = constraints.map {
                            Flt64(it.get(COPT.DoubleInfo.DualFarkas))
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
                val cuts = when (val result = mechanismModel.generateOptimalCut(result.value.obj, objectVariable, fixedVariables, dualSolution)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
                Ok(BendersDecompositionSolver.QuadraticFeasibleResult(
                    result.value,
                    dualSolution,
                    cuts.filterIsInstance<LinearInequality>(),
                    cuts.filterIsInstance<QuadraticInequality>()
                ))
            }

            is Failed -> {
                jobs.joinAll()
                if (result.error.code == ErrorCode.ORModelNoSolution) {
                    val cuts = when (val result = mechanismModel.generateFeasibleCut(fixedVariables, farkasSolution)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                    Ok(BendersDecompositionSolver.QuadraticInfeasibleResult(
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
