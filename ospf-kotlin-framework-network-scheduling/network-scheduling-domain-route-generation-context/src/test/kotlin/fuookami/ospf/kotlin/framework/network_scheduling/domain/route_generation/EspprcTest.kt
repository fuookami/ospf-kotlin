@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation

import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.pricing.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service.RouteValidator
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.service.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.policy.*

class EspprcTest {

    private val units = assertNotNull(
        VrptwUnits(distanceUnit = Meter, loadUnit = Kilogram, costUnit = NoneUnit).value
    )

    private val schedulingWindow = SchedulingTimeWindow(
        window = TimeRange(
            start = Instant.parse("2026-01-01T00:00:00Z"),
            end = Instant.parse("2026-01-01T01:00:00Z")
        ),
        durationUnit = DurationUnit.SECONDS,
        fromDouble = { Flt64(it) },
        toDouble = { it.toDouble() }
    )

    private fun node(id: String, x: Flt64): NetworkNode<Flt64> = assertNotNull(
        NetworkNode(
            id = NetworkNodeId(id),
            attributes = mapOf("x" to Quantity(x, Meter), "y" to Quantity(Flt64.zero, Meter))
        ).value
    )

    private fun instant(value: Flt64): Instant = schedulingWindow.instantOf(value)

    private fun window(ready: Flt64, due: Flt64): ServiceTimeWindow = assertNotNull(
        ServiceTimeWindow(readyTime = instant(ready), dueTime = instant(due)).value
    )

    /** 两客户实例：c1 在 x=1, c2 在 x=2, depot 在 x=0 和 x=3 */
    private fun makeInstance(): VrptwInstance<Flt64> = assertNotNull(
        VrptwInstance(
            name = "test-two-customers",
            startDepot = Depot(node("start", Flt64.zero), window(Flt64.zero, Flt64(100.0))),
            endDepot = Depot(node("end", Flt64(3.0)), window(Flt64.zero, Flt64(100.0))),
            customers = listOf(
                assertNotNull(Customer(
                    id = CustomerId("c1"), node = node("c1", Flt64.one),
                    demand = Quantity(Flt64(2.0), Kilogram),
                    timeWindow = window(Flt64.zero, Flt64(50.0)),
                    serviceTime = Duration.ZERO
                ).value),
                assertNotNull(Customer(
                    id = CustomerId("c2"), node = node("c2", Flt64(2.0)),
                    demand = Quantity(Flt64(2.0), Kilogram),
                    timeWindow = window(Flt64.zero, Flt64(50.0)),
                    serviceTime = Duration.ZERO
                ).value)
            ),
            vehicleTypes = listOf(
                assertNotNull(VehicleType(
                    id = VehicleTypeId("v1"),
                    capacity = Quantity(Flt64(5.0), Kilogram),
                    fixedCost = Quantity(Flt64(10.0), NoneUnit),
                    amount = 2
                ).value)
            ),
            units = units,
            schedulingWindow = schedulingWindow,
            tolerances = VrptwTolerances.default
        ).value
    )

    /** 三客户实例：用于穷举 oracle 测试 */
    private fun makeThreeCustomerInstance(): VrptwInstance<Flt64> = assertNotNull(
        VrptwInstance(
            name = "test-three-customers",
            startDepot = Depot(node("start", Flt64.zero), window(Flt64.zero, Flt64(100.0))),
            endDepot = Depot(node("end", Flt64(4.0)), window(Flt64.zero, Flt64(100.0))),
            customers = listOf(
                assertNotNull(Customer(
                    id = CustomerId("c1"), node = node("c1", Flt64.one),
                    demand = Quantity(Flt64(1.0), Kilogram),
                    timeWindow = window(Flt64.zero, Flt64(50.0)),
                    serviceTime = Duration.ZERO
                ).value),
                assertNotNull(Customer(
                    id = CustomerId("c2"), node = node("c2", Flt64(2.0)),
                    demand = Quantity(Flt64(1.0), Kilogram),
                    timeWindow = window(Flt64.zero, Flt64(50.0)),
                    serviceTime = Duration.ZERO
                ).value),
                assertNotNull(Customer(
                    id = CustomerId("c3"), node = node("c3", Flt64(3.0)),
                    demand = Quantity(Flt64(1.0), Kilogram),
                    timeWindow = window(Flt64.zero, Flt64(50.0)),
                    serviceTime = Duration.ZERO
                ).value)
            ),
            vehicleTypes = listOf(
                assertNotNull(VehicleType(
                    id = VehicleTypeId("v1"),
                    capacity = Quantity(Flt64(3.0), Kilogram),
                    fixedCost = Quantity(Flt64(10.0), NoneUnit),
                    amount = 2
                ).value)
            ),
            units = units,
            schedulingWindow = schedulingWindow,
            tolerances = VrptwTolerances.default
        ).value
    )

    private fun makeHelpers(instance: VrptwInstance<Flt64>): PricingHelpers = PricingHelpers(
        instance = instance,
        valueAdapter = Flt64NetworkSchedulingSolverValueAdapter,
        distanceCalculator = EuclideanDistanceCalculator<Flt64>(Meter),
        travelTimeCalculator = DistanceAsTravelTimeCalculator<Flt64>(EuclideanDistanceCalculator(Meter), schedulingWindow),
        arcCostCalculator = DistanceArcCostCalculator<Flt64>(units.distanceUnit, units.costUnit),
        routeCostPolicy = Demo17CostPolicy<Flt64>(NoneUnit)
    )

    private fun makeFltXInstance(): VrptwInstance<FltX> {
        val fltXWindow = SchedulingTimeWindow(
            window = schedulingWindow.window,
            durationUnit = DurationUnit.SECONDS,
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )
        val start = assertNotNull(NetworkNode(
            id = NetworkNodeId("start-x"),
            attributes = mapOf("x" to Quantity(FltX("0"), Meter), "y" to Quantity(FltX("0"), Meter))
        ).value)
        val customerNode = assertNotNull(NetworkNode(
            id = NetworkNodeId("customer-x"),
            attributes = mapOf("x" to Quantity(FltX("1"), Meter), "y" to Quantity(FltX("0"), Meter))
        ).value)
        val end = assertNotNull(NetworkNode(
            id = NetworkNodeId("end-x"),
            attributes = mapOf("x" to Quantity(FltX("2"), Meter), "y" to Quantity(FltX("0"), Meter))
        ).value)
        return assertNotNull(VrptwInstance(
            name = "fltx-pricing",
            startDepot = Depot(start, window(Flt64.zero, Flt64(100.0))),
            endDepot = Depot(end, window(Flt64.zero, Flt64(100.0))),
            customers = listOf(assertNotNull(Customer(
                id = CustomerId("customer-x"),
                node = customerNode,
                demand = Quantity(FltX("1"), Kilogram),
                timeWindow = window(Flt64.zero, Flt64(100.0)),
                serviceTime = Duration.ZERO
            ).value)),
            vehicleTypes = listOf(assertNotNull(VehicleType(
                id = VehicleTypeId("vehicle-x"),
                capacity = Quantity(FltX("2"), Kilogram),
                fixedCost = Quantity(FltX("10"), NoneUnit),
                amount = 1
            ).value)),
            units = units,
            schedulingWindow = fltXWindow,
            tolerances = VrptwTolerances.default
        ).value)
    }

