@file:Suppress("DEPRECATION")
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.CapacityQuantity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow

/**
 * 产能调度聚合类
 * Capacity Scheduling Aggregation Class
 *
 * 聚合产能调度相关的数据和逻辑。
 * Aggregates data and logic related to capacity scheduling.
 *
 * @param V 数值类型 / Numeric type
 * @param A 生产动作类型 / Production action type
 */
class CapacitySchedulingAggregation<V : RealNumber<V>, A : ProductionAction>(
    /**
     * 生产动作列表
     * List of production actions
     */
    val actions: List<A>,

    /**
     * 时隙列表
     * List of time slots
     */
    val slots: List<TimeSlot>,

    /**
     * 时间窗口
     * Time window
     */
    val timeWindow: TimeWindow<V>
) {
    /**
     * 按执行器分组的动作
     * Actions grouped by executor
     */
    val actionsByExecutor: Map<String, List<A>> = actions.groupBy { it.executor.id }

    /**
     * 时隙数量
     * Number of time slots
     */
    val slotCount: Int
        get() = slots.size

    /**
     * 动作数量
     * Number of actions
     */
    val actionCount: Int
        get() = actions.size

    /**
     * 执行器数量
     * Number of executors
     */
    val executorCount: Int
        get() = actionsByExecutor.size

    /**
     * 获取指定执行器的动作列表
     * Get actions for specified executor
     *
     * @param executorId Executor identifier / 执行器标识
     * @return List of actions for the executor / 该执行器的动作列表
     */
    fun actionsForExecutor(executorId: String): List<A> {
        return actionsByExecutor[executorId] ?: emptyList()
    }

    /**
     * 计算总产能（所有动作的最大产能之和）
     * Calculate total capacity (sum of max capacity for all actions)
     *
     * 使用泛型 V 的 unitCapacityV 和 upperBoundV 入口，避免 Flt64 转换。
     * Uses generic V unitCapacityV and upperBoundV entries to avoid Flt64 conversion.
     *
     * @return Total capacity / 总产能
     */
    @Deprecated(
        message = "Use the Quantity-typed method instead",
        replaceWith = ReplaceWith("totalCapacityQuantity().value")
    )
    fun totalCapacity(): V {
        val zero = timeWindow.fromDouble(0.0)
        var total = zero
        for (action in actions) {
            for (slot in slots) {
                val ub = action.upperBoundV(slot, timeWindow)
                val unitCap = action.unitCapacityQuantity(timeWindow).value
                // Convert to Double for multiplication, then back to V
                // unitCap is V (capacity per unit), ub is UInt64 (unit count)
                val capacityForSlot = timeWindow.toDouble(unitCap) * ub.toLong().toDouble()
                total += timeWindow.fromDouble(capacityForSlot)
            }
        }
        return total
    }

    /**
     * 计算总产能物理量 / Calculate total capacity quantity
     *
     * @param unit 产能单位 / Capacity unit
     * @return Total capacity as Quantity<V> / 总产能物理量
     */
    fun totalCapacityQuantity(unit: PhysicalUnit = NoneUnit): CapacityQuantity<V> {
        return Quantity(totalCapacity(), unit)
    }

    /**
     * 获取指定时隙的索引
     * Get index for specified time slot
     *
     * @param slot Time slot / 时隙
     * @return Index of the time slot / 时隙索引
     */
    fun indexOfSlot(slot: TimeSlot): Int {
        return slots.indexOf(slot)
    }

    /**
     * 获取指定动作的索引
     * Get index for specified action
     *
     * @param action Production action / 生产动作
     * @return Index of the action / 动作索引
     */
    fun indexOfAction(action: A): Int {
        return actions.indexOf(action)
    }
}

// ── Flt64 向后兼容 typealias ──

@Deprecated("Use CapacitySchedulingAggregation<Flt64, A> directly") typealias Flt64CapacitySchedulingAggregation<A> = CapacitySchedulingAggregation<Flt64, A>
