@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model

import kotlin.time.Instant
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.FlightTask

/**
 * 表示阻止航班任务修改的约束的锁定模型。Lock model representing constraints that prevent flight task modifications.
 *
 * @property lockedTasks The map of locked flight tasks to their lock times / 被锁定的航班任务及其锁定时间的映射
*/
class Lock(
    private val lockedTasks: Map<FlightTask, Instant> = emptyMap()
) {

    /**
     * 返回给定航班任务的锁定时间，如果未锁定则返回 null。Returns the locked time for the given flight task, or null if not locked.
     *
     * @param flightTask The flight task to query / 要查询的航班任务
     * @return The locked time for the task, or null if not locked / 任务的锁定时间，未锁定则返回 null
    */
    fun lockedTime(flightTask: FlightTask): Instant? {
        return lockedTasks[flightTask]
    }
}
