package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

enum class StepRelation {
    And,
    Or
}

abstract class TaskStep<T : AbstractMultiStepTask<T, S, E>, S : AbstractTaskStepPlan<S, T, E>, E : Executor>(
    val id: String,
    val name: String,
    val enabledExecutors: Set<E>,
    val status: Set<TaskStatus>
) {
    open val displayName: String? by ::name

    abstract fun duration(task: T, executor: E): Duration

    override fun toString(): String {
        return displayName ?: name
    }
}

data class ForwardTaskStepVector<T : AbstractMultiStepTask<T, S, E>, S : AbstractTaskStepPlan<S, T, E>, E : Executor>(
    val from: TaskStep<T, S, E>,
    val to: List<TaskStep<T, S, E>>,
    val relation: StepRelation
)

data class BackwardTaskStepVector<T : AbstractMultiStepTask<T, S, E>, S : AbstractTaskStepPlan<S, T, E>, E : Executor>(
    val from: List<TaskStep<T, S, E>>,
    val to: TaskStep<T, S, E>,
    val relation: StepRelation
)

open class TaskStepGraph<T : AbstractMultiStepTask<T, S, E>, S : AbstractTaskStepPlan<S, T, E>, E : Executor>(
    val id: String,
    val name: String,
    val steps: List<TaskStep<T, S, E>>,
    val startSteps: Pair<List<TaskStep<T, S, E>>, StepRelation>,
    // must be a DAG
    val forwardTaskStepVector: Map<TaskStep<T, S, E>, ForwardTaskStepVector<T, S, E>>,
    val backwardStepRelation: Map<TaskStep<T, S, E>, BackwardTaskStepVector<T, S, E>>
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

data class TaskStepGraphBuilder<T : AbstractMultiStepTask<T, S, E>, S : AbstractTaskStepPlan<S, T, E>, E : Executor>(
    var id: String? = null,
    var name: String? = null,
    val steps: MutableList<TaskStep<T, S, E>> = ArrayList(),
    var startSteps: Pair<List<TaskStep<T, S, E>>, StepRelation>? = null,
    private val forwardTaskStepVector: MutableMap<TaskStep<T, S, E>, ForwardTaskStepVector<T, S, E>> = HashMap(),
    private val backwardStepRelation: MutableMap<TaskStep<T, S, E>, BackwardTaskStepVector<T, S, E>> = HashMap()
) {
    val setSteps: MutableSet<TaskStep<T, S, E>> = HashSet()

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

    fun TaskStep<T, S, E>.start(
        steps: List<TaskStep<T, S, E>>,
        relation: StepRelation
    ): Try {
        if (steps.any { !this@TaskStepGraphBuilder.steps.contains(it) }) {
            return Failed(Err(ErrorCode.ApplicationError, "step not in"))
        }
        startSteps = Pair(steps, relation)
        return Ok(success)
    }

    fun TaskStep<T, S, E>.forward(
        from: TaskStep<T, S, E>,
        to: List<TaskStep<T, S, E>>,
        relation: StepRelation
    ): Try {
        forwardTaskStepVector[from] = ForwardTaskStepVector(
            from = from,
            to = to,
            relation = relation
        )
        return Ok(success)
    }

    fun TaskStep<T, S, E>.backward(
        from: List<TaskStep<T, S, E>>,
        to: TaskStep<T, S, E>,
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
        return Ok(success)
    }
}
