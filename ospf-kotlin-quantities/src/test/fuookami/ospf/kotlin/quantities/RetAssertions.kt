package fuookami.ospf.kotlin.quantities

import kotlin.test.fail
import fuookami.ospf.kotlin.utils.functional.Ret

fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return value ?: fail(message)
}

fun <T> T?.orFail(message: String = "value should not be null"): T {
    return this ?: fail(message)
}

