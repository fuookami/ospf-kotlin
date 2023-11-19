package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.findLastParallelly(crossinline predicate: Predicate<T>): T {
    return this.lastParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.findLastParallelly(concurrentAmount: UInt64, crossinline predicate: Predicate<T>): T {
    return this.lastParallelly(concurrentAmount, predicate)
}


