package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.math.*
import kotlin.time.*
import kotlinx.datetime.*
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
    companion object {
        @JvmStatic
        @JvmName("staticActualTime")
        protected fun actualTime(
            time: Instant,
            unavailableTimes: List<TimeRange> = emptyList(),
            connectionTime: Duration = Duration.ZERO
        ): Instant {
            var currentTime = time
            for (unavailableTime in unavailableTimes) {
                if (currentTime < unavailableTime.start) {
                    break
                }
                if (unavailableTime.contains(currentTime)) {
                    currentTime = unavailableTime.end + connectionTime
                }
            }
            return currentTime
        }

        @JvmStatic
        @JvmName("staticActualTime")
        protected fun actualTime(
            time: TimeRange,
            unavailableTimes: List<TimeRange> = emptyList(),
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

        @JvmStatic
        @JvmName("staticValidTime")
        protected fun validTime(
            time: TimeRange,
            unavailableTimes: List<TimeRange> = emptyList(),
            connectionTime: Duration = Duration.ZERO
        ): Duration {
            var currentTime = time.duration
            for (unavailableTime in unavailableTimes) {
                val intersectionTime = time.intersectionWith(unavailableTime)?.duration ?: Duration.ZERO
                if (intersectionTime != Duration.ZERO) {
                    currentTime -= min(
                        currentTime,
                        intersectionTime + connectionTime
                    )
                }
            }
            return currentTime
        }

        @JvmStatic
        @JvmName("staticValidTime")
        protected fun validTimes(
            time: TimeRange,
            unavailableTimes: List<TimeRange> = emptyList(),
            connectionTime: Duration = Duration.ZERO,
        ): List<TimeRange> {
            var currentTime = mutableListOf(time)
            for (unavailableTime in unavailableTimes) {
                currentTime = currentTime.flatMap { thisRestTime ->
                    val diff = thisRestTime - unavailableTime
                    diff.filter { thisRestTime.duration > connectionTime }
                }.toMutableList()
            }
            return currentTime
        }
    }

    open val unavailableTimes = unavailableTimes.sortedBy { it.start }

    fun actualTime(
        time: Instant,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): Instant {
        return WorkingCalendar.actualTime(
            time = time,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            connectionTime = connectionTime
        )
    }

    fun actualTime(
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): TimeRange {
        return WorkingCalendar.actualTime(
            time = time,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            connectionTime = connectionTime
        )
    }

    fun validTime(
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): Duration {
        return WorkingCalendar.validTime(
            time = time,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            connectionTime = connectionTime
        )
    }

    fun validTimes(
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO,
    ): List<TimeRange> {
        return WorkingCalendar.validTimes(
            time = time,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            connectionTime = connectionTime
        )
    }
}

typealias ProductivityCondition<T> = (T) -> Boolean

open class Productivity<T>(
    val timeWindow: TimeRange,
    val weekDays: Set<DayOfWeek> = emptySet(),
    val monthDays: Set<Int> = emptySet(),
    val capacities: Map<T, Duration>,
    val conditionCapacities: List<Pair<ProductivityCondition<T>, Duration>> = emptyList()
) {
    open fun capacityOf(material: T): Duration? {
        return capacities[material]
            ?: conditionCapacities.find { it.first(material) }?.second
    }

    open fun new(
        timeWindow: TimeRange? = null,
        weekDays: Set<DayOfWeek>? = null,
        monthDays: Set<Int>? = null,
        capacities: Map<T, Duration>? = null,
        conditionCapacities: List<Pair<ProductivityCondition<T>, Duration>>? = null
    ): Productivity<T> {
        return Productivity(
            timeWindow = timeWindow ?: this.timeWindow,
            weekDays = weekDays ?: this.weekDays,
            monthDays = monthDays ?: this.monthDays,
            capacities = capacities ?: this.capacities,
            conditionCapacities = conditionCapacities ?: this.conditionCapacities
        )
    }
}

