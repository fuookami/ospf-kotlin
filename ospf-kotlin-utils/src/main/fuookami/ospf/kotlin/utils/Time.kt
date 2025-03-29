package fuookami.ospf.kotlin.utils

import kotlin.time.*
import kotlinx.datetime.*

fun Instant.truncatedTo(unit: DurationUnit): Instant {
    return this
        .toJavaInstant()
        .truncatedTo(unit.toTimeUnit().toChronoUnit())
        .toKotlinInstant()
}

fun Iterable<Duration>.sum(): Duration {
    return this.fold(Duration.ZERO) { acc, duration -> acc + duration }
}

fun<T> Iterable<T>.sumOf(extractor: (T) -> Duration): Duration {
    return this.fold(Duration.ZERO) { acc, duration -> acc + extractor(duration) }
}
