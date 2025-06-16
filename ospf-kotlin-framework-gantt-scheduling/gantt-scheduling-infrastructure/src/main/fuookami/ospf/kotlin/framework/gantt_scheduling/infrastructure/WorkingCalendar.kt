package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.functional.*

open class WorkingCalendar(
    val timeWindow: TimeWindow,
    unavailableTimes: List<TimeRange> = emptyList()
) {
    data class ActualTime(
        val time: TimeRange,
        val workingTimes: List<TimeRange>,
        val breakTimes: List<TimeRange>,
        val connectionTimes: List<TimeRange>
    ) {
        val finishEnabled: Boolean get() = time.start != Instant.DISTANT_PAST && time.end != Instant.DISTANT_FUTURE

        infix fun eq(time: TimeRange): Boolean {
            return this.time == time
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ActualTime) return false

            if (time != other.time) return false
            if (!(workingTimes.toTypedArray() contentEquals other.workingTimes.toTypedArray())) return false
            if (!(breakTimes.toTypedArray() contentEquals other.breakTimes.toTypedArray())) return false
            if (!(connectionTimes.toTypedArray() contentEquals other.connectionTimes.toTypedArray())) return false

            return true
        }

        override fun hashCode(): Int {
            var result = time.hashCode()
            result = 31 * result + workingTimes.hashCode()
            result = 31 * result + breakTimes.hashCode()
            result = 31 * result + connectionTimes.hashCode()
            return result
        }
    }

    data class ValidTimes(
        val times: List<TimeRange>,
        val breakTimes: List<TimeRange>,
        val connectionTimes: List<TimeRange>
    )

    companion object {
        protected fun actualTime(
            time: Instant,
            unavailableTimes: List<TimeRange> = emptyList(),
            beforeConnectionTime: Duration? = null,
            afterConnectionTime: Duration? = null,
            beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
            afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null
        ): Instant {
            val mergedTimes = unavailableTimes.merge()
            if (mergedTimes.isEmpty()) {
                return time
            }

            var currentTime = time
            for ((i, thisUnavailableTime) in mergedTimes.withIndex()) {
                if (time <= thisUnavailableTime.start - max(
                        beforeConditionalConnectionTime?.invoke(mergedTimes.first()) ?: Duration.ZERO,
                        beforeConnectionTime ?: Duration.ZERO
                )) {
                    return currentTime
                }

                currentTime = thisUnavailableTime.end + max(
                    afterConditionalConnectionTime?.invoke(mergedTimes[i]) ?: Duration.ZERO,
                    afterConnectionTime ?: Duration.ZERO
                )
            }
            return currentTime
        }

        @JvmStatic
        @JvmName("staticActualTime")
        protected fun actualTime(
            time: TimeRange,
            unavailableTimes: List<TimeRange> = emptyList(),
            beforeConnectionTime: Duration? = null,
            afterConnectionTime: Duration? = null,
            beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
            afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
            breakTime: Pair<Duration, Duration>? = null
        ): ActualTime {
            val mergedTimes = unavailableTimes.merge()
            return if (mergedTimes.isEmpty()) {
                if (breakTime != null) {
                    var currentTime = time.start
                    var workingDuration = Duration.ZERO
                    val workingTimes = ArrayList<TimeRange>()
                    val breakTimes = ArrayList<TimeRange>()
                    while (workingDuration != time.duration) {
                        val thisDuration = min(breakTime.first, time.duration - workingDuration)
                        workingTimes.add(TimeRange(currentTime, currentTime + thisDuration))
                        currentTime = if (thisDuration < breakTime.first && (workingDuration + thisDuration) != time.duration) {
                            currentTime + thisDuration
                        } else {
                            currentTime + thisDuration + breakTime.second
                        }
                        workingDuration += thisDuration
                    }
                    ActualTime(
                        time = TimeRange(
                            start = time.start,
                            end = currentTime
                        ),
                        workingTimes = workingTimes,
                        breakTimes = breakTimes,
                        connectionTimes = emptyList()
                    )
                } else {
                    ActualTime(
                        time = time,
                        workingTimes = emptyList(),
                        breakTimes = emptyList(),
                        connectionTimes = emptyList()
                    )
                }
            } else {
                var currentTime = time.start
                var workingDuration = Duration.ZERO
                val workingTimes = ArrayList<TimeRange>()
                val breakTimes = ArrayList<TimeRange>()
                val connectionTimes = ArrayList<TimeRange>()
                var i = mergedTimes.withIndex().indexOfLast {
                    currentTime >= it.value.end
                }
                while (workingDuration != time.duration) {
                    if (i == mergedTimes.size - 1 && currentTime == Instant.DISTANT_FUTURE) {
                        break
                    } else if (i != mergedTimes.size - 1 && currentTime in mergedTimes[i + 1]) {
                        currentTime = mergedTimes[i + 1].end
                        i += 1
                        continue
                    }

                    val thisBeforeConnectionTime = if (i < mergedTimes.size - 1) {
                        max(
                            beforeConditionalConnectionTime?.invoke(mergedTimes[i + 1]) ?: Duration.ZERO,
                            beforeConnectionTime ?: Duration.ZERO
                        )
                    } else {
                        null
                    }
                    val thisAfterConnectionTime = if (i != -1 && i != 0 && currentTime <= mergedTimes[i].end) {
                        max(
                            afterConditionalConnectionTime?.invoke(mergedTimes[i]) ?: Duration.ZERO,
                            afterConnectionTime ?: Duration.ZERO
                        )
                    } else {
                        null
                    }

                    currentTime = if (thisAfterConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = currentTime,
                                end = currentTime + thisAfterConnectionTime
                            )
                        )
                        currentTime + thisAfterConnectionTime
                    } else {
                        currentTime
                    }
                    val thisEndTime = if (i == mergedTimes.size - 1) {
                        Instant.DISTANT_FUTURE
                    } else {
                        mergedTimes[i + 1].start - (thisBeforeConnectionTime ?: Duration.ZERO)
                    }
                    while (workingDuration != time.duration && currentTime != thisEndTime) {
                        val restDuration = min(
                            time.duration - workingDuration,
                            thisEndTime - currentTime
                        )
                        val thisDuration = if (breakTime != null) {
                            min(
                                breakTime.first,
                                restDuration
                            )
                        } else {
                            restDuration
                        }
                        workingTimes.add(
                            TimeRange(
                                start = currentTime,
                                end = currentTime + thisDuration
                            )
                        )
                        currentTime = if (breakTime != null && thisDuration == breakTime.first && (currentTime + thisDuration) != thisEndTime) {
                            breakTimes.add(
                                TimeRange(
                                    start = currentTime + thisDuration,
                                    end = currentTime + thisDuration + breakTime.second
                                )
                            )
                            currentTime + thisDuration + breakTime.second
                        } else {
                            currentTime + thisDuration
                        }
                        workingDuration += thisDuration
                    }
                    if (workingDuration != time.duration && thisBeforeConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = currentTime,
                                end = currentTime + thisBeforeConnectionTime
                            )
                        )
                    } else if (workingDuration == time.duration) {
                        break
                    }
                    currentTime = mergedTimes[i + 1].end
                    i += 1
                }
                ActualTime(
                    time = TimeRange(
                        start = time.start,
                        end = currentTime
                    ),
                    workingTimes = workingTimes,
                    breakTimes = breakTimes,
                    connectionTimes = connectionTimes
                )
            }
        }

        @JvmStatic
        @JvmName("staticValidTime")
        protected fun validTimes(
            time: TimeRange,
            unavailableTimes: List<TimeRange> = emptyList(),
            beforeConnectionTime: Duration? = null,
            afterConnectionTime: Duration? = null,
            beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
            afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
            breakTime: Pair<Duration, Duration>? = null
        ): ValidTimes {
            val mergedTimes = unavailableTimes.merge()
            return if (mergedTimes.isEmpty()) {
                if (breakTime != null) {
                    val (validTimes, breakTimes) = time.split(breakTime.first, breakTime.second)
                    ValidTimes(
                        times = validTimes,
                        breakTimes = breakTimes,
                        connectionTimes = emptyList()
                    )
                } else {
                    ValidTimes(
                        times = listOf(time),
                        breakTimes = emptyList(),
                        connectionTimes = emptyList()
                    )
                }
            } else {
                val validTimes = ArrayList<TimeRange>()
                val breakTimes = ArrayList<TimeRange>()
                val connectionTimes = ArrayList<TimeRange>()
                var currentTime = time.start
                var i = mergedTimes.withIndex().indexOfLast {
                    currentTime >= it.value.end
                }
                while (currentTime != time.end) {
                    if (i == mergedTimes.size - 1 && currentTime == Instant.DISTANT_FUTURE) {
                        break
                    } else if (i != mergedTimes.size - 1 && currentTime in mergedTimes[i + 1]) {
                        currentTime = mergedTimes[i + 1].end
                        i += 1
                        continue
                    }

                    val thisBeforeConnectionTime = if (i < mergedTimes.size - 1) {
                        max(
                            beforeConditionalConnectionTime?.invoke(mergedTimes[i + 1]) ?: Duration.ZERO,
                            beforeConnectionTime ?: Duration.ZERO
                        )
                    } else {
                        null
                    }
                    val thisAfterConnectionTime = if (i != -1 && i != 0 && currentTime <= mergedTimes[i].end) {
                        max(
                            afterConditionalConnectionTime?.invoke(mergedTimes[i]) ?: Duration.ZERO,
                            afterConnectionTime ?: Duration.ZERO
                        )
                    } else {
                        null
                    }

                    currentTime = if (thisAfterConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = currentTime,
                                end = currentTime + thisAfterConnectionTime
                            )
                        )
                        currentTime + thisAfterConnectionTime
                    } else {
                        currentTime
                    }
                    val thisEndTime = if (i == mergedTimes.size - 1) {
                        time.end
                    } else {
                        min(
                            time.end,
                            mergedTimes[i + 1].start - (thisBeforeConnectionTime ?: Duration.ZERO)
                        )
                    }
                    val baseTime = TimeRange(
                        start = currentTime,
                        end = thisEndTime
                    )
                    if (breakTime != null) {
                        val (thisValidTimes, thisBreakTimes) = baseTime.split(breakTime.first, breakTime.second)
                        validTimes.addAll(thisValidTimes)
                        breakTimes.addAll(thisBreakTimes)
                    } else {
                        validTimes.add(baseTime)
                    }
                    if (thisEndTime != time.end && thisBeforeConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = currentTime,
                                end = currentTime + thisBeforeConnectionTime
                            )
                        )
                    } else if (thisEndTime == time.end) {
                        break
                    }
                    currentTime = mergedTimes[i + 1].end
                    i += 1
                }
                return ValidTimes(
                    times = validTimes,
                    breakTimes = breakTimes,
                    connectionTimes = connectionTimes
                )
            }
        }
    }

    open val unavailableTimes = unavailableTimes.sortedBy { it.start }

    fun actualTime(
        time: Instant,
        unavailableTimes: List<TimeRange> = emptyList(),
        afterConnectionTime: Duration? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null
    ): Instant {
        return WorkingCalendar.actualTime(
            time = time,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            afterConnectionTime = afterConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime
        )
    }

    fun actualTime(
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): ActualTime {
        return WorkingCalendar.actualTime(
            time = time,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    fun validTimes(
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): ValidTimes {
        return WorkingCalendar.validTimes(
            time = time,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
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
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): ActualTime {
        val productivityCalendar = productivity.findFrom(startTime, Productivity<T>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return ActualTime(
                time = TimeRange(
                    start = startTime,
                    end = Instant.DISTANT_FUTURE
                ),
                workingTimes = emptyList(),
                breakTimes = emptyList(),
                connectionTimes = emptyList()
            )
        }

        return actualTimeFrom(
            material = material,
            startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    fun actualTimeFromOrNull(
        material: T,
        startTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): ActualTime? {
        val productivityCalendar = productivity.findFrom(startTime, Productivity<T>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return null
        }

        return actualTimeFrom(
            material = material,
            startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    suspend fun actualTimeFromParallelly(
        material: T,
        startTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): ActualTime {
        val productivityCalendar = productivity.findFromParallelly(startTime, Productivity<T>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return ActualTime(
                time = TimeRange(
                    start = startTime,
                    end = Instant.DISTANT_FUTURE
                ),
                workingTimes = emptyList(),
                breakTimes = emptyList(),
                connectionTimes = emptyList()
            )
        }

        return actualTimeFrom(
            material = material,
            startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    suspend fun actualTimeFromOrNullParallelly(
        material: T,
        startTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): ActualTime? {
        val productivityCalendar = productivity.findFromParallelly(startTime, Productivity<T>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return null
        }

        return actualTimeFrom(
            material = material,
            startTime = startTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    fun actualTimeUntil(
        material: T,
        endTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): ActualTime {
        val productivityCalendar = productivity.findUntil(endTime, Productivity<T>::timeWindow).reversed()
        if (productivityCalendar.isEmpty()) {
            return ActualTime(
                time = TimeRange(
                    start = Instant.DISTANT_PAST,
                    end = endTime
                ),
                workingTimes = emptyList(),
                breakTimes = emptyList(),
                connectionTimes = emptyList()
            )
        }

        return actualTimeUntil(
            material = material,
            endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    fun actualTimeUntilOrNull(
        material: T,
        endTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): ActualTime? {
        val productivityCalendar = productivity.findUntil(endTime, Productivity<T>::timeWindow).reversed()
        if (productivityCalendar.isEmpty()) {
            return null
        }

        return actualTimeUntil(
            material = material,
            endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    suspend fun actualTimeUntilParallelly(
        material: T,
        endTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): ActualTime {
        val productivityCalendar = productivity.findUntilParallelly(endTime, Productivity<T>::timeWindow).reversed()
        if (productivityCalendar.isEmpty()) {
            return ActualTime(
                time = TimeRange(
                    start = Instant.DISTANT_PAST,
                    end = endTime
                ),
                workingTimes = emptyList(),
                breakTimes = emptyList(),
                connectionTimes = emptyList()
            )
        }

        return actualTimeUntil(
            material = material,
            endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    suspend fun actualTimeUntilOrNullParallelly(
        material: T,
        endTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): ActualTime? {
        val productivityCalendar = productivity.findUntilParallelly(endTime, Productivity<T>::timeWindow).reversed()
        if (productivityCalendar.isEmpty()) {
            return null
        }

        return actualTimeUntil(
            material = material,
            endTime = endTime,
            productivityCalendar = productivityCalendar,
            quantity = quantity,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    fun actualQuantity(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): Q {
        val productivityCalendar = productivity.find(time, Productivity<T>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return constants.zero
        }

        return actualQuantity(
            material = material,
            time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    fun actualQuantityOrNull(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): Q? {
        val productivityCalendar = productivity.find(time, Productivity<T>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return null
        }

        return actualQuantity(
            material = material,
            time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    suspend fun actualQuantityParallelly(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): Q {
        val productivityCalendar = productivity.findParallelly(time, Productivity<T>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return constants.zero
        }

        return actualQuantity(
            material = material,
            time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    suspend fun actualQuantityOrNullParallelly(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): Q? {
        val productivityCalendar = productivity.findParallelly(time, Productivity<T>::timeWindow)
        if (productivityCalendar.isEmpty()) {
            return null
        }

        return actualQuantity(
            material = material,
            time = time,
            productivityCalendar = productivityCalendar,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
    }

    private fun actualTimeFrom(
        material: T,
        startTime: Instant,
        productivityCalendar: List<Productivity<T>>,
        quantity: Q,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): ActualTime {
        val baseTime = TimeRange(
            start = startTime,
            end = Instant.DISTANT_FUTURE
        )
        val validTimes = validTimes(
            time = baseTime,
            unavailableTimes = unavailableTimes,
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        )
        if (validTimes.times.isEmpty()) {
            return ActualTime(
                time = baseTime,
                workingTimes = emptyList(),
                breakTimes = emptyList(),
                connectionTimes = emptyList()
            )
        }

        var produceQuantity = constants.zero
        var currentTime = validTimes.times.first().start
        val workingTimes = ArrayList<TimeRange>()
        for (validTime in validTimes.times) {
            var flag = false

            for (calendar in productivityCalendar) {
                if (validTime.end <= calendar.timeWindow.start) {
                    continue
                } else if (flag && validTime.start >= calendar.timeWindow.end) {
                    break
                }
                flag = true

                val produceTime = validTime.intersectionWith(calendar.timeWindow) ?: continue
                val currentProductivity = calendar.capacityOf(material)
                    ?.let { Flt64.one / with(timeWindow) { it.value } }
                    ?: continue
                val maxQuantity = with(timeWindow) {
                    floor(produceTime.duration.value * currentProductivity)
                }
                val thisQuantity = min(
                    quantity - produceQuantity,
                    maxQuantity
                )
                produceQuantity += thisQuantity
                if (thisQuantity eq maxQuantity) {
                    currentTime = max(currentTime, produceTime.end)
                    workingTimes.add(produceTime)
                } else {
                    val actualProduceTime = (thisQuantity.toFlt64() / maxQuantity.toFlt64()).toDouble() * produceTime.duration
                    currentTime = max(currentTime, produceTime.start + actualProduceTime)
                    workingTimes.add(
                        TimeRange(
                            start = produceTime.start,
                            end = produceTime.start + actualProduceTime
                        )
                    )
                }

                if (produceQuantity eq quantity) {
                    break
                }
            }

            if (produceQuantity eq quantity) {
                break
            }
        }

        return if (produceQuantity eq quantity) {
            val time = TimeRange(
                start = startTime,
                end = currentTime
            )
            ActualTime(
                time = time,
                workingTimes = workingTimes,
                breakTimes = validTimes.breakTimes.filter {
                    it.withIntersection(time)
                },
                connectionTimes = validTimes.connectionTimes.filter {
                    it.withIntersection(time)
                }
            )
        } else {
            ActualTime(
                time = baseTime,
                workingTimes = workingTimes,
                breakTimes = validTimes.breakTimes,
                connectionTimes = validTimes.connectionTimes
            )
        }
    }

    private fun actualTimeUntil(
        material: T,
        endTime: Instant,
        productivityCalendar: List<Productivity<T>>,
        quantity: Q,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): ActualTime {
        TODO("not implemented yet")
    }

    private fun actualQuantity(
        material: T,
        time: TimeRange,
        productivityCalendar: List<Productivity<T>>,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: Duration? = null,
        afterConnectionTime: Duration? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> Duration?)? = null,
        breakTime: Pair<Duration, Duration>? = null
    ): Q {
        val validTimes = validTimes(
            time = time,
            unavailableTimes = unavailableTimes,
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            breakTime = breakTime
        ).times

        var quantity = constants.zero
        for (calendar in productivityCalendar) {
            for (validTime in validTimes) {
                if (validTime.end <= calendar.timeWindow.start) {
                    continue
                } else if (validTime.start >= calendar.timeWindow.end) {
                    break
                }

                val produceTime = validTime.intersectionWith(calendar.timeWindow)?.duration ?: continue
                val currentProductivity = calendar.capacityOf(material)
                    ?.let { Flt64.one / with(timeWindow) { it.value } }
                    ?: Flt64.zero
                quantity += with(timeWindow) {
                    floor(produceTime.value * currentProductivity)
                }
            }
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
