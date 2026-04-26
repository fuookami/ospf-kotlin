@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model


import fuookami.ospf.kotlin.math.algebra.number.*
import kotlin.time.Duration
import kotlinx.datetime.*
import fuookami.ospf.kotlin.math.*

data class FlightHour(
    val hours: Duration
) {
    companion object {
        val zero = FlightHour(Duration.ZERO)
    }

    operator fun plus(rhs: FlightHour): FlightHour {
        return FlightHour(hours + rhs.hours)
    }

    operator fun minus(rhs: FlightHour): FlightHour {
        return FlightHour(hours - rhs.hours)
    }

    infix fun ls(rhs: FlightHour): Boolean {
        return hours < rhs.hours
    }

    infix fun leq(rhs: FlightHour): Boolean {
        return hours <= rhs.hours
    }
}

data class FlightCycle(
    val cycles: UInt64
) {
    companion object {
        val zero = FlightCycle(UInt64.zero)
    }

    operator fun plus(rhs: FlightCycle): FlightCycle {
        return FlightCycle(cycles + rhs.cycles)
    }

    operator fun minus(rhs: FlightCycle): FlightCycle {
        return FlightCycle(cycles - rhs.cycles)
    }

    infix fun ls(rhs: FlightCycle): Boolean {
        return cycles < rhs.cycles
    }

    infix fun leq(rhs: FlightCycle): Boolean {
        return cycles <= rhs.cycles
    }
}

data class FlightCyclePeriod(
    val expirationTime: Instant,
    val remainingFlightHour: FlightHour?,
    val remainingFlightCycle: FlightCycle?
) {
    fun enabled(flightHour: FlightHour): Boolean {
        return remainingFlightHour == null || flightHour leq remainingFlightHour
    }

    fun enabled(flightCycle: FlightCycle): Boolean {
        return remainingFlightCycle == null || flightCycle leq remainingFlightCycle
    }

    fun overFlightHour(flightHour: FlightHour): FlightHour {
        return if (remainingFlightHour != null && remainingFlightHour ls flightHour) {
            flightHour - remainingFlightHour
        } else {
            FlightHour.zero
        }
    }

    fun overFlightCycle(flightCycle: FlightCycle): FlightCycle {
        return if (remainingFlightCycle != null && remainingFlightCycle leq flightCycle) {
            flightCycle - remainingFlightCycle
        } else {
            FlightCycle.zero
        }
    }
}

