package fuookami.ospf.kotlin.core.backend.plugins.mindopt

import kotlin.math.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import com.alibaba.damo.mindopt.*
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

class MindOPTLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: MindOPTLinearSolverCallBack? = null,
) : LinearSolver {
    override val name = "mindopt"

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        statusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        val impl = MindOPTLinearSolverImpl(config, callBack, statusCallBack)
        val result = impl(model)
        System.gc()
        return result
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        statusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        return if (solutionAmount leq UInt64.one) {
            this(model).map { it to emptyList() }
        } else {
            val results = ArrayList<Solution>()
            val impl = MindOPTLinearSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { MindOPTLinearSolverCallBack() }
                    .configuration { mindopt, _, _ ->
                        if (solutionAmount gr UInt64.one) {
                            mindopt.set(MDO.IntParam.MIP_SolutionPoolSize, solutionAmount.toInt())
                        }
                        ok
                    }
                    .analyzingSolution { mindopt, variables, _ ->
                        for (i in 0 until min(solutionAmount.toInt(), mindopt.get(MDO.IntAttr.SolCount))) {
                            mindopt.set(MDO.IntParam.MIP_SolutionNumber, i)
                            val thisResults = variables.map { Flt64(it.get(MDO.DoubleAttr.Xn)) }
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

private class MindOPTLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: MindOPTLinearSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null,
) : MindOPTSolver() {
    private var mip: Boolean = false

    private lateinit var mindoptVars: List<MDOVar>
    private lateinit var mindoptConstraints: List<MDOConstr>
    private lateinit var output: SolverOutput

    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    suspend operator fun invoke(model: LinearTriadModelView): Ret<SolverOutput> {
        mip = model.containsNotBinaryInteger

        val processes = arrayOf(
            { it.init(model.name, callBack?.creatingEnvironmentFunction) },
            { it.dump(model) },
            MindOPTLinearSolverImpl::configure,
            MindOPTLinearSolverImpl::solve,
            MindOPTLinearSolverImpl::analyzeStatus,
            MindOPTLinearSolverImpl::analyzeSolution
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
            mindoptVars = model.variables.map {
                mindoptModel.addVar(
                    it.lowerBound.toDouble(),
                    it.upperBound.toDouble(),
                    0.0,
                    MindOPTVariable(it.type).toMindOPTVar(),
                    it.name
                )
            }

            for ((col, variable) in model.variables.withIndex()) {
                variable.initialResult?.let {
                    mindoptVars[col].set(MDO.DoubleAttr.Start, it.toDouble())
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
                            ((i * segment) until minOf(model.constraints.size, (i + 1) * segment)).map { ii ->
                                val lhs = MDOLinExpr()
                                for (cell in model.constraints.lhs[ii]) {
                                    lhs.addTerm(cell.coefficient.toDouble(), mindoptVars[cell.colIndex])
                                }
                                ii to lhs
                            }
                        }
                    }
                    promises.flatMap { promise ->
                        val result = promise.await().map {
                            mindoptModel.addConstr(
                                it.second,
                                MindOPTConstraintSign(model.constraints.signs[it.first]).toMindOPTConstraintSign(),
                                model.constraints.rhs[it.first].toDouble(),
                                model.constraints.names[it.first]
                            )
                        }
                        result
                    }
                } else {
                    model.constraints.indices.map { i ->
                        val lhs = MDOLinExpr()
                        for (cell in model.constraints.lhs[i]) {
                            lhs.addTerm(cell.coefficient.toDouble(), mindoptVars[cell.colIndex])
                        }
                        mindoptModel.addConstr(
                            lhs,
                            MindOPTConstraintSign(model.constraints.signs[i]).toMindOPTConstraintSign(),
                            model.constraints.rhs[i].toDouble(),
                            model.constraints.names[i]
                        )
                    }
                }
            }
            System.gc()
            mindoptConstraints = constraints

            val obj = MDOLinExpr()
            for (cell in model.objective.obj) {
                obj.addTerm(cell.coefficient.toDouble(), mindoptVars[cell.colIndex])
            }
            mindoptModel.setObjective(
                obj,
                when (model.objective.category) {
                    ObjectCategory.Minimum -> {
                        MDO.MINIMIZE
                    }

                    ObjectCategory.Maximum -> {
                        MDO.MINIMIZE
                    }
                }
            )

            when (val result = callBack?.execIfContain(Point.AfterModeling, mindoptModel, mindoptVars, mindoptConstraints)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            ok
        } catch (e: MDOException) {
            Failed(Err(ErrorCode.OREngineModelingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineModelingException))
        }
    }

    private suspend fun configure(): Try {
        return try {
            mindoptModel.set(MDO.DoubleParam.MaxTime, config.time.toDouble(DurationUnit.SECONDS))
            mindoptModel.set(MDO.DoubleParam.MIP_GapAbs, config.gap.toDouble())
            mindoptModel.set(MDO.IntParam.NumThreads, config.threadNum.toInt())

            if (config.notImprovementTime != null || callBack?.nativeCallback != null || statusCallBack != null) {
                mindoptModel.setCallback(object : MDOCallback() {
                    override fun callback() {
                        callBack?.nativeCallback?.invoke(this)

                        if (where == MDO.CB_MIPNODE) {
                            val currentObj = Flt64(getDoubleInfo(MDO.CB_MIP_OBJBST))
                            val currentBound = Flt64(getDoubleInfo(MDO.CB_MIP_OBJBND))
                            val currentTime = mindoptModel.get(MDO.DoubleAttr.SolverTime).seconds

                            if (config.notImprovementTime != null) {
                                if (bestObj == null || bestBound == null || currentObj neq bestObj!! || currentBound neq bestBound!!) {
                                    bestObj = currentObj
                                    bestBound = currentBound
                                    bestTime = currentTime
                                } else if (currentTime - bestTime >= config.notImprovementTime!!) {
                                    abort()
                                }
                            }

                            statusCallBack?.let {
                                when (it(
                                    SolvingStatus(
                                        solver = "gurobi",
                                        obj = currentObj,
                                        possibleBestObj = currentBound,
                                        gap = (currentObj - currentBound + Flt64.decimalPrecision) / (currentObj + Flt64.decimalPrecision)
                                    )
                                )) {
                                    is Ok -> {}

                                    is Failed -> {
                                        abort()
                                    }
                                }
                            }
                        }
                    }
                })
            }

            when (val result = callBack?.execIfContain(Point.Configuration, mindoptModel, mindoptVars, mindoptConstraints)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            ok
        } catch (e: MDOException) {
            Failed(Err(ErrorCode.OREngineModelingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineModelingException))
        }
    }

    private suspend fun analyzeSolution(): Try {
        return try {
            if (status.succeeded) {
                val results = ArrayList<Flt64>()
                for (mindoptVar in mindoptVars) {
                    results.add(Flt64(mindoptVar.get(MDO.DoubleAttr.X)))
                }
                output = SolverOutput(
                    obj = Flt64(mindoptModel.get(MDO.DoubleAttr.ObjVal)),
                    solution = results,
                    time = mindoptModel.get(MDO.DoubleAttr.SolverTime).seconds,
                    possibleBestObj = Flt64(
                        if (mip) {
                            mindoptModel.get(MDO.DoubleAttr.DualObjVal)
                        } else {
                            mindoptModel.get(MDO.DoubleAttr.ObjVal)
                        }
                    ),
                    gap = Flt64(
                        if (mip) {
                            mindoptModel.get(MDO.DoubleAttr.MIP_GapAbs)
                        } else {
                            0.0
                        }
                    )
                )
                when (val result = callBack?.execIfContain(Point.AnalyzingSolution, mindoptModel, mindoptVars, mindoptConstraints)) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    else -> {}
                }
                ok
            } else {
                when (val result = callBack?.execIfContain(Point.AfterFailure, mindoptModel, mindoptVars, mindoptConstraints)) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    else -> {}
                }
                Failed(Err(status.errCode!!))
            }
        } catch (e: MDOException) {
            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineSolvingException))
        }
    }
}
