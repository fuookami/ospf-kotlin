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
    val compilation: TaskCompilation<T, E, A> = TaskCompilation(
        tasks = tasks,
        executors = executors,
        lockCancelTasks = lockCancelTasks,
        taskCancelEnabled = taskCancelEnabled,
        withExecutorLeisure = withExecutorLeisure
    )
    
    val switch: TaskSchedulingSwitch<T, E, A> = TaskSchedulingSwitch(
        timeWindow = timeWindow,
        tasks = tasks,
        executors = executors,
        compilation = compilation
    )

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
): AbstractTaskSchedulingAggregation<T, E, A>(
    timeWindow = timeWindow,
    tasks = tasks,
    executors = executors,
    lockCancelTasks = lockCancelTasks,
    taskCancelEnabled = taskCancelEnabled,
    withExecutorLeisure = withExecutorLeisure
)

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
) : AbstractTaskSchedulingAggregation<T, E, A>(
    timeWindow = timeWindow,
    tasks = tasks,
    executors = executors,
    lockCancelTasks = lockCancelTasks,
    taskCancelEnabled = taskCancelEnabled,
    withExecutorLeisure = withExecutorLeisure
) {
    val taskTime: TaskSchedulingTaskTime<T, E, A> = TaskSchedulingTaskTime(
        timeWindow = timeWindow,
        tasks = tasks,
        compilation = compilation,
        estimateEndTimeCalculator = estimateEndTimeCalculator,
        delayEnabled = delayEnabled,
        overMaxDelayEnabled = overMaxDelayEnabled,
        advanceEnabled = advanceEnabled,
        overMaxAdvanceEnabled = overMaxAdvanceEnabled,
        delayLastEndTimeEnabled = delayLastEndTimeEnabled,
        advanceEarliestEndTimeEnabled = advanceEarliestEndTimeEnabled
    )

    val makespan: Makespan<T, E, A> = Makespan(
        tasks = tasks,
        taskTime = taskTime,
        extra = makespanExtra
    )

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
