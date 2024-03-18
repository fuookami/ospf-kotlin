package fuookami.ospf.kotlin.core.backend.plugins.gurobi

import kotlin.time.*
import kotlin.time.Duration.Companion.milliseconds
import gurobi.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class GurobiLinearSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: GurobiLinearSolverCallBack? = null
) : LinearSolver {
    override suspend operator fun invoke(model: LinearTriadModelView): Ret<SolverOutput> {
        val impl = GurobiLinearSolverImpl(config, callBack)
        return impl(model)
    }
}

private class GurobiLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: GurobiLinearSolverCallBack? = null
) : GurobiSolver() {
    lateinit var grbVars: List<GRBVar>
    lateinit var grbConstraints: List<GRBConstr>
    lateinit var output: SolverOutput

    operator fun invoke(model: LinearTriadModelView): Ret<SolverOutput> {
        val gurobiConfig = if (config.extraConfig is GurobiSolverConfig) {
            config.extraConfig as GurobiSolverConfig
        } else {
            null
        }
        val server = gurobiConfig?.server
        val password = gurobiConfig?.password
        val connectionTime = gurobiConfig?.connectionTime

        val processes = arrayOf(
            {
                if (server != null && password != null && connectionTime != null) {
                    it.init(server, password, connectionTime, model.name)
                } else {
                    it.init(model.name)
                }
            },
            { it.dump(model) },
            GurobiLinearSolverImpl::configure,
            GurobiLinearSolverImpl::solve,
            GurobiLinearSolverImpl::analyzeStatus,
            GurobiLinearSolverImpl::analyzeSolution
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
        return try {
            grbVars = grbModel.addVars(
                model.variables.map { it.lowerBound.toDouble() }.toDoubleArray(),
                model.variables.map { it.upperBound.toDouble() }.toDoubleArray(),
                null,
                model.variables.map { GurobiVariable(it.type).toGurobiVar() }.toCharArray(),
                model.variables.map { it.name }.toTypedArray(),
                0,
                model.variables.size
            ).toList()

            for ((col, variable) in model.variables.withIndex()) {
                variable.initialResult?.let {
                    grbVars[col].set(GRB.DoubleAttr.Start, it.toDouble())
                }
            }

            var i = 0
            var j = 0
            val constraints = ArrayList<GRBConstr>()
            while (i != model.constraints.size) {
                val lhs = GRBLinExpr()
                while (j != model.constraints.lhs.size && i == model.constraints.lhs[j].rowIndex) {
                    val cell = model.constraints.lhs[j]
                    lhs.addTerm(cell.coefficient.toDouble(), grbVars[cell.colIndex])
                    ++j
                }
                constraints.add(
                    grbModel.addConstr(
                        lhs,
                        GurobiConstraintSign(model.constraints.signs[i]).toGurobiConstraintSign(),
                        model.constraints.rhs[i].toDouble(),
                        model.constraints.names[i]
                    )
                )
                ++i
            }
            grbConstraints = constraints

            val obj = GRBLinExpr()
            for (cell in model.objective.obj) {
                obj.addTerm(cell.coefficient.toDouble(), grbVars[cell.colIndex])
            }
            grbModel.setObjective(
                obj, when (model.objective.category) {
                    ObjectCategory.Minimum -> {
                        GRB.MINIMIZE
                    }

                    ObjectCategory.Maximum -> {
                        GRB.MAXIMIZE
                    }
                }
            )

            when (val result = callBack?.execIfContain(Point.AfterModeling, grbModel, grbVars, grbConstraints)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            ok
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineModelingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineModelingException))
        }
    }

    private fun configure(): Try {
        return try {
            grbModel.set(GRB.DoubleParam.TimeLimit, config.time.toDouble(DurationUnit.SECONDS))
            grbModel.set(GRB.DoubleParam.MIPGap, config.gap.toDouble())

            when (val result = callBack?.execIfContain(Point.Configuration, grbModel, grbVars, grbConstraints)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            ok
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineModelingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineModelingException))
        }
    }

    private fun analyzeSolution(): Try {
        return try {
            if (status.succeeded()) {
                val results = ArrayList<Flt64>()
                for (grbVar in grbVars) {
                    results.add(Flt64(grbVar.get(GRB.DoubleAttr.X)))
                }
                output = SolverOutput(
                    Flt64(grbModel.get(GRB.DoubleAttr.ObjVal)),
                    results,
                    grbModel.get(GRB.DoubleAttr.Runtime).toLong().milliseconds,
                    Flt64(if (grbModel.get(GRB.IntAttr.IsMIP) != 0) {
                        grbModel.get(GRB.DoubleAttr.ObjBound)
                    } else {
                        grbModel.get(GRB.DoubleAttr.ObjVal)
                    }),
                    Flt64(
                        if (grbModel.get(GRB.IntAttr.IsMIP) != 0) {
                            grbModel.get(GRB.DoubleAttr.MIPGap)
                        } else {
                            0.0
                        }
                    )
                )
                when (val result =
                    callBack?.execIfContain(Point.AnalyzingSolution, grbModel, grbVars, grbConstraints)) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    else -> {}
                }
                ok
            } else {
                when (val result = callBack?.execIfContain(Point.AfterFailure, grbModel, grbVars, grbConstraints)) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    else -> {}
                }
                Failed(Err(status.errCode()!!))
            }
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineSolvingException))
        }
    }
}
