@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import gurobi.*
import fuookami.ospf.kotlin.utils.concept.copyIfNotNullOr
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.nonNullConstraintPriorityAmount
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.config.GurobiSolverConfig
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble

/** Gurobi 线性求解器 / Gurobi linear solver */
class GurobiLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: GurobiLinearSolverCallBack? = null
) : LinearSolver {
    override val name = "gurobi"

    /**
     * 求解线性模型 / Solve linear model
     *
     * @param model 线性模型视图 / linear model view
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return 求解结果 / solving result
     */
    override suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
        return GurobiLinearSolverImpl(
            config = config,
            callBack = callBack,
            statusCallBack = solvingStatusCallBack
        ).use { impl ->
            val result = impl(model)
            cleanupAfterSolverRun()
            result
        }
    }

    /**
     * 求解线性模型，获取多个解 / Solve linear model, obtaining multiple solutions
     *
     * @param model 线性模型视图 / linear model view
     * @param solutionAmount 期望解的数量 / desired number of solutions
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return 求解结果及多个解 / solving result with multiple solutions
     */
    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
        return if (solutionAmount leq UInt64.one) {
            this(model).map { it to emptyList() }
        } else {
            val results = ArrayList<List<Flt64>>()
            GurobiLinearSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { GurobiLinearSolverCallBack() }
                    .configuration { _, gurobi, _, _ ->
                        if (solutionAmount gr UInt64.one) {
                            gurobi.set(GRB.DoubleParam.PoolGap, 1.0);
                            gurobi.set(GRB.IntParam.PoolSearchMode, 2);
                            gurobi.set(GRB.IntParam.PoolSolutions, solutionAmount.toInt())
                        }
                        ok
                    }
                    .analyzingSolution { _, gurobi, variables, _ ->
                        for (i in 0 until min(solutionAmount.toInt(), gurobi.get(GRB.IntAttr.SolCount))) {
                            gurobi.set(GRB.IntParam.SolutionNumber, i)
                            val thisResults = variables.map { Flt64(it.get(GRB.DoubleAttr.Xn)) }
                            if (!results.any { it.toTypedArray() contentEquals thisResults.toTypedArray() }) {
                                results.add(thisResults)
                            }
                        }
                        ok
                    },
                statusCallBack = solvingStatusCallBack
            ).use { impl ->
                val result = impl(model).map { it to results }
                cleanupAfterSolverRun()
                result
            }
        }
    }
}

