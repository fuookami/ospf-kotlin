package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

class MakespanMinimization<
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    private val timeWindow: TimeWindow,
    private val makespan: Makespan<*, E, A>,
    private val threshold: Instant = timeWindow.window.start,
    private val coefficient: Flt64 = Flt64.one,
    override val name: String = "makespan_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override operator fun invoke(model: AbstractLinearMetaModel): Try {
        val thresholdValue = timeWindow.valueOf(threshold)
        if (thresholdValue eq Flt64.zero) {
            when (val result = model.minimize(coefficient * makespan.makespan, "makespan")) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else {
            val slack = SlackFunction(
                if (timeWindow.continues) {
                    UContinuous
                } else {
                    UInteger
                },
                x = LinearPolynomial(makespan.makespan),
                threshold = LinearPolynomial(thresholdValue),
                name = "makespan_threshold"
            )
            when (val result = model.add(slack)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.minimize(coefficient * slack, "makespan")) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }
        return ok
    }
}
