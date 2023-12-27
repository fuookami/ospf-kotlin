package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.countParallelly(
    crossinline predicate: Predicate<T>
): Int {
    return this.countParallelly(UInt64.ten, predicate)
}

suspend inline fun <T> Iterable<T>.countParallelly(
    segment: UInt64,
    crossinline predicate: Predicate<T>
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
                thisSegment.count(predicate)
            })
        }

        promises.sumOf { it.await() }
    }
}

@JvmName("tryCountParallelly")
suspend inline fun <T> Iterable<T>.countParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<Int> {
    return this.countParallelly(UInt64.ten, predicate)
}

@JvmName("tryCountParallelly")
suspend inline fun <T> Iterable<T>.countParallelly(
    segment: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<Int> {
    var error: Error? = null

    return try {
        coroutineScope {
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
    crossinline predicate: Predicate<T>
): Int {
    return (this as Iterable<T>).countParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> Collection<T>.countParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
): Int {
    return (this as Iterable<T>).countParallelly(UInt64(this.size) / concurrentAmount, predicate)
}

@JvmName("tryCountParallelly")
suspend inline fun <T> Collection<T>.countParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<Int> {
    return (this as Iterable<T>).countParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

@JvmName("tryCountParallelly")
suspend inline fun <T> Collection<T>.countParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<Int> {
    return (this as Iterable<T>).countParallelly(UInt64(this.size) / concurrentAmount, predicate)
}

suspend inline fun <T> List<T>.countParallelly(
    crossinline predicate: Predicate<T>
): Int {
    return this.countParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

suspend inline fun <T> List<T>.countParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: Predicate<T>
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
                this@countParallelly.subList(j, k).count(predicate)
            })
            i = k
        }

        promises.sumOf { it.await() }
    }
}

@JvmName("tryCountParallelly")
suspend inline fun <T> List<T>.countParallelly(
    crossinline predicate: TryPredicate<T>
): Ret<Int> {
    return this.countParallelly(
        defaultConcurrentAmount,
        predicate
    )
}

@JvmName("tryCountParallelly")
suspend inline fun <T> List<T>.countParallelly(
    concurrentAmount: UInt64,
    crossinline predicate: TryPredicate<T>
): Ret<Int> {
    var error: Error? = null

    return try {
        coroutineScope {
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
                    this@countParallelly.subList(j, k).count {
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
