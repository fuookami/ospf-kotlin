@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkNodeId
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.path.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.network.NetworkBunchGenerationContext
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 批次生成配置。Configuration for bunch generation.
 *
 * @property withOrderChange 批次生成时是否允许顺序变更 / Whether to allow order changes during bunch generation
 * @property maximumLabelPerNode 每个节点保留的最大标签数 / Maximum number of labels retained per node
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
            arrivalTime = builder.arrivalTime,
            flightHour = builder.flightHour,
            flightCycle = builder.flightCycle,
            prevLabel = builder.prevLabel,
            node = builder.node,
            flightTask = builder.flightTask
        )
    }

    val reducedCost: Flt64 get() = cost.solverCostOrNull(Flt64.zero)!! - shadowPrice
    val isBetterBunch get() = reducedCost < Flt64.zero

    fun flightTasks(): List<FlightTask> {
        val labels = ArrayList<Label>()
        var label = prevLabel
        while (label != null && label.node !is RootNode) {
            labels.add(label)
            label = label.prevLabel
        }

        val flightTasks = ArrayList<FlightTask>(labels.size)
        while (labels.isNotEmpty()) {
            flightTasks.add(labels.removeLast().flightTask ?: return emptyList())
        }
        return flightTasks
    }

    fun withTotalCost(totalCost: Cost<*>): Label? {
        if (!totalCost.valid) return null
        @Suppress("UNCHECKED_CAST")
        return copy(cost = totalCost as Cost<FltX>)
    }

    /**
     * Generates a flight task bunch from this label by tracing back through predecessor labels.
     * 通过回溯前驱标签从此标签生成航班任务束。
     *
     * @param iteration 当前迭代索引 / The current iteration index
     * @param aircraft 分配给此束的航空器 / The aircraft assigned to this bunch
     * @return 使用已验证总代价的航班任务束 / A bunch with its validated total cost
    */
    fun generateBunch(
        iteration: Int64,
        aircraft: Aircraft
    ): FlightTaskBunch? {
        assert(node is EndNode)
        val flightTasks = flightTasks()
        if (flightTasks.isEmpty()) return null
        return FlightTaskBunch(aircraft, flightTasks, iteration, cost)
    }
}

