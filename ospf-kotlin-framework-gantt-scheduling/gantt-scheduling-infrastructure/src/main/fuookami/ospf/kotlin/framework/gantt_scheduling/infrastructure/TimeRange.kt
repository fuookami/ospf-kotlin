@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 时间范围及相关工具函数 / Time range and related utility functions
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.utils.functional.SuspendExtractor
import fuookami.ospf.kotlin.utils.max
import fuookami.ospf.kotlin.utils.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.serialization.Serializable
import kotlin.reflect.KProperty1
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * 时间范围 [start, end)，实现 TimeSlot 接口 / Time range [start, end), implementing TimeSlot interface
 *
 * @property start 开始时间（默认为 DISTANT_PAST）/ Start time (default DISTANT_PAST)
 * @property end 结束时间（默认为 DISTANT_FUTURE）/ End time (default DISTANT_FUTURE)
 */
// [b, e)
@Serializable
data class TimeRange(
    override val start: Instant = Instant.DISTANT_PAST,
    override val end: Instant = Instant.DISTANT_FUTURE
) : TimeSlot {
    private fun unsupportedReverseSplit(maxDuration: Duration): Nothing {
        throw UnsupportedOperationException(
            "TimeRange.rsplit 暂不支持 maxDuration($maxDuration) 大于区间时长($duration)。"
        )
    }

    companion object {
        /**
         * 从单个日期创建全天时间范围 / Create a full-day time range from a single date
         *
         * @param date 本地日期 / The local date
         * @return 该日期的全天时间范围 / The full-day time range for the date
         */
        operator fun invoke(date: LocalDate): TimeRange {
            val start = date.atStartOfDayIn(TimeZone.currentSystemDefault())
            return TimeRange(
                start = start,
                end = start + 1.days
            )
        }

        /**
         * 从日期范围创建时间范围 / Create a time range from a date range
         *
         * @param fromDate 起始日期 / The start date
         * @param toDate 结束日期（含）/ The end date (inclusive)
         * @return 对应的时间范围 / The corresponding time range
         */
        operator fun invoke(fromDate: LocalDate, toDate: LocalDate): TimeRange {
            val start = fromDate.atStartOfDayIn(TimeZone.currentSystemDefault())
            val end = toDate.atStartOfDayIn(TimeZone.currentSystemDefault()) + 1.days
            return TimeRange(
                start = start,
                end = end
            )
        }
    }

    override val time: TimeRange get() = this
    /** 是否为空范围 / Whether this is an empty range */
    val empty: Boolean get() = start >= end
    override val duration: Duration get() = end - start

    /** 获取 start 之前的前置区间 / Get the front range before start */
    val front: TimeRange?
        get() = if (start != Instant.DISTANT_PAST) {
            TimeRange(start = Instant.DISTANT_PAST, start)
        } else {
            null
        }

    /**
     * 获取当前范围与另一范围之间的前置间隙 / Get the front gap between this range and another range
     *
     * @param ano 另一时间范围 / Another time range
     * @return 前置间隙，若不存在则为null / The front gap, or null if none exists
     */
    fun frontBetween(ano: TimeRange): TimeRange? {
        return if (ano.end < start) {
            TimeRange(start = ano.end, end = start)
        } else {
            null
        }
    }

    /** 获取 end 之后的后置区间 / Get the back range after end */
    val back: TimeRange?
        get() = if (end != Instant.DISTANT_FUTURE) {
            TimeRange(start = end, end = Instant.DISTANT_FUTURE)
        } else {
            null
        }

    /**
     * 获取当前范围与另一范围之间的后置间隙 / Get the back gap between this range and another range
     *
     * @param ano 另一时间范围 / Another time range
     * @return 后置间隙，若不存在则为null / The back gap, or null if none exists
     */
    fun backBetween(ano: TimeRange): TimeRange? {
        return if (ano.start > this.end) {
            TimeRange(start = end, end = ano.start)
        } else {
            null
        }
    }

    /**
     * 判断是否与另一时间范围有交集 / Check whether this range intersects with another range
     *
     * @param ano 另一时间范围 / Another time range
     * @return 是否有交集 / Whether there is an intersection
     */
    fun withIntersection(ano: TimeRange): Boolean {
        return start < ano.end && ano.start < end
    }

    /**
     * 计算与另一时间范围的交集 / Compute the intersection with another range
     *
     * @param ano 另一时间范围 / Another time range
     * @return 交集，若不相交则为null / The intersection, or null if no intersection
     */
    infix fun intersect(ano: TimeRange): TimeRange? {
        return intersectionWith(ano)
    }

    /**
     * 计算与另一时间范围的交集（乘法运算符）/ Compute the intersection with another range (times operator)
     *
     * @param ano 另一时间范围 / Another time range
     * @return 交集，若不相交则为null / The intersection, or null if no intersection
     */
    operator fun times(ano: TimeRange): TimeRange? {
        return intersectionWith(ano)
    }

    /**
     * 计算与另一时间范围的交集 / Compute the intersection with another range
     *
     * @param ano 另一时间范围 / Another time range
     * @return 交集，若不相交则为null / The intersection, or null if no intersection
     */
    fun intersectionWith(ano: TimeRange): TimeRange? {
        val maxBegin = max(start, ano.start)
        val minEnd = min(end, ano.end)
        return if (minEnd > maxBegin) {
            TimeRange(maxBegin, minEnd)
        } else {
            null
        }
    }

    /**
     * 计算与另一时间范围的差集 / Compute the difference with another range
     *
     * @param ano 另一时间范围 / Another time range
     * @return 差集结果列表 / The list of difference results
     */
    infix fun subtract(ano: TimeRange): List<TimeRange> {
        return differenceWith(ano)
    }

    /**
     * 计算与另一时间范围的差集（减法运算符）/ Compute the difference with another range (minus operator)
     *
     * @param ano 另一时间范围 / Another time range
     * @return 差集结果列表 / The list of difference results
     */
    operator fun minus(ano: TimeRange): List<TimeRange> {
        return differenceWith(ano)
    }

    /**
     * 计算与另一时间范围的差集 / Compute the difference with another range
     *
     * @param ano 另一时间范围 / Another time range
     * @return 差集结果列表 / The list of difference results
     */
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

    /**
     * 计算与多个时间范围列表的差集 / Compute the difference with a list of time ranges
     *
     * @param ano 时间范围列表 / The list of time ranges
     * @return 差集结果列表 / The list of difference results
     */
    fun differenceWith(ano: List<TimeRange>): List<TimeRange> {
        val intersections = ArrayList<TimeRange>(ano.size)
        for (time in ano) {
            if (!time.withIntersection(this)) {
                continue
            }
            intersections.add(
                TimeRange(
                    max(time.start, start),
                    min(time.end, end)
                )
            )
        }
        val mergedTimes = intersections.merge()
        if (mergedTimes.isEmpty()) {
            return listOf(this)
        }

        val result = ArrayList<TimeRange>()
        var currentTime = start
        for (mergedTime in mergedTimes) {
            if (currentTime < mergedTime.start) {
                result.add(
                    TimeRange(
                        currentTime,
                        mergedTime.start
                    )
                )
            }
            currentTime = mergedTime.end
        }
        if (currentTime < end) {
            result.add(
                TimeRange(
                    currentTime,
                    end
                )
            )
        }
        return result
    }

    /**
     * 判断是否包含指定时间点 / Check whether this range contains the specified instant
     *
     * @param time 时间点 / The instant
     * @return 是否包含 / Whether contained
     */
    operator fun contains(time: Instant): Boolean {
        return start <= time && time < end;
    }

    /**
     * 判断是否包含指定时间范围 / Check whether this range contains the specified time range
     *
     * @param time 时间范围 / The time range
     * @return 是否包含 / Whether contained
     */
    operator fun contains(time: TimeRange): Boolean {
        return start <= time.start && time.end <= end;
    }

    /**
     * 按指定时间点列表拆分时间范围 / Split the time range at the specified instants
     *
     * @param times 拆分时间点列表 / The list of split instants
     * @return 拆分后的时间范围列表 / The list of split time ranges
     */
    fun split(
        times: List<Instant>
    ): List<TimeRange> {
        val splitPoints = java.util.TreeSet<Instant>()
        for (time in times) {
            if (time != start && contains(time)) {
                splitPoints.add(time)
            }
        }
        if (splitPoints.isEmpty()) {
            return listOf(this)
        }
        val result = ArrayList<TimeRange>(splitPoints.size + 1)
        var current = start
        for (splitPoint in splitPoints) {
            result.add(TimeRange(current, splitPoint))
            current = splitPoint
        }
        if (current < end) {
            result.add(TimeRange(current, end))
        }
        return result
    }

    override fun subOf(subTime: TimeRange): TimeSlot? {
        return intersect(subTime)
    }

    /**
     * 拆分结果，包含工作时间段和休息时间段 / Split result containing working time ranges and break time ranges
     *
     * @property times 工作时间段列表 / The list of working time ranges
     * @property breakTimes 休息时间段列表 / The list of break time ranges
     */
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

    /**
     * 按持续时间单元拆分时间范围 / Split the time range by duration unit
     *
     * @param unit 持续时间单元范围 / The duration unit range
     * @param currentDuration 当前已消耗的持续时间 / The current consumed duration
     * @param maxDuration 最大持续时间限制 / The maximum duration limit
     * @param breakTime 休息时间 / The break time duration
     * @return 拆分结果 / The split result
     */
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
                    unit.lb - if (breakTime != null && currentTime == start) {
                        currentDuration
                    } else {
                        Duration.ZERO
                    },
                    end - currentTime,
                    maxDuration?.let { it - totalDuration } ?: Duration.INFINITE
                ).min()
                if (maxDuration != null
                    && ((duration + totalDuration + (unit.ub - unit.lb)) >= maxDuration || (currentTime + duration + (unit.ub - unit.lb)) >= end)
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
                    && duration + if (currentTime == start) {
                        currentDuration
                    } else {
                        Duration.ZERO
                    } == unit.lb
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

    /**
     * 反向按持续时间单元拆分时间范围 / Reverse split the time range by duration unit
     *
     * @param unit 持续时间单元范围 / The duration unit range
     * @param maxDuration 最大持续时间限制 / The maximum duration limit
     * @param breakTime 休息时间 / The break time duration
     * @return 拆分结果 / The split result
     */
    fun rsplit(
        unit: DurationRange,
        maxDuration: Duration? = null,
        breakTime: Duration? = null
    ): SplitTimeRanges {
        if (maxDuration == null || maxDuration <= duration) {
            return split(
                unit = unit,
                currentDuration = Duration.ZERO,
                maxDuration = maxDuration,
                breakTime = breakTime
            )
        }

        return unsupportedReverseSplit(maxDuration)
    }

    /**
     * 判断是否在另一时间范围之前连续 / Check whether this range is continuous before another range
     *
     * @param time 另一时间范围 / Another time range
     * @return 是否连续 / Whether continuous
     */
    fun continuousBefore(time: TimeRange): Boolean {
        return end == time.start
    }

    /**
     * 判断是否在另一时间范围之后连续 / Check whether this range is continuous after another range
     *
     * @param time 另一时间范围 / Another time range
     * @return 是否连续 / Whether continuous
     */
    fun continuousAfter(time: TimeRange): Boolean {
        return start == time.end
    }

    /**
     * 判断是否与另一时间范围连续 / Check whether this range is continuous with another range
     *
     * @param time 另一时间范围 / Another time range
     * @return 是否连续 / Whether continuous
     */
    fun continuousWith(time: TimeRange): Boolean {
        return continuousBefore(time) || continuousAfter(time)
    }

    /**
     * 时间范围平移（加法）/ Shift the time range (plus)
     *
     * @param rhs 偏移量 / The offset duration
     * @return 平移后的时间范围 / The shifted time range
     */
    operator fun plus(rhs: Duration): TimeRange {
        return TimeRange(start + rhs, end + rhs)
    }

    /**
     * 时间范围平移（减法）/ Shift the time range (minus)
     *
     * @param rhs 偏移量 / The offset duration
     * @return 平移后的时间范围 / The shifted time range
     */
    operator fun minus(rhs: Duration): TimeRange {
        return TimeRange(start - rhs, end - rhs)
    }
}

