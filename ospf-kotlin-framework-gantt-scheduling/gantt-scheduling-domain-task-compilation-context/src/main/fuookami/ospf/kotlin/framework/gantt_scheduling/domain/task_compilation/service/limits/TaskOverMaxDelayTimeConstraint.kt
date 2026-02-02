package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

data class TaskOverMaxDelayShadowPriceKey<
    E : Executor,
    A : AssignmentPolicy<E>
>(
    val task: AbstractTask<E, A>
) : ShadowPriceKey(TaskOverMaxDelayShadowPriceKey::class)

class TaskOverMaxDelayTimeConstraint<
    Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    private val timeWindow: TimeWindow,
    tasks: List<AbstractTask<E, A>>,
    private val taskTime: TaskTime,
    private val shadowPriceExtractor: ((Args) -> Flt64?)? = null,
    override val name: String = "task_over_max_delay_time"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val tasks = if (taskTime.overMaxDelayEnabled) {
        tasks.filter { !it.delayEnabled && it.maxDelay != null }
    } else {
        tasks.filter { it.maxDelay != null }
    }

    override fun invoke(model: AbstractLinearMetaModel): Try {
        for (task in tasks) {
            when (val result = model.addConstraint(
                taskTime.delayTime[task] leq with(timeWindow) { task.maxDelay!!.value },
                name = "${name}_${task}",
                args = TaskOverMaxDelayShadowPriceKey(task)
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
                        map.map[TaskOverMaxDelayShadowPriceKey(args.task!!)]?.price ?: Flt64.zero
                    } else {
                        Flt64.zero
                    }
                }

                is BunchGanttSchedulingShadowPriceArguments<*, *> -> {
                    if (args.task != null) {
                        map.map[TaskOverMaxDelayShadowPriceKey(args.task!!)]?.price ?: Flt64.zero
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

    @Suppress("UNCHECKED_CAST")
    override fun refresh(
        map: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel,
        shadowPrices: MetaDualSolution
    ): Try {
        for (constraint in model.constraintsOfGroup()) {
            val task = (constraint.args as? TaskOverMaxDelayShadowPriceKey<E, A>)?.task ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                map.put(ShadowPrice(TaskOverMaxDelayShadowPriceKey(task), price))
            }
        }

        return ok
    }
}
