package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

class TaskStepConflictConstraint<
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    tasks: List<AbstractTask<E, A>>,
    private val compilation: Compilation,
    override val name: String = "task_step_conflict"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val conflictTaskGroup: List<List<T>> = TODO("not implement yet")

    override operator fun invoke(model: AbstractLinearMetaModel): Try {
        for ((i, tasks) in conflictTaskGroup.withIndex()) {
            when (val result = model.addConstraint(
                sum(tasks.map { t -> compilation.taskCompilation[t] }) leq UInt64.one,
                "task_step_conflict_$i"
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
        TODO("not implement yet")
    }

    override fun refresh(
        map: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel,
        shadowPrices: List<Flt64>
    ): Try {
        TODO("not implement yet")
    }
}
