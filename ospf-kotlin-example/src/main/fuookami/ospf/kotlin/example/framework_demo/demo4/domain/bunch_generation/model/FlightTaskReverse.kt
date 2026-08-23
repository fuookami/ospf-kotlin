@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** Manages reversible flight task pairs for order change operations.
 * 管理用于顺序变更操作的可反转航班任务对。
 *
 * @property symmetricalPairs 对称可反转对列表 / List of symmetrical reversible pairs
 * @property leftMapper 任务键到以该任务为前序的可反转对的映射 / Map from task key to reversible pairs where the task is the predecessor
 * @property rightMapper 任务键到以该任务为后续的可反转对的映射 / Map from task key to reversible pairs where the task is the successor
*/
class FlightTaskReverse private constructor(
    private val symmetricalPairs: List<ReversiblePair> = ArrayList(),
    private val leftMapper: Map<TaskKey, List<ReversiblePair>>,
    private val rightMapper: Map<TaskKey, List<ReversiblePair>>
) {

    /**
     * A pair of tasks that can be reversed.
     * 可以反转的一对任务。
     *
     * @property prevTask 可反转对中的前序任务 / The predecessor task in the reversible pair
     * @property succTask 可反转对中的后续任务 / The successor task in the reversible pair
     * @property symmetrical 该对是否对称（可以双向反转） / Whether the pair is symmetrical (can be reversed in both directions)
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
         * 从任务对列表创建 FlightTaskReverse。
         *
         * @param pairs 要设为可反转的航班任务对列表 / List of flight task pairs to make reversible
         * @param originBunches 原始航班任务束列表 / The original flight task bunches
         * @param lock 包含锁定时间信息的锁 / The lock containing locked time information
         * @param timeDifferenceLimit 反转允许的最大时间差 / The maximum time difference allowed for reversal
         * @return 新的 FlightTaskReverse 实例 / A new FlightTaskReverse instance
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
         * 检查两个任务是否可以反转。
         *
         * @param prevFlightTask 前序航班任务 / The predecessor flight task
         * @param succFlightTask 后续航班任务 / The successor flight task
         * @param lock 包含锁定时间信息的锁 / The lock containing locked time information
         * @param timeDifferenceLimit 反转允许的最大时间差 / The maximum time difference allowed for reversal
         * @return 如果两个任务可以反转则为 true / true if the two tasks can be reversed
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
         * 检查两个任务是否对称（可以在两个方向上反转）。
         *
         * @param prevFlightTask 前序航班任务 / The predecessor flight task
         * @param succFlightTask 后续航班任务 / The successor flight task
         * @param lock 包含锁定时间信息的锁 / The lock containing locked time information
         * @param timeDifferenceLimit 反转允许的最大时间差 / The maximum time difference allowed for reversal
         * @return 如果两个任务对称则为 true / true if the two tasks are symmetrical
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

/**
 * Checks whether two tasks are symmetrical considering their presence in the original bunches.
 * 考虑任务在原始束中的存在情况，检查两个任务是否对称。
 * @param originBunches 要搜索的原始航班任务束列表 / The original flight task bunches to search within
 * @param prevFlightTask 前序航班任务 / The predecessor flight task
 * @param succFlightTask 后续航班任务 / The successor flight task
 * @param lock 包含锁定时间信息的锁 / The lock containing locked time information
 * @param timeDifferenceLimit 反转允许的最大时间差 / The maximum time difference allowed for reversal
 * @return 如果两个任务出现在同一原始束中且前序任务的到达机场与后续任务的出发机场匹配则为 true / true if both tasks appear in the same original bunch and the arrival airport of the predecessor matches the departure airport of the successor
*/
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
     * 检查一对任务是否可以反转。
     *
     * @param prevFlightTask 前序航班任务 / The predecessor flight task
     * @param succFlightTask 后续航班任务 / The successor flight task
     * @return 如果该对可以反转则为 true / true if the pair can be reversed
    */
    fun contains(prevFlightTask: FlightTask, succFlightTask: FlightTask): Boolean {
        return leftMapper[prevFlightTask.key]?.any { it.succTask == succFlightTask } ?: false
    }

    /**
     * Checks if a pair of tasks are symmetrical.
     * 检查一对任务是否对称。
     *
     * @param prevFlightTask 前序航班任务 / The predecessor flight task
     * @param succFlightTask 后续航班任务 / The successor flight task
     * @return 如果该对对称则为 true / true if the pair is symmetrical
    */
    fun symmetrical(prevFlightTask: FlightTask, succFlightTask: FlightTask): Boolean {
        return leftMapper[prevFlightTask.key]?.find { it.succTask == succFlightTask.originTask }?.symmetrical ?: false
    }

    /**
     * Finds all reversible pairs where the given task is the predecessor.
     * 查找给定任务作为前序任务的所有可反转对。
     *
     * @param flightTask 要作为前序任务搜索的航班任务 / The flight task to search as predecessor
     * @return 该任务作为前序任务的可反转对列表 / List of reversible pairs where the task is the predecessor
    */
    fun leftFind(flightTask: FlightTask): List<ReversiblePair> {
        return leftMapper[flightTask.key] ?: emptyList()
    }

    /**
     * Finds all reversible pairs where the given task is the successor.
     * 查找给定任务作为后续任务的所有可反转对。
     *
     * @param flightTask 要作为后续任务搜索的航班任务 / The flight task to search as successor
     * @return 该任务作为后续任务的可反转对列表 / List of reversible pairs where the task is the successor
    */
    fun rightFind(flightTask: FlightTask): List<ReversiblePair> {
        return rightMapper[flightTask.key] ?: emptyList()
    }
}
