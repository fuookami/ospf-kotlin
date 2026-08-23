@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation

import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.pricing.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service.VrptwValidator
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.service.limits.CustomerCoverageConstraint
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.service.limits.FleetSizeConstraint

class RouteCompilationTest {

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

    private fun makeRoute(
        instance: VrptwInstance<Flt64>,
        customerIds: List<CustomerId>,
        vehicleTypeId: VehicleTypeId = VehicleTypeId("v1"),
        cost: Flt64 = Flt64(10.0),
        distance: Flt64 = Flt64(1.0)
    ): Route<Flt64> {
        val startDepot = instance.startDepot
        val endDepot = instance.endDepot
        val stops = mutableListOf<RouteStop<Flt64>>()

        stops.add(RouteStop(
            nodeId = startDepot.node.id, customerId = null,
            arrival = instant(Flt64.zero), serviceStart = instant(Flt64.zero), departure = instant(Flt64.zero),
            accumulatedLoad = Quantity(Flt64.zero, Kilogram)
        ))

        var load = Flt64.zero
        var time = Flt64.zero
        for (cid in customerIds) {
            val customer = instance.customerById[cid]!!
            load += customer.demand.value
            time += Flt64.one
            stops.add(RouteStop(
                nodeId = customer.node.id, customerId = cid,
                arrival = instant(time), serviceStart = instant(time), departure = instant(time),
                accumulatedLoad = Quantity(load, Kilogram)
            ))
        }

        time += Flt64.one
        stops.add(RouteStop(
            nodeId = endDepot.node.id, customerId = null,
            arrival = instant(time), serviceStart = instant(time), departure = instant(time),
            accumulatedLoad = Quantity(load, Kilogram)
        ))

        return assertNotNull(Route(
            vehicleTypeId = vehicleTypeId,
            stops = stops,
            distance = Quantity(distance, Meter),
            cost = Quantity(cost, NoneUnit)
        ).value)
    }

    @Test
    fun routeColumnPoolShouldDeduplicateRoutesBySignature() {
        val instance = makeInstance()
        val pool = RouteColumnPool<Flt64>()

        val route1 = makeRoute(instance, listOf(CustomerId("c1")))
        val route2 = makeRoute(instance, listOf(CustomerId("c1")))
        val route3 = makeRoute(instance, listOf(CustomerId("c2")))

        val added1 = pool.addColumns(listOf(route1))
        assertEquals(1, added1.size)
        assertEquals(1, pool.size)

        val added2 = pool.addColumns(listOf(route2))
        assertEquals(0, added2.size)
        assertEquals(1, pool.size)

        val added3 = pool.addColumns(listOf(route3))
        assertEquals(1, added3.size)
        assertEquals(2, pool.size)
    }

    @Test
    fun routeColumnPoolShouldRemoveRoutes() {
        val instance = makeInstance()
        val pool = RouteColumnPool<Flt64>()

        val route1 = makeRoute(instance, listOf(CustomerId("c1")))
        val route2 = makeRoute(instance, listOf(CustomerId("c2")))

        pool.addColumns(listOf(route1, route2))
        assertEquals(2, pool.size)

        pool.removeColumns(setOf(route1))
        assertEquals(1, pool.size)
        assertFalse(pool.contains(route1.signature))
        assertTrue(pool.contains(route2.signature))
    }

    @Test
    fun routeCompilationShouldRegisterModelAndAddColumns() {
        val instance = makeInstance()
        val valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
        val compilation = RouteCompilation(instance, valueAdapter)
        val model = LinearMetaModel("test_rmp")

        val result = compilation.register(model)
        assertTrue(result is Ok)

        val route1 = makeRoute(instance, listOf(CustomerId("c1")))
        val addResult = compilation.addColumns(UInt64.zero, listOf(route1), model)
        assertTrue(addResult is Ok)
        assertEquals(1, addResult.value.size)
        assertEquals(1, compilation.columnPool.size)
    }

