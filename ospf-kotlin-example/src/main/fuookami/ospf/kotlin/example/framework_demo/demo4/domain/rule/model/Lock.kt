@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model

import kotlin.time.Instant
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.FlightTask

/**
 * 表示阻止航班任务修改的约束的锁定模型。Lock model representing constraints that prevent flight task modifications.
 *
 * @property private val lockedTasks 参数。
 */
class Lock(
    private val lockedTasks: Map<FlightTask, Instant> = emptyMap()
) {
    /**
     * Returns the locked time for the given flight task, or null if not locked.
 *
     * @param flightTask 参数。
     * @return 返回结果。
     */
    fun lockedTime(flightTask: FlightTask): Instant? {
        return lockedTasks[flightTask]
    }
}
