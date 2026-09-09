@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service

import kotlin.time.Duration
import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.ServiceTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.*

/** 路线资源递推与成本独立校验器。 / Independent route resource-recursion and cost validator. */
object RouteValidator {
    /**
     * 重放并校验完整路线。 / Replay and validate a complete route.
     *
     * @param instance VRPTW 实例 / VRPTW instance
     * @param route 待校验路线 / Route to validate
     * @param distanceCalculator 距离策略 / Distance policy
     * @param travelTimeCalculator 时间策略 / Travel-time policy
     * @param arcCostCalculator 弧成本策略 / Arc-cost policy
     * @param routeCostPolicy 路线成本策略 / Route-cost policy
     * @param valueAdapter solver 数值适配器 / Solver value adapter
     * @return 成功或带上下文的失败 / Success or contextual failure
     */
    fun <V : RealNumber<V>> validate(
        instance: VrptwInstance<V>,
        route: Route<V>,
        distanceCalculator: DistanceCalculator<V>,
        travelTimeCalculator: TravelTimeCalculator<V>,
        arcCostCalculator: ArcCostCalculator<V>,
        routeCostPolicy: RouteCostPolicy<V>,
        valueAdapter: NetworkSchedulingSolverValueAdapter<V>
    ): Try {
        val vehicleType = instance.vehicleTypeById[route.vehicleTypeId]
            ?: return routeFailure(route, "车辆类型不存在", "vehicle type does not exist")
        val stops = route.stops
        if (stops.first().nodeId != instance.startDepot.node.id || stops.first().customerId != null) {
            return routeFailure(route, "首个停靠点不是起始仓库", "first stop is not the start depot")
        }
        if (stops.last().nodeId != instance.endDepot.node.id || stops.last().customerId != null) {
            return routeFailure(route, "最后停靠点不是结束仓库", "last stop is not the end depot")
        }
        val customerStops = stops.subList(1, stops.lastIndex)
        val customerIds = customerStops.mapNotNull { it.customerId }
        if (customerIds.size != customerStops.size || customerIds.toSet().size != customerIds.size) {
            return routeFailure(route, "客户缺失或重复访问", "customer is missing or visited repeatedly")
        }
        for (stop in customerStops) {
            val customer = stop.customerId?.let(instance.customerById::get)
                ?: return stopFailure(route, stop, "客户不存在", "customer does not exist")
            if (customer.node.id != stop.nodeId) {
                return stopFailure(route, stop, "客户与节点 ID 不匹配", "customer and node IDs do not match")
            }
        }

        val start = stops.first()
        if (!validLoadField(start, instance)) {
            return stopFailure(route, start, "负载单位不兼容", "load unit is incompatible")
        }
        if (!instance.startDepot.timeWindow.contains(start.serviceStart)) {
            return stopFailure(route, start, "出发时刻不在仓库时间窗内", "departure time is outside the depot time window")
        }
        if (start.serviceStart != start.departure || start.arrival > start.serviceStart) {
            return stopFailure(route, start, "起始仓库的到达/出发时刻非法", "start-depot arrival or departure time is invalid")
        }
        val zeroLoad = Quantity(start.accumulatedLoad.value.constants.zero, instance.units.loadUnit)
        if (!same(start.accumulatedLoad, zeroLoad)) {
            return stopFailure(route, start, "起始负载必须为 0", "initial load must be zero")
        }

        var totalDistance = Quantity(route.distance.value.constants.zero, instance.units.distanceUnit)
        val arcCosts = mutableListOf<Quantity<V>>()
        for (index in 1..stops.lastIndex) {
            val previous = stops[index - 1]
            val current = stops[index]
            if (!validLoadField(current, instance)) {
                return stopFailure(route, current, "负载单位不兼容", "load unit is incompatible")
            }
            val fromNode = node(instance, previous)
                ?: return stopFailure(route, previous, "节点不存在", "node does not exist")
            val toNode = node(instance, current)
                ?: return stopFailure(route, current, "节点不存在", "node does not exist")
            val distance = distanceCalculator.distance(fromNode, toNode).value
                ?: return arcFailure(route, fromNode, toNode, "距离计算失败", "distance calculation failed")
            val travelTime = travelTimeCalculator.travelTime(fromNode, toNode, vehicleType).value
                ?: return arcFailure(route, fromNode, toNode, "行驶时间计算失败", "travel-time calculation failed")
            val expectedArrival = previous.departure + travelTime
            if (current.arrival != expectedArrival) {
                return stopFailure(route, current, "到达时刻与递推结果不一致", "arrival time does not match the recurrence")
            }

            val window = timeWindow(instance, current)
                ?: return stopFailure(route, current, "时间窗不存在", "time window does not exist")
            val expectedServiceStart = maxOf(current.arrival, window.readyTime)
            if (current.serviceStart != expectedServiceStart || !window.contains(current.serviceStart)) {
                return stopFailure(route, current, "等待或服务开始时刻非法", "waiting or service-start time is invalid")
            }
            val serviceTime = serviceTime(instance, current)
            val expectedDeparture = current.serviceStart + serviceTime
            if (current.departure != expectedDeparture) {
                return stopFailure(route, current, "离开时刻与递推结果不一致", "departure time does not match the recurrence")
            }

            val demand = demand(instance, current)
            val expectedLoad = add(previous.accumulatedLoad, demand)
                ?: return stopFailure(route, current, "负载单位不兼容", "load unit is incompatible")
            if (!same(current.accumulatedLoad, expectedLoad)) {
                return stopFailure(route, current, "累计负载与递推结果不一致", "accumulated load does not match the recurrence")
            }
            val capacity = vehicleType.capacity.convertTo(instance.units.loadUnit)
                ?: return routeFailure(route, "车辆容量单位不兼容", "vehicle-capacity unit is incompatible")
            val normalizedLoad = current.accumulatedLoad.convertTo(instance.units.loadUnit)
                ?: return stopFailure(route, current, "负载单位不兼容", "load unit is incompatible")
            if (normalizedLoad.value gr capacity.value) {
                return stopFailure(route, current, "累计负载超过车辆容量", "accumulated load exceeds vehicle capacity")
            }

            totalDistance = add(totalDistance, distance)
                ?: return arcFailure(route, fromNode, toNode, "距离单位不兼容", "distance units are incompatible")
            val arcCost = arcCostCalculator.cost(
                from = fromNode,
                to = toNode,
                distance = distance,
                travelTime = travelTime,
                vehicleType = vehicleType
            ).value ?: return arcFailure(route, fromNode, toNode, "弧成本计算失败", "arc-cost calculation failed")
            arcCosts.add(arcCost)
        }

        if (!withinTolerance(route.distance, totalDistance, instance, valueAdapter, isCost = false)) {
            return routeFailure(route, "总距离与重算结果不一致", "total distance does not match the recomputed value")
        }
        val expectedCost = routeCostPolicy.cost(vehicleType, arcCosts).value
            ?: return routeFailure(route, "路线成本计算失败", "route-cost calculation failed")
        if (!withinTolerance(route.cost, expectedCost, instance, valueAdapter, isCost = true)) {
            return routeFailure(route, "总成本与重算结果不一致", "total cost does not match the recomputed value")
        }
        return ok
    }

