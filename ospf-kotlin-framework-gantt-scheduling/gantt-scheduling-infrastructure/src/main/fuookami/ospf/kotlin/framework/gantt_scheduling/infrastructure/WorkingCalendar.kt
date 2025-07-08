package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.math.*
import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.min
import fuookami.ospf.kotlin.utils.max
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
        val duration: Duration get() = time.duration
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
            beforeConnectionTime: DurationRange? = null,
            afterConnectionTime: DurationRange? = null,
            beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
            afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null
        ): Instant {
            val mergedTimes = unavailableTimes.merge()
            if (mergedTimes.isEmpty()) {
                return time
            }

            var currentTime = time
            for (thisUnavailableTime in mergedTimes) {
                if (time <= thisUnavailableTime.start - max(
                        beforeConditionalConnectionTime?.invoke(thisUnavailableTime)?.lb ?: Duration.ZERO,
                        beforeConnectionTime?.lb ?: Duration.ZERO
                    )
                ) {
                    return currentTime
                }

                currentTime = max(
                    currentTime,
                    thisUnavailableTime.end + max(
                        afterConditionalConnectionTime?.invoke(thisUnavailableTime)?.lb ?: Duration.ZERO,
                        afterConnectionTime?.lb ?: Duration.ZERO
                    )
                )
            }
            return currentTime
        }

        @JvmStatic
        @JvmName("staticActualTime")
        protected fun actualTime(
            time: TimeRange,
            unavailableTimes: List<TimeRange> = emptyList(),
            beforeConnectionTime: DurationRange? = null,
            afterConnectionTime: DurationRange? = null,
            beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
            afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
            currentDuration: Duration = Duration.ZERO,
            breakTime: Pair<DurationRange, Duration>? = null
        ): ActualTime {
            val mergedTimes = unavailableTimes.merge()
            return if (mergedTimes.isEmpty()) {
                if (breakTime != null) {
                    var currentTime = time.start
                    var totalDuration = Duration.ZERO
                    val workingTimes = ArrayList<TimeRange>()
                    val breakTimes = ArrayList<TimeRange>()
                    while (totalDuration != time.duration) {
                        val thisDuration = min(
                            breakTime.first.lb,
                            time.duration - totalDuration
                        )
                        workingTimes.add(
                            TimeRange(
                                start = currentTime,
                                end = currentTime + thisDuration
                            )
                        )
                        currentTime = if (thisDuration < breakTime.first.ub && (totalDuration + thisDuration) != time.duration) {
                            currentTime + thisDuration
                        } else {
                            breakTimes.add(
                                TimeRange(
                                    start = currentTime + thisDuration,
                                    end = currentTime + thisDuration + breakTime.second
                                )
                            )
                            currentTime + thisDuration + breakTime.second
                        }
                        totalDuration += thisDuration
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
                var totalDuration = Duration.ZERO
                val workingTimes = ArrayList<TimeRange>()
                val breakTimes = ArrayList<TimeRange>()
                val connectionTimes = ArrayList<TimeRange>()
                var i = mergedTimes.withIndex().indexOfLast {
                    currentTime >= it.value.end
                }
                while (totalDuration != time.duration) {
                    if (i == mergedTimes.lastIndex && currentTime == Instant.DISTANT_FUTURE) {
                        break
                    } else if (i != mergedTimes.lastIndex && currentTime in mergedTimes[i + 1]) {
                        currentTime = mergedTimes[i + 1].end
                        i += 1
                        continue
                    }

                    val thisBeforeConnectionTime = if (i < mergedTimes.lastIndex && (beforeConnectionTime != null || beforeConditionalConnectionTime != null)) {
                        DurationRange(
                            max(
                                beforeConditionalConnectionTime?.invoke(mergedTimes[i + 1])?.lb ?: Duration.ZERO,
                                beforeConnectionTime?.lb ?: Duration.ZERO
                            ),
                            max(
                                beforeConditionalConnectionTime?.invoke(mergedTimes[i + 1])?.ub ?: Duration.ZERO,
                                beforeConnectionTime?.ub ?: Duration.ZERO
                            )
                        )
                    } else {
                        null
                    }
                    val thisAfterConnectionTime = if (i != -1 && i != 0 && currentTime <= mergedTimes[i].end && (afterConnectionTime != null || afterConditionalConnectionTime != null)) {
                        DurationRange(
                            max(
                                afterConditionalConnectionTime?.invoke(mergedTimes[i])?.lb ?: Duration.ZERO,
                                afterConnectionTime?.lb ?: Duration.ZERO
                            ),
                            max(
                                afterConditionalConnectionTime?.invoke(mergedTimes[i])?.ub ?: Duration.ZERO,
                                afterConnectionTime?.ub ?: Duration.ZERO
                            )
                        )
                    } else {
                        null
                    }

                    val thisMaxDuration = if (i == -1 && thisBeforeConnectionTime != null) {
                        DurationRange(
                            mergedTimes.first().start - time.start - thisBeforeConnectionTime.ub,
                            mergedTimes.first().start - time.start - thisBeforeConnectionTime.lb
                        )
                    } else if (i != mergedTimes.lastIndex && (thisBeforeConnectionTime != null || thisAfterConnectionTime != null)) {
                        DurationRange(
                            mergedTimes[i + 1].start - mergedTimes[i].end - (thisBeforeConnectionTime?.ub ?: Duration.ZERO) - (thisAfterConnectionTime?.ub ?: Duration.ZERO),
                            mergedTimes[i + 1].start - mergedTimes[i].end - (thisBeforeConnectionTime?.lb ?: Duration.ZERO) - (thisAfterConnectionTime?.lb ?: Duration.ZERO)
                        )
                    } else {
                        null
                    }
                    val (thisActualBeforeConnectionTime, thisActualAfterConnectionTime) = if (thisMaxDuration != null
                        && (totalDuration + thisMaxDuration.lb) <= time.duration
                        && (totalDuration + thisMaxDuration.ub) >= time.duration
                    ) {
                        val restDuration = thisMaxDuration.ub - (time.duration - totalDuration)
                        if (thisBeforeConnectionTime != null && thisAfterConnectionTime != null) {
                            if (restDuration >= (thisBeforeConnectionTime.ub - thisBeforeConnectionTime.lb)) {
                                thisBeforeConnectionTime.ub to (thisAfterConnectionTime.lb + restDuration)
                            } else {
                                (thisBeforeConnectionTime.lb + restDuration) to thisAfterConnectionTime.lb
                            }
                        } else if (thisBeforeConnectionTime != null) {
                            (thisBeforeConnectionTime.lb + restDuration) to null
                        } else if (thisAfterConnectionTime != null) {
                            null to (thisAfterConnectionTime.lb + restDuration)
                        } else {
                            null to null
                        }
                    } else {
                        thisBeforeConnectionTime?.ub to thisAfterConnectionTime?.ub
                    }

                    currentTime = if (thisActualAfterConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = currentTime,
                                end = currentTime + thisActualAfterConnectionTime
                            )
                        )
                        currentTime + thisActualAfterConnectionTime
                    } else {
                        currentTime
                    }
                    val thisEndTime = if (i == mergedTimes.lastIndex) {
                        Instant.DISTANT_FUTURE
                    } else {
                        mergedTimes[i + 1].start - (thisActualBeforeConnectionTime ?: Duration.ZERO)
                    }
                    val baseTime = TimeRange(
                        start = currentTime,
                        end = thisEndTime
                    )
                    currentTime = if (breakTime != null) {
                        val offset = if (currentTime == time.start) { currentDuration } else { Duration.ZERO }
                        val (thisValidTimes, thisBreakTimes) = baseTime.split(
                            unit = breakTime.first,
                            currentDuration = offset,
                            maxDuration = time.duration - totalDuration,
                            breakTime = breakTime.second
                        )
                        for (validTime in thisValidTimes) {
                            totalDuration += validTime.duration
                        }
                        workingTimes.addAll(thisValidTimes)
                        breakTimes.addAll(thisBreakTimes)
                        (thisValidTimes.map { it.end } + thisBreakTimes.map { it.end }).max()
                    } else {
                        val duration = min(
                            baseTime.duration,
                            time.duration - totalDuration
                        )
                        totalDuration += duration
                        workingTimes.add(
                            TimeRange(
                                start = currentTime,
                                end = currentTime + duration
                            )
                        )
                        currentTime + duration
                    }
                    if (totalDuration != time.duration && thisActualBeforeConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = thisEndTime,
                                end = thisEndTime + thisActualBeforeConnectionTime
                            )
                        )
                    } else if (totalDuration == time.duration) {
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
            beforeConnectionTime: DurationRange? = null,
            afterConnectionTime: DurationRange? = null,
            beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
            afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
            currentDuration: Duration = Duration.ZERO,
            maxDuration: Duration? = null,
            breakTime: Pair<DurationRange, Duration>? = null
        ): ValidTimes {
            val mergedTimes = unavailableTimes.merge()
            return if (mergedTimes.isEmpty()) {
                if (breakTime != null) {
                    val (validTimes, breakTimes) = time.split(
                        unit = breakTime.first,
                        maxDuration = maxDuration,
                        breakTime = breakTime.second
                    )
                    ValidTimes(
                        times = validTimes,
                        breakTimes = breakTimes,
                        connectionTimes = emptyList()
                    )
                } else if (maxDuration != null) {
                    ValidTimes(
                        times = listOf(
                            TimeRange(
                                start = time.start,
                                end = time.start + maxDuration
                            )
                        ),
                        breakTimes = emptyList(),
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
                var totalDuration = Duration.ZERO
                while (currentTime < time.end) {
                    if (i == mergedTimes.lastIndex && currentTime == Instant.DISTANT_FUTURE) {
                        break
                    } else if (i != mergedTimes.lastIndex && mergedTimes[i + 1].contains(currentTime)) {
                        currentTime = mergedTimes[i + 1].end
                        i += 1
                        continue
                    }

                    val thisBeforeConnectionTime = if (i != mergedTimes.lastIndex
                        && (beforeConnectionTime != null || beforeConditionalConnectionTime != null)
                    ) {
                        DurationRange(
                            max(
                                beforeConditionalConnectionTime?.invoke(mergedTimes[i + 1])?.lb ?: Duration.ZERO,
                                beforeConnectionTime?.lb ?: Duration.ZERO
                            ),
                            max(
                                beforeConditionalConnectionTime?.invoke(mergedTimes[i + 1])?.ub ?: Duration.ZERO,
                                beforeConnectionTime?.ub ?: Duration.ZERO
                            )
                        )
                    } else {
                        null
                    }

                    val thisAfterConnectionTime = if (i != -1
                        && currentTime <= mergedTimes[i].end
                        && (afterConnectionTime != null || afterConditionalConnectionTime != null)
                    ) {
                        DurationRange(
                            max(
                                afterConditionalConnectionTime?.invoke(mergedTimes[i])?.lb ?: Duration.ZERO,
                                afterConnectionTime?.lb ?: Duration.ZERO
                            ),
                            max(
                                afterConditionalConnectionTime?.invoke(mergedTimes[i])?.ub ?: Duration.ZERO,
                                afterConnectionTime?.ub ?: Duration.ZERO
                            )
                        )
                    } else {
                        null
                    }

                    val thisMaxDuration = if (i == -1
                        && thisBeforeConnectionTime != null
                    ) {
                        DurationRange(
                            mergedTimes.first().start - time.start - thisBeforeConnectionTime.ub,
                            mergedTimes.first().start - time.start - thisBeforeConnectionTime.lb
                        )
                    } else if (i != mergedTimes.lastIndex
                        && (i == -1 || currentTime != mergedTimes[i].end)
                        && thisBeforeConnectionTime != null
                    ) {
                        DurationRange(
                            mergedTimes[i + 1].start
                                    - currentTime
                                    - thisBeforeConnectionTime.ub,
                            mergedTimes[i + 1].start
                                    - currentTime
                                    - thisBeforeConnectionTime.lb
                        )
                    } else if (i != -1
                        && i != mergedTimes.lastIndex
                        && currentTime == mergedTimes[i].end
                        && (thisBeforeConnectionTime != null || thisAfterConnectionTime != null)
                    ) {
                        DurationRange(
                            mergedTimes[i + 1].start
                                    - mergedTimes[i].end
                                    - (thisBeforeConnectionTime?.ub ?: Duration.ZERO)
                                    - (thisAfterConnectionTime?.ub ?: Duration.ZERO),
                            mergedTimes[i + 1].start
                                    - mergedTimes[i].end
                                    - (thisBeforeConnectionTime?.lb ?: Duration.ZERO)
                                    - (thisAfterConnectionTime?.lb ?: Duration.ZERO)
                        )
                    } else {
                        null
                    }

                    if (thisMaxDuration?.let { it.lb < Duration.ZERO } == true) {
                        if (i == -1) {
                            connectionTimes.add(
                                TimeRange(
                                    start = time.start,
                                    end = mergedTimes.first().start
                                )
                            )
                        } else if (currentTime != mergedTimes[i].end && thisBeforeConnectionTime != null) {
                            connectionTimes.add(
                                TimeRange(
                                    start = max(currentTime, mergedTimes[i + 1].start - thisBeforeConnectionTime.ub),
                                    end = mergedTimes[i + 1].start
                                )
                            )
                        } else if (currentTime == mergedTimes[i].end) {
                            if (thisBeforeConnectionTime != null && thisAfterConnectionTime != null) {
                                connectionTimes.add(
                                    TimeRange(
                                        start = mergedTimes[i].end,
                                        end = mergedTimes[i].end + thisAfterConnectionTime.lb
                                    )
                                )
                                connectionTimes.add(
                                    TimeRange(
                                        start = mergedTimes[i + 1].start - (mergedTimes[i + 1].start - mergedTimes[i].end - thisBeforeConnectionTime.lb),
                                        end = mergedTimes[i + 1].start
                                    )
                                )
                            } else {
                                connectionTimes.add(
                                    TimeRange(
                                        start = mergedTimes[i].end,
                                        end = mergedTimes[i + 1].start
                                    )
                                )
                            }
                        }
                        currentTime = mergedTimes[i + 1].end
                        i += 1
                        continue
                    }

                    val (thisActualBeforeConnectionTime, thisActualAfterConnectionTime) = if (maxDuration != null
                        && thisMaxDuration != null
                        && (totalDuration + thisMaxDuration.lb) <= maxDuration
                        && (totalDuration + thisMaxDuration.ub) >= maxDuration
                    ) {
                        val restDuration = thisMaxDuration.ub - (maxDuration - totalDuration)
                        if (thisBeforeConnectionTime != null && thisAfterConnectionTime != null) {
                            if (restDuration >= (thisBeforeConnectionTime.ub - thisBeforeConnectionTime.lb)) {
                                thisBeforeConnectionTime.ub to (thisAfterConnectionTime.lb + restDuration)
                            } else {
                                (thisBeforeConnectionTime.lb + restDuration) to thisAfterConnectionTime.lb
                            }
                        } else if (thisBeforeConnectionTime != null) {
                            (thisBeforeConnectionTime.lb + restDuration) to null
                        } else if (thisAfterConnectionTime != null) {
                            null to (thisAfterConnectionTime.lb + restDuration)
                        } else {
                            null to null
                        }
                    } else {
                        thisBeforeConnectionTime?.ub to thisAfterConnectionTime?.ub
                    }

                    currentTime = if (thisActualAfterConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = currentTime,
                                end = currentTime + thisActualAfterConnectionTime
                            )
                        )
                        currentTime + thisActualAfterConnectionTime
                    } else {
                        currentTime
                    }
                    val thisEndTime = if (i == mergedTimes.lastIndex) {
                        listOf(
                            time.end,
                            currentTime + (maxDuration ?: Duration.INFINITE) + if (maxDuration != null && breakTime != null) {
                                ceil(maxDuration / breakTime.first.lb) * breakTime.second
                            } else {
                                Duration.ZERO
                            }
                        )
                    } else {
                        listOf(
                            time.end,
                            mergedTimes[i + 1].start - (thisActualBeforeConnectionTime ?: Duration.ZERO),
                            currentTime + (maxDuration ?: Duration.INFINITE) + if (maxDuration != null && breakTime != null) {
                                ceil(maxDuration / breakTime.first.lb) * breakTime.second
                            } else {
                                Duration.ZERO
                            }
                        )
                    }.min()
                    val baseTime = TimeRange(
                        start = currentTime,
                        end = thisEndTime
                    )
                    if (breakTime != null) {
                        val offset = if (currentTime == time.start) { currentDuration } else { Duration.ZERO }
                        val (thisValidTimes, thisBreakTimes) = baseTime.split(
                            unit = breakTime.first,
                            currentDuration = offset,
                            maxDuration = maxDuration?.let { it - totalDuration },
                            breakTime = breakTime.second
                        )
                        for (validTime in thisValidTimes) {
                            totalDuration += validTime.duration
                        }
                        validTimes.addAll(thisValidTimes)
                        breakTimes.addAll(thisBreakTimes)
                    } else {
                        totalDuration += baseTime.duration
                        validTimes.add(baseTime)
                    }
                    if (thisEndTime != time.end && totalDuration != maxDuration && thisActualBeforeConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = thisEndTime,
                                end = thisEndTime + thisActualBeforeConnectionTime
                            )
                        )
                    } else if (thisEndTime == time.end || totalDuration == maxDuration) {
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

        @JvmStatic
        @JvmName("staticRValidTime")
        protected fun reversedValidTimes(
            time: TimeRange,
            unavailableTimes: List<TimeRange> = emptyList(),
            beforeConnectionTime: DurationRange? = null,
            afterConnectionTime: DurationRange? = null,
            beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
            afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
            maxDuration: Duration? = null,
            breakTime: Pair<DurationRange, Duration>? = null
        ): ValidTimes {
            val mergedTimes = unavailableTimes.merge()
            return if (mergedTimes.isEmpty()) {
                if (breakTime != null) {
                    val (validTimes, breakTimes) = time.rsplit(
                        unit = breakTime.first,
                        maxDuration = maxDuration,
                        breakTime = breakTime.second
                    )
                    ValidTimes(
                        times = validTimes,
                        breakTimes = breakTimes,
                        connectionTimes = emptyList()
                    )
                } else if (maxDuration != null) {
                    ValidTimes(
                        times = listOf(
                            TimeRange(
                                start = time.end - maxDuration,
                                end = time.end
                            )
                        ),
                        breakTimes = emptyList(),
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
                var currentTime = time.end
                var i = mergedTimes.withIndex().indexOfFirst {
                    currentTime <= it.value.start
                }
                if (i == -1) {
                    i = mergedTimes.size
                }
                var totalDuration = Duration.ZERO
                while (currentTime > time.start) {
                    if (i == 0 && currentTime == Instant.DISTANT_PAST) {
                        break
                    } else if (i != 0 && mergedTimes[i - 1].contains(currentTime)) {
                        currentTime = mergedTimes[i - 1].start
                        i -= 1
                        continue
                    }

                    val thisBeforeConnectionTime = if (i != mergedTimes.size) {
                        DurationRange(
                            max(
                                beforeConditionalConnectionTime?.invoke(mergedTimes[i])?.lb ?: Duration.ZERO,
                                beforeConnectionTime?.lb ?: Duration.ZERO
                            ),
                            max(
                                beforeConditionalConnectionTime?.invoke(mergedTimes[i])?.ub ?: Duration.ZERO,
                                beforeConnectionTime?.ub ?: Duration.ZERO
                            )
                        )
                    } else {
                        null
                    }

                    val thisAfterConnectionTime = if (i != 0 && currentTime >= mergedTimes[i - 1].end) {
                        DurationRange(
                            max(
                                afterConditionalConnectionTime?.invoke(mergedTimes[i - 1])?.lb ?: Duration.ZERO,
                                afterConnectionTime?.lb ?: Duration.ZERO
                            ),
                            max(
                                afterConditionalConnectionTime?.invoke(mergedTimes[i - 1])?.ub ?: Duration.ZERO,
                                afterConnectionTime?.ub ?: Duration.ZERO
                            )
                        )
                    } else {
                        null
                    }

                    val thisMaxDuration = if (i == mergedTimes.size
                        && thisAfterConnectionTime != null
                    ) {
                        DurationRange(
                            time.end
                                    - mergedTimes.last().end
                                    - thisAfterConnectionTime.ub,
                            time.end
                                    - mergedTimes.last().end
                                    - thisAfterConnectionTime.lb
                        )
                    } else if (i != 0
                        && i != mergedTimes.size
                        && (thisBeforeConnectionTime != null || thisAfterConnectionTime != null)
                    ) {
                        DurationRange(
                            min(currentTime, mergedTimes[i].start)
                                    - mergedTimes[i - 1].end
                                    - (thisBeforeConnectionTime?.ub ?: Duration.ZERO)
                                    - (thisAfterConnectionTime?.ub ?: Duration.ZERO),
                            min(currentTime, mergedTimes[i].start)
                                    - mergedTimes[i - 1].end
                                    - (thisBeforeConnectionTime?.lb ?: Duration.ZERO)
                                    - (thisAfterConnectionTime?.lb ?: Duration.ZERO)
                        )
                    } else {
                        null
                    }

                    if (thisMaxDuration?.let { it.lb < Duration.ZERO } == true) {
                        if (i == mergedTimes.size) {
                            connectionTimes.add(
                                TimeRange(
                                    start = mergedTimes.last().end,
                                    end = time.end
                                )
                            )
                        } else if (currentTime != mergedTimes[i].start && thisBeforeConnectionTime != null) {
                            connectionTimes.add(
                                TimeRange(
                                    start = max(currentTime, mergedTimes[i].start - thisBeforeConnectionTime.ub),
                                    end = mergedTimes[i].start
                                )
                            )
                        } else if (i != 0 && currentTime == mergedTimes[i].start) {
                            if (thisBeforeConnectionTime != null && thisAfterConnectionTime != null) {
                                connectionTimes.add(
                                    TimeRange(
                                        start = mergedTimes[i - 1].end,
                                        end = mergedTimes[i - 1].end + thisAfterConnectionTime.lb
                                    )
                                )
                                connectionTimes.add(
                                    TimeRange(
                                        start = mergedTimes[i].start - (min(currentTime, mergedTimes[i].start) - mergedTimes[i - 1].end - thisAfterConnectionTime.lb),
                                        end = mergedTimes[i].start
                                    )
                                )
                            } else {
                                connectionTimes.add(
                                    TimeRange(
                                        start = mergedTimes[i - 1].end,
                                        end = min(currentTime, mergedTimes[i].start)
                                    )
                                )
                            }
                        }
                        currentTime = mergedTimes[i - 1].start
                        i -= 1
                        continue
                    }

                    val (thisActualBeforeConnectionTime, thisActualAfterConnectionTime) = if (maxDuration != null
                        && thisMaxDuration != null
                        && (totalDuration + thisMaxDuration.lb) <= maxDuration
                        && (totalDuration + thisMaxDuration.ub) >= maxDuration
                    ) {
                        val restDuration = thisMaxDuration.ub - (maxDuration - totalDuration)
                        if (thisBeforeConnectionTime != null && thisAfterConnectionTime != null) {
                            if (restDuration >= (thisBeforeConnectionTime.ub - thisBeforeConnectionTime.lb)) {
                                thisBeforeConnectionTime.ub to (thisAfterConnectionTime.lb + restDuration)
                            } else {
                                (thisBeforeConnectionTime.lb + restDuration) to thisAfterConnectionTime.lb
                            }
                        } else if (thisBeforeConnectionTime != null) {
                            (thisBeforeConnectionTime.lb + restDuration) to null
                        } else {
                            null to restDuration
                        }
                    } else {
                        thisBeforeConnectionTime?.ub to thisAfterConnectionTime?.ub
                    }

                    currentTime = if (thisActualBeforeConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = currentTime - thisActualBeforeConnectionTime,
                                end = currentTime
                            )
                        )
                        currentTime - thisActualBeforeConnectionTime
                    } else {
                        currentTime
                    }
                    val thisStartTime = if (i == 0) {
                        listOf(
                            time.start,
                            currentTime - (maxDuration ?: Duration.INFINITE) - if (maxDuration != null && breakTime != null) {
                                ceil(maxDuration / breakTime.first.lb) * breakTime.second
                            } else {
                                Duration.ZERO
                            }
                        )
                    } else {
                        listOf(
                            time.start,
                            mergedTimes[i - 1].end + (thisActualAfterConnectionTime ?: Duration.ZERO),
                            currentTime - (maxDuration ?: Duration.INFINITE) - if (maxDuration != null && breakTime != null) {
                                ceil(maxDuration / breakTime.first.lb) * breakTime.second
                            } else {
                                Duration.ZERO
                            }
                        )
                    }.max()
                    val baseTime = TimeRange(
                        start = thisStartTime,
                        end = currentTime
                    )
                    if (breakTime != null) {
                        val (thisValidTimes, thisBreakTimes) = baseTime.rsplit(
                            unit = breakTime.first,
                            maxDuration = maxDuration?.let { it - totalDuration },
                            breakTime = breakTime.second
                        )
                        for (validTime in thisValidTimes) {
                            totalDuration += validTime.duration
                        }
                        validTimes.addAll(thisValidTimes)
                        breakTimes.addAll(thisBreakTimes)
                    } else {
                        totalDuration += baseTime.duration
                        validTimes.add(baseTime)
                    }

                    if (thisStartTime != time.start && totalDuration != maxDuration && thisActualAfterConnectionTime?.let { it > Duration.ZERO } == true) {
                        connectionTimes.add(
                            TimeRange(
                                start = thisStartTime - thisActualAfterConnectionTime,
                                end = thisStartTime
                            )
                        )
                    } else if (thisStartTime == time.start || totalDuration == maxDuration) {
                        break
                    }
                    currentTime = mergedTimes[i - 1].start
                    i -= 1
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
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null
    ): Instant {
        return WorkingCalendar.actualTime(
            time = time,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime
        )
    }

    fun actualTime(
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        return WorkingCalendar.actualTime(
            time = time,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    fun validTimes(
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ValidTimes {
        return WorkingCalendar.validTimes(
            time = time,
            unavailableTimes = (unavailableTimes + this.unavailableTimes).merge(),
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration,
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
                if (i == productivity.lastIndex) {
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
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
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
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    fun actualTimeFromOrNull(
        material: T,
        startTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
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
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    suspend fun actualTimeFromParallelly(
        material: T,
        startTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
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
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    suspend fun actualTimeFromOrNullParallelly(
        material: T,
        startTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
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
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    fun actualTimeUntil(
        material: T,
        endTime: Instant,
        quantity: Q = constants.one,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        breakTime: Pair<DurationRange, Duration>? = null
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
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        breakTime: Pair<DurationRange, Duration>? = null
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
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        breakTime: Pair<DurationRange, Duration>? = null
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
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        breakTime: Pair<DurationRange, Duration>? = null
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
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
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
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    fun actualQuantityOrNull(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
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
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    suspend fun actualQuantityParallelly(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
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
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    suspend fun actualQuantityOrNullParallelly(
        material: T,
        time: TimeRange,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
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
            currentDuration = currentDuration,
            breakTime = breakTime
        )
    }

    private fun actualTimeFrom(
        material: T,
        startTime: Instant,
        productivityCalendar: List<Productivity<T>>,
        quantity: Q,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        var produceQuantity = Flt64.zero
        var currentTime = startTime
        val workingTimes = ArrayList<TimeRange>()
        val breakTimes = ArrayList<TimeRange>()
        val connectionTimes = ArrayList<TimeRange>()
        for (calendar in productivityCalendar) {
            val currentProductivity = calendar.capacityOf(material)
                ?.let {
                    Flt64.one / with(timeWindow) {
                        it.value
                    }
                }
                ?: continue
            val maxDuration = with(timeWindow) {
                durationOf((quantity.toFlt64() - produceQuantity) / currentProductivity).ceil
            }

            val validTimes = validTimes(
                time = calendar.timeWindow.intersectionWith(TimeRange(start = currentTime)) ?: continue,
                unavailableTimes = unavailableTimes,
                beforeConnectionTime = beforeConnectionTime,
                afterConnectionTime = afterConnectionTime,
                beforeConditionalConnectionTime = beforeConditionalConnectionTime,
                afterConditionalConnectionTime = afterConditionalConnectionTime,
                currentDuration = if (currentTime == startTime) { currentDuration } else { Duration.ZERO },
                maxDuration = maxDuration,
                breakTime = breakTime
            )
            workingTimes.addAll(validTimes.times)
            breakTimes.addAll(validTimes.breakTimes)
            connectionTimes.addAll(validTimes.connectionTimes)
            for (produceTime in validTimes.times) {
                val thisQuantity = with(timeWindow) {
                    produceTime.duration.value * currentProductivity
                }
                produceQuantity += min(
                    quantity.toFlt64() - produceQuantity,
                    thisQuantity.toFlt64()
                )
            }
            currentTime = (
                    validTimes.times.map { it.end } +
                    validTimes.breakTimes.map { it.end } +
                    validTimes.connectionTimes.map { it.end }
            ).max()

            if (produceQuantity eq quantity.toFlt64()) {
                break
            }
        }

        return if (produceQuantity eq quantity.toFlt64()) {
            ActualTime(
                time = TimeRange(
                    start = startTime,
                    end = currentTime
                ),
                workingTimes = workingTimes,
                breakTimes = breakTimes,
                connectionTimes = connectionTimes
            )
        } else {
            ActualTime(
                time = TimeRange(
                    start = startTime,
                    end = Instant.DISTANT_FUTURE
                ),
                workingTimes = workingTimes,
                breakTimes = breakTimes,
                connectionTimes = connectionTimes
            )
        }
    }

    private fun actualTimeUntil(
        material: T,
        endTime: Instant,
        productivityCalendar: List<Productivity<T>>,
        quantity: Q,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        breakTime: Pair<DurationRange, Duration>? = null
    ): ActualTime {
        var produceQuantity = Flt64.zero
        var currentTime = endTime
        val workingTimes = ArrayList<TimeRange>()
        val breakTimes = ArrayList<TimeRange>()
        val connectionTimes = ArrayList<TimeRange>()
        for (calendar in productivityCalendar) {
            val currentProductivity = calendar.capacityOf(material)
                ?.let {
                    Flt64.one / with(timeWindow) {
                        it.value
                    }
                }
                ?: continue
            val maxDuration = with(timeWindow) {
                durationOf((quantity.toFlt64() - produceQuantity) / currentProductivity).ceil
            }

            val validTimes = reversedValidTimes(
                time = calendar.timeWindow.intersectionWith(TimeRange(end = currentTime)) ?: continue,
                unavailableTimes = unavailableTimes,
                beforeConnectionTime = beforeConnectionTime,
                afterConnectionTime = afterConnectionTime,
                beforeConditionalConnectionTime = beforeConditionalConnectionTime,
                afterConditionalConnectionTime = afterConditionalConnectionTime,
                maxDuration = maxDuration,
                breakTime = breakTime
            )
            workingTimes.addAll(validTimes.times)
            breakTimes.addAll(validTimes.breakTimes)
            connectionTimes.addAll(validTimes.connectionTimes)
            for (produceTime in validTimes.times) {
                val thisQuantity = with(timeWindow) {
                    produceTime.duration.value * currentProductivity
                }
                produceQuantity += min(
                    quantity.toFlt64() - produceQuantity,
                    thisQuantity.toFlt64()
                )
            }
            currentTime = (
                    validTimes.times.map { it.start } +
                    validTimes.breakTimes.map { it.start } +
                    validTimes.connectionTimes.map { it.start }
            ).min()

            if (produceQuantity eq quantity.toFlt64()) {
                break
            }
        }

        return if (produceQuantity eq quantity.toFlt64()) {
            ActualTime(
                time = TimeRange(
                    start = currentTime,
                    end = endTime
                ),
                workingTimes = workingTimes,
                breakTimes = breakTimes,
                connectionTimes = connectionTimes
            )
        } else {
            ActualTime(
                time = TimeRange(
                    start = Instant.DISTANT_PAST,
                    end = endTime
                ),
                workingTimes = workingTimes,
                breakTimes = breakTimes,
                connectionTimes = connectionTimes
            )
        }
    }

    private fun actualQuantity(
        material: T,
        time: TimeRange,
        productivityCalendar: List<Productivity<T>>,
        unavailableTimes: List<TimeRange> = emptyList(),
        beforeConnectionTime: DurationRange? = null,
        afterConnectionTime: DurationRange? = null,
        beforeConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        afterConditionalConnectionTime: ((TimeRange) -> DurationRange?)? = null,
        currentDuration: Duration = Duration.ZERO,
        breakTime: Pair<DurationRange, Duration>? = null
    ): Q {
        val validTimes = validTimes(
            time = time,
            unavailableTimes = unavailableTimes,
            beforeConnectionTime = beforeConnectionTime,
            afterConnectionTime = afterConnectionTime,
            beforeConditionalConnectionTime = beforeConditionalConnectionTime,
            afterConditionalConnectionTime = afterConditionalConnectionTime,
            currentDuration = currentDuration,
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
