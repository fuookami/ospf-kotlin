@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.service

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.BranchMask
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.pricing.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/**
 * Branch-aware 定价图构建器。 / Branch-aware pricing graph builder.
 *
 * 从 VrptwInstance、PricingDuals 和可选的 BranchMask 构建有向定价图。
 * 图中弧的 cost 是 reduced cost：objective cost - dual values。
 * 时间和负载使用 Flt64 数值，便于 ESPPRC 标签扩展。 / Constructs a directed pricing graph from a VrptwInstance, PricingDuals,
 * and optional BranchMask. Arc costs are reduced costs:
 * objective cost - dual values. Time and load use Flt64 numeric values
 * for ESPPRC label extension.
 */
class RouteGraphBuilder<V : RealNumber<V>>(
    private val instance: VrptwInstance<V>,
    private val valueAdapter: NetworkSchedulingSolverValueAdapter<V>,
    private val distanceCalculator: DistanceCalculator<V>,
    private val travelTimeCalculator: TravelTimeCalculator<V>,
    private val arcCostCalculator: ArcCostCalculator<V>,
    private val arcFeasibilityPolicy: ArcFeasibilityPolicy<V> = DefaultArcFeasibilityPolicy()
) {
    /**
     * 构建定价图。 / Build the pricing graph.
     */
    fun build(
        vehicleTypeId: VehicleTypeId,
        duals: PricingDuals,
        branchMask: BranchMask<VehicleTypeId>? = null
    ): Ret<PricingGraph> {
        val vehicleType = instance.vehicleTypeById[vehicleTypeId]
            ?: return networkSchedulingFailure(
                "构建定价图失败：车辆类型 ${vehicleTypeId.value} 不存在 / " +
                        "Failed to build pricing graph: vehicle type ${vehicleTypeId.value} does not exist"
            )

        val flt64Window = instance.schedulingWindow.toFlt64Boundary()

        // 构建节点列表
        // 索引 0 = start depot, 1..n = customers, n+1 = end depot
        val nodes = mutableListOf<PricingNode>()
        val customerIndexMap = mutableMapOf<NetworkNodeId, Int>() // nodeId -> index in nodes

        nodes.add(PricingNode(
            nodeId = instance.startDepot.node.id,
            isDepot = true,
            customerIndex = -1,
            readyTime = flt64Window.valueOf(instance.startDepot.timeWindow.readyTime),
            dueTime = flt64Window.valueOf(instance.startDepot.timeWindow.dueTime),
            serviceTime = Flt64.zero,
            demand = Flt64.zero
        ))
        customerIndexMap[instance.startDepot.node.id] = 0

        for ((custIdx, customer) in instance.customers.withIndex()) {
            val forbiddenByMask = branchMask?.allowsNode(vehicleTypeId, customer.node.id) == false
            if (forbiddenByMask) continue

            val nodeIndex = nodes.size
            val solverDemand = when (val r = valueAdapter.normalize(customer.demand, instance.units.loadUnit)) {
                is Ok -> r.value; is Failed -> return Failed(r.error); is Fatal -> return Fatal(r.errors)
            }
            nodes.add(PricingNode(
                nodeId = customer.node.id,
                isDepot = false,
                customerIndex = custIdx,
                readyTime = flt64Window.valueOf(customer.timeWindow.readyTime),
                dueTime = flt64Window.valueOf(customer.timeWindow.dueTime),
                serviceTime = flt64Window.valueOf(customer.serviceTime),
                demand = solverDemand
            ))
            customerIndexMap[customer.node.id] = nodeIndex
        }

        nodes.add(PricingNode(
            nodeId = instance.endDepot.node.id,
            isDepot = true,
            customerIndex = -1,
            readyTime = flt64Window.valueOf(instance.endDepot.timeWindow.readyTime),
            dueTime = flt64Window.valueOf(instance.endDepot.timeWindow.dueTime),
            serviceTime = Flt64.zero,
            demand = Flt64.zero
        ))
        customerIndexMap[instance.endDepot.node.id] = nodes.lastIndex

        // 构建弧
        val arcs = mutableListOf<PricingArc>()
        val vehicleCapacity = when (val r = valueAdapter.normalize(vehicleType.capacity, instance.units.loadUnit)) {
            is Ok -> r.value; is Failed -> return Failed(r.error); is Fatal -> return Fatal(r.errors)
        }

        for ((fromIdx, fromNode) in nodes.withIndex()) {
            for ((toIdx, toNode) in nodes.withIndex()) {
                if (fromIdx == toIdx) continue
                if (toNode.nodeId == instance.startDepot.node.id) continue // 不能回到起始 depot
                if (fromNode.nodeId == instance.endDepot.node.id) continue // 不能从结束 depot 出发

                // 分支掩码检查
                if (branchMask?.allowsArc(vehicleTypeId, fromNode.nodeId, toNode.nodeId) == false) continue

                // 解析领域节点对象 / Resolve domain node objects
                val fromNodeObj = if (fromNode.isDepot && fromNode.nodeId == instance.startDepot.node.id)
                    instance.startDepot.node else instance.customerByNodeId[fromNode.nodeId]?.node ?: continue
                val toNodeObj = if (toNode.isDepot && toNode.nodeId == instance.endDepot.node.id)
                    instance.endDepot.node else instance.customerByNodeId[toNode.nodeId]?.node ?: continue

                // 弧可行性
                val feasible = when (val r = arcFeasibilityPolicy.isFeasible(fromNodeObj, toNodeObj, vehicleType)) {
                    is Ok -> r.value; is Failed -> return Failed(r.error); is Fatal -> return Fatal(r.errors)
                }
                if (!feasible) continue

                // 计算 objective cost

                val distance = when (val r = distanceCalculator.distance(fromNodeObj, toNodeObj)) {
                    is Ok -> r.value; is Failed -> return Failed(r.error); is Fatal -> return Fatal(r.errors)
                }
                val travelTime = when (val r = travelTimeCalculator.travelTime(fromNodeObj, toNodeObj, vehicleType)) {
                    is Ok -> r.value; is Failed -> return Failed(r.error); is Fatal -> return Fatal(r.errors)
                }
                val arcCost = when (val r = arcCostCalculator.cost(fromNodeObj, toNodeObj, distance, travelTime, vehicleType)) {
                    is Ok -> r.value; is Failed -> return Failed(r.error); is Fatal -> return Fatal(r.errors)
                }
                val solverArcCost = when (val r = valueAdapter.normalize(arcCost, instance.units.costUnit)) {
                    is Ok -> r.value; is Failed -> return Failed(r.error); is Fatal -> return Fatal(r.errors)
                }

                val solverDistance = when (val r = valueAdapter.normalize(distance, instance.units.distanceUnit)) {
                    is Ok -> r.value; is Failed -> return Failed(r.error); is Fatal -> return Fatal(r.errors)
                }

                val routeObjectiveCost = if (fromNode.isDepot && fromNode.nodeId == instance.startDepot.node.id) {
                    val fixedCost = when (val r = valueAdapter.normalize(vehicleType.fixedCost, instance.units.costUnit)) {
                        is Ok -> r.value; is Failed -> return Failed(r.error); is Fatal -> return Fatal(r.errors)
                    }
                    fixedCost + solverArcCost
                } else {
                    solverArcCost
                }

                // Reduced cost = objective cost - customer dual - fleet dual
                val customerDual = if (toNode.isDepot) Flt64.zero
                    else duals.customer[instance.customers[toNode.customerIndex].id] ?: Flt64.zero
                val fleetDual = if (fromNode.isDepot && fromNode.nodeId == instance.startDepot.node.id)
                    duals.fleet[vehicleTypeId] ?: Flt64.zero
                else Flt64.zero

                val phaseObjectiveCost = when (duals.phase) {
                    PricingPhase.PhaseOne -> Flt64.zero
                    PricingPhase.PhaseTwo -> routeObjectiveCost
                }
                val reducedCost = phaseObjectiveCost - customerDual - fleetDual

                // 时间（Flt64 数值）
                val solverTravelTime = flt64Window.valueOf(travelTime)

                arcs.add(PricingArc(
                    fromIndex = fromIdx,
                    toIndex = toIdx,
                    objectiveCost = routeObjectiveCost,
                    reducedCost = reducedCost,
                    travelTime = solverTravelTime,
                    distance = solverDistance,
                    fromNodeId = fromNode.nodeId,
                    toNodeId = toNode.nodeId
                ))
            }
        }

        return Ok(PricingGraph(
            nodes = nodes.toList(),
            arcs = arcs.toList(),
            nodeIndexMap = customerIndexMap,
            customerCount = instance.customers.size,
            startDepotIndex = 0,
            endDepotIndex = nodes.lastIndex,
            vehicleCapacity = vehicleCapacity
        ))
    }
}

