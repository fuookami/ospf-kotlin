package fuookami.ospf.kotlin.utils.parallel

import kotlin.reflect.full.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.*

// sumOfParallelly: Sum of extracted values in parallel / 并行计算提取值的总和
@Suppress("UNCHECKED_CAST")
suspend inline fun <T, reified U> Iterable<T>.sumOfParallelly(
    crossinline extractor: SuspendExtractor<U, T>
): U where U : Arithmetic<U>, U : Plus<U, U> {
    return coroutineScope {
        val promises = ArrayList<Deferred<U>>()
        for (element in this@sumOfParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                extractor(element)
            })
        }
        var sum = (U::class.companionObjectInstance!! as ArithmeticConstants<U>).zero
        for (promise in promises) {
            sum += promise.await()
        }
        sum
    }
}

// trySumOfParallelly: Try version of sumOfParallelly / sumOfParallelly 的 try 版本
@Suppress("UNCHECKED_CAST")
suspend inline fun <T, reified U> Iterable<T>.trySumOfParallelly(
    crossinline extractor: SuspendTryExtractor<U, T>
): Ret<U> where U : Arithmetic<U>, U : Plus<U, U> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<U>>>()
            for (element in this@trySumOfParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(element)
                })
            }
            var sum = (U::class.companionObjectInstance!! as ArithmeticConstants<U>).zero
            for (promise in promises) {
                sum += when (val result = promise.await()) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        (U::class.companionObjectInstance!! as ArithmeticConstants<U>).zero
                    }
                }
            }
            Ok(sum)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok((U::class.companionObjectInstance!! as ArithmeticConstants<U>).zero)
    }
}

// foldParallelly: Fold elements in parallel / 并行折叠元素
suspend inline fun <T> Iterable<T>.foldParallelly(
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    return this.foldParallelly(segment = UInt64.ten, initial = initial, operation = operation)
}

// foldParallelly with segment: Fold elements in parallel with segment size / 带分段大小的并行折叠
suspend inline fun <T> Iterable<T>.foldParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
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
                    thisSegment.fold(initial) { acc, value -> operation(acc, value) }
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

// tryFoldParallelly: Try version of foldParallelly / foldParallelly 的 try 版本
suspend inline fun <T> Iterable<T>.tryFoldParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    return this.tryFoldParallelly(segment = UInt64.ten, initial = initial, operation = operation)
}

// tryFoldParallelly with segment: Try version of foldParallelly with segment size / 带分段大小的 try 版本并行折叠
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

// foldIndexedParallelly: Fold elements with index in parallel / 并行带索引折叠元素
suspend inline fun <T> Iterable<T>.foldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    return this.foldIndexedParallelly(segment = UInt64.ten, initial = initial, operation = operation)
}

// foldIndexedParallelly with segment: Fold elements with index in parallel with segment size / 带分段大小的并行带索引折叠
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
                    thisSegment.add(i to iterator.next())
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

// tryFoldIndexedParallelly: Try version of foldIndexedParallelly / foldIndexedParallelly 的 try 版本
suspend inline fun <T> Iterable<T>.tryFoldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    return this.tryFoldIndexedParallelly(
        segment = UInt64.ten,
        initial = initial,
        operation = operation
    )
}

// tryFoldIndexedParallelly with segment: Try version of foldIndexedParallelly with segment size / 带分段大小的 try 版本并行带索引折叠
suspend inline fun <T> Iterable<T>.tryFoldIndexedParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    var error: Error? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Pair<Int, Deferred<T>>>()
            val iterator = this@tryFoldIndexedParallelly.iterator()
            var i = 0
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<Pair<Int, T>>()
                var j = UInt64.zero
                while (iterator.hasNext() && j != segment) {
                    thisSegment.add(i to iterator.next())
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

// foldRightParallelly: Fold elements from right in parallel / 从右并行折叠元素
suspend inline fun <T> Iterable<T>.foldRightParallelly(
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    return this.foldRightParallelly(
        segment = UInt64.ten,
        initial = initial,
        operation = operation
    )
}

// foldRightParallelly with segment: Fold elements from right in parallel with segment size / 带分段大小的从右并行折叠
suspend inline fun <T> Iterable<T>.foldRightParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    var exception: Exception? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T>>()
            val iterator = this@foldRightParallelly.reversed().iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.fold(initial) { acc, value -> operation(acc, value) }
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

// tryFoldRightParallelly: Try version of foldRightParallelly / foldRightParallelly 的 try 版本
suspend inline fun <T> Iterable<T>.tryFoldRightParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    return this.tryFoldRightParallelly(
        segment = UInt64.ten,
        initial = initial,
        operation = operation
    )
}

// tryFoldRightParallelly with segment: Try version of foldRightParallelly with segment size / 带分段大小的 try 版本从右并行折叠
suspend inline fun <T> Iterable<T>.tryFoldRightParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    var error: Error? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T>>()
            val iterator = this@tryFoldRightParallelly.reversed().iterator()
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

// foldRightIndexedParallelly: Fold elements from right with index in parallel / 从右带索引并行折叠元素
suspend inline fun <T> Iterable<T>.foldRightIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    return this.foldRightIndexedParallelly(
        segment = UInt64.ten,
        initial = initial,
        operation = operation
    )
}

// foldRightIndexedParallelly with segment: Fold elements from right with index in parallel with segment size / 带分段大小的从右带索引并行折叠
suspend inline fun <T> Iterable<T>.foldRightIndexedParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    var exception: Exception? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Pair<Int, Deferred<T>>>()
            val iterator = this@foldRightIndexedParallelly.reversed().iterator()
            var i = 0
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<Pair<Int, T>>()
                var j = UInt64.zero
                while (iterator.hasNext() && j != segment) {
                    thisSegment.add(i to iterator.next())
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

// tryFoldRightIndexedParallelly: Try version of foldRightIndexedParallelly / foldRightIndexedParallelly 的 try 版本
suspend inline fun <T> Iterable<T>.tryFoldRightIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    return this.tryFoldRightIndexedParallelly(
        segment = UInt64.ten,
        initial = initial,
        operation = operation
    )
}

// tryFoldRightIndexedParallelly with segment: Try version of foldRightIndexedParallelly with segment size / 带分段大小的 try 版本从右带索引并行折叠
suspend inline fun <T> Iterable<T>.tryFoldRightIndexedParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    var error: Error? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Pair<Int, Deferred<T>>>()
            val iterator = this@tryFoldRightIndexedParallelly.withIndex().reversed().iterator()
            var i = 0
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<IndexedValue<T>>()
                var j = UInt64.zero
                while (iterator.hasNext() && j != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                    ++j
                }
                if (thisSegment.isNotEmpty()) {
                    promises.add(Pair(
                        thisSegment.first().index,
                        async(Dispatchers.Default) {
                            thisSegment.fold(initial) { lhs, rhs ->
                                if (error != null) {
                                    lhs
                                } else {
                                    when (val ret = operation(rhs.index, lhs, rhs.value)) {
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