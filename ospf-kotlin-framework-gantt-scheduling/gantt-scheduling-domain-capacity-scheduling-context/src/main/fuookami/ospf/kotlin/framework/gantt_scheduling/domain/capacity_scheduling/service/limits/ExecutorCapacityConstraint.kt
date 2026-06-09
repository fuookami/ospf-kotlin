@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.service.limits

import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.Capacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 设备产能约束（适用于所有 Capacity 实现）
 * Executor Capacity Constraint (works for all Capacity implementations)
 *
 * Total capacity per executor per slot should not exceed available duration.
 * 每台设备在每个时隙的总产能不超过可用时长。
 *
 * @param V 数值类型 / Numeric type
 * @param A 生产动作类型 / Production action type
 */
class ExecutorCapacityConstraint<V : RealNumber<V>, A : ProductionAction>(
    private val capacity: Capacity<A>,
    private val slots: List<TimeSlot>,
    private val timeWindow: TimeWindow<V>,
    val name: String = "executor_capacity"
) {
    /**
     * solver 边界使用的 Flt64 时间窗口
     * Flt64 time window for solver boundary
     */
    private val flt64TimeWindow: TimeWindow<Flt64> = TimeWindow(
        window = timeWindow.window,
        continues = timeWindow.continues,
        durationUnit = timeWindow.durationUnit,
        dateOffset = timeWindow.dateOffset,
        interval = timeWindow.interval,
        fromDouble = { Flt64(it) },
        toDouble = { it.toDouble() }
    )

    /**
     * 应用约束到模型
     * Apply constraint to model
     */
    operator fun invoke(model: LinearMetaModel<Flt64>): Try {
        for ((e, executor) in capacity.executors.withIndex()) {
            for ((s, slot) in slots.withIndex()) {
                // capacity[executor, slot] <= availableDuration
                // capacity[executor, slot] <= 可用时长
                val availableDuration = flt64TimeWindow.valueOf(slot.duration)
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

// ── Flt64 向后兼容 typealias ──

@Deprecated(
    message = "Use ExecutorCapacityConstraint<Flt64, A> directly",
    replaceWith = ReplaceWith("ExecutorCapacityConstraint<Flt64, A>")
)
typealias Flt64ExecutorCapacityConstraint<A> = ExecutorCapacityConstraint<Flt64, A>
