package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlinx.datetime.*

data class LocalDateOffset(
    val offset: LocalTime
) {
    fun date(time: LocalDateTime): LocalDate {
        return if (time.time < offset) {
            time.date.minus(DatePeriod(days = 1))
        } else {
            time.date
        }
    }
}
