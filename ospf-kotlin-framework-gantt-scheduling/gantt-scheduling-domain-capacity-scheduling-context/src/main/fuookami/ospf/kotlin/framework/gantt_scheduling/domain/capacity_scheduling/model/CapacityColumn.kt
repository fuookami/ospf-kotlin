package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64

/**
 * 产能�?
 * Capacity Column
 *
 * A column represents a complete allocation plan for an executor at a specific slot and order.
 * 一个产能列代表某台设备在某个时隙某个顺序位置的完整分配方案�?
 */
data class CapacityColumn<E : Executor, A : ProductionAction>(
    /**
     * 执行该列的设�?
     * Executor that performs this column
     */
    val executor: E,

    /**
     * 时隙索引
     * Slot index
     */
    val slotIndex: Int,

    /**
     * 顺序位置
     * Order position
     */
    val order: Int,

    /**
     * 动作分配
     * Action allocations
     * Map of action to amount
     */
    val allocations: Map<A, UInt64>,

    /**
     * 列成�?
     * Column cost
     */
    val cost: Flt64
) {
    /**
     * 获取指定动作的分配数�?
     * Get allocation amount for a specific action
     */
    fun amountFor(action: A): UInt64 {
        return allocations[action] ?: UInt64.zero
    }

    /**
     * 总分配数�?
     * Total allocation amount
     */
    val totalAmount: UInt64
        get() = allocations.values.fold(UInt64.zero) { acc, amount -> acc + amount }

    /**
     * 是否为空�?
     * Whether this is an empty column
     */
    val isEmpty: Boolean
        get() = allocations.isEmpty() || allocations.values.all { it == UInt64.zero }
}


