@file:Suppress("DEPRECATION")
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_generation.model

import fuookami.ospf.kotlin.math.algebra.concept.PlusGroup
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 泛型总成本计算器类型别名 / Generic total cost calculator typealias
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param V 数值类型 / Numeric type
 */
typealias TotalCostCalculatorV<T, E, V> = (executor: E, lastTask: T?, tasks: List<T>) -> Cost<V>?

/**
 * 向后兼容 Flt64 总成本计算器 / Backward compat Flt64 total cost calculator
 */
typealias TotalCostCalculator<T, E> = TotalCostCalculatorV<T, E, Flt64>

/**
 * 生成任务束（泛型版本）/ Generate bunch (generic version)
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
    label: LabelV<T, E, A, V>,
    iteration: Int64,
    executor: E,
    executorUsability: ExecutorInitialUsability<T, E, A>,
    totalCostCalculator: TotalCostCalculatorV<T, E, V>
): AbstractTaskBunch<T, E, A, V>? where V : RealNumber<V>, V : PlusGroup<V> {
    if (label.node !is EndNode) {
        return null
    }
    val labels = ArrayList<LabelV<T, E, A, V>>()
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
 * 生成自定义任务束（泛型版本）/ Generate custom bunch (generic version)
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
    label: LabelV<T, E, A, V>,
    iteration: Int64,
    executor: E,
    executorUsability: ExecutorInitialUsability<T, E, A>,
    totalCostCalculator: TotalCostCalculatorV<T, E, V>,
    bunchCtor: (executor: E, ExecutorInitialUsability<T, E, A>, List<T>, Int64, Cost<V>) -> B
): B? where V : RealNumber<V>, V : PlusGroup<V> {
    if (label.node !is EndNode) {
        return null
    }
    val labels = ArrayList<LabelV<T, E, A, V>>()
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
 * 标签类（泛型版本）/ Label class (generic version)
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param V 数值类型 / Numeric type
 * @param cost 成本 / Cost
 * @param shadowPrice 影子价格 / Shadow price (V type; callers convert from solver Flt64)
 * @param prevLabel 前一个标签 / Previous label
 * @param node 节点 / Node
 * @param task 任务 / Task
 */
open class LabelV<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>, V>(
    val cost: Cost<V>,
    val shadowPrice: V,

    val prevLabel: LabelV<T, E, A, V>? = null,
    val node: Node? = null,
    val task: T? = null
) where V : RealNumber<V>, V : PlusGroup<V> {
    open val reducedCost get() = cost.sum!! - shadowPrice
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
        totalCostCalculator: TotalCostCalculatorV<T, E, V>
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
        totalCostCalculator: TotalCostCalculatorV<T, E, V>,
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

/** 向后兼容 typealias — Flt64 label / Backward compat typealias */
typealias Label<T, E, A> = LabelV<T, E, A, Flt64>

/** 向后兼容 typealias — Flt64 label / Backward compat typealias */
typealias Flt64Label<T, E, A> = LabelV<T, E, A, Flt64>
