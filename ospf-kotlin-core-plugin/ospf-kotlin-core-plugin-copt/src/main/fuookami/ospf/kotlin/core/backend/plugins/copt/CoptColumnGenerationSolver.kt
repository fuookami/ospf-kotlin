package fuookami.ospf.kotlin.core.backend.plugins.copt

import java.util.*
import kotlin.math.*
import kotlinx.coroutines.*
import copt.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.framework.solver.*

class CoptColumnGenerationSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: CoptLinearSolverCallBack = CoptLinearSolverCallBack()
) : ColumnGenerationSolver {
    override val name = "copt"

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

            val results = ArrayList<Solution>()
            val solver = CoptLinearSolver(
                config = config,
                callBack = callBack.copy()
                    .configuration { _, copt, _, _ ->
                        if (amount gr UInt64.one) {
                            // todo: set copt parameter to limit number of solutions
                        }
                        ok
                    }
                    .analyzingSolution { _, copt, variables, _ ->
                        for (i in 0 until min(amount.toInt(), copt.get(COPT.IntAttr.PoolSols))) {
                            val thisResults = copt.getPoolSolution(i, variables.toTypedArray()).map { Flt64(it) }
                            if (!results.any { it.toTypedArray() contentEquals thisResults.toTypedArray() }) {
                                results.add(thisResults)
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
        }.use { mechanismModel ->
            val model = LinearTriadModel(
                model = mechanismModel,
                fixedVariables = null,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds ?: false,
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
            val solver = CoptLinearSolver(
                config = config,
                callBack = callBack.copy()
                    .analyzingSolution { _, _, _, constraints ->
                        dualSolution = model.tidyDualSolution(constraints.map { constraint ->
                            Flt64(constraint.get(COPT.DoubleInfo.Dual))
                        })
                        ok
                    }
            )

            when (val result = solver(model, solvingStatusCallBack)) {
                is Ok -> {
                    metaModel.tokens.setSolution(result.value.solution)
                    jobs.joinAll()
                    Ok(ColumnGenerationSolver.LPResult(result.value, dualSolution))
                }

                is Failed -> {
                    jobs.joinAll()
                    Failed(result.error)
                }
            }
        }
    }
}
