@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot

/**
 * 分时隙任务束接口
 * Slot-based task bunch interface
 *
 * 一个 SlotBasedBunch 只能属于一个时隙。
 * 时隙对应关系由 bunch 生成器保证。
 *
 * A SlotBasedBunch can only belong to one time slot.
 * The slot correspondence is ensured by the bunch generator.
 */
interface SlotBasedBunch<
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        > {

    /**
     * 所属时隙
     * The time slot this bunch belongs to
     */
    val slot: TimeSlot

    /**
     * 时隙索引
     * Slot index in the time window
     */
    val slotIndex: Int
}
