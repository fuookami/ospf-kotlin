package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Try

/**
 * `core.solver` 的插件支持 API：状态归一与失败兜底。
 * Plugin support APIs in `core.solver` for status normalization and failure fallback.
 *
 * 目标：统一 callback 失败中止语义与状态到错误码映射语义，减少插件重复实现。
 * Goal: unify callback-abort semantics and status-to-error-code mapping to reduce duplicated plugin logic.
 *
 * 非目标：不替代 solver 原生状态机，也不判断业务可恢复性。
 * Non-goal: does not replace native solver state machines or decide business-level recoverability.
 */

/**
 * 统一 callback 失败结果处理，命中失败分支时执行 abort 并返回 `true`。
 * Unified callback failure handling; executes abort and returns `true` on failure branches.
 */
inline fun shouldAbortOnCallbackFailure(
    callbackResult: Try?,
    abort: () -> Unit
): Boolean {
    return when (callbackResult) {
        is Failed, is Fatal -> {
            abort()
            true
        }

        else -> {
            false
        }
    }
}

/**
 * 将 `SolverStatus` 映射到错误码，并在缺失时返回兜底值。
 * Map `SolverStatus` to error code and return fallback when status error code is absent.
 */
fun SolverStatus.resolveErrCode(
    fallback: ErrorCode = ErrorCode.OREngineSolvingException
): ErrorCode {
    return errCode ?: fallback
}

/**
 * 基于 `SolverStatus` 构造统一失败结果。
 * Build a unified failed result from `SolverStatus`.
 */
fun failByStatus(
    status: SolverStatus,
    fallback: ErrorCode = ErrorCode.OREngineSolvingException
): Try {
    return Failed(Err(status.resolveErrCode(fallback)))
}

