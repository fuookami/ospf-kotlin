package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

open class ResourceCapacity(
    val time: TimeRange,
    val quantity: ValueRange<Flt64>,
    val lessEnabled: Boolean = false,
    val overEnabled: Boolean = false,
    val interval: Duration = Duration.INFINITE,
) {
    override fun toString() = "($quantity)_${interval}"
}

abstract class Resource<C : ResourceCapacity>() : ManualIndexed() {
    abstract val id: String
    abstract val name: String
    abstract val capacities: List<C>
    abstract val quantity: Flt64

    abstract fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantity(
        bunch: AbstractTaskBunch<T, E, A>,
        time: TimeRange
    ): Flt64
}

abstract class ExecutionResource<C : ResourceCapacity>(
    override val id: String,
    override val name: String,
    override val capacities: List<C>,
    override val quantity: Flt64 = Flt64.one
) : Resource<C>() {
    abstract fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedBy(
        task: T
    ): Flt64

    override fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantity(
        bunch: AbstractTaskBunch<T, E, A>,
        time: TimeRange
    ): Flt64 {
        var counter = Flt64.zero
        for (i in bunch.tasks.indices) {
            counter += usedBy(bunch[i])
        }
        return counter
    }
}

abstract class ConnectionResource<C : ResourceCapacity>(
    override val id: String,
    override val name: String,
    override val capacities: List<C>,
    override val quantity: Flt64 = Flt64.one
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

abstract class StorageResource<C : ResourceCapacity>(
    override val id: String,
    override val name: String,
    override val capacities: List<C>,
    override val quantity: Flt64 = Flt64.one
) : Resource<C>() {
    abstract fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> costBy(
        task: T,
        time: Duration
    ): Flt64

    open fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> costBy(
        task: T,
        time: TimeRange
    ): Flt64 {
        return this.costBy(task, time.duration)
    }

    abstract fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> supplyBy(
        task: T,
        time: Duration
    ): Flt64

    open fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> supplyBy(
        task: T,
        time: TimeRange
    ): Flt64 {
        return this.supplyBy(task, time.duration)
    }

    override fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantity(
        bunch: AbstractTaskBunch<T, E, A>,
        time: TimeRange
    ): Flt64 {
        var counter = Flt64.zero
        for (i in bunch.tasks.indices) {
            counter -= costBy(bunch.tasks[i], time)
            counter += supplyBy(bunch.tasks[i], time)
        }
        return counter
    }
}
