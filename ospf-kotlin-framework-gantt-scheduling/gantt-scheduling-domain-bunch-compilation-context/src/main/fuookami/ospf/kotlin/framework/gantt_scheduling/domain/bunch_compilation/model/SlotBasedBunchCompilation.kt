@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.frontend.variable.BinVariable1
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTaskBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.math.UInt64

/**
 * 分时隙任务束编译类
 * Slot-based bunch compilation class
 *
 * 继承 BunchCompilation，增加时隙相关功能。
 * Extends BunchCompilation with slot-related functionality.
 *
 * 每个 bunch 只能属于一个时隙，时隙对应关系由 bunch 生成器保证。
 * Each bunch can only belong to one time slot, ensured by the bunch generator.
 */
open class SlotBasedBunchCompilation<
        B,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val tasks: List<T>,
    private val executors: List<E>,
    private val slots: List<TimeSlot>,
    private val lockCancelTasks: Set<T> = emptySet(),
    override val withExecutorLeisure: Boolean = true
) : BunchCompilation<B, T, E, A>(tasks, executors, lockCancelTasks, withExecutorLeisure)
        where B : AbstractTaskBunch<T, E, A>, B : SlotBasedBunch<T, E, A> {

    /**
     * 按时隙分组的 bunches
     * Bunches grouped by slot
     */
    val bunchesBySlot: Map<TimeSlot, List<B>>
        get() = bunches.groupBy { it.slot }

    /**
     * 按时隙分组的 bunch 变量
     * Bunch variables grouped by slot
     */
    private val _xBySlot = HashMap<TimeSlot, ArrayList<BinVariable1>>()
    val xBySlot: Map<TimeSlot, List<BinVariable1>>
        get() = _xBySlot.mapValues { it.value.toList() }

    /**
     * 按时隙添加列
     * Add columns by slot
     *
     * @param iteration Current iteration number / 当前迭代数
     * @param newBunches New bunches to add / 要添加的新 bunch
     * @param model Linear meta model / 线性元模型
     * @return Added bunches grouped by slot / 按时隙分组的已添加 bunch
     */
    open suspend fun addColumnsBySlot(
        iteration: UInt64,
        newBunches: List<B>,
        model: AbstractLinearMetaModel
    ): Ret<Map<TimeSlot, List<B>>> {
        // First add columns using parent method
        // 首先使用父类方法添加列
        val unduplicatedBunches = when (val result = addColumns(iteration, newBunches, model)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // Group by slot and track variables
        // 按时隙分组并跟踪变量
        val bunchesBySlot = HashMap<TimeSlot, MutableList<B>>()
        for (bunch in unduplicatedBunches) {
            bunchesBySlot.getOrPut(bunch.slot) { mutableListOf() }.add(bunch)
        }

        // Update xBySlot tracking
        // 更新 xBySlot 跟踪
        for ((slot, slotBunches) in bunchesBySlot) {
            val slotVariables = _xBySlot.getOrPut(slot) { ArrayList() }
            // Find the last added x variable (corresponding to this iteration)
            // 找到最后添加的 x 变量（对应此迭代）
            if (x.isNotEmpty()) {
                slotVariables.add(x.last())
            }
        }

        @Suppress("UNCHECKED_CAST")
        return Ok(bunchesBySlot.mapValues { it.value.toList() } as Map<TimeSlot, List<B>>)
    }

    /**
     * 获取指定时隙的所有 bunch
     * Get all bunches for specified slot
     *
     * @param slot The time slot / 时隙
     * @return List of bunches in this slot / 该时隙的 bunch 列表
     */
    fun bunchesInSlot(slot: TimeSlot): List<B> {
        return bunchesBySlot[slot] ?: emptyList()
    }

    /**
     * 获取指定时隙和执行器的所有 bunch
     * Get all bunches for specified slot and executor
     *
     * @param slot The time slot / 时隙
     * @param executor The executor / 执行器
     * @return List of bunches in this slot for this executor / 该时隙该执行器的 bunch 列表
     */
    fun bunchesInSlot(slot: TimeSlot, executor: E): List<B> {
        return bunchesBySlot[slot]?.filter { it.executor == executor } ?: emptyList()
    }
}
