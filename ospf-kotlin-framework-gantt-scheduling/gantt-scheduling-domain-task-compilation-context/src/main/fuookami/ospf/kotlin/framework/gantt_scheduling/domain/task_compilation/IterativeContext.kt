package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.Solution
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.*

interface IterativeTaskCompilationContext<
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    IT : IterativeAbstractTask<E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
> {
    val aggregation: IterativeTaskCompilationAggregation<IT, T, E, A>
    val pipelineList: AbstractGanttSchedulingCGPipelineList<Args, E, A>

    val columnAmount get() = UInt64(aggregation.tasks.size - aggregation.removedTasks.size)

    fun register(model: AbstractLinearMetaModel): Try {
        when (val result = aggregation.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = pipelineList(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    suspend fun addColumns(
        iteration: UInt64,
        newTasks: List<IT>,
        model: AbstractLinearMetaModel
    ): Ret<List<AbstractTask<E, A>>> {
        val unduplicatedTasks = when (val result = aggregation.addColumns(
            iteration = iteration,
            newTasks = newTasks,
            model = model
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }

        return Ok(unduplicatedTasks)
    }

    fun removeColumns(
        maximumReducedCost: Flt64,
        maximumColumnAmount: UInt64,
        reducedCost: (IT) -> Flt64,
        fixedTasks: Set<IT>,
        keptTasks: Set<IT>,
        model: AbstractLinearMetaModel
    ): Ret<Flt64> {
        return aggregation.removeColumns(
            maximumReducedCost,
            maximumColumnAmount,
            reducedCost,
            fixedTasks,
            keptTasks,
            model
        )
    }

    fun extractShadowPrice(
        shadowPriceMap: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel,
        shadowPrices: List<Flt64>
    ): Try {
        for (pipeline in pipelineList) {
            when (val ret = pipeline.refresh(shadowPriceMap, model, shadowPrices)) {
                is Ok -> {}
                is Failed -> {
                    return Failed(ret.error)
                }
            }
            val extractor = pipeline.extractor() ?: continue
            shadowPriceMap.put(extractor)
        }
        return ok
    }

    fun extractFixedTasks(iteration: UInt64, model: AbstractLinearMetaModel): Ret<Set<IT>> {
        return aggregation.extractFixedTasks(iteration, model)
    }

    fun extractKeptTasks(iteration: UInt64, model: AbstractLinearMetaModel): Ret<Set<IT>> {
        return aggregation.extractKeptTasks(iteration, model)
    }

    fun extractHiddenExecutors(
        executors: List<E>,
        model: AbstractLinearMetaModel
    ): Ret<Set<E>> {
        return aggregation.extractHiddenExecutors(executors, model)
    }

    fun <Map : AbstractGanttSchedulingShadowPriceMap<Args, E, A>> selectFreeExecutors(
        fixedTasks: Set<IT>,
        hiddenExecutors: Set<E>,
        shadowPriceMap: Map,
        model: AbstractLinearMetaModel,
    ): Ret<Set<E>>

    fun globallyFix(fixedTasks: Set<IT>): Try {
        return aggregation.globallyFix(fixedTasks)
    }

    fun locallyFix(
        iteration: UInt64,
        bar: Flt64,
        fixedTasks: Set<IT>,
        model: AbstractLinearMetaModel
    ): Ret<Set<IT>> {
        return aggregation.locallyFix(iteration, bar, fixedTasks, model)
    }

    fun logResult(iteration: UInt64, model: AbstractLinearMetaModel): Try {
        return aggregation.logResult(iteration, model)
    }

    fun logTaskCost(iteration: UInt64, model: AbstractLinearMetaModel): Try {
        return aggregation.logTaskCost(iteration, model)
    }

    fun flush(iteration: UInt64): Try {
        return aggregation.flush(iteration, emptyList())
    }

    fun analyzeSolution(
        iteration: UInt64,
        tasks: List<T>,
        model: AbstractLinearMetaModel
    ): Ret<Solution<T, E, A>> {
        return SolutionAnalyzer(iteration, tasks, aggregation.tasksIteration, aggregation.compilation, model)
    }
}

interface ExtractIterativeTaskCompilationContext<
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    IT : IterativeAbstractTask<E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
> {
    val baseContext: IterativeTaskCompilationContext<Args, IT, T, E, A>

    fun register(model: MetaModel): Try

    fun addColumns(
        iteration: UInt64,
        newTasks: List<T>,
        model: AbstractLinearMetaModel
    ): Try

    fun extractShadowPrice(
        shadowPriceMap: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel,
        shadowPrices: List<Flt64>
    ): Try

    fun logResult(iteration: UInt64, model: AbstractLinearMetaModel): Try {
        return ok
    }
}
