@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.backend.plugins.scip

import fuookami.ospf.kotlin.core.backend.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.backend.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.backend.solver.output.SolvingStatus
import fuookami.ospf.kotlin.core.backend.solver.output.SolvingStatusCallBack

import fuookami.ospf.kotlin.core.backend.intermediate_model.LinearTriadModelView
import fuookami.ospf.kotlin.core.backend.intermediate_model.nonNullConstraintPriorityAmount
import fuookami.ospf.kotlin.core.backend.solver.LinearSolver
import fuookami.ospf.kotlin.core.backend.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.backend.solver.gap
import fuookami.ospf.kotlin.core.backend.solver.warnIgnoredConstraintPriority
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.core.frontend.model.mechanism.ObjectCategory
import fuookami.ospf.kotlin.core.frontend.model.mechanism.Sign
import fuookami.ospf.kotlin.utils.concept.copyIfNotNullOr
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.memoryUseOver
import fuookami.ospf.kotlin.math.operator.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import jscip.Event
import jscip.EventHandler
import jscip.EventHandlerRef
import jscip.EventMask
import java.util.UUID
import kotlin.time.DurationUnit
import kotlin.time.Duration.Companion.seconds

class ScipLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: ScipSolverCallBack? = null
) : LinearSolver {
    companion object {
        @JvmStatic
        fun loadLibraryInJar() {
            ScipSolver.loadLibraryInJar()
        }
    }

    override val name = "scip"

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput> {
        return ScipLinearSolverImpl(
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
            ScipLinearSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { ScipSolverCallBack() }
                    .configuration { _, scip, _, _ ->
                        if (solutionAmount gr UInt64.one) {
                            scip.setIntParam("heuristics/dins/solnum", solutionAmount.toInt())
                        }
                        ok
                    }
                    .analyzingSolution { _, scip, variables, _ ->
                        val bestSol = scip.bestSol
                        val sols = scip.sols
                        var i = UInt64.zero
                        for (sol in sols) {
                            if (sol != bestSol) {
                                val thisResults = ArrayList<Flt64>()
                                for (scipVar in variables) {
                                    thisResults.add(Flt64(scip.getSolVal(sol, scipVar)))
                                }
                                if (!results.any { it.toTypedArray() contentEquals thisResults.toTypedArray() }) {
                                    results.add(thisResults)
                                }
                            }
                            ++i
                            if (i >= solutionAmount) {
                                break
                            }
                        }
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

private class ScipLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: ScipSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null
) : ScipSolver() {
    private var mip: Boolean = false

    private lateinit var scipVars: List<jscip.Variable>
    private lateinit var scipConstraints: List<jscip.Constraint>
    private lateinit var output: FeasibleSolverOutput
    private var initialBestObj: Flt64? = null
    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestTime = 0.0.seconds

    override fun close() {
        for (constraint in scipConstraints) {
            scip.releaseCons(constraint)
        }
        for (variable in scipVars) {
            scip.releaseVar(variable)
        }
        super.close()
    }

    suspend operator fun invoke(model: LinearTriadModelView): Ret<FeasibleSolverOutput> {
        mip = model.containsNotBinaryInteger
        val processes = arrayOf(
            { it.init(model.name) },
            { it.dump(model) },
            { it.configure(model) },
            { it.solve(config.threadNum) },
            ScipLinearSolverImpl::analyzeStatus,
            { it.analyzeSolution(model) }
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
        warnIgnoredConstraintPriority("scip", model.nonNullConstraintPriorityAmount())

        scipVars = model.variables.map {
            scip.createVar(
                it.name,
                it.lowerBound.toSolverDouble("linear.variables[${it.name}].lowerBound"),
                it.upperBound.toSolverDouble("linear.variables[${it.name}].upperBound"),
                0.0,
                ScipVariable(it.type).toSCIPVar()
            )
        }.toList()

        if (model.variables.all { it.initialResult != null }) {
            val initialSolution = scip.createSol()
            for ((col, variable) in model.variables.withIndex().filter { it.value.initialResult != null }) {
                scip.setSolVal(initialSolution, scipVars[col], variable.initialResult!!.toSolverDouble("linear.variables[$col].initialResult"))
            }
            scip.addSolFree(initialSolution)
        } else if (model.variables.any { it.initialResult != null }) {
            val initialSolution = scip.createPartialSol()
            for ((col, variable) in model.variables.withIndex().filter { it.value.initialResult != null }) {
                scip.setSolVal(initialSolution, scipVars[col], variable.initialResult!!.toSolverDouble("linear.variables[$col].initialResult"))
            }
            scip.addSolFree(initialSolution)
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
                            val vars = ArrayList<jscip.Variable>()
                            val coefficients = ArrayList<Double>()
                            for (cell in model.constraints.lhs[ii]) {
                                vars.add(scipVars[cell.colIndex])
                                coefficients.add(cell.coefficient.toSolverDouble("linear.constraints.lhs[$ii][${cell.colIndex}].coefficient"))
                            }
                            ii to Triple(lb, coefficients to vars, ub)
                        }
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        constraints
                    }
                }
                promises.flatMap { promise ->
                    val result = promise.await().map {
                        val (lb, cells, ub) = it.second
                        val (coefficients, vars) = cells
                        val constraint = scip.createConsLinear(
                            model.constraints.names[it.first],
                            vars.toTypedArray(),
                            coefficients.toDoubleArray(),
                            lb.toSolverDouble("linear.constraints.bounds[${it.first}].lower"),
                            ub.toSolverDouble("linear.constraints.bounds[${it.first}].upper")
                        )
                        scip.addCons(constraint)
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
                    val vars = ArrayList<jscip.Variable>()
                    val coefficients = ArrayList<Double>()
                    for (cell in model.constraints.lhs[i]) {
                        vars.add(scipVars[cell.colIndex])
                        coefficients.add(cell.coefficient.toSolverDouble("linear.constraints.lhs[$i][${cell.colIndex}].coefficient"))
                    }
                    val constraint = scip.createConsLinear(
                        model.constraints.names[i],
                        vars.toTypedArray(),
                        coefficients.toDoubleArray(),
                        lb.toSolverDouble("linear.constraints.bounds[$i].lower"),
                        ub.toSolverDouble("linear.constraints.bounds[$i].upper")
                    )
                    scip.addCons(constraint)
                    constraint
                }
            }
        }
        System.gc()
        scipConstraints = constraints

        for (cell in model.objective.objective) {
            scip.changeVarObj(scipVars[cell.colIndex], cell.coefficient.toSolverDouble("linear.objective.cells[${cell.colIndex}].coefficient"))
        }
        when (model.objective.category) {
            ObjectCategory.Minimum -> {
                scip.setMinimize()
            }

            ObjectCategory.Maximum -> {
                scip.setMaximize()
            }
        }

        when (val result = callBack?.execIfContain(
            point = Point.AfterModeling,
            status = null,
            scip = scip,
            variables = scipVars,
            constraints = scipConstraints
        )) {
            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            else -> {}
        }
        return ok
    }

    private suspend fun configure(model: LinearTriadModelView): Try {
        scip.setRealParam("limits/time", config.time.toDouble(DurationUnit.SECONDS))
        scip.setRealParam("limits/gap", config.gap.toSolverDouble("linear.config.gap"))
        scip.setIntParam("parallel/maxnthreads", config.threadNum.toInt())

        if (config.notImprovementTime != null || callBack?.nativeCallback != null || statusCallBack != null) {
            scip.includeEventHandler("solve-monitor-${UUID.randomUUID()}", "native solving callback", object : EventHandler() {
                override fun getType(): Long {
                    return callBack?.nativeEventMask ?: (EventMask.LP_EVENT or EventMask.NODE_EVENT or EventMask.SOL_EVENT)
                }

                override fun execute(solverModel: jscip.Scip, self: EventHandlerRef, event: Event) {
                    try {
                        callBack?.nativeCallback?.invoke(this, solverModel, self, event)
                    } catch (_: Exception) {
                        solverModel.interruptSolve()
                        return
                    }

                    val bestSolution = solverModel.bestSol
                    val currentObj = if (bestSolution == null) {
                        Flt64(solverModel.primalbound)
                    } else {
                        Flt64(solverModel.getSolOrigObj(bestSolution))
                    }
                    val currentBound = Flt64(solverModel.dualbound)
                    val currentTime = solverModel.solvingTime.seconds

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
                            solverModel.interruptSolve()
                            return
                        }
                    }

                    statusCallBack?.let {
                        val currentBestSolution = if (bestSolution == null) {
                            null
                        } else {
                            scipVars.map { variable -> Flt64(solverModel.getSolVal(bestSolution, variable)) }
                        }
                        when (it(
                            fuookami.ospf.kotlin.core.backend.solver.output.SolvingStatus(
                                solver = "scip",
                                solverConfig = config,
                                intermediateModel = model,
                                solverModel = solverModel,
                                solverCallBack = this,
                                objectCategory = model.objective.category,
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
                                solverModel.interruptSolve()
                            }

                            is Fatal -> {
                                solverModel.interruptSolve()
                            }
                        }
                    }
                }
            })
        }

        scip.messagehdlr

        when (val result = callBack?.execIfContain(
            point = Point.Configuration,
            status = null,
            scip = scip,
            variables = scipVars,
            constraints = scipConstraints
        )) {
            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            else -> {}
        }
        return ok
    }

    private suspend fun analyzeSolution(model: LinearTriadModelView): Try {
        return if (status.succeeded) {
            val solution = scip.bestSol
            val results = ArrayList<Flt64>()
            for (scipVar in scipVars) {
                results.add(Flt64(scip.getSolVal(solution, scipVar)))
            }
            val obj = Flt64(scip.getSolOrigObj(solution)) + model.objective.constant
            val possibleBestObj = Flt64(scip.dualbound) + model.objective.constant
            val gap = if (mip) {
                gap(obj, possibleBestObj)
            } else {
                Flt64.zero
            }
            output = FeasibleSolverOutput(
                obj = obj,
                solution = results,
                time = solvingTime!!,
                possibleBestObj = possibleBestObj,
                gap = gap
            )

            when (val result = callBack?.execIfContain(
                point = Point.AnalyzingSolution,
                status = status,
                scip = scip,
                variables = scipVars,
                constraints = scipConstraints
            )) {
                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }

                else -> {}
            }
            return ok
        } else {
            when (val result = callBack?.execIfContain(
                point = Point.AfterFailure,
                status = status,
                scip = scip,
                variables = scipVars,
                constraints = scipConstraints
            )) {
                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }

                else -> {}
            }
            Failed(Err(status.errCode!!))
        }
    }
}


