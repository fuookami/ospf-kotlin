package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.service.limits

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
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
    override val name: String = "executor_capacity"
) : Pipeline<AbstractLinearMetaModel> {

    override fun invoke(model: AbstractLinearMetaModel): Try {
        for ((e, executor) in executors.withIndex()) {
            for ((s, slot) in slots.withIndex()) {
                // capacity[executor, slot] <= availableDuration
                // capacity[executor, slot] <= 可用时长
                val availableDuration = Flt64(slot.duration / timeWindow.interval)
                model.addConstraint(
                    constraint = capacity.capacity[e, s] leq availableDuration,
                    name = "${name}_${executor.id}_$s"
                )
            }
        }

        return ok
    }
}