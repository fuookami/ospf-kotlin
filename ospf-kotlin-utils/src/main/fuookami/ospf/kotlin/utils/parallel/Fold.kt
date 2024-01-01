package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T> Iterable<T>.foldParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> T
): T {
    return this.foldParallelly(UInt64.ten, initial, operation)
}

suspend inline fun <T> Iterable<T>.foldParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (acc: T, T) -> T
): T {
    var exception: Exception? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T>>()
            val iterator = this@foldParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.fold(initial, operation)
                })
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.await()
                accumulator = operation(accumulator, result)
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

suspend inline fun <T> Iterable<T>.tryFoldParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    return this.tryFoldParallelly(UInt64.ten, initial, operation)
}

suspend inline fun <T> Iterable<T>.tryFoldParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    var error: Error? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T>>()
            val iterator = this@tryFoldParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.fold(initial) { lhs, rhs ->
                        if (error != null) {
                            lhs
                        } else {
                            when (val ret = operation(lhs, rhs)) {
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
                })
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.await()
                accumulator = when (val ret = operation(accumulator, result)) {
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

suspend inline fun <T, R> Iterable<T>.foldParallelly(
    initial: R,
    crossinline extractor: Extractor<R, T>,
    crossinline operation: (acc: R, R) -> R
): R {
    return this.foldParallelly(UInt64.ten, initial, extractor, operation)
}

suspend inline fun <T, R> Iterable<T>.foldParallelly(
    segment: UInt64,
    initial: R,
    crossinline extractor: Extractor<R, T>,
    crossinline operation: (acc: R, R) -> R
): R {
    var exception: Exception? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R>>()
            val iterator = this@foldParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.fold(initial) { lhs, rhs ->
                        operation(lhs, extractor(rhs))
                    }
                })
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.await()
                accumulator = operation(accumulator, result)
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

suspend inline fun <T, R> Iterable<T>.tryFoldParallelly(
    initial: R,
    crossinline extractor: TryExtractor<R, T>,
    crossinline operation: (acc: R, R) -> Ret<R>
): Ret<R> {
    return this.tryFoldParallelly(UInt64.ten, initial, extractor, operation)
}

suspend inline fun <T, R> Iterable<T>.tryFoldParallelly(
    segment: UInt64,
    initial: R,
    crossinline extractor: TryExtractor<R, T>,
    crossinline operation: (acc: R, R) -> Ret<R>
): Ret<R> {
    var error: Error? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R>>()
            val iterator = this@tryFoldParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.fold(initial) { lhs, rhs ->
                        if (error != null) {
                            lhs
                        } else {
                            when (val ret = operation(lhs, when (val ret = extractor(rhs)) {
                                is Ok -> {
                                    ret.value
                                }

                                is Failed -> {
                                    error = ret.error
                                    return@fold lhs
                                }
                            })) {
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
                })
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.await()
                accumulator = when (val ret = operation(accumulator, result)) {
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

suspend inline fun <T> Collection<T>.foldParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> T
): T {
    return (this as Iterable<T>).foldParallelly(
        defaultConcurrentAmount,
        initial,
        operation
    )
}

suspend inline fun <T> Collection<T>.foldParallelly(
    concurrentAmount: UInt64,
    initial: T,
    crossinline operation: (acc: T, T) -> T
): T {
    return (this as Iterable<T>).foldParallelly(UInt64(this.size) / concurrentAmount, initial, operation)
}

suspend inline fun <T> Collection<T>.tryFoldParallelly(
    concurrentAmount: UInt64,
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    return (this as Iterable<T>).tryFoldParallelly(UInt64(this.size) / concurrentAmount, initial, operation)
}

suspend inline fun <T> Collection<T>.tryFoldParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    return (this as Iterable<T>).tryFoldParallelly(
        defaultConcurrentAmount,
        initial,
        operation
    )
}

suspend inline fun <T, R> Collection<T>.foldParallelly(
    initial: R,
    crossinline extractor: Extractor<R, T>,
    crossinline operation: (acc: R, R) -> R
): R {
    return (this as Iterable<T>).foldParallelly(
        defaultConcurrentAmount,
        initial,
        extractor,
        operation
    )
}

suspend inline fun <T, R> Collection<T>.foldParallelly(
    concurrentAmount: UInt64,
    initial: R,
    crossinline extractor: Extractor<R, T>,
    crossinline operation: (acc: R, R) -> R
): R {
    return (this as Iterable<T>).foldParallelly(
        UInt64(this.size) / concurrentAmount,
        initial,
        extractor,
        operation
    )
}

suspend inline fun <T, R> Collection<T>.tryFoldParallelly(
    initial: R,
    crossinline extractor: TryExtractor<R, T>,
    crossinline operation: (acc: R, R) -> Ret<R>
): Ret<R> {
    return (this as Iterable<T>).tryFoldParallelly(
        defaultConcurrentAmount,
        initial,
        extractor,
        operation
    )
}

suspend inline fun <T, R> Collection<T>.tryFoldParallelly(
    concurrentAmount: UInt64,
    initial: R,
    crossinline extractor: TryExtractor<R, T>,
    crossinline operation: (acc: R, R) -> Ret<R>
): Ret<R> {
    return (this as Iterable<T>).tryFoldParallelly(
        UInt64(this.size) / concurrentAmount,
        initial,
        extractor,
        operation
    )
}

suspend inline fun <T> List<T>.foldParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> T
): T {
    return this.foldParallelly(
        defaultConcurrentAmount,
        initial,
        operation
    )
}

suspend inline fun <T> List<T>.foldParallelly(
    concurrentAmount: UInt64,
    initial: T,
    crossinline operation: (acc: T, T) -> T
): T {
    var exception: Exception? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T>>()
            val iterator = this@foldParallelly.iterator()
            while (iterator.hasNext()) {
                val segmentAmount = this@foldParallelly.size / concurrentAmount.toInt()
                var i = 0
                while (i != this@foldParallelly.size) {
                    val j = i
                    val k = i + minOf(
                        segmentAmount,
                        this@foldParallelly.size - i
                    )
                    promises.add(async(Dispatchers.Default) {
                        this@foldParallelly.subList(j, k).fold(initial, operation)
                    })
                    i = k
                }

                promises.forEach {
                    accumulator = operation(accumulator, it.await())
                }
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.await()
                accumulator = operation(accumulator, result)
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

suspend inline fun <T> List<T>.tryFoldParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    return this.tryFoldParallelly(
        defaultConcurrentAmount,
        initial,
        operation
    )
}

suspend inline fun <T> List<T>.tryFoldParallelly(
    concurrentAmount: UInt64,
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    var error: Error? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T>>()
            val iterator = this@tryFoldParallelly.iterator()
            while (iterator.hasNext()) {
                val segmentAmount = this@tryFoldParallelly.size / concurrentAmount.toInt()
                var i = 0
                while (i != this@tryFoldParallelly.size) {
                    val j = i
                    val k = i + minOf(
                        segmentAmount,
                        this@tryFoldParallelly.size - i
                    )
                    promises.add(async(Dispatchers.Default) {
                        this@tryFoldParallelly.subList(j, k).fold(initial) { lhs, rhs ->
                            if (error != null) {
                                lhs
                            } else {
                                when (val ret = operation(lhs, rhs)) {
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
                    })
                    i = k
                }
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.await()
                accumulator = when (val ret = operation(accumulator, result)) {
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

suspend inline fun <T, R> List<T>.foldParallelly(
    initial: R,
    crossinline extractor: Extractor<R, T>,
    crossinline operation: (acc: R, R) -> R
): R {
    return this.foldParallelly(
        defaultConcurrentAmount,
        initial,
        extractor,
        operation
    )
}

suspend inline fun <T, R> List<T>.foldParallelly(
    concurrentAmount: UInt64,
    initial: R,
    crossinline extractor: Extractor<R, T>,
    crossinline operation: (acc: R, R) -> R
): R {
    var exception: Exception? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R>>()
            val iterator = this@foldParallelly.iterator()
            while (iterator.hasNext()) {
                val segmentAmount = this@foldParallelly.size / concurrentAmount.toInt()
                var i = 0
                while (i != this@foldParallelly.size) {
                    val j = i
                    val k = i + minOf(
                        segmentAmount,
                        this@foldParallelly.size - i
                    )
                    promises.add(async(Dispatchers.Default) {
                        this@foldParallelly.subList(j, k).fold(initial) { lhs, rhs ->
                            operation(lhs, extractor(rhs))
                        }
                    })
                    i = k
                }
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.await()
                accumulator = operation(accumulator, result)
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

suspend inline fun <T, R> List<T>.tryFoldParallelly(
    initial: R,
    crossinline extractor: TryExtractor<R, T>,
    crossinline operation: (acc: R, R) -> Ret<R>
): Ret<R> {
    return this.tryFoldParallelly(
        defaultConcurrentAmount,
        initial,
        extractor,
        operation
    )
}

suspend inline fun <T, R> List<T>.tryFoldParallelly(
    concurrentAmount: UInt64,
    initial: R,
    crossinline extractor: TryExtractor<R, T>,
    crossinline operation: (acc: R, R) -> Ret<R>
): Ret<R> {
    var error: Error? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R>>()
            val iterator = this@tryFoldParallelly.iterator()
            while (iterator.hasNext()) {
                val segmentAmount = this@tryFoldParallelly.size / concurrentAmount.toInt()
                var i = 0
                while (i != this@tryFoldParallelly.size) {
                    val j = i
                    val k = i + minOf(
                        segmentAmount,
                        this@tryFoldParallelly.size - i
                    )
                    promises.add(async(Dispatchers.Default) {
                        this@tryFoldParallelly.subList(j, k).fold(initial) { lhs, rhs ->
                            if (error != null) {
                                lhs
                            } else {
                                when (val ret = operation(lhs, when (val ret = extractor(rhs)) {
                                    is Ok -> {
                                        ret.value
                                    }

                                    is Failed -> {
                                        error = ret.error
                                        return@fold lhs
                                    }
                                })) {
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
                    })
                    i = k
                }
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.await()
                accumulator = when (val ret = operation(accumulator, result)) {
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
