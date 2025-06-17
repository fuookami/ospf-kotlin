package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.time.*

data class DurationRange(
    val lb: Duration,
    val ub: Duration
) {
    constructor(value: Duration) : this(value, value)
}
