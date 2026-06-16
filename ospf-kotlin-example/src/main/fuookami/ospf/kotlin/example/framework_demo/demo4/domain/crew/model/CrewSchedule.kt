@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

import fuookami.ospf.kotlin.utils.functional.*

/** A crew member's schedule mapping flight tasks to their assigned rank (pilot or crew man). */
data class CrewSchedule(
    val crewMan: AbstractCrewMan,
    val schedules: Map<FlightTask, Either<PilotRank, CrewManRank>>
)
