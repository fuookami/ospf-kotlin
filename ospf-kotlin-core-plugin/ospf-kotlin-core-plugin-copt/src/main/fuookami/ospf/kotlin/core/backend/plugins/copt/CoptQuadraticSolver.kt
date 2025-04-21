package fuookami.ospf.kotlin.core.backend.plugins.copt

import kotlin.math.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import copt.*
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

class CoptQuadraticSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: CoptQuadraticSolverCallBack? = null
) : QuadraticSolver {
    override val name = "copt"

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        statusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        val impl = CoptQuadraticSolverImpl(config, callBack, statusCallBack)
        val result = impl(model)
        System.gc()
        return result
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        statusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        return if (solutionAmount leq UInt64.one) {
            this(model).map { it to emptyList() }
        } else {
            val results = ArrayList<Solution>()
            val impl = CoptQuadraticSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { CoptQuadraticSolverCallBack() }
                    .configuration { copt, _, _ ->
                        if (solutionAmount gr UInt64.one) {
                            // todo: set copt parameter to limit number of solutions
                        }
                        ok
                    }
                    .analyzingSolution { copt, variables, _ ->
                        for (i in 0 until min(solutionAmount.toInt(), copt.get(COPT.IntAttr.PoolSols))) {
                            val thisResults = copt.getPoolSolution(i, variables.toTypedArray()).map { Flt64(it) }
                            if (!results.any { it.toTypedArray() contentEquals thisResults.toTypedArray() }) {
                                results.add(thisResults)
                            }
                        }
                        ok
                    },
                statusCallBack = statusCallBack
            )
            val result = impl(model).map { it to results }
            System.gc()
            return result
        }
    }
}

