package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

data class TaskAdvanceEarliestEndTimeShadowPriceKey<
    E : Executor,
    A : AssignmentPolicy<E>
>(
    val task: AbstractTask<E, A>
) : ShadowPriceKey(TaskAdvanceEarliestEndTimeShadowPriceKey::class)

class TaskAdvanceEarliestEndTimeConstraint<
    Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    private val timeWindow: TimeWindow,
    tasks: List<AbstractTask<E, A>>,
    private val taskTime: TaskTime,
    private val shadowPriceExtractor: ((Args) -> Flt64?)? = null,
    override val name: String = "task_advance_earliest_end_time"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val tasks = if (taskTime.advanceEarliestEndTimeEnabled) {
        tasks.filter { !it.advanceEnabled && it.earliestEndTime != null }
    } else {
        tasks.filter { it.earliestEndTime != null }
    }

    override fun invoke(model: AbstractLinearMetaModel): Try {
        for (task in tasks) {
            when (val result = model.addConstraint(
                taskTime.estimateEndTime[task] geq with(timeWindow) { task.earliestEndTime!!.value },
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

    override fun extractor(): AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> {
        return { map, args ->
            shadowPriceExtractor?.invoke(args) ?: when (args) {
                is TaskGanttSchedulingShadowPriceArguments<*, *> -> {
                    if (args.task != null) {
                        map.map[TaskAdvanceEarliestEndTimeShadowPriceKey(args.task!!)]?.price ?: Flt64.zero
                    } else {
                        Flt64.zero
                    }
                }

                is BunchGanttSchedulingShadowPriceArguments<*, *> -> {
                    if (args.task != null) {
                        map.map[TaskAdvanceEarliestEndTimeShadowPriceKey(args.task!!)]?.price ?: Flt64.zero
                    } else {
                        Flt64.zero
                    }
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
                map.put(ShadowPrice(TaskAdvanceEarliestEndTimeShadowPriceKey(iterator.next()), shadowPrices[j]))
            }

            if (!iterator.hasNext()) {
                break
            }
        }
        return ok
    }
}
