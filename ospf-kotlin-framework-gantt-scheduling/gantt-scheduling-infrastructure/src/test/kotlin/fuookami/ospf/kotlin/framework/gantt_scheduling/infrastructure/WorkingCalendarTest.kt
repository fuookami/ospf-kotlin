package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.*
import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.utils.math.*

class WorkingCalendarTest {
    @Test
    fun testWorkingCalendar() {
        val calendar = WorkingCalendar(
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
            calendar.actualTime(
                time = TimeRange(
                    start = Instant.parse("2020-08-30T14:00:00Z"),
                    end = Instant.parse("2020-08-30T20:00:00Z")
                )
            ) == TimeRange(
                start = Instant.parse("2020-08-30T14:00:00Z"),
                end = Instant.parse("2020-08-31T10:00:00Z")
            )
        )

        assert(
            calendar.actualTime(
                time = TimeRange(
                    start = Instant.parse("2020-08-30T20:00:00Z"),
                    end = Instant.parse("2020-08-30T22:00:00Z")
                )
            ) == TimeRange(
                start = Instant.parse("2020-08-30T20:00:00Z"),
                end = Instant.parse("2020-08-31T10:00:00Z")
            )
        )
    }

    @Test
    fun testProductivityCalendar1() {
        val calendar = DiscreteProductivityCalendar(
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
            calendar.actualTimeFrom(
                material = 1,
                quantity = UInt64(60),
                startTime = Instant.parse("2020-08-30T08:00:00Z")
            ) == TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )
        assert(
            calendar.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T09:00:00Z")
                )
            ) == UInt64(60)
        )

        assert(
            calendar.actualTimeFrom(
                material = 1,
                quantity = UInt64(60),
                startTime = Instant.parse("2020-08-30T14:00:00Z")
            ) == TimeRange(
                start = Instant.parse("2020-08-30T14:00:00Z"),
                end = Instant.parse("2020-08-30T16:00:00Z")
            )
        )
        assert(
            calendar.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T14:00:00Z"),
                    end = Instant.parse("2020-08-30T16:00:00Z")
                )
            ) == UInt64(60)
        )

        assert(
            calendar.actualTimeFrom(
                material = 1,
                quantity = UInt64(90),
                startTime = Instant.parse("2020-08-30T15:00:00Z")
            ) == TimeRange(
                start = Instant.parse("2020-08-30T15:00:00Z"),
                end = Instant.parse("2020-08-30T17:00:00Z")
            )
        )
        assert(
            calendar.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T15:00:00Z"),
                    end = Instant.parse("2020-08-30T17:00:00Z")
                )
            ) == UInt64(90)
        )

        assert(
            calendar.actualTimeFrom(
                material = 1,
                quantity = UInt64(120),
                startTime = Instant.parse("2020-08-30T17:00:00Z")
            ) == TimeRange(
                start = Instant.parse("2020-08-30T17:00:00Z"),
                end = Instant.parse("2020-08-31T08:30:00Z")
            )
        )
        assert(
            calendar.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T17:00:00Z"),
                    end = Instant.parse("2020-08-31T08:30:00Z")
                )
            ) == UInt64(120)
        )

        assert(
            calendar.actualTimeFrom(
                material = 1,
                quantity = UInt64(120),
                startTime = Instant.parse("2020-08-30T17:00:00Z")
            ) == TimeRange(
                start = Instant.parse("2020-08-30T17:00:00Z"),
                end = Instant.parse("2020-08-31T08:30:00Z")
            )
        )
        assert(
            calendar.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T17:00:00Z"),
                    end = Instant.parse("2020-08-31T08:30:00Z")
                )
            ) == UInt64(120)
        )
    }

    @Test
    fun testProductivityCalendar2() {
        val calendar = DiscreteProductivityCalendar(
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
            calendar.actualTimeFrom(
                material = 1,
                quantity = UInt64(60),
                startTime = Instant.parse("2020-08-30T08:00:00Z")
            ) == TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T09:00:00Z")
            )
        )
        assert(
            calendar.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T09:00:00Z")
                )
            ) == UInt64(60)
        )

        assert(
            calendar.actualTimeFrom(
                material = 1,
                quantity = UInt64(60),
                startTime = Instant.parse("2020-08-30T14:00:00Z")
            ) == TimeRange(
                start = Instant.parse("2020-08-30T14:00:00Z"),
                end = Instant.parse("2020-08-30T16:00:00Z")
            )
        )
        assert(
            calendar.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T14:00:00Z"),
                    end = Instant.parse("2020-08-30T16:00:00Z")
                )
            ) == UInt64(60)
        )

        assert(
            calendar.actualTimeFrom(
                material = 1,
                quantity = UInt64(90),
                startTime = Instant.parse("2020-08-30T15:00:00Z")
            ) == TimeRange(
                start = Instant.parse("2020-08-30T15:00:00Z"),
                end = Instant.parse("2020-08-30T17:00:00Z")
            )
        )
        assert(
            calendar.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T15:00:00Z"),
                    end = Instant.parse("2020-08-30T17:00:00Z")
                )
            ) == UInt64(90)
        )

        assert(
            calendar.actualTimeFrom(
                material = 1,
                quantity = UInt64(120),
                startTime = Instant.parse("2020-08-30T17:00:00Z")
            ) == TimeRange(
                start = Instant.parse("2020-08-30T17:00:00Z"),
                end = Instant.parse("2020-08-31T08:30:00Z")
            )
        )
        assert(
            calendar.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T17:00:00Z"),
                    end = Instant.parse("2020-08-31T08:30:00Z")
                )
            ) == UInt64(120)
        )

        assert(
            calendar.actualTimeFrom(
                material = 1,
                quantity = UInt64(210),
                startTime = Instant.parse("2020-08-30T17:00:00Z")
            ) == TimeRange(
                start = Instant.parse("2020-08-30T17:00:00Z"),
                end = Instant.parse("2020-08-31T10:00:00Z")
            )
        )
        assert(
            calendar.actualQuantity(
                material = 1,
                time = TimeRange(
                    start = Instant.parse("2020-08-30T17:00:00Z"),
                    end = Instant.parse("2020-08-31T10:00:00Z")
                )
            ) == UInt64(210)
        )
    }
}
