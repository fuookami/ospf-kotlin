package fuookami.ospf.kotlin.core.backend.plugins.hexaly

import kotlin.time.*
import kotlinx.datetime.*
import kotlinx.coroutines.*
import com.hexaly.optimizer.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

class HexalyLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: HexalySolverCallBack? = null
) : LinearSolver {
    override val name = "hexaly"

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput> {
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
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        return if (solutionAmount leq UInt64.one) {
            this(model).map { it to emptyList() }
        } else {
            val results = ArrayList<Solution>()
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
    private lateinit var output: FeasibleSolverOutput

    private var initialBestObj: Flt64? = null
    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    suspend operator fun invoke(model: LinearTriadModelView): Ret<FeasibleSolverOutput> {
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

                else -> {}
            }
        }
        return Ok(output)
    }

    private suspend fun dump(model: LinearTriadModelView): Try {
        return try {
            hexalyVars = model.variables.map {
                HexalyVariable(hexalyModel, it.type, it.lowerBound, it.upperBound).toHexalyVariable()
            }

            for ((col, variable) in model.variables.withIndex()) {
                variable.initialResult?.let {
                    hexalyVars[col].setValue(it.toDouble())
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
                                for (cell in model.constraints.lhs[ii]) {
                                    lhs.addOperands(
                                        hexalyModel.prod(cell.coefficient.toDouble(), hexalyVars[cell.colIndex])
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
                                Sign.LessEqual -> {
                                    hexalyModel.leq(it.second, model.constraints.rhs[it.first].toDouble())
                                }

                                Sign.Equal -> {
                                    hexalyModel.eq(it.second, model.constraints.rhs[it.first].toDouble())
                                }

                                Sign.GreaterEqual -> {
                                    hexalyModel.geq(it.second, model.constraints.rhs[it.first].toDouble())
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
                        for (cell in model.constraints.lhs[i]) {
                            lhs.addOperands(
                                hexalyModel.prod(cell.coefficient.toDouble(), hexalyVars[cell.colIndex])
                            )
                        }
                        val constraint = when (model.constraints.signs[i]) {
                            Sign.LessEqual -> {
                                hexalyModel.leq(lhs, model.constraints.rhs[i].toDouble())
                            }

                            Sign.Equal -> {
                                hexalyModel.eq(lhs, model.constraints.rhs[i].toDouble())
                            }

                            Sign.GreaterEqual -> {
                                hexalyModel.geq(lhs, model.constraints.rhs[i].toDouble())
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
            for (cell in model.objective.obj) {
                obj.addOperands(hexalyModel.prod(cell.coefficient.toDouble(), hexalyVars[cell.colIndex]))
            }
            obj.addOperand(model.objective.constant.toDouble())
            when (model.objective.category) {
                ObjectCategory.Maximum -> {
                    hexalyModel.maximize(obj)
                }

                ObjectCategory.Minimum -> {
                    hexalyModel.minimize(obj)
                }
            }
            hexalyObjective = obj

            when (val result = callBack?.execIfContain(Point.AfterModeling, null, optimizer, hexalyVars, hexalyConstraints)) {
                is Failed -> {
                    return Failed(result.error)
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

    private suspend fun configure(model: LinearTriadModelView): Try {
        return try {
            optimizer.param.timeLimit = config.time.toInt(DurationUnit.SECONDS)
            optimizer.param.nbThreads = config.threadNum.toInt()
            optimizer.param.setDoubleObjectiveThreshold(0, config.gap.toDouble())

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
                                    initialBestObj = initialBestObj ?: currentObj,
                                    gap = (currentObj - currentBound + Flt64.decimalPrecision) / (currentObj + Flt64.decimalPrecision),
                                    currentBestSolution = currentBestSolution
                                )
                            )) {
                                is Ok -> {}

                                is Failed -> {
                                    optimizer.stop()
                                }
                            }
                        }
                    }
                }
            }

            when (val result = callBack?.execIfContain(Point.Configuration, null, optimizer, hexalyVars, hexalyConstraints)) {
                is Failed -> {
                    return Failed(result.error)
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
                output = FeasibleSolverOutput(
                    obj = Flt64(hexalyObjective.doubleValue),
                    solution = results,
                    time = solvingTime!!,
                    possibleBestObj = Flt64(hexalySolution.getDoubleObjectiveBound(0)),
                    gap = Flt64(hexalySolution.getObjectiveGap(0))
                )

                when (val result = callBack?.execIfContain(Point.AnalyzingSolution, status, optimizer, hexalyVars, hexalyConstraints)) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    else -> {}
                }
                ok
            } else {
                when (val result = callBack?.execIfContain(Point.AfterFailure, status, optimizer, hexalyVars, hexalyConstraints)) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    else -> {}
                }
                Failed(Err(status.errCode!!))
            }
        } catch (e: HxException) {
            Failed(Err(ErrorCode.OREngineModelingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineModelingException))
        }
    }
}
