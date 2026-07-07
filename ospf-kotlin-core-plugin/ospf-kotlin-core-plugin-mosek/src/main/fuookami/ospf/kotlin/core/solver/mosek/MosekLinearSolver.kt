/** MOSEK 线性求解器 / MOSEK Linear Solver */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.mosek

import kotlin.time.Duration
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
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
import mosek.*

/**
 * MOSEK linear solver
 *
 * MOSEK 线性求解器
 *
 * @property callBack Solver callback / 求解器回调
 */
class MosekLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: MosekSolverCallBack? = null
) : LinearSolver {
    override val name = "mosek"

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
        return MosekLinearSolverImpl(
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
            MosekLinearSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { MosekSolverCallBack() }
                    .configuration { _, mosek ->
                        ok
                    }
                    .analyzingSolution { _, mosek ->
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
 * MOSEK linear solver internal implementation
 *
 * MOSEK 线性求解器内部实现
 *
 * @property config Solver configuration / 求解器配置
 * @property callBack Solver callback / 求解器回调
 * @property statusCallBack Solving status callback / 求解状态回调
 */
class MosekLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: MosekSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null
) : MosekSolver() {
    private lateinit var output: FeasibleSolverOutput<Flt64>

    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    suspend operator fun invoke(model: LinearTriadModelView): Ret<FeasibleSolverOutput<Flt64>> {
        val processes = arrayOf(
            { it.init(model.name, callBack?.creatingEnvironmentFunction) },
            { it.dump(model) },
            MosekLinearSolverImpl::configure,
            MosekLinearSolverImpl::solve,
            MosekLinearSolverImpl::analyzeStatus,
            MosekLinearSolverImpl::analyzeSolution
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
            warnIgnoredConstraintPriority("mosek", model.nonNullConstraintPriorityAmount())

            mosekModel.appendvars(model.variables.size)
            for ((col, variable) in model.variables.withIndex()) {
                mosekModel.putvarname(col, variable.name)
                if (variable.type.isContinuousType) {
                    mosekModel.putvartype(col, variabletype.type_cont)
                } else {
                    mosekModel.putvartype(col, variabletype.type_int)
                }
                if (variable.lowerBound.isNegativeInfinity() && variable.upperBound.isInfinity()) {
                    mosekModel.putvarbound(
                        col,
                        boundkey.fr,
                        Flt64.negativeInfinity.toSolverDouble("linear.variables[$col].lowerBound"),
                        Flt64.infinity.toSolverDouble("linear.variables[$col].upperBound")
                    )
                } else if (variable.lowerBound.isNegativeInfinity()) {
                    mosekModel.putvarbound(
                        col,
                        boundkey.up,
                        Flt64.negativeInfinity.toSolverDouble("linear.variables[$col].lowerBound"),
                        variable.upperBound.toSolverDouble("linear.variables[$col].upperBound")
                    )
                } else if (variable.upperBound.isInfinity()) {
                    mosekModel.putvarbound(
                        col,
                        boundkey.lo,
                        variable.lowerBound.toSolverDouble("linear.variables[$col].lowerBound"),
                        Flt64.infinity.toSolverDouble("linear.variables[$col].upperBound")
                    )
                } else if (variable.lowerBound eq variable.upperBound) {
                    mosekModel.putvarbound(
                        col,
                        boundkey.fx,
                        variable.lowerBound.toSolverDouble("linear.variables[$col].lowerBound"),
                        variable.upperBound.toSolverDouble("linear.variables[$col].upperBound")
                    )
                } else {
                    mosekModel.putvarbound(
                        col,
                        boundkey.ra,
                        variable.lowerBound.toSolverDouble("linear.variables[$col].lowerBound"),
                        variable.upperBound.toSolverDouble("linear.variables[$col].upperBound")
                    )
                }
            }
            // todo: initial solution

            mosekModel.appendcons(model.constraints.size)
            coroutineScope {
                if (Runtime.getRuntime().availableProcessors() > 2 && model.constraints.size > Runtime.getRuntime().availableProcessors()) {
                    val segment = computeConstraintSegmentSize(model.constraints.size)
                    val chunkAmount = (model.constraints.size + segment - 1) / segment
                    val promises = (0 until chunkAmount).map { i ->
                        async(Dispatchers.Default) {
                            val from = i * segment
                            val to = minOf(model.constraints.size, from + segment)
                            val constraints = (from until to).map { ii ->
                                val cols = ArrayList<Int>()
                                val coefficients = ArrayList<Double>()
                                model.constraints.sparseLhs.forEachEntry(ii) { colIndex, coefficient ->
                                    cols.add(colIndex)
                                    coefficients.add(coefficient.toSolverDouble("linear.constraints.lhs[$ii][$colIndex].coefficient"))
                                }
                                Triple(ii, cols, coefficients)
                            }
                            cleanupOnSolverMemoryPressure()
                            constraints
                        }
                    }
                    for (promise in promises) {
                        for ((i, cols, coefficients) in promise.await()) {
                            when (model.constraints.signs[i]) {
                                ConstraintRelation.LessEqual -> {
                                    mosekModel.putconbound(
                                        i,
                                        boundkey.lo,
                                        model.constraints.rhs[i].toSolverDouble("linear.constraints.bounds[$i].lower"),
                                        Flt64.infinity.toSolverDouble("linear.constraints.bounds[$i].upper")
                                    )
                                }

                                ConstraintRelation.GreaterEqual -> {
                                    mosekModel.putconbound(
                                        i,
                                        boundkey.up,
                                        Flt64.negativeInfinity.toSolverDouble("linear.constraints.bounds[$i].lower"),
                                        model.constraints.rhs[i].toSolverDouble("linear.constraints.bounds[$i].upper")
                                    )
                                }

                                ConstraintRelation.Equal -> {
                                    mosekModel.putconbound(
                                        i,
                                        boundkey.fx,
                                        model.constraints.rhs[i].toSolverDouble("linear.constraints.bounds[$i].lower"),
                                        model.constraints.rhs[i].toSolverDouble("linear.constraints.bounds[$i].upper")
                                    )
                                }
                            }
                            mosekModel.putarow(
                                i,
                                cols.toIntArray(),
                                coefficients.toDoubleArray()
                            )
                        }
                        cleanupOnSolverMemoryPressure()
                    }
                } else {
                    model.constraints.indices.map { i ->
                        val cols = ArrayList<Int>()
                        val coefficients = ArrayList<Double>()
                        model.constraints.sparseLhs.forEachEntry(i) { colIndex, coefficient ->
                            cols.add(colIndex)
                            coefficients.add(coefficient.toSolverDouble("linear.constraints.lhs[$i][$colIndex].coefficient"))
                        }
                        when (model.constraints.signs[i]) {
                            ConstraintRelation.LessEqual -> {
                                mosekModel.putconbound(
                                    i,
                                    boundkey.lo,
                                    model.constraints.rhs[i].toSolverDouble("linear.constraints.bounds[$i].lower"),
                                    Flt64.infinity.toSolverDouble("linear.constraints.bounds[$i].upper")
                                )
                            }

                            ConstraintRelation.GreaterEqual -> {
                                mosekModel.putconbound(
                                    i,
                                    boundkey.up,
                                    Flt64.negativeInfinity.toSolverDouble("linear.constraints.bounds[$i].lower"),
                                    model.constraints.rhs[i].toSolverDouble("linear.constraints.bounds[$i].upper")
                                )
                            }

                            ConstraintRelation.Equal -> {
                                mosekModel.putconbound(
                                    i,
                                    boundkey.fx,
                                    model.constraints.rhs[i].toSolverDouble("linear.constraints.bounds[$i].lower"),
                                    model.constraints.rhs[i].toSolverDouble("linear.constraints.bounds[$i].upper")
                                )
                            }
                        }
                        mosekModel.putarow(
                            i,
                            cols.toIntArray(),
                            coefficients.toDoubleArray()
                        )
                    }
                }
            }
            cleanupAfterSolverRun()

            for (cell in model.objective.objective) {
                mosekModel.putcj(cell.colIndex, cell.coefficient.toSolverDouble("linear.objective.cells[${cell.colIndex}].coefficient"))
            }
            when (model.objective.category) {
                ObjectCategory.Minimum -> {
                    mosekModel.putobjsense(objsense.minimize)
                }

                ObjectCategory.Maximum -> {
                    mosekModel.putobjsense(objsense.maximize)
                }
            }

            ok
        } catch (e: Exception) {
            solverModelingException(e.message)
        } catch (e: java.lang.Exception) {
            solverModelingException()
        }
    }

    private suspend fun configure(): Try {
        return Failed(
            Err(
                ErrorCode.OREngineModelingException,
                "MOSEK linear solver configuration is not implemented yet. / MOSEK 线性求解器配置尚未实现。"
            )
        )
    }

    private suspend fun analyzeSolution(): Try {
        return Failed(
            Err(
                ErrorCode.ORSolutionInvalid,
                "MOSEK linear solution extraction is not implemented yet. / MOSEK 线性解提取尚未实现。"
            )
        )
    }
}


