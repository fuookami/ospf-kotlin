package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.service.limits

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

/**
 * 设备产能约束（适用于所有 Capacity 实现）
 * Executor Capacity Constraint (works for all Capacity implementations)
 *
 * Total capacity per executor per slot should not exceed available duration.
 * 每台设备在每个时隙的总产能不超过可用时长。
 */
class ExecutorCapacityConstraint<A : ProductionAction>(
    private val capacity: Capacity<A>,
    private val executors: List<Executor>,
    private val slots: List<TimeSlot>,
    private val timeWindow: TimeWindow,
    val name: String = "executor_capacity"
) {
    /**
     * 应用约束到模型
     * Apply constraint to model
     */
    operator fun invoke(model: LinearMetaModel): Try {
        for ((e, executor) in executors.withIndex()) {
            for ((s, slot) in slots.withIndex()) {
                // capacity[executor, slot] <= availableDuration
                // capacity[executor, slot] <= 可用时长
                val availableDuration = timeWindow.valueOf(slot.duration)
                val constraint = capacity.capacity[e, s].toLinearPolynomial() leq availableDuration
                when (val result = model.addConstraint(constraint, name = "${name}_${executor.id}_$s")) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                }
            }
        }

        return ok
    }
}