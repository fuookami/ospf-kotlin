@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.ordinary.min
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.utils.max
import fuookami.ospf.kotlin.utils.min

/** 日历时长物理量 / Calendar duration quantity */
typealias CalendarDurationQuantity<V> = Quantity<V>

/**
 * 工作日历，管理不可用时间和实际时间计算 / Working calendar managing unavailable times and actual time calculations
 *
 * @property timeWindow 时间窗口 / The time window
 * @param unavailableTimes 不可用时间列表 / The list of unavailable times
 */
open class WorkingCalendar<V : RealNumber<V>>(
    val timeWindow: TimeWindow<V>,
    unavailableTimes: List<TimeRange> = emptyList()
) {
    /**
     * 实际时间结果，包含工作时间、休息时间和连接时间 / Actual time result containing working times, break times, and connection times
     *
     * @property time 实际时间范围 / The actual time range
     * @property workingTimes 工作时间段列表 / The list of working time ranges
     * @property breakTimes 休息时间段列表 / The list of break time ranges
     * @property connectionTimes 连接时间段列表 / The list of connection time ranges
     */
    data class ActualTime(
        val time: TimeRange,
        val workingTimes: List<TimeRange>,
        val breakTimes: List<TimeRange>,
        val connectionTimes: List<TimeRange>
    ) {
        val duration: Duration get() = time.duration
        /** 工作时长 / Working duration */
        val workingDuration: Duration get() = workingTimes.fold(Duration.ZERO) { acc, time -> acc + time.duration }
        /** 休息时长 / Break duration */
        val breakDuration: Duration get() = breakTimes.fold(Duration.ZERO) { acc, time -> acc + time.duration }
        /** 连接时长 / Connection duration */
        val connectionDuration: Duration get() = connectionTimes.fold(Duration.ZERO) { acc, time -> acc + time.duration }
        val finishEnabled: Boolean get() = time.start != Instant.DISTANT_PAST && time.end != Instant.DISTANT_FUTURE

        /**
         * 实际总时长物理量 / Actual total duration quantity
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间窗口 / Time window
         * @param unit 时长单位 / Duration unit
         * @return 实际总时长物理量 / Actual total duration quantity
         */
        fun <V : RealNumber<V>> durationQuantity(
            timeWindow: TimeWindow<V>,
            unit: PhysicalUnit = NoneUnit
        ): CalendarDurationQuantity<V> {
            return timeWindow.quantityOf(duration, unit)
        }

        /**
         * 工作时长物理量 / Working duration quantity
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间窗口 / Time window
         * @param unit 时长单位 / Duration unit
         * @return 工作时长物理量 / Working duration quantity
         */
        fun <V : RealNumber<V>> workingDurationQuantity(
            timeWindow: TimeWindow<V>,
            unit: PhysicalUnit = NoneUnit
        ): CalendarDurationQuantity<V> {
            return timeWindow.quantityOf(workingDuration, unit)
        }

        /**
         * 休息时长物理量 / Break duration quantity
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间窗口 / Time window
         * @param unit 时长单位 / Duration unit
         * @return 休息时长物理量 / Break duration quantity
         */
        fun <V : RealNumber<V>> breakDurationQuantity(
            timeWindow: TimeWindow<V>,
            unit: PhysicalUnit = NoneUnit
        ): CalendarDurationQuantity<V> {
            return timeWindow.quantityOf(breakDuration, unit)
        }

        /**
         * 连接时长物理量 / Connection duration quantity
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间窗口 / Time window
         * @param unit 时长单位 / Duration unit
         * @return 连接时长物理量 / Connection duration quantity
         */
        fun <V : RealNumber<V>> connectionDurationQuantity(
            timeWindow: TimeWindow<V>,
            unit: PhysicalUnit = NoneUnit
        ): CalendarDurationQuantity<V> {
            return timeWindow.quantityOf(connectionDuration, unit)
        }

        infix fun eq(time: TimeRange): Boolean {
            return this.time == time
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ActualTime) return false

            if (time != other.time) return false
            if (!(workingTimes.toTypedArray() contentEquals other.workingTimes.toTypedArray())) return false
            if (!(breakTimes.toTypedArray() contentEquals other.breakTimes.toTypedArray())) return false
            if (!(connectionTimes.toTypedArray() contentEquals other.connectionTimes.toTypedArray())) return false

            return true
        }

        override fun hashCode(): Int {
            var result = time.hashCode()
            result = 31 * result + workingTimes.hashCode()
            result = 31 * result + breakTimes.hashCode()
            result = 31 * result + connectionTimes.hashCode()
            return result
        }
    }

    /**
     * 有效时间结果，包含时间段、休息时间和连接时间 / Valid times result containing time ranges, break times, and connection times
     *
     * @property times 有效时间段列表 / The list of valid time ranges
     * @property breakTimes 休息时间段列表 / The list of break time ranges
     * @property connectionTimes 连接时间段列表 / The list of connection time ranges
     */
    data class ValidTimes(
        val times: List<TimeRange>,
        val breakTimes: List<TimeRange>,
        val connectionTimes: List<TimeRange>
    ) {
        /** 有效总时长 / Total valid duration */
        val duration: Duration get() = times.fold(Duration.ZERO) { acc, time -> acc + time.duration }
        /** 休息总时长 / Total break duration */
        val breakDuration: Duration get() = breakTimes.fold(Duration.ZERO) { acc, time -> acc + time.duration }
        /** 连接总时长 / Total connection duration */
        val connectionDuration: Duration get() = connectionTimes.fold(Duration.ZERO) { acc, time -> acc + time.duration }

        /**
         * 有效总时长物理量 / Total valid duration quantity
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间窗口 / Time window
         * @param unit 时长单位 / Duration unit
         * @return 有效总时长物理量 / Total valid duration quantity
         */
        fun <V : RealNumber<V>> durationQuantity(
            timeWindow: TimeWindow<V>,
            unit: PhysicalUnit = NoneUnit
        ): CalendarDurationQuantity<V> {
            return timeWindow.quantityOf(duration, unit)
        }

        /**
         * 休息总时长物理量 / Total break duration quantity
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间窗口 / Time window
         * @param unit 时长单位 / Duration unit
         * @return 休息总时长物理量 / Total break duration quantity
         */
        fun <V : RealNumber<V>> breakDurationQuantity(
            timeWindow: TimeWindow<V>,
            unit: PhysicalUnit = NoneUnit
        ): CalendarDurationQuantity<V> {
            return timeWindow.quantityOf(breakDuration, unit)
        }

        /**
         * 连接总时长物理量 / Total connection duration quantity
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间窗口 / Time window
         * @param unit 时长单位 / Duration unit
         * @return 连接总时长物理量 / Total connection duration quantity
         */
        fun <V : RealNumber<V>> connectionDurationQuantity(
            timeWindow: TimeWindow<V>,
            unit: PhysicalUnit = NoneUnit
        ): CalendarDurationQuantity<V> {
            return timeWindow.quantityOf(connectionDuration, unit)
        }
    }

    companion object {
        /**
         * 查找在指定时间之前或之时结束的最后一个时间范围的索引 / Find the index of the last time range ended before or at the specified time
         *
         * @param times 时间范围列表 / The list of time ranges
         * @param time 目标时间 / The target time
         * @return 索引，若未找到则为-1 / The index, or -1 if not found
         */
        internal fun indexOfLastEndedBeforeOrAt(
            times: List<TimeRange>,
            time: Instant
        ): Int {
            for (i in times.lastIndex downTo 0) {
                if (time >= times[i].end) {
                    return i
                }
            }
            return -1
        }

        /**
         * 查找在指定时间之后或之时开始的第一个时间范围的索引 / Find the index of the first time range started after or at the specified time
         *
         * @param times 时间范围列表 / The list of time ranges
         * @param time 目标时间 / The target time
         * @return 索引，若未找到则为-1 / The index, or -1 if not found
         */
        internal fun indexOfFirstStartedAfterOrAt(
            times: List<TimeRange>,
            time: Instant
        ): Int {
            for (i in times.indices) {
                if (time <= times[i].start) {
                    return i
                }
            }
            return -1
        }

        /**
         * 计算所有时间范围的最大结束时间 / Calculate the maximum end time across all time ranges
         *
         * @param times 时间范围列表 / The list of time ranges
         * @param breakTimes 休息时间列表 / The list of break times
         * @param connectionTimes 连接时间列表 / The list of connection times
         * @return 最大结束时间 / The maximum end time
         */
        internal fun maxEndTime(
            times: List<TimeRange>,
            breakTimes: List<TimeRange>,
            connectionTimes: List<TimeRange>
        ): Instant {
            var maximum = Instant.DISTANT_PAST
            for (time in times) {
                if (time.end > maximum) {
                    maximum = time.end
                }
            }
            for (time in breakTimes) {
                if (time.end > maximum) {
                    maximum = time.end
                }
            }
            for (time in connectionTimes) {
                if (time.end > maximum) {
                    maximum = time.end
                }
            }
            return maximum
        }

        /**
         * 计算所有时间范围的最小开始时间 / Calculate the minimum start time across all time ranges
         *
         * @param times 时间范围列表 / The list of time ranges
         * @param breakTimes 休息时间列表 / The list of break times
         * @param connectionTimes 连接时间列表 / The list of connection times
         * @return 最小开始时间 / The minimum start time
         */
        internal fun minStartTime(
            times: List<TimeRange>,
            breakTimes: List<TimeRange>,
            connectionTimes: List<TimeRange>
        ): Instant {
            var minimum = Instant.DISTANT_FUTURE
            for (time in times) {
                if (time.start < minimum) {
                    minimum = time.start
                }
            }
            for (time in breakTimes) {
                if (time.start < minimum) {
                    minimum = time.start
                }
            }
            for (time in connectionTimes) {
                if (time.start < minimum) {
                    minimum = time.start
                }
            }
            return minimum
        }

        /**
         * 计算考虑不可用时间后的实际时间点 / Calculate the actual time point considering unavailable times
         *
         * @param time 目标时间点 / The target time point
         * @param unavailableTimes 不可用时间列表 / The list of unavailable times
         * @param beforeConnectionTime 前置连接时间 / The before connection time
         * @param afterConnectionTime 后置连接时间 / The after connection time
         * @param beforeConditionalConnectionTime 条件前置连接时间 / The conditional before connection time
         * @param afterConditionalConnectionTime 条件后置连接时间 / The conditional after connection time
         * @return 实际时间点 / The actual time point
         */
        protected fun actualTime(
            time: Instant,
            unavailableTimes: List<TimeRange> = emptyList(),
            beforeConnectionTime: DurationRange? = null,
            afterConnectionTime: DurationRange? = null,
            beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
            afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null
        ): Instant {
            val mergedTimes = unavailableTimes.merge()
            if (mergedTimes.isEmpty()) {
                return time
            }

            var currentTime = time
            for (thisUnavailableTime in mergedTimes) {
                if (time <= thisUnavailableTime.start - max(
                        beforeConditionalConnectionTime?.invoke(thisUnavailableTime)?.lb ?: Duration.ZERO,
                        beforeConnectionTime?.lb ?: Duration.ZERO
                    )
                ) {
                    return currentTime
                }

                currentTime = max(
                    currentTime,
                    thisUnavailableTime.end + max(
                        afterConditionalConnectionTime?.invoke(thisUnavailableTime)?.lb ?: Duration.ZERO,
                        afterConnectionTime?.lb ?: Duration.ZERO
                    )
                )
            }
            return currentTime
        }

        @JvmStatic
        @JvmName("staticActualTime")
        protected fun actualTime(
            time: TimeRange,
            unavailableTimes: List<TimeRange> = emptyList(),
            beforeConnectionTime: DurationRange? = null,
            afterConnectionTime: DurationRange? = null,
            beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
            afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
            currentDuration: Duration = Duration.ZERO,
            breakTime: Pair<DurationRange, Duration>? = null
        ): ActualTime {
            val mergedTimes = unavailableTimes.merge()
            return if (mergedTimes.isEmpty()) {
                if (breakTime != null) {
                    var currentTime = time.start
                    var totalDuration = Duration.ZERO
                    val workingTimes = ArrayList<TimeRange>()
                    val breakTimes = ArrayList<TimeRange>()
                    while (totalDuration != time.duration) {
                        val thisDuration = min(
                            breakTime.first.lb,
                            time.duration - totalDuration
                        )
                        workingTimes.add(
                            TimeRange(
                                start = currentTime,
                                end = currentTime + thisDuration
                            )
                        )
                        currentTime = if (thisDuration < breakTime.first.ub && (totalDuration + thisDuration) != time.duration) {
                            currentTime + thisDuration
                        } else {
                            breakTimes.add(
                                TimeRange(
                                    start = currentTime + thisDuration,
                                    end = currentTime + thisDuration + breakTime.second
                                )
                            )
                            currentTime + thisDuration + breakTime.second
                        }
                        totalDuration += thisDuration
                    }
                    ActualTime(
                        time = TimeRange(
                            start = time.start,
                            end = currentTime
                        ),
                        workingTimes = workingTimes,
                        breakTimes = breakTimes,
                        connectionTimes = emptyList()
                    )
                } else {
                    ActualTime(
                        time = time,
                        workingTimes = emptyList(),
                        breakTimes = emptyList(),
                        connectionTimes = emptyList()
                    )
                }
            } else {
                var currentTime = time.start
                var totalDuration = Duration.ZERO
                val workingTimes = ArrayList<TimeRange>()
                val breakTimes = ArrayList<TimeRange>()
                val connectionTimes = ArrayList<TimeRange>()
                var i = indexOfLastEndedBeforeOrAt(mergedTimes, currentTime)
                while (totalDuration != time.duration) {
                    if (i == mergedTimes.lastIndex && currentTime == Instant.DISTANT_FUTURE) {
                        break
                    } else if (i != mergedTimes.lastIndex && currentTime in mergedTimes[i + 1]) {
                        currentTime = mergedTimes[i + 1].end
                        i += 1
                        continue
                    }

                    val thisBeforeConnectionTime = if (i < mergedTimes.lastIndex && (beforeConnectionTime != null || beforeConditionalConnectionTime != null)) {
                        DurationRange(
                            max(
                                beforeConditionalConnectionTime?.invoke(mergedTimes[i + 1])?.lb ?: Duration.ZERO,
                                beforeConnectionTime?.lb ?: Duration.ZERO
                            ),
                            max(
                                beforeConditionalConnectionTime?.invoke(mergedTimes[i + 1])?.ub ?: Duration.ZERO,
                                beforeConnectionTime?.ub ?: Duration.ZERO
                            )
                        )
                    } else {
                        null
                    }
                    val thisAfterConnectionTime =
                        if (i != -1 && i != 0 && currentTime <= mergedTimes[i].end && (afterConnectionTime != null || afterConditionalConnectionTime != null)) {
                            DurationRange(
                                max(
                                    afterConditionalConnectionTime?.invoke(mergedTimes[i])?.lb ?: Duration.ZERO,
                                    afterConnectionTime?.lb ?: Duration.ZERO
                                ),
                                max(
                                    afterConditionalConnectionTime?.invoke(mergedTimes[i])?.ub ?: Duration.ZERO,
                                    afterConnectionTime?.ub ?: Duration.ZERO
                                )
                            )
                        } else {
                            null
                        }

                    val thisMaxDuration = if (i == -1 && thisBeforeConnectionTime != null) {
                        DurationRange(
                            mergedTimes.first().start - time.start - thisBeforeConnectionTime.ub,
                            mergedTimes.first().start - time.start - thisBeforeConnectionTime.lb
                        )
                    } else if (i != mergedTimes.lastIndex && (thisBeforeConnectionTime != null || thisAfterConnectionTime != null)) {
                        DurationRange(
                            mergedTimes[i + 1].start - mergedTimes[i].end - (thisBeforeConnectionTime?.ub ?: Duration.ZERO) - (thisAfterConnectionTime?.ub ?: Duration.ZERO),
                            mergedTimes[i + 1].start - mergedTimes[i].end - (thisBeforeConnectionTime?.lb ?: Duration.ZERO) - (thisAfterConnectionTime?.lb ?: Duration.ZERO)
                        )
                    } else {
                        null
                    }
                    val (thisActualBeforeConnectionTime, thisActualAfterConnectionTime) = if (thisMaxDuration != null
                        && (totalDuration + thisMaxDuration.lb) <= time.duration
                        && (totalDuration + thisMaxDuration.ub) >= time.duration
                    ) {
                        val restDuration = thisMaxDuration.ub - (time.duration - totalDuration)
                        if (thisBeforeConnectionTime != null && thisAfterConnectionTime != null) {
                            if (restDuration >= (thisBeforeConnectionTime.ub - thisBeforeConnectionTime.lb)) {
                                thisBeforeConnectionTime.ub to (thisAfterConnectionTime.lb + restDuration)
                            } else {
                                (thisBeforeConnectionTime.lb + restDuration) to thisAfterConnectionTime.lb
                            }
                        } else if (thisBeforeConnectionTime != null) {
                            (thisBeforeConnectionTime.lb + restDuration) to null
                        } else if (thisAfterConnectionTime != null) {
                            null to (thisAfterConnectionTime.lb + restDuration)
                        } else {
                            null to null
                        }
                    } else {
                        thisBeforeConnectionTime?.ub to thisAfterConnectionTime?.ub
                    }

                    currentTime = if (thisActualAfterConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = currentTime,
                                end = currentTime + thisActualAfterConnectionTime
                            )
                        )
                        currentTime + thisActualAfterConnectionTime
                    } else {
                        currentTime
                    }
                    val thisEndTime = if (i == mergedTimes.lastIndex) {
                        Instant.DISTANT_FUTURE
                    } else {
                        mergedTimes[i + 1].start - (thisActualBeforeConnectionTime ?: Duration.ZERO)
                    }
                    val baseTime = TimeRange(
                        start = currentTime,
                        end = thisEndTime
                    )
                    currentTime = if (breakTime != null) {
                        val offset = if (currentTime == time.start) {
                            currentDuration
                        } else {
                            Duration.ZERO
                        }
                        val (thisValidTimes, thisBreakTimes) = baseTime.split(
                            unit = breakTime.first,
                            currentDuration = offset,
                            maxDuration = time.duration - totalDuration,
                            breakTime = breakTime.second
                        )
                        for (validTime in thisValidTimes) {
                            totalDuration += validTime.duration
                        }
                        workingTimes.addAll(thisValidTimes)
                        breakTimes.addAll(thisBreakTimes)
                        maxEndTime(thisValidTimes, thisBreakTimes, emptyList())
                    } else {
                        val duration = min(
                            baseTime.duration,
                            time.duration - totalDuration
                        )
                        totalDuration += duration
                        workingTimes.add(
                            TimeRange(
                                start = currentTime,
                                end = currentTime + duration
                            )
                        )
                        currentTime + duration
                    }
                    if (totalDuration != time.duration && thisActualBeforeConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = thisEndTime,
                                end = thisEndTime + thisActualBeforeConnectionTime
                            )
                        )
                    } else if (totalDuration == time.duration) {
                        break
                    }
                    currentTime = mergedTimes[i + 1].end
                    i += 1
                }
                ActualTime(
                    time = TimeRange(
                        start = time.start,
                        end = currentTime
                    ),
                    workingTimes = workingTimes,
                    breakTimes = breakTimes,
                    connectionTimes = connectionTimes
                )
            }
        }

        @JvmStatic
        @JvmName("staticValidTime")
        protected fun validTimes(
            time: TimeRange,
            unavailableTimes: List<TimeRange> = emptyList(),
            beforeConnectionTime: DurationRange? = null,
            afterConnectionTime: DurationRange? = null,
            beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
            afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
            currentDuration: Duration = Duration.ZERO,
            maxDuration: Duration? = null,
            breakTime: Pair<DurationRange, Duration>? = null
        ): ValidTimes {
            val mergedTimes = unavailableTimes.merge()
            return if (mergedTimes.isEmpty()) {
                if (breakTime != null) {
                    val (validTimes, breakTimes) = time.split(
                        unit = breakTime.first,
                        maxDuration = maxDuration,
                        breakTime = breakTime.second
                    )
                    ValidTimes(
                        times = validTimes,
                        breakTimes = breakTimes,
                        connectionTimes = emptyList()
                    )
                } else if (maxDuration != null) {
                    ValidTimes(
                        times = listOf(
                            TimeRange(
                                start = time.start,
                                end = time.start + maxDuration
                            )
                        ),
                        breakTimes = emptyList(),
                        connectionTimes = emptyList()
                    )
                } else {
                    ValidTimes(
                        times = listOf(time),
                        breakTimes = emptyList(),
                        connectionTimes = emptyList()
                    )
                }
            } else {
                val validTimes = ArrayList<TimeRange>()
                val breakTimes = ArrayList<TimeRange>()
                val connectionTimes = ArrayList<TimeRange>()
                var currentTime = time.start
                var i = indexOfLastEndedBeforeOrAt(mergedTimes, currentTime)
                var totalDuration = Duration.ZERO
                while (currentTime < time.end) {
                    if (i == mergedTimes.lastIndex && currentTime == Instant.DISTANT_FUTURE) {
                        break
                    } else if (i != mergedTimes.lastIndex && mergedTimes[i + 1].contains(currentTime)) {
                        currentTime = mergedTimes[i + 1].end
                        i += 1
                        continue
                    }

                    val thisBeforeConnectionTime = if (i != mergedTimes.lastIndex
                        && (beforeConnectionTime != null || beforeConditionalConnectionTime != null)
                    ) {
                        DurationRange(
                            max(
                                beforeConditionalConnectionTime?.invoke(mergedTimes[i + 1])?.lb ?: Duration.ZERO,
                                beforeConnectionTime?.lb ?: Duration.ZERO
                            ),
                            max(
                                beforeConditionalConnectionTime?.invoke(mergedTimes[i + 1])?.ub ?: Duration.ZERO,
                                beforeConnectionTime?.ub ?: Duration.ZERO
                            )
                        )
                    } else {
                        null
                    }

                    val thisAfterConnectionTime = if (i != -1
                        && currentTime <= mergedTimes[i].end
                        && (afterConnectionTime != null || afterConditionalConnectionTime != null)
                    ) {
                        DurationRange(
                            max(
                                afterConditionalConnectionTime?.invoke(mergedTimes[i])?.lb ?: Duration.ZERO,
                                afterConnectionTime?.lb ?: Duration.ZERO
                            ),
                            max(
                                afterConditionalConnectionTime?.invoke(mergedTimes[i])?.ub ?: Duration.ZERO,
                                afterConnectionTime?.ub ?: Duration.ZERO
                            )
                        )
                    } else {
                        null
                    }

                    val thisMaxDuration = if (i == -1
                        && thisBeforeConnectionTime != null
                    ) {
                        DurationRange(
                            mergedTimes.first().start - time.start - thisBeforeConnectionTime.ub,
                            mergedTimes.first().start - time.start - thisBeforeConnectionTime.lb
                        )
                    } else if (i != mergedTimes.lastIndex
                        && (i == -1 || currentTime != mergedTimes[i].end)
                        && thisBeforeConnectionTime != null
                    ) {
                        DurationRange(
                            mergedTimes[i + 1].start
                                    - currentTime
                                    - thisBeforeConnectionTime.ub,
                            mergedTimes[i + 1].start
                                    - currentTime
                                    - thisBeforeConnectionTime.lb
                        )
                    } else if (i != -1
                        && i != mergedTimes.lastIndex
                        && currentTime == mergedTimes[i].end
                        && (thisBeforeConnectionTime != null || thisAfterConnectionTime != null)
                    ) {
                        DurationRange(
                            mergedTimes[i + 1].start
                                    - mergedTimes[i].end
                                    - (thisBeforeConnectionTime?.ub ?: Duration.ZERO)
                                    - (thisAfterConnectionTime?.ub ?: Duration.ZERO),
                            mergedTimes[i + 1].start
                                    - mergedTimes[i].end
                                    - (thisBeforeConnectionTime?.lb ?: Duration.ZERO)
                                    - (thisAfterConnectionTime?.lb ?: Duration.ZERO)
                        )
                    } else {
                        null
                    }

                    if (thisMaxDuration?.let { it.lb < Duration.ZERO } == true) {
                        if (i == -1) {
                            connectionTimes.add(
                                TimeRange(
                                    start = time.start,
                                    end = mergedTimes.first().start
                                )
                            )
                        } else if (currentTime != mergedTimes[i].end && thisBeforeConnectionTime != null) {
                            connectionTimes.add(
                                TimeRange(
                                    start = max(currentTime, mergedTimes[i + 1].start - thisBeforeConnectionTime.ub),
                                    end = mergedTimes[i + 1].start
                                )
                            )
                        } else if (currentTime == mergedTimes[i].end) {
                            if (thisBeforeConnectionTime != null && thisAfterConnectionTime != null) {
                                connectionTimes.add(
                                    TimeRange(
                                        start = mergedTimes[i].end,
                                        end = mergedTimes[i].end + thisAfterConnectionTime.lb
                                    )
                                )
                                connectionTimes.add(
                                    TimeRange(
                                        start = mergedTimes[i + 1].start - (mergedTimes[i + 1].start - mergedTimes[i].end - thisBeforeConnectionTime.lb),
                                        end = mergedTimes[i + 1].start
                                    )
                                )
                            } else {
                                connectionTimes.add(
                                    TimeRange(
                                        start = mergedTimes[i].end,
                                        end = mergedTimes[i + 1].start
                                    )
                                )
                            }
                        }
                        currentTime = mergedTimes[i + 1].end
                        i += 1
                        continue
                    }

                    val (thisActualBeforeConnectionTime, thisActualAfterConnectionTime) = if (maxDuration != null
                        && thisMaxDuration != null
                        && (totalDuration + thisMaxDuration.lb) <= maxDuration
                        && (totalDuration + thisMaxDuration.ub) >= maxDuration
                    ) {
                        val restDuration = thisMaxDuration.ub - (maxDuration - totalDuration)
                        if (thisBeforeConnectionTime != null && thisAfterConnectionTime != null) {
                            if (restDuration >= (thisBeforeConnectionTime.ub - thisBeforeConnectionTime.lb)) {
                                thisBeforeConnectionTime.ub to (thisAfterConnectionTime.lb + restDuration)
                            } else {
                                (thisBeforeConnectionTime.lb + restDuration) to thisAfterConnectionTime.lb
                            }
                        } else if (thisBeforeConnectionTime != null) {
                            (thisBeforeConnectionTime.lb + restDuration) to null
                        } else if (thisAfterConnectionTime != null) {
                            null to (thisAfterConnectionTime.lb + restDuration)
                        } else {
                            null to null
                        }
                    } else {
                        thisBeforeConnectionTime?.ub to thisAfterConnectionTime?.ub
                    }

                    currentTime = if (thisActualAfterConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = currentTime,
                                end = currentTime + thisActualAfterConnectionTime
                            )
                        )
                        currentTime + thisActualAfterConnectionTime
                    } else {
                        currentTime
                    }
                    val thisEndTime = if (i == mergedTimes.lastIndex) {
                        listOf(
                            time.end,
                            currentTime + (maxDuration ?: Duration.INFINITE) + if (maxDuration != null && breakTime != null) {
                                breakTime.second * ceil(maxDuration / breakTime.first.lb).toInt().toDouble()
                            } else {
                                Duration.ZERO
                            }
                        )
                    } else {
                        listOf(
                            time.end,
                            mergedTimes[i + 1].start - (thisActualBeforeConnectionTime ?: Duration.ZERO),
                            currentTime + (maxDuration ?: Duration.INFINITE) + if (maxDuration != null && breakTime != null) {
                                breakTime.second * ceil(maxDuration / breakTime.first.lb).toInt().toDouble()
                            } else {
                                Duration.ZERO
                            }
                        )
                    }.min()
                    val baseTime = TimeRange(
                        start = currentTime,
                        end = thisEndTime
                    )
                    if (breakTime != null) {
                        val offset = if (currentTime == time.start) {
                            currentDuration
                        } else {
                            Duration.ZERO
                        }
                        val (thisValidTimes, thisBreakTimes) = baseTime.split(
                            unit = breakTime.first,
                            currentDuration = offset,
                            maxDuration = maxDuration?.let { it - totalDuration },
                            breakTime = breakTime.second
                        )
                        for (validTime in thisValidTimes) {
                            totalDuration += validTime.duration
                        }
                        validTimes.addAll(thisValidTimes)
                        breakTimes.addAll(thisBreakTimes)
                    } else {
                        totalDuration += baseTime.duration
                        validTimes.add(baseTime)
                    }
                    if (thisEndTime != time.end && totalDuration != maxDuration && thisActualBeforeConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = thisEndTime,
                                end = thisEndTime + thisActualBeforeConnectionTime
                            )
                        )
                    } else if (thisEndTime == time.end || totalDuration == maxDuration) {
                        break
                    }
                    currentTime = mergedTimes[i + 1].end
                    i += 1
                }
                return ValidTimes(
                    times = validTimes,
                    breakTimes = breakTimes,
                    connectionTimes = connectionTimes
                )
            }
        }

        @JvmStatic
        @JvmName("staticRValidTime")
        protected fun reversedValidTimes(
            time: TimeRange,
            unavailableTimes: List<TimeRange> = emptyList(),
            beforeConnectionTime: DurationRange? = null,
            afterConnectionTime: DurationRange? = null,
            beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
            afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
            maxDuration: Duration? = null,
            breakTime: Pair<DurationRange, Duration>? = null
        ): ValidTimes {
            val mergedTimes = unavailableTimes.merge()
            return if (mergedTimes.isEmpty()) {
                if (breakTime != null) {
                    val (validTimes, breakTimes) = time.rsplit(
                        unit = breakTime.first,
                        maxDuration = maxDuration,
                        breakTime = breakTime.second
                    ).value!!
                    ValidTimes(
                        times = validTimes,
                        breakTimes = breakTimes,
                        connectionTimes = emptyList()
                    )
                } else if (maxDuration != null) {
                    ValidTimes(
                        times = listOf(
                            TimeRange(
                                start = time.end - maxDuration,
                                end = time.end
                            )
                        ),
                        breakTimes = emptyList(),
                        connectionTimes = emptyList()
                    )
                } else {
                    ValidTimes(
                        times = listOf(time),
                        breakTimes = emptyList(),
                        connectionTimes = emptyList()
                    )
                }
            } else {
                val validTimes = ArrayList<TimeRange>()
                val breakTimes = ArrayList<TimeRange>()
                val connectionTimes = ArrayList<TimeRange>()
                var currentTime = time.end
                var i = indexOfFirstStartedAfterOrAt(mergedTimes, currentTime)
                if (i == -1) {
                    i = mergedTimes.size
                }
                var totalDuration = Duration.ZERO
                while (currentTime > time.start) {
                    if (i == 0 && currentTime == Instant.DISTANT_PAST) {
                        break
                    } else if (i != 0 && mergedTimes[i - 1].contains(currentTime)) {
                        currentTime = mergedTimes[i - 1].start
                        i -= 1
                        continue
                    }

                    val thisBeforeConnectionTime = if (i != mergedTimes.size) {
                        DurationRange(
                            max(
                                beforeConditionalConnectionTime?.invoke(mergedTimes[i])?.lb ?: Duration.ZERO,
                                beforeConnectionTime?.lb ?: Duration.ZERO
                            ),
                            max(
                                beforeConditionalConnectionTime?.invoke(mergedTimes[i])?.ub ?: Duration.ZERO,
                                beforeConnectionTime?.ub ?: Duration.ZERO
                            )
                        )
                    } else {
                        null
                    }

                    val thisAfterConnectionTime = if (i != 0 && currentTime >= mergedTimes[i - 1].end) {
                        DurationRange(
                            max(
                                afterConditionalConnectionTime?.invoke(mergedTimes[i - 1])?.lb ?: Duration.ZERO,
                                afterConnectionTime?.lb ?: Duration.ZERO
                            ),
                            max(
                                afterConditionalConnectionTime?.invoke(mergedTimes[i - 1])?.ub ?: Duration.ZERO,
                                afterConnectionTime?.ub ?: Duration.ZERO
                            )
                        )
                    } else {
                        null
                    }

                    val thisMaxDuration = if (i == mergedTimes.size
                        && thisAfterConnectionTime != null
                    ) {
                        DurationRange(
                            time.end
                                    - mergedTimes.last().end
                                    - thisAfterConnectionTime.ub,
                            time.end
                                    - mergedTimes.last().end
                                    - thisAfterConnectionTime.lb
                        )
                    } else if (i != 0
                        && i != mergedTimes.size
                        && (thisBeforeConnectionTime != null || thisAfterConnectionTime != null)
                    ) {
                        DurationRange(
                            min(currentTime, mergedTimes[i].start)
                                    - mergedTimes[i - 1].end
                                    - (thisBeforeConnectionTime?.ub ?: Duration.ZERO)
                                    - (thisAfterConnectionTime?.ub ?: Duration.ZERO),
                            min(currentTime, mergedTimes[i].start)
                                    - mergedTimes[i - 1].end
                                    - (thisBeforeConnectionTime?.lb ?: Duration.ZERO)
                                    - (thisAfterConnectionTime?.lb ?: Duration.ZERO)
                        )
                    } else {
                        null
                    }

                    if (thisMaxDuration?.let { it.lb < Duration.ZERO } == true) {
                        if (i == mergedTimes.size) {
                            connectionTimes.add(
                                TimeRange(
                                    start = mergedTimes.last().end,
                                    end = time.end
                                )
                            )
                        } else if (currentTime != mergedTimes[i].start && thisBeforeConnectionTime != null) {
                            connectionTimes.add(
                                TimeRange(
                                    start = max(currentTime, mergedTimes[i].start - thisBeforeConnectionTime.ub),
                                    end = mergedTimes[i].start
                                )
                            )
                        } else if (i != 0 && currentTime == mergedTimes[i].start) {
                            if (thisBeforeConnectionTime != null && thisAfterConnectionTime != null) {
                                connectionTimes.add(
                                    TimeRange(
                                        start = mergedTimes[i - 1].end,
                                        end = mergedTimes[i - 1].end + thisAfterConnectionTime.lb
                                    )
                                )
                                connectionTimes.add(
                                    TimeRange(
                                        start = mergedTimes[i].start - (min(currentTime, mergedTimes[i].start) - mergedTimes[i - 1].end - thisAfterConnectionTime.lb),
                                        end = mergedTimes[i].start
                                    )
                                )
                            } else {
                                connectionTimes.add(
                                    TimeRange(
                                        start = mergedTimes[i - 1].end,
                                        end = min(currentTime, mergedTimes[i].start)
                                    )
                                )
                            }
                        }
                        currentTime = mergedTimes[i - 1].start
                        i -= 1
                        continue
                    }

                    val (thisActualBeforeConnectionTime, thisActualAfterConnectionTime) = if (maxDuration != null
                        && thisMaxDuration != null
                        && (totalDuration + thisMaxDuration.lb) <= maxDuration
                        && (totalDuration + thisMaxDuration.ub) >= maxDuration
                    ) {
                        val restDuration = thisMaxDuration.ub - (maxDuration - totalDuration)
                        if (thisBeforeConnectionTime != null && thisAfterConnectionTime != null) {
                            if (restDuration >= (thisBeforeConnectionTime.ub - thisBeforeConnectionTime.lb)) {
                                thisBeforeConnectionTime.ub to (thisAfterConnectionTime.lb + restDuration)
                            } else {
                                (thisBeforeConnectionTime.lb + restDuration) to thisAfterConnectionTime.lb
                            }
                        } else if (thisBeforeConnectionTime != null) {
                            (thisBeforeConnectionTime.lb + restDuration) to null
                        } else {
                            null to restDuration
                        }
                    } else {
                        thisBeforeConnectionTime?.ub to thisAfterConnectionTime?.ub
                    }

                    currentTime = if (thisActualBeforeConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = currentTime - thisActualBeforeConnectionTime,
                                end = currentTime
                            )
                        )
                        currentTime - thisActualBeforeConnectionTime
                    } else {
                        currentTime
                    }
                    val thisStartTime = if (i == 0) {
                        listOf(
                            time.start,
                            currentTime - (maxDuration ?: Duration.INFINITE) - if (maxDuration != null && breakTime != null) {
                                breakTime.second * ceil(maxDuration / breakTime.first.lb).toInt().toDouble()
                            } else {
                                Duration.ZERO
                            }
                        )
                    } else {
                        listOf(
                            time.start,
                            mergedTimes[i - 1].end + (thisActualAfterConnectionTime ?: Duration.ZERO),
                            currentTime - (maxDuration ?: Duration.INFINITE) - if (maxDuration != null && breakTime != null) {
                                breakTime.second * ceil(maxDuration / breakTime.first.lb).toInt().toDouble()
                            } else {
                                Duration.ZERO
                            }
                        )
                    }.max()
                    val baseTime = TimeRange(
                        start = thisStartTime,
                        end = currentTime
                    )
                    if (breakTime != null) {
                        val (thisValidTimes, thisBreakTimes) = baseTime.rsplit(
                            unit = breakTime.first,
                            maxDuration = maxDuration?.let { it - totalDuration },
                            breakTime = breakTime.second
                        ).value!!
                        for (validTime in thisValidTimes) {
                            totalDuration += validTime.duration
                        }
                        validTimes.addAll(thisValidTimes)
                        breakTimes.addAll(thisBreakTimes)
                    } else {
                        totalDuration += baseTime.duration
                        validTimes.add(baseTime)
                    }

                    if (thisStartTime != time.start && totalDuration != maxDuration && thisActualAfterConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = thisStartTime - thisActualAfterConnectionTime,
                                end = thisStartTime
                            )
                        )
                    } else if (thisStartTime == time.start || totalDuration == maxDuration) {
                        break
                    }
                    currentTime = mergedTimes[i - 1].start
                    i -= 1
                }
                return ValidTimes(
                    times = validTimes,
                    breakTimes = breakTimes,
                    connectionTimes = connectionTimes
                )
            }
        }
    }

    /** 排序后的不可用时间列表 / Sorted list of unavailable times */
    open val unavailableTimes = unavailableTimes.sortedBy { it.start }

    /**
     * 计算考虑不可用时间后的实际时间点 / Calculate the actual time point considering unavailable times
     *
     * @param time 目标时间点 / The target time point
     * @param unavailableTimes 额外的不可用时间列表 / Additional list of unavailable times
     * @param beforeConnectionTime 前置连接时间 / The before connection time
     * @param afterConnectionTime 后置连接时间 / The after connection time
     * @param beforeConditionalConnectionTime 条件前置连接时间 / The conditional before connection time
     * @param afterConditionalConnectionTime 条件后置连接时间 / The conditional after connection time
     * @return 实际时间点 / The actual time point
     */
    fun actualTime(
        time: Instant,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null
    ): Instant {
        return WorkingCalendar.actualTime(
            time = time,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime
        )
    }

    /**
     * 计算考虑不可用时间后的实际时间范围 / Calculate the actual time range considering unavailable times
     *
     * @param time 目标时间范围 / The target time range
     * @param unavailableTimes 额外的不可用时间列表 / Additional list of unavailable times
     * @param beforeConnectionTime 前置连接时间 / The before connection time
     * @param afterConnectionTime 后置连接时间 / The after connection time
     * @param beforeConditionalConnectionTime 条件前置连接时间 / The conditional before connection time
     * @param afterConditionalConnectionTime 条件后置连接时间 / The conditional after connection time
     * @param currentDuration 当前已消耗的持续时间 / The current consumed duration
     * @param breakTime 休息时间配对 / The break time pair
     * @return 实际时间结果 / The actual time result
     */
    fun actualTime(
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        return WorkingCalendar.actualTime(
            time = time,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    /**
     * 计算有效时间范围 / Calculate valid time ranges
     *
     * @param time 目标时间范围 / The target time range
     * @param unavailableTimes 额外的不可用时间列表 / Additional list of unavailable times
     * @param beforeConnectionTime 前置连接时间 / The before connection time
     * @param afterConnectionTime 后置连接时间 / The after connection time
     * @param beforeConditionalConnectionTime 条件前置连接时间 / The conditional before connection time
     * @param afterConditionalConnectionTime 条件后置连接时间 / The conditional after connection time
     * @param currentDuration 当前已消耗的持续时间 / The current consumed duration
     * @param breakTime 休息时间配对 / The break time pair
     * @return 有效时间结果 / The valid times result
     */
    fun validTimes(
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ValidTimes {
        return WorkingCalendar.validTimes(
            time = time,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }
}

/**
 * 生产力条件类型别名 / Productivity condition type alias
 *
 * @param T 条件参数类型 / The condition parameter type
 */
typealias ProductivityCondition<T> = (T) -> Boolean

/**
 * 生产力，描述时间窗口内的生产能力 / Productivity describing production capacity within a time window
 *
 * @param Q 产量类型 / The quantity type
 * @param T 材料类型 / The material type
 * @param U 键类型 / The key type
 * @property timeWindow 时间窗口 / The time window
 * @property extractor 键提取器 / The key extractor
 * @property weekDays 生效的星期几集合 / The set of applicable weekdays
 * @property monthDays 生效的月份天数集合 / The set of applicable month days
 * @property capacities 材料到生产单位所需时间的映射 / Mapping from material to time required per unit
 * @property unitYields 材料到单位时间产量的映射 / Mapping from material to unit time production
 * @property conditionCapacities 条件产能列表 / The list of conditional capacities
 * @property conditionUnitYields 条件单位产量列表 / The list of conditional unit yields
 */
open class Productivity<Q, T, U>(
    val timeWindow: TimeRange,
    val extractor: Extractor<U, T>,
    val weekDays: Set<DayOfWeek> = emptySet(),
    val monthDays: Set<Int> = emptySet(),
    // time to produce unit
    val capacities: Map<U, Duration>,
    // unit time production
    val unitYields: Map<U, Q>,
    // time to produce unit
    val conditionCapacities: List<Pair<ProductivityCondition<T>, Duration>> = emptyList(),
    // unit time production
    val conditionUnitYields: List<Pair<ProductivityCondition<T>, Q>> = emptyList()
) {
    companion object {
        @JvmName("buildByCapacityAndUnitYieldWithoutExtractor")
        operator fun <Q, T> invoke(
            timeWindow: TimeRange,
            weekDays: Set<DayOfWeek> = emptySet(),
            monthDays: Set<Int> = emptySet(),
            capacities: Map<T, Duration>,
            unitYields: Map<T, Q>,
            conditionCapacities: List<Pair<ProductivityCondition<T>, Duration>> = emptyList(),
            conditionUnitYields: List<Pair<ProductivityCondition<T>, Q>> = emptyList()
        ): Productivity<Q, T, T> {
            return Productivity(
                timeWindow = timeWindow,
                extractor = { it },
                weekDays = weekDays,
                monthDays = monthDays,
                capacities = capacities,
                unitYields = unitYields,
                conditionCapacities = conditionCapacities,
                conditionUnitYields = conditionUnitYields
            )
        }

        @JvmName("buildByCapacityWithoutExtractor")
        operator fun <Q, T> invoke(
            timeWindow: TimeRange,
            weekDays: Set<DayOfWeek> = emptySet(),
            monthDays: Set<Int> = emptySet(),
            capacities: Map<T, Duration>,
            conditionCapacities: List<Pair<ProductivityCondition<T>, Duration>> = emptyList()
        ): Productivity<Q, T, T> {
            return Productivity(
                timeWindow = timeWindow,
                extractor = { it },
                weekDays = weekDays,
                monthDays = monthDays,
                capacities = capacities,
                unitYields = emptyMap(),
                conditionCapacities = conditionCapacities,
                conditionUnitYields = emptyList()
            )
        }

        @JvmName("buildByCapacityWithExtractor")
        operator fun <Q, T, U> invoke(
            timeWindow: TimeRange,
            extractor: Extractor<U, T>,
            weekDays: Set<DayOfWeek> = emptySet(),
            monthDays: Set<Int> = emptySet(),
            capacities: Map<U, Duration>,
            conditionCapacities: List<Pair<ProductivityCondition<T>, Duration>> = emptyList()
        ): Productivity<Q, T, U> {
            return Productivity(
                timeWindow = timeWindow,
                extractor = extractor,
                weekDays = weekDays,
                monthDays = monthDays,
                capacities = capacities,
                unitYields = emptyMap(),
                conditionCapacities = conditionCapacities,
                conditionUnitYields = emptyList()
            )
        }

        @JvmName("buildByUnitYieldWithoutExtractor")
        operator fun <Q, T> invoke(
            timeWindow: TimeRange,
            weekDays: Set<DayOfWeek> = emptySet(),
            monthDays: Set<Int> = emptySet(),
            unitYields: Map<T, Q>,
            conditionUnitYields: List<Pair<ProductivityCondition<T>, Q>> = emptyList()
        ): Productivity<Q, T, T> {
            return Productivity(
                timeWindow = timeWindow,
                extractor = { it },
                weekDays = weekDays,
                monthDays = monthDays,
                capacities = emptyMap(),
                unitYields = unitYields,
                conditionCapacities = emptyList(),
                conditionUnitYields = conditionUnitYields
            )
        }

        @JvmName("buildByUnitYieldWithExtractor")
        operator fun <Q, T, U> invoke(
            timeWindow: TimeRange,
            extractor: Extractor<U, T>,
            weekDays: Set<DayOfWeek> = emptySet(),
            monthDays: Set<Int> = emptySet(),
            unitYields: Map<U, Q>,
            conditionUnitYields: List<Pair<ProductivityCondition<T>, Q>> = emptyList()
        ): Productivity<Q, T, U> {
            return Productivity(
                timeWindow = timeWindow,
                extractor = extractor,
                weekDays = weekDays,
                monthDays = monthDays,
                capacities = emptyMap(),
                unitYields = unitYields,
                conditionCapacities = emptyList(),
                conditionUnitYields = conditionUnitYields
            )
        }
    }

    private val cache1 = HashMap<T, Duration?>()
    private val cache2 = HashMap<T, Q?>()

    /**
     * 获取指定材料的产能 / Get the capacity for the specified material
     *
     * @param material 材料 / The material
     * @return 产能，若无则为null / The capacity, or null if none
     */
    open fun capacityOf(material: T): Duration? {
        return capacities[extractor(material)]
            ?: cache1.getOrPut(material) {
                conditionCapacities.firstOrNull { it.first(material) }?.second
            }
    }

    /**
     * 获取指定材料的单位产量 / Get the unit yield for the specified material
     *
     * @param material 材料 / The material
     * @return 单位产量，若无则为null / The unit yield, or null if none
     */
    open fun unitYieldOf(material: T): Q? {
        return unitYields[extractor(material)]
            ?: cache2.getOrPut(material) {
                conditionUnitYields.firstOrNull { it.first(material) }?.second
            }
    }

    /**
     * 创建新的生产力实例 / Create a new productivity instance
     *
     * @param timeWindow 时间窗口 / The time window
     * @param extractor 键提取器 / The key extractor
     * @param weekDays 星期几集合 / The set of weekdays
     * @param monthDays 月份天数集合 / The set of month days
     * @param capacities 材料到产能的映射 / The capacities mapping
     * @param unitYields 材料到单位产量的映射 / The unit yields mapping
     * @param conditionCapacities 条件产能列表 / The conditional capacities list
     * @param conditionUnitYields 条件单位产量列表 / The conditional unit yields list
     * @return 新的生产力实例 / The new productivity instance
     */
    open fun new(
        timeWindow: TimeRange? = null,
        extractor: Extractor<U, T>? = null,
        weekDays: Set<DayOfWeek>? = null,
        monthDays: Set<Int>? = null,
        capacities: Map<U, Duration>? = null,
        unitYields: Map<U, Q>? = null,
        conditionCapacities: List<Pair<ProductivityCondition<T>, Duration>>? = null,
        conditionUnitYields: List<Pair<ProductivityCondition<T>, Q>>? = null
    ): Productivity<Q, T, U> {
        return Productivity(
            timeWindow = timeWindow ?: this.timeWindow,
            extractor = extractor ?: this.extractor,
            weekDays = weekDays ?: this.weekDays,
            monthDays = monthDays ?: this.monthDays,
            capacities = capacities ?: this.capacities,
            unitYields = unitYields ?: this.unitYields,
            conditionCapacities = conditionCapacities ?: this.conditionCapacities,
            conditionUnitYields = conditionUnitYields ?: this.conditionUnitYields,
        )
    }
}

/**
 * 生产力日历，结合工作日历和生产力信息 / Productivity calendar combining working calendar and productivity information
 *
 * @param Q 产量类型 / The quantity type
 * @param P 生产力类型 / The productivity type
 * @param T 材料类型 / The material type
 * @param U 键类型 / The key type
 * @param timeWindow 时间窗口 / The time window
 * @param productivity 生产力列表 / The list of productivities
 * @param unavailableTimes 不可用时间列表 / The list of unavailable times
 * @param constants 数值常量 / The numeric constants
 * @param mul 乘法运算 / The multiplication operation
 * @param div 除法运算 / The division operation
 * @param floor 向下取整运算 / The floor operation
 */
sealed class ProductivityCalendar<W, Q, P, T, U>(
    timeWindow: TimeWindow<W>,
    productivity: List<P>,
    unavailableTimes: List<TimeRange>? = null,
    private val constants: RealNumberConstants<Q>,
    private val mul: (TimeWindow<W>, Q, Duration) -> Q,
    private val div: (TimeWindow<W>, Q, Duration) -> Q,
    private val floor: Extractor<Q, Flt64>
) : WorkingCalendar<W>(timeWindow) where W : RealNumber<W>, P : Productivity<Q, T, U>, Q : RealNumber<Q>, Q : PlusGroup<Q>, Q : TimesGroup<Q> {
    /**
     * 在拆分时间窗时恢复同构建路径的具体 Productivity 子类型。
     * 日历中的 productivity 实例由同一 P 类型构造路径产生，cast 仅恢复泛型擦除隐藏的具体类型。
     *
     * Restores the concrete Productivity subtype when splitting time windows.
     * Productivity instances in this calendar are produced by the same P-generic construction path,
     * and this cast only restores the concrete type hidden by generic erasure.
     */
    @Suppress("UNCHECKED_CAST")
    private fun rebuiltProductivityOf(source: P, timeWindow: TimeRange): P {
        return source.new(timeWindow = timeWindow) as P
    }

    private fun calendarValueOf(quantity: Q): Flt64 = quantity.toFlt64()

    private fun calendarValueOf(duration: Duration): Flt64 = timeWindow.valueOf(duration).toFlt64()

    private fun calendarDurationOf(value: Flt64): Duration {
        return timeWindow.durationOf(timeWindow.fromDouble(value.toDouble()))
    }

    private fun calendarCeilDurationOf(value: Flt64): Duration {
        return timeWindow.ceil(calendarDurationOf(value))
    }

    private fun productivityRateOf(
        productivity: Productivity<Q, T, U>,
        material: T
    ): Flt64? {
        return productivity.unitYieldOf(material)?.let { calendarValueOf(it) }
            ?: productivity.capacityOf(material)
                ?.let { Flt64.one / calendarValueOf(it) }
    }

    /** 生产力列表（已处理不可用时间）/ Productivity list (with unavailable times processed) */
    val productivity: List<P> by lazy {
        if (unavailableTimes != null) {
            productivity.flatMap {
                val timeRanges = it.timeWindow.differenceWith(unavailableTimes)
                timeRanges.map { time ->
                    rebuiltProductivityOf(it, time)
                }
            }.sortedBy { it.timeWindow.start }
        } else {
            productivity.sortedBy { it.timeWindow.start }
        }
    }

    /** 平均产能映射 / Average capacity mapping */
    val averageCapacity: Map<U, Duration> by lazy {
        val materials = productivity.flatMap { it.capacities.keys }.distinct()
        materials.associateWith { material ->
            val thisProductivity = productivity.mapNotNull {
                it.capacities[material]?.let { capacity ->
                    it.timeWindow.duration to capacity
                }
            }
            val totalWeighted = thisProductivity.fold(Flt64.zero) { acc, (duration, capacity) ->
                acc + calendarValueOf(duration) * calendarValueOf(capacity)
            }
            val totalDuration = thisProductivity.fold(Flt64.zero) { acc, (duration, _) ->
                acc + calendarValueOf(duration)
            }
            calendarDurationOf(totalWeighted / totalDuration)
        }
    }

    /** 平均单位产量映射 / Average unit yield mapping */
    val averageUnitYield: Map<U, Q> by lazy {
        val materials = productivity.flatMap { it.unitYields.keys }.distinct()
        materials.associateWith { material ->
            val thisProductivity = productivity.mapNotNull {
                it.unitYields[material]?.let { capacity ->
                    it.timeWindow.duration to capacity
                }
            }
            div(
                timeWindow,
                thisProductivity.fold(constants.zero) { lhs, rhs -> lhs + mul(timeWindow, rhs.second, rhs.first) },
                thisProductivity.fold(Duration.ZERO) { lhs, rhs -> lhs + rhs.first }
            )
        }
    }

    override val unavailableTimes: List<TimeRange> by lazy {
        unavailableTimes
            ?: productivity.flatMapIndexed { i, produceTime ->
                val result = ArrayList<TimeRange>()
                if (i == 0) {
                    produceTime.timeWindow.front?.let { result.add(it) }
                } else {
                    produceTime.timeWindow.frontBetween(productivity[i - 1].timeWindow)?.let { result.add(it) }
                }
                if (i == productivity.lastIndex) {
                    produceTime.timeWindow.back?.let { result.add(it) }
                }
                result
            }
    }

    fun actualStartTimeFrom(
        material: T,
        startTime: Instant,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): Instant {
        val productivityCalendar = productivity.findFrom(startTime, Productivity<Q, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return Instant.DISTANT_FUTURE
        }

        var currentTime = startTime
        for (calendar in productivityCalendar) {
            val currentProductivity = productivityRateOf(calendar, material) ?: continue

            val validTimes = validTimes(
                time = calendar.timeWindow.intersectionWith(TimeRange(start = currentTime)) ?: continue,
                unavailableTimes = unavailableTimes,
                beforeConnectionTime = beforeConnectionTime,
                afterConnectionTime = afterConnectionTime,
                beforeConditionalConnectionTime = beforeConditionalConnectionTime,
                afterConditionalConnectionTime = afterConditionalConnectionTime,
                currentDuration = if (currentTime == startTime) {
                    currentDuration
                } else {
                    Duration.ZERO
                },
                maxDuration = Duration.INFINITE,
                breakTime = breakTime
            )
            for (produceTime in validTimes.times) {
                val thisQuantity = calendarValueOf(produceTime.duration) * currentProductivity
                if (thisQuantity gr calendarValueOf(constants.zero)) {
                    return produceTime.start
                }
            }
            currentTime = maxEndTime(
                validTimes.times,
                validTimes.breakTimes,
                validTimes.connectionTimes
            )
        }

        return Instant.DISTANT_FUTURE
    }

    fun actualTimeFrom(
        material: T,
        startTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        val productivityCalendar = productivity.findFrom(startTime, Productivity<Q, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return ActualTime(
                time = TimeRange(
                    start = startTime,
                    end = Instant.DISTANT_FUTURE
                ),
                workingTimes = emptyList(),
                breakTimes = emptyList(),
                connectionTimes = emptyList()
            )
        }

        return actualTimeFrom(
            material = material,
            startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    fun actualTimeFromOrNull(
        material: T,
        startTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime? {
        val productivityCalendar = productivity.findFrom(startTime, Productivity<Q, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return null
        }

        return actualTimeFrom(
            material = material,
            startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    suspend fun actualTimeFromParallelly(
        material: T,
        startTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        val productivityCalendar = productivity.findFromParallelly(startTime, Productivity<Q, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return ActualTime(
                time = TimeRange(
                    start = startTime,
                    end = Instant.DISTANT_FUTURE
                ),
                workingTimes = emptyList(),
                breakTimes = emptyList(),
                connectionTimes = emptyList()
            )
        }

        return actualTimeFrom(
            material = material,
            startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    suspend fun actualTimeFromOrNullParallelly(
        material: T,
        startTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime? {
        val productivityCalendar = productivity.findFromParallelly(startTime, Productivity<Q, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return null
        }

        return actualTimeFrom(
            material = material,
            startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    fun actualTimeUntil(
        material: T,
        endTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        val productivityCalendar = productivity.findUntil(endTime, Productivity<Q, T, U>::timeWindow).reversed()
        if (productivityCalendar.isEmpty()) {
            return ActualTime(
                time = TimeRange(
                    start = Instant.DISTANT_PAST,
                    end = endTime
                ),
                workingTimes = emptyList(),
                breakTimes = emptyList(),
                connectionTimes = emptyList()
            )
        }

        return actualTimeUntil(
            material = material,
            endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    fun actualTimeUntilOrNull(
        material: T,
        endTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime? {
        val productivityCalendar = productivity.findUntil(endTime, Productivity<Q, T, U>::timeWindow).reversed()
        if (productivityCalendar.isEmpty()) {
            return null
        }

        return actualTimeUntil(
            material = material,
            endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    suspend fun actualTimeUntilParallelly(
        material: T,
        endTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        val productivityCalendar = productivity.findUntilParallelly(endTime, Productivity<Q, T, U>::timeWindow).reversed()
        if (productivityCalendar.isEmpty()) {
            return ActualTime(
                time = TimeRange(
                    start = Instant.DISTANT_PAST,
                    end = endTime
                ),
                workingTimes = emptyList(),
                breakTimes = emptyList(),
                connectionTimes = emptyList()
            )
        }

        return actualTimeUntil(
            material = material,
            endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    suspend fun actualTimeUntilOrNullParallelly(
        material: T,
        endTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime? {
        val productivityCalendar = productivity.findUntilParallelly(endTime, Productivity<Q, T, U>::timeWindow).reversed()
        if (productivityCalendar.isEmpty()) {
            return null
        }

        return actualTimeUntil(
            material = material,
            endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    fun actualQuantity(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): Q {
        val productivityCalendar = productivity.find(time, Productivity<Q, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return constants.zero
        }

        return actualQuantity(
            material = material,
            time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    fun actualQuantityOrNull(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): Q? {
        val productivityCalendar = productivity.find(time, Productivity<Q, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return null
        }

        return actualQuantity(
            material = material,
            time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    suspend fun actualQuantityParallelly(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): Q {
        val productivityCalendar = productivity.findParallelly(time, Productivity<Q, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return constants.zero
        }

        return actualQuantity(
            material = material,
            time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    suspend fun actualQuantityOrNullParallelly(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): Q? {
        val productivityCalendar = productivity.findParallelly(time, Productivity<Q, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return null
        }

        return actualQuantity(
            material = material,
            time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    private fun actualTimeFrom(
        material: T,
        startTime: Instant,
        productivityCalendar: List<Productivity<Q, T, U>>,
        quantity: Q,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        var produceQuantity = Flt64.zero
        var currentTime = startTime
        val workingTimes = ArrayList<TimeRange>()
        val breakTimes = ArrayList<TimeRange>()
        val connectionTimes = ArrayList<TimeRange>()
        val quantityValue = calendarValueOf(quantity)
        for (calendar in productivityCalendar) {
            val currentProductivity = productivityRateOf(calendar, material) ?: continue
            val maxDuration = calendarCeilDurationOf((quantityValue - produceQuantity) / currentProductivity)

            val validTimes = validTimes(
                time = calendar.timeWindow.intersectionWith(TimeRange(start = currentTime)) ?: continue,
                unavailableTimes = unavailableTimes,
                beforeConnectionTime = beforeConnectionTime,
                afterConnectionTime = afterConnectionTime,
                beforeConditionalConnectionTime = beforeConditionalConnectionTime,
                afterConditionalConnectionTime = afterConditionalConnectionTime,
                currentDuration = if (currentTime == startTime) {
                    currentDuration
                } else {
                    Duration.ZERO
                },
                maxDuration = maxDuration,
                breakTime = breakTime
            )
            workingTimes.addAll(validTimes.times)
            breakTimes.addAll(validTimes.breakTimes)
            connectionTimes.addAll(validTimes.connectionTimes)
            for (produceTime in validTimes.times) {
                val thisQuantity = calendarValueOf(produceTime.duration) * currentProductivity
                produceQuantity += min(
                    quantityValue - produceQuantity,
                    thisQuantity
                )
            }
            currentTime = maxEndTime(
                validTimes.times,
                validTimes.breakTimes,
                validTimes.connectionTimes
            )

            if (produceQuantity eq quantityValue) {
                break
            }
        }

        return if (produceQuantity eq quantityValue) {
            ActualTime(
                time = TimeRange(
                    start = startTime,
                    end = currentTime
                ),
                workingTimes = workingTimes,
                breakTimes = breakTimes,
                connectionTimes = connectionTimes
            )
        } else {
            ActualTime(
                time = TimeRange(
                    start = startTime,
                    end = Instant.DISTANT_FUTURE
                ),
                workingTimes = workingTimes,
                breakTimes = breakTimes,
                connectionTimes = connectionTimes
            )
        }
    }

    private fun actualTimeUntil(
        material: T,
        endTime: Instant,
        productivityCalendar: List<Productivity<Q, T, U>>,
        quantity: Q,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        var produceQuantity = Flt64.zero
        var currentTime = endTime
        val workingTimes = ArrayList<TimeRange>()
        val breakTimes = ArrayList<TimeRange>()
        val connectionTimes = ArrayList<TimeRange>()
        val quantityValue = calendarValueOf(quantity)
        for (calendar in productivityCalendar) {
            val currentProductivity = productivityRateOf(calendar, material) ?: continue
            val maxDuration = calendarCeilDurationOf((quantityValue - produceQuantity) / currentProductivity)

            val validTimes = reversedValidTimes(
                time = calendar.timeWindow.intersectionWith(TimeRange(end = currentTime)) ?: continue,
                unavailableTimes = unavailableTimes,
                beforeConnectionTime = beforeConnectionTime,
                afterConnectionTime = afterConnectionTime,
                beforeConditionalConnectionTime = beforeConditionalConnectionTime,
                afterConditionalConnectionTime = afterConditionalConnectionTime,
                maxDuration = maxDuration,
                breakTime = breakTime
            )
            workingTimes.addAll(validTimes.times)
            breakTimes.addAll(validTimes.breakTimes)
            connectionTimes.addAll(validTimes.connectionTimes)
            for (produceTime in validTimes.times) {
                val thisQuantity = calendarValueOf(produceTime.duration) * currentProductivity
                produceQuantity += min(
                    quantityValue - produceQuantity,
                    thisQuantity
                )
            }
            currentTime = minStartTime(
                validTimes.times,
                validTimes.breakTimes,
                validTimes.connectionTimes
            )

            if (produceQuantity eq quantityValue) {
                break
            }
        }

        return if (produceQuantity eq quantityValue) {
            ActualTime(
                time = TimeRange(
                    start = currentTime,
                    end = endTime
                ),
                workingTimes = workingTimes,
                breakTimes = breakTimes,
                connectionTimes = connectionTimes
            )
        } else {
            ActualTime(
                time = TimeRange(
                    start = Instant.DISTANT_PAST,
                    end = endTime
                ),
                workingTimes = workingTimes,
                breakTimes = breakTimes,
                connectionTimes = connectionTimes
            )
        }
    }

    private fun actualQuantity(
        material: T,
        time: TimeRange,
        productivityCalendar: List<Productivity<Q, T, U>>,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): Q {
        val validTimes = validTimes(
            time = time,
            unavailableTimes = unavailableTimes,
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration,
            breakTime = breakTime
        ).times

        var quantity = constants.zero
        for (calendar in productivityCalendar) {
            for (validTime in validTimes) {
                if (validTime.end <= calendar.timeWindow.start) {
                    continue
                } else if (validTime.start >= calendar.timeWindow.end) {
                    break
                }

                val produceTime = validTime.intersectionWith(calendar.timeWindow)?.duration ?: continue
                val currentProductivity = productivityRateOf(calendar, material) ?: Flt64.zero
                quantity += floor(calendarValueOf(produceTime) * currentProductivity)
            }
        }
        return quantity
    }
}

private fun UInt64.calendarValueOf(): Flt64 = Flt64(toLong().toDouble())

/**
 * 离散生产力日历，使用 UInt64 作为产量类型 / Discrete productivity calendar using UInt64 as quantity type
 *
 * @param P 生产力类型 / The productivity type
 * @param T 材料类型 / The material type
 * @param U 键类型 / The key type
 * @param timeWindow 时间窗口 / The time window
 * @param productivity 生产力列表 / The list of productivities
 * @param unavailableTimes 不可用时间列表 / The list of unavailable times
 */
open class DiscreteProductivityCalendar<W, P, T, U>(
    timeWindow: TimeWindow<W>,
    productivity: List<P>,
    unavailableTimes: List<TimeRange>? = null
) : ProductivityCalendar<W, UInt64, P, T, U>(
    timeWindow = timeWindow,
    productivity = productivity,
    unavailableTimes = unavailableTimes,
    constants = UInt64,
    mul = { timeWindow, quantity, duration ->
        (quantity.calendarValueOf() * timeWindow.valueOf(duration).toFlt64()).floor().toUInt64()
    },
    div = { timeWindow, quantity, duration ->
        (quantity.calendarValueOf() / timeWindow.valueOf(duration).toFlt64()).floor().toUInt64()
    },
    floor = { it.floor().toUInt64() }
) where W : RealNumber<W>, P : Productivity<UInt64, T, U> {
    companion object {
        /**
         * 通过泛型时间窗口创建离散生产力日历，并集中转换到日历数值边界 /
         * Create a discrete productivity calendar from a generic time window and centralize conversion to the calendar numeric boundary
         *
         * @param V 时间窗口数值类型 / The time-window numeric type
         * @param P 生产力类型 / The productivity type
         * @param T 材料类型 / The material type
         * @param U 键类型 / The key type
         * @param timeWindow 时间窗口 / The time window
         * @param productivity 生产力列表 / The list of productivities
         * @param unavailableTimes 不可用时间列表 / The list of unavailable times
         * @return 离散生产力日历 / The discrete productivity calendar
         */
        operator fun <V : RealNumber<V>, P, T, U> invoke(
            timeWindow: TimeWindow<V>,
            productivity: List<P>,
            unavailableTimes: List<TimeRange>? = null
        ): DiscreteProductivityCalendar<V, P, T, U> where P : Productivity<UInt64, T, U> {
            return DiscreteProductivityCalendar(
                timeWindow = timeWindow,
                productivity = productivity,
                unavailableTimes = unavailableTimes
            )
        }
    }
}

// ============================================================================
// Quantity-based Productivity API (G2.5)
// 基于物理量的生产力 API，为产出数量提供 Quantity<V> 单位语义
// ============================================================================

/**
 * 基于物理量的生产力，描述时间窗口内的生产能力，产出数量携带单位 /
 * Quantity-based productivity describing production capacity within a time window,
 * with production quantities carrying physical units.
 *
 * @param V 数值类型 / The numeric value type
 * @param T 材料类型 / The material type
 * @param U 键类型 / The key type
 * @property timeWindow 时间窗口 / The time window
 * @property extractor 键提取器 / The key extractor
 * @property weekDays 生效的星期几集合 / The set of applicable weekdays
 * @property monthDays 生效的月份天数集合 / The set of applicable month days
 * @property capacities 材料到生产单位所需时间的映射 / Mapping from material to time required per unit
 * @property unitYields 材料到单位时间产量的映射（携带单位）/ Mapping from material to unit time production (with unit)
 * @property conditionCapacities 条件产能列表 / The list of conditional capacities
 * @property conditionUnitYields 条件单位产量列表（携带单位）/ The list of conditional unit yields (with unit)
 */
open class QuantityProductivity<V, T, U>(
    val timeWindow: TimeRange,
    val extractor: Extractor<U, T>,
    val weekDays: Set<DayOfWeek> = emptySet(),
    val monthDays: Set<Int> = emptySet(),
    val capacities: Map<U, Duration>,
    val unitYields: Map<U, Quantity<V>>,
    val conditionCapacities: List<Pair<ProductivityCondition<T>, Duration>> = emptyList(),
    val conditionUnitYields: List<Pair<ProductivityCondition<T>, Quantity<V>>> = emptyList()
) {
    companion object {
        @JvmName("buildByCapacityAndUnitYieldWithoutExtractor")
        operator fun <V, T> invoke(
            timeWindow: TimeRange,
            weekDays: Set<DayOfWeek> = emptySet(),
            monthDays: Set<Int> = emptySet(),
            capacities: Map<T, Duration>,
            unitYields: Map<T, Quantity<V>>,
            conditionCapacities: List<Pair<ProductivityCondition<T>, Duration>> = emptyList(),
            conditionUnitYields: List<Pair<ProductivityCondition<T>, Quantity<V>>> = emptyList()
        ): QuantityProductivity<V, T, T> {
            return QuantityProductivity(
                timeWindow = timeWindow,
                extractor = { it },
                weekDays = weekDays,
                monthDays = monthDays,
                capacities = capacities,
                unitYields = unitYields,
                conditionCapacities = conditionCapacities,
                conditionUnitYields = conditionUnitYields
            )
        }

        @JvmName("buildByCapacityWithoutExtractor")
        operator fun <V, T> invoke(
            timeWindow: TimeRange,
            weekDays: Set<DayOfWeek> = emptySet(),
            monthDays: Set<Int> = emptySet(),
            capacities: Map<T, Duration>,
            conditionCapacities: List<Pair<ProductivityCondition<T>, Duration>> = emptyList()
        ): QuantityProductivity<V, T, T> {
            return QuantityProductivity(
                timeWindow = timeWindow,
                extractor = { it },
                weekDays = weekDays,
                monthDays = monthDays,
                capacities = capacities,
                unitYields = emptyMap(),
                conditionCapacities = conditionCapacities,
                conditionUnitYields = emptyList()
            )
        }

        @JvmName("buildByCapacityWithExtractor")
        operator fun <V, T, U> invoke(
            timeWindow: TimeRange,
            extractor: Extractor<U, T>,
            weekDays: Set<DayOfWeek> = emptySet(),
            monthDays: Set<Int> = emptySet(),
            capacities: Map<U, Duration>,
            conditionCapacities: List<Pair<ProductivityCondition<T>, Duration>> = emptyList()
        ): QuantityProductivity<V, T, U> {
            return QuantityProductivity(
                timeWindow = timeWindow,
                extractor = extractor,
                weekDays = weekDays,
                monthDays = monthDays,
                capacities = capacities,
                unitYields = emptyMap(),
                conditionCapacities = conditionCapacities,
                conditionUnitYields = emptyList()
            )
        }

        @JvmName("buildByUnitYieldWithoutExtractor")
        operator fun <V, T> invoke(
            timeWindow: TimeRange,
            weekDays: Set<DayOfWeek> = emptySet(),
            monthDays: Set<Int> = emptySet(),
            unitYields: Map<T, Quantity<V>>,
            conditionUnitYields: List<Pair<ProductivityCondition<T>, Quantity<V>>> = emptyList()
        ): QuantityProductivity<V, T, T> {
            return QuantityProductivity(
                timeWindow = timeWindow,
                extractor = { it },
                weekDays = weekDays,
                monthDays = monthDays,
                capacities = emptyMap(),
                unitYields = unitYields,
                conditionCapacities = emptyList(),
                conditionUnitYields = conditionUnitYields
            )
        }

        @JvmName("buildByUnitYieldWithExtractor")
        operator fun <V, T, U> invoke(
            timeWindow: TimeRange,
            extractor: Extractor<U, T>,
            weekDays: Set<DayOfWeek> = emptySet(),
            monthDays: Set<Int> = emptySet(),
            unitYields: Map<U, Quantity<V>>,
            conditionUnitYields: List<Pair<ProductivityCondition<T>, Quantity<V>>> = emptyList()
        ): QuantityProductivity<V, T, U> {
            return QuantityProductivity(
                timeWindow = timeWindow,
                extractor = extractor,
                weekDays = weekDays,
                monthDays = monthDays,
                capacities = emptyMap(),
                unitYields = unitYields,
                conditionCapacities = emptyList(),
                conditionUnitYields = conditionUnitYields
            )
        }
    }

    private val cache1 = HashMap<T, Duration?>()
    private val cache2 = HashMap<T, Quantity<V>?>()

    /**
     * 获取指定材料的产能 / Get the capacity for the specified material
     */
    open fun capacityOf(material: T): Duration? {
        return capacities[extractor(material)]
            ?: cache1.getOrPut(material) {
                conditionCapacities.firstOrNull { it.first(material) }?.second
            }
    }

    /**
     * 获取指定材料的单位产量（携带单位）/ Get the unit yield for the specified material (with unit)
     */
    open fun unitYieldOf(material: T): Quantity<V>? {
        return unitYields[extractor(material)]
            ?: cache2.getOrPut(material) {
                conditionUnitYields.firstOrNull { it.first(material) }?.second
            }
    }

    /**
     * 创建新的生产力实例 / Create a new productivity instance
     */
    open fun new(
        timeWindow: TimeRange? = null,
        extractor: Extractor<U, T>? = null,
        weekDays: Set<DayOfWeek>? = null,
        monthDays: Set<Int>? = null,
        capacities: Map<U, Duration>? = null,
        unitYields: Map<U, Quantity<V>>? = null,
        conditionCapacities: List<Pair<ProductivityCondition<T>, Duration>>? = null,
        conditionUnitYields: List<Pair<ProductivityCondition<T>, Quantity<V>>>? = null
    ): QuantityProductivity<V, T, U> {
        return QuantityProductivity(
            timeWindow = timeWindow ?: this.timeWindow,
            extractor = extractor ?: this.extractor,
            weekDays = weekDays ?: this.weekDays,
            monthDays = monthDays ?: this.monthDays,
            capacities = capacities ?: this.capacities,
            unitYields = unitYields ?: this.unitYields,
            conditionCapacities = conditionCapacities ?: this.conditionCapacities,
            conditionUnitYields = conditionUnitYields ?: this.conditionUnitYields,
        )
    }
}

/**
 * 基于物理量的生产力日历，结合工作日历和生产力信息 /
 * Quantity-based productivity calendar combining working calendar and productivity information.
 *
 * 与 [ProductivityCalendar] 的区别在于产出数量携带 [Quantity] 单位，
 * 支持物理量纲检查和单位转换。内部计算逻辑不变，仅在 API 边界包装/解包 [Quantity]。
 *
 * @param V 数值类型 / The numeric value type
 * @param P 生产力类型 / The productivity type
 * @param T 材料类型 / The material type
 * @param U 键类型 / The key type
 * @param timeWindow 时间窗口 / The time window
 * @param productivity 生产力列表 / The list of productivities
 * @param unavailableTimes 不可用时间列表 / The list of unavailable times
 * @param quantityUnit 产出数量的默认单位 / The default unit for production quantities
 * @param constants 数值常量 / The numeric constants
 * @param quantityFloor 物理量向下取整运算 / The floor operation for quantities
 */
sealed class QuantityProductivityCalendar<W, V, P, T, U>(
    timeWindow: TimeWindow<W>,
    productivity: List<P>,
    unavailableTimes: List<TimeRange>? = null,
    private val quantityUnit: PhysicalUnit,
    private val constants: RealNumberConstants<V>,
    private val quantityFloor: (Flt64) -> Quantity<V>
) : WorkingCalendar<W>(timeWindow) where W : RealNumber<W>, P : QuantityProductivity<V, T, U>, V : RealNumber<V>, V : PlusGroup<V>, V : TimesGroup<V> {

    /**
     * 按材料解析产出数量的期望单位。
     * Resolves the expected quantity unit for a material.
     *
     * 只检查指定材料在候选生产力条目中的 unitYield；若没有显式 unitYield，则回退到日历级 quantityUnit。
     * Checks only the material's unitYield in candidate productivity entries; falls back to calendar-level quantityUnit when absent.
     *
     * @param material 材料 / The material
     * @param productivityCalendar 候选生产力条目 / The candidate productivity entries
     * @return 解析出的产出单位 / The resolved production unit
     * @throws IllegalArgumentException 当同一材料存在多个 unitYield 单位时 / When the same material has multiple unitYield units
     */
    private fun resolveQuantityUnit(
        material: T,
        productivityCalendar: List<QuantityProductivity<V, T, U>> = productivity
    ): PhysicalUnit {
        val units = productivityCalendar.mapNotNull {
            it.unitYieldOf(material)?.unit
        }.distinct()
        require(units.size <= 1) {
            "Inconsistent unitYield units for material '$material': $units"
        }
        return units.firstOrNull() ?: quantityUnit
    }

    /**
     * 校验输入数量的单位是否与指定材料的产出单位一致。
     * Validates that the input quantity's unit matches the material's production unit.
     *
     * @param material 材料 / The material
     * @param productivityCalendar 候选生产力条目 / The candidate productivity entries
     * @param quantity 输入数量 / The input quantity
     * @throws IllegalArgumentException 当单位不一致时 / When units don't match
     */
    private fun validateQuantityUnit(
        material: T,
        productivityCalendar: List<QuantityProductivity<V, T, U>>,
        quantity: Quantity<V>
    ) {
        val expected = resolveQuantityUnit(
            material = material,
            productivityCalendar = productivityCalendar
        )
        require(quantity.unit == expected) {
            "Quantity unit '${quantity.unit}' does not match productivity unit '$expected'"
        }
    }

    private fun calendarValueOf(quantity: Quantity<V>): Flt64 = quantity.value.toFlt64()

    private fun calendarValueOf(value: V): Flt64 = value.toFlt64()

    private fun calendarValueOf(duration: Duration): Flt64 = timeWindow.valueOf(duration).toFlt64()

    private fun calendarDurationOf(value: Flt64): Duration {
        return timeWindow.durationOf(timeWindow.fromDouble(value.toDouble()))
    }

    private fun calendarCeilDurationOf(value: Flt64): Duration {
        return timeWindow.ceil(calendarDurationOf(value))
    }

    private fun productivityRateOf(
        productivity: QuantityProductivity<V, T, U>,
        material: T
    ): Flt64? {
        return productivity.unitYieldOf(material)?.let { calendarValueOf(it) }
            ?: productivity.capacityOf(material)
                ?.let { Flt64.one / calendarValueOf(it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun rebuiltProductivityOf(source: P, timeWindow: TimeRange): P {
        return source.new(timeWindow = timeWindow) as P
    }

    /** 生产力列表（已处理不可用时间）/ Productivity list (with unavailable times processed) */
    val productivity: List<P> by lazy {
        if (unavailableTimes != null) {
            productivity.flatMap {
                val timeRanges = it.timeWindow.differenceWith(unavailableTimes)
                timeRanges.map { time ->
                    rebuiltProductivityOf(it, time)
                }
            }.sortedBy { it.timeWindow.start }
        } else {
            productivity.sortedBy { it.timeWindow.start }
        }
    }

    /** 平均产能映射 / Average capacity mapping */
    val averageCapacity: Map<U, Duration> by lazy {
        val materials = productivity.flatMap { it.capacities.keys }.distinct()
        materials.associateWith { material ->
            val thisProductivity = productivity.mapNotNull {
                it.capacities[material]?.let { capacity ->
                    it.timeWindow.duration to capacity
                }
            }
            val totalWeighted = thisProductivity.fold(Flt64.zero) { acc, (duration, capacity) ->
                acc + calendarValueOf(duration) * calendarValueOf(capacity)
            }
            val totalDuration = thisProductivity.fold(Flt64.zero) { acc, (duration, _) ->
                acc + calendarValueOf(duration)
            }
            calendarDurationOf(totalWeighted / totalDuration)
        }
    }

    /** 平均单位产量映射（携带单位）/ Average unit yield mapping (with unit) */
    val averageUnitYield: Map<U, Quantity<V>> by lazy {
        val materials = productivity.flatMap { it.unitYields.keys }.distinct()
        materials.associateWith { material ->
            val entries = productivity.mapNotNull {
                it.unitYields[material]?.let { qty ->
                    it.timeWindow.duration to qty
                }
            }
            if (entries.isEmpty()) {
                Quantity(constants.zero, quantityUnit)
            } else {
                // 校验所有条目的单位一致 / Validate all entries share the same unit
                val units = entries.map { it.second.unit }.distinct()
                require(units.size == 1) {
                    "Inconsistent unitYield units for material '$material': $units"
                }
                val yieldUnit = units.single()
                val totalWeighted = entries.fold(Flt64.zero) { acc, (dur, qty) ->
                    acc + calendarValueOf(qty) * calendarValueOf(dur)
                }
                val totalDuration = entries.fold(Duration.ZERO) { acc, (dur, _) -> acc + dur }
                val avgValue = totalWeighted / calendarValueOf(totalDuration)
                val floored = quantityFloor(avgValue)
                Quantity(floored.value, yieldUnit)
            }
        }
    }

    override val unavailableTimes: List<TimeRange> by lazy {
        unavailableTimes
            ?: productivity.flatMapIndexed { i, produceTime ->
                val result = ArrayList<TimeRange>()
                if (i == 0) {
                    produceTime.timeWindow.front?.let { result.add(it) }
                } else {
                    produceTime.timeWindow.frontBetween(productivity[i - 1].timeWindow)?.let { result.add(it) }
                }
                if (i == productivity.lastIndex) {
                    produceTime.timeWindow.back?.let { result.add(it) }
                }
                result
            }
    }

    fun actualStartTimeFrom(
        material: T,
        startTime: Instant,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): Instant {
        val productivityCalendar = productivity.findFrom(startTime, QuantityProductivity<V, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return Instant.DISTANT_FUTURE
        }

        var currentTime = startTime
        for (calendar in productivityCalendar) {
            val currentProductivity = productivityRateOf(calendar, material) ?: continue

            val validTimes = validTimes(
                time = calendar.timeWindow.intersectionWith(TimeRange(start = currentTime)) ?: continue,
                unavailableTimes = unavailableTimes,
                beforeConnectionTime = beforeConnectionTime,
                afterConnectionTime = afterConnectionTime,
                beforeConditionalConnectionTime = beforeConditionalConnectionTime,
                afterConditionalConnectionTime = afterConditionalConnectionTime,
                currentDuration = if (currentTime == startTime) {
                    currentDuration
                } else {
                    Duration.ZERO
                },
                maxDuration = Duration.INFINITE,
                breakTime = breakTime
            )
            for (produceTime in validTimes.times) {
                val thisQuantity = calendarValueOf(produceTime.duration) * currentProductivity
                if (thisQuantity gr calendarValueOf(constants.zero)) {
                    return produceTime.start
                }
            }
            currentTime = maxEndTime(
                validTimes.times,
                validTimes.breakTimes,
                validTimes.connectionTimes
            )
        }

        return Instant.DISTANT_FUTURE
    }

    fun actualTimeFrom(
        material: T,
        startTime: Instant,
        quantity: Quantity<V> = Quantity(constants.one, quantityUnit),
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        val productivityCalendar = productivity.findFrom(startTime, QuantityProductivity<V, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return ActualTime(
                time = TimeRange(start = startTime, end = Instant.DISTANT_FUTURE),
                workingTimes = emptyList(),
                breakTimes = emptyList(),
                connectionTimes = emptyList()
            )
        }
        return actualTimeFromInternal(
            material = material, startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration, breakTime = breakTime
        )
    }

    fun actualTimeFromOrNull(
        material: T,
        startTime: Instant,
        quantity: Quantity<V> = Quantity(constants.one, quantityUnit),
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime? {
        val productivityCalendar = productivity.findFrom(startTime, QuantityProductivity<V, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) return null
        return actualTimeFromInternal(
            material = material, startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration, breakTime = breakTime
        )
    }

    suspend fun actualTimeFromParallelly(
        material: T,
        startTime: Instant,
        quantity: Quantity<V> = Quantity(constants.one, quantityUnit),
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        val productivityCalendar = productivity.findFromParallelly(startTime, QuantityProductivity<V, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return ActualTime(
                time = TimeRange(start = startTime, end = Instant.DISTANT_FUTURE),
                workingTimes = emptyList(), breakTimes = emptyList(), connectionTimes = emptyList()
            )
        }
        return actualTimeFromInternal(
            material = material, startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration, breakTime = breakTime
        )
    }

    suspend fun actualTimeFromOrNullParallelly(
        material: T,
        startTime: Instant,
        quantity: Quantity<V> = Quantity(constants.one, quantityUnit),
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime? {
        val productivityCalendar = productivity.findFromParallelly(startTime, QuantityProductivity<V, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) return null
        return actualTimeFromInternal(
            material = material, startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration, breakTime = breakTime
        )
    }

    fun actualTimeUntil(
        material: T,
        endTime: Instant,
        quantity: Quantity<V> = Quantity(constants.one, quantityUnit),
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        val productivityCalendar = productivity.findUntil(endTime, QuantityProductivity<V, T, U>::timeWindow).reversed()
        if (productivityCalendar.isEmpty()) {
            return ActualTime(
                time = TimeRange(start = Instant.DISTANT_PAST, end = endTime),
                workingTimes = emptyList(), breakTimes = emptyList(), connectionTimes = emptyList()
            )
        }
        return actualTimeUntilInternal(
            material = material, endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    fun actualTimeUntilOrNull(
        material: T,
        endTime: Instant,
        quantity: Quantity<V> = Quantity(constants.one, quantityUnit),
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime? {
        val productivityCalendar = productivity.findUntil(endTime, QuantityProductivity<V, T, U>::timeWindow).reversed()
        if (productivityCalendar.isEmpty()) return null
        return actualTimeUntilInternal(
            material = material, endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    suspend fun actualTimeUntilParallelly(
        material: T,
        endTime: Instant,
        quantity: Quantity<V> = Quantity(constants.one, quantityUnit),
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        val productivityCalendar = productivity.findUntilParallelly(endTime, QuantityProductivity<V, T, U>::timeWindow).reversed()
        if (productivityCalendar.isEmpty()) {
            return ActualTime(
                time = TimeRange(start = Instant.DISTANT_PAST, end = endTime),
                workingTimes = emptyList(), breakTimes = emptyList(), connectionTimes = emptyList()
            )
        }
        return actualTimeUntilInternal(
            material = material, endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    suspend fun actualTimeUntilOrNullParallelly(
        material: T,
        endTime: Instant,
        quantity: Quantity<V> = Quantity(constants.one, quantityUnit),
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime? {
        val productivityCalendar = productivity.findUntilParallelly(endTime, QuantityProductivity<V, T, U>::timeWindow).reversed()
        if (productivityCalendar.isEmpty()) return null
        return actualTimeUntilInternal(
            material = material, endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    fun actualQuantity(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): Quantity<V> {
        val productivityCalendar = productivity.find(time, QuantityProductivity<V, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return Quantity(constants.zero, resolveQuantityUnit(material))
        }
        return actualQuantityInternal(
            material = material, time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration, breakTime = breakTime
        )
    }

    fun actualQuantityOrNull(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): Quantity<V>? {
        val productivityCalendar = productivity.find(time, QuantityProductivity<V, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) return null
        return actualQuantityInternal(
            material = material, time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration, breakTime = breakTime
        )
    }

    suspend fun actualQuantityParallelly(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): Quantity<V> {
        val productivityCalendar = productivity.findParallelly(time, QuantityProductivity<V, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return Quantity(constants.zero, resolveQuantityUnit(material))
        }
        return actualQuantityInternal(
            material = material, time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration, breakTime = breakTime
        )
    }

    suspend fun actualQuantityOrNullParallelly(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): Quantity<V>? {
        val productivityCalendar = productivity.findParallelly(time, QuantityProductivity<V, T, U>::timeWindow)
        if (productivityCalendar.isEmpty()) return null
        return actualQuantityInternal(
            material = material, time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration, breakTime = breakTime
        )
    }

    // -- 内部计算：沿用 ProductivityCalendar 逻辑，通过日历数值边界操作 V 值 / Internal computation: same logic as ProductivityCalendar, operating on V values through the calendar numeric boundary --

    private fun actualTimeFromInternal(
        material: T,
        startTime: Instant,
        productivityCalendar: List<QuantityProductivity<V, T, U>>,
        quantity: Quantity<V>,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        validateQuantityUnit(
            material = material,
            productivityCalendar = productivityCalendar,
            quantity = quantity
        )
        var produceQuantity = Flt64.zero
        var currentTime = startTime
        val workingTimes = ArrayList<TimeRange>()
        val breakTimes = ArrayList<TimeRange>()
        val connectionTimes = ArrayList<TimeRange>()
        val quantityValue = calendarValueOf(quantity)
        for (calendar in productivityCalendar) {
            val currentProductivity = productivityRateOf(calendar, material) ?: continue
            val maxDuration = calendarCeilDurationOf((quantityValue - produceQuantity) / currentProductivity)

            val validTimes = validTimes(
                time = calendar.timeWindow.intersectionWith(TimeRange(start = currentTime)) ?: continue,
                unavailableTimes = unavailableTimes,
                beforeConnectionTime = beforeConnectionTime,
                afterConnectionTime = afterConnectionTime,
                beforeConditionalConnectionTime = beforeConditionalConnectionTime,
                afterConditionalConnectionTime = afterConditionalConnectionTime,
                currentDuration = if (currentTime == startTime) currentDuration else Duration.ZERO,
                maxDuration = maxDuration,
                breakTime = breakTime
            )
            workingTimes.addAll(validTimes.times)
            breakTimes.addAll(validTimes.breakTimes)
            connectionTimes.addAll(validTimes.connectionTimes)
            for (produceTime in validTimes.times) {
                val thisQuantity = calendarValueOf(produceTime.duration) * currentProductivity
                produceQuantity += min(
                    quantityValue - produceQuantity,
                    thisQuantity
                )
            }
            currentTime = maxEndTime(validTimes.times, validTimes.breakTimes, validTimes.connectionTimes)

            if (produceQuantity eq quantityValue) {
                break
            }
        }

        return if (produceQuantity eq quantityValue) {
            ActualTime(
                time = TimeRange(start = startTime, end = currentTime),
                workingTimes = workingTimes, breakTimes = breakTimes, connectionTimes = connectionTimes
            )
        } else {
            ActualTime(
                time = TimeRange(start = startTime, end = Instant.DISTANT_FUTURE),
                workingTimes = workingTimes, breakTimes = breakTimes, connectionTimes = connectionTimes
            )
        }
    }

    private fun actualTimeUntilInternal(
        material: T,
        endTime: Instant,
        productivityCalendar: List<QuantityProductivity<V, T, U>>,
        quantity: Quantity<V>,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        validateQuantityUnit(
            material = material,
            productivityCalendar = productivityCalendar,
            quantity = quantity
        )
        var produceQuantity = Flt64.zero
        var currentTime = endTime
        val workingTimes = ArrayList<TimeRange>()
        val breakTimes = ArrayList<TimeRange>()
        val connectionTimes = ArrayList<TimeRange>()
        val quantityValue = calendarValueOf(quantity)
        for (calendar in productivityCalendar) {
            val currentProductivity = productivityRateOf(calendar, material) ?: continue
            val maxDuration = calendarCeilDurationOf((quantityValue - produceQuantity) / currentProductivity)

            val validTimes = reversedValidTimes(
                time = calendar.timeWindow.intersectionWith(TimeRange(end = currentTime)) ?: continue,
                unavailableTimes = unavailableTimes,
                beforeConnectionTime = beforeConnectionTime,
                afterConnectionTime = afterConnectionTime,
                beforeConditionalConnectionTime = beforeConditionalConnectionTime,
                afterConditionalConnectionTime = afterConditionalConnectionTime,
                maxDuration = maxDuration,
                breakTime = breakTime
            )
            workingTimes.addAll(validTimes.times)
            breakTimes.addAll(validTimes.breakTimes)
            connectionTimes.addAll(validTimes.connectionTimes)
            for (produceTime in validTimes.times) {
                val thisQuantity = calendarValueOf(produceTime.duration) * currentProductivity
                produceQuantity += min(
                    quantityValue - produceQuantity,
                    thisQuantity
                )
            }
            currentTime = minStartTime(validTimes.times, validTimes.breakTimes, validTimes.connectionTimes)

            if (produceQuantity eq quantityValue) {
                break
            }
        }

        return if (produceQuantity eq quantityValue) {
            ActualTime(
                time = TimeRange(start = currentTime, end = endTime),
                workingTimes = workingTimes, breakTimes = breakTimes, connectionTimes = connectionTimes
            )
        } else {
            ActualTime(
                time = TimeRange(start = Instant.DISTANT_PAST, end = endTime),
                workingTimes = workingTimes, breakTimes = breakTimes, connectionTimes = connectionTimes
            )
        }
    }

    private fun actualQuantityInternal(
        material: T,
        time: TimeRange,
        productivityCalendar: List<QuantityProductivity<V, T, U>>,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): Quantity<V> {
        val resolvedUnit = resolveQuantityUnit(
            material = material,
            productivityCalendar = productivityCalendar
        )
        val validTimes = validTimes(
            time = time,
            unavailableTimes = unavailableTimes,
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration,
            breakTime = breakTime
        ).times

        var totalFlt64 = Flt64.zero
        for (calendar in productivityCalendar) {
            for (validTime in validTimes) {
                if (validTime.end <= calendar.timeWindow.start) {
                    continue
                } else if (validTime.start >= calendar.timeWindow.end) {
                    break
                }

                val produceTime = validTime.intersectionWith(calendar.timeWindow)?.duration ?: continue
                val currentProductivity = productivityRateOf(calendar, material) ?: Flt64.zero
                totalFlt64 += calendarValueOf(produceTime) * currentProductivity
            }
        }
        val floored = quantityFloor(totalFlt64)
        return Quantity(floored.value, resolvedUnit)
    }
}

/**
 * 离散物理量生产力日历，使用 UInt64 作为产量数值类型 /
 * Discrete quantity-based productivity calendar using UInt64 as the quantity value type.
 *
 * @param P 生产力类型 / The productivity type
 * @param T 材料类型 / The material type
 * @param U 键类型 / The key type
 * @param timeWindow 时间窗口 / The time window
 * @param productivity 生产力列表 / The list of productivities
 * @param unavailableTimes 不可用时间列表 / The list of unavailable times
 * @param quantityUnit 产出数量的默认单位 / The default unit for production quantities
 */
open class DiscreteQuantityProductivityCalendar<W, P, T, U>(
    timeWindow: TimeWindow<W>,
    productivity: List<P>,
    unavailableTimes: List<TimeRange>? = null,
    quantityUnit: PhysicalUnit = NoneUnit
) : QuantityProductivityCalendar<W, UInt64, P, T, U>(
    timeWindow = timeWindow,
    productivity = productivity,
    unavailableTimes = unavailableTimes,
    quantityUnit = quantityUnit,
    constants = UInt64,
    quantityFloor = { value -> Quantity(value.floor().toUInt64(), quantityUnit) }
) where W : RealNumber<W>, P : QuantityProductivity<UInt64, T, U> {
    companion object {
        /**
         * 通过泛型时间窗口创建离散物理量生产力日历，并集中转换到日历数值边界 /
         * Create a discrete quantity productivity calendar from a generic time window and centralize conversion to the calendar numeric boundary
         *
         * @param V 时间窗口数值类型 / The time-window numeric type
         * @param P 生产力类型 / The productivity type
         * @param T 材料类型 / The material type
         * @param U 键类型 / The key type
         * @param timeWindow 时间窗口 / The time window
         * @param productivity 生产力列表 / The list of productivities
         * @param unavailableTimes 不可用时间列表 / The list of unavailable times
         * @param quantityUnit 产出数量的默认单位 / The default unit for production quantities
         * @return 离散物理量生产力日历 / The discrete quantity productivity calendar
         */
        operator fun <V : RealNumber<V>, P, T, U> invoke(
            timeWindow: TimeWindow<V>,
            productivity: List<P>,
            unavailableTimes: List<TimeRange>? = null,
            quantityUnit: PhysicalUnit = NoneUnit
        ): DiscreteQuantityProductivityCalendar<V, P, T, U> where P : QuantityProductivity<UInt64, T, U> {
            return DiscreteQuantityProductivityCalendar(
                timeWindow = timeWindow,
                productivity = productivity,
                unavailableTimes = unavailableTimes,
                quantityUnit = quantityUnit
            )
        }
    }
}

/**
 * 连续物理量生产力日历，产量数值类型由调用方提供 /
 * Continuous quantity-based productivity calendar with caller-provided quantity value type.
 *
 * @param V 产量数值类型 / The quantity value type
 * @param P 生产力类型 / The productivity type
 * @param T 材料类型 / The material type
 * @param U 键类型 / The key type
 * @param timeWindow 时间窗口 / The time window
 * @param productivity 生产力列表 / The list of productivities
 * @param unavailableTimes 不可用时间列表 / The list of unavailable times
 * @param quantityUnit 产出数量的默认单位 / The default unit for production quantities
 * @param constants 数值常量 / The numeric constants
 * @param quantityValueOf 从日历内部数值边界转换为产量数值 / Conversion from calendar internal numeric boundary to quantity value
 */
open class ContinuousQuantityProductivityCalendar<W, V, P, T, U>(
    timeWindow: TimeWindow<W>,
    productivity: List<P>,
    unavailableTimes: List<TimeRange>? = null,
    quantityUnit: PhysicalUnit = NoneUnit,
    constants: RealNumberConstants<V>,
    quantityValueOf: (Flt64) -> V
) : QuantityProductivityCalendar<W, V, P, T, U>(
    timeWindow = timeWindow,
    productivity = productivity,
    unavailableTimes = unavailableTimes,
    quantityUnit = quantityUnit,
    constants = constants,
    quantityFloor = { value -> Quantity(quantityValueOf(value), quantityUnit) }
) where W : RealNumber<W>, P : QuantityProductivity<V, T, U>, V : RealNumber<V>, V : PlusGroup<V>, V : TimesGroup<V> {
    companion object {
        /**
         * 通过泛型时间窗口创建连续物理量生产力日历，并集中转换到日历数值边界 /
         * Create a continuous quantity productivity calendar from a generic time window and centralize conversion to the calendar numeric boundary
         *
         * @param W 时间窗口数值类型 / The time-window numeric type
         * @param V 产量数值类型 / The quantity value type
         * @param P 生产力类型 / The productivity type
         * @param T 材料类型 / The material type
         * @param U 键类型 / The key type
         * @param timeWindow 时间窗口 / The time window
         * @param productivity 生产力列表 / The list of productivities
         * @param unavailableTimes 不可用时间列表 / The list of unavailable times
         * @param quantityUnit 产出数量的默认单位 / The default unit for production quantities
         * @param constants 数值常量 / The numeric constants
         * @param quantityValueOf 从日历内部数值边界转换为产量数值 / Conversion from calendar internal numeric boundary to quantity value
         * @return 连续物理量生产力日历 / The continuous quantity productivity calendar
         */
        operator fun <W, V, P, T, U> invoke(
            timeWindow: TimeWindow<W>,
            productivity: List<P>,
            unavailableTimes: List<TimeRange>? = null,
            quantityUnit: PhysicalUnit = NoneUnit,
            constants: RealNumberConstants<V>,
            quantityValueOf: (Flt64) -> V
        ): ContinuousQuantityProductivityCalendar<W, V, P, T, U> where W : RealNumber<W>, P : QuantityProductivity<V, T, U>, V : RealNumber<V>, V : PlusGroup<V>, V : TimesGroup<V> {
            return ContinuousQuantityProductivityCalendar(
                timeWindow = timeWindow,
                productivity = productivity,
                unavailableTimes = unavailableTimes,
                quantityUnit = quantityUnit,
                constants = constants,
                quantityValueOf = quantityValueOf
            )
        }
    }
}
