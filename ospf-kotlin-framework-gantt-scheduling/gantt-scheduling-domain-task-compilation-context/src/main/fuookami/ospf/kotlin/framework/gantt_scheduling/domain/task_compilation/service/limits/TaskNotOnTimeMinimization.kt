/** 任务未准时最小化 / Task not-on-time minimization */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.UInteger
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskTime
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.multiarray._a

/**
 * 任务未准时最小化 / Task not-on-time minimization
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param tasks 任务列表 / List of tasks
 * @param taskTime 任务时间对象 / Task time object
 * @param threshold 阈值数量 / Threshold quantity
 * @param coefficient 成本系数 / Cost coefficient
 * @param name 管道名称 / Pipeline name
 */
class TaskNotOnTimeMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
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
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        if (threshold eq UInt64.zero) {
            when (val result = model.minimize(
                polynomial = coefficient * sum(taskTime.notOnTime[_a].map { it.toLinearPolynomial() }),
                name = "task not on time"
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
                x = sum(taskTime.notOnTime[_a].map { it.toLinearPolynomial() }),
                threshold = threshold.toFlt64(),
                type = UInteger,
                name = "task_not_on_time_threshold"
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
                name = "task not on time"
            )
            ) {
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


