package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*

interface TaskSchedulingAggregation {
    val compilation: Compilation
    val taskTime: TaskTime
    val makespan: Makespan
}

class Aggregation<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    timeWindow: TimeWindow,
    tasks: List<T>,
    executors: List<E>,
    lockCancelTasks: Set<T> = emptySet(),
    estimateEndTimeCalculator: (T, LinearPolynomial) -> LinearPolynomial,
    taskCancelEnabled: Boolean = false,
    withExecutorLeisure: Boolean = false,
    delayEnabled: Boolean = false,
    advanceEnabled: Boolean = false,
    delayLastEndTimeEnabled: Boolean = false,
    advanceEarliestEndTimeEnabled: Boolean = false,
    makespanExtra: Boolean = false
) : TaskSchedulingAggregation {
    override val compilation: TaskCompilation<T, E, A> =
        TaskCompilation(tasks, executors, lockCancelTasks, taskCancelEnabled, withExecutorLeisure)
    override val taskTime: TaskSchedulingTaskTime<T, E, A> =
        TaskSchedulingTaskTime(timeWindow, tasks, compilation, estimateEndTimeCalculator, delayEnabled, advanceEnabled, delayLastEndTimeEnabled, advanceEarliestEndTimeEnabled)
    override val makespan: TaskSchedulingMakespan<T, E, A> =
        TaskSchedulingMakespan(tasks, taskTime, makespanExtra)

    fun register(model: LinearMetaModel): Try {
        when (val result = compilation.register(model)) {
            is Ok -> {}
            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = taskTime.register(model)) {
            is Ok -> {}
            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = makespan.register(model)) {
            is Ok -> {}
            is Failed -> {
                return Failed(result.error)
            }
        }

        return Ok(success)
    }
}
