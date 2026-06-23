@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 任务步骤图模型，定义多步任务的步骤关系 / Task step graph model defining step relationships for multi-step tasks
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.time.Duration
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.error.GanttSchedulingLifecycleError
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 步骤关系枚举 / Step relation enumeration
 */
enum class StepRelation {
    /** 与关系（所有前置步骤必须完成）/ And relation (all preceding steps must complete) */
    And,
    /** 或关系（任一前置步骤完成即可）/ Or relation (any preceding step completion is sufficient) */
    Or
}

/**
 * 抽象任务步骤 / Abstract task step
 *
 * @param T 多步任务类型 / The multi-step task type
 * @param S 步骤计划类型 / The step plan type
 * @param E 执行者类型 / The executor type
 * @property id 步骤ID / The step ID
 * @property name 步骤名称 / The step name
 * @property enabledExecutors 启用的执行者集合 / The set of enabled executors
 * @property status 任务状态集合 / The set of task statuses
 */
abstract class TaskStep<
        out T : AbstractMultiStepTask<T, S, E>,
        out S : AbstractTaskStepPlan<S, T, E>,
        out E : Executor
        >(
    val id: String,
    val name: String,
    val enabledExecutors: Set<E>,
    val status: Set<TaskStatus>
) {
    open val displayName: String? by ::name

    abstract fun duration(step: @UnsafeVariance S, executor: @UnsafeVariance E): Duration

    override fun toString(): String {
        return displayName ?: name
    }
}

/**
 * 前向任务步骤向量，表示步骤的前向关系 / Forward task step vector representing forward relationships between steps
 *
 * @param T 多步任务类型 / The multi-step task type
 * @param S 步骤计划类型 / The step plan type
 * @param E 执行者类型 / The executor type
 * @property from 源步骤 / The source step
 * @property to 目标步骤列表 / The list of target steps
 * @property relation 步骤关系 / The step relation
 */
data class ForwardTaskStepVector<
        out T : AbstractMultiStepTask<T, S, E>,
        out S : AbstractTaskStepPlan<S, T, E>,
        out E : Executor
        >(
    val from: TaskStep<T, S, E>,
    val to: List<TaskStep<T, S, E>>,
    val relation: StepRelation
)

/**
 * 后向任务步骤向量，表示步骤的后向关系 / Backward task step vector representing backward relationships between steps
 *
 * @param T 多步任务类型 / The multi-step task type
 * @param S 步骤计划类型 / The step plan type
 * @param E 执行者类型 / The executor type
 * @property from 源步骤列表 / The list of source steps
 * @property to 目标步骤 / The target step
 * @property relation 步骤关系 / The step relation
 */
data class BackwardTaskStepVector<
        out T : AbstractMultiStepTask<T, S, E>,
        out S : AbstractTaskStepPlan<S, T, E>,
        out E : Executor
        >(
    val from: List<TaskStep<T, S, E>>,
    val to: TaskStep<T, S, E>,
    val relation: StepRelation
)

/**
 * 任务步骤图，表示多步任务的步骤依赖关系（必须为DAG）/ Task step graph representing step dependencies for multi-step tasks (must be a DAG)
 *
 * @param T 多步任务类型 / The multi-step task type
 * @param S 步骤计划类型 / The step plan type
 * @param E 执行者类型 / The executor type
 * @property id 图ID / The graph ID
 * @property name 图名称 / The graph name
 * @property steps 步骤列表 / The list of steps
 * @property startSteps 起始步骤及关系 / The start steps and their relation
 * @property forwardTaskStepVector 前向步骤向量映射 / The forward step vector mapping
 * @property backwardStepRelation 后向步骤关系映射 / The backward step relation mapping
 */
