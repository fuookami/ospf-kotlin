@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.unit.Gram
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity

class WorkingCalendarTest {
    private fun minutesTimeWindow(timeWindow: TimeRange): TimeWindow<Flt64> {
        return TimeWindow.minutes(
            timeWindow = timeWindow,
            dateOffset = Flt64.zero,
            continues = true,
            interval = Flt64.one,
            fromDouble = { Flt64(it) },
            toDouble = { it.toDouble() }
        )
    }

    private fun genericMinutesTimeWindow(timeWindow: TimeRange): TimeWindow<FltX> {
        return TimeWindow.minutes(
            timeWindow = timeWindow,
            dateOffset = Quantity(FltX("0.0"), NoneUnit),
            continues = true,
            interval = Quantity(FltX("1.0"), NoneUnit),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )
    }

    @Test
    fun actualTimeShouldExposeGenericDurationQuantities() {
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T16:00:00Z")
            ),
            durationUnit = DurationUnit.HOURS,
            interval = 1.hours,
            fromDouble = { FltX(it) },
            toDouble = { it.toDouble() }
        )
        val actualTime = WorkingCalendar.ActualTime(
            time = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T12:00:00Z")
            ),
            workingTimes = listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T10:00:00Z")
                )
            ),
            breakTimes = listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T10:00:00Z"),
                    end = Instant.parse("2020-08-30T10:30:00Z")
                )
            ),
            connectionTimes = listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T10:30:00Z"),
                    end = Instant.parse("2020-08-30T11:00:00Z")
                )
            )
        )

        assert(actualTime.durationQuantity(timeWindow).value eq FltX("4.0"))
        assert(actualTime.workingDurationQuantity(timeWindow).value eq FltX("2.0"))
        assert(actualTime.breakDurationQuantity(timeWindow).value eq FltX("0.5"))
        assert(actualTime.connectionDurationQuantity(timeWindow).value eq FltX("0.5"))
    }

    @Test
    fun workingCalendarShouldAcceptGenericTimeWindowBoundary() {
        val calendar = WorkingCalendar(
            timeWindow = genericMinutesTimeWindow(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            ),
            unavailableTimes = listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T12:00:00Z"),
                    end = Instant.parse("2020-08-30T13:00:00Z")
                )
            )
        )

        val actualTime = calendar.actualTime(
            time = TimeRange(
                start = Instant.parse("2020-08-30T11:00:00Z"),
                end = Instant.parse("2020-08-30T13:00:00Z")
            )
        )

        assert(actualTime.time == TimeRange(
            start = Instant.parse("2020-08-30T11:00:00Z"),
            end = Instant.parse("2020-08-30T14:00:00Z")
        ))
    }

    @Test
    fun quantityProductivityCalendarShouldAcceptGenericTimeWindowBoundary() {
        val calendar = DiscreteQuantityProductivityCalendar(
            timeWindow = genericMinutesTimeWindow(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            ),
            productivity = listOf(
                QuantityProductivity<UInt64, Int, Int>(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T08:00:00Z"),
                        end = Instant.parse("2020-08-30T18:00:00Z")
                    ),
                    extractor = { it },
                    capacities = emptyMap(),
                    unitYields = mapOf(Pair(1, Quantity(UInt64(1), Kilogram)))
                )
            ),
            quantityUnit = Kilogram
        )

        val quantity = calendar.actualQuantity(
            material = 1,
            time = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )

        assert(quantity.value == UInt64(60))
        assert(quantity.unit == Kilogram)
    }

    @Test
    fun testWorkingCalendarActualTime() {
        val calendar1 = WorkingCalendar(
            timeWindow = minutesTimeWindow(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-31T18:00:00Z")
                )
            ),
            unavailableTimes = listOf(
                TimeRange(
                    end = Instant.parse("2020-08-30T08:00:00Z")
                ),
                TimeRange(
                    start = Instant.parse("2020-08-30T18:00:00Z"),
                    end = Instant.parse("2020-08-31T08:00:00Z")
                ),
                TimeRange(
                    start = Instant.parse("2020-08-31T18:00:00Z"),
                )
            )
        )

        assert(
            calendar1.actualTime(
                time = TimeRange(
                    start = Instant.parse("2020-08-30T14:00:00Z"),
                    end = Instant.parse("2020-08-30T20:00:00Z")
                )
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T14:00:00Z"),
                end = Instant.parse("2020-08-31T10:00:00Z")
            )
        )

        assert(
            calendar1.actualTime(
                time = TimeRange(
                    start = Instant.parse("2020-08-30T14:00:00Z"),
                    end = Instant.parse("2020-08-30T20:00:00Z")
                ),
                beforeConnectionTime = DurationRange(5.minutes),
                afterConnectionTime = DurationRange(5.minutes),
                beforeConditionalConnectionTime = null,
                afterConditionalConnectionTime = null,
                breakTime = Pair(DurationRange(1.hours), 5.minutes)
            ) == WorkingCalendar.ActualTime(
                time = TimeRange(
                    start = Instant.parse("2020-08-30T14:00:00Z"),
                    end = Instant.parse("2020-08-31T10:35:00Z")
                ),
                workingTimes = listOf(
                    TimeRange(
                        start = Instant.parse("2020-08-30T14:00:00Z"),
                        end = Instant.parse("2020-08-30T15:00:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-30T15:05:00Z"),
                        end = Instant.parse("2020-08-30T16:05:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-30T16:10:00Z"),
                        end = Instant.parse("2020-08-30T17:10:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-30T17:15:00Z"),
                        end = Instant.parse("2020-08-30T17:55:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-31T08:05:00Z"),
                        end = Instant.parse("2020-08-31T09:05:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-31T09:10:00Z"),
                        end = Instant.parse("2020-08-31T10:10:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-31T10:15:00Z"),
                        end = Instant.parse("2020-08-31T10:35:00Z")
                    )
                ),
                breakTimes = listOf(
                    TimeRange(
                        start = Instant.parse("2020-08-30T15:00:00Z"),
                        end = Instant.parse("2020-08-30T15:05:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-30T16:05:00Z"),
                        end = Instant.parse("2020-08-30T16:10:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-30T17:10:00Z"),
                        end = Instant.parse("2020-08-30T17:15:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-31T09:05:00Z"),
                        end = Instant.parse("2020-08-31T09:10:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-31T10:10:00Z"),
                        end = Instant.parse("2020-08-31T10:15:00Z")
                    )
                ),
                connectionTimes = listOf(
                    TimeRange(
                        start = Instant.parse("2020-08-30T17:55:00Z"),
                        end = Instant.parse("2020-08-30T18:00:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-31T08:00:00Z"),
                        end = Instant.parse("2020-08-31T08:05:00Z")
                    )
                )
            )
        )

        assert(
            calendar1.actualTime(
                time = TimeRange(
                    start = Instant.parse("2020-08-30T20:00:00Z"),
                    end = Instant.parse("2020-08-30T22:00:00Z")
                )
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T20:00:00Z"),
                end = Instant.parse("2020-08-31T10:00:00Z")
            )
        )

        val calendar2 = WorkingCalendar(
            timeWindow = minutesTimeWindow(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-31T18:00:00Z")
                )
            ),
            unavailableTimes = listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T18:00:00Z"),
                    end = Instant.parse("2020-08-31T08:00:00Z")
                )
            )
        )

        assert(
            calendar2.actualTime(
                time = TimeRange(
                    start = Instant.parse("2020-08-30T14:00:00Z"),
                    end = Instant.parse("2020-08-30T20:00:00Z")
                )
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T14:00:00Z"),
                end = Instant.parse("2020-08-31T10:00:00Z")
            )
        )
    }

    @Test
    fun testProductivityCalendarActualTime() {
        val calendar1 = DiscreteProductivityCalendar(
            timeWindow = minutesTimeWindow(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-31T18:00:00Z")
                )
            ),
            productivity = listOf(
                Productivity(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T08:00:00Z"),
                        end = Instant.parse("2020-08-30T12:00:00Z")
                    ),
                    capacities = mapOf(Pair(1, 1.minutes))
                ),
                Productivity(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T13:00:00Z"),
                        end = Instant.parse("2020-08-30T16:00:00Z")
                    ),
                    capacities = mapOf(Pair(1, 2.minutes))
                ),
                Productivity(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T16:00:00Z"),
                        end = Instant.parse("2020-08-30T18:00:00Z")
                    ),
                    capacities = mapOf(Pair(1, 1.minutes))
                ),
                Productivity(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-31T08:00:00Z"),
                        end = Instant.parse("2020-08-31T18:00:00Z")
                    ),
                    capacities = mapOf(Pair(1, 0.5.minutes))
                )
            )
        )

        assert(
            calendar1.actualTimeFrom(
                material = 1,
                quantity = UInt64(60),
                startTime = Instant.parse("2020-08-30T08:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )

        assert(
            calendar1.actualTimeUntil(
                material = 1,
                quantity = UInt64(60),
                endTime = Instant.parse("2020-08-30T09:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )

        assert(
            calendar1.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T09:00:00Z")
                )
            ) == UInt64(60)
        )

        assert(
            calendar1.actualTimeFrom(
                material = 1,
                quantity = UInt64(60),
                startTime = Instant.parse("2020-08-30T14:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T14:00:00Z"),
                end = Instant.parse("2020-08-30T16:00:00Z")
            )
        )

        assert(
            calendar1.actualTimeUntil(
                material = 1,
                quantity = UInt64(60),
                endTime = Instant.parse("2020-08-30T16:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T14:00:00Z"),
                end = Instant.parse("2020-08-30T16:00:00Z")
            )
        )

        assert(
            calendar1.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T14:00:00Z"),
                    end = Instant.parse("2020-08-30T16:00:00Z")
                )
            ) == UInt64(60)
        )

        assert(
            calendar1.actualTimeFrom(
                material = 1,
                quantity = UInt64(90),
                startTime = Instant.parse("2020-08-30T15:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T15:00:00Z"),
                end = Instant.parse("2020-08-30T17:00:00Z")
            )
        )

        assert(
            calendar1.actualTimeUntil(
                material = 1,
                quantity = UInt64(90),
                endTime = Instant.parse("2020-08-30T17:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T15:00:00Z"),
                end = Instant.parse("2020-08-30T17:00:00Z")
            )
        )

        assert(
            calendar1.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T15:00:00Z"),
                    end = Instant.parse("2020-08-30T17:00:00Z")
                )
            ) == UInt64(90)
        )

        assert(
            calendar1.actualTimeFrom(
                material = 1,
                quantity = UInt64(120),
                startTime = Instant.parse("2020-08-30T17:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T17:00:00Z"),
                end = Instant.parse("2020-08-31T08:30:00Z")
            )
        )

        assert(
            calendar1.actualTimeUntil(
                material = 1,
                quantity = UInt64(120),
                endTime = Instant.parse("2020-08-31T08:30:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T17:00:00Z"),
                end = Instant.parse("2020-08-31T08:30:00Z")
            )
        )

        assert(
            calendar1.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T17:00:00Z"),
                    end = Instant.parse("2020-08-31T08:30:00Z")
                )
            ) == UInt64(120)
        )

        assert(
            calendar1.actualTimeFrom(
                material = 1,
                quantity = UInt64(120),
                startTime = Instant.parse("2020-08-30T17:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T17:00:00Z"),
                end = Instant.parse("2020-08-31T08:30:00Z")
            )
        )

        assert(
            calendar1.actualTimeUntil(
                material = 1,
                quantity = UInt64(120),
                endTime = Instant.parse("2020-08-31T08:30:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T17:00:00Z"),
                end = Instant.parse("2020-08-31T08:30:00Z")
            )
        )

        assert(
            calendar1.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T17:00:00Z"),
                    end = Instant.parse("2020-08-31T08:30:00Z")
                )
            ) == UInt64(120)
        )

        val calendar2 = DiscreteProductivityCalendar(
            timeWindow = minutesTimeWindow(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-31T18:00:00Z")
                )
            ),
            productivity = listOf(
                Productivity(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T08:00:00Z"),
                        end = Instant.parse("2020-08-30T12:00:00Z")
                    ),
                    capacities = mapOf(Pair(1, 1.minutes))
                ),
                Productivity(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T13:00:00Z"),
                        end = Instant.parse("2020-08-30T16:00:00Z")
                    ),
                    capacities = mapOf(Pair(1, 2.minutes))
                ),
                Productivity(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T16:00:00Z"),
                        end = Instant.parse("2020-08-30T18:00:00Z")
                    ),
                    capacities = mapOf(Pair(1, 1.minutes))
                ),
                Productivity(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-31T08:00:00Z"),
                        end = Instant.parse("2020-08-31T09:00:00Z")
                    ),
                    capacities = mapOf(Pair(1, 0.5.minutes))
                ),
                Productivity(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-31T09:00:00Z"),
                        end = Instant.parse("2020-08-31T18:00:00Z")
                    ),
                    capacities = mapOf(Pair(1, 2.minutes))
                )
            )
        )

        assert(
            calendar2.actualTimeFrom(
                material = 1,
                quantity = UInt64(60),
                startTime = Instant.parse("2020-08-30T08:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )

        assert(
            calendar2.actualTimeUntil(
                material = 1,
                quantity = UInt64(60),
                endTime = Instant.parse("2020-08-30T09:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )

        assert(
            calendar2.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T09:00:00Z")
                )
            ) == UInt64(60)
        )

        assert(
            calendar2.actualTimeFrom(
                material = 1,
                quantity = UInt64(60),
                startTime = Instant.parse("2020-08-30T14:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T14:00:00Z"),
                end = Instant.parse("2020-08-30T16:00:00Z")
            )
        )

        assert(
            calendar2.actualTimeUntil(
                material = 1,
                quantity = UInt64(60),
                endTime = Instant.parse("2020-08-30T16:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T14:00:00Z"),
                end = Instant.parse("2020-08-30T16:00:00Z")
            )
        )

        assert(
            calendar2.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T14:00:00Z"),
                    end = Instant.parse("2020-08-30T16:00:00Z")
                )
            ) == UInt64(60)
        )

        assert(
            calendar2.actualTimeFrom(
                material = 1,
                quantity = UInt64(90),
                startTime = Instant.parse("2020-08-30T15:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T15:00:00Z"),
                end = Instant.parse("2020-08-30T17:00:00Z")
            )
        )

        assert(
            calendar2.actualTimeUntil(
                material = 1,
                quantity = UInt64(90),
                endTime = Instant.parse("2020-08-30T17:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T15:00:00Z"),
                end = Instant.parse("2020-08-30T17:00:00Z")
            )
        )

        assert(
            calendar2.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T15:00:00Z"),
                    end = Instant.parse("2020-08-30T17:00:00Z")
                )
            ) == UInt64(90)
        )

        assert(
            calendar2.actualTimeFrom(
                material = 1,
                quantity = UInt64(120),
                startTime = Instant.parse("2020-08-30T17:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T17:00:00Z"),
                end = Instant.parse("2020-08-31T08:30:00Z")
            )
        )

        assert(
            calendar2.actualTimeUntil(
                material = 1,
                quantity = UInt64(120),
                endTime = Instant.parse("2020-08-31T08:30:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T17:00:00Z"),
                end = Instant.parse("2020-08-31T08:30:00Z")
            )
        )

        assert(
            calendar2.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T17:00:00Z"),
                    end = Instant.parse("2020-08-31T08:30:00Z")
                )
            ) == UInt64(120)
        )

        assert(
            calendar2.actualTimeFrom(
                material = 1,
                quantity = UInt64(210),
                startTime = Instant.parse("2020-08-30T17:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T17:00:00Z"),
                end = Instant.parse("2020-08-31T10:00:00Z")
            )
        )

        assert(
            calendar2.actualTimeUntil(
                material = 1,
                quantity = UInt64(210),
                endTime = Instant.parse("2020-08-31T10:00:00Z")
            ).time == TimeRange(
                start = Instant.parse("2020-08-30T17:00:00Z"),
                end = Instant.parse("2020-08-31T10:00:00Z")
            )
        )

        assert(
            calendar2.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T17:00:00Z"),
                    end = Instant.parse("2020-08-31T10:00:00Z")
                )
            ) == UInt64(210)
        )
    }

    @Test
    fun testQuantityProductivityCalendarActualTime() {
        val calendar1 = DiscreteQuantityProductivityCalendar(
            timeWindow = minutesTimeWindow(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-31T18:00:00Z")
                )
            ),
            productivity = listOf(
                QuantityProductivity<UInt64, Int, Int>(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T08:00:00Z"),
                        end = Instant.parse("2020-08-30T12:00:00Z")
                    ),
                    extractor = { it },
                    capacities = mapOf(Pair(1, 1.minutes)),
                    unitYields = emptyMap()
                ),
                QuantityProductivity<UInt64, Int, Int>(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T13:00:00Z"),
                        end = Instant.parse("2020-08-30T16:00:00Z")
                    ),
                    extractor = { it },
                    capacities = mapOf(Pair(1, 2.minutes)),
                    unitYields = emptyMap()
                ),
                QuantityProductivity<UInt64, Int, Int>(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T16:00:00Z"),
                        end = Instant.parse("2020-08-30T18:00:00Z")
                    ),
                    extractor = { it },
                    capacities = mapOf(Pair(1, 1.minutes)),
                    unitYields = emptyMap()
                ),
                QuantityProductivity<UInt64, Int, Int>(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-31T08:00:00Z"),
                        end = Instant.parse("2020-08-31T18:00:00Z")
                    ),
                    extractor = { it },
                    capacities = mapOf(Pair(1, 0.5.minutes)),
                    unitYields = emptyMap()
                )
            )
        )

        // actualTimeFrom: 60 units from 08:00
        val timeFrom60 = calendar1.actualTimeFrom(
            material = 1,
            quantity = Quantity(UInt64(60), NoneUnit),
            startTime = Instant.parse("2020-08-30T08:00:00Z")
        )
        assert(
            timeFrom60.time == TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )

        // actualTimeUntil: 60 units until 09:00
        val timeUntil60 = calendar1.actualTimeUntil(
            material = 1,
            quantity = Quantity(UInt64(60), NoneUnit),
            endTime = Instant.parse("2020-08-30T09:00:00Z")
        )
        assert(
            timeUntil60.time == TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )

        // actualQuantity: returns Quantity<UInt64> with NoneUnit
        val qty = calendar1.actualQuantity(
            material = 1,
            time = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )
        assert(qty.value == UInt64(60))
        assert(qty.unit == NoneUnit)

        // Cross-window: 90 units from 15:00
        val timeFrom90 = calendar1.actualTimeFrom(
            material = 1,
            quantity = Quantity(UInt64(90), NoneUnit),
            startTime = Instant.parse("2020-08-30T15:00:00Z")
        )
        assert(
            timeFrom90.time == TimeRange(
                start = Instant.parse("2020-08-30T15:00:00Z"),
                end = Instant.parse("2020-08-30T17:00:00Z")
            )
        )

        val qty90 = calendar1.actualQuantity(
            material = 1,
            time = TimeRange(
                start = Instant.parse("2020-08-30T15:00:00Z"),
                end = Instant.parse("2020-08-30T17:00:00Z")
            )
        )
        assert(qty90.value == UInt64(90))

        // Overnight: 120 units from 17:00
        val timeFrom120 = calendar1.actualTimeFrom(
            material = 1,
            quantity = Quantity(UInt64(120), NoneUnit),
            startTime = Instant.parse("2020-08-30T17:00:00Z")
        )
        assert(
            timeFrom120.time == TimeRange(
                start = Instant.parse("2020-08-30T17:00:00Z"),
                end = Instant.parse("2020-08-31T08:30:00Z")
            )
        )

        val qty120 = calendar1.actualQuantity(
            material = 1,
            time = TimeRange(
                start = Instant.parse("2020-08-30T17:00:00Z"),
                end = Instant.parse("2020-08-31T08:30:00Z")
            )
        )
        assert(qty120.value == UInt64(120))

        // ContinuousQuantityProductivityCalendar basic test
        val continuousCalendar = ContinuousQuantityProductivityCalendar(
            timeWindow = minutesTimeWindow(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            ),
            productivity = listOf(
                QuantityProductivity<Flt64, Int, Int>(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T08:00:00Z"),
                        end = Instant.parse("2020-08-30T18:00:00Z")
                    ),
                    extractor = { it },
                    capacities = mapOf(Pair(1, 1.minutes)),
                    unitYields = emptyMap()
                )
            ),
            constants = Flt64,
            quantityValueOf = { it }
        )

        val continuousQty = continuousCalendar.actualQuantity(
            material = 1,
            time = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )
        assert(continuousQty.value == Flt64(60.0))
        assert(continuousQty.unit == NoneUnit)

        val continuousTimeFrom = continuousCalendar.actualTimeFrom(
            material = 1,
            quantity = Quantity(Flt64(60.0), NoneUnit),
            startTime = Instant.parse("2020-08-30T08:00:00Z")
        )
        assert(
            continuousTimeFrom.time == TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )
    }

    @Test
    fun testQuantityProductivityCalendarUnitYieldPath() {
        // 使用 Kilogram 作为产出单位，验证 unitYields 路径的物理单位语义
        // unitYield = 1 kg/min，60 分钟生产 60 kg
        val calendar = DiscreteQuantityProductivityCalendar(
            timeWindow = minutesTimeWindow(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            ),
            productivity = listOf(
                QuantityProductivity<UInt64, Int, Int>(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T08:00:00Z"),
                        end = Instant.parse("2020-08-30T18:00:00Z")
                    ),
                    extractor = { it },
                    capacities = emptyMap(),
                    unitYields = mapOf(Pair(1, Quantity(UInt64(1), Kilogram)))
                )
            ),
            quantityUnit = Kilogram
        )

        // actualQuantity 应返回 Quantity(UInt64(60), Kilogram) — 1 kg/min * 60 min
        val qty = calendar.actualQuantity(
            material = 1,
            time = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )
        assert(qty.value == UInt64(60)) { "Expected 60, got ${qty.value}" }
        assert(qty.unit == Kilogram) { "Expected Kilogram, got ${qty.unit}" }

        // actualTimeFrom 使用 Kilogram 单位应正常
        val timeFrom = calendar.actualTimeFrom(
            material = 1,
            quantity = Quantity(UInt64(60), Kilogram),
            startTime = Instant.parse("2020-08-30T08:00:00Z")
        )
        assert(
            timeFrom.time == TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )

        // actualTimeUntil 使用 Kilogram 单位应正常
        val timeUntil = calendar.actualTimeUntil(
            material = 1,
            quantity = Quantity(UInt64(60), Kilogram),
            endTime = Instant.parse("2020-08-30T09:00:00Z")
        )
        assert(
            timeUntil.time == TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )

        // averageUnitYield 应保留 Kilogram 单位，值为 1 kg/min
        val avgYield = calendar.averageUnitYield[1]
        assert(avgYield != null)
        assert(avgYield!!.unit == Kilogram) { "Expected Kilogram, got ${avgYield.unit}" }
        assert(avgYield.value == UInt64(1)) { "Expected 1, got ${avgYield.value}" }
    }

    @Test
    fun testQuantityProductivityCalendarUnitMismatch() {
        // 日历使用 Kilogram 单位
        val calendar = DiscreteQuantityProductivityCalendar(
            timeWindow = minutesTimeWindow(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            ),
            productivity = listOf(
                QuantityProductivity<UInt64, Int, Int>(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T08:00:00Z"),
                        end = Instant.parse("2020-08-30T18:00:00Z")
                    ),
                    extractor = { it },
                    capacities = emptyMap(),
                    unitYields = mapOf(Pair(1, Quantity(UInt64(60), Kilogram)))
                )
            ),
            quantityUnit = Kilogram
        )

        // 传入 Gram 单位的 quantity 应抛出 IllegalArgumentException
        var thrown = false
        try {
            calendar.actualTimeFrom(
                material = 1,
                quantity = Quantity(UInt64(60), Gram),
                startTime = Instant.parse("2020-08-30T08:00:00Z")
            )
        } catch (e: IllegalArgumentException) {
            thrown = true
            assert(e.message!!.contains("does not match")) { "Unexpected message: ${e.message}" }
        }
        assert(thrown) { "Expected IllegalArgumentException for unit mismatch" }

        // actualTimeUntil 同理
        thrown = false
        try {
            calendar.actualTimeUntil(
                material = 1,
                quantity = Quantity(UInt64(60), Gram),
                endTime = Instant.parse("2020-08-30T09:00:00Z")
            )
        } catch (e: IllegalArgumentException) {
            thrown = true
        }
        assert(thrown) { "Expected IllegalArgumentException for actualTimeUntil unit mismatch" }
    }

    @Test
    fun testQuantityProductivityCalendarInconsistentUnitYields() {
        // 两个 productivity 条目使用不同单位 → averageUnitYield 应拒绝
        val calendar = DiscreteQuantityProductivityCalendar(
            timeWindow = minutesTimeWindow(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            ),
            productivity = listOf(
                QuantityProductivity<UInt64, Int, Int>(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T08:00:00Z"),
                        end = Instant.parse("2020-08-30T12:00:00Z")
                    ),
                    extractor = { it },
                    capacities = emptyMap(),
                    unitYields = mapOf(Pair(1, Quantity(UInt64(60), Kilogram)))
                ),
                QuantityProductivity<UInt64, Int, Int>(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T13:00:00Z"),
                        end = Instant.parse("2020-08-30T18:00:00Z")
                    ),
                    extractor = { it },
                    capacities = emptyMap(),
                    unitYields = mapOf(Pair(1, Quantity(UInt64(30), Gram)))
                )
            ),
            quantityUnit = Kilogram
        )

        var thrown = false
        try {
            calendar.averageUnitYield
        } catch (e: IllegalArgumentException) {
            thrown = true
            assert(e.message!!.contains("Inconsistent")) { "Unexpected message: ${e.message}" }
        }
        assert(thrown) { "Expected IllegalArgumentException for inconsistent unitYield units" }
    }

    @Test
    fun testQuantityProductivityCalendarMaterialSpecificUnits() {
        val calendar = DiscreteQuantityProductivityCalendar(
            timeWindow = minutesTimeWindow(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            ),
            productivity = listOf(
                QuantityProductivity<UInt64, Int, Int>(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T08:00:00Z"),
                        end = Instant.parse("2020-08-30T18:00:00Z")
                    ),
                    extractor = { it },
                    capacities = emptyMap(),
                    unitYields = mapOf(
                        Pair(1, Quantity(UInt64(1), Kilogram)),
                        Pair(2, Quantity(UInt64(2), Gram))
                    )
                )
            ),
            quantityUnit = NoneUnit
        )

        val gramQuantity = calendar.actualQuantity(
            material = 2,
            time = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )
        assert(gramQuantity.value == UInt64(120)) { "Expected 120, got ${gramQuantity.value}" }
        assert(gramQuantity.unit == Gram) { "Expected Gram, got ${gramQuantity.unit}" }

        val gramTime = calendar.actualTimeFrom(
            material = 2,
            quantity = Quantity(UInt64(120), Gram),
            startTime = Instant.parse("2020-08-30T08:00:00Z")
        )
        assert(
            gramTime.time == TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )
    }

    @Test
    fun testQuantityProductivityCalendarActualQuantityRejectsInconsistentMaterialUnits() {
        val calendar = DiscreteQuantityProductivityCalendar(
            timeWindow = minutesTimeWindow(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            ),
            productivity = listOf(
                QuantityProductivity<UInt64, Int, Int>(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T08:00:00Z"),
                        end = Instant.parse("2020-08-30T12:00:00Z")
                    ),
                    extractor = { it },
                    capacities = emptyMap(),
                    unitYields = mapOf(Pair(1, Quantity(UInt64(60), Kilogram)))
                ),
                QuantityProductivity<UInt64, Int, Int>(
                    timeWindow = TimeRange(
                        start = Instant.parse("2020-08-30T13:00:00Z"),
                        end = Instant.parse("2020-08-30T18:00:00Z")
                    ),
                    extractor = { it },
                    capacities = emptyMap(),
                    unitYields = mapOf(Pair(1, Quantity(UInt64(30), Gram)))
                )
            ),
            quantityUnit = Kilogram
        )

        var thrown = false
        try {
            calendar.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            )
        } catch (e: IllegalArgumentException) {
            thrown = true
            assert(e.message!!.contains("Inconsistent")) { "Unexpected message: ${e.message}" }
        }
        assert(thrown) { "Expected IllegalArgumentException for inconsistent material unitYield units" }
    }
}
