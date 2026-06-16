@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model

import kotlin.time.Instant
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.FlightTask

/** Lock model representing constraints that prevent flight task modifications. */
class Lock(
    private val lockedTasks: Map<FlightTask, Instant> = emptyMap()
) {
    /** Returns the locked time for the given flight task, or null if not locked. */
    fun lockedTime(flightTask: FlightTask): Instant? {
        return lockedTasks[flightTask]
    }
}
