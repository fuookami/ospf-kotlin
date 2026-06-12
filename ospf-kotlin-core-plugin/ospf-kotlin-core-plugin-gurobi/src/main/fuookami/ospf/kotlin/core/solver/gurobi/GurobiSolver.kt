/** Gurobi 求解器基类 / Gurobi solver base */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.time.Duration
import kotlin.time.DurationUnit
import gurobi.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.output.SolverStatus

/** Gurobi 求解器抽象基类，提供环境初始化、求解和状态分析的通用实现 / Gurobi solver abstract base class, provides common implementation for environment initialization, solving, and status analysis */
abstract class GurobiSolver : AutoCloseable {
    protected lateinit var env: GRBEnv
    protected lateinit var grbModel: GRBModel
    protected lateinit var status: SolverStatus

    /** 关闭 Gurobi 模型和环境，释放资源 / Close Gurobi model and environment, release resources */
    override fun close() {
        grbModel.dispose()
        env.dispose()
    }

    /**
     * 使用远程服务器初始化 Gurobi 环境和模型 / Initialize Gurobi environment and model using remote server
     *
     * @param server 服务器地址 / server address
     * @param password 服务器密码 / server password
     * @param connectionTime 连接超时时间 / connection timeout duration
     * @param name 模型名称 / model name
     * @param callBack 创建环境回调函数 / creating environment callback function
     * @return 操作结果 / operation result
     */
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

    /**
     * 使用本地环境初始化 Gurobi 模型 / Initialize Gurobi model using local environment
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

    /**
     * 执行 Gurobi 求解 / Execute Gurobi solving
     *
     * @return 操作结果 / Operation result
     */
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

    /**
     * 分析 Gurobi 求解状态 / Analyze Gurobi solving status
     *
     * @return 操作结果 / Operation result
     */
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
