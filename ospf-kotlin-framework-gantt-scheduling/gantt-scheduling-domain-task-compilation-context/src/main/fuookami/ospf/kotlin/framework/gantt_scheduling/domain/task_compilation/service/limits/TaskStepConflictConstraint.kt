/** 任务步骤冲突约束 / Task step conflict constraint */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

/**
 * 任务步骤冲突约束 / Task step conflict constraint
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param tasks 任务列表 / List of tasks
 * @param compilation 编译结果 / Compilation result
 * @param shadowPriceExtractor 影子价格提取器 / Shadow price extractor
 * @param name 管道名称 / Pipeline name
 */
class TaskStepConflictConstraint<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    tasks: List<T>,
    private val compilation: Compilation,
    private val shadowPriceExtractor: ((Args) -> Flt64?)? = null,
    override val name: String = "task_step_conflict"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val conflictTaskGroup: List<List<T>> = emptyList()

    override operator fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        if (conflictTaskGroup.isEmpty()) {
            return Failed(ErrorCode.Other, "TaskStepConflictConstraint 暂未实现 conflictTaskGroup 推导逻辑。")
        }
        for ((i, tasks) in conflictTaskGroup.withIndex()) {
            when (val result = model.addConstraint(
                sum(tasks.map { t -> compilation.taskCompilation[t].toLinearPolynomial() }) leq Flt64.one,
                name = "task_step_conflict_$i"
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

    override fun extractor(): AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> {
        throw UnsupportedOperationException(
            "TaskStepConflictConstraint.extractor 暂未实现 shadow price 提取逻辑。"
        )
    }

    override fun refresh(
        shadowPriceMap: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        return Failed(ErrorCode.Other, "TaskStepConflictConstraint.refresh 暂未实现 shadow price 回填逻辑。")
    }
}
