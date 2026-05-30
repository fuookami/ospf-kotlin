@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.copt

import copt.*
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.model.basic.nonNullConstraintPriorityAmount
import fuookami.ospf.kotlin.core.solver.QuadraticSolver
import fuookami.ospf.kotlin.core.solver.cleanupAfterSolverRun
import fuookami.ospf.kotlin.core.solver.cleanupOnSolverMemoryPressure
import fuookami.ospf.kotlin.core.solver.computeConstraintSegmentSize
import fuookami.ospf.kotlin.core.solver.solvingException
import fuookami.ospf.kotlin.core.solver.modelingException
import fuookami.ospf.kotlin.core.solver.failByStatus
import fuookami.ospf.kotlin.core.solver.config.CoptSolverConfig
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.warnIgnoredConstraintPriority
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.solver.output.SolvingStatus
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.utils.concept.copyIfNotNullOr
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput

/** COPT 二次求解器 / COPT quadratic solver */
class CoptQuadraticSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: CoptQuadraticSolverCallBack? = null
) : QuadraticSolver {
    override val name = "copt"

    /**
     * 求解二次模型 / Solve quadratic model
     *
     * @param model 二次模型视图 / quadratic model view
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return 求解结果 / solving result
     */
    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
        val impl = CoptQuadraticSolverImpl(
            config = config,
            callBack = callBack,
            statusCallBack = solvingStatusCallBack
        )
        val result = impl(model)
        cleanupAfterSolverRun()
        return result
    }

    /**
     * 求解二次模型，获取多个解 / Solve quadratic model, obtaining multiple solutions
     *
     * @param model 二次模型视图 / quadratic model view
     * @param solutionAmount 期望解的数量 / desired number of solutions
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return 求解结果及多个解 / solving result with multiple solutions
     */
    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
        return if (solutionAmount leq UInt64.one) {
            this(model).map { it to emptyList() }
        } else {
            val results = ArrayList<List<Flt64>>()
            CoptQuadraticSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { CoptQuadraticSolverCallBack() }
                    .configuration { _, copt, _, _ ->
                        if (solutionAmount gr UInt64.one) {
                            // todo: set copt parameter to limit number of solutions
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

/** COPT 二次求解器内部实现 / COPT quadratic solver internal implementation */
private class CoptQuadraticSolverImpl(
    private val config: SolverConfig,
    private val callBack: CoptQuadraticSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null
) : CoptSolver() {
    private lateinit var coptVars: List<Var>
    private lateinit var coptConstraints: List<QConstraint>
    private lateinit var output: FeasibleSolverOutput<Flt64>

    private var initialBestObj: Flt64? = null
    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    /**
     * 执行求解流程 / Execute solving process
     *
     * @param model 二次模型视图 / quadratic model view
     * @return 求解结果 / solving result
     */
    suspend operator fun invoke(model: QuadraticTetradModelView): Ret<FeasibleSolverOutput<Flt64>> {
        val coptConfig = config.extraConfig as? CoptSolverConfig
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
            CoptQuadraticSolverImpl::solve,
            CoptQuadraticSolverImpl::analyzeStatus,
            CoptQuadraticSolverImpl::analyzeSolution
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
     * 将二次模型转储到 COPT / Dump quadratic model to COPT
     *
     * @param model 二次模型视图 / quadratic model view
     * @return 操作结果 / operation result
     */
    private suspend fun dump(model: QuadraticTetradModelView): Try {
        return try {
            warnIgnoredConstraintPriority("copt", model.nonNullConstraintPriorityAmount())

            coptVars = model.variables.mapIndexed { index, variable ->
                coptModel.addVar(
                    variable.lowerBound.toSolverDouble("quadratic.variables[$index].lowerBound"),
                    variable.upperBound.toSolverDouble("quadratic.variables[$index].upperBound"),
                    0.0,
                    CoptVariable(variable.type).toCoptVar(),
                    variable.name
                )
            }

            for ((col, variable) in model.variables.withIndex()) {
                variable.initialResult?.let {
                    coptModel.setMipStart(coptVars[col], it.toSolverDouble("quadratic.variables[$col].initialResult"))
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
                                val lhs = QuadExpr()
                                model.constraints.sparseLhs.forEachEntry(ii) { colIndex1, colIndex2, coefficient ->
                                    if (colIndex2 != null) {
                                        lhs.addTerm(coptVars[colIndex1], coptVars[colIndex2], coefficient.toSolverDouble("quadratic.constraints.lhs[$ii][$colIndex1,$colIndex2].coefficient"))
                                    } else {
                                        lhs.addTerm(coptVars[colIndex1], coefficient.toSolverDouble("quadratic.constraints.lhs[$ii][$colIndex1].coefficient"))
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
                            coptModel.addQConstr(
                                it.second,
                                CoptConstraintSign(model.constraints.signs[it.first]).toCoptConstraintSign(),
                                model.constraints.rhs[it.first].toSolverDouble("quadratic.constraints.rhs[${it.first}]"),
                                model.constraints.names[it.first]
                            )
                        }
                        cleanupOnSolverMemoryPressure()
                        result
                    }
                } else {
                    model.constraints.indices.map { i ->
                        val lhs = QuadExpr()
                        model.constraints.sparseLhs.forEachEntry(i) { colIndex1, colIndex2, coefficient ->
                            if (colIndex2 != null) {
                                lhs.addTerm(coptVars[colIndex1], coptVars[colIndex2], coefficient.toSolverDouble("quadratic.constraints.lhs[$i][$colIndex1,$colIndex2].coefficient"))
                            } else {
                                lhs.addTerm(coptVars[colIndex1], coefficient.toSolverDouble("quadratic.constraints.lhs[$i][$colIndex1].coefficient"))
                            }
                        }
                        coptModel.addQConstr(
                            lhs,
                            CoptConstraintSign(model.constraints.signs[i]).toCoptConstraintSign(),
                            model.constraints.rhs[i].toSolverDouble("quadratic.constraints.rhs[$i]"),
                            model.constraints.names[i]
                        )
                    }
                }
            }
            cleanupAfterSolverRun()
            coptConstraints = constraints

            val obj = QuadExpr()
            for ((index, cell) in model.objective.objective.withIndex()) {
                if (cell.colIndex2 != null) {
                    obj.addTerm(coptVars[cell.colIndex1], coptVars[cell.colIndex2!!], cell.coefficient.toSolverDouble("quadratic.objective.cells[$index][${cell.colIndex1},${cell.colIndex2}].coefficient"))
                } else {
                    obj.addTerm(coptVars[cell.colIndex1], cell.coefficient.toSolverDouble("quadratic.objective.cells[$index][${cell.colIndex1}].coefficient"))
                }
            }
            obj.addConstant(model.objective.constant.toSolverDouble("quadratic.objective.constant"))
            coptModel.setQuadObjective(
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
            modelingException(e.message)
        } catch (e: Exception) {
            modelingException()
        }
    }

    /**
     * 配置 COPT 二次求解器参数 / Configure COPT quadratic solver parameters
     *
     * @param model 二次模型视图 / quadratic model view
     * @return 操作结果 / operation result
     */
    private suspend fun configure(model: QuadraticTetradModelView): Try {
        return try {
            coptModel.set(COPT.DoubleParam.TimeLimit, config.time.toDouble(DurationUnit.SECONDS))
                coptModel.set(COPT.DoubleParam.AbsGap, config.gap.toSolverDouble("quadratic.config.gap"))
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
                            } else if (currentTime - bestTime >= config.notImprovementTime!!) {
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
                        }

                        // todo: add lazy constraint
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
            solvingException(e.message)
        } catch (e: Exception) {
            solvingException()
        }
    }

    /** 分析求解结果 / Analyze solving result */
    private suspend fun analyzeSolution(): Try {
        return try {
            if (status.succeeded) {
                val results = ArrayList<Flt64>()
                for (coptVar in coptVars) {
                    results.add(Flt64(coptVar.get(COPT.DoubleInfo.Value)))
                }
                output = FeasibleSolverOutput<Flt64>(
                    obj = if (coptModel.get(COPT.IntAttr.IsMIP) != 0) {
                        Flt64(coptModel.get(COPT.DoubleAttr.BestObj))
                    } else {
                        Flt64(coptModel.get(COPT.DoubleAttr.LpObjVal))
                    },
                    solution = results,
                    time = coptModel.get(COPT.DoubleAttr.SolvingTime).seconds,
                    possibleBestObj = Flt64(
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
            solvingException(e.message)
        } catch (e: Exception) {
            solvingException()
        }
    }
}
