package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Try

/**
 * 统一 callback 失败结果处理，命中失败时执行 abort 并返回 true。
 * Unified callback failure handling; abort on failure branches and return true.
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
 * 统一求解失败时的错误码兜底。
 * Unified fallback error code for solver failure paths.
 */
fun SolverStatus.resolveErrCode(
    fallback: ErrorCode = ErrorCode.OREngineSolvingException
): ErrorCode {
    return errCode ?: fallback
}

