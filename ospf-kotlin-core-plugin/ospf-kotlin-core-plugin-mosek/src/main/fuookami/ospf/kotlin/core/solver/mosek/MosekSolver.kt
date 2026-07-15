/** MOSEK 求解器基类 / MOSEK solver base */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.mosek

import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.solverSolvingException
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import mosek.*

/** MOSEK 求解器抽象基类，提供环境初始化和状态分析的通用实现 / MOSEK solver abstract base class, provides common implementation for environment initialization and status analysis */
abstract class MosekSolver : AutoCloseable {
    protected lateinit var env: Env
    protected lateinit var mosekModel: Task
    protected lateinit var status: SolverStatus

    /** 关闭 MOSEK 任务和环境，释放资源 / Close MOSEK task and environment, release resources */
    override fun close() {
        mosekModel.close()
        env.close()
    }

    /**
     * 初始化 MOSEK 求解器（尚未实现）/ Initialize MOSEK solver (not implemented yet)
     *
     * @param name 模型名称 / model name
     * @param callBack 创建环境回调函数 / creating environment callback function
     * @return 操作结果 / operation result
    */
    protected suspend fun init(
        name: String,
        callBack: CreatingEnvironmentFunction? = null
    ): Try {
        return Failed(
            Err(
                ErrorCode.OREngineEnvironmentLost,
                "MOSEK environment initialization is not implemented yet. / MOSEK 环境初始化尚未实现。"
            )
        )
    }

    /**
     * 执行 MOSEK 求解（尚未实现）
     * Execute MOSEK solving (not implemented yet)
     *
     * @return the solve result as Try / 以Try包装的求解结果
    */
    protected suspend fun solve(): Try {
        return Failed(
            Err(
                ErrorCode.OREngineSolvingException,
                "MOSEK solving flow is not implemented yet. / MOSEK 求解流程尚未实现。"
            )
        )
    }

    /**
     * 分析 MOSEK 求解状态
     * Analyze MOSEK solving status
     *
     * @return the status analysis result as Try / 以Try包装的状态分析结果
    */
    protected suspend fun analyzeStatus(): Try {
        return try {
            status = when (val result = mosekModel.getsolsta(soltype.bas)) {
                solsta.optimal -> {
                    SolverStatus.Optimal
                }

                solsta.prim_and_dual_feas -> {
                    SolverStatus.Feasible
                }

                solsta.dual_infeas_cer -> {
                    SolverStatus.Unbounded
                }

                solsta.prim_infeas_cer -> {
                    SolverStatus.Infeasible
                }

                else -> {
                    SolverStatus.SolvingException
                }
            }

            ok
        } catch (e: Exception) {
            solverSolvingException(e.message)
        } catch (e: java.lang.Exception) {
            solverSolvingException()
        }
    }
}
