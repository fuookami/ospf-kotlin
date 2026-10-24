@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.example.framework_demo.demo4

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

class PassengerPreparationTest {
    @Test
    fun `route and incoming indices preserve duplicate relations occurrences and polynomial order`() {
        val fixture = demo4TestFixture()
        val alternate = demo4FlightTask(fixture = fixture, actualId = "ALT", no = "ALT")
        val flights = CountingList(listOf(fixture.firstTask, alternate, fixture.secondTask, alternate))
        ManualIndexed.flush<FlightPassenger>()
        val people = listOf(fixture.firstTask, alternate, fixture.secondTask).mapIndexed { index, flight ->
            FlightPassenger(
                flight = flight,
                passenger = Passenger(
                    id = "P$index",
                    amount = UInt64(index + 2),
                    flights = listOf(flight to PassengerClass.Economy)
                )
            ).also { it.setIndexed() }
        }
        val change = PassengerChange(flights = flights, passengers = people, withFlightChange = true)
        val alternatives = change.toFlights
        assertEquals(flights.size, flights.reads)
        for (person in people) {
            assertEquals(flights.filter {
                it != person.flight && it.dep == person.flight.dep && it.arr == person.flight.arr
            }, alternatives[person.flight])
        }
        val model = LinearMetaModel(name = "passenger-index", converter = IntoValue.Identity)
        val cancel = PassengerCancel(people)
        assertTrue(cancel.register(model) is Ok)
        assertTrue(change.register(model) is Ok)
        val groups = linkedMapOf(
            fixture.firstTask to CountingList(listOf(people[0], people[0])),
            alternate to CountingList(listOf(people[1])),
            fixture.secondTask to CountingList(listOf(people[2]))
        )
        val amount = PassengerAmount(
            flights = listOf(fixture.firstTask, alternate, fixture.secondTask),
            passengers = groups,
            cancel = cancel,
            change = change
        )
        assertTrue(amount.register(model) is Ok)
        assertEquals((PassengerClass.entries.size + 1) * 4, groups.values.fold(0) { total, group -> total + group.reads })
        for (flight in amount.passengerAmount.keys) {
            for (cls in PassengerClass.entries) {
                var expected = LinearPolynomial()
                for (person in groups[flight] ?: emptyList()) {
                    if (person.cls == cls) expected += person.amount.toFlt64()
                    expected -= Flt64.one * cancel.passengerCancel[person]
                    if (person.cls == cls) {
                        expected -= sum(change.passengerClassChange[person, _a])
                        expected -= sum(change.passengerFlightChange[person, _a, _a])
                    } else {
                        expected += Flt64.one * change.passengerClassChange[person, cls]!!
                    }
                }
                for (person in groups.values.flatten()) {
                    if ((change.toFlights[person.flight] ?: emptyList()).contains(flight)) {
                        expected += Flt64.one * change.passengerFlightChange[person, flight, cls]!!
                    }
                }
                val actual = amount.passengerAmount.getValue(flight).getValue(cls).polynomial
                assertEquals(expected.monomials, actual.monomials)
                assertEquals(expected.constant.toDouble().toRawBits(), actual.constant.toDouble().toRawBits())
            }
        }
    }

    @Test
    fun `disabled flight change retains existing uninitialized variable precondition`() {
        val fixture = demo4TestFixture()
        ManualIndexed.flush<FlightPassenger>()
        val person = FlightPassenger(
            flight = fixture.firstTask,
            passenger = Passenger(amount = UInt64.one, flights = listOf(fixture.firstTask to PassengerClass.Economy))
        ).also { it.setIndexed() }
        val model = LinearMetaModel(name = "disabled-passenger-change", converter = IntoValue.Identity)
        val cancel = PassengerCancel(listOf(person))
        val change = PassengerChange(listOf(fixture.firstTask), listOf(person))
        assertTrue(cancel.register(model) is Ok)
        assertTrue(change.register(model) is Ok)
        val amount = PassengerAmount(
            flights = listOf(fixture.firstTask),
            passengers = mapOf(fixture.firstTask to listOf(person)),
            cancel = cancel,
            change = change
        )
        assertThrows(UninitializedPropertyAccessException::class.java) { amount.register(model) }
    }

    @Test
    fun `successor index keeps first branch and preparation reads each passenger twice`() {
        val fixture = demo4TestFixture()
        val alternate = demo4FlightTask(fixture = fixture, actualId = "ROOT-ALT", no = "RA")
        val branch = demo4FlightTask(
            fixture = fixture,
            actualId = "BRANCH",
            no = "BR",
            dep = fixture.destinationAirport,
            arr = fixture.baseAirport
        )
        val tasks = listOf(fixture.firstTask, fixture.secondTask, branch, alternate)
        ManualIndexed.flush<Aircraft>()
        fixture.aircraft.refreshIndex()
        val compilation = Compilation(tasks = tasks, executors = listOf(fixture.aircraft))
        val window = TimeWindow(
            window = fixture.originBunch.time!!,
            fromDouble = { Flt64(it) },
            toDouble = { it.toDouble() }
        )
        val time = TaskTime(timeWindow = window, tasks = tasks, compilation = compilation)
        time.estimateStartTime = LinearIntermediateSymbols1("est", Shape1(tasks.size)) { i, _ ->
            LinearExpressionSymbol(Flt64(i + 1), name = "est_$i")
        }
        time.estimateEndTime = LinearIntermediateSymbols1("eet", Shape1(tasks.size)) { i, _ ->
            LinearExpressionSymbol(Flt64(i + 2), name = "eet_$i")
        }
        ManualIndexed.flush<FlightPassenger>()
        fun person(flight: FlightTask, id: String, prev: FlightPassenger? = null): FlightPassenger {
            return FlightPassenger(
                flight = flight,
                passenger = Passenger(id = id, amount = UInt64.one, flights = listOf(flight to PassengerClass.Economy)),
                prev = prev
            ).also { it.setIndexed() }
        }
        val root = person(fixture.firstTask, "root")
        val first = person(fixture.secondTask, "first", root)
        val later = person(branch, "later", root.copy())
        val passengers = CountingList(listOf(root, first, later))
        val change = PassengerChange(flights = tasks, passengers = passengers, withFlightChange = true)
        val model = LinearMetaModel(name = "passenger-successor", converter = IntoValue.Identity)
        assertTrue(change.register(model) is Ok)
        passengers.reads = 0
        assertTrue(PassengerFlightChangeConstraint(
            timeWindow = window,
            passengers = passengers,
            time = time,
            change = change
        )(model) is Ok)
        assertEquals(passengers.size * 2, passengers.reads)
        val eet = model.tokens.symbols.filterIsInstance<LinearFunctionSymbolAdapter<Flt64>>()
            .map { it.delegate }.filterIsInstance<IfFunction<Flt64>>()
            .single { it.name == "${root}_${alternate}_eet" }
        val expected = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64(-1.0), time.estimateEndTime[root.flight]),
                LinearMonomial(Flt64.one, time.estimateStartTime[first.flight])
            ),
            constant = -window.toFlt64Boundary().valueOf(first.flight.dep.passengerTransferTime)
        )
        assertEquals(expected.monomials, eet.condition.monomials)
        assertEquals(expected.constant, eet.condition.constant)
    }

    private class CountingList<T>(private val source: List<T>) : AbstractList<T>() {
        var reads = 0
        override val size get() = source.size
        override fun get(index: Int): T {
            ++reads
            return source[index]
        }
    }
}
