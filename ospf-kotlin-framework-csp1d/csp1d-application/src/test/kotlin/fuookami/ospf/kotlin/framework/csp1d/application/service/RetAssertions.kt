package fuookami.ospf.kotlin.framework.csp1d.application.service

import kotlin.test.fail
import fuookami.ospf.kotlin.utils.functional.Ret

fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return value ?: fail(message)
}

