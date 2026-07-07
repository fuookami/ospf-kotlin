@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.utils

import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.plus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlin.time.Instant

/**
 * Time extension functions providing utilities for Instant, Duration, LocalDate, and LocalDateTime.
 * 中文为 Instant、Duration、LocalDate 和 LocalDateTime 提供扩展功能。
 */

/**
 * Returns the maximum of two Instant values.
 * 中文返回两个 Instant 值中的最大值。
 *
 * @param lhs the left-hand side instant / 左侧时间戳
 * @param rhs the right-hand side instant / 右侧时间戳
 * @return the larger instant / 较大的时间戳
 */
@OptIn(ExperimentalTime::class)
fun max(lhs: Instant, rhs: Instant): Instant {
    return if (lhs <= rhs) {
        rhs
    } else {
        lhs
    }
}

/**
 * Returns the minimum of two Instant values.
 * 中文返回两个 Instant 值中的最小值。
 *
 * @param lhs the left-hand side instant / 左侧时间戳
 * @param rhs the right-hand side instant / 右侧时间戳
 * @return the smaller instant / 较小的时间戳
 */
@OptIn(ExperimentalTime::class)
fun min(lhs: Instant, rhs: Instant): Instant {
    return if (lhs <= rhs) {
        lhs
    } else {
        rhs
    }
}

/**
 * Returns the maximum of two Duration values.
 * 中文返回两个 Duration 值中的最大值。
 *
 * @param lhs the left-hand side duration / 左侧持续时间
 * @param rhs the right-hand side duration / 右侧持续时间
 * @return the larger duration / 较大的持续时间
 */
fun max(lhs: Duration, rhs: Duration): Duration {
    return if (lhs <= rhs) {
        rhs
    } else {
        lhs
    }
}

/**
 * Returns the minimum of two Duration values.
 * 中文返回两个 Duration 值中的最小值。
 *
 * @param lhs the left-hand side duration / 左侧持续时间
 * @param rhs the right-hand side duration / 右侧持续时间
 * @return the smaller duration / 较小的持续时间
 */
fun min(lhs: Duration, rhs: Duration): Duration {
    return if (lhs <= rhs) {
        lhs
    } else {
        rhs
    }
}

/**
 * Truncates the instant to the specified time unit.
 * 中文将 Instant 截断到指定的时间单位。
 *
 * @param unit the time unit to truncate to / 时间单位
 * @return the truncated instant / 截断后的时间戳
 */
@OptIn(ExperimentalTime::class)
fun Instant.truncatedTo(unit: DurationUnit): Instant {
    return this
        .toJavaInstant()
        .truncatedTo(unit.toTimeUnit().toChronoUnit())
        .toKotlinInstant()
}

/**
 * Calculates the sum of all durations in the iterable.
 * 中文计算可迭代对象中所有持续时间的总和。
 *
 * @return the sum of all durations / 持续时间总和
 */
fun Iterable<Duration>.sum(): Duration {
    return this.fold(Duration.ZERO) { acc, duration -> acc + duration }
}

/**
 * Calculates the sum of durations extracted from each element.
 * 中文计算从每个元素提取的持续时间总和。
 *
 * @param extractor function to extract duration from each element / 从元素提取持续时间的函数
 * @return the sum of all extracted durations / 持续时间总和
 */
fun <T> Iterable<T>.sumOf(extractor: (T) -> Duration): Duration {
    return this.fold(Duration.ZERO) { acc, duration -> acc + extractor(duration) }
}

/**
 * Returns the instant of the next day (current instant plus one day).
 * 中文返回下一天的时间戳（当前时间戳加一天）。
 *
 * @return the instant of the next day / 下一天的时间戳
 */
@OptIn(ExperimentalTime::class)
fun Instant.nextDay(): Instant {
    return this + 1.days
}

/**
 * Returns the instant of the previous day (current instant minus one day).
 * 中文返回前一天的时间戳（当前时间戳减一天）。
 *
 * @return the instant of the previous day / 前一天的时间戳
 */
@OptIn(ExperimentalTime::class)
fun Instant.lastDay(): Instant {
    return this - 1.days
}

/**
 * Returns the next day (current date plus one day).
 * 中文返回下一天的日期（当前日期加一天）。
 *
 * @return the next day / 下一天的日期
 */
fun LocalDate.nextDay(): LocalDate {
    return this + DatePeriod(days = 1)
}

/**
 * Returns the previous day (current date minus one day).
 * 中文返回前一天的日期（当前日期减一天）。
 *
 * @return the previous day / 前一天的日期
 */
fun LocalDate.lastDay(): LocalDate {
    return this + DatePeriod(days = -1)
}

/**
 * Returns the next day with the same time.
 * 中文返回下一天的日期时间，保持相同的时间部分。
 *
 * @return the next day with the same time / 下一天的日期时间
 */
fun LocalDateTime.nextDay(): LocalDateTime {
    return LocalDateTime(this.date.nextDay(), this.time)
}

/**
 * Returns the previous day with the same time.
 * 中文返回前一天的日期时间，保持相同的时间部分。
 *
 * @return the previous day with the same time / 前一天的日期时间
 */
fun LocalDateTime.lastDay(): LocalDateTime {
    return LocalDateTime(this.date.lastDay(), this.time)
}
