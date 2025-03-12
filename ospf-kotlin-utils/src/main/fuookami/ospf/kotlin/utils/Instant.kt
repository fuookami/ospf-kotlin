package fuookami.ospf.kotlin.utils

import kotlin.time.*
import kotlinx.datetime.*

fun Instant.truncatedTo(unit: DurationUnit): Instant {
    return this
        .toJavaInstant()
        .truncatedTo(unit.toTimeUnit().toChronoUnit())
        .toKotlinInstant()
}
