@file:OptIn(kotlin.time.ExperimentalTime::class)
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
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.max
import fuookami.ospf.kotlin.utils.min
import fuookami.ospf.kotlin.multiarray.Shape1
import kotlin.time.Duration
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel

/**
 * 连接资源 / Connection resource
 *
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param id 资源ID / Resource ID
 * @param name 资源名称 / Resource name
 * @param capacities 容量列表 / List of capacities
 * @param initialQuantity 初始数量 / Initial quantity
 */
abstract class ConnectionResource<C : AbstractResourceCapacity<V>, V>(
    override val id: String,
    override val name: String,
    override val capacities: List<C>,
    @Deprecated(
        message = "Use initialQuantity(unit) returning Quantity instead",
        replaceWith = ReplaceWith("initialQuantity(NoneUnit).value")
    )
    override val initialQuantity: V = resourceQuantityZero(capacities)
) : Resource<C, V>() where V : RealNumber<V>, V : NumberField<V> {
    abstract fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedBy(
        prevTask: T?,
        task: T?,
        time: TimeRange
    ): V

    @Deprecated(
        message = "Use usedQuantityQuantity returning Quantity instead",
        replaceWith = ReplaceWith("usedQuantityQuantity(bunch, time).value")
    )
    override fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantity(
        bunch: AbstractTaskBunch<T, E, A, V>,
        time: TimeRange
    ): V {
        var counter = initialQuantity().value.constants.zero
        for (i in bunch.tasks.indices) {
            counter += if (i == 0) {
                usedBy(
                    prevTask = bunch.lastTask,
                    task = bunch[i],
                    time = time
                )
            } else {
                usedBy(
                    prevTask = bunch[i - 1],
                    task = bunch[i],
                    time = time
                )
            }
        }
        return counter
    }
}

/**
 * 连接资源时间槽 / Connection resource time slot
 *
 * @param R 连接资源类型 / Connection resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param origin 原始时间槽 / Origin time slot
 * @param resource 资源 / Resource
 * @param resourceCapacity 资源容量 / Resource capacity
 * @param indexInRule 规则内索引 / Index in rule
 */
