package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlin.reflect.*
import kotlinx.datetime.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.functional.*

// [b, e)
data class TimeRange(
    val start: Instant = Instant.DISTANT_PAST,
    val end: Instant = Instant.DISTANT_FUTURE
) {
    companion object {
        operator fun invoke(date: LocalDate): TimeRange {
            val start = date.atStartOfDayIn(TimeZone.currentSystemDefault())
            return TimeRange(
                start = start,
                end = start + 1.days
            )
        }

        operator fun invoke(fromDate: LocalDate, toDate: LocalDate): TimeRange {
            val start = fromDate.atStartOfDayIn(TimeZone.currentSystemDefault())
            val end = toDate.atStartOfDayIn(TimeZone.currentSystemDefault()) + 1.days
            return TimeRange(
                start = start,
                end = end
            )
        }
    }

    val empty: Boolean get() = start >= end
    val duration: Duration get() = end - start

    val front: TimeRange?
        get() = if (start != Instant.DISTANT_PAST) {
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

    val back: TimeRange?
        get() = if (end != Instant.DISTANT_FUTURE) {
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

    infix fun intersect(ano: TimeRange): TimeRange? {
        return intersectionWith(ano)
    }

    operator fun times(ano: TimeRange): TimeRange? {
        return intersectionWith(ano)
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

    infix fun subtract(ano: TimeRange): List<TimeRange> {
        return differenceWith(ano)
    }

    operator fun minus(ano: TimeRange): List<TimeRange> {
        return differenceWith(ano)
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
        val mergedTimes = ano
            .filter{ it.withIntersection(this) }
            .map {
                TimeRange(
                    max(it.start, start),
                    min(it.end, end)
                )
            }.merge()
        if (mergedTimes.isEmpty()) {
            return listOf(this)
        }

        val result = ArrayList<TimeRange>()
        var currentTime = start
        for (i in mergedTimes.indices) {
            if (currentTime < mergedTimes[i].start) {
                result.add(
                    TimeRange(
                        currentTime,
                        mergedTimes[i].start
                    )
                )
            }
            currentTime = mergedTimes[i].end
            if (i == mergedTimes.lastIndex) {
                if (currentTime < end) {
                    result.add(
                        TimeRange(
                            currentTime,
                            end
                        )
                    )
                }
            }
        }
        return result
    }

    operator fun contains(time: Instant): Boolean {
        return start <= time && time < end;
    }

    operator fun contains(time: TimeRange): Boolean {
        return start <= time.start && time.end <= end;
    }

    fun split(
        times: List<Instant>
    ): List<TimeRange> {
        val result = ArrayList<TimeRange>()
        val containsTimes = times
            .filter { it != start && contains(it) }
            .sorted()
            .distinct()
        if (containsTimes.isEmpty()) {
            return listOf(this)
        }
        for (i in containsTimes.indices) {
            if (i == 0) {
                result.add(TimeRange(start, containsTimes[i]))
            }
            if (i == containsTimes.lastIndex) {
                result.add(TimeRange(containsTimes[i], end))
            } else {
                result.add(TimeRange(containsTimes[i], containsTimes[i + 1]))
            }
        }
        return result
    }

    data class SplitTimeRanges(
        val times: List<TimeRange>,
        val breakTimes: List<TimeRange>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SplitTimeRanges) return false

            if (times != other.times) return false
            if (!(breakTimes.toTypedArray() contentEquals other.breakTimes.toTypedArray())) return false

            return true
        }

        override fun hashCode(): Int {
            var result = times.hashCode()
            result = 31 * result + breakTimes.hashCode()
            return result
        }
    }

    fun split(
        unit: DurationRange,
        currentDuration: Duration = Duration.ZERO,
        maxDuration: Duration? = null,
        breakTime: Duration? = null
    ): SplitTimeRanges {
        val times = ArrayList<TimeRange>()
        val breakTimes = ArrayList<TimeRange>()
        var currentTime = start
        var totalDuration = Duration.ZERO
        while (currentTime < end && (maxDuration == null || totalDuration < maxDuration)) {
            if (currentTime == start && currentDuration >= unit.lb && breakTime != null) {
                val duration = min(
                    end - currentTime,
                    maxDuration ?: Duration.INFINITE
                )
                if (currentDuration + duration <= unit.ub) {
                    times.add(
                        TimeRange(
                            currentTime,
                            currentTime + duration
                        )
                    )
                    currentTime = currentTime + duration
                    totalDuration += duration
                    break
                } else {
                    breakTimes.add(
                        TimeRange(
                            currentTime,
                            currentTime + breakTime
                        )
                    )
                    currentTime = currentTime + breakTime
                    continue
                }
            } else {
                val duration = listOf(
                    unit.lb - if (breakTime != null && currentTime == start) { currentDuration } else { Duration.ZERO },
                    end - currentTime,
                    maxDuration?.let { it - totalDuration } ?: Duration.INFINITE
                ).min()
                if (maxDuration != null
                    && (duration + totalDuration + (unit.ub - unit.lb)) >= maxDuration
                ) {
                    val extraDuration = min(
                        maxDuration - totalDuration,
                        end - currentTime
                    )
                    times.add(
                        TimeRange(
                            currentTime,
                            currentTime + extraDuration
                        )
                    )
                    totalDuration += extraDuration
                    currentTime = currentTime + extraDuration
                } else if (breakTime != null
                    && duration + if (currentTime == start) { currentDuration } else { Duration.ZERO } == unit.lb
                    && (currentTime + duration) != end
                ) {
                    if (currentTime + duration + breakTime + (unit.ub - unit.lb) >= end
                        || currentTime + duration + breakTime >= end
                    ) {
                        times.add(
                            TimeRange(
                                currentTime,
                                end - breakTime
                            )
                        )
                        breakTimes.add(
                            TimeRange(
                                end - breakTime,
                                end
                            )
                        )
                        totalDuration += (end - breakTime) - currentTime
                        currentTime = end
                    } else {
                        times.add(
                            TimeRange(
                                currentTime,
                                currentTime + duration
                            )
                        )
                        breakTimes.add(
                            TimeRange(
                                currentTime + duration,
                                currentTime + duration + breakTime
                            )
                        )
                        totalDuration += duration
                        currentTime += duration + breakTime
                    }
                } else {
                    times.add(
                        TimeRange(
                            currentTime,
                            currentTime + duration
                        )
                    )
                    totalDuration += duration
                    currentTime += duration
                }
            }
        }
        return SplitTimeRanges(times, breakTimes)
    }

    fun rsplit(
        unit: DurationRange,
        maxDuration: Duration? = null,
        breakTime: Duration? = null
    ): SplitTimeRanges {
        if (maxDuration == null || maxDuration <= duration) {
            return split(unit, Duration.ZERO, maxDuration, breakTime)
        }

        TODO("not implemented yet")
    }

    fun continuousBefore(time: TimeRange): Boolean {
        return end == time.start
    }

    fun continuousAfter(time: TimeRange): Boolean {
        return start == time.end
    }

    fun continuousWith(time: TimeRange): Boolean {
        return continuousBefore(time) || continuousAfter(time)
    }

    operator fun plus(rhs: Duration): TimeRange {
        return TimeRange(start + rhs, end + rhs)
    }

    operator fun minus(rhs: Duration): TimeRange {
        return TimeRange(start - rhs, end - rhs)
    }
}

fun List<TimeRange>.merge(): List<TimeRange> {
    if (this.isEmpty()) {
        return emptyList()
    }

    val times = this.sortedBy { it.start }
    val mergedTimes = ArrayList<TimeRange>()
    var currentTime = times.first()
    for (i in 1 until times.size) {
        if (times[i].start <= currentTime.end) {
            currentTime = TimeRange(
                currentTime.start,
                max(currentTime.end, times[i].end)
            )
        } else {
            mergedTimes.add(currentTime)
            currentTime = times[i]
        }
    }
    mergedTimes.add(currentTime)
    return mergedTimes
}

fun List<TimeRange>.frontAt(i: Int): TimeRange {
    return if (i == 0) {
        this[i].front!!
    } else {
        this[i].frontBetween(this[i - 1])!!
    }
}

fun List<TimeRange>.backAt(i: Int): TimeRange {
    return if (i == this.lastIndex) {
        this[i].back!!
    } else {
        this[i].backBetween(this[i + 1])!!
    }
}

inline fun <T> _findLowerBoundImpl(
    list: List<T>,
    time: TimeRange,
    crossinline extractor: Extractor<TimeRange, T>
): Int {
    return if (time.start <= extractor(list.first()).start) {
        0
    } else if (time.start >= extractor(list.last()).end) {
        list.size
    } else {
        var left = 0
        var right = list.size
        while (left < right) {
            val mid = (left + right) / 2
            if (time.start < extractor(list[mid]).start) {
                right = mid
            } else if (time.start >= extractor(list[mid]).end) {
                left = mid + 1
            } else {
                return mid
            }
        }
        left
    }
}

suspend inline fun <T> _findLowerBoundParallellyImpl(
    list: List<T>,
    time: TimeRange,
    crossinline extractor: SuspendExtractor<TimeRange, T>
): Int {
    return if (time.start <= extractor(list.first()).start) {
        0
    } else if (time.start >= extractor(list.last()).end) {
        list.size
    } else {
        var left = 0
        var right = list.size
        while (left < right) {
            val mid = (left + right) / 2
            if (time.start < extractor(list[mid]).start) {
                right = mid
            } else if (time.start >= extractor(list[mid]).end) {
                left = mid + 1
            } else {
                return mid
            }
        }
        left
    }
}

inline fun <T> _findUpperBoundImpl(
    list: List<T>,
    time: TimeRange,
    crossinline extractor: Extractor<TimeRange, T>
): Int {
    return if (time.end <= extractor(list.first()).start) {
        0
    } else if (time.end >= extractor(list.last()).end) {
        list.size
    } else {
        var left = 0
        var right = list.size
        while (left < right) {
            val mid = (left + right) / 2
            if (time.end < extractor(list[mid]).start) {
                right = mid
            } else if (time.end >= extractor(list[mid]).end) {
                left = mid + 1
            } else {
                return mid + 1
            }
        }
        left
    }
}

suspend inline fun <T> _findUpperBoundParallellyImpl(
    list: List<T>,
    time: TimeRange,
    crossinline extractor: SuspendExtractor<TimeRange, T>
): Int {
    return if (time.start <= extractor(list.first()).start) {
        0
    } else if (time.start >= extractor(list.last()).end) {
        list.size
    } else {
        var left = 0
        var right = list.size
        while (left < right) {
            val mid = (left + right) / 2
            if (time.end < extractor(list[mid]).start) {
                right = mid
            } else if (time.end >= extractor(list[mid]).end) {
                left = mid + 1
            } else {
                return mid + 1
            }
        }
        left
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
        val lowerBound = _findLowerBoundImpl(this@findImpl, time, extractor)
        val upperBound = _findUpperBoundImpl(this@findImpl, time, extractor)
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
            val lowerBoundPromise = async(Dispatchers.Default) {
                _findLowerBoundParallellyImpl(this@findParallellyImpl, time, extractor)
            }
            val upperBoundPromise = async(Dispatchers.Default) {
                _findUpperBoundParallellyImpl(this@findParallellyImpl, time, extractor)
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
