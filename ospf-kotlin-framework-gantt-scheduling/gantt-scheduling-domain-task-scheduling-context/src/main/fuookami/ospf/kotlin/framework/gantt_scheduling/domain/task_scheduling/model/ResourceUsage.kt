package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

interface ResourceTimeSlot<R : Resource<C>, C : ResourceCapacity> : Indexed {
    val resource: R
    val resourceCapacity: C
    val time: TimeRange
    val indexInRule: UInt64

    fun <E : Executor, A : AssignmentPolicy<E>> relatedTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): Boolean {
        return false
    }

    fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> invoke(bunch: AbstractTaskBunch<T, E, A>): Flt64 {
        return resource.usedQuantity(bunch, time)
    }
}

interface ResourceUsage<S : ResourceTimeSlot<R, C>, R : Resource<C>, C : ResourceCapacity> {
    val name: String

    val timeSlots: List<S>
    val quantity: LinearSymbols1
    val overQuantity: LinearSymbols1
    val lessQuantity: LinearSymbols1

    val overEnabled: Boolean
    val lessEnabled: Boolean

    fun register(model: LinearMetaModel): Try
}

abstract class AbstractResourceUsage<S : ResourceTimeSlot<R, C>, R : Resource<C>, C : ResourceCapacity>
    : ResourceUsage<S, R, C> {
    override lateinit var overQuantity: LinearSymbols1
    override lateinit var lessQuantity: LinearSymbols1

    override fun register(model: LinearMetaModel): Try {
        if (timeSlots.isNotEmpty()) {
            if (overEnabled) {
                if (!::overQuantity.isInitialized) {
                    overQuantity = LinearSymbols1(
                        "${name}_over_quantity",
                        Shape1(timeSlots.size)
                    ) { (i, _) ->
                        val slot = timeSlots[i]
                        if (slot.resourceCapacity.overEnabled) {
                            val slack = SlackFunction(
                                UContinuous,
                                x = LinearPolynomial(quantity[slot]),
                                threshold = LinearPolynomial(slot.resourceCapacity.quantity.upperBound.toFlt64()),
                                constraint = false,
                                name = "${name}_over_quantity_$slot"
                            )
                            slot.resourceCapacity.overQuantity?.let {
                                (slack.pos as URealVar).range.leq(it)
                            }
                            slack
                        } else {
                            LinearExpressionSymbol(LinearPolynomial(), "${name}_over_quantity_$slot")
                        }
                    }
                }
                model.addSymbols(overQuantity)
            }

            if (lessEnabled) {
                if (!::lessQuantity.isInitialized) {
                    lessQuantity = LinearSymbols1(
                        "${name}_less_quantity",
                        Shape1(timeSlots.size)
                    ) { (i, _) ->
                        val slot = timeSlots[i]
                        if (slot.resourceCapacity.lessEnabled) {
                            val slack = SlackFunction(
                                UContinuous,
                                x = LinearPolynomial(quantity[slot]),
                                threshold = LinearPolynomial(slot.resourceCapacity.quantity.lowerBound.toFlt64()),
                                withPositive = false,
                                constraint = false,
                                name = "${name}_less_quantity_$slot"
                            )
                            slot.resourceCapacity.lessQuantity?.let {
                                (slack.neg as URealVar).range.leq(it)
                            }
                            slack
                        } else {
                            LinearExpressionSymbol(LinearPolynomial(), "${name}_less_quantity_$slot")
                        }
                    }
                }
                model.addSymbols(lessQuantity)
            }
        }

        return Ok(success)
    }
}

// connection

