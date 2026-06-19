@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.AbstractResourceCapacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ExecutionResource
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ExecutionResourceTimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ResourceCapacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ResourceUsage
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.GenericSolverValueAdapter
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.schedulingSolverValueAdapter
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

class ResourceSolvedQuantityFltXTest {
    private val time = TimeRange(
        start = Instant.parse("2026-06-07T00:00:00Z"),
        end = Instant.parse("2026-06-07T01:00:00Z")
    )
    private val capacity = ResourceCapacity(
        time = time,
        quantityRangeValue = Quantity(
            value = ValueRange(
                lb = FltX("0"),
                ub = FltX("100"),
                lbInterval = Interval.Closed,
                ubInterval = Interval.Closed,
                constants = FltX
            ).value!!,
            unit = NoneUnit
        )
    )
    private val resource = object : ExecutionResource<ResourceCapacity<FltX>, FltX>(
        id = "resource",
        name = "resource",
        capacities = listOf(capacity),
        initialQuantityValue = FltX.zero
    ) {
        override fun <E : Executor, A : AssignmentPolicy<E>> usedBy(
            task: AbstractTask<E, A>,
            time: TimeRange
        ): FltX {
            return FltX("0")
        }
    }
    private val slot = ExecutionResourceTimeSlot(
        origin = time,
        resource = resource,
        resourceCapacity = capacity,
        indexInRule = UInt64.zero
    )
    private val model = LinearMetaModel(
        name = "resource-solved-quantity-test",
        converter = schedulingSolverValueAdapter
    )
    private val adapter = GenericSolverValueAdapter(FltX)

    @Test
    fun resourceUsageShouldExposeSolvedFltXQuantities() {
        val usage = ConstantResourceUsage(
            slot = slot,
            quantityValue = Flt64(7.5),
            overValue = Flt64(1.25),
            lessValue = Flt64(0.5)
        )

        val quantity = usage.solvedQuantity(
            slot = slot,
            model = model,
            adapter = adapter
        )
        val overQuantity = usage.solvedOverQuantity(
            slot = slot,
            model = model,
            adapter = adapter
        )
        val lessQuantity = usage.solvedLessQuantity(
            slot = slot,
            model = model,
            adapter = adapter
        )

        assertEquals(NoneUnit, quantity!!.unit)
        assertTrue(quantity.value eq FltX("7.5"))
        assertTrue(overQuantity!!.value eq FltX("1.25"))
        assertTrue(lessQuantity!!.value eq FltX("0.5"))
    }
}

private class ConstantResourceUsage<
        R : ExecutionResource<C, FltX>,
        C : AbstractResourceCapacity<FltX>
        >(
    slot: ExecutionResourceTimeSlot<R, C, FltX>,
    quantityValue: Flt64,
    overValue: Flt64,
    lessValue: Flt64
) : ResourceUsage<ExecutionResourceTimeSlot<R, C, FltX>, R, C, FltX> {
    override val name: String = "constant_resource_usage"
    override val timeSlots = listOf(slot)
    override val quantity = constantSymbols("resource_quantity", slot.index + 1, quantityValue)
    override val overQuantity = constantSymbols("resource_over_quantity", slot.index + 1, overValue)
    override val lessQuantity = constantSymbols("resource_less_quantity", slot.index + 1, lessValue)
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override fun register(model: MetaModel<Flt64>): Try {
        return ok
    }

    private fun constantSymbols(
        name: String,
        size: Int,
        value: Flt64
    ): LinearIntermediateSymbols1<Flt64> {
        return LinearIntermediateSymbols1(
            name = name,
            shape = Shape1(size)
        ) { _, _ ->
            LinearExpressionSymbol(value, name = name)
        }
    }
}
