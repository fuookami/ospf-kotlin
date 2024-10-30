package fuookami.ospf.kotlin.core.backend.plugins.gurobi11

import kotlin.math.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import com.gurobi.gurobi.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

class GurobiLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: GurobiLinearSolverCallBack? = null
) : LinearSolver {
    override val name = "gurobi"

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        statusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        val impl = GurobiLinearSolverImpl(config, callBack, statusCallBack)
        return impl(model)
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        statusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        return if (solutionAmount leq UInt64.one) {
            this(model).map { it to emptyList() }
        } else {
            val results = ArrayList<Solution>()
            val impl = GurobiLinearSolverImpl(config, callBack.ifNull { GurobiLinearSolverCallBack() }.copy()
                .configuration { gurobi, _, _ ->
                    if (solutionAmount gr UInt64.one) {
                        gurobi.set(GRB.DoubleParam.PoolGap, 1.0);
                        gurobi.set(GRB.IntParam.PoolSearchMode, 2);
                        gurobi.set(GRB.IntParam.PoolSolutions, solutionAmount.toInt())
                    }
                    ok
                }.analyzingSolution { gurobi, variables, _ ->
                    for (i in 0 until min(solutionAmount.toInt(), gurobi.get(GRB.IntAttr.SolCount))) {
                        gurobi.set(GRB.IntParam.SolutionNumber, i)
                        val thisResults = variables.map { Flt64(it.get(GRB.DoubleAttr.Xn)) }
                        if (!results.any { it.toTypedArray() contentEquals thisResults.toTypedArray() }) {
                            results.add(thisResults)
                        }
                    }
                    ok
                }, statusCallBack
            )
            impl(model).map { it to results }
        }
    }
}

