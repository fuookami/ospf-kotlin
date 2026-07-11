
/**
 * 设备产能约束
 * Executor Capacity Constraint
 *
 * 每台设备在每个时隙的总产能不超过可用时长。
 * Total capacity per executor per slot should not exceed available duration.
*/
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 设备产能约束（适用于所有 Capacity 实现）
 * Executor Capacity Constraint (works for all Capacity implementations)
 *
 * 每台设备在每个时隙的总产能不超过可用时长。
 * Total capacity per executor per slot should not exceed available duration.
 *
 * @param V 数值类型 / Numeric type
 * @param A 生产动作类型 / Production action type
 * @property capacity 产能编译对象 / Capacity compilation object
 * @property slots 时隙列表 / List of time slots
 * @property timeWindow 时间窗口 / Time window
 * @property name 约束名称 / Constraint name
*/
class ExecutorCapacityConstraint<V : RealNumber<V>, A : ProductionAction>(
    private val capacity: Capacity<A>,
    private val slots: List<TimeSlot>,
    private val timeWindow: TimeWindow<V>,
    val name: String = "executor_capacity"
) {

    /**
     * 应用约束到模型
     * Apply constraint to model
     *
     * @param model Linear meta model / 线性元模型
     * @return Try result / Try 结果
    */
    operator fun invoke(model: LinearMetaModel<Flt64>): Try {
        for ((e, executor) in capacity.executors.withIndex()) {
            for ((s, slot) in slots.withIndex()) {
                // capacity[executor, slot] <= availableDuration
                // capacity[executor, slot] <= 可用时长
                val availableDuration = timeWindow.valueOf(slot.duration).toSolverValue()
                val constraint = capacity.capacity[e, s].polynomial leq availableDuration
                when (val result = model.addConstraint(constraint, name = "${name}_${executor.id}_$s")) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }

        return ok
    }
}
