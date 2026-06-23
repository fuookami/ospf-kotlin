/**
 * 测试断言辅助工具
 * Test assertion helper utilities
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import kotlin.test.fail
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 提取 Ret 结果的值，失败时抛出断言错误
 * Extract value from Ret result, throw assertion error on failure
 *
 * @param message 失败时的错误信息 / Error message on failure
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
 * 断言可空值不为 null，为 null 时抛出断言错误
 * Assert nullable value is not null, throw assertion error when null
 *
 * @param message 为 null 时的错误信息 / Error message when null
 * @return 非空值 / Non-null value
 */
fun <T> T?.orFail(message: String = "value should not be null"): T {
    return this ?: fail(message)
}

