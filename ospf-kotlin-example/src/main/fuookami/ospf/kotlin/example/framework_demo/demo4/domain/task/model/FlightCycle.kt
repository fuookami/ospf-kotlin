@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import kotlin.time.Duration
import kotlin.time.Instant
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * 包装时长的飞行小时值（支持算术和比较）。A flight hour value wrapping a duration, supporting arithmetic and comparison.
 *
 * @property hours 参数。
 */
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

/**
 * 包装计数的飞行循环值（支持算术和比较）。A flight cycle value wrapping a count, supporting arithmetic and comparison.
 *
 * @property cycles 参数。
 */
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

/**
 * 具有过期时间和剩余飞行小时/循环限制的飞行循环维护周期。A flight cycle maintenance period with expiration time and remaining flight hour/cycle limits.
 *
 * @property expirationTime 参数。
 * @property remainingFlightHour 参数。
 * @property remainingFlightCycle 参数。
 */
data class FlightCyclePeriod(
    val expirationTime: Instant,
    val remainingFlightHour: FlightHour?,
    val remainingFlightCycle: FlightCycle?
) {
    /**
     * 检查给定飞行小时是否在剩余限制内。Checks whether the given flight hour is within the remaining limit.
 *
     * @param flightHour 参数。
     * @return 返回结果。
     */
    fun enabled(flightHour: FlightHour): Boolean {
        return remainingFlightHour == null || flightHour leq remainingFlightHour
    }

    /**
     * 检查给定飞行循环是否在剩余限制内。Checks whether the given flight cycle is within the remaining limit.
 *
     * @param flightCycle 参数。
     * @return 返回结果。
     */
    fun enabled(flightCycle: FlightCycle): Boolean {
        return remainingFlightCycle == null || flightCycle leq remainingFlightCycle
    }

    /**
     * 返回超出剩余限制的飞行小时数。Returns the excess flight hours beyond the remaining limit.
 *
     * @param flightHour 参数。
     * @return 返回结果。
     */
    fun overFlightHour(flightHour: FlightHour): FlightHour {
        return if (remainingFlightHour != null && remainingFlightHour ls flightHour) {
            flightHour - remainingFlightHour
        } else {
            FlightHour.zero
        }
    }

    /**
     * 返回超出剩余限制的飞行循环数。Returns the excess flight cycles beyond the remaining limit.
 *
     * @param flightCycle 参数。
     * @return 返回结果。
     */
    fun overFlightCycle(flightCycle: FlightCycle): FlightCycle {
        return if (remainingFlightCycle != null && remainingFlightCycle leq flightCycle) {
            flightCycle - remainingFlightCycle
        } else {
            FlightCycle.zero
        }
    }
}
