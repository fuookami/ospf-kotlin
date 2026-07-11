/** 任务成本最小化 / Task cost minimization */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

/**
 * 任务成本最小化 / Task cost minimization
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param compilation 迭代任务编译结果 / Iterative task compilation result
 * @param name 管道名称 / Pipeline name
*/
class TaskCostMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val compilation: IterativeTaskCompilation<*, *, E, A>,
    override val name: String = "task_cost_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.minimize(
            symbol = compilation.taskCost,
            name = "task cost"
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
