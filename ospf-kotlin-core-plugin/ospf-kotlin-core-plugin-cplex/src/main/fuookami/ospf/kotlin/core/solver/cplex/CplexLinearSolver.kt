/** CPLEX 线性求解器 / CPLEX Linear Solver */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.cplex

import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger
import ilog.concert.*
import ilog.cplex.IloCplex
import fuookami.ospf.kotlin.utils.concept.copyIfNotNullOr
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/** CPLEX 线性求解器 / CPLEX linear solver */
class CplexLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: CplexSolverCallBack? = null
) : LinearSolver {
    internal var nativePiecewiseWriter: (IloCplex, Map<VariableItemKey, IloNumVar>, List<NativePiecewiseData>) -> Try =
        ::addCplexNativePiecewise

    /**
     * 在最终编号前选择原生 SOS2，写入失败时重建完整 fallback。 /
     * Select native SOS2 before indexing and rebuild the complete fallback after a write failure.
     *
     * @param model 机制模型 / Mechanism model
     * @param converter 结果转换器 / Result converter
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解报告或错误 / Solve report or error
     */
    override suspend fun <V> solve(
        model: MechanismModel<V>,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
        if (config.functionExpansionPolicy == FunctionExpansionPolicy.EAGER ||
            model !is LinearMechanismModel<V> || model.functionExpansionPolicy == FunctionExpansionPolicy.EAGER
        ) {
            return super<LinearSolver>.solve(model, converter, solvingStatusCallBack)
        }
        val converted = when (val result = convertMechanismModelToFlt64(model)) {
            is Ok -> result.value as? LinearMechanismModel<Flt64>
                ?: return super<LinearSolver>.solve(model, converter, solvingStatusCallBack)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        try {
            val candidates = selectCplexNativePiecewise(converted)
            if (candidates.isEmpty()) {
                return super<LinearSolver>.solve(model, converter, solvingStatusCallBack)
            }
            val nativeData = ArrayList<NativePiecewiseData>(candidates.size)
            for (structure in candidates) {
                when (val prepared = prepareNativePiecewise(structure)) {
                    is Ok -> nativeData += prepared.value
                    is Failed -> return Failed(prepared.error)
                    is Fatal -> return Fatal(prepared.errors)
                }
            }
            val nativeModel = when (val result = LinearTriadModel.invokeResult(
                model = converted,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
                forceDumpBounds = config.dumpIntermediateModelForceBounds,
                concurrent = config.dumpIntermediateModelConcurrent,
                nativeFunctionKeys = candidates.map { it.resultVariable.key }.toSet()
            )) {
                is Ok -> result.value
                is Failed, is Fatal -> return super<LinearSolver>.solve(model, converter, solvingStatusCallBack)
            }
            val attempt = nativeModel.use {
                CplexLinearSolverImpl(
                    config = config,
                    callBack = callBack,
                    statusCallBack = solvingStatusCallBack,
                    nativePiecewiseData = nativeData,
                    nativePiecewiseWriter = nativePiecewiseWriter
                ).use { implementation ->
                    val result = implementation(it)
                    if (implementation.nativePiecewiseFailed) {
                        null
                    } else {
                        when (result) {
                            is Ok -> restoreNativePiecewiseSolution(
                                report = result.value.withLinearBackendMetadata(it, config, descriptor),
                                nativeModel = it,
                                originalTokens = converted.tokens.tokensInSolver,
                                structures = candidates,
                                backendName = "cplex"
                            )
                            is Failed -> Failed(result.error)
                            is Fatal -> Fatal(result.errors)
                        }
                    }
                }
            }
            return when (attempt) {
                null -> super<LinearSolver>.solve(model, converter, solvingStatusCallBack)
                is Ok -> Ok(attempt.value.convertTo(converter))
                is Failed -> Failed(attempt.error)
                is Fatal -> Fatal(attempt.errors)
            }
        } finally {
            converted.close()
        }
    }

    override val name = "cplex"

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
        when (val validation = model.identityValidation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return CplexLinearSolverImpl(
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
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        when (val validation = model.identityValidation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return if (solutionAmount leq UInt64.one) {
            this(model).map { it to emptyList() }
        } else {
            val results = ArrayList<List<Flt64>>()
            CplexLinearSolverImpl(
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

/** CPLEX 线性求解器内部实现 / CPLEX linear solver internal implementation */
private class CplexLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: CplexSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack?,
    private val nativePiecewiseData: List<NativePiecewiseData> = emptyList(),
    private val nativePiecewiseWriter: (IloCplex, Map<VariableItemKey, IloNumVar>, List<NativePiecewiseData>) -> Try =
        ::addCplexNativePiecewise
) : CplexSolver() {
    var nativePiecewiseFailed: Boolean = false
        private set

    private lateinit var cplexVars: List<IloNumVar>
    private lateinit var cplexConstraints: List<IloRange>
    private lateinit var output: SolveReport<Flt64>

    private var initialBestObj: Flt64? = null
    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    private val logger = logger()

    /**
     * 执行求解流程 / Execute solving process
     *
     * @param model 线性模型视图 / linear model view
     * @return 求解结果 / solving result
    */
    suspend operator fun invoke(model: LinearTriadModelView): Ret<SolveReport<Flt64>> {
        val processes = arrayOf(
            { it.init(model.name) },
            { it.dump(model) },
            { it.configure(model) },
            CplexLinearSolverImpl::solve,
            CplexLinearSolverImpl::analyzeStatus,
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
     * 将模型转储到 CPLEX / Dump model to CPLEX
     *
     * @param model 线性模型视图 / linear model view
     * @return 操作结果 / operation result
    */
    private suspend fun dump(model: LinearTriadModelView): Try {
        logger.trace { "Dumping to cplex model for $model" }
        warnIgnoredConstraintPriority("cplex", model.nonNullConstraintPriorityAmount())

        val variableDumpingData = prepareVariableDumpingData(
            variables = model.variables,
            scopeName = "linear"
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

        if (nativePiecewiseData.isNotEmpty()) {
            val variablesByKey = model.variables.mapIndexedNotNull { index, variable ->
                variable.origin?.key?.let { it to cplexVars[index] }
            }.toMap()
            val nativeWrite = try {
                NativeFunctionWriterRegistry(
                    listOf(
                        NativeFunctionWriter<IloCplex, IloNumVar> { nativeModel, nativeVariables, batch ->
                            nativePiecewiseWriter(
                                nativeModel,
                                nativeVariables,
                                batch.filterIsInstance<NativePiecewiseData>()
                            )
                        }
                    )
                ).write(
                    model = cplex,
                    variables = variablesByKey,
                    batches = listOf(nativePiecewiseData.map { it as Any })
                )
            } catch (error: LinkageError) {
                Failed(
                    ErrorCode.OREngineModelingException,
                    "CPLEX 原生 PWL SDK 不可用：${error.message ?: error::class.simpleName} / " +
                        "CPLEX native PWL SDK is unavailable: ${error.message ?: error::class.simpleName}"
                )
            } catch (error: Exception) {
                Failed(
                    ErrorCode.OREngineModelingException,
                    "CPLEX 原生 PWL 写入异常：${error.message ?: error::class.simpleName} / " +
                        "CPLEX native PWL write raised an exception: ${error.message ?: error::class.simpleName}"
                )
            }
            when (nativeWrite) {
                is Ok -> {}
                is Failed -> {
                    nativePiecewiseFailed = true
                    return Failed(nativeWrite.error)
                }
                is Fatal -> {
                    nativePiecewiseFailed = true
                    return Fatal(nativeWrite.errors)
                }
            }
        }

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
                            val lhs = cplex.linearNumExpr()
                            model.constraints.sparseLhs.forEachEntry(ii) { colIndex, coefficient ->
                                lhs.addTerm(coefficient.toSolverDouble("linear.constraints.lhs[$ii][$colIndex].coefficient"), cplexVars[colIndex])
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
                        val constraint = cplex.range(
                            lb.toSolverDouble("linear.constraints.bounds[${it.first}].lower"),
                            lhs,
                            ub.toSolverDouble("linear.constraints.bounds[${it.first}].upper"),
                            model.constraints.names[it.first]
                        )
                        cplex.add(constraint)
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
                    val lhs = cplex.linearNumExpr()
                    model.constraints.sparseLhs.forEachEntry(i) { colIndex, coefficient ->
                        lhs.addTerm(coefficient.toSolverDouble("linear.constraints.lhs[$i][$colIndex].coefficient"), cplexVars[colIndex])
                    }
                    val constraint = cplex.range(
                        lb.toSolverDouble("linear.constraints.bounds[$i].lower"),
                        lhs,
                        ub.toSolverDouble("linear.constraints.bounds[$i].upper"),
                        model.constraints.names[i]
                    )
                    cplex.add(constraint)
                    constraint
                }
            }
        }
        cleanupAfterSolverRun()
        cplexConstraints = constraints

        val objective = cplex.linearNumExpr()
        for (cell in model.objective.objective) {
            objective.addTerm(cell.coefficient.toSolverDouble("linear.objective.cells[${cell.colIndex}].coefficient"), cplexVars[cell.colIndex])
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

        logger.trace { "Cplex model dumped for $model" }
        return ok
    }

    /**
     * 配置 CPLEX 求解器参数 / Configure CPLEX solver parameters
     *
     * @param model 线性模型视图 / linear model view
     * @return 操作结果 / operation result
    */
    private suspend fun configure(model: LinearTriadModelView): Try {
        cplex.setParam(IloCplex.Param.TimeLimit, config.time.toDouble(DurationUnit.SECONDS))
        cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, config.gap.toSolverDouble("linear.config.gap"))
        cplex.setParam(IloCplex.Param.Threads, config.threadNum.toInt())

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
     * 执行 CPLEX 求解
     * Execute CPLEX solving
     *
     * @return 以Try包装的求解结果 / the solve result as Try
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
     * 分析求解结果 / Analyze solving result
     *
     * @param model 线性模型视图 / linear model view
     * @return 操作结果 / operation result
    */
    private suspend fun analyzeSolution(model: LinearTriadModelView): Try {
        return if (status.succeeded) {
            val obj = Flt64(cplex.objValue) + model.objective.constant
            val possibleBestObj = Flt64(cplex.bestObjValue) + model.objective.constant
            output = status.toSolveReport(
                objective = obj,
                values = cplexVars.map { Flt64(cplex.getValue(it)) },
                solveTime = cplex.cplexTime.seconds,
                bestBound = possibleBestObj,
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

