/** 执行资源建模：任务级资源消耗与时间槽 / Execution resource modeling: task-level resource consumption and time slots */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.max
import fuookami.ospf.kotlin.utils.min
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*

/**
 * 执行资源 / Execution resource
 *
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param id 资源ID / Resource ID
 * @param name 资源名称 / Resource name
 * @param capacities 容量列表 / List of capacities
 * @param initialQuantityValue 初始数量裸值 / Initial quantity raw value
 */
abstract class ExecutionResource<C : AbstractResourceCapacity<V>, V>(
    override val id: String,
    override val name: String,
    override val capacities: List<C>,
    override val initialQuantityValue: V = resourceQuantityZero(capacities)
        ?: throw IllegalArgumentException("resource capacities must contain at least one finite quantity bound.")
) : Resource<C, V>() where V : RealNumber<V>, V : NumberField<V> {
    /**
     * 计算任务在指定时间范围内的资源消耗量 / Calculate resource consumption of a task in the given time range
     *
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param task 任务 / Task
     * @param time 时间范围 / Time range
     * @return 资源消耗量裸值 / Resource consumption raw value
     */
    abstract fun <E : Executor, A : AssignmentPolicy<E>> usedBy(
        task: AbstractTask<E, A>,
        time: TimeRange
    ): V

    override fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantityQuantity(
        bunch: AbstractTaskBunch<T, E, A, V>,
        time: TimeRange,
        unit: PhysicalUnit
    ): ResourceQuantity<V> {
        var counter = initialQuantityValue.constants.zero
        for (task in bunch.tasks) {
            counter += usedBy(task, time)
        }
        return Quantity(counter, unit)
    }
}

/**
 * 执行资源时间槽 / Execution resource time slot
 *
 * @param R 执行资源类型 / Execution resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param origin 原始时间槽 / Origin time slot
 * @param resource 资源 / Resource
 * @param resourceCapacity 资源容量 / Resource capacity
 * @param indexInRule 规则内索引 / Index in rule
 */
data class ExecutionResourceTimeSlot<
        R : ExecutionResource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        >(
    override val origin: TimeSlot,
    override val resource: R,
    override val resourceCapacity: C,
    override val indexInRule: UInt64,
) : ResourceTimeSlot<R, C, V>, AutoIndexed(ExecutionResourceTimeSlot::class)
        where V : RealNumber<V>, V : NumberField<V> {
    /**
     * 计算任务在此时槽的资源消耗量 / Calculate resource consumption of a task at this time slot
     *
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param task 任务 / Task
     * @return 资源消耗量裸值 / Resource consumption raw value
     */
    fun <E : Executor, A : AssignmentPolicy<E>> usedBy(task: AbstractTask<E, A>): V {
        return resource.usedBy(task, time)
    }

    override fun <E : Executor, A : AssignmentPolicy<E>> relationTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): V {
        return if (task != null) {
            usedBy(task)
        } else {
            resource.initialQuantityValue.constants.zero
        }
    }

    override fun subOf(subTime: TimeRange): ExecutionResourceTimeSlot<R, C, V>? {
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
typealias ExecutionResourceUsage<R, C, V> = ResourceUsage<ExecutionResourceTimeSlot<R, C, V>, R, C, V>

/**
 * 抽象执行资源使用 / Abstract execution resource usage
 *
 * @param R 执行资源类型 / Execution resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param timeWindow 时间窗口 / Time window
 * @param resources 资源列表 / List of resources
 * @param times 时间槽列表 / List of time slots
 * @param interval 时间间隔 / Time interval
 */
abstract class AbstractExecutionResourceUsage<
        R : ExecutionResource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        >(
    protected val timeWindow: TimeWindow<V>,
    resources: List<R>,
    times: List<TimeSlot>,
    interval: Duration = timeWindow.interval
) : AbstractResourceUsage<ExecutionResourceTimeSlot<R, C, V>, R, C, V>()
        where V : RealNumber<V>, V : NumberField<V> {
    final override val timeSlots: List<ExecutionResourceTimeSlot<R, C, V>>

    init {
        AutoIndexed.flush<ExecutionResourceTimeSlot<R, C, V>>()

        val timeSlots = ArrayList<ExecutionResourceTimeSlot<R, C, V>>()
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
 * @param V 值类型 / Value type
 * @param timeWindow 时间窗口 / Time window
 * @param resources 资源列表 / List of resources
 * @param times 时间槽列表 / List of time slots
 * @param interval 时间间隔 / Time interval
 * @param name 名称 / Name
 * @param overEnabled 是否启用超量 / Whether over quantity is enabled
 * @param lessEnabled 是否启用不足 / Whether less quantity is enabled
 */
class TaskSchedulingExecutionResourceUsage<
        R : ExecutionResource<C, V>,
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
) : AbstractExecutionResourceUsage<R, C, V>(
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
        return Failed(
            ErrorCode.Other,
            "TaskSchedulingExecutionResourceUsage.register 暂未实现，请使用 BunchSchedulingExecutionResourceUsage 或补充任务级执行资源建模。"
        )
    }
}

/**
 * 任务束调度执行资源使用 / Bunch scheduling execution resource usage
 *
 * @param R 执行资源类型 / Execution resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param timeWindow 时间窗口 / Time window
 * @param resources 资源列表 / List of resources
 * @param times 时间槽列表 / List of time slots
 * @param interval 时间间隔 / Time interval
 * @param name 名称 / Name
 */
class BunchSchedulingExecutionResourceUsage<
        R : ExecutionResource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        > private constructor(
    timeWindow: TimeWindow<V>,
    resources: List<R>,
    times: List<TimeSlot>,
    interval: Duration = timeWindow.interval,
    override val name: String
) : AbstractExecutionResourceUsage<R, C, V>(
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

    /**
     * 添加列贡献 / Add column contribution
     *
     * 用于列生成场景，在每次迭代中添加新列的资源使用量贡献
     * Used for column generation, adds resource usage contribution from new columns in each iteration
     *
     * @param B 任务束类型 / Task bunch type
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param iteration 当前迭代 / Current iteration
     * @param bunches 任务束列表 / List of task bunches
     * @param compilation 编译对象 / Compilation object
     * @return 成功与否 / Success or failure
     */
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
                bunch.tasks.any { slot.relatedTo(null, it) }
            }

            if (thisBunches.isNotEmpty()) {
                quantity[slot].flush()
                for (bunch in thisBunches) {
                    quantity[slot].asMutable() += LinearMonomial(
                        slot.resource.usedQuantityQuantity(bunch, slot.time).value.toSolverValue(),
                        xi[bunch]
                    )
                }
            }
        }

        return ok
    }
}
