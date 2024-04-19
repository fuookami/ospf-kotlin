package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

interface GanttSchedulingShadowPriceArguments<
    out E : Executor,
    out A : AssignmentPolicy<E>
>

open class ExecutorGanttSchedulingShadowPriceArguments<
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    val executor: E
) : GanttSchedulingShadowPriceArguments<E, A>

open class TaskGanttSchedulingShadowPriceArguments<
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    open val prevTask: AbstractTask<E, A>?,
    open val thisTask: AbstractTask<E, A>?
) : GanttSchedulingShadowPriceArguments<E, A>

open class ResourceGanttSchedulingShadowPriceArguments<
    out R : Resource<C>,
    out C : ResourceCapacity,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    val resource: R,
    val time: TimeRange
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

operator fun <
    E : Executor,
    A : AssignmentPolicy<E>
> AbstractGanttSchedulingShadowPriceMap<
    GanttSchedulingShadowPriceArguments<E, A>, E, A
>.invoke(
    executor: E
): Flt64 {
    return invoke(ExecutorGanttSchedulingShadowPriceArguments(executor))
}

operator fun <
    E : Executor,
    A : AssignmentPolicy<E>
> AbstractGanttSchedulingShadowPriceMap<
    GanttSchedulingShadowPriceArguments<E, A>, E, A
>.invoke(
    prevTask: AbstractTask<E, A>?,
    thisTask: AbstractTask<E, A>?
): Flt64 {
    return invoke(TaskGanttSchedulingShadowPriceArguments(prevTask, thisTask))
}

operator fun <
    E : Executor,
    A : AssignmentPolicy<E>,
    R : Resource<C>,
    C : ResourceCapacity
> AbstractGanttSchedulingShadowPriceMap<
    GanttSchedulingShadowPriceArguments<E, A>, E, A
>.invoke(
    resource: R,
    time: TimeRange = TimeRange()
): Flt64 {
    return invoke(ResourceGanttSchedulingShadowPriceArguments(resource, time))
}

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
        ret -= this(ExecutorGanttSchedulingShadowPriceArguments(bunch.executor))
        for ((index, task) in bunch.tasks.withIndex()) {
            val prevTask = if (index != 0) {
                bunch.tasks[index - 1]
            } else {
                bunch.lastTask
            }
            ret -= this(TaskGanttSchedulingShadowPriceArguments(prevTask, task))
        }
        if (bunch.tasks.isNotEmpty()) {
            ret -= this(TaskGanttSchedulingShadowPriceArguments(bunch.tasks.last(), null))
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
    Args, LinearMetaModel, AbstractGanttSchedulingShadowPriceMap<Args, E, A>
>
typealias GanttSchedulingCGPipeline<E, A> = AbstractGanttSchedulingCGPipeline<
    GanttSchedulingShadowPriceArguments<E, A>, E, A
>

typealias AbstractGanttSchedulingCGPipelineList<Args, E, A> = List<
    CGPipeline<Args, LinearMetaModel, AbstractGanttSchedulingShadowPriceMap<Args, E, A>>
>
typealias GanttSchedulingCGPipelineList<E, A> = AbstractGanttSchedulingCGPipelineList<
    GanttSchedulingShadowPriceArguments<E, A>, E, A
>
