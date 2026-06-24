@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlin.time.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.model.task.Iteration
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Try

/** 航班恢复调度演示的主应用入口。Main application entry point for the flight recovery scheduling demo. */
class Application {
}

/** 演示跨甘特调度框架类型的泛型数量使用的示例。Sample demonstrating generic quantity usage across gantt scheduling framework types. */
object Demo4GenericQuantitySample {
    private val time = TimeRange(
        start = Instant.parse("2026-06-07T00:00:00Z"),
        end = Instant.parse("2026-06-07T08:00:00Z")
    )

    private val quantityRange = ValueRange(
        FltX("0"),
        FltX("100"),
        Interval.Closed,
        Interval.Closed,
        FltX
    ).value!!

    val cost = Cost(
        items = listOf(
            CostItem(
                tag = "generic-demo",
                costQuantity = Quantity(FltX("1.25"), NoneUnit)
            )
        ),
        costSum = Quantity(FltX("1.25"), NoneUnit)
    )

    val quantityFieldCost = Cost(
        items = listOf(
            CostItem(
                tag = "generic-quantity-demo",
                costQuantity = Quantity(FltX("2.50"), NoneUnit)
            )
        ),
        costSum = Quantity(FltX("2.50"), NoneUnit)
    )

    val costQuantity = cost.sumQuantity()
    val materialDemandQuantity = MaterialDemand(
        quantityRangeValue = Quantity(quantityRange, NoneUnit)
    ).quantityRange()
    val resourceCapacityQuantity = ResourceCapacity(
        time = time,
        quantityRangeValue = Quantity(quantityRange, NoneUnit)
    ).quantityRange()
    val timeWindow = TimeWindow(
        window = time,
        durationUnit = DurationUnit.HOURS,
        interval = 1.toDuration(DurationUnit.HOURS),
        fromDouble = { FltX(it.toString()) },
        toDouble = { it.toDouble() }
    )
    val timeWindowDurationQuantity = timeWindow.quantityOf(2.toDuration(DurationUnit.HOURS))
    val timeWindowInstantQuantity = timeWindow.quantityOf(Instant.parse("2026-06-07T03:00:00Z"))
    val calendarActualTime = WorkingCalendar.ActualTime(
        time = TimeRange(
            start = Instant.parse("2026-06-07T00:00:00Z"),
            end = Instant.parse("2026-06-07T04:00:00Z")
        ),
        workingTimes = listOf(
            TimeRange(
                start = Instant.parse("2026-06-07T00:00:00Z"),
                end = Instant.parse("2026-06-07T03:00:00Z")
            )
        ),
        breakTimes = emptyList(),
        connectionTimes = emptyList()
    )
    val calendarWorkingDurationQuantity = calendarActualTime.workingDurationQuantity(timeWindow)
    val capacityAllocationDurationQuantity = ActionAllocation(
        action = Demo4Action,
        slot = time,
        slotIndex = 0,
        amount = UInt64(2UL),
        duration = 2.toDuration(DurationUnit.HOURS)
    ).durationQuantity(timeWindow)
    val executorCapacityDurationQuantity = ExecutorCapacityResult(
        executor = Demo4Action.executor,
        slot = time,
        slotIndex = 0,
        totalDuration = 3.toDuration(DurationUnit.HOURS)
    ).totalDurationQuantity(timeWindow)
    val slotIntermediateValues = CapacityIntermediateValues<ProductionAction, String, String, Flt64>(
        slots = listOf(time),
        results = mapOf(
            time to SlotBasedCapacityResult<ProductionAction, String, String, Flt64>(
                slot = time,
                slotIndex = 0,
                actionAllocations = emptyList<ActionAllocation<ProductionAction>>(),
                totalCostQuantityValue = Quantity(Flt64(1.0), NoneUnit),
                produceQuantityByProduct = mapOf("product" to Quantity(Flt64(2.0), NoneUnit)),
                consumptionQuantityByMaterial = emptyMap(),
                resourceUsageQuantityByResource = emptyMap()
            )
        )
    ).convertTo(GenericSolverValueAdapter(FltX))
    val slotProduceQuantity = slotIntermediateValues.produceQuantity(time, "product")
    val iterationSnapshot = Iteration<Demo4Task, Executor, AssignmentPolicy<Executor>>()
        .snapshot(GenericSolverValueAdapter(FltX))
    val taskSolutionSummary = TaskSolution(
        assignedTasks = listOf(Demo4Task(id = "assigned", name = "assigned")),
        canceledTasks = listOf(Demo4Task(id = "canceled", name = "canceled"))
    ).summary
    private val solverAdapter = GenericSolverValueAdapter(FltX)
    private val solverModel = LinearMetaModel(
        name = "demo4-g8-solved-quantity",
        converter = SchedulingSolverValueAdapter.Flt64
    )
    private val taskTime = Demo4TaskTime()
    val taskEstimateStartQuantity = taskTime.estimateStartTimeQuantity(
        task = Demo4Task(id = "task-time", name = "task-time"),
        model = solverModel,
        adapter = solverAdapter
    )
    val taskDelayLastEndTimeQuantity = taskTime.delayLastEndTimeQuantity(
        task = Demo4Task(id = "task-time", name = "task-time"),
        model = solverModel,
        adapter = solverAdapter
    )
    val taskAdvanceEarliestEndTimeQuantity = taskTime.advanceEarliestEndTimeQuantity(
        task = Demo4Task(id = "task-time", name = "task-time"),
        model = solverModel,
        adapter = solverAdapter
    )
    val switchTimeQuantity = Demo4Switch().switchTimeQuantity(
        from = Demo4Task(id = "from-task", name = "from-task"),
        to = Demo4Task(id = "to-task", name = "to-task"),
        model = solverModel,
        adapter = solverAdapter
    )
    val makespanQuantity = Makespan(
        tasks = listOf(Demo4Task(id = "makespan", name = "makespan")),
        taskTime = taskTime
    ).apply {
        makespan = LinearExpressionSymbol(Flt64(4.0), name = "demo4_makespan")
    }.quantity(
        model = solverModel,
        adapter = solverAdapter
    )
    val overMaxDelayTimeQuantity = taskTime.overMaxDelayTimeQuantity(
        task = Demo4Task(id = "task-time", name = "task-time"),
        model = solverModel,
        adapter = solverAdapter
    )
    val overMaxAdvanceTimeQuantity = taskTime.overMaxAdvanceTimeQuantity(
        task = Demo4Task(id = "task-time", name = "task-time"),
        model = solverModel,
        adapter = solverAdapter
    )
    private val demo4Resource = Demo4Resource(
        capacities = listOf(
            ResourceCapacity(
                time = time,
                quantityRangeValue = Quantity(quantityRange, NoneUnit)
            )
        )
    )
    private val demo4ResourceSlot = ExecutionResourceTimeSlot(
        origin = time,
        resource = demo4Resource,
        resourceCapacity = demo4Resource.capacities.first(),
        indexInRule = UInt64.zero
    )
    val resourceSolvedQuantity = Demo4ResourceUsage(demo4ResourceSlot).solvedQuantity(
        slot = demo4ResourceSlot,
        model = solverModel,
        adapter = solverAdapter
    )
    private val product = Demo4Material(index = 0, label = "product")
    private val material = Demo4Material(index = 1, label = "material")
    val produceSolvedQuantity = Demo4Produce(product).solvedQuantity(
        product = product,
        model = solverModel,
        adapter = solverAdapter
    )
    val consumptionSolvedQuantity = Demo4Consumption(material).solvedQuantity(
        material = material,
        model = solverModel,
        adapter = solverAdapter
    )

