@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64
import kotlin.time.Duration

/**
 * 动作分配结果
 * Action Allocation Result
 *
 * 表示单个动作在特定时隙的分配结果�?
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
)

/**
 * 执行器产能结�?
 * Executor Capacity Result
 *
 * 表示单个执行器在特定时隙的产能使用情况�?
 * Represents capacity usage for a single executor in a specific time slot.
 */
data class ExecutorCapacityResult(
    /**
     * 执行�?
     * The executor
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
     * 总使用时�?
     * Total used duration
     */
    val totalDuration: Duration
)

/**
 * 产能调度�?
 * Capacity Scheduling Solution
 *
 * 存储产能调度的完整解�?
 * Stores complete solution for capacity scheduling.
 *
 * @param A 生产动作类型 / Production action type
 */
data class CapacitySchedulingSolution<A : ProductionAction>(
    /**
     * 所有动�?
     * All actions
     */
    val actions: List<A>,

    /**
     * 所有动作分�?
     * All action allocations
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
     * 所有执行器产能结果（按时隙分组�?
     * All executor capacity results grouped by slot
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
     * 获取指定时隙的动作分�?
     * Get action allocations for specified slot
     */
    fun allocationsInSlot(slot: TimeSlot): List<ActionAllocation<A>> {
        return allocationsBySlot[slot] ?: emptyList()
    }

    /**
     * 获取指定时隙的执行器产能结果
     * Get executor capacity results for specified slot
     */
    fun capacitiesInSlot(slot: TimeSlot): List<ExecutorCapacityResult> {
        return capacitiesBySlot[slot] ?: emptyList()
    }

    /**
     * 获取指定动作的所有分�?
     * Get all allocations for specified action
     */
    fun allocationsForAction(action: A): List<ActionAllocation<A>> {
        return actionAllocations.filter { it.action == action }
    }

    /**
     * 获取指定执行器的所有产能结�?
     * Get all capacity results for specified executor
     */
    fun capacitiesForExecutor(executor: Executor): List<ExecutorCapacityResult> {
        return executorCapacities.filter { it.executor == executor }
    }
}


