@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.copt

import copt.*
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlin.time.Duration
import kotlin.time.DurationUnit

private fun <T> executeCreatingEnvironmentCallback(
    target: T,
    callBack: ((T) -> Try)?
): Try {
    return when (val result = callBack?.invoke(target)) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> ok
    }
}

private fun environmentLost(message: String?): Try = Failed(Err(ErrorCode.OREngineEnvironmentLost, message))
private fun environmentLost(): Try = Failed(Err(ErrorCode.OREngineEnvironmentLost))
private fun solvingException(message: String?): Try = Failed(Err(ErrorCode.OREngineSolvingException, message))
private fun terminated(): Try = Failed(Err(ErrorCode.OREngineTerminated))

abstract class CoptSolver : AutoCloseable {
    protected lateinit var env: Envr
    protected lateinit var coptModel: Model
    protected lateinit var status: SolverStatus

    override fun close() {
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
            when (val callbackResult = executeCreatingEnvironmentCallback(config, callBack)) {
                is Failed -> return callbackResult
                is Fatal -> return callbackResult
                else -> {}
            }
            val env = Envr(config)
            coptModel = env.createModel(name)
            ok
        } catch (e: CoptException) {
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
            val config = EnvrConfig()
            when (val callbackResult = executeCreatingEnvironmentCallback(config, callBack)) {
                is Failed -> return callbackResult
                is Fatal -> return callbackResult
                else -> {}
            }
            env = Envr(config)
            coptModel = env.createModel(name)
            ok
        } catch (e: CoptException) {
            environmentLost(e.message)
        } catch (e: Exception) {
            environmentLost()
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
            solvingException(e.message)
        } catch (e: Exception) {
            terminated()
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
                    SolverStatus.InfeasibleOrUnbounded
                }

                else -> {
                    if (coptModel.get(COPT.IntAttr.PoolSols) > 0) {
                        SolverStatus.Feasible
                    } else {
                        SolverStatus.SolvingException
                    }
                }
            }
            ok
        } catch (e: CoptException) {
            environmentLost(e.message)
        } catch (e: Exception) {
            environmentLost()
        }
    }
}


