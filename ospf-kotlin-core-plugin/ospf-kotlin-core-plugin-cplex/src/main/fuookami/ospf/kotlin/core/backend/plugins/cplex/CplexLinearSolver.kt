package fuookami.ospf.kotlin.core.backend.plugins.cplex

import kotlin.math.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.*
import ilog.concert.*
import ilog.cplex.*
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

class CplexLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: CplexSolverCallBack? = null
) : LinearSolver {
    override val name = "cplex"

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        val impl = CplexLinearSolverImpl(config, callBack, solvingStatusCallBack)
        val result = impl(model)
        System.gc()
        return result
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        return if (solutionAmount leq UInt64.one) {
            this(model).map { it to emptyList() }
        } else {
            val results = ArrayList<Solution>()
            val impl = CplexLinearSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { CplexSolverCallBack() }
                    .configuration { _, cplex, _, _ ->
                        if (solutionAmount gr UInt64.one) {
                            cplex.setParam(IloCplex.Param.MIP.Pool.Intensity, 4)
                            cplex.setParam(IloCplex.Param.MIP.Pool.AbsGap, 0.0)
                            cplex.setParam(IloCplex.Param.MIP.Pool.Capacity, solutionAmount.toInt())
                            cplex.setParam(IloCplex.Param.MIP.Pool.Replace, 2)
                            cplex.setParam(IloCplex.Param.MIP.Limits.Populate, solutionAmount.cub().toInt())
                        }
                        ok
                    }
                    .solving { _, cplex, _, _ ->
                        try {
                            cplex.populate()
                            ok
                        } catch (e: IloException) {
                            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
                        }
                    }
                    .analyzingSolution { _, cplex, variables, _ ->
                        for (i in 0 until min(solutionAmount.toInt(), cplex.solnPoolNsolns)) {
                            val thisResults = variables.map { Flt64(cplex.getValue(it, i)) }
                            if (!results.any { it.toTypedArray() contentEquals thisResults.toTypedArray() }) {
                                results.add(thisResults)
                            }
                        }
                        ok
                    },
                statusCallBack = solvingStatusCallBack
            )
            val result = impl(model).map { Pair(it, results) }
            System.gc()
            return result
        }
    }
}

