package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.time.*
import kotlinx.datetime.*

interface TimeSlot {
    val time: TimeRange
    val start: Instant get() = time.start
    val end: Instant get() = time.end
    val duration: Duration get() = time.duration
}
