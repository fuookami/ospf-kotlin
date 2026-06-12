/** 任务执行器成本最小化 / Task executor cost minimization */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

/**
 * 任务执行器成本最小化 / Task executor cost minimization
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param tasks 任务列表 / List of tasks
 * @param executors 执行器列表 / List of executors
 * @param compilation 编译结果 / Compilation result
 * @param costCalculator 任务执行器成本计算器 / Task-executor cost calculator
 * @param name 管道名称 / Pipeline name
 */
class TaskExecutorCostMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val tasks: List<T>,
    private val executors: List<E>,
    private val compilation: Compilation,
    private val costCalculator: Extractor<Flt64?, Pair<T, E>> = { Flt64.one },
    override val name: String = "task_executor_cost"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.minimize(
            polynomial = sum(tasks.flatMap { t ->
                executors.map { e ->
                    val coefficient = costCalculator(Pair(t, e)) ?: Flt64.infinity
                    coefficient * compilation.taskAssignment[t, e].toLinearPolynomial()
                }
            }),
            name = "task executor"
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
