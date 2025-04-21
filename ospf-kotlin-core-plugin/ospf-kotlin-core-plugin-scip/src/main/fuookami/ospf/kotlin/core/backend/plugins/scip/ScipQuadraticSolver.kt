package fuookami.ospf.kotlin.core.backend.plugins.scip

import kotlin.time.*
import kotlinx.datetime.*
import kotlinx.coroutines.*
import jscip.*
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
        statusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        val impl = ScipQuadraticSolverImpl(config, callBack, statusCallBack)
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
            val impl = ScipQuadraticSolverImpl(
                config = config,
                callBack
                    .copyIfNotNullOr { ScipSolverCallBack() }
                    .configuration { scip, _, _ ->
                        if (solutionAmount gr UInt64.one) {
                            scip.setIntParam("heuristics/dins/solnum", solutionAmount.toInt())
                        }
                        ok
                    }
                    .analyzingSolution { scip, variables, _ ->
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
                statusCallBack = statusCallBack
            )
            val result = impl(model).map { it to results }
            System.gc()
            return result
        }
    }
}

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
    private lateinit var output: SolverOutput

    override fun finalize() {
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
        super.finalize()
    }

    suspend operator fun invoke(model: QuadraticTetradModelView): Ret<SolverOutput> {
        mip = model.containsNotBinaryInteger
        val processes = arrayOf(
            { it.init(model.name) },
            { it.dump(model) },
            ScipQuadraticSolverImpl::configure,
            { it.solve(config.threadNum) },
            ScipQuadraticSolverImpl::analyzeStatus,
            ScipQuadraticSolverImpl::analyzeSolution
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
        scipVars = model.variables.map {
            scip.createVar(
                it.name,
                it.lowerBound.toDouble(),
                it.upperBound.toDouble(),
                0.0,
                ScipVariable(it.type).toSCIPVar()
            )
        }.toList()

        val withResultAmount = model.variables.count { it.initialResult != null }
        if (withResultAmount == model.variables.size) {
            val initialSolution = scip.createSol()
            for ((col, variable) in model.variables.withIndex().filter { it.value.initialResult != null }) {
                scip.setSolVal(initialSolution, scipVars[col], variable.initialResult!!.toDouble())
            }
            scip.addSolFree(initialSolution)
        } else if (withResultAmount > 0) {
            val initialSolution = scip.createPartialSol()
            for ((col, variable) in model.variables.withIndex().filter { it.value.initialResult != null }) {
                scip.setSolVal(initialSolution, scipVars[col], variable.initialResult!!.toDouble())
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
                            val linearVars = ArrayList<jscip.Variable>()
                            val quadraticVars1 = ArrayList<jscip.Variable>()
                            val quadraticVars2 = ArrayList<jscip.Variable>()
                            val linerCoefficients = ArrayList<Double>()
                            val quadraticCoefficients = ArrayList<Double>()
                            for (cell in model.constraints.lhs[ii]) {
                                if (cell.colIndex2 == null) {
                                    linearVars.add(scipVars[cell.colIndex1])
                                    linerCoefficients.add(cell.coefficient.toDouble())
                                } else {
                                    quadraticVars1.add(scipVars[cell.colIndex1])
                                    quadraticVars2.add(scipVars[cell.colIndex2!!])
                                    quadraticCoefficients.add(cell.coefficient.toDouble())
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
                            lb.toDouble(),
                            ub.toDouble()
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
                    val linearVars = ArrayList<jscip.Variable>()
                    val quadraticVars1 = ArrayList<jscip.Variable>()
                    val quadraticVars2 = ArrayList<jscip.Variable>()
                    val linerCoefficients = ArrayList<Double>()
                    val quadraticCoefficients = ArrayList<Double>()
                    for (cell in model.constraints.lhs[i]) {
                        if (cell.colIndex2 == null) {
                            linearVars.add(scipVars[cell.colIndex1])
                            linerCoefficients.add(cell.coefficient.toDouble())
                        } else {
                            quadraticVars1.add(scipVars[cell.colIndex1])
                            quadraticVars2.add(scipVars[cell.colIndex2!!])
                            quadraticCoefficients.add(cell.coefficient.toDouble())
                        }
                    }
                    val constraint = scip.createConsQuadratic(
                        model.constraints.names[i],
                        quadraticVars1.toTypedArray(),
                        quadraticVars2.toTypedArray(),
                        quadraticCoefficients.toDoubleArray(),
                        linearVars.toTypedArray(),
                        linerCoefficients.toDoubleArray(),
                        lb.toDouble(),
                        ub.toDouble()
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
        for (cell in model.objective.obj) {
            if (cell.colIndex2 == null) {
                scip.changeVarObj(scipVars[cell.colIndex1], cell.coefficient.toDouble())
            } else {
                val qovar = scip.createVar(
                    "${scipVars[cell.colIndex1].name}_${scipVars[cell.colIndex2!!]}",
                    -Double.MAX_VALUE,
                    Double.MAX_VALUE,
                    0.0,
                    SCIP_Vartype.SCIP_VARTYPE_CONTINUOUS
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
                scip.changeVarObj(qovar, cell.coefficient.toDouble())
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

        when (val result = callBack?.execIfContain(Point.AfterModeling, scip, scipVars, scipConstraints)) {
            is Failed -> {
                return Failed(result.error)
            }

            else -> {}
        }
        return ok
    }

    private suspend fun configure(): Try {
        scip.setRealParam("limits/time", config.time.toDouble(DurationUnit.SECONDS))
        scip.setRealParam("limits/gap", config.gap.toDouble())
        scip.setIntParam("parallel/maxnthreads", config.threadNum.toInt())

        // todo: use call back to control it
        if (config.notImprovementTime != null) {
            scip.setRealParam("limits/stallnodes", config.notImprovementTime!!.toDouble(DurationUnit.MILLISECONDS))
        }

        when (val result = callBack?.execIfContain(Point.Configuration, scip, scipVars, scipConstraints)) {
            is Failed -> {
                return Failed(result.error)
            }

            else -> {}
        }
        return ok
    }

    private suspend fun solve(): Try {
        val begin = Clock.System.now()
        if (config.threadNum gr UInt64.one) {
            scip.solveConcurrent()
            val stage = scip.stage
            if (stage.swigValue() < SCIP_Stage.SCIP_STAGE_INITPRESOLVE.swigValue()) {
                scip.solve()
            }
        } else {
            scip.solve()
        }
        solvingTime = Clock.System.now() - begin

        return ok
    }

    private suspend fun analyzeSolution(): Try {
        return if (status.succeeded) {
            val solution = scip.bestSol
            val results = ArrayList<Flt64>()
            for (scipVar in scipVars) {
                results.add(Flt64(scip.getSolVal(solution, scipVar)))
            }
            val obj = Flt64(scip.getSolOrigObj(solution))
            val possibleBestObj = Flt64(scip.dualbound)
            val gap = if (mip) {
                (obj - possibleBestObj + Flt64.decimalPrecision) / (obj + Flt64.decimalPrecision)
            } else {
                Flt64.zero
            }
            output = SolverOutput(
                obj = obj,
                solution = results,
                time = solvingTime!!,
                possibleBestObj = possibleBestObj,
                gap = gap
            )

            when (val result = callBack?.execIfContain(Point.AnalyzingSolution, scip, scipVars, scipConstraints)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            return ok
        } else {
            Failed(Err(status.errCode!!))
        }
    }
}
