package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.math.*
import kotlin.time.*
import kotlinx.datetime.Instant
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

fun max(lhs: Duration, rhs: Duration): Duration {
    return if (lhs <= rhs) {
        rhs
    } else {
        lhs
    }
}

fun min(lhs: Duration, rhs: Duration): Duration {
    return if (lhs <= rhs) {
        lhs
    } else {
        rhs
    }
}

open class WorkingCalendar(
    val timeWindow: TimeWindow,
    unavailableTimes: List<TimeRange> = emptyList()
) {
    open val unavailableTimes = unavailableTimes.sortedBy { it.start }

    fun actualTime(
        time: TimeRange,
        connectionTime: Duration = Duration.ZERO
    ): TimeRange {
        var currentTime = time
        for (unavailableTime in unavailableTimes) {
            val intersection = currentTime.intersectionWith(unavailableTime)
            if (intersection == null && unavailableTime.start >= currentTime.start) {
                break
            }
            if (intersection != null && !intersection.empty) {
                currentTime = TimeRange(
                    start = currentTime.start,
                    end = max(currentTime.end, unavailableTime.end) + intersection.duration + connectionTime
                )
            }
        }
        return currentTime
    }
}

open class Productivity<T>(
    val timeWindow: TimeRange,
    val capacities: Map<T, Duration>
) {
    open fun capacityOf(material: T): Duration? {
        return capacities[material]
    }

    open fun new(
        timeWindow: TimeRange? = null,
        capacities: Map<T, Duration>? = null
    ): Productivity<T> {
        return Productivity(
            timeWindow = timeWindow ?: this.timeWindow,
            capacities = capacities ?: this.capacities
        )
    }
}

