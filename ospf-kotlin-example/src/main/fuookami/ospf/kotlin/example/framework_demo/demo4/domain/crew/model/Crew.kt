@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/** 枚举航班恢复系统中的机组成员类型。Enumerates the types of crew members in the flight recovery system. */
enum class CrewType {
    Operator,
    Attendant,
    Other
}

/** 表示具有身份和国籍信息的机组成员的密封接口。Sealed interface representing a crew member with identity and nationality information. */
sealed interface CrewMember {
    val type: CrewType
    val workerNo: WorkerNo?
    val name: String
    val displayName: String?
    val nationality: String
}

/**
 * 作为飞行员的机组成员（将身份字段委托给底层 [Pilot]）。A crew member who is a pilot, delegating identity fields to the underlying [Pilot].
 *
 * @property override val type 参数。
 * @property rank 参数。
 * @property pilot 参数。
 */
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

/**
 * 非飞行员的机组成员（将身份字段委托给底层 [CrewMan]）。A crew member who is not a pilot, delegating identity fields to the underlying [CrewMan].
 *
 * @property override val type 参数。
 * @property rank 参数。
 * @property crewMan 参数。
 */
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

/**
 * 分配给航班任务的机组（由飞行员和非飞行员成员组成）。A crew assigned to a flight task, composed of pilot and non-pilot members.
 *
 * @property flight 参数。
 * @property members 参数。
 */
data class Crew(
    val flight: FlightTask,
    val members: List<CrewMember>
) {
    /** 返回按职级分组的飞行员成员。Returns pilot members grouped by their rank. */
    val pilotMembers: Map<PilotRank, List<Pilot>> by lazy {
        members.filterIsInstance<CrewPilotMember>()
           .groupBy { it.rank }
           .mapValues { (rank, members) -> members.map { it.pilot } }
    }

    /** 返回按职级分组的非飞行员成员。Returns non-pilot members grouped by their rank. */
    val notPilotMembers: Map<CrewManRank, List<CrewMan>> by lazy {
        members.filterIsInstance<CrewNotPilotMember>()
           .groupBy { it.rank }
           .mapValues { (rank, members) -> members.map { it.crewMan } }
    }
}
