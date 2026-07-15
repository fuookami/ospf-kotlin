/**
 * Hexaly solver base
 * Hexaly 求解器基类
*/
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.hexaly

import kotlin.time.*
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.utils.functional.*
import com.hexaly.optimizer.*

/**
 * Hexaly solver abstract base class, provides common implementation for environment initialization, solving, and status analysis
 * Hexaly 求解器抽象基类，提供环境初始化、求解和状态分析的通用实现
*/
@OptIn(ExperimentalTime::class)
abstract class HexalySolver : AutoCloseable {
    protected lateinit var optimizer: HexalyOptimizer
    protected lateinit var hexalyModel: HxModel
    protected lateinit var hexalySolution: HxSolution
    protected lateinit var status: SolverStatus
    protected var beginTime: Instant? = null
    protected var solvingTime: Duration? = null

    /**
     * Close Hexaly model and optimizer, release resources
     * 关闭 Hexaly 模型和优化器，释放资源
    */
    override fun close() {
        hexalyModel.close()
        optimizer.close()
    }

    /**
     * Initialize Hexaly optimizer
     * 初始化 Hexaly 优化器
     *
     * @param name model name / 中文 模型名称
     * @param callBack creating environment callback function / 中文 创建环境回调函数
     * @return operation result / 中文 操作结果
    */
    protected suspend fun init(
        name: String,
        callBack: CreatingEnvironmentFunction? = null
    ): Try {
        return try {
            optimizer = HexalyOptimizer()
            when (val callbackResult = executeCreatingEnvironmentCallback(optimizer, callBack)) {
                is Failed -> return callbackResult
                is Fatal -> return callbackResult
                else -> {}
            }
            hexalyModel = optimizer.model

            ok
        } catch (e: HxException) {
            solverEnvironmentLost(e.message)
        } catch (e: Exception) {
            solverEnvironmentLost()
        }
    }

    /**
     * Execute Hexaly solving
     * 执行 Hexaly 求解
     *
     * @return operation result / 中文 操作结果
    */
    protected suspend fun solve(): Try {
        return try {
            beginTime = Clock.System.now()
            hexalyModel.close()
            optimizer.solve()
            solvingTime = Clock.System.now() - beginTime!!

            ok
        } catch (e: HxException) {
            solverSolvingException(e.message)
        } catch (e: Exception) {
            solverTerminated()
        }
    }

    /**
     * Analyze Hexaly solving status
     * 分析 Hexaly 求解状态
     *
     * @return operation result / 中文 操作结果
    */
    protected suspend fun analyzeStatus(): Try {
        return try {
            hexalySolution = optimizer.solution
            status = when (hexalySolution.status) {
                HxSolutionStatus.Optimal -> {
                    SolverStatus.Optimal
                }

                HxSolutionStatus.Feasible -> {
                    SolverStatus.Feasible
                }

                HxSolutionStatus.Infeasible -> {
                    SolverStatus.Infeasible
                }

                HxSolutionStatus.Inconsistent -> {
                    SolverStatus.Unbounded
                }

                else -> {
                    SolverStatus.SolvingException
                }
            }

            ok
        } catch (e: HxException) {
            solverSolvingException(e.message)
        } catch (e: Exception) {
            solverSolvingException()
        }
    }
}