/**
 * 合并重叠或相邻的时间范围列表 / Merge overlapping or adjacent time ranges in the list
 *
 * @return 合并后的时间范围列表 / The merged list of time ranges
 */
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

/**
 * 获取指定索引处的前置间隙 / Get the front gap at the specified index
 *
 * @param i 索引 / The index
 * @return 前置间隙 / The front gap
 */
fun List<TimeRange>.frontAt(i: Int): TimeRange {
    return if (i == 0) {
        requireNotNull(this[i].front) {
            "frontAt($i) 失败：首个时间段的 start 为 DISTANT_PAST，不存在前置区间。"
        }
    } else {
        requireNotNull(this[i].frontBetween(this[i - 1])) {
            "frontAt($i) 失败：time[$i] 与 time[${i - 1}] 不存在可用前置间隙。"
        }
    }
}

/**
 * 获取指定索引处的后置间隙 / Get the back gap at the specified index
 *
 * @param i 索引 / The index
 * @return 后置间隙 / The back gap
 */
fun List<TimeRange>.backAt(i: Int): TimeRange {
    return if (i == this.lastIndex) {
        requireNotNull(this[i].back) {
            "backAt($i) 失败：末个时间段的 end 为 DISTANT_FUTURE，不存在后置区间。"
        }
    } else {
        requireNotNull(this[i].backBetween(this[i + 1])) {
            "backAt($i) 失败：time[$i] 与 time[${i + 1}] 不存在可用后置间隙。"
        }
    }
}

