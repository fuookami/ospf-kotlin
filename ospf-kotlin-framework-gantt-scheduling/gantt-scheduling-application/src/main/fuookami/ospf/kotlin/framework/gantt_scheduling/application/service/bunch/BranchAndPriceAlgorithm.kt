@file:Suppress("UNCHECKED_CAST")
@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 任务束分支定价算法 / Bunch branch and price algorithm */
package fuookami.ospf.kotlin.framework.gantt_scheduling.application.service.bunch

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.BunchSolution
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.model.SolverRunId
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.model.bunch.Iteration

/**
 * 分支分组跟踪器 / Branch group tracker
 *
 * 单时域默认一个执行器对应一个分组；分时隙场景可让一个执行器对应多个 `(executor, slot)` 分组。
 * A single-horizon executor owns one group by default, while slot-based scheduling can expose multiple
 * `(executor, slot)` groups for the same executor.
 */
internal class BranchGroupTracker<B, E>(
    private val executors: List<E>,
    private val groupOfBunch: (B) -> Any,
    private val groupsOfExecutor: (E) -> Set<Any>
) {
    val amount: UInt64
        get() = UInt64(executors.flatMap(groupsOfExecutor).toSet().size.toULong())

    fun notFixedAmount(fixedBunches: Set<B>): UInt64 {
        val fixedGroups = fixedBunches.map(groupOfBunch).toSet()
        return amount - UInt64(fixedGroups.size.toULong())
    }

    fun allGroupsFixed(executor: E, fixedBunches: Set<B>): Boolean {
        val fixedGroups = fixedBunches.map(groupOfBunch).toSet()
        return groupsOfExecutor(executor).all { it in fixedGroups }
    }
}