private class CplexLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: CplexSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack?
) : CplexSolver() {
    private lateinit var cplexVars: List<IloNumVar>
    private lateinit var cplexConstraints: List<IloRange>
    private lateinit var output: SolverOutput

    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime: Duration = Duration.ZERO

    private val logger = logger()

    suspend operator fun invoke(model: LinearTriadModelView): Ret<SolverOutput> {
        val processes = arrayOf(
            { it.init(model.name) },
            { it.dump(model) },
            CplexLinearSolverImpl::configure,
            CplexLinearSolverImpl::solve,
            CplexLinearSolverImpl::analyzeStatus,
            { it.analyzeSolution(model) }
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
        logger.trace { "Dumping to cplex model for $model" }

        cplexVars = model.variables.map {
            cplex.numVar(it.lowerBound.toDouble(), it.upperBound.toDouble(), CplexVariable(it.type).toCplexVar())
        }.toList()

        if (cplex.isMIP && model.variables.any { it.initialResult != null }) {
            val initialSolution = model.variables.withIndex()
                .filter { it.value.initialResult != null }
                .map { Pair(cplexVars[it.index], it.value.initialResult!!.toDouble()) }
            cplex.addMIPStart(
                initialSolution.map { it.first }.toTypedArray(),
                initialSolution.map { it.second }.toDoubleArray()
            )
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
                            var lb = Flt64.negativeInfinity
                            var ub = Flt64.infinity
                            when (model.constraints.signs[ii]) {
                                Sign.GreaterEqual -> {
                                    lb = model.constraints.rhs[ii]
                                }

                                Sign.LessEqual -> {
                                    ub = model.constraints.rhs[ii]
                                }

                                Sign.Equal -> {
                                    lb = model.constraints.rhs[ii]
                                    ub = model.constraints.rhs[ii]
                                }
                            }
                            val lhs = cplex.linearNumExpr()
                            for (cell in model.constraints.lhs[ii]) {
                                lhs.addTerm(cell.coefficient.toDouble(), cplexVars[cell.colIndex])
                            }
                            ii to Triple(lb, lhs, ub)
                        }
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        constraints
                    }
                }
                promises.flatMap { promise ->
                    val result = promise.await().map {
                        val (lb, lhs, ub) = it.second
                        val constraint = cplex.range(lb.toDouble(), lhs, ub.toDouble(), model.constraints.names[it.first])
                        cplex.add(constraint)
                        constraint
                    }
                    if (memoryUseOver()) {
                        System.gc()
                    }
                    result
                }
            } else {
                model.constraints.indices.map { i ->
                    var lb = Flt64.negativeInfinity
                    var ub = Flt64.infinity
                    when (model.constraints.signs[i]) {
                        Sign.GreaterEqual -> {
                            lb = model.constraints.rhs[i]
                        }

                        Sign.LessEqual -> {
                            ub = model.constraints.rhs[i]
                        }

                        Sign.Equal -> {
                            lb = model.constraints.rhs[i]
                            ub = model.constraints.rhs[i]
                        }
                    }
                    val lhs = cplex.linearNumExpr()
                    for (cell in model.constraints.lhs[i]) {
                        lhs.addTerm(cell.coefficient.toDouble(), cplexVars[cell.colIndex])
                    }
                    val constraint = cplex.range(lb.toDouble(), lhs, ub.toDouble(), model.constraints.names[i])
                    cplex.add(constraint)
                    constraint
                }
            }
        }
        System.gc()
        cplexConstraints = constraints

        val objective = cplex.linearNumExpr()
        for (cell in model.objective.obj) {
            objective.addTerm(cell.coefficient.toDouble(), cplexVars[cell.colIndex])
        }
        when (model.objective.category) {
            ObjectCategory.Minimum -> {
                cplex.add(cplex.minimize(objective))
            }

            ObjectCategory.Maximum -> {
                cplex.add(cplex.maximize(objective))
            }
        }

        when (val result = callBack?.execIfContain(Point.AfterModeling, null, cplex, cplexVars, cplexConstraints)) {
            is Failed -> {
                return Failed(result.error)
            }

            else -> {}
        }

        logger.trace { "Cplex model dumped for $model" }
        return ok
    }

    private suspend fun configure(): Try {
        cplex.setParam(IloCplex.DoubleParam.TiLim, config.time.toDouble(DurationUnit.SECONDS))
        cplex.setParam(IloCplex.DoubleParam.EpGap, config.gap.toDouble())
        cplex.setParam(IloCplex.IntParam.Threads, config.threadNum.toInt())

        if (config.notImprovementTime != null || callBack?.nativeCallback != null || statusCallBack != null) {
            cplex.use(object : IloCplex.MIPInfoCallback() {
                override fun main() {
                    callBack?.nativeCallback?.invoke(this)

                    val currentObj = Flt64(incumbentObjValue)
                    val currentBound = Flt64(bestObjValue)
                    val currentTime = cplexTime.seconds

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
                                solver = "cplex",
                                time = currentTime,
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
            })
        }

        when (val result = callBack?.execIfContain(Point.Configuration, null, cplex, cplexVars, cplexConstraints)) {
            is Failed -> {
                return Failed(result.error)
            }

            else -> {}
        }
        return ok
    }

    private suspend fun solve(): Try {
        when (val result = callBack?.execIfContain(Point.Solving, null, cplex, cplexVars, cplexConstraints)) {
            is Failed -> {
                return Failed(result.error)
            }

            null -> {
                try {
                    cplex.solve()
                } catch (e: IloException) {
                    return Failed(Err(ErrorCode.OREngineSolvingException, e.message))
                }
                return ok
            }

            else -> {}
        }
        return ok
    }

    private suspend fun analyzeSolution(model: LinearTriadModelView): Try {
        return if (status.succeeded) {
            val obj = Flt64(cplex.objValue) + model.objective.constant
            val possibleBestObj = Flt64(cplex.bestObjValue) + model.objective.constant
            output = FeasibilitySolverOutput(
                obj = obj,
                solution = cplexVars.map { Flt64(cplex.getValue(it)) },
                time = cplex.cplexTime.seconds,
                possibleBestObj = possibleBestObj,
                gap = if (cplex.isMIP) {
                    gap(obj, possibleBestObj)
                } else {
                    Flt64.zero
                }
            )

            when (val result = callBack?.execIfContain(Point.AnalyzingSolution, status, cplex, cplexVars, cplexConstraints)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            ok
        } else {
            when (val result = callBack?.execIfContain(Point.AfterFailure, status, cplex, cplexVars, cplexConstraints)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            Failed(Err(status.errCode!!))
        }
    }
}
