/**
 * 测试用 Ret 断言辅助 / Ret assertion helpers for tests
*/
package fuookami.ospf.kotlin.core.plugin.onnx

import kotlin.test.fail
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 解包 Ret 结果值，失败时以断言错误终止 / Unwrap a Ret value, failing the test when the result is not Ok
 *
 * @param message 断言失败消息 / assertion failure message
 * @return 结果值 / result value
 */
fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return value ?: fail("$message: ${errorOrFail()}")
}

/**
 * 解包 Ret 结果错误 / Unwrap the error of a failed Ret
 *
 * @return 底层错误 / underlying error
 */
fun <T> Ret<T>.errorOrFail(): Error<ErrorCode> {
    val failed = this as? Failed ?: fail("result should be failed: $this")
    return failed.error
}

/**
 * 取失败结果中的结构化错误值 / Get the structured error value from a failed result
 *
 * @param T 期望的错误类型 / expected error type
 * @return 结构化错误 / structured error
 */
inline fun <reified T : Any> Ret<*>.structuredError(): T {
    val error = errorOrFail().value ?: fail("failed result carries no structured error")
    return error as? T ?: fail("expected ${T::class.simpleName}, got ${error::class.simpleName}")
}
