package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*

data class ExecutorCompilationShadowPriceKey<E : Executor>(
    val executor: E
) : ShadowPriceKey(ExecutorCompilationShadowPriceKey::class)

class ExecutorCompilationConstraint<Args : GanttSchedulingShadowPriceArguments<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    private val executors: List<E>,
    private val compilation: Compilation,
    override val name: String = "executor_compilation"
) : GanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: LinearMetaModel): Try {
        for (executor in executors) {
            model.addConstraint(
                compilation.executorCompilation[executor] eq UInt64.one,
                "${name}_$executor"
            )
        }

        return Ok(success)
    }

    override fun extractor(): ShadowPriceExtractor<Args, AbstractGanttSchedulingShadowPriceMap<Args, E, A>> {
        return { map, args: Args ->
            map.map[ExecutorCompilationShadowPriceKey(args.executor)]?.price ?: Flt64.zero
        }
    }

    override fun refresh(
        map: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: LinearMetaModel,
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
        return Ok(success)
    }
}
