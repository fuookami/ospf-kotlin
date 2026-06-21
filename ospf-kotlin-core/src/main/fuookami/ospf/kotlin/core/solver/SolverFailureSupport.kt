/** 求解器失败支持 / Solver failure support */
package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.error.*

/**
 * `core.solver` 的插件支持 API：失败构造与回调失败透传。
 * Plugin support APIs in `core.solver` for failure construction and callback-failure passthrough.
 *
 * 目标：为跨模块 solver 插件提供统一 Try 失败口径，不绑定任何具体商业/开源 solver SDK。
 * Goal: provide a unified Try-failure contract for cross-module solver plugins without coupling to any specific solver SDK.
 *
 * 非目标：不负责求解流程编排、模型语义解释或算法策略。
 * Non-goal: does not handle solving-flow orchestration, model semantics, or algorithm strategy.
 *
 * 稳定性：按“新增优先、兼容优先”维护；除明确迁移窗口外不破坏既有调用语义。
 * Stability: maintained with additive and compatibility-first policy; existing call semantics should remain stable outside explicit migration windows.
 */

/**
 * 执行创建环境回调并统一透传失败分支。
 * Execute creating-environment callback and pass through failure branches.
 *
 * 该函数仅做结果归一，不持有环境对象生命周期。
 * This function only normalizes callback outcomes and does not own environment lifecycle.
 *
 * @param T 目标对象类型 / Target object type
 * @param target 目标对象 / Target object
 * @param callBack 创建环境回调（可选）/ Creating-environment callback (optional)
 * @return 统一结果 / Unified result
 */
fun <T> executeCreatingEnvironmentCallback(
    target: T,
    callBack: ((T) -> Try)?
): Try {
    return when (val result = callBack?.invoke(target)) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> ok
    }
}

/**
 * 构造”求解环境丢失”失败结果。
 * Build a failure result for lost solver environment.
 *
 * @param message 错误信息（可选）/ Error message (optional)
 * @return 失败结果 / Failure result
 */
fun environmentLost(message: String? = null): Try = Failed(Err(ErrorCode.OREngineEnvironmentLost, message))

/**
 * 构造”求解环境丢失”失败结果（携带 SolverError detail）。
 * Build a failure result for lost solver environment (with SolverError detail).
 *
 * @param detail 求解器错误详情 / Solver error detail
 * @return 失败结果 / Failure result
 */
fun environmentLost(detail: SolverError): Try = Failed(
    ExErr(
        code = ErrorCode.OREngineEnvironmentLost,
        message = detail.message,
        value = detail
    )
)

/**
 * 构造”求解阶段异常”失败结果。
 * Build a failure result for solving-stage exception.
 *
 * @param message 错误信息（可选）/ Error message (optional)
 * @return 失败结果 / Failure result
 */
fun solvingException(message: String? = null): Try = Failed(Err(ErrorCode.OREngineSolvingException, message))

/**
 * 构造”求解阶段异常”失败结果（携带 SolverError detail）。
 * Build a failure result for solving-stage exception (with SolverError detail).
 *
 * @param detail 求解器错误详情 / Solver error detail
 * @return 失败结果 / Failure result
 */
fun solvingException(detail: SolverError): Try = Failed(
    ExErr(
        code = ErrorCode.OREngineSolvingException,
        message = detail.message,
        value = detail
    )
)

/**
 * 构造”建模阶段异常”失败结果。
 * Build a failure result for modeling-stage exception.
 *
 * @param message 错误信息（可选）/ Error message (optional)
 * @return 失败结果 / Failure result
 */
fun modelingException(message: String? = null): Try = Failed(Err(ErrorCode.OREngineModelingException, message))

/**
 * 构造”建模阶段异常”失败结果（携带 SolverError detail）。
 * Build a failure result for modeling-stage exception (with SolverError detail).
 *
 * @param detail 求解器错误详情 / Solver error detail
 * @return 失败结果 / Failure result
 */
fun modelingException(detail: SolverError): Try = Failed(
    ExErr(
        code = ErrorCode.OREngineModelingException,
        message = detail.message,
        value = detail
    )
)

/**
 * 构造”外部终止”失败结果。
 * Build a failure result for externally terminated solving.
 *
 * @return 失败结果 / Failure result
 */
fun terminated(): Try = Failed(Err(ErrorCode.OREngineTerminated))

/**
 * 构造”外部终止”失败结果（携带 SolverError detail）。
 * Build a failure result for externally terminated solving (with SolverError detail).
 *
 * @param detail 求解器错误详情 / Solver error detail
 * @return 失败结果 / Failure result
 */
fun terminated(detail: SolverError): Try = Failed(
    ExErr(
        code = ErrorCode.OREngineTerminated,
        message = detail.message,
        value = detail
    )
)

// ============================================================================
// 命名错误子类型工厂函数
// Named error subclass factory functions
// ============================================================================

/**
 * 构造”求解器未找到”失败结果（命名子类型）。
 * Build a failure result for solver not found (named subclass).
 *
 * 调用方可通过 `when (error is SolverNotFoundError)` 稳定断言。
 * Callers can assert via `when (error is SolverNotFoundError)`.
 *
 * @param solver 尝试查找的求解器名称（可选）/ Name of the solver that was looked up (optional)
 * @return 失败结果 / Failure result
 */
fun solverNotFound(solver: String? = null): Try = Failed(SolverNotFoundError(solver))

/**
 * 构造”求解器环境丢失”失败结果（命名子类型）。
 * Build a failure result for lost solver environment (named subclass).
 *
 * 调用方可通过 `when (error is SolverEnvironmentLostError)` 稳定断言。
 * Callers can assert via `when (error is SolverEnvironmentLostError)`.
 *
 * @param detail 环境丢失的描述（可选）/ Description of the environment loss (optional)
 * @return 失败结果 / Failure result
 */
fun solverEnvironmentLost(detail: String? = null): Try = Failed(SolverEnvironmentLostError(detail))

/**
 * 构造”求解器求解异常”失败结果（命名子类型）。
 * Build a failure result for solver solving exception (named subclass).
 *
 * 调用方可通过 `when (error is SolverSolvingError)` 稳定断言。
 * Callers can assert via `when (error is SolverSolvingError)`.
 *
 * @param detail 求解异常的描述（可选）/ Description of the solving exception (optional)
 * @return 失败结果 / Failure result
 */
fun solverSolvingException(detail: String? = null): Try = Failed(SolverSolvingError(detail))

/**
 * 构造”求解器建模异常”失败结果（命名子类型）。
 * Build a failure result for solver modeling exception (named subclass).
 *
 * 调用方可通过 `when (error is SolverModelingError)` 稳定断言。
 * Callers can assert via `when (error is SolverModelingError)`.
 *
 * @param detail 建模异常的描述（可选）/ Description of the modeling exception (optional)
 * @return 失败结果 / Failure result
 */
fun solverModelingException(detail: String? = null): Try = Failed(SolverModelingError(detail))

/**
 * 构造”求解器终止”失败结果（命名子类型）。
 * Build a failure result for solver terminated (named subclass).
 *
 * 调用方可通过 `when (error is SolverTerminatedError)` 稳定断言。
 * Callers can assert via `when (error is SolverTerminatedError)`.
 *
 * @return 失败结果 / Failure result
 */
fun solverTerminated(): Try = Failed(SolverTerminatedError())
