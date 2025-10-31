package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

data class ExecutorCompilationShadowPriceKey<E : Executor>(
    val executor: E
) : ShadowPriceKey(ExecutorCompilationShadowPriceKey::class)

class ExecutorCompilationConstraint<
    Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    private val executors: List<E>,
    private val compilation: Compilation,
    private val shadowPriceExtractor: ((Args) -> Flt64?)? = null,
    override val name: String = "executor_compilation"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: AbstractLinearMetaModel): Try {
        for (executor in executors) {
            when (val result = model.addConstraint(
                compilation.executorCompilation[executor] eq UInt64.one,
                name = "${name}_$executor",
                args = ExecutorCompilationShadowPriceKey(executor)
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun extractor(): AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> {
        return { map, args ->
            shadowPriceExtractor?.invoke(args) ?: when (args) {
                is TaskGanttSchedulingShadowPriceArguments<*, *> -> {
                    if (args.task == null) {
                        map.map[ExecutorCompilationShadowPriceKey(args.executor)]?.price ?: Flt64.zero
                    } else {
                        Flt64.zero
                    }
                }

                is BunchGanttSchedulingShadowPriceArguments<*, *> -> {
                    if (args.task == null && args.prevTask == null) {
                        map.map[ExecutorCompilationShadowPriceKey(args.executor)]?.price ?: Flt64.zero
                    } else {
                        Flt64.zero
                    }
                }

                else -> {
                    Flt64.zero
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun refresh(
        map: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel,
        shadowPrices: MetaDualSolution
    ): Try {
        for (constraint in model.constraintsOfGroup()) {
            val executor = (constraint.args as? ExecutorCompilationShadowPriceKey<E>)?.executor ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                map.put(ShadowPrice(ExecutorCompilationShadowPriceKey(executor), price))
            }
        }

        return ok
    }
}
