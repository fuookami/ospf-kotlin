package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.BunchCompilation
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.Aircraft
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.FlightTask
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.FlightTaskAssignment
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.FlightTaskBunch
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.PassengerClass
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model.FleetBalance
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model.FlightLink
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service.limits.FleetBalanceLimit
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service.limits.FlightLinkLimit
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model.FlightPassenger
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model.Passenger
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model.PassengerCancel
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model.PassengerChange
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service.limits.PassengerCancelMinimization
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service.limits.PassengerClassChangeMinimization
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service.limits.PassengerFlightChangeMinimization

/** 验证空集合不会进入没有泛型零元的成本求和 DSL。 / Verifies empty sets do not reach the sum DSL without a generic identity. */
class EmptyObjectivePipelineTest {

    @Test
    fun `empty Demo4 objective domains register as no-op objectives`() {
        val compilation = BunchCompilation<FlightTaskBunch, FltX, FlightTask, Aircraft, FlightTaskAssignment>(
            tasks = emptyList(),
            executors = emptyList()
        )
        val model = LinearMetaModel<Flt64>(
            name = "demo4-empty-objective-pipelines",
            converter = IntoValue.Identity
        )

        assertIs<Ok<*, *, *>>(
            FlightLinkLimit(
                flightLink = FlightLink(emptyList(), compilation),
                coefficient = { Flt64.one }
            ).invoke(model)
        )
        assertIs<Ok<*, *, *>>(
            FleetBalanceLimit(
                fleetBalance = FleetBalance(emptyList(), emptyList(), compilation),
                coefficient = { _, _ -> Flt64.one }
            ).invoke(model)
        )

        val noPassengers = emptyList<FlightPassenger>()
        val passengerCancel = PassengerCancel(noPassengers)
        val passengerChange = PassengerChange(emptyList(), noPassengers)
        assertIs<Ok<*, *, *>>(
            PassengerCancelMinimization(noPassengers, passengerCancel).invoke(model)
        )
        assertIs<Ok<*, *, *>>(
            PassengerClassChangeMinimization(noPassengers, passengerChange).invoke(model)
        )
        assertIs<Ok<*, *, *>>(
            PassengerFlightChangeMinimization(noPassengers, passengerChange).invoke(model)
        )

        val fixture = demo4TestFixture()
        val passenger = Passenger(
            amount = UInt64.one,
            flights = listOf(fixture.firstTask to PassengerClass.Economy)
        )
        val flightPassenger = FlightPassenger(fixture.firstTask, passenger)
        val noAlternativeFlights = PassengerChange(
            flights = listOf(fixture.firstTask),
            passengers = listOf(flightPassenger),
            withFlightChange = true
        )
        assertIs<Ok<*, *, *>>(
            PassengerFlightChangeMinimization(
                passengers = listOf(flightPassenger),
                change = noAlternativeFlights
            ).invoke(model)
        )

        assertEquals(0, model.subObjects.size)
    }
}
