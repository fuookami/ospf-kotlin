package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import kotlin.time.*
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

class TaskDelayLastEndTimeMinimization<
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    private val timeWindow: TimeWindow,
    tasks: List<T>,
    private val taskTime: TaskTime,
    private val threshold: Extractor<Duration?, T> = { Duration.ZERO },
    private val coefficient: Extractor<Flt64?, T> = { Flt64.one },
    override val name: String = "task_delay_last_end_time_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val tasks = if (taskTime.delayLastEndTimeEnabled) {
        tasks.filter { it.delayEnabled && it.lastEndTime != null }
    } else {
        emptyList()
    }

    override fun invoke(model: AbstractLinearMetaModel): Try {
        if (taskTime.delayLastEndTimeEnabled) {
            val cost = MutableLinearPolynomial()
            for (task in tasks) {
                val delayTime = taskTime.delayLastEndTime[task]
                val thisThreshold = threshold(task)?.let { timeWindow.valueOf(it) } ?: Flt64.zero
                val thisCoefficient = coefficient(task) ?: Flt64.infinity
                if (thisThreshold eq Flt64.zero) {
                    cost += thisCoefficient * delayTime
                } else {
                    val slack = SlackFunction(
                        if (timeWindow.continues) {
                            UContinuous
                        } else {
                            UInteger
                        },
                        x = LinearPolynomial(delayTime),
                        threshold = LinearPolynomial(thisThreshold),
                        name = "delay_last_end_time_threshold_$task"
                    )
                    when (val result = model.add(slack)) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                    cost += thisCoefficient * slack
                }
            }

            when (val result = model.minimize(
                cost,
                "task delay last end time"
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
