package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

class TaskCancelMinimization<
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    tasks: List<T>,
    private val compilation: Compilation,
    private val coefficient: Extractor<Flt64?, T> = { Flt64.one },
    override val name: String = "task_cancel_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val tasks = if (compilation.taskCancelEnabled) {
        tasks.filter { it.cancelEnabled }
    } else {
        emptyList()
    }

    override fun invoke(model: AbstractLinearMetaModel): Try {
        if (compilation.taskCancelEnabled) {
            when (val result = model.minimize(
                sum(tasks.map { task ->
                    val thisCoefficient = coefficient(task) ?: Flt64.infinity
                    thisCoefficient * compilation.y[task]
                }),
                "task cancel"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }
}
