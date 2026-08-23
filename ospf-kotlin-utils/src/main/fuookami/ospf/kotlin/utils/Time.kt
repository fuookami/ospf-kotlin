@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.utils

import java.time.temporal.ChronoUnit
import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.plus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlin.time.Instant

/**
 * Time extension functions providing utilities for Instant, Duration, LocalDate, and LocalDateTime.
 * 为 Instant、Duration、LocalDate 和 LocalDateTime 提供扩展功能。
*/

/**
 * Returns the maximum of two Instant values.
 * 返回两个 Instant 值中的最大值。
 *
 * @param lhs 左侧时间戳 / the left-hand side instant
 * @param rhs 右侧时间戳 / the right-hand side instant
 * @return 较大的时间戳 / the larger instant
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
 * 返回两个 Instant 值中的最小值。
 *
 * @param lhs 左侧时间戳 / the left-hand side instant
 * @param rhs 右侧时间戳 / the right-hand side instant
 * @return 较小的时间戳 / the smaller instant
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
 * 返回两个 Duration 值中的最大值。
 *
 * @param lhs 左侧持续时间 / the left-hand side duration
 * @param rhs 右侧持续时间 / the right-hand side duration
 * @return 较大的持续时间 / the larger duration
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
 * 返回两个 Duration 值中的最小值。
 *
 * @param lhs 左侧持续时间 / the left-hand side duration
 * @param rhs 右侧持续时间 / the right-hand side duration
 * @return 较小的持续时间 / the smaller duration
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
 * 将 Instant 截断到指定的时间单位。
 *
 * @param unit 时间单位 / the time unit to truncate to
 * @return 截断后的时间戳 / the truncated instant
*/
@OptIn(ExperimentalTime::class)
fun Instant.truncatedTo(unit: DurationUnit): Instant {
    return this
        .toJavaInstant()
        .truncatedTo(unit.toChronoUnit())
        .toKotlinInstant()
}

private fun DurationUnit.toChronoUnit(): ChronoUnit {
    return when (this) {
        DurationUnit.NANOSECONDS -> ChronoUnit.NANOS
        DurationUnit.MICROSECONDS -> ChronoUnit.MICROS
        DurationUnit.MILLISECONDS -> ChronoUnit.MILLIS
        DurationUnit.SECONDS -> ChronoUnit.SECONDS
        DurationUnit.MINUTES -> ChronoUnit.MINUTES
        DurationUnit.HOURS -> ChronoUnit.HOURS
        DurationUnit.DAYS -> ChronoUnit.DAYS
    }
}

/**
 * Calculates the sum of all durations in the iterable.
 * 计算可迭代对象中所有持续时间的总和。
 *
 * @return 持续时间总和 / the sum of all durations
*/
fun Iterable<Duration>.sum(): Duration {
    return this.fold(Duration.ZERO) { acc, duration -> acc + duration }
}

/**
 * Calculates the sum of durations extracted from each element.
 * 计算从每个元素提取的持续时间总和。
 *
 * @param extractor 从元素提取持续时间的函数 / function to extract duration from each element
 * @return 持续时间总和 / the sum of all extracted durations
*/
fun <T> Iterable<T>.sumOf(extractor: (T) -> Duration): Duration {
    return this.fold(Duration.ZERO) { acc, duration -> acc + extractor(duration) }
}

/**
 * Returns the instant of the next day (current instant plus one day).
 * 返回下一天的时间戳（当前时间戳加一天）。
 *
 * @return 下一天的时间戳 / the instant of the next day
*/
@OptIn(ExperimentalTime::class)
fun Instant.nextDay(): Instant {
    return this + 1.days
}

/**
 * Returns the instant of the previous day (current instant minus one day).
 * 返回前一天的时间戳（当前时间戳减一天）。
 *
 * @return 前一天的时间戳 / the instant of the previous day
*/
@OptIn(ExperimentalTime::class)
fun Instant.lastDay(): Instant {
    return this - 1.days
}

/**
 * Returns the next day (current date plus one day).
 * 返回下一天的日期（当前日期加一天）。
 *
 * @return 下一天的日期 / the next day
*/
fun LocalDate.nextDay(): LocalDate {
    return this + DatePeriod(days = 1)
}

/**
 * Returns the previous day (current date minus one day).
 * 返回前一天的日期（当前日期减一天）。
 *
 * @return 前一天的日期 / the previous day
*/
fun LocalDate.lastDay(): LocalDate {
    return this + DatePeriod(days = -1)
}

/**
 * Returns the next day with the same time.
 * 返回下一天的日期时间，保持相同的时间部分。
 *
 * @return 下一天的日期时间 / the next day with the same time
*/
fun LocalDateTime.nextDay(): LocalDateTime {
    return LocalDateTime(this.date.nextDay(), this.time)
}

/**
 * Returns the previous day with the same time.
 * 返回前一天的日期时间，保持相同的时间部分。
 *
 * @return 前一天的日期时间 / the previous day with the same time
*/
fun LocalDateTime.lastDay(): LocalDateTime {
    return LocalDateTime(this.date.lastDay(), this.time)
}