    private fun <V : RealNumber<V>> node(instance: VrptwInstance<V>, stop: RouteStop<V>): NetworkNode<V>? {
        return when (stop.nodeId) {
            instance.startDepot.node.id -> instance.startDepot.node
            instance.endDepot.node.id -> instance.endDepot.node
            else -> instance.customerByNodeId[stop.nodeId]?.node
        }
    }

    private fun <V : RealNumber<V>> timeWindow(
        instance: VrptwInstance<V>,
        stop: RouteStop<V>
    ): ServiceTimeWindow? {
        return when (stop.nodeId) {
            instance.startDepot.node.id -> instance.startDepot.timeWindow
            instance.endDepot.node.id -> instance.endDepot.timeWindow
            else -> instance.customerByNodeId[stop.nodeId]?.timeWindow
        }
    }

    private fun <V : RealNumber<V>> serviceTime(instance: VrptwInstance<V>, stop: RouteStop<V>): Duration {
        val customer = instance.customerByNodeId[stop.nodeId]
        return customer?.serviceTime
            ?: Duration.ZERO
    }

    private fun <V : RealNumber<V>> demand(instance: VrptwInstance<V>, stop: RouteStop<V>): Quantity<V> {
        val customer = instance.customerByNodeId[stop.nodeId]
        return customer?.demand
            ?: Quantity(stop.accumulatedLoad.value.constants.zero, instance.units.loadUnit)
    }

