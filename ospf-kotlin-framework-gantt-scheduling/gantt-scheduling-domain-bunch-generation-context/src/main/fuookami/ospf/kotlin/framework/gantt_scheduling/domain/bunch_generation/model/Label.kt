@file:Suppress("DEPRECATION")

/** 任务束生成标签模型 / Bunch generation label model */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_generation.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * 总成本计算器类型别名 / Total cost calculator typealias
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 */
typealias TotalCostCalculator<T, E> = (executor: E, lastTask: T?, tasks: List<T>) -> Cost<Flt64>?

/**
 * 生成任务束 / Generate bunch
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param label 标签 / Label
 * @param iteration 迭代次数 / Iteration count
 * @param executor 执行器 / Executor
 * @param executorUsability 执行器初始可用性 / Executor initial usability
 * @param totalCostCalculator 总成本计算器 / Total cost calculator
 * @return 任务束或null / Bunch or null
 */
private fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> generateBunch(
    label: Label<T, E, A>,
    iteration: Int64,
    executor: E,
    executorUsability: ExecutorInitialUsability<T, E, A>,
    totalCostCalculator: TotalCostCalculator<T, E>
): AbstractTaskBunch<T, E, A, Flt64>? {
    if (label.node !is EndNode) {
        return null
    }
    // in beginning, it should be the succ node of root node at the top of the stack
    // 起始时应为栈顶根节点的后继节点
    // it means that nodes in the stack is in descending order
    // 这意味着栈中节点按降序排列
    // so the tasks will be in increasing order
    // 因此任务将按升序排列
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
    return totalCost?.let {
        AbstractTaskBunch(
            executor = executor,
            initialUsability = executorUsability,
            tasks = tasks,
            cost = it,
            iteration = iteration
        )
    }
}

/**
 * 生成自定义任务束 / Generate custom bunch
 *
 * @param B 任务束类型 / Bunch type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param label 标签 / Label
 * @param iteration 迭代次数 / Iteration count
 * @param executor 执行器 / Executor
 * @param executorUsability 执行器初始可用性 / Executor initial usability
 * @param totalCostCalculator 总成本计算器 / Total cost calculator
 * @param bunchCtor 任务束构造器 / Bunch constructor
 * @return 任务束或null / Bunch or null
 */
private fun <B : AbstractTaskBunch<T, E, A, Flt64>, T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> generateBunch(
    label: Label<T, E, A>,
    iteration: Int64,
    executor: E,
    executorUsability: ExecutorInitialUsability<T, E, A>,
    totalCostCalculator: TotalCostCalculator<T, E>,
    bunchCtor: (executor: E, ExecutorInitialUsability<T, E, A>, List<T>, Int64, Cost<Flt64>) -> B
): B? {
    if (label.node !is EndNode) {
        return null
    }
    // in beginning, it should be the succ node of root node at the top of the stack
    // 起始时应为栈顶根节点的后继节点
    // it means that nodes in the stack is in descending order
    // 这意味着栈中节点按降序排列
    // so the tasks will be in increasing order
    // 因此任务将按升序排列
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

/**
 * 标签类 / Label class
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param cost 成本 / Cost
 * @param shadowPrice 影子价格 / Shadow price
 * @param prevLabel 前一个标签 / Previous label
 * @param node 节点 / Node
 * @param task 任务 / Task
 */
open class Label<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    val cost: Cost<Flt64>,
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
                is TaskNode<*, *, *, *> -> {
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
            is TaskNode<*, *, *, *> -> {
                trace.add(node.index)
            }

            else -> {}
        }
        this.trace = trace
    }

    /**
     * 检查节点是否已访问 / Check if node is visited
     *
     * @param node 节点 / Node
     * @return 是否已访问 / Whether visited
     */
    fun visited(node: Node): Boolean {
        return when (node) {
            is RootNode, is EndNode -> {
                false
            }

            is TaskNode<*, *, *, *> -> {
                return trace.contains(node.index)
            }
        }
    }

    /**
     * 生成任务束 / Generate bunch
     *
     * @param iteration 迭代次数 / Iteration count
     * @param executor 执行器 / Executor
     * @param executorUsability 执行器初始可用性 / Executor initial usability
     * @param totalCostCalculator 总成本计算器 / Total cost calculator
     * @return 任务束或null / Bunch or null
     */
    fun generateBunch(
        iteration: Int64,
        executor: E,
        executorUsability: ExecutorInitialUsability<T, E, A>,
        totalCostCalculator: TotalCostCalculator<T, E>
    ): AbstractTaskBunch<T, E, A, Flt64>? {
        return generateBunch(
            label = this,
            iteration = iteration,
            executor = executor,
            executorUsability = executorUsability,
            totalCostCalculator = totalCostCalculator
        )
    }

    /**
     * 生成自定义任务束 / Generate custom bunch
     *
     * @param B 任务束类型 / Bunch type
     * @param iteration 迭代次数 / Iteration count
     * @param executor 执行器 / Executor
     * @param executorUsability 执行器初始可用性 / Executor initial usability
     * @param totalCostCalculator 总成本计算器 / Total cost calculator
     * @param bunchCtor 任务束构造器 / Bunch constructor
     * @return 任务束或null / Bunch or null
     */
    fun <B : AbstractTaskBunch<T, E, A, Flt64>> generateBunch(
        iteration: Int64,
        executor: E,
        executorUsability: ExecutorInitialUsability<T, E, A>,
        totalCostCalculator: TotalCostCalculator<T, E>,
        bunchCtor: (executor: E, ExecutorInitialUsability<T, E, A>, List<T>, Int64, Cost<Flt64>) -> B
    ): B? {
        return generateBunch(
            label = this,
            iteration = iteration,
            executor = executor,
            executorUsability = executorUsability,
            totalCostCalculator = totalCostCalculator,
            bunchCtor = bunchCtor
        )
    }
}


