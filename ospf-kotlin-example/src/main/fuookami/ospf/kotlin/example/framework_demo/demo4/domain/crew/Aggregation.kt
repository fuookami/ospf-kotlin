@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model.*

class Aggregation(
    val crews: List<Crew>,
    val crewSchedules: List<CrewSchedule>,
    val transitTimes: TransitTimeMap
)