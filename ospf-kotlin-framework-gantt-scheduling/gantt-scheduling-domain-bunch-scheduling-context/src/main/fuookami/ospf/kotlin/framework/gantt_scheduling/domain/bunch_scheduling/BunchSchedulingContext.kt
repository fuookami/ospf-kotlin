package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_scheduling

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_scheduling.service.*

interface BunchSchedulingContext<Args : GanttSchedulingShadowPriceArguments<E, A>, T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> {
    val aggregation : BunchSchedulingAggregation<T, E, A>
    val pipelineList: GanttSchedulingCGPipelineList<Args, E, A>

    val columnAmount get() = UInt64(aggregation.bunches.size - aggregation.removedBunches.size)

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
        newBunches: List<AbstractTaskBunch<T, E, A>>,
        model: LinearMetaModel
    ): Ret<List<AbstractTaskBunch<T, E, A>>> {
        val unduplicatedBunches = when (val result = aggregation.addColumns(iteration, newBunches, model)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }

        return Ok(unduplicatedBunches)
    }

    fun removeColumns(
        maximumReducedCost: Flt64,
        maximumColumnAmount: UInt64,
        reducedCost: (AbstractTaskBunch<T, E, A>) -> Flt64,
        fixedBunches: Set<AbstractTaskBunch<T, E, A>>,
        keptBunches: Set<AbstractTaskBunch<T, E, A>>,
        model: LinearMetaModel
    ): Ret<Flt64> {
        return aggregation.removeColumns(maximumReducedCost, maximumColumnAmount, reducedCost, fixedBunches, keptBunches, model)
    }

    fun extractShadowPrice(shadowPriceMap: AbstractGanttSchedulingShadowPriceMap<Args, E, A>, model: LinearMetaModel, shadowPrices: List<Flt64>): Try {
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

    fun extractFixedBunches(iteration: UInt64, model: LinearMetaModel): Ret<Set<AbstractTaskBunch<T, E, A>>> {
        return aggregation.extractFixedBunches(iteration, model)
    }

    fun extractKeptBunches(iteration: UInt64, model: LinearMetaModel): Ret<Set<AbstractTaskBunch<T, E, A>>> {
        return aggregation.extractKeptBunches(iteration, model)
    }

    fun extractHiddenExecutors(
        executors: List<E>,
        model: LinearMetaModel
    ): Ret<Set<E>> {
        return aggregation.extractHiddenExecutors(executors, model)
    }

    fun selectFreeExecutors(
        fixedBunches: Set<AbstractTaskBunch<T, E, A>>,
        hiddenExecutors: Set<E>,
        model: LinearMetaModel,
    ): Ret<Set<E>>

    fun globallyFix(fixedBunches: Set<AbstractTaskBunch<T, E, A>>): Try {
        return aggregation.globallyFix(fixedBunches)
    }

    fun locallyFix(
        iteration: UInt64,
        bar: Flt64,
        fixedBunches: Set<AbstractTaskBunch<T, E, A>>,
        model: LinearMetaModel
    ): Ret<Set<AbstractTaskBunch<T, E, A>>> {
        return aggregation.locallyFix(iteration, bar, fixedBunches, model)
    }

    fun logResult(iteration: UInt64, model: LinearMetaModel): Try {
        return aggregation.logResult(iteration, model)
    }

    fun logBunchCost(iteration: UInt64, model: LinearMetaModel): Try {
        return aggregation.logBunchCost(iteration, model)
    }

    fun flush(iteration: UInt64): Try {
        return aggregation.flush(iteration, emptyList())
    }

    fun analyzeTaskSolution(
        iteration: UInt64,
        tasks: List<T>,
        model: LinearMetaModel
    ): Ret<Solution<T, E, A>> {
        val analyzer = TaskSolutionAnalyzer<T, E, A>()
        return analyzer(iteration, tasks, aggregation.bunchesIteration, aggregation.compilation, model)
    }

    fun analyzeBunchSolution(
        iteration: UInt64,
        tasks: List<T>,
        model: LinearMetaModel
    ): Ret<BunchSolution<T, E, A>> {
        val analyzer = BunchSolutionAnalyzer<T, E, A>()
        return analyzer(iteration, tasks, aggregation.bunchesIteration, aggregation.compilation, model)
    }
}

interface ExtractBunchSchedulingContext<Args : GanttSchedulingShadowPriceArguments<E, A>, T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> {
    val baseContext: BunchSchedulingContext<Args, T, E, A>

    fun register(model: LinearMetaModel): Try
    fun addColumns(iteration: UInt64, newBunches: List<AbstractTaskBunch<T, E, A>>, model: LinearMetaModel): Try
    fun extractShadowPrice(shadowPriceMap: AbstractGanttSchedulingShadowPriceMap<Args, E, A>, model: LinearMetaModel, shadowPrices: List<Flt64>): Try

    fun logResult(iteration: UInt64, model: LinearMetaModel): Try {
        return Ok(success)
    }
}
