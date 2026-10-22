@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.service

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.path.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.pricing.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.policy.LabelDominancePolicy
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/**
 * ESPPRC 精确定价器。 / ESPPRC exact pricer.
 *
 * 使用基于标签扩展的 Elementarity Shortest Path Problem with Resource Constraints 算法。
 * 支持：
 * - 单路线 elementarity
 * - reduced cost、time、load 三资源支配
 * - 基于 elementarity 和容量单调性的客户排除标记
 * - 前驱索引链回溯生成完整 Route 和 RouteStop
 * - branch mask 在构图和扩展时均生效
 * - 只返回 reduced cost < -pricingTolerance 的列
 *
 * Uses label-extension-based ESPPRC algorithm.
 */
class EspprcPricer<V : FloatingNumber<V>>(
    private val instance: VrptwInstance<V>,
    private val valueAdapter: NetworkSchedulingSolverValueAdapter<V>,
    private val dominancePolicy: LabelDominancePolicy = LabelDominancePolicy.Default,
    private val routeCostPolicy: RouteCostPolicy<V> = FixedPlusArcCostPolicy(instance.units.costUnit)
) {
    /**
     * 执行精确定价。 / Execute exact pricing.
     *
     * @param graph 定价图 / Pricing graph
     * @param request 定价请求 / Pricing request
     * @return 定价结果 / Pricing result
     */
    fun price(graph: PricingGraph<V>, request: PricingRequest<V>): Ret<PricingResult<V>> {
        val pricingTolerance = valueAdapter.fromSolverValue(request.pricingTolerance).value
            ?: return networkSchedulingFailure(
                "执行定价失败：定价容差转换失败 / Failed to execute pricing: pricing tolerance conversion failed"
            )
        val customerCount = graph.customerCount
        val endDepotIdx = graph.endDepotIndex
        val zero = graph.vehicleCapacity.constants.zero
        val negativePricingTolerance = zero - pricingTolerance
            ?: return networkSchedulingFailure(
                "执行定价失败：无法计算负定价容差 / Failed to execute pricing: cannot calculate negative pricing tolerance"
            )

        val topology = when (val result = NetworkGraph(
            nodes = graph.nodes,
            arcs = graph.arcs,
            access = NetworkGraphAccess<PricingNode<V>, PricingArc<V>>(
                nodeId = { it.nodeId },
                arcId = { it.id },
                from = { it.fromNodeId },
                to = { it.toNodeId }
            )
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val start = graph.nodes[graph.startDepotIndex]
        val root = EspprcLabel(
            reducedCost = zero,
            time = start.readyTime,
            load = zero,
            currentNode = start.nodeId,
            visited = VisitedCustomers.empty(customerCount),
            forbidden = ForbiddenCustomers.empty(customerCount),
            predecessor = -1,
            additiveObjectiveCost = zero
        )
        // 仅应用显式认证的支配策略；默认策略比较全部未来相关资源，负行驶时间已在构图时拒绝。
        // Apply only certified dominance; the default compares all future-relevant resources and negative travel times are rejected during graph construction.
        val exactDominance: ((PathLabel<EspprcLabel<V>, PricingArc<V>>, PathLabel<EspprcLabel<V>, PricingArc<V>>) -> Boolean)? =
            if (routeCostPolicy.isAdditive && dominancePolicy.isExact) {
                { a, b -> dominancePolicy.dominates(a.state, b.state) }
            } else {
                null
            }
        val problem = PathProblem(
            graph = topology,
            source = start.nodeId,
            initial = root,
            transition = { previous, arc ->
                val current = previous.state
                val next = graph.nodes[arc.toIndex]
                if (!next.isDepot && (current.visited.contains(next.customerIndex) || current.forbidden.contains(next.customerIndex))) {
                    ok(null)
                } else {
                    val serviceStart = maxOf(current.time + arc.travelTime, next.readyTime)
                    val load = current.load + next.demand
                    if (serviceStart gr next.dueTime || load gr graph.vehicleCapacity) {
                        ok(null)
                    } else {
                        val visited = if (next.isDepot) current.visited else current.visited.add(next.customerIndex)
                        val departure = serviceStart + next.serviceTime
                        ok(EspprcLabel(
                            reducedCost = current.reducedCost + arc.reducedCost,
                            time = departure,
                            load = load,
                            currentNode = next.nodeId,
                            visited = visited,
                            forbidden = updateForbiddenCustomers(current.forbidden, next, visited, graph, load),
                            predecessor = previous.index,
                            arcCosts = current.arcCosts + arc.arcCost,
                            additiveObjectiveCost = (current.additiveObjectiveCost ?: zero) + arc.objectiveCost
                        ))
                    }
                }
            },
            terminal = { node, _ -> node == graph.nodes[endDepotIdx].nodeId },
            accept = { it.reducedCost ls negativePricingTolerance },
            terminalContribution = { terminal ->
                val state = terminal.state
                val vehicleType = instance.vehicleTypeById[request.vehicleTypeId]
                if (vehicleType == null) {
                    networkSchedulingFailure(
                        "执行定价失败：车辆类型不存在 / Failed to execute pricing: vehicle type does not exist"
                    )
                } else {
                    when (val result = routeCostPolicy.cost(vehicleType, state.arcCosts)) {
                        is Ok -> when (val normalized = valueAdapter.normalizeValue(result.value, instance.units.costUnit)) {
                            is Ok -> {
                                if (request.duals.phase == PricingPhase.PhaseTwo && state.additiveObjectiveCost == null) {
                                    networkSchedulingFailure(
                                        "执行定价失败：缺少增量目标成本 / Failed to execute pricing: accumulated additive objective cost is missing"
                                    )
                                } else {
                                    val reducedCost = if (request.duals.phase == PricingPhase.PhaseTwo) {
                                        state.reducedCost - (state.additiveObjectiveCost ?: zero) + normalized.value
                                    } else {
                                        state.reducedCost
                                    }
                                    ok(state.copy(reducedCost = reducedCost, routeCost = normalized.value))
                                }
                            }
                            is Failed -> Failed(normalized.error)
                            is Fatal -> Fatal(normalized.errors)
                        }
                        is Failed -> Failed(result.error)
                        is Fatal -> Fatal(result.errors)
                    }
                }
            },
            dominates = exactDominance,
            dominanceIsExact = exactDominance != null,
            order = compareBy { it.reducedCost }
        )
        val search = when (val result = PathSearchContext().search(
            problem = problem,
            budget = PathSearchBudget(shouldStop = { request.interruptionChecker.shouldStop() })
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val minReducedCost = search.paths.minOfOrNull { it.state.reducedCost } ?: zero
        val interrupted = search.completion == PathSearchCompletion.Interrupted
        val exactPricingComplete = search.complete && minReducedCost geq negativePricingTolerance

        // 回溯生成路线 / Backtrack to generate routes
        val routes = mutableListOf<Route<V>>()

        for (pathLabel in search.paths) {
            if (routes.size >= request.maxColumnsPerPricing) break
            val route = backtrackRoute(search.trace(pathLabel), graph, request)
            when (route) {
                is Ok -> routes.add(route.value)
                is Failed -> return Failed(route.error)
                is Fatal -> return Fatal(route.errors)
            }
        }

        return Ok(PricingResult(
            routes = routes,
            minReducedCost = minReducedCost,
            exactPricingComplete = exactPricingComplete,
            interrupted = interrupted
        ))
    }

    /**
     * 保守的不可扩展客户标记。 / Conservative marking of customers that cannot be extended.
     *
     * 仅标记由 elementarity 和容量单调性必然排除的客户。
     * Only mark customers excluded by elementarity or monotone capacity bounds.
     *
     * 不能根据单条 outgoing 弧的时间窗结果永久标记客户：非 metric 图中可能存在
     * 经其他客户到达的替代路径，平行弧也可能有不同的可行性。
     * A single outgoing arc must not permanently forbid a customer: non-metric graphs
     * may provide an alternative path through another customer, and parallel arcs may differ.
     */
    private fun updateForbiddenCustomers(
        currentForbidden: ForbiddenCustomers,
        toNode: PricingNode<V>,
        visited: VisitedCustomers,
        graph: PricingGraph<V>,
        currentLoad: V
    ): ForbiddenCustomers {
        var forbidden = currentForbidden
        if (toNode.isDepot) return forbidden

        // 标记当前访问的客户为 forbidden（不可再访问）
        forbidden = forbidden.add(toNode.customerIndex)

        // 容量单调性：客户需求非负，因此当前负载超出容量后不可能通过后续访问恢复。
        // Monotone capacity: demands are non-negative, so an over-capacity candidate can never recover later.
        for (candidateNode in graph.nodes) {
            if (candidateNode.isDepot) continue
            if (visited.contains(candidateNode.customerIndex)) continue
            if (forbidden.contains(candidateNode.customerIndex)) continue

            // 容量不可达检查 / Capacity unreachable check
            val loadAtCandidate = currentLoad + candidateNode.demand
            if (loadAtCandidate gr graph.vehicleCapacity) {
                forbidden = forbidden.add(candidateNode.customerIndex)
            }
        }

        return forbidden
    }

    /**
     * 从标签回溯生成完整路线。 / Backtrack from label to generate complete route.
     */
    private fun backtrackRoute(
        pathLabels: List<PathLabel<EspprcLabel<V>, PricingArc<V>>>,
        graph: PricingGraph<V>,
        request: PricingRequest<V>
    ): Ret<Route<V>> {
        val schedulingWindow = instance.schedulingWindow
        val vehicleType = instance.vehicleTypeById[request.vehicleTypeId]
            ?: return networkSchedulingFailure(
                "回溯路线失败：车辆类型不存在 / Failed to backtrack route: vehicle type does not exist"
            )

        val path = pathLabels.map { it.state }
        val pathArcs = pathLabels.drop(1).mapNotNull { it.incoming }
        if (pathArcs.size != path.size - 1) {
            return networkSchedulingFailure(
                "回溯路线失败：前驱链缺少弧 / Failed to backtrack route: predecessor chain is missing an arc"
            )
        }
        val totalCost = path.lastOrNull()?.routeCost
            ?: return networkSchedulingFailure(
                "回溯路线失败：终点路线成本缺失 / Failed to backtrack route: terminal route cost is missing"
            )

        // 构建 RouteStop 列表 / Build RouteStop list
        val stops = mutableListOf<RouteStop<V>>()
        val zero = graph.vehicleCapacity.constants.zero
        var accumulatedLoad = Quantity(zero, instance.units.loadUnit)
        var totalDistance = zero

        for (i in path.indices) {
            val nodeLabel = path[i]
            val nodeIdx = graph.nodeIndexMap[nodeLabel.currentNode] ?: continue
            val node = graph.nodes[nodeIdx]

            // nodeLabel.time = departure time = serviceStart + serviceTime
            // arrival = departure - serviceTime (如果无等待) 或 readyTime (如果有等待)
            // 更准确：从前驱弧推算 arrival
            val arrivalTime: V = if (i == 0) {
                // 起始 depot：arrival = departure = readyTime
                nodeLabel.time
            } else {
                // 从前驱弧推算 arrival = previous departure + travel time
                val prevLabel = path[i - 1]
                val arc = pathArcs.getOrNull(i - 1)
                if (arc != null) {
                    prevLabel.time + arc.travelTime
                } else {
                    // fallback: departure - serviceTime
                    val fallbackArrivalTime: V? = nodeLabel.time - node.serviceTime
                    fallbackArrivalTime
                        ?: return networkSchedulingFailure(
                            "回溯路线失败：无法计算到达时间 / Failed to backtrack route: cannot calculate arrival time"
                        )
                }
            }
            val serviceStartTime = maxOf(arrivalTime, node.readyTime)
            val departureTime = nodeLabel.time

            val arrivalInstant = schedulingWindow.instantOf(arrivalTime)
            val serviceStartInstant = schedulingWindow.instantOf(serviceStartTime)
            val departureInstant = schedulingWindow.instantOf(departureTime)

            val customerId = if (node.isDepot) null
                else instance.customers[node.customerIndex].id

            if (!node.isDepot) {
                accumulatedLoad = Quantity(accumulatedLoad.value + node.demand, instance.units.loadUnit)
            }

            // 累加弧距离 / Accumulate arc distance
            if (i > 0) {
                val arc = pathArcs.getOrNull(i - 1)
                if (arc != null) {
                    totalDistance = totalDistance + arc.distance
                }
            }

            stops.add(RouteStop(
                nodeId = node.nodeId,
                customerId = customerId,
                arrival = arrivalInstant,
                serviceStart = serviceStartInstant,
                departure = departureInstant,
                accumulatedLoad = accumulatedLoad
            ))
        }

        return Route(
            vehicleTypeId = request.vehicleTypeId,
            stops = stops,
            distance = Quantity(totalDistance, instance.units.distanceUnit),
            cost = Quantity(totalCost, instance.units.costUnit),
            arcIds = pathArcs.map { it.id }
        )
    }
}
