@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.core.variable.UInteger
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.Switch
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import kotlin.time.Duration

class SwitchTimeMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val timeWindow: TimeWindow,
    private val tasks: List<T>,
    private val switch: Switch,
    private val threshold: Extractor<Duration?, Pair<T, T>> = { Duration.ZERO },
    private val coefficient: Extractor<Flt64?, Pair<T, T>> = { Flt64.one },
    override val name: String = "switch_time_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val cost = MutableLinearPolynomial<Flt64>(constant = Flt64.zero)
        for (task1 in tasks) {
            for (task2 in tasks) {
                val switchTime = switch.switchTime[task1, task2]
                val thisThreshold = threshold(Pair(task1, task2))?.let { with(timeWindow) { it.value } } ?: Flt64.zero
                val thisCoefficient = coefficient(Pair(task1, task2)) ?: Flt64.infinity
                if (thisThreshold eq Flt64.zero) {
                    cost += thisCoefficient * switchTime.toLinearPolynomial()
                } else {
                    val slack = thresholdSlack(
                        x = switchTime,
                        threshold = thisThreshold,
                        type = if (timeWindow.continues) {
                            UContinuous
                        } else {
                            UInteger
                        },
                        name = "switch_time_threshold_${task1}_${task2}"
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
                    cost += thisCoefficient * slack.positiveSlackPolynomial()
                }
            }
        }
        when (val result = model.minimize(
            polynomial = cost.toLinearPolynomial(),
            name = "switch time"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }
}