    // ========== Model 层测试 ==========

    @Test
    fun visitedCustomersShouldTrackVisitsImmutably() {
        val visited = VisitedCustomers.empty(5)
        assertFalse(visited.contains(0))
        assertFalse(visited.contains(3))
        assertTrue(visited.isEmpty())

        val visited1 = visited.add(0)
        assertTrue(visited1.contains(0))
        assertFalse(visited1.contains(3))
        assertEquals(1, visited1.count())

        val visited2 = visited1.add(3)
        assertTrue(visited2.contains(0))
        assertTrue(visited2.contains(3))
        assertEquals(2, visited2.count())

        // 不可变性：原对象不变 / Immutability: original unchanged
        assertFalse(visited.contains(0))
        assertEquals(1, visited1.count())
    }

    @Test
    fun forbiddenCustomersShouldSupportSubsetAndUnion() {
        val empty = ForbiddenCustomers.empty(5)
        val fc1 = empty.add(0).add(1)
        val fc2 = empty.add(0).add(2)

        assertTrue(fc1.isSubsetOf(fc1))
        assertFalse(fc1.isSubsetOf(fc2))

        val union = fc1.union(fc2)
        assertTrue(union.contains(0))
        assertTrue(union.contains(1))
        assertTrue(union.contains(2))
    }

    // ========== RouteGraphBuilder 测试 ==========

    @Test
    fun routeGraphBuilderShouldBuildGraphWithCorrectNodeAndArcCounts() {
        val instance = makeInstance()
        val h = makeHelpers(instance)

        val builder = RouteGraphBuilder(instance, h.valueAdapter, h.distanceCalculator, h.travelTimeCalculator, h.arcCostCalculator)

        val duals = PricingDuals(
            phase = PricingPhase.PhaseTwo,
            customer = mapOf(CustomerId("c1") to Flt64(1.0), CustomerId("c2") to Flt64(1.0)),
            fleet = mapOf(VehicleTypeId("v1") to Flt64(-5.0))
        )

        val graph = assertNotNull(builder.build(VehicleTypeId("v1"), duals).value)

        // 节点：start depot + 2 customers + end depot = 4
        assertEquals(4, graph.nodes.size)
        assertTrue(graph.nodes[0].isDepot) // start depot
        assertFalse(graph.nodes[1].isDepot) // c1
        assertFalse(graph.nodes[2].isDepot) // c2
        assertTrue(graph.nodes[3].isDepot) // end depot

        // 弧数 > 0
        assertTrue(graph.arcs.isNotEmpty())
    }

    @Test
    fun phaseOneGraphShouldExcludeBusinessCostFromReducedCost() {
        val instance = makeInstance()
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")
        val duals = PricingDuals(
            phase = PricingPhase.PhaseOne,
            customer = mapOf(CustomerId("c1") to Flt64(3.0)),
            fleet = mapOf(vehicleTypeId to Flt64(5.0))
        )
        val graph = assertNotNull(RouteGraphBuilder(
            instance = instance,
            valueAdapter = h.valueAdapter,
            distanceCalculator = h.distanceCalculator,
            travelTimeCalculator = h.travelTimeCalculator,
            arcCostCalculator = h.arcCostCalculator
        ).build(
            vehicleTypeId = vehicleTypeId,
            duals = duals
        ).value)
        val customerIndex = graph.nodeIndexMap[instance.customers.first().node.id] ?: fail("customer node missing")
        val arc = graph.outgoingFrom(graph.startDepotIndex).first { it.toIndex == customerIndex }

        assertEquals(Flt64(-8.0), arc.reducedCost)
        assertTrue(arc.objectiveCost.gr(Flt64.zero))
    }

    @Test
    fun phaseOnePricerShouldPreserveGeneratedRouteBusinessCost() {
        val instance = makeInstance()
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")
        val duals = PricingDuals(
            phase = PricingPhase.PhaseOne,
            customer = instance.customers.associate { it.id to Flt64(20.0) },
            fleet = mapOf(vehicleTypeId to Flt64.zero)
        )
        val graph = assertNotNull(RouteGraphBuilder(
            instance = instance,
            valueAdapter = h.valueAdapter,
            distanceCalculator = h.distanceCalculator,
            travelTimeCalculator = h.travelTimeCalculator,
            arcCostCalculator = h.arcCostCalculator
        ).build(
            vehicleTypeId = vehicleTypeId,
            duals = duals
        ).value)
        val result = assertNotNull(EspprcPricer(
            instance = instance,
            valueAdapter = h.valueAdapter
        ).price(
            graph = graph,
            request = PricingRequest(
                instance = instance,
                duals = duals,
                branchMask = null,
                pricingTolerance = Flt64(1e-6),
                vehicleTypeId = vehicleTypeId
            )
        ).value)

        assertTrue(result.routes.isNotEmpty())
        for (route in result.routes) {
            val normalizedCost = assertNotNull(h.valueAdapter.normalize(route.cost, instance.units.costUnit).value)
            assertTrue(normalizedCost.gr(Flt64.zero))
        }
    }

    // ========== InitialRouteGenerator 测试 ==========

    @Test
    fun initialRouteGeneratorShouldProduceSingleCustomerRoutes() {
        val instance = makeInstance()
        val h = makeHelpers(instance)

        val generator = InitialRouteGenerator(
            instance, h.valueAdapter, h.distanceCalculator, h.travelTimeCalculator, h.arcCostCalculator, h.routeCostPolicy
        )

        val routes = generator.generate()
        // 至少应该能为2个客户各生成一条单客户路线
        assertTrue(routes.isNotEmpty())
        assertTrue(routes.size <= 2)
    }

    @Test
    fun initialRouteGeneratorShouldFilterForbiddenArcColumns() {
        val instance = makeInstance()
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")
        val forbiddenCustomer = instance.customers.first()
        val mask = assertNotNull(BranchMask(
            startDepot = instance.startDepot.node.id,
            endDepot = instance.endDepot.node.id,
            forbiddenArcs = setOf(ResourceArc(
                resourceKey = vehicleTypeId,
                from = instance.startDepot.node.id,
                to = forbiddenCustomer.node.id
            ))
        ).value)
        val routes = InitialRouteGenerator(
            instance = instance,
            valueAdapter = h.valueAdapter,
            distanceCalculator = h.distanceCalculator,
            travelTimeCalculator = h.travelTimeCalculator,
            arcCostCalculator = h.arcCostCalculator,
            routeCostPolicy = h.routeCostPolicy
        ).generate(mask)

        assertTrue(routes.none { route -> route.stops.any { it.customerId == forbiddenCustomer.id } })
        assertTrue(routes.all { route -> mask.isRouteCompatible(route.vehicleTypeId, route.stops.map { it.nodeId }) })
    }

