/** 标签模型 / Label model */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_generation.model

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 总成本计算器类型别名 / Total cost calculator typealias
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param V 数值类型 / Numeric type
*/
typealias TotalCostCalculator<T, E, V> = (executor: E, lastTask: T?, tasks: List<T>) -> Cost<V>?

/**
 * 生成任务束 / Generate bunch
 *
 * @param V 数值类型 / Numeric type
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
private fun <V, T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> generateBunch(
    label: Label<T, E, A, V>,
    iteration: Int64,
    executor: E,
    executorUsability: ExecutorInitialUsability<T, E, A>,
    totalCostCalculator: TotalCostCalculator<T, E, V>
): AbstractTaskBunch<T, E, A, V>? where V : RealNumber<V>, V : PlusGroup<V> {
    if (label.node !is EndNode) {
        return null
    }
    val labels = ArrayList<Label<T, E, A, V>>()
    var currLabel = label.prevLabel
    while (currLabel!!.node !is RootNode) {
        labels.add(currLabel)
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
 * @param V 数值类型 / Numeric type
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
private fun <B : AbstractTaskBunch<T, E, A, V>, V, T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> generateBunch(
    label: Label<T, E, A, V>,
    iteration: Int64,
    executor: E,
    executorUsability: ExecutorInitialUsability<T, E, A>,
    totalCostCalculator: TotalCostCalculator<T, E, V>,
    bunchCtor: (executor: E, ExecutorInitialUsability<T, E, A>, List<T>, Int64, Cost<V>) -> B
): B? where V : RealNumber<V>, V : PlusGroup<V> {
    if (label.node !is EndNode) {
        return null
    }
    val labels = ArrayList<Label<T, E, A, V>>()
    var currLabel = label.prevLabel
    while (currLabel!!.node !is RootNode) {
        labels.add(currLabel)
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
 * 标签类，用于列生成中的标签算法 / Label class for labeling algorithm in column generation
 *
 * 每个标签代表从根节点到当前节点的部分路径，包含累计成本和影子价格信息。
 * Each label represents a partial path from the root node to the current node, containing accumulated cost and shadow price information.
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param V 数值类型 / Numeric type
 * @property cost 累计成本 / Accumulated cost
 * @property shadowPrice 累计影子价格 / Accumulated shadow price
 * @property prevLabel 前驱标签 / Previous label in the path
 * @property node 当前节点 / Current node
 * @property task 当前任务 / Current task
 * @property reducedCost 检验成本（成本减去影子价格）/ Reduced cost (cost minus shadow price)
 * @property executorChange 执行器变更次数（0或1）/ Executor change count (0 or 1)
 * @property trace 已访问的任务节点索引轨迹 / Trace of visited task node indices
 * @property isBetterBunch 是否为更优束（检验成本小于零）/ Whether this is a better bunch (reduced cost less than zero)
 * @property plan 关联的计划，若任务为计划任务 / Associated plan if the task is a planned task
*/
open class Label<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>, V>(
    val cost: Cost<V>,
    val shadowPrice: V,

    val prevLabel: Label<T, E, A, V>? = null,
    val node: Node? = null,
    val task: T? = null
) where V : RealNumber<V>, V : PlusGroup<V> {
    open val reducedCost get() = cost.costSum!!.value - shadowPrice
    val executorChange: UInt64 =
        if (task?.executorChanged == true) {
            UInt64.one
        } else {
            UInt64.zero
        }
    val trace: List<UInt64>
    val isBetterBunch get() = reducedCost ls shadowPrice.constants.zero
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
        totalCostCalculator: TotalCostCalculator<T, E, V>
    ): AbstractTaskBunch<T, E, A, V>? {
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
    fun <B : AbstractTaskBunch<T, E, A, V>> generateBunch(
        iteration: Int64,
        executor: E,
        executorUsability: ExecutorInitialUsability<T, E, A>,
        totalCostCalculator: TotalCostCalculator<T, E, V>,
        bunchCtor: (executor: E, ExecutorInitialUsability<T, E, A>, List<T>, Int64, Cost<V>) -> B
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
