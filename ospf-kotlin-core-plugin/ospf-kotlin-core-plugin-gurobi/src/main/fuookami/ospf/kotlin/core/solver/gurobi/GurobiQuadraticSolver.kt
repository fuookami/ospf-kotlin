/** Gurobi 二次求解器 / Gurobi Quadratic Solver */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.core.model.basic.nonNullConstraintPriorityAmount
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
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

/** Gurobi 二次求解器 / Gurobi quadratic solver */
class GurobiQuadraticSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: GurobiQuadraticSolverCallBack? = null
) : QuadraticSolver {
    override val name = "gurobi"
    override val descriptor = SolverDescriptor(
        solverId = "gurobi",
        backendName = "Gurobi",
        backendVersion = gurobiNativeVersion(),
        pluginVersion = GurobiQuadraticSolver::class.java.`package`.implementationVersion,
        capabilities = SolverCapabilities(
            modelTypes = setOf(SolverModelType.QP, SolverModelType.QCP),
            nativeIIS = true,
            warmStart = true,
            solutionPool = true,
            callback = true,
            interrupt = true
        )
    )

    override fun diagnosticAnalyzers(
        config: IISConfig
    ): List<InfeasibilityAnalyzer<QuadraticTetradModelView>> {
        return listOf(
            GurobiQuadraticNativeIISAnalyzer(this.config, config, callBack)
        )
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolveReport<Flt64>> {
        return invoke(model, solvingStatusCallBack, null)
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?,
        cancellationToken: CancellationToken?
    ): Ret<SolveReport<Flt64>> {
        when (val validation = model.identityValidation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return GurobiQuadraticSolverImpl(
            config = config,
            callBack = callBack,
            statusCallBack = solvingStatusCallBack,
            cancellationToken = cancellationToken
        ).use { impl ->
            val result = impl(model)
            cleanupAfterSolverRun()
            result.map { report ->
                report.withQuadraticBackendMetadata(model, config, descriptor)
            }
        }
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        return invoke(model, solutionAmount, solvingStatusCallBack, null)
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?,
        cancellationToken: CancellationToken?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        return if (solutionAmount leq UInt64.one) {
            this(model, solvingStatusCallBack, cancellationToken).map { it to emptyList() }
        } else {
            val results = ArrayList<List<Flt64>>()
            GurobiQuadraticSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { GurobiQuadraticSolverCallBack() }
                    .configuration { _, gurobi, _, _ ->
                        if (solutionAmount gr UInt64.one) {
                            gurobi.set(GRB.DoubleParam.PoolGap, 1.0);
                            gurobi.set(GRB.IntParam.PoolSearchMode, 2);
                            gurobi.set(GRB.IntParam.PoolSolutions, solutionAmount.toInt())
                        }
                        ok
                    }
                    .analyzingSolution { _, gurobi, variables, _ ->
                        for (i in 0 until kotlin.math.min(solutionAmount.toInt(), gurobi.get(GRB.IntAttr.SolCount))) {
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
                    report.withQuadraticBackendMetadata(model, config, descriptor) to solutions
                }
            }
        }
    }
}

/** Gurobi 二次求解器内部实现 / Gurobi quadratic solver internal implementation */
private class GurobiQuadraticSolverImpl(
    private val config: SolverConfig,
    private val callBack: GurobiQuadraticSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null,
    private val cancellationToken: CancellationToken? = null
) : GurobiSolver() {
    private lateinit var grbVars: List<GRBVar>
    private lateinit var grbConstraints: List<GRBQConstr>
    private lateinit var output: SolveReport<Flt64>

    private var initialBestObj: Flt64? = null
    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestSolution: List<Flt64>? = null
    private var bestTime: Duration = Duration.ZERO

    suspend operator fun invoke(model: QuadraticTetradModelView): Ret<SolveReport<Flt64>> {
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
            GurobiQuadraticSolverImpl::solve,
            { it.analyzeStatus(cancellationToken) },
            GurobiQuadraticSolverImpl::analyzeSolution
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
 * Dump the quadratic model into Gurobi variables, constraints, and objective.
 * 将二次模型转储为 Gurobi 变量、约束和目标函数。
 *
 * @param model 待转储的二次模型视图 / the quadratic model view to dump
 * @return 转储成功返回成功，建模错误返回失败 / success if model was dumped, or failure on modeling error
*/
    private suspend fun dump(model: QuadraticTetradModelView): Try {
        return try {
            warnIgnoredConstraintPriority("gurobi", model.nonNullConstraintPriorityAmount())

            val variableDumpingData = prepareVariableDumpingData(
                variables = model.variables,
                scopeName = "quadratic"
            )
            val variableAmount = model.variables.size
            val variableTypes = CharArray(variableAmount)
            for (col in model.variables.indices) {
                variableTypes[col] = GurobiVariable(model.variables[col].type).toGurobiVar()
            }
            val variableNames = Array(variableAmount) { col ->
                nativeElementName(
                    identityId = model.variables[col].id?.value,
                    fallbackName = variableDumpingData.names[col],
                    category = "variable",
                    identityScope = model.variables[col].identityScope
                )
            }
            grbVars = grbModel.addVars(
                variableDumpingData.lowerBounds,
                variableDumpingData.upperBounds,
                null,
                variableTypes,
                variableNames,
                0,
                variableAmount
            ).toList()

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
                                val lhs = GRBQuadExpr()
                                model.constraints.sparseLhs.forEachEntry(ii) { colIndex1, colIndex2, coefficient ->
                                    if (colIndex2 == null) {
                                        lhs.addTerm(
                                            coefficient.toSolverDouble("quadratic.constraints.lhs[$ii][$colIndex1].coefficient"),
                                            grbVars[colIndex1]
                                        )
                                    } else {
                                        lhs.addTerm(
                                            coefficient.toSolverDouble("quadratic.constraints.lhs[$ii][$colIndex1,$colIndex2].coefficient"),
                                            grbVars[colIndex1],
                                            grbVars[colIndex2]
                                        )
                                    }
                                }
                                ii to lhs
                            }
                            cleanupOnSolverMemoryPressure()
                            constraints
                        }
                    }
                    promises.flatMap { promise ->
                        val result = promise.await().map {
                            grbModel.addQConstr(
                                it.second,
                                GurobiConstraintSign(model.constraints.signs[it.first]).toGurobiConstraintSign(),
                                model.constraints.rhs[it.first].toSolverDouble("quadratic.constraints.rhs[${it.first}]"),
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
                        val lhs = GRBQuadExpr()
                        model.constraints.sparseLhs.forEachEntry(i) { colIndex1, colIndex2, coefficient ->
                            if (colIndex2 == null) {
                                lhs.addTerm(
                                    coefficient.toSolverDouble("quadratic.constraints.lhs[$i][$colIndex1].coefficient"),
                                    grbVars[colIndex1]
                                )
                            } else {
                                lhs.addTerm(
                                    coefficient.toSolverDouble("quadratic.constraints.lhs[$i][$colIndex1,$colIndex2].coefficient"),
                                    grbVars[colIndex1],
                                    grbVars[colIndex2]
                                )
                            }
                        }
                        grbModel.addQConstr(
                            lhs,
                            GurobiConstraintSign(model.constraints.signs[i]).toGurobiConstraintSign(),
                            model.constraints.rhs[i].toSolverDouble("quadratic.constraints.rhs[$i]"),
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

            val obj = GRBQuadExpr()
            for (cell in model.objective.objective) {
                if (cell.colIndex2 == null) {
                    obj.addTerm(
                        cell.coefficient.toSolverDouble("quadratic.objective.cells[${cell.colIndex1}].coefficient"),
                        grbVars[cell.colIndex1]
                    )
                } else {
                    obj.addTerm(
                        cell.coefficient.toSolverDouble("quadratic.objective.cells[${cell.colIndex1},${cell.colIndex2}].coefficient"),
                        grbVars[cell.colIndex1],
                        grbVars[cell.colIndex2!!]
                    )
                }
            }
            obj.addConstant(model.objective.constant.toSolverDouble("quadratic.objective.constant"))
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
 * Configure Gurobi solver parameters for the quadratic model.
 * 为二次模型配置 Gurobi 求解器参数。
 *
 * @param model 待配置的二次模型视图 / the quadratic model view to configure
 * @return 配置成功返回成功，出错返回失败 / success if configuration was applied, or failure on error
*/
    private suspend fun configure(model: QuadraticTetradModelView): Try {
        return try {
            when (val cancellation = registerCancellation(cancellationToken)) {
                is Failed -> return cancellation
                is Fatal -> return cancellation
                else -> {}
            }
            grbModel.set(GRB.DoubleParam.TimeLimit, config.time.toDouble(DurationUnit.SECONDS))
            grbModel.set(GRB.DoubleParam.MIPGap, config.gap.toSolverDouble("quadratic.config.gap"))
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
                                        time = currentTime,
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
 * Analyze the Gurobi solving result and extract the solution output.
 * 分析 Gurobi 求解结果并提取解输出。
 *
 * @return 成功时返回提取结果，求解失败时返回失败 / success if solution was extracted, or failure if solving failed
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