sealed class ProductivityCalendar<Q, P, T>(
    timeWindow: TimeWindow,
    productivity: List<P>,
    unavailableTimes: List<TimeRange>? = null,
    private val constants: RealNumberConstants<Q>,
    private val floor: Extractor<Q, Flt64>
) : WorkingCalendar(timeWindow) where P : Productivity<T>, Q : RealNumber<Q>, Q : PlusGroup<Q> {
    companion object {
        operator fun <P, T> invoke(
            timeWindow: TimeWindow,
            productivity: List<P>,
            continuous: Boolean
        ): ProductivityCalendar<*, P, T> where P : Productivity<T> {
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
                timeRanges.map { time ->
                    it.new(
                        timeWindow = time
                    ) as P
                }
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
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): TimeRange {
        val productivityCalendar = productivity.findFrom(startTime, Productivity<T>::timeWindow)
        return actualTimeFrom(
            material = material,
            startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = unavailableTimes,
            connectionTime = connectionTime
        )
    }

    fun actualTimeFromOrNull(
        material: T,
        startTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): TimeRange? {
        val productivityCalendar = productivity.findFrom(startTime, Productivity<T>::timeWindow)
        return actualTimeFromOrNull(
            material = material,
            startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = unavailableTimes,
            connectionTime = connectionTime
        )
    }

    suspend fun actualTimeFromParallelly(
        material: T,
        startTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): TimeRange {
        val productivityCalendar = productivity.findFromParallelly(startTime, Productivity<T>::timeWindow)
        return actualTimeFrom(
            material = material,
            startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = unavailableTimes,
            connectionTime = connectionTime
        )
    }

    suspend fun actualTimeFromOrNullParallelly(
        material: T,
        startTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): TimeRange? {
        val productivityCalendar = productivity.findFromParallelly(startTime, Productivity<T>::timeWindow)
        return actualTimeFromOrNull(
            material = material,
            startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = unavailableTimes,
            connectionTime = connectionTime
        )
    }

    fun actualTimeUntil(
        material: T,
        endTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): TimeRange {
        val productivityCalendar = productivity.findUntil(endTime, Productivity<T>::timeWindow).reversed()
        return actualTimeUntil(
            material = material,
            endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = unavailableTimes,
            connectionTime = connectionTime
        )
    }

    fun actualTimeUntilOrNull(
        material: T,
        endTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): TimeRange? {
        val productivityCalendar = productivity.findUntil(endTime, Productivity<T>::timeWindow).reversed()
        return actualTimeUntilOrNull(
            material = material,
            endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = unavailableTimes,
            connectionTime = connectionTime
        )
    }

    suspend fun actualTimeUntilParallelly(
        material: T,
        endTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): TimeRange {
        val productivityCalendar = productivity.findUntilParallelly(endTime, Productivity<T>::timeWindow).reversed()
        return actualTimeUntil(
            material = material,
            endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = unavailableTimes,
            connectionTime = connectionTime
        )
    }

    suspend fun actualTimeUntilOrNullParallelly(
        material: T,
        endTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): TimeRange? {
        val productivityCalendar = productivity.findUntilParallelly(endTime, Productivity<T>::timeWindow).reversed()
        return actualTimeUntilOrNull(
            material = material,
            endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = unavailableTimes,
            connectionTime = connectionTime
        )
    }

    fun actualQuantity(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): Q {
        val productivityCalendar = productivity.find(time, Productivity<T>::timeWindow)
        return actualQuantity(
            material = material,
            time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = unavailableTimes,
            connectionTime = connectionTime
        )
    }

    fun actualQuantityOrNull(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): Q? {
        val productivityCalendar = productivity.find(time, Productivity<T>::timeWindow)
        return actualQuantityOrNull(
            material = material,
            time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = unavailableTimes,
            connectionTime = connectionTime
        )
    }

    suspend fun actualQuantityParallelly(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): Q {
        val productivityCalendar = productivity.findParallelly(time, Productivity<T>::timeWindow)
        return actualQuantity(
            material = material,
            time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = unavailableTimes,
            connectionTime = connectionTime
        )
    }

    suspend fun actualQuantityOrNullParallelly(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): Q? {
        val productivityCalendar = productivity.findParallelly(time, Productivity<T>::timeWindow)
        return actualQuantityOrNull(
            material = material,
            time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = unavailableTimes,
            connectionTime = connectionTime
        )
    }

    private fun actualTimeFrom(
        material: T,
        startTime: Instant,
        productivityCalendar: List<Productivity<T>>,
        quantity: Q,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): TimeRange {
        return actualTimeFromOrNull(
            material = material,
            startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = unavailableTimes,
            connectionTime = connectionTime
        ) ?: TimeRange(start = startTime)
    }

    private fun actualTimeFromOrNull(
        material: T,
        startTime: Instant,
        productivityCalendar: List<Productivity<T>>,
        quantity: Q,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): TimeRange? {
        if (productivityCalendar.isEmpty()) {
            return null
        }

        var currentTime = max(startTime, productivityCalendar.first().timeWindow.start)
        var restQuantity = quantity
        for (calendar in productivityCalendar) {
            // todo: calculate with dayOfWeek and dayOfMonth appointment
            currentTime = max(
                currentTime,
                calendar.timeWindow.start + connectionTime
            )
            val currentProductivity = calendar.capacityOf(material)
                ?.let { Flt64.one / with(timeWindow) { it.value } }
                ?: Flt64.zero
            val maxProduceTime = with(timeWindow) {
                WorkingCalendar.validTime(
                    TimeRange(currentTime, calendar.timeWindow.end),
                    unavailableTimes,
                    connectionTime
                ).value
            }
            val maxProduceQuantity = floor(maxProduceTime * currentProductivity)
            if (maxProduceQuantity geq restQuantity) {
                val thisProduceTime = with(timeWindow) { (restQuantity.toFlt64() / currentProductivity).duration }
                currentTime += thisProduceTime
                restQuantity = constants.zero
                break
            } else {
                restQuantity -= maxProduceQuantity
            }
        }
        return if (restQuantity eq constants.zero) {
            TimeRange(
                start = startTime,
                end = currentTime
            )
        } else {
            null
        }
    }

    private fun actualTimeUntil(
        material: T,
        endTime: Instant,
        productivityCalendar: List<Productivity<T>>,
        quantity: Q,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): TimeRange {
        return actualTimeUntilOrNull(
            material = material,
            endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = unavailableTimes,
            connectionTime = connectionTime
        ) ?: TimeRange(end = endTime)
    }

    private fun actualTimeUntilOrNull(
        material: T,
        endTime: Instant,
        productivityCalendar: List<Productivity<T>>,
        quantity: Q,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): TimeRange? {
        if (productivityCalendar.isEmpty()) {
            return null
        }

        var currentTime = min(endTime, productivityCalendar.first().timeWindow.end)
        var restAmount = quantity
        for (calendar in productivityCalendar) {
            // todo: calculate with dayOfWeek and dayOfMonth appointment
            currentTime = min(
                currentTime,
                calendar.timeWindow.end
            )
            val currentProductivity = calendar.capacityOf(material)
                ?.let { Flt64.one / with(timeWindow) { it.value } }
                ?: Flt64.zero
            val maxProduceTime = with(timeWindow) {
                WorkingCalendar.validTime(
                    TimeRange(calendar.timeWindow.end, currentTime + connectionTime),
                    unavailableTimes,
                    connectionTime
                ).value
            }
            val maxProduceAmount = floor(maxProduceTime * currentProductivity)
            if (maxProduceAmount >= restAmount) {
                val thisProduceTime = with(timeWindow) { (restAmount.toFlt64() / currentProductivity).duration }
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
            null
        }
    }

    private fun actualQuantity(
        material: T,
        time: TimeRange,
        productivityCalendar: List<Productivity<T>>,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): Q {
        return actualQuantityOrNull(
            material = material,
            time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = unavailableTimes,
            connectionTime = connectionTime
        ) ?: constants.zero
    }

    private fun actualQuantityOrNull(
        material: T,
        time: TimeRange,
        productivityCalendar: List<Productivity<T>>,
        unavailableTimes: List<TimeRange> = emptyList(),
        connectionTime: Duration = Duration.ZERO
    ): Q? {
        if (productivityCalendar.isEmpty()) {
            return null
        }

        var quantity = constants.zero
        for (calendar in productivityCalendar) {
            // todo: calculate with dayOfWeek and dayOfMonth appointment
            val intersection = time.intersectionWith(calendar.timeWindow) ?: continue
            val produceTime = with(timeWindow) {
                WorkingCalendar.validTime(
                    if (intersection.start == calendar.timeWindow.start) {
                        TimeRange(intersection.start + connectionTime, intersection.end)
                    } else {
                        intersection
                    },
                    unavailableTimes,
                    connectionTime
                ).value
            }
            val currentProductivity = calendar.capacityOf(material)
                ?.let { Flt64.one / with(timeWindow) { it.value } }
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
        where P : Productivity<T>

open class ContinuousProductivityCalendar<P, T>(
    timeWindow: TimeWindow,
    productivity: List<P>,
    unavailableTimes: List<TimeRange>? = null
) : ProductivityCalendar<Flt64, P, T>(timeWindow, productivity, unavailableTimes, Flt64, { it }) where P : Productivity<T>
