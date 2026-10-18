package fuookami.ospf.kotlin.framework.bpp2d.domain.item

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