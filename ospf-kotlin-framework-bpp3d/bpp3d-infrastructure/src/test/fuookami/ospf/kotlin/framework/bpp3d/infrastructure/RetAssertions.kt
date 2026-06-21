package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.test.fail
import fuookami.ospf.kotlin.utils.functional.*

fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return when (this) {
        is Ok -> value
        is Failed -> fail(message)
        is Fatal -> fail(message)
    }
}

fun <T> T?.orFail(message: String = "value should not be null"): T {
    return this ?: fail(message)
}

