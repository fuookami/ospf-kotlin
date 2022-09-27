package fuookami.ospf.kotlin.core.backend.plugins.scip

import kotlin.time.*
import kotlinx.datetime.*
import jscip.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class SCIPLinearSolver(
    private val config: LinearSolverConfig,
    private val callBack: SCIPSolverCallBack? = null
) {
    operator fun invoke(model: LinearTriadModel): Result<LinearSolverOutput, Error> {
        val impl = SCIPLinearSolverImpl(config, callBack)
        return impl(model)
    }
}

private class SCIPLinearSolverImpl(
    private val config: LinearSolverConfig,
    private val callBack: SCIPSolverCallBack? = null
) {
    var mip: Boolean = false

    lateinit var scip: Scip
    lateinit var scipVars: List<jscip.Variable>
    lateinit var scipConstraints: List<jscip.Constraint>
    var solvingTime: Duration? = null
    lateinit var status: SolvingStatus
    lateinit var output: LinearSolverOutput

    companion object {
        init {
            System.loadLibrary("jscip")
        }
    }

    protected fun finalize() {
        for (constraint in scipConstraints) {
            scip.releaseCons(constraint)
        }
        for (variable in scipVars) {
            scip.releaseVar(variable)
        }
        scip.free()
    }

    operator fun invoke(model: LinearTriadModel): Result<LinearSolverOutput, Error> {
        assert(!this::scip.isInitialized)

        mip = model.containsNotBinaryInteger()
        val processes = arrayOf(
            { it.init(model) },
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

    private fun init(model: LinearTriadModel): Try<Error> {
        scip = Scip()
        scip.create(model.name)
        return Ok(success)
    }

    private fun dump(model: LinearTriadModel): Try<Error> {
        scipVars = model.variables.map {
            scip.createVar(
                it.name,
                it.lowerBound.toDouble(),
                it.upperBound.toDouble(),
                0.0,
                SCIPVariable(it.type).toSCIPVar()
            )
        }.toList()

        var i = 0
        var j = 0
        val constraints = ArrayList<jscip.Constraint>()
        while (i != model.constraints.size) {
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
            while (j != model.constraints.lhs.size && i == model.constraints.lhs[j].rowIndex) {
                val cell = model.constraints.lhs[j]
                vars.add(scipVars[cell.colIndex])
                coefficients.add(cell.coefficient.toDouble())
                ++j
            }
            val constraint = scip.createConsLinear(
                model.constraints.names[i], vars.toTypedArray(),
                coefficients.toDoubleArray(), lb.toDouble(), ub.toDouble()
            )
            constraints.add(constraint)
            ++i
            scip.addCons(constraint)
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

        callBack?.execIfContain(Point.AfterModeling, scip, scipVars, scipConstraints)
        return Ok(success)
    }

    private fun configure(): Try<Error> {
        scip.setRealParam("limits/time", config.time.toDouble(DurationUnit.SECONDS))
        scip.setRealParam("limits/gap", config.gap.toDouble())
        scip.setIntParam("parallel/maxnthreads", config.threadNum.toInt())

        callBack?.execIfContain(Point.Configuration, scip, scipVars, scipConstraints)
        return Ok(success)
    }

    private fun solve(): Try<Error> {
        val begin = Clock.System.now()
        scip.solve()
        solvingTime = Clock.System.now() - begin

        return Ok(success)
    }

    private fun analyzeStatus(): Try<Error> {
        val eq = Equal(Flt64)

        val solution = scip.bestSol
        val gap = if (mip) {
            val obj = Flt64(scip.getSolOrigObj(solution))
            val possibleBestObj = Flt64(scip.dualbound)
            (obj - possibleBestObj + Flt64.epsilon) / (obj + Flt64.epsilon)
        } else {
            Flt64.zero
        }
        status = if (solution != null) {
            if (eq(gap, Flt64.zero)) {
                SolvingStatus.Optimal
            } else {
                SolvingStatus.Feasible
            }
        } else {
            SolvingStatus.NoSolution
        }

        return Ok(success)
    }

    private fun analyzeSolution(): Try<Error> {
        return if (status.succeeded()) {
            val solution = scip.bestSol
            val results = ArrayList<Flt64>()
            for (scipVar in scipVars) {
                results.add(Flt64(scip.getSolVal(solution, scipVar)))
            }
            val obj = Flt64(scip.getSolOrigObj(solution))
            val possibleBestObj = Flt64(scip.dualbound)
            val gap = if (mip) {
                (obj - possibleBestObj + Flt64.epsilon) / (obj + Flt64.epsilon)
            } else {
                Flt64.zero
            }
            output = LinearSolverOutput(
                obj,
                results,
                solvingTime!!,
                possibleBestObj,
                gap
            )

            callBack?.execIfContain(Point.AnalyzingSolution, scip, scipVars, scipConstraints)
            return Ok(success)
        } else {
            Failed(Err(status.errCode()!!))
        }
    }
}