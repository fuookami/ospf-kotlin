@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model.*

/**
 * 机组域对象聚合（包括机组成员、排班和中转时间）。Aggregation of crew domain objects including crew members, schedules, and transit times.
 *
 * @property crews 参数。
 * @property crewSchedules 参数。
 * @property transitTimes 参数。
 */
class Aggregation(
    val crews: List<Crew>,
    val crewSchedules: List<CrewSchedule>,
    val transitTimes: TransitTimeMap
)
