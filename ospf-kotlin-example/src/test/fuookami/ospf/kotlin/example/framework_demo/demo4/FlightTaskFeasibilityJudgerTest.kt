@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlin.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 航班任务可行性判定器的配置测试 / Tests for FlightTaskFeasibilityJudger configuration.
 */
class FlightTaskFeasibilityJudgerTest {

    @Test
    fun `default Config has checkEnabledTime true`() {
        val config = FlightTaskFeasibilityJudger.Config()
        assertTrue(config.checkEnabledTime)
    }

    @Test
    fun `default Config has null departureTime`() {
        val config = FlightTaskFeasibilityJudger.Config()
        assertNull(config.departureTime)
    }

    @Test
    fun `custom Config overrides checkEnabledTime`() {
        val config = FlightTaskFeasibilityJudger.Config(
            checkEnabledTime = false
        )
        assertFalse(config.checkEnabledTime)
    }

    @Test
    fun `diagnose reports aircraft type mismatch`() {
        val fixture = demo4TestFixture()
        val incompatibleAircraft = aircraft(
            fixture = fixture,
            regNo = "B9002",
            minorType = minorType(fixture, "A321", "A321-TEST")
        )
        val task = demo4FlightTask(
            fixture = fixture,
            actualId = "DIAG-TYPE",
            no = "DIAG-TYPE",
            aircraft = incompatibleAircraft,
            status = setOf(FlightTaskStatus.NotAircraftTypeChange)
        )

        val diagnostic = judger(fixture).diagnose(fixture.aircraft, null, task)

        assertFailure(FlightTaskFeasibilityFailureStage.AircraftType, diagnostic)
    }

    @Test
    fun `diagnose reports aircraft minor type mismatch`() {
        val fixture = demo4TestFixture()
        val incompatibleAircraft = aircraft(
            fixture = fixture,
            regNo = "B9003",
            minorType = minorType(fixture, "A320", "A320-OTHER")
        )
        val task = demo4FlightTask(
            fixture = fixture,
            actualId = "DIAG-MINOR-TYPE",
            no = "DIAG-MINOR-TYPE",
            aircraft = incompatibleAircraft,
            status = setOf(
                FlightTaskStatus.NotAircraftTypeChange,
                FlightTaskStatus.NotAircraftMinorTypeChange
            )
        )

        val diagnostic = judger(fixture).diagnose(fixture.aircraft, null, task)

        assertFailure(FlightTaskFeasibilityFailureStage.AircraftMinorType, diagnostic)
    }

    @Test
    fun `diagnose reports capacity category mismatch`() {
        val fixture = demo4TestFixture()
        val cargoAircraft = aircraft(
            fixture = fixture,
            regNo = "B9004",
            capacity = AircraftCapacity.Cargo(FltX(1000.0))
        )
        val task = demo4FlightTask(
            fixture = fixture,
            actualId = "DIAG-CAPACITY",
            no = "DIAG-CAPACITY",
            aircraft = cargoAircraft
        )

        val diagnostic = judger(fixture).diagnose(fixture.aircraft, null, task)

        assertFailure(FlightTaskFeasibilityFailureStage.Capacity, diagnostic)
    }

    @Test
    fun `diagnose reports aircraft usability time mismatch`() {
        val fixture = demo4TestFixture()
        val lateUsability = AircraftUsability(
            lastTask = null,
            location = fixture.baseAirport,
            enabledTime = fixture.now + 30.minutes
        )

        val diagnostic = judger(fixture, usability = lateUsability)
            .diagnose(fixture.aircraft, null, fixture.firstTask)

        assertFailure(FlightTaskFeasibilityFailureStage.Usability, diagnostic)
    }

    @Test
    fun `diagnose reports airport connection mismatch`() {
        val fixture = demo4TestFixture()
        val disconnectedTask = demo4FlightTask(
            fixture = fixture,
            actualId = "DIAG-AIRPORT",
            no = "DIAG-AIRPORT",
            dep = fixture.baseAirport,
            arr = fixture.destinationAirport,
            scheduledTime = TimeRange(
                fixture.now + 2.hours,
                fixture.now + 3.hours
            )
        )

        val diagnostic = judger(fixture)
            .diagnose(fixture.aircraft, fixture.firstTask, disconnectedTask)

        assertFailure(FlightTaskFeasibilityFailureStage.AirportConnection, diagnostic)
    }

    @Test
    fun `diagnose reports time window mismatch`() {
        val fixture = demo4TestFixture()
        val transfer = demo4TransferTask(
            fixture = fixture,
            actualId = "DIAG-TIME-WINDOW",
            dep = fixture.destinationAirport,
            arr = fixture.baseAirport,
            timeWindow = TimeRange(
                fixture.now + 1.hours,
                fixture.now + 90.minutes
            ),
            duration = 1.hours
        )

        val diagnostic = judger(fixture)
            .diagnose(fixture.aircraft, fixture.firstTask, transfer)

        assertFailure(FlightTaskFeasibilityFailureStage.TimeWindow, diagnostic)
    }

    @Test
    fun `diagnose reports rule rejection`() {
        val fixture = demo4TestFixture()
        val diagnostic = judger(
            fixture = fixture,
            ruleChecker = RuleChecker { _, _, _ -> false }
        ).diagnose(fixture.aircraft, fixture.firstTask, fixture.secondTask)

        assertFailure(FlightTaskFeasibilityFailureStage.Rules, diagnostic)
    }

    private fun judger(
        fixture: Demo4TestFixture,
        usability: AircraftUsability = fixture.usability,
        ruleChecker: RuleChecker = RuleChecker { _, _, _ -> true }
    ): FlightTaskFeasibilityJudger {
        return FlightTaskFeasibilityJudger(
            aircraftUsability = mapOf(fixture.aircraft to usability),
            connectionTimeCalculator = ConnectionTimeCalculator { _, _, _ -> 30.minutes },
            ruleChecker = ruleChecker
        )
    }

    private fun aircraft(
        fixture: Demo4TestFixture,
        regNo: String,
        minorType: AircraftMinorType = fixture.aircraft.minorType,
        capacity: AircraftCapacity = AircraftCapacity.Passenger(
            mapOf(PassengerClass.Economy to UInt64(180UL))
        )
    ): Aircraft {
        return Aircraft(
            regNo = AircraftRegisterNumber(regNo),
            minorType = minorType,
            capacity = capacity
        ).also {
            it._usability = AircraftUsability(
                lastTask = null,
                location = fixture.baseAirport,
                enabledTime = fixture.now
            )
        }
    }

    private fun minorType(
        fixture: Demo4TestFixture,
        typeCode: String,
        minorTypeCode: String,
        maxFlyTime: Duration? = null
    ): AircraftMinorType {
        return AircraftMinorType(
            type = AircraftType(AircraftTypeCode(typeCode)),
            code = AircraftMinorTypeCode(minorTypeCode),
            costPerHour = FltX(5000.0),
            routeFlyTime = mapOf(
                Route(fixture.baseAirport, fixture.destinationAirport) to 1.hours,
                Route(fixture.destinationAirport, fixture.baseAirport) to 1.hours
            ),
            connectionTime = mapOf(
                fixture.baseAirport to 30.minutes,
                fixture.destinationAirport to 30.minutes
            ),
            maxFlyTime = maxFlyTime
        )
    }

    private fun assertFailure(
        expected: FlightTaskFeasibilityFailureStage,
        diagnostic: FlightTaskFeasibilityDiagnostic
    ) {
        assertFalse(diagnostic.feasible)
        assertEquals(expected, diagnostic.failureStage)
        assertEquals(expected.message, diagnostic.message)
    }
}
