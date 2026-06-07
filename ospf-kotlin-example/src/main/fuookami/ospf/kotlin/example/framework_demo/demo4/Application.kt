@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlin.time.Instant
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.MaterialDemand
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ResourceCapacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Cost
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.CostItem
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange

class Application {
}

object Demo4GenericQuantitySample {
    private val time = TimeRange(
        start = Instant.parse("2026-06-07T00:00:00Z"),
        end = Instant.parse("2026-06-07T08:00:00Z")
    )

    private val quantityRange = ValueRange(
        FltX("0"),
        FltX("100"),
        Interval.Closed,
        Interval.Closed,
        FltX
    ).value!!

    val cost = Cost(
        items = listOf(
            CostItem(
                tag = "generic-demo",
                value = FltX("1.25")
            )
        ),
        constants = FltX
    )

    val quantityFieldCost = Cost(
        items = listOf(
            CostItem(
                tag = "generic-quantity-demo",
                costQuantity = Quantity(FltX("2.50"), NoneUnit)
            )
        ),
        costSum = Quantity(FltX("2.50"), NoneUnit)
    )

    val costQuantity = cost.sumQuantity()
    val materialDemandQuantity = MaterialDemand(
        quantityRangeValue = Quantity(quantityRange, NoneUnit)
    ).quantityRange()
    val resourceCapacityQuantity = ResourceCapacity(
        time = time,
        quantityRangeValue = Quantity(quantityRange, NoneUnit)
    ).quantityRange()

    @Suppress("unused")
    val rawQuantity = Quantity(FltX("3.5"), costQuantity!!.unit)

    @Suppress("unused")
    val quantityFieldSum = quantityFieldCost.costSum
}
