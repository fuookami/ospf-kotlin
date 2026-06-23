package fuookami.ospf.kotlin.framework.bpp3d.application.service

import kotlin.test.fail
import fuookami.ospf.kotlin.utils.functional.Ret

/** 解包 Ret 结果值，失败时抛出断言错误 / Unwrap Ret result value, throw on failure */
fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return value ?: fail(message)
}

