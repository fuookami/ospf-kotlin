@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.core.variable.UInteger
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingCGPipeline
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingShadowPriceArguments
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.Makespan
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import kotlin.time.Instant

class MakespanMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val timeWindow: TimeWindow,
    private val makespan: Makespan<*, E, A>,
    private val threshold: Instant = timeWindow.window.start,
    private val coefficient: Flt64 = Flt64.one,
    override val name: String = "makespan_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override operator fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val thresholdValue = with(timeWindow) { threshold.value }
        if (thresholdValue eq Flt64.zero) {
            when (val result = model.minimize(
                polynomial = coefficient * makespan.makespan.toLinearPolynomial(),
                name = "makespan"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        } else {
            val slack = thresholdSlack(
                x = makespan.makespan,
                threshold = thresholdValue,
                type = if (timeWindow.continues) {
                    UContinuous
                } else {
                    UInteger
                },
                name = "makespan_threshold"
            )
            when (val result = model.add(slack)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
            when (val result = model.minimize(
                polynomial = coefficient * slack.positiveSlackPolynomial(),
                name = "makespan"
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


