package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.time.*
import kotlin.reflect.*
import kotlinx.datetime.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.*

fun max(lhs: Instant, rhs: Instant): Instant {
    return if (lhs <= rhs) {
        rhs
    } else {
        lhs
    }
}

fun min(lhs: Instant, rhs: Instant): Instant {
    return if (lhs <= rhs) {
        lhs
    } else {
        rhs
    }
}

// [b, e)
data class TimeRange(
    val start: Instant = Instant.DISTANT_PAST,
    val end: Instant = Instant.DISTANT_FUTURE
) {
    val empty: Boolean get() = start >= end
    val duration: Duration get() = end - start
    
    val front: TimeRange? get() = if (start != Instant.DISTANT_PAST) {
        TimeRange(start = Instant.DISTANT_PAST, start)
    } else {
        null
    }
    
    fun frontBetween(ano: TimeRange): TimeRange? {
        return if (ano.end < start) {
            TimeRange(start = ano.end, end = start)
        } else {
            null
        }
    }
    
    val back: TimeRange? get() = if (end != Instant.DISTANT_FUTURE) {
        TimeRange(start = end, end = Instant.DISTANT_FUTURE)
    } else {
        null
    }
    
    fun backBetween(ano: TimeRange): TimeRange? {
        return if (ano.start > this.end) {
            TimeRange(start = end, end = ano.start)
        } else {
            null
        }
    }

    fun withIntersection(ano: TimeRange): Boolean {
        return start <= ano.end && ano.start < end
    }

    fun intersectionWith(ano: TimeRange): TimeRange? {
        val maxBegin = max(start, ano.start)
        val minEnd = min(end, ano.end)
        return if (minEnd > maxBegin) {
            TimeRange(maxBegin, minEnd)
        } else {
            null
        }
    }

    fun differenceWith(ano: TimeRange): List<TimeRange> {
        val intersection = intersectionWith(ano)
            ?: return listOf(this)
        return if (intersection.start == start && intersection.end == end) {
            emptyList()
        } else if (intersection.start == start) {
            listOf(
                TimeRange(
                    start = intersection.end,
                    end = end
                )
            )
        } else if (intersection.end == end) {
            listOf(
                TimeRange(
                    start = start,
                    end = intersection.start
                )
            )
        } else {
            listOf(
                TimeRange(
                    start = start,
                    end = intersection.start
                ),
                TimeRange(
                    start = intersection.end,
                    end = end
                )
            )
        }
    }

    fun differenceWith(ano: List<TimeRange>): List<TimeRange> {
        var rest: List<TimeRange> = arrayListOf(this)
        for (rhs in ano) {
            rest = rest.flatMap { it.differenceWith(rhs) }
        }
        return rest
    }

    fun contains(time: Instant): Boolean {
        return start <= time && time < end;
    }

    fun contains(time: TimeRange): Boolean {
        return start <= time.start && time.end <= end;
    }

    operator fun plus(rhs: Duration): TimeRange {
        return TimeRange(start + rhs, end + rhs)
    }

    operator fun minus(rhs: Duration): TimeRange {
        return TimeRange(start - rhs, end - rhs)
    }
}

fun List<TimeRange>.frontAt(i: Int): TimeRange {
    return if (i == 0) {
        this[i].front!!
    } else {
        this[i].frontBetween(this[i - 1])!!
    }
}

fun List<TimeRange>.backAt(i: Int): TimeRange {
    return if (i == (this.size - 1)) {
        this[i].back!!
    } else {
        this[i].backBetween(this[i + 1])!!
    }
}

inline fun <T> List<T>.findImpl(
    time: TimeRange,
    crossinline extractor: Extractor<TimeRange, T>
): Pair<Int, Int>? {
    return if (this.isEmpty()) {
        null
    } else if (this.size == 1) {
        if (extractor(this.first()).withIntersection(time)) {
            Pair(0, 1)
        } else {
            null
        }
    } else {
        val lowerBound = if (time.start == Instant.DISTANT_PAST) {
            0
        } else {
            var step = this@findImpl.size / 2
            var i = this@findImpl.size / 2
            while (i < this@findImpl.size) {
                if (extractor(this@findImpl[i]).contains(time.start)) {
                    break
                } else if (time.start < extractor(this@findImpl[i]).start) {
                    if (i == 0 || extractor(this@findImpl[i - 1]).end <= time.start) {
                        break
                    } else {
                        i -= step
                    }
                } else {
                    i += step
                }
                step /= 2
            }
            i
        }
        val upperBound = if (time.end == Instant.DISTANT_FUTURE) {
            this@findImpl.size
        } else {
            var step = this@findImpl.size / 2
            var i = this@findImpl.size / 2
            while (i < this@findImpl.size) {
                if (extractor(this@findImpl[i]).contains(time.end)) {
                    ++i
                    break
                } else if (time.end <= extractor(this@findImpl[i]).start) {
                    if (i == 0 || extractor(this@findImpl[i - 1]).start < time.end) {
                        break
                    } else {
                        i -= step
                    }
                } else {
                    i += step
                }
                step /= 2
            }
            i
        }

        Pair(lowerBound, upperBound)
    }
}

