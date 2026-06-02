@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 执行资源模型 / Execution resource model */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbols1
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.BunchCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTaskBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.concept.AutoIndexed
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.utils.max
import fuookami.ospf.kotlin.utils.min
import fuookami.ospf.kotlin.multiarray.Shape1
import kotlin.time.Duration
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel

/**
 * 执行资源 / Execution resource
 *
 * @param C 资源容量类型 / Resource capacity type
 * @param id 资源ID / Resource ID
 * @param name 资源名称 / Resource name
 * @param capacities 容量列表 / List of capacities
 * @param initialQuantity 初始数量 / Initial quantity
 */
abstract class ExecutionResource<out C : AbstractResourceCapacity>(
    override val id: String,
    override val name: String,
    override val capacities: List<C>,
    override val initialQuantity: Flt64 = Flt64.zero
) : Resource<C>() {
    abstract fun <E : Executor, A : AssignmentPolicy<E>> usedBy(
        task: AbstractTask<E, A>,
        time: TimeRange
    ): Flt64

    override fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantity(
        bunch: AbstractTaskBunch<T, E, A, Flt64>,
        time: TimeRange
    ): Flt64 {
        var counter = Flt64.zero
        for (task in bunch.tasks) {
            counter += usedBy(task, time)
        }
        return counter
    }
}

/**
 * 执行资源时间槽 / Execution resource time slot
 *
 * @param R 执行资源类型 / Execution resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param origin 原始时间槽 / Origin time slot
 * @param resource 资源 / Resource
 * @param resourceCapacity 资源容量 / Resource capacity
 * @param indexInRule 规则内索引 / Index in rule
 */
data class ExecutionResourceTimeSlot<
        out R : ExecutionResource<C>,
        out C : AbstractResourceCapacity
        >(
    override val origin: TimeSlot,
    override val resource: R,
    override val resourceCapacity: C,
    override val indexInRule: UInt64,
) : ResourceTimeSlot<R, C>, AutoIndexed(ExecutionResourceTimeSlot::class) {
    fun <E : Executor, A : AssignmentPolicy<E>> usedBy(task: AbstractTask<E, A>): Flt64 {
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

/** 执行资源使用类型别名 / Execution resource usage typealias */
typealias ExecutionResourceUsage<R, C> = ResourceUsage<ExecutionResourceTimeSlot<R, C>, R, C>

/**
 * 抽象执行资源使用 / Abstract execution resource usage
 *
 * @param R 执行资源类型 / Execution resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param timeWindow 时间窗口 / Time window
 * @param resources 资源列表 / List of resources
 * @param times 时间槽列表 / List of time slots
 * @param interval 时间间隔 / Time interval
 */
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

/**
 * 任务调度执行资源使用 / Task scheduling execution resource usage
 *
 * @param R 执行资源类型 / Execution resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param timeWindow 时间窗口 / Time window
 * @param resources 资源列表 / List of resources
 * @param times 时间槽列表 / List of time slots
 * @param interval 时间间隔 / Time interval
 * @param name 名称 / Name
 * @param overEnabled 是否启用超量 / Whether over quantity is enabled
 * @param lessEnabled 是否启用不足 / Whether less quantity is enabled
 */
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

    override lateinit var quantity: LinearIntermediateSymbols1<Flt64>

    override fun register(model: MetaModel<Flt64>): Try {
        throw UnsupportedOperationException(
            "TaskSchedulingExecutionResourceUsage.register 暂未实现，请使用 BunchSchedulingExecutionResourceUsage 或补充任务级执行资源建模。"
        )
    }
}

/**
 * 任务束调度执行资源使用 / Bunch scheduling execution resource usage
 *
 * @param R 执行资源类型 / Execution resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param timeWindow 时间窗口 / Time window
 * @param resources 资源列表 / List of resources
 * @param times 时间槽列表 / List of time slots
 * @param interval 时间间隔 / Time interval
 * @param name 名称 / Name
 */
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

    override lateinit var quantity: LinearExpressionSymbols1<Flt64>

    override fun register(model: MetaModel<Flt64>): Try {
        if (timeSlots.isNotEmpty()) {
            if (!::quantity.isInitialized) {
                quantity = LinearExpressionSymbols1<Flt64>(
                    name = "${name}_quantity",
                    shape = Shape1(timeSlots.size)
                ) { s, _ ->
                    val slot = timeSlots[s]
                    LinearExpressionSymbol(
                        constant = slot.resource.initialQuantity,
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

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }

    fun <
            B : AbstractTaskBunch<T, E, A, Flt64>,
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
                    quantity[slot].asMutable() += LinearMonomial(
                        slot.resource.usedQuantity(bunch, slot.time),
                        xi[bunch]
                    )
                }
            }
        }

        return ok
    }
}
