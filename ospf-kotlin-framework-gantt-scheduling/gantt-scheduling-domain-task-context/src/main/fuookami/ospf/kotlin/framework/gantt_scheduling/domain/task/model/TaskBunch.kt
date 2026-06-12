@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 任务束模型，表示一组有序任务 / Task bunch model representing an ordered group of tasks
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.utils.functional.Eq
import fuookami.ospf.kotlin.utils.sumOf
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange

/**
 * 抽象任务束，表示分配给同一执行者的一组有序任务 / Abstract task bunch representing an ordered group of tasks assigned to the same executor
 *
 * @param T 任务类型 / The task type
 * @param E 执行者类型 / The executor type
 * @param A 分配策略类型 / The assignment policy type
 * @param V 数值类型 / The numeric type for cost values (defaults to Flt64 for backward compatibility)
 * @property executor 执行者 / The executor
 * @property time 时间范围 / The time range
 * @property tasks 任务列表 / The list of tasks
 * @property cost 成本 / The cost
 * @property initialUsability 初始可用性 / The initial usability
 * @property iteration 迭代次数 / The iteration number
 */
open class AbstractTaskBunch<
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>,
        V : RealNumber<V>
        > internal constructor(
    open val executor: E,
    val time: TimeRange,
    open val tasks: List<T>,
    val cost: Cost<V>,
    open val initialUsability: ExecutorInitialUsability<T, E, A>,
    val iteration: Int64 = Int64(-1)
) : ManualIndexed(), Eq<AbstractTaskBunch<@UnsafeVariance T, @UnsafeVariance E, @UnsafeVariance A, @UnsafeVariance V>> {
    companion object {
        val originIteration = UInt64.maximum

        // DISTANT_FUTURE replacement: represents a far future instant (year 9999)
        private val DISTANT_FUTURE: Instant = Instant.parse("9999-12-31T23:59:59.999999999Z")
    }

    constructor(
        executor: E,
        time: Instant,
        initialUsability: ExecutorInitialUsability<T, E, A>,
        iteration: Int64 = Int64(-1)
    ) : this(
        executor = executor,
        time = TimeRange(time, DISTANT_FUTURE),
        tasks = emptyList(),
        cost = ImmutableCost<V>(emptyList()),
        initialUsability = initialUsability,
        iteration = iteration,
    )

    constructor(
        executor: E,
        initialUsability: ExecutorInitialUsability<T, E, A>,
        tasks: List<T>,
        cost: Cost<V> = ImmutableCost<V>(emptyList()),
        iteration: Int64 = Int64(-1)
    ) : this(
        executor = executor,
        time = TimeRange(tasks.first().time!!.start, tasks.last().time!!.end),
        tasks = tasks,
        cost = cost,
        initialUsability = initialUsability,
        iteration = iteration
    )

    /** 任务数量 / Number of tasks */
    open val size get() = tasks.size
    /** 是否为空 / Whether the bunch is empty */
    open val empty get() = tasks.isEmpty()
    /** 上一个任务 / The last task */
    open val lastTask get() = initialUsability.lastTask

    /** 成本密度（成本/任务数）/ Cost density (cost/number of tasks) */
    open val costDensity by lazy {
        cost.solverCost(Flt64.zero) / Flt64(size.toDouble())
    }

    /** 忙碌时间 / Busy time */
    open val busyTime: Duration by lazy {
        tasks.withIndex().sumOf { (i, task) ->
            val prevTask = if (i > 0) {
                tasks[i - 1]
            } else {
                lastTask
            }
            val succTask = if (i != tasks.lastIndex) {
                tasks[i + 1]
            } else {
                null
            }
            task.duration!! + task.connectionTime(
                executor = task.executor!!,
                prevTask = prevTask,
                succTask = succTask
            )
        }
    }

    /** 总延迟时间 / Total delay time */
    open val totalDelay: Duration by lazy {
        tasks.sumOf { it.delay }
    }

    /** 总提前时间 / Total advance time */
    open val totalAdvance: Duration by lazy {
        tasks.sumOf { it.advance }
    }

    /** 执行者变更次数 / Number of executor changes */
    open val executorChange: UInt64 by lazy {
        UInt64(tasks.count { it.executorChanged }.toULong())
    }

    /** 任务键到索引的映射 / Mapping from task key to index */
    open val keys: Map<TaskKey, Int> by lazy {
        tasks.withIndex().associate {
            it.value.key to it.index
        }
    }

    /** 完工时间 / Makespan */
    open val makespan: Instant by lazy {
        tasks
            .mapNotNull { it.time?.end }
            .maxOrNull()
            ?: initialUsability.enabledTime
    }

    /** 任务连接对列表 / List of task connection pairs */
    open val connections: List<Pair<T?, T?>> by lazy {
        (1..tasks.size).map {
            when (it) {
                0 -> {
                    Pair(null, tasks[it])
                }

                tasks.size -> {
                    Pair(tasks[it - 1], null)
                }

                else -> {
                    Pair(tasks[it - 1], tasks[it])
                }
            }
        }
    }

    open operator fun get(index: Int): T {
        return tasks[index]
    }

    open fun contains(task: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>): Boolean {
        return keys.contains(task.key)
    }

    open fun contains(
        prev: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>,
        succ: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>
    ): Boolean {
        val prevTask = keys[prev.key]
        val succTask = keys[succ.key]
        return if (prevTask != null && succTask != null) {
            (succTask - prevTask) == 1
        } else {
            false
        }
    }

    open fun contains(
        taskPair: Pair<AbstractTask<@UnsafeVariance E, @UnsafeVariance A>, AbstractTask<@UnsafeVariance E, @UnsafeVariance A>>
    ): Boolean {
        return contains(taskPair.first, taskPair.second)
    }

    open fun get(originTask: AbstractTask<@UnsafeVariance E, @UnsafeVariance A>): T? {
        val task = keys[originTask.key]
        return if (task != null) {
            tasks[task]
        } else {
            null
        }
    }

    override fun partialEq(rhs: AbstractTaskBunch<@UnsafeVariance T, @UnsafeVariance E, @UnsafeVariance A, @UnsafeVariance V>): Boolean? {
        if (this === rhs) return true
        if (this::class != rhs::class) return false

        if (executor != rhs.executor) return false

        if (tasks.size != rhs.tasks.size) return false
        for (i in tasks.indices) {
            if (tasks[i] neq rhs.tasks[i]) {
                return false
            }
        }

        return true
    }
}
