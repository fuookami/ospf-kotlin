package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.Compilation
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

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
    private val conflictTaskGroup: List<List<T>> = throw UnsupportedOperationException(
        "TaskStepConflictConstraint 暂未实现 conflictTaskGroup 推导逻辑。"
    )

    override operator fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
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
        map: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        throw UnsupportedOperationException(
            "TaskStepConflictConstraint.refresh 暂未实现 shadow price 回填逻辑。"
        )
    }
}


