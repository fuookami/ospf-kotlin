@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** 管理用于顺序变更操作的可反转航班任务对。Manages reversible flight task pairs for order change operations. */
class FlightTaskReverse private constructor(
    private val symmetricalPairs: List<ReversiblePair> = ArrayList(),
    private val leftMapper: Map<TaskKey, List<ReversiblePair>>,
    private val rightMapper: Map<TaskKey, List<ReversiblePair>>
) {
    /**
     * 可以反转的一对任务。A pair of tasks that can be reversed.
     *
     * @property prevTask 参数。
     * @property succTask 参数。
     * @property symmetrical 参数。
     */
    data class ReversiblePair(
        val prevTask: FlightTask,
        val succTask: FlightTask,
        val symmetrical: Boolean
    )

    companion object {
        val defaultTimeDifferenceLimit = 5.hours
        val criticalSize = UInt64(200UL)

        /**
         * Creates a FlightTaskReverse from a list of task pairs.
 *
         * @param pairs 参数。
         * @param originBunches 参数。
         * @param lock 参数。
         * @param timeDifferenceLimit 参数。
         * @return 返回结果。
         */
        operator fun invoke(
            pairs: List<Pair<FlightTask, FlightTask>>,
            originBunches: List<FlightTaskBunch>,
            lock: Lock,
            timeDifferenceLimit: Duration
        ): FlightTaskReverse {
            val symmetricalPairs = ArrayList<ReversiblePair>()
            val leftMapper = HashMap<TaskKey, ArrayList<ReversiblePair>>()
            val rightMapper = HashMap<TaskKey, ArrayList<ReversiblePair>>()

            for (pair in pairs) {
                assert(reverseEnabled(pair.first, pair.second, lock, timeDifferenceLimit))

                val reversiblePair = ReversiblePair(
                    pair.first,
                    pair.second,
                    symmetrical(originBunches, pair.first, pair.second, lock, timeDifferenceLimit)
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
            return FlightTaskReverse(
                symmetricalPairs = symmetricalPairs,
                leftMapper = leftMapper,
                rightMapper = rightMapper
            )
        }

        /**
         * Checks if two tasks can be reversed.
 *
         * @param prevFlightTask 参数。
         * @param succFlightTask 参数。
         * @param lock 参数。
         * @param timeDifferenceLimit 参数。
         * @return 返回结果。
         */
        fun reverseEnabled(
            prevFlightTask: FlightTask,
            succFlightTask: FlightTask,
            lock: Lock,
            timeDifferenceLimit: Duration
        ): Boolean {
            if (prevFlightTask.dep != succFlightTask.arr) {
                return false
            }
            if (!prevFlightTask.delayEnabled && !succFlightTask.advanceEnabled) {
                return false
            }
            if (lock.lockedTime(prevFlightTask) != null) {
                return false
            }

            val prevScheduledTime = prevFlightTask.scheduledTime
            val succScheduledTime = succFlightTask.scheduledTime
            if (prevScheduledTime != null && succScheduledTime != null
                && prevScheduledTime.start < succScheduledTime.start
                && (succScheduledTime.start - prevScheduledTime.start) <= timeDifferenceLimit
            ) {
                return true
            }

            val prevTimeWindow = prevFlightTask.timeWindow
            val succTimeWindow = succFlightTask.timeWindow
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
            val prevDuration = prevFlightTask.duration
            val succDuration = succFlightTask.duration
            if (prevTimeWindow != null && succTimeWindow != null
                && prevDuration != null && succDuration != null
                && (prevTimeWindow.end - prevDuration) <= (succTimeWindow.end - succDuration)
            ) {
                return true
            }

            return false
        }

        /**
         * Checks if two tasks are symmetrical (can be reversed in both directions).
 *
         * @param prevFlightTask 参数。
         * @param succFlightTask 参数。
         * @param lock 参数。
         * @param timeDifferenceLimit 参数。
         * @return 返回结果。
         */
        fun symmetrical(
            prevFlightTask: FlightTask,
            succFlightTask: FlightTask,
            lock: Lock,
            timeDifferenceLimit: Duration
        ): Boolean {
            return reverseEnabled(prevFlightTask, succFlightTask, lock, timeDifferenceLimit)
                    && prevFlightTask.aircraft == succFlightTask.aircraft
        }

        private fun symmetrical(
            originBunches: List<FlightTaskBunch>,
            prevFlightTask: FlightTask,
            succFlightTask: FlightTask,
            lock: Lock,
            timeDifferenceLimit: Duration
        ): Boolean {
            assert(reverseEnabled(prevFlightTask, succFlightTask, lock, timeDifferenceLimit))
            return originBunches.any { it.contains(prevFlightTask, succFlightTask) }
                    && prevFlightTask.arr == succFlightTask.dep
        }
    }

    /**
     * Checks if a pair of tasks can be reversed.
 *
     * @param prevFlightTask 参数。
     * @param succFlightTask 参数。
     * @return 返回结果。
     */
    fun contains(prevFlightTask: FlightTask, succFlightTask: FlightTask): Boolean {
        return leftMapper[prevFlightTask.key]?.any { it.succTask == succFlightTask } ?: false
    }

    /**
     * Checks if a pair of tasks are symmetrical.
 *
     * @param prevFlightTask 参数。
     * @param succFlightTask 参数。
     * @return 返回结果。
     */
    fun symmetrical(prevFlightTask: FlightTask, succFlightTask: FlightTask): Boolean {
        return leftMapper[prevFlightTask.key]?.find { it.succTask == succFlightTask.originTask }?.symmetrical ?: false
    }

    /**
     * Finds all reversible pairs where the given task is the predecessor.
 *
     * @param flightTask 参数。
     * @return 返回结果。
     */
    fun leftFind(flightTask: FlightTask): List<ReversiblePair> {
        return leftMapper[flightTask.key] ?: emptyList()
    }

    /**
     * Finds all reversible pairs where the given task is the successor.
 *
     * @param flightTask 参数。
     * @return 返回结果。
     */
    fun rightFind(flightTask: FlightTask): List<ReversiblePair> {
        return rightMapper[flightTask.key] ?: emptyList()
    }
}
