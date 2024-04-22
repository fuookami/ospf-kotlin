package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

class TaskExecutorCostMinimization<
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    private val tasks: List<T>,
    private val executors: List<E>,
    private val compilation: Compilation,
    private val costCalculator: Extractor<Flt64?, Pair<T, E>> = { Flt64.one },
    override val name: String = "task_executor_cost"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: AbstractLinearMetaModel): Try {
        when (val result = model.minimize(
            sum(tasks.flatMap { t ->
                executors.map { e ->
                    val coefficient = costCalculator(Pair(t, e)) ?: Flt64.infinity
                    coefficient * compilation.taskAssignment[t, e]
                }
            }),
            "task executor"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}
