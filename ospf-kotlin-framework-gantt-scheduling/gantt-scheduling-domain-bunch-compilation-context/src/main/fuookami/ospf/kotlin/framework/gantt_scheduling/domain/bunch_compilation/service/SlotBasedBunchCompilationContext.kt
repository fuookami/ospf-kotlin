@file:Suppress("DEPRECATION")
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service

import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.BunchCompilationContext
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.CapacityIntermediateValues
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.SlotBasedBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.SlotConstraints
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot

/**
 * 分时隙任务束编译上下文接口
 * Slot-based bunch compilation context interface
 *
 * 扩展 BunchCompilationContext，增加时隙相关功能。
 * Extends BunchCompilationContext with slot-related functionality.
 *
 * 提供产能预求解功能，获取时隙级中间值。
 * Provides capacity pre-solving functionality to obtain slot-level intermediate values.
 *
 * 注意：capacityPreSolver / preSolveCapacity / slotConstraints 继续使用 Flt64，
 * 因为产能预求解器直接操作 solver 模型（solver boundary）。
 * Note: capacityPreSolver / preSolveCapacity / slotConstraints continue to use Flt64,
 * because the capacity pre-solver operates directly on the solver model (solver boundary).
 */
interface SlotBasedBunchCompilationContextV<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        B,
        V : RealNumber<V>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        Action : ProductionAction,
        M,
        R
        > : BunchCompilationContext<Args, B, V, T, E, A>
        where B : AbstractTaskBunch<T, E, A, V>, B : SlotBasedBunch<T, E, A> {

    /**
     * 时隙列表
     * List of time slots
     */
    val slots: List<TimeSlot>

    /**
     * 产能预求解器 (solver boundary — Flt64)
     * Capacity pre-solver
     */
    val capacityPreSolver: SlotBasedCapacityPreSolver<E, Action, M, R>

    /**
     * 产能中间值（预求解后填充）
     * Capacity intermediate values (populated after pre-solving)
     */
    val intermediateValues: CapacityIntermediateValues<Action, M, R, Flt64>?

    /**
     * 执行产能预求解
     * Execute capacity pre-solving
     *
     * 先求解 capacity scheduling 问题，然后提取中间值。
     * First solves the capacity scheduling problem, then extracts intermediate values.
     *
     * @param model Linear meta model / 线性元模型 (solver boundary — Flt64)
     * @param solver Solver / 求解器
     * @return Intermediate values / 中间值
     */
    suspend fun preSolveCapacity(
        model: AbstractLinearMetaModel<Flt64>,
        solver: CapacityPreSolveSolver
    ): Ret<CapacityIntermediateValues<Action, M, R, Flt64>>

    /**
     * 获取指定时隙的约束
     * Get constraints for specified slot
     *
     * @param slot The time slot / 时隙
     * @param tolerance Tolerance for constraint bounds / 约束边界的容差
     * @return Slot constraints / 时隙约束
     */
    fun slotConstraints(slot: TimeSlot, tolerance: Flt64 = Flt64.zero): SlotConstraints<M, R, Flt64>? {
        return intermediateValues?.slotConstraints(slot, tolerance)
    }

    /**
     * 获取所有时隙的约束
     * Get constraints for all slots
     *
     * @param tolerance Tolerance for constraint bounds / 约束边界的容差
     * @return Map of slot to constraints / 时隙到约束的映射
     */
    fun allSlotConstraints(tolerance: Flt64 = Flt64.zero): Map<TimeSlot, SlotConstraints<M, R, Flt64>> {
        return slots.mapNotNull { slot ->
            intermediateValues?.slotConstraints(slot, tolerance)?.let { slot to it }
        }.toMap()
    }

    /**
     * 按时隙添加列
     * Add columns by slot
     *
     * @param iteration Current iteration number / 当前迭代号
     * @param newBunches New bunches to add / 要添加的新 bunch
     * @param model Linear meta model / 线性元模型 (solver boundary — Flt64)
     * @return Added bunches grouped by slot / 按时隙分组的已添加 bunch
     */
    suspend fun addColumnsBySlot(
        iteration: UInt64,
        newBunches: List<B>,
        model: AbstractLinearMetaModel<Flt64>
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
