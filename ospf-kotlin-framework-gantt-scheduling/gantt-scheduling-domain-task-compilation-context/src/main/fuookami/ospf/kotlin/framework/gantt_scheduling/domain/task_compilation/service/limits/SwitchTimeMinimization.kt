@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 切换时间最小化 / Switch time minimization */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.core.variable.UInteger
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.Switch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.SolverTimeWindowBoundary
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import kotlin.time.Duration

/**
 * 切换时间最小化 / Switch time minimization
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param timeWindow 时间窗口 / Time window
 * @param tasks 任务列表 / List of tasks
 * @param switch 切换对象 / Switch object
 * @param threshold 阈值提取器 / Threshold extractor
 * @param coefficient 切换时间系数提取器 / Extractor for switch time coefficient
 * @param name 管道名称 / Pipeline name
 */
class SwitchTimeMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val timeWindow: TimeWindow<*>,
    private val tasks: List<T>,
    private val switch: Switch,
    private val threshold: Extractor<Duration?, Pair<T, T>> = { Duration.ZERO },
    private val coefficient: Extractor<Flt64?, Pair<T, T>> = { Flt64.one },
    override val name: String = "switch_time_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    /**
     * 通过 solver 时间窗口边界创建切换时间最小化 / Create switch time minimization from a solver time-window boundary
     *
     * @param timeBoundary solver 时间窗口边界 / Solver time-window boundary
     * @param tasks 任务列表 / List of tasks
     * @param switch 切换对象 / Switch object
     * @param threshold 阈值提取器 / Threshold extractor
     * @param coefficient 切换时间系数提取器 / Extractor for switch time coefficient
     * @param name 管道名称 / Pipeline name
     */
    constructor(
        timeBoundary: SolverTimeWindowBoundary,
        tasks: List<T>,
        switch: Switch,
        threshold: Extractor<Duration?, Pair<T, T>> = { Duration.ZERO },
        coefficient: Extractor<Flt64?, Pair<T, T>> = { Flt64.one },
        name: String = "switch_time_minimization"
    ) : this(
        timeWindow = timeBoundary.source,
        tasks = tasks,
        switch = switch,
        threshold = threshold,
        coefficient = coefficient,
        name = name
    )

    private val timeBoundary = SolverTimeWindowBoundary(timeWindow.toFlt64Boundary())

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val cost = MutableLinearPolynomial<Flt64>(constant = Flt64.zero)
        for (task1 in tasks) {
            for (task2 in tasks) {
                val switchTime = switch.switchTime[task1, task2]
                val thisThreshold = threshold(Pair(task1, task2))?.let { timeBoundary.valueOf(it) } ?: Flt64.zero
                val thisCoefficient = coefficient(Pair(task1, task2)) ?: Flt64.infinity
                if (thisThreshold eq Flt64.zero) {
                    cost += thisCoefficient * switchTime.toLinearPolynomial()
                } else {
                    val slack = thresholdSlack(
                        x = switchTime,
                        threshold = thisThreshold,
                        type = if (timeBoundary.continues) {
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
