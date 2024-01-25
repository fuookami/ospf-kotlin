package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*

data class TaskDelayTimeShadowPriceKey<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    val task: T
) : ShadowPriceKey(TaskDelayTimeShadowPriceKey::class)

class TaskDelayTimeConstraint<Args : GanttSchedulingShadowPriceArguments<E, A>, T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    private val timeWindow: TimeWindow,
    tasks: List<T>,
    private val taskTime: TaskTime,
    override val name: String = "task_delay_time"
) : GanttSchedulingCGPipeline<Args, E, A> {
    private val tasks = if (taskTime.delayEnabled) {
        tasks.filter { !it.delayEnabled && it.scheduledTime != null }
    } else {
        tasks.filter { it.scheduledTime != null }
    }

    override fun invoke(model: LinearMetaModel): Try {
        for (task in tasks) {
            model.addConstraint(
                taskTime.estimateStartTime[task] leq timeWindow.valueOf(task.scheduledTime!!.start),
                "${name}_$task"
            )
        }

        return Ok(success)
    }

    override fun extractor(): ShadowPriceExtractor<Args, AbstractGanttSchedulingShadowPriceMap<Args, E, A>> {
        return { map, args: Args ->
            args.thisTask?.let { map.map[TaskDelayTimeShadowPriceKey(it)]?.price } ?: Flt64.zero
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
                map.put(ShadowPrice(TaskDelayTimeShadowPriceKey(iterator.next()), shadowPrices[j]))
            }

            if (!iterator.hasNext()) {
                break
            }
        }
        return Ok(success)
    }
}
