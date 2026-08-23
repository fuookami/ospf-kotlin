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
import fuookami.ospf.kotlin.core.solver.iis.FarkasInfeasibilityAnalyzer
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.iis.InfeasibilityAnalyzer
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.concept.copyIfNotNullOr
import fuookami.ospf.kotlin.utils.functional.*
import jscip.*

/**
 * SCIP linear solver
 *
 : SCIP 线性求解器
 *
 * @property config 求解器配置 / solver configuration
 * @property callBack 求解器回调 / solver callback
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
         * 中文从 JAR 包中加载 SCIP 原生库
         *
         * @return 以Try包装的加载结果 / the load result as Try
        */
        @JvmStatic
        fun loadLibraryInJar(): Try {
            return ScipSolver.loadLibraryInJar()
        }
    }

    override val name = "scip"
    override val descriptor = SolverDescriptor(
        solverId = "scip",
        backendName = "SCIP",
        backendVersion = ScipSolver.runtimeVersion(),
        pluginVersion = ScipLinearSolver::class.java.`package`.implementationVersion,
        capabilities = SolverCapabilities(
            modelTypes = setOf(SolverModelType.LP, SolverModelType.MIP),
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
        return listOf(ScipFarkasInfeasibilityAnalyzer(this.config, config, callBack))
    }

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
        return ScipLinearSolverImpl(
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

/**
 * SCIP linear solver implementation
 *
 * SCIP 线性求解器实现
 *
 * @property config 求解器配置 / solver configuration
 * @property callBack 求解器回调 / solver callback
 * @property statusCallBack 求解状态回调 / solving status callback
*/
private class ScipLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: ScipSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null,
    private val cancellationToken: CancellationToken? = null
) : ScipSolver() {
    private var mip: Boolean = false

    private lateinit var scipVars: List<jscip.Variable>
    private lateinit var scipConstraints: List<jscip.Constraint>
    private lateinit var output: SolveReport<Flt64>
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

    suspend operator fun invoke(model: LinearTriadModelView): Ret<SolveReport<Flt64>> {
        if (cancellationToken?.isCancellationRequested == true) {
            return Ok(cancelledSolveReport(cancellationToken.record?.reason))
        }
        mip = model.containsNotBinaryInteger
        val processes: Array<suspend (ScipLinearSolverImpl) -> Try> = arrayOf(
            { solver -> solver.init(model.name) },
            { solver -> solver.dump(model) },
            { solver -> solver.configure(model) },
            { solver -> solver.solve(config.threadNum) },
            { solver -> solver.analyzeStatus(cancellationToken) },
            { solver -> solver.analyzeSolution(model) }
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
     * 将线性模型导出到 SCIP
     *
     * @param model 线性三元模型视图 / linear triad model view
     * @return 操作结果 / operation result
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
                    nativeElementName(
                        identityId = model.variables[col].id?.value,
                        fallbackName = variableDumpingData.names[col],
                        category = "variable",
                        identityScope = model.variables[col].identityScope
                    ),
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
                            nativeElementName(
                                identityId = model.constraints.ids.getOrNull(it.first)?.value,
                                fallbackName = model.constraints.names[it.first],
                                category = "constraint",
                                identityScope = model.constraints.identityScopeAt(it.first)
                            ),
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
                        nativeElementName(
                            identityId = model.constraints.ids.getOrNull(i)?.value,
                            fallbackName = model.constraints.names[i],
                            category = "constraint",
                            identityScope = model.constraints.identityScopeAt(i)
                        ),
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
     * 配置 SCIP 求解器参数
     *
     * @param model 线性三元模型视图 / linear triad model view
     * @return 操作结果 / operation result
    */
    private suspend fun configure(model: LinearTriadModelView): Try {
        when (val cancellation = registerCancellation(cancellationToken)) {
            is Failed -> return cancellation
            is Fatal -> return cancellation
            else -> {}
        }
        scip.setRealParam("limits/time", config.time.toDouble(DurationUnit.SECONDS))
        scip.setRealParam("limits/gap", config.gap.toSolverDouble("linear.config.gap"))
        scip.setIntParam("parallel/maxnthreads", config.threadNum.toInt())
        when (val backendConfiguration = applyBackendConfiguration(config.backendConfiguration, config.threadNum.toInt())) {
            is Failed -> return Failed(backendConfiguration.error)
            is Fatal -> return Fatal(backendConfiguration.errors)
            else -> Unit
        }

        if (config.notImprovementTime != null || callBack?.nativeCallback != null ||
            statusCallBack != null || cancellationToken != null) {
            object : EventHandler(
                scip,
                "solve-monitor-${UUID.randomUUID()}",
                "native solving callback",
                callBack?.nativeEventMask ?: (EventMask.LP_EVENT or EventMask.NODE_EVENT or EventMask.SOL_EVENT)
            ) {
                override fun execute(event: Event) {
                    val solverModel = scip
                    if (cancellationToken?.isCancellationRequested == true) {
                        solverModel.interruptSolve()
                        return
                    }
                    try {
                        callBack?.nativeCallback?.invoke(this, solverModel, event)
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
                        } else if (currentTime - bestTime >= notImprovementTime
                            && config.interruptibleTime?.let { currentTime >= it } ?: true
                            && config.interruptibleGap?.let { (currentObj - currentBound).abs() ls it } ?: true
                        ) {
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
                            SolvingStatus(
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
            }.include()
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
     * 分析求解结果并提取解
     *
     * @param model 线性三元模型视图 / linear triad model view
     * @return 操作结果 / operation result
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
            } else if (status == SolverStatus.Optimal) {
                Flt64.zero
            } else {
                null
            }
            output = status.toSolveReport(
                objective = obj,
                values = results,
                solveTime = solvingTime!!,
                bestBound = possibleBestObj,
                gap = gap,
                terminationReason = terminationReason
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
            output = status.toSolveReport(
                solveTime = solvingTime ?: kotlin.time.Duration.ZERO,
                bestBound = Flt64(scip.dualbound),
                terminationReason = terminationReason
            )
            ok
        }
    }
}

