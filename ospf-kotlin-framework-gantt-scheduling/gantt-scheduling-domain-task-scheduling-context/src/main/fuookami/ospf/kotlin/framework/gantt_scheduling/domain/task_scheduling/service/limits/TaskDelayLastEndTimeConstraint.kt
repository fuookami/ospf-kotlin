package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*

data class TaskDelayLastEndTimeShadowPriceKey<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    val task: T
) : ShadowPriceKey(TaskDelayLastEndTimeShadowPriceKey::class)

class TaskDelayLastEndTimeConstraint<Args : GanttSchedulingShadowPriceArguments<E, A>, T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    private val timeWindow: TimeWindow,
    tasks: List<T>,
    private val taskTime: TaskTime,
    override val name: String = "task_delay_last_end_time"
) : GanttSchedulingCGPipeline<Args, E, A> {
    private val tasks = if (taskTime.delayLastEndTimeEnabled) {
        tasks.filter { !it.delayEnabled && it.lastEndTime != null }
    } else {
        tasks.filter { it.lastEndTime != null }
    }

    override operator fun invoke(model: LinearMetaModel): Try {
        for (task in tasks) {
            model.addConstraint(
                taskTime.estimateEndTime[task] leq timeWindow.valueOf(task.lastEndTime!!),
                "${name}_$task"
            )
        }
        return Ok(success)
    }

    override fun extractor(): ShadowPriceExtractor<Args, AbstractGanttSchedulingShadowPriceMap<Args, E, A>> {
        return { map, args: Args ->
            args.thisTask?.let { map.map[TaskDelayLastEndTimeShadowPriceKey(it)]?.price } ?: Flt64.zero
        }
    }

    override fun refresh(
        map: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: LinearMetaModel,
        shadowPrices: List<Flt64>
    ): Try {
        val indices = model.indicesOfConstraintGroup(name) ?: model.constraints.indices
        val iterator = tasks.iterator()
        for (j in indices) {
            if (model.constraints[j].name.startsWith(name)) {
                map.put(ShadowPrice(TaskDelayLastEndTimeShadowPriceKey(iterator.next()), shadowPrices[j]))
            }

            if (!iterator.hasNext()) {
                break
            }
        }
        return Ok(success)
    }
}
