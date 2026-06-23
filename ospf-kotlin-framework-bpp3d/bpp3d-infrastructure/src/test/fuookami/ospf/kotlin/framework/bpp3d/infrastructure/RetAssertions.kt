package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.test.fail
import fuookami.ospf.kotlin.utils.functional.*

/** 解包 Ret 结果值，失败时抛出断言错误 / Unwrap Ret result value, throw on failure */
fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return when (this) {
        is Ok -> value
        is Failed -> fail(message)
        is Fatal -> fail(message)
    }
}

/** 断言可空值非空 / Assert nullable value is not null */
fun <T> T?.orFail(message: String = "value should not be null"): T {
    return this ?: fail(message)
}

