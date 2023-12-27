package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.foldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    return this.foldIndexedParallelly(UInt64.ten, initial, operation)
}

suspend inline fun <T> Iterable<T>.foldIndexedParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    var exception: Exception? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Pair<Int, Deferred<T>>>()
            val iterator = this@foldIndexedParallelly.iterator()
            var i = 0
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<Pair<Int, T>>()
                var j = UInt64.zero
                while (iterator.hasNext() && j != segment) {
                    thisSegment.add(Pair(i, iterator.next()))
                    ++i
                    ++j
                }
                if (thisSegment.isNotEmpty()) {
                    promises.add(Pair(
                        thisSegment.first().first,
                        async(Dispatchers.Default) {
                            thisSegment.fold(initial) { lhs, rhs ->
                                operation(rhs.first, lhs, rhs.second)
                            }
                        }
                    ))
                }
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.second.await()
                accumulator = operation(promise.first, accumulator, result)
                if (index == 0 && accumulator != result) {
                    exception = IllegalArgumentException("operation is order dependent")
                    cancel()
                    return@coroutineScope accumulator
                }
            }

            accumulator
        }
    } catch (e: CancellationException) {
        exception?.let { throw it }
        accumulator
    }
}

@JvmName("tryFoldIndexedParallelly")
suspend inline fun <T> Iterable<T>.foldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    return this.foldIndexedParallelly(UInt64.ten, initial, operation)
}

@JvmName("tryFoldIndexedParallelly")
suspend inline fun <T> Iterable<T>.foldIndexedParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    var error: Error? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Pair<Int, Deferred<T>>>()
            val iterator = this@foldIndexedParallelly.iterator()
            var i = 0
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<Pair<Int, T>>()
                var j = UInt64.zero
                while (iterator.hasNext() && j != segment) {
                    thisSegment.add(Pair(i, iterator.next()))
                    ++i
                    ++j
                }
                if (thisSegment.isNotEmpty()) {
                    promises.add(Pair(
                        thisSegment.first().first,
                        async(Dispatchers.Default) {
                            thisSegment.fold(initial) { lhs, rhs ->
                                if (error != null) {
                                    lhs
                                } else {
                                    when (val ret = operation(rhs.first, lhs, rhs.second)) {
                                        is Ok -> {
                                            ret.value
                                        }

                                        is Failed -> {
                                            error = ret.error
                                            lhs
                                        }
                                    }
                                }
                            }
                        }
                    ))
                }
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.second.await()
                accumulator = when (val ret = operation(promise.first, accumulator, result)) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        return@coroutineScope Failed(error as Error)
                    }
                }
                if (index == 0 && accumulator != result) {
                    error = Err(ErrorCode.ApplicationException, "operation is order dependent")
                    cancel()
                    return@coroutineScope Failed(error as Error)
                }
            }

            Ok(accumulator)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(accumulator)
    }
}

suspend inline fun <T, R> Iterable<T>.foldIndexedParallelly(
    initial: R,
    crossinline extractor: IndexedExtractor<R, T>,
    crossinline operation: (index: Int, acc: R, R) -> R
): R {
    return this.foldIndexedParallelly(
        UInt64.ten,
        initial,
        extractor,
        operation
    )
}

suspend inline fun <T, R> Iterable<T>.foldIndexedParallelly(
    segment: UInt64,
    initial: R,
    crossinline extractor: IndexedExtractor<R, T>,
    crossinline operation: (index: Int, acc: R, R) -> R
): R {
    var exception: Exception? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Pair<Int, Deferred<R>>>()
            val iterator = this@foldIndexedParallelly.iterator()
            var i = 0
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<Pair<Int, T>>()
                var j = UInt64.zero
                while (iterator.hasNext() && j != segment) {
                    thisSegment.add(Pair(i, iterator.next()))
                    ++i
                    ++j
                }
                if (thisSegment.isNotEmpty()) {
                    promises.add(Pair(
                        thisSegment.first().first,
                        async(Dispatchers.Default) {
                            thisSegment.fold(initial) { lhs, rhs ->
                                operation(rhs.first, lhs, extractor(rhs.first, rhs.second))
                            }
                        }
                    ))
                }
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.second.await()
                accumulator = operation(promise.first, accumulator, result)
                if (index == 0 && accumulator != result) {
                    exception = IllegalArgumentException("operation is order dependent")
                    cancel()
                    return@coroutineScope accumulator
                }
            }

            accumulator
        }
    } catch (e: CancellationException) {
        exception?.let { throw it }
        accumulator
    }
}

