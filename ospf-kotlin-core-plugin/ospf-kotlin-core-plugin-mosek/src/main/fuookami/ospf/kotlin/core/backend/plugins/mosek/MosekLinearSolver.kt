package fuookami.ospf.kotlin.core.backend.plugins.mosek

import kotlin.math.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import mosek.*
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

class MosekLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: MosekSolverCallBack? = null
) : LinearSolver {
    override val name = "mosek"

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        statusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        val impl = MosekLinearSolverImpl(config, callBack, statusCallBack)
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
            val impl = MosekLinearSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { MosekSolverCallBack() }
                    .configuration { mosek ->
                        ok
                    }.analyzingSolution { mosek ->
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

class MosekLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: MosekSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null
) : MosekSolver() {
    private lateinit var output: SolverOutput

    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    suspend operator fun invoke(model: LinearTriadModelView): Ret<SolverOutput> {
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

                else -> {}
            }
        }
        return Ok(output)
    }

    private suspend fun dump(model: LinearTriadModelView): Try {
        return try {
            mosekModel.appendvars(model.variables.size)
            for ((col, variable) in model.variables.withIndex()) {
                mosekModel.putvarname(col, variable.name)
                if (variable.type.isContinuousType) {
                    mosekModel.putvartype(col, variabletype.type_cont)
                } else {
                    mosekModel.putvartype(col, variabletype.type_int)
                }
                if (variable.lowerBound.isNegativeInfinity() && variable.upperBound.isInfinity()) {
                    mosekModel.putvarbound(col, boundkey.fr, Flt64.negativeInfinity.toDouble(), Flt64.infinity.toDouble())
                } else if (variable.lowerBound.isNegativeInfinity()) {
                    mosekModel.putvarbound(col, boundkey.up, Flt64.negativeInfinity.toDouble(), variable.upperBound.toDouble())
                } else if (variable.upperBound.isInfinity()) {
                    mosekModel.putvarbound(col, boundkey.lo, variable.lowerBound.toDouble(), Flt64.infinity.toDouble())
                } else if (variable.lowerBound eq variable.upperBound) {
                    mosekModel.putvarbound(col, boundkey.fx, variable.lowerBound.toDouble(), variable.upperBound.toDouble())
                } else {
                    mosekModel.putvarbound(col, boundkey.ra, variable.lowerBound.toDouble(), variable.upperBound.toDouble())
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
                            ((i * segment) until minOf(model.constraints.size, (i + 1) * segment)).map { ii ->
                                val cols = ArrayList<Int>()
                                val coefficients = ArrayList<Double>()
                                for (cell in model.constraints.lhs[ii]) {
                                    cols.add(cell.colIndex)
                                    coefficients.add(cell.coefficient.toDouble())
                                }
                                Triple(ii, cols, coefficients)
                            }
                        }
                    }
                    for (promise in promises) {
                        for ((i, cols, coefficients) in promise.await()) {
                            when (model.constraints.signs[i]) {
                                Sign.LessEqual -> {
                                    mosekModel.putconbound(
                                        i,
                                        boundkey.lo,
                                        model.constraints.rhs[i].toDouble(),
                                        Flt64.infinity.toDouble()
                                    )
                                }

                                Sign.GreaterEqual -> {
                                    mosekModel.putconbound(
                                        i,
                                        boundkey.up,
                                        Flt64.negativeInfinity.toDouble(),
                                        model.constraints.rhs[i].toDouble()
                                    )
                                }

                                Sign.Equal -> {
                                    mosekModel.putconbound(
                                        i,
                                        boundkey.fx,
                                        model.constraints.rhs[i].toDouble(),
                                        model.constraints.rhs[i].toDouble()
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
                } else {
                    model.constraints.indices.map { i ->
                        val cols = ArrayList<Int>()
                        val coefficients = ArrayList<Double>()
                        for (cell in model.constraints.lhs[i]) {
                            cols.add(cell.colIndex)
                            coefficients.add(cell.coefficient.toDouble())
                        }
                        when (model.constraints.signs[i]) {
                            Sign.LessEqual -> {
                                mosekModel.putconbound(
                                    i,
                                    boundkey.lo,
                                    model.constraints.rhs[i].toDouble(),
                                    Flt64.infinity.toDouble()
                                )
                            }

                            Sign.GreaterEqual -> {
                                mosekModel.putconbound(
                                    i,
                                    boundkey.up,
                                    Flt64.negativeInfinity.toDouble(),
                                    model.constraints.rhs[i].toDouble()
                                )
                            }

                            Sign.Equal -> {
                                mosekModel.putconbound(
                                    i,
                                    boundkey.fx,
                                    model.constraints.rhs[i].toDouble(),
                                    model.constraints.rhs[i].toDouble()
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

            for (cell in model.objective.obj) {
                mosekModel.putcj(cell.colIndex, cell.coefficient.toDouble())
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
