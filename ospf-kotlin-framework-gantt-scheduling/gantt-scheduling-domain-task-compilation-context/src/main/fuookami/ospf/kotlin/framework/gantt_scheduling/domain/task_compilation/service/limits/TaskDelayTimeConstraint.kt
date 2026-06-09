@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 任务延迟时间约束 / Task delay time constraint */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskTime
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.SolverTimeWindowBoundary
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 任务延迟时间影子价格键 / Task delay time shadow price key
 *
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param task 任务 / Task
 */
data class TaskDelayTimeShadowPriceKey<
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    val task: AbstractTask<E, A>
) : ShadowPriceKey(TaskDelayTimeShadowPriceKey::class)

/**
 * 任务延迟时间约束 / Task delay time constraint
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param timeWindow 时间窗口 / Time window
 * @param tasks 任务列表 / List of tasks
 * @param taskTime 任务时间对象 / Task time object
 * @param shadowPriceExtractor 影子价格提取器 / Shadow price extractor
 * @param name 管道名称 / Pipeline name
 */
class TaskDelayTimeConstraint<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val timeWindow: TimeWindow<Flt64>,
    tasks: List<AbstractTask<E, A>>,
    private val taskTime: TaskTime,
    private val shadowPriceExtractor: ((Args) -> Flt64?)? = null,
    override val name: String = "task_delay_time"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    /**
     * 通过 solver 时间窗口边界创建任务延迟时间约束 / Create task delay time constraint from a solver time-window boundary
     *
     * @param timeBoundary solver 时间窗口边界 / Solver time-window boundary
     * @param tasks 任务列表 / List of tasks
     * @param taskTime 任务时间对象 / Task time object
     * @param shadowPriceExtractor 影子价格提取器 / Shadow price extractor
     * @param name 管道名称 / Pipeline name
     */
    constructor(
        timeBoundary: SolverTimeWindowBoundary,
        tasks: List<AbstractTask<E, A>>,
        taskTime: TaskTime,
        shadowPriceExtractor: ((Args) -> Flt64?)? = null,
        name: String = "task_delay_time"
    ) : this(
        timeWindow = timeBoundary.source,
        tasks = tasks,
        taskTime = taskTime,
        shadowPriceExtractor = shadowPriceExtractor,
        name = name
    )

    private val timeBoundary = SolverTimeWindowBoundary(timeWindow)

    private val tasks = if (taskTime.delayEnabled) {
        tasks.filter { !it.delayEnabled && it.scheduledTime != null }
    } else {
        tasks.filter { it.scheduledTime != null }
    }

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (task in tasks) {
            val scheduledTime = requireNotNull(task.scheduledTime) {
                "TaskDelayTimeConstraint.invoke 要求 task.scheduledTime 非空: $task"
            }
            when (val result = model.addConstraint(
                taskTime.estimateStartTime[task] leq timeBoundary.valueOf(scheduledTime.start),
                name = "${name}_${task}",
                args = TaskDelayTimeShadowPriceKey(task)
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }

    override fun extractor(): AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> {
        return { map, args ->
            shadowPriceExtractor?.invoke(args) ?: when (args) {
                is TaskGanttSchedulingShadowPriceArguments<*, *> -> {
                    args.task?.let { task ->
                        map.map[TaskDelayTimeShadowPriceKey(task)]?.price ?: Flt64.zero
                    } ?: Flt64.zero
                }

                is BunchGanttSchedulingShadowPriceArguments<*, *> -> {
                    args.task?.let { task ->
                        map.map[TaskDelayTimeShadowPriceKey(task)]?.price ?: Flt64.zero
                    } ?: Flt64.zero
                }

                else -> {
                    Flt64.zero
                }
            }
        }
    }
    override fun refresh(
        shadowPriceMap: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        for (constraint in model.constraintsOfGroup()) {
            val task = shadowPriceKeyOf<TaskDelayTimeShadowPriceKey<E, A>>(constraint.args)?.task ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                shadowPriceMap.put(ShadowPrice(TaskDelayTimeShadowPriceKey(task), price))
            }
        }

        return ok
    }
}


