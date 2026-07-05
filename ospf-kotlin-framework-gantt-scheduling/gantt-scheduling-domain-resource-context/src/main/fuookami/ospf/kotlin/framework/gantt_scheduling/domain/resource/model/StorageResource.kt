/** 存储资源建模：库存资源供给与消耗 / Storage resource modeling: storage resource supply and consumption */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import kotlin.time.Duration
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.max
import fuookami.ospf.kotlin.utils.min

/**
 * 存储资源 / Storage resource
 *
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param id 资源ID / Resource ID
 * @param name 资源名称 / Resource name
 * @param capacities 容量列表 / List of capacities
 * @param initialQuantityValue 初始数量裸值 / Initial quantity raw value
 */
abstract class StorageResource<C : AbstractResourceCapacity<V>, V>(
    override val id: ResourceId,
    override val name: String,
    override val capacities: List<C>,
    override val initialQuantityValue: V
) : Resource<C, V>() where V : RealNumber<V>, V : NumberField<V> {
    /**
     * 计算指定时长内的固定成本 / Calculate fixed cost in the given duration
     *
     * @param time 时长 / Duration
     * @return 固定成本裸值 / Fixed cost raw value
     */
    open fun fixedCostIn(time: Duration): V {
        return initialQuantityValue.constants.zero
    }

    /**
     * 计算指定时间范围内的固定成本 / Calculate fixed cost in the given time range
     *
     * @param time 时间范围 / Time range
     * @return 固定成本裸值 / Fixed cost raw value
     */
    open fun fixedCostIn(time: TimeRange): V {
        return fixedCostIn(time.duration)
    }

    /**
     * 计算任务在指定时长内的资源消耗量 / Calculate resource consumption of a task in the given duration
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param task 任务 / Task
     * @param time 时长 / Duration
     * @return 资源消耗量裸值 / Resource consumption raw value
     */
    abstract fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> costBy(
        task: T,
        time: Duration
    ): V

    /**
     * 计算任务在指定时间范围内的资源消耗量 / Calculate resource consumption of a task in the given time range
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param task 任务 / Task
     * @param time 时间范围 / Time range
     * @return 资源消耗量裸值 / Resource consumption raw value
     */
    open fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> costBy(
        task: T,
        time: TimeRange
    ): V {
        val intersectionTime = task.time?.intersectionWith(time) ?: time
        return this.costBy(task, intersectionTime.duration)
    }

    /**
     * 计算指定时长内的固定供给量 / Calculate fixed supply in the given duration
     *
     * @param time 时长 / Duration
     * @return 固定供给量裸值 / Fixed supply raw value
     */
    open fun fixedSupplyIn(time: Duration): V {
        return initialQuantityValue.constants.zero
    }

    /**
     * 计算指定时间范围内的固定供给量 / Calculate fixed supply in the given time range
     *
     * @param time 时间范围 / Time range
     * @return 固定供给量裸值 / Fixed supply raw value
     */
    open fun fixedSupplyIn(time: TimeRange): V {
        return fixedSupplyIn(time.duration)
    }

    /**
     * 计算任务在指定时长内的资源供给量 / Calculate resource supply of a task in the given duration
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param task 任务 / Task
     * @param time 时长 / Duration
     * @return 资源供给量裸值 / Resource supply raw value
     */
    abstract fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> supplyBy(
        task: T,
        time: Duration
    ): V

    /**
     * 计算任务在指定时间范围内的资源供给量 / Calculate resource supply of a task in the given time range
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param task 任务 / Task
     * @param time 时间范围 / Time range
     * @return 资源供给量裸值 / Resource supply raw value
     */
    open fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> supplyBy(
        task: T,
        time: TimeRange
    ): V {
        val intersectionTime = task.time?.intersectionWith(time) ?: time
        return this.supplyBy(task, intersectionTime.duration)
    }

    /**
     * 计算任务在指定时间范围内的净使用量（供给-消耗）/ Calculate net usage of a task in the given time range (supply - cost)
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param task 任务 / Task
     * @param time 时间范围 / Time range
     * @return 净使用量裸值 / Net usage raw value
     */
    fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantity(
        task: T,
        time: TimeRange
    ): V {
        return supplyBy(task, time) - costBy(task, time)
    }

    /**
     * 计算任务束在指定时间范围内的资源消耗量 / Calculate resource consumption of a task bunch in the given time range
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param bunch 任务束 / Task bunch
     * @param time 时间范围 / Time range
     * @return 资源消耗量裸值 / Resource consumption raw value
     */
    open fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> costBy(
        bunch: AbstractTaskBunch<T, E, A, V>,
        time: TimeRange
    ): V {
        var sum = initialQuantityValue.constants.zero
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

    /**
     * 计算任务束在指定时间范围内的资源供给量 / Calculate resource supply of a task bunch in the given time range
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param bunch 任务束 / Task bunch
     * @param time 时间范围 / Time range
     * @return 资源供给量裸值 / Resource supply raw value
     */
    open fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> supplyBy(
        bunch: AbstractTaskBunch<T, E, A, V>,
        time: TimeRange
    ): V {
        var sum = initialQuantityValue.constants.zero
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

    override fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantityQuantity(
        bunch: AbstractTaskBunch<T, E, A, V>,
        time: TimeRange,
        unit: PhysicalUnit
    ): ResourceQuantity<V> {
        return Quantity(supplyBy(bunch, time) - costBy(bunch, time), unit)
    }
}

/**
 * 存储资源时间槽 / Storage resource time slot
 *
 * @param R 存储资源类型 / Storage resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param timeWindow 时间窗口 / Time window
 * @param origin 原始时间槽 / Origin time slot
 * @param resource 资源 / Resource
 * @param resourceCapacity 资源容量 / Resource capacity
 * @param indexInRule 规则内索引 / Index in rule
 */
data class StorageResourceTimeSlot<
        R : StorageResource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        >(
    private val timeWindow: TimeWindow<V>,
    override val origin: TimeSlot,
    override val resource: R,
    override val resourceCapacity: C,
    override val indexInRule: UInt64,
) : ResourceTimeSlot<R, C, V>, AutoIndexed(StorageResourceTimeSlot::class)
        where V : RealNumber<V>, V : NumberField<V> {
    /**
     * 计算任务在此时槽的资源消耗量 / Calculate resource consumption of a task at this time slot
     *
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param task 任务 / Task
     * @return 资源消耗量裸值 / Resource consumption raw value
     */
    fun <E : Executor, A : AssignmentPolicy<E>> costBy(task: AbstractTask<E, A>): V {
        return resource.costBy(task, time)
    }

    /**
     * 计算任务在此时槽的资源供给量 / Calculate resource supply of a task at this time slot
     *
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param task 任务 / Task
     * @return 资源供给量裸值 / Resource supply raw value
     */
    fun <E : Executor, A : AssignmentPolicy<E>> supplyBy(task: AbstractTask<E, A>): V {
        return resource.supplyBy(task, time)
    }

    override fun <E : Executor, A : AssignmentPolicy<E>> relationTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): V {
        return if (task != null) {
            supplyBy(task) - costBy(task)
        } else {
            resource.initialQuantityValue.constants.zero
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

/** 存储资源使用类型别名 / Storage resource usage typealias */
typealias StorageResourceUsage<R, C, V> = ResourceUsage<StorageResourceTimeSlot<R, C, V>, R, C, V>

/**
 * 抽象存储资源使用 / Abstract storage resource usage
 *
 * @param E 执行器类型 / Executor type
 * @param R 存储资源类型 / Storage resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param timeWindow 时间窗口 / Time window
 * @param executors 执行器列表 / List of executors
 * @param resources 资源列表 / List of resources
 * @param times 时间槽列表 / List of time slots
 * @param interval 时间间隔 / Time interval
 * @property executorSupply 执行器供给变量 / Executor supply variables
 * @property supply 供给变量 / Supply variables
 * @property cost 消耗变量 / Cost variables
 * @property quantity 数量变量 / Quantity variables
 */
abstract class AbstractStorageResourceUsage<
        E : Executor,
        R : StorageResource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        >(
    protected val timeWindow: TimeWindow<V>,
    protected val executors: List<E>,
    protected val resources: List<R>,
    times: List<TimeSlot>,
    interval: Duration = timeWindow.interval
) : AbstractResourceUsage<StorageResourceTimeSlot<R, C, V>, R, C, V>()
        where V : RealNumber<V>, V : NumberField<V> {
    abstract val executorSupply: LinearIntermediateSymbols3<Flt64>
    lateinit var supply: LinearIntermediateSymbols2<Flt64>
    abstract val cost: LinearIntermediateSymbols2<Flt64>

    override lateinit var quantity: LinearIntermediateSymbols1<Flt64>

    final override val timeSlots: List<StorageResourceTimeSlot<R, C, V>>

    init {
        AutoIndexed.flush<StorageResourceTimeSlot<R, C, V>>()

        val timeSlots = ArrayList<StorageResourceTimeSlot<R, C, V>>()
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

    override fun register(model: MetaModel<Flt64>): Try {
        if (!::supply.isInitialized) {
            supply = LinearIntermediateSymbols2<Flt64>(
                name = "${name}_supply",
                shape = Shape2(resources.size, timeWindow.timeSlots.size)
            ) { _, (r, s) ->
                val resource = resources[r]
                val slot = timeWindow.timeSlots[s]
                val time = TimeRange(timeWindow.start, slot.end)
                val fixedSupply = resource.fixedSupplyIn(time)
                val r = resources.indexOf(resource)
                val t = timeWindow.timeSlots.indexOfFirst { it.end == time.end }
                val executorSum = executorSupply[_a, r, t].fold(LinearPolynomial<Flt64>(emptyList(), Flt64.zero)) { acc, elem ->
                    acc + (elem as LinearIntermediateSymbol<Flt64>).toLinearPolynomial()
                }
                LinearExpressionSymbol(
                    polynomial = LinearPolynomial(emptyList(), fixedSupply.toSolverValue()) + executorSum,
                    name = "${name}_supply_${resource}_${slot}"
                )
            }
        }
        when (val result = model.add(supply)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (timeSlots.isNotEmpty()) {
            if (!::quantity.isInitialized) {
                quantity = LinearIntermediateSymbols1<Flt64>(
                    name = "${name}_quantity",
                    shape = Shape1(timeSlots.size)
                ) { s, _ ->
                    val slot = timeSlots[s]
                    val t = timeWindow.timeSlots.indexOfFirst { it.end == slot.time.end }
                    val r = resources.indexOf(slot.resource)
                    val quantityPoly = LinearPolynomial(emptyList(), slot.resource.solverInitialQuantity()) +
                        ((/* unchecked */ supply[r, t] as LinearIntermediateSymbol<Flt64>)).toLinearPolynomial() -
                        ((/* unchecked */ cost[r, t] as LinearIntermediateSymbol<Flt64>)).toLinearPolynomial()
                    LinearExpressionSymbol(
                        polynomial = quantityPoly,
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

        return super.register(model)
    }
}

/**
 * 任务调度存储资源使用 / Task scheduling storage resource usage
 *
 * @param E 执行器类型 / Executor type
 * @param R 存储资源类型 / Storage resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param timeWindow 时间窗口 / Time window
 * @param executors 执行器列表 / List of executors
 * @param resources 资源列表 / List of resources
 * @param times 时间槽列表 / List of time slots
 * @param interval 时间间隔 / Time interval
 * @param name 名称 / Name
 * @param overEnabled 是否启用超量 / Whether over quantity is enabled
 * @param lessEnabled 是否启用不足 / Whether less quantity is enabled
 */
class TaskSchedulingStorageResourceUsage<
        E : Executor,
        R : StorageResource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        >(
    timeWindow: TimeWindow<V>,
    executors: List<E>,
    resources: List<R>,
    times: List<TimeSlot>,
    interval: Duration = timeWindow.interval,
    override val name: String,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = false
) : AbstractStorageResourceUsage<E, R, C, V>(
    timeWindow = timeWindow,
    executors = executors,
    resources = resources,
    times = times,
    interval = interval
) where V : RealNumber<V>, V : NumberField<V> {
    constructor(
        timeWindow: TimeWindow<V>,
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
        timeWindow: TimeWindow<V>,
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

    override lateinit var executorSupply: LinearIntermediateSymbols3<Flt64>
    override lateinit var cost: LinearIntermediateSymbols2<Flt64>

    override fun register(model: MetaModel<Flt64>): Try {
        return Failed(
            ErrorCode.ApplicationFailed,
            "TaskSchedulingStorageResourceUsage.register 暂未实现，请使用 IterativeTaskSchedulingStorageResourceUsage 或补充任务级库位资源建模。"
        )
    }
}

/**
 * 迭代任务调度存储资源使用 / Iterative task scheduling storage resource usage
 *
 * @param E 执行器类型 / Executor type
 * @param R 存储资源类型 / Storage resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param timeWindow 时间窗口 / Time window
 * @param executors 执行器列表 / List of executors
 * @param resources 资源列表 / List of resources
 * @param times 时间槽列表 / List of time slots
 * @param interval 时间间隔 / Time interval
 * @param name 名称 / Name
 */
class IterativeTaskSchedulingStorageResourceUsage<
        E : Executor,
        R : StorageResource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        > private constructor(
    timeWindow: TimeWindow<V>,
    executors: List<E>,
    resources: List<R>,
    times: List<TimeSlot>,
    interval: Duration = timeWindow.interval,
    override val name: String
) : AbstractStorageResourceUsage<E, R, C, V>(
    timeWindow = timeWindow,
    executors = executors,
    resources = resources,
    times = times,
    interval = interval
) where V : RealNumber<V>, V : NumberField<V> {
    constructor(
        timeWindow: TimeWindow<V>,
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
        timeWindow: TimeWindow<V>,
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

    override lateinit var executorSupply: LinearExpressionSymbols3<Flt64>
    override lateinit var cost: LinearExpressionSymbols2<Flt64>

    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override fun register(model: MetaModel<Flt64>): Try {
        if (!::executorSupply.isInitialized) {
            executorSupply = LinearExpressionSymbols3(
                name = "${name}_executor_supply",
                shape = Shape3(executors.size, resources.size, timeWindow.timeSlots.size)
            ) { _, (e, r, s) ->
                val executor = executors[e]
                val resource = resources[r]
                val timeSlot = timeWindow.timeSlots[s]
                LinearExpressionSymbol(
                    Flt64,
                    name = "${name}_executor_supply_${executor}_${resource}_${timeSlot}"
                )
            }
        }
        when (val result = model.add(executorSupply)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
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
                    constant = fixedCost.toSolverValue(),
                    name = "${name}_cost_${resource}_${slot}"
                )
            }
        }
        when (val result = model.add(cost)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return super.register(model)
    }

    /**
     * 添加列贡献 / Add column contribution
     *
     * 用于迭代任务列生成场景，在每次迭代中添加新列的资源使用量贡献
     * Used for iterative task column generation, adds resource usage contribution from new columns in each iteration
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param iteration 当前迭代 / Current iteration
     * @param tasks 任务列表 / List of tasks
     * @param compilation 迭代编译对象 / Iterative compilation object
     * @return 成功与否 / Success or failure
     */
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
                                if (supplyQuantity != supplyQuantity.constants.zero) {
                                    Pair(task, supplyQuantity)
                                } else {
                                    null
                                }
                            }

                            if (thisTasks.isNotEmpty()) {
                                executorSupply[e, r, t].flush()
                                for ((task, supplyQuantity) in thisTasks) {
                                    executorSupply[e, r, t].asMutable() += LinearMonomial(supplyQuantity.toSolverValue(), xi[task])
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
                            if (costQuantity != costQuantity.constants.zero) {
                                Pair(task, costQuantity)
                            } else {
                                null
                            }
                        }
                        if (thisTasks.isNotEmpty()) {
                            cost[r, t].flush()
                            for ((task, costQuantity) in thisTasks) {
                                cost[r, t].asMutable() += LinearMonomial(costQuantity.toSolverValue(), xi[task])
                            }
                        }
                    }
                }
            }
        }
        return ok
    }
}

/**
 * 任务束调度存储资源使用 / Bunch scheduling storage resource usage
 *
 * @param E 执行器类型 / Executor type
 * @param R 存储资源类型 / Storage resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param timeWindow 时间窗口 / Time window
 * @param executors 执行器列表 / List of executors
 * @param resources 资源列表 / List of resources
 * @param times 时间槽列表 / List of time slots
 * @param interval 时间间隔 / Time interval
 * @param name 名称 / Name
 */
class BunchSchedulingStorageResourceUsage<
        E : Executor,
        R : StorageResource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        >(
    timeWindow: TimeWindow<V>,
    executors: List<E>,
    resources: List<R>,
    times: List<TimeSlot>,
    interval: Duration = timeWindow.interval,
    override val name: String
) : AbstractStorageResourceUsage<E, R, C, V>(
    timeWindow = timeWindow,
    executors = executors,
    resources = resources,
    times = times,
    interval = interval
) where V : RealNumber<V>, V : NumberField<V> {
    constructor(
        timeWindow: TimeWindow<V>,
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
        timeWindow: TimeWindow<V>,
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

    override lateinit var executorSupply: LinearExpressionSymbols3<Flt64>
    override lateinit var cost: LinearExpressionSymbols2<Flt64>

    override fun register(model: MetaModel<Flt64>): Try {
        if (!::executorSupply.isInitialized) {
            executorSupply = LinearExpressionSymbols3<Flt64>(
                name = "${name}_executor_supply",
                shape = Shape3(executors.size, resources.size, timeWindow.timeSlots.size)
            ) { _, (e, r, s) ->
                val executor = executors[e]
                val resource = resources[r]
                val slot = timeWindow.timeSlots[s]
                LinearExpressionSymbol(
                    Flt64,
                    name = "${name}_executor_supply_${executor}_${resource}_${slot}"
                )
            }
        }
        when (val result = model.add(executorSupply)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::cost.isInitialized) {
            cost = LinearExpressionSymbols2<Flt64>(
                name = "${name}_cost",
                shape = Shape2(resources.size, timeWindow.timeSlots.size)
            ) { _, (r, t) ->
                val resource = resources[r]
                val slot = timeWindow.timeSlots[t]
                val time = TimeRange(timeWindow.start, slot.end)
                val fixedCost = resource.fixedCostIn(time)
                LinearExpressionSymbol(
                    constant = fixedCost.toSolverValue(),
                    name = "${name}_cost_${resource}_${slot}"
                )
            }
        }
        when (val result = model.add(cost)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return super.register(model)
    }

    /**
     * 添加列贡献 / Add column contribution
     *
     * 用于任务束列生成场景，在每次迭代中添加新列的资源使用量贡献
     * Used for task bunch column generation, adds resource usage contribution from new columns in each iteration
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
    suspend fun <
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
                                if (supplyQuantity != supplyQuantity.constants.zero) {
                                    Pair(bunch, supplyQuantity)
                                } else {
                                    null
                                }
                            }

                            if (thisBunches.isNotEmpty()) {
                                executorSupply[e, r, t].flush()
                                for ((bunch, supplyQuantity) in thisBunches) {
                                    executorSupply[e, r, t].asMutable() += LinearMonomial(supplyQuantity.toSolverValue(), xi[bunch])
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
                            if (costQuantity != costQuantity.constants.zero) {
                                Pair(bunch, costQuantity)
                            } else {
                                null
                            }
                        }

                        if (thisBunches.isNotEmpty()) {
                            cost[r, t].flush()
                            for ((bunch, costQuantity) in thisBunches) {
                                cost[r, t].asMutable() += LinearMonomial(costQuantity.toSolverValue(), xi[bunch])
                            }
                        }
                    }
                }
            }
        }

        return ok
    }
}
