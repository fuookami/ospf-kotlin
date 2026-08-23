/** Gurobi 求解器基类 / Gurobi solver base */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.time.Duration
import kotlin.time.DurationUnit
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.report.CancellationToken
import fuookami.ospf.kotlin.core.solver.report.CancellationRecord
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import gurobi.*

/** Resolve the native Gurobi version without exposing license or environment details. /
 * 在不暴露许可证和环境细节的前提下解析 Gurobi 原生版本。 */
internal fun gurobiNativeVersion(): String? {
    return runCatching {
        val major = GRB::class.java.getField("VERSION_MAJOR").getInt(null)
        val minor = GRB::class.java.getField("VERSION_MINOR").getInt(null)
        val technical = GRB::class.java.getField("VERSION_TECHNICAL").getInt(null)
        "$major.$minor.$technical"
    }.getOrNull()
}

/** Map a completed Gurobi status before applying a late cancellation request. / 将已完成的 Gurobi 状态优先于迟到的取消请求映射。 */
internal fun gurobiTerminationReason(
    nativeStatus: Int,
    cancellationRequested: Boolean
): TerminationReason {
    return when {
        nativeStatus == GRB.OPTIMAL || nativeStatus == GRB.INFEASIBLE ||
            nativeStatus == GRB.UNBOUNDED || nativeStatus == GRB.INF_OR_UNBD -> TerminationReason.Completed
        cancellationRequested -> TerminationReason.Cancelled
        nativeStatus == GRB.TIME_LIMIT -> TerminationReason.TimeLimit
        nativeStatus == GRB.NODE_LIMIT -> TerminationReason.NodeLimit
        nativeStatus == GRB.ITERATION_LIMIT -> TerminationReason.IterationLimit
        nativeStatus == GRB.SOLUTION_LIMIT -> TerminationReason.SolutionLimit
        nativeStatus == GRB.USER_OBJ_LIMIT -> TerminationReason.ObjectiveLimit
        nativeStatus == GRB.INTERRUPTED -> TerminationReason.Interrupted
        nativeStatus == GRB.NUMERIC -> TerminationReason.NumericalFailure
        else -> TerminationReason.BackendFailure
    }
}

/** Gurobi 求解器抽象基类，提供环境初始化、求解和状态分析的通用实现 / Gurobi solver abstract base class, provides common implementation for environment initialization, solving, and status analysis */
abstract class GurobiSolver : AutoCloseable {
    protected lateinit var env: GRBEnv
    protected lateinit var grbModel: GRBModel
    protected lateinit var status: SolverStatus
    protected var terminationReason: TerminationReason = TerminationReason.Completed
    private var cancellationToken: CancellationToken? = null
    private var cancellationListener: ((CancellationRecord) -> Try)? = null

    /** Read native iteration/node counters when the current binding exposes them. / 读取当前绑定提供的原生迭代和节点计数。 */
    protected fun nativeIterationsOrNull(): ULong? = runCatching {
        grbModel.get(GRB.DoubleAttr.IterCount).toULong()
    }.getOrNull()

    protected fun nativeNodesOrNull(): ULong? = runCatching {
        grbModel.get(GRB.DoubleAttr.NodeCount).toULong()
    }.getOrNull()

    /** 关闭 Gurobi 模型和环境，释放资源 / Close Gurobi model and environment, release resources */
    override fun close() {
        cancellationListener?.let { listener ->
            cancellationToken?.unregister(listener)
        }
        cancellationListener = null
        cancellationToken = null
        if (::grbModel.isInitialized) {
            grbModel.dispose()
        }
        if (::env.isInitialized) {
            env.dispose()
        }
    }

