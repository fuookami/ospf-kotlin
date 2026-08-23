@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 分时隙任务束编译上下文服务 / Slot-based bunch compilation context service */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service

import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.BunchCompilationContext
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*

/**
 * 分时隙任务束编译上下文接口 / Slot-based bunch compilation context interface
 *
 * 扩展 BunchCompilationContext，增加时隙相关功能。
 * Extends BunchCompilationContext with slot-related functionality.
 *
 * 提供产能预求解功能，获取时隙级中间值。 / Provides capacity pre-solving functionality to obtain slot-level intermediate values.
*/
interface SlotBasedBunchCompilationContext<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        B,
        V,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        Action : ProductionAction,
        M,
        R
        > : BunchCompilationContext<Args, B, V, T, E, A>
        where V : RealNumber<V>, V : PlusGroup<V>, B : AbstractTaskBunch<T, E, A, V>, B : SlotBasedBunch<T, E, A> {

    /**
     * 时隙列表 / List of time slots
    */
    val slots: List<TimeSlot>

    /**
     * 产能预求解器 / Capacity pre-solver
    */
    val capacityPreSolver: SlotBasedCapacityPreSolver<V, E, Action, M, R>

    /**
     * 产能中间值（预求解后填充） / Capacity intermediate values (populated after pre-solving)
    */
    val intermediateValues: CapacityIntermediateValues<Action, M, R, V>?

    /**
     * 执行产能预求解 / Execute capacity pre-solving
     *
     * 先求解 capacity scheduling 问题，然后提取中间值。
     * First solves the capacity scheduling problem, then extracts intermediate values.
     *
     * @param model 线性元模型 (solver boundary — Flt64) / Linear meta model
     * @param solver 求解器 / Solver
     * @return 中间值 / Intermediate values
    */
    suspend fun preSolveCapacity(
        model: AbstractLinearMetaModel<Flt64>,
        solver: CapacityPreSolveSolver
    ): Ret<CapacityIntermediateValues<Action, M, R, V>>

    /**
     * 获取指定时隙的约束 / Get constraints for specified slot
     *
     * @param slot 时隙 / The time slot
     * @param tolerance 约束边界的容差 / Tolerance for constraint bounds
     * @return 时隙约束 / Slot constraints
    */
    fun slotConstraints(slot: TimeSlot, tolerance: V? = null): SlotConstraints<M, R, V>? {
        return intermediateValues?.slotConstraints(slot, tolerance)
    }

    /**
     * 获取所有时隙的约束 / Get constraints for all slots
     *
     * @param tolerance 约束边界的容差 / Tolerance for constraint bounds
     * @return 时隙到约束的映射 / Map of slot to constraints
    */
    fun allSlotConstraints(tolerance: V? = null): Map<TimeSlot, SlotConstraints<M, R, V>> {
        return slots.mapNotNull { slot ->
            intermediateValues?.slotConstraints(slot, tolerance)?.let { slot to it }
        }.toMap()
    }

    /**
     * 按时隙添加列 / Add columns by slot
     *
     * @param iteration 当前迭代号 / Current iteration number
     * @param newBunches 要添加的新 bunch / New bunches to add
     * @param model 线性元模型 (solver boundary — Flt64) / Linear meta model
     * @return 按时隙分组的已添加 bunch / Added bunches grouped by slot
    */
    suspend fun addColumnsBySlot(
        iteration: UInt64,
        newBunches: List<B>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Map<TimeSlot, List<B>>>

    /**
     * 获取指定时隙的所有 bunch / Get all bunches for specified slot
     *
     * @param slot 时隙 / The time slot
     * @return 该时隙的 bunch 列表 / List of bunches in this slot
    */
    fun bunchesInSlot(slot: TimeSlot): List<B>
}
