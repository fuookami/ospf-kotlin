@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskTime
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

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

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (task in tasks) {
            val maxDelay = requireNotNull(task.maxDelay) {
                "TaskOverMaxDelayTimeConstraint.invoke 要求 task.maxDelay 非空: $task"
            }
            when (val result = model.addConstraint(
                taskTime.delayTime[task] leq with(timeWindow) { maxDelay.value },
                name = "${name}_${task}",
                args = TaskOverMaxDelayShadowPriceKey(task)
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }

    override fun extractor(): AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> {
        return { map, args ->
            shadowPriceExtractor?.invoke(args) ?: when (args) {
                is TaskGanttSchedulingShadowPriceArguments<*, *> -> {
                    args.task?.let { task ->
                        map.map[TaskOverMaxDelayShadowPriceKey(task)]?.price ?: Flt64.zero
                    } ?: Flt64.zero
                }

                is BunchGanttSchedulingShadowPriceArguments<*, *> -> {
                    args.task?.let { task ->
                        map.map[TaskOverMaxDelayShadowPriceKey(task)]?.price ?: Flt64.zero
                    } ?: Flt64.zero
                }

                else -> {
                    Flt64.zero
                }
            }
        }
    }
    override fun refresh(
        shadowPriceMap: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        for (constraint in model.constraintsOfGroup()) {
            val task = (constraint.args as? TaskOverMaxDelayShadowPriceKey<E, A>)?.task ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                shadowPriceMap.put(ShadowPrice(TaskOverMaxDelayShadowPriceKey(task), price))
            }
        }

        return ok
    }
}