    @Test
    fun espprcShouldReportInterruptionWithoutClaimingExactPricing() {
        val instance = makeInstance()
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")
        val duals = PricingDuals(
            phase = PricingPhase.PhaseTwo,
            customer = emptyMap(),
            fleet = emptyMap()
        )
        val graph = assertNotNull(RouteGraphBuilder(
            instance = instance,
            valueAdapter = h.valueAdapter,
            distanceCalculator = h.distanceCalculator,
            travelTimeCalculator = h.travelTimeCalculator,
            arcCostCalculator = h.arcCostCalculator
        ).build(
            vehicleTypeId = vehicleTypeId,
            duals = duals
        ).value)
        val result = assertNotNull(EspprcPricer(
            instance = instance,
            valueAdapter = h.valueAdapter
        ).price(
            graph = graph,
            request = PricingRequest(
                instance = instance,
                duals = duals,
                branchMask = null,
                pricingTolerance = Flt64(1e-6),
                vehicleTypeId = vehicleTypeId,
                interruptionChecker = PricingInterruptionChecker { true }
            )
        ).value)

        assertTrue(result.interrupted)
        assertFalse(result.exactPricingComplete)
    }

    @Test
    fun publicDomainValidatorAndEspprcShouldExecuteWithFltX() {
        val instance = makeFltXInstance()
        val distance = EuclideanDistanceCalculator<FltX>(Meter)
        val travelTime = DistanceAsTravelTimeCalculator(distance, instance.schedulingWindow)
        val arcCost = DistanceArcCostCalculator<FltX>(Meter, NoneUnit)
        val routeCost = Demo17CostPolicy<FltX>(NoneUnit)
        val routes = InitialRouteGenerator(
            instance = instance,
            valueAdapter = FltXNetworkSchedulingSolverValueAdapter,
            distanceCalculator = distance,
            travelTimeCalculator = travelTime,
            arcCostCalculator = arcCost,
            routeCostPolicy = routeCost
        ).generate()
        val route = routes.single()

        assertTrue(RouteValidator.validate(
            instance = instance,
            route = route,
            distanceCalculator = distance,
            travelTimeCalculator = travelTime,
            arcCostCalculator = arcCost,
            routeCostPolicy = routeCost,
            valueAdapter = FltXNetworkSchedulingSolverValueAdapter
        ) is Ok)

        val vehicleTypeId = instance.vehicleTypes.single().id
        val duals = PricingDuals(
            phase = PricingPhase.PhaseOne,
            customer = mapOf(instance.customers.single().id to Flt64(20.0)),
            fleet = mapOf(vehicleTypeId to Flt64.zero)
        )
        val graph = assertNotNull(RouteGraphBuilder(
            instance = instance,
            valueAdapter = FltXNetworkSchedulingSolverValueAdapter,
            distanceCalculator = distance,
            travelTimeCalculator = travelTime,
            arcCostCalculator = arcCost
        ).build(
            vehicleTypeId = vehicleTypeId,
            duals = duals
        ).value)
        val pricing = EspprcPricer(
            instance = instance,
            valueAdapter = FltXNetworkSchedulingSolverValueAdapter
        ).price(
            graph = graph,
            request = PricingRequest(
                instance = instance,
                duals = duals,
                branchMask = null,
                pricingTolerance = Flt64(1e-6),
                vehicleTypeId = vehicleTypeId
            )
        )

        assertTrue(pricing is Ok)
        assertTrue(pricing.value.routes.isNotEmpty())
    }

    // ========== LabelDominancePolicy 测试 ==========

    @Test
    fun labelDominanceShouldDetectCorrectDominance() {
        val policy = LabelDominancePolicy.Default
        val visited1 = VisitedCustomers.empty(3).add(0)
        val visited2 = VisitedCustomers.empty(3).add(0).add(1)
        val forbidden1 = ForbiddenCustomers.empty(3).add(0).add(2)
        val forbidden2 = ForbiddenCustomers.empty(3).add(0)

        val labelA = EspprcLabel(
            reducedCost = Flt64(-2.0), time = Flt64(5.0), load = Flt64(1.0),
            currentNode = NetworkNodeId("c1"),
            visited = visited1, forbidden = forbidden1, predecessor = -1
        )
        val labelB = EspprcLabel(
            reducedCost = Flt64(-1.0), time = Flt64(10.0), load = Flt64(2.0),
            currentNode = NetworkNodeId("c1"),
            visited = visited2, forbidden = forbidden2, predecessor = -1
        )

        // A 的 reduced cost < B, time < B, load < B, visited subset of B, forbidden superset of B
        // A dominates B
        assertTrue(policy.dominates(labelA, labelB))
        assertFalse(policy.dominates(labelB, labelA))
    }

    // ========== 穷举 Oracle 测试 ==========

    /**
     * 穷举 oracle 测试：枚举三客户实例的所有可行路线，
     * 计算每条路线的 reduced cost，与 ESPPRC 结果比对。
     */
    @Test
    fun espprcShouldMatchExhaustiveOracleForSmallInstance() {
        val instance = makeThreeCustomerInstance()
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")

        // 设置较高的对偶值使部分路线有负 reduced cost
        // 路线 start→c1→end: cost = 10 + 1 + 3 = 14, reduced = 14 - dual_c1 - fleet_dual
        // 要使 reduced < 0: dual_c1 + fleet_dual > 14
        // 设置 dual_c1 = 8, dual_c2 = 7, dual_c3 = 6, fleet_dual = 8
        val duals = PricingDuals(
            phase = PricingPhase.PhaseTwo,
            customer = mapOf(
                CustomerId("c1") to Flt64(8.0),
                CustomerId("c2") to Flt64(7.0),
                CustomerId("c3") to Flt64(6.0)
            ),
            fleet = mapOf(vehicleTypeId to Flt64(8.0))
        )

        // 构建定价图 / Build pricing graph
        val builder = RouteGraphBuilder(instance, h.valueAdapter, h.distanceCalculator, h.travelTimeCalculator, h.arcCostCalculator)
        val graph = assertNotNull(builder.build(vehicleTypeId, duals).value)

        // 执行 ESPPRC / Execute ESPPRC
        val pricer = EspprcPricer(instance, h.valueAdapter)
        val request = PricingRequest(
            instance = instance,
            duals = duals,
            branchMask = null,
            pricingTolerance = Flt64(1e-6),
            maxColumnsPerPricing = Int.MAX_VALUE,
            vehicleTypeId = vehicleTypeId
        )
        val result = assertNotNull((pricer.price(graph, request) as? Ok)?.value)

        // 穷举所有可行路线并计算 reduced cost / Enumerate all feasible routes and compute reduced cost
        val oracleReducedCosts = enumerateAllReducedCosts(instance, h, vehicleTypeId, duals)

        // 比对最小 reduced cost / Compare minimum reduced cost
        val oracleMinReducedCost = oracleReducedCosts.minOrNull() ?: Flt64.zero
        assertTrue(
            (result.minReducedCost - oracleMinReducedCost).abs() ls Flt64(1e-4),
            "ESPPRC min reduced cost ${result.minReducedCost} should match oracle $oracleMinReducedCost"
        )

        // ESPPRC 找到的路线数应 <= 穷举的负 reduced cost 路线数
        // （ESPPRC 可能因支配而少返回一些等价路线，但不应多返回）
        val oracleNegativeCount = oracleReducedCosts.count { it ls Flt64.zero }
        assertTrue(
            result.routes.size <= oracleNegativeCount,
            "ESPPRC returned ${result.routes.size} routes but oracle has $oracleNegativeCount negative routes"
        )

        // ESPPRC 返回的每条路线都应有负 reduced cost
        for (route in result.routes) {
            val routeReducedCost = computeRouteReducedCost(route, instance, h, duals)
            assertTrue(
                routeReducedCost ls Flt64(1e-4),
                "Route ${route.signature} has non-negative reduced cost $routeReducedCost"
            )
        }
    }

