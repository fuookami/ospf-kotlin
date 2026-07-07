/**
 * Hexaly quadratic solver
 * Hexaly 二次求解器
 */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.hexaly

import kotlin.time.*
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
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import com.hexaly.optimizer.*

/**
 * Hexaly quadratic solver
 * Hexaly 二次求解器
 *
 * @property config solver configuration / 中文 求解器配置
 * @property callBack Hexaly solver callback manager / 中文 Hexaly 求解器回调管理器
 */
class HexalyQuadraticSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: HexalySolverCallBack? = null
) : QuadraticSolver {
    override val name = "hexaly"

    override suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
        return HexalyQuadraticSolverImpl(
            config = config,
            callBack = callBack,
            statusCallBack = solvingStatusCallBack
        ).let { impl ->
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
            HexalyQuadraticSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { HexalySolverCallBack() }
                    .configuration { _, hexaly, _, _ ->
                        ok
                    }
                    .analyzingSolution { _, hexaly, variables, _ ->
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
 * Hexaly quadratic solver implementation
 * Hexaly 二次求解器实现
 *
 * @property config solver configuration / 中文 求解器配置
 * @property callBack Hexaly solver callback manager / 中文 Hexaly 求解器回调管理器
 * @property statusCallBack solving status callback / 中文 求解状态回调
 */
private class HexalyQuadraticSolverImpl(
    private val config: SolverConfig,
    private val callBack: HexalySolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null
) : HexalySolver() {
    private lateinit var hexalyVars: List<HxExpression>
    private lateinit var hexalyConstraints: List<HxExpression>
    private lateinit var hexalyObjective: HxExpression
    private lateinit var output: FeasibleSolverOutput<Flt64>

    private var initialBestObj: Flt64? = null
    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    suspend operator fun invoke(model: QuadraticTetradModelView): Ret<FeasibleSolverOutput<Flt64>> {
        val processes = arrayOf(
            { it.init(model.name, callBack?.creatingEnvironmentFunction) },
            { it.dump(model) },
            { it.configure(model) },
            HexalyQuadraticSolverImpl::solve,
            HexalyQuadraticSolverImpl::analyzeStatus,
            HexalyQuadraticSolverImpl::analyzeSolution
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
     * Dump quadratic model into Hexaly model
     * 将二次模型转储到 Hexaly 模型
     *
     * @param model quadratic tetrad model view / 中文 二次四元组模型视图
     * @return operation result / 中文 操作结果
     */
    private suspend fun dump(model: QuadraticTetradModelView): Try {
        return try {
            warnIgnoredConstraintPriority("hexaly", model.nonNullConstraintPriorityAmount())

            hexalyVars = model.variables.map {
                HexalyVariable(hexalyModel, it.type, it.lowerBound, it.upperBound).toHexalyVariable()
            }

            for ((col, variable) in model.variables.withIndex()) {
                variable.initialResult?.let {
                    hexalyVars[col].setValue(it.toSolverDouble("quadratic.variables[$col].initialResult"))
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
                                val lhs = hexalyModel.sum()
                                model.constraints.sparseLhs.forEachEntry(ii) { colIndex1, colIndex2, coefficient ->
                                    if (colIndex2 != null) {
                                        lhs.addOperands(
                                            hexalyModel.prod(
                                                hexalyModel.prod(
                                                    coefficient.toSolverDouble("quadratic.constraints.lhs[$ii][$colIndex1,$colIndex2].coefficient"),
                                                    hexalyVars[colIndex1]
                                                ),
                                                hexalyVars[colIndex2]
                                            )
                                        )
                                    } else {
                                        lhs.addOperands(
                                            hexalyModel.prod(coefficient.toSolverDouble("quadratic.constraints.lhs[$ii][$colIndex1].coefficient"), hexalyVars[colIndex1])
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
                            val constraint = when (model.constraints.signs[it.first]) {
                                ConstraintRelation.LessEqual -> {
                                    hexalyModel.leq(it.second, model.constraints.rhs[it.first].toSolverDouble("quadratic.constraints.rhs[${it.first}]"))
                                }

                                ConstraintRelation.Equal -> {
                                    hexalyModel.eq(it.second, model.constraints.rhs[it.first].toSolverDouble("quadratic.constraints.rhs[${it.first}]"))
                                }

                                ConstraintRelation.GreaterEqual -> {
                                    hexalyModel.geq(it.second, model.constraints.rhs[it.first].toSolverDouble("quadratic.constraints.rhs[${it.first}]"))
                                }
                            }
                            hexalyModel.constraint(constraint)
                            constraint
                        }
                        cleanupOnSolverMemoryPressure()
                        result
                    }
                } else {
                    model.constraints.indices.map { i ->
                        val lhs = hexalyModel.sum()
                        model.constraints.sparseLhs.forEachEntry(i) { colIndex1, colIndex2, coefficient ->
                            if (colIndex2 != null) {
                                lhs.addOperands(
                                    hexalyModel.prod(
                                        hexalyModel.prod(coefficient.toSolverDouble("quadratic.constraints.lhs[$i][$colIndex1,$colIndex2].coefficient"), hexalyVars[colIndex1]),
                                        hexalyVars[colIndex2]
                                    )
                                )
                            } else {
                                lhs.addOperands(
                                    hexalyModel.prod(coefficient.toSolverDouble("quadratic.constraints.lhs[$i][$colIndex1].coefficient"), hexalyVars[colIndex1])
                                )
                            }
                        }
                        val constraint = when (model.constraints.signs[i]) {
                            ConstraintRelation.LessEqual -> {
                                hexalyModel.leq(lhs, model.constraints.rhs[i].toSolverDouble("quadratic.constraints.rhs[$i]"))
                            }

                            ConstraintRelation.Equal -> {
                                hexalyModel.eq(lhs, model.constraints.rhs[i].toSolverDouble("quadratic.constraints.rhs[$i]"))
                            }

                            ConstraintRelation.GreaterEqual -> {
                                hexalyModel.geq(lhs, model.constraints.rhs[i].toSolverDouble("quadratic.constraints.rhs[$i]"))
                            }
                        }
                        hexalyModel.constraint(constraint)
                        constraint
                    }
                }
            }
            cleanupAfterSolverRun()
            hexalyConstraints = constraints

            val obj = hexalyModel.sum()
            for (cell in model.objective.objective) {
                if (cell.colIndex2 != null) {
                    obj.addOperands(
                        hexalyModel.prod(
                            hexalyModel.prod(
                                cell.coefficient.toSolverDouble("quadratic.objective.cells[${cell.colIndex1},${cell.colIndex2}].coefficient"),
                                hexalyVars[cell.colIndex1]
                            ),
                            hexalyVars[cell.colIndex2!!]
                        )
                    )
                } else {
                    obj.addOperands(
                        hexalyModel.prod(cell.coefficient.toSolverDouble("quadratic.objective.cells[${cell.colIndex1}].coefficient"), hexalyVars[cell.colIndex1])
                    )
                }
            }
            obj.addOperand(model.objective.constant.toSolverDouble("quadratic.objective.constant"))
            when (model.objective.category) {
                ObjectCategory.Maximum -> {
                    hexalyModel.maximize(obj)
                }

                ObjectCategory.Minimum -> {
                    hexalyModel.minimize(obj)
                }
            }
            hexalyObjective = obj

            when (val result = callBack?.execIfContain(
                point = Point.AfterModeling,
                status = null,
                hexaly = optimizer,
                variables = hexalyVars,
                constraints = hexalyConstraints
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
        } catch (e: HxException) {
            solverModelingException(e.message)
        } catch (e: Exception) {
            solverModelingException()
        }
    }

    /**
     * Configure Hexaly optimizer parameters
     * 配置 Hexaly 优化器参数
     *
     * @param model quadratic tetrad model view / 中文 二次四元组模型视图
     * @return operation result / 中文 操作结果
     */
    @OptIn(ExperimentalTime::class)
    private suspend fun configure(model: QuadraticTetradModelView): Try {
        return try {
            optimizer.param.timeLimit = config.time.toInt(DurationUnit.SECONDS)
            optimizer.param.nbThreads = config.threadNum.toInt()
            optimizer.param.setDoubleObjectiveThreshold(0, config.gap.toSolverDouble("quadratic.config.gap"))

            if (config.notImprovementTime != null || callBack?.nativeCallback != null || statusCallBack != null) {
                optimizer.addCallback(HxCallbackType.IterationTicked) { optimizer, callBackType ->
                    callBack?.nativeCallback?.invoke(optimizer, callBackType)

                    if (callBackType == HxCallbackType.IterationTicked) {
                        val currentSolution = optimizer.solution
                        val currentObj = Flt64(currentSolution.getDoubleValue(hexalyObjective))
                        val currentBound = Flt64(currentSolution.getDoubleObjectiveBound(0))
                        val currentTime = Clock.System.now() - beginTime!!
                        val currentBestSolution = hexalyVars.map { Flt64(currentSolution.getDoubleValue(it)) }

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
                                optimizer.stop()
                            }
                        }

                        statusCallBack?.let {
                            when (it(
                                SolvingStatus(
                                    solver = "hexaly",
                                    solverConfig = config,
                                    intermediateModel = model,
                                    solverModel = hexalyModel,
                                    solverCallBack = this,
                                    objectCategory = when (hexalyModel.getObjectiveDirection(0)) {
                                        HxObjectiveDirection.Minimize -> {
                                            ObjectCategory.Minimum
                                        }

                                        HxObjectiveDirection.Maximize -> {
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
                            )) {
                                is Ok -> {}

                                is Failed -> {
                                    optimizer.stop()
                                }

                                is Fatal -> {
                                    optimizer.stop()
                                }
                            }
                        }
                    }
                }
            }

            when (val result = callBack?.execIfContain(
                point = Point.AfterModeling,
                status = null,
                hexaly = optimizer,
                variables = hexalyVars,
                constraints = hexalyConstraints
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
        } catch (e: HxException) {
            solverModelingException(e.message)
        } catch (e: Exception) {
            solverModelingException()
        }
    }

    /**
     * Analyze solution from Hexaly solver
     * 分析 Hexaly 求解器的解
     *
     * @return operation result / 中文 操作结果
     */
    private suspend fun analyzeSolution(): Try {
        return try {
            if (status.succeeded) {
                val results = ArrayList<Flt64>()
                for (hexalyVar in hexalyVars) {
                    results.add(Flt64(hexalyVar.doubleValue))
                }
                output = FeasibleSolverOutput<Flt64>(
                    obj = Flt64(hexalyObjective.doubleValue),
                    solution = results,
                    time = solvingTime!!,
                    possibleBestObj = Flt64(hexalySolution.getDoubleObjectiveBound(0)),
                    gap = Flt64(hexalySolution.getObjectiveGap(0))
                )

                when (val result = callBack?.execIfContain(
                    point = Point.AnalyzingSolution,
                    status = status,
                    hexaly = optimizer,
                    variables = hexalyVars,
                    constraints = hexalyConstraints
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
                    hexaly = optimizer,
                    variables = hexalyVars,
                    constraints = hexalyConstraints
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
        } catch (e: HxException) {
            solverModelingException(e.message)
        } catch (e: Exception) {
            solverModelingException()
        }
    }
}
