@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.backend.plugins.mosek

import fuookami.ospf.kotlin.core.backend.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.backend.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.backend.solver.output.SolvingStatus
import fuookami.ospf.kotlin.core.backend.solver.output.SolvingStatusCallBack

import fuookami.ospf.kotlin.core.backend.intermediate_model.LinearTriadModelView
import fuookami.ospf.kotlin.core.backend.intermediate_model.nonNullConstraintPriorityAmount
import fuookami.ospf.kotlin.core.backend.solver.LinearSolver
import fuookami.ospf.kotlin.core.backend.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.backend.solver.warnIgnoredConstraintPriority
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.core.frontend.model.mechanism.ObjectCategory
import fuookami.ospf.kotlin.core.frontend.model.mechanism.Sign
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
import mosek.Exception
import mosek.boundkey
import mosek.objsense
import mosek.variabletype
import kotlin.time.Duration

class MosekLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: MosekSolverCallBack? = null
) : LinearSolver {
    override val name = "mosek"

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput> {
        return MosekLinearSolverImpl(
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
                System.gc()
                result
            }
        }
    }
}

class MosekLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: MosekSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null
) : MosekSolver() {
    private lateinit var output: FeasibleSolverOutput

    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    suspend operator fun invoke(model: LinearTriadModelView): Ret<FeasibleSolverOutput> {
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
                    mosekModel.putvarbound(col, boundkey.fr, Flt64.negativeInfinity.toSolverDouble("linear.variables[$col].lowerBound"), Flt64.infinity.toSolverDouble("linear.variables[$col].upperBound"))
                } else if (variable.lowerBound.isNegativeInfinity()) {
                    mosekModel.putvarbound(col, boundkey.up, Flt64.negativeInfinity.toSolverDouble("linear.variables[$col].lowerBound"), variable.upperBound.toSolverDouble("linear.variables[$col].upperBound"))
                } else if (variable.upperBound.isInfinity()) {
                    mosekModel.putvarbound(col, boundkey.lo, variable.lowerBound.toSolverDouble("linear.variables[$col].lowerBound"), Flt64.infinity.toSolverDouble("linear.variables[$col].upperBound"))
                } else if (variable.lowerBound eq variable.upperBound) {
                    mosekModel.putvarbound(col, boundkey.fx, variable.lowerBound.toSolverDouble("linear.variables[$col].lowerBound"), variable.upperBound.toSolverDouble("linear.variables[$col].upperBound"))
                } else {
                    mosekModel.putvarbound(col, boundkey.ra, variable.lowerBound.toSolverDouble("linear.variables[$col].lowerBound"), variable.upperBound.toSolverDouble("linear.variables[$col].upperBound"))
                }
            }
            // todo: initial solution

            mosekModel.appendcons(model.constraints.size)
            coroutineScope {
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
                                val cols = ArrayList<Int>()
                                val coefficients = ArrayList<Double>()
                                for (cell in model.constraints.lhs[ii]) {
                                    cols.add(cell.colIndex)
                                    coefficients.add(cell.coefficient.toSolverDouble("linear.constraints.lhs[$ii][${cell.colIndex}].coefficient"))
                                }
                                Triple(ii, cols, coefficients)
                            }
                            if (memoryUseOver()) {
                                System.gc()
                            }
                            constraints
                        }
                    }
                    for (promise in promises) {
                        for ((i, cols, coefficients) in promise.await()) {
                            when (model.constraints.signs[i]) {
                                Sign.LessEqual -> {
                                    mosekModel.putconbound(
                                        i,
                                        boundkey.lo,
                                        model.constraints.rhs[i].toSolverDouble("linear.constraints.bounds[$i].lower"),
                                        Flt64.infinity.toSolverDouble("linear.constraints.bounds[$i].upper")
                                    )
                                }

                                Sign.GreaterEqual -> {
                                    mosekModel.putconbound(
                                        i,
                                        boundkey.up,
                                        Flt64.negativeInfinity.toSolverDouble("linear.constraints.bounds[$i].lower"),
                                        model.constraints.rhs[i].toSolverDouble("linear.constraints.bounds[$i].upper")
                                    )
                                }

                                Sign.Equal -> {
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
                        if (memoryUseOver()) {
                            System.gc()
                        }
                    }
                } else {
                    model.constraints.indices.map { i ->
                        val cols = ArrayList<Int>()
                        val coefficients = ArrayList<Double>()
                        for (cell in model.constraints.lhs[i]) {
                            cols.add(cell.colIndex)
                            coefficients.add(cell.coefficient.toSolverDouble("linear.constraints.lhs[$i][${cell.colIndex}].coefficient"))
                        }
                        when (model.constraints.signs[i]) {
                            Sign.LessEqual -> {
                                mosekModel.putconbound(
                                    i,
                                    boundkey.lo,
                                    model.constraints.rhs[i].toSolverDouble("linear.constraints.bounds[$i].lower"),
                                    Flt64.infinity.toSolverDouble("linear.constraints.bounds[$i].upper")
                                )
                            }

                            Sign.GreaterEqual -> {
                                mosekModel.putconbound(
                                    i,
                                    boundkey.up,
                                    Flt64.negativeInfinity.toSolverDouble("linear.constraints.bounds[$i].lower"),
                                    model.constraints.rhs[i].toSolverDouble("linear.constraints.bounds[$i].upper")
                                )
                            }

                            Sign.Equal -> {
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
            System.gc()

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
            Failed(Err(ErrorCode.OREngineModelingException, e.message))
        } catch (e: java.lang.Exception) {
            Failed(Err(ErrorCode.OREngineModelingException))
        }
    }

    private suspend fun configure(): Try {
        TODO("not implemented yet")
    }

    private suspend fun analyzeSolution(): Try {
        TODO("not implemented yet")
    }
}



