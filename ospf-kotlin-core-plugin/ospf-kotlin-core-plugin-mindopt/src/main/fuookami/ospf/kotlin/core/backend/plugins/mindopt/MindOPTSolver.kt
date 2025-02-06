package fuookami.ospf.kotlin.core.backend.plugins.mindopt

import com.alibaba.damo.mindopt.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

abstract class MindOPTSolver {
    protected lateinit var env: MDOEnv
    protected lateinit var mindoptModel: MDOModel
    protected lateinit var status: SolverStatus

    protected fun finalize() {
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
                    SolverStatus.SolvingException
                }

                else -> {
                    if (mindoptModel.get(MDO.IntAttr.SolCount) > 0) {
                        SolverStatus.Feasible
                    } else {
                        SolverStatus.Infeasible
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
