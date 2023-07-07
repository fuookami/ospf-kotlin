package fuookami.ospf.kotlin.framework.gantt_scheduling.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*

data class ResourceCapacity(
    val time: TimeRange,
    val amount: UInt64,
    val interval: Duration = Duration.INFINITE,
) {
    override fun toString() = "${amount}_${interval}"
}

abstract class Resource<E : Executor>(
    val id: String,
    val name: String,
    val capacities: List<ResourceCapacity>
) : ManualIndexed() {
    abstract fun usedBy(prevTask: Task<E>?, task: Task<E>?, time: TimeRange): Boolean

    open fun usedTime(bunch: TaskBunch<E>, time: TimeRange): UInt64 {
        var counter = UInt64.zero
        for (i in bunch.tasks.indices) {
            val used = if (i == 0) {
                usedBy(bunch.lastTask, bunch.tasks[i], time)
            } else {
                usedBy(bunch.tasks[i - 1], bunch.tasks[i], time)
            }
            counter += if (used) {
                UInt64.one
            } else {
                UInt64.zero
            }
        }
        return counter
    }
}
