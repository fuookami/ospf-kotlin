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
 * @property withOrderChange 参数。
 * @property maximumLabelPerNode 参数。
 */
data class BunchGenerationConfiguration(
    val withOrderChange: Boolean = false,
    val maximumLabelPerNode: UInt64 = UInt64(100UL),
    val maximumColumnGeneratedPerAircraft: UInt64 = UInt64(10UL)
)

/** 标号设置算法的内部标签。Internal label for the Label Setting algorithm. */
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

/** 标号设置算法的内部标签。Internal label for the Label Setting algorithm. */
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
 * @property private val aircraft 参数。
 * @property private val aircraftUsability 参数。
 * @property private val graph 参数。
 * @property private val connectionTimeCalculator 参数。
 * @property private val minimumDepartureTimeCalculator 参数。
 * @property private val costCalculator 参数。
 * @property private val totalCostCalculator 参数。
 * @property private val configuration 参数。
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
     * Generates bunches for the given iteration and shadow price map.
 *
     * @param iteration 参数。
     * @param shadowPriceMap 参数。
     * @return 返回结果。
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

    private fun initRootLabel(labels: LabelMap, shadowPriceMap: ShadowPriceMap) {
        assert(labels.isEmpty())
        val rootNode = graph[Node.root]!!
        val builder = LabelBuilder(rootNode, enabledTime)
        builder.shadowPrice += shadowPriceMap(aircraft)
        labels[rootNode] = mutableListOf(Label(builder))
    }

    private fun getLabels(labels: LabelMap, node: Node): MutableList<Label> {
        if (!labels.containsKey(node)) {
            labels[node] = ArrayList()
        }
        return labels[node]!!
    }

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
