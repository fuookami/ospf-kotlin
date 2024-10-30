package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

enum class StepRelation {
    And,
    Or
}

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

data class ForwardTaskStepVector<
    out T : AbstractMultiStepTask<T, S, E>,
    out S : AbstractTaskStepPlan<S, T, E>,
    out E : Executor
>(
    val from: TaskStep<T, S, E>,
    val to: List<TaskStep<T, S, E>>,
    val relation: StepRelation
)

data class BackwardTaskStepVector<
    out T : AbstractMultiStepTask<T, S, E>,
    out S : AbstractTaskStepPlan<S, T, E>,
    out E : Executor
>(
    val from: List<TaskStep<T, S, E>>,
    val to: TaskStep<T, S, E>,
    val relation: StepRelation
)

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
            return Failed(Err(ErrorCode.ApplicationError, "step not in"))
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
                return Failed(Err(ErrorCode.ApplicationError, "no step to ${to.id}"))
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
