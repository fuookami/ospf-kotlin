package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import kotlin.time.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

open class TaskReverseBuilder<
    out B : AbstractTaskBunch<T, E, A>,
    out T : AbstractPlannedTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
> {
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
            assert(reverseEnabled(pair.first, pair.second, timeLockedTasks, timeDifferenceLimit))

            val reversiblePair = TaskReverse.ReversiblePair(
                pair.first,
                pair.second,
                symmetrical(originBunches, pair.first, pair.second, timeLockedTasks, timeDifferenceLimit)
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
        if (prevTimeWindow != null && succTimeWindow != null
            && prevDuration != null && succDuration != null
            && (prevTimeWindow.end - prevDuration) <= (succTimeWindow.end - succDuration)
        ) {
            return true
        }

        return false
    }

    open fun symmetrical(
        prevTask: @UnsafeVariance T,
        succTask: @UnsafeVariance T,
        timeLockedTasks: Set<@UnsafeVariance T> = emptySet(),
        timeDifferenceLimit: Duration = Duration.ZERO
    ): Boolean {
        return reverseEnabled(prevTask, succTask, timeLockedTasks, timeDifferenceLimit)
                && prevTask.executor == succTask.executor
    }

    protected open fun symmetrical(
        originBunches: List<@UnsafeVariance B>,
        prevTask: @UnsafeVariance T,
        succTask: @UnsafeVariance T,
        timeLockedTasks: Set<@UnsafeVariance T> = emptySet(),
        timeDifferenceLimit: Duration = Duration.ZERO
    ): Boolean {
        assert(reverseEnabled(prevTask, succTask, timeLockedTasks, timeDifferenceLimit))
        return originBunches.any { it.contains(prevTask, succTask) }
    }
}

class TaskReverse<
    out T : AbstractPlannedTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
> internal constructor(
    private val symmetricalPairs: List<ReversiblePair<T, E, A>> = ArrayList(),
    private val leftMapper: Map<TaskKey, List<ReversiblePair<T, E, A>>>,
    private val rightMapper: Map<TaskKey, List<ReversiblePair<T, E, A>>>
) {
    data class ReversiblePair<
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
    >(
        val prevTask: T,
        val succTask: T,
        val symmetrical: Boolean
    )

    fun contains(prevTask: @UnsafeVariance T, succTask: @UnsafeVariance T): Boolean {
        return leftMapper[prevTask.key]?.any { it.succTask == succTask } ?: false
    }

    fun symmetrical(prevTask: @UnsafeVariance T, succTask: @UnsafeVariance T): Boolean {
        return leftMapper[prevTask.key]?.find { it.succTask.plan == succTask.plan }?.symmetrical ?: false
    }

    fun leftFind(flightTask: @UnsafeVariance T): List<ReversiblePair<T, E, A>> {
        return leftMapper[flightTask.key] ?: emptyList()
    }

    fun rightFind(flightTask: @UnsafeVariance T): List<ReversiblePair<T, E, A>> {
        return rightMapper[flightTask.key] ?: emptyList()
    }
}
