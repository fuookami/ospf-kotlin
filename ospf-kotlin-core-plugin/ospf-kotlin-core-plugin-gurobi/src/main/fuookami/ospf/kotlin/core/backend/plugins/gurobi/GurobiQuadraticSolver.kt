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

class GurobiQuadraticSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: GurobiQuadraticSolverCallBack? = null
) : QuadraticSolver {
    override suspend fun invoke(model: QuadraticTetradModelView): Ret<SolverOutput> {
        val impl = GurobiQuadraticSolverImpl(config, callBack)
        return impl(model)
    }
}

private class GurobiQuadraticSolverImpl(
    private val config: SolverConfig,
    private val callBack: GurobiQuadraticSolverCallBack? = null
) : GurobiSolver() {
    lateinit var grbVars: List<GRBVar>
    lateinit var grbConstraints: List<GRBQConstr>
    lateinit var output: SolverOutput

    operator fun invoke(model: QuadraticTetradModelView): Ret<SolverOutput> {
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
            GurobiQuadraticSolverImpl::configure,
            GurobiQuadraticSolverImpl::solve,
            GurobiQuadraticSolverImpl::analyzeStatus,
            GurobiQuadraticSolverImpl::analyzeSolution
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

    private fun dump(model: QuadraticTetradModelView): Try {
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
            val constraints = ArrayList<GRBQConstr>()
            while (i != model.constraints.size) {
                val lhs = GRBQuadExpr()
                while (j != model.constraints.lhs.size && i == model.constraints.lhs[j].rowIndex) {
                    val cell = model.constraints.lhs[j]
                    if (cell.colIndex2 == null) {
                        lhs.addTerm(cell.coefficient.toDouble(), grbVars[cell.colIndex1])
                    } else {
                        lhs.addTerm(cell.coefficient.toDouble(), grbVars[cell.colIndex1], grbVars[cell.colIndex2!!])
                    }
                    ++j
                }
                constraints.add(
                    grbModel.addQConstr(
                        lhs,
                        GurobiConstraintSign(model.constraints.signs[i]).toGurobiConstraintSign(),
                        model.constraints.rhs[i].toDouble(),
                        model.constraints.names[i]
                    )
                )
                ++i
            }
            grbConstraints = constraints

            val obj = GRBQuadExpr()
            for (cell in model.objective.obj) {
                if (cell.colIndex2 == null) {
                    obj.addTerm(cell.coefficient.toDouble(), grbVars[cell.colIndex1])
                } else {
                    obj.addTerm(cell.coefficient.toDouble(), grbVars[cell.colIndex1], grbVars[cell.colIndex2!!])
                }
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
            grbModel.set(GRB.IntParam.NonConvex, 2)

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
