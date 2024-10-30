package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.Solution

interface BunchCompilationContext<
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    B : AbstractTaskBunch<T, E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
> {
    val aggregation: BunchCompilationAggregation<B, T, E, A>
    val pipelineList: AbstractGanttSchedulingCGPipelineList<Args, E, A>

    val columnAmount get() = UInt64(aggregation.bunches.size - aggregation.removedBunches.size)

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
        newBunches: List<B>,
        model: AbstractLinearMetaModel
    ): Ret<List<B>> {
        val unduplicatedBunches = when (val result = aggregation.addColumns(
            iteration = iteration,
            newBunches = newBunches,
            model = model
        )) {
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
        reducedCost: (B) -> Flt64,
        fixedBunches: Set<B>,
        keptBunches: Set<B>,
        model: AbstractLinearMetaModel
    ): Ret<Flt64> {
        return aggregation.removeColumns(
            maximumReducedCost,
            maximumColumnAmount,
            reducedCost,
            fixedBunches,
            keptBunches,
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

    fun extractFixedBunches(iteration: UInt64, model: AbstractLinearMetaModel): Ret<Set<B>> {
        return aggregation.extractFixedBunches(iteration, model)
    }

    fun extractKeptBunches(iteration: UInt64, model: AbstractLinearMetaModel): Ret<Set<B>> {
        return aggregation.extractKeptBunches(iteration, model)
    }

    fun extractHiddenExecutors(
        executors: List<E>,
        model: AbstractLinearMetaModel
    ): Ret<Set<E>> {
        return aggregation.extractHiddenExecutors(executors, model)
    }

    fun <Map : AbstractGanttSchedulingShadowPriceMap<Args, E, A>> selectFreeExecutors(
        fixedBunches: Set<B>,
        hiddenExecutors: Set<E>,
        shadowPriceMap: Map,
        model: AbstractLinearMetaModel,
    ): Ret<Set<E>>

    fun globallyFix(fixedBunches: Set<B>): Try {
        return aggregation.globallyFix(fixedBunches)
    }

    fun locallyFix(
        iteration: UInt64,
        bar: Flt64,
        fixedBunches: Set<B>,
        model: AbstractLinearMetaModel
    ): Ret<Set<B>> {
        return aggregation.locallyFix(iteration, bar, fixedBunches, model)
    }

    fun logResult(iteration: UInt64, model: AbstractLinearMetaModel): Try {
        return aggregation.logResult(iteration, model)
    }

    fun logBunchCost(iteration: UInt64, model: AbstractLinearMetaModel): Try {
        return aggregation.logBunchCost(iteration, model)
    }

    fun flush(iteration: UInt64): Try {
        return aggregation.flush(iteration, emptyList())
    }

    fun analyzeTaskSolution(
        iteration: UInt64,
        tasks: List<T>,
        model: AbstractLinearMetaModel
    ): Ret<Solution<T, E, A>> {
        return TaskSolutionAnalyzer(iteration, tasks, aggregation.bunchesIteration, aggregation.compilation, model)
    }

    fun analyzeBunchSolution(
        iteration: UInt64,
        tasks: List<T>,
        model: AbstractLinearMetaModel
    ): Ret<BunchSolution<B, T, E, A>> {
        return BunchSolutionAnalyzer(iteration, tasks, aggregation.bunchesIteration, aggregation.compilation, model)
    }
}

interface ExtractBunchCompilationContext<
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    B : AbstractTaskBunch<T, E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
> {
    val baseContext: BunchCompilationContext<Args, B, T, E, A>

    fun register(model: MetaModel): Try

    fun addColumns(
        iteration: UInt64,
        newBunches: List<B>,
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
