@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.service

import kotlin.time.Duration
import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
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
 * - Feillet 2004 不可达客户提前标记
 * - 前驱索引链回溯生成完整 Route 和 RouteStop
 * - branch mask 在构图和扩展时均生效
 * - 只返回 reduced cost < -pricingTolerance 的列
 *
 * Uses label-extension-based ESPPRC algorithm.
 */
class EspprcPricer<V : RealNumber<V>>(
    private val instance: VrptwInstance<V>,
    private val valueAdapter: NetworkSchedulingSolverValueAdapter<V>,
    private val dominancePolicy: LabelDominancePolicy = LabelDominancePolicy.Default
) {
    /**
     * 执行精确定价。 / Execute exact pricing.
     *
     * @param graph 定价图 / Pricing graph
     * @param request 定价请求 / Pricing request
     * @return 定价结果 / Pricing result
     */
    fun price(graph: PricingGraph, request: PricingRequest<V>): Ret<PricingResult<V>> {
        val customerCount = graph.customerCount
        val endDepotIdx = graph.endDepotIndex

        // 每个节点的活跃标签索引列表 / Active label indices at each node
        val labelsAtNode = Array(graph.nodes.size) { mutableListOf<Int>() }
        // dominated 标记数组 / Dominated marker array
        val dominated = mutableSetOf<Int>()
        // 全局标签列表（用于前驱回溯） / Global label list (for predecessor backtracking)
        val allLabels = mutableListOf<EspprcLabel>()

        // 创建根标签（在起始 depot，时间为 0，负载为 0） / Create root label
        val startNode = graph.nodes[graph.startDepotIndex]
        val rootLabel = EspprcLabel(
            reducedCost = Flt64.zero,
            time = startNode.readyTime,
            load = Flt64.zero,
            currentNode = startNode.nodeId,
            visited = VisitedCustomers.empty(customerCount),
            forbidden = ForbiddenCustomers.empty(customerCount),
            predecessor = -1,
            routeIndex = -1
        )
        allLabels.add(rootLabel)
        labelsAtNode[graph.startDepotIndex].add(0)

        // BFS 式标签扩展 / BFS-style label extension
        var labelIdCounter = 1
        var minReducedCost = Flt64.zero
        val negativeLabels = mutableListOf<EspprcLabel>()

        // 使用队列逐层扩展 / Use queue for level-by-level extension
        val queue = ArrayDeque<Int>()
        queue.add(0) // root label index
        var interrupted = false

        while (queue.isNotEmpty()) {
            if (request.interruptionChecker.shouldStop()) {
                interrupted = true
                break
            }
            val currentLabelIdx = queue.removeFirst()
            if (currentLabelIdx in dominated) continue

            val currentLabel = allLabels[currentLabelIdx]
            val currentNodeIdx = graph.nodeIndexMap[currentLabel.currentNode] ?: continue

            // 扩展到所有后继弧 / Extend to all successor arcs
            for (arc in graph.outgoingFrom(currentNodeIdx)) {
                if (request.interruptionChecker.shouldStop()) {
                    interrupted = true
                    break
                }
                val toNode = graph.nodes[arc.toIndex]

                // elementarity 检查：不能重复访问客户 / Elementarity check
                if (!toNode.isDepot && currentLabel.visited.contains(toNode.customerIndex)) continue

                // 不可达客户检查 / Forbidden customer check
                if (!toNode.isDepot && currentLabel.forbidden.contains(toNode.customerIndex)) continue

                // 时间资源扩展 / Time resource extension
                val arrivalTime = currentLabel.time + arc.travelTime
                val serviceStartTime = maxOf(arrivalTime, toNode.readyTime)

                // 时间窗可行性 / Time window feasibility
                if (serviceStartTime gr toNode.dueTime) continue

                // 负载资源扩展 / Load resource extension
                val newLoad = currentLabel.load + toNode.demand

                // 容量可行性 / Capacity feasibility
                if (newLoad gr graph.vehicleCapacity) continue

                // Reduced cost 扩展 / Reduced cost extension
                val newReducedCost = currentLabel.reducedCost + arc.reducedCost

                // 更新 visited / Update visited
                val newVisited = if (toNode.isDepot) currentLabel.visited
                    else currentLabel.visited.add(toNode.customerIndex)

                // Feillet 2004 不可达客户标记 / Feillet 2004 unreachable customer marking
                val departureTimeFromToNode = serviceStartTime + toNode.serviceTime
                val newForbidden = updateForbiddenCustomers(
                    currentLabel.forbidden, toNode, newVisited, graph,
                    departureTimeFromToNode, newLoad
                )

                // 创建新标签 / Create new label
                val newLabel = EspprcLabel(
                    reducedCost = newReducedCost,
                    time = serviceStartTime + toNode.serviceTime,
                    load = newLoad,
                    currentNode = toNode.nodeId,
                    visited = newVisited,
                    forbidden = newForbidden,
                    predecessor = currentLabelIdx,
                    routeIndex = -1
                )

                // 支配检查 / Dominance check
                val dominatedByExisting = labelsAtNode[arc.toIndex].any { existingIdx ->
                    existingIdx !in dominated && dominancePolicy.dominates(allLabels[existingIdx], newLabel)
                }
                if (dominatedByExisting) continue

                // 移除被新标签支配的旧标签 / Remove labels dominated by the new one
                val toRemove = labelsAtNode[arc.toIndex].filter { existingIdx ->
                    existingIdx !in dominated && dominancePolicy.dominates(newLabel, allLabels[existingIdx])
                }
                for (removeIdx in toRemove) {
                    dominated.add(removeIdx)
                }
                labelsAtNode[arc.toIndex].removeAll(toRemove.toSet())

                // 添加新标签 / Add new label
                val newLabelIdx = labelIdCounter++
                allLabels.add(newLabel)
                labelsAtNode[arc.toIndex].add(newLabelIdx)
                queue.add(newLabelIdx)

                // 到达 end depot 时检查 reduced cost / Check reduced cost when reaching end depot
                if (arc.toIndex == endDepotIdx && newReducedCost ls -request.pricingTolerance) {
                    negativeLabels.add(newLabel)
                    if (minReducedCost gr newReducedCost) {
                        minReducedCost = newReducedCost
                    }
                }
            }
        }

        // 精确定价完成：没有任何标签的 reduced cost 小于 -pricingTolerance
        val exactPricingComplete = !interrupted && minReducedCost geq -request.pricingTolerance

        // 回溯生成路线 / Backtrack to generate routes
        val routes = mutableListOf<Route<V>>()
        val sortedNegativeLabels = negativeLabels.sortedBy { it.reducedCost }

        for (label in sortedNegativeLabels) {
            if (routes.size >= request.maxColumnsPerPricing) break
            val route = backtrackRoute(label, allLabels, graph, request)
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
     * Feillet 2004 不可达客户标记。 / Feillet 2004 unreachable customer marking.
     *
     * 在扩展到节点 j 后，检查哪些未访问客户从 j 出发不可达（时间窗或容量）。
     * 使用当前标签的 departure time 和 load 来判断。
     */
    private fun updateForbiddenCustomers(
        currentForbidden: ForbiddenCustomers,
        toNode: PricingNode,
        visited: VisitedCustomers,
        graph: PricingGraph,
        departureTime: Flt64,
        currentLoad: Flt64
    ): ForbiddenCustomers {
        var forbidden = currentForbidden
        if (toNode.isDepot) return forbidden

        // 标记当前访问的客户为 forbidden（不可再访问）
        forbidden = forbidden.add(toNode.customerIndex)

        // Feillet 标记：检查未访问客户是否从此节点出发不可达
        val toNodeIdx = graph.nodeIndexMap[toNode.nodeId] ?: return forbidden

        for (arc in graph.outgoingFrom(toNodeIdx)) {
            val candidateNode = graph.nodes[arc.toIndex]
            if (candidateNode.isDepot) continue
            if (visited.contains(candidateNode.customerIndex)) continue
            if (forbidden.contains(candidateNode.customerIndex)) continue

            // 时间窗不可达检查 / Time window unreachable check
            val arrivalAtCandidate = departureTime + arc.travelTime
            val serviceStartAtCandidate = maxOf(arrivalAtCandidate, candidateNode.readyTime)
            if (serviceStartAtCandidate gr candidateNode.dueTime) {
                forbidden = forbidden.add(candidateNode.customerIndex)
                continue
            }

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
        label: EspprcLabel,
        allLabels: List<EspprcLabel>,
        graph: PricingGraph,
        request: PricingRequest<V>
    ): Ret<Route<V>> {
        val flt64Window = instance.schedulingWindow.toFlt64Boundary()
        val vehicleType = instance.vehicleTypeById[request.vehicleTypeId]
            ?: return networkSchedulingFailure(
                "回溯路线失败：车辆类型不存在 / Failed to backtrack route: vehicle type does not exist"
            )

        // 收集路径节点 / Collect path nodes
        val path = mutableListOf<EspprcLabel>()
        var current: EspprcLabel? = label
        while (current != null) {
            path.add(0, current)
            current = if (current.predecessor >= 0) allLabels[current.predecessor] else null
        }

        // 收集路径弧 / Collect path arcs
        val pathArcs = mutableListOf<PricingArc>()
        for (i in 0 until path.lastIndex) {
            val fromNodeId = path[i].currentNode
            val toNodeId = path[i + 1].currentNode
            val fromIdx = graph.nodeIndexMap[fromNodeId] ?: continue
            val arc = graph.outgoingFrom(fromIdx).firstOrNull { it.toNodeId == toNodeId }
            if (arc != null) pathArcs.add(arc)
        }

        // 构建 RouteStop 列表 / Build RouteStop list
        val stops = mutableListOf<RouteStop<V>>()
        var accumulatedLoad = Quantity(valueAdapter.fromSolverValue(Flt64.zero).value
            ?: return networkSchedulingFailure(
                "回溯路线失败：负载转换失败 / Failed to backtrack route: load conversion failed"
            ), instance.units.loadUnit)
        var totalSolverDistance = Flt64.zero
        var totalSolverCost = Flt64.zero

        for (i in path.indices) {
            val nodeLabel = path[i]
            val nodeIdx = graph.nodeIndexMap[nodeLabel.currentNode] ?: continue
            val node = graph.nodes[nodeIdx]

            // nodeLabel.time = departure time = serviceStart + serviceTime
            // arrival = departure - serviceTime (如果无等待) 或 readyTime (如果有等待)
            // 更准确：从前驱弧推算 arrival
            val arrivalFlt64: Flt64 = if (i == 0) {
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
                    nodeLabel.time - node.serviceTime
                }
            }
            val serviceStartFlt64 = maxOf(arrivalFlt64, node.readyTime)
            val departureFlt64 = nodeLabel.time

            val arrivalInstant = flt64Window.instantOf(arrivalFlt64)
            val serviceStartInstant = flt64Window.instantOf(serviceStartFlt64)
            val departureInstant = flt64Window.instantOf(departureFlt64)

            val customerId = if (node.isDepot) null
                else instance.customers[node.customerIndex].id

            if (!node.isDepot) {
                val demand = instance.customers[node.customerIndex].demand
                val convertedDemand = demand.convertTo(instance.units.loadUnit)
                    ?: return networkSchedulingFailure(
                        "回溯路线失败：需求单位转换失败 / Failed to backtrack route: demand unit conversion failed"
                    )
                accumulatedLoad = Quantity(accumulatedLoad.value + convertedDemand.value, instance.units.loadUnit)
            }

            // 累加弧距离 / Accumulate arc distance
            if (i > 0) {
                val arc = pathArcs.getOrNull(i - 1)
                if (arc != null) {
                    totalSolverDistance = totalSolverDistance + arc.distance
                    totalSolverCost = totalSolverCost + arc.objectiveCost
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

        val costValue = valueAdapter.fromSolverValue(totalSolverCost)
            .value ?: return networkSchedulingFailure(
            "回溯路线失败：成本转换失败 / Failed to backtrack route: cost conversion failed"
        )

        val distanceValue = valueAdapter.fromSolverValue(totalSolverDistance)
            .value ?: return networkSchedulingFailure(
            "回溯路线失败：距离转换失败 / Failed to backtrack route: distance conversion failed"
        )

        return Route(
            vehicleTypeId = request.vehicleTypeId,
            stops = stops,
            distance = Quantity(distanceValue, instance.units.distanceUnit),
            cost = Quantity(costValue, instance.units.costUnit)
        )
    }
}
