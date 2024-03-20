package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*

abstract class AbstractTaskSchedulingAggregation<E : Executor, A : AssignmentPolicy<E>>(
    timeWindow: TimeWindow,
    tasks: List<AbstractTask<E, A>>,
    executors: List<E>,
    lockCancelTasks: Set<AbstractTask<E, A>> = emptySet(),
    taskCancelEnabled: Boolean = false,
    withExecutorLeisure: Boolean = false,
) {
    val compilation: TaskCompilation<E, A> =
        TaskCompilation(tasks, executors, lockCancelTasks, taskCancelEnabled, withExecutorLeisure)
    val switch: TaskSchedulingSwitch<E, A> =
        TaskSchedulingSwitch(timeWindow, tasks, executors, compilation)

    open fun register(model: LinearMetaModel): Try {
        when (val result = compilation.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = switch.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return Ok(success)
    }
}

open class TaskCompilationAggregation<E : Executor, A : AssignmentPolicy<E>>(
    timeWindow: TimeWindow,
    tasks: List<AbstractTask<E, A>>,
    executors: List<E>,
    lockCancelTasks: Set<AbstractTask<E, A>> = emptySet(),
    taskCancelEnabled: Boolean = false,
    withExecutorLeisure: Boolean = false,
): AbstractTaskSchedulingAggregation<E, A>(timeWindow, tasks, executors, lockCancelTasks, taskCancelEnabled, withExecutorLeisure)

open class TaskCompilationAggregationWithTime<E : Executor, A : AssignmentPolicy<E>>(
    timeWindow: TimeWindow,
    tasks: List<AbstractTask<E, A>>,
    executors: List<E>,
    lockCancelTasks: Set<AbstractTask<E, A>> = emptySet(),
    estimateEndTimeCalculator: (AbstractTask<E, A>, LinearPolynomial) -> LinearPolynomial,
    taskCancelEnabled: Boolean = false,
    withExecutorLeisure: Boolean = false,
    delayEnabled: Boolean = false,
    overMaxDelayEnabled: Boolean = false,
    advanceEnabled: Boolean = false,
    overMaxAdvanceEnabled: Boolean = false,
    delayLastEndTimeEnabled: Boolean = false,
    advanceEarliestEndTimeEnabled: Boolean = false,
    makespanExtra: Boolean = false
): AbstractTaskSchedulingAggregation<E, A>(timeWindow, tasks, executors, lockCancelTasks, taskCancelEnabled, withExecutorLeisure) {
    val taskTime: TaskSchedulingTaskTime<E, A> =
        TaskSchedulingTaskTime(
            timeWindow,
            tasks,
            compilation,
            estimateEndTimeCalculator,
            delayEnabled,
            overMaxDelayEnabled,
            advanceEnabled,
            overMaxAdvanceEnabled,
            delayLastEndTimeEnabled,
            advanceEarliestEndTimeEnabled
        )
    val makespan: Makespan<E, A> =
        Makespan(tasks, taskTime, makespanExtra)

    override fun register(model: LinearMetaModel): Try {
        when (val result = super.register(model)) {
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
