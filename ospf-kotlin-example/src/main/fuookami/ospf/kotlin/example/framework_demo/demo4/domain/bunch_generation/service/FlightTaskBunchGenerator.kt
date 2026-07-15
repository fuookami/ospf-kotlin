@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 批次生成配置。Configuration for bunch generation.
 *
 * @property withOrderChange Whether to allow order changes during bunch generation / 批次生成时是否允许顺序变更
 * @property maximumLabelPerNode Maximum number of labels retained per node / 每个节点保留的最大标签数
 * @property maximumColumnGeneratedPerAircraft 每架飞机最大列生成数 / Maximum columns generated per aircraft
*/
data class BunchGenerationConfiguration(
    val withOrderChange: Boolean = false,
    val maximumLabelPerNode: UInt64 = UInt64(100UL),
    val maximumColumnGeneratedPerAircraft: UInt64 = UInt64(10UL)
)

/**
 * 标号设置算法的内部标签。Internal label for the Label Setting algorithm.
 *
 * @param cost 代价 / Cost
 * @param shadowPrice 影子价格 / Shadow price
 * @param delay 延迟 / Delay
 * @param arrivalTime 到达时间 / Arrival time
 * @param flightHour 飞行小时 / Flight hour
 * @param flightCycle 飞行循环 / Flight cycle
 * @param prevLabel 前驱标签 / Previous label
 * @param node 节点 / Node
 * @param flightTask 航班任务 / Flight task
*/
private data class LabelBuilder(
    var cost: Cost<FltX> = Cost(FltX),
    var shadowPrice: Flt64 = Flt64.zero,
    val delay: Duration = Duration.ZERO,
    val arrivalTime: Instant,
    val flightHour: FlightHour = FlightHour.zero,
    val flightCycle: FlightCycle = FlightCycle.zero,

    val prevLabel: Label? = null,
    val node: Node,
    val flightTask: FlightTask? = null
) {
    companion object {
        operator fun invoke(node: Node, arrivalTime: Instant) = LabelBuilder(
            arrivalTime = arrivalTime,
            node = node
        )

        operator fun invoke(node: Node, previousLabel: Label) = LabelBuilder(
            cost = previousLabel.cost.copy(),
            shadowPrice = previousLabel.shadowPrice,
            arrivalTime = previousLabel.arrivalTime,
            node = node,
            prevLabel = previousLabel
        )

        operator fun invoke(node: Node, previousLabel: Label, recoveryFlightTask: FlightTask) = LabelBuilder(
            cost = previousLabel.cost.copy(),
            shadowPrice = previousLabel.shadowPrice,
            delay = previousLabel.delay + recoveryFlightTask.actualDelay,
            arrivalTime = recoveryFlightTask.time!!.end,
            flightHour = previousLabel.flightHour + (recoveryFlightTask.flightHour ?: FlightHour.zero),
            flightCycle = previousLabel.flightCycle + recoveryFlightTask.flightCycle,
            prevLabel = previousLabel,
            node = node,
            flightTask = recoveryFlightTask
        )
    }
}

