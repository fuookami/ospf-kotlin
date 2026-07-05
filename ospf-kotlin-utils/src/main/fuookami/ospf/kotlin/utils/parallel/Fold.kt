/**
 * 并行折叠操作
 *
 * Sequential fold/reduce operations with yield-based coroutine scheduling.
 */
package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 最大分段值
 *
 * Maximum segment value for fold operations.
 */
@PublishedApi
internal val maxSegmentValue = Int.MAX_VALUE.toLong()

/**
 * 规范化分段大小
 *
 * Normalize segment size to valid integer range.
 *
 * @param segment 分段大小 / Segment size
 * @return 规范化后的分段大小 / Normalized segment size
 */
@PublishedApi
internal fun normalizeSegment(segment: Long): Int {
    require(segment > 0) { "segment must be greater than zero." }
    return if (segment > maxSegmentValue) {
        Int.MAX_VALUE
    } else {
        segment.toInt()
    }
}

/**
 * 更新分段计数器
 *
 * Update segment counter and yield when reaching segment size.
 *
 * @param counter 当前计数器值 / Current counter value
 * @param segmentSize 分段大小 / Segment size
 * @return 新的计数器值 / New counter value
 */
@PublishedApi
internal suspend fun nextSegmentCounter(counter: Int, segmentSize: Int): Int {
    val next = counter + 1
    return if (next >= segmentSize) {
        yield()
        0
    } else {
        next
    }
}

/**
 * 并行折叠集合元素（默认分段）
 *
 * Fold collection elements with default segment size (10).
 *
 * @param T 元素类型 / Element type
 * @param initial 初始值 / Initial value
 * @param operation 累积操作函数 / Accumulation operation function
 * @return 折叠后的结果 / Folded result
 */
suspend inline fun <T> Iterable<T>.foldParallelly(
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    return foldParallelly(segment = 10L, initial = initial, operation = operation)
}

/**
 * 并行折叠集合元素（指定分段）
 *
 * Fold collection elements with specified segment size.
 *
 * @param T 元素类型 / Element type
 * @param segment 分段大小（控制 yield 频率）/ Segment size (controls yield frequency)
 * @param initial 初始值 / Initial value
 * @param operation 累积操作函数 / Accumulation operation function
 * @return 折叠后的结果 / Folded result
 */
suspend inline fun <T> Iterable<T>.foldParallelly(
    segment: Long,
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    for (element in this) {
        accumulator = operation(accumulator, element)
        segmentCounter = nextSegmentCounter(segmentCounter, segmentSize)
    }
    return accumulator
}

/**
 * 并行折叠集合元素（带错误处理，默认分段）
 *
 * Fold collection elements with default segment size (10) and error handling.
 *
 * @param T 元素类型 / Element type
 * @param initial 初始值 / Initial value
 * @param operation 累积操作函数（返回 Ret）/ Accumulation operation function (returns Ret)
 * @return 折叠结果或错误 / Folded result or error
 */
suspend inline fun <T> Iterable<T>.tryFoldParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    return tryFoldParallelly(segment = 10L, initial = initial, operation = operation)
}

/**
 * 并行折叠集合元素（带错误收集，默认分段）
 *
 * Fold collection elements with default segment size (10) and error collection.
 *
 * @param T 元素类型 / Element type
 * @param initial 初始值 / Initial value
 * @param operation 累积操作函数（返回 Ret）/ Accumulation operation function (returns Ret)
 * @return 折叠结果或错误集合 / Folded result or error collection
 */
suspend inline fun <T> Iterable<T>.exTryFoldParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): ExRet<T> {
    return exTryFoldParallelly(segment = 10L, initial = initial, operation = operation)
}

/**
 * 并行折叠集合元素（带错误处理，指定分段）
 *
 * Fold collection elements with specified segment size and error handling.
 *
 * @param T 元素类型 / Element type
 * @param segment 分段大小（控制 yield 频率）/ Segment size (controls yield frequency)
 * @param initial 初始值 / Initial value
 * @param operation 累积操作函数（返回 Ret）/ Accumulation operation function (returns Ret)
 * @return 折叠结果或错误 / Folded result or error
 */
suspend inline fun <T> Iterable<T>.tryFoldParallelly(
    segment: Long,
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    for (element in this) {
        when (val ret = operation(accumulator, element)) {
            is Ok -> accumulator = ret.value
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }
        segmentCounter = nextSegmentCounter(segmentCounter, segmentSize)
    }
    return Ok(accumulator)
}

