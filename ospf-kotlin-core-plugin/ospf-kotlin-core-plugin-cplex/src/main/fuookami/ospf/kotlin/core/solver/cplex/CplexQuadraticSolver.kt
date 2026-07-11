/** CPLEX 二次求解器 / CPLEX Quadratic Solver */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.cplex

import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.concept.copyIfNotNullOr
import fuookami.ospf.kotlin.utils.functional.*
import ilog.concert.*
import ilog.cplex.IloCplex

/** CPLEX 二次求解器 / CPLEX quadratic solver */
class CplexQuadraticSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: CplexSolverCallBack? = null
) : QuadraticSolver {
    override val name = "cplex"

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
        return CplexQuadraticSolverImpl(
            config = config,
            callBack = callBack,
            statusCallBack = solvingStatusCallBack
        ).use { impl ->
            val result = impl(model)
            cleanupAfterSolverRun()
            result
        }
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
        return if (solutionAmount leq UInt64.one) {
            this(model).map { it to emptyList() }
        } else {
            val results = ArrayList<List<Flt64>>()
            CplexQuadraticSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { CplexSolverCallBack() }
                    .configuration { _, cplex, _, _ ->
                        if (solutionAmount gr UInt64.one) {
                            cplex.setParam(IloCplex.Param.MIP.Pool.Intensity, 4)
                            cplex.setParam(IloCplex.Param.MIP.Pool.AbsGap, 0.0)
                            cplex.setParam(IloCplex.Param.MIP.Pool.Capacity, solutionAmount.toInt())
                            cplex.setParam(IloCplex.Param.MIP.Pool.Replace, 2)
                            cplex.setParam(IloCplex.Param.MIP.Limits.Populate, solutionAmount.cub().toInt())
                        }
                        ok
                    }
                    .solving { _, cplex, _, _ ->
                        try {
                            cplex.populate()
                            ok
                        } catch (e: IloException) {
                            solverSolvingException(e.message)
                        }
                    }
                    .analyzingSolution { _, cplex, variables, _ ->
                        for (i in 0 until min(solutionAmount.toInt(), cplex.solnPoolNsolns)) {
                            val thisResults = variables.map { Flt64(cplex.getValue(it, i)) }
                            if (!results.any { it.toTypedArray() contentEquals thisResults.toTypedArray() }) {
                                results.add(thisResults)
                            }
                        }
                        ok
                    },
                statusCallBack = solvingStatusCallBack
            ).use { impl ->
                val result = impl(model).map { Pair(it, results) }
                cleanupAfterSolverRun()
                result
            }
        }
    }
}

