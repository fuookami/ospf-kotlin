@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model

import kotlin.time.*
import java.util.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/** 评估航班任务是否匹配流量控制标准的条件接口。Condition interface for evaluating whether a flight task matches flow control criteria. */
interface AbstractFlowControlCondition {
    /**
     * 评估给定任务是否匹配此条件。Evaluates whether the given task matches this condition.
     *
     * @param task 参数。
     * @return 返回结果。
     */
    operator fun invoke(task: FlightTask): Boolean
}

/**
 * 按航班类型和飞机子机型过滤的具体流量控制条件。Concrete flow control condition filtering by flight types and aircraft minor types.
 *
 * @property flightTypes 参数。
 */
data class FlowControlCondition(
    val flightTypes: Set<FlightType> = emptySet(),
    val aircraftMinorTypes: Set<AircraftMinorType> = emptySet()
) {
    /**
     * 评估给定任务是否匹配此条件。Evaluates whether the given task matches this condition.
     *
     * @param task 参数。
     * @return 返回结果。
     */
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

/** 描述任务何时与机场交互的流量控制场景枚举。Enumerates the flow control scenes describing when tasks interact with an airport. */
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

    /** 评估给定任务对在指定机场和时间是否匹配此场景。Evaluates whether the given task pair matches this scene at the specified airport and time. */
    abstract operator fun invoke(
        prevTask: FlightTask?,
        task: FlightTask?,
        airport: Airport,
        time: TimeRange,
        condition: AbstractFlowControlCondition? = null
    ): Boolean

    /** 计算束中在指定机场和时间匹配此场景的任务数量。Counts how many tasks in the bunch match this scene at the specified airport and time. */
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

/**
 * 流量控制的容量规格（包括数量和时间间隔）。Capacity specification for a flow control, including amount and time interval.
 *
 * @property amount 参数。
 * @property interval 参数。
 */
data class FlowControlCapacity(
    val amount: UInt64,
    val interval: Duration,
) {
    companion object {
        /**
         * 为给定时间范围创建关闭（零容量）的流量控制。Creates a closed (zero capacity) flow control for the given time range.
         *
         * @param time 参数。
         */
        fun close(time: TimeRange) = FlowControlCapacity(UInt64.zero, time.duration)
    }

    val closed = amount == UInt64.zero

    override fun toString() = if (closed) "closed" else "${amount}_${interval.toInt(DurationUnit.MINUTES)}m"
}

/**
 * 指定机场在给定场景和时间范围内容量限制的流量控制规则。A flow control rule specifying capacity limits at an airport for a given scene and time range.
 *
 * @property id 参数。
 */
data class FlowControl(
    val id: String = UUID.randomUUID().toString(),
    val airport: Airport,
    override val time: TimeRange,
    val condition: AbstractFlowControlCondition? = null,
    val scene: FlowControlScene,
    val capacity: FlowControlCapacity,
    override val name: String = "${airport.icao}_${scene}_${capacity}_${time.start.toShortString()}_${time.end.toShortString()}"
) : AbstractResourceCapacity<FltX> {
    val closed by capacity::closed

    override val quantityRangeValue = Quantity(
        ValueRange(FltX.zero, capacity.amount.toFltX()).value!!,
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

/**
 * 表示机场特定场景的一组流量控制规则的流量资源。A flow resource representing a set of flow control rules at an airport for a specific scene.
 *
 * @property id 参数。
 */
class Flow(
    id: String = UUID.randomUUID().toString(),
    val airport: Airport,
    val scene: FlowControlScene,
    capacities: List<FlowControl>,
) : ConnectionResource<FlowControl, FltX>(
    id = ResourceIdImpl(id),
    name = "${airport}_flow",
    capacities = capacities,
    initialQuantityValue = FltX.zero,
) {
    init {
        assert(capacities.all { it.airport == airport && it.scene == scene })
    }

    override fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedBy(
        prevTask: T?,
        task: T?,
        time: TimeRange
    ): FltX {
        return FltX(capacities.count {
            scene(
                prevTask = prevTask as FlightTask?,
                task = task as FlightTask?,
                airport = airport,
                time = time,
                condition = it.condition
            )
        }.toLong())
    }
}