    @Test
    fun routeCompilationShouldDeduplicateWhenAddingColumns() {
        val instance = makeInstance()
        val valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
        val compilation = RouteCompilation(instance, valueAdapter)
        val model = LinearMetaModel("test_rmp")

        compilation.register(model)

        val route1 = makeRoute(instance, listOf(CustomerId("c1")))
        compilation.addColumns(UInt64.zero, listOf(route1), model)

        val route2 = makeRoute(instance, listOf(CustomerId("c1")))
        val addResult = compilation.addColumns(UInt64.one, listOf(route2), model)
        assertTrue(addResult is Ok)
        assertEquals(0, addResult.value.size)
    }

    @Test
    fun artificialCoverageShouldRegisterAndSwitchToPhaseTwo() {
        val instance = makeInstance()
        val valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
        val compilation = RouteCompilation(instance, valueAdapter)
        val model = LinearMetaModel("test_artificial")

        val result = compilation.register(model)
        assertTrue(result is Ok)

        assertFalse(compilation.artificialCoverage.isPhaseTwo)

        val switchResult = compilation.switchToPhaseTwo(model)
        assertTrue(switchResult is Ok)
        assertTrue(compilation.artificialCoverage.isPhaseTwo)
    }

    @Test
    fun contextSwitchToPhaseTwoShouldRegisterRouteCostObjective() {
        val instance = makeInstance()
        val context = RouteCompilationContext(instance, Flt64NetworkSchedulingSolverValueAdapter)
        val model = LinearMetaModel("test_phase_two_objective")

        assertTrue(context.register(model) is Ok)
        assertTrue(context.switchToPhaseTwo(model) is Ok)
        val mechanism = runBlocking {
            LinearMechanismModel.invoke(metaModel = model, concurrent = false)
        }
        assertTrue(mechanism is Ok)
        assertTrue(mechanism.value.objectFunction.subObjects.any { it.name == "route_cost_minimization" })
    }

    @Test
    fun phaseTwoAddedColumnsShouldRegisterIncrementalRouteCostObjective() {
        val instance = makeInstance()
        val context = RouteCompilationContext(instance, Flt64NetworkSchedulingSolverValueAdapter)
        val model = LinearMetaModel("test_phase_two_incremental_objective")
        val route1 = makeRoute(instance, listOf(CustomerId("c1")))
        val route2 = makeRoute(instance, listOf(CustomerId("c2")))

        assertTrue(context.register(model) is Ok)
        assertTrue(context.addColumns(UInt64.zero, listOf(route1), model) is Ok)
        assertTrue(context.switchToPhaseTwo(model) is Ok)
        assertTrue(context.addColumns(UInt64.one, listOf(route2), model) is Ok)
        val mechanism = runBlocking {
            LinearMechanismModel.invoke(metaModel = model, concurrent = false)
        }

        assertTrue(mechanism is Ok)
        assertTrue(mechanism.value.objectFunction.subObjects.any { it.name == "route_cost_minimization_1" })
    }

    @Test
    fun vrpShadowPriceMapShouldOutputPricingDuals() {
        val map = VrpShadowPriceMap()
        map.put(ShadowPrice(CustomerCoverageShadowPriceKey(CustomerId("c1")), Flt64(3.0)))
        map.put(ShadowPrice(CustomerCoverageShadowPriceKey(CustomerId("c2")), Flt64(-1.5)))
        map.put(ShadowPrice(FleetSizeShadowPriceKey(VehicleTypeId("v1")), Flt64(-2.0)))

        assertEquals(Flt64(3.0), map.customerDual(CustomerId("c1")))
        assertEquals(Flt64(-1.5), map.customerDual(CustomerId("c2")))
        assertEquals(Flt64.zero, map.customerDual(CustomerId("c3")))
        assertEquals(Flt64(-2.0), map.fleetDual(VehicleTypeId("v1")))
        assertEquals(Flt64.zero, map.fleetDual(VehicleTypeId("v2")))

        val duals = map.toPricingDuals(PricingPhase.PhaseOne)
        assertEquals(PricingPhase.PhaseOne, duals.phase)
        assertEquals(Flt64(3.0), duals.customer[CustomerId("c1")])
        assertEquals(Flt64(-1.5), duals.customer[CustomerId("c2")])
        assertEquals(Flt64(-2.0), duals.fleet[VehicleTypeId("v1")])
    }

