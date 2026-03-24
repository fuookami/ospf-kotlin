@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling

import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.Capacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.CapacitySchedulingSolution
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try

/**
 * 产能调度上下文接口
 * Capacity Scheduling Context Interface
 *
 * 提供产能调度的统一抽象接口。
 * Provides unified abstract interface for capacity scheduling.
 *
 * @param A 生产动作类型 / Production action type
 */
interface CapacitySchedulingContext<A : ProductionAction> {
    /**
     * 生产动作列表
     * List of production actions
     */
    val actions: List<A>

    /**
     * 时隙列表
     * List of time slots
     */
    val slots: List<TimeSlot>

    /**
     * 时间窗口
     * Time window
     */
    val timeWindow: TimeWindow

    /**
     * 产能编译对象
     * Capacity compilation object
     */
    val compilation: Capacity<A>

    /**
     * 注册到模型
     * Register to model
     *
     * @param model Linear meta model / 线性元模型
     * @return Try result / Try 结果
     */
    fun register(model: LinearMetaModel): Try

    /**
     * 从模型解析解
     * Extract solution from model
     *
     * @param model Abstract linear meta model / 抽象线性元模型
     * @return Capacity scheduling solution / 产能调度解
     */
    fun extractSolution(model: AbstractLinearMetaModel): Ret<CapacitySchedulingSolution<A>>
}