@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 任务超最大提前时间最小化 / Task over-max advance time minimization */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.core.variable.UInteger
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskTime
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import kotlin.time.Duration

/**
 * 任务超最大提前时间最小化 / Task over-max advance time minimization
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param timeWindow 时间窗口 / Time window
 * @param tasks 任务列表 / List of tasks
 * @param taskTime 任务时间对象 / Task time object
 * @param threshold 阈值提取器 / Threshold extractor
 * @param coefficient 成本系数提取器 / Extractor for cost coefficient
 * @param name 管道名称 / Pipeline name
 */
class TaskOverMaxAdvanceTimeMinimization<
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
    override val name: String = "task_over_max_advance_time_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val tasks = if (taskTime.overMaxAdvanceEnabled) {
        tasks.filter { it.advanceEnabled && it.maxAdvance != null }
    } else {
        emptyList()
    }

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        if (taskTime.overMaxAdvanceEnabled) {
            val cost = MutableLinearPolynomial<Flt64>(constant = Flt64.zero)
            for (task in tasks) {
                val overMaxAdvanceTime = taskTime.overMaxAdvanceTime[task]
                val thisThreshold = threshold(task)?.let { with(timeWindow) { it.value } } ?: Flt64.zero
                val thisCoefficient = coefficient(task) ?: Flt64.infinity
                if (thisThreshold eq Flt64.zero) {
                    cost += thisCoefficient * overMaxAdvanceTime.toLinearPolynomial()
                } else {
                    val slack = thresholdSlack(
                        x = overMaxAdvanceTime,
                        threshold = thisThreshold,
                        type = if (timeWindow.continues) {
                            UContinuous
                        } else {
                            UInteger
                        },
                        name = "over_max_advance_time_threshold_${task}"
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
            when (val result = model.minimize(
                polynomial = cost.toLinearPolynomial(),
                name = "task over max advance time"
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


