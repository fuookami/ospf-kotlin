@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlinx.datetime.Instant
import kotlin.time.Duration

interface TimeSlot {
    val time: TimeRange
    val start: Instant get() = time.start
    val end: Instant get() = time.end
    val duration: Duration get() = time.duration

    fun subOf(subTime: TimeRange): TimeSlot?
}
