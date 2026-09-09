@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.BranchMask
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service.RouteValidator
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/**
 * 初始路线生成器。 / Initial route generator.
 *
 * 为 RMP 生成初始可行列。启发式策略：
 * - 为每个客户生成单客户路线
 * - 通过 RouteValidator 复核可行性
 * - 过滤与 branch mask 不兼容的路线
 *
 * Generates initial feasible columns for the RMP. Heuristic strategies:
 * - Single-customer routes for each customer
 * - Validate feasibility through RouteValidator
 * - Filter routes incompatible with branch mask
 */
class InitialRouteGenerator<V : RealNumber<V>>(
    private val instance: VrptwInstance<V>,
    private val valueAdapter: NetworkSchedulingSolverValueAdapter<V>,
    private val distanceCalculator: DistanceCalculator<V>,
    private val travelTimeCalculator: TravelTimeCalculator<V>,
    private val arcCostCalculator: ArcCostCalculator<V>,
    private val routeCostPolicy: RouteCostPolicy<V>,
    private val arcFeasibilityPolicy: ArcFeasibilityPolicy<V> = DefaultArcFeasibilityPolicy(),
    private val routeValidator: RouteValidator = RouteValidator
) {
    /**
     * 生成初始路线。 / Generate initial routes.
     *
     * 启发式失败不是致命错误，返回空列表即可。 / Heuristic failure is not fatal; returning an empty list is acceptable.
     *
     * @param branchMask 分支掩码（可空） / Branch mask (nullable)
     * @return 初始可行路线列表 / List of initial feasible routes
     */
    fun generate(branchMask: BranchMask<VehicleTypeId>? = null): List<Route<V>> {
        val routes = mutableListOf<Route<V>>()

        for ((custIdx, customer) in instance.customers.withIndex()) {
            // 分支掩码检查
            val vehicleType = instance.vehicleTypes.firstOrNull { vt ->
                branchMask?.allowsNode(vt.id, customer.node.id) != false
            } ?: continue

            val route = buildSingleCustomerRoute(customer, vehicleType) ?: continue
            if (branchMask?.isRouteCompatible(
                    resourceKey = vehicleType.id,
                    path = route.stops.map { it.nodeId }
                ) == false
            ) {
                continue
            }

            // 复核可行性 / Validate feasibility
            val validation = routeValidator.validate(
                instance, route, distanceCalculator, travelTimeCalculator,
                arcCostCalculator, routeCostPolicy, valueAdapter
            )
            if (validation is Ok) {
                routes.add(route)
            }
        }

        return routes
    }

    /**
     * 构建单客户路线：start depot -> customer -> end depot。 / Build single-customer route.
     */
    private fun buildSingleCustomerRoute(customer: Customer<V>, vehicleType: VehicleType<V>): Route<V>? {
        val schedulingWindow = instance.schedulingWindow
        val zero = vehicleType.capacity.value.constants.zero

        val startDepot = instance.startDepot
        val endDepot = instance.endDepot
        val startArcFeasible = arcFeasibilityPolicy.isFeasible(
            from = startDepot.node,
            to = customer.node,
            vehicleType = vehicleType
        ).value ?: return null
        val endArcFeasible = arcFeasibilityPolicy.isFeasible(
            from = customer.node,
            to = endDepot.node,
            vehicleType = vehicleType
        ).value ?: return null
        if (!startArcFeasible || !endArcFeasible) return null

        // 时间递推 / Time recurrence
        var currentTime = schedulingWindow.valueOf(startDepot.timeWindow.readyTime)
        var currentLoad = zero
        var totalDistance = zero
        val arcCosts = mutableListOf<Quantity<V>>()

        val stops = mutableListOf<RouteStop<V>>()

        // 起始 depot / Start depot
        val startDeparture = currentTime
        stops.add(RouteStop(
            nodeId = startDepot.node.id,
            customerId = null,
            arrival = schedulingWindow.instantOf(startDeparture),
            serviceStart = schedulingWindow.instantOf(startDeparture),
            departure = schedulingWindow.instantOf(startDeparture),
            accumulatedLoad = Quantity(zero, instance.units.loadUnit)
        ))

        // start -> customer
        val travelTime1 = travelTimeCalculator.travelTime(startDepot.node, customer.node, vehicleType).value ?: return null
        val distance1 = distanceCalculator.distance(startDepot.node, customer.node).value ?: return null
        val arcCost1 = arcCostCalculator.cost(startDepot.node, customer.node, distance1, travelTime1, vehicleType).value ?: return null
        arcCosts.add(arcCost1)
        totalDistance = totalDistance + (valueAdapter.normalizeValue(distance1, instance.units.distanceUnit).value ?: return null)

        val arrival1 = startDeparture + schedulingWindow.valueOf(travelTime1)
        val serviceStart1 = maxOf(arrival1, schedulingWindow.valueOf(customer.timeWindow.readyTime))
        if (serviceStart1 gr schedulingWindow.valueOf(customer.timeWindow.dueTime)) return null

        currentLoad = currentLoad + (valueAdapter.normalizeValue(customer.demand, instance.units.loadUnit).value ?: return null)
        val vehicleCapacity = valueAdapter.normalizeValue(vehicleType.capacity, instance.units.loadUnit).value ?: return null
        if (currentLoad gr vehicleCapacity) return null

        val departure1 = serviceStart1 + schedulingWindow.valueOf(customer.serviceTime)
        currentTime = departure1

        stops.add(RouteStop(
            nodeId = customer.node.id,
            customerId = customer.id,
            arrival = schedulingWindow.instantOf(arrival1),
            serviceStart = schedulingWindow.instantOf(serviceStart1),
            departure = schedulingWindow.instantOf(departure1),
            accumulatedLoad = Quantity(currentLoad, instance.units.loadUnit)
        ))

        // customer -> end depot
        val travelTime2 = travelTimeCalculator.travelTime(customer.node, endDepot.node, vehicleType).value ?: return null
        val distance2 = distanceCalculator.distance(customer.node, endDepot.node).value ?: return null
        val arcCost2 = arcCostCalculator.cost(customer.node, endDepot.node, distance2, travelTime2, vehicleType).value ?: return null
        arcCosts.add(arcCost2)
        totalDistance = totalDistance + (valueAdapter.normalizeValue(distance2, instance.units.distanceUnit).value ?: return null)

        val arrival2 = currentTime + schedulingWindow.valueOf(travelTime2)
        val serviceStart2 = maxOf(arrival2, schedulingWindow.valueOf(endDepot.timeWindow.readyTime))
        if (serviceStart2 gr schedulingWindow.valueOf(endDepot.timeWindow.dueTime)) return null

        stops.add(RouteStop(
            nodeId = endDepot.node.id,
            customerId = null,
            arrival = schedulingWindow.instantOf(arrival2),
            serviceStart = schedulingWindow.instantOf(serviceStart2),
            departure = schedulingWindow.instantOf(serviceStart2),
            accumulatedLoad = Quantity(currentLoad, instance.units.loadUnit)
        ))

        val cost = routeCostPolicy.cost(vehicleType, arcCosts).value ?: return null

        return Route(
            vehicleTypeId = vehicleType.id,
            stops = stops,
            distance = Quantity(totalDistance, instance.units.distanceUnit),
            cost = cost
        ).value
    }
}
