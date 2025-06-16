package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlinx.datetime.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test

class TimeRangeDifferenceTest {
    @Test
    fun testTimeRangeDifference1() {
        val calendar = TimeRange(
            start = Instant.parse("2020-08-30T08:00:00Z"),
            end = Instant.parse("2020-08-30T18:00:00Z")
        )

        assert(
            calendar.differenceWith(
            TimeRange(
                    start = Instant.parse("2020-08-30T12:00:00Z"),
                    end = Instant.parse("2020-08-30T14:00:00Z")
                )
            ) == listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T12:00:00Z")
                ),
                TimeRange(
                    start = Instant.parse("2020-08-30T14:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            )
        )

        assert(
            calendar.differenceWith(
                TimeRange(
                    start = Instant.parse("2020-08-30T06:00:00Z"),
                    end = Instant.parse("2020-08-30T12:00:00Z")
                )
            ) == listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T12:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            )
        )

        assert(
            calendar.differenceWith(
                TimeRange(
                    start = Instant.parse("2020-08-30T12:00:00Z"),
                    end = Instant.parse("2020-08-30T20:00:00Z")
                )
            ) == listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T12:00:00Z")
                )
            )
        )

        assert(
            calendar.differenceWith(
                TimeRange(
                    start = Instant.parse("2020-08-30T06:00:00Z"),
                    end = Instant.parse("2020-08-30T20:00:00Z")
                )
            ).isEmpty()
        )

        assert(
            calendar.differenceWith(
                TimeRange(
                    start = Instant.parse("2020-08-30T00:00:00Z"),
                    end = Instant.parse("2020-08-30T06:00:00Z")
                )
            ) == listOf(calendar)
        )

        assert(
            calendar.differenceWith(
                TimeRange(
                    start = Instant.parse("2020-08-30T20:00:00Z"),
                    end = Instant.parse("2020-08-31T00:00:00Z")
                )
            ) == listOf(calendar)
        )
    }

    @Test
    fun testTimeRangeDifference2() {
        val calendar = TimeRange(
            start = Instant.parse("2020-08-30T08:00:00Z"),
            end = Instant.parse("2020-08-30T18:00:00Z")
        )

        assert(
            calendar.differenceWith(
                listOf(
                    TimeRange(
                        start = Instant.parse("2020-08-30T12:00:00Z"),
                        end = Instant.parse("2020-08-30T14:00:00Z")
                    )
                )
            ) == listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T12:00:00Z")
                ),
                TimeRange(
                    start = Instant.parse("2020-08-30T14:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            )
        )

        assert(
            calendar.differenceWith(
                listOf(
                    TimeRange(
                        start = Instant.parse("2020-08-30T06:00:00Z"),
                        end = Instant.parse("2020-08-30T12:00:00Z")
                    )
                )
            ) == listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T12:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            )
        )

        assert(
            calendar.differenceWith(
                listOf(
                    TimeRange(
                        start = Instant.parse("2020-08-30T12:00:00Z"),
                        end = Instant.parse("2020-08-30T20:00:00Z")
                    )
                )
            ) == listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T12:00:00Z")
                )
            )
        )

        assert(
            calendar.differenceWith(
                listOf(
                    TimeRange(
                        start = Instant.parse("2020-08-30T10:00:00Z"),
                        end = Instant.parse("2020-08-30T12:00:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-30T14:00:00Z"),
                        end = Instant.parse("2020-08-30T16:00:00Z")
                    )
                )
            ) == listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T10:00:00Z")
                ),
                TimeRange(
                    start = Instant.parse("2020-08-30T12:00:00Z"),
                    end = Instant.parse("2020-08-30T14:00:00Z")
                ),
                TimeRange(
                    start = Instant.parse("2020-08-30T16:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            )
        )

        assert(
            calendar.differenceWith(
                listOf(
                    TimeRange(
                        start = Instant.parse("2020-08-30T06:00:00Z"),
                        end = Instant.parse("2020-08-30T12:00:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-30T14:00:00Z"),
                        end = Instant.parse("2020-08-30T16:00:00Z")
                    )
                )
            ) == listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T12:00:00Z"),
                    end = Instant.parse("2020-08-30T14:00:00Z")
                ),
                TimeRange(
                    start = Instant.parse("2020-08-30T16:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            )
        )

        assert(
            calendar.differenceWith(
                listOf(
                    TimeRange(
                        start = Instant.parse("2020-08-30T10:00:00Z"),
                        end = Instant.parse("2020-08-30T12:00:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-30T16:00:00Z"),
                        end = Instant.parse("2020-08-30T20:00:00Z")
                    )
                )
            ) == listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T10:00:00Z")
                ),
                TimeRange(
                    start = Instant.parse("2020-08-30T12:00:00Z"),
                    end = Instant.parse("2020-08-30T16:00:00Z")
                )
            )
        )

        assert(
            calendar.differenceWith(
                listOf(
                    TimeRange(
                        start = Instant.parse("2020-08-30T06:00:00Z"),
                        end = Instant.parse("2020-08-30T20:00:00Z")
                    )
                )
            ).isEmpty()
        )

        assert(
            calendar.differenceWith(
                listOf(
                    TimeRange(
                        start = Instant.parse("2020-08-30T00:00:00Z"),
                        end = Instant.parse("2020-08-30T06:00:00Z")
                    )
                )
            ) == listOf(calendar)
        )

        assert(
            calendar.differenceWith(
                listOf(
                    TimeRange(
                        start = Instant.parse("2020-08-30T20:00:00Z"),
                        end = Instant.parse("2020-08-30T00:00:00Z")
                    )
                )
            ) == listOf(calendar)
        )

        assert(
            calendar.differenceWith(
                listOf(
                    TimeRange(
                        start = Instant.parse("2020-08-30T12:00:00Z"),
                        end = Instant.parse("2020-08-30T14:00:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-30T13:00:00Z"),
                        end = Instant.parse("2020-08-30T15:00:00Z")
                    )
                )
            ) == listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T08:00:00Z"),
                    end = Instant.parse("2020-08-30T12:00:00Z")
                ),
                TimeRange(
                    start = Instant.parse("2020-08-30T15:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            )
        )

        assert(
            calendar.differenceWith(
                listOf(
                    TimeRange(
                        start = Instant.parse("2020-08-30T00:00:00Z"),
                        end = Instant.parse("2020-08-30T06:00:00Z")
                    ),
                    TimeRange(
                        start = Instant.parse("2020-08-30T05:00:00Z"),
                        end = Instant.parse("2020-08-30T15:00:00Z")
                    )
                )
            ) == listOf(
                TimeRange(
                    start = Instant.parse("2020-08-30T15:00:00Z"),
                    end = Instant.parse("2020-08-30T18:00:00Z")
                )
            )
        )
    }
}
