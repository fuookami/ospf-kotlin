@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.core.model.basic.nonNullConstraintPriorityAmount
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.config.GurobiSolverConfig
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.iis.InfeasibilityAnalyzer
import fuookami.ospf.kotlin.core.solver.nativeElementName
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.concept.copyIfNotNullOr
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import gurobi.*

/** Gurobi 线性求解器 / Gurobi linear solver */
class GurobiLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: GurobiLinearSolverCallBack? = null
) : LinearSolver {
    override val name = "gurobi"
    override val descriptor = SolverDescriptor(
        solverId = "gurobi",
        backendName = "Gurobi",
        backendVersion = gurobiNativeVersion(),
        pluginVersion = GurobiLinearSolver::class.java.`package`.implementationVersion,
        capabilities = SolverCapabilities(
            modelTypes = setOf(SolverModelType.LP, SolverModelType.MIP),
            nativeIIS = true,
            dual = true,
            farkas = true,
            warmStart = true,
            solutionPool = true,
            callback = true,
            interrupt = true
        )
    )

    override fun diagnosticAnalyzers(
        config: IISConfig
    ): List<InfeasibilityAnalyzer<LinearTriadModelView>> {
        return listOf(
            GurobiNativeIISAnalyzer(this.config, config, callBack),
            GurobiFarkasAnalyzer(this.config, config, callBack)
        )
    }

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
    ): Ret<SolveReport<Flt64>> {
        return invoke(model, solvingStatusCallBack, null)
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?,
        cancellationToken: CancellationToken?
    ): Ret<SolveReport<Flt64>> {
        when (val validation = model.identityValidation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return GurobiLinearSolverImpl(
            config = config,
            callBack = callBack,
            statusCallBack = solvingStatusCallBack,
            cancellationToken = cancellationToken
        ).use { impl ->
            val result = impl(model)
            cleanupAfterSolverRun()
            result.map { report ->
                report.withLinearBackendMetadata(model, config, descriptor)
            }
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
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        return invoke(model, solutionAmount, solvingStatusCallBack, null)
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?,
        cancellationToken: CancellationToken?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        return if (solutionAmount leq UInt64.one) {
            this(model, solvingStatusCallBack, cancellationToken).map { it to emptyList() }
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
                statusCallBack = solvingStatusCallBack,
                cancellationToken = cancellationToken
            ).use { impl ->
                val result = impl(model).map { it to results }
                cleanupAfterSolverRun()
                result.map { (report, solutions) ->
                    report.withLinearBackendMetadata(model, config, descriptor) to solutions
                }
            }
        }
    }
}

/** Gurobi 线性求解器内部实现 / Gurobi linear solver internal implementation */
private class GurobiLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: GurobiLinearSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null,
    private val cancellationToken: CancellationToken? = null
) : GurobiSolver() {
    private lateinit var grbVars: List<GRBVar>
    private lateinit var grbConstraints: List<GRBConstr>
    private lateinit var output: SolveReport<Flt64>

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
    suspend operator fun invoke(model: LinearTriadModelView): Ret<SolveReport<Flt64>> {
        if (cancellationToken?.isCancellationRequested == true) {
            return Ok(cancelledSolveReport(cancellationToken.record?.reason))
        }
        val gurobiConfig = config.backendConfiguration as? GurobiSolverConfig
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
            { it.analyzeStatus(cancellationToken) },
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
                        nativeElementName(
                            identityId = model.variables[col].id?.value,
                            fallbackName = variableDumpingData.names[col],
                            category = "variable",
                            identityScope = model.variables[col].identityScope
                        )
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
                                nativeElementName(
                                    identityId = model.constraints.ids.getOrNull(it.first)?.value,
                                    fallbackName = model.constraints.names[it.first],
                                    category = "constraint",
                                    identityScope = model.constraints.identityScopeAt(it.first)
                                )
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
                            nativeElementName(
                                identityId = model.constraints.ids.getOrNull(i)?.value,
                                fallbackName = model.constraints.names[i],
                                category = "constraint",
                                identityScope = model.constraints.identityScopeAt(i)
                            )
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
            solverModelingException(e.message)
        } catch (e: Exception) {
            solverModelingException()
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
            when (val cancellation = registerCancellation(cancellationToken)) {
                is Failed -> return cancellation
                is Fatal -> return cancellation
                else -> {}
            }
            grbModel.set(GRB.DoubleParam.TimeLimit, config.time.toDouble(DurationUnit.SECONDS))
            grbModel.set(GRB.DoubleParam.MIPGap, config.gap.toSolverDouble("linear.config.gap"))
            grbModel.set(GRB.IntParam.Threads, config.threadNum.toInt())

            if (config.notImprovementTime != null || callBack?.nativeCallback != null ||
                statusCallBack != null || cancellationToken != null) {
                grbModel.setCallback(object : GRBCallback() {
                    override fun callback() {
                        if (cancellationToken?.isCancellationRequested == true) {
                            abort()
                            return
                        }
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
                                } else if (currentTime - bestTime >= notImprovementTime
                                    && config.interruptibleTime?.let { currentTime >= it } ?: true
                                    && config.interruptibleGap?.let { (currentObj - currentBound).abs() ls it } ?: true
                                ) {
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
            solverModelingException(e.message)
        } catch (e: Exception) {
            solverModelingException()
        }
    }

    /**
     * 分析求解结果 / Analyze solving result
     *
     * @return 以Try包装的分析结果 / the analysis result as Try
    */
    private suspend fun analyzeSolution(): Try {
        return try {
            if (status.succeeded) {
                val results = ArrayList<Flt64>()
                for (grbVar in grbVars) {
                    results.add(Flt64(grbVar.get(GRB.DoubleAttr.X)))
                }
                val isMip = grbModel.get(GRB.IntAttr.IsMIP) != 0
                val possibleBestObj = when {
                    isMip -> Flt64(grbModel.get(GRB.DoubleAttr.ObjBound))
                    status == SolverStatus.Optimal -> Flt64(grbModel.get(GRB.DoubleAttr.ObjVal))
                    else -> try {
                        Flt64(grbModel.get(GRB.DoubleAttr.ObjBound))
                    } catch (_: Exception) {
                        null
                    }
                }
                val gap = when {
                    isMip -> Flt64(grbModel.get(GRB.DoubleAttr.MIPGap))
                    status == SolverStatus.Optimal -> Flt64.zero
                    else -> null
                }
                output = status.toSolveReport(
                    objective = Flt64(grbModel.get(GRB.DoubleAttr.ObjVal)),
                    values = results,
                    solveTime = grbModel.get(GRB.DoubleAttr.Runtime).seconds,
                    bestBound = possibleBestObj,
                    gap = gap,
                    iterations = nativeIterationsOrNull(),
                    nodes = nativeNodesOrNull(),
                    terminationReason = terminationReason
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
                output = status.toSolveReport(
                    solveTime = grbModel.get(GRB.DoubleAttr.Runtime).seconds,
                    bestBound = try {
                        Flt64(grbModel.get(GRB.DoubleAttr.ObjBound))
                    } catch (_: Exception) {
                        null
                    },
                    iterations = nativeIterationsOrNull(),
                    nodes = nativeNodesOrNull(),
                    terminationReason = terminationReason
                )
                ok
            }
        } catch (e: GRBException) {
            solverSolvingException(e.message)
        } catch (e: Exception) {
            solverSolvingException()
        }
    }
}