/**
 * 二分查找下界实现 / Binary search lower bound implementation
 *
 * @param T 列表元素类型 / The list element type
 * @param list 有序列表 / The sorted list
 * @param time 目标时间范围 / The target time range
 * @param extractor 时间范围提取器 / The time range extractor
 * @return 下界索引 / The lower bound index
 */
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

/**
 * 并行二分查找下界实现 / Parallel binary search lower bound implementation
 *
 * @param T 列表元素类型 / The list element type
 * @param list 有序列表 / The sorted list
 * @param time 目标时间范围 / The target time range
 * @param extractor 挂起时间范围提取器 / The suspend time range extractor
 * @return 下界索引 / The lower bound index
 */
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

/**
 * 二分查找上界实现 / Binary search upper bound implementation
 *
 * @param T 列表元素类型 / The list element type
 * @param list 有序列表 / The sorted list
 * @param time 目标时间范围 / The target time range
 * @param extractor 时间范围提取器 / The time range extractor
 * @return 上界索引 / The upper bound index
 */
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

/**
 * 并行二分查找上界实现 / Parallel binary search upper bound implementation
 *
 * @param T 列表元素类型 / The list element type
 * @param list 有序列表 / The sorted list
 * @param time 目标时间范围 / The target time range
 * @param extractor 挂起时间范围提取器 / The suspend time range extractor
 * @return 上界索引 / The upper bound index
 */
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

