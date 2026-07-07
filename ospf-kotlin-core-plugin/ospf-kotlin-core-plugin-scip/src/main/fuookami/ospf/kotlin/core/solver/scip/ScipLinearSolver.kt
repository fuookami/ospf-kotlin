/** SCIP 线性求解器 / SCIP Linear Solver */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.scip

import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.concept.copyIfNotNullOr
import fuookami.ospf.kotlin.utils.functional.*
import jscip.*

/**
 * SCIP linear solver
 *
 * 中文: SCIP 线性求解器
 *
 * @property config solver configuration / 求解器配置
 * @property callBack solver callback / 求解器回调
 */
class ScipLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: ScipSolverCallBack? = null
) : LinearSolver {
    /** Companion object providing library loading utility / 伴生对象，提供库加载工具 */
    companion object {
        /**
         * Load SCIP native library from JAR package
         *
         * 中文: 从 JAR 包中加载 SCIP 原生库
         *
         * @return operation result / 操作结果
         */
        @JvmStatic
        fun loadLibraryInJar(): Try {
            return ScipSolver.loadLibraryInJar()
        }
    }

    override val name = "scip"

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
        return ScipLinearSolverImpl(
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
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
        return if (solutionAmount leq UInt64.one) {
            this(model).map { it to emptyList() }
        } else {
            val results = ArrayList<List<Flt64>>()
            ScipLinearSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { ScipSolverCallBack() }
                    .configuration { _, scip, _, _ ->
                        if (solutionAmount gr UInt64.one) {
                            scip.setIntParam("heuristics/dins/solnum", solutionAmount.toInt())
                        }
                        ok
                    }
                    .analyzingSolution { _, scip, variables, _ ->
                        val bestSol = scip.bestSol
                        val sols = scip.sols
                        var i = UInt64.zero
                        for (sol in sols) {
                            if (sol != bestSol) {
                                val thisResults = ArrayList<Flt64>()
                                for (scipVar in variables) {
                                    thisResults.add(Flt64(scip.getSolVal(sol, scipVar)))
                                }
                                if (!results.any { it.toTypedArray() contentEquals thisResults.toTypedArray() }) {
                                    results.add(thisResults)
                                }
                            }
                            ++i
                            if (i >= solutionAmount) {
                                break
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

/**
 * SCIP linear solver implementation
 *
 * 中文: SCIP 线性求解器实现
 *
 * @property config solver configuration / 求解器配置
 * @property callBack solver callback / 求解器回调
 * @property statusCallBack solving status callback / 求解状态回调
 */
private class ScipLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: ScipSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null
) : ScipSolver() {
    private var mip: Boolean = false

    private lateinit var scipVars: List<jscip.Variable>
    private lateinit var scipConstraints: List<jscip.Constraint>
    private lateinit var output: FeasibleSolverOutput<Flt64>
    private var initialBestObj: Flt64? = null
    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime = 0.0.seconds

    override fun close() {
        for (constraint in scipConstraints) {
            scip.releaseCons(constraint)
        }
        for (variable in scipVars) {
            scip.releaseVar(variable)
        }
        super.close()
    }

    suspend operator fun invoke(model: LinearTriadModelView): Ret<FeasibleSolverOutput<Flt64>> {
        mip = model.containsNotBinaryInteger
        val processes = arrayOf(
            { it.init(model.name) },
            { it.dump(model) },
            { it.configure(model) },
            { it.solve(config.threadNum) },
            ScipLinearSolverImpl::analyzeStatus,
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
     * Dump the linear model into SCIP
     *
     * 中文: 将线性模型导出到 SCIP
     *
     * @param model linear triad model view / 线性三元模型视图
     * @return operation result / 操作结果
     */
    private suspend fun dump(model: LinearTriadModelView): Try {
        warnIgnoredConstraintPriority("scip", model.nonNullConstraintPriorityAmount())

        val variableDumpingData = prepareVariableDumpingData(
            variables = model.variables,
            scopeName = "linear"
        )
        val vars = ArrayList<jscip.Variable>(model.variables.size)
        for (col in model.variables.indices) {
            vars.add(
                scip.createVar(
                    variableDumpingData.names[col],
                    variableDumpingData.lowerBounds[col],
                    variableDumpingData.upperBounds[col],
                    0.0,
                    ScipVariable(model.variables[col].type).toSCIPVar()
                )
            )
        }
        scipVars = vars

        if (variableDumpingData.initialResults.size == model.variables.size) {
            val initialSolution = scip.createSol()
            for ((col, initialResult) in variableDumpingData.initialResults) {
                scip.setSolVal(initialSolution, scipVars[col], initialResult)
            }
            scip.addSolFree(initialSolution)
        } else if (variableDumpingData.initialResults.isNotEmpty()) {
            val initialSolution = scip.createPartialSol()
            for ((col, initialResult) in variableDumpingData.initialResults) {
                scip.setSolVal(initialSolution, scipVars[col], initialResult)
            }
            scip.addSolFree(initialSolution)
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
                            val vars = ArrayList<jscip.Variable>()
                            val coefficients = ArrayList<Double>()
                            model.constraints.sparseLhs.forEachEntry(ii) { colIndex, coefficient ->
                                vars.add(scipVars[colIndex])
                                coefficients.add(coefficient.toSolverDouble("linear.constraints.lhs[$ii][$colIndex].coefficient"))
                            }
                            ii to Triple(lb, coefficients to vars, ub)
                        }
                        cleanupOnSolverMemoryPressure()
                        constraints
                    }
                }
                promises.flatMap { promise ->
                    val result = promise.await().map {
                        val (lb, cells, ub) = it.second
                        val (coefficients, vars) = cells
                        val constraint = scip.createConsLinear(
                            model.constraints.names[it.first],
                            vars.toTypedArray(),
                            coefficients.toDoubleArray(),
                            lb.toSolverDouble("linear.constraints.bounds[${it.first}].lower"),
                            ub.toSolverDouble("linear.constraints.bounds[${it.first}].upper")
                        )
                        scip.addCons(constraint)
                        constraint
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
                    val vars = ArrayList<jscip.Variable>()
                    val coefficients = ArrayList<Double>()
                    model.constraints.sparseLhs.forEachEntry(i) { colIndex, coefficient ->
                        vars.add(scipVars[colIndex])
                        coefficients.add(coefficient.toSolverDouble("linear.constraints.lhs[$i][$colIndex].coefficient"))
                    }
                    val constraint = scip.createConsLinear(
                        model.constraints.names[i],
                        vars.toTypedArray(),
                        coefficients.toDoubleArray(),
                        lb.toSolverDouble("linear.constraints.bounds[$i].lower"),
                        ub.toSolverDouble("linear.constraints.bounds[$i].upper")
                    )
                    scip.addCons(constraint)
                    constraint
                }
            }
        }
        cleanupAfterSolverRun()
        scipConstraints = constraints

        for (cell in model.objective.objective) {
            scip.changeVarObj(scipVars[cell.colIndex], cell.coefficient.toSolverDouble("linear.objective.cells[${cell.colIndex}].coefficient"))
        }
        when (model.objective.category) {
            ObjectCategory.Minimum -> {
                scip.setMinimize()
            }

            ObjectCategory.Maximum -> {
                scip.setMaximize()
            }
        }

        when (val result = callBack?.execIfContain(
            point = Point.AfterModeling,
            status = null,
            scip = scip,
            variables = scipVars,
            constraints = scipConstraints
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
     * Configure SCIP solver parameters
     *
     * 中文: 配置 SCIP 求解器参数
     *
     * @param model linear triad model view / 线性三元模型视图
     * @return operation result / 操作结果
     */
    private suspend fun configure(model: LinearTriadModelView): Try {
        scip.setRealParam("limits/time", config.time.toDouble(DurationUnit.SECONDS))
        scip.setRealParam("limits/gap", config.gap.toSolverDouble("linear.config.gap"))
        scip.setIntParam("parallel/maxnthreads", config.threadNum.toInt())

        if (config.notImprovementTime != null || callBack?.nativeCallback != null || statusCallBack != null) {
            scip.includeEventHandler("solve-monitor-${UUID.randomUUID()}", "native solving callback", object : EventHandler() {
                override fun getType(): Long {
                    return callBack?.nativeEventMask ?: (EventMask.LP_EVENT or EventMask.NODE_EVENT or EventMask.SOL_EVENT)
                }

                override fun execute(solverModel: jscip.Scip, self: EventHandlerRef, event: Event) {
                    try {
                        callBack?.nativeCallback?.invoke(this, solverModel, self, event)
                    } catch (_: Exception) {
                        solverModel.interruptSolve()
                        return
                    }

                    val bestSolution = solverModel.bestSol
                    val currentObj = if (bestSolution == null) {
                        Flt64(solverModel.primalbound)
                    } else {
                        Flt64(solverModel.getSolOrigObj(bestSolution))
                    }
                    val currentBound = Flt64(solverModel.dualbound)
                    val currentTime = solverModel.solvingTime.seconds

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
                            solverModel.interruptSolve()
                            return
                        }
                    }

                    statusCallBack?.let {
                        val currentBestSolution = if (bestSolution == null) {
                            null
                        } else {
                            scipVars.map { variable -> Flt64(solverModel.getSolVal(bestSolution, variable)) }
                        }
                        val callbackResult = it(
                            fuookami.ospf.kotlin.core.solver.output.SolvingStatus(
                                solver = "scip",
                                solverConfig = config,
                                intermediateModel = model,
                                solverModel = solverModel,
                                solverCallBack = this,
                                objectCategory = model.objective.category,
                                time = currentTime,
                                obj = currentObj,
                                possibleBestObj = currentBound,
                                bestBound = currentBound,
                                initialBestObj = initialBestObj ?: currentObj,
                                gap = (currentObj - currentBound + Flt64.decimalPrecision) / (currentObj + Flt64.decimalPrecision),
                                currentBestSolution = currentBestSolution
                            )
                        )
                        if (shouldAbortOnCallbackFailure(callbackResult) { solverModel.interruptSolve() }) {
                            return
                        }
                    }
                }
            })
        }

        scip.messagehdlr

        when (val result = callBack?.execIfContain(
            point = Point.Configuration,
            status = null,
            scip = scip,
            variables = scipVars,
            constraints = scipConstraints
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
     * Analyze the solving result and extract solution
     *
     * 中文: 分析求解结果并提取解
     *
     * @param model linear triad model view / 线性三元模型视图
     * @return operation result / 操作结果
     */
    private suspend fun analyzeSolution(model: LinearTriadModelView): Try {
        return if (status.succeeded) {
            val solution = scip.bestSol
            val results = ArrayList<Flt64>()
            for (scipVar in scipVars) {
                results.add(Flt64(scip.getSolVal(solution, scipVar)))
            }
            val obj = Flt64(scip.getSolOrigObj(solution)) + model.objective.constant
            val possibleBestObj = Flt64(scip.dualbound) + model.objective.constant
            val gap = if (mip) {
                gap(obj, possibleBestObj)
            } else {
                Flt64.zero
            }
            output = FeasibleSolverOutput<Flt64>(
                obj = obj,
                solution = results,
                time = solvingTime!!,
                possibleBestObj = possibleBestObj,
                gap = gap
            )

            when (val result = callBack?.execIfContain(
                point = Point.AnalyzingSolution,
                status = status,
                scip = scip,
                variables = scipVars,
                constraints = scipConstraints
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
        } else {
            when (val result = callBack?.execIfContain(
                point = Point.AfterFailure,
                status = status,
                scip = scip,
                variables = scipVars,
                constraints = scipConstraints
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

