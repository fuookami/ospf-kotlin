package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*

data class TimeWindow(
    val window: TimeRange,
    val continues: Boolean = true,
    val durationUnit: DurationUnit = DurationUnit.SECONDS,
) {
    companion object {
        fun seconds(timeWindow: TimeRange, continues: Boolean = true) = TimeWindow(
            window = timeWindow,
            continues = continues,
            durationUnit = DurationUnit.SECONDS
        )

        fun minutes(timeWindow: TimeRange, continues: Boolean = true) = TimeWindow(
            window = timeWindow,
            continues = continues,
            durationUnit = DurationUnit.MINUTES
        )

        fun hours(timeWindow: TimeRange, continues: Boolean = true) = TimeWindow(
            window = timeWindow,
            continues = continues,
            durationUnit = DurationUnit.HOURS
        )
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
    val duration: Duration by window::duration

    fun withIntersection(ano: TimeRange): Boolean {
        return window.withIntersection(ano)
    }

    fun contains(time: Instant): Boolean {
        return window.contains(time)
    }

    fun contains(time: TimeRange): Boolean {
        return window.contains(time)
    }

    fun new(window: TimeRange, continues: Boolean): TimeWindow {
        return TimeWindow(
            window = window,
            continues = continues,
            durationUnit = durationUnit
        )
    }
}
