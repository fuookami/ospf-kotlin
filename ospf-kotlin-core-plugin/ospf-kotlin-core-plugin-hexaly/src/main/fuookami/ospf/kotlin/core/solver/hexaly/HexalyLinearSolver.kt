@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.hexaly

import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.solver.output.SolvingStatus
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack

import com.hexaly.optimizer.HxCallbackType
import com.hexaly.optimizer.HxException
import com.hexaly.optimizer.HxExpression
import com.hexaly.optimizer.HxObjectiveDirection
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.basic.nonNullConstraintPriorityAmount
import fuookami.ospf.kotlin.core.solver.LinearSolver
import fuookami.ospf.kotlin.core.solver.resolveErrCode
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.warnIgnoredConstraintPriority
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.utils.concept.copyIfNotNullOr
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.memoryUseOver
import fuookami.ospf.kotlin.math.operator.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput

class HexalyLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: HexalySolverCallBack? = null
) : LinearSolver {
    override val name = "hexaly"

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
        return HexalyLinearSolverImpl(
            config = config,
            callBack = callBack,
            statusCallBack = solvingStatusCallBack
        ).use { impl ->
            val result = impl(model)
            System.gc()
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
            HexalyLinearSolverImpl(
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
                System.gc()
                result
            }
        }
    }
}

private class HexalyLinearSolverImpl(
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

    suspend operator fun invoke(model: LinearTriadModelView): Ret<FeasibleSolverOutput<Flt64>> {
        val processes = arrayOf(
            { it.init(model.name, callBack?.creatingEnvironmentFunction) },
            { it.dump(model) },
            { it.configure(model) },
            HexalyLinearSolverImpl::solve,
            HexalyLinearSolverImpl::analyzeStatus,
            HexalyLinearSolverImpl::analyzeSolution
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

    private suspend fun dump(model: LinearTriadModelView): Try {
        return try {
            warnIgnoredConstraintPriority("hexaly", model.nonNullConstraintPriorityAmount())

            hexalyVars = model.variables.map {
                HexalyVariable(hexalyModel, it.type, it.lowerBound, it.upperBound).toHexalyVariable()
            }

            for ((col, variable) in model.variables.withIndex()) {
                variable.initialResult?.let {
                    hexalyVars[col].setValue(it.toSolverDouble("linear.variables[$col].initialResult"))
                }
            }

            val constraints = coroutineScope {
                if (Runtime.getRuntime().availableProcessors() > 2 && model.constraints.size > Runtime.getRuntime().availableProcessors()) {
                    val factor = Flt64(model.constraints.size / (Runtime.getRuntime().availableProcessors() - 1)).lg()!!.floor().toUInt64().toInt()
                    val segment = if (factor >= 1) {
                        pow(UInt64.ten, factor).toInt()
                    } else {
                        10
                    }
                    val promises = (0..(model.constraints.size / segment)).map { i ->
                        val constraints = async(Dispatchers.Default) {
                            ((i * segment) until minOf(model.constraints.size, (i + 1) * segment)).map { ii ->
                                val lhs = hexalyModel.sum()
                                model.constraints.sparseLhs.forEachEntry(ii) { colIndex, coefficient ->
                                    lhs.addOperands(
                                        hexalyModel.prod(coefficient.toSolverDouble("linear.constraints.lhs[$ii][$colIndex].coefficient"), hexalyVars[colIndex])
                                    )
                                }
                                ii to lhs
                            }
                        }
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        constraints
                    }
                    promises.flatMap { promise ->
                        val result = promise.await().map {
                            val constraint = when (model.constraints.signs[it.first]) {
                                ConstraintRelation.LessEqual -> {
                                    hexalyModel.leq(it.second, model.constraints.rhs[it.first].toSolverDouble("linear.constraints.rhs[${it.first}]"))
                                }

                                ConstraintRelation.Equal -> {
                                    hexalyModel.eq(it.second, model.constraints.rhs[it.first].toSolverDouble("linear.constraints.rhs[${it.first}]"))
                                }

                                ConstraintRelation.GreaterEqual -> {
                                    hexalyModel.geq(it.second, model.constraints.rhs[it.first].toSolverDouble("linear.constraints.rhs[${it.first}]"))
                                }
                            }
                            hexalyModel.constraint(constraint)
                            constraint
                        }
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        result
                    }
                } else {
                    model.constraints.indices.map { i ->
                        val lhs = hexalyModel.sum()
                        model.constraints.sparseLhs.forEachEntry(i) { colIndex, coefficient ->
                            lhs.addOperands(
                                hexalyModel.prod(coefficient.toSolverDouble("linear.constraints.lhs[$i][$colIndex].coefficient"), hexalyVars[colIndex])
                            )
                        }
                        val constraint = when (model.constraints.signs[i]) {
                            ConstraintRelation.LessEqual -> {
                                hexalyModel.leq(lhs, model.constraints.rhs[i].toSolverDouble("linear.constraints.rhs[$i]"))
                            }

                            ConstraintRelation.Equal -> {
                                hexalyModel.eq(lhs, model.constraints.rhs[i].toSolverDouble("linear.constraints.rhs[$i]"))
                            }

                            ConstraintRelation.GreaterEqual -> {
                                hexalyModel.geq(lhs, model.constraints.rhs[i].toSolverDouble("linear.constraints.rhs[$i]"))
                            }
                        }
                        hexalyModel.constraint(constraint)
                        constraint
                    }
                }
            }
            System.gc()
            hexalyConstraints = constraints

            val obj = hexalyModel.sum()
            for (cell in model.objective.objective) {
                obj.addOperands(hexalyModel.prod(cell.coefficient.toSolverDouble("linear.objective.cells[${cell.colIndex}].coefficient"), hexalyVars[cell.colIndex]))
            }
            obj.addOperand(model.objective.constant.toSolverDouble("linear.objective.constant"))
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
            Failed(Err(ErrorCode.OREngineModelingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineModelingException))
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun configure(model: LinearTriadModelView): Try {
        return try {
            optimizer.param.timeLimit = config.time.toInt(DurationUnit.SECONDS)
            optimizer.param.nbThreads = config.threadNum.toInt()
            optimizer.param.setDoubleObjectiveThreshold(0, config.gap.toSolverDouble("linear.config.gap"))

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
                point = Point.Configuration,
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
            Failed(Err(ErrorCode.OREngineModelingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineModelingException))
        }
    }

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
                Failed(Err(status.resolveErrCode()))
            }
        } catch (e: HxException) {
            Failed(Err(ErrorCode.OREngineModelingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineModelingException))
        }
    }
}

