/** MindOPT 求解器基类 / MindOPT solver base */
package fuookami.ospf.kotlin.core.solver.mindopt

import com.alibaba.damo.mindopt.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.output.SolverStatus

/** MindOPT 求解器抽象基类，提供环境初始化、求解和状态分析的通用实现 / MindOPT solver abstract base class, provides common implementation for environment initialization, solving, and status analysis */
abstract class MindOPTSolver : AutoCloseable {
    protected lateinit var env: MDOEnv
    protected lateinit var mindoptModel: MDOModel
    protected lateinit var status: SolverStatus

    /** 关闭 MindOPT 模型和环境，释放资源 / Close MindOPT model and environment, release resources */
    override fun close() {
        mindoptModel.dispose()
        env.dispose()
    }

    /**
     * 初始化 MindOPT 求解器 / Initialize MindOPT solver
     *
     * @param name 模型名称 / model name
     * @param callBack 创建环境回调函数 / creating environment callback function
     * @return 操作结果 / operation result
     */
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

    /** 执行 MindOPT 求解 / Execute MindOPT solving */
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

    /** 分析 MindOPT 求解状态 / Analyze MindOPT solving status */
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