@JvmName("tryFoldIndexedParallelly")
suspend inline fun <T, R> Iterable<T>.foldIndexedParallelly(
    initial: R,
    crossinline extractor: TryIndexedExtractor<R, T>,
    crossinline operation: (index: Int, acc: R, R) -> Ret<R>
): Ret<R> {
    return this.foldIndexedParallelly(
        UInt64.ten,
        initial,
        extractor,
        operation
    )
}

@JvmName("tryFoldIndexedParallelly")
suspend inline fun <T, R> Iterable<T>.foldIndexedParallelly(
    segment: UInt64,
    initial: R,
    crossinline extractor: TryIndexedExtractor<R, T>,
    crossinline operation: (index: Int, acc: R, R) -> Ret<R>
): Ret<R> {
    var error: Error? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Pair<Int, Deferred<R>>>()
            val iterator = this@foldIndexedParallelly.iterator()
            var i = 0
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<Pair<Int, T>>()
                var j = UInt64.zero
                while (iterator.hasNext() && j != segment) {
                    thisSegment.add(Pair(i, iterator.next()))
                    ++i
                    ++j
                }
                if (thisSegment.isNotEmpty()) {
                    promises.add(Pair(
                        thisSegment.first().first,
                        async(Dispatchers.Default) {
                            thisSegment.fold(initial) { lhs, rhs ->
                                if (error != null) {
                                    lhs
                                } else {
                                    when (val ret = operation(rhs.first, lhs,
                                        when (val ret = extractor(rhs.first, rhs.second)) {
                                            is Ok -> {
                                                ret.value
                                            }

                                            is Failed -> {
                                                error = ret.error
                                                return@fold lhs
                                            }
                                        }
                                    )) {
                                        is Ok -> {
                                            ret.value
                                        }

                                        is Failed -> {
                                            error = ret.error
                                            lhs
                                        }
                                    }
                                }
                            }
                        }
                    ))
                }
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.second.await()
                accumulator = when (val ret = operation(promise.first, accumulator, result)) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        return@coroutineScope Failed(error as Error)
                    }
                }
                if (index == 0 && accumulator != result) {
                    error = Err(ErrorCode.ApplicationException, "operation is order dependent")
                    cancel()
                    return@coroutineScope Failed(error as Error)
                }
            }

            Ok(accumulator)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(accumulator)
    }
}

suspend inline fun <T> Collection<T>.foldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    return (this as Iterable<T>).foldIndexedParallelly(
        defaultConcurrentAmount,
        initial,
        operation
    )
}

suspend inline fun <T> Collection<T>.foldIndexedParallelly(
    concurrentAmount: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    return (this as Iterable<T>).foldIndexedParallelly(
        concurrentAmount,
        initial,
        operation
    )
}

@JvmName("tryFoldIndexedParallelly")
suspend inline fun <T> Collection<T>.foldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    return (this as Iterable<T>).foldIndexedParallelly(
        defaultConcurrentAmount,
        initial,
        operation
    )
}

@JvmName("tryFoldIndexedParallelly")
suspend inline fun <T> Collection<T>.foldIndexedParallelly(
    concurrentAmount: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    return (this as Iterable<T>).foldIndexedParallelly(
        concurrentAmount,
        initial,
        operation
    )
}

suspend inline fun <T, R> Collection<T>.foldIndexedParallelly(
    initial: R,
    crossinline extractor: IndexedExtractor<R, T>,
    crossinline operation: (index: Int, acc: R, R) -> R
): R {
    return (this as Iterable<T>).foldIndexedParallelly(
        defaultConcurrentAmount,
        initial,
        extractor,
        operation
    )
}