/**
 * 使用标号设置算法生成航班任务束。Generates flight task bunches using Label Setting algorithm.
 *
 * @property aircraft 生成束的飞机 / The aircraft for which bunches are generated
 * @property aircraftUsability 飞机可用性上下文 / The aircraft usability context
 * @property graph 批次生成的路线图 / The route graph for bunch generation
 * @property connectionTimeCalculator 任务间的连接时间计算器 / The connection time calculator between tasks
 * @property minimumDepartureTimeCalculator 最早出发时间计算器 / The minimum departure time calculator
 * @property costCalculator 增量代价计算器 / The incremental cost calculator
 * @property totalCostCalculator 束的总代价计算器 / The total cost calculator for a bunch
 * @property configuration 批次生成配置 / The bunch generation configuration
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
    private val enabledTime by aircraftUsability::enabledTime

    /** 最近一次生成合同，不将预算截断视为收敛。 / Latest generation contract; truncation is not convergence. */
    var lastBatch: ColumnGenerationBatch<FlightTaskBunch>? = null
        private set

    /** 最近一次 pricing 统计 / Diagnostics from the most recent pricing round. */
    var diagnostics: PricingDiagnostics = PricingDiagnostics()
        private set

    /**
     * 为给定的迭代和影子价格映射生成束。Generates bunches for the given iteration and shadow price map.
     *
     * @param iteration 当前迭代索引 / The current iteration index
     * @param shadowPriceMap 用于对偶值的影子价格映射 / The shadow price map for dual values
     * @return 生成的航班任务束列表 / The list of generated flight task bunches
    */
    operator fun invoke(iteration: Int64, shadowPriceMap: ShadowPriceMap): Ret<List<FlightTaskBunch>> {
        val currentDiagnostics = PricingDiagnostics()
        diagnostics = currentDiagnostics
        val topology = when (val result = graph.network()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val builder = LabelBuilder(RootNode, enabledTime)
        builder.shadowPrice += shadowPriceMap(aircraft)
        val validTerminalLabelIds = mutableSetOf<Int>()
        val problem = PathProblem(
            graph = topology,
            source = NetworkNodeId(RootNode.index.toString()),
            initial = Label(builder),
            transition = { previous, edge ->
                val label = previous.state
                if (edge.to is EndNode) {
                    if (label.node is RootNode) {
                        Ok(null)
                    } else {
                        val end = LabelBuilder(EndNode, label)
                        end.shadowPrice += shadowPriceMap(label.flightTask!!, null as FlightTask?)
                        Ok(Label(end))
                    }
                } else {
                    currentDiagnostics.candidateTaskCount += 1
                    val successor = generateFlightTaskLabel(label, edge.to, shadowPriceMap)
                    if (successor == null) currentDiagnostics.infeasibleExpansionCount += 1
                    else currentDiagnostics.feasibleExpansionCount += 1
                    Ok(successor)
                }
            },
            terminal = { _, state -> state.node is EndNode },
            accept = { it.isBetterBunch },
            terminalContribution = { pathLabel ->
                val flightTasks = pathLabel.state.flightTasks()
                val totalCost = totalCostCalculator(aircraft, flightTasks)
                val terminalState = totalCost?.let(pathLabel.state::withTotalCost)
                if (flightTasks.isEmpty() || terminalState == null) {
                    currentDiagnostics.invalidColumnCount += 1
                    Ok(null)
                } else {
                    validTerminalLabelIds.add(pathLabel.index)
                    Ok(terminalState)
                }
            },
            // 不沿用忽略到达状态与访问历史的旧支配。 / Do not reuse old dominance ignoring arrival state and history.
            order = compareBy { it.reducedCost }
        )
        val configuredLabelLimit = configuration.maximumLabelPerNode.toInt()
        val labelLimit = configuredLabelLimit.coerceAtLeast(1)
        val batch = when (val result = NetworkBunchGenerationContext().generate(
            problem = problem,
            budget = PathSearchBudget(
                maximumLabels = if (configuredLabelLimit < 1) 1 else Int.MAX_VALUE,
                maximumLabelsPerNode = labelLimit
            ),
            scope = ColumnGenerationScope(
                request = iteration.toString(),
                group = aircraft.toString(),
                branch = "demo4-business-domain",
                global = false
            ),
            maximumResults = configuration.maximumColumnGeneratedPerAircraft.toInt().coerceAtLeast(0),
            noImprovementEvidence = { search ->
                currentDiagnostics.invalidColumnCount == 0 && search.labels.none {
                    it.index in validTerminalLabelIds && it.state.node is EndNode && it.state.isBetterBunch
                }
            },
            onSearchCompleted = { search ->
                currentDiagnostics.expandedLabelCount = search.expandedLabels
                currentDiagnostics.dominatedLabelCount = search.dominatedLabels
                currentDiagnostics.negativeReducedCostLabelCount = search.labels.count {
                    it.index in validTerminalLabelIds && it.state.node is EndNode && it.state.isBetterBunch
                }
                currentDiagnostics.searchTruncatedByLabelLimit = search.completion == PathSearchCompletion.BudgetLimit
                currentDiagnostics.labelLimitReachedCount = if (currentDiagnostics.searchTruncatedByLabelLimit) 1 else 0
                currentDiagnostics.noFeasiblePath = search.terminalCount == 0 &&
                    currentDiagnostics.invalidColumnCount == 0 && search.complete
            },
            extract = { label, _ ->
                Ok(label.state.generateBunch(iteration, aircraft))
            }
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        lastBatch = batch
        currentDiagnostics.generatedColumnCount = batch.columns.size
        currentDiagnostics.noNegativeReducedCost = currentDiagnostics.negativeReducedCostLabelCount == 0 &&
            currentDiagnostics.invalidColumnCount == 0 &&
            batch.completion == ColumnGenerationCompletion.Exhausted
        return Ok(batch.columns)
    }

    /**
     * Generates a new label for the successor task node by computing departure time, recovery flight task, cost, and shadow price.
     * 通过计算出发时间、恢复航班任务、代价和影子价格，为后继任务节点生成新标签。
     *
     * @param prevLabel 前驱标签 / The predecessor label
     * @param succNode 后继任务节点 / The successor task node
     * @param shadowPriceMap 用于对偶值的影子价格映射 / The shadow price map for dual values
     * @return 生成的标签，若任务不可行则返回 null / The generated label, or null if the task is infeasible
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
        if (cost == null || !cost.valid) {
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
     * @param dep 出发机场 / The departure airport
     * @param succTask 后继航班任务 / The successor flight task
     * @param time 恢复任务的时间范围 / The time range for the recovery task
     * @return 恢复航班任务，若恢复未启用则返回 null / The recovery flight task, or null if recovery is not enabled
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
     * Computes the minimum departure time for the successor task node considering connection time and aircraft usability.
     * 考虑连接时间和航空器可用性，计算后继任务节点的最早出发时间。
     *
     * @param prevLabel 前驱标签 / The predecessor label
     * @param succNode 后继任务节点 / The successor task node
     * @return 最早出发时间时刻 / The minimum departure time instant
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
