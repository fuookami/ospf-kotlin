@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

data class CrewSchedule(
    val crewMan: AbstractCrewMan,
    val schedules: Map<FlightTask, Either<PilotRank, CrewManRank>>
)
