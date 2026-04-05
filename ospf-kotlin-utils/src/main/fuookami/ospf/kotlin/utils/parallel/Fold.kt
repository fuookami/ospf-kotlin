package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.yield
import fuookami.ospf.kotlin.utils.functional.ExRet
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.SuspendTryExtractor

/**
 * 并行折叠操作
 *
 * Parallel fold/reduce operations.
 *
 * 注意：Fold 操作本质上是顺序执行的累积操作，不适用于并行处理。
 * Note: Fold operations are inherently sequential accumulation operations, not suitable for parallelism.
 * segment 参数用于控制 yield 频率，防止长时间阻塞协程。
 * The segment parameter controls yield frequency to prevent long-running coroutine blocking.
 * 因此 Fold 系列不纳入 UTL-005 的并发控制范围。
 * Therefore, Fold series is excluded from UTL-005 concurrency control scope.
 */

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

suspend inline fun <T> Iterable<T>.foldParallelly(
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    return foldParallelly(segment = 10L, initial = initial, operation = operation)
}

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

suspend inline fun <T> Iterable<T>.tryFoldParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    return tryFoldParallelly(segment = 10L, initial = initial, operation = operation)
}

suspend inline fun <T> Iterable<T>.exTryFoldParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): ExRet<T> {
    return exTryFoldParallelly(segment = 10L, initial = initial, operation = operation)
}

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

suspend inline fun <T> Iterable<T>.exTryFoldParallelly(
    segment: Long,
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): ExRet<T> {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
    for (element in this) {
        when (val ret = operation(accumulator, element)) {
            is Ok -> accumulator = ret.value
            is Failed, is Fatal -> errors.appendFrom(ret)
        }
        segmentCounter = nextSegmentCounter(segmentCounter, segmentSize)
    }
    return exResultOf(accumulator, errors)
}

suspend inline fun <T> Iterable<T>.foldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    return foldIndexedParallelly(segment = 10L, initial = initial, operation = operation)
}

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

suspend inline fun <T> Iterable<T>.tryFoldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    return tryFoldIndexedParallelly(segment = 10L, initial = initial, operation = operation)
}

suspend inline fun <T> Iterable<T>.exTryFoldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): ExRet<T> {
    return exTryFoldIndexedParallelly(segment = 10L, initial = initial, operation = operation)
}

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

suspend inline fun <T> Iterable<T>.exTryFoldIndexedParallelly(
    segment: Long,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): ExRet<T> {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
    for ((index, element) in withIndex()) {
        when (val ret = operation(index, accumulator, element)) {
            is Ok -> accumulator = ret.value
            is Failed, is Fatal -> errors.appendFrom(ret)
        }
        segmentCounter = nextSegmentCounter(segmentCounter, segmentSize)
    }
    return exResultOf(accumulator, errors)
}

suspend inline fun <T> Iterable<T>.foldRightParallelly(
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    return foldRightParallelly(segment = 10L, initial = initial, operation = operation)
}

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

suspend inline fun <T> Iterable<T>.tryFoldRightParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    return tryFoldRightParallelly(segment = 10L, initial = initial, operation = operation)
}

suspend inline fun <T> Iterable<T>.exTryFoldRightParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): ExRet<T> {
    return exTryFoldRightParallelly(segment = 10L, initial = initial, operation = operation)
}

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

suspend inline fun <T> Iterable<T>.exTryFoldRightParallelly(
    segment: Long,
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): ExRet<T> {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
    for (element in toList().asReversed()) {
        when (val ret = operation(accumulator, element)) {
            is Ok -> accumulator = ret.value
            is Failed, is Fatal -> errors.appendFrom(ret)
        }
        segmentCounter = nextSegmentCounter(segmentCounter, segmentSize)
    }
    return exResultOf(accumulator, errors)
}

suspend inline fun <T> Iterable<T>.foldRightIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    return foldRightIndexedParallelly(segment = 10L, initial = initial, operation = operation)
}

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

suspend inline fun <T> Iterable<T>.tryFoldRightIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    return tryFoldRightIndexedParallelly(segment = 10L, initial = initial, operation = operation)
}

suspend inline fun <T> Iterable<T>.exTryFoldRightIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): ExRet<T> {
    return exTryFoldRightIndexedParallelly(segment = 10L, initial = initial, operation = operation)
}

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

suspend inline fun <T> Iterable<T>.exTryFoldRightIndexedParallelly(
    segment: Long,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): ExRet<T> {
    val segmentSize = normalizeSegment(segment)
    var segmentCounter = 0
    var accumulator = initial
    val errors = ArrayList<fuookami.ospf.kotlin.utils.error.Error>()
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



