package fuookami.ospf.kotlin.framework.gantt_scheduling.cg.model

import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*

typealias TotalCostCalculator<E> = (executor: E, lastTask: Task<E>?, tasks: List<Task<E>>) -> Cost?
typealias BunchGenerator<E> = (
    label: Label<E>,
    iteration: UInt64,
    executor: E,
    executorUsability: ExecutorUsability<E>,
    totalCostCalculator: TotalCostCalculator<E>
) -> TaskBunch<E>?

private fun <E : Executor> generateBunch(
    label: Label<E>,
    iteration: UInt64,
    executor: E,
    executorUsability: ExecutorUsability<E>,
    totalCostCalculator: TotalCostCalculator<E>
): TaskBunch<E>? {
    if (label.node !is EndNode) {
        return null
    }
    // in beginning, it should be the succ node of root node at the top of the stack
    // it means that nodes in the stack is in descending order
    // so the tasks will be in increasing order
    val labels = ArrayList<Label<E>>()
    var currlabel = label.prevLabel
    while (currlabel!!.node !is RootNode) {
        labels.add(label)
        currlabel = currlabel.prevLabel
    }

    val tasks = ArrayList<Task<E>>()
    while (labels.isNotEmpty()) {
        currlabel = labels.last()
        labels.removeLast()

        tasks.add(currlabel.task!!)
    }
    val totalCost = totalCostCalculator(executor, executorUsability.lastTask, tasks)
    return totalCost?.let { TaskBunch(executor, executorUsability, tasks, iteration, it) }
}

class Label<E : Executor> internal constructor(
    val cost: Cost,
    val shadowPrice: Flt64,
    val delay: Duration,
    val completeTime: Instant,

    val prevLabel: Label<E>?,
    val node: Node,
    val task: Task<E>?,

    val bunchGenerator: BunchGenerator<E>,
    val lessOperator: Comparator<Label<E>>
) {
    val reducedCost get() = cost.sum!! - shadowPrice
    val executorChange: UInt64 = // (prevLabel?.aircraftChange ?: UInt64.zero) +
        if (task?.executorChanged == true) {
            UInt64.one
        } else {
            UInt64.zero
        }
    val trace: List<UInt64>
    val isBetterBunch get() = reducedCost ls Flt64.zero
    val originTask get() = task?.originTask

    init {
        assert(
            when (node) {
                is TaskNode<*, *> -> {
                    task != null
                }

                is RootNode -> {
                    task == null && prevLabel == null
                }

                is EndNode -> {
                    task == null
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
        iteration: UInt64,
        executor: E,
        executorUsability: ExecutorUsability<E>,
        totalCostCalculator: TotalCostCalculator<E>
    ): TaskBunch<E>? {
        return bunchGenerator(this, iteration, executor, executorUsability, totalCostCalculator)
    }

    infix fun ls(rhs: Label<E>): Boolean {
        return lessOperator(this, rhs)
    }
}

class LabelBuilder<E : Executor, L : Label<E>>(
    var cost: Cost = Cost(),
    var shadowPrice: Flt64 = Flt64.zero,
    val delay: Duration = Duration.ZERO,
    val completeTime: Instant,

    val prevLabel: Label<E>? = null,
    val node: Node,
    val task: Task<E>? = null,

    val bunchGenerator: BunchGenerator<E>,
    val lessOperator: Comparator<Label<E>>
) {
    @Suppress("UNCHECKED_CAST")
    operator fun invoke(): L {
        return Label(
            cost = cost,
            shadowPrice = shadowPrice,
            delay = delay,
            completeTime = completeTime,

            prevLabel = prevLabel,
            node = node,
            task = task,
            bunchGenerator = bunchGenerator,
            lessOperator = lessOperator
        ) as L
    }
}

class LabelBuilderInitializer<E : Executor, L : Label<E>>(
    val bunchGenerator: BunchGenerator<E> = { label: Label<E>, iteration: UInt64, executor: E, executorUsability: ExecutorUsability<E>, totalCostCalculator: TotalCostCalculator<E> ->
        generateBunch(label, iteration, executor, executorUsability, totalCostCalculator)
    },
    val lessOperator: Comparator<Label<E>> = { lhs, rhs ->
        lhs.reducedCost ls rhs.reducedCost
                && lhs.delay <= rhs.delay
                && ((lhs.node is EndNode) || (lhs.executorChange >= rhs.executorChange))
    }
) {
    operator fun invoke(node: Node, completeTime: Instant): LabelBuilder<E, L> {
        return LabelBuilder(
            completeTime = completeTime,
            node = node,
            bunchGenerator = bunchGenerator,
            lessOperator = lessOperator
        )
    }

    operator fun invoke(node: Node, previousLabel: L): LabelBuilder<E, L> {
        return LabelBuilder(
            cost = previousLabel.cost.copy(),
            shadowPrice = previousLabel.shadowPrice,
            completeTime = previousLabel.completeTime,
            node = node,
            prevLabel = previousLabel,
            bunchGenerator = bunchGenerator,
            lessOperator = lessOperator
        )
    }

    operator fun invoke(node: Node, previousLabel: L, assignedTask: Task<E>): LabelBuilder<E, L> {
        return LabelBuilder(
            cost = previousLabel.cost.copy(),
            shadowPrice = previousLabel.shadowPrice,
            delay = previousLabel.delay + assignedTask.delay,
            completeTime = assignedTask.time!!.end,
            prevLabel = previousLabel,
            node = node,
            task = assignedTask,
            bunchGenerator = bunchGenerator,
            lessOperator = lessOperator
        )
    }
}
