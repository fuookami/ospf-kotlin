package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure;

import kotlinx.datetime.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test

class TimeRangeFindTest {
    @Test
    fun testTimeRange() {
        val calendar = listOf(
            TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T18:00:00Z")
            ),
            TimeRange(
                start = Instant.parse("2020-08-31T08:00:00Z"),
                end = Instant.parse("2020-08-31T18:00:00Z")
            )
        )

        assert(
            runBlocking {
                calendar.find(
                    TimeRange(
                        start = Instant.parse("2020-08-30T19:00:00Z"),
                        end = Instant.parse("2020-08-31T07:00:00Z")
                    )
                ).size
            } == 0
        )

        assert(
            runBlocking {
                calendar.find(
                    TimeRange(
                        start = Instant.parse("2020-08-30T07:00:00Z"),
                        end = Instant.parse("2020-08-31T07:00:00Z")
                    )
                ).size
            } == 1
        )

        assert(
            runBlocking {
                calendar.find(
                    TimeRange(
                        start = Instant.parse("2020-08-31T07:00:00Z"),
                        end = Instant.parse("2020-08-31T19:00:00Z")
                    )
                ).size
            } == 1
        )

        assert(
            runBlocking {
                calendar.find(
                    TimeRange(
                        start = Instant.parse("2020-08-30T07:00:00Z"),
                        end = Instant.parse("2020-08-31T19:00:00Z")
                    )
                ).size
            } == 2
        )

        assert(
            runBlocking {
                calendar.find(
                    TimeRange(
                        start = Instant.parse("2020-08-30T07:00:00Z"),
                        end = Instant.parse("2020-08-31T17:00:00Z")
                    )
                ).size
            } == 2
        )
    }

    @Test
    fun testInstant() {
        val calendar = listOf(
            TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T18:00:00Z")
            ),
            TimeRange(
                start = Instant.parse("2020-08-31T08:00:00Z"),
                end = Instant.parse("2020-08-31T18:00:00Z")
            )
        )

        assert(
            runBlocking {
                calendar.findFrom(
                    Instant.parse("2020-08-31T19:00:00Z")
                ).size
            } == 0
        )

        assert(
            runBlocking {
                calendar.findFrom(
                    Instant.parse("2020-08-31T07:00:00Z")
                ).size
            } == 1
        )

        assert(
            runBlocking {
                calendar.findFrom(
                    Instant.parse("2020-08-31T09:00:00Z")
                ).size
            } == 1
        )

        assert(
            runBlocking {
                calendar.findFrom(
                    Instant.parse("2020-08-30T09:00:00Z")
                ).size
            } == 2
        )

        assert(
            runBlocking {
                calendar.findFrom(
                    Instant.parse("2020-08-30T07:00:00Z")
                ).size
            } == 2
        )
    }
}
