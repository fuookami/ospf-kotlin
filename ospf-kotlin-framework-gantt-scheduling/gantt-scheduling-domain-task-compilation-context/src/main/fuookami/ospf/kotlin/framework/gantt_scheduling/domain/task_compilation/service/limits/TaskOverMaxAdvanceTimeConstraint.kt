package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

data class TaskOverMaxAdvanceShadowPriceKey<
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    val task: T
) : ShadowPriceKey(TaskOverMaxAdvanceShadowPriceKey::class)

class TaskOverMaxAdvanceTimeConstraint<
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    private val timeWindow: TimeWindow,
    tasks: List<T>,
    private val taskTime: TaskTime,
    override val name: String = "task_over_max_advance_time"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val tasks = if (taskTime.overMaxAdvanceEnabled) {
        tasks.filter { !it.advanceEnabled && it.maxAdvance != null }
    } else {
        tasks.filter { it.maxAdvance != null }
    }

    override fun invoke(model: AbstractLinearMetaModel): Try {
        for (task in tasks) {
            when (val result = model.addConstraint(
                taskTime.advanceTime[task] leq with(timeWindow) { task.maxAdvance!!.value },
                "${name}_$task"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    @Suppress("UNCHECKED_CAST")
    override fun extractor(): AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> {
        return { map, args: Args ->
            when (args) {
                is TaskGanttSchedulingShadowPriceArguments<*, *> -> {
                    (args.thisTask as? T?)
                        ?.let { map.map[TaskOverMaxAdvanceShadowPriceKey(it)]?.price }
                        ?: Flt64.zero
                }

                else -> {
                    Flt64.zero
                }
            }
        }
    }

    override fun refresh(
        map: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel,
        shadowPrices: List<Flt64>
    ): Try {
        val indices = model.indicesOfConstraintGroup(name) ?: model.constraints.indices
        val iterator = tasks.iterator()
        for (j in indices) {
            if (model.constraints[j].name.startsWith(name)) {
                map.put(ShadowPrice(TaskOverMaxAdvanceShadowPriceKey(iterator.next()), shadowPrices[j]))
            }

            if (!iterator.hasNext()) {
                break
            }
        }
        return ok
    }
}
