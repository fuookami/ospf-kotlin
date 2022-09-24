package fuookami.ospf.kotlin.core.backend.plugins.gurobi

import kotlin.time.*
import kotlin.time.Duration.Companion.milliseconds
import gurobi.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class GurobiLinearSolver(
    private val config: LinearSolverConfig
) {
    operator fun invoke(model: LinearTriadModel): Result<LinearSolverOutput, Err> {
        val impl = GurobiLinearSolverImpl(config)
        return impl(model)
    }
}

private class GurobiLinearSolverImpl(
    private val config: LinearSolverConfig
) {
    lateinit var env: GRBEnv
    lateinit var grbModel: GRBModel
    lateinit var grbVars: Array<GRBVar>
    lateinit var status: SolvingStatus
    lateinit var output: LinearSolverOutput

    protected fun finalize() {
        grbModel.dispose();
        env.dispose();
    }

    operator fun invoke(model: LinearTriadModel): Result<LinearSolverOutput, Err> {
        assert(!this::env.isInitialized)

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
                    it.init(server, password, connectionTime)
                } else {
                    it.init()
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

    private fun init(server: String, password: String, connectionTime: Duration): Try<Err> {
        return try {
            env = GRBEnv(true)
            env.set(GRB.IntParam.ServerTimeout, connectionTime.toInt(DurationUnit.SECONDS))
            env.set(GRB.DoubleParam.CSQueueTimeout, connectionTime.toDouble(DurationUnit.SECONDS))
            env.set(GRB.StringParam.ComputeServer, server)
            env.set(GRB.StringParam.ServerPassword, password)
            env.start()

            grbModel = GRBModel(env)
            Ok(success)
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost))
        }
    }

    private fun init(): Try<Err> {
        return try {
            env = GRBEnv()
            grbModel = GRBModel(env)
            Ok(success)
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost))
        }
    }

    private fun dump(model: fuookami.ospf.kotlin.core.backend.intermediate_model.LinearTriadModel): Try<Err> {
        return try {
            grbVars = grbModel.addVars(
                model.variables.map { it.lowerBound.toDouble() }.toDoubleArray(),
                model.variables.map { it.upperBound.toDouble() }.toDoubleArray(),
                null,
                model.variables.map { GurobiVariable(it.type).toGurobiVar() }.toCharArray(),
                model.variables.map { it.name }.toTypedArray(),
                0,
                model.variables.size
            )

            var i = 0
            var j = 0
            while (i != model.constraints.size) {
                val lhs = GRBLinExpr()
                while (j != model.constraints.lhs.size && i == model.constraints.lhs[j].rowIndex) {
                    val cell = model.constraints.lhs[j]
                    lhs.addTerm(cell.coefficient.toDouble(), grbVars[cell.colIndex])
                    ++j
                }
                grbModel.addConstr(
                    lhs, GurobiConstraintSign(model.constraints.signs[i]).toGurobiConstraintSign(),
                    model.constraints.rhs[i].toDouble(), model.constraints.names[i]
                )
                ++i
            }

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

            Ok(success)
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineModelingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineModelingException))
        }
    }

    private fun configure(): Try<Err> {
        return try {
            grbModel.set(GRB.DoubleParam.TimeLimit, config.time.toDouble(DurationUnit.SECONDS))
            grbModel.set(GRB.DoubleParam.MIPGap, config.gap.toDouble())

            Ok(success)
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineModelingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineModelingException))
        }
    }

    private fun solve(): Try<Err> {
        return try {
            grbModel.optimize()

            Ok(success)
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineTerminated))
        }
    }

    private fun analyzeStatus(): Try<Err> {
        return try {
            status = when (grbModel.get(GRB.IntAttr.Status)) {
                GRB.OPTIMAL -> {
                    SolvingStatus.Optimal
                }

                GRB.INFEASIBLE -> {
                    SolvingStatus.NoSolution
                }

                GRB.UNBOUNDED -> {
                    SolvingStatus.Unbounded
                }

                GRB.INF_OR_UNBD -> {
                    SolvingStatus.NoSolution
                }

                else -> {
                    if (grbModel.get(GRB.IntAttr.SolCount) > 0) {
                        SolvingStatus.Feasible
                    } else {
                        SolvingStatus.NoSolution
                    }
                }
            }

            Ok(success)
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineSolvingException))
        }
    }

    private fun analyzeSolution(): Try<Err> {
        return try {
            if (status.succeeded()) {
                output = LinearSolverOutput(
                    Flt64(grbModel.get(GRB.DoubleAttr.Obj)),
                    grbVars.map { Flt64(grbModel.get(GRB.DoubleAttr.X)) },
                    (grbModel.get(GRB.DoubleAttr.Runtime) * 1000.0).toLong().milliseconds,
                    Flt64(grbModel.get(GRB.DoubleAttr.ObjBound)),
                    Flt64(
                        if (grbModel.get(GRB.IntAttr.IsMIP) != 0) {
                            grbModel.get(GRB.DoubleAttr.MIPGap)
                        } else {
                            0.0
                        }
                    )
                )
            } else {
                // to do
            }

            Ok(success)
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineSolvingException))
        }
    }
}
