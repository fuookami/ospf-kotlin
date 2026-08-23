@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo5

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiColumnGenerationSolver
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service.RouteValidator
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.RouteCompilationContext
import fuookami.ospf.kotlin.framework.network_scheduling.application.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.adapter.Demo17InstanceAdapter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 使用实时 Gurobi 求解器的 demo5 VRPTW 分支定价端到端测试。 / End-to-end test for demo5 VRPTW Branch-and-Price with live Gurobi solver.
 *
 * 此测试在 `demo5-gurobi-bp` 配置下受控，仅在使用 -Pdemo5-gurobi-bp 显式选择时运行。
 * This test is gated under the `demo5-gurobi-bp` profile and only runs
 * when explicitly selected with -Pdemo5-gurobi-bp.
 */
class Demo5BranchAndPriceE2ETest {

    private fun schedulingWindow(): SchedulingTimeWindow<Flt64> = SchedulingTimeWindow(
        window = TimeRange(
            start = Instant.parse("2026-01-01T00:00:00Z"),
            end = Instant.parse("2026-01-01T01:00:00Z")
        ),
        durationUnit = DurationUnit.SECONDS,
        fromDouble = { Flt64(it) },
        toDouble = { it.toDouble() }
    )

    private fun makeSolver(
        timeLimit: Duration = 30.seconds,
        gap: Flt64 = Flt64.zero
    ): ColumnGenerationSolver = GurobiColumnGenerationSolver(
        config = SolverConfig(
            time = timeLimit,
            gap = gap
        )
    )

    private fun makePolicy(
        schedulingWindow: SchedulingTimeWindow<Flt64>,
        costPolicy: String = "demo17"
    ): BranchAndPriceAlgorithm.Policy<Flt64> {
        val distanceCalculator: DistanceCalculator<Flt64> = when (costPolicy) {
            "solomon" -> SolomonDistancePolicy(Meter)
            else -> EuclideanDistanceCalculator(Meter)
        }
        return BranchAndPriceAlgorithm.Policy(
            valueAdapter = Flt64NetworkSchedulingSolverValueAdapter,
            distanceCalculator = distanceCalculator,
            travelTimeCalculator = DistanceAsTravelTimeCalculator(distanceCalculator, schedulingWindow),
            arcCostCalculator = DistanceArcCostCalculator(Meter, NoneUnit),
            routeCostPolicy = when (costPolicy) {
                "solomon" -> FixedPlusArcCostPolicy(NoneUnit)
                else -> Demo17CostPolicy(NoneUnit)
            }
        )
    }

    // ========== 25-Customer Demo17 B&P ==========

    @Test
    fun `demo17 25-customer B&P matches direct MIP contract`(): Unit = runBlocking {
        val sw = schedulingWindow()
        val instance = when (val r = Demo17InstanceAdapter.first25Customers(sw)) {
            is Ok -> r.value
            is Failed -> fail("Demo17 adapter failed: ${r.error}")
            is Fatal -> fail("Demo17 adapter fatal: ${r.errors}")
        }

        val solver = makeSolver(timeLimit = 60.seconds)
        val policy = makePolicy(sw, "demo17")
        val service = VrptwApplicationService(
            instance = instance,
            solver = solver,
            configuration = BranchAndPriceAlgorithm.Configuration(
                timeLimit = 60.seconds
            ),
            policy = policy
        )

        val result = when (val r = service.solve()) {
            is Ok -> r.value
            is Failed -> fail("Solve failed: ${r.error}")
            is Fatal -> fail("Solve fatal: ${r.errors}")
        }

        assertNotEquals(BranchAndPriceStatus.SolverStopped, result.status)
        result.solution?.let { solution ->
            assertSolutionFeasible(
                instance = instance,
                routes = solution.routes,
                policy = policy
            )
            assertTrue((result.upperBound - solution.totalCost.value).abs().leq(Flt64(1e-6)))
        }

        val directMip = when (val r = Demo5DirectMipOracle.solve(
            instance = instance,
            policy = policy,
            solver = makeSolver(
                timeLimit = 60.seconds,
                gap = Flt64(1e-4)
            )
        )) {
            is Ok -> r.value
            is Failed -> fail("Direct MIP solve failed: ${r.error}")
            is Fatal -> fail("Direct MIP solve fatal: ${r.errors}")
        }
        val directRoutes = directMip.customerSequences.map { sequence ->
            buildRoute(instance, sequence, policy)
        }
        assertSolutionFeasible(
            instance = instance,
            routes = directRoutes,
            policy = policy
        )
        assertTrue(directMip.bestBound.leq(directMip.objective + Flt64(1e-6)))

        if (result.status == BranchAndPriceStatus.Optimal && directMip.optimal) {
            assertTrue((directMip.objective - result.upperBound).abs().leq(Flt64(1e-4)))
        } else {
            if (result.lowerBound.isFinite() && !result.lowerBound.isNegativeInfinity()) {
                assertTrue(result.lowerBound.leq(directMip.objective + Flt64(1e-4)))
            }
            result.solution?.let {
                assertTrue(directMip.bestBound.leq(result.upperBound + Flt64(1e-4)))
            }
        }
    }

