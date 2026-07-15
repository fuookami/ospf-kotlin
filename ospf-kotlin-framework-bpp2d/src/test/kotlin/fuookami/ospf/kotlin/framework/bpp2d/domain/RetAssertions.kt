package fuookami.ospf.kotlin.framework.bpp2d.domain

import kotlin.test.fail
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 解包 Ret 结果，失败时抛出断言错误。
 * Unwrap Ret result, throw assertion error on failure.
 *
 * @param message 断言失败时的错误信息 / Error message when assertion fails
 * @return 成功时的值 / Value on success
 */
fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return when (this) {
        is Ok -> value
        is Failed -> fail(message)
        is Fatal -> fail(message)
    }
}

/**
 * 断言可空值非空，为空时抛出断言错误。
 * Assert nullable value is not null, throw assertion error when null.
 *
 * @param message 断言失败时的错误信息 / Error message when assertion fails
 * @return 非空值 / Non-null value
 */
fun <T> T?.orFail(message: String = "value should not be null"): T {
    return this ?: fail(message)
}

