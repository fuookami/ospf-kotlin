package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.core.model.mechanism.eq
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.Compilation
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

data class TaskCompilationShadowPriceKey<
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    val task: AbstractTask<E, A>
) : ShadowPriceKey(TaskCompilationShadowPriceKey::class)

class TaskCompilationConstraint<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val tasks: List<AbstractTask<E, A>>,
    private val compilation: Compilation,
    private val shadowPriceExtractor: ((Args) -> Flt64?)? = null,
    override val name: String = "task_compilation"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override operator fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (task in tasks) {
            when (val result = model.addConstraint(
                compilation.taskCompilation[task] eq 1,
                name = "${name}_${task}",
                args = TaskCompilationShadowPriceKey(task)
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
        return { map, args ->
            shadowPriceExtractor?.invoke(args) ?: when (args) {
                is TaskGanttSchedulingShadowPriceArguments<*, *> -> {
                    args.task?.let { task ->
                        map.map[TaskCompilationShadowPriceKey(task)]?.price ?: Flt64.zero
                    } ?: Flt64.zero
                }

                is BunchGanttSchedulingShadowPriceArguments<*, *> -> {
                    args.task?.let { task ->
                        map.map[TaskCompilationShadowPriceKey(task)]?.price ?: Flt64.zero
                    } ?: Flt64.zero
                }

                else -> {
                    Flt64.zero
                }
            }
        }
    }
    override fun refresh(
        shadowPriceMap: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        for (constraint in model.constraintsOfGroup()) {
            val task = (constraint.args as? TaskCompilationShadowPriceKey<E, A>)?.task ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                shadowPriceMap.put(ShadowPrice(TaskCompilationShadowPriceKey(task), price))
            }
        }

        return ok
    }
}


