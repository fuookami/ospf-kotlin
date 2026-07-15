/** 分时隙任务束聚合模型 / Slot-based bunch aggregation model */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 分时隙任务束聚合 / Slot-based bunch aggregation
 *
 * @param B 任务束类型 / Bunch type
 * @param V 数值类型 / Numeric type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
*/
open class SlotBasedBunchAggregation<
        B,
        V : RealNumber<V>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        > : BunchAggregation<B, V, T, E, A>()
        where B : AbstractTaskBunch<T, E, A, V>, B : SlotBasedBunch<T, E, A> {
    protected override infix fun B.sameColumnAs(other: B): Boolean {
        return !(this neq other)
            && this.slotIndex == other.slotIndex
            && this.slot == other.slot
    }
}
