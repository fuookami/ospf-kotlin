package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_selection.service

import kotlin.time.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.solver.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.Solution
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_selection.model.*

class BranchAndPriceAlgorithm<
    Map : AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    IT : IterativeAbstractTask<E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>    
>(
    private val executors: List<E>,
    private val tasks: List<T>,
    private val initialTasks: List<IT>,
    private val solver: ColumnGenerationSolver,
    private val policy: Policy<Map, Args, IT, T, E, A>,
    private val configuration: Configuration
) {
    data class Policy<
        Map : AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        Args : GanttSchedulingShadowPriceArguments<E, A>,
        IT : IterativeAbstractTask<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
    >(
        val contextBuilder: () -> IterativeTaskCompilationContext<Args, IT, T, E, A>,
        val extractContextBuilder: List<(IterativeTaskCompilationContext<Args, IT, T, E, A>) -> List<ExtractIterativeTaskCompilationContext<Args, IT, T, E, A>>>,
        val shadowPriceMap: () -> Map,
        val reducedCost: (Map, IT) -> Flt64,
        val taskGenerator: suspend (UInt64, List<E>, Map) -> Ret<List<IT>>,
    )
    
    data class Configuration(
        val solver: String? = null,
        val maxBadReducedAmount: UInt64 = UInt64(20UL),
        val maximumColumnAmount: UInt64 = UInt64(50000UL),
        val minimumColumnAmountPerExecutor: UInt64 = UInt64.zero,
        val timeLimit: Duration = 30000.seconds
    )

    private val logger = logger()

    private val context = policy.contextBuilder()
    private val extractContexts = policy.extractContextBuilder.flatMap { it(context) }
    private lateinit var shadowPriceMap: Map

    private val fixedTasks = HashSet<IT>()
    private val keptTasks = HashSet<IT>()
    private val hiddenExecutors = HashSet<E>()

    private var mainProblemSolvingTimes: UInt64 = UInt64.zero
    private var mainProblemSolvingTime: Duration = Duration.ZERO
    private var mainProblemModelingTime: Duration = Duration.ZERO
    private var subProblemSolvingTimes: UInt64 = UInt64.zero
    private var subProblemSolvingTime: Duration = Duration.ZERO

    private val columnAmount: UInt64 get() = context.columnAmount
    private val executorAmount: UInt64 get() = UInt64(executors.size)

    private fun notFixedExecutorAmount(fixedTasks: Set<IT>): UInt64 {
        return executorAmount - UInt64(fixedTasks.map { it.executor!! }.distinct().size)
    }

    private fun minimumColumnAmount(fixedTasks: Set<IT>, configuration: Configuration): UInt64 {
        return notFixedExecutorAmount(fixedTasks) * configuration.minimumColumnAmountPerExecutor
    }

    suspend operator fun invoke(id: String): Ret<Solution<T, E, A>> {
        var maximumReducedCost1 = Flt64(50.0)
        var maximumReducedCost2 = Flt64(3000.0)

        val beginTime = Clock.System.now()
        try {
            lateinit var bestSolution: Solution<T, E, A>
            val model = LinearMetaModel(id)
            var iteration = Iteration<IT, E, A>()
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
            logMILPResults(iteration.iteration, model)

            bestSolution = when (val result = analyzeSolution(iteration.iteration, model)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }
            }
            mainProblemSolvingTimes += UInt64.one
            mainProblemSolvingTime += ipRet.time
            iteration.refreshIpObj(ipRet.obj)

            if (ipRet.obj eq Flt64.zero) {
                return Ok(bestSolution)
            }

            when (fixTasks(iteration.iteration, model)) {
                is Ok -> {}

                is Failed -> {
                    return Ok(bestSolution)
                }
            }
            when (keepTasks(iteration.iteration, model)) {
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
                logLPResults(iteration.iteration, model)

                when (hideExecutors(model)) {
                    is Ok -> {}

                    is Failed -> {
                        return Ok(bestSolution)
                    }
                }

                logger.debug { "Global column generation of iteration $mainIteration begin!" }

                // globally column generation
                // it runs only 1 time
                for (count in 0 until 1) {
                    ++iteration
                    val newTasks = when (val result = solveSP(id, iteration, executors, shadowPriceMap)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            return Ok(bestSolution)
                        }
                    }
                    if (newTasks.isEmpty()) {
                        logger.debug { "There is no task generated in global column generation of iteration $mainIteration." }
                        if (iteration.optimalRate eq Flt64.one) {
                            return Ok(bestSolution)
                        }
                    }
                    val newTaskAmount = UInt64(newTasks.size.toULong())

                    when (addColumns(iteration.iteration, newTasks, model)) {
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
                    logLPResults(iteration.iteration, model)

                    val badReducedAmount = UInt64(fixedTasks.count { policy.reducedCost(shadowPriceMap, it) gr Flt64.zero })
                    if (columnAmount > configuration.maximumColumnAmount) {
                        maximumReducedCost1 = when (val result = removeColumns(maximumReducedCost1, configuration.maximumColumnAmount, shadowPriceMap, fixedTasks, keptTasks, model)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                return Ok(bestSolution)
                            }
                        }
                    }
                    if (badReducedAmount >= configuration.maxBadReducedAmount
                        || newTaskAmount <= minimumColumnAmount(fixedTasks, configuration)
                    ) {
                        break
                    }
                }
                maximumReducedCost1 = Flt64(50.0)

                logger.debug { "Global column generation of iteration $mainIteration end!" }

                val freeExecutors = when (val result = selectFreeExecutors(shadowPriceMap, model)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        return Ok(bestSolution)
                    }
                }
                val fixedTasks = when (val result = globallyFix(freeExecutors)) {
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
                    logLPResults(iteration.iteration, model)

                    ++iteration
                    val newTasks = when (val result = solveSP(id, iteration, freeExecutorList, shadowPriceMap)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            return Ok(bestSolution)
                        }
                    }
                    if (newTasks.isEmpty()) {
                        --iteration
                        logger.debug { "There is no task generated in local column generation of iteration $mainIteration: $iteration." }
                        break
                    }
                    val newTaskAmount = UInt64(newTasks.size.toULong())

                    when (addColumns(iteration.iteration, newTasks, model)) {
                        is Ok -> {}

                        is Failed -> {
                            return Ok(bestSolution)
                        }
                    }
                    val newFixedTasks = when (val result = locallyFix(iteration.iteration, fixedTasks, model)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            return Ok(bestSolution)
                        }
                    }
                    if (newFixedTasks.isNotEmpty()) {
                        for (task in newFixedTasks) {
                            freeExecutorList.remove(task.executor!!)
                        }
                        fixedTasks.addAll(newFixedTasks)
                    } else {
                        break
                    }

                    if (columnAmount > configuration.maximumColumnAmount
                        && newTaskAmount > minimumColumnAmount(fixedTasks, configuration)
                    ) {
                        maximumReducedCost2 = when (val result = removeColumns(
                            maximumReducedCost2, 
                            configuration.maximumColumnAmount, 
                            shadowPriceMap, 
                            fixedTasks, 
                            keptTasks, 
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

                this.fixedTasks.clear()
                this.fixedTasks.addAll(fixedTasks)
                // 所有生产设备已经有被固定的列（串）或者被隐藏，求解一次 IP 结束本次主迭代
                val thisIpRet = when (val result = solver.solveMILP("${id}_${iteration}_ip", model)) {
                    is Ok -> {
                        model.setSolution(result.value.solution)
                        result.value
                    }

                    is Failed -> {
                        return Ok(bestSolution)
                    }
                }
                mainProblemSolvingTimes += UInt64.one
                mainProblemSolvingTime += thisIpRet.time
                logMILPResults(iteration.iteration, model)
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

                // 结束一次主迭代后，刷新所有被固定的列（串）以及被隐藏的生产设备
                flush(iteration.iteration)
                iteration.halveStep()

                logger.debug { "Iteration $mainIteration end, optimal rate: ${String.format("%.2f", (iteration.optimalRate * Flt64(100.0)).toDouble())}%" }
                ++mainIteration
            }

            return Ok(bestSolution)
        } catch (e: Exception) {
            print(e.stackTraceToString())
            return Failed(Err(ErrorCode.ApplicationException, e.message))
        }
    }

    private fun heartBeat(id: String, optimalRate: Flt64) {
        logger.info { "Heart beat, current optimal rate: ${String.format("%.2f", (optimalRate * Flt64(100.0)).toDouble())}%" }
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
        
        when (val result = context.addColumns(UInt64.zero, initialTasks, model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    private suspend fun solveRMP(
        id: String,
        iteration: Iteration<IT, E, A>,
        model: LinearMetaModel,
        withKeeping: Boolean
    ): Result<Map, Error> {
        val lpRet = when (val result = solver.solveLP("${id}_${iteration}_lp", model)) {
            is Ok -> {
                model.setSolution(result.value.solution)
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }

        mainProblemSolvingTimes += UInt64.one
        mainProblemSolvingTime += lpRet.result.time
        if (iteration.refreshLpObj(lpRet.result.obj) && withKeeping) {
            when (val ret = keepTasks(iteration.iteration, model)) {
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
        iteration: Iteration<IT, E, A>,
        executors: List<E>,
        shadowPriceMap: Map
    ): Result<List<IT>, Error> {
        val beginTime = Clock.System.now()
        val newTasks = when (val results = policy.taskGenerator(iteration.iteration, executors, shadowPriceMap)) {
            is Ok -> {
                results.value
            }

            is Failed -> {
                return Failed(results.error)
            }
        }
        subProblemSolvingTimes += UInt64.one
        subProblemSolvingTime = Clock.System.now() - beginTime
        iteration.refreshLowerBound(newTasks) { policy.reducedCost(shadowPriceMap, it) }
        heartBeat(id, iteration.optimalRate)
        return Ok(newTasks)
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

    private suspend fun addColumns(iteration: UInt64, newTasks: List<IT>, model: AbstractLinearMetaModel): Try {
        val beginTime = Clock.System.now()

        val unduplicatedPlans = when (val result = context.addColumns(iteration, newTasks, model)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (unduplicatedPlans.isNotEmpty()) {
            model.flush()
        }

        mainProblemModelingTime += Clock.System.now() - beginTime
        return ok
    }

    private fun removeColumns(
        maximumReducedCost: Flt64,
        maximumColumnAmount: UInt64,
        shadowPriceMap: Map,
        fixedTasks: Set<IT>,
        keptTasks: Set<IT>,
        model: AbstractLinearMetaModel
    ): Result<Flt64, Error> {
        val newMaximumReducedCost = when (val result = context.removeColumns(
            maximumReducedCost,
            maximumColumnAmount,
            { policy.reducedCost(shadowPriceMap, it) },
            fixedTasks,
            keptTasks,
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

    private fun fixTasks(
        iteration: UInt64,
        model: AbstractLinearMetaModel
    ): Try {
        return when (val result = context.extractFixedTasks(iteration, model)) {
            is Ok -> {
                fixedTasks.addAll(result.value)
                Ok(success)
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }

    private fun keepTasks(
        iteration: UInt64,
        model: AbstractLinearMetaModel
    ): Try {
        return when (val result = context.extractKeptTasks(iteration, model)) {
            is Ok -> {
                keptTasks.addAll(result.value)
                Ok(success)
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }

    private fun hideExecutors(model: AbstractLinearMetaModel): Try {
        return when (val result = context.extractHiddenExecutors(
            executors,
            model
        )) {
            is Ok -> {
                hiddenExecutors.addAll(result.value)
                Ok(success)
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }

    private fun selectFreeExecutors(
        shadowPriceMap: Map,
        model: AbstractLinearMetaModel
    ): Ret<Set<E>> {
        return when (val result = context.selectFreeExecutors(
            fixedTasks,
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

    private fun globallyFix(freeExecutors: Set<E>): Ret<Set<IT>> {
        val fixedTasks = HashSet<IT>()
        for (task in this.fixedTasks) {
            if (!freeExecutors.contains(task.executor!!)) {
                fixedTasks.add(task)
            }
        }

        when (val result = context.globallyFix(fixedTasks)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
        return Ok(fixedTasks)
    }

    private fun locallyFix(iteration: UInt64, fixedTasks: Set<IT>, model: AbstractLinearMetaModel): Ret<Set<IT>> {
        val fixBar = Flt64(0.9)
        return when (val ret = context.locallyFix(iteration, fixBar, fixedTasks, model)) {
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

        keptTasks.clear()
        hiddenExecutors.clear()
        return ok
    }

    private fun analyzeSolution(
        iteration: UInt64,
        model: AbstractLinearMetaModel
    ): Result<Solution<T, E, A>, Error> {
        return when (val result = context.analyzeSolution(iteration, tasks, model)) {
            is Ok -> {
                Ok(result.value)
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }

    private fun logLPResults(iteration: UInt64, model: AbstractLinearMetaModel): Try {
        when (val result = context.logResult(iteration, model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
        return ok
    }

    private fun logMILPResults(iteration: UInt64, model: AbstractLinearMetaModel): Try {
        when (val result = logLPResults(iteration, model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = context.logTaskCost(iteration, model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}
