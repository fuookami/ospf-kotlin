@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration
import kotlinx.datetime.LocalDate
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*

/** Demo4 行为测试使用的最小真实领域场景。Minimal real domain scenario for Demo4 behavior tests. */
internal data class Demo4TestFixture(
    val now: Instant,
    val baseAirport: Airport,
    val destinationAirport: Airport,
    val aircraft: Aircraft,
    val usability: AircraftUsability,
    val firstTask: FlightTask,
    val secondTask: FlightTask,
    val originBunch: FlightTaskBunch
)

/** 创建稳定、无外部求解器依赖的 Demo4 测试场景。Creates a deterministic solver-free Demo4 test scenario. */
internal fun demo4TestFixture(): Demo4TestFixture {
    val now = Instant.parse("2026-06-23T08:00:00Z")
    val baseAirport = Airport(ICAO("ZBAA"), AirportType.Domestic, base = true)
    val destinationAirport = Airport(ICAO("ZPPP"), AirportType.Domestic)
    val aircraftType = AircraftType(AircraftTypeCode("A320"))
    val minorType = AircraftMinorType(
        type = aircraftType,
        code = AircraftMinorTypeCode("A320"),
        costPerHour = FltX(5000.0),
        routeFlyTime = mapOf(
            Route(baseAirport, destinationAirport) to 1.toDuration(DurationUnit.HOURS),
            Route(destinationAirport, baseAirport) to 1.toDuration(DurationUnit.HOURS)
        ),
        connectionTime = mapOf(
            baseAirport to 30.toDuration(DurationUnit.MINUTES),
            destinationAirport to 30.toDuration(DurationUnit.MINUTES)
        )
    )
    val aircraft = Aircraft(
        regNo = AircraftRegisterNumber("B0001"),
        minorType = minorType,
        capacity = AircraftCapacity.Passenger(mapOf(PassengerClass.Economy to UInt64(180UL)))
    )
    aircraft.setIndexed()

    val firstTask = FlightLeg(
        FlightLegPlan(
            actualId = "DEMO4-FL001",
            no = "CA001",
            type = FlightType.Domestic,
            date = LocalDate(2026, 6, 23),
            aircraft = aircraft,
            enabledAircrafts = setOf(aircraft),
            dep = baseAirport,
            arr = destinationAirport,
            scheduledTime = TimeRange(now, now + 1.toDuration(DurationUnit.HOURS)),
            estimatedTime = null,
            actualTime = null,
            outTime = null,
            flightTaskStatus = emptySet()
        )
    )
    val secondTask = FlightLeg(
        FlightLegPlan(
            actualId = "DEMO4-FL002",
            no = "CA002",
            type = FlightType.Domestic,
            date = LocalDate(2026, 6, 23),
            aircraft = aircraft,
            enabledAircrafts = setOf(aircraft),
            dep = destinationAirport,
            arr = baseAirport,
            scheduledTime = TimeRange(
                now + 2.toDuration(DurationUnit.HOURS),
                now + 3.toDuration(DurationUnit.HOURS)
            ),
            estimatedTime = null,
            actualTime = null,
            outTime = null,
            flightTaskStatus = emptySet()
        )
    )
    val usability = AircraftUsability(
        lastTask = null,
        location = baseAirport,
        enabledTime = now
    )
    aircraft._usability = usability

    return Demo4TestFixture(
        now = now,
        baseAirport = baseAirport,
        destinationAirport = destinationAirport,
        aircraft = aircraft,
        usability = usability,
        firstTask = firstTask,
        secondTask = secondTask,
        originBunch = FlightTaskBunch(
            aircraft = aircraft,
            tasks = listOf(firstTask, secondTask),
            iteration = Int64.zero,
            cost = demo4Cost(2.0)
        )
    )
}

/** 创建指定飞机和时间的最小航段。Creates a minimal flight leg for a selected aircraft and time. */
internal fun demo4FlightTask(
    fixture: Demo4TestFixture,
    actualId: String,
    no: String,
    aircraft: Aircraft = fixture.aircraft,
    dep: Airport = fixture.baseAirport,
    arr: Airport = fixture.destinationAirport,
    scheduledTime: TimeRange = TimeRange(fixture.now, fixture.now + 1.toDuration(DurationUnit.HOURS)),
    status: Set<FlightTaskStatus> = emptySet()
): FlightTask {
    return FlightLeg(
        FlightLegPlan(
            actualId = actualId,
            no = no,
            type = FlightType(dep, arr),
            date = LocalDate(2026, 6, 23),
            aircraft = aircraft,
            enabledAircrafts = setOf(aircraft),
            dep = dep,
            arr = arr,
            scheduledTime = scheduledTime,
            estimatedTime = null,
            actualTime = null,
            outTime = null,
            flightTaskStatus = status
        )
    )
}

/** 创建用于时间窗判定的中转任务。Creates a transfer task for time-window checks. */
internal fun demo4TransferTask(
    fixture: Demo4TestFixture,
    actualId: String,
    dep: Airport,
    arr: Airport,
    timeWindow: TimeRange,
    duration: kotlin.time.Duration
): FlightTask {
    return Transfer(
        TransferPlan(
            dep = dep,
            arr = arr,
            timeWindow = timeWindow,
            aircraft = fixture.aircraft,
            enabledAircrafts = setOf(fixture.aircraft),
            duration = duration,
            actualId = actualId
        )
    )
}

/** 构造仅允许反向换序的任务对。Creates a task pair that requires the reverse-order branch. */
internal fun demo4ReverseTaskPair(fixture: Demo4TestFixture): Pair<FlightTask, FlightTask> {
    val first = FlightLeg(
        FlightLegPlan(
            actualId = "DEMO4-REV-FL001",
            no = "REV001",
            type = FlightType.Domestic,
            date = LocalDate(2026, 6, 23),
            aircraft = fixture.aircraft,
            enabledAircrafts = setOf(fixture.aircraft),
            dep = fixture.baseAirport,
            arr = fixture.destinationAirport,
            scheduledTime = TimeRange(fixture.now + 2.toDuration(DurationUnit.HOURS), fixture.now + 3.toDuration(DurationUnit.HOURS)),
            estimatedTime = null,
            actualTime = null,
            outTime = null,
            flightTaskStatus = emptySet()
        )
    )
    val second = FlightLeg(
        FlightLegPlan(
            actualId = "DEMO4-REV-FL002",
            no = "REV002",
            type = FlightType.Domestic,
            date = LocalDate(2026, 6, 23),
            aircraft = fixture.aircraft,
            enabledAircrafts = setOf(fixture.aircraft),
            dep = fixture.destinationAirport,
            arr = fixture.baseAirport,
            scheduledTime = TimeRange(fixture.now, fixture.now + 1.toDuration(DurationUnit.HOURS)),
            estimatedTime = null,
            actualTime = null,
            outTime = null,
            flightTaskStatus = emptySet()
        )
    )
    return first to second
}

/** 构造测试使用的有单位成本。Creates a unit-bearing cost for tests. */
internal fun demo4Cost(value: Double): Cost<FltX> {
    val cost = FltX(value)
    return Cost(
        items = listOf(
            CostItem(
                tag = "demo4-test",
                costQuantity = Quantity(cost, NoneUnit)
            )
        ),
        costSum = Quantity(cost, NoneUnit)
    )
}

/** 创建缺少成本总和的无效成本。Creates an invalid cost without a sum. */
internal fun demo4InvalidCost(): Cost<FltX> {
    return Cost(emptyList(), null)
}
