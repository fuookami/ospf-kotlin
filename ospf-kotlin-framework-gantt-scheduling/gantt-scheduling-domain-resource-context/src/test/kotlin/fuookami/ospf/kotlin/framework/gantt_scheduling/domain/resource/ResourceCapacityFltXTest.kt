@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ResourceCapacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange

class ResourceCapacityFltXTest {
    private val timeRange = TimeRange(
        start = Instant.parse("2024-01-01T08:00:00Z"),
        end = Instant.parse("2024-01-01T18:00:00Z")
    )

    @Test
    fun resourceCapacityShouldSupportFltX() {
        val quantityRange = ValueRange(FltX("0"), FltX("100"), Interval.Closed, Interval.Closed, FltX).value!!
        val capacity = ResourceCapacity<FltX>(
            time = timeRange,
            quantity = quantityRange,
            lessQuantity = FltX("5.0"),
            overQuantity = FltX("10.0"),
            interval = 2.hours,
            name = "warehouse-A"
        )

        assertTrue(capacity.quantity.lowerBound.value eq FltX("0"))
        assertTrue(capacity.quantity.upperBound.value eq FltX("100"))
        assertTrue(capacity.lessQuantity!! eq FltX("5.0"))
        assertTrue(capacity.overQuantity!! eq FltX("10.0"))
        assertEquals(2.hours, capacity.interval)
        assertEquals("warehouse-A", capacity.name)
        assertTrue(capacity.lessEnabled)
        assertTrue(capacity.overEnabled)
    }

    @Test
    fun resourceCapacityDefaultsShouldWorkWithFltX() {
        val quantityRange = ValueRange(FltX("10"), FltX("50"), Interval.Closed, Interval.Closed, FltX).value!!
        val capacity = ResourceCapacity<FltX>(
            time = timeRange,
            quantity = quantityRange
        )

        assertTrue(capacity.quantity.lowerBound.value eq FltX("10"))
        assertTrue(capacity.quantity.upperBound.value eq FltX("50"))
        assertNull(capacity.lessQuantity)
        assertNull(capacity.overQuantity)
        assertEquals(Duration.INFINITE, capacity.interval)
        assertNull(capacity.name)
        assertTrue(!capacity.lessEnabled)
        assertTrue(!capacity.overEnabled)
    }

    @Test
    fun resourceCapacityValueRangeContainsWithFltX() {
        val quantityRange = ValueRange(FltX("0"), FltX("100"), Interval.Closed, Interval.Closed, FltX).value!!
        val capacity = ResourceCapacity<FltX>(
            time = timeRange,
            quantity = quantityRange
        )

        assertTrue(FltX("50") in capacity.quantity)
        assertTrue(FltX("0") in capacity.quantity)
        assertTrue(FltX("100") in capacity.quantity)
    }

    @Test
    fun resourceCapacityFixedValueRangeWithFltX() {
        val fixedRange = ValueRange(FltX("25"), FltX("25"), Interval.Closed, Interval.Closed, FltX).value!!
        val capacity = ResourceCapacity<FltX>(
            time = timeRange,
            quantity = fixedRange,
            interval = 1.hours
        )

        assertTrue(capacity.quantity.fixed)
        assertTrue(capacity.quantity.fixedValue!! eq FltX("25"))
    }

    @Test
    fun resourceCapacityWithOnlyLessQuantityFltX() {
        val quantityRange = ValueRange(FltX("5"), FltX("200"), Interval.Closed, Interval.Closed, FltX).value!!
        val capacity = ResourceCapacity<FltX>(
            time = timeRange,
            quantity = quantityRange,
            lessQuantity = FltX("2.5")
        )

        assertTrue(capacity.lessEnabled)
        assertTrue(!capacity.overEnabled)
        assertTrue(capacity.lessQuantity!! eq FltX("2.5"))
    }

    @Test
    fun resourceCapacityWithOnlyOverQuantityFltX() {
        val quantityRange = ValueRange(FltX("0"), FltX("500"), Interval.Closed, Interval.Closed, FltX).value!!
        val capacity = ResourceCapacity<FltX>(
            time = timeRange,
            quantity = quantityRange,
            overQuantity = FltX("15.0")
        )

        assertTrue(!capacity.lessEnabled)
        assertTrue(capacity.overEnabled)
        assertTrue(capacity.overQuantity!! eq FltX("15.0"))
    }
}
