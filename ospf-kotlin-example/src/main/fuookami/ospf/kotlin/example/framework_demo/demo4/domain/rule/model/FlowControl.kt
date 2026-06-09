@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model

import java.util.*
import kotlin.time.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

interface AbstractFlowControlCondition {
    operator fun invoke(task: FlightTask): Boolean
}

data class FlowControlCondition(
    val flightTypes: Set<FlightType> = emptySet(),
    val aircraftMinorTypes: Set<AircraftMinorType> = emptySet()
) {
    operator fun invoke(task: FlightTask): Boolean {
        if (task.isFlight) {
            return false
        }

        if (flightTypes.isNotEmpty()) {
            val type = when (task) {
                is FlightLeg -> {
                    task.plan.type
                }

                else -> {
                    FlightType(task.dep, task.arr)
                }
            }
            if (!flightTypes.contains(type)) {
                return false
            }
        }

        if (task.aircraft != null && aircraftMinorTypes.isNotEmpty()) {
            if (!aircraftMinorTypes.contains(task.aircraft!!.minorType)) {
                return false
            }
        }

        return true
    }
}

enum class FlowControlScene {
    Departure {
        override operator fun invoke(
            prevTask: FlightTask?,
            task: FlightTask?,
            airport: Airport,
            time: TimeRange,
            condition: AbstractFlowControlCondition?
        ): Boolean {
            return task != null
                    && condition?.invoke(task) != false
                    && task.departedWhen(airport, time)
        }
    },

    Arrival {
        override fun invoke(
            prevTask: FlightTask?,
            task: FlightTask?,
            airport: Airport,
            time: TimeRange,
            condition: AbstractFlowControlCondition?
        ): Boolean {
            return task != null
                    && condition?.invoke(task) != false
                    && task.arrivedWhen(airport, time)
        }
    },

    DepartureArrival {
        override fun invoke(
            prevTask: FlightTask?,
            task: FlightTask?,
            airport: Airport,
            time: TimeRange,
            condition: AbstractFlowControlCondition?
        ): Boolean {
            return task != null
                    && condition?.invoke(task) != false
                    && (task.departedWhen(airport, time) || task.arrivedWhen(airport, time))
        }
    },

    Stay {
        override fun invoke(
            prevTask: FlightTask?,
            task: FlightTask?,
            airport: Airport,
            time: TimeRange,
            condition: AbstractFlowControlCondition?
        ): Boolean {
            return if (prevTask != null && task != null && condition?.invoke(task) != false) {
                task.locatedWhen(prevTask, airport, time)
            } else if (prevTask != null && condition?.invoke(prevTask) != false) {
                prevTask.arrivedWhen(airport, time)
            } else if (task != null && condition?.invoke(task) != false) {
                task.departedWhen(airport, time)
            } else {
                false
            }
        }
    };

    abstract operator fun invoke(
        prevTask: FlightTask?,
        task: FlightTask?,
        airport: Airport,
        time: TimeRange,
        condition: AbstractFlowControlCondition? = null
    ): Boolean

    open operator fun invoke(
        bunch: FlightTaskBunch,
        airport: Airport,
        time: TimeRange,
        condition: AbstractFlowControlCondition? = null
    ): UInt64 {
        return UInt64(bunch.tasks.indices.count { i ->
            if (i == 0) {
                this(null, bunch.tasks[i], airport, time, condition)
            } else {
                this(bunch.tasks[i - 1], bunch.tasks[i], airport, time, condition)
            }
        })
    }
}

data class FlowControlCapacity(
    val amount: UInt64,
    val interval: Duration,
) {
    companion object {
        fun close(time: TimeRange) = FlowControlCapacity(UInt64.zero, time.duration)
    }

    val closed = amount == UInt64.zero

    override fun toString() = if (closed) "closed" else "${amount}_${interval.toInt(DurationUnit.MINUTES)}m"
}

data class FlowControl(
    val id: String = UUID.randomUUID().toString(),
    val airport: Airport,
    override val time: TimeRange,
    val condition: AbstractFlowControlCondition? = null,
    val scene: FlowControlScene,
    val capacity: FlowControlCapacity,
    override val name: String = "${airport.icao}_${scene}_${capacity}_${time.start.toShortString()}_${time.end.toShortString()}"
) : AbstractResourceCapacity<Flt64> {
    val closed by capacity::closed

    override val quantityRangeValue = Quantity(
        ValueRange(Flt64.zero, capacity.amount.toFlt64()).value!!,
        NoneUnit
    )
    override val interval by capacity::interval
    override val lessEnabled = true
    override val overEnabled = true

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FlowControl

        if (airport != other.airport) return false
        if (time != other.time) return false
        if (condition != other.condition) return false
        if (scene != other.scene) return false
        if (capacity != other.capacity) return false

        return true
    }
}

class Flow(
    id: String = UUID.randomUUID().toString(),
    val airport: Airport,
    val scene: FlowControlScene,
    capacities: List<FlowControl>,
) : ConnectionResource<FlowControl, Flt64>(
    id = id,
    name = "${airport}_flow",
    capacities = capacities,
    initialQuantityValue = Flt64.zero,
) {
    init {
        assert(capacities.all { it.airport == airport && it.scene == scene })
    }

    override fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedBy(
        prevTask: T?,
        task: T?,
        time: TimeRange
    ): Flt64 {
        return Flt64(capacities.count {
            scene(
                prevTask = prevTask as FlightTask?,
                task = task as FlightTask?,
                airport = airport,
                time = time,
                condition = it.condition
            )
        })
    }
}
