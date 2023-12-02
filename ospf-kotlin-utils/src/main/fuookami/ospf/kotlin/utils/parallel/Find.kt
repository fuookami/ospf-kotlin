package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.findParallelly(crossinline predicate: Predicate<T>): T {
    return this.firstParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.findParallelly(concurrentAmount: UInt64, crossinline predicate: Predicate<T>): T {
    return this.firstParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Iterable<T>.findParallelly(crossinline predicate: TryPredicate<T>): Result<T, Error> {
    return this.firstParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.findParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Result<T, Error> {
    return this.firstParallelly(concurrentAmount, predicate)
}