    private fun <V : RealNumber<V>> validLoadField(stop: RouteStop<V>, instance: VrptwInstance<V>): Boolean {
        return stop.accumulatedLoad.convertTo(instance.units.loadUnit) != null
    }

    private fun <V : RealNumber<V>> add(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V>? {
        val normalized = rhs.convertTo(lhs.unit) ?: return null
        return Quantity(lhs.value + normalized.value, lhs.unit)
    }

    private fun <V : RealNumber<V>> same(lhs: Quantity<V>, rhs: Quantity<V>): Boolean {
        val normalized = rhs.convertTo(lhs.unit) ?: return false
        return lhs.value eq normalized.value
    }

    private fun <V : RealNumber<V>> withinTolerance(
        actual: Quantity<V>,
        expected: Quantity<V>,
        instance: VrptwInstance<V>,
        adapter: NetworkSchedulingSolverValueAdapter<V>,
        isCost: Boolean
    ): Boolean {
        val unit = if (isCost) instance.units.costUnit else instance.units.distanceUnit
        val actualValue = adapter.normalizeValue(actual, unit).value ?: return false
        val expectedValue = adapter.normalizeValue(expected, unit).value ?: return false
        val tolerance = adapter.fromSolverValue(instance.tolerances.costValidation).value ?: return false
        // RealNumber 保证排序与加法，但不保证减法（例如无符号值）。
        // RealNumber guarantees ordering and addition, but not subtraction (for example unsigned values).
        // 对称边界等价于绝对差检查，并保持领域数值类型 V。
        // Symmetric bounds are equivalent to an absolute-difference check and keep the domain type V.
        return actualValue leq (expectedValue + tolerance) &&
                expectedValue leq (actualValue + tolerance)
    }

    private fun <V : RealNumber<V>> routeFailure(
        route: Route<V>,
        chinese: String,
        english: String
    ): Try {
        return networkSchedulingFailure(
            "路线校验失败：路线 ${route.signature} $chinese / Route validation failed: route ${route.signature} $english"
        )
    }

    private fun <V : RealNumber<V>> stopFailure(
        route: Route<V>,
        stop: RouteStop<V>,
        chinese: String,
        english: String
    ): Try {
        return networkSchedulingFailure(
            "路线校验失败：路线 ${route.signature} 节点 ${stop.nodeId.value} $chinese / " +
                    "Route validation failed: route ${route.signature}, node ${stop.nodeId.value} $english"
        )
    }

    private fun <V : RealNumber<V>> arcFailure(
        route: Route<V>,
        from: NetworkNode<V>,
        to: NetworkNode<V>,
        chinese: String,
        english: String
    ): Try {
        return networkSchedulingFailure(
            "路线校验失败：路线 ${route.signature} 弧 ${from.id.value}->${to.id.value} $chinese / " +
                    "Route validation failed: route ${route.signature}, arc ${from.id.value}->${to.id.value} $english"
        )
    }
}
