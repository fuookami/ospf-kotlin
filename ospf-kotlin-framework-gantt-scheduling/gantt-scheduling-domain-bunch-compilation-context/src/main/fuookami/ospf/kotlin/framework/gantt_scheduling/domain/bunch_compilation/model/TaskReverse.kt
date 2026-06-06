@file:Suppress("DEPRECATION")
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 任务反转构建器 / Task reverse builder
 *
 * @param B 任务束类型 / Bunch type
 * @param V 数值类型 / Numeric type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 */
open class TaskReverseBuilderV<
        out B : AbstractTaskBunch<T, E, A, V>,
        V : RealNumber<V>,
        out T : AbstractPlannedTask<*, E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        > {
    /**
     * 构建任务反转 / Build task reverse
     *
     * @param pairs 任务对列表 / List of task pairs
     * @param originBunches 原始任务束列表 / List of origin bunches
     * @param timeLockedTasks 时间锁定任务集合 / Set of time-locked tasks
     * @param timeDifferenceLimit 时间差限制 / Time difference limit
     * @return 任务反转对象 / Task reverse object
     */
    operator fun invoke(
        pairs: List<Pair<@UnsafeVariance T, @UnsafeVariance T>>,
        originBunches: List<@UnsafeVariance B>,
        timeLockedTasks: Set<@UnsafeVariance T> = emptySet(),
        timeDifferenceLimit: Duration = Duration.ZERO
    ): TaskReverse<T, E, A> {
        val symmetricalPairs = ArrayList<TaskReverse.ReversiblePair<T, E, A>>()
        val leftMapper = HashMap<TaskKey, ArrayList<TaskReverse.ReversiblePair<T, E, A>>>()
        val rightMapper = HashMap<TaskKey, ArrayList<TaskReverse.ReversiblePair<T, E, A>>>()

        for (pair in pairs) {
            assert(
                reverseEnabled(
                    prevTask = pair.first,
                    succTask = pair.second,
                    timeLockedTasks = timeLockedTasks,
                    timeDifferenceLimit = timeDifferenceLimit
                )
            )

            val reversiblePair = TaskReverse.ReversiblePair(
                prevTask = pair.first,
                succTask = pair.second,
                symmetrical = symmetrical(
                    originBunches = originBunches,
                    prevTask = pair.first,
                    succTask = pair.second,
                    timeLockedTasks = timeLockedTasks,
                    timeDifferenceLimit = timeDifferenceLimit
                )
            )
            if (!leftMapper.containsKey(pair.first.key)) {
                leftMapper[pair.first.key] = ArrayList()
            }
            leftMapper[pair.first.key]!!.add(reversiblePair)
            if (!rightMapper.containsKey(pair.second.key)) {
                rightMapper[pair.second.key] = ArrayList()
            }
            rightMapper[pair.second.key]!!.add(reversiblePair)

            if (reversiblePair.symmetrical) {
                symmetricalPairs.add(reversiblePair)
            }
        }
        return TaskReverse(
            symmetricalPairs = symmetricalPairs,
            leftMapper = leftMapper,
            rightMapper = rightMapper
        )
    }

    /**
     * 检查反转是否启用 / Check if reverse is enabled
     *
     * @param prevTask 前一个任务 / Previous task
     * @param succTask 后一个任务 / Successor task
     * @param timeLockedTasks 时间锁定任务集合 / Set of time-locked tasks
     * @param timeDifferenceLimit 时间差限制 / Time difference limit
     * @return 是否启用反转 / Whether reverse is enabled
     */
    open fun reverseEnabled(
        prevTask: @UnsafeVariance T,
        succTask: @UnsafeVariance T,
        timeLockedTasks: Set<@UnsafeVariance T> = emptySet(),
        timeDifferenceLimit: Duration = Duration.ZERO
    ): Boolean {
        if (!prevTask.delayEnabled && !succTask.advanceEnabled) {
            return false
        }
        if (!timeLockedTasks.contains(prevTask)) {
            return false
        }

        val prevScheduledTime = prevTask.scheduledTime
        val succScheduledTime = succTask.scheduledTime
        if (prevScheduledTime != null && succScheduledTime != null
            && prevScheduledTime.start < succScheduledTime.start
            && (succScheduledTime.start - prevScheduledTime.start) <= timeDifferenceLimit
        ) {
            return true
        }

        val prevTimeWindow = prevTask.timeWindow
        val succTimeWindow = succTask.timeWindow
        if (prevTimeWindow != null && succScheduledTime != null
            && prevTimeWindow.start < succScheduledTime.start
            && succScheduledTime.start < prevTimeWindow.end
        ) {
            return true
        }
        if (prevScheduledTime != null && succTimeWindow != null
            && prevScheduledTime.start < succTimeWindow.start
            && (succTimeWindow.start - prevScheduledTime.start) <= timeDifferenceLimit
        ) {
            return true
        }
        val prevDuration = prevTask.duration
        val succDuration = succTask.duration
        return (prevTimeWindow != null && succTimeWindow != null
                && prevDuration != null && succDuration != null
                && (prevTimeWindow.end - prevDuration) <= (succTimeWindow.end - succDuration))
    }

    /**
     * 检查是否对称 / Check if symmetrical
     *
     * @param prevTask 前一个任务 / Previous task
     * @param succTask 后一个任务 / Successor task
     * @param timeLockedTasks 时间锁定任务集合 / Set of time-locked tasks
     * @param timeDifferenceLimit 时间差限制 / Time difference limit
     * @return 是否对称 / Whether symmetrical
     */
    open fun symmetrical(
        prevTask: @UnsafeVariance T,
        succTask: @UnsafeVariance T,
        timeLockedTasks: Set<@UnsafeVariance T> = emptySet(),
        timeDifferenceLimit: Duration = Duration.ZERO
    ): Boolean {
        return reverseEnabled(
            prevTask = prevTask,
            succTask = succTask,
            timeLockedTasks = timeLockedTasks,
            timeDifferenceLimit = timeDifferenceLimit
        ) && prevTask.executor == succTask.executor
    }

    protected open fun symmetrical(
        originBunches: List<@UnsafeVariance B>,
        prevTask: @UnsafeVariance T,
        succTask: @UnsafeVariance T,
        timeLockedTasks: Set<@UnsafeVariance T> = emptySet(),
        timeDifferenceLimit: Duration = Duration.ZERO
    ): Boolean {
        assert(
            reverseEnabled(
                prevTask = prevTask,
                succTask = succTask,
                timeLockedTasks = timeLockedTasks,
                timeDifferenceLimit = timeDifferenceLimit
            )
        )
        return originBunches.any { it.contains(prevTask, succTask) }
    }
}

