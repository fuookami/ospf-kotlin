package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*

data class TimeWindow(
    val window: TimeRange,
    val continues: Boolean = true,
    val durationUnit: DurationUnit = DurationUnit.SECONDS,
    val interval: Duration = 1.toDuration(durationUnit)
) {
    companion object {
        fun seconds(timeWindow: TimeRange, continues: Boolean = true, interval: Flt64 = Flt64.one): TimeWindow {
            return TimeWindow(
                window = timeWindow,
                continues = continues,
                durationUnit = DurationUnit.SECONDS,
                interval = interval.toDouble().toDuration(DurationUnit.SECONDS)
            )
        }

        fun minutes(timeWindow: TimeRange, continues: Boolean = true, interval: Flt64 = Flt64.one): TimeWindow {
            return TimeWindow(
                window = timeWindow,
                continues = continues,
                durationUnit = DurationUnit.MINUTES,
                interval = interval.toDouble().toDuration(DurationUnit.MINUTES)
            )
        }

        fun hours(timeWindow: TimeRange, continues: Boolean = true, interval: Flt64 = Flt64.one): TimeWindow {
            return TimeWindow(
                window = timeWindow,
                continues = continues,
                durationUnit = DurationUnit.HOURS,
                interval = interval.toDouble().toDuration(DurationUnit.HOURS)
            )
        }
    }
    fun valueOf(duration: Duration) = if (continues) {
        Flt64(duration.toDouble(durationUnit))
    } else {
        Flt64(duration.toInt(durationUnit))
    }

    fun durationOf(duration: Flt64) = if (continues) {
        duration.toDouble().toDuration(durationUnit)
    } else {
        duration.round().toDouble().toDuration(durationUnit)
    }

    fun valueOf(timePoint: Instant) = if (continues) {
        Flt64((timePoint - window.start).toDouble(durationUnit))
    } else {
        Flt64((timePoint - window.start).toInt(durationUnit))
    }

    fun instantOf(timePoint: Flt64) = if (continues) {
        window.start + timePoint.toDouble().toDuration(durationUnit)
    } else {
        window.start + timePoint.round().toDouble().toDuration(durationUnit)
    }

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
        val upperInterval = scale.toInt() * interval
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
        val scaleInterval = scale.toInt() * interval
        val (upperUnit, upperInterval) = if (upperInterval > 1.toDuration(DurationUnit.DAYS) && durationUnit.ordinal < DurationUnit.DAYS.ordinal) {
            Pair(DurationUnit.DAYS, 1.toDuration(DurationUnit.DAYS))
        } else if (upperInterval > 1.toDuration(DurationUnit.HOURS) && durationUnit.ordinal < DurationUnit.HOURS.ordinal) {
            Pair(DurationUnit.HOURS, 1.toDuration(DurationUnit.HOURS))
        } else if (upperInterval > 1.toDuration(DurationUnit.MINUTES) && durationUnit.ordinal < DurationUnit.MINUTES.ordinal) {
            Pair(DurationUnit.MINUTES, 1.toDuration(DurationUnit.MINUTES))
        } else {
            Pair(durationUnit, upperInterval)
        }
        return TimeWindow(
            window = window,
            continues = continues,
            durationUnit = upperUnit,
            interval = upperInterval
        )
    }

    val timeSlots: List<TimeRange> by lazy {
        val timeSlots = ArrayList<TimeRange>()
        var current = this.start
        while (current != end) {
            val duration = min(end - current, interval)
            timeSlots.add(TimeRange(
                start = current,
                end = current + duration
            ))
            current += duration
        }
        timeSlots
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

    fun split(unit: Duration): List<TimeRange> {
        return window.split(unit)
    }

    fun new(window: TimeRange, continues: Boolean): TimeWindow {
        return TimeWindow(
            window = window,
            continues = continues,
            durationUnit = durationUnit
        )
    }
}