/**
 * 分支定价算法 / Branch and price algorithm
 *
 * @param Map 影子价格映射类型 / Shadow price map type
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param B 任务束类型 / Bunch type
 * @param V 数值类型 / Value type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param executors 执行器列表 / List of executors
 * @param tasks 任务列表 / List of tasks
 * @param initialBunches 初始任务束列表 / List of initial bunches
 * @param solver 列生成求解器 / Column generation solver
 * @param policy 策略 / Policy
 * @param configuration 配置 / Configuration
*/
class BranchAndPriceAlgorithm<
        Map : AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        B : AbstractTaskBunch<T, E, A, V>,
        V : RealNumber<V>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val executors: List<E>,
    private val tasks: List<T>,
    private val initialBunches: List<B>,
    private val solver: ColumnGenerationSolver,
    private val policy: Policy<Map, Args, B, V, T, E, A>,
    private val configuration: Configuration,
    /**
     * Optional slot calendar used by the executor-slot pricing entry point.
     * When [Policy.bunchGeneratorByExecutorAndSlot] is configured, one pricing
     * request is issued for every visible `(executor, slot)` pair.
     */
    private val slots: List<TimeSlot> = emptyList()
) {

    /**
     * 策略 / Policy
     *
     * @param Map 影子价格映射类型 / Shadow price map type
     * @param Args 影子价格参数类型 / Shadow price arguments type
     * @param B 任务束类型 / Bunch type
     * @param V 数值类型 / Value type
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @property contextBuilder 上下文构建器 / Context builder
     * @property extractContextBuilder 提取上下文构建器列表 / Extract context builder list
     * @property shadowPriceMap 影子价格映射构建器 / Shadow price map builder
     * @property reducedCost 约简成本标量函数，仅用于 branch-and-price 内部列筛选 / Reduced-cost scalar function used only for internal column filtering
     * @property bunchGenerator 任务束生成器 / Bunch generator
     * @property bunchGeneratorWithBranchContext 带分支状态的可选任务束生成器 / Optional bunch generator carrying branch state
     * @property bunchGeneratorByExecutorAndSlot 按执行器和时隙调用的可选定价器 / Optional executor-slot pricing generator
     * @property branchGroupOfBunch 任务束的分支分组键，默认按执行器 / Branch group key for a bunch, executor by default
     * @property branchGroupsOfExecutor 一个执行器对应的分支分组键 / Branch groups associated with an executor
    */
    data class Policy<
            Map : AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
            Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
            B : AbstractTaskBunch<T, E, A, V>,
            V : RealNumber<V>,
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>
            >(
        val contextBuilder: () -> BunchCompilationContext<Args, B, V, T, E, A>,
        val extractContextBuilder: List<(BunchCompilationContext<Args, B, V, T, E, A>) -> List<ExtractBunchCompilationContext<Args, B, V, T, E, A>>>,
        val shadowPriceMap: () -> Map,
        val reducedCost: (Map, AbstractTaskBunch<T, E, A, V>) -> Flt64,
        val bunchGenerator: suspend (UInt64, List<E>, Map) -> Ret<List<B>>,
        val bunchGeneratorWithBranchContext: (suspend (UInt64, List<E>, Map, Set<B>, Set<B>, Set<E>) -> Ret<List<B>>)? = null,
        val bunchGeneratorByExecutorAndSlot: (suspend (UInt64, E, TimeSlot, Map, Set<B>, Set<B>, Set<E>) -> Ret<List<B>>)? = null,
        val branchGroupOfBunch: (B) -> Any = { it.executor },
        val branchGroupsOfExecutor: (E) -> Set<Any> = { setOf(it) }
    )

    /**
     * 配置 / Configuration
     *
     * @property badReducedAmount 约简成本差的数量阈值 / Bad reduced cost amount threshold
     * @property maximumColumnAmount 最大列数 / Maximum column amount
     * @property minimumColumnAmountPerExecutor 每个执行器最小列数 / Minimum column amount per executor
     * @property timeLimit 时间限制 / Time limit
    */
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
    private var lastKeptBunches: kotlin.collections.Map<B, Flt64> = emptyMap()
    private val hiddenExecutors = HashSet<E>()

    private var mainProblemSolvingTimes: UInt64 = UInt64.zero
    private var mainProblemSolvingTime: Duration = Duration.ZERO
    private var mainProblemModelingTime: Duration = Duration.ZERO
    private var subProblemSolvingTimes: UInt64 = UInt64.zero
    private var subProblemSolvingTime: Duration = Duration.ZERO

    private val columnAmount: UInt64 get() = context.columnAmount
    private val branchGroupTracker = BranchGroupTracker(
        executors = executors,
        groupOfBunch = policy.branchGroupOfBunch,
        groupsOfExecutor = policy.branchGroupsOfExecutor
    )

    /**
     * Calculate the number of executors not associated with fixed bunches.
     * 计算未关联固定任务束的执行器数量
     *
     * @param fixedBunches Set of fixed bunches / 已固定的任务束集合
     * @return Number of non-fixed executors / 未固定的执行器数量
    */
    private fun notFixedExtractorAmount(fixedBunches: Set<B>): UInt64 {
        return branchGroupTracker.notFixedAmount(fixedBunches)
    }

    /**
     * Calculate the minimum column amount requirement per executor in the current state.
     * 计算当前状态下每个执行器的最小列数要求
     *
     * @param fixedBunches Set of fixed bunches / 已固定的任务束集合
     * @param configuration Algorithm configuration / 算法配置
     * @return Minimum column amount / 最小列数
    */
    private fun minimumColumnAmount(
        fixedBunches: Set<B>,
        configuration: Configuration
    ): UInt64 {
        return notFixedExtractorAmount(fixedBunches) * configuration.minimumColumnAmountPerExecutor
    }

    /**
     * 执行分支定价算法 / Execute branch and price algorithm
     *
     * @param id 标识符 / Identifier
     * @param heartBeatCallBack 心跳回调，第三个参数为无量纲最优率标量 / Heartbeat callback whose third argument is a dimensionless optimal-rate scalar
     * @return 任务束解 / Bunch solution
    */
    suspend operator fun invoke(
        id: SolverRunId,
        heartBeatCallBack: ((kotlin.time.Instant, Duration, Flt64) -> Try)? = null
    ): Ret<BunchSolution<B, V, T, E, A>> {
        var maximumReducedCost1 = Flt64(50.0)
        var maximumReducedCost2 = Flt64(3000.0)

        val beginTime = Clock.System.now()
        lateinit var bestSolution: BunchSolution<B, V, T, E, A>
        return LinearMetaModel<Flt64>(id.value, converter = schedulingSolverValueAdapter).use { model ->
            try {

                var iteration = Iteration<T, E, A, V>()
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
                logIpResults(iteration.iteration, model)

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

                    is Fatal -> {
                        return Ok(bestSolution)
                    }
                }
                when (keepBunch(iteration.iteration, model)) {
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
                    logLpResults(iteration.iteration, model)

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
                    for (count in 0..<1) {
                        ++iteration
                        val newBunches = when (val result = solveSP(id, iteration, executors, shadowPriceMap)) {
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
                        logLpResults(iteration.iteration, model)

                        val reducedAmount = UInt64(fixedBunches.count { policy.reducedCost(shadowPriceMap, it) gr Flt64.zero })
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

                                is Fatal -> {
                                    return Fatal(result.errors)
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

                        is Fatal -> {
                            return Fatal(result.errors)
                        }
                    }
                    val fixedBunches = when (val result = globallyFix(freeExecutors)) {
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
                        logLpResults(iteration.iteration, model)

                        ++iteration
                        val newBunches = when (val result = solveSP(id, iteration, freeExecutorList, shadowPriceMap)) {
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

                            is Fatal -> {
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

                            is Fatal -> {
                                return Fatal(result.errors)
                            }
                        }
                        if (newFixedBunches.isNotEmpty()) {
                            for (bunch in newFixedBunches) {
                                val allGroupsFixed = branchGroupTracker.allGroupsFixed(
                                    executor = bunch.executor,
                                    fixedBunches = fixedBunches + newFixedBunches
                                )
                                if (allGroupsFixed) {
                                    freeExecutorList.remove(bunch.executor)
                                }
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

                                is Fatal -> {
                                    return Fatal(result.errors)
                                }
                            }
                        }
                    }

                    logger.debug { "Local column generation of iteration $mainIteration end!" }

                    this.fixedBunches.clear()
                    this.fixedBunches.addAll(fixedBunches)
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

                            is Fatal -> {
                                return Fatal(result.errors)
                            }
                        }
                    }
                    heartBeat(iteration.optimalRate)

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
        logger.info {
            "Heart beat, current optimal rate: ${
                String.format(
                    "%.2f",
                    (optimalRate * Flt64(100.0)).toDouble()
                )
            }%"
        }
    }

    /**
     * Register the context and extract contexts in the model, and add initial columns.
     * 在模型中注册上下文和提取上下文，并添加初始列
     *
     * @param model Linear meta model / 线性元模型
     * @return Operation result / 操作结果
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

        val unduplicatedBunches = when (val result = context.addColumns(UInt64.zero, initialBunches, model)) {
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

        for (extractContext in extractContexts) {
            when (val result = extractContext.addColumns(UInt64.zero, unduplicatedBunches, model)) {
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
     * Solve the linear relaxation of the restricted master problem (RMP) and extract shadow prices.
     * 求解受限主问题（RMP）的线性松弛，提取影子价格
     *
     * @param id Solver run identifier / 求解器运行标识
     * @param iteration Current iteration / 当前迭代
     * @param model Linear meta model / 线性元模型
     * @param withKeeping Whether to perform bunch keeping when the objective improves / 目标值改善时是否执行任务束保留
     * @return Shadow price map / 影子价格映射
    */
    private suspend fun solveRMP(
        id: SolverRunId,
        iteration: Iteration<T, E, A, V>,
        model: LinearMetaModel<Flt64>,
        withKeeping: Boolean
    ): Ret<Map> {
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

        refresh(lpRet)
        if (iteration.refreshLpObj(lpRet.result.obj) && withKeeping) {
            when (val ret = keepBunch(iteration.iteration, model)) {
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
     * Solve the sub-problem (pricing problem) to generate new bunches with negative reduced cost.
     * 求解子问题（定价问题），生成具有负约简成本的新任务束
     *
     * @param id Solver run identifier / 求解器运行标识
     * @param iteration Current iteration / 当前迭代
     * @param executors List of executors / 执行器列表
     * @param shadowPriceMap Shadow price map / 影子价格映射
     * @return List of newly generated bunches / 新生成的任务束列表
    */
    private suspend fun solveSP(
        id: SolverRunId,
        iteration: Iteration<T, E, A, V>,
        executors: List<E>,
        shadowPriceMap: Map
    ): Ret<List<B>> {
        val beginTime = Clock.System.now()
        val slotGenerator = policy.bunchGeneratorByExecutorAndSlot
        val newBunches = if (slotGenerator != null && slots.isNotEmpty()) {
            val generated = mutableListOf<B>()
            for (executor in executors) {
                for (slot in slots) {
                    when (val result = slotGenerator.invoke(
                        iteration.iteration,
                        executor,
                        slot,
                        shadowPriceMap,
                        fixedBunches.toSet(),
                        keptBunches.toSet(),
                        hiddenExecutors.toSet()
                    )) {
                        is Ok -> generated += result.value
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                }
            }
            generated
        } else {
            when (val results = policy.bunchGeneratorWithBranchContext?.invoke(
                iteration.iteration,
                executors,
                shadowPriceMap,
                fixedBunches.toSet(),
                keptBunches.toSet(),
                hiddenExecutors.toSet()
            ) ?: policy.bunchGenerator(iteration.iteration, executors, shadowPriceMap)) {
                is Ok -> results.value
                is Failed -> return Failed(results.error)
                is Fatal -> return Fatal(results.errors)
            }
        }
        subProblemSolvingTimes += UInt64.one
        subProblemSolvingTime = Clock.System.now() - beginTime
        iteration.refreshLowerBound(newBunches) { policy.reducedCost(shadowPriceMap, it) }
        heartBeat(iteration.optimalRate)
        return Ok(newBunches)
    }

    /**
     * Extract the shadow price map from the LP dual solution.
     * 从 LP 对偶解中提取影子价格映射
     *
     * @param model Linear meta model / 线性元模型
     * @param shadowPrices Dual solution / 对偶解
     * @return Shadow price map / 影子价格映射
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
     * Add newly generated columns (bunches) to the model, deduplicate, and flush the model.
     * 向模型添加新生成的列（任务束），去重后刷新模型
     *
     * @param iteration Current iteration number / 当前迭代编号
     * @param newBunches List of newly generated bunches / 新生成的任务束列表
     * @param model Linear meta model / 线性元模型
     * @return Operation result / 操作结果
    */
    private suspend fun addColumns(
        iteration: UInt64,
        newBunches: List<B>,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        val beginTime = Clock.System.now()

        val unduplicatedBunches = when (val result = context.addColumns(iteration, newBunches, model)) {
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

        if (unduplicatedBunches.isNotEmpty()) {
            (model as AbstractMetaModel<Flt64>).flush()
        }

        for (extractContext in extractContexts) {
            when (val result = extractContext.addColumns(iteration, unduplicatedBunches, model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        mainProblemModelingTime += Clock.System.now() - beginTime
        return ok
    }

    /**
     * Remove redundant columns based on reduced cost to keep total column count under the limit.
     * 根据约简成本移除冗余列，控制列总数不超过上限
     *
     * @param maximumReducedCost Maximum reduced cost threshold / 最大约简成本阈值
     * @param maximumColumnAmount Maximum column amount / 最大列数
     * @param shadowPriceMap Shadow price map / 影子价格映射
     * @param fixedBunches Set of fixed bunches / 已固定的任务束集合
     * @param keptBunches Set of bunches to keep / 需保留的任务束集合
     * @param model Linear meta model / 线性元模型
     * @return New maximum reduced cost threshold / 新的最大约简成本阈值
    */
    private fun removeColumns(
        maximumReducedCost: Flt64,
        maximumColumnAmount: UInt64,
        shadowPriceMap: Map,
        fixedBunches: Set<B>,
        keptBunches: Set<B>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Flt64> {
        val newMaximumReducedCost = when (val result = context.removeColumns(
            maximumReducedCost,
            maximumColumnAmount,
            { bunch: AbstractTaskBunch<T, E, A, V> -> policy.reducedCost(shadowPriceMap, bunch) },
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

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return Ok(newMaximumReducedCost)
    }

    /**
     * Extract and fix bunches from the model (bind bunches to specific executors).
     * 从模型中提取并固定任务束（将任务束绑定到特定执行器）
     *
     * @param iteration Current iteration number / 当前迭代编号
     * @param model Linear meta model / 线性元模型
     * @return Operation result / 操作结果
    */
    private fun fixBunch(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return when (val result = context.extractFixedBunches(iteration, model)) {
            is Ok -> {
                fixedBunches.addAll(result.value)
                ok
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
     * Extract and keep bunches from the model (exempt from column removal).
     * 从模型中提取并保留任务束（在列删除中不被移除）
     *
     * @param iteration Current iteration number / 当前迭代编号
     * @param model Linear meta model / 线性元模型
     * @return Operation result / 操作结果
    */
    private fun keepBunch(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = context.extractKeptBunches(iteration, model)) {
            is Ok -> {
                keptBunches.addAll(result.value)
            }

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = context.extractKeptBunchesWithRatio(iteration, model)) {
            is Ok -> {
                lastKeptBunches = result.value
            }

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
     * Extract and hide executors from the model.
     * 从模型中提取并隐藏执行器
     *
     * @param model Linear meta model / 线性元模型
     * @return Operation result / 操作结果
    */
    private fun hideExecutors(model: AbstractLinearMetaModel<Flt64>): Try {
        return when (val result = context.extractHiddenExecutors(executors, model)) {
            is Ok -> {
                hiddenExecutors.addAll(result.value)
                ok
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
     * Select free executors (not fixed or hidden) based on shadow prices.
     * 根据影子价格选择自由执行器（未被固定或隐藏）
     *
     * @param model Linear meta model / 线性元模型
     * @return Set of free executors / 自由执行器集合
    */
    private fun selectFreeExecutors(
        model: AbstractLinearMetaModel<Flt64>
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

            is Fatal -> {
                Fatal(result.errors)
            }
        }
    }

    /**
     * Update main problem solving statistics from an LP result.
     * 根据 LP 求解结果更新主问题求解统计
     *
     * @param feasibleLpResult Feasible LP result / 可行 LP 结果
    */
    private fun refresh(feasibleLpResult: ColumnGenerationSolver.LPResult) {
        mainProblemSolvingTimes += UInt64.one
        mainProblemSolvingTime += feasibleLpResult.result.time
    }

    /**
     * Update main problem solving statistics from an IP result.
     * 根据 IP 求解结果更新主问题求解统计
     *
     * @param ipResult Feasible IP result / 可行 IP 结果
    */
    private fun refresh(ipResult: FeasibleSolverOutput<Flt64>) {
        mainProblemSolvingTimes += UInt64.one
        mainProblemSolvingTime += ipResult.time
    }

    /**
     * Globally fix bunches: commit fixed bunches that are not in the free executors set to the context.
     * 全局固定任务束：将不在自由执行器中的已固定任务束提交到上下文
     *
     * @param freeExecutors Set of free executors / 自由执行器集合
     * @return Set of fixed bunches / 已固定的任务束集合
    */
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

            is Fatal -> {
                return Fatal(result.errors)
            }
        }
        return Ok(fixedBunches)
    }

    /**
     * Locally fix bunches: pin high-certainty bunches to executors based on the current solution.
     * 局部固定任务束：根据当前解将高确定性的任务束固定到执行器
     *
     * @param iteration Current iteration number / 当前迭代编号
     * @param fixedBunches Set of already fixed bunches / 已固定的任务束集合
     * @param model Linear meta model / 线性元模型
     * @return Set of newly fixed bunches / 新固定的任务束集合
    */
    private fun locallyFix(
        iteration: UInt64,
        fixedBunches: Set<B>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Set<B>> {
        val fixBar = Flt64(0.9)
        return when (val ret = context.locallyFix(iteration, fixBar, fixedBunches, model)) {
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
     * Flush the context state of the current iteration, clearing kept bunches and hidden executors.
     * 刷新当前迭代的上下文状态，清除保留任务束和隐藏执行器
     *
     * @param iteration Current iteration number / 当前迭代编号
     * @return Operation result / 操作结果
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

        keptBunches.clear()
        hiddenExecutors.clear()
        return ok
    }

    /**
     * Analyze the current solution and extract the bunch scheduling plan.
     * 分析当前解，提取任务束调度方案
     *
     * @param iteration Current iteration number / 当前迭代编号
     * @param model Linear meta model / 线性元模型
     * @return Bunch solution / 任务束解
    */
    private fun analyzeSolution(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<BunchSolution<B, V, T, E, A>> {
        return when (val result = context.analyzeBunchSolution(iteration, tasks, model)) {
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
     * Log the LP solving results.
     * 记录 LP 求解结果日志
     *
     * @param iteration Current iteration number / 当前迭代编号
     * @param model Linear meta model / 线性元模型
     * @return Operation result / 操作结果
    */
    private fun logLpResults(iteration: UInt64, model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = context.logResult(iteration, model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        for (extractContext in extractContexts) {
            when (val result = extractContext.logResult(iteration, model)) {
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
     * Log the IP solving results, including LP results and bunch cost.
     * 记录 IP 求解结果日志，包括 LP 结果和任务束成本
     *
     * @param iteration Current iteration number / 当前迭代编号
     * @param model Linear meta model / 线性元模型
     * @return Operation result / 操作结果
    */
    private fun logIpResults(iteration: UInt64, model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = logLpResults(iteration, model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = context.logBunchCost(iteration, model)) {
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
}
