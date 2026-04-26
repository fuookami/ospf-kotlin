@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingCGPipeline
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingShadowPriceArguments
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.Compilation
import fuookami.ospf.kotlin.utils.functional.*

class ExecutorCostMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val executors: List<E>,
    private val compilation: Compilation,
    private val coefficient: Extractor<Flt64?, E>? = null,
    override val name: String = "executor_cost_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override operator fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        coefficient?.let {
            when (val result = model.minimize(
                polynomial = sum(executors.map { e ->
                    val penalty = it(e) ?: Flt64.infinity
                    penalty * compilation.executorCompilation[e].toMathLinearPolynomial()
                }),
                name = "executor"
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
}



