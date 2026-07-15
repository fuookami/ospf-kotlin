package fuookami.ospf.kotlin.multiarray

import kotlin.test.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 测试用 Result 断言辅助
 *
 * Result assertion helpers for tests.
 */
internal fun <T> Ret<T>.valueOrFail(): T {
    return when (this) {
        is Ok -> value
        is Failed -> fail("Expected Ok result, got Failed: $message")
        is Fatal -> fail("Expected Ok result, got Fatal")
    }
}

/**
 * 断言 Result 成功并比较成功值
 *
 * Assert that Result is Ok and compare its success value.
 */
internal fun <T> assertOkEquals(expected: T, actual: Ret<T>) {
    assertEquals(expected, actual.valueOrFail())
}

/**
 * 断言 Result 为 Failed
 *
 * Assert that Result is Failed.
 */
internal fun <T> assertFailed(actual: Ret<T>) {
    assertTrue(actual is Failed, "Expected Failed result")
}