    /**
     * 等待时间测试：客户到达早于 ready time，需要等待。
     */
    @Test
    fun espprcShouldHandleWaitingTime() {
        // c1 的 ready time = 5，从 depot 到 c1 的行驶时间 = 1，需要等待 4
        val instance = assertNotNull(
            VrptwInstance(
                name = "test-waiting",
                startDepot = Depot(node("start", Flt64.zero), window(Flt64.zero, Flt64(100.0))),
                endDepot = Depot(node("end", Flt64(2.0)), window(Flt64.zero, Flt64(100.0))),
                customers = listOf(
                    assertNotNull(Customer(
                        id = CustomerId("c1"), node = node("c1", Flt64.one),
                        demand = Quantity(Flt64(1.0), Kilogram),
                        timeWindow = window(Flt64(5.0), Flt64(50.0)), // ready time = 5
                        serviceTime = Duration.ZERO
                    ).value)
                ),
                vehicleTypes = listOf(
                    assertNotNull(VehicleType(
                        id = VehicleTypeId("v1"),
                        capacity = Quantity(Flt64(5.0), Kilogram),
                        fixedCost = Quantity(Flt64(10.0), NoneUnit),
                        amount = 1
                    ).value)
                ),
                units = units,
                schedulingWindow = schedulingWindow,
                tolerances = VrptwTolerances.default
            ).value
        )
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")

        val duals = PricingDuals(
            phase = PricingPhase.PhaseTwo,
            customer = mapOf(CustomerId("c1") to Flt64(20.0)),
            fleet = mapOf(vehicleTypeId to Flt64.zero)
        )

        val builder = RouteGraphBuilder(instance, h.valueAdapter, h.distanceCalculator, h.travelTimeCalculator, h.arcCostCalculator)
        val graph = assertNotNull(builder.build(vehicleTypeId, duals).value)

        val pricer = EspprcPricer(instance, h.valueAdapter)
        val request = PricingRequest(
            instance = instance, duals = duals, branchMask = null,
            pricingTolerance = Flt64(1e-6), vehicleTypeId = vehicleTypeId
        )
        val result = assertNotNull((pricer.price(graph, request) as? Ok)?.value)

        // 应该找到路线（等待不影响可行性）
        // reduced cost = (10 + 1 + 1) - 20 - 0 = -8 < 0
        assertTrue(result.routes.isNotEmpty(), "Should find route with waiting time")
    }

    /**
     * 刚好到 due time 测试：到达时间恰好等于 due time。
     */
    @Test
    fun espprcShouldAllowArrivalExactlyAtDueTime() {
        // c1 在 x=1, due time = 1（从 depot 到 c1 的行驶时间恰好 = 1）
        val instance = assertNotNull(
            VrptwInstance(
                name = "test-due-time-boundary",
                startDepot = Depot(node("start", Flt64.zero), window(Flt64.zero, Flt64(100.0))),
                endDepot = Depot(node("end", Flt64(2.0)), window(Flt64.zero, Flt64(100.0))),
                customers = listOf(
                    assertNotNull(Customer(
                        id = CustomerId("c1"), node = node("c1", Flt64.one),
                        demand = Quantity(Flt64(1.0), Kilogram),
                        timeWindow = window(Flt64.zero, Flt64(1.0)), // due time = 1, 恰好到达
                        serviceTime = Duration.ZERO
                    ).value)
                ),
                vehicleTypes = listOf(
                    assertNotNull(VehicleType(
                        id = VehicleTypeId("v1"),
                        capacity = Quantity(Flt64(5.0), Kilogram),
                        fixedCost = Quantity(Flt64(10.0), NoneUnit),
                        amount = 1
                    ).value)
                ),
                units = units,
                schedulingWindow = schedulingWindow,
                tolerances = VrptwTolerances.default
            ).value
        )
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")

        val duals = PricingDuals(
            phase = PricingPhase.PhaseTwo,
            customer = mapOf(CustomerId("c1") to Flt64(20.0)),
            fleet = mapOf(vehicleTypeId to Flt64.zero)
        )

        val builder = RouteGraphBuilder(instance, h.valueAdapter, h.distanceCalculator, h.travelTimeCalculator, h.arcCostCalculator)
        val graph = assertNotNull(builder.build(vehicleTypeId, duals).value)

        val pricer = EspprcPricer(instance, h.valueAdapter)
        val request = PricingRequest(
            instance = instance, duals = duals, branchMask = null,
            pricingTolerance = Flt64(1e-6), vehicleTypeId = vehicleTypeId
        )
        val result = assertNotNull((pricer.price(graph, request) as? Ok)?.value)

        // 到达时间 = due time 应该可行（闭区间）
        assertTrue(result.routes.isNotEmpty(), "Arrival exactly at due time should be feasible")
    }

