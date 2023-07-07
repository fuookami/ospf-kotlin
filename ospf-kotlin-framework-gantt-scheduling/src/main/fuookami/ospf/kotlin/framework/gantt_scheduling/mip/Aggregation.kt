package fuookami.ospf.kotlin.framework.gantt_scheduling.mip

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.mip.model.*

open class Aggregation<E : Executor>(
    taskCancelEnabled: Boolean = false,
    withExecutorLeisure: Boolean = false,
    delayEnabled: Boolean = false,
    advanceEnabled: Boolean = false,
    overExpirationTimeEnabled: Boolean = false,
    makespanExtra: Boolean = false
) {
    open val compilation: Compilation<E> = Compilation(taskCancelEnabled, withExecutorLeisure)
    open val taskTime: TaskTime<E> = TaskTime(delayEnabled, advanceEnabled, overExpirationTimeEnabled)
    open val makespan: Makespan<E> = Makespan(makespanExtra)

    open fun register(
        timeWindow: TimeWindow,
        tasks: List<Task<E>>,
        executors: List<E>,
        ectCalculator: (task: Task<E>, est: Item<*, *>) -> LinearSymbol,
        lockCancelTasks: Set<Task<E>> = emptySet(),
        model: LinearMetaModel
    ): Try<Error> {
        when (val result = compilation.register(tasks, executors, lockCancelTasks, model)) {
            is Ok -> {}
            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = taskTime.register(timeWindow, tasks, ectCalculator, model)) {
            is Ok -> {}
            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = makespan.register(tasks, taskTime, model)) {
            is Ok -> {}
            is Failed -> {
                return Failed(result.error)
            }
        }

        return Ok(success)
    }
}
