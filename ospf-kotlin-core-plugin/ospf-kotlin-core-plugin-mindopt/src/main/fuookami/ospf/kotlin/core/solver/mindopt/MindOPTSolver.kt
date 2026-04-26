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
            when (val result = callBack?.invoke(env)) {
                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }

                else -> {}
            }
            mindoptModel = MDOModel(env, name)
            ok
        } catch (e: MDOException) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineEnvironmentLost))
        }
    }

    protected suspend fun solve(): Try {
        return try {
            mindoptModel.optimize()
            ok
        } catch (e: MDOException) {
            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineSolvingException))
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
            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineSolvingException))
        }
    }
}