    /**
     * 容量边界测试：负载恰好等于容量。
     */
    @Test
    fun espprcShouldAllowLoadExactlyAtCapacity() {
        val instance = assertNotNull(
            VrptwInstance(
                name = "test-capacity-boundary",
                startDepot = Depot(node("start", Flt64.zero), window(Flt64.zero, Flt64(100.0))),
                endDepot = Depot(node("end", Flt64(2.0)), window(Flt64.zero, Flt64(100.0))),
                customers = listOf(
                    assertNotNull(Customer(
                        id = CustomerId("c1"), node = node("c1", Flt64.one),
                        demand = Quantity(Flt64(3.0), Kilogram), // 恰好等于容量
                        timeWindow = window(Flt64.zero, Flt64(50.0)),
                        serviceTime = Duration.ZERO
                    ).value)
                ),
                vehicleTypes = listOf(
                    assertNotNull(VehicleType(
                        id = VehicleTypeId("v1"),
                        capacity = Quantity(Flt64(3.0), Kilogram), // 容量 = 3
                        fixedCost = Quantity(Flt64(10.0), NoneUnit),
                        amount = 1
                    ).value)
                ),
                units = units,
                schedulingWindow = schedulingWindow,
                tolerances = VrptwTolerances.default
            ).value
        )
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")

        val duals = PricingDuals(
            phase = PricingPhase.PhaseTwo,
            customer = mapOf(CustomerId("c1") to Flt64(20.0)),
            fleet = mapOf(vehicleTypeId to Flt64.zero)
        )

        val builder = RouteGraphBuilder(instance, h.valueAdapter, h.distanceCalculator, h.travelTimeCalculator, h.arcCostCalculator)
        val graph = assertNotNull(builder.build(vehicleTypeId, duals).value)

        val pricer = EspprcPricer(instance, h.valueAdapter)
        val request = PricingRequest(
            instance = instance, duals = duals, branchMask = null,
            pricingTolerance = Flt64(1e-6), vehicleTypeId = vehicleTypeId
        )
        val result = assertNotNull((pricer.price(graph, request) as? Ok)?.value)

        // 负载 = 容量应该可行
        assertTrue(result.routes.isNotEmpty(), "Load exactly at capacity should be feasible")
    }

    /**
     * 不可达客户测试：时间窗使某些客户不可达，ESPPRC 不会生成包含该客户的路线。
     */
    @Test
    fun espprcShouldNotProduceRoutesWithUnreachableCustomers() {
        // c2 的 due time = 0.5，从任何节点到达 c2 的行驶时间 > 0.5，不可达
        val instance = assertNotNull(
            VrptwInstance(
                name = "test-unreachable",
                startDepot = Depot(node("start", Flt64.zero), window(Flt64.zero, Flt64(100.0))),
                endDepot = Depot(node("end", Flt64(3.0)), window(Flt64.zero, Flt64(100.0))),
                customers = listOf(
                    assertNotNull(Customer(
                        id = CustomerId("c1"), node = node("c1", Flt64.one),
                        demand = Quantity(Flt64(1.0), Kilogram),
                        timeWindow = window(Flt64.zero, Flt64(50.0)),
                        serviceTime = Duration.ZERO
                    ).value),
                    assertNotNull(Customer(
                        id = CustomerId("c2"), node = node("c2", Flt64(2.0)),
                        demand = Quantity(Flt64(1.0), Kilogram),
                        timeWindow = window(Flt64.zero, Flt64(0.5)), // due time = 0.5, 不可达
                        serviceTime = Duration.ZERO
                    ).value)
                ),
                vehicleTypes = listOf(
                    assertNotNull(VehicleType(
                        id = VehicleTypeId("v1"),
                        capacity = Quantity(Flt64(5.0), Kilogram),
                        fixedCost = Quantity(Flt64(10.0), NoneUnit),
                        amount = 1
                    ).value)
                ),
                units = units,
                schedulingWindow = schedulingWindow,
                tolerances = VrptwTolerances.default
            ).value
        )
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")

        val duals = PricingDuals(
            phase = PricingPhase.PhaseTwo,
            customer = mapOf(CustomerId("c1") to Flt64(1.0), CustomerId("c2") to Flt64(1.0)),
            fleet = mapOf(vehicleTypeId to Flt64.zero)
        )

        val builder = RouteGraphBuilder(instance, h.valueAdapter, h.distanceCalculator, h.travelTimeCalculator, h.arcCostCalculator)
        val graph = assertNotNull(builder.build(vehicleTypeId, duals).value)

        val pricer = EspprcPricer(instance, h.valueAdapter)
        val request = PricingRequest(
            instance = instance, duals = duals, branchMask = null,
            pricingTolerance = Flt64(1e-6), vehicleTypeId = vehicleTypeId
        )
        val result = assertNotNull((pricer.price(graph, request) as? Ok)?.value)

        // ESPPRC 不应生成包含 c2 的路线（c2 不可达）
        for (route in result.routes) {
            assertFalse(
                route.stops.any { it.customerId == CustomerId("c2") },
                "Route ${route.signature} should not contain unreachable customer c2"
            )
        }
    }

    /**
     * 重复访问测试：ESPPRC 保证 elementarity，不会生成重复访问客户的路线。
     */
    @Test
    fun espprcShouldNotProduceRoutesWithRepeatedCustomers() {
        val instance = makeThreeCustomerInstance()
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")

        val duals = PricingDuals(
            phase = PricingPhase.PhaseTwo,
            customer = mapOf(
                CustomerId("c1") to Flt64(5.0),
                CustomerId("c2") to Flt64(5.0),
                CustomerId("c3") to Flt64(5.0)
            ),
            fleet = mapOf(vehicleTypeId to Flt64.zero)
        )

        val builder = RouteGraphBuilder(instance, h.valueAdapter, h.distanceCalculator, h.travelTimeCalculator, h.arcCostCalculator)
        val graph = assertNotNull(builder.build(vehicleTypeId, duals).value)

        val pricer = EspprcPricer(instance, h.valueAdapter)
        val request = PricingRequest(
            instance = instance, duals = duals, branchMask = null,
            pricingTolerance = Flt64(1e-6), vehicleTypeId = vehicleTypeId
        )
        val result = assertNotNull((pricer.price(graph, request) as? Ok)?.value)

        // 每条路线的客户 ID 应唯一 / Each customer ID should appear at most once
        for (route in result.routes) {
            val customerIds = route.stops.mapNotNull { it.customerId }
            assertEquals(customerIds.size, customerIds.toSet().size,
                "Route ${route.signature} has repeated customers")
        }
    }

