@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearExpressionSymbols2
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.utils.functional.Ret

/**
 * 产能编译抽象接口
 * Capacity Compilation Abstract Interface
 *
 * Provides unified capacity calculation interface for constraints and objectives.
 * 提供统一的产能计算接口，用于约束和目标函数。
 */
interface Capacity<A : ProductionAction> {
    /**
     * 执行器列表（与 capacity 第一维索引一致）
     * Executor list (aligned with the first dimension index of capacity)
     */
    val executors: List<Executor>

    /**
     * 动作-时隙的操作时间
     * Operation time per action-slot
     *
     * Represents capacity (duration) allocated to each action in each time slot.
     * 表示每个动作在每个时隙分配的产能（时长）。
     */
    val operationTime: LinearExpressionSymbols2  // [action, slot] -> duration

    /**
     * 设备-时隙的总产能
     * Total capacity per executor-slot
     *
     * Represents total capacity (duration) per executor in each time slot.
     * 表示每个设备在每个时隙的总产能（时长）。
     */
    val capacity: LinearExpressionSymbols2  // [executor, slot] -> duration

    /**
     * 解析解
     * Extract solution from model
     *
     * @param model Linear meta model / 线性元模型
     * @return Capacity scheduling solution / 产能调度解
     */
    fun extractSolution(model: AbstractLinearMetaModel): Ret<CapacitySchedulingSolution<A>>
}
