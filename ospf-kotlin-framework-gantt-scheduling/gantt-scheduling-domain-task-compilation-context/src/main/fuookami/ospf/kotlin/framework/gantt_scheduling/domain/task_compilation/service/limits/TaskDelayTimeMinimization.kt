@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.core.intermediate_model.times
import fuookami.ospf.kotlin.core.intermediate_model.MutableLinearPolynomial
import fuookami.ospf.kotlin.core.intermediate_symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.core.variable.UInteger
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskTime
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import kotlin.time.Duration

class TaskDelayTimeMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val timeWindow: TimeWindow,
    tasks: List<T>,
    private val taskTime: TaskTime,
    private val threshold: Extractor<Duration?, T> = { Duration.ZERO },
    private val coefficient: Extractor<Flt64?, T> = { Flt64.one },
    override val name: String = "task_delay_time_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val tasks = if (taskTime.delayEnabled) {
        tasks.filter { it.delayEnabled && it.scheduledTime != null }
    } else {
        emptyList()
    }

    override fun invoke(model: AbstractLinearMetaModel): Try {
        if (taskTime.delayEnabled) {
            val cost = MutableLinearPolynomial()
            for (task in tasks) {
                val delayTime = taskTime.delayTime[task]
                val thisThreshold = threshold(task)?.let { with(timeWindow) { it.value } } ?: Flt64.zero
                val thisCoefficient = coefficient(task) ?: Flt64.infinity
                if (thisThreshold eq Flt64.zero) {
                    cost += thisCoefficient * delayTime
                } else {
                    val slack = SlackFunction(
                        x = delayTime,
                        threshold = thisThreshold,
                        type = if (timeWindow.continues) {
                            UContinuous
                        } else {
                            UInteger
                        },
                        name = "delay_time_threshold_${task}"
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
                    cost += thisCoefficient * slack
                }
            }

            when (val result = model.minimize(
                polynomial = cost,
                name = "task delay time"
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



