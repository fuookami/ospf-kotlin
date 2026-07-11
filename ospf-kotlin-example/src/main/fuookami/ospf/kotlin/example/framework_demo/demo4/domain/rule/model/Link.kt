@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model

import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** 表示两个连续航班任务之间具有分割成本的链接的密封类。Sealed class representing a link between two consecutive flight tasks with a split cost. */
sealed class Link(
    val type: String
) : ManualIndexed() {
    abstract val prevTask: FlightTask
    abstract val succTask: FlightTask
    abstract val splitCost: FltX

    override fun toString() = "${type}_${prevTask}_${succTask}"
}

/**
 * 两个未恢复航段之间的连接链接。A connecting link between two unrecovered flight legs.
 *
*/
data class ConnectingLink(
    override val prevTask: FlightTask,
    override val succTask: FlightTask,
    override val splitCost: FltX
) : Link("connecting") {
    init {
        assert((prevTask is FlightLeg) && (!prevTask.recovered))
        assert((succTask is FlightLeg) && (!succTask.recovered))
    }

    override fun hashCode(): Int {
        return prevTask.hashCode() xor succTask.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConnectingLink

        if (prevTask != other.prevTask) return false
        if (succTask != other.succTask) return false

        return true
    }
}

/**
 * 具有计算连接时间的两个未恢复航段之间的经停链接。A stopover link between two unrecovered flight legs with computed connection time.
 *
*/
data class StopoverLink(
    override val prevTask: FlightTask,
    override val succTask: FlightTask,
    override val splitCost: FltX
) : Link("stopover") {
    init {
        assert((prevTask is FlightLeg) && (!prevTask.recovered))
        assert((succTask is FlightLeg) && (!succTask.recovered))
    }

    val connectionTime = succTask.scheduledTime!!.start - prevTask.scheduledTime!!.end

    override fun hashCode(): Int {
        return prevTask.hashCode() xor succTask.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StopoverLink

        if (prevTask != other.prevTask) return false
        if (succTask != other.succTask) return false

        return true
    }
}

/**
 * 忽略连接时间约束的两个未恢复航段之间的链接。A link between two unrecovered flight legs that ignores connection time constraints.
 *
*/
data class ConnectionTimeIgnoringLink(
    override val prevTask: FlightTask,
    override val succTask: FlightTask,
    override val splitCost: FltX
) : Link("connection_time_ignoring") {
    init {
        assert((prevTask is FlightLeg) && (!prevTask.recovered))
        assert((succTask is FlightLeg) && (!succTask.recovered))
    }

    override fun hashCode(): Int {
        return prevTask.hashCode() xor succTask.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StopoverLink

        if (prevTask != other.prevTask) return false
        if (succTask != other.succTask) return false

        return true
    }
}

/**
 * 提供按前驱和后继任务查找的所有链接类型映射。A map of all link types providing lookup by predecessor and successor tasks.
 *
 * @property connectingLinks The list of connecting links / 连接链接列表
 * @property stopoverLinks The list of stopover links / 经停链接列表
 * @property connectionTimeIgnoringLinks The list of connection-time-ignoring links / 忽略连接时间的链接列表
*/
class LinkMap(
    val connectingLinks: List<ConnectingLink>,
    val stopoverLinks: List<StopoverLink>,
    val connectionTimeIgnoringLinks: List<ConnectionTimeIgnoringLink>
) {
    val links: List<Link> = connectingLinks + stopoverLinks + connectionTimeIgnoringLinks
    val leftMapper by lazy { links.groupBy { it.prevTask } }
    val rightMapper by lazy { links.groupBy { it.succTask } }

    /**
     * 返回给定任务作为前驱的所有链接。Returns all links where the given task is the predecessor.
     *
     * @param task The flight task to look up / 要查找的航班任务
     * @return All links where the given task is the predecessor / 给定任务作为前驱的所有链接
    */
    fun linksAfter(task: FlightTask): List<Link> {
        return leftMapper[task] ?: emptyList()
    }

    /**
     * 返回给定任务作为后继的所有链接。Returns all links where the given task is the successor.
     *
     * @param task The flight task to look up / 要查找的航班任务
     * @return All links where the given task is the successor / 给定任务作为后继的所有链接
    */
    fun linksBefore(task: FlightTask): List<Link> {
        return rightMapper[task] ?: emptyList()
    }
}