private class GurobiLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: GurobiLinearSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null
) : GurobiSolver() {
    lateinit var grbVars: List<GRBVar>
    lateinit var grbConstraints: List<GRBConstr>
    lateinit var output: SolverOutput

    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    suspend operator fun invoke(model: LinearTriadModelView): Ret<SolverOutput> {
        val gurobiConfig = if (config.extraConfig is GurobiSolverConfig) {
            config.extraConfig as GurobiSolverConfig
        } else {
            null
        }
        val server = gurobiConfig?.server
        val password = gurobiConfig?.password
        val connectionTime = gurobiConfig?.connectionTime

        val processes = arrayOf(
            {
                if (server != null && password != null && connectionTime != null) {
                    it.init(server, password, connectionTime, model.name)
                } else {
                    it.init(model.name)
                }
            },
            { it.dump(model) },
            GurobiLinearSolverImpl::configure,
            GurobiLinearSolverImpl::solve,
            GurobiLinearSolverImpl::analyzeStatus,
            GurobiLinearSolverImpl::analyzeSolution
        )
        for (process in processes) {
            when (val result = process(this)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
        }
        return Ok(output)
    }

    private suspend fun dump(model: LinearTriadModelView): Try {
        return try {
            grbVars = grbModel.addVars(
                model.variables.map { it.lowerBound.toDouble() }.toDoubleArray(),
                model.variables.map { it.upperBound.toDouble() }.toDoubleArray(),
                null,
                model.variables.map { GurobiVariable(it.type).toGurobiVar() }.toCharArray(),
                model.variables.map { it.name }.toTypedArray(),
                0,
                model.variables.size
            ).toList()

            for ((col, variable) in model.variables.withIndex()) {
                variable.initialResult?.let {
                    grbVars[col].set(GRB.DoubleAttr.Start, it.toDouble())
                }
            }

            val constraints = coroutineScope {
                val promises = model.constraints.indices.map { i ->
                    i to async(Dispatchers.Default) {
                        val lhs = GRBLinExpr()
                        for (cell in model.constraints.lhs[i]) {
                            lhs.addTerm(cell.coefficient.toDouble(), grbVars[cell.colIndex])
                        }
                        lhs
                    }
                }
                promises.map {
                    grbModel.addConstr(
                        it.second.await(),
                        GurobiConstraintSign(model.constraints.signs[it.first]).toGurobiConstraintSign(),
                        model.constraints.rhs[it.first].toDouble(),
                        model.constraints.names[it.first]
                    )
                }
            }
            grbConstraints = constraints

            val obj = GRBLinExpr()
            for (cell in model.objective.obj) {
                obj.addTerm(cell.coefficient.toDouble(), grbVars[cell.colIndex])
            }
            grbModel.setObjective(
                obj, when (model.objective.category) {
                    ObjectCategory.Minimum -> {
                        GRB.MINIMIZE
                    }

                    ObjectCategory.Maximum -> {
                        GRB.MAXIMIZE
                    }
                }
            )

            when (val result = callBack?.execIfContain(Point.AfterModeling, grbModel, grbVars, grbConstraints)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            ok
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineModelingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineModelingException))
        }
    }

    private suspend fun configure(): Try {
        return try {
            grbModel.set(GRB.DoubleParam.TimeLimit, config.time.toDouble(DurationUnit.SECONDS))
            grbModel.set(GRB.DoubleParam.MIPGap, config.gap.toDouble())
            grbModel.set(GRB.IntParam.Threads, config.threadNum.toInt())

            if (config.notImprovementTime != null || callBack?.nativeCallback != null || statusCallBack != null) {
                grbModel.setCallback(object: GRBCallback() {
                    override fun callback() {
                        callBack?.nativeCallback?.invoke(this)

                        if (where == GRB.CB_MIP) {
                            val currentObj = Flt64(getDoubleInfo(GRB.Callback.MIP_OBJBST))
                            val currentBound = Flt64(getDoubleInfo(GRB.Callback.MIP_OBJBND))
                            val currentTime = getDoubleInfo(GRB.Callback.RUNTIME).seconds

                            if (config.notImprovementTime != null) {
                                if (bestObj == null || bestBound == null || currentObj neq bestObj!! || currentBound neq bestBound!!) {
                                    bestObj = currentObj
                                    bestBound = currentBound
                                    bestTime = currentTime
                                } else if (currentTime - bestTime >= config.notImprovementTime!!) {
                                    abort()
                                }
                            }

                            statusCallBack?.let {
                                when (it(SolvingStatus(
                                    solver = "gurobi",
                                    obj = currentObj,
                                    possibleBestObj = currentBound,
                                    gap = (currentObj - currentBound + Flt64.decimalPrecision) / (currentObj + Flt64.decimalPrecision)
                                ))) {
                                    is Ok -> {}

                                    is Failed -> {
                                        abort()
                                    }
                                }
                            }
                        }
                    }
                })
            }

            when (val result = callBack?.execIfContain(Point.Configuration, grbModel, grbVars, grbConstraints)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            ok
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineModelingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineModelingException))
        }
    }

    private suspend fun analyzeSolution(): Try {
        return try {
            if (status.succeeded()) {
                val results = ArrayList<Flt64>()
                for (grbVar in grbVars) {
                    results.add(Flt64(grbVar.get(GRB.DoubleAttr.X)))
                }
                output = SolverOutput(
                    Flt64(grbModel.get(GRB.DoubleAttr.ObjVal)),
                    results,
                    grbModel.get(GRB.DoubleAttr.Runtime).seconds,
                    Flt64(if (grbModel.get(GRB.IntAttr.IsMIP) != 0) {
                        grbModel.get(GRB.DoubleAttr.ObjBound)
                    } else {
                        grbModel.get(GRB.DoubleAttr.ObjVal)
                    }),
                    Flt64(
                        if (grbModel.get(GRB.IntAttr.IsMIP) != 0) {
                            grbModel.get(GRB.DoubleAttr.MIPGap)
                        } else {
                            0.0
                        }
                    )
                )
                when (val result =
                    callBack?.execIfContain(Point.AnalyzingSolution, grbModel, grbVars, grbConstraints)) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    else -> {}
                }
                ok
            } else {
                when (val result = callBack?.execIfContain(Point.AfterFailure, grbModel, grbVars, grbConstraints)) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    else -> {}
                }
                Failed(Err(status.errCode()!!))
            }
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineSolvingException))
        }
    }
}
