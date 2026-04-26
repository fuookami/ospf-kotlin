@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model


import fuookami.ospf.kotlin.math.algebra.number.*
import java.util.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

class Passenger(
    val id: String = UUID.randomUUID().toString(),
    val amount: UInt64,
    val flights: List<Pair<FlightTask, PassengerClass>>
) {
    init {
        assert(flights.isNotEmpty())
        assert(flights.all { it.first.type.isFlightType })
        assert(flights.indices.all {
            if (it == 0) {
                true
            } else {
                flights[it - 1].first.arr == flights[it].first.dep
            }
        })
    }

    private val flightTaskKeys = flights.associate { it.first.key to it.second }

    val route: List<Airport> by lazy {
        val route = ArrayList<Airport>()
        route.add(flights.first().first.dep)
        for ((flight, _) in flights) {
            route.add(flight.arr)
        }
        route
    }

    val dep get() = route.first()
    val arr get() = route.last()
    val transfer get() = route.size > 2

    operator fun contains(task: FlightTask): Boolean {
        return task.key in flightTaskKeys
    }

    operator fun get(task: FlightTask): PassengerClass? {
        return flightTaskKeys[task.key]
    }
}

data class FlightPassenger(
    val flight: FlightTask,
    val passenger: Passenger,
    val prev: FlightPassenger? = null
) : ManualIndexed() {
    val cls = passenger[flight]!!
    val amount = passenger.amount

    override fun hashCode(): Int {
        return passenger.hashCode() xor flight.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FlightPassenger) return false

        if (passenger != other.passenger) return false
        if (flight != other.flight) return false

        return true
    }

    override fun toString() = "${flight.name}_${cls.toShortString()}_${passenger.amount}_${index}"
}

