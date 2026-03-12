package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

/**
 * 动作分配结果
 * Action Allocation Result
 *
 * Represents the allocation of a production action in a specific time slot.
 * 表示生产动作在特定时隙的分配结果。
 */
data class ActionAllocation<A : ProductionAction>(
    val action: A,
    val slot: TimeSlot,
    val slotIndex: Int,
    val amount: UInt64,
    val duration: Duration,
    val order: Int? = null  // Only for CapacityOrderCompilation / 仅用于 CapacityOrderCompilation
)

/**
 * 设备产能结果
 * Executor Capacity Result
 *
 * Represents the total capacity of an executor in a specific time slot.
 * 表示设备在特定时隙的总产能。
 */
data class ExecutorCapacityResult(
    val executor: Executor,
    val slot: TimeSlot,
    val slotIndex: Int,
    val totalDuration: Duration
)

/**
 * 产能调度解
 * Capacity Scheduling Solution
 *
 * Contains the solution of capacity scheduling problem.
 * 包含产能调度问题的解。
 */
data class CapacitySchedulingSolution<A : ProductionAction>(
    val actions: List<A>,
    val slots: List<TimeSlot>,
    val actionAllocations: List<ActionAllocation<A>>,
    val executorCapacities: List<ExecutorCapacityResult>
) {
    /**
     * 获取指定动作的所有分配
     * Get all allocations for a specific action
     */
    fun allocationsFor(action: A): List<ActionAllocation<A>> {
        return actionAllocations.filter { it.action == action }
    }

    /**
     * 获取指定设备的所有产能结果
     * Get all capacity results for a specific executor
     */
    fun capacitiesFor(executor: Executor): List<ExecutorCapacityResult> {
        return executorCapacities.filter { it.executor == executor }
    }

    /**
     * 获取指定时隙的所有分配
     * Get all allocations for a specific slot
     */
    fun allocationsAt(slotIndex: Int): List<ActionAllocation<A>> {
        return actionAllocations.filter { it.slotIndex == slotIndex }
    }

    /**
     * 获取指定时隙的设备产能结果
     * Get executor capacity results for a specific slot
     */
    fun capacitiesAt(slotIndex: Int): List<ExecutorCapacityResult> {
        return executorCapacities.filter { it.slotIndex == slotIndex }
    }

    /**
     * 总成本
     * Total cost
     */
    val totalCost: Flt64
        get() = actionAllocations.map { 
            it.action.unitCost(it.slot.time) * Flt64(it.amount) 
        }.fold(Flt64.zero) { acc, cost -> acc + cost }
}