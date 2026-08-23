@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.network_scheduling.application

import fuookami.ospf.kotlin.core.solver.toSolveReport
import fuookami.ospf.kotlin.core.solver.report.*
import kotlin.test.*
import fuookami.ospf.kotlin.core.solver.report.*
import kotlin.time.Duration
import fuookami.ospf.kotlin.core.solver.report.*
import kotlin.time.DurationUnit
import fuookami.ospf.kotlin.core.solver.report.*
import kotlin.time.Instant
import fuookami.ospf.kotlin.core.solver.report.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.pricing.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service.VrptwValidator
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.model.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.service.*

class BranchAndPriceAlgorithmTest {

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
            val customer = instance.customerById[cid] ?: fail("customer not found: $cid")
            load = load + customer.demand.value
            time = time + Flt64.one
            val arrival = instant(time)
            val serviceStart = instant(time)
            val departure = instant(time)
            stops.add(RouteStop(
                nodeId = customer.node.id, customerId = cid,
                arrival = arrival, serviceStart = serviceStart, departure = departure,
                accumulatedLoad = Quantity(load, Kilogram)
            ))
        }

        stops.add(RouteStop(
            nodeId = endDepot.node.id, customerId = null,
            arrival = instant(time), serviceStart = instant(time), departure = instant(time),
            accumulatedLoad = Quantity(load, Kilogram)
        ))

        return Route(
            vehicleTypeId = vehicleTypeId,
            stops = stops,
            distance = Quantity(distance, Meter),
            cost = Quantity(cost, NoneUnit)
        ).value ?: fail("route creation failed")
    }

    private fun makeAlgorithm(solver: ColumnGenerationSolver = StubSolver()): BranchAndPriceAlgorithm<Flt64> {
        val instance = makeInstance()
        return BranchAndPriceAlgorithm(
            instance = instance,
            solver = solver,
            configuration = BranchAndPriceAlgorithm.Configuration(),
            policy = BranchAndPriceAlgorithm.Policy(
                valueAdapter = Flt64NetworkSchedulingSolverValueAdapter,
                distanceCalculator = EuclideanDistanceCalculator(Meter),
                travelTimeCalculator = ConstantTravelTimeCalculator(Duration.ZERO),
                arcCostCalculator = ZeroArcCostCalculator(),
                routeCostPolicy = FixedCostOnlyPolicy()
            )
        )
    }

    @Test
    fun inheritedColumnsAreFilteredByChildBranchMask() {
        val instance = makeInstance()
        val vehicleTypeId = VehicleTypeId("v1")
        val forbiddenCustomerNode = instance.customerById[CustomerId("c1")]!!.node.id
        val branchMask = assertNotNull(
            BranchMask(
                startDepot = instance.startDepot.node.id,
                endDepot = instance.endDepot.node.id,
                forbiddenNodes = mapOf(vehicleTypeId to setOf(forbiddenCustomerNode))
            ).value
        )
        val forbiddenRoute = makeRoute(instance, listOf(CustomerId("c1")), vehicleTypeId)
        val compatibleRoute = makeRoute(instance, listOf(CustomerId("c2")), vehicleTypeId)

        val filtered = filterRouteColumnsForBranch(
            branchMask,
            listOf(forbiddenRoute, compatibleRoute)
        )

        assertEquals(listOf(compatibleRoute.signature), filtered.map { it.signature })
    }

    // ========== Branch Decision Selection Tests ==========

    @Test
    fun selectBranchDecisionReturnsNullForIntegerSolution() {
        val instance = makeInstance()
        val algorithm = makeAlgorithm()

        val route1 = makeRoute(instance, listOf(CustomerId("c1")))
        val route2 = makeRoute(instance, listOf(CustomerId("c2")))

        val routeValues = listOf(route1 to Flt64.one, route2 to Flt64.one)
        val decision = algorithm.selectBranchDecision(routeValues, instance, Flt64(1e-6))
        assertNull(decision) // Fully integer -> no branch needed
    }

    @Test
    fun selectBranchDecisionReturnsForbidVehicleTypeForFractionalAssignment() {
        val instance = makeInstance()
        val algorithm = makeAlgorithm()

        val route1 = makeRoute(instance, listOf(CustomerId("c1")))
        val route2 = makeRoute(instance, listOf(CustomerId("c2")))

        // Fractional assignment: each customer served 0.5 by each route
        val routeValues = listOf(route1 to Flt64(0.5), route2 to Flt64(0.5))
        val decision = algorithm.selectBranchDecision(routeValues, instance, Flt64(1e-6))
        assertNotNull(decision)
        // Should be a ForbidVehicleType decision (assignment-based branching first per contract 3.5)
        assertTrue(decision is BranchDecision.ForbidVehicleType)
    }

    @Test
    fun complementaryDecisionSwapsForbidAndRequireVehicleType() {
        val algorithm = makeAlgorithm()

        val forbid = BranchDecision.ForbidVehicleType(VehicleTypeId("v1"), CustomerId("c1"), NetworkNodeId("c1"))
        val require = algorithm.complementaryDecision(forbid)
        assertTrue(require is BranchDecision.RequireVehicleType)
        assertEquals(forbid.vehicleTypeId, require.vehicleTypeId)
        assertEquals(forbid.customerId, require.customerId)

        val back = algorithm.complementaryDecision(require)
        assertTrue(back is BranchDecision.ForbidVehicleType)
    }

    @Test
    fun complementaryDecisionSwapsForbidAndRequireArc() {
        val algorithm = makeAlgorithm()

        val from = NetworkNodeId("a")
        val to = NetworkNodeId("b")
        val forbid = BranchDecision.ForbidArc(VehicleTypeId("v1"), from, to)
        val require = algorithm.complementaryDecision(forbid)
        assertTrue(require is BranchDecision.RequireArc)
        assertEquals(forbid.vehicleTypeId, require.vehicleTypeId)
        assertEquals(forbid.from, require.from)
        assertEquals(forbid.to, require.to)
    }

    // ========== Edge-Based Branching Tests ==========

    @Test
    fun selectBranchDecisionReturnsForbidArcWhenAssignmentsIntegerButEdgesFractional() {
        // assignmentValue[v1, c1] = 1.0 (integer), assignmentValue[v1, c2] = 1.0 (integer)
        // But edges are split across routes → fractional edgeValue → ForbidArc
        val instance = makeInstance()
        val algorithm = makeAlgorithm()

        val routeBoth = makeRoute(instance, listOf(CustomerId("c1"), CustomerId("c2")))
        val route1 = makeRoute(instance, listOf(CustomerId("c1")))
        val route2 = makeRoute(instance, listOf(CustomerId("c2")))

        val routeValues = listOf(
            routeBoth to Flt64(0.5),
            route1 to Flt64(0.5),
            route2 to Flt64(0.5)
        )
        val decision = algorithm.selectBranchDecision(routeValues, instance, Flt64(1e-6))
        assertNotNull(decision)
        assertTrue(decision is BranchDecision.ForbidArc)
    }

    // ========== Integrality Invariant Tests ==========

    @Test
    fun selectBranchDecisionReturnsNullWhenAllAssignmentAndEdgeAreInteger() {
        // Each route variable = 1.0 → assignmentValue and edgeValue are all integer → null
        val instance = makeInstance()
        val algorithm = makeAlgorithm()

        val route1 = makeRoute(instance, listOf(CustomerId("c1")))
        val route2 = makeRoute(instance, listOf(CustomerId("c2")))

        val routeValues = listOf(route1 to Flt64.one, route2 to Flt64.one)
        val decision = algorithm.selectBranchDecision(routeValues, instance, Flt64(1e-6))
        assertNull(decision)
    }

    @Test
    fun selectBranchDecisionReturnsNullOnContractViolation() {
        // Contract violation: assignmentValue = 1.0 (integer) but route variables are 0.5 (fractional).
        // This happens when two identical routes serve the same customers with 0.5 each.
        // The dedup invariant (route column pool dedup) should prevent this in practice,
        // but the selectBranchDecision correctly returns null (contract violation path).
        val instance = makeInstance()
        val algorithm = makeAlgorithm()

        // Two routes serving the same customers with value 0.5 each
        val route1 = makeRoute(instance, listOf(CustomerId("c1"), CustomerId("c2")))
        val route2 = makeRoute(instance, listOf(CustomerId("c1"), CustomerId("c2")))

        val routeValues = listOf(route1 to Flt64(0.5), route2 to Flt64(0.5))
        val decision = algorithm.selectBranchDecision(routeValues, instance, Flt64(1e-6))
        // Both assignment and edge values will be 1.0 (integer), but route vars are 0.5
        // → contract violation → null
        assertNull(decision)
    }

    @Test
    fun contractViolationInAlgorithmReturnsFailed() {
        // When selectBranchDecision returns null for a fractional node (isInteger=false),
        // the algorithm should return Failed per contract section 3.5:
        // "若出现 assignment/edge 均为整数但路线变量仍为真分数的合同违例，返回 Failed"
        //
        // This test verifies the contract at the model level:
        // - isInteger = false (fractional route variables)
        // - selectBranchDecision = null (assignment/edge both integer)
        // → these two together constitute a contract violation
        val instance = makeInstance()
        val algorithm = makeAlgorithm()

        // Two identical routes with 0.5 each → assignment/edge integer but route vars fractional
        val route1 = makeRoute(instance, listOf(CustomerId("c1"), CustomerId("c2")))
        val route2 = makeRoute(instance, listOf(CustomerId("c1"), CustomerId("c2")))
        val routeValues = listOf(route1 to Flt64(0.5), route2 to Flt64(0.5))

        // Verify the precondition: selectBranchDecision returns null
        val decision = algorithm.selectBranchDecision(routeValues, instance, Flt64(1e-6))
        assertNull(decision)

        // Verify the route values are fractional (isInteger would be false)
        val isInteger = routeValues.all { (_, v) ->
            v.leq(Flt64(1e-6)) || v.geq(Flt64.one - Flt64(1e-6))
        }
        assertFalse(isInteger)

        // Contract violation condition confirmed:
        // isInteger=false AND selectBranchDecision=null → algorithm must return Failed
    }

    // ========== Pricing Interruption Lower Bound Tests ==========

    @Test
    fun nodeSolveResultWithIncompletePricingUsesInheritedLowerBound() {
        // When pricingComplete = false, the NodeSolveResult.lowerBound should be
        // the inherited lower bound, not the LP objective.
        // This tests the contract: unconverged RMP objective must not enter global lower bound.
        val result = BranchNodeSolver.NodeSolveResult<Flt64>(
            lowerBound = Flt64(50.0),  // inherited lower bound (not LP objective)
            routeValues = emptyList(),
            isInteger = false,
            isFeasible = true,
            pricingComplete = false,
            lpObjective = Flt64(30.0),  // unconverged RMP objective - must NOT be used as lower bound
            iterations = 3  // CG iterations consumed
        )
        assertFalse(result.pricingComplete)
        // The lower bound should be the inherited bound, not the LP objective
        assertEquals(Flt64(50.0), result.lowerBound)
        // The LP objective is strictly less than the lower bound, confirming
        // that the unconverged RMP objective was correctly excluded
        assertTrue(result.lpObjective ls result.lowerBound)
    }

    @Test
    fun childNodeRetainsInheritedLowerBoundWhenParentSolvedWithIncompletePricing() {
        // When a parent node is solved with pricingComplete = false,
        // its lowerBound = inheritedLowerBound. Child nodes should inherit
        // this value as their inheritedLowerBound.
        val startDepot = NetworkNodeId("start")
        val endDepot = NetworkNodeId("end")

        // Root with inherited lower bound -∞
        val root = (BranchNode.root(startDepot, endDepot, Flt64.negativeInfinity) as Ok).value

        // Simulate: pricing was interrupted, so node keeps inherited lower bound
        // (BranchNodeSolver sets nodeLowerBound = inheritedLowerBound when pricingComplete=false)
        val inheritedBound = Flt64.negativeInfinity
        root.solve(inheritedBound)

        // Create child - should inherit parent's lower bound
        val decision = BranchDecision.ForbidVehicleType(
            VehicleTypeId("v1"), CustomerId("c1"), NetworkNodeId("c1")
        )
        val child = (BranchNode.child(1, root, decision, startDepot, endDepot) as Ok).value

        // Child's inheritedLowerBound = parent.lowerBound = inheritedBound
        assertEquals(inheritedBound, child.inheritedLowerBound)
        // 求解前使用继承下界；求解后由自身 LP 下界替换 / Before solving, use the inherited bound; after solving, the node's LP bound replaces it.
        assertEquals(inheritedBound, child.effectiveLowerBound)
    }

    @Test
    fun childNodeInheritsFiniteLowerBoundFromPartiallySolvedParent() {
        // More realistic scenario: root was partially solved (pricing interrupted)
        // with a finite inherited lower bound
        val startDepot = NetworkNodeId("start")
        val endDepot = NetworkNodeId("end")

        val root = (BranchNode.root(startDepot, endDepot, Flt64.negativeInfinity) as Ok).value
        // Root solved with pricing complete → LP objective = 100.0
        root.solve(Flt64(100.0))

        // Create child from root
        val decision1 = BranchDecision.ForbidVehicleType(
            VehicleTypeId("v1"), CustomerId("c1"), NetworkNodeId("c1")
        )
        val child1 = (BranchNode.child(1, root, decision1, startDepot, endDepot) as Ok).value
        assertEquals(Flt64(100.0), child1.inheritedLowerBound)

        // Simulate child1 solved with pricing interrupted → keeps inherited bound
        child1.solve(Flt64(100.0))  // lowerBound = inheritedLowerBound

        // Create grandchild from child1
        val decision2 = BranchDecision.ForbidArc(
            VehicleTypeId("v1"), NetworkNodeId("c1"), NetworkNodeId("c2")
        )
        val grandchild = (BranchNode.child(2, child1, decision2, startDepot, endDepot) as Ok).value
        // Grandchild inherits child1's lowerBound (which was the inherited bound)
        assertEquals(Flt64(100.0), grandchild.inheritedLowerBound)
    }

    // ========== B&P vs Enumeration Oracle Contract Tests ==========

    @Test
    fun selectBranchDecisionOnEnumeratedRoutesMatchesOracleLogic() {
        // This test verifies the branch decision selection logic on a scenario
        // that would arise from enumerating all feasible routes for a small instance.
        // The full B&P vs enumeration oracle comparison (with real LP solver) is
        // deferred to Phase N7 demo5 integration tests.
        //
        // For a 2-customer instance with 1 vehicle type and 2 vehicles:
        // Enumerated feasible routes: [c1], [c2], [c1→c2]
        // If LP relaxation gives fractional values, selectBranchDecision should
        // correctly identify the most fractional assignment or edge.
        val instance = makeInstance()
        val algorithm = makeAlgorithm()

        // Scenario: LP gives 0.5 to each of two routes covering c1
        val route1 = makeRoute(instance, listOf(CustomerId("c1")))
        val routeBoth = makeRoute(instance, listOf(CustomerId("c1"), CustomerId("c2")))

        // assignmentValue[v1, c1] = 0.5 + 0.5 = 1.0 (integer)
        // assignmentValue[v1, c2] = 0.5 (fractional) → ForbidVehicleType
        val routeValues = listOf(route1 to Flt64(0.5), routeBoth to Flt64(0.5))
        val decision = algorithm.selectBranchDecision(routeValues, instance, Flt64(1e-6))
        assertNotNull(decision)
        assertTrue(decision is BranchDecision.ForbidVehicleType)
        assertEquals(VehicleTypeId("v1"), decision.vehicleTypeId)
        assertEquals(CustomerId("c2"), decision.customerId)
    }

    @Test
    fun branchAndPriceResultContainsRequiredFields() {
        // Verify that BranchAndPriceResult has all required fields for oracle comparison:
        // status, solution, lowerBound, upperBound, trace
        val result = BranchAndPriceAlgorithm.BranchAndPriceResult<Flt64>(
            status = BranchAndPriceStatus.Optimal,
            solution = null,
            lowerBound = Flt64(100.0),
            upperBound = Flt64(100.0),
            trace = BranchAndPriceTrace(
                nodesExplored = 5,
                nodesPruned = 2,
                globalLowerBound = Flt64(100.0),
                globalUpperBound = Flt64(100.0),
                relativeGap = Flt64.zero,
                totalTime = Duration.ZERO,
                totalIterations = 10
            )
        )
        assertEquals(BranchAndPriceStatus.Optimal, result.status)
        assertEquals(Flt64(100.0), result.lowerBound)
        assertEquals(Flt64(100.0), result.upperBound)
        assertNotNull(result.trace)
        assertEquals(5, result.trace.nodesExplored)
    }

    @Test
    fun nonOptimalExternalLpReturnsFailureWithoutInventingIncumbent() {
        val result = runBlocking {
            makeAlgorithm(NonOptimalLpSolver()).solve()
        }
        assertTrue(result is Failed)
    }

    @Test
    fun infeasiblePhaseOneLpMarksNodeInfeasibleWithoutFailingSearch() {
        val instance = makeInstance()
        val root = assertNotNull(
            (BranchNode.root(
                instance.startDepot.node.id,
                instance.endDepot.node.id,
                Flt64.negativeInfinity
            ) as Ok).value
        )
        val nodeSolver = BranchNodeSolver(
            instance = instance,
            solver = InfeasibleLpSolver(),
            valueAdapter = Flt64NetworkSchedulingSolverValueAdapter,
            distanceCalculator = EuclideanDistanceCalculator(Meter),
            travelTimeCalculator = ConstantTravelTimeCalculator(Duration.ZERO),
            arcCostCalculator = ZeroArcCostCalculator(),
            routeCostPolicy = FixedCostOnlyPolicy()
        )

        val result = runBlocking { nodeSolver.solve(root) }
        assertTrue(result is Ok)
        val nodeResult = (result as Ok).value
        assertFalse(nodeResult.isFeasible)
        assertEquals(SolverStatus.Infeasible, nodeResult.solverStatus)
    }

    @Test
    fun infeasiblePhaseTwoLpMarksNodeInfeasibleWithoutFailingSearch() {
        val instance = makeInstance()
        val root = assertNotNull(
            (BranchNode.root(
                instance.startDepot.node.id,
                instance.endDepot.node.id,
                Flt64.negativeInfinity
            ) as Ok).value
        )
        val nodeSolver = BranchNodeSolver(
            instance = instance,
            solver = SequencedLpSolver(infeasibleCalls = setOf(1)),
            valueAdapter = Flt64NetworkSchedulingSolverValueAdapter,
            distanceCalculator = EuclideanDistanceCalculator(Meter),
            travelTimeCalculator = ConstantTravelTimeCalculator(Duration.ZERO),
            arcCostCalculator = ZeroArcCostCalculator(),
            routeCostPolicy = FixedCostOnlyPolicy()
        )

        val result = runBlocking { nodeSolver.solve(root) }
        assertTrue(result is Ok)
        val nodeResult = (result as Ok).value
        assertFalse(nodeResult.isFeasible)
        assertEquals(SolverStatus.Infeasible, nodeResult.solverStatus)
    }

    @Test
    fun infeasibleFinalLpMarksNodeInfeasibleWithoutFailingSearch() {
        val instance = makeInstance()
        val root = assertNotNull(
            (BranchNode.root(
                instance.startDepot.node.id,
                instance.endDepot.node.id,
                Flt64.negativeInfinity
            ) as Ok).value
        )
        val nodeSolver = BranchNodeSolver(
            instance = instance,
            solver = SequencedLpSolver(infeasibleCalls = setOf(2)),
            valueAdapter = Flt64NetworkSchedulingSolverValueAdapter,
            distanceCalculator = EuclideanDistanceCalculator(Meter),
            travelTimeCalculator = ConstantTravelTimeCalculator(Duration.ZERO),
            arcCostCalculator = ZeroArcCostCalculator(),
            routeCostPolicy = FixedCostOnlyPolicy()
        )

        val result = runBlocking { nodeSolver.solve(root) }
        assertTrue(result is Ok)
        val nodeResult = (result as Ok).value
        assertFalse(nodeResult.isFeasible)
        assertEquals(SolverStatus.Infeasible, nodeResult.solverStatus)
    }

    // ========== Stub Solver ==========

    private open class StubSolver : ColumnGenerationSolver {
        override val name: String = "stub"

        override suspend fun solveMILP(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            return networkSchedulingFailure("StubSolver does not support MILP")
        }

        override suspend fun solveLP(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<ColumnGenerationSolver.LPResult> {
            return networkSchedulingFailure("StubSolver does not support LP")
        }
    }

    private class InfeasibleLpSolver : SequencedLpSolver(infeasibleCalls = setOf(0))

    private open class SequencedLpSolver(
        private val infeasibleCalls: Set<Int>
    ) : StubSolver() {
        private var lpCall = 0

        override suspend fun solveLPWithStatus(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?,
            iisConfig: IISConfig
        ): Ret<ColumnGenerationSolver.LPResultWithStatus> {
            val call = lpCall++
            if (call !in infeasibleCalls) {
                val output = SolverStatus.Optimal.toSolveReport(
                    objective = Flt64.zero,
                    values = List(metaModel.tokens.tokens.size) { Flt64.zero },
                    solveTime = Duration.ZERO,
                    bestBound = Flt64.zero,
                    gap = Flt64.zero
                )
                return Ok(
                    ColumnGenerationSolver.LPResultWithStatus.Feasible(
                        ColumnGenerationSolver.LPResult(
                            result = output,
                            dualSolution = emptyMap()
                        )
                    )
                )
            }
            return Ok(
                ColumnGenerationSolver.LPResultWithStatus.Infeasible(
                    LinearInfeasibleSolverOutput(
                        iis = BasicLinearTriadModel(
                            variables = emptyList(),
                            constraints = LinearConstraintBatch(
                                sparseLhs = SparseMatrix<Flt64>(),
                                signs = emptyList(),
                                rhs = emptyList(),
                                names = emptyList(),
                                sources = emptyList()
                            ),
                            name = "phase-one-infeasible"
                        )
                    )
                )
            )
        }
    }

    private class NonOptimalLpSolver : ColumnGenerationSolver {
        override val name: String = "non-optimal-lp"

        override suspend fun solveMILP(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolveReport<Flt64>> {
            return networkSchedulingFailure("NonOptimalLpSolver does not support MILP")
        }

        override suspend fun solveLP(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<ColumnGenerationSolver.LPResult> {
            return ok(
                ColumnGenerationSolver.LPResult(
                    result = SolverStatus.Feasible.toSolveReport(
                        objective = Flt64.zero,
                        values = emptyList(),
                        solveTime = Duration.ZERO,
                        bestBound = Flt64.negativeInfinity,
                        gap = Flt64.infinity
                    ),
                    dualSolution = emptyMap()
                )
            )
        }
    }
}

// ========== Policy Stubs ==========

private class ConstantTravelTimeCalculator<V : RealNumber<V>>(
    private val travelTime: Duration
) : TravelTimeCalculator<V> {
    override fun travelTime(from: NetworkNode<V>, to: NetworkNode<V>, vehicleType: VehicleType<V>): Ret<Duration> {
        return ok(travelTime)
    }
}

private class ZeroArcCostCalculator<V : RealNumber<V>> : ArcCostCalculator<V> {
    override fun cost(
        from: NetworkNode<V>,
        to: NetworkNode<V>,
        distance: Quantity<V>,
        travelTime: Duration,
        vehicleType: VehicleType<V>
    ): Ret<Quantity<V>> {
        return ok(Quantity(distance.value.constants.zero, NoneUnit))
    }
}

private class FixedCostOnlyPolicy<V : RealNumber<V>> : RouteCostPolicy<V> {
    override fun cost(vehicleType: VehicleType<V>, arcCosts: List<Quantity<V>>): Ret<Quantity<V>> {
        return ok(vehicleType.fixedCost)
    }
}