/**
 * 标号设置算法的内部标签。Internal label for the Label Setting algorithm.
 *
 * @param cost 代价 / Cost
 * @param shadowPrice 影子价格 / Shadow price
 * @param delay 延迟 / Delay
 * @param arrivalTime 到达时间 / Arrival time
 * @param flightHour 飞行小时 / Flight hour
 * @param flightCycle 飞行循环 / Flight cycle
 * @param prevLabel 前驱标签 / Previous label
 * @param node 节点 / Node
 * @param flightTask 航班任务 / Flight task
*/
private data class Label(
    val cost: Cost<FltX>,
    val shadowPrice: Flt64,
    val delay: Duration,
    val arrivalTime: Instant,
    val flightHour: FlightHour,
    val flightCycle: FlightCycle,

    val prevLabel: Label?,
    val node: Node,
    val flightTask: FlightTask?
) {
    companion object {
        operator fun invoke(builder: LabelBuilder) = Label(
            cost = builder.cost,
            shadowPrice = builder.shadowPrice,
            delay = builder.delay,
            arrivalTime = builder.arrivalTime,
            flightHour = builder.flightHour,
            flightCycle = builder.flightCycle,
            prevLabel = builder.prevLabel,
            node = builder.node,
            flightTask = builder.flightTask
        )
    }

    val reducedCost: Flt64 get() = cost.solverCostOrNull(Flt64.zero)!! - shadowPrice
    val aircraftChange: UInt64 =
        if (flightTask?.aircraftChanged == true) {
            UInt64.one
        } else {
            UInt64.zero
        }
    val trace: List<UInt64>
    val isBetterBunch get() = reducedCost < Flt64.zero
    val originFlightTask get() = flightTask?.originTask

    init {
        assert(
            when (node) {
                is TaskNode -> {
                    flightTask != null
                }

                is RootNode -> {
                    flightTask == null && prevLabel == null
                }

                is EndNode -> {
                    flightTask == null
                }
            }
        )

        val trace = prevLabel?.trace?.toMutableList() ?: ArrayList()
        when (node) {
            is TaskNode -> {
                trace.add(node.index)
            }

            else -> {}
        }
        this.trace = trace
    }

    /**
     * Checks whether the given node has already been visited in this label's trace.
     * 检查给定节点是否已在此标签的轨迹中被访问过。
     *
     * @param node The node to check / 要检查的节点
     * @return Whether the node has been visited / 节点是否已被访问
    */
    fun visited(node: Node): Boolean {
        return when (node) {
            is RootNode, is EndNode -> {
                false
            }

            is TaskNode -> {
                return trace.contains(node.index)
            }
        }
    }

    /**
     * Generates a flight task bunch from this label by tracing back through predecessor labels.
     * 通过回溯前驱标签从此标签生成航班任务束。
     *
     * @param iteration The current iteration index / 当前迭代索引
     * @param aircraft The aircraft assigned to this bunch / 分配给此束的航空器
     * @param aircraftUsability The aircraft usability context / 航空器可用性上下文
     * @param totalCostCalculator The total cost calculator function / 总代价计算函数
     * @return The generated flight task bunch, or null if total cost is unavailable / 生成的航班任务束，若总代价不可用则返回 null
    */
    fun generateBunch(
        iteration: Int64,
        aircraft: Aircraft,
        aircraftUsability: AircraftUsability,
        totalCostCalculator: TotalCostCalculator
    ): FlightTaskBunch? {
        assert(node is EndNode)
        val labels = ArrayList<Label>()
        var label = prevLabel
        while (label!!.node !is RootNode) {
            labels.add(label)
            label = label.prevLabel
        }

        val flightTasks = ArrayList<FlightTask>()
        while (labels.isNotEmpty()) {
            label = labels.last()
            labels.removeLast()

            flightTasks.add(label.flightTask!!)
        }
        val totalCost = totalCostCalculator(aircraft, flightTasks)
        @Suppress("UNCHECKED_CAST")
        return totalCost?.let { FlightTaskBunch(aircraft, flightTasks, iteration, it as Cost<FltX>) }
    }

    /**
     * Lexicographic dominance comparison: returns true if this label dominates the right-hand side label.
     * 字典序支配比较：若此标签支配右侧标签则返回 true。
     *
     * @param rhs The right-hand side label to compare against / 用于比较的右侧标签
     * @return Whether this label dominates the rhs label / 此标签是否支配 rhs 标签
    */
    infix fun ls(rhs: Label): Boolean {
        return (reducedCost - rhs.reducedCost) < Flt64.zero
                && delay <= rhs.delay
                && ((node is EndNode) || (aircraftChange >= rhs.aircraftChange))
    }
}

private typealias LabelMap = MutableMap<Node, MutableList<Label>>

