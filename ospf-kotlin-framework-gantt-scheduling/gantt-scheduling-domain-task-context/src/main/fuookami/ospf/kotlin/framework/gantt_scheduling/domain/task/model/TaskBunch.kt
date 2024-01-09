package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.time.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrasturcutre.*

open class TaskBunch<E : Executor> internal constructor(
    val executor: E,
    val time: TimeRange,
    val tasks: List<Task<E>>,
    val iteration: UInt64,
    val cost: Cost,

    val ability: ExecutorUsability<E>
) : ManualIndexed() {
    val size by tasks::size
    val empty get() = tasks.isEmpty()
    val lastTask by ability::lastTask
    val costDensity = (cost.sum ?: Flt64.zero) / Flt64(size.toDouble())
    val busyTime: Duration
    val totalDelay: Duration
    val executorChange: UInt64
    val keys: Map<TaskKey, Int>
    val redundancy: Map<TaskKey, Pair<Duration, Duration>>

    companion object {
        val originIteration = UInt64.maximum
    }

    constructor(
        executor: E,
        time: Instant,
        ability: ExecutorUsability<E>,
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
        ability: ExecutorUsability<E>,
        tasks: List<Task<E>>,
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

    init {
        val taskKeys = HashMap<TaskKey, Int>()
        for ((index, task) in tasks.withIndex()) {
            taskKeys[task.key] = index
        }
        keys = taskKeys

        val taskTimeRedundancy = HashMap<TaskKey, Pair<Duration, Duration>>()
        if (tasks.isNotEmpty()) {
            var currentTaskTimeWindow = Pair(ability.enabledTime, tasks[0].latestNormalStartTime(executor))
            for (task in tasks) {
                // todo
            }
        }
        redundancy = taskTimeRedundancy

        var busyTime = Duration.ZERO
        for (i in tasks.indices) {
            busyTime += tasks[i].duration!!

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
            busyTime += tasks[i].connectionTime(executor, prevTask, succTask)
        }
        this.busyTime = busyTime
        this.executorChange = UInt64(tasks.count { it.executorChanged }.toULong())

        totalDelay = tasks.sumOf { it.delay.toLong(DurationUnit.MILLISECONDS) }.milliseconds
    }

    operator fun get(index: Int): Task<E> {
        return tasks[index]
    }

    fun contains(task: Task<E>): Boolean {
        return keys.contains(task.key)
    }

    fun contains(prev: Task<E>, succ: Task<E>): Boolean {
        val prevTask = keys[prev.key]
        val succTask = keys[succ.key]
        return if (prevTask != null && succTask != null) {
            (succTask - prevTask) == 1
        } else {
            false
        }
    }

    fun contains(taskPair: Pair<Task<E>, Task<E>>): Boolean {
        return contains(taskPair.first, taskPair.second)
    }

    fun get(originTask: Task<E>): Task<E>? {
        val task = keys[originTask.key]
        return if (task != null) {
            assert(tasks[task].originTask == originTask)
            tasks[task]
        } else {
            null
        }
    }
}
