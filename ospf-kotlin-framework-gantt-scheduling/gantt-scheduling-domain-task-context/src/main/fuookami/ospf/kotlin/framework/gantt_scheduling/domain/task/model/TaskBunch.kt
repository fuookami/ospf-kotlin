package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

open class AbstractTaskBunch<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> internal constructor(
    val executor: E,
    val time: TimeRange,
    val tasks: List<T>,
    val iteration: UInt64,
    val cost: Cost,
    val ability: ExecutorUsability<E, A>
) : ManualIndexed() {
    val size by tasks::size
    val empty get() = tasks.isEmpty()
    val lastTask by ability::lastTask
    val costDensity = (cost.sum ?: Flt64.zero) / Flt64(size.toDouble())
    val busyTime: Duration by lazy {
        tasks.foldIndexed(Duration.ZERO) { i, busyTime, task ->
            val prevTask = if (i > 0) {
                tasks[i - 1]
            } else {
                lastTask
            }
            val succTask = if (i != (tasks.size - 1)) {
                tasks[i + 1]
            } else {
                null
            }
            busyTime + task.duration!! + task.connectionTime(task.executor!!, prevTask, succTask)
        }
    }
    val totalDelay: Duration by lazy { tasks.fold(Duration.ZERO) { delay, task -> delay + task.delay } }
    val totalAdvance: Duration by lazy { tasks.fold(Duration.ZERO) { advance, task -> advance + task.advance } }
    val executorChange: UInt64 by lazy { UInt64(tasks.count { it.executorChanged }.toULong()) }
    val keys: Map<TaskKey, Int> by lazy { tasks.withIndex().associate { Pair(it.value.key, it.index) } }

    companion object {
        val originIteration = UInt64.maximum
    }

    constructor(
        executor: E,
        time: Instant,
        ability: ExecutorUsability<E, A>,
        iteration: UInt64
    ) : this(
        executor = executor,
        time = TimeRange(time, Instant.DISTANT_FUTURE),
        tasks = emptyList(),
        iteration = iteration,
        cost = Cost(),
        ability = ability
    )

    constructor(
        executor: E,
        ability: ExecutorUsability<E, A>,
        tasks: List<T>,
        iteration: UInt64,
        cost: Cost = Cost()
    ) : this(
        executor = executor,
        time = TimeRange(tasks.first().time!!.start, tasks.last().time!!.end),
        tasks = tasks,
        iteration = iteration,
        cost = cost,
        ability = ability
    )

    operator fun get(index: Int): T {
        return tasks[index]
    }

    fun contains(task: AbstractTask<E, A>): Boolean {
        return keys.contains(task.key)
    }

    fun contains(prev: AbstractTask<E, A>, succ: AbstractTask<E, A>): Boolean {
        val prevTask = keys[prev.key]
        val succTask = keys[succ.key]
        return if (prevTask != null && succTask != null) {
            (succTask - prevTask) == 1
        } else {
            false
        }
    }

    fun contains(taskPair: Pair<AbstractTask<E, A>, AbstractTask<E, A>>): Boolean {
        return contains(taskPair.first, taskPair.second)
    }

    fun get(originTask: AbstractTask<E, A>): T? {
        val task = keys[originTask.key]
        return if (task != null) {
            assert(tasks[task].plan == originTask.plan)
            tasks[task]
        } else {
            null
        }
    }
}

typealias TaskBunch<E, A> = AbstractTaskBunch<Task<E>, E, A>
