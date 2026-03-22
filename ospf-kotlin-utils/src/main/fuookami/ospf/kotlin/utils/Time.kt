@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.utils

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlin.time.*
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalTime::class)
fun max(lhs: Instant, rhs: Instant): Instant {
    return if (lhs <= rhs) {
        rhs
    } else {
        lhs
    }
}

@OptIn(ExperimentalTime::class)
fun min(lhs: Instant, rhs: Instant): Instant {
    return if (lhs <= rhs) {
        lhs
    } else {
        rhs
    }
}

fun max(lhs: Duration, rhs: Duration): Duration {
    return if (lhs <= rhs) {
        rhs
    } else {
        lhs
    }
}

fun min(lhs: Duration, rhs: Duration): Duration {
    return if (lhs <= rhs) {
        lhs
    } else {
        rhs
    }
}

@OptIn(ExperimentalTime::class)
fun Instant.truncatedTo(unit: DurationUnit): Instant {
    return this
        .toJavaInstant()
        .truncatedTo(unit.toTimeUnit().toChronoUnit())
        .toKotlinInstant()
}

fun Iterable<Duration>.sum(): Duration {
    return this.fold(Duration.ZERO) { acc, duration -> acc + duration }
}

fun <T> Iterable<T>.sumOf(extractor: (T) -> Duration): Duration {
    return this.fold(Duration.ZERO) { acc, duration -> acc + extractor(duration) }
}

@OptIn(ExperimentalTime::class)
fun Instant.nextDay(): Instant {
    return this + 1.days
}

@OptIn(ExperimentalTime::class)
fun Instant.lastDay(): Instant {
    return this - 1.days
}

fun LocalDate.nextDay(): LocalDate {
    return this + DatePeriod(days = 1)
}

fun LocalDate.lastDay(): LocalDate {
    return this + DatePeriod(days = -1)
}

fun LocalDateTime.nextDay(): LocalDateTime {
    return LocalDateTime(this.date.nextDay(), this.time)
}

fun LocalDateTime.lastDay(): LocalDateTime {
    return LocalDateTime(this.date.lastDay(), this.time)
}
