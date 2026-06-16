@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

import fuookami.ospf.kotlin.utils.concept.*

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/** Sealed class representing a link between two consecutive flight tasks with a split cost. */
sealed class Link(
    val type: String
) : ManualIndexed() {
    abstract val prevTask: FlightTask
    abstract val succTask: FlightTask
    abstract val splitCost: FltX

    override fun toString() = "${type}_${prevTask}_${succTask}"
}

/** A connecting link between two unrecovered flight legs. */
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

/** A stopover link between two unrecovered flight legs with computed connection time. */
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

/** A link between two unrecovered flight legs that ignores connection time constraints. */
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

/** A map of all link types providing lookup by predecessor and successor tasks. */
class LinkMap(
    val connectingLinks: List<ConnectingLink>,
    val stopoverLinks: List<StopoverLink>,
    val connectionTimeIgnoringLinks: List<ConnectionTimeIgnoringLink>
) {
    val links: List<Link> = connectingLinks + stopoverLinks + connectionTimeIgnoringLinks
    val leftMapper by lazy { links.groupBy { it.prevTask } }
    val rightMapper by lazy { links.groupBy { it.succTask } }

    /** Returns all links where the given task is the predecessor. */
    fun linksAfter(task: FlightTask): List<Link> {
        return leftMapper[task] ?: emptyList()
    }

    /** Returns all links where the given task is the successor. */
    fun linksBefore(task: FlightTask): List<Link> {
        return rightMapper[task] ?: emptyList()
    }
}
