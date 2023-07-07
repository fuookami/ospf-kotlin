package fuookami.ospf.kotlin.framework.gantt_scheduling.model

import kotlin.time.*

class WorkingCalendar(
    val timeWindow: TimeRange,
    unavailableTimes: List<TimeRange> = emptyList()
) {
    val unavailableTimes = unavailableTimes.sortedBy { it.start }

    fun actualTime(time: TimeRange, connectionTime: Duration = Duration.ZERO): TimeRange {
        var currentTime = time
        for (unavailableTime in unavailableTimes) {
            val intersection = currentTime.intersectionWith(unavailableTime)
            if (intersection.empty && intersection.start >= currentTime.start) {
                break
            }
            if (!intersection.empty) {
                currentTime = TimeRange(
                    start = currentTime.start,
                    end = max(currentTime.end, unavailableTime.end) + intersection.duration + connectionTime
                )
            }
        }
        return currentTime
    }
}
