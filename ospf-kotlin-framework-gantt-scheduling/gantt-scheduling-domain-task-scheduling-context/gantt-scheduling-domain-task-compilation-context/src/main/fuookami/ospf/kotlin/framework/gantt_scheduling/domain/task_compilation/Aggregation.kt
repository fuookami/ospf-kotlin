package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

abstract class AbstractTaskSchedulingAggregation<
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    timeWindow: TimeWindow,
    tasks: List<T>,
    executors: List<E>,
    lockCancelTasks: Set<T> = emptySet(),
    taskCancelEnabled: Boolean = false,
    withExecutorLeisure: Boolean = false,
) {
    val compilation: TaskCompilation<T, E, A> =
        TaskCompilation(tasks, executors, lockCancelTasks, taskCancelEnabled, withExecutorLeisure)
    val switch: TaskSchedulingSwitch<T, E, A> =
        TaskSchedulingSwitch(timeWindow, tasks, executors, compilation)

    open fun register(model: MetaModel): Try {
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

        return ok
    }
}

open class TaskCompilationAggregation<
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    timeWindow: TimeWindow,
    tasks: List<T>,
    executors: List<E>,
    lockCancelTasks: Set<T> = emptySet(),
    taskCancelEnabled: Boolean = false,
    withExecutorLeisure: Boolean = false,
): AbstractTaskSchedulingAggregation<T, E, A>(timeWindow, tasks, executors, lockCancelTasks, taskCancelEnabled, withExecutorLeisure)

open class TaskCompilationAggregationWithTime<
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    timeWindow: TimeWindow,
    tasks: List<T>,
    executors: List<E>,
    lockCancelTasks: Set<T> = emptySet(),
    estimateEndTimeCalculator: (T, LinearPolynomial) -> LinearPolynomial,
    taskCancelEnabled: Boolean = false,
    withExecutorLeisure: Boolean = false,
    delayEnabled: Boolean = false,
    overMaxDelayEnabled: Boolean = false,
    advanceEnabled: Boolean = false,
    overMaxAdvanceEnabled: Boolean = false,
    delayLastEndTimeEnabled: Boolean = false,
    advanceEarliestEndTimeEnabled: Boolean = false,
    makespanExtra: Boolean = false
): AbstractTaskSchedulingAggregation<T, E, A>(timeWindow, tasks, executors, lockCancelTasks, taskCancelEnabled, withExecutorLeisure) {
    val taskTime: TaskSchedulingTaskTime<T, E, A> =
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
    val makespan: Makespan<T, E, A> =
        Makespan(tasks, taskTime, makespanExtra)

    override fun register(model: MetaModel): Try {
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

        return ok
    }
}
