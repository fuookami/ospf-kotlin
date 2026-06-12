
/**
 * 产能列
 * Capacity Column
 *
 * 一个产能列代表某台设备在某个时隙某个顺序位置的完整分配方案。
 * A column represents a complete allocation plan for an executor at a specific slot and order.
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 产能列
 * Capacity Column
 *
 * A column represents a complete allocation plan for an executor at a specific slot and order.
 * 一个产能列代表某台设备在某个时隙某个顺序位置的完整分配方案。
 *
 * @param E 执行者类型 / Executor type
 * @param A 生产动作类型 / Production action type
 * @param V 数值类型 / Numeric type for cost
 * @property executor 执行该列的设备 / Executor that performs this column
 * @property slotIndex 时隙索引 / Slot index
 * @property order 顺序位置 / Order position
 * @property allocations 动作分配 / Action allocations
 * @property columnCost 列成本物理量 / Column cost quantity
 */
data class CapacityColumn<E : Executor, A : ProductionAction, V : RealNumber<V>>(
    val executor: E,
    val slotIndex: Int,
    val order: Int,
    val allocations: Map<A, UInt64>,
    val columnCost: CapacityCostQuantity<V>
) {
    /**
     * 获取指定动作的分配数量
     * Get allocation amount for a specific action
     */
    fun amountFor(action: A): UInt64 {
        return allocations[action] ?: UInt64.zero
    }

    /**
     * 总分配数量
     * Total allocation amount
     */
    val totalAmount: UInt64
        get() = allocations.values.fold(UInt64.zero) { acc, amount -> acc + amount }

    /**
     * 是否为空列
     * Whether this is an empty column
     */
    val isEmpty: Boolean
        get() = allocations.isEmpty() || allocations.values.all { it == UInt64.zero }

    /**
     * 将列成本包装为物理量 / Wrap column cost as quantity
     *
     * @param unit 成本单位 / Cost unit
     * @return 列成本物理量 / Column cost quantity
     */
    fun costQuantity(unit: PhysicalUnit = NoneUnit): CapacityCostQuantity<V> {
        return Quantity(columnCost.value, unit)
    }
}