/**
 * 使用标号设置算法生成航班任务束。Generates flight task bunches using Label Setting algorithm.
 *
 * @property aircraft The aircraft for which bunches are generated / 生成束的飞机
 * @property aircraftUsability The aircraft usability context / 飞机可用性上下文
 * @property graph The route graph for bunch generation / 批次生成的路线图
 * @property connectionTimeCalculator The connection time calculator between tasks / 任务间的连接时间计算器
 * @property minimumDepartureTimeCalculator The minimum departure time calculator / 最早出发时间计算器
 * @property costCalculator The incremental cost calculator / 增量代价计算器
 * @property totalCostCalculator The total cost calculator for a bunch / 束的总代价计算器
 * @property configuration The bunch generation configuration / 批次生成配置
*/
class FlightTaskBunchGenerator(
    private val aircraft: Aircraft,
    private val aircraftUsability: AircraftUsability,
    private val graph: Graph,
    private val connectionTimeCalculator: ConnectionTimeCalculator,
    private val minimumDepartureTimeCalculator: MinimumDepartureTimeCalculator,
    private val costCalculator: CostCalculator,
    private val totalCostCalculator: TotalCostCalculator,
    private val configuration: BunchGenerationConfiguration
) {
    companion object {
        /**
         * Sorts the graph nodes in topological order using Kahn's algorithm with time-based tie-breaking.
         * 使用 Kahn 算法按拓扑排序图节点，以时间为平局打破依据。
         *
         * @param graph The directed acyclic graph to sort / 要排序的有向无环图
         * @return The list of nodes in topological order / 按拓扑排序的节点列表
        */
        private fun sortNodes(graph: Graph): List<Node> {
            val inDegree = HashMap<Node, UInt64>()
            for ((_, node) in graph.nodes) {
                inDegree[node] = UInt64.zero
            }
            for ((_, node) in graph.nodes) {
                for (edge in graph[node]) {
                    inDegree[edge.to] = (inDegree[edge.to] ?: UInt64.zero) + UInt64.one
                }
            }

            val nodes = ArrayList<Node>()
            while (inDegree.isNotEmpty()) {
                val removeNeededNodes = inDegree.filterValues { it == UInt64.zero }
                if (removeNeededNodes.isNotEmpty()) {
                    for ((node, _) in removeNeededNodes) {
                        for (edge in graph[node]) {
                            inDegree[edge.to] = inDegree[edge.to]!! - UInt64.one
                        }
                        nodes.add(node)
                        inDegree.remove(node)
                    }
                } else {
                    val minInDegree = inDegree.values.min()
                    val minInDegreeNodes = inDegree.filterValues { it == minInDegree }
                    nodes.addAll(minInDegreeNodes.keys.toList().sortedBy { it.time })
                    for ((node, _) in minInDegreeNodes) {
                        for (edge in graph[node]) {
                            inDegree[edge.to] = inDegree[edge.to]!! - UInt64.one
                        }
                        inDegree.remove(node)
                    }
                }
            }
            return nodes
        }
    }

    private val enabledTime by aircraftUsability::enabledTime
    private val nodes = if (!configuration.withOrderChange) { sortNodes(graph) } else { emptyList() }

    /**
     * 为给定的迭代和影子价格映射生成束。Generates bunches for the given iteration and shadow price map.
     *
     * @param iteration The current iteration index / 当前迭代索引
     * @param shadowPriceMap The shadow price map for dual values / 用于对偶值的影子价格映射
     * @return The list of generated flight task bunches / 生成的航班任务束列表
    */
    operator fun invoke(iteration: Int64, shadowPriceMap: ShadowPriceMap): Ret<List<FlightTaskBunch>> {
        val labels: LabelMap = HashMap()
        initRootLabel(labels, shadowPriceMap)

        if (configuration.withOrderChange) {
            val labelDeque = ArrayList<Label>()
            labelDeque.addAll(labels[graph[Node.root]!!]!!)

            while (labelDeque.isNotEmpty()) {
                val prevLabel = labelDeque.first()
                labelDeque.removeFirst()
                val prevNode = prevLabel.node
                val edges = graph[prevNode].sortedBy { it.to.time }

                for (edge in edges) {
                    val succNode = edge.to
                    val succLabels = getLabels(labels, succNode)
                    if (succNode is EndNode) {
                        if (prevNode !is RootNode) {
                            val builder = LabelBuilder(succNode, prevLabel)
                            builder.shadowPrice += shadowPriceMap(prevLabel.flightTask!!, null as FlightTask?)
                            insertLabel(succLabels, Label(builder))
                        }
                    } else if (!prevLabel.visited(succNode)) {
                        val succLabel = generateFlightTaskLabel(prevLabel, succNode, shadowPriceMap)
                        if (succLabel != null) {
                            insertLabel(succLabels, succLabel)
                            labelDeque.add(succLabel)
                        }
                    }
                }
            }
        } else {
            for (prevNode in nodes) {
                for (prevLabel in getLabels(labels, prevNode)) {
                    for (edge in graph[prevNode]) {
                        val succNode = edge.to
                        val succLabels = getLabels(labels, succNode)

                        if (succNode is EndNode) {
                            if (prevNode !is RootNode) {
                                val builder = LabelBuilder(succNode, prevLabel)
                                builder.shadowPrice += shadowPriceMap(prevLabel.flightTask!!, null as FlightTask?)
                                insertLabel(succLabels, Label(builder))
                            }
                        } else if (!prevLabel.visited(succNode)) {
                            val succLabel = generateFlightTaskLabel(prevLabel, succNode, shadowPriceMap)
                            if (succLabel != null) {
                                insertLabel(succLabels, succLabel)
                            }
                        }
                    }
                }
            }
        }
        return Ok(selectBunches(iteration, labels[EndNode]!!))
    }

    /**
     * Initializes the root label in the label map with the aircraft's enabled time and shadow price.
     * 使用航空器的启用时间和影子价格初始化标签映射中的根标签。
     *
     * @param labels The mutable label map to initialize / 要初始化的可变标签映射
     * @param shadowPriceMap The shadow price map for dual values / 用于对偶值的影子价格映射
    */
    private fun initRootLabel(labels: LabelMap, shadowPriceMap: ShadowPriceMap) {
        assert(labels.isEmpty())
        val rootNode = graph[Node.root]!!
        val builder = LabelBuilder(rootNode, enabledTime)
        builder.shadowPrice += shadowPriceMap(aircraft)
        labels[rootNode] = mutableListOf(Label(builder))
    }

    /**
     * Retrieves the list of labels for the given node, creating an empty list if none exists.
     * 获取给定节点的标签列表，若不存在则创建空列表。
     *
     * @param labels The mutable label map / 可变标签映射
     * @param node The node whose labels to retrieve / 要获取标签的节点
     * @return The mutable list of labels for the node / 该节点的可变标签列表
    */
    private fun getLabels(labels: LabelMap, node: Node): MutableList<Label> {
        if (!labels.containsKey(node)) {
            labels[node] = ArrayList()
        }
        return labels[node]!!
    }

    /**
     * Generates a new label for the successor task node by computing departure time, recovery flight task, cost, and shadow price.
     * 通过计算出发时间、恢复航班任务、代价和影子价格，为后继任务节点生成新标签。
     *
     * @param prevLabel The predecessor label / 前驱标签
     * @param succNode The successor task node / 后继任务节点
     * @param shadowPriceMap The shadow price map for dual values / 用于对偶值的影子价格映射
     * @return The generated label, or null if the task is infeasible / 生成的标签，若任务不可行则返回 null
    */
    private fun generateFlightTaskLabel(prevLabel: Label, succNode: Node, shadowPriceMap: ShadowPriceMap): Label? {
        assert(succNode is TaskNode)
        succNode as TaskNode

        val succTask = succNode.task

        val minDepTime = getMinDepartureTime(prevLabel, succNode)
        val duration = succTask.duration(aircraft)
        val time = if (succTask.scheduledTime != null) {
            TimeRange(minDepTime, minDepTime + duration)
        } else {
            if ((minDepTime + duration) > succTask.timeWindow!!.end) {
                return null
            }
            TimeRange(minDepTime, minDepTime + duration)
        }

        val prevArr = if (prevLabel.node is RootNode) {
            succTask.dep
        } else {
            prevLabel.flightTask!!.arr
        }
        val recoveryTask = generateRecoveryFlightTask(prevArr, succTask, time) ?: return null

        val flightHour = prevLabel.flightHour + (recoveryTask.flightHour ?: FlightHour.zero)
        val flightCycle = prevLabel.flightCycle + recoveryTask.flightCycle
        val cost = if (prevLabel.node is RootNode) {
            costCalculator(aircraft, aircraftUsability.lastTask as? FlightTask, recoveryTask, flightHour, flightCycle)
        } else {
            costCalculator(aircraft, prevLabel.flightTask!!, recoveryTask, flightHour, flightCycle)
        }
        if (cost == null) {
            return null
        }
        val shadowPrice = if (prevLabel.node is RootNode) {
            shadowPriceMap(aircraftUsability.lastTask as? FlightTask, recoveryTask)
        } else {
            shadowPriceMap(prevLabel.flightTask!!, recoveryTask)
        }

        val builder = LabelBuilder(succNode, prevLabel, recoveryTask)
        @Suppress("UNCHECKED_CAST")
        builder.cost += cost as Cost<FltX>
        builder.shadowPrice += shadowPrice
        return Label(builder)
    }

    /**
     * Generates a recovery flight task by applying aircraft assignment and route recovery to the successor task.
     * 通过对后继任务应用航空器分配和航线恢复来生成恢复航班任务。
     *
     * @param dep The departure airport / 出发机场
     * @param succTask The successor flight task / 后继航班任务
     * @param time The time range for the recovery task / 恢复任务的时间范围
     * @return The recovery flight task, or null if recovery is not enabled / 恢复航班任务，若恢复未启用则返回 null
    */
    private fun generateRecoveryFlightTask(dep: Airport, succTask: FlightTask, time: TimeRange): FlightTask? {
        val aircraft: Aircraft? = if (succTask.aircraft == null || succTask.aircraft != this.aircraft) {
            this.aircraft
        } else {
            null
        }

        val arr = succTask.actualArr(dep) ?: return null

        val recoveryPolicy = FlightTaskAssignment(
            aircraft = aircraft,
            time = time,
            route = Route(dep, arr)
        )

        return if (recoveryPolicy.empty) {
            succTask
        } else if (!succTask.recoveryEnabled(recoveryPolicy)) {
            null
        } else {
            succTask.recovery(recoveryPolicy)
        }
    }

    /**
     * Inserts a label into the label list, applying dominance pruning and capacity limits for task nodes.
     * 将标签插入标签列表，对任务节点应用支配剪枝和容量限制。
     *
     * @param labels The mutable list of labels for the node / 该节点的可变标签列表
     * @param label The label to insert / 要插入的标签
    */
    private fun insertLabel(labels: MutableList<Label>, label: Label) {
        when (label.node) {
            is TaskNode -> {
                if (labels.any { it ls label }) {
                    return
                }
                labels.removeAll { label ls it }
                if (labels.size > configuration.maximumLabelPerNode.toInt()) {
                    for (i in configuration.maximumLabelPerNode.toInt() until labels.size) {
                        labels.removeAt(i)
                    }
                }

                for (i in labels.indices) {
                    if ((label.reducedCost - labels[i].reducedCost) < Flt64.zero) {
                        labels.add(i, label)
                        return
                    }
                }
                labels.add(label)
            }
            is EndNode -> {
                labels.add(label)
            }
            else -> { }
        }
    }

    /**
     * Selects the best flight task bunches from end-node labels based on reduced cost, up to the configured maximum.
     * 根据缩减代价从终端节点标签中选择最优航班任务束，直到达到配置的最大数量。
     *
     * @param iteration The current iteration index / 当前迭代索引
     * @param labels The list of labels at the end node / 终端节点的标签列表
     * @return The list of selected flight task bunches / 选中的航班任务束列表
    */
    private fun selectBunches(iteration: Int64, labels: List<Label>): List<FlightTaskBunch> {
        val bunches = ArrayList<FlightTaskBunch>()
        val sortedLabels = labels.asIterable().filter { it.isBetterBunch }.sortedBy { it.reducedCost }
        for (label in sortedLabels) {
            val newBunch = label.generateBunch(iteration, aircraft, aircraftUsability, totalCostCalculator)
            if (newBunch != null) {
                bunches.add(newBunch)
            }
            if (bunches.size == configuration.maximumColumnGeneratedPerAircraft.toInt()) {
                break
            }
        }
        return bunches
    }

    /**
     * Computes the minimum departure time for the successor task node considering connection time and aircraft usability.
     * 考虑连接时间和航空器可用性，计算后继任务节点的最早出发时间。
     *
     * @param prevLabel The predecessor label / 前驱标签
     * @param succNode The successor task node / 后继任务节点
     * @return The minimum departure time instant / 最早出发时间时刻
    */
    private fun getMinDepartureTime(prevLabel: Label, succNode: Node): Instant {
        assert(succNode is TaskNode)
        val thisFlightTask = (succNode as TaskNode).task
        return if (prevLabel.node is RootNode && aircraftUsability.lastTask == null) {
            minimumDepartureTimeCalculator(prevLabel.arrivalTime, aircraft, thisFlightTask, 0.minutes)
        } else {
            val prevFlightTask = if (prevLabel.node is RootNode) {
                aircraftUsability.lastTask as FlightTask
            } else {
                prevLabel.flightTask!!
            }
            val connectionTime = connectionTimeCalculator(aircraft, prevFlightTask, thisFlightTask)
            minimumDepartureTimeCalculator(prevLabel.arrivalTime, aircraft, thisFlightTask, connectionTime)
        }
    }
}
