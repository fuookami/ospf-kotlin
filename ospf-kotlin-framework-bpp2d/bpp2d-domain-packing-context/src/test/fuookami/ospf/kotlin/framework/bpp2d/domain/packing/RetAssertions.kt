package fuookami.ospf.kotlin.framework.bpp2d.domain.packing

import kotlin.test.fail
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 解包 Ret 结果，失败时抛出断言错误。 / Unwrap a Ret result, throwing an assertion error on failure.
 *
 * @param message 断言失败时的错误信息 / error message when the assertion fails
 * @return 成功时的值 / value on success
*/
fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return when (this) {
        is Ok -> value
        is Failed -> fail("$message: ${error.message}")
        is Fatal -> fail("$message: ${errors.joinToString { it.message }}")
    }
}

/**
 * 断言可空值非空，为空时抛出断言错误。 / Assert that a nullable value is not null.
 *
 * @param message 断言失败时的错误信息 / error message when the assertion fails
 * @return 非空值 / non-null value
*/
fun <T> T?.orFail(message: String = "value should not be null"): T {
    return this ?: fail(message)
}

/**
 * 取失败结果的错误消息，成功时抛出断言错误。 / Read the error message of a failure, throwing an assertion
 * error on success.
 *
 * @param message 断言失败时的错误信息 / error message when the assertion fails
 * @return 错误消息 / error message
*/
fun <T> Ret<T>.errorMessageOrFail(message: String = "result should fail"): String {
    return when (this) {
        is Ok -> fail("$message: but the result succeeded with ${value}")
        is Failed -> error.message
        is Fatal -> errors.joinToString { it.message }
    }
}

/**
 * 断言 Try 结果失败，成功时抛出断言错误。 / Assert that a Try result fails, throwing an assertion error on
 * success.
 *
 * @param message 断言失败时的错误信息 / error message when the assertion fails
 * @return 失败结果本身 / the failure itself
*/
fun <T> Ret<T>.failedOrFail(message: String = "result should fail"): Ret<T> {
    if (this is Ok) {
        fail("$message: but the result succeeded with $value")
    }
    return this
}
