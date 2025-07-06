package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_generation.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

typealias TotalCostCalculator<T, E> = (executor: E, lastTask: T?, tasks: List<T>) -> Cost?

private fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> generateBunch(
    label: Label<T, E, A>,
    iteration: Int64,
    executor: E,
    executorUsability: ExecutorInitialUsability<T, E, A>,
    totalCostCalculator: TotalCostCalculator<T, E>
): AbstractTaskBunch<T, E, A>? {
    if (label.node !is EndNode) {
        return null
    }
    // in beginning, it should be the succ node of root node at the top of the stack
    // it means that nodes in the stack is in descending order
    // so the tasks will be in increasing order
    val labels = ArrayList<Label<T, E, A>>()
    var currLabel = label.prevLabel
    while (currLabel!!.node !is RootNode) {
        labels.add(label)
        currLabel = currLabel.prevLabel
    }

    val tasks = ArrayList<T>()
    while (labels.isNotEmpty()) {
        currLabel = labels.last()
        labels.removeAt(labels.lastIndex)

        tasks.add(currLabel.task!!)
    }
    val totalCost = totalCostCalculator(executor, executorUsability.lastTask, tasks)
    return totalCost?.let { AbstractTaskBunch(executor, executorUsability, tasks, it, iteration) }
}

private fun <B : AbstractTaskBunch<T, E, A>, T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> generateBunch(
    label: Label<T, E, A>,
    iteration: Int64,
    executor: E,
    executorUsability: ExecutorInitialUsability<T, E, A>,
    totalCostCalculator: TotalCostCalculator<T, E>,
    bunchCtor: (executor: E, ExecutorInitialUsability<T, E, A>, List<T>, Int64, Cost) -> B
): B? {
    if (label.node !is EndNode) {
        return null
    }
    // in beginning, it should be the succ node of root node at the top of the stack
    // it means that nodes in the stack is in descending order
    // so the tasks will be in increasing order
    val labels = ArrayList<Label<T, E, A>>()
    var currLabel = label.prevLabel
    while (currLabel!!.node !is RootNode) {
        labels.add(label)
        currLabel = currLabel.prevLabel
    }

    val tasks = ArrayList<T>()
    while (labels.isNotEmpty()) {
        currLabel = labels.last()
        labels.removeAt(labels.lastIndex)

        tasks.add(currLabel.task!!)
    }
    val totalCost = totalCostCalculator(executor, executorUsability.lastTask, tasks)
    return totalCost?.let { bunchCtor(executor, executorUsability, tasks, iteration, it) }
}

open class Label<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    val cost: Cost,
    val shadowPrice: Flt64,

    val prevLabel: Label<T, E, A>? = null,
    val node: Node? = null,
    val task: T? = null
) {
    open val reducedCost get() = cost.sum!! - shadowPrice
    val executorChange: UInt64 =
        if (task?.executorChanged == true) {
            UInt64.one
        } else {
            UInt64.zero
        }
    val trace: List<UInt64>
    val isBetterBunch get() = reducedCost ls Flt64.zero
    val plan
        get() = when (task) {
            is AbstractPlannedTask<*, *, *> -> {
                task.plan
            }

            else -> {
                null
            }
        }

    init {
        assert(
            when (node) {
                is TaskNode<*, *> -> {
                    task != null && task is AbstractPlannedTask<*, *, *>
                }

                is RootNode -> {
                    task == null && prevLabel == null
                }

                is EndNode -> {
                    task == null
                }

                null -> {
                    task != null && task is AbstractUnplannedTask<*, *>
                }
            }
        )

        val trace = prevLabel?.trace?.toMutableList() ?: ArrayList()
        when (node) {
            is TaskNode<*, *> -> {
                trace.add(node.index)
            }

            else -> {}
        }
        this.trace = trace
    }

    fun visited(node: Node): Boolean {
        return when (node) {
            is RootNode, is EndNode -> {
                false
            }

            is TaskNode<*, *> -> {
                return trace.contains(node.index)
            }
        }
    }

    fun generateBunch(
        iteration: Int64,
        executor: E,
        executorUsability: ExecutorInitialUsability<T, E, A>,
        totalCostCalculator: TotalCostCalculator<T, E>
    ): AbstractTaskBunch<T, E, A>? {
        return generateBunch(this, iteration, executor, executorUsability, totalCostCalculator)
    }

    fun <B : AbstractTaskBunch<T, E, A>> generateBunch(
        iteration: Int64,
        executor: E,
        executorUsability: ExecutorInitialUsability<T, E, A>,
        totalCostCalculator: TotalCostCalculator<T, E>,
        bunchCtor: (executor: E, ExecutorInitialUsability<T, E, A>, List<T>, Int64, Cost) -> B
    ): B? {
        return generateBunch(this, iteration, executor, executorUsability, totalCostCalculator, bunchCtor)
    }
}
