@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 时间窗口及相关工具函数 / Time window and related utility functions
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.toDuration
import fuookami.ospf.kotlin.utils.max
import fuookami.ospf.kotlin.utils.min
import fuookami.ospf.kotlin.utils.truncatedTo
import kotlinx.datetime.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * 时间窗口，提供时间离散化和舍入功能 / Time window providing time discretization and rounding capabilities
 *
 * @property window 基础时间范围 / The underlying time range
 * @property continues 是否连续 / Whether the time window is continuous
 * @property durationUnit 持续时间单位 / The duration unit
 * @property dateOffset 日期偏移量 / The date offset duration
 * @property interval 时间间隔 / The time interval
 */
data class TimeWindow(
    val window: TimeRange,
    val continues: Boolean = true,
    val durationUnit: DurationUnit = DurationUnit.SECONDS,
    val dateOffset: Duration = Duration.ZERO,
    val interval: Duration = 1.toDuration(durationUnit)
) {
    private fun unsupportedUpperDurationUnit(): Nothing {
        throw UnsupportedOperationException(
            "TimeWindow.upper/upperInterval 暂不支持 durationUnit=$durationUnit，仅支持 SECONDS/MINUTES/HOURS。"
        )
    }

    companion object {
        /**
         * 创建秒级时间窗口 / Create a seconds-level time window
         *
         * @param timeWindow 基础时间范围 / The underlying time range
         * @param dateOffset 日期偏移量 / The date offset
         * @param continues 是否连续 / Whether continuous
         * @param interval 时间间隔 / The time interval
         * @return 秒级时间窗口 / The seconds-level time window
         */
        fun seconds(
            timeWindow: TimeRange,
            dateOffset: Flt64 = Flt64.zero,
            continues: Boolean = true,
            interval: Flt64 = Flt64.one
        ): TimeWindow {
            return TimeWindow(
                window = timeWindow,
                dateOffset = dateOffset.toDuration(DurationUnit.SECONDS),
                continues = continues,
                durationUnit = DurationUnit.SECONDS,
                interval = interval.toDouble().toDuration(DurationUnit.SECONDS)
            )
        }

        /**
         * 创建分钟级时间窗口 / Create a minutes-level time window
         *
         * @param timeWindow 基础时间范围 / The underlying time range
         * @param dateOffset 日期偏移量 / The date offset
         * @param continues 是否连续 / Whether continuous
         * @param interval 时间间隔 / The time interval
         * @return 分钟级时间窗口 / The minutes-level time window
         */
        fun minutes(
            timeWindow: TimeRange,
            dateOffset: Flt64 = Flt64.zero,
            continues: Boolean = true,
            interval: Flt64 = Flt64.one
        ): TimeWindow {
            return TimeWindow(
                window = timeWindow,
                dateOffset = dateOffset.toDuration(DurationUnit.MINUTES),
                continues = continues,
                durationUnit = DurationUnit.MINUTES,
                interval = interval.toDouble().toDuration(DurationUnit.MINUTES)
            )
        }

        /**
         * 创建小时级时间窗口 / Create an hours-level time window
         *
         * @param timeWindow 基础时间范围 / The underlying time range
         * @param dateOffset 日期偏移量 / The date offset
         * @param continues 是否连续 / Whether continuous
         * @param interval 时间间隔 / The time interval
         * @return 小时级时间窗口 / The hours-level time window
         */
        fun hours(
            timeWindow: TimeRange,
            dateOffset: Flt64 = Flt64.zero,
            continues: Boolean = true,
            interval: Flt64 = Flt64.one
        ): TimeWindow {
            return TimeWindow(
                window = timeWindow,
                dateOffset = dateOffset.toDuration(DurationUnit.HOURS),
                continues = continues,
                durationUnit = DurationUnit.HOURS,
                interval = interval.toDouble().toDuration(DurationUnit.HOURS)
            )
        }
    }

    /** 将持续时间转换为数值 / Convert duration to numeric value */
    val Duration.value: Flt64 get() = Flt64(this.toDouble(durationUnit))
    val Duration.round: Duration get() = round(this.toDouble(durationUnit)).toDuration(durationUnit)
    val Duration.floor: Duration get() = floor(this.toDouble(durationUnit)).toDuration(durationUnit)
    val Duration.ceil: Duration get() = ceil(this.toDouble(durationUnit)).toDuration(durationUnit)

    /**
     * 获取持续时间的数值 / Get the numeric value of a duration
     *
     * @param duration 持续时间 / The duration
     * @return 数值 / The numeric value
     */
    fun valueOf(duration: Duration) = duration.value

    /**
     * 舍入持续时间 / Round a duration
     *
     * @param duration 持续时间 / The duration
     * @return 舍入后的持续时间 / The rounded duration
     */
    fun round(duration: Duration) = duration.round

    /**
     * 向下取整持续时间 / Floor a duration
     *
     * @param duration 持续时间 / The duration
     * @return 向下取整后的持续时间 / The floored duration
     */
    fun floor(duration: Duration) = duration.floor

    /**
     * 向上取整持续时间 / Ceil a duration
     *
     * @param duration 持续时间 / The duration
     * @return 向上取整后的持续时间 / The ceiled duration
     */
    fun ceil(duration: Duration) = duration.ceil

    /** 将时间点转换为相对于窗口起始的数值 / Convert an instant to a numeric value relative to window start */
    val Instant.value: Flt64 get() = Flt64((this - window.start).toDouble(durationUnit))
    val Instant.round: Instant get() = window.start + (this - window.start).round
    val Instant.floor: Instant get() = window.start + (this - window.start).floor
    val Instant.ceil: Instant get() = window.start + (this - window.start).ceil

    /**
     * 获取时间点的数值 / Get the numeric value of an instant
     *
     * @param instant 时间点 / The instant
     * @return 数值 / The numeric value
     */
    fun valueOf(instant: Instant) = instant.value

    /**
     * 舍入时间点 / Round an instant
     *
     * @param instant 时间点 / The instant
     * @return 舍入后的时间点 / The rounded instant
     */
    fun round(instant: Instant) = instant.round

    /**
     * 向下取整时间点 / Floor an instant
     *
     * @param instant 时间点 / The instant
     * @return 向下取整后的时间点 / The floored instant
     */
    fun floor(instant: Instant) = instant.floor

    /**
     * 向上取整时间点 / Ceil an instant
     *
     * @param instant 时间点 / The instant
     * @return 向上取整后的时间点 / The ceiled instant
     */
    fun ceil(instant: Instant) = instant.ceil

    /** 将数值转换为持续时间 / Convert a numeric value to duration */
    val Flt64.duration: Duration get() = this.toDouble().toDuration(durationUnit)
    val Int64.duration: Duration get() = this.toFlt64().duration
    val UInt64.duration: Duration get() = this.toFlt64().duration

    /**
     * 从数值创建持续时间 / Create a duration from a numeric value
     *
     * @param duration 数值 / The numeric value
     * @return 持续时间 / The duration
     */
    fun durationOf(duration: Flt64) = duration.duration

    /**
     * 从整数值创建持续时间 / Create a duration from an integer value
     *
     * @param duration 整数值 / The integer value
     * @return 持续时间 / The duration
     */
    fun durationOf(duration: Int64) = duration.toFlt64().duration

    /**
     * 从无符号整数值创建持续时间 / Create a duration from an unsigned integer value
     *
     * @param duration 无符号整数值 / The unsigned integer value
     * @return 持续时间 / The duration
     */
    fun durationOf(duration: UInt64) = duration.toFlt64().duration

    /** 将数值转换为时间点 / Convert a numeric value to an instant */
    val Flt64.instant: Instant get() = window.start + this.toDouble().toDuration(durationUnit)
    val Int64.instant: Instant get() = window.start + this.toFlt64().duration
    val UInt64.instant: Instant get() = window.start + this.toFlt64().duration

    /**
     * 从数值创建时间点 / Create an instant from a numeric value
     *
     * @param instant 数值 / The numeric value
     * @return 时间点 / The instant
     */
    fun instantOf(instant: Flt64) = window.start + instant.duration

    /**
     * 从整数值创建时间点 / Create an instant from an integer value
     *
     * @param instant 整数值 / The integer value
     * @return 时间点 / The instant
     */
    fun instantOf(instant: Int64) = window.start + instant.toFlt64().duration

    /**
     * 从无符号整数值创建时间点 / Create an instant from an unsigned integer value
     *
     * @param instant 无符号整数值 / The unsigned integer value
     * @return 时间点 / The instant
     */
    fun instantOf(instant: UInt64) = window.start + instant.toFlt64().duration

    /** 是否为空窗口 / Whether this is an empty window */
    val empty: Boolean by window::empty
    /** 开始时间 / Start time */
    val start: Instant by window::start
    /** 结束时间 / End time */
    val end: Instant by window::end
    /** 持续时间 / Duration */
    val duration: Duration by window::duration
    /** 上级时间间隔 / The upper-level time interval */
    val upperInterval: Duration by lazy {
        when (durationUnit) {
            DurationUnit.SECONDS -> {
                1.toDuration(DurationUnit.MINUTES)
            }

            DurationUnit.MINUTES -> {
                1.toDuration(DurationUnit.HOURS)
            }

            DurationUnit.HOURS -> {
                1.toDuration(DurationUnit.DAYS)
            }

            else -> {
                unsupportedUpperDurationUnit()
            }
        }
    }

    /** 上级时间窗口 / The upper-level time window */
    val upper: TimeWindow by lazy {
        TimeWindow(
            window = window,
            continues = continues,
            durationUnit = when (durationUnit) {
                DurationUnit.SECONDS -> {
                    DurationUnit.MINUTES
                }

                DurationUnit.MINUTES -> {
                    DurationUnit.HOURS
                }

                DurationUnit.HOURS -> {
                    DurationUnit.DAYS
                }

                else -> {
                    unsupportedUpperDurationUnit()
                }
            },
            upperInterval
        )
    }

    /**
     * 按缩放比例计算上级时间间隔 / Calculate upper interval by scale
     *
     * @param scale 缩放比例 / The scale factor
     * @return 上级时间间隔 / The upper interval
     */
    fun upperIntervalByScale(scale: UInt64): Duration {
        val upperInterval = interval * scale.toInt().toDouble()
        return if (upperInterval > 1.toDuration(DurationUnit.DAYS) && durationUnit.ordinal < DurationUnit.DAYS.ordinal) {
            1.toDuration(DurationUnit.DAYS)
        } else if (upperInterval > 1.toDuration(DurationUnit.HOURS) && durationUnit.ordinal < DurationUnit.HOURS.ordinal) {
            1.toDuration(DurationUnit.HOURS)
        } else if (upperInterval > 1.toDuration(DurationUnit.MINUTES) && durationUnit.ordinal < DurationUnit.MINUTES.ordinal) {
            1.toDuration(DurationUnit.MINUTES)
        } else {
            upperInterval
        }
    }

    /**
     * 按缩放比例创建上级时间窗口 / Create upper time window by scale
     *
     * @param scale 缩放比例 / The scale factor
     * @return 上级时间窗口 / The upper time window
     */
    fun upperByScale(scale: UInt64): TimeWindow {
        val scaleInterval = interval * scale.toInt().toDouble()
        val (upperUnit, upperInterval) = if (scaleInterval > 1.toDuration(DurationUnit.DAYS) && durationUnit.ordinal < DurationUnit.DAYS.ordinal) {
            Pair(DurationUnit.DAYS, 1.toDuration(DurationUnit.DAYS))
        } else if (scaleInterval > 1.toDuration(DurationUnit.HOURS) && durationUnit.ordinal < DurationUnit.HOURS.ordinal) {
            Pair(DurationUnit.HOURS, 1.toDuration(DurationUnit.HOURS))
        } else if (scaleInterval > 1.toDuration(DurationUnit.MINUTES) && durationUnit.ordinal < DurationUnit.MINUTES.ordinal) {
            Pair(DurationUnit.MINUTES, 1.toDuration(DurationUnit.MINUTES))
        } else {
            Pair(durationUnit, scaleInterval)
        }
        return TimeWindow(
            window = window,
            continues = continues,
            durationUnit = upperUnit,
            interval = upperInterval
        )
    }

    /** 按默认间隔划分的时间段列表 / List of time slots divided by default interval */
    val timeSlots: List<TimeRange> by lazy {
        timeSlotsOf(interval)
    }

    /**
     * 按指定间隔划分时间段 / Divide time slots by the specified interval
     *
     * @param interval 时间间隔 / The time interval
     * @return 时间段列表 / The list of time slots
     */
    fun timeSlotsOf(interval: Duration): List<TimeRange> {
        val timeSlots = ArrayList<TimeRange>()
        var current = start
        while (current != end) {
            val duration = min(end - current, interval)
            timeSlots.add(
                TimeRange(
                    start = current,
                    end = current + duration
                )
            )
            current += duration
        }
        return timeSlots
    }

    /** 按上级间隔划分的舍入时间段列表 / List of rounded time slots divided by upper interval */
    val roundTimeSlots: List<TimeRange> by lazy {
        roundTimeSlotsOf(upper.interval)
    }

    /**
     * 按指定间隔生成舍入时间段 / Generate rounded time slots by the specified interval
     *
     * @param interval 时间间隔 / The time interval
     * @param excludedTimes 需排除的时间范围 / Time ranges to exclude
     * @return 舍入时间段列表 / The list of rounded time slots
     */
    fun roundTimeSlotsOf(
        interval: Duration,
        excludedTimes: List<TimeRange> = emptyList()
    ): List<TimeRange> {
        return roundTimeSlotsOf(
            intervals = mapOf(null to interval),
            excludedTimes = excludedTimes
        )
    }

    /**
     * Generate rounded time slots based on intervals and excluded times.
     * 生成基于时间粒度和排除时间的舍入时间段�?
     *
     * @param intervals Time intervals mapping, key is the time range for specific interval, null for default.
     *                  时间粒度映射，键为特定时间粒度的时间范围，null 表示默认值�?
     * @param excludedTimes Time ranges to be excluded from the generated slots.
     *                      需要从生成的时段中排除的时间范围�?
     * @return List of time slots without merging. The caller should handle merging if needed.
     *         未合并的时段列表，调用方应根据需要进行合并�?
     */
    fun roundTimeSlotsOf(
        intervals: Map<TimeRange?, Duration>,
        excludedTimes: List<TimeRange> = emptyList()
    ): List<TimeRange> {
        val timeSlots = ArrayList<TimeRange>()
        val defaultInterval = intervals[null] ?: upperInterval
        val specificIntervals = ArrayList<Pair<TimeRange, Duration>>(intervals.size)
        for ((timeRange, thisInterval) in intervals) {
            if (timeRange != null) {
                specificIntervals.add(timeRange to thisInterval)
            }
        }
        var current = start
        fun intervalAt(time: Instant): Duration {
            for ((timeRange, thisInterval) in specificIntervals) {
                if (timeRange.contains(time)) {
                    return thisInterval
                }
            }
            return defaultInterval
        }
        var currentInterval = intervalAt(start)
        val end1 = (start.truncatedTo(upper.durationUnit) + currentInterval * kotlin.math.ceil(upper.interval / currentInterval).toInt())
            .let {
                if ((it - start) < currentInterval) {
                    it + upper.interval
                } else {
                    it
                }
            }
        val firstStageEnd = min(end1, end)
        while (current < firstStageEnd) {
            var duration = if (current == start) {
                val durationDiff = (end1 - start)
                val floorRatio = kotlin.math.floor(durationDiff / currentInterval)
                durationDiff - (floorRatio * currentInterval.toDouble(DurationUnit.SECONDS)).toDuration(DurationUnit.SECONDS)
            } else {
                min(firstStageEnd - current, currentInterval)
            }
            if (duration <= Duration.ZERO) {
                duration = min(firstStageEnd - current, currentInterval)
            }
            if (duration <= Duration.ZERO) {
                break
            }
            timeSlots.add(
                TimeRange(
                    start = max(start, current),
                    end = current + duration
                )
            )
            current += duration
            currentInterval = intervalAt(current)
        }
        val end2 = end.truncatedTo(upper.durationUnit)
        while (current < end2) {
            val duration = min(end2 - current, currentInterval)
            if (duration <= Duration.ZERO) {
                break
            }
            timeSlots.add(
                TimeRange(
                    start = current,
                    end = current + duration
                )
            )
            current += duration
            currentInterval = intervalAt(current)
        }
        while (current < end) {
            val duration = min(end - current, currentInterval)
            if (duration <= Duration.ZERO) {
                break
            }
            timeSlots.add(
                TimeRange(
                    start = current,
                    end = current + duration
                )
            )
            current += duration
            currentInterval = intervalAt(current)
        }
        if (excludedTimes.isEmpty()) {
            return timeSlots
        }
        val clippedExcludedTimes = ArrayList<TimeRange>(excludedTimes.size)
        for (excludedTime in excludedTimes) {
            if (!excludedTime.withIntersection(window)) {
                continue
            }
            clippedExcludedTimes.add(
                TimeRange(
                    start = max(excludedTime.start, start),
                    end = min(excludedTime.end, end)
                )
            )
        }
        if (clippedExcludedTimes.isEmpty()) {
            return timeSlots
        }
        val normalizedExcludedTimes = clippedExcludedTimes.merge()
        if (normalizedExcludedTimes.isEmpty()) {
            return timeSlots
        }

        val pieces = ArrayList<TimeRange>()
        for (slot in timeSlots) {
            pieces.addAll(slot.differenceWith(normalizedExcludedTimes))
        }
        return pieces
    }

    /**
     * 判断是否与另一时间范围有交集 / Check whether this window intersects with another range
     *
     * @param ano 另一时间范围 / Another time range
     * @return 是否有交集 / Whether there is an intersection
     */
    fun withIntersection(ano: TimeRange): Boolean {
        return window.withIntersection(ano)
    }

    /**
     * 判断是否包含指定时间点 / Check whether this window contains the specified instant
     *
     * @param time 时间点 / The instant
     * @return 是否包含 / Whether contained
     */
    fun contains(time: Instant): Boolean {
        return window.contains(time)
    }

    /**
     * 判断是否包含指定时间范围 / Check whether this window contains the specified time range
     *
     * @param time 时间范围 / The time range
     * @return 是否包含 / Whether contained
     */
    fun contains(time: TimeRange): Boolean {
        return window.contains(time)
    }

    /**
     * 按指定时间点拆分窗口 / Split the window at the specified instants
     *
     * @param times 拆分时间点列表 / The list of split instants
     * @return 拆分后的时间范围列表 / The list of split time ranges
     */
    fun split(
        times: List<Instant>
    ): List<TimeRange> {
        return window.split(times)
    }

    /**
     * 按持续时间单元拆分窗口 / Split the window by duration unit
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
    ): TimeRange.SplitTimeRanges {
        return window.split(
            unit = unit,
            currentDuration = currentDuration,
            maxDuration = maxDuration,
            breakTime = breakTime
        )
    }

    /**
     * 反向按持续时间单元拆分窗口 / Reverse split the window by duration unit
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
    ): TimeRange.SplitTimeRanges {
        return window.rsplit(
            unit = unit,
            maxDuration = maxDuration,
            breakTime = breakTime
        )
    }

    /**
     * 计算给定时间所属的日期 / Calculate the date to which the given time belongs
     *
     * @param time 本地日期时间 / The local date time
     * @param timeZone 时区 / The time zone
     * @return 所属的本地日期 / The local date
     */
    fun date(
        time: LocalDateTime,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): LocalDate {
        return if (time.toInstant(timeZone) < (time.date.atStartOfDayIn(timeZone) + dateOffset)) {
            time.date.minus(DatePeriod(days = 1))
        } else {
            time.date
        }
    }

    /**
     * 创建新的时间窗口 / Create a new time window
     *
     * @param window 基础时间范围 / The underlying time range
     * @param continues 是否连续 / Whether continuous
     * @return 新的时间窗口 / The new time window
     */
    fun new(window: TimeRange, continues: Boolean): TimeWindow {
        return TimeWindow(
            window = window,
            continues = continues,
            durationUnit = durationUnit
        )
    }
}