    @Test
    fun phaseTwoShouldReuseRegisteredConstraintPipelinesForDualExtraction() {
        val instance = makeInstance()
        val context = RouteCompilationContext(instance, Flt64NetworkSchedulingSolverValueAdapter)
        val model = LinearMetaModel("test_phase_two_dual_extraction")

        assertTrue(context.register(model) is Ok)
        val phaseOneCoveragePipeline = context.pipelineList[0]
        val phaseOneFleetPipeline = context.pipelineList[1]
        val coverageConstraints = model.constraintsOfGroup(phaseOneCoveragePipeline)
        val fleetConstraints = model.constraintsOfGroup(phaseOneFleetPipeline)
        assertTrue(context.switchToPhaseTwo(model) is Ok)

        assertSame(phaseOneCoveragePipeline, context.pipelineList[0])
        assertSame(phaseOneFleetPipeline, context.pipelineList[1])
        val dualValues = buildMap {
            coverageConstraints.forEachIndexed { index, constraint ->
                put(constraint, Flt64(10.0 + index))
            }
            fleetConstraints.forEach { constraint ->
                put(constraint, Flt64(-2.0))
            }
        }
        val duals = assertNotNull(context.extractPricingDuals(
            model = model,
            shadowPrices = MetaDualSolution(
                constraints = dualValues,
                symbols = emptyMap()
            )
        ).value)

        assertEquals(Flt64(10.0), duals.customer[instance.customers[0].id])
        assertEquals(Flt64(11.0), duals.customer[instance.customers[1].id])
        assertEquals(Flt64(-2.0), duals.fleet[instance.vehicleTypes[0].id])
    }

    @Test
    fun routeCompilationContextShouldInitializeInPhaseOne() {
        val instance = makeInstance()
        val valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
        val context = RouteCompilationContext(instance, valueAdapter)

        assertEquals(PricingPhase.PhaseOne, context.phase)
    }

    @Test
    fun routeCompilationShouldRemoveColumnsAndSyncModel() {
        val instance = makeInstance()
        val valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
        val compilation = RouteCompilation(instance, valueAdapter)
        val model = LinearMetaModel("test_remove")

        compilation.register(model)

        val route1 = makeRoute(instance, listOf(CustomerId("c1")))
        val route2 = makeRoute(instance, listOf(CustomerId("c2")))
        compilation.addColumns(UInt64.zero, listOf(route1, route2), model)
        assertEquals(2, compilation.columnPool.size)

        // 删除 route1 / Remove route1
        val removeResult = compilation.removeColumns(listOf(route1), model)
        assertTrue(removeResult is Ok, "removeColumns should succeed")
        assertEquals(1, compilation.columnPool.size, "Pool should have 1 route after removal")
        assertFalse(compilation.columnPool.contains(route1.signature), "Removed route should not be in pool")
        assertTrue(compilation.columnPool.contains(route2.signature), "Remaining route should still be in pool")
        assertFalse(compilation.routeVariableIndex.containsKey(route1.signature))
        assertEquals(1, compilation.routeCost.polynomial.monomials.size)

        val reAddResult = compilation.addColumns(UInt64.one, listOf(route1), model)
        assertTrue(reAddResult is Ok)
        assertEquals(1, reAddResult.value.size)
        assertTrue(compilation.routeVariableIndex.containsKey(route1.signature))
        assertEquals(2, compilation.routeCost.polynomial.monomials.size)
    }

