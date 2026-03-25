@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service

import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.BunchCompilationContext
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.CapacityIntermediateValues
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.SlotBasedBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.SlotConstraints
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.UInt64

/**
 * 分时隙任务束编译上下文接口
 * Slot-based bunch compilation context interface
 *
 * 扩展 BunchCompilationContext，增加时隙相关功能。
 * Extends BunchCompilationContext with slot-related functionality.
 *
 * 提供产能预求解功能，获取时隙级中间值。
 * Provides capacity pre-solving functionality to obtain slot-level intermediate values.
 */
interface SlotBasedBunchCompilationContext<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        B,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        Action : ProductionAction,
        M,
        R
        > : BunchCompilationContext<Args, B, T, E, A>
        where B : AbstractTaskBunch<T, E, A>, B : SlotBasedBunch<T, E, A> {

    /**
     * 时隙列表
     * List of time slots
     */
    val slots: List<TimeSlot>

    /**
     * 产能预求解器
     * Capacity pre-solver
     */
    val capacityPreSolver: SlotBasedCapacityPreSolver<E, Action, M, R>

    /**
     * 产能中间值（预求解后填充）
     * Capacity intermediate values (populated after pre-solving)
     */
    val intermediateValues: CapacityIntermediateValues<Action, M, R>?

    /**
     * 执行产能预求解
     * Execute capacity pre-solving
     *
     * 先求解 capacity scheduling 问题，然后提取中间值。
     * First solves the capacity scheduling problem, then extracts intermediate values.
     *
     * @param model Linear meta model / 线性元模型
     * @param solver Solver / 求解器
     * @return Intermediate values / 中间值
     */
    suspend fun preSolveCapacity(
        model: AbstractLinearMetaModel,
        solver: CapacityPreSolveSolver
    ): Ret<CapacityIntermediateValues<Action, M, R>>

    /**
     * 获取指定时隙的约束
     * Get constraints for specified slot
     *
     * @param slot The time slot / 时隙
     * @param tolerance Tolerance for constraint bounds / 约束边界的容差
     * @return Slot constraints / 时隙约束
     */
    fun slotConstraints(slot: TimeSlot, tolerance: Flt64 = Flt64.zero): SlotConstraints<M, R>? {
        return intermediateValues?.slotConstraints(slot, tolerance)
    }

    /**
     * 获取所有时隙的约束
     * Get constraints for all slots
     *
     * @param tolerance Tolerance for constraint bounds / 约束边界的容差
     * @return Map of slot to constraints / 时隙到约束的映射
     */
    fun allSlotConstraints(tolerance: Flt64 = Flt64.zero): Map<TimeSlot, SlotConstraints<M, R>> {
        return slots.mapNotNull { slot ->
            intermediateValues?.slotConstraints(slot, tolerance)?.let { slot to it }
        }.toMap()
    }

    /**
     * 按时隙添加列
     * Add columns by slot
     *
     * @param iteration Current iteration number / 当前迭代数
     * @param newBunches New bunches to add / 要添加的新 bunch
     * @param model Linear meta model / 线性元模型
     * @return Added bunches grouped by slot / 按时隙分组的已添加 bunch
     */
    suspend fun addColumnsBySlot(
        iteration: UInt64,
        newBunches: List<B>,
        model: AbstractLinearMetaModel
    ): Ret<Map<TimeSlot, List<B>>>

    /**
     * 获取指定时隙的所有 bunch
     * Get all bunches for specified slot
     *
     * @param slot The time slot / 时隙
     * @return List of bunches in this slot / 该时隙的 bunch 列表
     */
    fun bunchesInSlot(slot: TimeSlot): List<B>
}
