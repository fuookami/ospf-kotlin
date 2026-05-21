@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.copt

import copt.*
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.basic.nonNullConstraintPriorityAmount
import fuookami.ospf.kotlin.core.solver.LinearSolver
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

class CoptLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: CoptLinearSolverCallBack? = null
) : LinearSolver {
    override val name = "copt"

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
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

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
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

private class CoptLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: CoptLinearSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null
) : CoptSolver() {
    private lateinit var coptVars: List<Var>
    private lateinit var coptConstraints: List<Constraint>
    private lateinit var output: FeasibleSolverOutput<Flt64>

    private var initialBestObj: Flt64? = null
    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    suspend operator fun invoke(model: LinearTriadModelView): Ret<FeasibleSolverOutput<Flt64>> {
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
            modelingException(e.message)
        } catch (e: Exception) {
            modelingException()
        }
    }

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

                            // todo: add lazy constraint
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
            solvingException(e.message)
        } catch (e: Exception) {
            solvingException()
        }
    }

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
