package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

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
 * 构造“求解环境丢失”失败结果。
 * Build a failure result for lost solver environment.
 */
fun environmentLost(message: String? = null): Try = Failed(Err(ErrorCode.OREngineEnvironmentLost, message))

/**
 * 构造“求解阶段异常”失败结果。
 * Build a failure result for solving-stage exception.
 */
fun solvingException(message: String? = null): Try = Failed(Err(ErrorCode.OREngineSolvingException, message))

/**
 * 构造“建模阶段异常”失败结果。
 * Build a failure result for modeling-stage exception.
 */
fun modelingException(message: String? = null): Try = Failed(Err(ErrorCode.OREngineModelingException, message))

/**
 * 构造“外部终止”失败结果。
 * Build a failure result for externally terminated solving.
 */
fun terminated(): Try = Failed(Err(ErrorCode.OREngineTerminated))