/** 定价图节点 / Pricing graph node */
data class PricingNode(
    val nodeId: NetworkNodeId,
    val isDepot: Boolean,
    val customerIndex: Int,
    val readyTime: Flt64,
    val dueTime: Flt64,
    val serviceTime: Flt64,
    val demand: Flt64
)

/** 定价图弧 / Pricing graph arc */
data class PricingArc(
    val fromIndex: Int,
    val toIndex: Int,
    val objectiveCost: Flt64,
    val reducedCost: Flt64,
    val travelTime: Flt64,
    val distance: Flt64,
    val fromNodeId: NetworkNodeId,
    val toNodeId: NetworkNodeId
)

/** 定价图 / Pricing graph */
data class PricingGraph(
    val nodes: List<PricingNode>,
    val arcs: List<PricingArc>,
    val nodeIndexMap: Map<NetworkNodeId, Int>,
    val customerCount: Int,
    val startDepotIndex: Int,
    val endDepotIndex: Int,
    val vehicleCapacity: Flt64
) {
    /** 从指定节点出发的弧 / Arcs outgoing from the specified node */
    private val outgoingCache = mutableMapOf<Int, List<PricingArc>>()

    fun outgoingFrom(nodeIndex: Int): List<PricingArc> {
        return outgoingCache.getOrPut(nodeIndex) { arcs.filter { it.fromIndex == nodeIndex } }
    }
}