    // ========== 100-Customer Demo17 Smoke ==========

    @Test
    fun `demo17 100-customer smoke test with time limit`(): Unit = runBlocking {
        val sw = schedulingWindow()
        val instance = when (val r = Demo17InstanceAdapter.all100Customers(sw)) {
            is Ok -> r.value
            is Failed -> fail("Demo17 adapter failed: ${r.error}")
            is Fatal -> fail("Demo17 adapter fatal: ${r.errors}")
        }

        val solver = makeSolver()
        val policy = makePolicy(sw, "demo17")
        val service = VrptwApplicationService(
            instance = instance,
            solver = solver,
            configuration = BranchAndPriceAlgorithm.Configuration(
                timeLimit = 60.toDuration(DurationUnit.SECONDS),
                nodeLimit = 50
            ),
            policy = policy
        )

        val result = when (val r = service.solve()) {
            is Ok -> r.value
            is Failed -> fail("Solve failed: ${r.error}")
            is Fatal -> fail("Solve fatal: ${r.errors}")
        }

        // 验证返回有效状态（不是 Failed）/ Verify valid status (not Failed)
        assertNotEquals(BranchAndPriceStatus.SolverStopped, result.status, "Should not be SolverStopped")

        // 如果有 incumbent，验证路线 / If incumbent exists, verify routes
        if (result.solution != null) {
            for (route in result.solution!!.routes) {
                val validation = RouteValidator.validate(
                    instance, route,
                    policy.distanceCalculator,
                    policy.travelTimeCalculator,
                    policy.arcCostCalculator,
                    policy.routeCostPolicy,
                    Flt64NetworkSchedulingSolverValueAdapter
                )
                assertTrue(validation.ok, "Route should pass validation: $validation")
            }
        }
    }

    // ========== Small Instance Oracle Comparison ==========

    @Test
    fun `small instance B&P matches full-route master MILP oracle`(): Unit = runBlocking {
        val sw = schedulingWindow()
        val instance = makeSmallInstance(sw)

        val solver = makeSolver()
        val policy = makePolicy(sw, "demo17")
        val service = VrptwApplicationService(
            instance = instance,
            solver = solver,
            configuration = BranchAndPriceAlgorithm.Configuration(
                timeLimit = 60.toDuration(DurationUnit.SECONDS)
            ),
            policy = policy
        )

        val bpResult = when (val r = service.solve()) {
            is Ok -> r.value
            is Failed -> fail("B&P solve failed: ${r.error}")
            is Fatal -> fail("B&P solve fatal: ${r.errors}")
        }

        assertEquals(BranchAndPriceStatus.Optimal, bpResult.status)
        val allRoutes = enumerateAllRoutes(instance, policy)
        val oracleModel = LinearMetaModel("small_full_route_master")
        val oracleContext = RouteCompilationContext(
            instance = instance,
            valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
        )
        assertTrue(oracleContext.register(oracleModel) is Ok)
        assertTrue(oracleContext.addColumns(UInt64.zero, allRoutes, oracleModel) is Ok)
        assertTrue(oracleContext.switchToPhaseTwo(oracleModel) is Ok)
        val oracle = when (val solve = solver.solveMILP(
            name = "small_full_route_master",
            metaModel = oracleModel
        )) {
            is Ok -> solve.value
            is Failed -> fail("Oracle solve failed: ${solve.error}")
            is Fatal -> fail("Oracle solve fatal: ${solve.errors}")
        }
        val bpSolution = bpResult.solution ?: fail("B&P should produce a solution")
        val bpCost = bpSolution.totalCost.value

        assertTrue((oracle.obj - bpCost).abs().leq(Flt64(1e-6)))

        // 验证 B&P 路线可行 / Verify B&P routes are feasible
        if (bpResult.solution != null) {
            for (route in bpResult.solution!!.routes) {
                val validation = RouteValidator.validate(
                    instance, route,
                    policy.distanceCalculator,
                    policy.travelTimeCalculator,
                    policy.arcCostCalculator,
                    policy.routeCostPolicy,
                    Flt64NetworkSchedulingSolverValueAdapter
                )
                assertTrue(validation.ok, "B&P route should pass validation: $validation")
            }

            // 验证所有客户被覆盖 / Verify all customers covered
            val servedCustomers = bpResult.solution!!.routes.flatMap { route ->
                route.stops.mapNotNull { it.customerId }
            }.toSet()
            assertEquals(
                instance.customers.map { it.id }.toSet(),
                servedCustomers,
                "All customers must be served exactly once"
            )
        }
    }

