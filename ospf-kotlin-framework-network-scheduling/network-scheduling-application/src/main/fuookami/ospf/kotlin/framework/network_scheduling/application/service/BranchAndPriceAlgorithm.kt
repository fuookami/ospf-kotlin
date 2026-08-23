@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.network_scheduling.application.service

import kotlin.time.Duration
import kotlin.time.TimeSource
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.progress.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.pricing.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service.RouteValidator
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.model.PricingInterruptionChecker
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.policy.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.model.*

/**
 * VRPTW Branch-and-Price 算法。 / VRPTW Branch-and-Price algorithm.
 *
 * 使用 best-bound 队列迭代处理节点，不使用递归。
 * 尚未求解的子节点继承父节点 LP 下界作为有效下界，求解后再替换为自身下界。
 * 全局下界是所有活动节点当前有效下界的最小值。 / Uses a best-bound queue to iteratively process nodes, without recursion.
 * Unsolved child nodes inherit the parent LP lower bound as effective lower bound;
 * after solving, it is replaced by the own lower bound.
 * The global lower bound is the minimum of all active nodes' effective lower bounds.
 */
class BranchAndPriceAlgorithm<V : RealNumber<V>>(
    private val instance: VrptwInstance<V>,
    private val solver: ColumnGenerationSolver,
    private val configuration: Configuration,
    private val policy: Policy<V>
) {
    /**
     * B&P 配置。 / B&P configuration.
     */
    data class Configuration(
        val timeLimit: Duration = Duration.INFINITE,
        val nodeLimit: Int = Int.MAX_VALUE,
        val relativeGapTolerance: Flt64 = Flt64(1e-4),
        val maxColumnsPerPricing: Int = Int.MAX_VALUE,
        val maxCGIterationsPerNode: Int = 1000
    )

    /**
     * B&P 策略注入。 / B&P policy injection.
     */
    data class Policy<V : RealNumber<V>>(
        val valueAdapter: NetworkSchedulingSolverValueAdapter<V>,
        val distanceCalculator: DistanceCalculator<V>,
        val travelTimeCalculator: TravelTimeCalculator<V>,
        val arcCostCalculator: ArcCostCalculator<V>,
        val routeCostPolicy: RouteCostPolicy<V>,
        val arcFeasibilityPolicy: ArcFeasibilityPolicy<V> = DefaultArcFeasibilityPolicy(),
        val labelDominancePolicy: LabelDominancePolicy = LabelDominancePolicy.Default,
        val pricingColumnSelector: PricingColumnSelector<V> = PricingColumnSelector.default(),
        val traceListener: BranchAndPriceTraceListener = BranchAndPriceTraceListener.None,
        val extraPipelines: List<CGPipeline<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap>> = emptyList()
    )

    /**
     * B&P 求解结果。 / B&P solve result.
     */
    data class BranchAndPriceResult<V : RealNumber<V>>(
        val status: BranchAndPriceStatus,
        val solution: VrptwSolution<V>?,
        val lowerBound: Flt64,
        val upperBound: Flt64,
        val trace: BranchAndPriceTrace
    )

    /**
     * 求解 VRPTW。 / Solve VRPTW.
     */
    suspend fun solve(progressContext: SolverProgressContext? = null): Ret<BranchAndPriceResult<V>> {
        val beginTime = TimeSource.Monotonic.markNow()
        progressContext?.report(
            SolverProgressSnapshot(
                stage = SolverStages.BranchAndPrice,
                subStage = SolverSubStage(
                    key = "branch_and_price_start",
                    defaultTemplate = "初始化分支定价",
                    messageKey = "i18n.ospf.substage.branch_and_price_start"
                ),
                progressInStage = 0,
                overallProgress = 0
            )
        )
        val startDepot = instance.startDepot.node.id
        val endDepot = instance.endDepot.node.id

        // 创建根节点 / Create root node
        val rootNode = when (val result = BranchNode.root(startDepot, endDepot)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // Best-bound 优先队列 / Best-bound priority queue
        val queue = java.util.PriorityQueue<BranchNode>(
            compareBy { it.effectiveLowerBound }
        )
        queue.add(rootNode)

        var incumbent: List<Route<V>>? = null
        var incumbentCost: Flt64? = null
        var globalLowerBound = Flt64.negativeInfinity
        var nodesExplored = 0
        var nodesPruned = 0
        var totalIterations = 0
        var nodeIdCounter = 1
        var columnPool = emptyList<Route<V>>()

        while (queue.isNotEmpty()) {
            queue.minOfOrNull { it.effectiveLowerBound }?.let { globalLowerBound = it }
            val elapsed = beginTime.elapsedNow()

            // 检查时间上限 / Check time limit
            // TimeLimit 是可解释的正常终态：即使尚无 incumbent，全局下界（可能为 -inf）
            // 仍是诚实的下界，不伪装为 SolverStopped（后者保留给携带有效 bound/incumbent 的外部 solver 异常停止）。
            // TimeLimit is an explainable normal terminal state: even without an incumbent, the global
            // lower bound (possibly -inf) is an honest bound and is not disguised as SolverStopped
            // (which is reserved for external solver abnormal stops carrying a valid bound/incumbent).
            if (elapsed >= configuration.timeLimit) {
                return assembleResult(
                    status = BranchAndPriceStatus.TimeLimit,
                    incumbent = incumbent, incumbentCost = incumbentCost,
                    globalLowerBound = globalLowerBound, nodesExplored = nodesExplored,
                    nodesPruned = nodesPruned, totalIterations = totalIterations,
                    elapsed = elapsed
                )
            }

            // 检查节点上限 / Check node limit
            // NodeLimit 同样是可解释的正常终态，无 incumbent 时仍如实返回下界与空解。
            // NodeLimit is likewise an explainable normal terminal state; without an incumbent it still
            // faithfully reports the lower bound and an empty solution.
            if (nodesExplored >= configuration.nodeLimit) {
                return assembleResult(
                    status = BranchAndPriceStatus.NodeLimit,
                    incumbent = incumbent, incumbentCost = incumbentCost,
                    globalLowerBound = globalLowerBound, nodesExplored = nodesExplored,
                    nodesPruned = nodesPruned, totalIterations = totalIterations,
                    elapsed = elapsed
                )
            }

            // 检查 gap / Check gap
            if (incumbentCost != null && globalLowerBound.isFinite() && !globalLowerBound.isNegativeInfinity()) {
                val gap = (incumbentCost - globalLowerBound) / maxOf(Flt64.one, incumbentCost.abs())
                if (gap.leq(configuration.relativeGapTolerance)) {
                    return assembleResult(
                        status = BranchAndPriceStatus.Optimal,
                        incumbent = incumbent, incumbentCost = incumbentCost,
                        globalLowerBound = globalLowerBound, nodesExplored = nodesExplored,
                        nodesPruned = nodesPruned, totalIterations = totalIterations,
                        elapsed = elapsed
                    )
                }
            }

            // 取 best-bound 节点 / Poll best-bound node
            val node = queue.poll()!!

            // 剪枝 / Pruning
            if (!node.canImprove(incumbentCost, instance.tolerances.pricing)) {
                node.prune()
                nodesPruned++
                globalLowerBound = queue.minOfOrNull { it.effectiveLowerBound }
                    ?: incumbentCost
                    ?: globalLowerBound
                continue
            }

            // 求解节点 / Solve node
            val nodeSolver = BranchNodeSolver(
                instance = instance,
                solver = solver,
                valueAdapter = policy.valueAdapter,
                distanceCalculator = policy.distanceCalculator,
                travelTimeCalculator = policy.travelTimeCalculator,
                arcCostCalculator = policy.arcCostCalculator,
                routeCostPolicy = policy.routeCostPolicy,
                arcFeasibilityPolicy = policy.arcFeasibilityPolicy,
                labelDominancePolicy = policy.labelDominancePolicy,
                pricingColumnSelector = policy.pricingColumnSelector,
                extraPipelines = policy.extraPipelines,
                maxColumnsPerPricing = configuration.maxColumnsPerPricing,
                maxCGIterations = configuration.maxCGIterationsPerNode,
                interruptionChecker = PricingInterruptionChecker {
                    beginTime.elapsedNow() >= configuration.timeLimit
                },
                inheritedColumns = columnPool
            )

            val result = when (val r = nodeSolver.solve(node)) {
                is Ok -> r.value
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }

            nodesExplored++
            totalIterations += result.iterations
            columnPool = (columnPool + result.columns).distinctBy { it.signature }

            val nodeProgress = if (configuration.nodeLimit == Int.MAX_VALUE) {
                (nodesExplored.coerceAtMost(100) * 99 / 100)
            } else if (configuration.nodeLimit == 0) {
                100
            } else {
                (nodesExplored * 100 / configuration.nodeLimit).coerceIn(0, 99)
            }
            when (val progressResult = progressContext?.report(
                SolverProgressSnapshot(
                    stage = SolverStages.BranchAndPrice,
                    subStage = SolverSubStage(
                        key = "branch_node",
                        defaultTemplate = "处理分支节点 {node}（迭代 {iter}）",
                        messageKey = "i18n.ospf.substage.branch_node",
                        args = mapOf(
                            "node" to node.id.toString(),
                            "iter" to totalIterations.toString()
                        )
                    ),
                    progressInStage = nodeProgress,
                    overallProgress = nodeProgress,
                    diagnostics = mapOf(
                        "nodesExplored" to nodesExplored.toString(),
                        "nodesPruned" to nodesPruned.toString(),
                        "iterations" to totalIterations.toString(),
                        "lowerBound" to globalLowerBound.toString(),
                        "upperBound" to (incumbentCost?.toString() ?: "")
                    )
                )
            )) {
                null, is Ok -> {}
                is Failed -> return Failed(progressResult.error)
                is Fatal -> return Fatal(progressResult.errors)
            }

            if (result.timeLimitReached) {
                val queueBound = queue.minOfOrNull { it.effectiveLowerBound }
                return assembleResult(
                    status = BranchAndPriceStatus.TimeLimit,
                    incumbent = incumbent,
                    incumbentCost = incumbentCost,
                    globalLowerBound = listOfNotNull(globalLowerBound, node.inheritedLowerBound, queueBound).minOrNull()
                        ?: globalLowerBound,
                    nodesExplored = nodesExplored,
                    nodesPruned = nodesPruned,
                    totalIterations = totalIterations,
                    elapsed = beginTime.elapsedNow()
                )
            }
            if (result.solverStatus == SolverStatus.Infeasible) {
                // 外部 LP 只证明当前节点不可行；其它活动节点仍需继续处理。
                // An external LP proves only this node infeasible; other active nodes remain viable.
                node.markInfeasible()
                globalLowerBound = queue.minOfOrNull { it.effectiveLowerBound }
                    ?: incumbentCost
                    ?: Flt64.infinity
                continue
            }
            if (result.solverStatus != SolverStatus.Optimal) {
                // 非最优 LP 没有精确定价证书，不能用于更新下界、定价或剪枝；按冻结合同返回失败。
                // A non-optimal LP has no exact-pricing certificate and cannot update bounds,
                // pricing, or pruning; return failure as required by the frozen contract.
                return networkSchedulingFailure(
                    "节点 ${node.id} 的 LP 状态为 ${result.solverStatus}，未提供精确最优性证书 / " +
                        "LP at node ${node.id} ended with ${result.solverStatus} without an exact optimality certificate"
                )
            }
            if (!result.pricingComplete) {
                return networkSchedulingFailure(
                    "节点 ${node.id} 未完成精确定价，不能更新全局下界 / " +
                        "Exact pricing at node ${node.id} is incomplete and cannot update the global lower bound"
                )
            }

            // 不可行 → 跳过 / Infeasible → skip
            if (!result.isFeasible) {
                globalLowerBound = queue.minOfOrNull { it.effectiveLowerBound }
                    ?: incumbentCost
                    ?: Flt64.infinity
                continue
            }

            // 更新全局下界 / Update global lower bound
            val queueMin = if (queue.isNotEmpty()) queue.peek()?.effectiveLowerBound else null
            globalLowerBound = if (queueMin != null) minOf(result.lowerBound, queueMin) else result.lowerBound

            when (val traceResult = policy.traceListener.onTrace(BranchAndPriceTrace(
                nodesExplored = nodesExplored,
                nodesPruned = nodesPruned,
                globalLowerBound = globalLowerBound,
                globalUpperBound = incumbentCost ?: Flt64.infinity,
                relativeGap = if (incumbentCost != null && globalLowerBound.isFinite()) {
                    (incumbentCost - globalLowerBound) / maxOf(Flt64.one, incumbentCost.abs())
                } else {
                    Flt64.infinity
                },
                totalTime = beginTime.elapsedNow(),
                totalIterations = totalIterations
            ))) {
                is Ok -> {}
                is Failed -> return Failed(traceResult.error)
                is Fatal -> return Fatal(traceResult.errors)
            }

            if (result.isInteger) {
                // 整数解 → 更新 incumbent / Integer solution → update incumbent
                val selectedRoutes = result.routeValues
                    .filter { (_, v) -> v.geq(Flt64.one - instance.tolerances.integrality) }
                    .map { (r, _) -> r }

                if (selectedRoutes.isNotEmpty()) {
                    val cost = computeSolutionCost(selectedRoutes)
                    if (incumbentCost == null || cost.ls(incumbentCost)) {
                        // RouteValidator 复核 / RouteValidator verification
                        val validated = selectedRoutes.all { route ->
                            RouteValidator.validate(
                                instance, route, policy.distanceCalculator,
                                policy.travelTimeCalculator, policy.arcCostCalculator,
                                policy.routeCostPolicy, policy.valueAdapter
                            ) is Ok
                        }
                        if (validated) {
                            incumbent = selectedRoutes
                            incumbentCost = cost
                            // 剪枝：移除不能改进 incumbent 的节点 / Prune nodes that cannot improve incumbent
                            val toRemove = queue.filter { !it.canImprove(incumbentCost, instance.tolerances.pricing) }
                            queue.removeAll(toRemove)
                            nodesPruned += toRemove.size
                        }
                    }
                }
            } else {
                // 非整数 → 分支 / Fractional → branch
                if (incumbentCost == null || result.lowerBound.ls(incumbentCost)) {
                    val branchDecision = selectBranchDecision(
                        result.routeValues, instance, instance.tolerances.integrality
                    )
                    if (branchDecision != null) {
                        // 创建左右子节点 / Create left and right child nodes
                        val leftDecision = branchDecision
                        val rightDecision = complementaryDecision(branchDecision)

                        val leftChild = BranchNode.child(nodeIdCounter++, node, leftDecision, startDepot, endDepot)
                        val rightChild = BranchNode.child(nodeIdCounter++, node, rightDecision, startDepot, endDepot)

                        when {
                            leftChild is Ok && rightChild is Ok -> {
                                queue.add(leftChild.value)
                                queue.add(rightChild.value)
                            }
                            leftChild is Failed -> return Failed(leftChild.error)
                            leftChild is Fatal -> return Fatal(leftChild.errors)
                            rightChild is Failed -> return Failed(rightChild.error)
                            rightChild is Fatal -> return Fatal(rightChild.errors)
                        }
                    } else {
                        // 合同违例: assignment/edge 均为整数但路线变量仍为真分数
                        // Contract violation: assignment/edge both integer but route variables still fractional
                        return networkSchedulingFailure(
                            "Contract violation at node ${node.id}: assignment and edge values are integer " +
                                "but route variables remain fractional. This indicates a dedup invariant failure " +
                                "or parallel arcs causing ambiguous edge aggregation. " +
                                "Node lower bound: ${result.lowerBound}, route values: ${result.routeValues.map { it.second }}"
                        )
                    }
                }
            }

            // 当前节点已经被其子节点或 incumbent 替换；全局下界只保留活动队列和已关闭可行节点的证明值。
            // The current node is replaced by its children or the incumbent; keep the global bound
            // from active queue nodes and the closed feasible node proof value.
            val queueBound = queue.minOfOrNull { it.effectiveLowerBound }
            val closedBound = incumbentCost ?: result.lowerBound
            globalLowerBound = if (queueBound != null) {
                minOf(queueBound, closedBound)
            } else {
                closedBound
            }
        }

        // 队列为空 / Queue empty
        val elapsed = beginTime.elapsedNow()
        val finalStatus = when {
            incumbent == null -> BranchAndPriceStatus.Infeasible
            globalLowerBound.isFinite() && !globalLowerBound.isNegativeInfinity() -> {
                val gap = (incumbentCost!! - globalLowerBound) /
                    maxOf(Flt64.one, incumbentCost.abs())
                if (gap.leq(configuration.relativeGapTolerance)) {
                    BranchAndPriceStatus.Optimal
                } else {
                    BranchAndPriceStatus.Feasible
                }
            }
            else -> BranchAndPriceStatus.Feasible
        }
        return assembleResult(
            status = finalStatus,
            incumbent = incumbent, incumbentCost = incumbentCost,
            globalLowerBound = globalLowerBound, nodesExplored = nodesExplored,
            nodesPruned = nodesPruned, totalIterations = totalIterations,
            elapsed = elapsed
        )
    }

    /**
     * 选择分支决策。 / Select branch decision.
     *
     * 按合同 3.5 节：先 assignmentValue，后 edgeValue。
     * Per contract section 3.5: assignmentValue first, then edgeValue.
     */
    fun selectBranchDecision(
        routeValues: List<Pair<Route<V>, Flt64>>,
        instance: VrptwInstance<V>,
        integralityTolerance: Flt64
    ): BranchDecision? {
        // 1. 计算 assignmentValue[k, i] / Compute assignmentValue[k, i]
        val assignmentValue = mutableMapOf<Pair<VehicleTypeId, CustomerId>, Flt64>()
        for ((route, value) in routeValues) {
            for (stop in route.stops) {
                val cid = stop.customerId ?: continue
                val key = route.vehicleTypeId to cid
                assignmentValue[key] = (assignmentValue[key] ?: Flt64.zero) + value
            }
        }

        // 2. 找最接近 0.5 的非整数 assignmentValue / Find assignmentValue closest to 0.5
        var bestAssignment: Triple<VehicleTypeId, CustomerId, NetworkNodeId>? = null
        var bestAssignmentFractionality = Flt64.one

        for ((key, value) in assignmentValue) {
            // 跳过整数值：value 接近 0 或 1 时不作为分支候选 / Skip integer values
            if (value.leq(integralityTolerance) || value.geq(Flt64.one - integralityTolerance)) continue
            // fractionality = |value - 0.5|，越小越接近 0.5 / fractionality = |value - 0.5|
            val fractionality = (value - Flt64(0.5)).abs()
            if (fractionality.ls(bestAssignmentFractionality)) {
                bestAssignmentFractionality = fractionality
                val (vehicleTypeId, customerId) = key
                val customerNodeId = instance.customerById[customerId]?.node?.id ?: continue
                bestAssignment = Triple(vehicleTypeId, customerId, customerNodeId)
            }
        }

        if (bestAssignment != null) {
            val (vehicleTypeId, customerId, customerNodeId) = bestAssignment
            return BranchDecision.ForbidVehicleType(vehicleTypeId, customerId, customerNodeId)
        }

        // 3. 计算 edgeValue[k, (i, j)] / Compute edgeValue[k, (i, j)]
        val edgeValue = mutableMapOf<Triple<VehicleTypeId, NetworkNodeId, NetworkNodeId>, Flt64>()
        for ((route, value) in routeValues) {
            val path = route.stops.map { it.nodeId }
            for (i in 0 until path.lastIndex) {
                val key = Triple(route.vehicleTypeId, path[i], path[i + 1])
                edgeValue[key] = (edgeValue[key] ?: Flt64.zero) + value
            }
        }

        // 4. 找最接近 0.5 的非整数 edgeValue / Find edgeValue closest to 0.5
        var bestArc: Triple<VehicleTypeId, NetworkNodeId, NetworkNodeId>? = null
        var bestArcFractionality = Flt64.one

        for ((key, value) in edgeValue) {
            // 跳过整数值 / Skip integer values
            if (value.leq(integralityTolerance) || value.geq(Flt64.one - integralityTolerance)) continue
            val fractionality = (value - Flt64(0.5)).abs()
            if (fractionality.ls(bestArcFractionality)) {
                bestArcFractionality = fractionality
                bestArc = key
            }
        }

        if (bestArc != null) {
            val (vehicleTypeId, from, to) = bestArc
            return BranchDecision.ForbidArc(vehicleTypeId, from, to)
        }

        // 5. assignment/edge 均为整数，检查路线变量 / Both integer, check route variables
        for ((_, value) in routeValues) {
            val fractionality = minOf(value, Flt64.one - value)
            if (fractionality.gr(integralityTolerance)) {
                // 合同违例: assignment/edge 整数但路线变量真分数
                // Contract violation: assignment/edge integer but route variable fractional
                return null
            }
        }

        // 完全整数 / Fully integer
        return null
    }

    /**
     * 获取互补分支决策。 / Get complementary branch decision.
     */
    fun complementaryDecision(decision: BranchDecision): BranchDecision = when (decision) {
        is BranchDecision.ForbidVehicleType -> BranchDecision.RequireVehicleType(
            decision.vehicleTypeId, decision.customerId, decision.customerNodeId
        )
        is BranchDecision.RequireVehicleType -> BranchDecision.ForbidVehicleType(
            decision.vehicleTypeId, decision.customerId, decision.customerNodeId
        )
        is BranchDecision.ForbidArc -> BranchDecision.RequireArc(
            decision.vehicleTypeId, decision.from, decision.to
        )
        is BranchDecision.RequireArc -> BranchDecision.ForbidArc(
            decision.vehicleTypeId, decision.from, decision.to
        )
    }

    /**
     * 计算解的总成本。 / Compute total solution cost.
     */
    private fun computeSolutionCost(routes: List<Route<V>>): Flt64 {
        var total = Flt64.zero
        for (route in routes) {
            val cost = policy.valueAdapter.normalize(route.cost, instance.units.costUnit).value ?: Flt64.zero
            total = total + cost
        }
        return total
    }

    /**
     * 组装最终结果。 / Assemble final result.
     */
    private fun assembleResult(
        status: BranchAndPriceStatus,
        incumbent: List<Route<V>>?,
        incumbentCost: Flt64?,
        globalLowerBound: Flt64,
        nodesExplored: Int,
        nodesPruned: Int,
        totalIterations: Int,
        elapsed: Duration
    ): Ret<BranchAndPriceResult<V>> {
        val upperBound = incumbentCost ?: Flt64.infinity
        val relativeGap = if (incumbentCost != null && globalLowerBound.isFinite() && !globalLowerBound.isNegativeInfinity()) {
            (incumbentCost - globalLowerBound) / maxOf(Flt64.one, incumbentCost.abs())
        } else {
            Flt64.infinity
        }

        val solution = if (incumbent != null && incumbent.isNotEmpty()) {
            // 直接累加路线的 Quantity<V> 值 / Sum route Quantity<V> values directly
            val distanceUnit = instance.units.distanceUnit
            val costUnit = instance.units.costUnit
            val totalDistance = incumbent.map { route ->
                route.distance.convertTo(distanceUnit) ?: Quantity(route.distance.value.constants.zero, distanceUnit)
            }.reduce { acc, q -> Quantity(acc.value + q.value, distanceUnit) }
            val totalCost = incumbent.map { route ->
                route.cost.convertTo(costUnit) ?: Quantity(route.cost.value.constants.zero, costUnit)
            }.reduce { acc, q -> Quantity(acc.value + q.value, costUnit) }

            VrptwSolution(
                routes = incumbent,
                totalDistance = totalDistance,
                totalCost = totalCost
            )
        } else null

        return Ok(BranchAndPriceResult(
            status = status,
            solution = solution,
            lowerBound = globalLowerBound,
            upperBound = upperBound,
            trace = BranchAndPriceTrace(
                nodesExplored = nodesExplored,
                nodesPruned = nodesPruned,
                globalLowerBound = globalLowerBound,
                globalUpperBound = upperBound,
                relativeGap = relativeGap,
                totalTime = elapsed,
                totalIterations = totalIterations
            )
        ))
    }
}
