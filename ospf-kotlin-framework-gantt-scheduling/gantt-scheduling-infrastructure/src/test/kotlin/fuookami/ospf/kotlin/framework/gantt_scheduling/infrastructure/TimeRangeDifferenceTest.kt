
@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.test.*
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

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

    @Test
    fun testFrontAtAndBackAtRequireMeaningfulMessage() {
        val frontUnavailable = listOf(
            TimeRange(
                start = Instant.DISTANT_PAST,
                end = Instant.parse("2020-08-30T10:00:00Z")
            )
        )
        val frontException = runCatching { frontUnavailable.frontAt(0) }.exceptionOrNull()
        assert(frontException is IllegalArgumentException)
        assert(frontException!!.message!!.contains("frontAt(0)"))

        val backUnavailable = listOf(
            TimeRange(
                start = Instant.parse("2020-08-30T10:00:00Z"),
                end = Instant.DISTANT_FUTURE
            )
        )
        val backException = runCatching { backUnavailable.backAt(0) }.exceptionOrNull()
        assert(backException is IllegalArgumentException)
        assert(backException!!.message!!.contains("backAt(0)"))
    }

    @Test
    fun testRsplitRejectsUnsupportedMaxDuration() {
        val calendar = TimeRange(
            start = Instant.parse("2020-08-30T08:00:00Z"),
            end = Instant.parse("2020-08-30T10:00:00Z")
        )
        val result = calendar.rsplit(
            unit = DurationRange(1.hours, 2.hours),
            maxDuration = 3.hours
        )
        assertTrue(result is Failed)
        assertTrue((result as Failed<*, *, *>).error.message.contains("TimeRange.rsplit"))
    }
}
