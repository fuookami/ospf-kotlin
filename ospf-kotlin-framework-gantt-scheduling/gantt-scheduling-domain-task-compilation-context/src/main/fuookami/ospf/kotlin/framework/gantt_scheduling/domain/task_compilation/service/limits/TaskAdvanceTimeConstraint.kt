@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.core.model.mechanism.geq
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskTime
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

data class TaskAdvanceTimeShadowPriceKey<
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    val task: AbstractTask<E, A>
) : ShadowPriceKey(TaskAdvanceTimeShadowPriceKey::class)

class TaskAdvanceTimeConstraint<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val timeWindow: TimeWindow,
    tasks: List<AbstractTask<E, A>>,
    private val taskTime: TaskTime,
    private val shadowPriceExtractor: ((Args) -> Flt64?)? = null,
    override val name: String = "task_advance_time"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val tasks = if (taskTime.advanceEnabled) {
        tasks.filter { !it.advanceEnabled && it.scheduledTime != null }
    } else {
        tasks.filter { it.scheduledTime != null }
    }

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (task in tasks) {
            val scheduledTime = requireNotNull(task.scheduledTime) {
                "TaskAdvanceTimeConstraint.invoke 要求 task.scheduledTime 非空: $task"
            }
            when (val result = model.addConstraint(
                taskTime.estimateStartTime[task] geq with(timeWindow) { scheduledTime.start.value },
                name = "${name}_${task}",
                args = TaskAdvanceTimeShadowPriceKey(task)
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
                        map.map[TaskAdvanceTimeShadowPriceKey(task)]?.price ?: Flt64.zero
                    } ?: Flt64.zero
                }

                is BunchGanttSchedulingShadowPriceArguments<*, *> -> {
                    args.task?.let { task ->
                        map.map[TaskAdvanceTimeShadowPriceKey(task)]?.price ?: Flt64.zero
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
            val task = shadowPriceKeyOf<TaskAdvanceTimeShadowPriceKey<E, A>>(constraint.args)?.task ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                shadowPriceMap.put(ShadowPrice(TaskAdvanceTimeShadowPriceKey(task), price))
            }
        }

        return ok
    }
}