/**
 * 并行折叠集合元素（带错误收集，指定分段）
 *
 * Fold collection elements with specified segment size and error collection.
 *
 * @param T 元素类型 / Element type
 * @param segment 分段大小（控制 yield 频率）/ Segment size (controls yield frequency)
 * @param initial 初始值 / Initial value
 * @param operation 累积操作函数（返回 Ret）/ Accumulation operation function (returns Ret)
 * @return 折叠结果或错误集合 / Folded result or error collection
 */
suspend inline fun <T> Iterable<T>.exTryFoldParallelly(
    segment: Long,
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): ExRet<T> {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    val errors = ArrayList<Error<ErrorCode>>()
    for (element in this) {
        when (val ret = operation(accumulator, element)) {
            is Ok -> accumulator = ret.value
            is Failed, is Fatal -> errors.appendFrom(ret)
        }
        segmentCounter = nextSegmentCounter(segmentCounter, segmentSize)
    }
    return exResultOf(accumulator, errors)
}

/**
 * 并行折叠集合元素（带索引，默认分段）
 *
 * Fold collection elements with index information and default segment size (10).
 *
 * @param T 元素类型 / Element type
 * @param initial 初始值 / Initial value
 * @param operation 带索引的累积操作函数 / Indexed accumulation operation function
 * @return 折叠后的结果 / Folded result
 */
suspend inline fun <T> Iterable<T>.foldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    return foldIndexedParallelly(segment = 10L, initial = initial, operation = operation)
}

/**
 * 并行折叠集合元素（带索引，指定分段）
 *
 * Fold collection elements with index information and specified segment size.
 *
 * @param T 元素类型 / Element type
 * @param segment 分段大小（控制 yield 频率）/ Segment size (controls yield frequency)
 * @param initial 初始值 / Initial value
 * @param operation 带索引的累积操作函数 / Indexed accumulation operation function
 * @return 折叠后的结果 / Folded result
 */
suspend inline fun <T> Iterable<T>.foldIndexedParallelly(
    segment: Long,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    for ((index, element) in withIndex()) {
        accumulator = operation(index, accumulator, element)
        segmentCounter = nextSegmentCounter(segmentCounter, segmentSize)
    }
    return accumulator
}

/**
 * 并行折叠集合元素（带索引，带错误处理，默认分段）
 *
 * Fold collection elements with index information, default segment size (10) and error handling.
 *
 * @param T 元素类型 / Element type
 * @param initial 初始值 / Initial value
 * @param operation 带索引的累积操作函数（返回 Ret）/ Indexed accumulation operation function (returns Ret)
 * @return 折叠结果或错误 / Folded result or error
 */
suspend inline fun <T> Iterable<T>.tryFoldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    return tryFoldIndexedParallelly(segment = 10L, initial = initial, operation = operation)
}

/**
 * 并行折叠集合元素（带索引，带错误收集，默认分段）
 *
 * Fold collection elements with index information, default segment size (10) and error collection.
 *
 * @param T 元素类型 / Element type
 * @param initial 初始值 / Initial value
 * @param operation 带索引的累积操作函数（返回 Ret）/ Indexed accumulation operation function (returns Ret)
 * @return 折叠结果或错误集合 / Folded result or error collection
 */
suspend inline fun <T> Iterable<T>.exTryFoldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): ExRet<T> {
    return exTryFoldIndexedParallelly(segment = 10L, initial = initial, operation = operation)
}

/**
 * 并行折叠集合元素（带索引，带错误处理，指定分段）
 *
 * Fold collection elements with index information, specified segment size and error handling.
 *
 * @param T 元素类型 / Element type
 * @param segment 分段大小（控制 yield 频率）/ Segment size (controls yield frequency)
 * @param initial 初始值 / Initial value
 * @param operation 带索引的累积操作函数（返回 Ret）/ Indexed accumulation operation function (returns Ret)
 * @return 折叠结果或错误 / Folded result or error
 */
suspend inline fun <T> Iterable<T>.tryFoldIndexedParallelly(
    segment: Long,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    for ((index, element) in withIndex()) {
        when (val ret = operation(index, accumulator, element)) {
            is Ok -> accumulator = ret.value
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }
        segmentCounter = nextSegmentCounter(segmentCounter, segmentSize)
    }
    return Ok(accumulator)
}

/**
 * 并行折叠集合元素（带索引，带错误收集，指定分段）
 *
 * Fold collection elements with index information, specified segment size and error collection.
 *
 * @param T 元素类型 / Element type
 * @param segment 分段大小（控制 yield 频率）/ Segment size (controls yield frequency)
 * @param initial 初始值 / Initial value
 * @param operation 带索引的累积操作函数（返回 Ret）/ Indexed accumulation operation function (returns Ret)
 * @return 折叠结果或错误集合 / Folded result or error collection
 */
