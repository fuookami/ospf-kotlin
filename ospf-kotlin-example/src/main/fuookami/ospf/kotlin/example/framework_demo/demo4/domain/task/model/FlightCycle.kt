@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import kotlin.time.Duration
import kotlin.time.Instant
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * 包装时长的飞行小时值（支持算术和比较）。A flight hour value wrapping a duration, supporting arithmetic and comparison.
 *
 * @property hours The flight hours duration / 飞行小时时长
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

/**
 * Checks if this value is strictly less than the other / 检查此值是否严格小于另一个值
 *
 * @param rhs The right-hand side value to compare / 要比较的右侧值
 * @return true if this value is strictly less, false otherwise / 如果此值严格小于则为true，否则为false
*/

    infix fun ls(rhs: FlightHour): Boolean {
        return hours < rhs.hours
    }

/**
 * Checks if this value is less than or equal to the other / 检查此值是否小于或等于另一个值
 *
 * @param rhs The right-hand side value to compare / 要比较的右侧值
 * @return true if this value is less than or equal, false otherwise / 如果此值小于或等于则为true，否则为false
*/

    infix fun leq(rhs: FlightHour): Boolean {
        return hours <= rhs.hours
    }
}

/**
 * 包装计数的飞行循环值（支持算术和比较）。A flight cycle value wrapping a count, supporting arithmetic and comparison.
 *
 * @property cycles The flight cycle count / 飞行循环计数
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

/**
 * Checks if this value is strictly less than the other / 检查此值是否严格小于另一个值
 *
 * @param rhs The right-hand side value to compare / 要比较的右侧值
 * @return true if this value is strictly less, false otherwise / 如果此值严格小于则为true，否则为false
*/

    infix fun ls(rhs: FlightCycle): Boolean {
        return cycles < rhs.cycles
    }

/**
 * Checks if this value is less than or equal to the other / 检查此值是否小于或等于另一个值
 *
 * @param rhs The right-hand side value to compare / 要比较的右侧值
 * @return true if this value is less than or equal, false otherwise / 如果此值小于或等于则为true，否则为false
*/

    infix fun leq(rhs: FlightCycle): Boolean {
        return cycles <= rhs.cycles
    }
}

/**
 * 具有过期时间和剩余飞行小时/循环限制的飞行循环维护周期。A flight cycle maintenance period with expiration time and remaining flight hour/cycle limits.
 *
 * @property expirationTime The expiration time of the period / 周期的过期时间
 * @property remainingFlightHour The remaining flight hour limit / 剩余飞行小时限制
 * @property remainingFlightCycle The remaining flight cycle limit / 剩余飞行循环限制
*/
data class FlightCyclePeriod(
    val expirationTime: Instant,
    val remainingFlightHour: FlightHour?,
    val remainingFlightCycle: FlightCycle?
) {

    /**
     * 检查给定飞行小时是否在剩余限制内。Checks whether the given flight hour is within the remaining limit.
     *
     * @param flightHour The flight hour to check / 要检查的飞行小时
     * @return true if within the remaining limit, false otherwise / 如果在剩余限制内则为true，否则为false
    */
    fun enabled(flightHour: FlightHour): Boolean {
        return remainingFlightHour == null || flightHour leq remainingFlightHour
    }

    /**
     * 检查给定飞行循环是否在剩余限制内。Checks whether the given flight cycle is within the remaining limit.
     *
     * @param flightCycle The flight cycle to check / 要检查的飞行循环
     * @return true if within the remaining limit, false otherwise / 如果在剩余限制内则为true，否则为false
    */
    fun enabled(flightCycle: FlightCycle): Boolean {
        return remainingFlightCycle == null || flightCycle leq remainingFlightCycle
    }

    /**
     * 返回超出剩余限制的飞行小时数。Returns the excess flight hours beyond the remaining limit.
     *
     * @param flightHour The flight hour to compare / 要比较的飞行小时
     * @return The excess flight hours beyond the remaining limit / 超出剩余限制的飞行小时
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
     * @param flightCycle The flight cycle to compare / 要比较的飞行循环
     * @return The excess flight cycles beyond the remaining limit / 超出剩余限制的飞行循环
    */
    fun overFlightCycle(flightCycle: FlightCycle): FlightCycle {
        return if (remainingFlightCycle != null && remainingFlightCycle leq flightCycle) {
            flightCycle - remainingFlightCycle
        } else {
            FlightCycle.zero
        }
    }
}
