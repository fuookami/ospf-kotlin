@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import kotlin.time.Instant
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/**
 * 通过注册号标识的飞机（具有子机型和容量信息）。An aircraft identified by register number, with minor type and capacity information.
 *
 * @property regNo 参数。
 * @property minorType 参数。
 * @property capacity 参数。
 */
data class Aircraft(
    val regNo: AircraftRegisterNumber,
    val minorType: AircraftMinorType,
    val capacity: AircraftCapacity
) : Executor(AircraftId(regNo.no), regNo.no) {
    internal lateinit var _usability: AircraftUsability
    val usability get() = _usability

    val type by minorType::type
    val costPerHour by minorType::costPerHour
    val routeFlyTime by minorType::routeFlyTime
    val maxFlyTime by minorType::maxFlyTime
    val maxRouteFlyTime by minorType::maxRouteFlyTime
    val connectionTime by minorType::connectionTime
    val maxConnectionTime by minorType::maxConnectionTime

    companion object {
        private val pool = HashMap<AircraftRegisterNumber, Aircraft>()
        val values by pool::values

        /**
         * 从池中按注册号检索飞机。Retrieves an [Aircraft] by register number from the pool.
 *
         * @param regNo 参数。
         * @return 返回结果。
         */
        operator fun invoke(regNo: AircraftRegisterNumber): Aircraft? {
            return pool[regNo]
        }
    }

    init {
        pool[regNo] = this
    }

    /**
     * 返回给定舱位的乘客容量，如果不是客机则返回零。Returns the passenger capacity for the given class, or zero if not a passenger aircraft.
 *
     * @param cls 参数。
     * @return 返回结果。
     */
    fun capacity(cls: PassengerClass): UInt64 {
        return when (val capacity = this.capacity) {
            is AircraftCapacity.Passenger -> { capacity[cls] }
            else -> { UInt64.zero }
        }
    }

    override fun hashCode(): Int {
        assert(regNo.no.all { it.isDigit() || it.isUpperCase() })

        var ret = 0
        for (ch in regNo.no) {
            ret = ret shl 5
            ret = if (ch.isDigit()) {
                ret or (ch - '0')
            } else {
                ret or (ch - 'A') + 10
            }
        }
        return ret
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Aircraft

        return regNo == other.regNo
    }

    override fun toString() = "$regNo"
}

/**
 * 跟踪飞机可用性（包括位置、启用时间和飞行循环周期）。Tracks aircraft usability including location, enabled time, and flight cycle periods.
 *
 * @property lastTask 参数。
 * @property location 参数。
 * @property enabledTime 参数。
 * @property flightCyclePeriods 参数。
 */
class AircraftUsability(
    lastTask: FlightTask?,
    val location: Airport,
    enabledTime: Instant,
    val flightCyclePeriods: List<FlightCyclePeriod> = emptyList()
) : ExecutorInitialUsability<FlightTask, Aircraft, FlightTaskAssignment>(lastTask, enabledTime) {
    /**
     * 返回给定时间和飞行小时超出的飞行周期数。Returns the number of flight cycle periods exceeded for the given time and flight hour.
 *
     * @param time 参数。
     * @param flightHour 参数。
     * @return 返回结果。
     */
    fun overFlightHourTimes(time: Instant, flightHour: FlightHour): UInt64 {
        return UInt64(flightCyclePeriods.count {
            time < it.expirationTime && !it.enabled(flightHour)
        })
    }

    /**
     * 返回给定时间和飞行循环超出的飞行周期数。Returns the number of flight cycle periods exceeded for the given time and flight cycle.
 *
     * @param time 参数。
     * @param flightCycle 参数。
     * @return 返回结果。
     */
    fun overFlightCycleTimes(time: Instant, flightCycle: FlightCycle): UInt64 {
        return UInt64(flightCyclePeriods.count {
            time < it.expirationTime && !it.enabled(flightCycle)
        })
    }

    /**
     * 返回所有周期的总超出飞行小时数。Returns the total excess flight hours across all periods.
 *
     * @param time 参数。
     * @param flightHour 参数。
     * @return 返回结果。
     */
    fun overFlightHour(time: Instant, flightHour: FlightHour): FlightHour {
        var ret = FlightHour.zero
        for (period in flightCyclePeriods) {
            if (time >= period.expirationTime) {
                continue
            }
            ret += period.overFlightHour(flightHour)
        }
        return ret
    }

    /**
     * 返回所有周期的总超出飞行循环数。Returns the total excess flight cycles across all periods.
 *
     * @param time 参数。
     * @param flightCycle 参数。
     * @return 返回结果。
     */
    fun overFlightCycle(time: Instant, flightCycle: FlightCycle): FlightCycle {
        var ret = FlightCycle.zero
        for (period in flightCyclePeriods) {
            if (time >= period.expirationTime) {
                continue
            }
            ret += period.overFlightCycle(flightCycle)
        }
        return ret
    }
}
