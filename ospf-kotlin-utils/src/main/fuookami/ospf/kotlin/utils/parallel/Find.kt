package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.findParallelly(
    crossinline predicate: SuspendPredicate<T>
): T {
    return this.firstParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.findParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendPredicate<T>
): T {
    return this.firstParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Iterable<T>.tryFindParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T> {
    return this.tryFirstParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.tryFindParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T> {
    return this.tryFirstParallelly(concurrentAmount, predicate)
}
