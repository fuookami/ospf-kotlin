@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.network_scheduling.application

import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.service.*

class VrptwApplicationServiceTest {

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

    private fun window(ready: Flt64, due: Flt64): ServiceTimeWindow = assertNotNull(
        ServiceTimeWindow(readyTime = schedulingWindow.instantOf(ready), dueTime = schedulingWindow.instantOf(due)).value
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

    private fun makePolicy() = BranchAndPriceAlgorithm.Policy(
        valueAdapter = Flt64NetworkSchedulingSolverValueAdapter,
        distanceCalculator = object : DistanceCalculator<Flt64> {
            override fun distance(from: NetworkNode<Flt64>, to: NetworkNode<Flt64>): Ret<Quantity<Flt64>> {
                val dx = to.attributes["x"]!!.value - from.attributes["x"]!!.value
                val dy = to.attributes["y"]!!.value - from.attributes["y"]!!.value
                return ok(Quantity((dx * dx + dy * dy).sqrt(), Meter))
            }
        },
        travelTimeCalculator = object : TravelTimeCalculator<Flt64> {
            override fun travelTime(from: NetworkNode<Flt64>, to: NetworkNode<Flt64>, vehicleType: VehicleType<Flt64>): Ret<Duration> {
                return ok(Duration.ZERO)
            }
        },
        arcCostCalculator = object : ArcCostCalculator<Flt64> {
            override fun cost(
                from: NetworkNode<Flt64>,
                to: NetworkNode<Flt64>,
                distance: Quantity<Flt64>,
                travelTime: Duration,
                vehicleType: VehicleType<Flt64>
            ): Ret<Quantity<Flt64>> {
                return ok(Quantity(Flt64.zero, NoneUnit))
            }
        },
        routeCostPolicy = object : RouteCostPolicy<Flt64> {
            override fun cost(vehicleType: VehicleType<Flt64>, arcCosts: List<Quantity<Flt64>>): Ret<Quantity<Flt64>> {
                return ok(vehicleType.fixedCost)
            }
        }
    )

    // ========== VrptwSolveResult Tests ==========

    @Test
    fun vrptwSolveResultContainsAllRequiredFields() {
        val result = VrptwSolveResult<Flt64>(
            status = BranchAndPriceStatus.Optimal,
            solution = null,
            lowerBound = Flt64(100.0),
            upperBound = Flt64(100.0),
            relativeGap = Flt64.zero,
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
        assertNull(result.solution)
        assertEquals(Flt64(100.0), result.lowerBound)
        assertEquals(Flt64(100.0), result.upperBound)
        assertEquals(Flt64.zero, result.relativeGap)
        assertNotNull(result.trace)
    }

    // ========== BranchAndPriceStatus 6-Value Tests ==========

    @Test
    fun branchAndPriceStatusHasAllSixStates() {
        val all = listOf(
            BranchAndPriceStatus.Optimal,
            BranchAndPriceStatus.Feasible,
            BranchAndPriceStatus.Infeasible,
            BranchAndPriceStatus.TimeLimit,
            BranchAndPriceStatus.NodeLimit,
            BranchAndPriceStatus.SolverStopped
        )
        assertEquals(6, all.distinct().size)
    }

    // ========== SolutionEnricher Tests ==========

    @Test
    fun solutionEnricherFakeCanModifyResultWithoutChangingMainFlow() {
        val original = VrptwSolveResult<Flt64>(
            status = BranchAndPriceStatus.Feasible,
            solution = null,
            lowerBound = Flt64(50.0),
            upperBound = Flt64(80.0),
            relativeGap = Flt64(0.6),
            trace = BranchAndPriceTrace(
                nodesExplored = 3, nodesPruned = 0,
                globalLowerBound = Flt64(50.0), globalUpperBound = Flt64(80.0),
                relativeGap = Flt64(0.6), totalTime = Duration.ZERO, totalIterations = 5
            )
        )

        var enrichCalled = false
        val fakeEnricher = SolutionEnricher<Flt64> { result ->
            enrichCalled = true
            result
        }

        val enriched = fakeEnricher.enrich(original)
        assertTrue(enrichCalled)
        assertEquals(original.status, enriched.status)
        assertEquals(original.lowerBound, enriched.lowerBound)
    }

    // ========== Status Mapping Tests (through solve) ==========

    // 时间上限即使无 incumbent 也映射为 TimeLimit（而非 SolverStopped），是可解释的正常终态。
    // Time limit maps to TimeLimit even without an incumbent (not SolverStopped): an explainable normal terminal state.
    @Test
    fun timeLimitWithoutIncumbentMapsToTimeLimit() = kotlinx.coroutines.runBlocking {
        val service = VrptwApplicationService(
            instance = makeInstance(),
            solver = StubSolver(),
            configuration = BranchAndPriceAlgorithm.Configuration(timeLimit = Duration.ZERO),
            policy = makePolicy()
        )
        val result = service.solve()
        assertTrue(result.ok, "time-limited solve should return Ok, not Failed")
        val solveResult = assertNotNull(result.value)
        assertEquals(BranchAndPriceStatus.TimeLimit, solveResult.status)
        assertNull(solveResult.solution)
    }

    // 节点上限即使无 incumbent 也映射为 NodeLimit（而非 SolverStopped）。
    // Node limit maps to NodeLimit even without an incumbent (not SolverStopped).
    @Test
    fun nodeLimitWithoutIncumbentMapsToNodeLimit() = kotlinx.coroutines.runBlocking {
        val service = VrptwApplicationService(
            instance = makeInstance(),
            solver = StubSolver(),
            configuration = BranchAndPriceAlgorithm.Configuration(nodeLimit = 0),
            policy = makePolicy()
        )
        val result = service.solve()
        assertTrue(result.ok, "node-limited solve should return Ok, not Failed")
        val solveResult = assertNotNull(result.value)
        assertEquals(BranchAndPriceStatus.NodeLimit, solveResult.status)
        assertNull(solveResult.solution)
    }

    // ========== Extension Through Main Flow Tests ==========

    // enricher 在 solve() 主流程末尾被调用，其返回值透传到最终结果，无需修改主流程。
    // The enricher is invoked at the end of solve() and its returned value flows through, without modifying the main flow.
    @Test
    fun solutionEnricherRunsThroughSolve() = kotlinx.coroutines.runBlocking {
        var enrichCalled = false
        val markerGap = Flt64(-999.0)
        val enricher = SolutionEnricher<Flt64> { result ->
            enrichCalled = true
            VrptwSolveResult(
                status = result.status,
                solution = result.solution,
                lowerBound = result.lowerBound,
                upperBound = result.upperBound,
                relativeGap = markerGap,
                trace = result.trace
            )
        }
        val service = VrptwApplicationService(
            instance = makeInstance(),
            solver = StubSolver(),
            // timeLimit = ZERO 让算法在不调用 solver 的情况下正常返回 Ok(TimeLimit)，从而抵达 enricher。
            // timeLimit = ZERO makes the algorithm return Ok(TimeLimit) without calling the solver, reaching the enricher.
            configuration = BranchAndPriceAlgorithm.Configuration(timeLimit = Duration.ZERO),
            policy = makePolicy(),
            solutionEnrichers = listOf(enricher)
        )
        val result = service.solve()
        assertTrue(result.ok)
        val solveResult = assertNotNull(result.value)
        assertTrue(enrichCalled, "enricher should be invoked inside solve()")
        assertEquals(markerGap, solveResult.relativeGap, "enricher's returned value should flow through")
    }

    // ========== VrptwApplicationService Construction Tests ==========

    @Test
    fun vrptwApplicationServiceCanBeConstructed() {
        val instance = makeInstance()
        val service = VrptwApplicationService(
            instance = instance,
            solver = StubSolver(),
            policy = makePolicy()
        )
        assertNotNull(service)
    }

    @Test
    fun vrptwApplicationServiceMapsAlgorithmFailureToFailed() = kotlinx.coroutines.runBlocking {
        val instance = makeInstance()
        val service = VrptwApplicationService(
            instance = instance,
            solver = StubSolver(),
            configuration = BranchAndPriceAlgorithm.Configuration(),
            policy = makePolicy()
        )
        // StubSolver 在 Phase I 首次 solveLP 即失败，Failed 一路上抛，不伪装为正常终态。
        // StubSolver fails on Phase I's first solveLP; the Failure propagates and is not disguised as a normal terminal state.
        val result = service.solve()
        assertTrue(result.failed, "solver-call failure should map to Failed, not a normal terminal state")
    }

    // ========== Stub Solver ==========

    private class StubSolver : ColumnGenerationSolver {
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
}
