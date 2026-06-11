@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 最大完工时间最小化 / Makespan minimization */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.core.variable.UInteger
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingCGPipeline
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingShadowPriceArguments
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.Makespan
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.SolverTimeWindowBoundary
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import kotlin.time.Instant

/**
 * 最大完工时间最小化 / Makespan minimization
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param timeWindow 时间窗口 / Time window
 * @param makespan 最大完工时间对象 / Makespan object
 * @param threshold 阈值时间点 / Threshold time point
 * @param coefficient 成本系数 / Cost coefficient
 * @param name 管道名称 / Pipeline name
 */
class MakespanMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val timeWindow: TimeWindow<*>,
    private val makespan: Makespan<*, E, A>,
    private val threshold: Instant = timeWindow.window.start,
    private val coefficient: Flt64 = Flt64.one,
    override val name: String = "makespan_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    /**
     * 通过 solver 时间窗口边界创建最大完工时间最小化 / Create makespan minimization from a solver time-window boundary
     *
     * @param timeBoundary solver 时间窗口边界 / Solver time-window boundary
     * @param makespan 最大完工时间对象 / Makespan object
     * @param threshold 阈值时间点 / Threshold time point
     * @param coefficient 成本系数 / Cost coefficient
     * @param name 管道名称 / Pipeline name
     */
    constructor(
        timeBoundary: SolverTimeWindowBoundary,
        makespan: Makespan<*, E, A>,
        threshold: Instant = timeBoundary.source.window.start,
        coefficient: Flt64 = Flt64.one,
        name: String = "makespan_minimization"
    ) : this(
        timeWindow = timeBoundary.source,
        makespan = makespan,
        threshold = threshold,
        coefficient = coefficient,
        name = name
    )

    private val timeBoundary = SolverTimeWindowBoundary(timeWindow.toFlt64Boundary())

    override operator fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val thresholdValue = timeBoundary.valueOf(threshold)
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
                type = if (timeBoundary.continues) {
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


