@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_selection.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_selection.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 使用实时 Gurobi 求解器的 demo4 分支定价端到端测试。
 * End-to-end test for demo4 Branch-and-Price with live Gurobi solver.
 *
 * 验证 B&P 算法可以求解最小场景，并返回覆盖全部航班任务的可行解。
 * Verifies that the B&P algorithm solves a minimal scenario and returns a feasible solution covering all flight tasks.
 *
 * 此测试在 `demo4-gurobi-cg` 配置下受控，仅在使用 -Pdemo4-gurobi-cg 显式选择时运行。
 * This test is gated under the `demo4-gurobi-cg` profile and only runs
 * when explicitly selected with -Pdemo4-gurobi-cg.
 */
class Demo4BranchAndPriceE2ETest {

    @Test
    fun `branch-and-price algorithm solves minimal scenario`() = runBlocking {
        // Build minimal domain objects
        val now = Instant.parse("2026-06-23T08:00:00Z")
        val baseAirport = Airport(ICAO("ZBAA"), AirportType.Domestic, base = true)
        val destAirport = Airport(ICAO("ZPPP"), AirportType.Domestic)

        val aircraftType = AircraftType(AircraftTypeCode("A320"))
        val minorType = AircraftMinorType(
            type = aircraftType,
            code = AircraftMinorTypeCode("A320"),
            costPerHour = FltX(5000.0),
            routeFlyTime = emptyMap(),
            connectionTime = emptyMap()
        )
        val capacity = AircraftCapacity.Passenger(
            mapOf(PassengerClass.Economy to UInt64(180UL))
        )

        val aircraft1 = Aircraft(AircraftRegisterNumber("B0001"), minorType, capacity)
        val aircraft2 = Aircraft(AircraftRegisterNumber("B0002"), minorType, capacity)
        aircraft1.setIndexed()
        aircraft2.setIndexed()

        val aircrafts = listOf(aircraft1, aircraft2)

        // Create flight legs
        val leg1Plan = FlightLegPlan(
            actualId = "FL001",
            no = "CA001",
            type = FlightType.Domestic,
            date = kotlinx.datetime.LocalDate(2026, 6, 23),
            aircraft = aircraft1,
            enabledAircrafts = setOf(aircraft1, aircraft2),
            dep = baseAirport,
            arr = destAirport,
            scheduledTime = TimeRange(now, now + 2.toDuration(DurationUnit.HOURS)),
            estimatedTime = null,
            actualTime = null,
            outTime = null,
            flightTaskStatus = emptySet()
        )
        val leg1 = FlightLeg(leg1Plan)

        val leg2Plan = FlightLegPlan(
            actualId = "FL002",
            no = "CA002",
            type = FlightType.Domestic,
            date = kotlinx.datetime.LocalDate(2026, 6, 23),
            aircraft = aircraft2,
            enabledAircrafts = setOf(aircraft1, aircraft2),
            dep = destAirport,
            arr = baseAirport,
            scheduledTime = TimeRange(now + 3.toDuration(DurationUnit.HOURS), now + 5.toDuration(DurationUnit.HOURS)),
            estimatedTime = null,
            actualTime = null,
            outTime = null,
            flightTaskStatus = emptySet()
        )
        val leg2 = FlightLeg(leg2Plan)

        val flightTasks = listOf<FlightTask>(leg1, leg2)

        // Aircraft usability
        val usability1 = AircraftUsability(lastTask = null, location = baseAirport, enabledTime = now)
        val usability2 = AircraftUsability(lastTask = null, location = destAirport, enabledTime = now)
        aircraft1._usability = usability1
        aircraft2._usability = usability2

        fun e2eCost(taskCount: Int): Cost<FltX> {
            val value = FltX(taskCount.toDouble())
            return Cost(
                items = listOf(
                    CostItem(
                        tag = "demo4-e2e",
                        costQuantity = Quantity(value, NoneUnit)
                    )
                ),
                costSum = Quantity(value, NoneUnit)
            )
        }

        val originBunches = listOf(
            FlightTaskBunch(aircraft1, listOf(leg1), Int64.zero, e2eCost(1)),
            FlightTaskBunch(aircraft2, listOf(leg2), Int64.zero, e2eCost(1))
        )

        // Bunch generation context
        val bunchGenContext = BunchGenerationContext()
        val lock = Lock()
        val connectionTimeCalculator: ConnectionTimeCalculator = { _, _, _ -> 30.toDuration(DurationUnit.MINUTES) }
        val minimumDepartureTimeCalculator: MinimumDepartureTimeCalculator = { arrivalTime, _, _, connectionTime -> arrivalTime + connectionTime }
        val ruleChecker: RuleChecker = { _, _, _ -> true }
        val costCalculator: CostCalculator = { _, _, _, _, _ -> e2eCost(1) }
        val totalCostCalculator: TotalCostCalculator = { _, tasks -> e2eCost(tasks.size) }

        val initResult = bunchGenContext.init(
            aircrafts = aircrafts,
            aircraftUsability = mapOf(aircraft1 to usability1, aircraft2 to usability2),
            flightTasks = flightTasks,
            originBunches = originBunches,
            lock = lock,
            connectionTimeCalculator = connectionTimeCalculator,
            minimumDepartureTimeCalculator = minimumDepartureTimeCalculator,
            ruleChecker = ruleChecker,
            costCalculator = costCalculator,
            totalCostCalculator = totalCostCalculator
        )
        assertTrue(initResult is Ok, "BunchGenerationContext.init should succeed: $initResult")
        assertEquals(aircrafts.size, bunchGenContext.initialFlightBunches.size)

        // Bunch compilation context
        val bunchCompContext = BunchCompilationContext()

        // Time window
        val timeWindow = TimeWindow(
            window = TimeRange(now, now + 24.toDuration(DurationUnit.HOURS)),
            continues = true,
            durationUnit = DurationUnit.HOURS,
            dateOffset = Duration.ZERO,
            interval = 1.toDuration(DurationUnit.HOURS),
            fromDouble = { FltX(it) },
            toDouble = { it.toDouble() }
        )

        // Bunch selection context
        val selectionContext = BunchSelectionContext(
            aircrafts = aircrafts,
            recoveryNeededAircrafts = aircrafts,
            recoveryNeededFlightTasks = flightTasks,
            timeWindow = timeWindow,
            flows = emptyList(),
            links = LinkMap(emptyList(), emptyList(), emptyList()),
            bunchGenerationContext = bunchGenContext,
            bunchCompilationContext = bunchCompContext
        )

        // Initialize compilation
        val compilationResult = selectionContext.initCompilation(originBunches = originBunches)
        assertTrue(compilationResult is Ok, "initCompilation should succeed: $compilationResult")

        // Build solver via LinearSolverBuilder
        val solver = LinearSolverBuilder()

        // Create B&P algorithm — verify construction succeeds
        val algorithm = BranchAndPriceAlgorithm(
            context = selectionContext,
            solver = solver,
            configuration = BranchAndPriceAlgorithm.Configuration(
                badReducedAmount = UInt64(5UL),
                maximumColumnAmount = UInt64(1000UL),
                minimumColumnAmountPerExecutor = UInt64(1UL),
                timeLimit = 300.toDuration(DurationUnit.SECONDS)
            )
        )

        // Invoke the algorithm and require a feasible solution from the minimal scenario.
        val solution = when (val result = algorithm("demo4_e2e_test")) {
            is Ok -> {
                result.value
            }
            is Failed -> {
                fail("Branch-and-price should solve the minimal scenario, but failed: ${result.error}")
            }
            is Fatal -> {
                fail("Branch-and-price should solve the minimal scenario, but was fatal: ${result.errors}")
            }
        }
        assertTrue(solution.bunches.isNotEmpty(), "Solution should contain assigned bunches")
        assertEquals(UInt64(flightTasks.size.toULong()), solution.summary.totalTaskCount)
        assertEquals(UInt64(flightTasks.size.toULong()), solution.summary.assignedTaskCount)
        assertEquals(UInt64.zero, solution.summary.canceledTaskCount)
    }
}