suspend inline fun <T, R> Collection<T>.foldIndexedParallelly(
    concurrentAmount: UInt64,
    initial: R,
    crossinline extractor: IndexedExtractor<R, T>,
    crossinline operation: (index: Int, acc: R, R) -> R
): R {
    return (this as Iterable<T>).foldIndexedParallelly(
        concurrentAmount,
        initial,
        extractor,
        operation
    )
}

@JvmName("tryFoldIndexedParallelly")
suspend inline fun <T, R> Collection<T>.foldIndexedParallelly(
    initial: R,
    crossinline extractor: TryIndexedExtractor<R, T>,
    crossinline operation: (index: Int, acc: R, R) -> Ret<R>
): Ret<R> {
    return (this as Iterable<T>).foldIndexedParallelly(
        defaultConcurrentAmount,
        initial,
        extractor,
        operation
    )
}

@JvmName("tryFoldIndexedParallelly")
suspend inline fun <T, R> Collection<T>.foldIndexedParallelly(
    concurrentAmount: UInt64,
    initial: R,
    crossinline extractor: TryIndexedExtractor<R, T>,
    crossinline operation: (index: Int, acc: R, R) -> Ret<R>
): Ret<R> {
    return (this as Iterable<T>).foldIndexedParallelly(
        concurrentAmount,
        initial,
        extractor,
        operation
    )
}

suspend inline fun <T> List<T>.foldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    return this.foldIndexedParallelly(
        defaultConcurrentAmount,
        initial,
        operation
    )
}

suspend inline fun <T> List<T>.foldIndexedParallelly(
    concurrentAmount: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    var exception: Exception? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Pair<Int, Deferred<T>>>()
            val segmentAmount = this@foldIndexedParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@foldIndexedParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@foldIndexedParallelly.size - i
                )
                val thisSegment = this@foldIndexedParallelly.subList(j, k).toList()
                if (thisSegment.isNotEmpty()) {
                    promises.add(Pair(
                        j,
                        async(Dispatchers.Default) {
                            thisSegment.foldIndexed(initial) { index, lhs, rhs ->
                                operation(index + j, lhs, rhs)
                            }
                        }
                    ))
                }
                i = k
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.second.await()
                accumulator = operation(promise.first, accumulator, result)
                if (index == 0 && accumulator != result) {
                    exception = IllegalArgumentException("operation is order dependent")
                    cancel()
                    return@coroutineScope accumulator
                }
            }

            accumulator
        }
    } catch (e: CancellationException) {
        exception?.let { throw it }
        accumulator
    }
}

@JvmName("tryFoldIndexedParallelly")
suspend inline fun <T> List<T>.foldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    return this.foldIndexedParallelly(
        defaultConcurrentAmount,
        initial,
        operation
    )
}

@JvmName("tryFoldIndexedParallelly")
suspend inline fun <T> List<T>.foldIndexedParallelly(
    concurrentAmount: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    var error: Error? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Pair<Int, Deferred<T>>>()
            val segmentAmount = this@foldIndexedParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@foldIndexedParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@foldIndexedParallelly.size - i
                )
                val thisSegment = this@foldIndexedParallelly.subList(j, k).toList()
                if (thisSegment.isNotEmpty()) {
                    promises.add(Pair(
                        j,
                        async(Dispatchers.Default) {
                            thisSegment.foldIndexed(initial) { index, lhs, rhs ->
                                if (error != null) {
                                    lhs
                                } else {
                                    when (val ret = operation(index + j, lhs, rhs)) {
                                        is Ok -> {
                                            ret.value
                                        }

                                        is Failed -> {
                                            error = ret.error
                                            lhs
                                        }
                                    }
                                }
                            }
                        }
                    ))
                }
                i = k
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.second.await()
                accumulator = when (val ret = operation(promise.first, accumulator, result)) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        return@coroutineScope Failed(error as Error)
                    }
                }
                if (index == 0 && accumulator != result) {
                    error = Err(ErrorCode.ApplicationException, "operation is order dependent")
                    cancel()
                    return@coroutineScope Failed(error as Error)
                }
            }

            Ok(accumulator)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(accumulator)
    }
}

