@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.copt

import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.*
import copt.*
import copt.Constraint
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.concept.copyIfNotNullOr
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.nonNullConstraintPriorityAmount
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.config.CoptSolverConfig
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/**
 * COPT 线性求解器 / COPT linear solver
 *
 * @property config 求解器配置 / solver configuration
 * @property callBack 线性求解器回调 / linear solver callback
 */
class CoptLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: CoptLinearSolverCallBack? = null
) : LinearSolver {
    override val name = "copt"

    internal var nativePiecewiseWriter: (Model, Map<VariableItemKey, Var>, List<NativePiecewiseData>) -> Try =
        ::addCoptNativePiecewise

    /**
     * 从机制模型执行单解求解，并在允许延迟展开时尝试 COPT 原生 PWL。 /
     * Solve one mechanism-model solution and try COPT native PWL when deferred expansion is allowed.
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
            return super<LinearSolver>.solve(
                model = model,
                converter = converter,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
        val converted = when (val result = convertMechanismModelToFlt64(model)) {
            is Ok -> result.value as? LinearMechanismModel<Flt64>
                ?: return super<LinearSolver>.solve(
                    model = model,
                    converter = converter,
                    solvingStatusCallBack = solvingStatusCallBack
                )
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        try {
            val candidates = selectCoptNativePiecewise(converted)
            if (candidates.isEmpty()) {
                return super<LinearSolver>.solve(
                    model = model,
                    converter = converter,
                    solvingStatusCallBack = solvingStatusCallBack
                )
            }
            val nativeData = ArrayList<NativePiecewiseData>(candidates.size)
            for (structure in candidates) {
                when (val prepared = prepareCoptNativePiecewise(structure)) {
                    is Ok -> nativeData += prepared.value
                    is Failed -> return super<LinearSolver>.solve(
                        model = model,
                        converter = converter,
                        solvingStatusCallBack = solvingStatusCallBack
                    )
                    is Fatal -> return super<LinearSolver>.solve(
                        model = model,
                        converter = converter,
                        solvingStatusCallBack = solvingStatusCallBack
                    )
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
                is Failed, is Fatal -> return super<LinearSolver>.solve(
                    model = model,
                    converter = converter,
                    solvingStatusCallBack = solvingStatusCallBack
                )
            }
            val attempt = nativeModel.use {
                CoptLinearSolverImpl(
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
                                backendName = "copt"
                            )
                            is Failed -> Failed(result.error)
                            is Fatal -> Fatal(result.errors)
                        }
                    }
                }
            }
            return when (attempt) {
                null -> super<LinearSolver>.solve(
                    model = model,
                    converter = converter,
                    solvingStatusCallBack = solvingStatusCallBack
                )
                is Ok -> Ok(attempt.value.convertTo(converter))
                is Failed -> Failed(attempt.error)
                is Fatal -> Fatal(attempt.errors)
            }
        } finally {
            converted.close()
        }
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
        when (val validation = model.identityValidation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return CoptLinearSolverImpl(
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
            CoptLinearSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { CoptLinearSolverCallBack() }
                    .configuration { _, copt, _, _ ->
                        if (solutionAmount gr UInt64.one) {
                            // 设置 COPT 参数以限制解数量 / Set the COPT parameter to limit the number of solutions
                        }
                        ok
                    }
                    .analyzingSolution { _, copt, variables, _ ->
                        for (i in 0 until min(solutionAmount.toInt(), copt.get(COPT.IntAttr.PoolSols))) {
                            val thisResults = copt.getPoolSolution(i, variables.toTypedArray()).map { Flt64(it) }
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

/**
 * COPT 线性求解器内部实现 / COPT linear solver internal implementation
 *
 * @property config 求解器配置 / solver configuration
 * @property callBack 线性求解器回调 / linear solver callback
 * @property statusCallBack 求解状态回调 / solving status callback
 */
private class CoptLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: CoptLinearSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null,
    private val nativePiecewiseData: List<NativePiecewiseData> = emptyList(),
    private val nativePiecewiseWriter: (Model, Map<VariableItemKey, Var>, List<NativePiecewiseData>) -> Try =
        ::addCoptNativePiecewise
) : CoptSolver() {
    var nativePiecewiseFailed: Boolean = false
        private set

    private lateinit var coptVars: List<Var>
    private lateinit var coptConstraints: List<Constraint>
    private lateinit var output: SolveReport<Flt64>

    private var initialBestObj: Flt64? = null
    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    /**
     * 执行求解流程 / Execute solving process
     *
     * @param model 线性模型视图 / linear model view
     * @return 求解结果 / solving result
     */
    suspend operator fun invoke(model: LinearTriadModelView): Ret<SolveReport<Flt64>> {
        val coptConfig = config.backendConfiguration as? CoptSolverConfig
        val server = coptConfig?.server
        val port = coptConfig?.port
        val password = coptConfig?.password
        val connectionTime = coptConfig?.connectionTime

        val processes = arrayOf(
            {
                if (server != null && port != null && password != null && connectionTime != null) {
                    it.init(
                        server = server,
                        port = port,
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
            CoptLinearSolverImpl::solve,
            CoptLinearSolverImpl::analyzeStatus,
            CoptLinearSolverImpl::analyzeSolution
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
     * 将模型转储到 COPT / Dump model to COPT
     *
     * @param model 线性模型视图 / linear model view
     * @return 操作结果 / operation result
     */
    private suspend fun dump(model: LinearTriadModelView): Try {
        return try {
            warnIgnoredConstraintPriority("copt", model.nonNullConstraintPriorityAmount())

            coptVars = model.variables.mapIndexed { index, variable ->
                coptModel.addVar(
                    variable.lowerBound.toSolverDouble("linear.variables[$index].lowerBound"),
                    variable.upperBound.toSolverDouble("linear.variables[$index].upperBound"),
                    0.0,
                    CoptVariable(variable.type).toCoptVar(),
                    variable.name
                )
            }

            if (nativePiecewiseData.isNotEmpty()) {
                val variablesByKey = model.variables.mapIndexedNotNull { index, variable ->
                    variable.origin?.key?.let { it to coptVars[index] }
                }.toMap()
                val nativeWrite = try {
                    NativeFunctionWriterRegistry(
                        listOf(
                            NativeFunctionWriter<Model, Var> { nativeModel, nativeVariables, batch ->
                                nativePiecewiseWriter(
                                    nativeModel,
                                    nativeVariables,
                                    batch.filterIsInstance<NativePiecewiseData>()
                                )
                            }
                        )
                    ).write(
                        model = coptModel,
                        variables = variablesByKey,
                        batches = listOf(nativePiecewiseData.map { it as Any })
                    )
                } catch (error: LinkageError) {
                    Failed(
                        Err(
                            ErrorCode.OREngineModelingException,
                            "COPT PWL SDK API 不可用：${error.message ?: error::class.simpleName} / " +
                                "COPT PWL SDK API is unavailable: ${error.message ?: error::class.simpleName}"
                        )
                    )
                } catch (error: Exception) {
                    Failed(
                        Err(
                            ErrorCode.OREngineModelingException,
                            "COPT PWL 写入失败：${error.message ?: error::class.simpleName} / " +
                                "COPT PWL write failed: ${error.message ?: error::class.simpleName}"
                        )
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

            for ((col, variable) in model.variables.withIndex()) {
                variable.initialResult?.let {
                    coptModel.setMipStart(coptVars[col], it.toSolverDouble("linear.variables[$col].initialResult"))
                }
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
                                val lhs = Expr()
                                model.constraints.sparseLhs.forEachEntry(ii) { colIndex, coefficient ->
                                    lhs.addTerm(coptVars[colIndex], coefficient.toSolverDouble("linear.constraints.lhs[$ii][$colIndex].coefficient"))
                                }
                                ii to lhs
                            }
                            cleanupOnSolverMemoryPressure()
                            constraints
                        }
                    }
                    promises.flatMap { promise ->
                        val result = promise.await().map {
                            coptModel.addConstr(
                                it.second,
                                CoptConstraintSign(model.constraints.signs[it.first]).toCoptConstraintSign(),
                                model.constraints.rhs[it.first].toSolverDouble("linear.constraints.rhs[${it.first}]"),
                                model.constraints.names[it.first]
                            )
                        }
                        cleanupOnSolverMemoryPressure()
                        result
                    }
                } else {
                    model.constraints.indices.map { i ->
                        val lhs = Expr()
                        model.constraints.sparseLhs.forEachEntry(i) { colIndex, coefficient ->
                            lhs.addTerm(coptVars[colIndex], coefficient.toSolverDouble("linear.constraints.lhs[$i][$colIndex].coefficient"))
                        }
                        coptModel.addConstr(
                            lhs,
                            CoptConstraintSign(model.constraints.signs[i]).toCoptConstraintSign(),
                            model.constraints.rhs[i].toSolverDouble("linear.constraints.rhs[$i]"),
                            model.constraints.names[i]
                        )
                    }
                }
            }
            cleanupAfterSolverRun()
            coptConstraints = constraints

            val obj = Expr()
            for ((index, cell) in model.objective.objective.withIndex()) {
                obj.addTerm(coptVars[cell.colIndex], cell.coefficient.toSolverDouble("linear.objective.cells[$index].coefficient"))
            }
            obj.addConstant(model.objective.constant.toSolverDouble("linear.objective.constant"))
            coptModel.setObjective(
                obj,
                when (model.objective.category) {
                    ObjectCategory.Minimum -> {
                        COPT.MINIMIZE
                    }

                    ObjectCategory.Maximum -> {
                        COPT.MAXIMIZE
                    }
                }
            )

            when (val result = callBack?.execIfContain(
                point = Point.AfterModeling,
                status = null,
                copt = coptModel,
                variables = coptVars,
                constraints = coptConstraints
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
        } catch (e: CoptException) {
            solverModelingException(e.message)
        } catch (e: Exception) {
            solverModelingException()
        }
    }

    /**
     * 配置 COPT 求解器参数 / Configure COPT solver parameters
     *
     * @param model 线性模型视图 / linear model view
     * @return 操作结果 / operation result
     */
    private suspend fun configure(model: LinearTriadModelView): Try {
        return try {
            coptModel.set(COPT.DoubleParam.TimeLimit, config.time.toDouble(DurationUnit.SECONDS))
            coptModel.set(COPT.DoubleParam.AbsGap, config.gap.toSolverDouble("linear.config.gap"))
            coptModel.set(COPT.IntParam.Threads, config.threadNum.toInt())

            if (config.notImprovementTime != null || callBack?.nativeCallback != null || statusCallBack != null) {
                coptModel.setCallback(object : CallbackBase() {
                    override fun callback() {
                        callBack?.nativeCallback?.invoke(this)

                        val currentObj = Flt64(get(COPT.CallBackInfo.BestObj))
                        val currentBound = Flt64(get(COPT.CallBackInfo.BestBound))
                        val currentTime = coptModel.get(COPT.DoubleAttr.SolvingTime).seconds
                        val currentBestSolution = this.solution.map { Flt64(it) }

                        if (initialBestObj == null) {
                            initialBestObj = currentObj
                        }

                        if (config.notImprovementTime != null) {
                            if (bestObj == null
                                || bestBound == null
                                || (currentObj - bestObj!!).abs() geq config.improveThreshold
                                || (currentBound - bestBound!!).abs() geq config.improveThreshold
                            ) {
                                bestObj = currentObj
                                bestBound = currentBound
                                bestTime = currentTime
                            } else if (currentTime - bestTime >= config.notImprovementTime!!
                                && config.interruptibleTime?.let { currentTime >= it } ?: true
                                && config.interruptibleGap?.let { (currentObj - currentBound).abs() ls it } ?: true
                            ) {
                                interrupt()
                            }
                        }

                        statusCallBack?.let {
                            when (it(
                                SolvingStatus(
                                    solver = "copt",
                                    time = currentTime,
                                    solverConfig = config,
                                    intermediateModel = model,
                                    solverModel = coptModel,
                                    solverCallBack = this,
                                    objectCategory = when (coptModel.get(COPT.IntAttr.ObjSense)) {
                                        COPT.MINIMIZE -> {
                                            ObjectCategory.Minimum
                                        }

                                        COPT.MAXIMIZE -> {
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
                                    currentBestSolution = currentBestSolution
                                )
                            )) {
                                is Ok -> {}

                                is Failed -> {
                                    interrupt()
                                }

                                is Fatal -> {
                                    interrupt()
                                }
                            }

                            // 添加惰性约束 / Add lazy constraints
                        }
                    }
                }, COPT.CALL_BACK_CONTEXT_MIP_NODE)
            }

            when (val result = callBack?.execIfContain(
                point = Point.Configuration,
                status = null,
                copt = coptModel,
                variables = coptVars,
                constraints = coptConstraints
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
        } catch (e: CoptException) {
            solverSolvingException(e.message)
        } catch (e: Exception) {
            solverSolvingException()
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
                for (coptVar in coptVars) {
                    results.add(Flt64(coptVar.get(COPT.DoubleInfo.Value)))
                }
                output = status.toSolveReport(
                    objective = if (coptModel.get(COPT.IntAttr.IsMIP) != 0) {
                        Flt64(coptModel.get(COPT.DoubleAttr.BestObj))
                    } else {
                        Flt64(coptModel.get(COPT.DoubleAttr.LpObjVal))
                    },
                    values = results,
                    solveTime = coptModel.get(COPT.DoubleAttr.SolvingTime).seconds,
                    bestBound = Flt64(
                        if (coptModel.get(COPT.IntAttr.IsMIP) != 0) {
                            coptModel.get(COPT.DoubleAttr.BestBound)
                        } else {
                            coptModel.get(COPT.DoubleAttr.BestObj)
                        }
                    ),
                    gap = Flt64(
                        if (coptModel.get(COPT.IntAttr.IsMIP) != 0) {
                            coptModel.get(COPT.DoubleAttr.BestGap)
                        } else {
                            0.0
                        }
                    )
                )
                when (val result = callBack?.execIfContain(
                    point = Point.AnalyzingSolution,
                    status = status,
                    copt = coptModel,
                    variables = coptVars,
                    constraints = coptConstraints
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
                    copt = coptModel,
                    variables = coptVars,
                    constraints = coptConstraints
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
        } catch (e: CoptException) {
            solverSolvingException(e.message)
        } catch (e: Exception) {
            solverSolvingException()
        }
    }
}
