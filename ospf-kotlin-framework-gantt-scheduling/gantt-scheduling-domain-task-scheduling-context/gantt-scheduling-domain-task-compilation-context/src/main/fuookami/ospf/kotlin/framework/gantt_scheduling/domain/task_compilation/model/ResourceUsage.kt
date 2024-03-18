package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import kotlin.time.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

interface ResourceTimeSlot<
    out R : Resource<C>,
    out C : ResourceCapacity
> : Indexed {
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

    fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> invoke(
        bunch: AbstractTaskBunch<T, E, A>
    ): Flt64 {
        return resource.usedQuantity(bunch, time)
    }
}

interface ResourceUsage<
    out S : ResourceTimeSlot<R, C>,
    out R : Resource<C>,
    out C : ResourceCapacity
> {
    val name: String

    val timeSlots: List<S>
    val quantity: LinearSymbols1
    val overQuantity: LinearSymbols1
    val lessQuantity: LinearSymbols1

    val overEnabled: Boolean
    val lessEnabled: Boolean

    fun register(model: LinearMetaModel): Try
}

abstract class AbstractResourceUsage<
    out S : ResourceTimeSlot<R, C>,
    out R : Resource<C>,
    out C : ResourceCapacity
> : ResourceUsage<S, R, C> {
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

        return ok
    }
}

// connection

data class ConnectionResourceTimeSlot<
    out R : ConnectionResource<C>,
    out C : ResourceCapacity
>(
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

abstract class AbstractConnectionResourceUsage<
    out R : ConnectionResource<C>,
    out C : ResourceCapacity
>(
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

class TaskSchedulingConnectionResourceUsage<
    out R : ConnectionResource<C>,
    out C : ResourceCapacity
>(
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

data class ExecutionResourceTimeSlot<
    out R : ExecutionResource<C>,
    out C : ResourceCapacity
>(
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

abstract class AbstractExecutionResourceUsage<
    out R : ExecutionResource<C>,
    out C : ResourceCapacity
>(
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

class TaskSchedulingExecutionResourceUsage<
    out R : ExecutionResource<C>,
    out C : ResourceCapacity
>(
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

data class StorageResourceTimeSlot<
    out R : StorageResource<C>,
    out C : ResourceCapacity
>(
    private val timeWindow: TimeWindow,
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
        return task != null
                && (resource.costBy(task, TimeRange(task.time!!.start, timeWindow.end)) neq Flt64.zero
                    || resource.supplyBy(task, TimeRange(task.time!!.start, timeWindow.end)) neq Flt64.zero
                )
    }

    override fun toString() = "${resource}_${indexInRule}"
}

typealias StorageResourceUsage<R, C> = ResourceUsage<StorageResourceTimeSlot<R, C>, R, C>

abstract class AbstractStorageResourceUsage<
    out R : StorageResource<C>,
    out C : ResourceCapacity
>(
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
                    timeSlots.add(StorageResourceTimeSlot(timeWindow, resource, capacity, time, index))
                    beginTime += thisInterval
                    ++index
                }
            }
        }
        this.timeSlots = timeSlots
    }
}

class TaskSchedulingStorageResourceUsage<
    out R : StorageResource<C>,
    out C : ResourceCapacity
>(
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

class IterativeTaskSchedulingStorageResourceUsage<
    out R : StorageResource<C>,
    out C : ResourceCapacity
>(
    timeWindow: TimeWindow,
    resources: List<R>,
    interval: Duration = timeWindow.interval,
    override val name: String
) : AbstractStorageResourceUsage<R, C>(timeWindow, resources, interval) {
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override lateinit var quantity: LinearExpressionSymbols1

    override fun register(model: LinearMetaModel): Try {
        if (timeSlots.isNotEmpty()) {
            if (!::quantity.isInitialized) {
                quantity = flatMap(
                    "${name}_quantity",
                    timeSlots,
                    { s ->
                        val time = TimeRange(timeWindow.start, s.time.end)
                        val fixedSupply = s.resource.fixedSupplyIn(time)
                        val fixedCost = s.resource.fixedCostIn(time)
                        LinearPolynomial(s.resource.initialQuantity + fixedSupply - fixedCost)
                    },
                    { (_, s) -> "$s" }
                )
                for (slot in timeSlots) {
                    quantity[slot].range.set(
                        ValueRange(
                            slot.resourceCapacity.quantity.lowerBound.toFlt64() -
                                    (slot.resourceCapacity.lessQuantity ?: Flt64.zero),
                            slot.resourceCapacity.quantity.upperBound.toFlt64() +
                                    (slot.resourceCapacity.overQuantity ?: Flt64.zero)
                        )
                    )
                }
            }
        }

        return super.register(model)
    }

    suspend fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> addColumns(
        iteration: UInt64,
        tasks: List<T>,
        compilation: IterativeTaskCompilation<*, T, E, A>
    ): Try {
        assert(tasks.isNotEmpty())

        val xi = compilation.x[iteration.toInt()]

        coroutineScope {
            for (slot in timeSlots) {
                launch {
                    val thisTasks = tasks.mapNotNull {
                        val usedQuantity = slot.resource.usedQuantity(
                            it,
                            TimeRange(timeWindow.start, slot.time.end)
                        )
                        if (usedQuantity != Flt64.zero) {
                            Pair(it, usedQuantity)
                        } else {
                            null
                        }
                    }

                    if (thisTasks.isNotEmpty()) {
                        quantity[slot].flush()
                        for (task in thisTasks) {
                            quantity[slot].asMutable() += task.second * xi[task.first]
                        }
                    }
                }
            }
        }

        return ok
    }
}
