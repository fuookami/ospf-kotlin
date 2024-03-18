package fuookami.ospf.kotlin.core.backend.plugins.cplex

import kotlin.time.*
import kotlin.time.Duration.Companion.microseconds
import ilog.concert.*
import ilog.cplex.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class CplexLinearSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: CplexSolverCallBack? = null
) : LinearSolver {
    override suspend operator fun invoke(model: LinearTriadModelView): Ret<SolverOutput> {
        val impl = CplexLinearSolverImpl(config, callBack)
        return impl(model)
    }
}

private class CplexLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: CplexSolverCallBack? = null
): CplexSolver() {
    lateinit var cplexVars: List<IloNumVar>
    lateinit var cplexConstraint: List<IloRange>
    lateinit var output: SolverOutput

    operator fun invoke(model: LinearTriadModelView): Ret<SolverOutput> {
        val processes = arrayOf(
            { it.init(model.name) },
            { it.dump(model) },
            CplexLinearSolverImpl::configure,
            CplexLinearSolverImpl::solve,
            CplexLinearSolverImpl::analyzeStatus,
            CplexLinearSolverImpl::analyzeSolution
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

        var i = 0
        var j = 0
        val constraints = ArrayList<IloRange>()
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
            val lhs = cplex.linearNumExpr()
            while (j != model.constraints.lhs.size && i == model.constraints.lhs[j].rowIndex) {
                val cell = model.constraints.lhs[j]
                lhs.addTerm(cell.coefficient.toDouble(), cplexVars[cell.colIndex])
                ++j
            }
            val constraint = cplex.range(lb.toDouble(), lhs, ub.toDouble(), model.constraints.names[i])
            constraints.add(constraint)
            ++i
            cplex.add(constraint)
        }
        cplexConstraint = constraints

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

        when (val result = callBack?.execIfContain(Point.AfterModeling, cplex, cplexVars, cplexConstraint)) {
            is Failed -> {
                return Failed(result.error)
            }

            else -> {}
        }
        return ok
    }

    private fun configure(): Try {
        cplex.setParam(IloCplex.DoubleParam.TiLim, config.time.toDouble(DurationUnit.SECONDS))
        cplex.setParam(IloCplex.DoubleParam.EpGap, config.gap.toDouble())
        cplex.setParam(IloCplex.IntParam.Threads, config.threadNum.toInt())

        when (val result = callBack?.execIfContain(Point.Configuration, cplex, cplexVars, cplexConstraint)) {
            is Failed -> {
                return Failed(result.error)
            }

            else -> {}
        }
        return ok
    }

    private fun solve(): Try {
        when (val result = callBack?.execIfContain(Point.Solving, cplex, cplexVars, cplexConstraint)) {
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

    private fun analyzeSolution(): Try {
        return if (status.succeeded()) {
            output = SolverOutput(
                Flt64(cplex.objValue),
                cplexVars.map { Flt64(cplex.getValue(it)) },
                cplex.cplexTime.toLong().microseconds,
                Flt64(cplex.bestObjValue),
                Flt64(
                    if (cplex.isMIP) {
                        cplex.mipRelativeGap
                    } else {
                        0.0
                    }
                )
            )

            when (val result = callBack?.execIfContain(Point.AnalyzingSolution, cplex, cplexVars, cplexConstraint)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            ok
        } else {
            Failed(Err(status.errCode()!!))
        }
    }
}
