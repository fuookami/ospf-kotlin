package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

interface AbstractResourceCapacity {
    val time: TimeRange
    val quantity: ValueRange<Flt64>
    val lessQuantity: Flt64? get() = null
    val overQuantity: Flt64? get() = null
    val interval: Duration
    val name: String? get() = null
    val lessEnabled: Boolean get() = lessQuantity != null
    val overEnabled: Boolean get() = overQuantity != null
}

open class ResourceCapacity(
    override val time: TimeRange,
    override val quantity: ValueRange<Flt64>,
    override val lessQuantity: Flt64? = null,
    override val overQuantity: Flt64? = null,
    override val interval: Duration = Duration.INFINITE,
    override val name: String? = null
): AbstractResourceCapacity {
    override fun toString() = name ?: "${quantity}_${interval}"
}

abstract class Resource<out C : AbstractResourceCapacity> : ManualIndexed() {
    abstract val id: String
    abstract val name: String
    abstract val capacities: List<C>
    abstract val initialQuantity: Flt64

    abstract fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantity(
        bunch: AbstractTaskBunch<T, E, A>,
        time: TimeRange
    ): Flt64
}

abstract class ExecutionResource<out C : AbstractResourceCapacity>(
    override val id: String,
    override val name: String,
    override val capacities: List<C>,
    override val initialQuantity: Flt64 = Flt64.zero
) : Resource<C>() {
    abstract fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedBy(
        task: T,
        time: TimeRange
    ): Flt64

    override fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantity(
        bunch: AbstractTaskBunch<T, E, A>,
        time: TimeRange
    ): Flt64 {
        var counter = Flt64.zero
        for (i in bunch.tasks.indices) {
            counter += usedBy(bunch[i], time)
        }
        return counter
    }
}

abstract class ConnectionResource<out C : AbstractResourceCapacity>(
    override val id: String,
    override val name: String,
    override val capacities: List<C>,
    override val initialQuantity: Flt64 = Flt64.zero
) : Resource<C>() {
    abstract fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedBy(
        prevTask: T?,
        task: T?,
        time: TimeRange
    ): Flt64

    override fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantity(
        bunch: AbstractTaskBunch<T, E, A>,
        time: TimeRange
    ): Flt64 {
        var counter = Flt64.zero
        for (i in bunch.tasks.indices) {
            counter += if (i == 0) {
                usedBy(bunch.lastTask, bunch[i], time)
            } else {
                usedBy(bunch[i - 1], bunch[i], time)
            }
        }
        return counter
    }
}

abstract class StorageResource<out C : AbstractResourceCapacity>(
    override val id: String,
    override val name: String,
    override val capacities: List<C>,
    override val initialQuantity: Flt64 = Flt64.zero
) : Resource<C>() {
    open fun fixedCostIn(time: Duration): Flt64 {
        return Flt64.zero
    }

    open fun fixedCostIn(time: TimeRange): Flt64 {
        return fixedCostIn(time.duration)
    }

    abstract fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> costBy(
        task: T,
        time: Duration
    ): Flt64

    open fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> costBy(
        task: T,
        time: TimeRange
    ): Flt64 {
        val intersectionTime = task.time?.intersectionWith(time) ?: time
        return this.costBy(task, intersectionTime.duration)
    }

    open fun fixedSupplyIn(time: Duration): Flt64 {
        return Flt64.zero
    }

    open fun fixedSupplyIn(time: TimeRange): Flt64 {
        return fixedSupplyIn(time.duration)
    }

    abstract fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> supplyBy(
        task: T,
        time: Duration
    ): Flt64

    open fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> supplyBy(
        task: T,
        time: TimeRange
    ): Flt64 {
        val intersectionTime = task.time?.intersectionWith(time) ?: time
        return this.supplyBy(task, intersectionTime.duration)
    }

    fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantity(
        task: T,
        time: TimeRange
    ): Flt64 {
        return supplyBy(task, time) - costBy(task, time)
    }

    open fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> costBy(
        bunch: AbstractTaskBunch<T, E, A>,
        time: TimeRange
    ): Flt64 {
        var sum = Flt64.zero
        for (i in bunch.tasks.indices) {
            sum += costBy(bunch.tasks[i], time)
            when (val currentTime = bunch.tasks[i].time) {
                is TimeRange -> {
                    if (currentTime.end >= time.end) {
                        break
                    }
                }
            }
        }
        return sum
    }

    open fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> supplyBy(
        bunch: AbstractTaskBunch<T, E, A>,
        time: TimeRange
    ): Flt64 {
        var sum = Flt64.zero
        for (i in bunch.tasks.indices) {
            sum += supplyBy(bunch.tasks[i], time)
            when (val currentTime = bunch.tasks[i].time) {
                is TimeRange -> {
                    if (currentTime.end >= time.end) {
                        break
                    }
                }
            }
        }
        return sum
    }

    override fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantity(
        bunch: AbstractTaskBunch<T, E, A>,
        time: TimeRange
    ): Flt64 {
        return supplyBy(bunch, time) - costBy(bunch, time)
    }
}