/**
 * 在列表中查找与给定时间范围相交的元素范围 / Find the range of elements intersecting with the given time range
 *
 * @param T 列表元素类型 / The list element type
 * @param time 目标时间范围 / The target time range
 * @param extractor 时间范围提取器 / The time range extractor
 * @return 下界和上界的配对，若无匹配则为null / Pair of lower and upper bounds, or null if no match
 */
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
        val lowerBound = _findLowerBoundImpl(
            list = this@findImpl,
            time = time,
            extractor = extractor
        )
        val upperBound = _findUpperBoundImpl(
            list = this@findImpl,
            time = time,
            extractor = extractor
        )
        Pair(lowerBound, upperBound)
    }
}

/**
 * 并行在列表中查找与给定时间范围相交的元素范围 / Parallel find the range of elements intersecting with the given time range
 *
 * @param T 列表元素类型 / The list element type
 * @param time 目标时间范围 / The target time range
 * @param extractor 挂起时间范围提取器 / The suspend time range extractor
 * @return 下界和上界的配对，若无匹配则为null / Pair of lower and upper bounds, or null if no match
 */
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
                _findLowerBoundParallellyImpl(
                    list = this@findParallellyImpl,
                    time = time,
                    extractor = extractor
                )
            }
            val upperBoundPromise = async(Dispatchers.Default) {
                _findUpperBoundParallellyImpl(
                    list = this@findParallellyImpl,
                    time = time,
                    extractor = extractor
                )
            }
            Pair(lowerBoundPromise.await(), upperBoundPromise.await())
        }
    }
}

