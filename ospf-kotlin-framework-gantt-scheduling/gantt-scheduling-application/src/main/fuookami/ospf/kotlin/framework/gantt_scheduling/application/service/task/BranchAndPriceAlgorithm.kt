@file:Suppress("UNCHECKED_CAST")
@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 任务分支定价算法 / Task branch and price algorithm */
package fuookami.ospf.kotlin.framework.gantt_scheduling.application.service.task

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskSolution
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.model.SolverRunId
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.model.task.Iteration

/**
 * 分支定价算法 / Branch and price algorithm
 *
 * @param Map 影子价格映射类型 / Shadow price map type
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param V 数值类型 / Value type
 * @param IT 迭代任务类型 / Iterative task type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param executors 执行器列表 / List of executors
 * @param tasks 任务列表 / List of tasks
 * @param initialTasks 初始任务列表 / List of initial tasks
 * @param solver 列生成求解器 / Column generation solver
 * @param policy 策略 / Policy
 * @param configuration 配置 / Configuration
*/
class BranchAndPriceAlgorithm<
        Map : AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        V : RealNumber<V>,
        IT : IterativeAbstractTask<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val executors: List<E>,
    private val tasks: List<T>,
    private val initialTasks: List<IT>,
    private val solver: ColumnGenerationSolver,
    private val policy: Policy<Map, Args, V, IT, T, E, A>,
    private val configuration: Configuration
) {

    /**
     * 策略 / Policy
     *
     * @param Map 影子价格映射类型 / Shadow price map type
     * @param Args 影子价格参数类型 / Shadow price arguments type
     * @param IT 迭代任务类型 / Iterative task type
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @property contextBuilder 上下文构建器 / Context builder
     * @property extractContextBuilder 提取上下文构建器列表 / Extract context builder list
     * @property shadowPriceMap 影子价格映射构建器 / Shadow price map builder
     * @property reducedCost 约简成本标量函数，仅用于 branch-and-price 内部列筛选 / Reduced-cost scalar function used only for internal column filtering
     * @property taskGenerator 任务生成器 / Task generator
    */
    data class Policy<
            Map : AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
            Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
            V : RealNumber<V>,
            IT : IterativeAbstractTask<E, A>,
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>
            >(
        val contextBuilder: () -> IterativeTaskCompilationContext<Args, V, IT, T, E, A>,
        val extractContextBuilder: List<(IterativeTaskCompilationContext<Args, V, IT, T, E, A>) -> List<ExtractIterativeTaskCompilationContext<Args, V, IT, T, E, A>>>,
        val shadowPriceMap: () -> Map,
        val reducedCost: (Map, IT) -> Flt64,
        val taskGenerator: suspend (UInt64, List<E>, Map) -> Ret<List<IT>>,
    )

    /**
     * 配置 / Configuration
     *
     * @property solver 求解器名称 / Solver name
     * @property maxBadReducedAmount 约简成本差的最大数量阈值 / Maximum bad reduced cost amount threshold
     * @property maximumColumnAmount 最大列数 / Maximum column amount
     * @property minimumColumnAmountPerExecutor 每个执行器最小列数 / Minimum column amount per executor
     * @property timeLimit 时间限制 / Time limit
    */
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

    /**
     * 计算未固定任务的执行器数量。
     * Calculate the number of executors that are not associated with fixed tasks.
     *
     * @param fixedTasks 已固定的任务集合 / The set of fixed tasks.
     * @return 未固定任务的执行器数量 / The number of executors without fixed tasks.
    */
    private fun notFixedExecutorAmount(fixedTasks: Set<IT>): UInt64 {
        return executorAmount - UInt64(fixedTasks.map { it.executor!! }.distinct().size)
    }

    /**
     * 计算当前状态下每个执行器的最小列数要求。
     * Calculate the minimum column amount requirement per executor in the current state.
     *
     * @param fixedTasks 已固定的任务集合 / The set of fixed tasks.
     * @param configuration 算法配置 / The algorithm configuration.
     * @return 最小列数 / The minimum column amount.
    */
    private fun minimumColumnAmount(fixedTasks: Set<IT>, configuration: Configuration): UInt64 {
        return notFixedExecutorAmount(fixedTasks) * configuration.minimumColumnAmountPerExecutor
    }

    /**
     * 执行分支定价算法 / Execute branch and price algorithm
     *
     * @param id 标识符 / Identifier
     * @return 任务解 / Task solution
    */
    suspend operator fun invoke(id: SolverRunId): Ret<TaskSolution<T, E, A>> {
        var maximumReducedCost1 = Flt64(50.0)
        var maximumReducedCost2 = Flt64(3000.0)

        val beginTime = Clock.System.now()
        lateinit var bestSolution: TaskSolution<T, E, A>
        return LinearMetaModel<Flt64>(id.value, converter = schedulingSolverValueAdapter).use { model ->
            try {
                var iteration = Iteration<IT, E, A>()
                when (val result = register(model)) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }

                // solve ip with initial column / 使用初始列求解 IP
                val ipRet = when (val result = solver.solveMILP("${id.value}_$iteration", model)) {
                    is Ok -> {
                        model.setSolution(result.value.solution)
                        result.value
                    }

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
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

                    is Fatal -> {
                        return Fatal(result.errors)
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

                    is Fatal -> {
                        return Ok(bestSolution)
                    }
                }
                when (keepTasks(iteration.iteration, model)) {
                    is Ok -> {}

                    is Failed -> {
                        return Ok(bestSolution)
                    }

                    is Fatal -> {
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

                        is Fatal -> {
                            return Fatal(result.errors)
                        }
                    }
                    logLPResults(iteration.iteration, model)

                    when (hideExecutors(model)) {
                        is Ok -> {}

                        is Failed -> {
                            return Ok(bestSolution)
                        }

                        is Fatal -> {
                            return Ok(bestSolution)
                        }
                    }

                    logger.debug { "Global column generation of iteration $mainIteration begin!" }

                    // globally column generation / 全局列生成
                    // it runs only 1 time / 仅运行 1 次
                    for (count in 0 until 1) {
                        ++iteration
                        val newTasks = when (val result = solveSP(id, iteration, executors, shadowPriceMap)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                return Ok(bestSolution)
                            }

                            is Fatal -> {
                                return Fatal(result.errors)
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

                            is Fatal -> {
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

                            is Fatal -> {
                                return Fatal(result.errors)
                            }
                        }
                        logLPResults(iteration.iteration, model)

                        val badReducedAmount = UInt64(fixedTasks.count { policy.reducedCost(shadowPriceMap, it) gr Flt64.zero })
                        if (columnAmount > configuration.maximumColumnAmount) {
                            maximumReducedCost1 =
                                when (val result = removeColumns(maximumReducedCost1, configuration.maximumColumnAmount, shadowPriceMap, fixedTasks, keptTasks, model)) {
                                    is Ok -> {
                                        result.value
                                    }

                                    is Failed -> {
                                        return Ok(bestSolution)
                                    }

                                    is Fatal -> {
                                        return Fatal(result.errors)
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

                        is Fatal -> {
                            return Fatal(result.errors)
                        }
                    }
                    val fixedTasks = when (val result = globallyFix(freeExecutors)) {
                        is Ok -> {
                            result.value.toHashSet()
                        }

                        is Failed -> {
                            return Ok(bestSolution)
                        }

                        is Fatal -> {
                            return Fatal(result.errors)
                        }
                    }
                    val freeExecutorList = freeExecutors.toMutableList()

                    logger.debug { "Local column generation of iteration $mainIteration begin!" }

                    // locally column generation / 局部列生成
                    while (true) {
                        shadowPriceMap = when (val result = solveRMP(id, iteration, model, false)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                return Ok(bestSolution)
                            }

                            is Fatal -> {
                                return Fatal(result.errors)
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

                            is Fatal -> {
                                return Fatal(result.errors)
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

                            is Fatal -> {
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

                            is Fatal -> {
                                return Fatal(result.errors)
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

                                is Fatal -> {
                                    return Fatal(result.errors)
                                }
                            }
                        }
                    }

                    logger.debug { "Local column generation of iteration $mainIteration end!" }

                    this.fixedTasks.clear()
                    this.fixedTasks.addAll(fixedTasks)
                    // 所有生产设备已经有被固定的列（串）或者被隐藏，求解一个 IP 结束本次主迭代
                    val thisIpRet = when (val result = solver.solveMILP("${id.value}_${iteration}_ip", model)) {
                        is Ok -> {
                            model.setSolution(result.value.solution)
                            result.value
                        }

                        is Failed -> {
                            return Ok(bestSolution)
                        }

                        is Fatal -> {
                            return Fatal(result.errors)
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

                            is Fatal -> {
                                return Fatal(result.errors)
                            }
                        }
                    }
                    heartBeat(iteration.optimalRate)

                    flush(iteration.iteration)
                    iteration.halveStep()

                    logger.debug { "Iteration $mainIteration end, optimal rate: ${String.format("%.2f", (iteration.optimalRate * Flt64(100.0)).toDouble())}%" }
                    ++mainIteration
                }

                Ok(bestSolution)
            } catch (e: Exception) {
                print(e.stackTraceToString())
                return Failed(Err(ErrorCode.ApplicationException, e.message))
            }
        }
    }

    /**
     * 记录算法心跳，最优率是无量纲内部标量 / Record algorithm heartbeat with dimensionless internal optimal-rate scalar
     *
     * @param optimalRate 无量纲最优率标量 / Dimensionless optimal-rate scalar
    */
    private fun heartBeat(optimalRate: Flt64) {
        logger.info { "Heart beat, current optimal rate: ${String.format("%.2f", (optimalRate * Flt64(100.0)).toDouble())}%" }
    }

    /**
     * 在模型中注册上下文和提取上下文，并添加初始列。
     * Register the context and extract contexts in the model, and add initial columns.
     *
     * @param model 线性元模型 / The linear meta model.
     * @return 操作结果 / The result of the operation.
    */
    private suspend fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = context.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        for (extractContext in extractContexts) {
            when (val result = extractContext.register(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        when (val result = context.addColumns(UInt64.zero, initialTasks, model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }

    /**
     * 求解受限主问题（RMP）的线性松弛，提取影子价格。
     * Solve the linear relaxation of the restricted master problem (RMP) and extract shadow prices.
     *
     * @param id 求解器运行标识 / The solver run identifier.
     * @param iteration 当前迭代 / The current iteration.
     * @param model 线性元模型 / The linear meta model.
     * @param withKeeping 是否在目标值改善时执行任务保留 / Whether to perform task keeping when the objective improves.
     * @return 影子价格映射 / The shadow price map.
    */
    private suspend fun solveRMP(
        id: SolverRunId,
        iteration: Iteration<IT, E, A>,
        model: LinearMetaModel<Flt64>,
        withKeeping: Boolean
    ): Result<Map, ErrorCode, Error<ErrorCode>> {
        val lpRet = when (val result = solver.solveLP("${id.value}_${iteration}_lp", model)) {
            is Ok -> {
                model.setSolution(result.value.solution)
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
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

                is Fatal -> {
                    return Fatal(ret.errors)
                }
            }
        }

        val shadowPriceMap = when (val ret = extractShadowPrice(model, lpRet.dualSolution.toMeta())) {
            is Ok -> {
                ret.value
            }

            is Failed -> {
                return Failed(ret.error)
            }

            is Fatal -> {
                return Fatal(ret.errors)
            }
        }

        return Ok(shadowPriceMap)
    }

    /**
     * 求解子问题（定价问题），生成具有负约简成本的新列。
     * Solve the sub-problem (pricing problem) to generate new columns with negative reduced cost.
     *
     * @param id 求解器运行标识 / The solver run identifier.
     * @param iteration 当前迭代 / The current iteration.
     * @param executors 执行器列表 / The list of executors.
     * @param shadowPriceMap 影子价格映射 / The shadow price map.
     * @return 新生成的任务列表 / The list of newly generated tasks.
    */
    private suspend fun solveSP(
        id: SolverRunId,
        iteration: Iteration<IT, E, A>,
        executors: List<E>,
        shadowPriceMap: Map
    ): Result<List<IT>, ErrorCode, Error<ErrorCode>> {
        val beginTime = Clock.System.now()
        val newTasks = when (val results = policy.taskGenerator(iteration.iteration, executors, shadowPriceMap)) {
            is Ok -> {
                results.value
            }

            is Failed -> {
                return Failed(results.error)
            }

            is Fatal -> {
                return Fatal(results.errors)
            }
        }
        subProblemSolvingTimes += UInt64.one
        subProblemSolvingTime = Clock.System.now() - beginTime
        iteration.refreshLowerBound(newTasks) { policy.reducedCost(shadowPriceMap, it) }
        heartBeat(iteration.optimalRate)
        return Ok(newTasks)
    }

    /**
     * 从 LP 对偶解中提取影子价格映射。
     * Extract the shadow price map from the LP dual solution.
     *
     * @param model 线性元模型 / The linear meta model.
     * @param shadowPrices 对偶解 / The dual solution.
     * @return 影子价格映射 / The shadow price map.
    */
    private fun extractShadowPrice(
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Ret<Map> {
        val map = policy.shadowPriceMap()

        when (val result = context.extractShadowPrice(map, model, shadowPrices)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        for (extractContext in extractContexts) {
            when (val result = extractContext.extractShadowPrice(map, model, shadowPrices)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return Ok(map)
    }

    /**
     * 向模型添加新生成的列（任务），并去重后刷新模型。
     * Add newly generated columns (tasks) to the model, deduplicate, and flush the model.
     *
     * @param iteration 当前迭代编号 / Current iteration number.
     * @param newTasks 新生成的任务列表 / The list of newly generated tasks.
     * @param model 线性元模型 / The linear meta model.
     * @return 操作结果 / The result of the operation.
    */
    private suspend fun addColumns(iteration: UInt64, newTasks: List<IT>, model: AbstractLinearMetaModel<Flt64>): Try {
        val beginTime = Clock.System.now()

        val unduplicatedPlans = when (val result = context.addColumns(iteration, newTasks, model)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (unduplicatedPlans.isNotEmpty()) {
            (model as AbstractMetaModel<Flt64>).flush()
        }

        mainProblemModelingTime += Clock.System.now() - beginTime
        return ok
    }

    /**
     * 根据约简成本移除冗余列，控制列总数不超过上限。
     * Remove redundant columns based on reduced cost to keep total column count under the limit.
     *
     * @param maximumReducedCost 最大约简成本阈值 / The maximum reduced cost threshold.
     * @param maximumColumnAmount 最大列数 / The maximum column amount.
     * @param shadowPriceMap 影子价格映射 / The shadow price map.
     * @param fixedTasks 已固定的任务集合 / The set of fixed tasks.
     * @param keptTasks 需保留的任务集合 / The set of tasks to keep.
     * @param model 线性元模型 / The linear meta model.
     * @return 新的最大约简成本阈值 / The new maximum reduced cost threshold.
    */
    private fun removeColumns(
        maximumReducedCost: Flt64,
        maximumColumnAmount: UInt64,
        shadowPriceMap: Map,
        fixedTasks: Set<IT>,
        keptTasks: Set<IT>,
        model: AbstractLinearMetaModel<Flt64>
    ): Result<Flt64, ErrorCode, Error<ErrorCode>> {
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

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return Ok(newMaximumReducedCost)
    }

    /**
     * 从模型中提取并固定任务（将任务绑定到特定执行器）。
     * Extract and fix tasks from the model (bind tasks to specific executors).
     *
     * @param iteration 当前迭代编号 / Current iteration number.
     * @param model 线性元模型 / The linear meta model.
     * @return 操作结果 / The result of the operation.
    */
    private fun fixTasks(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return when (val result = context.extractFixedTasks(iteration, model)) {
            is Ok -> {
                fixedTasks.addAll(result.value)
                Ok(success)
            }

            is Failed -> {
                Failed(result.error)
            }

            is Fatal -> {
                Fatal(result.errors)
            }
        }
    }

    /**
     * 从模型中提取并保留任务（在列删除中不被移除）。
     * Extract and keep tasks from the model (exempt from column removal).
     *
     * @param iteration 当前迭代编号 / Current iteration number.
     * @param model 线性元模型 / The linear meta model.
     * @return 操作结果 / The result of the operation.
    */
    private fun keepTasks(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return when (val result = context.extractKeptTasks(iteration, model)) {
            is Ok -> {
                keptTasks.addAll(result.value)
                Ok(success)
            }

            is Failed -> {
                Failed(result.error)
            }

            is Fatal -> {
                Fatal(result.errors)
            }
        }
    }

    /**
     * 从模型中提取并隐藏执行器。
     * Extract and hide executors from the model.
     *
     * @param model 线性元模型 / The linear meta model.
     * @return 操作结果 / The result of the operation.
    */
    private fun hideExecutors(model: AbstractLinearMetaModel<Flt64>): Try {
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

            is Fatal -> {
                Fatal(result.errors)
            }
        }
    }

    /**
     * 根据影子价格选择自由执行器（未被固定或隐藏）。
     * Select free executors (not fixed or hidden) based on shadow prices.
     *
     * @param shadowPriceMap 影子价格映射 / The shadow price map.
     * @param model 线性元模型 / The linear meta model.
     * @return 自由执行器集合 / The set of free executors.
    */
    private fun selectFreeExecutors(
        shadowPriceMap: Map,
        model: AbstractLinearMetaModel<Flt64>
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

            is Fatal -> {
                Fatal(result.errors)
            }
        }
    }

    /**
     * 全局固定任务：将不在自由执行器中的已固定任务提交到上下文。
     * Globally fix tasks: commit fixed tasks that are not in the free executors set to the context.
     *
     * @param freeExecutors 自由执行器集合 / The set of free executors.
     * @return 已固定的任务集合 / The set of fixed tasks.
    */
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

            is Fatal -> {
                return Fatal(result.errors)
            }
        }
        return Ok(fixedTasks)
    }

    /**
     * 局部固定任务：根据当前解将高确定性的任务固定到执行器。
     * Locally fix tasks: pin high-certainty tasks to executors based on the current solution.
     *
     * @param iteration 当前迭代编号 / Current iteration number.
     * @param fixedTasks 已固定的任务集合 / The set of already fixed tasks.
     * @param model 线性元模型 / The linear meta model.
     * @return 新固定的任务集合 / The set of newly fixed tasks.
    */
    private fun locallyFix(iteration: UInt64, fixedTasks: Set<IT>, model: AbstractLinearMetaModel<Flt64>): Ret<Set<IT>> {
        val fixBar = Flt64(0.9)
        return when (val ret = context.locallyFix(iteration, fixBar, fixedTasks, model)) {
            is Ok -> {
                Ok(ret.value)
            }

            is Failed -> {
                Failed(ret.error)
            }

            is Fatal -> {
                Fatal(ret.errors)
            }
        }
    }

    /**
     * 刷新当前迭代的上下文状态，清除保留任务和隐藏执行器。
     * Flush the context state of the current iteration, clearing kept tasks and hidden executors.
     *
     * @param iteration 当前迭代编号 / Current iteration number.
     * @return 操作结果 / The result of the operation.
    */
    private fun flush(iteration: UInt64): Try {
        when (val ret = context.flush(iteration)) {
            is Ok -> {}

            is Failed -> {
                return Failed(ret.error)
            }

            is Fatal -> {
                return Fatal(ret.errors)
            }
        }

        keptTasks.clear()
        hiddenExecutors.clear()
        return ok
    }

    /**
     * 分析当前解，提取任务调度方案。
     * Analyze the current solution and extract the task scheduling plan.
     *
     * @param iteration 当前迭代编号 / Current iteration number.
     * @param model 线性元模型 / The linear meta model.
     * @return 任务解 / The task solution.
    */
    private fun analyzeSolution(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Result<TaskSolution<T, E, A>, ErrorCode, Error<ErrorCode>> {
        return when (val result = context.analyzeSolution(iteration, tasks, model)) {
            is Ok -> {
                Ok(result.value)
            }

            is Failed -> {
                Failed(result.error)
            }

            is Fatal -> {
                Fatal(result.errors)
            }
        }
    }

    /**
     * 记录 LP 求解结果日志（仅在非生产环境）。
     * Log the LP solving results (only in non-production environment).
     *
     * @param iteration 当前迭代编号 / Current iteration number.
     * @param model 线性元模型 / The linear meta model.
     * @return 操作结果 / The result of the operation.
    */
    private fun logLPResults(iteration: UInt64, model: AbstractLinearMetaModel<Flt64>): Try {
        if (System.getProperty("env", "prod") != "prod") {
            when (val result = context.logResult(iteration, model)) {
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

    /**
     * 记录 MILP 求解结果日志（仅在非生产环境）。
     * Log the MILP solving results (only in non-production environment).
     *
     * @param iteration 当前迭代编号 / Current iteration number.
     * @param model 线性元模型 / The linear meta model.
     * @return 操作结果 / The result of the operation.
    */
    private fun logMILPResults(iteration: UInt64, model: AbstractLinearMetaModel<Flt64>): Try {
        if (System.getProperty("env", "prod") != "prod") {
            when (val result = logLPResults(iteration, model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }

            when (val result = context.logTaskCost(iteration, model)) {
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
}
