@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 任务束编译聚合 / Bunch compilation aggregation */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*

/**
 * Computes the next reduced-cost cutoff for column pruning.
 * 计算列剪枝的下一个约简成本阈值
 *
 * @param maximumReducedCost Current maximum reduced cost / 当前最大约简成本
 * @return Next reduced-cost cutoff value / 下一个约简成本阈值
*/
private fun nextReducedCostCutoff(maximumReducedCost: Flt64): Flt64 {
    val reducedCostCutoff = maximumReducedCost.floor().toInt64() * Int64(2L) / Int64(3L)
    return max(Flt64(reducedCostCutoff.toLong().toDouble()), Flt64(5.0))
}

/**
 * Abstract bunch compilation aggregation.
 * 抽象任务束编译聚合
 *
 * @param B Bunch type / 任务束类型
 * @param V Numeric type / 数值类型
 * @param T Task type / 任务类型
 * @param E Executor type / 执行器类型
 * @param A Assignment policy type / 分配策略类型
 * @param tasks List of tasks / 任务列表
 * @param executors List of executors / 执行器列表
 * @param lockCancelTasks Set of locked cancel tasks / 锁定取消任务集合
 * @param withExecutorLeisure Whether to include executor leisure / 是否包含执行器空闲
*/
abstract class AbstractBunchCompilationAggregation<
        B : AbstractTaskBunch<T, E, A, V>,
        V : RealNumber<V>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    protected val tasks: List<T>,
    protected val executors: List<E>,
    protected val lockCancelTasks: Set<T> = emptySet(),
    withExecutorLeisure: Boolean = true
) {
    private val logger = org.apache.logging.log4j.kotlin.logger("BunchSchedulingAggregation")

    val compilation: BunchCompilation<B, V, T, E, A> = BunchCompilation(
        tasks = tasks,
        executors = executors,
        lockCancelTasks = lockCancelTasks,
        withExecutorLeisure = withExecutorLeisure
    )

    val bunchesIteration: List<List<B>> by compilation::bunchesIteration
    val bunches: List<B> by compilation::bunches
    val removedBunches: Set<B> by compilation::removedBunches
    val lastIterationBunches: List<B> by compilation::lastIterationBunches

    /**
     * 注册到模型 / Register to model
     *
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
    */
    open fun register(model: MetaModel<Flt64>): Try {
        when (val result = compilation.register(model)) {
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
     * 添加列 / Add columns
     *
     * @param iteration 迭代次数 / Iteration count
     * @param newBunches 新任务束列表 / List of new bunches
     * @param model 线性元模型 / Linear meta model
     * @return 去重后的任务束列表 / Deduplicated bunch list
    */
    open suspend fun addColumns(
        iteration: UInt64,
        newBunches: List<B>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<B>> {
        val unduplicatedBunches = when (val result = compilation.addColumns(
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

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return Ok(unduplicatedBunches)
    }

    /**
     * 移除列 / Remove columns
     *
     * @param maximumReducedCost 最大约简成本 / Maximum reduced cost
     * @param maximumColumnAmount 最大列数 / Maximum column amount
     * @param reducedCost 约简成本函数 / Reduced cost function
     * @param fixedBunches 固定任务束集合 / Set of fixed bunches
     * @param keptBunches 保留任务束集合 / Set of kept bunches
     * @param model 线性元模型 / Linear meta model
     * @return 更新后的最大约简成本 / Updated maximum reduced cost
    */
    open fun removeColumns(
        maximumReducedCost: Flt64,
        maximumColumnAmount: UInt64,
        reducedCost: (B) -> Flt64,
        fixedBunches: Set<B>,
        keptBunches: Set<B>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Flt64> {
        for (bunch in bunches) {
            if (removedBunches.contains(bunch)) {
                continue
            }

            if (!(reducedCost(bunch) ls maximumReducedCost)
                && !fixedBunches.contains(bunch)
                && !keptBunches.contains(bunch)
            ) {
                compilation.aggregation.removeColumn(bunch)
            }
        }

        for (bunch in removedBunches) {
            val xi = compilation.x[bunch.iteration.toInt()]
            xi[bunch].range.eq(false)
            model.remove(xi[bunch])
        }

        val remainingAmount = UInt64(bunches.size.toULong())
        return if (remainingAmount > maximumColumnAmount) {
            Ok(nextReducedCostCutoff(maximumReducedCost))
        } else {
            Ok(maximumReducedCost)
        }
    }

    /**
     * 提取固定任务束 / Extract fixed bunches
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 固定任务束集合 / Set of fixed bunches
    */
    open fun extractFixedBunches(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Set<B>> {
        return extractBunches(iteration, model) { it eq Flt64.one }
            .map { it.keys }
    }

    /**
     * 提取保留任务束 / Extract kept bunches
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 保留任务束集合 / Set of kept bunches
    */
    open fun extractKeptBunches(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Set<B>> {
        return extractBunches(iteration, model) { it gr Flt64.zero }
            .map { it.keys }
    }

    /**
     * 提取保留任务束及比率 / Extract kept bunches with ratio
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 任务束到比率的映射 / Map of bunch to ratio
    */
    open fun extractKeptBunchesWithRatio(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Map<B, Flt64>> {
        return extractBunches(iteration, model) { it gr Flt64.zero }
    }

    /**
     * 提取隐藏执行器 / Extract hidden executors
     *
     * @param executors 执行器列表 / List of executors
     * @param model 线性元模型 / Linear meta model
     * @return 隐藏执行器集合 / Set of hidden executors
    */
    open fun extractHiddenExecutors(
        executors: List<E>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Set<E>> {
        val z = compilation.z
        val ret = HashSet<E>()
        for (token in model.tokens.tokens) {
            if (token belongsTo z) {
                if (token.result!! gr Flt64.zero) {
                    ret.add(executors[token.variable.index])
                }
            }
        }
        return Ok(ret)
    }

    /**
     * 全局固定 / Globally fix
     *
     * @param fixedBunches 固定任务束集合 / Set of fixed bunches
     * @return 操作结果 / Operation result
    */
    open fun globallyFix(fixedBunches: Set<B>): Try {
        for (bunch in fixedBunches) {
            assert(!removedBunches.contains(bunch))
            val xi = compilation.x[bunch.iteration.toInt()]
            xi[bunch].range.eq(true)
        }
        return ok
    }

    /**
     * 局部固定 / Locally fix
     *
     * @param iteration 迭代次数 / Iteration count
     * @param bar 阈值 / Threshold
     * @param fixedBunches 固定任务束集合 / Set of fixed bunches
     * @param model 线性元模型 / Linear meta model
     * @param withFixNot 是否固定非选中项 / Whether to fix not-selected items
     * @return 新固定的任务束集合 / Set of newly fixed bunches
    */
    open fun locallyFix(
        iteration: UInt64,
        bar: Flt64,
        fixedBunches: Set<B>,
        model: AbstractLinearMetaModel<Flt64>,
        withFixNot: Boolean = false
    ): Ret<Set<B>> {
        var flag = true
        val ret = HashSet<B>()

        var bestValue = Flt64.zero
        var bestIteration = UInt64.zero
        var bestIndex = 0

        val y = compilation.y
        for (token in model.tokens.tokens) {
            if (token belongsTo y && (token.result!! gr bar)) {
                y[token.variable.index].range.eq(true)
                flag = false
            }

            for (i in UInt64.zero..iteration) {
                if (token belongsTo compilation.x[i.toInt()]) {
                    val xi = compilation.x[i.toInt()]

                    if (token belongsTo xi) {
                        val bunch = bunchesIteration[i.toInt()][token.variable.index]
                        assert(!removedBunches.contains(bunch))

                        if ((token.result != null)
                            && (token.result!! geq bestValue)
                            && !fixedBunches.contains(bunch)
                        ) {
                            bestValue = token.result!!
                            bestIteration = i
                            bestIndex = token.variable.index
                        }
                        if (token.result != null) {
                            if ((token.result!! geq bar)
                                && !fixedBunches.contains(bunch)
                            ) {
                                ret.add(bunch)
                                xi[token.variable.index].range.eq(true)
                            }
                            if (withFixNot
                                && token.result!! leq (Flt64.one - bar)
                                && !fixedBunches.contains(bunch)
                            ) {
                                xi[token.variable.index].range.eq(false)
                            }
                        }
                    }
                }
            }
        }

        // if not fix any one bunch or cancel any task / 如果未固定任何任务束或取消任何任务
        // fix the best if the value greater than 1 - bar / 如果最佳值大于 1 - bar 则固定最佳项
        if (flag && ret.isEmpty() && (bestValue geq (Flt64.one - bar))) {
            val xi = compilation.x[bestIteration.toInt()][bestIndex]
            ret.add(bunchesIteration[bestIteration.toInt()][bestIndex])
            xi.range.eq(true)
        }

        return Ok(ret)
    }

    /**
     * 记录结果 / Log result
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 操作结果 / Operation result
    */
    open fun logResult(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        for (token in model.tokens.tokens) {
            if (token.result!! gr Flt64.zero) {
                logger.debug { "${token.name} = ${token.result!!}" }
            }
        }

        for (obj in model.subObjects) {
            logger.debug { "${obj.name} = ${obj.evaluate()}" }
        }

        return ok
    }

    /**
     * 记录任务束成本 / Log bunch cost
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 操作结果 / Operation result
    */
    open fun logBunchCost(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        for (token in model.tokens.tokens) {
            if ((token.result!! eq Flt64.one) && token.name.startsWith("x")) {
                for (i in UInt64.zero..iteration) {
                    val xi = compilation.x[i.toInt()]

                    if (token.variable belongsTo xi) {
                        val bunch = bunchesIteration[i.toInt()][token.variable.index]
                        logger.debug { "${bunch.executor} cost: ${bunch.cost.costSum!!.value}" }
                        break
                    }
                }
            }
        }

        return ok
    }

    /**
     * 刷新变量范围 / Flush variable ranges
     *
     * @param iteration 迭代次数 / Iteration count
     * @param tasks 任务列表 / List of tasks
     * @param lockCancelTasks 锁定取消任务集合 / Set of locked cancel tasks
     * @return 操作结果 / Operation result
    */
    fun flush(
        iteration: UInt64,
        tasks: List<T>,
        lockCancelTasks: Set<T> = emptySet()
    ): Try {
        val y = compilation.y
        for (task in tasks) {
            if (task.cancelEnabled && when (task) {
                    is AbstractPlannedTask<*, *, *> -> {
                        !lockCancelTasks.any { (it as AbstractPlannedTask<*, *, *>).plan == task.plan }
                    }

                    else -> {
                        true
                    }
                }
            ) {
                y[task].range.set(ValueRange(Binary.minimum, Binary.maximum).value!!)
            }
        }
        for (i in UInt64.zero..iteration) {
            val xi = compilation.x[i.toInt()]

            for (bunch in bunchesIteration[i.toInt()]) {
                if (!removedBunches.contains(bunch)) {
                    xi[bunch].range.set(ValueRange(Binary.minimum, Binary.maximum).value!!)
                }
            }
        }
        return ok
    }

/**
 * Extracts bunches matching the given predicate from the solved model.
 * 从已求解的模型中提取满足给定谓词的任务束。
 * @param iteration Iteration number / 迭代编号
 * @param model Solved linear meta model / 已求解的线性元模型
 * @param predicate Filter predicate on the token result value / 对令牌结果值的过滤谓词
 * @return Map of matching bunches to their solution values / 匹配的任务束到其解值的映射
*/
    private fun extractBunches(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>,
        predicate: (Flt64) -> Boolean
    ): Ret<Map<B, Flt64>> {
        val ret = HashMap<B, Flt64>()
        for (token in model.tokens.tokens) {
            if (!predicate(token.result!!)) {
                continue
            }

            for (i in 0..iteration.toInt()) {
                val xi = compilation.x[i]

                if (token belongsTo xi) {
                    val bunch = bunchesIteration[i][token.variable.index]
                    assert(!removedBunches.contains(bunch))
                    ret[bunch] = token.result!!
                }
            }
        }
        return Ok(ret)
    }
}

/**
 * 任务束编译聚合 / Bunch compilation aggregation
 *
 * @param B 任务束类型 / Bunch type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param tasks 任务列表 / List of tasks
 * @param executors 执行器列表 / List of executors
 * @param lockCancelTasks 锁定取消任务集合 / Set of locked cancel tasks
 * @param withExecutorLeisure 是否包含执行器空闲 / Whether to include executor leisure
*/
open class BunchCompilationAggregation<
        B : AbstractTaskBunch<T, E, A, V>,
        V : RealNumber<V>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    tasks: List<T>,
    executors: List<E>,
    lockCancelTasks: Set<T> = emptySet(),
    withExecutorLeisure: Boolean = true
) : AbstractBunchCompilationAggregation<B, V, T, E, A>(
    tasks = tasks,
    executors = executors,
    lockCancelTasks = lockCancelTasks,
    withExecutorLeisure = withExecutorLeisure
)

/**
 * 带时间的任务束编译聚合 / Bunch compilation aggregation with time
 *
 * @param B 任务束类型 / Bunch type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param timeWindow 时间窗口 / Time window
 * @param tasks 任务列表 / List of tasks
 * @param executors 执行器列表 / List of executors
 * @param lockCancelTasks 锁定取消任务集合 / Set of locked cancel tasks
 * @param withExecutorLeisure 是否包含执行器空闲 / Whether to include executor leisure
 * @param redundancyRange 冗余范围 / Redundancy range
 * @param makespanExtra 是否额外计算完工时间 / Whether to compute makespan extra
*/
open class BunchCompilationAggregationWithTime<
        B : AbstractTaskBunch<T, E, A, V>,
        V : RealNumber<V>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    timeWindow: TimeWindow<*>,
    tasks: List<T>,
    executors: List<E>,
    lockCancelTasks: Set<T> = emptySet(),
    withExecutorLeisure: Boolean = true,
    redundancyRange: Duration? = null,
    makespanExtra: Boolean = false
) : AbstractBunchCompilationAggregation<B, V, T, E, A>(
    tasks = tasks,
    executors = executors,
    lockCancelTasks = lockCancelTasks,
    withExecutorLeisure = withExecutorLeisure
) {
    private val timeBoundary = SolverTimeWindowBoundary(timeWindow.toFlt64Boundary())

    val taskTime: BunchSchedulingTaskTime<B, V, T, E, A> = BunchSchedulingTaskTime(
        timeBoundary = timeBoundary,
        tasks = tasks,
        compilation = compilation,
        redundancyRange = redundancyRange
    )

    /**
     * 通过 solver 时间窗口边界创建带时间的任务束编译聚合 /
     * Create bunch compilation aggregation with time from a solver time-window boundary
     *
     * @param timeBoundary solver 时间窗口边界 / Solver time-window boundary
     * @param tasks 任务列表 / List of tasks
     * @param executors 执行器列表 / List of executors
     * @param lockCancelTasks 锁定取消任务集合 / Set of locked cancel tasks
     * @param withExecutorLeisure 是否包含执行器空闲 / Whether to include executor leisure
     * @param redundancyRange 冗余范围 / Redundancy range
     * @param makespanExtra 是否额外计算完工时间 / Whether to compute makespan extra
    */
    constructor(
        timeBoundary: SolverTimeWindowBoundary,
        tasks: List<T>,
        executors: List<E>,
        lockCancelTasks: Set<T> = emptySet(),
        withExecutorLeisure: Boolean = true,
        redundancyRange: Duration? = null,
        makespanExtra: Boolean = false
    ) : this(
        timeWindow = timeBoundary.source,
        tasks = tasks,
        executors = executors,
        lockCancelTasks = lockCancelTasks,
        withExecutorLeisure = withExecutorLeisure,
        redundancyRange = redundancyRange,
        makespanExtra = makespanExtra
    )

    val makespan: Makespan<T, E, A> = Makespan(
        tasks = tasks,
        taskTime = taskTime,
        extra = makespanExtra
    )

    /**
     * 注册到模型 / Register to model
     *
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
    */
    override fun register(model: MetaModel<Flt64>): Try {
        when (val result = super.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = taskTime.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = makespan.register(model)) {
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
     * 添加列 / Add columns
     *
     * @param iteration 迭代次数 / Iteration count
     * @param newBunches 新任务束列表 / List of new bunches
     * @param model 线性元模型 / Linear meta model
     * @return 去重后的任务束列表 / Deduplicated bunch list
    */
    override suspend fun addColumns(
        iteration: UInt64,
        newBunches: List<B>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<B>> {
        val unduplicatedBunches = when (val result = super.addColumns(
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

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = taskTime.addColumns(
            iteration = iteration,
            bunches = unduplicatedBunches,
            model = model
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

        return Ok(unduplicatedBunches)
    }
}
