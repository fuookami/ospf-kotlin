/**
 * 本地日期偏移，用于计算跨日边界 / Local date offset for cross-day boundary calculation
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlinx.datetime.*

/**
 * 本地日期偏移，当时间早于偏移量时视为前一天 / Local date offset; when time is before the offset, it is considered as the previous day
 *
 * @property offset 日偏移时间 / The daily offset time
 */
data class LocalDateOffset(
    val offset: LocalTime
) {
    /**
     * 根据时间计算所属日期 / Calculate the date to which the given time belongs
     *
     * @param time 本地日期时间 / The local date time
     * @return 所属的本地日期 / The local date
     */
    fun date(time: LocalDateTime): LocalDate {
        return if (time.time < offset) {
            time.date.minus(DatePeriod(days = 1))
        } else {
            time.date
        }
    }
}