data class ConnectionResourceTimeSlot<R : ConnectionResource<C>, C : ResourceCapacity>(
    override val resource: R,
    override val resourceCapacity: C,
    override val time: TimeRange,
    override val indexInRule: UInt64,
) : ResourceTimeSlot<R, C>, AutoIndexed(ConnectionResourceTimeSlot::class) {
    fun <E : Executor, A : AssignmentPolicy<E>> usedBy(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): Flt64 {
        return resource.usedBy(prevTask, task, time)
    }

    override fun <E : Executor, A : AssignmentPolicy<E>> relatedTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): Boolean {
        return usedBy(prevTask, task) neq Flt64.zero
    }

    override fun toString() = "${resource}_${indexInRule}"
}

typealias ConnectionResourceUsage<R, C> = ResourceUsage<ConnectionResourceTimeSlot<R, C>, R, C>

abstract class AbstractConnectionResourceUsage<R : ConnectionResource<C>, C : ResourceCapacity>(
    protected val timeWindow: TimeWindow,
    resources: List<R>,
    interval: Duration = timeWindow.interval
) : AbstractResourceUsage<ConnectionResourceTimeSlot<R, C>, R, C>() {
    final override val timeSlots: List<ConnectionResourceTimeSlot<R, C>>

    init {
        AutoIndexed.flush<ConnectionResourceTimeSlot<R, C>>()

        val timeSlots = ArrayList<ConnectionResourceTimeSlot<R, C>>()
        for (resource in resources) {
            for (capacity in resource.capacities) {
                var index = UInt64.zero
                var beginTime = maxOf(capacity.time.start, timeWindow.window.start)
                val endTime = minOf(capacity.time.end, timeWindow.window.end)
                while (beginTime < endTime) {
                    val thisInterval = minOf(endTime - beginTime, capacity.interval, interval)
                    val time = TimeRange(beginTime, beginTime + thisInterval)
                    timeSlots.add(ConnectionResourceTimeSlot(resource, capacity, time, index))
                    beginTime += thisInterval
                    ++index
                }
            }
        }
        this.timeSlots = timeSlots
    }
}

class TaskSchedulingConnectionResourceUsage<R : ConnectionResource<C>, C : ResourceCapacity>(
    timeWindow: TimeWindow,
    resources: List<R>,
    interval: Duration = timeWindow.interval,
    override val name: String,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = false
) : AbstractConnectionResourceUsage<R, C>(timeWindow, resources, interval) {
    override lateinit var quantity: LinearSymbols1

    override fun register(model: LinearMetaModel): Try {
        TODO("NOT IMPLEMENT YET")
    }
}

// execution

data class ExecutionResourceTimeSlot<R : ExecutionResource<C>, C : ResourceCapacity>(
    override val resource: R,
    override val resourceCapacity: C,
    override val time: TimeRange,
    override val indexInRule: UInt64,
) : ResourceTimeSlot<R, C>, AutoIndexed(ExecutionResourceTimeSlot::class) {
    fun <E : Executor, A : AssignmentPolicy<E>> usedBy(task: AbstractTask<E, A>): Flt64 {
        return resource.usedBy(task)
    }

    override fun <E : Executor, A : AssignmentPolicy<E>> relatedTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): Boolean {
        return task != null && usedBy(task) neq Flt64.zero
    }

    override fun toString() = "${resource}_${indexInRule}"
}

typealias ExecutionResourceUsage<R, C> = ResourceUsage<ExecutionResourceTimeSlot<R, C>, R, C>

abstract class AbstractExecutionResourceUsage<R : ExecutionResource<C>, C : ResourceCapacity>(
    protected val timeWindow: TimeWindow,
    resources: List<R>,
    interval: Duration = timeWindow.interval
) : AbstractResourceUsage<ExecutionResourceTimeSlot<R, C>, R, C>() {
    final override val timeSlots: List<ExecutionResourceTimeSlot<R, C>>

    init {
        AutoIndexed.flush<ExecutionResourceTimeSlot<R, C>>()

        val timeSlots = ArrayList<ExecutionResourceTimeSlot<R, C>>()
        for (resource in resources) {
            for (capacity in resource.capacities) {
                var index = UInt64.zero
                var beginTime = maxOf(capacity.time.start, timeWindow.window.start)
                val endTime = minOf(capacity.time.end, timeWindow.window.end)
                while (beginTime < endTime) {
                    val thisInterval = minOf(endTime - beginTime, capacity.interval, interval)
                    val time = TimeRange(beginTime, beginTime + thisInterval)
                    timeSlots.add(ExecutionResourceTimeSlot(resource, capacity, time, index))
                    beginTime += thisInterval
                    ++index
                }
            }
        }
        this.timeSlots = timeSlots
    }
}