suspend inline fun <T, R> List<T>.foldIndexedParallelly(
    initial: R,
    crossinline extractor: IndexedExtractor<R, T>,
    crossinline operation: (index: Int, acc: R, R) -> R
): R {
    return this.foldIndexedParallelly(
        defaultConcurrentAmount,
        initial,
        extractor,
        operation
    )
}

suspend inline fun <T, R> List<T>.foldIndexedParallelly(
    concurrentAmount: UInt64,
    initial: R,
    crossinline extractor: IndexedExtractor<R, T>,
    crossinline operation: (index: Int, acc: R, R) -> R
): R {
    var exception: Exception? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Pair<Int, Deferred<R>>>()
            val segmentAmount = this@foldIndexedParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@foldIndexedParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@foldIndexedParallelly.size - i
                )
                val thisSegment = this@foldIndexedParallelly.subList(j, k).toList()
                if (thisSegment.isNotEmpty()) {
                    promises.add(Pair(
                        j,
                        async(Dispatchers.Default) {
                            thisSegment.foldIndexed(initial) { index, lhs, rhs ->
                                operation(index + j, lhs, extractor(index + j, rhs))
                            }
                        }
                    ))
                }
                i = k
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.second.await()
                accumulator = operation(promise.first, accumulator, result)
                if (index == 0 && accumulator != result) {
                    exception = IllegalArgumentException("operation is order dependent")
                    cancel()
                    return@coroutineScope accumulator
                }
            }

            accumulator
        }
    } catch (e: CancellationException) {
        exception?.let { throw it }
        accumulator
    }
}

@JvmName("tryFoldIndexedParallelly")
suspend inline fun <T, R> List<T>.foldIndexedParallelly(
    initial: R,
    crossinline extractor: TryIndexedExtractor<R, T>,
    crossinline operation: (index: Int, acc: R, R) -> Ret<R>
): Ret<R> {
    return this.foldIndexedParallelly(
        defaultConcurrentAmount,
        initial,
        extractor,
        operation
    )
}

@JvmName("tryFoldIndexedParallelly")
suspend inline fun <T, R> List<T>.foldIndexedParallelly(
    concurrentAmount: UInt64,
    initial: R,
    crossinline extractor: TryIndexedExtractor<R, T>,
    crossinline operation: (index: Int, acc: R, R) -> Ret<R>
): Ret<R> {
    var error: Error? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Pair<Int, Deferred<R>>>()
            val segmentAmount = this@foldIndexedParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@foldIndexedParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@foldIndexedParallelly.size - i
                )
                val thisSegment = this@foldIndexedParallelly.subList(j, k).toList()
                if (thisSegment.isNotEmpty()) {
                    promises.add(Pair(
                        j,
                        async(Dispatchers.Default) {
                            thisSegment.foldIndexed(initial) { index, lhs, rhs ->
                                if (error != null) {
                                    lhs
                                } else {
                                    when (val ret = operation(index + j, lhs,
                                        when (val ret = extractor(index + j, rhs)) {
                                            is Ok -> {
                                                ret.value
                                            }

                                            is Failed -> {
                                                error = ret.error
                                                return@foldIndexed lhs
                                            }
                                        }
                                    )) {
                                        is Ok -> {
                                            ret.value
                                        }

                                        is Failed -> {
                                            error = ret.error
                                            lhs
                                        }
                                    }
                                }
                            }
                        }
                    ))
                }
                i = k
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.second.await()
                accumulator = when (val ret = operation(promise.first, accumulator, result)) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        return@coroutineScope accumulator
                    }
                }
                if (index == 0 && accumulator != result) {
                    error = Err(ErrorCode.ApplicationException, "operation is order dependent")
                    cancel()
                    return@coroutineScope accumulator
                }
            }

            accumulator
        }.let { Ok(it) }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(accumulator)
    }
}
