package fuookami.ospf.kotlin.core.backend.plugins.gurobi11

import kotlin.math.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import com.gurobi.gurobi.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

class GurobiQuadraticSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: GurobiQuadraticSolverCallBack? = null
) : QuadraticSolver {
    override val name = "gurobi"

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        statusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        val impl = GurobiQuadraticSolverImpl(config, callBack, statusCallBack)
        val result = impl(model)
        System.gc()
        return result
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        statusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        return if (solutionAmount leq UInt64.one) {
            this(model).map { it to emptyList() }
        } else {
            val results = ArrayList<Solution>()
            val impl = GurobiQuadraticSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { GurobiQuadraticSolverCallBack() }
                    .configuration { gurobi, _, _ ->
                        if (solutionAmount gr UInt64.one) {
                            gurobi.set(GRB.DoubleParam.PoolGap, 1.0);
                            gurobi.set(GRB.IntParam.PoolSearchMode, 2);
                            gurobi.set(GRB.IntParam.PoolSolutions, solutionAmount.toInt())
                        }
                        ok
                    }
                    .analyzingSolution { gurobi, variables, _ ->
                        for (i in 0 until min(solutionAmount.toInt(), gurobi.get(GRB.IntAttr.SolCount))) {
                            gurobi.set(GRB.IntParam.SolutionNumber, i)
                            val thisResults = variables.map { Flt64(it.get(GRB.DoubleAttr.Xn)) }
                            if (!results.any { it.toTypedArray() contentEquals thisResults.toTypedArray() }) {
                                results.add(thisResults)
                            }
                        }
                        ok
                    },
                statusCallBack = statusCallBack
            )
            val result = impl(model).map { it to results }
            System.gc()
            return result
        }
    }
}

private class GurobiQuadraticSolverImpl(
    private val config: SolverConfig,
    private val callBack: GurobiQuadraticSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null
) : GurobiSolver() {
    private lateinit var grbVars: List<GRBVar>
    private lateinit var grbConstraints: List<GRBQConstr>
    private lateinit var output: SolverOutput

    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    suspend operator fun invoke(model: QuadraticTetradModelView): Ret<SolverOutput> {
        val gurobiConfig = config.extraConfig as? GurobiSolverConfig
        val server = gurobiConfig?.server
        val password = gurobiConfig?.password
        val connectionTime = gurobiConfig?.connectionTime

        val processes = arrayOf(
            {
                if (server != null && password != null && connectionTime != null) {
                    it.init(server, password, connectionTime, model.name, callBack?.creatingEnvironmentFunction)
                } else {
                    it.init(model.name, callBack?.creatingEnvironmentFunction)
                }
            },
            { it.dump(model) },
            GurobiQuadraticSolverImpl::configure,
            GurobiQuadraticSolverImpl::solve,
            GurobiQuadraticSolverImpl::analyzeStatus,
            GurobiQuadraticSolverImpl::analyzeSolution
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

    private suspend fun dump(model: QuadraticTetradModelView): Try {
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
                if (Runtime.getRuntime().availableProcessors() > 2 && model.constraints.size > Runtime.getRuntime().availableProcessors()) {
                    val factor = Flt64(model.constraints.size / (Runtime.getRuntime().availableProcessors() - 1)).lg()!!.floor().toUInt64().toInt()
                    val segment = if (factor >= 1) {
                        pow(UInt64.ten, factor).toInt()
                    } else {
                        10
                    }
                    val promises = (0..(model.constraints.size / segment)).map { i ->
                        async(Dispatchers.Default) {
                            ((i * segment) until minOf(model.constraints.size, (i + 1) * segment)).map { ii ->
                                val lhs = GRBQuadExpr()
                                for (cell in model.constraints.lhs[ii]) {
                                    if (cell.colIndex2 == null) {
                                        lhs.addTerm(cell.coefficient.toDouble(), grbVars[cell.colIndex1])
                                    } else {
                                        lhs.addTerm(cell.coefficient.toDouble(), grbVars[cell.colIndex1], grbVars[cell.colIndex2!!])
                                    }
                                }
                                ii to lhs
                            }
                        }
                    }
                    promises.flatMap { promise ->
                        val result = promise.await().map {
                            grbModel.addQConstr(
                                it.second,
                                GurobiConstraintSign(model.constraints.signs[it.first]).toGurobiConstraintSign(),
                                model.constraints.rhs[it.first].toDouble(),
                                model.constraints.names[it.first]
                            )
                        }
                        result
                    }
                } else {
                    model.constraints.indices.map { i ->
                        val lhs = GRBQuadExpr()
                        for (cell in model.constraints.lhs[i]) {
                            if (cell.colIndex2 == null) {
                                lhs.addTerm(cell.coefficient.toDouble(), grbVars[cell.colIndex1])
                            } else {
                                lhs.addTerm(cell.coefficient.toDouble(), grbVars[cell.colIndex1], grbVars[cell.colIndex2!!])
                            }
                        }
                        grbModel.addQConstr(
                            lhs,
                            GurobiConstraintSign(model.constraints.signs[i]).toGurobiConstraintSign(),
                            model.constraints.rhs[i].toDouble(),
                            model.constraints.names[i]
                        )
                    }
                }
            }
            System.gc()
            grbConstraints = constraints

            val obj = GRBQuadExpr()
            for (cell in model.objective.obj) {
                if (cell.colIndex2 == null) {
                    obj.addTerm(cell.coefficient.toDouble(), grbVars[cell.colIndex1])
                } else {
                    obj.addTerm(cell.coefficient.toDouble(), grbVars[cell.colIndex1], grbVars[cell.colIndex2!!])
                }
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
                grbModel.setCallback(object : GRBCallback() {
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
                                when (it(
                                    SolvingStatus(
                                        solver = "gurobi",
                                        obj = currentObj,
                                        possibleBestObj = currentBound,
                                        gap = (currentObj - currentBound + Flt64.decimalPrecision) / (currentObj + Flt64.decimalPrecision)
                                    )
                                )) {
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
            if (status.succeeded) {
                val results = ArrayList<Flt64>()
                for (grbVar in grbVars) {
                    results.add(Flt64(grbVar.get(GRB.DoubleAttr.X)))
                }
                output = SolverOutput(
                    obj = Flt64(grbModel.get(GRB.DoubleAttr.ObjVal)),
                    solution = results,
                    time = grbModel.get(GRB.DoubleAttr.Runtime).seconds,
                    possibleBestObj = Flt64(
                        if (grbModel.get(GRB.IntAttr.IsMIP) != 0) {
                            grbModel.get(GRB.DoubleAttr.ObjBound)
                        } else {
                            grbModel.get(GRB.DoubleAttr.ObjVal)
                        }
                    ),
                    gap = Flt64(
                        if (grbModel.get(GRB.IntAttr.IsMIP) != 0) {
                            grbModel.get(GRB.DoubleAttr.MIPGap)
                        } else {
                            0.0
                        }
                    )
                )
                when (val result = callBack?.execIfContain(Point.AnalyzingSolution, grbModel, grbVars, grbConstraints)) {
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
                Failed(Err(status.errCode!!))
            }
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineSolvingException))
        }
    }
}
