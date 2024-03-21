package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.service.*

interface IterativeTaskSchedulingContext<
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
> {
    val aggregation: IterativeTaskSchedulingAggregation<E, A>
    val pipelineList: AbstractGanttSchedulingCGPipelineList<Args, E, A>

    val columnAmount get() = UInt64(aggregation.tasks.size - aggregation.removedTasks.size)

    fun register(model: LinearMetaModel): Try {
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

        return Ok(success)
    }

    suspend fun addColumns(
        iteration: UInt64,
        newTasks: List<AbstractTask<E, A>>,
        model: LinearMetaModel
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
        reducedCost: (AbstractTask<E, A>) -> Flt64,
        fixedTasks: Set<AbstractTask<E, A>>,
        keptTasks: Set<AbstractTask<E, A>>,
        model: LinearMetaModel
    ): Ret<Flt64> {
        return aggregation.removedColumns(
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
        model: LinearMetaModel,
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
        return Ok(success)
    }

    fun extractFixedTasks(iteration: UInt64, model: LinearMetaModel): Ret<Map<AbstractTask<E, A>, UInt64>> {
        return aggregation.extractFixedTasks(iteration, model)
    }

    fun extractKeptTasks(iteration: UInt64, model: LinearMetaModel): Ret<Map<AbstractTask<E, A>, UInt64>> {
        return aggregation.extractKeptTasks(iteration, model)
    }

    fun extractHiddenExecutors(
        executors: List<E>,
        model: LinearMetaModel
    ): Ret<Set<E>> {
        return aggregation.extractHiddenExecutors(executors, model)
    }

    fun selectFreeExecutors(
        fixedTasks: Set<AbstractTask<E, A>>,
        hiddenExecutors: Set<E>,
        model: LinearMetaModel,
    ): Ret<Set<E>>

    fun globallyFix(fixedTasks: Map<AbstractTask<E, A>, UInt64>): Try {
        return aggregation.globallyFix(fixedTasks)
    }

    fun locallyFix(
        iteration: UInt64,
        bar: Flt64,
        fixedTasks: Map<AbstractTask<E, A>, UInt64>,
        model: LinearMetaModel
    ): Ret<Map<AbstractTask<E, A>, UInt64>> {
        return aggregation.locallyFix(iteration, bar, fixedTasks, model)
    }

    fun logResult(iteration: UInt64, model: LinearMetaModel): Try {
        return aggregation.logResult(iteration, model)
    }

    fun logBunchCost(iteration: UInt64, model: LinearMetaModel): Try {
        return aggregation.logTaskCost(iteration, model)
    }

    fun flush(iteration: UInt64): Try {
        return aggregation.flush(iteration, emptyList())
    }

    fun analyzeTaskSolution(
        iteration: UInt64,
        tasks: List<AbstractTask<E, A>>,
        model: LinearMetaModel
    ): Ret<Solution<E, A>> {
        val analyzer = SolutionAnalyzer<E, A>()
        return analyzer(iteration, tasks, aggregation.tasksIteration, aggregation.compilation, model)
    }
}

interface ExtractIterativeTaskSchedulingContext<
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
> {
    val baseContext: IterativeTaskSchedulingContext<Args, E, A>

    fun register(model: LinearMetaModel): Try

    fun addColumns(
        iteration: UInt64,
        newTasks: List<AbstractTask<E, A>>,
        model: LinearMetaModel
    ): Try

    fun extractShadowPrice(
        shadowPriceMap: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: LinearMetaModel,
        shadowPrices: List<Flt64>
    ): Try

    fun logResult(iteration: UInt64, model: LinearMetaModel): Try {
        return Ok(success)
    }
}
