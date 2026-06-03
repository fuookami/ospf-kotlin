@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class WorkingCalendarTest {
    @Test
    fun testWorkingCalendarActualTime() {
        val calendar1 = WorkingCalendar(
            timeWindow = TimeWindow.minutes(
                timeWindow = TimeRange(
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
            timeWindow = TimeWindow.minutes(
                timeWindow = TimeRange(
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
            timeWindow = TimeWindow.minutes(
                timeWindow = TimeRange(
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
            timeWindow = TimeWindow.minutes(
                timeWindow = TimeRange(
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
            timeWindow = TimeWindow.minutes(
                timeWindow = TimeRange(
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
            timeWindow = TimeWindow.minutes(
                timeWindow = TimeRange(
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
            )
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
}


