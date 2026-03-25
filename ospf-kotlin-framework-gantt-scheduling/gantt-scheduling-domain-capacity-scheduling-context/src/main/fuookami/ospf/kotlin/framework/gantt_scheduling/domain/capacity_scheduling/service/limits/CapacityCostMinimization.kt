package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.service.limits

import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.Capacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.CapacityCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.CapacityOrderCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.IterativeCapacityCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 产能成本最小化目标
 * Capacity Cost Minimization Objective
 *
 * 最小化产能调度的总成本。
 * Minimizes total cost of capacity scheduling.
 *
 * 成本 = sum(action.unitCost * x[action, slot])
 * Cost = sum(action.unitCost * x[action, slot])
 *
 * @param A 生产动作类型 / Production action type
 */
class CapacityCostMinimization<A : ProductionAction>(
    /**
     * 产能编译对象
     * Capacity compilation object
     */
    private val capacity: Capacity<A>,

    /**
     * 生产动作列表
     * List of production actions
     */
    private val actions: List<A>,

    /**
     * 时隙列表
     * List of time slots
     */
    private val slots: List<TimeSlot>,

    /**
     * 时间窗口
     * Time window
     */
    private val timeWindow: TimeWindow,

    /**
     * 目标名称
     * Objective name
     */
    val name: String = "capacity_cost_minimization"
) {
    /**
     * 应用目标到模型
     * Apply objective to model
     *
     * 设置最小化成本目标函数。
     * Sets up the minimization cost objective function.
     *
     * @param model Linear meta model / 线性元模型
     * @return Try result / Try 结果
     */
    operator fun invoke(model: LinearMetaModel): Try {
        val costSymbol = when (capacity) {
            is CapacityCompilation<*> -> capacity.cost
            is CapacityOrderCompilation<*> -> capacity.cost
            is IterativeCapacityCompilation<*, *> -> capacity.cost
            else -> null
        } ?: return Failed(
            ErrorCode.IllegalArgument,
            "capacity_cost_minimization requires a registered concrete capacity compilation. " +
                "actions=${actions.size}, slots=${slots.size}, durationUnit=${timeWindow.durationUnit}."
        )

        // Set objective to minimize cost
        // 设置目标为最小化成本
        when (val result = model.minimize(symbol = costSymbol, name = name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }
}
