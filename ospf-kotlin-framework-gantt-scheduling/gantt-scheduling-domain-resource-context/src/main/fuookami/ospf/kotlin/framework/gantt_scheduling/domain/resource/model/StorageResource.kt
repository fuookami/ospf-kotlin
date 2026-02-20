package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import kotlin.time.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*

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

data class StorageResourceTimeSlot<
    out R : StorageResource<C>,
    out C : AbstractResourceCapacity
>(
    private val timeWindow: TimeWindow,
    override val origin: TimeSlot,
    override val resource: R,
    override val resourceCapacity: C,
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
            supplyBy(task) - costBy(task)
        } else {
            Flt64.zero
        }
    }

    override fun subOf(subTime: TimeRange): TimeSlot? {
        return origin.subOf(subTime)?.let {
            StorageResourceTimeSlot(
                timeWindow = timeWindow,
                origin = it,
                resource = resource,
                resourceCapacity = resourceCapacity,
                indexInRule = indexInRule
            )
        }
    }

    override fun toString(): String {
        return "${resource}_${resourceCapacity}_${indexInRule}"
    }
}

typealias StorageResourceUsage<R, C> = ResourceUsage<StorageResourceTimeSlot<R, C>, R, C>

abstract class AbstractStorageResourceUsage<
    out E : Executor,
    out R : StorageResource<C>,
    out C : AbstractResourceCapacity
