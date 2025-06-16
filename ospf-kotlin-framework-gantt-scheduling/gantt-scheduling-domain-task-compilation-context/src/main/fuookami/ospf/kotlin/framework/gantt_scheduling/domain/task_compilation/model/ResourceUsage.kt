package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import kotlin.time.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
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
        return relationTo(prevTask, task) neq Flt64.zero
    }

    fun <E : Executor, A : AssignmentPolicy<E>> relationTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): Flt64 {
        return Flt64.zero
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
    val quantity: LinearIntermediateSymbols1
    val overQuantity: LinearIntermediateSymbols1
    val lessQuantity: LinearIntermediateSymbols1

    val overEnabled: Boolean
    val lessEnabled: Boolean

    fun register(model: MetaModel): Try
}

abstract class AbstractResourceUsage<
    out S : ResourceTimeSlot<R, C>,
    out R : Resource<C>,
    out C : ResourceCapacity
> : ResourceUsage<S, R, C> {
    override lateinit var overQuantity: LinearIntermediateSymbols1
    override lateinit var lessQuantity: LinearIntermediateSymbols1

    override fun register(model: MetaModel): Try {
        if (timeSlots.isNotEmpty()) {
            if (overEnabled) {
                if (!::overQuantity.isInitialized) {
                    overQuantity = LinearIntermediateSymbols1(
                        "${name}_over_quantity",
                        Shape1(timeSlots.size)
                    ) { i, _ ->
                        val slot = timeSlots[i]
                        if (slot.resourceCapacity.overEnabled) {
                            val slack = SlackFunction(
                                UContinuous,
                                x = LinearPolynomial(quantity[slot]),
                                threshold = LinearPolynomial(slot.resourceCapacity.quantity.upperBound.value.unwrap()),
                                constraint = false,
                                name = "${name}_over_quantity_$slot"
                            )
                            slot.resourceCapacity.overQuantity?.let {
                                slack.pos!!.range.leq(it)
                            }
                            slack
                        } else {
                            LinearExpressionSymbol(LinearPolynomial(), "${name}_over_quantity_$slot")
                        }
                    }
                }
                when (val result = model.add(overQuantity)) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            if (lessEnabled) {
                if (!::lessQuantity.isInitialized) {
                    lessQuantity = LinearIntermediateSymbols1(
                        "${name}_less_quantity",
                        Shape1(timeSlots.size)
                    ) { i, _ ->
                        val slot = timeSlots[i]
                        if (slot.resourceCapacity.lessEnabled) {
                            val slack = SlackFunction(
                                UContinuous,
                                x = LinearPolynomial(quantity[slot]),
                                threshold = LinearPolynomial(slot.resourceCapacity.quantity.lowerBound.value.unwrap()),
                                withPositive = false,
                                constraint = false,
                                name = "${name}_less_quantity_$slot"
                            )
                            slot.resourceCapacity.lessQuantity?.let {
                                slack.neg!!.range.leq(it)
                            }
                            slack
                        } else {
                            LinearExpressionSymbol(LinearPolynomial(), "${name}_less_quantity_$slot")
                        }
                    }
                }
                when (val result = model.add(lessQuantity)) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
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

    override fun <E : Executor, A : AssignmentPolicy<E>> relationTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): Flt64 {
        return usedBy(prevTask, task)
    }

    override fun toString() = "${resource}_${resourceCapacity}_${indexInRule}"
}

typealias ConnectionResourceUsage<R, C> = ResourceUsage<ConnectionResourceTimeSlot<R, C>, R, C>

abstract class AbstractConnectionResourceUsage<
    out R : ConnectionResource<C>,
    out C : ResourceCapacity
>(
    protected val timeWindow: TimeWindow,
    resources: List<R>,
    times: List<TimeRange>,
    interval: Duration = timeWindow.interval
) : AbstractResourceUsage<ConnectionResourceTimeSlot<R, C>, R, C>() {
    final override val timeSlots: List<ConnectionResourceTimeSlot<R, C>>

    init {
        AutoIndexed.flush<ConnectionResourceTimeSlot<R, C>>()

        val timeSlots = ArrayList<ConnectionResourceTimeSlot<R, C>>()
        for (resource in resources) {
            for (capacity in resource.capacities) {
                var index = UInt64.zero
                if (times.isNotEmpty()) {
                    val thisTimes = times.filter { it.withIntersection(capacity.time) }
                    for (time in thisTimes) {
                        val thisTime = TimeRange(
                            max(time.start, capacity.time.start),
                            min(time.end, capacity.time.end)
                        )
                        timeSlots.add(ConnectionResourceTimeSlot(resource, capacity, thisTime, index))
                        ++index
                    }
                } else {
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
        }
        this.timeSlots = timeSlots
    }
}

class TaskSchedulingConnectionResourceUsage<
    out R : ConnectionResource<C>,
    out C : ResourceCapacity
>private constructor(
    timeWindow: TimeWindow,
    resources: List<R>,
    times: List<TimeRange>,
    interval: Duration,
    override val name: String,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = false
) : AbstractConnectionResourceUsage<R, C>(timeWindow, resources, times, interval) {
    constructor(
        timeWindow: TimeWindow,
        resources: List<R>,
        times: List<TimeRange>,
        name: String,
        overEnabled: Boolean = false,
        lessEnabled: Boolean = false
    ) : this(
        timeWindow,
        resources,
        times,
        timeWindow.interval,
        name,
        overEnabled,
        lessEnabled
    )

    constructor(
        timeWindow: TimeWindow,
        resources: List<R>,
        interval: Duration = timeWindow.interval,
        name: String,
        overEnabled: Boolean = false,
        lessEnabled: Boolean = false
    ) : this(
        timeWindow,
        resources,
        emptyList(),
        interval,
        name,
        overEnabled,
        lessEnabled
    )

    override lateinit var quantity: LinearIntermediateSymbols1

    override fun register(model: MetaModel): Try {
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
    fun <E : Executor, A : AssignmentPolicy<E>> usedBy(
        task: AbstractTask<E, A>
    ): Flt64 {
        return resource.usedBy(task, time)
    }

    override fun <E : Executor, A : AssignmentPolicy<E>> relationTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): Flt64 {
        return if (task != null) {
            usedBy(task)
        } else {
            Flt64.zero
        }
    }

    override fun toString() = "${resource}_${resourceCapacity}_${indexInRule}"
}

typealias ExecutionResourceUsage<R, C> = ResourceUsage<ExecutionResourceTimeSlot<R, C>, R, C>

abstract class AbstractExecutionResourceUsage<
    out R : ExecutionResource<C>,
    out C : ResourceCapacity
>(
    protected val timeWindow: TimeWindow,
    resources: List<R>,
    times: List<TimeRange>,
    interval: Duration = timeWindow.interval
) : AbstractResourceUsage<ExecutionResourceTimeSlot<R, C>, R, C>() {
    final override val timeSlots: List<ExecutionResourceTimeSlot<R, C>>

    init {
        AutoIndexed.flush<ExecutionResourceTimeSlot<R, C>>()

        val timeSlots = ArrayList<ExecutionResourceTimeSlot<R, C>>()
        for (resource in resources) {
            for (capacity in resource.capacities) {
                var index = UInt64.zero
                if (times.isNotEmpty()) {
                    val thisTimes = times.filter { it.withIntersection(capacity.time) }
                    for (time in thisTimes) {
                        val thisTime = TimeRange(
                            max(time.start, capacity.time.start),
                            min(time.end, capacity.time.end)
                        )
                        timeSlots.add(ExecutionResourceTimeSlot(resource, capacity, thisTime, index))
                        ++index
                    }
                } else {
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
        }
        this.timeSlots = timeSlots
    }
}

class TaskSchedulingExecutionResourceUsage<
    out R : ExecutionResource<C>,
    out C : ResourceCapacity
>private constructor(
    timeWindow: TimeWindow,
    resources: List<R>,
    times: List<TimeRange>,
    interval: Duration = timeWindow.interval,
    override val name: String,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = false
) : AbstractExecutionResourceUsage<R, C>(timeWindow, resources, times, interval) {
    constructor(
        timeWindow: TimeWindow,
        resources: List<R>,
        times: List<TimeRange>,
        name: String,
        overEnabled: Boolean = false,
        lessEnabled: Boolean = false
    ) : this(
        timeWindow,
        resources,
        times,
        timeWindow.interval,
        name,
        overEnabled,
        lessEnabled
    )

    constructor(
        timeWindow: TimeWindow,
        resources: List<R>,
        interval: Duration = timeWindow.interval,
        name: String,
        overEnabled: Boolean = false,
        lessEnabled: Boolean = false
    ) : this(
        timeWindow,
        resources,
        emptyList(),
        interval,
        name,
        overEnabled,
        lessEnabled
    )

    override lateinit var quantity: LinearIntermediateSymbols1

    override fun register(model: MetaModel): Try {
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

    override fun <E : Executor, A : AssignmentPolicy<E>> relationTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): Flt64 {
        return if (task != null) {
            val timeRange = TimeRange(
                min(
                    task.time!!.start,
                    time.start
                ),
                min(
                    task.time!!.end,
                    time.end
                )
            )
            resource.supplyBy(task, timeRange) - resource.costBy(task, timeRange)
        } else {
            Flt64.zero
        }
    }

    override fun toString() = "${resource}_${resourceCapacity}_${indexInRule}"
}

typealias StorageResourceUsage<R, C> = ResourceUsage<StorageResourceTimeSlot<R, C>, R, C>

abstract class AbstractStorageResourceUsage<
    out E : Executor,
    out R : StorageResource<C>,
    out C : ResourceCapacity
>(
    protected val timeWindow: TimeWindow,
    protected val executors: List<E>,
    protected val resources: List<R>,
    times: List<TimeRange>,
    interval: Duration = timeWindow.interval
) : AbstractResourceUsage<StorageResourceTimeSlot<R, C>, R, C>() {
    abstract val executorSupply: LinearIntermediateSymbols3
    lateinit var supply: LinearIntermediateSymbols2
    abstract val cost: LinearIntermediateSymbols2

    override lateinit var quantity: LinearIntermediateSymbols1

    final override val timeSlots: List<StorageResourceTimeSlot<R, C>>

    init {
        AutoIndexed.flush<StorageResourceTimeSlot<R, C>>()

        val timeSlots = ArrayList<StorageResourceTimeSlot<R, C>>()
        for (resource in resources) {
            for (capacity in resource.capacities) {
                var index = UInt64.zero
                if (times.isNotEmpty()) {
                    val thisTimes = times.filter { it.withIntersection(capacity.time) }
                    for (time in thisTimes) {
                        val thisTime = TimeRange(
                            max(time.start, capacity.time.start),
                            min(time.end, capacity.time.end)
                        )
                        timeSlots.add(StorageResourceTimeSlot(timeWindow, resource, capacity, thisTime, index))
                        ++index
                    }
                } else {
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
        }
        this.timeSlots = timeSlots
    }

    override fun register(model: MetaModel): Try {
        if (!::supply.isInitialized) {
            supply = flatMap(
                "${name}_supply",
                resources,
                timeWindow.timeSlots,
                { resource, timeSlot ->
                    val time = TimeRange(timeWindow.start, timeSlot.end)
                    val fixedSupply = resource.fixedSupplyIn(time)
                    val r = resources.indexOf(resource)
                    val t = timeWindow.timeSlots.indexOfFirst { it.end == time.end }
                    LinearPolynomial(fixedSupply + sum(executorSupply[_a, r, t]))
                },
                { (_, r), (_, t) -> "${r}_${t}" }
            )
        }
        when (val result = model.add(supply)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (timeSlots.isNotEmpty()) {
            if (!::quantity.isInitialized) {
                quantity = flatMap(
                    "${name}_quantity",
                    timeSlots,
                    { s ->
                        val t = timeWindow.timeSlots.indexOfFirst { it.end == s.time.end }
                        val r = resources.indexOf(s.resource)
                        LinearPolynomial(s.resource.initialQuantity + supply[r, t] - cost[r, t])
                    },
                    { (_, s) -> "$s" }
                )
                for (slot in timeSlots) {
                    quantity[slot].range.set(
                        ValueRange(
                            slot.resourceCapacity.quantity.lowerBound.value.unwrap() - (slot.resourceCapacity.lessQuantity ?: Flt64.zero),
                            slot.resourceCapacity.quantity.upperBound.value.unwrap() + (slot.resourceCapacity.overQuantity ?: Flt64.zero)
                        ).value!!
                    )
                }
            }
            when (val result = model.add(quantity)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return super.register(model)
    }
}

class TaskSchedulingStorageResourceUsage<
    out E : Executor,
    out R : StorageResource<C>,
    out C : ResourceCapacity
>(
    timeWindow: TimeWindow,
    executors: List<E>,
    resources: List<R>,
    times: List<TimeRange>,
    interval: Duration = timeWindow.interval,
    override val name: String,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = false
) : AbstractStorageResourceUsage<E, R, C>(timeWindow, executors, resources, times, interval) {
    constructor(
        timeWindow: TimeWindow,
        executors: List<E>,
        resources: List<R>,
        times: List<TimeRange>,
        name: String,
        overEnabled: Boolean = false,
        lessEnabled: Boolean = false
    ) : this(
        timeWindow,
        executors,
        resources,
        times,
        timeWindow.interval,
        name,
        overEnabled,
        lessEnabled
    )

    constructor(
        timeWindow: TimeWindow,
        executors: List<E>,
        resources: List<R>,
        interval: Duration = timeWindow.interval,
        name: String,
        overEnabled: Boolean = false,
        lessEnabled: Boolean = false
    ) : this(
        timeWindow,
        executors,
        resources,
        emptyList(),
        interval,
        name,
        overEnabled,
        lessEnabled
    )

    override lateinit var executorSupply: LinearIntermediateSymbols3
    override lateinit var cost: LinearIntermediateSymbols2

    override fun register(model: MetaModel): Try {
        TODO("NOT IMPLEMENT YET")
    }
}

class IterativeTaskSchedulingStorageResourceUsage<
    out E : Executor,
    out R : StorageResource<C>,
    out C : ResourceCapacity
> private constructor(
    timeWindow: TimeWindow,
    executors: List<E>,
    resources: List<R>,
    times: List<TimeRange>,
    interval: Duration = timeWindow.interval,
    override val name: String
) : AbstractStorageResourceUsage<E, R, C>(timeWindow, executors, resources, times, interval) {
    constructor(
        timeWindow: TimeWindow,
        executors: List<E>,
        resources: List<R>,
        times: List<TimeRange>,
        name: String
    ) : this(
        timeWindow,
        executors,
        resources,
        times,
        timeWindow.interval,
        name
    )

    constructor(
        timeWindow: TimeWindow,
        executors: List<E>,
        resources: List<R>,
        interval: Duration = timeWindow.interval,
        name: String
    ) : this(
        timeWindow,
        executors,
        resources,
        emptyList(),
        interval,
        name
    )

    override lateinit var executorSupply: LinearExpressionSymbols3
    override lateinit var cost: LinearExpressionSymbols2

    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override fun register(model: MetaModel): Try {
        if (!::executorSupply.isInitialized) {
            executorSupply = flatMap(
                "${name}_executor_supply",
                executors,
                resources,
                timeWindow.timeSlots,
                { _, _, _ -> LinearPolynomial() },
                { (_, e), (_, r), (_, t) -> "${e}_${r}_${t}" }
            )
        }
        when (val result = model.add(executorSupply)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::cost.isInitialized) {
            cost = flatMap(
                "${name}_cost",
                resources,
                timeWindow.timeSlots,
                { r, t ->
                    val time = TimeRange(timeWindow.start, t.end)
                    val fixedCost = r.fixedCostIn(time)
                    LinearPolynomial(fixedCost)
                },
                { (_, r), (_, t) -> "${r}_${t}" }
            )
        }
        when (val result = model.add(cost)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
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
            for ((e, executor) in executors.withIndex()) {
                for ((r, resource) in resources.withIndex()) {
                    for ((t, timeSlot) in timeWindow.timeSlots.withIndex()) {
                        launch(Dispatchers.Default) {
                            val thisTasks = tasks.mapNotNull { task ->
                                if (task.executor != executor) {
                                    return@mapNotNull null
                                }
                                val supplyQuantity = resource.supplyBy(
                                    task,
                                    TimeRange(timeWindow.start, timeSlot.end)
                                )
                                if (supplyQuantity != Flt64.zero) {
                                    Pair(task, supplyQuantity)
                                } else {
                                    null
                                }
                            }

                            if (thisTasks.isNotEmpty()) {
                                executorSupply[e, r, t].flush()
                                for ((task, supplyQuantity) in thisTasks) {
                                    executorSupply[e, r, t].asMutable() += xi[task] * supplyQuantity
                                }
                            }
                        }
                    }
                }
            }
            for ((r, resource) in resources.withIndex()) {
                for ((t, timeSlot) in timeWindow.timeSlots.withIndex()) {
                    launch(Dispatchers.Default) {
                        val thisTasks = tasks.mapNotNull { task ->
                            val costQuantity = resource.costBy(
                                task,
                                TimeRange(timeWindow.start, timeSlot.end)
                            )
                            if (costQuantity != Flt64.zero) {
                                Pair(task, costQuantity)
                            } else {
                                null
                            }
                        }
                        if (thisTasks.isNotEmpty()) {
                            cost[r, t].flush()
                            for ((task, costQuantity) in thisTasks) {
                                cost[r, t].asMutable() += xi[task] * costQuantity
                            }
                        }
                    }
                }
            }
        }
        return ok
    }
}
