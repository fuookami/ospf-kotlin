@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 时间窗口模型，提供时间离散化和舍入功能 / Time window model providing time discretization and rounding capabilities
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import kotlin.time.Instant
import kotlin.time.toDuration
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.max
import fuookami.ospf.kotlin.utils.min
import fuookami.ospf.kotlin.utils.truncatedTo
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.toDuration
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*

/** 时间窗口数值物理量 / Time-window value quantity */
typealias TimeWindowValueQuantity<V> = Quantity<V>

/**
 * 泛型时间窗口，提供时间离散化和舍入功能 / Generic time window providing time discretization and rounding capabilities
 *
 * @property window 基础时间范围 / The underlying time range
 * @property continues 是否连续 / Whether the time window is continuous
 * @property durationUnit 持续时间单位 / The duration unit
 * @property dateOffset 日期偏移量 / The date offset duration
 * @property interval 时间间隔 / The time interval
 * @property fromDouble 从 Double 转换为 V / Convert from Double to V
 * @property toDouble 从 V 转换为 Double / Convert from V to Double
 */
data class TimeWindow<V : RealNumber<V>>(
    val window: TimeRange,
    val continues: Boolean = true,
    val durationUnit: DurationUnit = DurationUnit.SECONDS,
    val dateOffset: Duration = Duration.ZERO,
    val interval: Duration = 1.toDuration(durationUnit),
    val fromDouble: (Double) -> V,
    val toDouble: (V) -> Double
) {
    companion object {
        /**
         * 创建秒级泛型时间窗口 / Create a seconds-level generic time window
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间范围 / Time range
         * @param dateOffset 日期偏移量 / Date offset
         * @param continues 是否连续 / Whether continuous
         * @param interval 时间间隔 / Time interval
         * @param fromDouble 从 Double 转换为 V / Convert from Double to V
         * @param toDouble 从 V 转换为 Double / Convert from V to Double
         * @return 泛型时间窗口 / Generic time window
         */
        fun <V : RealNumber<V>> seconds(
            timeWindow: TimeRange,
            dateOffset: V,
            continues: Boolean = true,
            interval: V,
            fromDouble: (Double) -> V,
            toDouble: (V) -> Double
        ): TimeWindow<V> {
            return TimeWindow(
                window = timeWindow,
                dateOffset = toDouble(dateOffset).toDuration(DurationUnit.SECONDS),
                continues = continues,
                durationUnit = DurationUnit.SECONDS,
                interval = toDouble(interval).toDuration(DurationUnit.SECONDS),
                fromDouble = fromDouble,
                toDouble = toDouble
            )
        }

        /**
         * 通过物理量创建秒级泛型时间窗口 / Create a seconds-level generic time window from quantities
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间范围 / Time range
         * @param dateOffset 日期偏移量物理量 / Date offset quantity
         * @param continues 是否连续 / Whether continuous
         * @param interval 时间间隔物理量 / Time interval quantity
         * @param fromDouble 从 Double 转换为 V / Convert from Double to V
         * @param toDouble 从 V 转换为 Double / Convert from V to Double
         * @return 泛型时间窗口 / Generic time window
         */
        fun <V : RealNumber<V>> seconds(
            timeWindow: TimeRange,
            dateOffset: TimeWindowValueQuantity<V>,
            continues: Boolean = true,
            interval: TimeWindowValueQuantity<V>,
            fromDouble: (Double) -> V,
            toDouble: (V) -> Double
        ): TimeWindow<V> {
            return seconds(
                timeWindow = timeWindow,
                dateOffset = dateOffset.value,
                continues = continues,
                interval = interval.value,
                fromDouble = fromDouble,
                toDouble = toDouble
            )
        }

        /**
         * 创建分钟级泛型时间窗口 / Create a minutes-level generic time window
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间范围 / Time range
         * @param dateOffset 日期偏移量 / Date offset
         * @param continues 是否连续 / Whether continuous
         * @param interval 时间间隔 / Time interval
         * @param fromDouble 从 Double 转换为 V / Convert from Double to V
         * @param toDouble 从 V 转换为 Double / Convert from V to Double
         * @return 泛型时间窗口 / Generic time window
         */
        fun <V : RealNumber<V>> minutes(
            timeWindow: TimeRange,
            dateOffset: V,
            continues: Boolean = true,
            interval: V,
            fromDouble: (Double) -> V,
            toDouble: (V) -> Double
        ): TimeWindow<V> {
            return TimeWindow(
                window = timeWindow,
                dateOffset = toDouble(dateOffset).toDuration(DurationUnit.MINUTES),
                continues = continues,
                durationUnit = DurationUnit.MINUTES,
                interval = toDouble(interval).toDuration(DurationUnit.MINUTES),
                fromDouble = fromDouble,
                toDouble = toDouble
            )
        }

        /**
         * 通过物理量创建分钟级泛型时间窗口 / Create a minutes-level generic time window from quantities
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间范围 / Time range
         * @param dateOffset 日期偏移量物理量 / Date offset quantity
         * @param continues 是否连续 / Whether continuous
         * @param interval 时间间隔物理量 / Time interval quantity
         * @param fromDouble 从 Double 转换为 V / Convert from Double to V
         * @param toDouble 从 V 转换为 Double / Convert from V to Double
         * @return 泛型时间窗口 / Generic time window
         */
        fun <V : RealNumber<V>> minutes(
            timeWindow: TimeRange,
            dateOffset: TimeWindowValueQuantity<V>,
            continues: Boolean = true,
            interval: TimeWindowValueQuantity<V>,
            fromDouble: (Double) -> V,
            toDouble: (V) -> Double
        ): TimeWindow<V> {
            return minutes(
                timeWindow = timeWindow,
                dateOffset = dateOffset.value,
                continues = continues,
                interval = interval.value,
                fromDouble = fromDouble,
                toDouble = toDouble
            )
        }

        /**
         * 创建小时级泛型时间窗口 / Create an hours-level generic time window
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间范围 / Time range
         * @param dateOffset 日期偏移量 / Date offset
         * @param continues 是否连续 / Whether continuous
         * @param interval 时间间隔 / Time interval
         * @param fromDouble 从 Double 转换为 V / Convert from Double to V
         * @param toDouble 从 V 转换为 Double / Convert from V to Double
         * @return 泛型时间窗口 / Generic time window
         */
        fun <V : RealNumber<V>> hours(
            timeWindow: TimeRange,
            dateOffset: V,
            continues: Boolean = true,
            interval: V,
            fromDouble: (Double) -> V,
            toDouble: (V) -> Double
        ): TimeWindow<V> {
            return TimeWindow(
                window = timeWindow,
                dateOffset = toDouble(dateOffset).toDuration(DurationUnit.HOURS),
                continues = continues,
                durationUnit = DurationUnit.HOURS,
                interval = toDouble(interval).toDuration(DurationUnit.HOURS),
                fromDouble = fromDouble,
                toDouble = toDouble
            )
        }

        /**
         * 通过物理量创建小时级泛型时间窗口 / Create an hours-level generic time window from quantities
         *
         * @param V 数值类型 / Numeric type
         * @param timeWindow 时间范围 / Time range
         * @param dateOffset 日期偏移量物理量 / Date offset quantity
         * @param continues 是否连续 / Whether continuous
         * @param interval 时间间隔物理量 / Time interval quantity
         * @param fromDouble 从 Double 转换为 V / Convert from Double to V
         * @param toDouble 从 V 转换为 Double / Convert from V to Double
         * @return 泛型时间窗口 / Generic time window
         */
        fun <V : RealNumber<V>> hours(
            timeWindow: TimeRange,
            dateOffset: TimeWindowValueQuantity<V>,
            continues: Boolean = true,
            interval: TimeWindowValueQuantity<V>,
            fromDouble: (Double) -> V,
            toDouble: (V) -> Double
        ): TimeWindow<V> {
            return hours(
                timeWindow = timeWindow,
                dateOffset = dateOffset.value,
                continues = continues,
                interval = interval.value,
                fromDouble = fromDouble,
                toDouble = toDouble
            )
        }

    }

    /**
     * 转换为日历数值边界，用于仍固定 solver 数值类型的日历或 solver 内部 /
     * Convert to a calendar numeric boundary for calendar or solver internals that still use the solver numeric type
     *
     * @return 日历数值时间窗口边界 / The calendar numeric time-window boundary
     */
    fun toFlt64Boundary(): TimeWindow<Flt64> {
        return TimeWindow(
            window = window,
            continues = continues,
            durationUnit = durationUnit,
            dateOffset = dateOffset,
            interval = interval,
            fromDouble = { Flt64(it) },
            toDouble = { it.toDouble() }
        )
    }

    /** 将持续时间转换为 V 数值 / Convert duration to V numeric value */
    val Duration.value: V get() = fromDouble(this.toDouble(durationUnit))
    val Duration.round: Duration get() = round(this.toDouble(durationUnit)).toDuration(durationUnit)
    val Duration.floor: Duration get() = floor(this.toDouble(durationUnit)).toDuration(durationUnit)
    val Duration.ceil: Duration get() = ceil(this.toDouble(durationUnit)).toDuration(durationUnit)

    /**
     * 获取持续时间的 V 数值 / Get the V numeric value of a duration
     */
    fun valueOf(duration: Duration): V = duration.value

    /**
     * 获取持续时间的物理量数值 / Get the quantity value of a duration
     *
     * @param duration 持续时间 / Duration
     * @param unit 时间数值单位 / Time value unit
     * @return 持续时间物理量数值 / Duration quantity value
     */
    fun quantityOf(
        duration: Duration,
        unit: PhysicalUnit = NoneUnit
    ): TimeWindowValueQuantity<V> {
        return Quantity(valueOf(duration), unit)
    }

    /**
     * 舍入持续时间 / Round a duration
     */
    fun round(duration: Duration) = duration.round

    /**
     * 向下取整持续时间 / Floor a duration
     */
    fun floor(duration: Duration) = duration.floor

    /**
     * 向上取整持续时间 / Ceil a duration
     */
    fun ceil(duration: Duration) = duration.ceil

    /** 将时间点转换为相对于窗口起始的 V 数值 / Convert an instant to a V value relative to window start */
    val Instant.value: V get() = fromDouble((this - window.start).toDouble(durationUnit))
    val Instant.round: Instant get() = window.start + (this - window.start).round
    val Instant.floor: Instant get() = window.start + (this - window.start).floor
    val Instant.ceil: Instant get() = window.start + (this - window.start).ceil

    /**
     * 获取时间点的 V 数值 / Get the V numeric value of an instant
     */
    fun valueOf(instant: Instant): V = instant.value

    /**
     * 获取时间点相对窗口起点的物理量数值 / Get the quantity value of an instant relative to window start
     *
     * @param instant 时间点 / Instant
     * @param unit 时间数值单位 / Time value unit
     * @return 时间点物理量数值 / Instant quantity value
     */
    fun quantityOf(
        instant: Instant,
        unit: PhysicalUnit = NoneUnit
    ): TimeWindowValueQuantity<V> {
        return Quantity(valueOf(instant), unit)
    }

    /**
     * 舍入时间点 / Round an instant
     */
    fun round(instant: Instant) = instant.round

    /**
     * 向下取整时间点 / Floor an instant
     */
    fun floor(instant: Instant) = instant.floor

    /**
     * 向上取整时间点 / Ceil an instant
     */
    fun ceil(instant: Instant) = instant.ceil

    /** 将 V 数值转换为持续时间 / Convert V numeric value to duration */
    val V.duration: Duration get() = toDouble(this).toDuration(durationUnit)
    val Int64.duration: Duration get() = timeWindowValue().toDuration(durationUnit)
    val UInt64.duration: Duration get() = timeWindowValue().toDuration(durationUnit)

    /**
     * 从 V 数值创建持续时间 / Create a duration from a V value
     */
    fun durationOf(duration: V) = duration.duration

    /**
     * 从整数值创建持续时间 / Create a duration from an integer value
     */
    fun durationOf(duration: Int64) = duration.duration

    /**
     * 从无符号整数值创建持续时间 / Create a duration from an unsigned integer value
     */
    fun durationOf(duration: UInt64) = duration.duration

    /** 将 V 数值转换为时间点 / Convert V numeric value to an instant */
    val V.instant: Instant get() = window.start + toDouble(this).toDuration(durationUnit)
    val Int64.instant: Instant get() = window.start + timeWindowValue().toDuration(durationUnit)
    val UInt64.instant: Instant get() = window.start + timeWindowValue().toDuration(durationUnit)

    /**
     * 从 V 数值创建时间点 / Create an instant from a V value
     */
    fun instantOf(instant: V) = instant.instant

    /**
     * 从整数值创建时间点 / Create an instant from an integer value
     */
    fun instantOf(instant: Int64) = instant.instant

    /**
     * 从无符号整数值创建时间点 / Create an instant from an unsigned integer value
     */
    fun instantOf(instant: UInt64) = instant.instant

    /** 是否为空窗口 / Whether this is an empty window */
    val empty: Boolean by window::empty
    /** 开始时间 / Start time */
    val start: Instant by window::start
    /** 结束时间 / End time */
    val end: Instant by window::end
    /** 持续时间 / Duration */
    val duration: Duration by window::duration
    /** 上级时间间隔（nullable） / The upper-level time interval (nullable) */
    val upperIntervalOrNull: Duration? by lazy {
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
                null
            }
        }
    }

    /** 上级时间间隔 / The upper-level time interval */
    fun upperInterval(): Ret<Duration> {
        return when (durationUnit) {
            DurationUnit.SECONDS -> Ok(1.toDuration(DurationUnit.MINUTES))
            DurationUnit.MINUTES -> Ok(1.toDuration(DurationUnit.HOURS))
            DurationUnit.HOURS -> Ok(1.toDuration(DurationUnit.DAYS))
            else -> Failed(ErrorCode.Other, "TimeWindow.upperInterval 暂不支持 durationUnit=$durationUnit，仅支持 SECONDS/MINUTES/HOURS。")
        }
    }

    private fun Int64.timeWindowValue() = toLong().toDouble()

    private fun UInt64.timeWindowValue() = toLong().toDouble()

    /** 上级时间窗口（nullable） / The upper-level time window (nullable) */
    val upperOrNull: TimeWindow<V>? by lazy {
        when (durationUnit) {
            DurationUnit.SECONDS -> DurationUnit.MINUTES
            DurationUnit.MINUTES -> DurationUnit.HOURS
            DurationUnit.HOURS -> DurationUnit.DAYS
            else -> null
        }?.let { upperUnit ->
            TimeWindow(
                window = window,
                continues = continues,
                durationUnit = upperUnit,
                interval = upperIntervalOrNull ?: interval,
                fromDouble = fromDouble,
                toDouble = toDouble
            )
        }
    }

    /** 上级时间窗口 / The upper-level time window */
    fun upper(): Ret<TimeWindow<V>> {
        val upperUnit = when (durationUnit) {
            DurationUnit.SECONDS -> DurationUnit.MINUTES
            DurationUnit.MINUTES -> DurationUnit.HOURS
            DurationUnit.HOURS -> DurationUnit.DAYS
            else -> return Failed(ErrorCode.Other, "TimeWindow.upper 暂不支持 durationUnit=$durationUnit，仅支持 SECONDS/MINUTES/HOURS。")
        }
        return Ok(TimeWindow(
            window = window,
            continues = continues,
            durationUnit = upperUnit,
            interval = upperIntervalOrNull!!,
            fromDouble = fromDouble,
            toDouble = toDouble
        ))
    }

    /**
     * 按缩放比例计算上级时间间隔 / Calculate upper interval by scale
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
     */
    fun upperByScale(scale: UInt64): TimeWindow<V> {
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
            interval = upperInterval,
            fromDouble = fromDouble,
            toDouble = toDouble
        )
    }

    /** 按默认间隔划分的时间段列表 / List of time slots divided by default interval */
    val timeSlots: List<TimeRange> by lazy {
        timeSlotsOf(interval)
    }

    /**
     * 按指定间隔划分时间段 / Divide time slots by the specified interval
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
        roundTimeSlotsOf(upperOrNull?.interval ?: interval)
    }

    /**
     * 按指定间隔生成舍入时间段 / Generate rounded time slots by the specified interval
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
     * 生成基于时间粒度和排除时间的舍入时间段。
     */
    fun roundTimeSlotsOf(
        intervals: Map<TimeRange?, Duration>,
        excludedTimes: List<TimeRange> = emptyList()
    ): List<TimeRange> {
        val timeSlots = ArrayList<TimeRange>()
        val defaultInterval = intervals[null] ?: upperIntervalOrNull ?: interval
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
        val upperDurationUnit = upperOrNull?.durationUnit ?: durationUnit
        val upperIntervalVal = upperOrNull?.interval ?: interval
        val end1 = (start.truncatedTo(upperDurationUnit) + currentInterval * kotlin.math.ceil(upperIntervalVal / currentInterval).toInt())
            .let {
                if ((it - start) < currentInterval) {
                    it + upperIntervalVal
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
        val end2 = end.truncatedTo(upperDurationUnit)
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
     */
    fun withIntersection(ano: TimeRange): Boolean {
        return window.withIntersection(ano)
    }

    /**
     * 判断是否包含指定时间点 / Check whether this window contains the specified instant
     */
    fun contains(time: Instant): Boolean {
        return window.contains(time)
    }

    /**
     * 判断是否包含指定时间范围 / Check whether this window contains the specified time range
     */
    fun contains(time: TimeRange): Boolean {
        return window.contains(time)
    }

    /**
     * 按指定时间点拆分窗口 / Split the window at the specified instants
     */
    fun split(
        times: List<Instant>
    ): List<TimeRange> {
        return window.split(times)
    }

    /**
     * 按持续时间单元拆分窗口 / Split the window by duration unit
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
     */
    fun rsplit(
        unit: DurationRange,
        maxDuration: Duration? = null,
        breakTime: Duration? = null
    ): Ret<TimeRange.SplitTimeRanges> {
        return window.rsplit(
            unit = unit,
            maxDuration = maxDuration,
            breakTime = breakTime
        )
    }

    /**
     * 计算给定时间所属的日期 / Calculate the date to which the given time belongs
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
     */
    fun new(window: TimeRange, continues: Boolean): TimeWindow<V> {
        return TimeWindow(
            window = window,
            continues = continues,
            durationUnit = durationUnit,
            fromDouble = fromDouble,
            toDouble = toDouble
        )
    }
}
