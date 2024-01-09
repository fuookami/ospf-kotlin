package fuookami.ospf.kotlin.framework.gantt_scheduling.infrasturcutre

import kotlin.math.*
import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import kotlinx.datetime.Instant

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
}

sealed class ProductivityCalendar<Q, P, T>(
    timeWindow: TimeWindow,
    productivity: List<P>
): WorkingCalendar(timeWindow) where P: Productivity<T> {
    companion object {
        operator fun <P, T> invoke(
            timeWindow: TimeWindow,
            productivity: List<P>,
            flag: UInt64.Companion
        ): DiscreteProductivityCalendar<P, T> where P: Productivity<T> {
            return DiscreteProductivityCalendar(
                timeWindow = timeWindow,
                productivity = productivity
            )
        }

        operator fun <P, T> invoke(
            timeWindow: TimeWindow,
            productivity: List<P>,
            flag: Flt64.Companion
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

    val productivity: List<P> = productivity.sortedBy { it.timeWindow.start }

    override val unavailableTimes: List<TimeRange> by lazy {
        productivity.flatMapIndexed { i, produceTime ->
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

    abstract suspend fun actualTimeFrom(
        material: T,
        quantity: Q,
        startTime: Instant,
        connectionTime: Duration = Duration.ZERO
    ): TimeRange

    abstract suspend fun actualTimeUntil(
        material: T,
        quantity: Q,
        endTime: Instant,
        connectionTime: Duration = Duration.ZERO
    ): TimeRange

    abstract suspend fun actualQuantity(
        material: T,
        time: TimeRange,
        connectionTime: Duration = Duration.ZERO
    ): Q
}

open class DiscreteProductivityCalendar<P, T>(
    timeWindow: TimeWindow,
    productivity: List<P>
) : ProductivityCalendar<UInt64, P, T>(timeWindow, productivity) where P: Productivity<T> {
    override suspend fun actualTimeFrom(
        material: T,
        quantity: UInt64,
        startTime: Instant,
        connectionTime: Duration
    ): TimeRange {
        val productivityCalendar = productivity.findFrom(startTime, Productivity<T>::timeWindow)
        var currentTime = max(startTime, productivityCalendar.first().timeWindow.start)
        var restAmount = quantity
        for (calendar in productivityCalendar) {
            currentTime = max(
                currentTime,
                calendar.timeWindow.start + connectionTime
            )
            val currentProductivity = calendar.capacityOf(material)
                ?.let { Flt64.one / timeWindow.valueOf(it) }
                ?: Flt64.zero
            val maxProduceTime = timeWindow.valueOf(calendar.timeWindow.end - currentTime)
            val maxProduceAmount = (maxProduceTime * currentProductivity).floor().toUInt64()
            if (maxProduceAmount >= restAmount) {
                val thisProduceTime = timeWindow.durationOf(restAmount.toFlt64() / currentProductivity)
                currentTime += thisProduceTime
                restAmount = UInt64.zero
                break
            } else {
                restAmount -= maxProduceAmount
            }
        }
        return if (restAmount == UInt64.zero) {
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

    override suspend fun actualTimeUntil(
        material: T,
        quantity: UInt64,
        endTime: Instant,
        connectionTime: Duration
    ): TimeRange {
        val productivityCalendar = productivity.findUntil(endTime, Productivity<T>::timeWindow).reversed()
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
            val maxProduceAmount = (maxProduceTime * currentProductivity).floor().toUInt64()
            if (maxProduceAmount >= restAmount) {
                val thisProduceTime = timeWindow.durationOf(restAmount.toFlt64() / currentProductivity)
                currentTime -= thisProduceTime
                restAmount = UInt64.zero
                break
            } else {
                restAmount -= maxProduceAmount
            }
        }
        return if (restAmount == UInt64.zero) {
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

    override suspend fun actualQuantity(
        material: T,
        time: TimeRange,
        connectionTime: Duration
    ): UInt64 {
        var amount = UInt64.zero
        val productivityCalendar = productivity.find(time, Productivity<T>::timeWindow)
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
            amount += (produceTime * currentProductivity).floor().toUInt64()
        }
        return amount
    }
}

open class ContinuousProductivityCalendar<P, T>(
    timeWindow: TimeWindow,
    productivity: List<P>
) : ProductivityCalendar<Flt64, P, T>(timeWindow, productivity) where P: Productivity<T> {
    override suspend fun actualTimeFrom(
        material: T,
        quantity: Flt64,
        startTime: Instant,
        connectionTime: Duration
    ): TimeRange {
        val productivityCalendar = productivity.findFrom(startTime, Productivity<T>::timeWindow)
        var currentTime = max(startTime, productivityCalendar.first().timeWindow.start)
        var restAmount = quantity
        for (calendar in productivityCalendar) {
            currentTime = max(
                currentTime,
                calendar.timeWindow.start + connectionTime
            )
            val currentProductivity = calendar.capacityOf(material)
                ?.let { Flt64.one / timeWindow.valueOf(it) }
                ?: Flt64.zero
            val maxProduceTime = timeWindow.valueOf(calendar.timeWindow.end - currentTime)
            val maxProduceAmount = maxProduceTime * currentProductivity
            if (maxProduceAmount geq restAmount) {
                val thisProduceTime = timeWindow.durationOf(restAmount / currentProductivity)
                currentTime += thisProduceTime
                restAmount = Flt64.zero
                break
            } else {
                restAmount -= maxProduceAmount
            }
        }
        return if (restAmount eq Flt64.zero) {
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

    override suspend fun actualTimeUntil(
        material: T,
        quantity: Flt64,
        endTime: Instant,
        connectionTime: Duration
    ): TimeRange {
        val productivityCalendar = productivity.findUntil(endTime, Productivity<T>::timeWindow).reversed()
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
            val maxProduceAmount = maxProduceTime * currentProductivity
            if (maxProduceAmount geq restAmount) {
                val thisProduceTime = timeWindow.durationOf(restAmount / currentProductivity)
                currentTime -= thisProduceTime
                restAmount = Flt64.zero
                break
            } else {
                restAmount -= maxProduceAmount
            }
        }
        return if (restAmount eq Flt64.zero) {
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

    override suspend fun actualQuantity(
        material: T,
        time: TimeRange,
        connectionTime: Duration
    ): Flt64 {
        var amount = Flt64.zero
        val productivityCalendar = productivity.find(time, Productivity<T>::timeWindow)
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
            amount += produceTime * currentProductivity
        }
        return amount
    }
}
