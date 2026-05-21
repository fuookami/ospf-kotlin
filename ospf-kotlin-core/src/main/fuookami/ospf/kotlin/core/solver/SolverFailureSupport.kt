package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * 统一创建环境回调结果处理，失败/致命分支原样透传。
 * Unified creating-environment callback result handling with passthrough on failed/fatal branches.
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

fun environmentLost(message: String? = null): Try = Failed(Err(ErrorCode.OREngineEnvironmentLost, message))

fun solvingException(message: String? = null): Try = Failed(Err(ErrorCode.OREngineSolvingException, message))

fun modelingException(message: String? = null): Try = Failed(Err(ErrorCode.OREngineModelingException, message))

fun terminated(): Try = Failed(Err(ErrorCode.OREngineTerminated))