suspend inline fun <T> Iterable<T>.exTryFoldIndexedParallelly(
    segment: Long,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): ExRet<T> {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    val errors = ArrayList<Error<ErrorCode>>()
    for ((index, element) in withIndex()) {
        when (val ret = operation(index, accumulator, element)) {
            is Ok -> accumulator = ret.value
            is Failed, is Fatal -> errors.appendFrom(ret)
        }
        segmentCounter = nextSegmentCounter(segmentCounter, segmentSize)
    }
    return exResultOf(accumulator, errors)
}

/**
 * 并行反向折叠集合元素（默认分段）
 *
 * Fold collection elements from right to left with default segment size (10).
 *
 * @param T 元素类型 / Element type
 * @param initial 初始值 / Initial value
 * @param operation 累积操作函数 / Accumulation operation function
 * @return 折叠后的结果 / Folded result
 */
suspend inline fun <T> Iterable<T>.foldRightParallelly(
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    return foldRightParallelly(segment = 10L, initial = initial, operation = operation)
}

/**
 * 并行反向折叠集合元素（指定分段）
 *
 * Fold collection elements from right to left with specified segment size.
 *
 * @param T 元素类型 / Element type
 * @param segment 分段大小（控制 yield 频率）/ Segment size (controls yield frequency)
 * @param initial 初始值 / Initial value
 * @param operation 累积操作函数 / Accumulation operation function
 * @return 折叠后的结果 / Folded result
 */
suspend inline fun <T> Iterable<T>.foldRightParallelly(
    segment: Long,
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    for (element in toList().asReversed()) {
        accumulator = operation(accumulator, element)
        segmentCounter = nextSegmentCounter(segmentCounter, segmentSize)
    }
    return accumulator
}

/**
 * 并行反向折叠集合元素（带错误处理，默认分段）
 *
 * Fold collection elements from right to left with default segment size (10) and error handling.
 *
 * @param T 元素类型 / Element type
 * @param initial 初始值 / Initial value
 * @param operation 累积操作函数（返回 Ret）/ Accumulation operation function (returns Ret)
 * @return 折叠结果或错误 / Folded result or error
 */
suspend inline fun <T> Iterable<T>.tryFoldRightParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    return tryFoldRightParallelly(segment = 10L, initial = initial, operation = operation)
}

/**
 * 并行反向折叠集合元素（带错误收集，默认分段）
 *
 * Fold collection elements from right to left with default segment size (10) and error collection.
 *
 * @param T 元素类型 / Element type
 * @param initial 初始值 / Initial value
 * @param operation 累积操作函数（返回 Ret）/ Accumulation operation function (returns Ret)
 * @return 折叠结果或错误集合 / Folded result or error collection
 */
suspend inline fun <T> Iterable<T>.exTryFoldRightParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): ExRet<T> {
    return exTryFoldRightParallelly(segment = 10L, initial = initial, operation = operation)
}

/**
 * 并行反向折叠集合元素（带错误处理，指定分段）
 *
 * Fold collection elements from right to left with specified segment size and error handling.
 *
 * @param T 元素类型 / Element type
 * @param segment 分段大小（控制 yield 频率）/ Segment size (controls yield frequency)
 * @param initial 初始值 / Initial value
 * @param operation 累积操作函数（返回 Ret）/ Accumulation operation function (returns Ret)
 * @return 折叠结果或错误 / Folded result or error
 */
suspend inline fun <T> Iterable<T>.tryFoldRightParallelly(
    segment: Long,
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    for (element in toList().asReversed()) {
        when (val ret = operation(accumulator, element)) {
            is Ok -> accumulator = ret.value
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }
        segmentCounter = nextSegmentCounter(segmentCounter, segmentSize)
    }
    return Ok(accumulator)
}

/**
 * 并行反向折叠集合元素（带错误收集，指定分段）
 *
 * Fold collection elements from right to left with specified segment size and error collection.
 *
 * @param T 元素类型 / Element type
 * @param segment 分段大小（控制 yield 频率）/ Segment size (controls yield frequency)
 * @param initial 初始值 / Initial value
 * @param operation 累积操作函数（返回 Ret）/ Accumulation operation function (returns Ret)
 * @return 折叠结果或错误集合 / Folded result or error collection
 */
suspend inline fun <T> Iterable<T>.exTryFoldRightParallelly(
    segment: Long,
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): ExRet<T> {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    val errors = ArrayList<Error<ErrorCode>>()
    for (element in toList().asReversed()) {
        when (val ret = operation(accumulator, element)) {
            is Ok -> accumulator = ret.value
            is Failed, is Fatal -> errors.appendFrom(ret)
        }
        segmentCounter = nextSegmentCounter(segmentCounter, segmentSize)
    }
    return exResultOf(accumulator, errors)
}