/**
 * 从指定时间点开始查找相交元素范围 / Find the range of elements intersecting from the specified instant
 *
 * @param T 列表元素类型 / The list element type
 * @param time 起始时间点 / The start instant
 * @param extractor 时间范围提取器 / The time range extractor
 * @return 下界和上界的配对，若无匹配则为null / Pair of lower and upper bounds, or null if no match
 */
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

/**
 * 并行从指定时间点开始查找相交元素范围 / Parallel find the range of elements intersecting from the specified instant
 *
 * @param T 列表元素类型 / The list element type
 * @param time 起始时间点 / The start instant
 * @param extractor 挂起时间范围提取器 / The suspend time range extractor
 * @return 下界和上界的配对，若无匹配则为null / Pair of lower and upper bounds, or null if no match
 */
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

/**
 * 查找直到指定时间点的相交元素范围 / Find the range of elements intersecting until the specified instant
 *
 * @param T 列表元素类型 / The list element type
 * @param time 结束时间点 / The end instant
 * @param extractor 时间范围提取器 / The time range extractor
 * @return 下界和上界的配对，若无匹配则为null / Pair of lower and upper bounds, or null if no match
 */
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

/**
 * 并行查找直到指定时间点的相交元素范围 / Parallel find the range of elements intersecting until the specified instant
 *
 * @param T 列表元素类型 / The list element type
 * @param time 结束时间点 / The end instant
 * @param extractor 挂起时间范围提取器 / The suspend time range extractor
 * @return 下界和上界的配对，若无匹配则为null / Pair of lower and upper bounds, or null if no match
 */
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

/**
 * 查找与给定时间范围相交的时间范围 / Find time ranges intersecting with the given time range
 *
 * @param time 目标时间范围 / The target time range
 * @return 相交的时间范围列表 / The list of intersecting time ranges
 */