private class CoptQuadraticSolverImpl(
    private val config: SolverConfig,
    private val callBack: CoptQuadraticSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null
) : CoptSolver() {
    private lateinit var coptVars: List<Var>
    private lateinit var coptConstraints: List<QConstraint>
    private lateinit var output: SolverOutput

    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    suspend operator fun invoke(model: QuadraticTetradModelView): Ret<SolverOutput> {
        val coptConfig = config.extraConfig as? CoptSolverConfig
        val server = coptConfig?.server
        val port = coptConfig?.port
        val password = coptConfig?.password
        val connectionTime = coptConfig?.connectionTime

        val processes = arrayOf(
            {
                if (server != null && port != null && password != null && connectionTime != null) {
                    it.init(server, port, password, connectionTime, model.name, callBack?.creatingEnvironmentFunction)
                } else {
                    it.init(model.name, callBack?.creatingEnvironmentFunction)
                }
            },
            { it.dump(model) },
            CoptQuadraticSolverImpl::configure,
            CoptQuadraticSolverImpl::solve,
            CoptQuadraticSolverImpl::analyzeStatus,
            CoptQuadraticSolverImpl::analyzeSolution
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

    private suspend fun dump(model: QuadraticTetradModelView): Try {
        return try {
            coptVars = model.variables.map {
                coptModel.addVar(
                    it.lowerBound.toDouble(),
                    it.upperBound.toDouble(),
                    0.0,
                    CoptVariable(it.type).toCoptVar(),
                    it.name
                )
            }

            for ((col, variable) in model.variables.withIndex()) {
                variable.initialResult?.let {
                    coptModel.setMipStart(coptVars[col], it.toDouble())
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
                        async(Dispatchers.Default) {
                            val constraints = ((i * segment) until minOf(model.constraints.size, (i + 1) * segment)).map { ii ->
                                val lhs = QuadExpr()
                                for (cell in model.constraints.lhs[ii]) {
                                    if (cell.colIndex2 != null) {
                                        lhs.addTerm(coptVars[cell.colIndex1], coptVars[cell.colIndex2!!], cell.coefficient.toDouble())
                                    } else {
                                        lhs.addTerm(coptVars[cell.colIndex1], cell.coefficient.toDouble())
                                    }
                                }
                                ii to lhs
                            }
                            if (memoryUseOver()) {
                                System.gc()
                            }
                            constraints
                        }
                    }
                    promises.flatMap { promise ->
                        val result = promise.await().map {
                            coptModel.addQConstr(
                                it.second,
                                CoptConstraintSign(model.constraints.signs[it.first]).toCoptConstraintSign(),
                                model.constraints.rhs[it.first].toDouble(),
                                model.constraints.names[it.first]
                            )
                        }
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        result
                    }
                } else {
                    model.constraints.indices.map { i ->
                        val lhs = QuadExpr()
                        for (cell in model.constraints.lhs[i]) {
                            if (cell.colIndex2 != null) {
                                lhs.addTerm(coptVars[cell.colIndex1], coptVars[cell.colIndex2!!], cell.coefficient.toDouble())
                            } else {
                                lhs.addTerm(coptVars[cell.colIndex1], cell.coefficient.toDouble())
                            }
                        }
                        coptModel.addQConstr(
                            lhs,
                            CoptConstraintSign(model.constraints.signs[i]).toCoptConstraintSign(),
                            model.constraints.rhs[i].toDouble(),
                            model.constraints.names[i]
                        )
                    }
                }
            }
            System.gc()
            coptConstraints = constraints

            val obj = QuadExpr()
            for (cell in model.objective.obj) {
                if (cell.colIndex2 != null) {
                    obj.addTerm(coptVars[cell.colIndex1], coptVars[cell.colIndex2!!], cell.coefficient.toDouble())
                } else {
                    obj.addTerm(coptVars[cell.colIndex1], cell.coefficient.toDouble())
                }
            }
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

            when (val result = callBack?.execIfContain(Point.AfterModeling, coptModel, coptVars, coptConstraints)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            ok
        } catch (e: CoptException) {
            Failed(Err(ErrorCode.OREngineModelingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineModelingException))
        }
    }

    private suspend fun configure(): Try {
        return try {
            coptModel.set(COPT.DoubleParam.TimeLimit, config.time.toDouble(DurationUnit.SECONDS))
            coptModel.set(COPT.DoubleParam.AbsGap, config.gap.toDouble())
            coptModel.set(COPT.IntParam.Threads, config.threadNum.toInt())

            if (config.notImprovementTime != null || callBack?.nativeCallback != null || statusCallBack != null) {
                coptModel.setCallback(object : CallbackBase() {
                    override fun callback() {
                        callBack?.nativeCallback?.invoke(this)

                        val currentObj = Flt64(get(COPT.CallBackInfo.BestObj))
                        val currentBound = Flt64(get(COPT.CallBackInfo.BestBound))
                        val currentTime = coptModel.get(COPT.DoubleAttr.SolvingTime).seconds

                        if (config.notImprovementTime != null) {
                            if (bestObj == null || bestBound == null || currentObj neq bestObj!! || currentBound neq bestBound!!) {
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
                                    obj = currentObj,
                                    possibleBestObj = currentBound,
                                    gap = (currentObj - currentBound + Flt64.decimalPrecision) / (currentObj + Flt64.decimalPrecision)
                                )
                            )) {
                                is Ok -> {}

                                is Failed -> {
                                    interrupt()
                                }
                            }
                        }
                    }
                }, COPT.CALL_BACK_CONTEXT_MIP_NODE)
            }

            when (val result = callBack?.execIfContain(Point.Configuration, coptModel, coptVars, coptConstraints)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            ok
        } catch (e: CoptException) {
            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineSolvingException))
        }
    }

    private suspend fun analyzeSolution(): Try {
        return try {
            if (status.succeeded) {
                val results = ArrayList<Flt64>()
                for (coptVar in coptVars) {
                    results.add(Flt64(coptVar.get(COPT.DoubleInfo.Value)))
                }
                output = SolverOutput(
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
                when (val result = callBack?.execIfContain(Point.AnalyzingSolution, coptModel, coptVars, coptConstraints)) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    else -> {}
                }
                ok
            } else {
                when (val result = callBack?.execIfContain(Point.AfterFailure, coptModel, coptVars, coptConstraints)) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    else -> {}
                }
                Failed(Err(status.errCode!!))
            }
        } catch (e: CoptException) {
            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineSolvingException))
        }
    }
}
