@file:OptIn(kotlin.time.ExperimentalTime::class)
/** 分时隙任务束接口模型 / Slot-based task bunch interface model */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

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