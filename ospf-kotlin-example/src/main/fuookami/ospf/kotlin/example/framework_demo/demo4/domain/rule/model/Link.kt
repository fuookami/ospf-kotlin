@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

sealed class Link(
    val type: String
) : ManualIndexed() {
    abstract val prevTask: FlightTask
    abstract val succTask: FlightTask
    abstract val splitCost: Flt64

    override fun toString() = "${type}_${prevTask}_${succTask}"
}

data class ConnectingLink(
    override val prevTask: FlightTask,
    override val succTask: FlightTask,
    override val splitCost: Flt64
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

data class StopoverLink(
    override val prevTask: FlightTask,
    override val succTask: FlightTask,
    override val splitCost: Flt64
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

data class ConnectionTimeIgnoringLink(
    override val prevTask: FlightTask,
    override val succTask: FlightTask,
    override val splitCost: Flt64
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

class LinkMap(
    val connectingLinks: List<ConnectingLink>,
    val stopoverLinks: List<StopoverLink>,
    val connectionTimeIgnoringLinks: List<ConnectionTimeIgnoringLink>
) {
    val links: List<Link> = connectingLinks + stopoverLinks + connectionTimeIgnoringLinks
    val leftMapper by lazy { links.groupBy { it.prevTask } }
    val rightMapper by lazy { links.groupBy { it.succTask } }

    fun linksAfter(task: FlightTask): List<Link> {
        return leftMapper[task] ?: emptyList()
    }

    fun linksBefore(task: FlightTask): List<Link> {
        return rightMapper[task] ?: emptyList()
    }
}
