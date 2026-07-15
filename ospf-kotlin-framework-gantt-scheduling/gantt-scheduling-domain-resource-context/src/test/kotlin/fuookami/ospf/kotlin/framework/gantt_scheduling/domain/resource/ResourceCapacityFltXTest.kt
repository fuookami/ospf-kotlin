@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
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
            quantityRangeValue = Quantity(quantityRange, NoneUnit),
            lessQuantityValue = Quantity(FltX("5.0"), NoneUnit),
            overQuantityValue = Quantity(FltX("10.0"), NoneUnit),
            interval = 2.hours,
            name = "warehouse-A"
        )

        assertTrue(capacity.quantityRangeValue.value.lowerBound.value eq FltX("0"))
        assertTrue(capacity.quantityRangeValue.value.upperBound.value eq FltX("100"))
        assertTrue(capacity.lessQuantityValue!!.value eq FltX("5.0"))
        assertTrue(capacity.overQuantityValue!!.value eq FltX("10.0"))
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
            quantityRangeValue = Quantity(quantityRange, NoneUnit)
        )

        assertTrue(capacity.quantityRangeValue.value.lowerBound.value eq FltX("10"))
        assertTrue(capacity.quantityRangeValue.value.upperBound.value eq FltX("50"))
        assertNull(capacity.lessQuantityValue)
        assertNull(capacity.overQuantityValue)
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
            quantityRangeValue = Quantity(quantityRange, NoneUnit)
        )

        assertTrue(FltX("50") in capacity.quantityRangeValue.value)
        assertTrue(FltX("0") in capacity.quantityRangeValue.value)
        assertTrue(FltX("100") in capacity.quantityRangeValue.value)
    }

    @Test
    fun resourceCapacityFixedValueRangeWithFltX() {
        val fixedRange = ValueRange(FltX("25"), FltX("25"), Interval.Closed, Interval.Closed, FltX).value!!
        val capacity = ResourceCapacity<FltX>(
            time = timeRange,
            quantityRangeValue = Quantity(fixedRange, NoneUnit),
            interval = 1.hours
        )

        assertTrue(capacity.quantityRangeValue.value.fixed)
        assertTrue(capacity.quantityRangeValue.value.fixedValue!! eq FltX("25"))
    }

    @Test
    fun resourceCapacityWithOnlyLessQuantityFltX() {
        val quantityRange = ValueRange(FltX("5"), FltX("200"), Interval.Closed, Interval.Closed, FltX).value!!
        val capacity = ResourceCapacity<FltX>(
            time = timeRange,
            quantityRangeValue = Quantity(quantityRange, NoneUnit),
            lessQuantityValue = Quantity(FltX("2.5"), NoneUnit)
        )

        assertTrue(capacity.lessEnabled)
        assertTrue(!capacity.overEnabled)
        assertTrue(capacity.lessQuantityValue!!.value eq FltX("2.5"))
    }

    @Test
    fun resourceCapacityWithOnlyOverQuantityFltX() {
        val quantityRange = ValueRange(FltX("0"), FltX("500"), Interval.Closed, Interval.Closed, FltX).value!!
        val capacity = ResourceCapacity<FltX>(
            time = timeRange,
            quantityRangeValue = Quantity(quantityRange, NoneUnit),
            overQuantityValue = Quantity(FltX("15.0"), NoneUnit)
        )

        assertTrue(!capacity.lessEnabled)
        assertTrue(capacity.overEnabled)
        assertTrue(capacity.overQuantityValue!!.value eq FltX("15.0"))
    }
}
