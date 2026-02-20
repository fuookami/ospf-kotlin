package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import kotlin.time.*
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
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*

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

data class ExecutionResourceTimeSlot<
    out R : ExecutionResource<C>,
    out C : AbstractResourceCapacity
>(
    override val origin: TimeSlot,
    override val resource: R,
    override val resourceCapacity: C,
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

    override fun subOf(subTime: TimeRange): ExecutionResourceTimeSlot<R, C>? {
        return origin.subOf(subTime)?.let {
            ExecutionResourceTimeSlot(
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

typealias ExecutionResourceUsage<R, C> = ResourceUsage<ExecutionResourceTimeSlot<R, C>, R, C>

abstract class AbstractExecutionResourceUsage<
    out R : ExecutionResource<C>,
    out C : AbstractResourceCapacity
>(
    protected val timeWindow: TimeWindow,
    resources: List<R>,
    times: List<TimeSlot>,
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
                    val thisTimes = times.filter { it.time.withIntersection(capacity.time) }
                    for (time in thisTimes) {
                        val thisTime = TimeRange(
                            max(time.start, capacity.time.start),
                            min(time.end, capacity.time.end)
                        )
                        time.subOf(thisTime)?.let {
                            timeSlots.add(
                                ExecutionResourceTimeSlot(
                                    origin = it,
                                    resource = resource,
                                    resourceCapacity = capacity,
                                    indexInRule = index
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
                            ExecutionResourceTimeSlot(
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
}

class TaskSchedulingExecutionResourceUsage<
    out R : ExecutionResource<C>,
    out C : AbstractResourceCapacity
> private constructor(
    timeWindow: TimeWindow,
    resources: List<R>,
    times: List<TimeSlot>,
    interval: Duration = timeWindow.interval,
    override val name: String,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = false
) : AbstractExecutionResourceUsage<R, C>(
    timeWindow = timeWindow,
    resources = resources,
    times = times,
    interval = interval
) {
    constructor(
        timeWindow: TimeWindow,
        resources: List<R>,
        times: List<TimeSlot>,
        name: String,
        overEnabled: Boolean = false,
        lessEnabled: Boolean = false
    ) : this(
        timeWindow = timeWindow,
        resources = resources,
        times = times,
        interval = timeWindow.interval,
        name = name,
        overEnabled = overEnabled,
        lessEnabled = lessEnabled
    )

    constructor(
        timeWindow: TimeWindow,
        resources: List<R>,
        interval: Duration = timeWindow.interval,
        name: String,
        overEnabled: Boolean = false,
        lessEnabled: Boolean = false
    ) : this(
        timeWindow = timeWindow,
        resources = resources,
        times = emptyList(),
        interval = interval,
        name = name,
        overEnabled = overEnabled,
        lessEnabled = lessEnabled
    )

    override lateinit var quantity: LinearIntermediateSymbols1

    override fun register(model: MetaModel): Try {
        TODO("NOT IMPLEMENT YET")
    }
}

class BunchSchedulingExecutionResourceUsage<
    out R : ExecutionResource<C>,
    out C : AbstractResourceCapacity
> private constructor(
    timeWindow: TimeWindow,
    resources: List<R>,
    times: List<TimeSlot>,
    interval: Duration = timeWindow.interval,
    override val name: String
) : AbstractExecutionResourceUsage<R, C>(
    timeWindow = timeWindow,
    resources = resources,
    times = times,
    interval = interval
) {
    constructor(
        timeWindow: TimeWindow,
        resources: List<R>,
        times: List<TimeSlot>,
        name: String
    ) : this(
        timeWindow = timeWindow,
        resources = resources,
        times = times,
        interval = timeWindow.interval,
        name = name
    )

    constructor(
        timeWindow: TimeWindow,
        resources: List<R>,
        interval: Duration = timeWindow.interval,
        name: String
    ) : this(
        timeWindow = timeWindow,
        resources = resources,
        times = emptyList(),
        interval = interval,
        name = name
    )

    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override lateinit var quantity: LinearExpressionSymbols1

    override fun register(model: MetaModel): Try {
        if (timeSlots.isNotEmpty()) {
            if (!::quantity.isInitialized) {
                quantity = LinearExpressionSymbols1(
                    name = "${name}_quantity",
                    shape = Shape1(timeSlots.size)
                ) { s, _ ->
                    val slot = timeSlots[s]
                    LinearExpressionSymbol(
                        polynomial = LinearPolynomial(slot.resource.initialQuantity),
                        name = "${name}_quantity_${slot}"
                    )
                }
                for (slot in timeSlots) {
                    quantity[slot].range.set(
                        ValueRange(
                            slot.resourceCapacity.quantity.lowerBound.value.unwrap() - (slot.resourceCapacity.lessQuantity
                                ?: Flt64.zero),
                            slot.resourceCapacity.quantity.upperBound.value.unwrap() + (slot.resourceCapacity.overQuantity
                                ?: Flt64.zero)
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

        return ok
    }

    fun <
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

        for (slot in timeSlots) {
            val thisBunches = bunches.filter { bunch ->
                bunch.tasks.any { slot.relatedTo(null, it) }
            }

            if (thisBunches.isNotEmpty()) {
                quantity[slot].flush()
                for (bunch in thisBunches) {
                    quantity[slot].asMutable() += slot.resource.usedQuantity(
                        bunch,
                        slot.time
                    ) * xi[bunch]
                }
            }
        }

        return ok
    }
}
