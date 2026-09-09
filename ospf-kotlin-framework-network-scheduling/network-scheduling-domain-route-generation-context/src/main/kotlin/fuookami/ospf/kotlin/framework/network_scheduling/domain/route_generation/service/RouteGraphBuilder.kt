@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.service

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.BranchMask
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.pricing.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/**
 * Branch-aware 定价图构建器。 / Branch-aware pricing graph builder.
 *
 * 从 VrptwInstance、PricingDuals 和可选的 BranchMask 构建有向定价图。
 * 图中弧的 reduced cost 为 objective cost - dual values；距离、时间、负载和业务成本保留 V。
 * Arc reduced costs are objective cost - dual values; distance, time, load and business cost preserve V.
 */
class RouteGraphBuilder<V : FloatingNumber<V>>(
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
    ): Ret<PricingGraph<V>> {
        val vehicleType = instance.vehicleTypeById[vehicleTypeId]
            ?: return networkSchedulingFailure(
                "构建定价图失败：车辆类型 ${vehicleTypeId.value} 不存在 / " +
                        "Failed to build pricing graph: vehicle type ${vehicleTypeId.value} does not exist"
            )

        val schedulingWindow: SchedulingTimeWindow<V> = instance.schedulingWindow
        val zero = vehicleType.capacity.value.constants.zero

        // 构建节点列表
        // 索引 0 = start depot, 1..n = customers, n+1 = end depot
        val nodes = mutableListOf<PricingNode<V>>()
        val customerIndexMap = mutableMapOf<NetworkNodeId, Int>() // nodeId -> index in nodes

        nodes.add(PricingNode(
            nodeId = instance.startDepot.node.id,
            isDepot = true,
            customerIndex = -1,
            readyTime = schedulingWindow.valueOf(instance.startDepot.timeWindow.readyTime),
            dueTime = schedulingWindow.valueOf(instance.startDepot.timeWindow.dueTime),
            serviceTime = schedulingWindow.valueOf(Duration.ZERO),
            demand = zero
        ))
        customerIndexMap[instance.startDepot.node.id] = 0

        for ((custIdx, customer) in instance.customers.withIndex()) {
            val forbiddenByMask = branchMask?.allowsNode(vehicleTypeId, customer.node.id) == false
            if (forbiddenByMask) continue

            val nodeIndex = nodes.size
            val demand = when (val r = valueAdapter.normalizeValue(customer.demand, instance.units.loadUnit)) {
                is Ok -> r.value; is Failed -> return Failed(r.error); is Fatal -> return Fatal(r.errors)
            }
            nodes.add(PricingNode(
                nodeId = customer.node.id,
                isDepot = false,
                customerIndex = custIdx,
                readyTime = schedulingWindow.valueOf(customer.timeWindow.readyTime),
                dueTime = schedulingWindow.valueOf(customer.timeWindow.dueTime),
                serviceTime = schedulingWindow.valueOf(customer.serviceTime),
                demand = demand
            ))
            customerIndexMap[customer.node.id] = nodeIndex
        }

        nodes.add(PricingNode(
            nodeId = instance.endDepot.node.id,
            isDepot = true,
            customerIndex = -1,
            readyTime = schedulingWindow.valueOf(instance.endDepot.timeWindow.readyTime),
            dueTime = schedulingWindow.valueOf(instance.endDepot.timeWindow.dueTime),
            serviceTime = schedulingWindow.valueOf(Duration.ZERO),
            demand = zero
        ))
        customerIndexMap[instance.endDepot.node.id] = nodes.lastIndex

        // 构建弧
        val arcs = mutableListOf<PricingArc<V>>()
        val vehicleCapacity = when (val r = valueAdapter.normalizeValue(vehicleType.capacity, instance.units.loadUnit)) {
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
                val normalizedArcCost = when (val r = valueAdapter.normalizeValue(arcCost, instance.units.costUnit)) {
                    is Ok -> r.value; is Failed -> return Failed(r.error); is Fatal -> return Fatal(r.errors)
                }

                val normalizedDistance = when (val r = valueAdapter.normalizeValue(distance, instance.units.distanceUnit)) {
                    is Ok -> r.value; is Failed -> return Failed(r.error); is Fatal -> return Fatal(r.errors)
                }

                val routeObjectiveCost = if (fromNode.isDepot && fromNode.nodeId == instance.startDepot.node.id) {
                    val fixedCost = when (val r = valueAdapter.normalizeValue(vehicleType.fixedCost, instance.units.costUnit)) {
                        is Ok -> r.value; is Failed -> return Failed(r.error); is Fatal -> return Fatal(r.errors)
                    }
                    fixedCost + normalizedArcCost
                } else {
                    normalizedArcCost
                }

                // Reduced cost = objective cost - customer dual - fleet dual
                val customerDual = if (toNode.isDepot) {
                    zero
                } else {
                    val solverDual = duals.customer[instance.customers[toNode.customerIndex].id] ?: Flt64.zero
                    valueAdapter.fromSolverValue(solverDual).value
                        ?: return networkSchedulingFailure(
                            "构建定价图失败：客户对偶转换失败 / Failed to build pricing graph: customer dual conversion failed"
                        )
                }
                val fleetDual = if (fromNode.isDepot && fromNode.nodeId == instance.startDepot.node.id) {
                    val solverDual = duals.fleet[vehicleTypeId] ?: Flt64.zero
                    valueAdapter.fromSolverValue(solverDual).value
                        ?: return networkSchedulingFailure(
                            "构建定价图失败：车队对偶转换失败 / Failed to build pricing graph: fleet dual conversion failed"
                        )
                } else {
                    zero
                }

                val phaseObjectiveCost = when (duals.phase) {
                    PricingPhase.PhaseOne -> zero
                    PricingPhase.PhaseTwo -> routeObjectiveCost
                }
                val reducedCost = phaseObjectiveCost - customerDual - fleetDual

                // 时间、距离和业务成本保留 V；仅 reduced cost 参与 solver 适配时才转换。
                // Preserve time, distance and business cost as V; convert only at the solver boundary.
                val travelTimeValue = schedulingWindow.valueOf(travelTime)

                arcs.add(PricingArc(
                    fromIndex = fromIdx,
                    toIndex = toIdx,
                    objectiveCost = routeObjectiveCost,
                    reducedCost = reducedCost,
                    travelTime = travelTimeValue,
                    distance = normalizedDistance,
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
data class PricingNode<V : FloatingNumber<V>>(
    val nodeId: NetworkNodeId,
    val isDepot: Boolean,
    val customerIndex: Int,
    val readyTime: V,
    val dueTime: V,
    val serviceTime: V,
    val demand: V
)

/** 定价图弧 / Pricing graph arc */
data class PricingArc<V : FloatingNumber<V>>(
    val fromIndex: Int,
    val toIndex: Int,
    val objectiveCost: V,
    val reducedCost: V,
    val travelTime: V,
    val distance: V,
    val fromNodeId: NetworkNodeId,
    val toNodeId: NetworkNodeId
)

/** 定价图 / Pricing graph */
data class PricingGraph<V : FloatingNumber<V>>(
    val nodes: List<PricingNode<V>>,
    val arcs: List<PricingArc<V>>,
    val nodeIndexMap: Map<NetworkNodeId, Int>,
    val customerCount: Int,
    val startDepotIndex: Int,
    val endDepotIndex: Int,
    val vehicleCapacity: V
) {
    /** 从指定节点出发的弧 / Arcs outgoing from the specified node */
    private val outgoingCache = mutableMapOf<Int, List<PricingArc<V>>>()

    fun outgoingFrom(nodeIndex: Int): List<PricingArc<V>> {
        return outgoingCache.getOrPut(nodeIndex) { arcs.filter { it.fromIndex == nodeIndex } }
    }
}
