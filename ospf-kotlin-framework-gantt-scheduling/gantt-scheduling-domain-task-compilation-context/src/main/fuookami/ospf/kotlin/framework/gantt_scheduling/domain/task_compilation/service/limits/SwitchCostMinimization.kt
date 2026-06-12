/** 切换成本最小化 / Switch cost minimization */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

/**
 * 切换成本最小化 / Switch cost minimization
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param executors 执行器列表 / List of executors
 * @param tasks 任务列表 / List of tasks
 * @param switch 切换对象 / Switch object
 * @param coefficient 切换成本系数提取器 / Extractor for switch cost coefficient
 * @param name 管道名称 / Pipeline name
 */
class SwitchCostMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val executors: List<E>,
    private val tasks: List<T>,
    private val switch: Switch,
    private val coefficient: Extractor<Flt64?, Triple<E, T, T>> = { Flt64.one },
    override val name: String = "switch_cost_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val cost = MutableLinearPolynomial<Flt64>(constant = Flt64.zero)
        for (executor in executors) {
            for (task1 in tasks) {
                for (task2 in tasks) {
                    val thisCoefficient = coefficient(Triple(executor, task1, task2)) ?: Flt64.infinity
                    cost += thisCoefficient * switch.switch[executor, task1, task2].toLinearPolynomial()
                }
            }
        }
        when (val result = model.minimize(
            polynomial = cost.toLinearPolynomial(),
            name = "switch cost"
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