suspend inline fun <T> List<T>.findParallellyImpl(
    time: TimeRange,
    crossinline extractor: SuspendExtractor<TimeRange, T>
): Pair<Int, Int>? {
    return if (this.isEmpty()) {
        null
    } else if (this.size == 1) {
        if (extractor(this.first()).withIntersection(time)) {
            Pair(0, 1)
        } else {
            null
        }
    } else {
        coroutineScope {
            val lowerBoundPromise = if (time.start == Instant.DISTANT_PAST) {
                async(Dispatchers.Default) {
                    0
                }
            } else {
                async(Dispatchers.Default) {
                    var step = this@findParallellyImpl.size / 2
                    var i = this@findParallellyImpl.size / 2
                    while (i < this@findParallellyImpl.size) {
                        if (extractor(this@findParallellyImpl[i]).contains(time.start)) {
                            break
                        } else if (time.start < extractor(this@findParallellyImpl[i]).start) {
                            if (i == 0 || extractor(this@findParallellyImpl[i - 1]).end <= time.start) {
                                break
                            } else {
                                i -= step
                            }
                        } else {
                            i += step
                        }
                        step /= 2
                    }
                    i
                }
            }
            val upperBoundPromise = if (time.end == Instant.DISTANT_FUTURE) {
                async(Dispatchers.Default) {
                    this@findParallellyImpl.size
                }
            } else {
                async(Dispatchers.Default) {
                    var step = this@findParallellyImpl.size / 2
                    var i = this@findParallellyImpl.size / 2
                    while (i < this@findParallellyImpl.size) {
                        if (extractor(this@findParallellyImpl[i]).contains(time.end)) {
                            ++i
                            break
                        } else if (time.end <= extractor(this@findParallellyImpl[i]).start) {
                            if (i == 0 || extractor(this@findParallellyImpl[i - 1]).start < time.end) {
                                break
                            } else {
                                i -= step
                            }
                        } else {
                            i += step
                        }
                        step /= 2
                    }
                    i
                }
            }

            Pair(lowerBoundPromise.await(), upperBoundPromise.await())
        }
    }
}

inline fun <T> List<T>.findFromImpl(
    time: Instant,
    crossinline extractor: Extractor<TimeRange, T>
): Pair<Int, Int>? {
    return this.findImpl(
        time = TimeRange(
            start = time
        ),
        extractor = extractor
    )
}

suspend inline fun <T> List<T>.findFromParallellyImpl(
    time: Instant,
    crossinline extractor: SuspendExtractor<TimeRange, T>
): Pair<Int, Int>? {
    return this.findParallellyImpl(
        time = TimeRange(
            start = time
        ),
        extractor = extractor
    )
}

inline fun <T> List<T>.findUntilImpl(
    time: Instant,
    crossinline extractor: Extractor<TimeRange, T>
): Pair<Int, Int>? {
    return this.findImpl(
        time = TimeRange(
            end = time
        ),
        extractor = extractor
    )
}

suspend inline fun <T> List<T>.findUntilParallellyImpl(
    time: Instant,
    crossinline extractor: SuspendExtractor<TimeRange, T>
): Pair<Int, Int>? {
    return this.findParallellyImpl(
        time = TimeRange(
            end = time
        ),
        extractor = extractor
    )
}

fun List<TimeRange>.find(
    time: TimeRange
): List<TimeRange> {
    return this.findImpl(time) { it }
        ?.let { this@find.subList(it.first, it.second) }
        ?: emptyList()
}

