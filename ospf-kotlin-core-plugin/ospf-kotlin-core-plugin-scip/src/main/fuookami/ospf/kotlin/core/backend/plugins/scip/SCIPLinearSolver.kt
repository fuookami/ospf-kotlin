package fuookami.ospf.kotlin.core.backend.plugins.scip

import kotlin.time.*
import kotlinx.datetime.*
import jscip.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class SCIPLinearSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: SCIPSolverCallBack? = null
) : LinearSolver {
    override suspend operator fun invoke(model: LinearTriadModelView): Ret<SolverOutput> {
        val impl = SCIPLinearSolverImpl(config, callBack)
        return impl(model)
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

    operator fun invoke(model: LinearTriadModelView): Ret<SolverOutput> {
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

    private fun dump(model: LinearTriadModelView): Try {
        scipVars = model.variables.map {
            scip.createVar(
                it.name,
                it.lowerBound.toDouble(),
                it.upperBound.toDouble(),
                0.0,
                SCIPVariable(it.type).toSCIPVar()
            )
        }.toList()

        if (model.variables.any { it.initialResult != null }) {
            val initialSolution = scip.createPartialSol()
            for ((col, variable) in model.variables.withIndex().filter { it.value.initialResult != null }) {
                scip.setSolVal(initialSolution, scipVars[col], variable.initialResult!!.toDouble())
            }
            scip.addSolFree(initialSolution)
        }

        var i = 0
        var j = 0
        val constraints = ArrayList<jscip.Constraint>()
        while (i != model.constraints.size) {
            var lhs = Flt64.negativeInfinity
            var rhs = Flt64.infinity
            when (model.constraints.signs[i]) {
                Sign.GreaterEqual -> {
                    lhs = model.constraints.rhs[i]
                }

                Sign.LessEqual -> {
                    rhs = model.constraints.rhs[i]
                }

                Sign.Equal -> {
                    lhs = model.constraints.rhs[i]
                    rhs = model.constraints.rhs[i]
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
                coefficients.toDoubleArray(), lhs.toDouble(), rhs.toDouble()
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

        when (val result = callBack?.execIfContain(Point.AfterModeling, scip, scipVars, scipConstraints)) {
            is Failed -> {
                return Failed(result.error)
            }

            else -> {}
        }
        return ok
    }

    private fun configure(): Try {
        scip.setRealParam("limits/time", config.time.toDouble(DurationUnit.SECONDS))
        scip.setRealParam("limits/gap", config.gap.toDouble())
        scip.setIntParam("parallel/maxnthreads", config.threadNum.toInt())

        when (val result = callBack?.execIfContain(Point.Configuration, scip, scipVars, scipConstraints)) {
            is Failed -> {
                return Failed(result.error)
            }

            else -> {}
        }
        return ok
    }

    private fun solve(): Try {
        val begin = Clock.System.now()
        scip.solveConcurrent()
        val stage = scip.stage
        if (stage.swigValue() < SCIP_Stage.SCIP_STAGE_INITPRESOLVE.swigValue()) {
            scip.solve()
        }
        solvingTime = Clock.System.now() - begin

        return ok
    }

    private fun analyzeSolution(): Try {
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
