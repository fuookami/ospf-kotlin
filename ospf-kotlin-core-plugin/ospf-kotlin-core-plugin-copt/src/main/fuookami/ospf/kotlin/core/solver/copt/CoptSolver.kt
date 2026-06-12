/** COPT 求解器基类 / COPT solver base */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.copt

import kotlin.time.Duration
import kotlin.time.DurationUnit
import copt.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.output.SolverStatus

/** COPT 求解器抽象基类，提供环境初始化、求解和状态分析的通用实现 / COPT solver abstract base class, provides common implementation for environment initialization, solving, and status analysis */
abstract class CoptSolver : AutoCloseable {
    protected lateinit var env: Envr
    protected lateinit var coptModel: Model
    protected lateinit var status: SolverStatus

    /** 关闭 COPT 模型和环境，释放资源 / Close COPT model and environment, release resources */
    override fun close() {
        coptModel.dispose()
        env.dispose()
    }

    /**
     * 使用远程服务器初始化 COPT 环境和模型 / Initialize COPT environment and model using remote server
     *
     * @param server 服务器地址 / server address
     * @param port 服务器端口 / server port
     * @param password 服务器密码 / server password
     * @param connectionTime 连接超时时间 / connection timeout duration
     * @param name 模型名称 / model name
     * @param callBack 创建环境回调函数 / creating environment callback function
     * @return 操作结果 / operation result
     */
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

    /**
     * 使用本地环境初始化 COPT 模型 / Initialize COPT model using local environment
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

    /** 执行 COPT 求解 / Execute COPT solving */
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

    /** 分析 COPT 求解状态 / Analyze COPT solving status */
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