>(
    protected val timeWindow: TimeWindow,
    protected val executors: List<E>,
    protected val resources: List<R>,
    times: List<TimeSlot>,
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
                    val thisTimes = times.filter { it.time.withIntersection(capacity.time) }
                    for (time in thisTimes) {
                        val thisTime = TimeRange(
                            max(time.start, capacity.time.start),
                            min(time.end, capacity.time.end)
                        )
                        time.subOf(thisTime)?.let {
                            timeSlots.add(
                                StorageResourceTimeSlot(
                                    timeWindow = timeWindow,
                                    origin = it,
                                    resource = resource,
                                    resourceCapacity = capacity,
                                    indexInRule = index,
                                )
                            )
                            ++index
                        }
                    }
                } else {
                    var beginTime = maxOf(capacity.time.start, timeWindow.window.start)
                    val endTime = minOf(capacity.time.end, timeWindow.window.end)
                    while (beginTime < endTime) {
                        val thisInterval = minOf(endTime - beginTime, capacity.interval, interval)
                        val time = TimeRange(beginTime, beginTime + thisInterval)
                        timeSlots.add(
                            StorageResourceTimeSlot(
                                timeWindow = timeWindow,
                                origin = time,
                                resource = resource,
                                resourceCapacity = capacity,
                                indexInRule = index
                            )
                        )
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
            supply = LinearIntermediateSymbols2(
                name = "${name}_supply",
                shape = Shape2(resources.size, timeWindow.timeSlots.size)
            ) { _, (r, s) ->
                val resource = resources[r]
                val slot = timeWindow.timeSlots[s]
                val time = TimeRange(timeWindow.start, slot.end)
                val fixedSupply = resource.fixedSupplyIn(time)
                val r = resources.indexOf(resource)
                val t = timeWindow.timeSlots.indexOfFirst { it.end == time.end }
                LinearExpressionSymbol(
                    polynomial = fixedSupply + sum(executorSupply[_a, r, t]),
                    name = "${name}_supply_${resource}_${slot}"
                )
            }
        }
        when (val result = model.add(supply)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (timeSlots.isNotEmpty()) {
            if (!::quantity.isInitialized) {
                quantity = LinearIntermediateSymbols1(
                    name = "${name}_quantity",
                    shape = Shape1(timeSlots.size)
                ) { s, _ ->
                    val slot = timeSlots[s]
                    val t = timeWindow.timeSlots.indexOfFirst { it.end == slot.time.end }
                    val r = resources.indexOf(slot.resource)
                    LinearExpressionSymbol(
                        polynomial = slot.resource.initialQuantity + supply[r, t] - cost[r, t],
                        name = "${name}_quantity_${slot}"
                    )
                }
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
    out C : AbstractResourceCapacity
>(
    timeWindow: TimeWindow,
    executors: List<E>,
    resources: List<R>,
    times: List<TimeSlot>,
    interval: Duration = timeWindow.interval,
    override val name: String,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = false
) : AbstractStorageResourceUsage<E, R, C>(
    timeWindow = timeWindow,
    executors = executors,
    resources = resources,
    times = times,
    interval = interval
) {
    constructor(
        timeWindow: TimeWindow,
        executors: List<E>,
        resources: List<R>,
        times: List<TimeRange>,
        name: String,
        overEnabled: Boolean = false,
        lessEnabled: Boolean = false
    ) : this(
        timeWindow = timeWindow,
        executors = executors,
        resources = resources,
        times = times,
        interval = timeWindow.interval,
        name = name,
        overEnabled = overEnabled,
        lessEnabled = lessEnabled
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
        timeWindow = timeWindow,
        executors = executors,
        resources = resources,
        times = emptyList(),
        interval = interval,
        name = name,
        overEnabled = overEnabled,
        lessEnabled = lessEnabled
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
    out C : AbstractResourceCapacity
> private constructor(
    timeWindow: TimeWindow,
    executors: List<E>,
    resources: List<R>,
    times: List<TimeSlot>,
    interval: Duration = timeWindow.interval,
    override val name: String
) : AbstractStorageResourceUsage<E, R, C>(
    timeWindow = timeWindow,
    executors = executors,
    resources = resources,
    times = times,
    interval = interval
) {
    constructor(
        timeWindow: TimeWindow,
        executors: List<E>,
        resources: List<R>,
        times: List<TimeRange>,
        name: String
    ) : this(
        timeWindow = timeWindow,
        executors = executors,
        resources = resources,
        times = times,
        interval = timeWindow.interval,
        name = name
    )

    constructor(
        timeWindow: TimeWindow,
        executors: List<E>,
        resources: List<R>,
        interval: Duration = timeWindow.interval,
        name: String
    ) : this(
        timeWindow = timeWindow,
        executors = executors,
        resources = resources,
        times = emptyList(),
        interval = interval,
        name = name
    )

    override lateinit var executorSupply: LinearExpressionSymbols3
    override lateinit var cost: LinearExpressionSymbols2

    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override fun register(model: MetaModel): Try {
        if (!::executorSupply.isInitialized) {
            executorSupply = LinearExpressionSymbols3(
                name = "${name}_executor_supply",
                shape = Shape3(executors.size, resources.size, timeWindow.timeSlots.size)
            ) { _, (e, r, s) ->
                val executor = executors[e]
                val resource = resources[r]
                val timeSlot = timeWindow.timeSlots[s]
                LinearExpressionSymbol(
                    name = "${name}_executor_supply_${executor}_${resource}_${timeSlot}"
                )
            }
        }
        when (val result = model.add(executorSupply)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::cost.isInitialized) {
            cost = LinearExpressionSymbols2(
                name = "${name}_cost",
                shape = Shape2(resources.size, timeWindow.timeSlots.size)
            ) { _, (r, t) ->
                val resource = resources[r]
                val slot = timeWindow.timeSlots[t]
                val time = TimeRange(timeWindow.start, slot.end)
                val fixedCost = resource.fixedCostIn(time)
                LinearExpressionSymbol(
                    constant = fixedCost,
                    name = "${name}_cost_${resource}_${slot}"
                )
            }
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

class BunchSchedulingStorageResourceUsage<
    out E : Executor,
    out R : StorageResource<C>,
    out C : AbstractResourceCapacity
>(
    timeWindow: TimeWindow,
    executors: List<E>,
    resources: List<R>,
    times: List<TimeSlot>,
    interval: Duration = timeWindow.interval,
    override val name: String
) : AbstractStorageResourceUsage<E, R, C>(
    timeWindow = timeWindow,
    executors = executors,
    resources = resources,
    times = times,
    interval = interval
) {
    constructor(
        timeWindow: TimeWindow,
        executors: List<E>,
        resources: List<R>,
        times: List<TimeSlot>,
        name: String
    ) : this(
        timeWindow = timeWindow,
        executors = executors,
        resources = resources,
        times = times,
        interval = timeWindow.interval,
        name = name
    )

    constructor(
        timeWindow: TimeWindow,
        executors: List<E>,
        resources: List<R>,
        interval: Duration = timeWindow.interval,
        name: String
    ) : this(
        timeWindow = timeWindow,
        executors = executors,
        resources = resources,
        times = emptyList(),
        interval = interval,
        name = name
    )

    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override lateinit var executorSupply: LinearExpressionSymbols3
    override lateinit var cost: LinearExpressionSymbols2

    override fun register(model: MetaModel): Try {
        if (!::executorSupply.isInitialized) {
            executorSupply = LinearExpressionSymbols3(
                name = "${name}_executor_supply",
                shape = Shape3(executors.size, resources.size, timeWindow.timeSlots.size)
            ) { _, (e, r, s) ->
                val executor = executors[e]
                val resource = resources[r]
                val slot = timeWindow.timeSlots[s]
                LinearExpressionSymbol(
                    name = "${name}_executor_supply_${executor}_${resource}_${slot}"
                )
            }
        }
        when (val result = model.add(executorSupply)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::cost.isInitialized) {
            cost = LinearExpressionSymbols2(
                name = "${name}_cost",
                shape = Shape2(resources.size, timeWindow.timeSlots.size)
            ) { _, (r, t) ->
                val resource = resources[r]
                val slot = timeWindow.timeSlots[t]
                val time = TimeRange(timeWindow.start, slot.end)
                val fixedCost = resource.fixedCostIn(time)
                LinearExpressionSymbol(
                    constant = fixedCost,
                    name = "${name}_cost_${resource}_${slot}"
                )
            }
        }
        when (val result = model.add(cost)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return super.register(model)
    }

    suspend fun <
        B : AbstractTaskBunch<T, E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
    > addColumns(
        iteration: UInt64,
        bunches: List<B>,
        compilation: BunchCompilation<B, T, E, A>
    ): Try {
        assert(bunches.isNotEmpty())

        val xi = compilation.x[iteration.toInt()]

        coroutineScope {
            for ((e, executor) in executors.withIndex()) {
                for ((r, resource) in resources.withIndex()) {
                    for ((t, timeSlot) in timeWindow.timeSlots.withIndex()) {
                        launch(Dispatchers.Default) {
                            val thisBunches = bunches.mapNotNull { bunch ->
                                if (bunch.executor != executor) {
                                    return@mapNotNull null
                                }
                                val supplyQuantity = resource.supplyBy(
                                    bunch,
                                    TimeRange(timeWindow.start, timeSlot.end)
                                )
                                if (supplyQuantity != Flt64.zero) {
                                    Pair(bunch, supplyQuantity)
                                } else {
                                    null
                                }
                            }

                            if (thisBunches.isNotEmpty()) {
                                executorSupply[e, r, t].flush()
                                for ((bunch, supplyQuantity) in thisBunches) {
                                    executorSupply[e, r, t].asMutable() += xi[bunch] * supplyQuantity
                                }
                            }
                        }
                    }
                }
            }
            for ((r, resource) in resources.withIndex()) {
                for ((t, timeSlot) in timeWindow.timeSlots.withIndex()) {
                    launch(Dispatchers.Default) {
                        val thisBunches = bunches.mapNotNull { bunch ->
                            val costQuantity = resource.costBy(
                                bunch,
                                TimeRange(timeWindow.start, timeSlot.end)
                            )
                            if (costQuantity != Flt64.zero) {
                                Pair(bunch, costQuantity)
                            } else {
                                null
                            }
                        }

                        if (thisBunches.isNotEmpty()) {
                            cost[r, t].flush()
                            for ((bunch, costQuantity) in thisBunches) {
                                cost[r, t].asMutable() += xi[bunch] * costQuantity
                            }
                        }
                    }
                }
            }
        }

        return ok
    }
}