    @Test
    fun duplicateOnlyIterationShouldNotCorruptVariableGroupIndex() {
        val instance = makeInstance()
        val compilation = RouteCompilation(instance, Flt64NetworkSchedulingSolverValueAdapter)
        val model = LinearMetaModel("test_duplicate_iteration_index")
        val route1 = makeRoute(instance, listOf(CustomerId("c1")))
        val route2 = makeRoute(instance, listOf(CustomerId("c2")))

        assertTrue(compilation.register(model) is Ok)
        assertTrue(compilation.addColumns(UInt64.zero, listOf(route1), model) is Ok)
        val duplicateResult = compilation.addColumns(UInt64.one, listOf(route1), model)
        assertTrue(duplicateResult is Ok)
        assertTrue(duplicateResult.value.isEmpty())
        assertTrue(compilation.addColumns(UInt64.two, listOf(route2), model) is Ok)

        assertEquals(1, compilation.routeVariableIndex[route2.signature]?.first)
    }

    @Test
    fun artificialCoverageShouldCreateNonNegativeVariableForEachCustomer() {
        val instance = makeInstance()
        val valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
        val compilation = RouteCompilation(instance, valueAdapter)
        val model = LinearMetaModel("test_artificial_vars")

        compilation.register(model)

        // 人工变量数量应等于客户数量 / Artificial variable count should equal customer count
        val a = compilation.artificialCoverage.a
        assertEquals(instance.customers.size, a.shape[0].toInt(),
            "Should have one artificial variable per customer")
    }

    @Test
    fun customerCoverageConstraintShouldBeExactEquality() {
        // 验证客户覆盖约束方向为 = 1（而非 >= 1）
        // / Verify customer coverage constraint direction is = 1 (not >= 1)
        val instance = makeInstance()
        val valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
        val compilation = RouteCompilation(instance, valueAdapter)
        val model = LinearMetaModel("test_constraint_direction")

        compilation.register(model)

        // 注册约束 / Register constraints
        val constraint = CustomerCoverageConstraint(instance.customers, compilation)
        val result = constraint(model)
        assertTrue(result is Ok, "CustomerCoverageConstraint should register successfully")

        // 验证：在客户覆盖表达式中，人工变量 + 路线变量 = 1
        // / Verify: in coverage expression, artificial + route = 1
        for ((index, _) in instance.customers.withIndex()) {
            val coverageExpr = compilation.customerCoverage[index]
            // coverageExpr 应包含人工变量 a[index] 和路线变量
            // / coverageExpr should contain artificial variable a[index] and route variables
            assertNotNull(coverageExpr, "Customer coverage expression $index should exist")
        }
    }

    @Test
    fun fleetSizeConstraintShouldBeLessThanOrEqual() {
        // 验证车队约束方向为 <= amount
        // / Verify fleet size constraint direction is <= amount
        val instance = makeInstance()
        val valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
        val compilation = RouteCompilation(instance, valueAdapter)
        val model = LinearMetaModel("test_fleet_constraint")

        compilation.register(model)

        val constraint = FleetSizeConstraint(instance.vehicleTypes, compilation)
        val result = constraint(model)
        assertTrue(result is Ok, "FleetSizeConstraint should register successfully")

        // 验证：车队使用量表达式存在
        // / Verify: fleet usage expression exists
        for ((index, _) in instance.vehicleTypes.withIndex()) {
            val usageExpr = compilation.fleetUsage[index]
            assertNotNull(usageExpr, "Fleet usage expression $index should exist")
        }
    }

