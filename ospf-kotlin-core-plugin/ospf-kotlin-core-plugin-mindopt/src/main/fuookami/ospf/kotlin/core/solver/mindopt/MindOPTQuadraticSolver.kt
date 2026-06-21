/** MindOPT 二次求解器 / MindOPT Quadratic Solver */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.mindopt

import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import com.alibaba.damo.mindopt.*
import fuookami.ospf.kotlin.utils.concept.copyIfNotNullOr
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.nonNullConstraintPriorityAmount
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble

/** MindOPT 二次求解器 / MindOPT quadratic solver */
class MindOPTQuadraticSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: MindOPTQuadraticSolverCallBack? = null,
) : QuadraticSolver {
    override val name = "mindopt"

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
        return MindOPTQuadraticSolverImpl(
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
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
        return if (solutionAmount leq UInt64.one) {
            this(model).map { it to emptyList() }
        } else {
            val results = ArrayList<List<Flt64>>()
            MindOPTQuadraticSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { MindOPTQuadraticSolverCallBack() }
                    .configuration { _, mindopt, _, _ ->
                        if (solutionAmount gr UInt64.one) {
                            mindopt.set(MDO.IntParam.MIP_SolutionPoolSize, solutionAmount.toInt())
                        }
                        ok
                    }
                    .analyzingSolution { _, mindopt, variables, _ ->
                        for (i in 0 until min(solutionAmount.toInt(), mindopt.get(MDO.IntAttr.SolCount))) {
                            mindopt.set(MDO.IntParam.MIP_SolutionNumber, i)
                            val thisResults = variables.map { Flt64(it.get(MDO.DoubleAttr.Xn)) }
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

private class MindOPTQuadraticSolverImpl(
    private val config: SolverConfig,
    private val callBack: MindOPTQuadraticSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null,
) : MindOPTSolver() {
    private var mip: Boolean = false

    private lateinit var mindoptVars: List<MDOVar>
    private lateinit var mindoptConstraints: List<MDOQConstr>
    private lateinit var output: FeasibleSolverOutput<Flt64>

    private var initialBestObj: Flt64? = null
    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    suspend operator fun invoke(model: QuadraticTetradModelView): Ret<FeasibleSolverOutput<Flt64>> {
        mip = model.containsNotBinaryInteger

        val processes = arrayOf(
            { it.init(model.name, callBack?.creatingEnvironmentFunction) },
            { it.dump(model) },
            { it.configure(model) },
            MindOPTQuadraticSolverImpl::solve,
            MindOPTQuadraticSolverImpl::analyzeStatus,
            MindOPTQuadraticSolverImpl::analyzeSolution
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

    private suspend fun dump(model: QuadraticTetradModelView): Try {
        return try {
            warnIgnoredConstraintPriority("mindopt", model.nonNullConstraintPriorityAmount())

            mindoptVars = model.variables.map {
                mindoptModel.addVar(
                    it.lowerBound.toSolverDouble("quadratic.variables[${it.name}].lowerBound"),
                    it.upperBound.toSolverDouble("quadratic.variables[${it.name}].upperBound"),
                    0.0,
                    MindOPTVariable(it.type).toMindOPTVar(),
                    it.name
                )
            }

            for ((col, variable) in model.variables.withIndex()) {
                variable.initialResult?.let {
                    mindoptVars[col].set(MDO.DoubleAttr.Start, it.toSolverDouble("quadratic.variables[$col].initialResult"))
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
                                val lhs = MDOQuadExpr()
                                model.constraints.sparseLhs.forEachEntry(ii) { colIndex1, colIndex2, coefficient ->
                                    if (colIndex2 != null) {
                                        lhs.addTerm(
                                            coefficient.toSolverDouble("quadratic.constraints.lhs[$ii][$colIndex1,$colIndex2].coefficient"),
                                            mindoptVars[colIndex1],
                                            mindoptVars[colIndex2]
                                        )
                                    } else {
                                        lhs.addTerm(coefficient.toSolverDouble("quadratic.constraints.lhs[$ii][$colIndex1].coefficient"), mindoptVars[colIndex1])
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
                            mindoptModel.addQConstr(
                                it.second,
                                MindOPTConstraintSign(model.constraints.signs[it.first]).toMindOPTConstraintSign(),
                                model.constraints.rhs[it.first].toSolverDouble("quadratic.constraints.rhs[${it.first}]"),
                                model.constraints.names[it.first]
                            )
                        }
                        cleanupOnSolverMemoryPressure()
                        result
                    }
                } else {
                    model.constraints.indices.map { i ->
                        val lhs = MDOQuadExpr()
                        model.constraints.sparseLhs.forEachEntry(i) { colIndex1, colIndex2, coefficient ->
                            if (colIndex2 != null) {
                                lhs.addTerm(
                                    coefficient.toSolverDouble("quadratic.constraints.lhs[$i][$colIndex1,$colIndex2].coefficient"),
                                    mindoptVars[colIndex1],
                                    mindoptVars[colIndex2]
                                )
                            } else {
                                lhs.addTerm(coefficient.toSolverDouble("quadratic.constraints.lhs[$i][$colIndex1].coefficient"), mindoptVars[colIndex1])
                            }
                        }
                        mindoptModel.addQConstr(
                            lhs,
                            MindOPTConstraintSign(model.constraints.signs[i]).toMindOPTConstraintSign(),
                            model.constraints.rhs[i].toSolverDouble("quadratic.constraints.rhs[$i]"),
                            model.constraints.names[i]
                        )
                    }
                }
            }
            cleanupAfterSolverRun()
            mindoptConstraints = constraints

            val obj = MDOQuadExpr()
            for (cell in model.objective.objective) {
                if (cell.colIndex2 != null) {
                    obj.addTerm(
                        cell.coefficient.toSolverDouble("quadratic.objective.cells[${cell.colIndex1},${cell.colIndex2}].coefficient"),
                        mindoptVars[cell.colIndex1],
                        mindoptVars[cell.colIndex2!!]
                    )
                } else {
                    obj.addTerm(cell.coefficient.toSolverDouble("quadratic.objective.cells[${cell.colIndex1}].coefficient"), mindoptVars[cell.colIndex1])
                }
            }
            obj.addConstant(model.objective.constant.toSolverDouble("quadratic.objective.constant"))
            mindoptModel.setObjective(
                obj,
                when (model.objective.category) {
                    ObjectCategory.Minimum -> {
                        MDO.MINIMIZE
                    }

                    ObjectCategory.Maximum -> {
                        MDO.MAXIMIZE
                    }
                }
            )

            when (val result = callBack?.execIfContain(
                point = Point.AfterModeling,
                status = null,
                mindopt = mindoptModel,
                variables = mindoptVars,
                constraints = mindoptConstraints
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
        } catch (e: MDOException) {
            solverModelingException(e.message)
        } catch (e: Exception) {
            solverModelingException()
        }
    }

    private suspend fun configure(model: QuadraticTetradModelView): Try {
        return try {
            mindoptModel.set(MDO.DoubleParam.MaxTime, config.time.toDouble(DurationUnit.SECONDS))
            mindoptModel.set(MDO.DoubleParam.MIP_GapAbs, config.gap.toSolverDouble("quadratic.config.gap"))
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
                                if (bestObj == null
                                    || bestBound == null
                                    || (currentObj - bestObj!!).abs() geq config.improveThreshold
                                    || (currentBound - bestBound!!).abs() geq config.improveThreshold
                                ) {
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
                                        solver = "mindopt",
                                        solverConfig = config,
                                        intermediateModel = model,
                                        solverModel = mindoptModel,
                                        solverCallBack = this,
                                        objectCategory = when (mindoptModel.get(MDO.IntAttr.ModelSense)) {
                                            MDO.MINIMIZE -> {
                                                ObjectCategory.Minimum
                                            }

                                            MDO.MAXIMIZE -> {
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
                                        gap = (currentObj - currentBound + Flt64.decimalPrecision) / (currentObj + Flt64.decimalPrecision)
                                    )
                                )) {
                                    is Ok -> {}

                                    is Failed -> {
                                        abort()
                                    }

                                    is Fatal -> {
                                        abort()
                                    }
                                }
                            }
                        }
                    }
                })
            }

            when (val result = callBack?.execIfContain(
                point = Point.AfterModeling,
                status = null,
                mindopt = mindoptModel,
                variables = mindoptVars,
                constraints = mindoptConstraints
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
        } catch (e: MDOException) {
            solverModelingException(e.message)
        } catch (e: Exception) {
            solverModelingException()
        }
    }

    private suspend fun analyzeSolution(): Try {
        return try {
            if (status.succeeded) {
                val results = ArrayList<Flt64>()
                for (mindoptVar in mindoptVars) {
                    results.add(Flt64(mindoptVar.get(MDO.DoubleAttr.X)))
                }
                output = FeasibleSolverOutput<Flt64>(
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
                when (val result = callBack?.execIfContain(
                    point = Point.AnalyzingSolution,
                    status = status,
                    mindopt = mindoptModel,
                    variables = mindoptVars,
                    constraints = mindoptConstraints
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
                    mindopt = mindoptModel,
                    variables = mindoptVars,
                    constraints = mindoptConstraints
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
        } catch (e: MDOException) {
            solverSolvingException(e.message)
        } catch (e: Exception) {
            solverSolvingException()
        }
    }
}