sealed class ProductivityCalendar<Q, P, T>(
    timeWindow: TimeWindow,
    productivity: List<P>,
    unavailableTimes: List<TimeRange>? = null,
    private val constants: RealNumberConstants<Q>,
    private val floor: Extractor<Q, Flt64>
): WorkingCalendar(timeWindow) where P: Productivity<T>, Q: RealNumber<Q>, Q: PlusGroup<Q> {
    companion object {
        operator fun <P, T> invoke(
            timeWindow: TimeWindow,
            productivity: List<P>,
            constants: UInt64.Companion
        ): DiscreteProductivityCalendar<P, T> where P: Productivity<T> {
            return DiscreteProductivityCalendar(
                timeWindow = timeWindow,
                productivity = productivity
            )
        }

        operator fun <P, T> invoke(
            timeWindow: TimeWindow,
            productivity: List<P>,
            constants: Flt64.Companion
        ): ContinuousProductivityCalendar<P, T> where P: Productivity<T> {
            return ContinuousProductivityCalendar(
                timeWindow = timeWindow,
                productivity = productivity
            )
        }

        operator fun <P, T> invoke(
            timeWindow: TimeWindow,
            productivity: List<P>,
            continuous: Boolean
        ): ProductivityCalendar<*, P, T> where P: Productivity<T> {
            return if (continuous) {
                ContinuousProductivityCalendar(
                    timeWindow = timeWindow,
                    productivity = productivity
                )
            } else {
                DiscreteProductivityCalendar(
                    timeWindow = timeWindow,
                    productivity = productivity
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    val productivity: List<P> by lazy {
        if (unavailableTimes != null) {
            productivity.flatMap {
                val timeRanges = it.timeWindow.differenceWith(unavailableTimes)
                timeRanges.map { time -> it.new(
                    timeWindow = time
                ) as P }
            }.sortedBy { it.timeWindow.start }
        } else {
            productivity.sortedBy { it.timeWindow.start }
        }
    }

    override val unavailableTimes: List<TimeRange> by lazy {
        unavailableTimes
            ?: productivity.flatMapIndexed { i, produceTime ->
                val result = ArrayList<TimeRange>()
                if (i == 0) {
                    produceTime.timeWindow.front?.let { result.add(it) }
                } else {
                    produceTime.timeWindow.frontBetween(productivity[i - 1].timeWindow)?.let { result.add(it) }
                }
                if (i == (productivity.size - 1)) {
                    produceTime.timeWindow.back?.let { result.add(it) }
                }
                result
            }
    }

    fun actualTimeFrom(
        material: T,
        startTime: Instant,
        quantity: Q = constants.one,
        connectionTime: Duration = Duration.ZERO
    ): TimeRange {
        val productivityCalendar = productivity.findFrom(startTime, Productivity<T>::timeWindow)
        return actualTimeFrom(
            material,
            startTime,
            productivityCalendar,
            quantity,
            connectionTime
        )
    }

    suspend fun actualTimeFromParallelly(
        material: T,
        startTime: Instant,
        quantity: Q = constants.one,
        connectionTime: Duration = Duration.ZERO
    ): TimeRange {
        val productivityCalendar = productivity.findFromParallelly(startTime, Productivity<T>::timeWindow)
        return actualTimeFrom(
            material,
            startTime,
            productivityCalendar,
            quantity,
            connectionTime
        )
    }

    fun actualTimeUntil(
        material: T,
        endTime: Instant,
        quantity: Q = constants.one,
        connectionTime: Duration = Duration.ZERO
    ): TimeRange {
        val productivityCalendar = productivity.findUntil(endTime, Productivity<T>::timeWindow).reversed()
        return actualTimeUntil(
            material,
            endTime,
            productivityCalendar,
            quantity,
            connectionTime
        )
    }

    suspend fun actualTimeUntilParallelly(
        material: T,
        endTime: Instant,
        quantity: Q = constants.one,
        connectionTime: Duration = Duration.ZERO
    ): TimeRange {
        val productivityCalendar = productivity.findUntilParallelly(endTime, Productivity<T>::timeWindow).reversed()
        return actualTimeUntil(
            material,
            endTime,
            productivityCalendar,
            quantity,
            connectionTime
        )
    }

    fun actualQuantity(
        material: T,
        time: TimeRange,
        connectionTime: Duration
    ): Q {
        val productivityCalendar = productivity.find(time, Productivity<T>::timeWindow)
        return actualQuantity(
            material,
            time,
            productivityCalendar,
            connectionTime
        )
    }

    suspend fun actualQuantityParallelly(
        material: T,
        time: TimeRange,
        connectionTime: Duration
    ): Q {
        val productivityCalendar = productivity.findParallelly(time, Productivity<T>::timeWindow)
        return actualQuantity(
            material,
            time,
            productivityCalendar,
            connectionTime
        )
    }

    private fun actualTimeFrom(
        material: T,
        startTime: Instant,
        productivityCalendar: List<Productivity<T>>,
        quantity: Q,
        connectionTime: Duration
    ): TimeRange {
        var currentTime = max(startTime, productivityCalendar.first().timeWindow.start)
        var restQuantity = quantity
        for (calendar in productivityCalendar) {
            currentTime = max(
                currentTime,
                calendar.timeWindow.start + connectionTime
            )
            val currentProductivity = calendar.capacityOf(material)
                ?.let { Flt64.one / timeWindow.valueOf(it) }
                ?: Flt64.zero
            val maxProduceTime = timeWindow.valueOf(calendar.timeWindow.end - currentTime)
            val maxProduceQuantity = floor(maxProduceTime * currentProductivity)
            if (maxProduceQuantity >= restQuantity) {
                val thisProduceTime = timeWindow.durationOf(restQuantity.toFlt64() / currentProductivity)
                currentTime += thisProduceTime
                restQuantity = constants.zero
                break
            } else {
                restQuantity -= maxProduceQuantity
            }
        }
        return if (restQuantity == constants.zero) {
            TimeRange(
                start = startTime,
                end = currentTime
            )
        } else {
            TimeRange(
                start = startTime
            )
        }
    }

    private fun actualTimeUntil(
        material: T,
        endTime: Instant,
        productivityCalendar: List<Productivity<T>>,
        quantity: Q,
        connectionTime: Duration
    ): TimeRange {
        var currentTime = min(endTime, productivityCalendar.first().timeWindow.end)
        var restAmount = quantity
        for (calendar in productivityCalendar) {
            currentTime = min(
                currentTime,
                calendar.timeWindow.end
            )
            val currentProductivity = calendar.capacityOf(material)
                ?.let { Flt64.one / timeWindow.valueOf(it) }
                ?: Flt64.zero
            val maxProduceTime = timeWindow.valueOf(currentTime - calendar.timeWindow.start - connectionTime)
            val maxProduceAmount = floor(maxProduceTime * currentProductivity)
            if (maxProduceAmount >= restAmount) {
                val thisProduceTime = timeWindow.durationOf(restAmount.toFlt64() / currentProductivity)
                currentTime -= thisProduceTime
                restAmount = constants.zero
                break
            } else {
                restAmount -= maxProduceAmount
            }
        }
        return if (restAmount == constants.zero) {
            TimeRange(
                start = currentTime,
                end = endTime
            )
        } else {
            TimeRange(
                end = endTime
            )
        }
    }

    private fun actualQuantity(
        material: T,
        time: TimeRange,
        productivityCalendar: List<Productivity<T>>,
        connectionTime: Duration
    ): Q {
        var quantity = constants.zero
        for (calendar in productivityCalendar) {
            val intersection = time.intersectionWith(calendar.timeWindow)
                ?: continue
            val produceTime = timeWindow.valueOf(
                if (intersection.start == calendar.timeWindow.start) {
                    intersection.duration - connectionTime
                } else {
                    intersection.duration
                }
            )
            val currentProductivity = calendar.capacityOf(material)
                ?.let { Flt64.one / timeWindow.valueOf(it) }
                ?: Flt64.zero
            quantity += floor(produceTime * currentProductivity)
        }
        return quantity
    }
}

open class DiscreteProductivityCalendar<P, T>(
    timeWindow: TimeWindow,
    productivity: List<P>,
    unavailableTimes: List<TimeRange>? = null
) : ProductivityCalendar<UInt64, P, T>(timeWindow, productivity, unavailableTimes, UInt64, { it.floor().toUInt64() })
        where P: Productivity<T>

open class ContinuousProductivityCalendar<P, T>(
    timeWindow: TimeWindow,
    productivity: List<P>,
    unavailableTimes: List<TimeRange>? = null
) : ProductivityCalendar<Flt64, P, T>(timeWindow, productivity, unavailableTimes, Flt64, { it }) where P: Productivity<T>
