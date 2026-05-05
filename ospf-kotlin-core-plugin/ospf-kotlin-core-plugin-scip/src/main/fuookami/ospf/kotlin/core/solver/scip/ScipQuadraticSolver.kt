@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.scip

import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.solver.output.SolvingStatus
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack

import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.model.basic.nonNullConstraintPriorityAmount
import fuookami.ospf.kotlin.core.solver.QuadraticSolver
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.gap
import fuookami.ospf.kotlin.core.solver.warnIgnoredConstraintPriority
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
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
import kotlin.time.ExperimentalTime
import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput

class ScipQuadraticSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: ScipSolverCallBack? = null
) : QuadraticSolver {
    companion object {
        @JvmStatic
        fun loadLibraryInJar() {
            ScipSolver.loadLibraryInJar()
        }
    }

    override val name = "scip"

    override suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
        val impl = ScipQuadraticSolverImpl(
            config = config,
            callBack = callBack,
            statusCallBack = solvingStatusCallBack
        )
        val result = impl(model)
        System.gc()
        return result
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
            val impl = ScipQuadraticSolverImpl(
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
            )
            val result = impl(model).map { it to results }
            System.gc()
            return result
        }
    }
}

@OptIn(ExperimentalTime::class)
private class ScipQuadraticSolverImpl(
    private val config: SolverConfig,
    private val callBack: ScipSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null
) : ScipSolver() {
    private var mip: Boolean = false

    private lateinit var scipVars: List<jscip.Variable>
    private lateinit var scipConstraints: List<jscip.Constraint>
    private lateinit var scipQuadraticObjectiveVars: List<jscip.Variable>
    private lateinit var scipQuadraticObjectiveTransformers: List<jscip.Constraint>
    private lateinit var output: FeasibleSolverOutput<Flt64>
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
        for (constraint in scipQuadraticObjectiveTransformers) {
            scip.releaseCons(constraint)
        }
        for (variable in scipQuadraticObjectiveVars) {
            scip.releaseVar(variable)
        }
        super.close()
    }

    suspend operator fun invoke(model: QuadraticTetradModelView): Ret<FeasibleSolverOutput<Flt64>> {
        mip = model.containsNotBinaryInteger
        val processes = arrayOf(
            { it.init(model.name) },
            { it.dump(model) },
            { it.configure(model) },
            { it.solve(config.threadNum) },
            ScipQuadraticSolverImpl::analyzeStatus,
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

    private suspend fun dump(model: QuadraticTetradModelView): Try {
        warnIgnoredConstraintPriority("scip", model.nonNullConstraintPriorityAmount())

        scipVars = model.variables.map {
            scip.createVar(
                it.name,
                it.lowerBound.toSolverDouble("quadratic.variables[${it.name}].lowerBound"),
                it.upperBound.toSolverDouble("quadratic.variables[${it.name}].upperBound"),
                0.0,
                ScipVariable(it.type).toSCIPVar()
            )
        }.toList()

        val withResultAmount = model.variables.count { it.initialResult != null }
        if (withResultAmount == model.variables.size) {
            val initialSolution = scip.createSol()
            for ((col, variable) in model.variables.withIndex().filter { it.value.initialResult != null }) {
                scip.setSolVal(initialSolution, scipVars[col], variable.initialResult!!.toSolverDouble("quadratic.variables[$col].initialResult"))
            }
            scip.addSolFree(initialSolution)
        } else if (withResultAmount > 0) {
            val initialSolution = scip.createPartialSol()
            for ((col, variable) in model.variables.withIndex().filter { it.value.initialResult != null }) {
                scip.setSolVal(initialSolution, scipVars[col], variable.initialResult!!.toSolverDouble("quadratic.variables[$col].initialResult"))
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
                                ConstraintRelation.GreaterEqual -> {
                                    lb = model.constraints.rhs[ii]
                                }

                                ConstraintRelation.LessEqual -> {
                                    ub = model.constraints.rhs[ii]
                                }

                                ConstraintRelation.Equal -> {
                                    lb = model.constraints.rhs[ii]
                                    ub = model.constraints.rhs[ii]
                                }
                            }
                            val linearVars = ArrayList<jscip.Variable>()
                            val quadraticVars1 = ArrayList<jscip.Variable>()
                            val quadraticVars2 = ArrayList<jscip.Variable>()
                            val linerCoefficients = ArrayList<Double>()
                            val quadraticCoefficients = ArrayList<Double>()
                            model.constraints.sparseLhs.forEachEntry(ii) { colIndex1, colIndex2, coefficient ->
                                if (colIndex2 == null) {
                                    linearVars.add(scipVars[colIndex1])
                                    linerCoefficients.add(coefficient.toSolverDouble("quadratic.constraints.lhs[$ii][$colIndex1].coefficient"))
                                } else {
                                    quadraticVars1.add(scipVars[colIndex1])
                                    quadraticVars2.add(scipVars[colIndex2])
                                    quadraticCoefficients.add(coefficient.toSolverDouble("quadratic.constraints.lhs[$ii][$colIndex1,$colIndex2].coefficient"))
                                }
                            }
                            ii to Triple(lb to ub, linerCoefficients to linearVars, Triple(quadraticCoefficients, quadraticVars1, quadraticVars2))
                        }
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        constraints
                    }
                }
                promises.flatMap { promise ->
                    val result = promise.await().map {
                        val (range, linearCells, quadraticCells) = it.second
                        val (lb, ub) = range
                        val (linerCoefficients, linearVars) = linearCells
                        val (quadraticCoefficients, quadraticVars1, quadraticVars2) = quadraticCells
                        val constraint = scip.createConsQuadratic(
                            model.constraints.names[it.first],
                            quadraticVars1.toTypedArray(),
                            quadraticVars2.toTypedArray(),
                            quadraticCoefficients.toDoubleArray(),
                            linearVars.toTypedArray(),
                            linerCoefficients.toDoubleArray(),
                            lb.toSolverDouble("quadratic.constraints.bounds[${it.first}].lower"),
                            ub.toSolverDouble("quadratic.constraints.bounds[${it.first}].upper")
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
                        ConstraintRelation.GreaterEqual -> {
                            lb = model.constraints.rhs[i]
                        }

                        ConstraintRelation.LessEqual -> {
                            ub = model.constraints.rhs[i]
                        }

                        ConstraintRelation.Equal -> {
                            lb = model.constraints.rhs[i]
                            ub = model.constraints.rhs[i]
                        }
                    }
                    val linearVars = ArrayList<jscip.Variable>()
                    val quadraticVars1 = ArrayList<jscip.Variable>()
                    val quadraticVars2 = ArrayList<jscip.Variable>()
                    val linerCoefficients = ArrayList<Double>()
                    val quadraticCoefficients = ArrayList<Double>()
                    model.constraints.sparseLhs.forEachEntry(i) { colIndex1, colIndex2, coefficient ->
                        if (colIndex2 == null) {
                            linearVars.add(scipVars[colIndex1])
                            linerCoefficients.add(coefficient.toSolverDouble("quadratic.constraints.lhs[$i][$colIndex1].coefficient"))
                        } else {
                            quadraticVars1.add(scipVars[colIndex1])
                            quadraticVars2.add(scipVars[colIndex2])
                            quadraticCoefficients.add(coefficient.toSolverDouble("quadratic.constraints.lhs[$i][$colIndex1,$colIndex2].coefficient"))
                        }
                    }
                    val constraint = scip.createConsQuadratic(
                        model.constraints.names[i],
                        quadraticVars1.toTypedArray(),
                        quadraticVars2.toTypedArray(),
                        quadraticCoefficients.toDoubleArray(),
                        linearVars.toTypedArray(),
                        linerCoefficients.toDoubleArray(),
                        lb.toSolverDouble("quadratic.constraints.bounds[$i].lower"),
                        ub.toSolverDouble("quadratic.constraints.bounds[$i].upper")
                    )
                    scip.addCons(constraint)
                    constraint
                }
            }
        }
        System.gc()
        scipConstraints = constraints

        val qovars = ArrayList<jscip.Variable>()
        val qocons = ArrayList<jscip.Constraint>()
        for (cell in model.objective.objective) {
            if (cell.colIndex2 == null) {
                scip.changeVarObj(scipVars[cell.colIndex1], cell.coefficient.toSolverDouble("quadratic.objective.cells[${cell.colIndex1}].coefficient"))
            } else {
                val qovar = scip.createVar(
                    "${scipVars[cell.colIndex1].name}_${scipVars[cell.colIndex2!!]}",
                    -Double.MAX_VALUE,
                    Double.MAX_VALUE,
                    0.0,
                    jscip.SCIP_Vartype.SCIP_VARTYPE_CONTINUOUS
                )
                val qocon = scip.createConsQuadratic(
                    "${scipVars[cell.colIndex1].name}_${scipVars[cell.colIndex2!!]}",
                    arrayOf(scipVars[cell.colIndex1]),
                    arrayOf(scipVars[cell.colIndex2!!]),
                    doubleArrayOf(1.0),
                    arrayOf(qovar),
                    doubleArrayOf(-1.0),
                    0.0,
                    0.0
                )
                scip.addCons(qocon)
                scip.changeVarObj(qovar, cell.coefficient.toSolverDouble("quadratic.objective.cells[${cell.colIndex1},${cell.colIndex2}].coefficient"))
                qovars.add(qovar)
                qocons.add(qocon)
            }
        }
        scipQuadraticObjectiveVars = qovars
        scipQuadraticObjectiveTransformers = qocons

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

    private suspend fun configure(model: QuadraticTetradModelView): Try {
        scip.setRealParam("limits/time", config.time.toDouble(DurationUnit.SECONDS))
        scip.setRealParam("limits/gap", config.gap.toSolverDouble("quadratic.config.gap"))
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
                            fuookami.ospf.kotlin.core.solver.output.SolvingStatus(
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

    private suspend fun analyzeSolution(model: QuadraticTetradModelView): Try {
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
            output = FeasibleSolverOutput<Flt64>(
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