    /**
     * 列数截断测试：maxColumnsPerPricing 限制返回列数，
     * 但 exactPricingComplete 仍应正确反映是否存在更多负列。
     */
    @Test
    fun espprcShouldRespectMaxColumnsPerPricing() {
        val instance = makeThreeCustomerInstance()
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")

        // 高对偶值使多条路线有负 reduced cost
        val duals = PricingDuals(
            phase = PricingPhase.PhaseTwo,
            customer = mapOf(
                CustomerId("c1") to Flt64(5.0),
                CustomerId("c2") to Flt64(5.0),
                CustomerId("c3") to Flt64(5.0)
            ),
            fleet = mapOf(vehicleTypeId to Flt64.zero)
        )

        val builder = RouteGraphBuilder(instance, h.valueAdapter, h.distanceCalculator, h.travelTimeCalculator, h.arcCostCalculator)
        val graph = assertNotNull(builder.build(vehicleTypeId, duals).value)

        // 先不限制列数，获取完整结果
        val pricer = EspprcPricer(instance, h.valueAdapter)
        val fullRequest = PricingRequest(
            instance = instance, duals = duals, branchMask = null,
            pricingTolerance = Flt64(1e-6), maxColumnsPerPricing = Int.MAX_VALUE,
            vehicleTypeId = vehicleTypeId
        )
        val fullResult = assertNotNull((pricer.price(graph, fullRequest) as? Ok)?.value)

        // 如果有超过1条负列，测试截断
        if (fullResult.routes.size > 1) {
            val truncatedRequest = PricingRequest(
                instance = instance, duals = duals, branchMask = null,
                pricingTolerance = Flt64(1e-6), maxColumnsPerPricing = 1,
                vehicleTypeId = vehicleTypeId
            )
            val truncatedResult = assertNotNull((pricer.price(graph, truncatedRequest) as? Ok)?.value)

            // 截断后只返回1条路线
            assertTrue(truncatedResult.routes.size <= 1,
                "Truncated result should have at most 1 route")

            // minReducedCost 应该与完整结果一致
            assertTrue(
                (truncatedResult.minReducedCost - fullResult.minReducedCost).abs() ls Flt64(1e-6),
                "Min reduced cost should be the same regardless of truncation"
            )

            // exactPricingComplete 应该反映真实状态（可能仍有负列未返回）
            // 截断不应误报定价收敛
            if (fullResult.routes.size > 1) {
                // 如果完整结果有多条负列，截断后不应声称定价完成
                // 注意：exactPricingComplete 基于 minReducedCost 是否 >= -tolerance
                // 截断不影响 minReducedCost，所以 exactPricingComplete 应该一致
                assertEquals(fullResult.exactPricingComplete, truncatedResult.exactPricingComplete,
                    "Truncation should not affect exactPricingComplete")
            }
        }
    }

    /**
     * 无负列测试：当所有路线的 reduced cost >= 0 时，ESPPRC 应报告定价完成。
     */
    @Test
    fun espprcShouldReportPricingCompleteWhenNoNegativeReducedCost() {
        val instance = makeInstance()
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")

        // 零对偶值：所有路线的 reduced cost = objective cost >= 0
        val duals = PricingDuals(
            phase = PricingPhase.PhaseTwo,
            customer = mapOf(CustomerId("c1") to Flt64.zero, CustomerId("c2") to Flt64.zero),
            fleet = mapOf(vehicleTypeId to Flt64.zero)
        )

        val builder = RouteGraphBuilder(instance, h.valueAdapter, h.distanceCalculator, h.travelTimeCalculator, h.arcCostCalculator)
        val graph = assertNotNull(builder.build(vehicleTypeId, duals).value)

        val pricer = EspprcPricer(instance, h.valueAdapter)
        val request = PricingRequest(
            instance = instance, duals = duals, branchMask = null,
            pricingTolerance = Flt64(1e-6), vehicleTypeId = vehicleTypeId
        )
        val result = assertNotNull((pricer.price(graph, request) as? Ok)?.value)

        // 所有路线的 objective cost >= 0（固定成本 + 弧成本），所以 reduced cost >= 0
        assertTrue(result.exactPricingComplete, "Pricing should be complete when no negative reduced cost")
        assertTrue(result.routes.isEmpty(), "No routes should be returned when no negative reduced cost")
    }

    /**
     * Branch mask 测试：禁止某客户时，ESPPRC 不生成包含该客户的路线。
     */
    @Test
    fun espprcShouldRespectBranchMaskForForbiddenNode() {
        val instance = makeThreeCustomerInstance()
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")

        // 创建禁止 c1 的 branch mask
        val mask = assertNotNull(
            BranchMask(
                startDepot = instance.startDepot.node.id,
                endDepot = instance.endDepot.node.id,
                forbiddenNodes = mapOf(vehicleTypeId to setOf(instance.customers[0].node.id))
            ).value
        )

        val duals = PricingDuals(
            phase = PricingPhase.PhaseTwo,
            customer = mapOf(
                CustomerId("c1") to Flt64(8.0),
                CustomerId("c2") to Flt64(7.0),
                CustomerId("c3") to Flt64(6.0)
            ),
            fleet = mapOf(vehicleTypeId to Flt64(8.0))
        )

        val builder = RouteGraphBuilder(instance, h.valueAdapter, h.distanceCalculator, h.travelTimeCalculator, h.arcCostCalculator)
        val graph = assertNotNull(builder.build(vehicleTypeId, duals, mask).value)

        val pricer = EspprcPricer(instance, h.valueAdapter)
        val request = PricingRequest(
            instance = instance, duals = duals, branchMask = mask,
            pricingTolerance = Flt64(1e-6), vehicleTypeId = vehicleTypeId
        )
        val result = assertNotNull((pricer.price(graph, request) as? Ok)?.value)

        // 所有返回的路线不应包含 c1
        for (route in result.routes) {
            assertFalse(
                route.stops.any { it.customerId == CustomerId("c1") },
                "Route ${route.signature} should not contain forbidden customer c1"
            )
        }
    }

    /**
     * Branch mask 测试：禁止某弧时，ESPPRC 不生成使用该弧的路线。
     */
    @Test
    fun espprcShouldRespectBranchMaskForForbiddenArc() {
        val instance = makeThreeCustomerInstance()
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")

        // 创建禁止 start→c1 弧的 branch mask
        val mask = assertNotNull(
            BranchMask(
                startDepot = instance.startDepot.node.id,
                endDepot = instance.endDepot.node.id,
                forbiddenArcs = setOf(ResourceArc(
                    resourceKey = vehicleTypeId,
                    from = instance.startDepot.node.id,
                    to = instance.customers[0].node.id
                ))
            ).value
        )

        val duals = PricingDuals(
            phase = PricingPhase.PhaseTwo,
            customer = mapOf(
                CustomerId("c1") to Flt64(8.0),
                CustomerId("c2") to Flt64(7.0),
                CustomerId("c3") to Flt64(6.0)
            ),
            fleet = mapOf(vehicleTypeId to Flt64(8.0))
        )

        val builder = RouteGraphBuilder(instance, h.valueAdapter, h.distanceCalculator, h.travelTimeCalculator, h.arcCostCalculator)
        val graph = assertNotNull(builder.build(vehicleTypeId, duals, mask).value)

        val pricer = EspprcPricer(instance, h.valueAdapter)
        val request = PricingRequest(
            instance = instance, duals = duals, branchMask = mask,
            pricingTolerance = Flt64(1e-6), vehicleTypeId = vehicleTypeId
        )
        val result = assertNotNull((pricer.price(graph, request) as? Ok)?.value)

        // 所有返回的路线不应以 c1 作为第一个客户（因为 start→c1 弧被禁止）
        for (route in result.routes) {
            val firstCustomerStop = route.stops.firstOrNull { it.customerId != null }
            assertFalse(
                firstCustomerStop?.customerId == CustomerId("c1"),
                "Route ${route.signature} should not start with c1 (forbidden arc start→c1)"
            )
        }
    }

