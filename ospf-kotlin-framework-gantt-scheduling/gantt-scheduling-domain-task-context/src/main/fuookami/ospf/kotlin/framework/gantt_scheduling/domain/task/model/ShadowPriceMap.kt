package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

open class GanttSchedulingShadowPriceArguments<E : Executor, A : AssignmentPolicy<E>>(
    open val prevTask: AbstractTask<E, A>?,
    open val thisTask: AbstractTask<E, A>?,
    val executor: E
)

open class AbstractGanttSchedulingShadowPriceMap<Args : GanttSchedulingShadowPriceArguments<E, A>, E : Executor, A : AssignmentPolicy<E>>
    : AbstractShadowPriceMap<Args, AbstractGanttSchedulingShadowPriceMap<Args, E, A>>()

typealias GanttSchedulingShadowPriceMap<E, A> = AbstractGanttSchedulingShadowPriceMap<GanttSchedulingShadowPriceArguments<E, A>, E, A>

operator fun <E : Executor, A : AssignmentPolicy<E>> AbstractGanttSchedulingShadowPriceMap<GanttSchedulingShadowPriceArguments<E, A>, E, A>.invoke(
    prevTask: AbstractTask<E, A>?,
    thisTask: AbstractTask<E, A>?,
    executor: E
): Flt64 {
    return invoke(GanttSchedulingShadowPriceArguments(prevTask, thisTask, executor))
}

fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> AbstractGanttSchedulingShadowPriceMap<GanttSchedulingShadowPriceArguments<E, A>, E, A>.reducedCost(
    bunch: AbstractTaskBunch<T, E, A>
): Flt64 {
    var ret = bunch.cost.sum!!
    if (bunch.executor.indexed) {
        ret -= this(null, null, bunch.executor)
        for ((index, task) in bunch.tasks.withIndex()) {
            val prevTask = if (index != 0) {
                bunch.tasks[index - 1]
            } else {
                bunch.lastTask
            }
            ret -= this(prevTask, task, bunch.executor)
        }
        if (bunch.tasks.isNotEmpty()) {
            ret -= this(bunch.tasks.last(), null, bunch.executor)
        }
    }
    return ret
}

typealias GanttSchedulingShadowPriceExtractor<Args, E, A> = ShadowPriceExtractor<Args, AbstractGanttSchedulingShadowPriceMap<Args, E, A>>
typealias GanttSchedulingCGPipeline<Args, E, A> = CGPipeline<Args, LinearMetaModel, AbstractGanttSchedulingShadowPriceMap<Args, E, A>>
typealias GanttSchedulingCGPipelineList<Args, E, A> = List<CGPipeline<Args, LinearMetaModel, AbstractGanttSchedulingShadowPriceMap<Args, E, A>>>