/**
 * 任务反转 / Task reverse
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param symmetricalPairs 对称任务对列表 / List of symmetrical pairs
 * @param leftMapper 左映射 / Left mapper
 * @param rightMapper 右映射 / Right mapper
 */
class TaskReverse<
        out T : AbstractPlannedTask<*, E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        > internal constructor(
    private val symmetricalPairs: List<ReversiblePair<T, E, A>> = ArrayList(),
    private val leftMapper: Map<TaskKey, List<ReversiblePair<T, E, A>>>,
    private val rightMapper: Map<TaskKey, List<ReversiblePair<T, E, A>>>
) {
    /**
     * 可反转任务对 / Reversible task pair
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param prevTask 前一个任务 / Previous task
     * @param succTask 后一个任务 / Successor task
     * @param symmetrical 是否对称 / Whether symmetrical
     */
    data class ReversiblePair<
            out T : AbstractTask<E, A>,
            out E : Executor,
            out A : AssignmentPolicy<E>
            >(
        val prevTask: T,
        val succTask: T,
        val symmetrical: Boolean
    )

    /**
     * 检查是否包含任务对 / Check if contains task pair
     *
     * @param prevTask 前一个任务 / Previous task
     * @param succTask 后一个任务 / Successor task
     * @return 是否包含 / Whether contains
     */
    fun contains(prevTask: @UnsafeVariance T, succTask: @UnsafeVariance T): Boolean {
        return leftMapper[prevTask.key]?.any { it.succTask == succTask } ?: false
    }

    /**
     * 检查任务对是否对称 / Check if task pair is symmetrical
     *
     * @param prevTask 前一个任务 / Previous task
     * @param succTask 后一个任务 / Successor task
     * @return 是否对称 / Whether symmetrical
     */
    fun symmetrical(prevTask: @UnsafeVariance T, succTask: @UnsafeVariance T): Boolean {
        return leftMapper[prevTask.key]?.find { it.succTask.plan == succTask.plan }?.symmetrical ?: false
    }

    /**
     * 左查找 / Left find
     *
     * @param flightTask 任务 / Task
     * @return 可反转任务对列表 / List of reversible pairs
     */
    fun leftFind(flightTask: @UnsafeVariance T): List<ReversiblePair<T, E, A>> {
        return leftMapper[flightTask.key] ?: emptyList()
    }

    /**
     * 右查找 / Right find
     *
     * @param flightTask 任务 / Task
     * @return 可反转任务对列表 / List of reversible pairs
     */
    fun rightFind(flightTask: @UnsafeVariance T): List<ReversiblePair<T, E, A>> {
        return rightMapper[flightTask.key] ?: emptyList()
    }
}

/** 向后兼容 typealias — Flt64 task reverse builder / Backward compat typealias */
typealias TaskReverseBuilder<B, T, E, A> = TaskReverseBuilderV<B, Flt64, T, E, A>

/** 向后兼容 typealias — Flt64 task reverse builder / Backward compat typealias */
typealias Flt64TaskReverseBuilder<B, T, E, A> = TaskReverseBuilderV<B, Flt64, T, E, A>
