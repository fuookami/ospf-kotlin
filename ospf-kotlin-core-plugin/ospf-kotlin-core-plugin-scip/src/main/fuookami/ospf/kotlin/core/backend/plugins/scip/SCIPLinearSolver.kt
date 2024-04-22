package fuookami.ospf.kotlin.core.backend.plugins.scip

import kotlin.time.*
import kotlinx.datetime.*
import kotlinx.coroutines.*
import jscip.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

class SCIPLinearSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: SCIPSolverCallBack? = null
) : LinearSolver {
    override suspend operator fun invoke(model: LinearTriadModelView): Ret<SolverOutput> {
        val impl = SCIPLinearSolverImpl(config, callBack)
        return impl(model)
    }

    override suspend fun invoke(model: LinearTriadModelView, solutionAmount: UInt64): Ret<Pair<SolverOutput, List<Solution>>> {
        return if (solutionAmount leq UInt64.one) {
            this(model).map { it to emptyList() }
        } else {
            val results = ArrayList<Solution>()
            val impl = SCIPLinearSolverImpl(config, callBack.ifNull { SCIPSolverCallBack() }.copy()
                .configuration { scip, _, _ ->
                    scip.setIntParam("heuristics/dins/solnum", min(UInt64.ten, solutionAmount).toInt())
                    ok
                }
                .analyzingSolution { scip, variables, _ ->
                    val bestSol = scip.bestSol
                    val sols = scip.sols
                    var i = UInt64.zero
                    for (sol in sols) {
                        if (sol != bestSol) {
                            val thisResults = java.util.ArrayList<Flt64>()
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
                }
            )
            impl(model).map { it to results }
        }
    }
}

private class SCIPLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: SCIPSolverCallBack? = null
) : SCIPSolver() {
    var mip: Boolean = false

    lateinit var scipVars: List<jscip.Variable>
    lateinit var scipConstraints: List<jscip.Constraint>
    var solvingTime: Duration? = null
    lateinit var output: SolverOutput

    override fun finalize() {
        for (constraint in scipConstraints) {
            scip.releaseCons(constraint)
        }
        for (variable in scipVars) {
            scip.releaseVar(variable)
        }
        super.finalize()
    }

    suspend operator fun invoke(model: LinearTriadModelView): Ret<SolverOutput> {
        mip = model.containsNotBinaryInteger
        val processes = arrayOf(
            { it.init(model.name) },
            { it.dump(model) },
            SCIPLinearSolverImpl::configure,
            SCIPLinearSolverImpl::solve,
            SCIPLinearSolverImpl::analyzeStatus,
            SCIPLinearSolverImpl::analyzeSolution
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
        scipVars = model.variables.map {
            scip.createVar(
                it.name,
                it.lowerBound.toDouble(),
                it.upperBound.toDouble(),
                0.0,
                SCIPVariable(it.type).toSCIPVar()
            )
        }.toList()

        if (model.variables.all { it.initialResult != null }) {
            val initialSolution = scip.createSol()
            for ((col, variable) in model.variables.withIndex().filter { it.value.initialResult != null }) {
                scip.setSolVal(initialSolution, scipVars[col], variable.initialResult!!.toDouble())
            }
            scip.addSolFree(initialSolution)
        } else if (model.variables.any { it.initialResult != null }) {
            val initialSolution = scip.createPartialSol()
            for ((col, variable) in model.variables.withIndex().filter { it.value.initialResult != null }) {
                scip.setSolVal(initialSolution, scipVars[col], variable.initialResult!!.toDouble())
            }
            scip.addSolFree(initialSolution)
        }

        val constraints = coroutineScope {
            val promises = model.constraints.indices.map { i ->
                i to async(Dispatchers.Default) {
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
                        coefficients.add(cell.coefficient.toDouble())
                    }
                    Triple(lb, coefficients to vars, ub)
                }
            }
            promises.map {
                val (lb, cells, ub) = it.second.await()
                val (coefficients, vars) = cells
                val constraint = scip.createConsLinear(
                    model.constraints.names[it.first],
                    vars.toTypedArray(),
                    coefficients.toDoubleArray(),
                    lb.toDouble(),
                    ub.toDouble()
                )
                scip.addCons(constraint)
                constraint
            }
        }
        scipConstraints = constraints

        for (cell in model.objective.obj) {
            scip.changeVarObj(scipVars[cell.colIndex], cell.coefficient.toDouble())
        }
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
        scip.solveConcurrent()
        val stage = scip.stage
        if (stage.swigValue() < SCIP_Stage.SCIP_STAGE_INITPRESOLVE.swigValue()) {
            scip.solve()
        }
        solvingTime = Clock.System.now() - begin

        return ok
    }

    private suspend fun analyzeSolution(): Try {
        return if (status.succeeded()) {
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
                obj,
                results,
                solvingTime!!,
                possibleBestObj,
                gap
            )

            when (val result = callBack?.execIfContain(Point.AnalyzingSolution, scip, scipVars, scipConstraints)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            return ok
        } else {
            Failed(Err(status.errCode()!!))
        }
    }
}