/** CPLEX 二次求解器内部实现 / CPLEX quadratic solver internal implementation */
private class CplexQuadraticSolverImpl(
    private val config: SolverConfig,
    private val callBack: CplexSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null
) : CplexSolver() {
    private lateinit var cplexVars: List<IloNumVar>
    private lateinit var cplexConstraints: List<IloRange>
    private lateinit var output: FeasibleSolverOutput<Flt64>

    private var initialBestObj: Flt64? = null
    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    suspend operator fun invoke(model: QuadraticTetradModelView): Ret<FeasibleSolverOutput<Flt64>> {
        val processes = arrayOf(
            { it.init(model.name) },
            { it.dump(model) },
            { it.configure(model) },
            CplexQuadraticSolverImpl::solve,
            CplexQuadraticSolverImpl::analyzeStatus,
            { it.analyzeSolution(model) }
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
     * Dump the quadratic model to CPLEX
     * 将二次模型转储到 CPLEX
     *
     * @param model quadratic model view / 二次模型视图
     * @return operation result / 操作结果
    */
    private suspend fun dump(model: QuadraticTetradModelView): Try {
        warnIgnoredConstraintPriority("cplex", model.nonNullConstraintPriorityAmount())

        val variableDumpingData = prepareVariableDumpingData(
            variables = model.variables,
            scopeName = "quadratic"
        )
        val vars = ArrayList<IloNumVar>(model.variables.size)
        for (col in model.variables.indices) {
            vars.add(
                cplex.numVar(
                    variableDumpingData.lowerBounds[col],
                    variableDumpingData.upperBounds[col],
                    CplexVariable(model.variables[col].type).toCplexVar()
                )
            )
        }
        cplexVars = vars

        if (cplex.isMIP && variableDumpingData.initialResults.isNotEmpty()) {
            val initialVars = ArrayList<IloNumVar>(variableDumpingData.initialResults.size)
            val initialValues = DoubleArray(variableDumpingData.initialResults.size)
            for ((i, pair) in variableDumpingData.initialResults.withIndex()) {
                initialVars.add(cplexVars[pair.first])
                initialValues[i] = pair.second
            }
            cplex.addMIPStart(
                initialVars.toTypedArray(),
                initialValues
            )
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
                            var lb = Flt64.negativeInfinity
                            var ub = Flt64.infinity
                            when (model.constraints.signs[ii]) {
                                ConstraintRelation.GreaterEqual -> {
                                    lb = model.constraints.rhs[ii]
                                }

                                ConstraintRelation.LessEqual -> {
                                    ub = model.constraints.rhs[ii]
                                }

                                ConstraintRelation.Equal -> {
                                    lb = model.constraints.rhs[ii]
                                    ub = model.constraints.rhs[ii]
                                }
                            }
                            val lhs = cplex.lqNumExpr()
                            model.constraints.sparseLhs.forEachEntry(ii) { colIndex1, colIndex2, coefficient ->
                                if (colIndex2 == null) {
                                    lhs.addTerm(coefficient.toSolverDouble("quadratic.constraints.lhs[$ii][$colIndex1].coefficient"), cplexVars[colIndex1])
                                } else {
                                    lhs.addTerm(
                                        coefficient.toSolverDouble("quadratic.constraints.lhs[$ii][$colIndex1,$colIndex2].coefficient"),
                                        cplexVars[colIndex1],
                                        cplexVars[colIndex2]
                                    )
                                }
                            }
                            ii to Triple(lb, lhs, ub)
                        }
                        cleanupOnSolverMemoryPressure()
                        constraints
                    }
                }
                promises.flatMap { promise ->
                    val result = promise.await().map {
                        val (lb, lhs, ub) = it.second
                        val cplexConstraint = cplex.range(
                            lb.toSolverDouble("quadratic.constraints.bounds[${it.first}].lower"),
                            lhs,
                            ub.toSolverDouble("quadratic.constraints.bounds[${it.first}].upper"),
                            model.constraints.names[it.first]
                        )
                        cplex.add(cplexConstraint)
                        cplexConstraint
                    }
                    cleanupOnSolverMemoryPressure()
                    result
                }
            } else {
                model.constraints.indices.map { i ->
                    var lb = Flt64.negativeInfinity
                    var ub = Flt64.infinity
                    when (model.constraints.signs[i]) {
                        ConstraintRelation.GreaterEqual -> {
                            lb = model.constraints.rhs[i]
                        }

                        ConstraintRelation.LessEqual -> {
                            ub = model.constraints.rhs[i]
                        }

                        ConstraintRelation.Equal -> {
                            lb = model.constraints.rhs[i]
                            ub = model.constraints.rhs[i]
                        }
                    }
                    val lhs = cplex.lqNumExpr()
                    model.constraints.sparseLhs.forEachEntry(i) { colIndex1, colIndex2, coefficient ->
                        if (colIndex2 == null) {
                            lhs.addTerm(coefficient.toSolverDouble("quadratic.constraints.lhs[$i][$colIndex1].coefficient"), cplexVars[colIndex1])
                        } else {
                            lhs.addTerm(coefficient.toSolverDouble("quadratic.constraints.lhs[$i][$colIndex1,$colIndex2].coefficient"), cplexVars[colIndex1], cplexVars[colIndex2])
                        }
                    }
                    val cplexConstraint = cplex.range(
                        lb.toSolverDouble("quadratic.constraints.bounds[$i].lower"),
                        lhs,
                        ub.toSolverDouble("quadratic.constraints.bounds[$i].upper"),
                        model.constraints.names[i]
                    )
                    cplex.add(cplexConstraint)
                    cplexConstraint
                }
            }
        }
        cleanupAfterSolverRun()
        cplexConstraints = constraints

        val objective = cplex.lqNumExpr()
        for (cell in model.objective.objective) {
            if (cell.colIndex2 == null) {
                objective.addTerm(
                    cell.coefficient.toSolverDouble("quadratic.objective.cells[${cell.colIndex1}].coefficient"),
                    cplexVars[cell.colIndex1]
                )
            } else {
                objective.addTerm(
                    cell.coefficient.toSolverDouble("quadratic.objective.cells[${cell.colIndex1},${cell.colIndex2}].coefficient"),
                    cplexVars[cell.colIndex1],
                    cplexVars[cell.colIndex2!!]
                )
            }
        }
        when (model.objective.category) {
            ObjectCategory.Minimum -> {
                cplex.add(cplex.minimize(objective))
            }

            ObjectCategory.Maximum -> {
                cplex.add(cplex.maximize(objective))
            }
        }

        when (val result = callBack?.execIfContain(
            point = Point.AfterModeling,
            status = null,
            cplex = cplex,
            variables = cplexVars,
            constraints = cplexConstraints
        )) {
            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            else -> {}
        }
        return ok
    }

    /**
     * Configure CPLEX solver parameters for quadratic model
     * 为二次模型配置 CPLEX 求解器参数
     *
     * @param model quadratic model view / 二次模型视图
     * @return operation result / 操作结果
    */
    private suspend fun configure(model: QuadraticTetradModelView): Try {
        cplex.setParam(IloCplex.Param.TimeLimit, config.time.toDouble(DurationUnit.SECONDS))
        cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, config.gap.toSolverDouble("quadratic.config.gap"))
        cplex.setParam(IloCplex.Param.Threads, config.threadNum.toInt())
        cplex.setParam(IloCplex.Param.OptimalityTarget, IloCplex.OptimalityTarget.OptimalGlobal)

        if (config.notImprovementTime != null || callBack?.nativeCallback != null || statusCallBack != null) {
            cplex.use(object : IloCplex.MIPInfoCallback() {
                override fun main() {
                    callBack?.nativeCallback?.invoke(this)

                    val currentObj = Flt64(incumbentObjValue)
                    val currentBound = Flt64(bestObjValue)
                    val currentTime = cplexTime.seconds
                    val currentBestSolution = cplexVars.map { Flt64(cplex.getValue(it)) }

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
                                solver = "cplex",
                                solverConfig = config,
                                intermediateModel = model,
                                solverModel = cplex,
                                solverCallBack = this,
                                objectCategory = when (cplex.objective.sense) {
                                    IloObjectiveSense.Minimize -> {
                                        ObjectCategory.Minimum
                                    }

                                    IloObjectiveSense.Maximize -> {
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
                                currentBestSolution = currentBestSolution
                            )
                        )
                        if (shouldAbortOnCallbackFailure(callbackResult) { abort() }) {
                            return
                        }
                    }

                    // todo: add lazy constraint
                }
            })
        }

        when (val result = callBack?.execIfContain(
            point = Point.Configuration,
            status = null,
            cplex = cplex,
            variables = cplexVars,
            constraints = cplexConstraints
        )) {
            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            else -> {}
        }
        return ok
    }

    /**
     * Execute CPLEX solving for quadratic model
     * 执行二次模型的 CPLEX 求解
     *
     * @return operation result / 操作结果
    */
    private suspend fun solve(): Try {
        when (val result = callBack?.execIfContain(
            point = Point.Solving,
            status = null,
            cplex = cplex,
            variables = cplexVars,
            constraints = cplexConstraints
        )) {
            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            null -> {
                try {
                    cplex.solve()
                } catch (e: IloException) {
                    return solverSolvingException(e.message)
                }
                return ok
            }

            else -> {}
        }
        return ok
    }

/**
 * Analyze the CPLEX solving result and extract the solution output.
 * 分析 CPLEX 求解结果并提取解输出。
 *
 * @param model the quadratic model view used for solving / 用于求解的二次模型视图
 * @return success if solution was extracted, or failure if solving failed / 成功时返回提取结果，求解失败时返回失败
*/
    private suspend fun analyzeSolution(model: QuadraticTetradModelView): Try {
        return if (status.succeeded) {
            val obj = Flt64(cplex.objValue) + model.objective.constant
            val possibleBestObj = Flt64(cplex.bestObjValue) + model.objective.constant
            output = FeasibleSolverOutput<Flt64>(
                obj = obj,
                solution = cplexVars.map { Flt64(cplex.getValue(it)) },
                time = cplex.cplexTime.seconds,
                possibleBestObj = possibleBestObj,
                gap = if (cplex.isMIP) {
                    gap(obj, possibleBestObj)
                } else {
                    Flt64.zero
                }
            )

            when (val result = callBack?.execIfContain(
                point = Point.AnalyzingSolution,
                status = status,
                cplex = cplex,
                variables = cplexVars,
                constraints = cplexConstraints
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
                cplex = cplex,
                variables = cplexVars,
                constraints = cplexConstraints
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
    }
}

