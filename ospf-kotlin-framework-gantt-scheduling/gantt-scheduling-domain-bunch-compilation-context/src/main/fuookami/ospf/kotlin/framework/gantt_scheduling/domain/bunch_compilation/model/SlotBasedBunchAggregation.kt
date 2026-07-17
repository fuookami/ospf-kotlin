/** 分时隙任务束聚合模型 / Slot-based bunch aggregation model */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.BunchCompilationAggregation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot

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

/**
 * 使用权威时隙变量映射的任务束编译聚合。
 *
 * 与普通 [BunchCompilationAggregation] 保持相同的上下文接口，但把内部编译模型
 * 替换为 [SlotBasedBunchCompilation]，使 `(executor, slot)` 选择表达式和新增列的
 * 真实变量映射可以被上层列生成算法直接复用。
 */
open class SlotBasedBunchCompilationAggregation<
        B,
        V : RealNumber<V>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    tasks: List<T>,
    executors: List<E>,
    slots: List<TimeSlot>,
    lockCancelTasks: Set<T> = emptySet(),
    withExecutorLeisure: Boolean = true
        ) : BunchCompilationAggregation<B, V, T, E, A>(
    tasks = tasks,
    executors = executors,
    lockCancelTasks = lockCancelTasks,
    withExecutorLeisure = withExecutorLeisure,
    compilation = SlotBasedBunchCompilation(
        tasks = tasks,
        executors = executors,
        slots = slots,
        lockCancelTasks = lockCancelTasks,
        withExecutorLeisure = withExecutorLeisure
    )
        ) where B : AbstractTaskBunch<T, E, A, V>, B : SlotBasedBunch<T, E, A> {

    val slotCompilation: SlotBasedBunchCompilation<B, V, T, E, A>
        get() = compilation as SlotBasedBunchCompilation<B, V, T, E, A>
}
