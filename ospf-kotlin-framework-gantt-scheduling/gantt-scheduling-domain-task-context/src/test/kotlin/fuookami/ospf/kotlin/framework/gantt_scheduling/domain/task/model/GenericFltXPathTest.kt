@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow

class GenericFltXPathTest {
    @Test
    fun costShouldSupportFltX() {
        val cost = Cost(
            items = listOf(
                CostItem(
                    tag = "setup",
                    value = FltX("1.25")
                ),
                CostItem(
                    tag = "processing",
                    value = FltX("2.75")
                )
            ),
            constants = FltX
        )

        assertTrue(cost.valid)
        assertTrue(cost.sum!! eq FltX("4.00"))
    }

    @Test
    fun timeWindowShouldSupportFltX() {
        val timeWindow = TimeWindow(
            window = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T10:00:00Z")
            ),
            durationUnit = DurationUnit.MINUTES,
            interval = 15.minutes,
            fromDouble = { FltX(it) },
            toDouble = { it.toDouble() }
        )

        val instant = Instant.parse("2020-08-30T09:30:00Z")

        assertTrue(timeWindow.valueOf(instant) eq FltX(90.0))
        assertEquals(Instant.parse("2020-08-30T08:45:00Z"), timeWindow.instantOf(FltX(45.0)))
        assertEquals(30.minutes, timeWindow.durationOf(FltX(30.0)))
    }
}
