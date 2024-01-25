package fuookami.ospf.kotlin.framework.gantt_scheduling.cg.model

import kotlin.time.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*

open class TaskReverseBuilder<E : Executor> {
    operator fun invoke(
        pairs: List<Pair<Task<E>, Task<E>>>,
        originBunches: List<TaskBunch<E>>,
        timeLockedTasks: Set<Task<E>> = emptySet(),
        timeDifferenceLimit: Duration = Duration.ZERO
    ): TaskReverse<E> {
        val symmetricalPairs = ArrayList<TaskReverse.ReversiblePair<E>>()
        val leftMapper = HashMap<TaskKey, ArrayList<TaskReverse.ReversiblePair<E>>>()
        val rightMapper = HashMap<TaskKey, ArrayList<TaskReverse.ReversiblePair<E>>>()

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
        prevTask: Task<E>,
        succTask: Task<E>,
        timeLockedTasks: Set<Task<E>> = emptySet(),
        timeDifferenceLimit: Duration = Duration.ZERO
    ): Boolean {
        //    if (!prevTask.isFlight || !succTask.isFlight) {
        //        return false
        //    }
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
        prevTask: Task<E>,
        succTask: Task<E>,
        timeLockedTasks: Set<Task<E>> = emptySet(),
        timeDifferenceLimit: Duration = Duration.ZERO
    ): Boolean {
        return reverseEnabled(prevTask, succTask, timeLockedTasks, timeDifferenceLimit)
                && prevTask.executor == succTask.executor
    }

    protected open fun symmetrical(
        originBunches: List<TaskBunch<E>>,
        prevTask: Task<E>,
        succTask: Task<E>,
        timeLockedTasks: Set<Task<E>> = emptySet(),
        timeDifferenceLimit: Duration = Duration.ZERO
    ): Boolean {
        assert(reverseEnabled(prevTask, succTask, timeLockedTasks, timeDifferenceLimit))
        return originBunches.any { it.contains(prevTask, succTask) }
    }
}

class TaskReverse<E : Executor> internal constructor(
    private val symmetricalPairs: List<ReversiblePair<E>> = ArrayList(),
    private val leftMapper: Map<TaskKey, List<ReversiblePair<E>>>,
    private val rightMapper: Map<TaskKey, List<ReversiblePair<E>>>
) {
    data class ReversiblePair<E : Executor>(
        val prevTask: Task<E>,
        val succTask: Task<E>,
        val symmetrical: Boolean
    )

    fun contains(prevTask: Task<E>, succTask: Task<E>): Boolean {
        return leftMapper[prevTask.key]?.any { it.succTask == succTask } ?: false
    }

    fun symmetrical(prevTask: Task<E>, succTask: Task<E>): Boolean {
        return leftMapper[prevTask.key]?.find { it.succTask == succTask.originTask }?.symmetrical ?: false
    }

    fun leftFind(flightTask: Task<E>): List<ReversiblePair<E>> {
        return leftMapper[flightTask.key] ?: emptyList()
    }

    fun rightFind(flightTask: Task<E>): List<ReversiblePair<E>> {
        return rightMapper[flightTask.key] ?: emptyList()
    }
}
