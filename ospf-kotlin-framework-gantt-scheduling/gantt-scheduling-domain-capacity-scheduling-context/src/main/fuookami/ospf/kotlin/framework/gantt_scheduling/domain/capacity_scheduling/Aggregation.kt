@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 产能调度聚合�?
 * Capacity Scheduling Aggregation Class
 *
 * 聚合产能调度相关的数据和逻辑�?
 * Aggregates data and logic related to capacity scheduling.
 *
 * @param A 生产动作类型 / Production action type
 */
class CapacitySchedulingAggregation<A : ProductionAction>(
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
    val timeWindow: TimeWindow<Flt64>
) {
    /**
     * 按执行器分组的动�?
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
     * 执行器数�?
     * Number of executors
     */
    val executorCount: Int
        get() = actionsByExecutor.size

    /**
     * 获取指定执行器的动作列表
     * Get actions for specified executor
     *
     * @param executorId Executor identifier / 执行器标�?
     * @return List of actions for the executor / 该执行器的动作列�?
     */
    fun actionsForExecutor(executorId: String): List<A> {
        return actionsByExecutor[executorId] ?: emptyList()
    }

    /**
     * 计算总产能（所有动作的最大产能之和）
     * Calculate total capacity (sum of max capacity for all actions)
     *
     * @return Total capacity / 总产�?
     */
    fun totalCapacity(): Flt64 {
        var total = Flt64.zero
        for (action in actions) {
            for (slot in slots) {
                val ub = action.upperBound(slot, timeWindow)
                val unitCap = action.unitCapacity(timeWindow)
                total += unitCap * ub.toFlt64()
            }
        }
        return total
    }

    /**
     * 获取指定时隙的索�?
     * Get index for specified time slot
     *
     * @param slot Time slot / 时隙
     * @return Index of the time slot / 时隙索引
     */
    fun indexOfSlot(slot: TimeSlot): Int {
        return slots.indexOf(slot)
    }

    /**
     * 获取指定动作的索�?
     * Get index for specified action
     *
     * @param action Production action / 生产动作
     * @return Index of the action / 动作索引
     */
    fun indexOfAction(action: A): Int {
        return actions.indexOf(action)
    }
}

