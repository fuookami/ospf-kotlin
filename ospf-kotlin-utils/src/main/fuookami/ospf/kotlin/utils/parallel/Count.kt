package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.countParallelly(
    crossinline predicate: SuspendPredicate<T>
): Int {
    return this.countParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.countParallelly(
    segment: UInt64,
    crossinline predicate: SuspendPredicate<T>
): Int {
    return coroutineScope {
        val promises = ArrayList<Deferred<Int>>()
        val iterator = this@countParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.count { predicate(it) }
            })
        }

        promises.sumOf { it.await() }
    }
}

suspend inline fun <T> Iterable<T>.tryCountParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Int> {
    return this.tryCountParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.tryCountParallelly(
    segment: UInt64,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Int> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Int>>()
            val iterator = this@tryCountParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.count {
                        when (val result = predicate(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                false
                            }
                        }
                    }
                })
            }

            Ok(promises.sumOf { it.await() })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(0)
    }
}

suspend inline fun <T> Collection<T>.countParallelly(
    crossinline predicate: SuspendPredicate<T>
): Int {
    return (this as Iterable<T>).countParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.countParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendPredicate<T>
): Int {
    return (this as Iterable<T>).countParallelly(this.usize / concurrentAmount, predicate)
}

suspend inline fun <T> Collection<T>.tryCountParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Int> {
    return (this as Iterable<T>).tryCountParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.tryCountParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Int> {
    return (this as Iterable<T>).tryCountParallelly(this.usize / concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.countParallelly(
    crossinline predicate: SuspendPredicate<T>
): Int {
    return this.countParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.countParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendPredicate<T>
): Int {
    return coroutineScope {
        val promises = ArrayList<Deferred<Int>>()
        val segmentAmount = this@countParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@countParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@countParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@countParallelly.subList(j, k).count { predicate(it) }
            })
            i = k
        }

        promises.sumOf { it.await() }
    }
}

suspend inline fun <T> List<T>.tryCountParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Int> {
    return this.tryCountParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.tryCountParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Int> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Int>>()
            val segmentAmount = this@tryCountParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@tryCountParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@tryCountParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@tryCountParallelly.subList(j, k).count {
                        when (val result = predicate(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                false
                            }
                        }
                    }
                })
                i = k
            }

            Ok(promises.sumOf { it.await() })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(0)
    }
}
