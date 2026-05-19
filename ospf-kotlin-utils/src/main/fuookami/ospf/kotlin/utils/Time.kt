/**
 * 时间扩展函数
 *
 * Time extension functions providing utilities for Instant, Duration, LocalDate, and LocalDateTime.
 * 为 Instant、Duration、LocalDate 和 LocalDateTime 提供扩展功能。
 */
@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.utils

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlin.time.*
import kotlin.time.Duration.Companion.days

/**
 * 返回两个时间戳中的较大值
 *
 * Returns the maximum of two Instant values.
 * 返回两个 Instant 值中的最大值。
 *
 * @param lhs 第一个时间戳 / The first instant
 * @param rhs 第二个时间戳 / The second instant
 * @return 较大的时间戳 / The larger instant
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
 * 返回两个时间戳中的较小值
 *
 * Returns the minimum of two Instant values.
 * 返回两个 Instant 值中的最小值。
 *
 * @param lhs 第一个时间戳 / The first instant
 * @param rhs 第二个时间戳 / The second instant
 * @return 较小的时间戳 / The smaller instant
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
 * 返回两个持续时间中的较大值
 *
 * Returns the maximum of two Duration values.
 * 返回两个 Duration 值中的最大值。
 *
 * @param lhs 第一个持续时间 / The first duration
 * @param rhs 第二个持续时间 / The second duration
 * @return 较大的持续时间 / The larger duration
 */
fun max(lhs: Duration, rhs: Duration): Duration {
    return if (lhs <= rhs) {
        rhs
    } else {
        lhs
    }
}

/**
 * 返回两个持续时间中的较小值
 *
 * Returns the minimum of two Duration values.
 * 返回两个 Duration 值中的最小值。
 *
 * @param lhs 第一个持续时间 / The first duration
 * @param rhs 第二个持续时间 / The second duration
 * @return 较小的持续时间 / The smaller duration
 */
fun min(lhs: Duration, rhs: Duration): Duration {
    return if (lhs <= rhs) {
        lhs
    } else {
        rhs
    }
}

/**
 * 将时间戳截断到指定的时间单位
 *
 * Truncates the instant to the specified time unit.
 * 将 Instant 截断到指定的时间单位。
 *
 * @param unit 时间单位 / The time unit to truncate to
 * @return 截断后的时间戳 / The truncated instant
 */
@OptIn(ExperimentalTime::class)
fun Instant.truncatedTo(unit: DurationUnit): Instant {
    return this
        .toJavaInstant()
        .truncatedTo(unit.toTimeUnit().toChronoUnit())
        .toKotlinInstant()
}

/**
 * 计算持续时间集合的总和
 *
 * Calculates the sum of all durations in the iterable.
 * 计算可迭代对象中所有持续时间的总和。
 *
 * @return 持续时间总和 / The sum of all durations
 */
fun Iterable<Duration>.sum(): Duration {
    return this.fold(Duration.ZERO) { acc, duration -> acc + duration }
}

/**
 * 使用提取函数计算持续时间总和
 *
 * Calculates the sum of durations extracted from each element.
 * 计算从每个元素提取的持续时间总和。
 *
 * @param extractor 从元素提取持续时间的函数 / Function to extract duration from each element
 * @return 持续时间总和 / The sum of all extracted durations
 */
fun <T> Iterable<T>.sumOf(extractor: (T) -> Duration): Duration {
    return this.fold(Duration.ZERO) { acc, duration -> acc + extractor(duration) }
}

/**
 * 获取下一天的时间戳
 *
 * Returns the instant of the next day (current instant plus one day).
 * 返回下一天的时间戳（当前时间戳加一天）。
 *
 * @return 下一天的时间戳 / The instant of the next day
 */
@OptIn(ExperimentalTime::class)
fun Instant.nextDay(): Instant {
    return this + 1.days
}

/**
 * 获取前一天的时间戳
 *
 * Returns the instant of the previous day (current instant minus one day).
 * 返回前一天的时间戳（当前时间戳减一天）。
 *
 * @return 前一天的时间戳 / The instant of the previous day
 */
@OptIn(ExperimentalTime::class)
fun Instant.lastDay(): Instant {
    return this - 1.days
}

/**
 * 获取下一天的日期
 *
 * Returns the next day (current date plus one day).
 * 返回下一天的日期（当前日期加一天）。
 *
 * @return 下一天的日期 / The next day
 */
fun LocalDate.nextDay(): LocalDate {
    return this + DatePeriod(days = 1)
}

/**
 * 获取前一天的日期
 *
 * Returns the previous day (current date minus one day).
 * 返回前一天的日期（当前日期减一天）。
 *
 * @return 前一天的日期 / The previous day
 */
fun LocalDate.lastDay(): LocalDate {
    return this + DatePeriod(days = -1)
}

/**
 * 获取下一天的日期时间
 *
 * Returns the next day with the same time.
 * 返回下一天的日期时间，保持相同的时间部分。
 *
 * @return 下一天的日期时间 / The next day with the same time
 */
fun LocalDateTime.nextDay(): LocalDateTime {
    return LocalDateTime(this.date.nextDay(), this.time)
}

/**
 * 获取前一天的日期时间
 *
 * Returns the previous day with the same time.
 * 返回前一天的日期时间，保持相同的时间部分。
 *
 * @return 前一天的日期时间 / The previous day with the same time
 */
fun LocalDateTime.lastDay(): LocalDateTime {
    return LocalDateTime(this.date.lastDay(), this.time)
}