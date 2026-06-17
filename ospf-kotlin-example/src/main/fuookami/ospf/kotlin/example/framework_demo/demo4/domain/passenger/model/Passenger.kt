@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model

import java.util.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 具有数量和航段列表的乘客（每个航段分配一个舱位）。A passenger with an amount and a list of flight legs, each assigned a passenger class.
 *
 * @property id 参数。
 */
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

    /** 此乘客访问的机场路线。The route of airports visited by this passenger. */
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

    /**
     * Checks whether this passenger is on the given flight task.
 *
     * @param task 参数。
     * @return 返回结果。
     */
    operator fun contains(task: FlightTask): Boolean {
        return task.key in flightTaskKeys
    }

    /**
     * Returns the passenger class for the given flight task, or null if not on that flight.
 *
     * @param task 参数。
     * @return 返回结果。
     */
    operator fun get(task: FlightTask): PassengerClass? {
        return flightTaskKeys[task.key]
    }
}

/**
 * 将乘客链接到特定航班的乘客-航班关联（具有可选前一航段）。A passenger-flight association linking a passenger to a specific flight with optional previous leg.
 *
 * @property flight 参数。
 * @property passenger 参数。
 * @property prev 参数。
 */
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
