package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_selection.service

import kotlin.time.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.framework.solver.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_selection.model.*

class BranchAndPriceAlgorithm<
    Map : AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    B : AbstractTaskBunch<T, E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    private val executors: List<E>,
    private val tasks: List<T>,
    private val initialBunches: List<B>,
    private val solver: ColumnGenerationSolver,
    private val policy: Policy<Map, Args, B, T, E, A>,
    private val configuration: Configuration
) {
    data class Policy<
        Map : AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        Args : GanttSchedulingShadowPriceArguments<E, A>,
        B : AbstractTaskBunch<T, E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
    >(
        val contextBuilder: () -> BunchCompilationContext<Args, B, T, E, A>,
        val extractContextBuilder: List<(BunchCompilationContext<Args, B, T, E, A>) -> List<ExtractBunchCompilationContext<Args, B, T, E, A>>>,
        val shadowPriceMap: () -> Map,
        val reducedCost: (Map, AbstractTaskBunch<T, E, A>) -> Flt64,
        val bunchGenerator: suspend (UInt64, List<E>, Map) -> Ret<List<B>>,
    )

    data class Configuration(
        val badReducedAmount: UInt64 = UInt64(20UL),
        val maximumColumnAmount: UInt64 = UInt64(50000UL),
        val minimumColumnAmountPerExecutor: UInt64 = UInt64.zero,
        val timeLimit: Duration = 30000.seconds,
    )

    private val logger = logger()

    private val context = policy.contextBuilder()
    private val extractContexts = policy.extractContextBuilder.flatMap { it(context) }
    private lateinit var shadowPriceMap: Map

    private val fixedBunches = HashSet<B>()
    private val keptBunches = HashSet<B>()
    private val hiddenExecutors = HashSet<E>()

    private var mainProblemSolvingTimes: UInt64 = UInt64.zero
    private var mainProblemSolvingTime: Duration = Duration.ZERO
    private var mainProblemModelingTime: Duration = Duration.ZERO
    private var subProblemSolvingTimes: UInt64 = UInt64.zero
    private var subProblemSolvingTime: Duration = Duration.ZERO

    private val columnAmount: UInt64 get() = context.columnAmount
    private val executorAmount: UInt64 get() = UInt64(executors.size)

    private fun notFixedExtractorAmount(fixedBunches: Set<B>): UInt64 =
        executorAmount - UInt64(fixedBunches.size.toULong())

    private fun minimumColumnAmount(
        fixedBunches: Set<B>,
        configuration: Configuration
    ): UInt64 =
        notFixedExtractorAmount(fixedBunches) * configuration.minimumColumnAmountPerExecutor

    suspend operator fun invoke(id: String): Ret<BunchSolution<B, T, E, A>> {
        var maximumReducedCost1 = Flt64(50.0)
        var maximumReducedCost2 = Flt64(3000.0)

        val beginTime = Clock.System.now()
        try {
            lateinit var bestSolution: BunchSolution<B, T, E, A>
            val model = LinearMetaModel(id)
            var iteration = Iteration<T, E, A>()
            when (val result = register(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            // solve ip with initial column
            val ipRet = when (val result = solver.solveMILP("${id}_$iteration", model)) {
                is Ok -> {
                    model.setSolution(result.value.solution)
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }
            }
            logIpResults(iteration.iteration, model)

            bestSolution = when (val result = analyzeSolution(iteration.iteration, model)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }
            }
            refresh(ipRet)
            iteration.refreshIpObj(ipRet.obj)

            if (ipRet.obj eq Flt64.zero) {
                return Ok(bestSolution)
            }

            when (fixBunch(iteration.iteration, model)) {
                is Ok -> {}

                is Failed -> {
                    return Ok(bestSolution)
                }
            }
            when (keepBunch(iteration.iteration, model)) {
                is Ok -> {}

                is Failed -> {
                    return Ok(bestSolution)
                }
            }

            var mainIteration = UInt64.one
            while (!iteration.isImprovementSlow
                && iteration.runTime < configuration.timeLimit
            ) {
                logger.debug { "Iteration $mainIteration begin!" }

                shadowPriceMap = when (val result = solveRMP(id, iteration, model, true)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        return Ok(bestSolution)
                    }
                }
                logLpResults(iteration.iteration, model)

                when (hideExecutors(model)) {
                    is Ok -> {}

                    is Failed -> {
                        return Ok(bestSolution)
                    }
                }

                logger.debug { "Global column generation of iteration $mainIteration begin!" }

                // globally column generation
                // it runs only 1 time
                for (count in 0..<1) {
                    ++iteration
                    val newBunches = when (val result = solveSP(id, iteration, executors, shadowPriceMap)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            return Ok(bestSolution)
                        }
                    }
                    if (newBunches.isEmpty()) {
                        logger.debug { "There is no bunch generated in global column generation of iteration $mainIteration." }
                        if (iteration.optimalRate eq Flt64.one) {
                            return Ok(bestSolution)
                        }
                    }
                    val newBunchAmount = UInt64(newBunches.size.toULong())

                    when (addColumns(iteration.iteration, newBunches, model)) {
                        is Ok -> {}

                        is Failed -> {
                            return Ok(bestSolution)
                        }
                    }

                    shadowPriceMap = when (val result = solveRMP(id, iteration, model, true)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            return Ok(bestSolution)
                        }
                    }
                    logLpResults(iteration.iteration, model)

                    val reducedAmount =
                        UInt64(fixedBunches.count { policy.reducedCost(shadowPriceMap, it) gr Flt64.zero })
                    if (columnAmount > configuration.maximumColumnAmount) {
                        maximumReducedCost1 = when (val result = removeColumns(
                            maximumReducedCost1,
                            configuration.maximumColumnAmount,
                            shadowPriceMap,
                            fixedBunches,
                            keptBunches,
                            model
                        )) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                return Ok(bestSolution)
                            }
                        }
                    }
                    if (reducedAmount >= configuration.badReducedAmount
                        || newBunchAmount <= minimumColumnAmount(fixedBunches, configuration)
                    ) {
                        break
                    }
                }
                maximumReducedCost1 = Flt64(50.0)

                logger.debug { "Global column generation of iteration $mainIteration end!" }

                val freeExecutors = when (val result = selectFreeExecutors(model)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        return Ok(bestSolution)
                    }
                }
                val fixedBunches = when (val result = globallyFix(freeExecutors)) {
                    is Ok -> {
                        result.value.toHashSet()
                    }

                    is Failed -> {
                        return Ok(bestSolution)
                    }
                }
                val freeExecutorList = freeExecutors.toMutableList()

                logger.debug { "Local column generation of iteration $mainIteration begin!" }

                // locally column generation
                while (true) {
                    shadowPriceMap = when (val result = solveRMP(id, iteration, model, false)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            return Ok(bestSolution)
                        }
                    }
                    logLpResults(iteration.iteration, model)

                    ++iteration
                    val newBunches = when (val result = solveSP(id, iteration, freeExecutorList, shadowPriceMap)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            return Ok(bestSolution)
                        }
                    }
                    if (newBunches.isEmpty()) {
                        --iteration
                        logger.debug { "There is no bunch generated in local column generation of iteration $mainIteration: $iteration." }
                        break
                    }
                    val newBunchAmount = UInt64(newBunches.size.toULong())

                    when (addColumns(iteration.iteration, newBunches, model)) {
                        is Ok -> {}

                        is Failed -> {
                            return Ok(bestSolution)
                        }
                    }
                    val newFixedBunches = when (val result = locallyFix(iteration.iteration, fixedBunches, model)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            return Ok(bestSolution)
                        }
                    }
                    if (newFixedBunches.isNotEmpty()) {
                        for (bunch in newFixedBunches) {
                            freeExecutorList.remove(bunch.executor)
                        }
                        fixedBunches.addAll(newFixedBunches)
                    } else {
                        break
                    }

                    if (columnAmount > configuration.maximumColumnAmount
                        && newBunchAmount > minimumColumnAmount(fixedBunches, configuration)
                    ) {
                        maximumReducedCost2 = when (val result = removeColumns(
                            maximumReducedCost2,
                            configuration.maximumColumnAmount,
                            shadowPriceMap,
                            fixedBunches,
                            keptBunches,
                            model
                        )) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                return Ok(bestSolution)
                            }
                        }
                    }
                }

                logger.debug { "Local column generation of iteration $mainIteration end!" }

                this.fixedBunches.clear()
                this.fixedBunches.addAll(fixedBunches)
                val thisIpRet = when (val result = solver.solveMILP("${id}_${iteration}_ip", model)) {
                    is Ok -> {
                        model.setSolution(result.value.solution)
                        result.value
                    }

                    is Failed -> {
                        return Ok(bestSolution)
                    }
                }
                refresh(thisIpRet)
                logIpResults(iteration.iteration, model)
                if (iteration.refreshIpObj(thisIpRet.obj)) {
                    when (val result = analyzeSolution(iteration.iteration, model)) {
                        is Ok -> {
                            bestSolution = result.value
                            if (thisIpRet.obj eq Flt64.zero) {
                                return Ok(bestSolution)
                            }
                        }

                        is Failed -> {
                            return Ok(bestSolution)
                        }
                    }
                }
                heartBeat(id, iteration.optimalRate)

                flush(iteration.iteration)
                iteration.halveStep()

                logger.debug {
                    "Iteration $mainIteration end, optimal rate: ${
                        String.format(
                            "%.2f",
                            (iteration.optimalRate * Flt64(100.0)).toDouble()
                        )
                    }%"
                }
                ++mainIteration
            }

            return Ok(bestSolution)
        } catch (e: Exception) {
            print(e.stackTraceToString())
            return Failed(Err(ErrorCode.ApplicationException, e.message))
        }
    }

    private fun heartBeat(id: String, optimalRate: Flt64) {
        logger.info {
            "Heart beat, current optimal rate: ${
                String.format(
                    "%.2f",
                    (optimalRate * Flt64(100.0)).toDouble()
                )
            }%"
        }
    }

    private suspend fun register(model: AbstractLinearMetaModel): Try {
        when (val result = context.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        for (extractContext in extractContexts) {
            when (val result = extractContext.register(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        val unduplicatedBunches = when (val result = context.addColumns(UInt64.zero, initialBunches, model)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }

        for (extractContext in extractContexts) {
            when (val result = extractContext.addColumns(UInt64.zero, unduplicatedBunches, model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    private suspend fun solveRMP(
        id: String,
        iteration: Iteration<T, E, A>,
        model: LinearMetaModel,
        withKeeping: Boolean
    ): Ret<Map> {
        val lpRet = when (val result = solver.solveLP("${id}_${iteration}_lp", model)) {
            is Ok -> {
                model.setSolution(result.value.solution)
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }

        refresh(lpRet)
        if (iteration.refreshLpObj(lpRet.result.obj) && withKeeping) {
            when (val ret = keepBunch(iteration.iteration, model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(ret.error)
                }
            }
        }

        val shadowPriceMap = when (val ret = extractShadowPrice(model, lpRet.dualSolution)) {
            is Ok -> {
                ret.value
            }

            is Failed -> {
                return Failed(ret.error)
            }
        }

        return Ok(shadowPriceMap)
    }

    private suspend fun solveSP(
        id: String,
        iteration: Iteration<T, E, A>,
        executors: List<E>,
        shadowPriceMap: Map
    ): Ret<List<B>> {
        val beginTime = Clock.System.now()
        val newBunches = when (val results = policy.bunchGenerator(iteration.iteration, executors, shadowPriceMap)) {
            is Ok -> {
                results.value
            }

            is Failed -> {
                return Failed(results.error)
            }
        }
        subProblemSolvingTimes += UInt64.one
        subProblemSolvingTime = Clock.System.now() - beginTime
        iteration.refreshLowerBound(newBunches) { policy.reducedCost(shadowPriceMap, it) }
        heartBeat(id, iteration.optimalRate)
        return Ok(newBunches)
    }

    private fun extractShadowPrice(
        model: AbstractLinearMetaModel,
        shadowPrices: List<Flt64>
    ): Ret<Map> {
        val map = policy.shadowPriceMap()

        when (val result = context.extractShadowPrice(map, model, shadowPrices)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        for (extractContext in extractContexts) {
            when (val result = extractContext.extractShadowPrice(map, model, shadowPrices)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return Ok(map)
    }

    private suspend fun addColumns(
        iteration: UInt64,
        newBunches: List<B>,
        model: AbstractLinearMetaModel
    ): Try {
        val beginTime = Clock.System.now()

        val unduplicatedBunches = when (val result = context.addColumns(iteration, newBunches, model)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (unduplicatedBunches.isNotEmpty()) {
            model.flush()
        }

        for (extractContext in extractContexts) {
            when (val result = extractContext.addColumns(iteration, unduplicatedBunches, model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        mainProblemModelingTime += Clock.System.now() - beginTime
        return ok
    }

    private fun removeColumns(
        maximumReducedCost: Flt64,
        maximumColumnAmount: UInt64,
        shadowPriceMap: Map,
        fixedBunches: Set<B>,
        keptBunches: Set<B>,
        model: AbstractLinearMetaModel
    ): Ret<Flt64> {
        val newMaximumReducedCost = when (val result = context.removeColumns(
            maximumReducedCost,
            maximumColumnAmount,
            { bunch: AbstractTaskBunch<T, E, A> -> policy.reducedCost(shadowPriceMap, bunch) },
            fixedBunches,
            keptBunches,
            model
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }

        return Ok(newMaximumReducedCost)
    }

    private fun fixBunch(
        iteration: UInt64,
        model: AbstractLinearMetaModel
    ): Try {
        return when (val result = context.extractFixedBunches(iteration, model)) {
            is Ok -> {
                fixedBunches.addAll(result.value)
                ok
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }

    private fun keepBunch(
        iteration: UInt64,
        model: AbstractLinearMetaModel
    ): Try {
        return when (val result = context.extractKeptBunches(iteration, model)) {
            is Ok -> {
                keptBunches.addAll(result.value)
                ok
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }

    private fun hideExecutors(model: AbstractLinearMetaModel): Try {
        return when (val result = context.extractHiddenExecutors(executors, model)) {
            is Ok -> {
                hiddenExecutors.addAll(result.value)
                ok
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }

    private fun selectFreeExecutors(
        model: AbstractLinearMetaModel
    ): Ret<Set<E>> {
        return when (val result = context.selectFreeExecutors(
            fixedBunches,
            hiddenExecutors,
            shadowPriceMap,
            model
        )) {
            is Ok -> {
                Ok(result.value)
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }

    private fun refresh(lpResult: ColumnGenerationSolver.LPResult) {
        mainProblemSolvingTimes += UInt64.one
        mainProblemSolvingTime += lpResult.result.time
    }

    private fun refresh(ipResult: SolverOutput) {
        mainProblemSolvingTimes += UInt64.one
        mainProblemSolvingTime += ipResult.time
    }

    private fun globallyFix(
        freeExecutors: Set<Executor>
    ): Ret<Set<B>> {
        val fixedBunches = HashSet<B>()
        for (bunch in this.fixedBunches) {
            if (!freeExecutors.contains(bunch.executor)) {
                fixedBunches.add(bunch)
            }
        }

        when (val result = context.globallyFix(fixedBunches)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
        return Ok(fixedBunches)
    }

    private fun locallyFix(
        iteration: UInt64,
        fixedBunches: Set<B>,
        model: AbstractLinearMetaModel
    ): Ret<Set<B>> {
        val fixBar = Flt64(0.9)
        return when (val ret = context.locallyFix(iteration, fixBar, fixedBunches, model)) {
            is Ok -> {
                Ok(ret.value)
            }

            is Failed -> {
                Failed(ret.error)
            }
        }
    }

    private fun flush(iteration: UInt64): Try {
        when (val ret = context.flush(iteration)) {
            is Ok -> {}

            is Failed -> {
                return Failed(ret.error)
            }
        }

        keptBunches.clear()
        hiddenExecutors.clear()
        return ok
    }

    private fun analyzeSolution(
        iteration: UInt64,
        model: AbstractLinearMetaModel
    ): Ret<BunchSolution<B, T, E, A>> {
        return when (val result = context.analyzeBunchSolution(iteration, tasks, model)) {
            is Ok -> {
                Ok(result.value)
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }

    private fun logLpResults(iteration: UInt64, model: AbstractLinearMetaModel): Try {
        when (val result = context.logResult(iteration, model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        for (extractContext in extractContexts) {
            when (val result = extractContext.logResult(iteration, model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    private fun logIpResults(iteration: UInt64, model: AbstractLinearMetaModel): Try {
        when (val result = logLpResults(iteration, model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = context.logBunchCost(iteration, model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}
