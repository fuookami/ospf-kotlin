package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, U> Iterable<T>.sumOfParallelly(
    constants: ArithmeticConstants<U>,
    crossinline extractor: SuspendExtractor<U, T>
): U
        where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfParallelly(UInt64.ten, constants, extractor)
}

suspend inline fun <T, U> Iterable<T>.sumOfParallelly(
    segment: UInt64,
    constants: ArithmeticConstants<U>,
    crossinline extractor: SuspendExtractor<U, T>
): U
        where U : Arithmetic<U>, U : Plus<U, U> {
    var sum = constants.zero
    coroutineScope {
        val promises = ArrayList<Deferred<U>>()
        val iterator = this@sumOfParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.sum(constants)
            })
        }

        promises.forEach {
            sum += it.await()
        }
    }
    return sum
}

suspend inline fun <T, U> Iterable<T>.trySumOfParallelly(
    constants: ArithmeticConstants<U>,
    crossinline extractor: SuspendTryExtractor<U, T>
): Ret<U>
        where U : Arithmetic<U>, U : Plus<U, U> {
    return this.trySumOfParallelly(UInt64.ten, constants, extractor)
}

suspend inline fun <T, U> Iterable<T>.trySumOfParallelly(
    segment: UInt64,
    constants: ArithmeticConstants<U>,
    crossinline extractor: SuspendTryExtractor<U, T>
): Ret<U>
        where U : Arithmetic<U>, U : Plus<U, U> {
    var error: Error? = null
    var sum = constants.zero

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<U>>()
            val iterator = this@trySumOfParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.map {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                constants.zero
                            }
                        }
                    }.sum(constants)
                })
            }

            promises.forEach {
                sum += it.await()
            }
        }

        Ok(sum)
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(constants.zero)
    }
}

suspend inline fun <T, U> Collection<T>.sumOfParallelly(
    constants: ArithmeticConstants<U>,
    crossinline extractor: SuspendExtractor<U, T>
): U
        where U : Arithmetic<U>, U : Plus<U, U> {
    return (this as Iterable<T>).sumOfParallelly(
        defaultConcurrentAmount,
        constants,
        extractor
    )
}

suspend inline fun <T, U> Collection<T>.sumOfParallelly(
    concurrentAmount: UInt64,
    constants: ArithmeticConstants<U>,
    crossinline extractor: SuspendExtractor<U, T>
): U
        where U : Arithmetic<U>, U : Plus<U, U> {
    return (this as Iterable<T>).sumOfParallelly(this.usize / concurrentAmount, constants, extractor)
}

suspend inline fun <T, U> Collection<T>.trySumOfParallelly(
    constants: ArithmeticConstants<U>,
    crossinline extractor: SuspendTryExtractor<U, T>
): Ret<U>
        where U : Arithmetic<U>, U : Plus<U, U> {
    return (this as Iterable<T>).trySumOfParallelly(
        defaultConcurrentAmount,
        constants,
        extractor
    )
}

suspend inline fun <T, U> Collection<T>.trySumOfParallelly(
    concurrentAmount: UInt64,
    constants: ArithmeticConstants<U>,
    crossinline extractor: SuspendTryExtractor<U, T>
): Ret<U>
        where U : Arithmetic<U>, U : Plus<U, U> {
    return (this as Iterable<T>).trySumOfParallelly(this.usize / concurrentAmount, constants, extractor)
}

suspend inline fun <T, U> List<T>.sumOfParallelly(
    constants: ArithmeticConstants<U>,
    crossinline extractor: SuspendExtractor<U, T>
): U
        where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfParallelly(
        defaultConcurrentAmount,
        constants,
        extractor
    )
}

suspend inline fun <T, U> List<T>.sumOfParallelly(
    concurrentAmount: UInt64,
    constants: ArithmeticConstants<U>,
    crossinline extractor: SuspendExtractor<U, T>
): U
        where U : Arithmetic<U>, U : Plus<U, U> {
    var sum = constants.zero
    coroutineScope {
        val promises = ArrayList<Deferred<U>>()
        val segmentAmount = this@sumOfParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@sumOfParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@sumOfParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@sumOfParallelly.subList(j, k).map { extractor(it) }.sum(constants)
            })
            i = k
        }

        promises.forEach {
            sum += it.await()
        }
    }
    return sum
}

suspend inline fun <T, U> List<T>.trySumOfParallelly(
    constants: ArithmeticConstants<U>,
    crossinline extractor: SuspendTryExtractor<U, T>
): Ret<U>
        where U : Arithmetic<U>, U : Plus<U, U> {
    return this.trySumOfParallelly(
        defaultConcurrentAmount,
        constants,
        extractor
    )
}

suspend inline fun <T, U> List<T>.trySumOfParallelly(
    concurrentAmount: UInt64,
    constants: ArithmeticConstants<U>,
    crossinline extractor: SuspendTryExtractor<U, T>
): Ret<U>
        where U : Arithmetic<U>, U : Plus<U, U> {
    var error: Error? = null
    var sum = constants.zero

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<U>>()
            val segmentAmount = this@trySumOfParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@trySumOfParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@trySumOfParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@trySumOfParallelly.subList(j, k).map {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                constants.zero
                            }
                        }
                    }.sum(constants)
                })
                i = k
            }

            promises.forEach {
                sum += it.await()
            }
        }

        Ok(sum)
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(constants.zero)
    }
}