data class ConnectionResourceTimeSlot<
        R : ConnectionResource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        >(
    override val origin: TimeSlot,
    override val resource: R,
    override val resourceCapacity: C,
    override val indexInRule: UInt64,
) : ResourceTimeSlot<R, C, V>, AutoIndexed(ConnectionResourceTimeSlot::class)
        where V : RealNumber<V>, V : NumberField<V> {
    fun <E : Executor, A : AssignmentPolicy<E>> usedBy(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): V {
        return resource.usedBy(
            prevTask = prevTask,
            task = task,
            time = time
        )
    }

    override fun <E : Executor, A : AssignmentPolicy<E>> relationTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): V {
        return usedBy(prevTask, task)
    }

    override fun subOf(subTime: TimeRange): ConnectionResourceTimeSlot<R, C, V>? {
        return origin.subOf(subTime)?.let {
            ConnectionResourceTimeSlot(
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

/** 连接资源使用类型别名 / Connection resource usage typealias */
typealias ConnectionResourceUsage<R, C, V> = ResourceUsage<ConnectionResourceTimeSlot<R, C, V>, R, C, V>

/**
 * 抽象连接资源使用 / Abstract connection resource usage
 *
 * @param R 连接资源类型 / Connection resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param timeWindow 时间窗口 / Time window
 * @param resources 资源列表 / List of resources
 * @param times 时间槽列表 / List of time slots
 * @param interval 时间间隔 / Time interval
 */
abstract class AbstractConnectionResourceUsage<
        R : ConnectionResource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        >(
    protected val timeWindow: TimeWindow<V>,
    resources: List<R>,
    times: List<TimeSlot>,
    interval: Duration = timeWindow.interval
) : AbstractResourceUsage<ConnectionResourceTimeSlot<R, C, V>, R, C, V>()
        where V : RealNumber<V>, V : NumberField<V> {
    final override val timeSlots: List<ConnectionResourceTimeSlot<R, C, V>>

    init {
        AutoIndexed.flush<ConnectionResourceTimeSlot<R, C, V>>()

        val timeSlots = ArrayList<ConnectionResourceTimeSlot<R, C, V>>()
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
                                ConnectionResourceTimeSlot(
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
                            ConnectionResourceTimeSlot(
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
 * 任务调度连接资源使用 / Task scheduling connection resource usage
 *
 * @param R 连接资源类型 / Connection resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param timeWindow 时间窗口 / Time window
 * @param resources 资源列表 / List of resources
 * @param times 时间槽列表 / List of time slots
 * @param interval 时间间隔 / Time interval
 * @param name 名称 / Name
 * @param overEnabled 是否启用超量 / Whether over quantity is enabled
 * @param lessEnabled 是否启用不足 / Whether less quantity is enabled
 */
class TaskSchedulingConnectionResourceUsage<
        R : ConnectionResource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        > private constructor(
    timeWindow: TimeWindow<V>,
    resources: List<R>,
    times: List<TimeSlot>,
    interval: Duration = timeWindow.interval,
    override val name: String,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = false
) : AbstractConnectionResourceUsage<R, C, V>(
    timeWindow = timeWindow,
    resources = resources,
    times = times,
    interval = interval
) where V : RealNumber<V>, V : NumberField<V> {
    constructor(
        timeWindow: TimeWindow<V>,
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
        timeWindow: TimeWindow<V>,
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
            "TaskSchedulingConnectionResourceUsage.register 暂未实现，请使用 BunchSchedulingConnectionResourceUsage 或补充任务级连接资源建模。"
        )
    }
}

/**
 * 任务束调度连接资源使用 / Bunch scheduling connection resource usage
 *
 * @param R 连接资源类型 / Connection resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param timeWindow 时间窗口 / Time window
 * @param resources 资源列表 / List of resources
 * @param times 时间槽列表 / List of time slots
 * @param interval 时间间隔 / Time interval
 * @param name 名称 / Name
 */
class BunchSchedulingConnectionResourceUsage<
        R : ConnectionResource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        > private constructor(
    timeWindow: TimeWindow<V>,
    resources: List<R>,
    times: List<TimeSlot>,
    interval: Duration = timeWindow.interval,
    override val name: String
) : AbstractConnectionResourceUsage<R, C, V>(
    timeWindow = timeWindow,
    resources = resources,
    times = times,
    interval = interval
) where V : RealNumber<V>, V : NumberField<V> {
    constructor(
        timeWindow: TimeWindow<V>,
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
        timeWindow: TimeWindow<V>,
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
                        constant = slot.resource.solverInitialQuantity(),
                        name = "${name}_quantity_${slot}"
                    )
                }
                for (slot in timeSlots) {
                    quantity[slot].range.set(slot.resourceCapacity.solverValueRange())
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
            B : AbstractTaskBunch<T, E, A, V>,
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>
            > addColumns(
        iteration: UInt64,
        bunches: List<B>,
        compilation: BunchCompilation<B, V, T, E, A>
    ): Try {
        assert(bunches.isNotEmpty())

        val xi = compilation.x[iteration.toInt()]

        for (slot in timeSlots) {
            val thisBunches = bunches.filter { bunch ->
                bunch.connections.any { slot.relatedTo(it.first, it.second) }
            }

            if (thisBunches.isNotEmpty()) {
                quantity[slot].flush()
                for (bunch in thisBunches) {
                    quantity[slot].asMutable() += LinearMonomial(
                        slot.resource.usedQuantityQuantity(bunch, slot.time).value.solverResourceQuantity(),
                        xi[bunch]
                    )
                }
            }
        }

        return ok
    }
}
