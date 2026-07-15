@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow

class TaskQuantityFltXPathTest {
    @Test
    fun costShouldSupportFltX() {
        val cost = Cost(
            items = listOf(
                CostItem(
                    tag = "setup",
                    costQuantity = Quantity(FltX("1.25"), NoneUnit)
                ),
                CostItem(
                    tag = "processing",
                    costQuantity = Quantity(FltX("2.75"), NoneUnit)
                )
            ),
            costSum = Quantity(FltX("4.00"), NoneUnit)
        )

        assertTrue(cost.valid)
        assertTrue(cost.costSum!!.value eq FltX("4.00"))
    }

    @Test
    fun costQuantityShouldSupportFltX() {
        val item = CostItem(
            tag = "processing",
            costQuantity = Quantity(FltX("2.75"), NoneUnit)
        )
        val cost = Cost(
            items = listOf(item),
            costSum = Quantity(FltX("2.75"), NoneUnit)
        )

        val itemQuantity = item.quantity()
        val sumQuantity = cost.sumQuantity()

        assertEquals(NoneUnit, itemQuantity!!.unit)
        assertTrue(itemQuantity.value eq FltX("2.75"))
        assertEquals(NoneUnit, sumQuantity!!.unit)
        assertTrue(sumQuantity.value eq FltX("2.75"))
    }

    @Test
    fun costShouldSupportFltXQuantityFields() {
        val item = CostItem(
            tag = "quantity-processing",
            costQuantity = Quantity(FltX("2.75"), NoneUnit)
        )
        val cost = Cost(
            items = listOf(item),
            costSum = Quantity(FltX("2.75"), NoneUnit)
        )

        assertTrue(item.costQuantity!!.value eq FltX("2.75"))
        assertEquals(NoneUnit, item.costQuantity!!.unit)
        assertTrue(cost.valid)
        assertTrue(cost.costSum!!.value eq FltX("2.75"))
        assertEquals(NoneUnit, cost.costSum!!.unit)
    }

    @Test
    fun timeWindowShouldSupportFltX() {
        val timeWindow = TimeWindow.minutes(
            timeWindow = TimeRange(
                start = Instant.parse("2020-08-30T08:00:00Z"),
                end = Instant.parse("2020-08-30T10:00:00Z")
            ),
            dateOffset = FltX.zero,
            interval = FltX("15.0"),
            fromDouble = { FltX(it.toString()) },
            toDouble = { it.toDouble() }
        )

        val instant = Instant.parse("2020-08-30T09:30:00Z")

        assertTrue(timeWindow.valueOf(instant) eq FltX(90.0))
        assertEquals(Instant.parse("2020-08-30T08:45:00Z"), timeWindow.instantOf(FltX(45.0)))
        assertEquals(30.minutes, timeWindow.durationOf(FltX(30.0)))
    }
}
