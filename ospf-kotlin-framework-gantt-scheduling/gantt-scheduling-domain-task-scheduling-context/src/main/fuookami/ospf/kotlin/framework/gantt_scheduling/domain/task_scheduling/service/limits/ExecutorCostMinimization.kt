package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*

class ExecutorCostMinimization<Args : GanttSchedulingShadowPriceArguments<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    private val executors: List<E>,
    private val compilation: Compilation,
    private val executorCostCalculator: Extractor<Flt64?, E>? = null,
    override val name: String = "executor_cost_minimization"
) : GanttSchedulingCGPipeline<Args, E, A> {
    override operator fun invoke(model: LinearMetaModel): Try {
        executorCostCalculator?.let {
            model.minimize(
                sum(executors.map { e ->
                    val penalty = it(e) ?: Flt64.infinity
                    penalty * compilation.executorCompilation[e]
                }),
                "executor"
            )
        }
        return Ok(success)
    }
}
