@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 任务编译聚合 / Task compilation aggregation */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation

import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.Makespan
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskSchedulingSwitch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskSchedulingTaskTime
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 抽象任务调度聚合 / Abstract task scheduling aggregation
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param timeWindow 时间窗口 / Time window
 * @param tasks 任务列表 / List of tasks
 * @param executors 执行器列表 / List of executors
 * @param lockCancelTasks 锁定取消任务集合 / Set of locked cancel tasks
 * @param taskCancelEnabled 是否启用任务取消 / Whether task cancellation is enabled
 * @param withExecutorLeisure 是否包含执行器空闲 / Whether to include executor leisure
 */
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

    /**
     * 注册到模型 / Register to model
     *
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
     */
    open fun register(model: MetaModel<Flt64>): Try {
        when (val result = compilation.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = switch.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }
}

/**
 * 任务编译聚合 / Task compilation aggregation
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param timeWindow 时间窗口 / Time window
 * @param tasks 任务列表 / List of tasks
 * @param executors 执行器列表 / List of executors
 * @param lockCancelTasks 锁定取消任务集合 / Set of locked cancel tasks
 * @param taskCancelEnabled 是否启用任务取消 / Whether task cancellation is enabled
 * @param withExecutorLeisure 是否包含执行器空闲 / Whether to include executor leisure
 */
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
) : AbstractTaskSchedulingAggregation<T, E, A>(
    timeWindow = timeWindow,
    tasks = tasks,
    executors = executors,
    lockCancelTasks = lockCancelTasks,
    taskCancelEnabled = taskCancelEnabled,
    withExecutorLeisure = withExecutorLeisure
)

/**
 * 带时间的任务编译聚合 / Task compilation aggregation with time
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param timeWindow 时间窗口 / Time window
 * @param tasks 任务列表 / List of tasks
 * @param executors 执行器列表 / List of executors
 * @param lockCancelTasks 锁定取消任务集合 / Set of locked cancel tasks
 * @param estimateEndTimeCalculator 预估结束时间计算器 / Estimate end time calculator
 * @param taskCancelEnabled 是否启用任务取消 / Whether task cancellation is enabled
 * @param withExecutorLeisure 是否包含执行器空闲 / Whether to include executor leisure
 * @param delayEnabled 是否启用延迟 / Whether delay is enabled
 * @param overMaxDelayEnabled 是否启用超最大延迟 / Whether over-max delay is enabled
 * @param advanceEnabled 是否启用提前 / Whether advance is enabled
 * @param overMaxAdvanceEnabled 是否启用超最大提前 / Whether over-max advance is enabled
 * @param delayLastEndTimeEnabled 是否启用延迟最后结束时间 / Whether delay last end time is enabled
 * @param advanceEarliestEndTimeEnabled 是否启用提前最早结束时间 / Whether advance earliest end time is enabled
 * @param makespanExtra 是否额外计算完工时间 / Whether to compute makespan extra
 */
open class TaskCompilationAggregationWithTime<
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    timeWindow: TimeWindow,
    tasks: List<T>,
    executors: List<E>,
    lockCancelTasks: Set<T> = emptySet(),
    estimateEndTimeCalculator: (T, LinearPolynomial<Flt64>) -> LinearPolynomial<Flt64>,
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

    override fun register(model: MetaModel<Flt64>): Try {
        when (val result = super.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = taskTime.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = makespan.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }
}