    @Suppress("unused")
    val rawQuantity = Quantity(FltX("3.5"), costQuantity!!.unit)

    @Suppress("unused")
    val quantityFieldSum = quantityFieldCost.costSum
}

/** 测试泛型数量转换的演示任务实现。Demo task implementation for testing generic quantity conversions. */
private data class Demo4Task(
    override val index: Int = 0,
    override val id: String,
    override val name: String,
    override val iteration: Int64 = Int64.zero
) : IterativeAbstractTask<Executor, AssignmentPolicy<Executor>> {
    override fun partialEq(rhs: AbstractTask<Executor, AssignmentPolicy<Executor>>): Boolean? {
        return this === rhs
    }
}

/** 用于测试的具有常量符号值的演示任务时间配置。Demo task time configuration with constant symbol values for testing. */
private class Demo4TaskTime : TaskTime {
    override val delayEnabled: Boolean = true
    override val overMaxDelayEnabled: Boolean = false
    override val advanceEnabled: Boolean = true
    override val overMaxAdvanceEnabled: Boolean = false
    override val delayLastEndTimeEnabled: Boolean = false
    override val advanceEarliestEndTimeEnabled: Boolean = false
    override val estimateStartTime = constantSymbols("demo4_estimate_start_time", Flt64(1.0))
    override val estimateEndTime = constantSymbols("demo4_estimate_end_time", Flt64(2.0))
    override val delayTime = constantSymbols("demo4_delay_time", Flt64.zero)
    override val advanceTime = constantSymbols("demo4_advance_time", Flt64.zero)
    override val overMaxDelayTime = constantSymbols("demo4_over_max_delay_time", Flt64.zero)
    override val overMaxAdvanceTime = constantSymbols("demo4_over_max_advance_time", Flt64.zero)
    override val delayLastEndTime = constantSymbols("demo4_delay_last_end_time", Flt64.zero)
    override val advanceEarliestEndTime = constantSymbols("demo4_advance_earliest_end_time", Flt64.zero)
    override val onTime = constantSymbols("demo4_on_time", Flt64.one)
    override val notOnTime = constantSymbols("demo4_not_on_time", Flt64.zero)

    override fun register(model: MetaModel<Flt64>): Try {
        return ok
    }
}