    /**
     * Register direct native termination for a solve token. /
     * 为求解令牌注册直接原生终止监听。
     *
     * Reflection keeps this adapter compatible with the Gurobi Java bindings whose
     * terminate method was added across supported native versions. /
     * 通过反射兼容受支持原生版本中 terminate 方法的绑定差异。
     *
     * @param token 求解取消令牌 / Solve cancellation token
     * @return 注册结果 / Registration result
     */
    protected fun registerCancellation(token: CancellationToken?): Try {
        if (token == null) {
            return ok
        }
        cancellationToken?.let { previousToken ->
            cancellationListener?.let { listener -> previousToken.unregister(listener) }
        }
        val listener: (CancellationRecord) -> Try = {
            try {
                grbModel.javaClass.getMethod("terminate").invoke(grbModel)
                ok
            } catch (error: Exception) {
                Failed(
                    ErrorCode.OREngineTerminated,
                    "Gurobi 原生终止失败：${error.message ?: error::class.simpleName} / " +
                        "Gurobi native termination failed: ${error.message ?: error::class.simpleName}"
                )
            }
        }
        cancellationToken = token
        cancellationListener = listener
        return token.register(listener)
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
            suppressIntegrationOutput(env)
            env.set(GRB.IntParam.ServerTimeout, connectionTime.toInt(DurationUnit.SECONDS))
            env.set(GRB.DoubleParam.CSQueueTimeout, connectionTime.toDouble(DurationUnit.SECONDS))
            env.set(GRB.StringParam.ComputeServer, server)
            env.set(GRB.StringParam.ServerPassword, password)
            when (val callbackResult = executeCreatingEnvironmentCallback(env, callBack?.let { it::invoke })) {
                is Failed -> return callbackResult
                is Fatal -> return callbackResult
                else -> {}
            }
            env.start()

            grbModel = GRBModel(env)
            grbModel.set(GRB.StringAttr.ModelName, name)
            ok
        } catch (e: GRBException) {
            solverEnvironmentLost(e.message)
        } catch (e: Exception) {
            solverEnvironmentLost()
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
            suppressIntegrationOutput(env)
            when (val callbackResult = executeCreatingEnvironmentCallback(env, callBack?.let { it::invoke })) {
                is Failed -> return callbackResult
                is Fatal -> return callbackResult
                else -> {}
            }
            grbModel = GRBModel(env)
            grbModel.set(GRB.StringAttr.ModelName, name)
            ok
        } catch (e: GRBException) {
            solverEnvironmentLost(e.message)
        } catch (e: Exception) {
            solverEnvironmentLost()
        }
    }

    /** Keep native Gurobi logs out of Failsafe's fork protocol during integration tests. */
    private fun suppressIntegrationOutput(environment: GRBEnv) {
        if (System.getProperty("ospf.gurobi.suppressOutput") == "true") {
            environment.set(GRB.IntParam.OutputFlag, 0)
        }
    }

    /**
     * 执行 Gurobi 求解 / Execute Gurobi solving
     *
     * @return 操作结果 / operation result
    */
    protected suspend fun solve(): Try {
        return try {
            grbModel.optimize()
            ok
        } catch (e: GRBException) {
            solverSolvingException(e.message)
        } catch (e: Exception) {
            solverTerminated()
        }
    }

    /**
     * 分析 Gurobi 求解状态 / Analyze Gurobi solving status
     *
     * @return 操作结果 / operation result
    */
    protected suspend fun analyzeStatus(cancellationToken: CancellationToken? = null): Try {
        return try {
            val nativeStatus = grbModel.get(GRB.IntAttr.Status)
            val hasSolution = grbModel.get(GRB.IntAttr.SolCount) > 0
            terminationReason = gurobiTerminationReason(
                nativeStatus = nativeStatus,
                cancellationRequested = cancellationToken?.isCancellationRequested == true
            )
            status = when (nativeStatus) {
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
                    if (hasSolution) {
                        SolverStatus.Feasible
                    } else {
                        SolverStatus.SolvingException
                    }
                }
            }
            ok
        } catch (e: GRBException) {
            solverSolvingException(e.message)
        } catch (e: Exception) {
            solverSolvingException()
        }
    }
}
