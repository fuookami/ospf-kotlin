package fuookami.ospf.kotlin.core.backend.plugins.gurobi

import kotlin.time.*
import gurobi.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

abstract class GurobiSolver {
    protected lateinit var env: GRBEnv
    protected lateinit var grbModel: GRBModel
    protected lateinit var status: SolverStatus

    protected fun finalize() {
        grbModel.dispose()
        env.dispose()
    }

    protected suspend fun init(
        server: String,
        password: String,
        connectionTime: Duration,
        name: String,
        callBack: CreatingEnvironmentFunction? = null
    ): Try {
        return try {
            env = GRBEnv(true)
            env.set(GRB.IntParam.ServerTimeout, connectionTime.toInt(DurationUnit.SECONDS))
            env.set(GRB.DoubleParam.CSQueueTimeout, connectionTime.toDouble(DurationUnit.SECONDS))
            env.set(GRB.StringParam.ComputeServer, server)
            env.set(GRB.StringParam.ServerPassword, password)
            when (val result = callBack?.invoke(env)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            env.start()

            grbModel = GRBModel(env)
            grbModel.set(GRB.StringAttr.ModelName, name)
            ok
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost))
        }
    }

    protected suspend fun init(
        name: String,
        callBack: CreatingEnvironmentFunction? = null
    ): Try {
        return try {
            env = GRBEnv()
            when (val result = callBack?.invoke(env)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            grbModel = GRBModel(env)
            grbModel.set(GRB.StringAttr.ModelName, name)
            ok
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost))
        }
    }

    protected suspend fun solve(): Try {
        return try {
            grbModel.optimize()
            ok
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineTerminated))
        }
    }

    protected suspend fun analyzeStatus(): Try {
        return try {
            status = when (grbModel.get(GRB.IntAttr.Status)) {
                GRB.OPTIMAL -> {
                    SolverStatus.Optimal
                }

                GRB.INFEASIBLE -> {
                    SolverStatus.Infeasible
                }

                GRB.UNBOUNDED -> {
                    SolverStatus.Unbounded
                }

                GRB.INF_OR_UNBD -> {
                    SolverStatus.Infeasible
                }

                else -> {
                    if (grbModel.get(GRB.IntAttr.SolCount) > 0) {
                        SolverStatus.Feasible
                    } else {
                        SolverStatus.Infeasible
                    }
                }
            }
            ok
        } catch (e: GRBException) {
            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineSolvingException))
        }
    }
}
