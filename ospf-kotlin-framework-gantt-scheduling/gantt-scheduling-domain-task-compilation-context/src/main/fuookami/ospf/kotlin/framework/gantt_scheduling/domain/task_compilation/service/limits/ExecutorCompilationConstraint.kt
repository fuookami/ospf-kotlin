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
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    private val executors: List<E>,
    private val compilation: Compilation,
    override val name: String = "executor_compilation"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: AbstractLinearMetaModel): Try {
        for (executor in executors) {
            when (val result = model.addConstraint(
                compilation.executorCompilation[executor] eq UInt64.one,
                "${name}_$executor"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    @Suppress("UNCHECKED_CAST")
    override fun extractor(): AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> {
        return { map, args: Args ->
            when (args) {
                is ExecutorGanttSchedulingShadowPriceArguments<*, *> -> {
                    (args.executor as? E)
                        ?.let { map.map[ExecutorCompilationShadowPriceKey(it)]?.price ?: Flt64.zero }
                        ?: Flt64.zero
                }

                is TaskGanttSchedulingShadowPriceArguments<*, *> -> {
                    (args.thisTask?.executor as? E)
                        ?.let { map.map[ExecutorCompilationShadowPriceKey(it)]?.price ?: Flt64.zero }
                        ?: Flt64.zero
                }

                else -> {
                    Flt64.zero
                }
            }
        }
    }

    override fun refresh(
        map: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel,
        shadowPrices: List<Flt64>
    ): Try {
        val indices = model.indicesOfConstraintGroup(name) ?: model.constraints.indices
        val iterator = executors.iterator()
        for (j in indices) {
            if (model.constraints[j].name.startsWith(name)) {
                map.put(ShadowPrice(ExecutorCompilationShadowPriceKey(iterator.next()), shadowPrices[j]))
            }

            if (!iterator.hasNext()) {
                break
            }
        }
        return ok
    }
}
