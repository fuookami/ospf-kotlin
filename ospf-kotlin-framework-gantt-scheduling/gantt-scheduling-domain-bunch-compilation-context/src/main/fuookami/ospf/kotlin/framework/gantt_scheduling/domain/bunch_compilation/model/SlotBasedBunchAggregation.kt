@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTaskBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.math.algebra.number.Flt64

open class SlotBasedBunchAggregation<
        B,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        > : BunchAggregation<B, Flt64, T, E, A>()
        where B : AbstractTaskBunch<T, E, A, Flt64>, B : SlotBasedBunch<T, E, A> {
    protected override infix fun B.sameColumnAs(other: B): Boolean {
        return !(this neq other)
            && this.slotIndex == other.slotIndex
            && this.slot == other.slot
    }
}