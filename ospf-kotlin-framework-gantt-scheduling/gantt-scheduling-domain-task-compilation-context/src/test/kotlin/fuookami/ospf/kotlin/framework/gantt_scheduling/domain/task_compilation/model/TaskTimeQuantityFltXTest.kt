package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols2
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols3
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.GenericSolverValueAdapter
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.SchedulingSolverValueAdapter
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.multiarray.Shape2
import fuookami.ospf.kotlin.multiarray.Shape3
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

class TaskTimeQuantityFltXTest {
    private val task = QuantityTask(index = 0, id = "task-0")
    private val model = LinearMetaModel(
        name = "task-time-quantity-test",
        converter = SchedulingSolverValueAdapter.Flt64
    )
    private val adapter = GenericSolverValueAdapter(FltX)

    @Test
    fun taskTimeShouldExposeSolvedFltXQuantities() {
        val taskTime = ConstantTaskTime()

        val estimateStart = taskTime.estimateStartTimeQuantity(
            task = task,
            model = model,
            adapter = adapter
        )
        val estimateEnd = taskTime.estimateEndTimeQuantity(
            task = task,
            model = model,
            adapter = adapter
        )
        val delay = taskTime.delayTimeQuantity(
            task = task,
            model = model,
            adapter = adapter
        )
        val advance = taskTime.advanceTimeQuantity(
            task = task,
            model = model,
            adapter = adapter
        )
        val delayLastEndTime = taskTime.delayLastEndTimeQuantity(
            task = task,
            model = model,
            adapter = adapter
        )
        val advanceEarliestEndTime = taskTime.advanceEarliestEndTimeQuantity(
            task = task,
            model = model,
            adapter = adapter
        )

        assertEquals(NoneUnit, estimateStart!!.unit)
        assertTrue(estimateStart.value eq FltX("1.5"))
        assertTrue(estimateEnd!!.value eq FltX("3.5"))
        assertTrue(delay!!.value eq FltX("0.25"))
        assertTrue(advance!!.value eq FltX("0.75"))
        assertTrue(delayLastEndTime!!.value eq FltX("1.25"))
        assertTrue(advanceEarliestEndTime!!.value eq FltX("1.75"))
    }

    @Test
    fun switchShouldExposeSolvedFltXQuantity() {
        val switch = ConstantSwitch()

        val quantity = switch.switchTimeQuantity(
            from = task,
            to = task,
            model = model,
            adapter = adapter
        )

        assertEquals(NoneUnit, quantity!!.unit)
        assertTrue(quantity.value eq FltX("2.25"))
    }

    @Test
    fun makespanShouldExposeSolvedFltXQuantity() {
        val makespan = Makespan(
            tasks = listOf(task),
            taskTime = ConstantTaskTime()
        )
        makespan.makespan = LinearExpressionSymbol(Flt64(4.5), name = "makespan")

        val quantity = makespan.quantity(
            model = model,
            adapter = adapter
        )

        assertEquals(NoneUnit, quantity!!.unit)
        assertTrue(quantity.value eq FltX("4.5"))
    }
}

private data class QuantityTask(
    override val index: Int,
    override val id: String,
    override val name: String = id
) : AbstractTask<Executor, AssignmentPolicy<Executor>> {
    override fun partialEq(rhs: AbstractTask<Executor, AssignmentPolicy<Executor>>): Boolean? {
        return this === rhs
    }
}

private class ConstantTaskTime : TaskTime {
    override val delayEnabled: Boolean = true
    override val overMaxDelayEnabled: Boolean = false
    override val advanceEnabled: Boolean = true
    override val overMaxAdvanceEnabled: Boolean = false
    override val delayLastEndTimeEnabled: Boolean = false
    override val advanceEarliestEndTimeEnabled: Boolean = false
    override val estimateStartTime = constantSymbols("estimate_start_time", Flt64(1.5))
    override val estimateEndTime = constantSymbols("estimate_end_time", Flt64(3.5))
    override val delayTime = constantSymbols("delay_time", Flt64(0.25))
    override val advanceTime = constantSymbols("advance_time", Flt64(0.75))
    override val overMaxDelayTime = constantSymbols("over_max_delay_time", Flt64.zero)
    override val overMaxAdvanceTime = constantSymbols("over_max_advance_time", Flt64.zero)
    override val delayLastEndTime = constantSymbols("delay_last_end_time", Flt64(1.25))
    override val advanceEarliestEndTime = constantSymbols("advance_earliest_end_time", Flt64(1.75))
    override val onTime = constantSymbols("on_time", Flt64.one)
    override val notOnTime = constantSymbols("not_on_time", Flt64.zero)

    override fun register(model: MetaModel<Flt64>): Try {
        return ok
    }

    private fun constantSymbols(
        name: String,
        value: Flt64
    ): LinearIntermediateSymbols1<Flt64> {
        return LinearIntermediateSymbols1(
            name = name,
            shape = Shape1(1)
        ) { _, _ ->
            LinearExpressionSymbol(value, name = name)
        }
    }
}

private class ConstantSwitch : Switch {
    override val switch = LinearIntermediateSymbols3<Flt64>(
        name = "switch",
        shape = Shape3(1, 1, 1)
    ) { _, _ ->
        LinearExpressionSymbol(Flt64.one, name = "switch")
    }
    override val switchTime = LinearIntermediateSymbols2<Flt64>(
        name = "switch_time",
        shape = Shape2(1, 1)
    ) { _, _ ->
        LinearExpressionSymbol(Flt64(2.25), name = "switch_time")
    }

    override fun register(model: MetaModel<Flt64>): Try {
        return ok
    }
}
