package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.findLastParallelly(
    crossinline predicate: Predicate<T>
): T {
    return this.lastParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.findLastParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
): T {
    return this.lastParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Iterable<T>.tryFindLastParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<T> {
    return this.tryLastParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.tryFindLastParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<T> {
    return this.tryLastParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Collection<T>.findLastParallelly(
    crossinline predicate: Predicate<T>
): T {
    return this.lastParallelly(predicate)
}

suspend inline fun <T> Collection<T>.findLastParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
): T {
    return this.lastParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> Collection<T>.tryFindLastParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<T> {
    return this.tryLastParallelly(predicate)
}

suspend inline fun <T> Collection<T>.tryFindLastParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<T> {
    return this.tryLastParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.findLastParallelly(
    crossinline predicate: Predicate<T>
): T {
    return this.lastParallelly(predicate)
}

suspend inline fun <T> List<T>.findLastParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
): T {
    return this.lastParallelly(concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.tryFindLastParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<T> {
    return this.tryLastParallelly(predicate)
}

suspend inline fun <T> List<T>.tryFindLastParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<T> {
    return this.tryLastParallelly(concurrentAmount, predicate)
}