/** 任务转换建模的演示切换实现。Demo switch implementation for task transition modeling. */
private class Demo4Switch : Switch {
    override val switch = LinearIntermediateSymbols3<Flt64>(
        name = "demo4_switch",
        shape = Shape3(1, 1, 1)
    ) { _, _ ->
        LinearExpressionSymbol(Flt64.one, name = "demo4_switch")
    }
    override val switchTime = LinearIntermediateSymbols2<Flt64>(
        name = "demo4_switch_time",
        shape = Shape2(1, 1)
    ) { _, _ ->
        LinearExpressionSymbol(Flt64(1.5), name = "demo4_switch_time")
    }

    override fun register(model: MetaModel<Flt64>): Try {
        return ok
    }
}

/** 容量调度示例的演示执行资源。Demo execution resource for capacity scheduling samples. */
private class Demo4Resource(
    override val capacities: List<ResourceCapacity<FltX>>
) : ExecutionResource<ResourceCapacity<FltX>, FltX>(
    id = "demo4-resource",
    name = "demo4-resource",
    capacities = capacities,
    initialQuantityValue = FltX.zero
) {
    override fun <E : Executor, A : AssignmentPolicy<E>> usedBy(
        task: AbstractTask<E, A>,
        time: TimeRange
    ): FltX {
        return FltX.zero
    }
}

/** 容量示例的演示资源使用跟踪。Demo resource usage tracking for capacity samples. */
private class Demo4ResourceUsage(
    slot: ExecutionResourceTimeSlot<Demo4Resource, ResourceCapacity<FltX>, FltX>
) : ResourceUsage<ExecutionResourceTimeSlot<Demo4Resource, ResourceCapacity<FltX>, FltX>, Demo4Resource, ResourceCapacity<FltX>, FltX> {
    override val name: String = "demo4_resource_usage"
    override val timeSlots = listOf(slot)
    override val quantity = constantSymbols("demo4_resource_quantity", Flt64(3.0))
    override val overQuantity = constantSymbols("demo4_resource_over_quantity", Flt64.zero)
    override val lessQuantity = constantSymbols("demo4_resource_less_quantity", Flt64.zero)
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override fun register(model: MetaModel<Flt64>): Try {
        return ok
    }
}

/** 生产/消耗建模的演示物料。Demo material for production/consumption modeling. */
private data class Demo4Material(
    override val index: Int,
    val label: String
) : Material {
    override val material: Material get() = this
}

/** 物料输出建模的演示生产量。Demo production quantity for material output modeling. */
private class Demo4Produce(product: AbstractMaterial) : Produce {
    override val quantity = constantSymbols("demo4_produce_quantity", Flt64(5.0), product.index + 1)
    override val overQuantity = constantSymbols("demo4_produce_over_quantity", Flt64.zero, product.index + 1)
    override val lessQuantity = constantSymbols("demo4_produce_less_quantity", Flt64.zero, product.index + 1)
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        return ok
    }
}

/** 物料输入建模的演示消耗量。Demo consumption quantity for material input modeling. */
private class Demo4Consumption(material: AbstractMaterial) : Consumption {
    override val quantity = constantSymbols("demo4_consumption_quantity", Flt64(2.0), material.index + 1)
    override val overQuantity = constantSymbols("demo4_consumption_over_quantity", Flt64.zero, material.index + 1)
    override val lessQuantity = constantSymbols("demo4_consumption_less_quantity", Flt64.zero, material.index + 1)
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        return ok
    }
}

/**
 * 创建给定名称和值的一维常量 [LinearExpressionSymbol] 数组。
 * Creates a 1D array of constant [LinearExpressionSymbol] with the given name and value.
 *
 * @param name 名称 / Name
 * @param value 值 / Value
 * @param size 大小 / Size
 * @return 线性中间符号一维数组 / 1D linear intermediate symbols
 */
private fun constantSymbols(
    name: String,
    value: Flt64,
    size: Int = 1
): LinearIntermediateSymbols1<Flt64> {
    return LinearIntermediateSymbols1(
        name = name,
        shape = Shape1(size)
    ) { _, _ ->
        LinearExpressionSymbol(value, name = name)
    }
}

/** 容量分配示例的演示生产动作。Demo production action for capacity allocation samples. */
private object Demo4Action : ProductionAction {
    override val id: String = "demo4-action"
    override val name: String = "demo4-action"
    override val executor: Executor = Executor("demo4-executor", "demo4-executor")
    override val discrete: Boolean = false

    override fun <V : RealNumber<V>> unitCapacity(timeWindow: TimeWindow<V>): V {
        return timeWindow.fromDouble(1.0)
    }

    override fun <V : RealNumber<V>> unitCost(time: Instant, fromDouble: (Double) -> V): V {
        return fromDouble(1.0)
    }

    override fun <V : RealNumber<V>> upperBound(
        slot: TimeSlot,
        timeWindow: TimeWindow<V>
    ): UInt64 {
        return UInt64.one
    }
}
