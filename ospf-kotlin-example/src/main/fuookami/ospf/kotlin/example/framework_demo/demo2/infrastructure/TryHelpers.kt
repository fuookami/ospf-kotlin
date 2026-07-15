package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Execute a Try-returning operation, returning early on failure, reducing repetitive when/Ok/Failed/Fatal boilerplate.
 * 执行返回 Try 的操作，失败时提前返回，减少重复的 when/Ok/Failed/Fatal 样板代码。
 *
 * @param failedHandler Handler invoked on Failed result, which must never return. / Failed 结果时调用的处理器，必须永不返回
 * @param fatalHandler Handler invoked on Fatal result, which must never return. / Fatal 结果时调用的处理器，必须永不返回
*/
inline fun Try.orReturn(failedHandler: (Error<ErrorCode>) -> Nothing, fatalHandler: (List<Error<ErrorCode>>) -> Nothing) {
    when (this) {
        is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}
        is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> failedHandler(error)
        is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> fatalHandler(errors)
    }
}

/**
 * Execute a Ret<T>-returning operation, returning the value or throwing on failure.
 * 执行返回 Ret<T> 的操作，返回值或在失败时抛出异常。
 *
 * @param T The value type contained in the Ret result. / Ret 结果中包含的值类型
 * @param failedHandler Handler invoked on Failed result, which must never return. / Failed 结果时调用的处理器，必须永不返回
 * @param fatalHandler Handler invoked on Fatal result, which must never return. / Fatal 结果时调用的处理器，必须永不返回
 * @return The unwrapped value on success. / 成功时解包的值
*/
inline fun <T> Ret<T>.orReturn(
    failedHandler: (Error<ErrorCode>) -> Nothing,
    fatalHandler: (List<Error<ErrorCode>>) -> Nothing
): T {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> value as T
        is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> failedHandler(error)
        is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> fatalHandler(errors)
    }
}
