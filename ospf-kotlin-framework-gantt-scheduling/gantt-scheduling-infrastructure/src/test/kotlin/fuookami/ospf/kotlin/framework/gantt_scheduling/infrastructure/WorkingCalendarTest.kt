package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.*
import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.utils.math.*

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
}
