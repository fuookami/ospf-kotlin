@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.MaterialDemand
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ResourceCapacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Cost
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.CostItem
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.GenericSolverValueAdapter
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.IterativeAbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskSolution
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.model.task.Iteration

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
    val timeWindow = TimeWindow(
        window = time,
        durationUnit = DurationUnit.HOURS,
        interval = 1.toDuration(DurationUnit.HOURS),
        fromDouble = { FltX(it.toString()) },
        toDouble = { it.toDouble() }
    )
    val timeWindowDurationQuantity = timeWindow.quantityOf(2.toDuration(DurationUnit.HOURS))
    val timeWindowInstantQuantity = timeWindow.quantityOf(Instant.parse("2026-06-07T03:00:00Z"))
    val iterationSnapshot = Iteration<Demo4Task, Executor, AssignmentPolicy<Executor>>()
        .snapshot(GenericSolverValueAdapter(FltX))
    val taskSolutionSummary = TaskSolution(
        assignedTasks = listOf(Demo4Task(id = "assigned", name = "assigned")),
        canceledTasks = listOf(Demo4Task(id = "canceled", name = "canceled"))
    ).summary

    @Suppress("unused")
    val rawQuantity = Quantity(FltX("3.5"), costQuantity!!.unit)

    @Suppress("unused")
    val quantityFieldSum = quantityFieldCost.costSum
}

private data class Demo4Task(
    override val index: Int = 0,
    override val id: String,
    override val name: String,
    override val iteration: Int64 = Int64.zero
) : IterativeAbstractTask<Executor, AssignmentPolicy<Executor>> {
    override fun partialEq(rhs: AbstractTask<Executor, AssignmentPolicy<Executor>>): Boolean? {
        return this === rhs
    }
}
