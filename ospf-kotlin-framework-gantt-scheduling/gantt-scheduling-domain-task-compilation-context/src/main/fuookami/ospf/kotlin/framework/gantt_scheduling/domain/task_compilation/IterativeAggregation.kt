@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 迭代任务编译聚合 / Iterative task compilation aggregation */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.core.variable.Binary
import fuookami.ospf.kotlin.core.variable.eq
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.IterativeTaskCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.IterativeTaskSchedulingTaskTime
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.Makespan
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.SolverTimeWindowBoundary
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import kotlin.time.Duration

private fun nextReducedCostCutoff(maximumReducedCost: Flt64): Flt64 {
    val reducedCostCutoff = maximumReducedCost.floor().toInt64() * Int64(2L) / Int64(3L)
    return max(Flt64(reducedCostCutoff.toLong().toDouble()), Flt64(5.0))
}

/**
 * 抽象迭代任务编译聚合 / Abstract iterative task compilation aggregation
 *
 * @param IT 迭代任务类型 / Iterative task type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param tasks 任务列表 / List of tasks
 * @param executors 执行器列表 / List of executors
 * @param lockedCancelTasks 锁定取消任务集合 / Set of locked cancel tasks
 */
abstract class AbstractIterativeTaskCompilationAggregation<
        V : RealNumber<V>,
        IT : IterativeAbstractTask<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    tasks: List<T>,
    executors: List<E>,
    lockedCancelTasks: Set<T> = emptySet()
) {
    /**
     * 策略 / Policy
     *
     * @param IT 迭代任务类型 / Iterative task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param cost 成本函数 / Cost function
     * @param conflict 冲突函数 / Conflict function
     */
    data class Policy<
            IT : IterativeAbstractTask<E, A>,
            V : RealNumber<V>,
            out E : Executor,
            out A : AssignmentPolicy<E>
            >(
        val cost: (IT) -> Cost<V>,
        val conflict: (IT, IT) -> Boolean
    )

    private val logger = org.apache.logging.log4j.kotlin.logger("IterativeTaskSchedulingAggregation")

    val compilation: IterativeTaskCompilation<IT, T, E, A> = IterativeTaskCompilation(
        originTasks = tasks,
        executors = executors,
        lockedCancelTasks = lockedCancelTasks
    )
    abstract val policy: Policy<IT, V, E, A>

    val tasksIteration: List<List<IT>> by compilation::tasksIteration
    val tasks: List<IT> by compilation::tasks
    val removedTasks: Set<IT> by compilation::removedTasks
    val lastIterationTasks: List<IT> by compilation::lastIterationTasks

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
     * @param newTasks 新任务列表 / List of new tasks
     * @param model 线性元模型 / Linear meta model
     * @return 去重后的任务列表 / Deduplicated task list
     */
    open suspend fun addColumns(
        iteration: UInt64,
        newTasks: List<IT>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<IT>> {
        val unduplicatedTasks = when (val result = compilation.addColumns(
            iteration = iteration,
            newTasks = newTasks,
            model = model,
            cost = policy.cost,
            conflict = policy.conflict
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

        return Ok(unduplicatedTasks)
    }

    /**
     * 移除列 / Remove columns
     *
     * @param maximumReducedCost 最大约简成本 / Maximum reduced cost
     * @param maximumColumnAmount 最大列数 / Maximum column amount
     * @param reducedCost 约简成本函数 / Reduced cost function
     * @param fixedTasks 固定任务集合 / Set of fixed tasks
     * @param keptTasks 保留任务集合 / Set of kept tasks
     * @param model 线性元模型 / Linear meta model
     * @return 更新后的最大约简成本 / Updated maximum reduced cost
     */
    open fun removeColumns(
        maximumReducedCost: Flt64,
        maximumColumnAmount: UInt64,
        reducedCost: (IT) -> Flt64,
        fixedTasks: Set<IT>,
        keptTasks: Set<IT>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Flt64> {
        for (task in tasks) {
            if (removedTasks.contains(task)) {
                continue
            }

            if (!(reducedCost(task) ls maximumReducedCost)
                && !fixedTasks.contains(task)
                && !keptTasks.contains(task)
            ) {
                compilation.aggregation.removeColumn(task)
            }
        }

        for (task in compilation.removedTasks) {
            val xi = compilation.x[task.iteration.toInt()]
            xi[task].range.eq(false)
            model.remove(xi[task])
        }

        val remainingAmount = UInt64(tasks.size.toULong())
        return if (remainingAmount > maximumColumnAmount) {
            Ok(nextReducedCostCutoff(maximumReducedCost))
        } else {
            Ok(maximumReducedCost)
        }
    }

    /**
     * 提取固定任务 / Extract fixed tasks
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 固定任务集合 / Set of fixed tasks
     */
    open fun extractFixedTasks(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Set<IT>> {
        return extractTasks(iteration, model) { it eq Flt64.one }
    }

    /**
     * 提取保留任务 / Extract kept tasks
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 保留任务集合 / Set of kept tasks
     */
    open fun extractKeptTasks(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Set<IT>> {
        return extractTasks(iteration, model) { it gr Flt64.zero }
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
            if (token.belongsTo(z)) {
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
     * @param fixedTasks 固定任务集合 / Set of fixed tasks
     * @return 操作结果 / Operation result
     */
    open fun globallyFix(
        fixedTasks: Set<IT>
    ): Try {
        for (task in fixedTasks) {
            assert(!removedTasks.contains(task))
            val xi = compilation.x[task.iteration.toInt()]
            logger.debug { "globally fix: ${xi[task]}" }
            xi[task].range.eq(true)
        }
        return ok
    }

    /**
     * 局部固定 / Locally fix
     *
     * @param iteration 迭代次数 / Iteration count
     * @param bar 阈值 / Threshold
     * @param fixedTasks 固定任务集合 / Set of fixed tasks
     * @param model 线性元模型 / Linear meta model
     * @return 新固定的任务集合 / Set of newly fixed tasks
     */
    open fun locallyFix(
        iteration: UInt64,
        bar: Flt64,
        fixedTasks: Set<IT>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Set<IT>> {
        var flag = true
        val ret = HashSet<IT>()

        var bestValue = Flt64.zero
        var bestIteration = UInt64.zero
        var bestIndex = 0

        val y = compilation.y
        for (token in model.tokens.tokens) {
            if (token.belongsTo(y) && (token.result!! gr bar)) {
                logger.debug { "locally fix: ${y[token.variable.index]}" }
                y[token.variable.index].range.eq(true)
                flag = false
            }

            for (i in UInt64.zero..iteration) {
                if (token.belongsTo(compilation.x[i.toInt()])) {
                    val xi = compilation.x[i.toInt()]

                    val task = tasksIteration[i.toInt()][token.variable.index]
                    assert(!removedTasks.contains(task))

                    if ((token.result != null)
                        && (token.result!! geq bestValue)
                        && !fixedTasks.contains(task)
                    ) {
                        bestValue = token.result!!
                        bestIteration = i
                        bestIndex = token.variable.index
                    }
                    if ((token.result != null)
                        && (token.result!! geq bar)
                        && !fixedTasks.contains(task)
                    ) {
                        ret.add(task)
                        logger.debug { "locally fix: ${xi[token.variable.index]}" }
                        xi[token.variable.index].range.eq(true)
                    }
                }
            }
        }

        // if not fix any one bunch or cancel any task / 如果未固定任何任务束或取消任何任务
        // fix the best if the value greater than 1e-3 / 如果最佳值大于 1e-3 则固定最佳项
        if (flag && ret.isEmpty() && (bestValue geq Flt64(1e-3))) {
            val xi = compilation.x[bestIteration.toInt()][bestIndex]
            ret.add(tasksIteration[bestIteration.toInt()][bestIndex])
            logger.debug { "locally fix: $xi" }
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
     * 记录任务成本 / Log task cost
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 操作结果 / Operation result
     */
    open fun logTaskCost(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        for (token in model.tokens.tokens) {
            if ((token.result!! eq Flt64.one) && token.name.startsWith("x")) {
                for (i in UInt64.zero..iteration) {
                    val xi = compilation.x[i.toInt()]

                    if (token.variable.belongsTo(xi)) {
                        val task = tasksIteration[i.toInt()][token.variable.index]
                        logger.debug { "${task.executor} cost: ${policy.cost(task).costSum!!.value}" }
                        break
                    }
                }
            }
        }

        return ok
    }

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

            for (task in tasksIteration[i.toInt()]) {
                if (!removedTasks.contains(task)) {
                    xi[task].range.set(ValueRange(Binary.minimum, Binary.maximum).value!!)
                }
            }
        }
        return ok
    }

    private fun extractTasks(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>,
        predicate: (Flt64) -> Boolean
    ): Ret<Set<IT>> {
        val ret = HashSet<IT>()
        for (token in model.tokens.tokens) {
            if (!predicate(token.result!!)) {
                continue
            }

            for (i in 0..iteration.toInt()) {
                val xi = compilation.x[i]

                if (token.belongsTo(xi)) {
                    val task = tasksIteration[i][token.variable.index]
                    assert(!removedTasks.contains(task))
                    ret.add(task)
                }
            }
        }
        return Ok(ret)
    }
}

open class IterativeTaskCompilationAggregation<
        V : RealNumber<V>,
        IT : IterativeAbstractTask<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    tasks: List<T>,
    executors: List<E>,
    override val policy: Policy<IT, V, E, A>,
    lockedCancelTasks: Set<T> = emptySet()
) : AbstractIterativeTaskCompilationAggregation<V, IT, T, E, A>(
    tasks = tasks,
    executors = executors,
    lockedCancelTasks = lockedCancelTasks
)

open class IterativeTaskCompilationAggregationWithTime<
        V : RealNumber<V>,
        IT : IterativeAbstractTask<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    timeWindow: TimeWindow<Flt64>,
    tasks: List<T>,
    executors: List<E>,
    override val policy: Policy<IT, V, E, A>,
    lockCancelTasks: Set<T> = emptySet(),
    redundancyRange: Duration? = null,
    makespanExtra: Boolean = false
) : AbstractIterativeTaskCompilationAggregation<V, IT, T, E, A>(
    tasks = tasks,
    executors = executors,
    lockedCancelTasks = lockCancelTasks
) {
    private val timeBoundary = SolverTimeWindowBoundary(timeWindow)

    val taskTime: IterativeTaskSchedulingTaskTime<IT, T, E, A> = IterativeTaskSchedulingTaskTime(
        timeBoundary = timeBoundary,
        tasks = tasks,
        compilation = compilation,
        redundancyRange = redundancyRange
    )

    /**
     * 通过 solver 时间窗口边界创建带时间的迭代任务编译聚合 /
     * Create iterative task compilation aggregation with time from a solver time-window boundary
     *
     * @param timeBoundary solver 时间窗口边界 / Solver time-window boundary
     * @param tasks 任务列表 / List of tasks
     * @param executors 执行器列表 / List of executors
     * @param policy 策略 / Policy
     * @param lockCancelTasks 锁定取消任务集合 / Set of locked cancel tasks
     * @param redundancyRange 冗余范围 / Redundancy range
     * @param makespanExtra 是否额外计算完工时间 / Whether to compute makespan extra
     */
    constructor(
        timeBoundary: SolverTimeWindowBoundary,
        tasks: List<T>,
        executors: List<E>,
        policy: Policy<IT, V, E, A>,
        lockCancelTasks: Set<T> = emptySet(),
        redundancyRange: Duration? = null,
        makespanExtra: Boolean = false
    ) : this(
        timeWindow = timeBoundary.source,
        tasks = tasks,
        executors = executors,
        policy = policy,
        lockCancelTasks = lockCancelTasks,
        redundancyRange = redundancyRange,
        makespanExtra = makespanExtra
    )

    val makespan: Makespan<T, E, A> = Makespan(
        tasks = tasks,
        taskTime = taskTime,
        extra = makespanExtra
    )

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

    override suspend fun addColumns(
        iteration: UInt64,
        newTasks: List<IT>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<IT>> {
        val unduplicatedBunches = when (val result = super.addColumns(
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

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = taskTime.addColumns(
            iteration = iteration,
            newTasks = unduplicatedBunches,
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
