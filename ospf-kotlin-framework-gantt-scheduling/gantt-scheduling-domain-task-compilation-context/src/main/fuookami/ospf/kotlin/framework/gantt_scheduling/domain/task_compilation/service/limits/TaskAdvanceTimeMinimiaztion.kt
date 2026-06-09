@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 任务提前时间最小化 / Task advance time minimization */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.core.variable.UInteger
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskTime
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.SolverTimeWindowBoundary
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import kotlin.time.Duration

/**
 * 任务提前时间最小化 / Task advance time minimization
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
class TaskAdvanceTimeMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val timeWindow: TimeWindow<Flt64>,
    tasks: List<T>,
    private val taskTime: TaskTime,
    private val threshold: Extractor<Duration?, T> = { Duration.ZERO },
    private val coefficient: Extractor<Flt64?, T> = { Flt64.one },
    override val name: String = "task_advance_time_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    /**
     * 通过 solver 时间窗口边界创建任务提前时间最小化 / Create task advance time minimization from a solver time-window boundary
     *
     * @param timeBoundary solver 时间窗口边界 / Solver time-window boundary
     * @param tasks 任务列表 / List of tasks
     * @param taskTime 任务时间对象 / Task time object
     * @param threshold 阈值提取器 / Threshold extractor
     * @param coefficient 成本系数提取器 / Extractor for cost coefficient
     * @param name 管道名称 / Pipeline name
     */
    constructor(
        timeBoundary: SolverTimeWindowBoundary,
        tasks: List<T>,
        taskTime: TaskTime,
        threshold: Extractor<Duration?, T> = { Duration.ZERO },
        coefficient: Extractor<Flt64?, T> = { Flt64.one },
        name: String = "task_advance_time_minimization"
    ) : this(
        timeWindow = timeBoundary.source,
        tasks = tasks,
        taskTime = taskTime,
        threshold = threshold,
        coefficient = coefficient,
        name = name
    )

    private val timeBoundary = SolverTimeWindowBoundary(timeWindow)

    private val tasks = if (taskTime.advanceEnabled) {
        tasks.filter { it.advanceEnabled && it.scheduledTime != null }
    } else {
        emptyList()
    }

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        if (taskTime.advanceEnabled) {
            val cost = MutableLinearPolynomial<Flt64>(constant = Flt64.zero)
            for (task in tasks) {
                val advanceTime = taskTime.advanceTime[task]
                val thisThreshold = threshold(task)?.let { timeBoundary.valueOf(it) } ?: Flt64.zero
                val thisCoefficient = coefficient(task) ?: Flt64.infinity
                if (thisThreshold eq Flt64.zero) {
                    cost += thisCoefficient * advanceTime.toLinearPolynomial()
                } else {
                    val slack = thresholdSlack(
                        x = advanceTime,
                        threshold = thisThreshold,
                        type = if (timeBoundary.continues) {
                            UContinuous
                        } else {
                            UInteger
                        },
                        name = "advance_time_threshold_${task}"
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
                name = "task advance time"
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