fun List<TimeRange>.find(
    time: TimeRange
): List<TimeRange> {
    return this.findImpl(time) { it }
        ?.let { this@find.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 并行查找与给定时间范围相交的时间范围 / Parallel find time ranges intersecting with the given time range
 *
 * @param time 目标时间范围 / The target time range
 * @return 相交的时间范围列表 / The list of intersecting time ranges
 */
suspend fun List<TimeRange>.findParallelly(
    time: TimeRange
): List<TimeRange> {
    return this.findParallellyImpl(time) { it }
        ?.let { this@findParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 从指定时间点开始查找时间范围 / Find time ranges from the specified instant
 *
 * @param time 起始时间点 / The start instant
 * @return 匹配的时间范围列表 / The list of matching time ranges
 */
fun List<TimeRange>.findFrom(
    time: Instant
): List<TimeRange> {
    return this.findFromImpl(time) { it }
        ?.let { this@findFrom.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 并行从指定时间点开始查找时间范围 / Parallel find time ranges from the specified instant
 *
 * @param time 起始时间点 / The start instant
 * @return 匹配的时间范围列表 / The list of matching time ranges
 */
suspend fun List<TimeRange>.findFromParallelly(
    time: Instant
): List<TimeRange> {
    return this.findFromParallellyImpl(time) { it }
        ?.let { this@findFromParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 查找直到指定时间点的时间范围 / Find time ranges until the specified instant
 *
 * @param time 结束时间点 / The end instant
 * @return 匹配的时间范围列表 / The list of matching time ranges
 */
fun List<TimeRange>.findUntil(
    time: Instant
): List<TimeRange> {
    return this.findUntilImpl(time) { it }
        ?.let { this@findUntil.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 并行查找直到指定时间点的时间范围 / Parallel find time ranges until the specified instant
 *
 * @param time 结束时间点 / The end instant
 * @return 匹配的时间范围列表 / The list of matching time ranges
 */
suspend fun List<TimeRange>.findUntilParallelly(
    time: Instant
): List<TimeRange> {
    return this.findUntilParallellyImpl(time) { it }
        ?.let { this@findUntilParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 使用属性引用查找与给定时间范围相交的元素 / Find elements intersecting with the given time range using property reference
 *
 * @param T 列表元素类型 / The list element type
 * @param time 目标时间范围 / The target time range
 * @param extractor 属性引用 / The property reference
 * @return 匹配的元素列表 / The list of matching elements
 */
fun <T> List<T>.find(
    time: TimeRange,
    extractor: KProperty1<T, TimeRange>
): List<T> {
    return this.findImpl(time) { extractor(it) }
        ?.let { this@find.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 并行使用属性引用查找与给定时间范围相交的元素 / Parallel find elements intersecting with the given time range using property reference
 *
 * @param T 列表元素类型 / The list element type
 * @param time 目标时间范围 / The target time range
 * @param extractor 属性引用 / The property reference
 * @return 匹配的元素列表 / The list of matching elements
 */
suspend fun <T> List<T>.findParallelly(
    time: TimeRange,
    extractor: KProperty1<T, TimeRange>
): List<T> {
    return this.findParallellyImpl(time) { extractor(it) }
        ?.let { this@findParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 使用属性引用从指定时间点开始查找元素 / Find elements from the specified instant using property reference
 *
 * @param T 列表元素类型 / The list element type
 * @param time 起始时间点 / The start instant
 * @param extractor 属性引用 / The property reference
 * @return 匹配的元素列表 / The list of matching elements
 */
fun <T> List<T>.findFrom(
    time: Instant,
    extractor: KProperty1<T, TimeRange>
): List<T> {
    return this.findFromImpl(time) { extractor(it) }
        ?.let { this@findFrom.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 并行使用属性引用从指定时间点开始查找元素 / Parallel find elements from the specified instant using property reference
 *
 * @param T 列表元素类型 / The list element type
 * @param time 起始时间点 / The start instant
 * @param extractor 属性引用 / The property reference
 * @return 匹配的元素列表 / The list of matching elements
 */
suspend fun <T> List<T>.findFromParallelly(
    time: Instant,
    extractor: KProperty1<T, TimeRange>
): List<T> {
    return this.findFromParallellyImpl(time) { extractor(it) }
        ?.let { this@findFromParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 使用属性引用查找直到指定时间点的元素 / Find elements until the specified instant using property reference
 *
 * @param T 列表元素类型 / The list element type
 * @param time 结束时间点 / The end instant
 * @param extractor 属性引用 / The property reference
 * @return 匹配的元素列表 / The list of matching elements
 */
fun <T> List<T>.findUntil(
    time: Instant,
    extractor: KProperty1<T, TimeRange>
): List<T> {
    return this.findUntilImpl(time) { extractor(it) }
        ?.let { this@findUntil.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 并行使用属性引用查找直到指定时间点的元素 / Parallel find elements until the specified instant using property reference
 *
 * @param T 列表元素类型 / The list element type
 * @param time 结束时间点 / The end instant
 * @param extractor 属性引用 / The property reference
 * @return 匹配的元素列表 / The list of matching elements
 */
suspend fun <T> List<T>.findUntilParallelly(
    time: Instant,
    extractor: KProperty1<T, TimeRange>
): List<T> {
    return this.findUntilParallellyImpl(time) { extractor(it) }
        ?.let { this@findUntilParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 使用函数查找与给定时间范围相交的元素 / Find elements intersecting with the given time range using function
 *
 * @param T 列表元素类型 / The list element type
 * @param time 目标时间范围 / The target time range
 * @param extractor 时间范围提取函数 / The time range extraction function
 * @return 匹配的元素列表 / The list of matching elements
 */
inline fun <T> List<T>.find(
    time: TimeRange,
    crossinline extractor: Extractor<TimeRange, T>
): List<T> {
    return this.findImpl(time) { extractor(it) }
        ?.let { this@find.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 并行使用函数查找与给定时间范围相交的元素 / Parallel find elements intersecting with the given time range using function
 *
 * @param T 列表元素类型 / The list element type
 * @param time 目标时间范围 / The target time range
 * @param extractor 挂起时间范围提取函数 / The suspend time range extraction function
 * @return 匹配的元素列表 / The list of matching elements
 */
suspend inline fun <T> List<T>.findParallelly(
    time: TimeRange,
    crossinline extractor: SuspendExtractor<TimeRange, T>
): List<T> {
    return this.findParallellyImpl(time) { extractor(it) }
        ?.let { this@findParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 使用函数从指定时间点开始查找元素 / Find elements from the specified instant using function
 *
 * @param T 列表元素类型 / The list element type
 * @param time 起始时间点 / The start instant
 * @param extractor 时间范围提取函数 / The time range extraction function
 * @return 匹配的元素列表 / The list of matching elements
 */
inline fun <T> List<T>.findFrom(
    time: Instant,
    crossinline extractor: Extractor<TimeRange, T>
): List<T> {
    return this.findFromImpl(time) { extractor(it) }
        ?.let { this@findFrom.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 并行使用函数从指定时间点开始查找元素 / Parallel find elements from the specified instant using function
 *
 * @param T 列表元素类型 / The list element type
 * @param time 起始时间点 / The start instant
 * @param extractor 挂起时间范围提取函数 / The suspend time range extraction function
 * @return 匹配的元素列表 / The list of matching elements
 */
suspend inline fun <T> List<T>.findFromParallelly(
    time: Instant,
    crossinline extractor: SuspendExtractor<TimeRange, T>
): List<T> {
    return this.findFromParallellyImpl(time) { extractor(it) }
        ?.let { this@findFromParallelly.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 使用函数查找直到指定时间点的元素 / Find elements until the specified instant using function
 *
 * @param T 列表元素类型 / The list element type
 * @param time 结束时间点 / The end instant
 * @param extractor 时间范围提取函数 / The time range extraction function
 * @return 匹配的元素列表 / The list of matching elements
 */
inline fun <T> List<T>.findUntil(
    time: Instant,
    crossinline extractor: Extractor<TimeRange, T>
): List<T> {
    return this.findUntilImpl(time) { extractor(it) }
        ?.let { this@findUntil.subList(it.first, it.second) }
        ?: emptyList()
}

/**
 * 并行使用函数查找直到指定时间点的元素 / Parallel find elements until the specified instant using function
 *
 * @param T 列表元素类型 / The list element type
 * @param time 结束时间点 / The end instant
 * @param extractor 挂起时间范围提取函数 / The suspend time range extraction function
 * @return 匹配的元素列表 / The list of matching elements
 */
suspend inline fun <T> List<T>.findUntilParallelly(
    time: Instant,
    crossinline extractor: SuspendExtractor<TimeRange, T>
): List<T> {
    return this.findUntilParallellyImpl(time) { extractor(it) }
        ?.let { this@findUntilParallelly.subList(it.first, it.second) }
        ?: emptyList()
}