open class TaskStepGraph<
        out T : AbstractMultiStepTask<T, S, E>,
        out S : AbstractTaskStepPlan<S, T, E>,
        out E : Executor
        >(
    val id: String,
    val name: String,
    val steps: List<TaskStep<T, S, E>>,
    val startSteps: Pair<List<TaskStep<T, S, E>>, StepRelation>,
    // must be a DAG
    val forwardTaskStepVector: Map<
            TaskStep<@UnsafeVariance T, @UnsafeVariance S, @UnsafeVariance E>,
            ForwardTaskStepVector<T, S, E>
            >,
    val backwardStepRelation: Map<
            TaskStep<@UnsafeVariance T, @UnsafeVariance S, @UnsafeVariance E>,
            BackwardTaskStepVector<T, S, E>
            >
) {
    companion object {
        fun <T : AbstractMultiStepTask<T, S, E>, S : AbstractTaskStepPlan<S, T, E>, E : Executor> build(
            ctx: TaskStepGraphBuilder<T, S, E>.() -> Unit
        ): TaskStepGraph<T, S, E> {
            val builder = TaskStepGraphBuilder<T, S, E>()
            ctx(builder)
            return builder()
        }
    }
}

/**
 * 任务步骤图构建器 / Task step graph builder
 *
 * @param T 多步任务类型 / The multi-step task type
 * @param S 步骤计划类型 / The step plan type
 * @param E 执行者类型 / The executor type
 * @property id 图ID / The graph ID
 * @property name 图名称 / The graph name
 * @property steps 步骤列表 / The list of steps
 * @property startSteps 起始步骤及关系 / The start steps and their relation
 */
data class TaskStepGraphBuilder<
        out T : AbstractMultiStepTask<T, S, E>,
        out S : AbstractTaskStepPlan<S, T, E>,
        out E : Executor
        >(
    var id: String? = null,
    var name: String? = null,
    val steps: MutableList<TaskStep<@UnsafeVariance T, @UnsafeVariance S, @UnsafeVariance E>> = ArrayList(),
    var startSteps: Pair<List<TaskStep<@UnsafeVariance T, @UnsafeVariance S, @UnsafeVariance E>>, StepRelation>? = null,
    private val forwardTaskStepVector: MutableMap<TaskStep<T, S, E>, ForwardTaskStepVector<T, S, E>> = HashMap(),
    private val backwardStepRelation: MutableMap<TaskStep<T, S, E>, BackwardTaskStepVector<T, S, E>> = HashMap()
) {
    val setSteps: MutableSet<TaskStep<@UnsafeVariance T, @UnsafeVariance S, @UnsafeVariance E>> = HashSet()

    operator fun invoke(): TaskStepGraph<T, S, E> {
        return TaskStepGraph(
            id = id!!,
            name = name!!,
            steps = steps,
            startSteps = startSteps!!,
            forwardTaskStepVector = forwardTaskStepVector,
            backwardStepRelation = backwardStepRelation
        )
    }

    fun TaskStep<@UnsafeVariance T, @UnsafeVariance S, @UnsafeVariance E>.start(
        steps: List<TaskStep<@UnsafeVariance T, @UnsafeVariance S, @UnsafeVariance E>>,
        relation: StepRelation
    ): Try {
        if (steps.any { !this@TaskStepGraphBuilder.steps.contains(it) }) {
            return Failed(GanttSchedulingLifecycleError("step not in"))
        }
        startSteps = Pair(steps, relation)
        return ok
    }

    fun TaskStep<@UnsafeVariance T, @UnsafeVariance S, @UnsafeVariance E>.forward(
        from: TaskStep<@UnsafeVariance T, @UnsafeVariance S, @UnsafeVariance E>,
        to: List<TaskStep<@UnsafeVariance T, @UnsafeVariance S, @UnsafeVariance E>>,
        relation: StepRelation
    ): Try {
        forwardTaskStepVector[from] = ForwardTaskStepVector(
            from = from,
            to = to,
            relation = relation
        )
        return ok
    }

    fun TaskStep<@UnsafeVariance T, @UnsafeVariance S, @UnsafeVariance E>.backward(
        from: List<TaskStep<@UnsafeVariance T, @UnsafeVariance S, @UnsafeVariance E>>,
        to: TaskStep<@UnsafeVariance T, @UnsafeVariance S, @UnsafeVariance E>,
        relation: StepRelation
    ): Try {
        for (step in from) {
            if ((forwardTaskStepVector[step]?.to ?: emptyList()).any { it == to }) {
                return Failed(GanttSchedulingLifecycleError("no step to ${to.id}"))
            }
        }

        backwardStepRelation[to] = BackwardTaskStepVector(
            from = from,
            to = to,
            relation = relation
        )
        return ok
    }
}