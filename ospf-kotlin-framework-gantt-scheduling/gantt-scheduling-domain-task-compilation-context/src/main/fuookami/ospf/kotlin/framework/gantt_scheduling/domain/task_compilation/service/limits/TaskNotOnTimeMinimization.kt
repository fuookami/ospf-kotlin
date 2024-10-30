package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

class TaskNotOnTimeMinimization<
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    private val tasks: List<T>,
    private val taskTime: TaskTime,
    private val threshold: UInt64 = UInt64.zero,
    private val coefficient: Flt64 = Flt64.one,
    override val name: String = "task_not_on_time_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: AbstractLinearMetaModel): Try {
        if (threshold eq UInt64.zero) {
            when (val result = model.minimize(
                coefficient * sum(taskTime.notOnTime[_a]),
                "task not on time"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else {
            val slack = SlackFunction(
                UInteger,
                x = sum(taskTime.notOnTime[_a]),
                threshold = LinearPolynomial(threshold),
                name = "task_not_on_time_threshold"
            )
            when (val result = model.add(slack)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.minimize(
                coefficient * slack,
                "task not on time")
            ) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }
}
