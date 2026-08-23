@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model.*

/**
 * 机组域对象聚合（包括机组成员、排班和中转时间）。Aggregation of crew domain objects including crew members, schedules, and transit times.
 *
 * @property crews 机组列表 / List of crews
 * @property crewSchedules 机组成员排班列表 / List of crew schedules
 * @property transitTimes 中转时间映射 / Map of transit times
*/
class Aggregation(
    val crews: List<Crew>,
    val crewSchedules: List<CrewSchedule>,
    val transitTimes: TransitTimeMap
)
