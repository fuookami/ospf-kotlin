@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

enum class CrewType {
    Operator,
    Attendant,
    Other
}

sealed interface CrewMember {
    val type: CrewType
    val workerNo: WorkerNo?
    val name: String
    val displayName: String?
    val nationality: String
}

data class CrewPilotMember(
    override val type: CrewType,
    val rank: PilotRank,
    val pilot: Pilot
) : CrewMember {
    override val workerNo by pilot::workerNo
    override val name by pilot::name
    override val displayName by pilot::displayName
    override val nationality by pilot::nationality

    override fun toString(): String {
        return "${rank}_${pilot}"
    }
}

data class CrewNotPilotMember(
    override val type: CrewType,
    val rank: CrewManRank,
    val crewMan: CrewMan
) : CrewMember {
    override val workerNo by crewMan::workerNo
    override val name by crewMan::name
    override val displayName by crewMan::displayName
    override val nationality by crewMan::nationality

    override fun toString(): String {
        return "${rank}_${crewMan}"
    }
}

data class Crew(
    val flight: FlightTask,
    val members: List<CrewMember>
) {
    val pilotMembers: Map<PilotRank, List<Pilot>> by lazy {
        members.filterIsInstance<CrewPilotMember>()
           .groupBy { it.rank }
           .mapValues { (rank, members) -> members.map { it.pilot } }
    }

    val notPilotMembers: Map<CrewManRank, List<CrewMan>> by lazy {
        members.filterIsInstance<CrewNotPilotMember>()
           .groupBy { it.rank }
           .mapValues { (rank, members) -> members.map { it.crewMan } }
    }
}