class TaskSchedulingExecutionResourceUsage<R : ExecutionResource<C>, C : ResourceCapacity>(
    timeWindow: TimeWindow,
    resources: List<R>,
    interval: Duration = timeWindow.interval,
    override val name: String,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = false
) : AbstractExecutionResourceUsage<R, C>(timeWindow, resources, interval) {
    override lateinit var quantity: LinearSymbols1

    override fun register(model: LinearMetaModel): Try {
        TODO("NOT IMPLEMENT YET")
    }
}

// storage

data class StorageResourceTimeSlot<R : StorageResource<C>, C : ResourceCapacity>(
    override val resource: R,
    override val resourceCapacity: C,
    override val time: TimeRange,
    override val indexInRule: UInt64,
) : ResourceTimeSlot<R, C>, AutoIndexed(StorageResourceTimeSlot::class) {
    fun <E : Executor, A : AssignmentPolicy<E>> costBy(task: AbstractTask<E, A>): Flt64 {
        return resource.costBy(task, time)
    }

    fun <E : Executor, A : AssignmentPolicy<E>> supplyBy(task: AbstractTask<E, A>): Flt64 {
        return resource.supplyBy(task, time)
    }

    override fun <E : Executor, A : AssignmentPolicy<E>> relatedTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): Boolean {
        return task != null && (costBy(task) neq Flt64.zero || supplyBy(task) neq Flt64.zero)
    }

    override fun toString() = "${resource}_${indexInRule}"
}

typealias StorageResourceUsage<R, C> = ResourceUsage<StorageResourceTimeSlot<R, C>, R, C>

abstract class AbstractStorageResourceUsage<R : StorageResource<C>, C : ResourceCapacity>(
    protected val timeWindow: TimeWindow,
    resources: List<R>,
    interval: Duration = timeWindow.interval
) : AbstractResourceUsage<StorageResourceTimeSlot<R, C>, R, C>() {
    final override val timeSlots: List<StorageResourceTimeSlot<R, C>>

    init {
        AutoIndexed.flush<StorageResourceTimeSlot<R, C>>()

        val timeSlots = ArrayList<StorageResourceTimeSlot<R, C>>()
        for (resource in resources) {
            for (capacity in resource.capacities) {
                var index = UInt64.zero
                var beginTime = maxOf(capacity.time.start, timeWindow.window.start)
                val endTime = minOf(capacity.time.end, timeWindow.window.end)
                while (beginTime < endTime) {
                    val thisInterval = minOf(endTime - beginTime, capacity.interval, interval)
                    val time = TimeRange(beginTime, beginTime + thisInterval)
                    timeSlots.add(StorageResourceTimeSlot(resource, capacity, time, index))
                    beginTime += thisInterval
                    ++index
                }
            }
        }
        this.timeSlots = timeSlots
    }
}

class TaskSchedulingStorageResourceUsage<R : StorageResource<C>, C : ResourceCapacity>(
    timeWindow: TimeWindow,
    resources: List<R>,
    interval: Duration = timeWindow.interval,
    override val name: String,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = false
) : AbstractStorageResourceUsage<R, C>(timeWindow, resources, interval) {
    override lateinit var quantity: LinearSymbols1

    override fun register(model: LinearMetaModel): Try {
        TODO("NOT IMPLEMENT YET")
    }
}
