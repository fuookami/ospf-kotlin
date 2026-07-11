
/**
 * 产能调度解
 * Capacity Scheduling Solution
 *
 * 存储产能调度的完整解。
 * Stores complete solution for capacity scheduling.
*/
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/** 产能时长物理量 / Capacity duration quantity */
typealias CapacityDurationQuantity<V> = Quantity<V>

/**
 * 动作分配结果
 * Action Allocation Result
 *
 * Represents allocation result for a single action in a specific time slot.
 *
 * @param A 生产动作类型 / Production action type
*/
data class ActionAllocation<A : ProductionAction>(

    /**
     * 生产动作
     * The production action
    */
    val action: A,

    /**
     * 时隙
     * The time slot
    */
    val slot: TimeSlot,

    /**
     * 时隙索引
     * Slot index
    */
    val slotIndex: Int,

    /**
     * 分配数量
     * Allocated amount
    */
    val amount: UInt64,

    /**
     * 分配时长
     * Allocated duration
    */
    val duration: Duration,

    /**
     * 顺序（可选）
     * Order (optional)
    */
    val order: Int = 0
) {

    /**
     * 分配时长物理量 / Allocation duration quantity
     *
     * @param V 数值类型 / Numeric type
     * @param timeWindow 时间窗口 / Time window
     * @param unit 时长单位 / Duration unit
     * @return 分配时长物理量 / Allocation duration quantity
    */
    fun <V : RealNumber<V>> durationQuantity(
        timeWindow: TimeWindow<V>,
        unit: PhysicalUnit = NoneUnit
    ): CapacityDurationQuantity<V> {
        return timeWindow.quantityOf(duration, unit)
    }
}

/**
 * 执行器产能结果
 * Executor Capacity Result
 *
 * Represents capacity usage for a single executor in a specific time slot.
*/
data class ExecutorCapacityResult(

    /**
     * 执行器
    */
    val executor: Executor,

    /**
     * 时隙
     * The time slot
    */
    val slot: TimeSlot,

    /**
     * 时隙索引
     * Slot index
    */
    val slotIndex: Int,

    /**
     * 执行器总使用时长
     * Total used duration
    */
    val totalDuration: Duration
) {

    /**
     * 总使用时长物理量 / Total used duration quantity
     *
     * @param V 数值类型 / Numeric type
     * @param timeWindow 时间窗口 / Time window
     * @param unit 时长单位 / Duration unit
     * @return 总使用时长物理量 / Total used duration quantity
    */
    fun <V : RealNumber<V>> totalDurationQuantity(
        timeWindow: TimeWindow<V>,
        unit: PhysicalUnit = NoneUnit
    ): CapacityDurationQuantity<V> {
        return timeWindow.quantityOf(totalDuration, unit)
    }
}

/**
 * 产能调度解
 * Capacity Scheduling Solution
 *
 * Stores complete solution for capacity scheduling.
 *
 * @param A 生产动作类型 / Production action type
*/
data class CapacitySchedulingSolution<A : ProductionAction>(

    /**
     * 所有生产动作
    */
    val actions: List<A>,

    /**
     * 所有动作分配结果
    */
    val actionAllocations: List<ActionAllocation<A>>,

    /**
     * 所有执行器产能结果
     * All executor capacity results
    */
    val executorCapacities: List<ExecutorCapacityResult>,

    /**
     * 所有动作分配（按时隙分组）
     * All action allocations grouped by slot
    */
    val allocationsBySlot: Map<TimeSlot, List<ActionAllocation<A>>> = actionAllocations.groupBy { it.slot },

    /**
     * 所有执行器产能结果（按时隙分组）
    */
    val capacitiesBySlot: Map<TimeSlot, List<ExecutorCapacityResult>> = executorCapacities.groupBy { it.slot }
) {
    constructor(
        actions: List<A>,
        slots: List<TimeSlot>,
        actionAllocations: List<ActionAllocation<A>>,
        executorCapacities: List<ExecutorCapacityResult>
    ) : this(
        actions = actions,
        actionAllocations = actionAllocations,
        executorCapacities = executorCapacities,
        allocationsBySlot = actionAllocations.groupBy { it.slot },
        capacitiesBySlot = executorCapacities.groupBy { it.slot }
    )

    /**
     * 获取指定时隙的动作分配结果
     *
     * @param slot Time slot to query / 要查询的时隙
     * @return Action allocations in the specified slot / 指定时隙内的动作分配列表
    */
    fun allocationsInSlot(slot: TimeSlot): List<ActionAllocation<A>> {
        return allocationsBySlot[slot] ?: emptyList()
    }

    /**
     * 获取指定时隙的执行器产能结果
     * Get executor capacity results for specified slot
     *
     * @param slot Time slot to query / 要查询的时隙
     * @return Action allocations in the specified slot / 指定时隙内的动作分配列表
    */
    fun capacitiesInSlot(slot: TimeSlot): List<ExecutorCapacityResult> {
        return capacitiesBySlot[slot] ?: emptyList()
    }

    /**
     * 获取指定动作的所有分配结果
     *
     * @param action Production action to query / 要查询的生产动作
     * @return All allocation results for the specified action / 指定动作的所有分配结果列表
    */
    fun allocationsForAction(action: A): List<ActionAllocation<A>> {
        return actionAllocations.filter { it.action == action }
    }

    /**
     * 获取指定执行器的所有产能结果
     *
     * @param executor Executor to query / 要查询的执行器
     * @return All capacity results for the specified executor / 指定执行器的所有产能结果列表
    */
    fun capacitiesForExecutor(executor: Executor): List<ExecutorCapacityResult> {
        return executorCapacities.filter { it.executor == executor }
    }
}

