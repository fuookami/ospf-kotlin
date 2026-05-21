package fuookami.ospf.kotlin.core.solver.mindopt

import com.alibaba.damo.mindopt.MDO
import com.alibaba.damo.mindopt.MDOEnv
import com.alibaba.damo.mindopt.MDOException
import com.alibaba.damo.mindopt.MDOModel
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

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
private fun solvingException(): Try = Failed(Err(ErrorCode.OREngineSolvingException))

abstract class MindOPTSolver : AutoCloseable {
    protected lateinit var env: MDOEnv
    protected lateinit var mindoptModel: MDOModel
    protected lateinit var status: SolverStatus

    override fun close() {
        mindoptModel.dispose()
        env.dispose()
    }

    protected suspend fun init(
        name: String,
        callBack: CreatingEnvironmentFunction? = null
    ): Try {
        return try {
            env = MDOEnv()
            when (val callbackResult = executeCreatingEnvironmentCallback(env, callBack)) {
                is Failed -> return callbackResult
                is Fatal -> return callbackResult
                else -> {}
            }
            mindoptModel = MDOModel(env, name)
            ok
        } catch (e: MDOException) {
            environmentLost(e.message)
        } catch (e: Exception) {
            environmentLost()
        }
    }

    protected suspend fun solve(): Try {
        return try {
            mindoptModel.optimize()
            ok
        } catch (e: MDOException) {
            solvingException(e.message)
        } catch (e: Exception) {
            solvingException()
        }
    }

    protected suspend fun analyzeStatus(): Try {
        return try {
            status = when (mindoptModel.get(MDO.IntAttr.Status)) {
                MDO.Status.OPTIMAL -> {
                    SolverStatus.Optimal
                }

                MDO.Status.INFEASIBLE -> {
                    SolverStatus.Infeasible
                }

                MDO.Status.UNBOUNDED -> {
                    SolverStatus.Unbounded
                }

                MDO.Status.INF_OR_UBD -> {
                    SolverStatus.InfeasibleOrUnbounded
                }

                else -> {
                    if (mindoptModel.get(MDO.IntAttr.SolCount) > 0) {
                        SolverStatus.Feasible
                    } else {
                        SolverStatus.SolvingException
                    }
                }
            }
            ok
        } catch (e: MDOException) {
            solvingException(e.message)
        } catch (e: Exception) {
            solvingException()
        }
    }
}
