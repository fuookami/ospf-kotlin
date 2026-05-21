@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.gurobi

import fuookami.ospf.kotlin.core.solver.environmentLost
import fuookami.ospf.kotlin.core.solver.executeCreatingEnvironmentCallback
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.solvingException
import fuookami.ospf.kotlin.core.solver.terminated
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import gurobi.GRB
import gurobi.GRBEnv
import gurobi.GRBException
import gurobi.GRBModel
import kotlin.time.Duration
import kotlin.time.DurationUnit

abstract class GurobiSolver : AutoCloseable {
    protected lateinit var env: GRBEnv
    protected lateinit var grbModel: GRBModel
    protected lateinit var status: SolverStatus

    override fun close() {
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
            when (val callbackResult = executeCreatingEnvironmentCallback(env, callBack)) {
                is Failed -> return callbackResult
                is Fatal -> return callbackResult
                else -> {}
            }
            env.start()

            grbModel = GRBModel(env)
            grbModel.set(GRB.StringAttr.ModelName, name)
            ok
        } catch (e: GRBException) {
            environmentLost(e.message)
        } catch (e: Exception) {
            environmentLost()
        }
    }

    protected suspend fun init(
        name: String,
        callBack: CreatingEnvironmentFunction? = null
    ): Try {
        return try {
            env = GRBEnv()
            when (val callbackResult = executeCreatingEnvironmentCallback(env, callBack)) {
                is Failed -> return callbackResult
                is Fatal -> return callbackResult
                else -> {}
            }
            grbModel = GRBModel(env)
            grbModel.set(GRB.StringAttr.ModelName, name)
            ok
        } catch (e: GRBException) {
            environmentLost(e.message)
        } catch (e: Exception) {
            environmentLost()
        }
    }

    protected suspend fun solve(): Try {
        return try {
            grbModel.optimize()
            ok
        } catch (e: GRBException) {
            solvingException(e.message)
        } catch (e: Exception) {
            terminated()
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
                    SolverStatus.InfeasibleOrUnbounded
                }

                else -> {
                    if (grbModel.get(GRB.IntAttr.SolCount) > 0) {
                        SolverStatus.Feasible
                    } else {
                        SolverStatus.SolvingException
                    }
                }
            }
            ok
        } catch (e: GRBException) {
            solvingException(e.message)
        } catch (e: Exception) {
            solvingException()
        }
    }
}