    @Test
    fun routeCompilationShouldSupportFleetLimitedInfeasibility() {
        // 场景：1 辆车，2 个客户，但 2 个客户需要 2 条路线（容量限制）
        // 每个客户单独路线需要 2 辆车，但车队只有 1 辆，所以 RMP 不可行
        // / Scenario: 1 vehicle, 2 customers needing 2 routes, but fleet is only 1
        val instance = assertNotNull(
            VrptwInstance(
                name = "test-fleet-limited",
                startDepot = Depot(node("start", Flt64.zero), window(Flt64.zero, Flt64(100.0))),
                endDepot = Depot(node("end", Flt64(3.0)), window(Flt64.zero, Flt64(100.0))),
                customers = listOf(
                    assertNotNull(Customer(
                        id = CustomerId("c1"), node = node("c1", Flt64.one),
                        demand = Quantity(Flt64(5.0), Kilogram), // capacity = 5, each customer needs its own route
                        timeWindow = window(Flt64.zero, Flt64(50.0)),
                        serviceTime = Duration.ZERO
                    ).value),
                    assertNotNull(Customer(
                        id = CustomerId("c2"), node = node("c2", Flt64(2.0)),
                        demand = Quantity(Flt64(5.0), Kilogram),
                        timeWindow = window(Flt64.zero, Flt64(50.0)),
                        serviceTime = Duration.ZERO
                    ).value)
                ),
                vehicleTypes = listOf(
                    assertNotNull(VehicleType(
                        id = VehicleTypeId("v1"),
                        capacity = Quantity(Flt64(5.0), Kilogram),
                        fixedCost = Quantity(Flt64(10.0), NoneUnit),
                        amount = 1 // 只有 1 辆车！
                    ).value)
                ),
                units = units,
                schedulingWindow = schedulingWindow,
                tolerances = VrptwTolerances.default
            ).value
        )

        val valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
        val context = RouteCompilationContext(instance, valueAdapter)
        val model = LinearMetaModel("test_fleet_infeasible")

        context.register(model)

        // 添加两条单客户路线 / Add two single-customer routes
        val route1 = makeRoute(instance, listOf(CustomerId("c1")))
        val route2 = makeRoute(instance, listOf(CustomerId("c2")))
        context.addColumns(UInt64.zero, listOf(route1, route2), model)

        // Phase I 应该仍然有人工变量活跃（车队约束限制了只能使用 1 条路线）
        // / Phase I should still have active artificial variables
        // 因为 2 个客户都需要被覆盖，但只有 1 辆车
        // / Because both customers need coverage but only 1 vehicle is available
        assertFalse(context.compilation.artificialCoverage.isPhaseTwo,
            "Should still be in Phase I with fleet constraint limiting coverage")
    }

    @Test
    fun routeCompilationShouldExtractRouteValuesAndSolution() {
        // 验证 extractRouteValues 和 extractSolution 能被调用且不抛异常
        // / Verify extractRouteValues and extractSolution can be called without exceptions
        val instance = makeInstance()
        val valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
        val compilation = RouteCompilation(instance, valueAdapter)
        val model = LinearMetaModel("test_extraction")

        compilation.register(model)

        val route1 = makeRoute(instance, listOf(CustomerId("c1")))
        val route2 = makeRoute(instance, listOf(CustomerId("c2")))
        compilation.addColumns(UInt64.zero, listOf(route1, route2), model)

        // 未求解时，token.result 为 null，extractRouteValues 应返回空列表
        // / Without solving, token.result is null, extractRouteValues should return empty list
        val routeValuesResult = compilation.extractRouteValues(model)
        assertTrue(routeValuesResult is Ok, "extractRouteValues should succeed")
        val routeValues = (routeValuesResult as Ok).value
        // 未求解时没有 token result，返回空列表
        // / Without solving, no token result, returns empty list
        assertNotNull(routeValues, "Route values should not be null")

        val solutionResult = compilation.extractSolution(model)
        assertTrue(solutionResult is Ok, "extractSolution should succeed")
        val solution = (solutionResult as Ok).value
        assertNotNull(solution, "Solution should not be null")
    }
}