    // ========== Error Path Tests ==========

    @Test
    fun `solver failure returns Failed not normal terminal state`(): Unit = runBlocking {
        val sw = schedulingWindow()
        val instance = makeSmallInstance(sw)

        // 使用始终失败的 StubSolver / Use always-failing StubSolver
        val stubSolver = object : ColumnGenerationSolver {
            override val name = "stub"
            override suspend fun solveMILP(
                name: String, metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<SolveReport<Flt64>> {
                return networkSchedulingFailure("StubSolver MILP failure")
            }
            override suspend fun solveLP(
                name: String, metaModel: LinearMetaModel<Flt64>,
                toLogModel: Boolean,
                registrationStatusCallBack: fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack?,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<ColumnGenerationSolver.LPResult> {
                return networkSchedulingFailure("StubSolver LP failure")
            }
        }

        val policy = makePolicy(sw, "demo17")
        val service = VrptwApplicationService(
            instance = instance,
            solver = stubSolver,
            policy = policy
        )

        val result = service.solve()
        // StubSolver 在 Phase I 首次 solveLP 即失败 → Failed，不伪装为正常终态
        // StubSolver fails on Phase I's first solveLP → Failed, not disguised as normal terminal state
        assertTrue(result.failed, "solver-call failure should map to Failed, not a normal terminal state")
    }

    // ========== totalIterations Bug Fix Verification ==========

    @Test
    fun `totalIterations is positive after B&P with real LP solver`(): Unit = runBlocking {
        val sw = schedulingWindow()
        val instance = makeSmallInstance(sw)

        val solver = makeSolver()
        val policy = makePolicy(sw, "demo17")
        val service = VrptwApplicationService(
            instance = instance,
            solver = solver,
            configuration = BranchAndPriceAlgorithm.Configuration(
                timeLimit = 60.toDuration(DurationUnit.SECONDS)
            ),
            policy = policy
        )

        val result = when (val r = service.solve()) {
            is Ok -> r.value
            is Failed -> fail("Solve failed: ${r.error}")
            is Fatal -> fail("Solve fatal: ${r.errors}")
        }

        assertTrue(
            result.trace.totalIterations > 0,
            "totalIterations must be positive after B&P with real LP solver, got ${result.trace.totalIterations}"
        )
    }

    // ========== Pricing Interruption with Real LP ==========

    @Test
    fun `pricing iteration exhaustion occurs after real phase LP solves`(): Unit = runBlocking {
        val sw = schedulingWindow()
        val instance = makeSmallInstance(sw)

        val solver = LpCountingSolver(makeSolver())
        val policy = makePolicy(sw, "demo17")
        val service = VrptwApplicationService(
            instance = instance,
            solver = solver,
            configuration = BranchAndPriceAlgorithm.Configuration(
                timeLimit = 60.toDuration(DurationUnit.SECONDS),
                maxCGIterationsPerNode = 1
            ),
            policy = policy
        )

        val result = service.solve()
        assertTrue(solver.lpSolveNames.any { it.contains("_phase1_") })
        assertTrue(solver.lpSolveNames.any { it.contains("_phase2_") })
        when (result) {
            is Failed -> assertTrue(
                result.message.contains("Phase II pricing") &&
                    result.message.contains("did not converge within 1 iterations"),
                "failure must be caused by Phase II pricing iteration exhaustion: ${result.message}"
            )
            is Ok -> fail("pricing iteration exhaustion should return Failed")
            is Fatal -> fail("pricing iteration exhaustion returned Fatal: ${result.errors}")
        }
    }

    // ========== Helper Methods ==========

    private class LpCountingSolver(
        private val delegate: ColumnGenerationSolver
    ) : ColumnGenerationSolver by delegate {
        val lpSolveNames = mutableListOf<String>()

        override suspend fun solveLPWithStatus(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?,
            iisConfig: IISConfig
        ): Ret<ColumnGenerationSolver.LPResultWithStatus> {
            val result = delegate.solveLPWithStatus(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
            if (result is Ok) {
                val output = result.value
                if (output is ColumnGenerationSolver.LPResultWithStatus.Feasible &&
                    output.result.status == SolverStatus.Optimal
                ) {
                    lpSolveNames.add(name)
                }
            }
            return result
        }
    }

    private fun assertSolutionFeasible(
        instance: VrptwInstance<Flt64>,
        routes: List<Route<Flt64>>,
        policy: BranchAndPriceAlgorithm.Policy<Flt64>
    ) {
        val servedCustomers = routes.flatMap { route ->
            route.stops.mapNotNull { it.customerId }
        }
        assertEquals(instance.customers.size, servedCustomers.size)
        assertEquals(
            instance.customers.map { it.id }.toSet(),
            servedCustomers.toSet(),
            "All customers must be served exactly once"
        )
        for (route in routes) {
            val validation = RouteValidator.validate(
                instance = instance,
                route = route,
                distanceCalculator = policy.distanceCalculator,
                travelTimeCalculator = policy.travelTimeCalculator,
                arcCostCalculator = policy.arcCostCalculator,
                routeCostPolicy = policy.routeCostPolicy,
                valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
            )
            assertTrue(validation.ok, "Route ${route.vehicleTypeId} should pass validation: $validation")
        }
    }

    private fun makeSmallInstance(sw: SchedulingTimeWindow<Flt64>): VrptwInstance<Flt64> {
        val units = VrptwUnits(distanceUnit = Meter, loadUnit = Kilogram, costUnit = NoneUnit).value
            ?: fail("units creation failed")

        fun node(id: String, x: Flt64) = NetworkNode(
            id = NetworkNodeId(id),
            attributes = mapOf("x" to Quantity(x, Meter), "y" to Quantity(Flt64.zero, Meter))
        ).value ?: fail("node creation failed")

        fun window(ready: Flt64, due: Flt64) = ServiceTimeWindow(
            readyTime = sw.instantOf(ready),
            dueTime = sw.instantOf(due)
        ).value ?: fail("window creation failed")

        return VrptwInstance(
            name = "small-e2e",
            startDepot = Depot(node("start", Flt64.zero), window(Flt64.zero, Flt64(100.0))),
            endDepot = Depot(node("end", Flt64(4.0)), window(Flt64.zero, Flt64(100.0))),
            customers = listOf(
                Customer(
                    id = CustomerId("c1"), node = node("c1", Flt64.one),
                    demand = Quantity(Flt64(1.0), Kilogram),
                    timeWindow = window(Flt64.zero, Flt64(50.0)),
                    serviceTime = Duration.ZERO
                ).value ?: fail("customer c1 failed"),
                Customer(
                    id = CustomerId("c2"), node = node("c2", Flt64(2.0)),
                    demand = Quantity(Flt64(1.0), Kilogram),
                    timeWindow = window(Flt64.zero, Flt64(50.0)),
                    serviceTime = Duration.ZERO
                ).value ?: fail("customer c2 failed")
            ),
            vehicleTypes = listOf(
                VehicleType(
                    id = VehicleTypeId("v1"),
                    capacity = Quantity(Flt64(5.0), Kilogram),
                    fixedCost = Quantity(Flt64(10.0), NoneUnit),
                    amount = 2
                ).value ?: fail("vehicle type failed")
            ),
            units = units,
            schedulingWindow = sw,
            tolerances = VrptwTolerances.default
        ).value ?: fail("instance creation failed")
    }

    private fun enumerateAllRoutes(
        instance: VrptwInstance<Flt64>,
        policy: BranchAndPriceAlgorithm.Policy<Flt64>
    ): List<Route<Flt64>> {
        val customers = instance.customers
        val sequences = listOf(
            listOf(customers[0]),
            listOf(customers[1]),
            listOf(customers[0], customers[1]),
            listOf(customers[1], customers[0])
        )
        return sequences.map { sequence ->
            buildRoute(instance, sequence, policy)
        }
    }

    private fun buildRoute(
        instance: VrptwInstance<Flt64>,
        customers: List<Customer<Flt64>>,
        policy: BranchAndPriceAlgorithm.Policy<Flt64>
    ): Route<Flt64> {
        val vehicleType = instance.vehicleTypes.single()
        val nodes = listOf(instance.startDepot.node) + customers.map { it.node } + instance.endDepot.node
        val arcCosts = mutableListOf<Quantity<Flt64>>()
        var totalDistance = Flt64.zero
        var currentTime = instance.schedulingWindow.valueOf(instance.startDepot.timeWindow.readyTime)
        var currentLoad = Flt64.zero
        val stops = mutableListOf(RouteStop(
            nodeId = instance.startDepot.node.id,
            customerId = null,
            arrival = instance.schedulingWindow.instantOf(currentTime),
            serviceStart = instance.schedulingWindow.instantOf(currentTime),
            departure = instance.schedulingWindow.instantOf(currentTime),
            accumulatedLoad = Quantity(currentLoad, instance.units.loadUnit)
        ))

        for (index in 0 until nodes.lastIndex) {
            val from = nodes[index]
            val to = nodes[index + 1]
            val distance = policy.distanceCalculator.distance(from, to).value
                ?: fail("distance calculation failed")
            val travelTime = policy.travelTimeCalculator.travelTime(from, to, vehicleType).value
                ?: fail("travel-time calculation failed")
            val arcCost = policy.arcCostCalculator.cost(
                from = from,
                to = to,
                distance = distance,
                travelTime = travelTime,
                vehicleType = vehicleType
            ).value ?: fail("arc-cost calculation failed")
            arcCosts.add(arcCost)
            totalDistance += Flt64NetworkSchedulingSolverValueAdapter.normalize(
                quantity = distance,
                targetUnit = instance.units.distanceUnit
            ).value ?: fail("distance normalization failed")
            val arrival = currentTime + instance.schedulingWindow.valueOf(travelTime)
            val customer = customers.getOrNull(index)
            val readyTime = customer?.timeWindow?.readyTime ?: instance.endDepot.timeWindow.readyTime
            val serviceStart = maxOf(arrival, instance.schedulingWindow.valueOf(readyTime))
            if (customer != null) {
                currentLoad += customer.demand.value
            }
            val departure = serviceStart + if (customer != null) {
                instance.schedulingWindow.valueOf(customer.serviceTime)
            } else {
                Flt64.zero
            }
            stops.add(RouteStop(
                nodeId = to.id,
                customerId = customer?.id,
                arrival = instance.schedulingWindow.instantOf(arrival),
                serviceStart = instance.schedulingWindow.instantOf(serviceStart),
                departure = instance.schedulingWindow.instantOf(departure),
                accumulatedLoad = Quantity(currentLoad, instance.units.loadUnit)
            ))
            currentTime = departure
        }
        val route = Route(
            vehicleTypeId = vehicleType.id,
            stops = stops,
            distance = Quantity(totalDistance, instance.units.distanceUnit),
            cost = policy.routeCostPolicy.cost(vehicleType, arcCosts).value
                ?: fail("route-cost calculation failed")
        ).value ?: fail("route creation failed")
        assertTrue(RouteValidator.validate(
            instance = instance,
            route = route,
            distanceCalculator = policy.distanceCalculator,
            travelTimeCalculator = policy.travelTimeCalculator,
            arcCostCalculator = policy.arcCostCalculator,
            routeCostPolicy = policy.routeCostPolicy,
            valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
        ) is Ok)
        return route
    }
}
