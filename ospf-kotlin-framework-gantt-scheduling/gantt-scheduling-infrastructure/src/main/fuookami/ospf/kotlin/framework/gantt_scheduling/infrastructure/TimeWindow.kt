@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

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

data class TimeWindow(
    val window: TimeRange,
    val continues: Boolean = true,
    val durationUnit: DurationUnit = DurationUnit.SECONDS,
    val dateOffset: Duration = Duration.ZERO,
    val interval: Duration = 1.toDuration(durationUnit)
) {
    companion object {
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

    val Duration.value: Flt64 get() = Flt64(this.toDouble(durationUnit))
    val Duration.round: Duration get() = round(this.toDouble(durationUnit)).toDuration(durationUnit)
    val Duration.floor: Duration get() = floor(this.toDouble(durationUnit)).toDuration(durationUnit)
    val Duration.ceil: Duration get() = ceil(this.toDouble(durationUnit)).toDuration(durationUnit)

    fun valueOf(duration: Duration) = duration.value
    fun round(duration: Duration) = duration.round
    fun floor(duration: Duration) = duration.floor
    fun ceil(duration: Duration) = duration.ceil

    val Instant.value: Flt64 get() = Flt64((this - window.start).toDouble(durationUnit))
    val Instant.round: Instant get() = window.start + (this - window.start).round
    val Instant.floor: Instant get() = window.start + (this - window.start).floor
    val Instant.ceil: Instant get() = window.start + (this - window.start).ceil

    fun valueOf(instant: Instant) = instant.value
    fun round(instant: Instant) = instant.round
    fun floor(instant: Instant) = instant.floor
    fun ceil(instant: Instant) = instant.ceil

    val Flt64.duration: Duration get() = this.toDouble().toDuration(durationUnit)
    val Int64.duration: Duration get() = this.toFlt64().duration
    val UInt64.duration: Duration get() = this.toFlt64().duration

    fun durationOf(duration: Flt64) = duration.duration
    fun durationOf(duration: Int64) = duration.toFlt64().duration
    fun durationOf(duration: UInt64) = duration.toFlt64().duration

    val Flt64.instant: Instant get() = window.start + this.toDouble().toDuration(durationUnit)
    val Int64.instant: Instant get() = window.start + this.toFlt64().duration
    val UInt64.instant: Instant get() = window.start + this.toFlt64().duration

    fun instantOf(instant: Flt64) = window.start + instant.duration
    fun instantOf(instant: Int64) = window.start + instant.toFlt64().duration
    fun instantOf(instant: UInt64) = window.start + instant.toFlt64().duration

    val empty: Boolean by window::empty
    val start: Instant by window::start
    val end: Instant by window::end
    val duration: Duration by window::duration
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
                TODO("NOT IMPLEMENT YET")
            }
        }
    }

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
                    TODO("NOT IMPLEMENT YET")
                }
            },
            upperInterval
        )
    }

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

    val timeSlots: List<TimeRange> by lazy {
        timeSlotsOf(interval)
    }

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

    val roundTimeSlots: List<TimeRange> by lazy {
        roundTimeSlotsOf(upper.interval)
    }

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

    fun withIntersection(ano: TimeRange): Boolean {
        return window.withIntersection(ano)
    }

    fun contains(time: Instant): Boolean {
        return window.contains(time)
    }

    fun contains(time: TimeRange): Boolean {
        return window.contains(time)
    }

    fun split(
        times: List<Instant>
    ): List<TimeRange> {
        return window.split(times)
    }

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

    fun new(window: TimeRange, continues: Boolean): TimeWindow {
        return TimeWindow(
            window = window,
            continues = continues,
            durationUnit = durationUnit
        )
    }
}


