package fuookami.ospf.kotlin.framework.gantt_scheduling.model

import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours

data class TimeWindow(
    val window: TimeRange,
    val continues: Boolean,
    private val durationUnit: DurationUnit,
) {
    companion object {
        fun seconds(timeWindow: TimeRange, continues: Boolean) = TimeWindow(
            window = timeWindow,
            continues = continues,
            durationUnit = DurationUnit.SECONDS
        )

        fun minutes(timeWindow: TimeRange, continues: Boolean) = TimeWindow(
            window = timeWindow,
            continues = continues,
            durationUnit = DurationUnit.MINUTES
        )

        fun hours(timeWindow: TimeRange, continues: Boolean) = TimeWindow(
            window = timeWindow,
            continues = continues,
            durationUnit = DurationUnit.HOURS
        )
    }

    fun dump(timePoint: Duration) = Flt64(timePoint.toDouble(durationUnit))
    fun dump(timePoint: Instant) = Flt64((timePoint - window.start).toDouble(durationUnit))
    fun dump(timePoint: Flt64) = window.start + timePoint.toDouble().toDuration(durationUnit)

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
