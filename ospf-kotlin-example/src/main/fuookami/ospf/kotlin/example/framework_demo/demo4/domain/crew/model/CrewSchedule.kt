@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 机组成员的排班，将航班任务映射到其分配的职级（飞行员或机组人员）。A crew member's schedule mapping flight tasks to their assigned rank (pilot or crew man).
 *
 * @property crewMan The crew member this schedule belongs to / 该排班所属的机组成员
 * @property schedules Mapping of flight tasks to assigned ranks / 航班任务到分配职级的映射
*/
data class CrewSchedule(
    val crewMan: AbstractCrewMan,
    val schedules: Map<FlightTask, Either<PilotRank, CrewManRank>>
)
