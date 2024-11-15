package fuookami.ospf.kotlin.core.backend.plugins.copt

import kotlin.time.*
import copt.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

abstract class CoptSolver {
    protected lateinit var env: Envr
    protected lateinit var coptModel: Model
    protected lateinit var status: SolverStatus

    protected fun finalize() {
        coptModel.dispose()
        env.dispose()
    }

    protected suspend fun init(
        server: String,
        port: UInt64,
        password: String,
        connectionTime: Duration,
        name: String,
        callBack: CreatingEnvironmentFunction? = null
    ): Try {
        return try {
            val config = EnvrConfig()
            config.set(COPT.Client.Cluster, server)
            config.set(COPT.Client.Port, port.toString())
            config.set(COPT.Client.Password, password)
            config.set(COPT.Client.WaitTime, connectionTime.toInt(DurationUnit.SECONDS).toString())
            when (val result = callBack?.invoke(config)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            val env = Envr(config)
            coptModel = env.createModel(name)
            ok
        } catch (e: CoptException) {
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
            val config = EnvrConfig()
            when (val result = callBack?.invoke(config)) {
                is Failed -> {
                    return Failed(result.error)
                }

                else -> {}
            }
            env = Envr(config)
            coptModel = env.createModel(name)
            ok
        } catch (e: CoptException) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost))
        }
    }

    protected suspend fun solve(): Try {
        return try {
            if (coptModel.get(COPT.IntAttr.IsMIP) != 0) {
                coptModel.solve()
            } else {
                coptModel.solveLp()
            }
            ok
        } catch (e: CoptException) {
            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineTerminated))
        }
    }

    protected suspend fun analyzeStatus(): Try {
        return try {
            status = when (if (coptModel.get(COPT.IntAttr.IsMIP) != 0) {
                coptModel.get(COPT.IntAttr.MipStatus)
            } else {
                coptModel.get(COPT.IntAttr.LpStatus)
            }) {
                COPT.Status.Optimal.value -> {
                    SolverStatus.Optimal
                }

                COPT.Status.Infeasible.value -> {
                    SolverStatus.Infeasible
                }

                COPT.Status.Unbounded.value -> {
                    SolverStatus.Unbounded
                }

                COPT.Status.InfeasibleOrUnbounded.value -> {
                    SolverStatus.Infeasible
                }

                else -> {
                    if (coptModel.get(COPT.IntAttr.PoolSols) > 0) {
                        SolverStatus.Feasible
                    } else {
                        SolverStatus.Infeasible
                    }
                }
            }
            ok
        } catch (e: CoptException) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost))
        }
    }
}