/**
 * 并行反向折叠集合元素（带索引，默认分段）
 *
 * Fold collection elements from right to left with index information and default segment size (10).
 *
 * @param T 元素类型 / Element type
 * @param initial 初始值 / Initial value
 * @param operation 带索引的累积操作函数 / Indexed accumulation operation function
 * @return 折叠后的结果 / Folded result
 */
suspend inline fun <T> Iterable<T>.foldRightIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    return foldRightIndexedParallelly(segment = 10L, initial = initial, operation = operation)
}

/**
 * 并行反向折叠集合元素（带索引，指定分段）
 *
 * Fold collection elements from right to left with index information and specified segment size.
 *
 * @param T 元素类型 / Element type
 * @param segment 分段大小（控制 yield 频率）/ Segment size (controls yield frequency)
 * @param initial 初始值 / Initial value
 * @param operation 带索引的累积操作函数 / Indexed accumulation operation function
 * @return 折叠后的结果 / Folded result
 */
suspend inline fun <T> Iterable<T>.foldRightIndexedParallelly(
    segment: Long,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    val elements = toList()
    for (index in elements.indices.reversed()) {
        accumulator = operation(index, accumulator, elements[index])
        segmentCounter = nextSegmentCounter(segmentCounter, segmentSize)
    }
    return accumulator
}

/**
 * 并行反向折叠集合元素（带索引，带错误处理，默认分段）
 *
 * Fold collection elements from right to left with index information, default segment size (10) and error handling.
 *
 * @param T 元素类型 / Element type
 * @param initial 初始值 / Initial value
 * @param operation 带索引的累积操作函数（返回 Ret）/ Indexed accumulation operation function (returns Ret)
 * @return 折叠结果或错误 / Folded result or error
 */
suspend inline fun <T> Iterable<T>.tryFoldRightIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    return tryFoldRightIndexedParallelly(segment = 10L, initial = initial, operation = operation)
}

/**
 * 并行反向折叠集合元素（带索引，带错误收集，默认分段）
 *
 * Fold collection elements from right to left with index information, default segment size (10) and error collection.
 *
 * @param T 元素类型 / Element type
 * @param initial 初始值 / Initial value
 * @param operation 带索引的累积操作函数（返回 Ret）/ Indexed accumulation operation function (returns Ret)
 * @return 折叠结果或错误集合 / Folded result or error collection
 */
suspend inline fun <T> Iterable<T>.exTryFoldRightIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): ExRet<T> {
    return exTryFoldRightIndexedParallelly(segment = 10L, initial = initial, operation = operation)
}

/**
 * 并行反向折叠集合元素（带索引，带错误处理，指定分段）
 *
 * Fold collection elements from right to left with index information, specified segment size and error handling.
 *
 * @param T 元素类型 / Element type
 * @param segment 分段大小（控制 yield 频率）/ Segment size (controls yield frequency)
 * @param initial 初始值 / Initial value
 * @param operation 带索引的累积操作函数（返回 Ret）/ Indexed accumulation operation function (returns Ret)
 * @return 折叠结果或错误 / Folded result or error
 */
suspend inline fun <T> Iterable<T>.tryFoldRightIndexedParallelly(
    segment: Long,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    val elements = toList()
    for (index in elements.indices.reversed()) {
        when (val ret = operation(index, accumulator, elements[index])) {
            is Ok -> accumulator = ret.value
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }
        segmentCounter = nextSegmentCounter(segmentCounter, segmentSize)
    }
    return Ok(accumulator)
}

/**
 * 并行反向折叠集合元素（带索引，带错误收集，指定分段）
 *
 * Fold collection elements from right to left with index information, specified segment size and error collection.
 *
 * @param T 元素类型 / Element type
 * @param segment 分段大小（控制 yield 频率）/ Segment size (controls yield frequency)
 * @param initial 初始值 / Initial value
 * @param operation 带索引的累积操作函数（返回 Ret）/ Indexed accumulation operation function (returns Ret)
 * @return 折叠结果或错误集合 / Folded result or error collection
 */
suspend inline fun <T> Iterable<T>.exTryFoldRightIndexedParallelly(
    segment: Long,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): ExRet<T> {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    val errors = ArrayList<Error<ErrorCode>>()
    val elements = toList()
    for (index in elements.indices.reversed()) {
        when (val ret = operation(index, accumulator, elements[index])) {
            is Ok -> accumulator = ret.value
            is Failed, is Fatal -> errors.appendFrom(ret)
        }
        segmentCounter = nextSegmentCounter(segmentCounter, segmentSize)
    }
    return exResultOf(accumulator, errors)
}
