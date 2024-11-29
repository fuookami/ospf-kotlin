package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

interface GanttSchedulingShadowPriceArguments<
    out E : Executor,
    out A : AssignmentPolicy<E>
> {
    val executor: E
    val task: AbstractTask<E, A>?
}

open class TaskGanttSchedulingShadowPriceArguments<
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    override val executor: E,
    override val task: AbstractTask<E, A>? = null,
    open val prevTask: AbstractTask<E, A>? = null
) : GanttSchedulingShadowPriceArguments<E, A>

open class AbstractGanttSchedulingShadowPriceMap<
    out Args : GanttSchedulingShadowPriceArguments<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
> : AbstractShadowPriceMap<
    @UnsafeVariance Args, AbstractGanttSchedulingShadowPriceMap<@UnsafeVariance Args, @UnsafeVariance E, @UnsafeVariance A>
>()

typealias GanttSchedulingShadowPriceMap<E, A> = AbstractGanttSchedulingShadowPriceMap<
    GanttSchedulingShadowPriceArguments<E, A>, E, A
>

fun <
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
> AbstractGanttSchedulingShadowPriceMap<
    GanttSchedulingShadowPriceArguments<E, A>, E, A
>.reducedCost(
    bunch: AbstractTaskBunch<T, E, A>
): Flt64 {
    var ret = bunch.cost.sum!!
    if (bunch.executor.indexed) {
        ret -= this(TaskGanttSchedulingShadowPriceArguments(bunch.executor))
        for ((index, task) in bunch.tasks.withIndex()) {
            val prevTask = if (index != 0) {
                bunch.tasks[index - 1]
            } else {
                bunch.lastTask
            }
            ret -= this(TaskGanttSchedulingShadowPriceArguments(bunch.executor, prevTask, task))
        }
        if (bunch.tasks.isNotEmpty()) {
            ret -= this(TaskGanttSchedulingShadowPriceArguments(bunch.executor, bunch.tasks.last(), null))
        }
    }
    return ret
}

typealias AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> = ShadowPriceExtractor<
    Args, AbstractGanttSchedulingShadowPriceMap<Args, E, A>
>
typealias GanttSchedulingShadowPriceExtractor<E, A> = AbstractGanttSchedulingShadowPriceExtractor<
    GanttSchedulingShadowPriceArguments<E, A>, E, A
>

typealias AbstractGanttSchedulingCGPipeline<Args, E, A> = CGPipeline<
    Args, AbstractLinearMetaModel, AbstractGanttSchedulingShadowPriceMap<Args, E, A>
>
typealias GanttSchedulingCGPipeline<E, A> = AbstractGanttSchedulingCGPipeline<
    GanttSchedulingShadowPriceArguments<E, A>, E, A
>

typealias AbstractGanttSchedulingCGPipelineList<Args, E, A> = List<
    CGPipeline<Args, AbstractLinearMetaModel, AbstractGanttSchedulingShadowPriceMap<Args, E, A>>
>
typealias GanttSchedulingCGPipelineList<E, A> = AbstractGanttSchedulingCGPipelineList<
    GanttSchedulingShadowPriceArguments<E, A>, E, A
>