/** Gurobi 线性求解器内部实现 / Gurobi linear solver internal implementation */
private class GurobiLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: GurobiLinearSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null
) : GurobiSolver() {
    private lateinit var grbVars: List<GRBVar>
    private lateinit var grbConstraints: List<GRBConstr>
    private lateinit var output: FeasibleSolverOutput<Flt64>

    private var initialBestObj: Flt64? = null
    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestSolution: List<Flt64>? = null
    private var bestTime: Duration = Duration.ZERO

    /**
     * 执行求解流程 / Execute solving process
     *
     * @param model 线性模型视图 / linear model view
     * @return 求解结果 / solving result
     */
    suspend operator fun invoke(model: LinearTriadModelView): Ret<FeasibleSolverOutput<Flt64>> {
        val gurobiConfig = config.extraConfig as? GurobiSolverConfig
        val server = gurobiConfig?.server
        val password = gurobiConfig?.password
        val connectionTime = gurobiConfig?.connectionTime

        val processes = arrayOf(
            {
                if (server != null && password != null && connectionTime != null) {
                    it.init(
                        server = server,
                        password = password,
                        connectionTime = connectionTime,
                        name = model.name,
                        callBack = callBack?.creatingEnvironmentFunction
                    )
                } else {
                    it.init(
                        name = model.name,
                        callBack = callBack?.creatingEnvironmentFunction
                    )
                }
            },
            { it.dump(model) },
            { it.configure(model) },
            GurobiLinearSolverImpl::solve,
            GurobiLinearSolverImpl::analyzeStatus,
            GurobiLinearSolverImpl::analyzeSolution
        )
        for (process in processes) {
            when (val result = process(this)) {
                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }

                else -> {}
            }
        }
        return Ok(output)
    }

    /**
     * 将模型转储到 Gurobi / Dump model to Gurobi
     *
     * @param model 线性模型视图 / linear model view
     * @return 操作结果 / operation result
     */
    private suspend fun dump(model: LinearTriadModelView): Try {
        return try {
            warnIgnoredConstraintPriority("gurobi", model.nonNullConstraintPriorityAmount())

            val variableDumpingData = prepareVariableDumpingData(
                variables = model.variables,
                scopeName = "linear"
            )
            val vars = ArrayList<GRBVar>(model.variables.size)
            for (col in model.variables.indices) {
                vars.add(
                    grbModel.addVar(
                        variableDumpingData.lowerBounds[col],
                        variableDumpingData.upperBounds[col],
                        0.0,
                        GurobiVariable(model.variables[col].type).toGurobiVar(),
                        variableDumpingData.names[col]
                    )
                )
            }
            grbVars = vars

            for ((col, initialResult) in variableDumpingData.initialResults) {
                grbVars[col].set(GRB.DoubleAttr.Start, initialResult)
            }

            val constraints = coroutineScope {
                if (Runtime.getRuntime().availableProcessors() > 2 && model.constraints.size > Runtime.getRuntime().availableProcessors()) {
                    val segment = computeConstraintSegmentSize(model.constraints.size)
                    val chunkAmount = (model.constraints.size + segment - 1) / segment
                    val promises = (0 until chunkAmount).map { i ->
                        async(Dispatchers.Default) {
                            val from = i * segment
                            val to = minOf(model.constraints.size, from + segment)
                            val constraints = (from until to).map { ii ->
                                val lhs = GRBLinExpr()
                                model.constraints.sparseLhs.forEachEntry(ii) { colIndex, coefficient ->
                                    lhs.addTerm(
                                        coefficient.toSolverDouble("linear.constraints.lhs[$ii][$colIndex].coefficient"),
                                        grbVars[colIndex]
                                    )
                                }
                                ii to lhs
                            }
                            cleanupOnSolverMemoryPressure()
                            constraints
                        }
                    }
                    promises.flatMap { promise ->
                        val result = promise.await().map {
                            grbModel.addConstr(
                                it.second,
                                GurobiConstraintSign(model.constraints.signs[it.first]).toGurobiConstraintSign(),
                                model.constraints.rhs[it.first].toSolverDouble("linear.constraints.rhs[${it.first}]"),
                                model.constraints.names[it.first]
                            )
                        }
                        cleanupOnSolverMemoryPressure()
                        result
                    }
                } else {
                    model.constraints.indices.map { i ->
                        val lhs = GRBLinExpr()
                        model.constraints.sparseLhs.forEachEntry(i) { colIndex, coefficient ->
                            lhs.addTerm(
                                coefficient.toSolverDouble("linear.constraints.lhs[$i][$colIndex].coefficient"),
                                grbVars[colIndex]
                            )
                        }
                        grbModel.addConstr(
                            lhs,
                            GurobiConstraintSign(model.constraints.signs[i]).toGurobiConstraintSign(),
                            model.constraints.rhs[i].toSolverDouble("linear.constraints.rhs[$i]"),
                            model.constraints.names[i]
                        )
                    }
                }
            }
            cleanupAfterSolverRun()
            grbConstraints = constraints

            val obj = GRBLinExpr()
            for (cell in model.objective.objective) {
                obj.addTerm(
                    cell.coefficient.toSolverDouble("linear.objective.cells[${cell.colIndex}].coefficient"),
                    grbVars[cell.colIndex]
                )
            }
            obj.addConstant(model.objective.constant.toSolverDouble("linear.objective.constant"))
            grbModel.setObjective(
                obj,
                when (model.objective.category) {
                    ObjectCategory.Minimum -> {
                        GRB.MINIMIZE
                    }

                    ObjectCategory.Maximum -> {
                        GRB.MAXIMIZE
                    }
                }
            )

            when (val result = callBack?.execIfContain(
                point = Point.AfterModeling,
                status = null,
                gurobi = grbModel,
                variables = grbVars,
                constraints = grbConstraints
            )) {
                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }

                else -> {}
            }
            ok
        } catch (e: GRBException) {
            modelingException(e.message)
        } catch (e: Exception) {
            modelingException()
        }
    }

    /**
     * 配置 Gurobi 求解器参数 / Configure Gurobi solver parameters
     *
     * @param model 线性模型视图 / linear model view
     * @return 操作结果 / operation result
     */
    private suspend fun configure(model: LinearTriadModelView): Try {
        return try {
            grbModel.set(GRB.DoubleParam.TimeLimit, config.time.toDouble(DurationUnit.SECONDS))
            grbModel.set(GRB.DoubleParam.MIPGap, config.gap.toSolverDouble("linear.config.gap"))
            grbModel.set(GRB.IntParam.Threads, config.threadNum.toInt())

            if (config.notImprovementTime != null || callBack?.nativeCallback != null || statusCallBack != null) {
                grbModel.setCallback(object : GRBCallback() {
                    override fun callback() {
                        callBack?.nativeCallback?.invoke(this)

                        if (where == GRB.CB_MIPSOL) {
                            bestSolution = getSolution(grbVars.toTypedArray()).map { Flt64(it) }
                        }
                        if (where == GRB.CB_MIP) {
                            val currentObj = Flt64(getDoubleInfo(GRB.Callback.MIP_OBJBST))
                            val currentBound = Flt64(getDoubleInfo(GRB.Callback.MIP_OBJBND))
                            val currentTime = getDoubleInfo(GRB.Callback.RUNTIME).seconds

                            if (initialBestObj == null) {
                                initialBestObj = currentObj
                            }

                            config.notImprovementTime?.let { notImprovementTime ->
                                val previousBestObj = bestObj
                                val previousBestBound = bestBound
                                if (previousBestObj == null
                                    || previousBestBound == null
                                    || (currentObj - previousBestObj).abs() geq config.improveThreshold
                                    || (currentBound - previousBestBound).abs() geq config.improveThreshold
                                ) {
                                    bestObj = currentObj
                                    bestBound = currentBound
                                    bestTime = currentTime
                                } else if (currentTime - bestTime >= notImprovementTime) {
                                    abort()
                                }
                            }

                            statusCallBack?.let {
                                val callbackResult = it(
                                    SolvingStatus(
                                        solver = "gurobi",
                                        solverConfig = config,
                                        intermediateModel = model,
                                        solverModel = grbModel,
                                        solverCallBack = this,
                                        objectCategory = when (grbModel.get(GRB.IntAttr.ModelSense)) {
                                            GRB.MINIMIZE -> {
                                                ObjectCategory.Minimum
                                            }

                                            GRB.MAXIMIZE -> {
                                                ObjectCategory.Maximum
                                            }

                                            else -> {
                                                null
                                            }
                                        },
                                        time = currentTime,
                                        obj = currentObj,
                                        possibleBestObj = currentBound,
                                        bestBound = currentBound,
                                        initialBestObj = initialBestObj ?: currentObj,
                                        gap = (currentObj - currentBound + Flt64.decimalPrecision) / (currentObj + Flt64.decimalPrecision),
                                        currentBestSolution = bestSolution
                                    )
                                )
                                if (shouldAbortOnCallbackFailure(callbackResult) { abort() }) {
                                    return
                                }
                            }
                        }
                    }
                })
            }

            when (val result = callBack?.execIfContain(
                point = Point.Configuration,
                status = null,
                gurobi = grbModel,
                variables = grbVars,
                constraints = grbConstraints
            )) {
                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }

                else -> {}
            }
            ok
        } catch (e: GRBException) {
            modelingException(e.message)
        } catch (e: Exception) {
            modelingException()
        }
    }

    /** 分析求解结果 / Analyze solving result */
    private suspend fun analyzeSolution(): Try {
        return try {
            if (status.succeeded) {
                val results = ArrayList<Flt64>()
                for (grbVar in grbVars) {
                    results.add(Flt64(grbVar.get(GRB.DoubleAttr.X)))
                }
                output = FeasibleSolverOutput<Flt64>(
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
                when (val result = callBack?.execIfContain(
                    point = Point.AnalyzingSolution,
                    status = status,
                    gurobi = grbModel,
                    variables = grbVars,
                    constraints = grbConstraints
                )) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }

                    else -> {}
                }
                ok
            } else {
                when (val result = callBack?.execIfContain(
                    point = Point.AfterFailure,
                    status = status,
                    gurobi = grbModel,
                    variables = grbVars,
                    constraints = grbConstraints
                )) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }

                    else -> {}
                }
                failByStatus(status)
            }
        } catch (e: GRBException) {
            solvingException(e.message)
        } catch (e: Exception) {
            solvingException()
        }
    }
}
