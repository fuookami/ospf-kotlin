package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

open class AbstractTaskBunch<
    out T : AbstractTask<E, A>, 
    out E : Executor,
    out A : AssignmentPolicy<E>
> internal constructor(
    open val executor: E,
    val time: TimeRange,
    open val tasks: List<T>,
    val cost: Cost,
    open val initialUsability: ExecutorInitialUsability<T, E, A>,
    val iteration: Int64 = Int64(-1)
) : ManualIndexed(), Eq<AbstractTaskBunch<@UnsafeVariance T, @UnsafeVariance E, @UnsafeVariance A>> {
    companion object {
        val originIteration = UInt64.maximum
    }

    constructor(
        executor: E,
        time: Instant,
        initialUsability: ExecutorInitialUsability<T, E, A>,
        iteration: Int64 = Int64(-1)
    ) : this(
        executor = executor,
        time = TimeRange(time, Instant.DISTANT_FUTURE),
        tasks = emptyList(),
        cost = Cost(),
        initialUsability = initialUsability,
        iteration = iteration,
    )

    constructor(
        executor: E,
        initialUsability: ExecutorInitialUsability<T, E, A>,
        tasks: List<T>,
        cost: Cost = Cost(),
        iteration: Int64 = Int64(-1)
    ) : this(
        executor = executor,
        time = TimeRange(tasks.first().time!!.start, tasks.last().time!!.end),
        tasks = tasks,
        cost = cost,
        initialUsability = initialUsability,
        iteration = iteration
    )

    open val size get() = tasks.size
    open val empty get() = tasks.isEmpty()
    open val lastTask get() = initialUsability.lastTask

    open val costDensity by lazy {
        (cost.sum ?: Flt64.zero) / Flt64(size.toDouble())
    }

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
            task.duration!! + task.connectionTime(task.executor!!, prevTask, succTask)
        }
    }

    open val totalDelay: Duration by lazy {
        tasks.sumOf { it.delay }
    }

    open val totalAdvance: Duration by lazy {
        tasks.sumOf { it.advance }
    }

    open val executorChange: UInt64 by lazy {
        UInt64(tasks.count { it.executorChanged }.toULong())
    }

    open val keys: Map<TaskKey, Int> by lazy {
        tasks.withIndex().associate {
            it.value.key to it.index
        }
    }

    open val makespan: Instant by lazy {
        tasks
            .mapNotNull { it.time?.end }
            .maxOrNull()
            ?: initialUsability.enabledTime
    }

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

    override fun partialEq(rhs: AbstractTaskBunch<@UnsafeVariance T, @UnsafeVariance E, @UnsafeVariance A>): Boolean? {
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

typealias TaskBunch<E, A> = AbstractTaskBunch<Task<*, E>, E, A>