suspend fun List<TimeRange>.findParallelly(
    time: TimeRange
): List<TimeRange> {
    return this.findParallellyImpl(time) { it }
        ?.let { this@findParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

fun List<TimeRange>.findFrom(
    time: Instant
): List<TimeRange> {
    return this.findFromImpl(time) { it }
        ?.let { this@findFrom.subList(it.first, it.second) }
        ?: emptyList()
}

suspend fun List<TimeRange>.findFromParallelly(
    time: Instant
): List<TimeRange> {
    return this.findFromParallellyImpl(time) { it }
        ?.let { this@findFromParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

fun List<TimeRange>.findUntil(
    time: Instant
): List<TimeRange> {
    return this.findUntilImpl(time) { it }
        ?.let { this@findUntil.subList(it.first, it.second) }
        ?: emptyList()
}

suspend fun List<TimeRange>.findUntilParallelly(
    time: Instant
): List<TimeRange> {
    return this.findUntilParallellyImpl(time) { it }
        ?.let { this@findUntilParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

fun <T> List<T>.find(
    time: TimeRange,
    extractor: KProperty1<T, TimeRange>
): List<T> {
    return this.findImpl(time) { extractor(it) }
        ?.let { this@find.subList(it.first, it.second) }
        ?: emptyList()
}

suspend fun <T> List<T>.findParallelly(
    time: TimeRange,
    extractor: KProperty1<T, TimeRange>
): List<T> {
    return this.findParallellyImpl(time) { extractor(it) }
        ?.let { this@findParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

fun <T> List<T>.findFrom(
    time: Instant,
    extractor: KProperty1<T, TimeRange>
): List<T> {
    return this.findFromImpl(time) { extractor(it) }
        ?.let { this@findFrom.subList(it.first, it.second) }
        ?: emptyList()
}

suspend fun <T> List<T>.findFromParallelly(
    time: Instant,
    extractor: KProperty1<T, TimeRange>
): List<T> {
    return this.findFromParallellyImpl(time) { extractor(it) }
        ?.let { this@findFromParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

fun <T> List<T>.findUntil(
    time: Instant,
    extractor: KProperty1<T, TimeRange>
): List<T> {
    return this.findUntilImpl(time) { extractor(it) }
        ?.let { this@findUntil.subList(it.first, it.second) }
        ?: emptyList()
}

suspend fun <T> List<T>.findUntilParallelly(
    time: Instant,
    extractor: KProperty1<T, TimeRange>
): List<T> {
    return this.findUntilParallellyImpl(time) { extractor(it) }
        ?.let { this@findUntilParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

inline fun <T> List<T>.find(
    time: TimeRange,
    crossinline extractor: Extractor<TimeRange, T>
): List<T> {
    val timeRanges = this.map { extractor(it) }
    return timeRanges.findImpl(time) { it }
        ?.let { this@find.subList(it.first, it.second) }
        ?: emptyList()
}

suspend inline fun <T> List<T>.findParallelly(
    time: TimeRange,
    crossinline extractor: SuspendExtractor<TimeRange, T>
): List<T> {
    val timeRanges = this.map { extractor(it) }
    return timeRanges.findParallellyImpl(time) { it }
        ?.let { this@findParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

inline fun <T> List<T>.findFrom(
    time: Instant,
    crossinline extractor: Extractor<TimeRange, T>
): List<T> {
    val timeRanges = this.map { extractor(it) }
    return timeRanges.findFromImpl(time) { it }
        ?.let { this@findFrom.subList(it.first, it.second) }
        ?: emptyList()
}

suspend inline fun <T> List<T>.findFromParallelly(
    time: Instant,
    crossinline extractor: SuspendExtractor<TimeRange, T>
): List<T> {
    val timeRanges = this.map { extractor(it) }
    return timeRanges.findFromParallellyImpl(time) { it }
        ?.let { this@findFromParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

inline fun <T> List<T>.findUntil(
    time: Instant,
    crossinline extractor: Extractor<TimeRange, T>
): List<T> {
    val timeRanges = this.map { extractor(it) }
    return timeRanges.findUntilImpl(time) { it }
        ?.let { this@findUntil.subList(it.first, it.second) }
        ?: emptyList()
}

suspend inline fun <T> List<T>.findUntilParallelly(
    time: Instant,
    crossinline extractor: SuspendExtractor<TimeRange, T>
): List<T> {
    val timeRanges = this.map { extractor(it) }
    return timeRanges.findUntilParallellyImpl(time) { it }
        ?.let { this@findUntilParallelly.subList(it.first, it.second) }
        ?: emptyList()
}
