@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 持续时间范围 / Duration range
*/
package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.time.Duration

/**
 * 持续时间范围，表示下界和上界 / Duration range with lower and upper bounds
 *
 * @property lb 下界持续时间 / Lower bound duration
 * @property ub 上界持续时间 / Upper bound duration
*/
data class DurationRange(
    val lb: Duration,
    val ub: Duration
) {

    /**
     * 构造一个上下界相等的持续时间范围 / Construct a duration range where lower and upper bounds are equal
     *
     * @param value 持续时间值 / The duration value
    */
    constructor(value: Duration) : this(value, value)
}