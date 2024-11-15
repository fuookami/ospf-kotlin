package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

interface AbstractGanttSchedulingShadowPriceArguments<
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
) : AbstractGanttSchedulingShadowPriceArguments<E, A>

open class BunchGanttSchedulingShadowPriceArguments<
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    override val executor: E,
    override val task: AbstractTask<E, A>? = null,
    open val prevTask: AbstractTask<E, A>? = null
) : AbstractGanttSchedulingShadowPriceArguments<E, A>

open class AbstractGanttSchedulingShadowPriceMap<
    out Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
> : AbstractShadowPriceMap<
    @UnsafeVariance Args, AbstractGanttSchedulingShadowPriceMap<@UnsafeVariance Args, @UnsafeVariance E, @UnsafeVariance A>
>()

typealias GanttSchedulingShadowPriceMap<E, A> = AbstractGanttSchedulingShadowPriceMap<
    AbstractGanttSchedulingShadowPriceArguments<E, A>, E, A
>

fun <
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
> AbstractGanttSchedulingShadowPriceMap<
    AbstractGanttSchedulingShadowPriceArguments<E, A>, E, A
>.reducedCost(
    bunch: AbstractTaskBunch<T, E, A>
): Flt64 {
    var ret = bunch.cost.sum!!
    if (bunch.executor.indexed) {
        ret -= this(BunchGanttSchedulingShadowPriceArguments(bunch.executor))
        for ((index, task) in bunch.tasks.withIndex()) {
            val prevTask = if (index != 0) {
                bunch.tasks[index - 1]
            } else {
                bunch.lastTask
            }
            ret -= this(BunchGanttSchedulingShadowPriceArguments(bunch.executor, prevTask, task))
        }
        if (bunch.tasks.isNotEmpty()) {
            ret -= this(BunchGanttSchedulingShadowPriceArguments(bunch.executor, bunch.tasks.last(), null))
        }
    }
    return ret
}

typealias AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> = ShadowPriceExtractor<
    Args, AbstractGanttSchedulingShadowPriceMap<Args, E, A>
>
typealias GanttSchedulingShadowPriceExtractor<E, A> = AbstractGanttSchedulingShadowPriceExtractor<
    AbstractGanttSchedulingShadowPriceArguments<E, A>, E, A
>

typealias AbstractGanttSchedulingCGPipeline<Args, E, A> = CGPipeline<
    Args, AbstractLinearMetaModel, AbstractGanttSchedulingShadowPriceMap<Args, E, A>
>
typealias GanttSchedulingCGPipeline<E, A> = AbstractGanttSchedulingCGPipeline<
    AbstractGanttSchedulingShadowPriceArguments<E, A>, E, A
>

typealias AbstractGanttSchedulingCGPipelineList<Args, E, A> = List<
    CGPipeline<Args, AbstractLinearMetaModel, AbstractGanttSchedulingShadowPriceMap<Args, E, A>>
>
typealias GanttSchedulingCGPipelineList<E, A> = AbstractGanttSchedulingCGPipelineList<
    AbstractGanttSchedulingShadowPriceArguments<E, A>, E, A
>