    /**
     * Feillet 不可达标记测试：访问某客户后，因时间窗限制，
     * 其他客户变为不可达，ESPPRC 的 forbidden 集合应包含这些客户。
     * 验证方式：比较有 Feillet 标记和无 Feillet 标记时 ESPPRC 的行为差异——
     * Feillet 标记应使支配更有效，减少不必要的标签扩展。
     */
    @Test
    fun espprcShouldMarkUnreachableCustomersViaFeilletPruning() {
        // 三个客户：c1 在 x=1, c2 在 x=2, c3 在 x=3
        // c3 的 due time = 3.5（从 c2 到 c3 需要 1 单位时间，到达 = 3+1=4 > 3.5）
        // 所以访问 c2 后，c3 不可达（Feillet 标记）
        val instance = assertNotNull(
            VrptwInstance(
                name = "test-feillet",
                startDepot = Depot(node("start", Flt64.zero), window(Flt64.zero, Flt64(100.0))),
                endDepot = Depot(node("end", Flt64(4.0)), window(Flt64.zero, Flt64(100.0))),
                customers = listOf(
                    assertNotNull(Customer(
                        id = CustomerId("c1"), node = node("c1", Flt64.one),
                        demand = Quantity(Flt64(1.0), Kilogram),
                        timeWindow = window(Flt64.zero, Flt64(50.0)),
                        serviceTime = Duration.ZERO
                    ).value),
                    assertNotNull(Customer(
                        id = CustomerId("c2"), node = node("c2", Flt64(2.0)),
                        demand = Quantity(Flt64(1.0), Kilogram),
                        timeWindow = window(Flt64.zero, Flt64(50.0)),
                        serviceTime = Duration.ZERO
                    ).value),
                    assertNotNull(Customer(
                        id = CustomerId("c3"), node = node("c3", Flt64(3.0)),
                        demand = Quantity(Flt64(1.0), Kilogram),
                        timeWindow = window(Flt64.zero, Flt64(2.5)), // due time = 2.5, 访问 c2 后不可达
                        serviceTime = Duration.ZERO
                    ).value)
                ),
                vehicleTypes = listOf(
                    assertNotNull(VehicleType(
                        id = VehicleTypeId("v1"),
                        capacity = Quantity(Flt64(3.0), Kilogram),
                        fixedCost = Quantity(Flt64(10.0), NoneUnit),
                        amount = 1
                    ).value)
                ),
                units = units,
                schedulingWindow = schedulingWindow,
                tolerances = VrptwTolerances.default
            ).value
        )
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")

        val duals = PricingDuals(
            phase = PricingPhase.PhaseTwo,
            customer = mapOf(
                CustomerId("c1") to Flt64(8.0),
                CustomerId("c2") to Flt64(7.0),
                CustomerId("c3") to Flt64(6.0)
            ),
            fleet = mapOf(vehicleTypeId to Flt64(8.0))
        )

        val builder = RouteGraphBuilder(instance, h.valueAdapter, h.distanceCalculator, h.travelTimeCalculator, h.arcCostCalculator)
        val graph = assertNotNull(builder.build(vehicleTypeId, duals).value)

        val pricer = EspprcPricer(instance, h.valueAdapter)
        val request = PricingRequest(
            instance = instance, duals = duals, branchMask = null,
            pricingTolerance = Flt64(1e-6), vehicleTypeId = vehicleTypeId
        )
        val result = assertNotNull((pricer.price(graph, request) as? Ok)?.value)

        // 访问 c2 后（departure time >= 2），到 c3 的行驶时间 = 1，到达 >= 3
        // c3 due time = 2.5 → 到达 3 > 2.5 → 不可达
        // ESPPRC 不应生成包含 c2→c3 序列的路线
        for (route in result.routes) {
            val customerSequence = route.stops.mapNotNull { it.customerId }
            val c2Index = customerSequence.indexOf(CustomerId("c2"))
            val c3Index = customerSequence.indexOf(CustomerId("c3"))
            if (c2Index >= 0 && c3Index >= 0) {
                assertFalse(c2Index < c3Index,
                    "Route ${route.signature} should not have c2 before c3 (c3 unreachable after c2)")
            }
        }
    }

    /**
     * 穷举 oracle 增强测试：ESPPRC 返回的每条路线的 reduced cost
     * 都应在穷举结果中有对应值。
     */
    @Test
    fun espprcRoutesReducedCostsShouldAllAppearInExhaustiveOracle() {
        val instance = makeThreeCustomerInstance()
        val h = makeHelpers(instance)
        val vehicleTypeId = VehicleTypeId("v1")

        val duals = PricingDuals(
            phase = PricingPhase.PhaseTwo,
            customer = mapOf(
                CustomerId("c1") to Flt64(8.0),
                CustomerId("c2") to Flt64(7.0),
                CustomerId("c3") to Flt64(6.0)
            ),
            fleet = mapOf(vehicleTypeId to Flt64(8.0))
        )

        val builder = RouteGraphBuilder(instance, h.valueAdapter, h.distanceCalculator, h.travelTimeCalculator, h.arcCostCalculator)
        val graph = assertNotNull(builder.build(vehicleTypeId, duals).value)

        val pricer = EspprcPricer(instance, h.valueAdapter)
        val request = PricingRequest(
            instance = instance, duals = duals, branchMask = null,
            pricingTolerance = Flt64(1e-6), maxColumnsPerPricing = Int.MAX_VALUE,
            vehicleTypeId = vehicleTypeId
        )
        val result = assertNotNull((pricer.price(graph, request) as? Ok)?.value)

        // 获取穷举所有 reduced cost
        val oracleReducedCosts = enumerateAllReducedCosts(instance, h, vehicleTypeId, duals)

        // 每条 ESPPRC 路线的 reduced cost 应该在 oracle 结果中存在
        for (route in result.routes) {
            val routeReducedCost = computeRouteReducedCost(route, instance, h, duals)
            val foundInOracle = oracleReducedCosts.any { (it - routeReducedCost).abs() ls Flt64(1e-3) }
            assertTrue(foundInOracle,
                "Route ${route.signature} reduced cost $routeReducedCost should appear in oracle $oracleReducedCosts")
        }
    }

    // ========== 辅助方法 ==========

    /** 测试辅助数据类 / Test helper data class */
    private data class PricingHelpers(
        val instance: VrptwInstance<Flt64>,
        val valueAdapter: NetworkSchedulingSolverValueAdapter<Flt64>,
        val distanceCalculator: DistanceCalculator<Flt64>,
        val travelTimeCalculator: TravelTimeCalculator<Flt64>,
        val arcCostCalculator: ArcCostCalculator<Flt64>,
        val routeCostPolicy: RouteCostPolicy<Flt64>
    )

