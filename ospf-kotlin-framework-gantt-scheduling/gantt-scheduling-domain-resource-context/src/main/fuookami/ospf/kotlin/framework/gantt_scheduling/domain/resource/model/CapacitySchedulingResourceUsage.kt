@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearExpressionSymbols1
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.utils.multi_array.Shape1
import kotlin.time.Duration

/**
 * 产能调度场景的资源使用量管理抽象基类
 * Abstract base class for resource usage in Capacity Scheduling
 *
 * 提供产能调度场景下资源使用量计算的通用框架
 * Provides a common framework for resource usage calculation in capacity scheduling scenarios
 */
abstract class CapacitySchedulingResourceUsage<
        A : ProductionAction,
        S : ResourceTimeSlot<R, C>,
        R : Resource<C>,
        C : AbstractResourceCapacity
        >(
    protected val timeWindow: TimeWindow,
    resources: List<R>,
    protected val actions: List<A>,
    interval: Duration = timeWindow.interval
) : AbstractResourceUsage<S, R, C>() {

    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    abstract override val name: String
    abstract override var quantity: LinearExpressionSymbols1

    /**
     * 注册变量到模�?
     * Register variables to model
     */
    abstract fun register(model: LinearMetaModel): Try

    /**
     * 初始�?quantity 变量
     * Initialize quantity variables
     *
     * 子类应在 init 块中先计�?timeSlots，然后调用此方法初始�?quantity
     * Subclasses should calculate timeSlots first in init block, then call this method
     */
    protected fun initQuantity(timeSlots: List<S>) {
        quantity = LinearExpressionSymbols1(
            name = "${name}_quantity",
            shape = Shape1(timeSlots.size)
        ) { s, _ ->
            val slot = timeSlots[s]
            LinearExpressionSymbol(
                name = "${name}_quantity_${slot}"
            )
        }
        for (slot in timeSlots) {
            quantity[slot].range.set(
                ValueRange(
                    slot.resourceCapacity.quantity.lowerBound.value.unwrap() - (slot.resourceCapacity.lessQuantity ?: Flt64.zero),
                    slot.resourceCapacity.quantity.upperBound.value.unwrap() + (slot.resourceCapacity.overQuantity ?: Flt64.zero)
                ).value!!
            )
        }
    }

    /**
     * �?quantity 变量添加到模�?
     * Add quantity variables to model
     */
    protected fun addQuantityToModel(model: LinearMetaModel, timeSlots: List<S>): Try {
        if (timeSlots.isNotEmpty()) {
            when (val result = model.add(quantity)) {
                is Ok -> {}
                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }
}



