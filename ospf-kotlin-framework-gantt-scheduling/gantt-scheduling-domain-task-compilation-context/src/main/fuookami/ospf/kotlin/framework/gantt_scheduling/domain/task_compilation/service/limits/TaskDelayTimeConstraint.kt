package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

data class TaskDelayTimeShadowPriceKey<
    E : Executor,
    A : AssignmentPolicy<E>
>(
    val task: AbstractTask<E, A>
) : ShadowPriceKey(TaskDelayTimeShadowPriceKey::class)

class TaskDelayTimeConstraint<
    Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    private val timeWindow: TimeWindow,
    tasks: List<AbstractTask<E, A>>,
    private val taskTime: TaskTime,
    private val shadowPriceExtractor: ((Args) -> Flt64?)? = null,
    override val name: String = "task_delay_time"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val tasks = if (taskTime.delayEnabled) {
        tasks.filter { !it.delayEnabled && it.scheduledTime != null }
    } else {
        tasks.filter { it.scheduledTime != null }
    }

    override fun invoke(model: AbstractLinearMetaModel): Try {
        for (task in tasks) {
            when (val result = model.addConstraint(
                taskTime.estimateStartTime[task] leq with(timeWindow) { task.scheduledTime!!.start.value },
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
                        map.map[TaskDelayTimeShadowPriceKey(args.task!!)]?.price ?: Flt64.zero
                    } else {
                        Flt64.zero
                    }
                }

                is BunchGanttSchedulingShadowPriceArguments<*, *> -> {
                    if (args.task != null) {
                        map.map[TaskDelayTimeShadowPriceKey(args.task!!)]?.price ?: Flt64.zero
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
        shadowPrices: MetaDualSolution
    ): Try {
        val indices = model.indicesOfConstraintGroup(name) ?: model.constraints.indices
        val iterator = tasks.iterator()
        for (j in indices) {
            if (model.constraints[j].name.startsWith(name)) {
                shadowPrices.constraints[model.constraints[j]]?.let { price ->
                    map.put(ShadowPrice(TaskDelayTimeShadowPriceKey(iterator.next()), price))
                }
            }

            if (!iterator.hasNext()) {
                break
            }
        }
        return ok
    }
}