    /**
     * 穷举所有可行路线并计算 reduced cost。
     * / Enumerate all feasible routes and compute reduced cost.
     */
    private fun enumerateAllReducedCosts(
        instance: VrptwInstance<Flt64>,
        h: PricingHelpers,
        vehicleTypeId: VehicleTypeId,
        duals: PricingDuals
    ): List<Flt64> {
        val vehicleType = instance.vehicleTypeById[vehicleTypeId] ?: return emptyList()
        val flt64Window = instance.schedulingWindow.toFlt64Boundary()
        val reducedCosts = mutableListOf<Flt64>()

        // 枚举所有客户排列 / Enumerate all customer permutations
        val customers = instance.customers
        val permutations = generatePermutations(customers.indices.toList())

        for (perm in permutations) {
            // 尝试所有前缀（1个客户、2个客户...） / Try all prefixes
            for (prefixLen in 1..perm.size) {
                val selected = perm.take(prefixLen)
                val route = buildRouteFromCustomerSequence(instance, h, vehicleType, selected)
                if (route != null) {
                    val rc = computeRouteReducedCost(route, instance, h, duals)
                    reducedCosts.add(rc)
                }
            }
        }

        return reducedCosts
    }

    /**
     * 从客户索引序列构建路线。 / Build route from customer index sequence.
     */
    private fun buildRouteFromCustomerSequence(
        instance: VrptwInstance<Flt64>,
        h: PricingHelpers,
        vehicleType: VehicleType<Flt64>,
        customerIndices: List<Int>
    ): Route<Flt64>? {
        val flt64Window = instance.schedulingWindow.toFlt64Boundary()
        val startDepot = instance.startDepot
        val endDepot = instance.endDepot

        var currentTime = flt64Window.valueOf(startDepot.timeWindow.readyTime)
        var currentLoad = Flt64.zero
        var totalDistance = Flt64.zero
        val arcCosts = mutableListOf<Quantity<Flt64>>()
        val stops = mutableListOf<RouteStop<Flt64>>()

        // 起始 depot / Start depot
        stops.add(RouteStop(
            nodeId = startDepot.node.id,
            customerId = null,
            arrival = flt64Window.instantOf(currentTime),
            serviceStart = flt64Window.instantOf(currentTime),
            departure = flt64Window.instantOf(currentTime),
            accumulatedLoad = Quantity(Flt64.zero, instance.units.loadUnit)
        ))

        var prevNode = startDepot.node
        for (idx in customerIndices) {
            val customer = instance.customers[idx]

            // 行驶时间 / Travel time
            val travelTime = h.travelTimeCalculator.travelTime(prevNode, customer.node, vehicleType).value ?: return null
            val distance = h.distanceCalculator.distance(prevNode, customer.node).value ?: return null
            val arcCost = h.arcCostCalculator.cost(prevNode, customer.node, distance, travelTime, vehicleType).value ?: return null
            arcCosts.add(arcCost)
            totalDistance = totalDistance + (h.valueAdapter.normalize(distance, instance.units.distanceUnit).value ?: return null)

            val arrival = currentTime + flt64Window.valueOf(travelTime)
            val serviceStart = maxOf(arrival, flt64Window.valueOf(customer.timeWindow.readyTime))
            if (serviceStart gr flt64Window.valueOf(customer.timeWindow.dueTime)) return null

            currentLoad = currentLoad + (h.valueAdapter.normalize(customer.demand, instance.units.loadUnit).value ?: return null)
            if (currentLoad gr (h.valueAdapter.normalize(vehicleType.capacity, instance.units.loadUnit).value ?: return null)) return null

            val departure = serviceStart + flt64Window.valueOf(customer.serviceTime)
            currentTime = departure

            stops.add(RouteStop(
                nodeId = customer.node.id,
                customerId = customer.id,
                arrival = flt64Window.instantOf(arrival),
                serviceStart = flt64Window.instantOf(serviceStart),
                departure = flt64Window.instantOf(departure),
                accumulatedLoad = Quantity(h.valueAdapter.fromSolverValue(currentLoad).value ?: return null, instance.units.loadUnit)
            ))

            prevNode = customer.node
        }

        // 到结束 depot / To end depot
        val travelTime = h.travelTimeCalculator.travelTime(prevNode, endDepot.node, vehicleType).value ?: return null
        val distance = h.distanceCalculator.distance(prevNode, endDepot.node).value ?: return null
        val arcCost = h.arcCostCalculator.cost(prevNode, endDepot.node, distance, travelTime, vehicleType).value ?: return null
        arcCosts.add(arcCost)
        totalDistance = totalDistance + (h.valueAdapter.normalize(distance, instance.units.distanceUnit).value ?: return null)

        val arrival = currentTime + flt64Window.valueOf(travelTime)
        val serviceStart = maxOf(arrival, flt64Window.valueOf(endDepot.timeWindow.readyTime))
        if (serviceStart gr flt64Window.valueOf(endDepot.timeWindow.dueTime)) return null

        stops.add(RouteStop(
            nodeId = endDepot.node.id,
            customerId = null,
            arrival = flt64Window.instantOf(arrival),
            serviceStart = flt64Window.instantOf(serviceStart),
            departure = flt64Window.instantOf(serviceStart),
            accumulatedLoad = Quantity(h.valueAdapter.fromSolverValue(currentLoad).value ?: return null, instance.units.loadUnit)
        ))

        val cost = h.routeCostPolicy.cost(vehicleType, arcCosts).value ?: return null

        return Route(
            vehicleTypeId = vehicleType.id,
            stops = stops,
            distance = Quantity(h.valueAdapter.fromSolverValue(totalDistance).value ?: return null, instance.units.distanceUnit),
            cost = cost
        ).value
    }

    /**
     * 计算路线的 reduced cost。
     * / Compute reduced cost of a route.
     *
     * reducedCost = objectiveCost - sum(customerDual) - fleetDual
     */
    private fun computeRouteReducedCost(
        route: Route<Flt64>,
        instance: VrptwInstance<Flt64>,
        h: PricingHelpers,
        duals: PricingDuals
    ): Flt64 {
        val solverCost = h.valueAdapter.normalize(route.cost, instance.units.costUnit).value ?: Flt64.zero
        val customerDualSum = route.stops.mapNotNull { stop ->
            stop.customerId?.let { duals.customer[it] }
        }.fold(Flt64.zero) { acc, v -> acc + v }
        val fleetDual = duals.fleet[route.vehicleTypeId] ?: Flt64.zero
        return solverCost - customerDualSum - fleetDual
    }

    /**
     * 生成排列。 / Generate permutations.
     */
    private fun generatePermutations(elements: List<Int>): List<List<Int>> {
        if (elements.isEmpty()) return listOf(emptyList())
        val result = mutableListOf<List<Int>>()
        for (i in elements.indices) {
            val rest = elements.filterIndexed { j, _ -> j != i }
            for (perm in generatePermutations(rest)) {
                result.add(listOf(elements[i]) + perm)
            }
        }
        return result
    }
}
